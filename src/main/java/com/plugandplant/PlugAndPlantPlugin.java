package com.plugandplant;

import com.google.inject.Provides;
import java.awt.Color;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.EnumComposition;
import net.runelite.api.EnumID;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.NPC;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.RuneScapeProfileChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStats;
import net.runelite.client.game.ItemVariationMapping;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.plugins.banktags.BankTagsPlugin;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;

@Slf4j
@PluginDescriptor(
	name = "Plug & Plant Farming",
	description = "Herb and tree runs with whatever seeds you bring, built on Time Tracking, Bank Tags and Shortest Path",
	tags = {"farming", "farm run", "herb run", "tree run", "ironman", "herbs", "trees", "allotment", "flower", "contract"}
)
@PluginDependency(BankTagsPlugin.class)
public class PlugAndPlantPlugin extends Plugin implements PlugAndPlantPanel.Listener
{
	private static final String TIME_TRACKING_PLUGIN = "Time Tracking";
	private static final String SHORTEST_PATH_PLUGIN = "Shortest Path";

	// Per-account choices for a location; each key ends in the location's enum name, and those for
	// tree runs start with the tree prefix

	/**
	 * Which plugins are on rarely changes and takes a walk over every plugin to find out.
	 */
	private static final int PLUGIN_CHECK_TICKS = 10;
	/**
	 * How far past a location's patches the player can wander and still count as being there.
	 */
	private static final int LEAVING_SLACK = 15;
	/**
	 * How long a stop has to have nothing left to do before it counts as done. Arriving at a patch
	 * can show its state wrongly for a moment before the game catches up.
	 */
	private static final int SETTLE_TICKS = 3;

	private static final int[] RUNE_POUCH_TYPES = {
		VarbitID.RUNE_POUCH_TYPE_1, VarbitID.RUNE_POUCH_TYPE_2, VarbitID.RUNE_POUCH_TYPE_3,
		VarbitID.RUNE_POUCH_TYPE_4, VarbitID.RUNE_POUCH_TYPE_5, VarbitID.RUNE_POUCH_TYPE_6,
	};
	private static final int[] RUNE_POUCH_QUANTITIES = {
		VarbitID.RUNE_POUCH_QUANTITY_1, VarbitID.RUNE_POUCH_QUANTITY_2, VarbitID.RUNE_POUCH_QUANTITY_3,
		VarbitID.RUNE_POUCH_QUANTITY_4, VarbitID.RUNE_POUCH_QUANTITY_5, VarbitID.RUNE_POUCH_QUANTITY_6,
	};

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private ConfigManager configManager;

	@Inject
	private PluginManager pluginManager;

	@Inject
	private EventBus eventBus;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private ItemManager itemManager;

	@Inject
	private PlugAndPlantConfig config;

	@Inject
	private PatchTracker patchTracker;

	@Inject
	private PatchObjects patchObjects;

	@Inject
	private Gardeners gardeners;

	@Inject
	private Banks banks;

	@Inject
	private PresetStore presetStore;

	@Inject
	private ShortestPathRouter router;

	@Inject
	private BankFilter bankFilter;

	@Inject
	private PatchOverlay patchOverlay;

	@Inject
	private ItemHighlightOverlay itemHighlightOverlay;

	@Inject
	private ChecklistOverlay checklistOverlay;

	private PlugAndPlantPanel panel;
	private NavigationButton navButton;
	/**
	 * Each graceful piece's item ids across every recolour.
	 */
	private List<List<Integer>> gracefulPieces = List.of();

	// Run state, only touched on the client thread
	private boolean runActive;
	/**
	 * The preset the run in progress was started with, fixed for the run.
	 */
	private RunPreset activePreset = RunPreset.herbRun();
	/**
	 * The player's presets, read when an account logs in; empty until then.
	 */
	private List<RunPreset> presets = List.of();
	/**
	 * The locations the run visits, fixed when it starts: those whose anchor patches needed attention
	 * then. Each is visited once, so a patch that turns ready later doesn't send the player back.
	 */
	private final Set<FarmLocation> runStops = EnumSet.noneOf(FarmLocation.class);
	private final Set<FarmLocation> completedStops = EnumSet.noneOf(FarmLocation.class);
	/**
	 * Stops the player has been at on this run, to notice them leaving.
	 */
	private final Set<FarmLocation> reachedStops = EnumSet.noneOf(FarmLocation.class);
	/**
	 * The game tick since which each remaining stop has had nothing left to do.
	 */
	private final Map<FarmLocation, Integer> idleSince = new EnumMap<>(FarmLocation.class);
	/**
	 * Stops where the player has stood while there was something to do. Once those run out, the
	 * stop is done; the wait for things to settle is only for a stop that looks done on arrival.
	 */
	private final Set<FarmLocation> workedStops = EnumSet.noneOf(FarmLocation.class);
	/**
	 * What the shown path was asked for with, or null if no path has been asked for this run.
	 */
	@Nullable
	private Route route;
	/**
	 * Each patch's status at the last refresh, to notice a patch being planted.
	 */
	private final Map<FarmPatch, PatchStatus> previousStatuses = new HashMap<>();
	/**
	 * Patches planted on this run, while the player is still beside them: they can still be composted
	 * and their gardener paid.
	 */
	private final Set<FarmPatch> justPlanted = new HashSet<>();
	private PanelState lastPanelState;
	private int ticks;
	private boolean timeTrackingActive;
	private boolean shortestPathActive;
	private boolean bankTagsActive;

	@Override
	protected void startUp() throws Exception
	{
		List<List<Integer>> pieces = new ArrayList<>();
		for (int piece : RunItems.GRACEFUL_PIECES)
		{
			pieces.add(List.copyOf(ItemVariationMapping.getVariations(ItemVariationMapping.map(piece))));
		}
		gracefulPieces = pieces;

		panel = new PlugAndPlantPanel(this);
		navButton = NavigationButton.builder()
			.tooltip("Plug & Plant Farming")
			.icon(ImageUtil.loadImageResource(PlugAndPlantPlugin.class, "panel_icon.png"))
			.priority(7)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);

		overlayManager.add(patchOverlay);
		overlayManager.add(itemHighlightOverlay);
		overlayManager.add(checklistOverlay);
		eventBus.register(patchObjects);
		eventBus.register(gardeners);
		eventBus.register(banks);
		eventBus.register(bankFilter);
		eventBus.register(router);

		clientThread.invokeLater(() ->
		{
			bankFilter.register();
			updateItemLists();
			patchObjects.scan();
			gardeners.scan();
			banks.scan();
			checkPlugins();
			refresh();
		});
	}

	@Override
	protected void shutDown() throws Exception
	{
		clientToolbar.removeNavigation(navButton);
		overlayManager.remove(patchOverlay);
		overlayManager.remove(itemHighlightOverlay);
		overlayManager.remove(checklistOverlay);
		eventBus.unregister(patchObjects);
		eventBus.unregister(gardeners);
		eventBus.unregister(banks);
		eventBus.unregister(bankFilter);
		eventBus.unregister(router);

		clientThread.invokeLater(() ->
		{
			endRun();
			bankFilter.unregister();
			patchObjects.clear();
			gardeners.clear();
			banks.clear();
			patchOverlay.setHighlights(List.of(), List.of(), List.of());
			itemHighlightOverlay.setItems(Map.of());
			checklistOverlay.setContent(null);
			previousStatuses.clear();
			lastPanelState = null;
		});
	}

	@Provides
	PlugAndPlantConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(PlugAndPlantConfig.class);
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (++ticks % PLUGIN_CHECK_TICKS == 0)
		{
			checkPlugins();
		}
		refresh();
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGIN_SCREEN)
		{
			endRun();
		}
		refresh();
	}

	@Subscribe
	public void onRuneScapeProfileChanged(RuneScapeProfileChanged event)
	{
		clientThread.invoke(() ->
		{
			// Presets are per account; the new one's are read on the next refresh
			presets = List.of();
			refresh();
		});
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		// Time Tracking saves a patch here the moment it sees it change
		if (PatchTracker.TIME_TRACKING_GROUP.equals(event.getGroup()) || PlugAndPlantConfig.GROUP.equals(event.getGroup()))
		{
			clientThread.invoke(this::refresh);
		}
		if (PlugAndPlantConfig.GROUP.equals(event.getGroup())
			&& (PlugAndPlantConfig.ALWAYS_SHOW.equals(event.getKey()) || PlugAndPlantConfig.NEVER_SHOW.equals(event.getKey())))
		{
			clientThread.invoke(this::updateItemLists);
		}
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		if (event.getContainerId() == InventoryID.INV || event.getContainerId() == InventoryID.WORN)
		{
			refresh();
		}
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded event)
	{
		if (event.getGroupId() == InterfaceID.BANKMAIN && runActive && config.filterBank())
		{
			bankFilter.openWhenBuilt();
		}
	}

	private void updateItemLists()
	{
		bankFilter.setItemLists(ItemList.parse(config.alwaysShow()), ItemList.parse(config.neverShow()));
	}

	@Override
	public void setBankItems(String key, String text)
	{
		// The config change this posts updates the filter
		configManager.setConfiguration(PlugAndPlantConfig.GROUP, key, text);
	}

	@Override
	public void startRun()
	{
		clientThread.invoke(() ->
		{
			if (runActive || client.getGameState() != GameState.LOGGED_IN)
			{
				return;
			}

			RunPreset preset = selectedPreset();
			Set<FarmLocation> stops = stopsFor(preset, patchTracker.readStates(Instant.now().getEpochSecond()),
				locationSettings(preset), farmingLevel());
			if (stops.isEmpty())
			{
				return;
			}

			runActive = true;
			activePreset = preset;
			runStops.clear();
			runStops.addAll(stops);
			completedStops.clear();
			reachedStops.clear();
			idleSince.clear();
			workedStops.clear();
			route = null;
			refresh();

			if (runActive && config.filterBank() && bankFilter.isBankOpen())
			{
				bankFilter.open();
			}
		});
	}

	@Override
	public void stopRun()
	{
		clientThread.invoke(() ->
		{
			endRun();
			refresh();
		});
	}

	@Override
	public void reroute()
	{
		clientThread.invoke(() ->
		{
			route = null;
			refresh();
		});
	}

	@Override
	public void skipStop(FarmLocation location)
	{
		clientThread.invoke(() ->
		{
			if (runActive && runStops.contains(location))
			{
				// For when a stop can't be finished, such as a patch the player can't get to
				completedStops.add(location);
				refresh();
			}
		});
	}

	@Override
	public void setLocationSettings(FarmLocation location, LocationSettings settings)
	{
		clientThread.invoke(() ->
		{
			// The teleport is the location's whatever the run; whether to stop there is the preset's
			presetStore.setTeleport(location, settings.getTeleport());
			changePreset(preset -> preset.withLocationEnabled(location, settings.isEnabled()));
		});
	}

	@Override
	public void setRunOption(RunOption option, boolean on)
	{
		clientThread.invoke(() -> changePreset(preset -> option.set(preset, on)));
	}

	@Override
	public void setPatchRole(PatchType type, PatchRole role)
	{
		clientThread.invoke(() -> changePreset(preset -> preset.withRole(type, role)));
	}

	@Override
	public void setPatchExcluded(FarmLocation location, PatchType type, boolean excluded)
	{
		clientThread.invoke(() -> changePreset(preset -> preset.withExcluded(location, type, excluded)));
	}

	@Override
	public void selectPreset(String id)
	{
		clientThread.invoke(() ->
		{
			if (!runActive && presets.stream().anyMatch(preset -> preset.getId().equals(id)))
			{
				presetStore.select(id);
				refresh();
			}
		});
	}

	@Override
	public void newPreset()
	{
		clientThread.invoke(() ->
		{
			if (runActive || presets.isEmpty())
			{
				return;
			}
			// A copy of the one shown, to change from there
			RunPreset copy = selectedPreset().withId("custom-" + System.currentTimeMillis()).withName(uniqueName("New run"));
			List<RunPreset> changed = new ArrayList<>(presets);
			changed.add(copy);
			savePresets(changed);
			presetStore.select(copy.getId());
			refresh();
		});
	}

	@Override
	public void renamePreset(String name)
	{
		String trimmed = name.trim();
		if (!trimmed.isEmpty())
		{
			clientThread.invoke(() -> changePreset(preset -> preset.withName(trimmed)));
		}
	}

	@Override
	public void deletePreset()
	{
		clientThread.invoke(() ->
		{
			if (runActive || presets.size() < 2)
			{
				return;
			}
			RunPreset doomed = selectedPreset();
			List<RunPreset> changed = new ArrayList<>(presets);
			changed.removeIf(preset -> preset.getId().equals(doomed.getId()));
			savePresets(changed);
			presetStore.select(changed.get(0).getId());
			refresh();
		});
	}

	/**
	 * Changes the preset shown, saves it, and shows the change. Must be called on the client thread.
	 */
	private void changePreset(java.util.function.UnaryOperator<RunPreset> change)
	{
		if (runActive || presets.isEmpty())
		{
			return;
		}
		RunPreset selected = selectedPreset();
		List<RunPreset> changed = new ArrayList<>();
		for (RunPreset preset : presets)
		{
			changed.add(preset.getId().equals(selected.getId()) ? change.apply(preset) : preset);
		}
		savePresets(changed);
		refresh();
	}

	private void savePresets(List<RunPreset> changed)
	{
		presets = List.copyOf(changed);
		presetStore.save(presets);
	}

	private String uniqueName(String base)
	{
		String name = base;
		for (int n = 2; presets.stream().anyMatch(preset -> preset.getName().equalsIgnoreCase(base)) && n < 100; n++)
		{
			String candidate = base + " " + n;
			if (presets.stream().noneMatch(preset -> preset.getName().equalsIgnoreCase(candidate)))
			{
				return candidate;
			}
		}
		return name;
	}

	/**
	 * The preset the side panel shows and a run would start with.
	 */
	private RunPreset selectedPreset()
	{
		if (presets.isEmpty() && configManager.getRSProfileKey() != null)
		{
			presets = List.copyOf(presetStore.load());
		}
		if (presets.isEmpty())
		{
			return RunPreset.herbRun();
		}
		String id = presetStore.selectedId();
		for (RunPreset preset : presets)
		{
			if (preset.getId().equals(id))
			{
				return preset;
			}
		}
		return presets.get(0);
	}

	/**
	 * The preset of the run in progress, or the one the player has picked.
	 */
	private RunPreset preset()
	{
		return runActive ? activePreset : selectedPreset();
	}

	/**
	 * Re-reads everything, moves the run along, and updates the overlays and panel.
	 */
	private void refresh()
	{
		RunPreset preset = preset();
		boolean loggedIn = client.getGameState() == GameState.LOGGED_IN;
		long now = Instant.now().getEpochSecond();
		Map<FarmPatch, PatchState> patches = patchTracker.readStates(now);
		Set<FarmPatch> composted = patchTracker.readComposted();
		Set<FarmPatch> protectedTrees = patchTracker.readProtected();
		Map<FarmLocation, LocationSettings> settings = locationSettings(preset);
		Carried carried = carried();
		WorldPoint playerPoint = loggedIn && client.getLocalPlayer() != null ? Positions.of(client, client.getLocalPlayer()) : null;
		FarmingContract contract = loggedIn ? contract() : null;
		int farmingLevel = farmingLevel();

		updateJustPlanted(patches, playerPoint);

		// The checklist goes by what the player wants; the steps by what they can do with what they carry
		RunPlan.Options wantedOptions = new RunPlan.Options(preset, preset.isCompost(), preset.isProtect(), farmingLevel);
		boolean carryingCompost = carried.has(ItemID.BOTTOMLESS_COMPOST_BUCKET_FILLED) || carried.hasAny(RunItems.COMPOST_BUCKETS);
		RunPlan.Options options = new RunPlan.Options(preset, preset.isCompost() && carryingCompost, preset.isProtect(), farmingLevel);

		if (runActive && playerPoint != null)
		{
			joinStopAt(preset, patches, settings, playerPoint, farmingLevel);
		}
		List<FarmLocation> stops = orderedStops(preset, runActive ? remainingStops() : stopsFor(preset, patches, settings, farmingLevel), playerPoint);
		RunPlan plan = RunPlan.of(stops, patches, composted, protectedTrees, justPlanted, options, carried, contract);
		if (runActive && playerPoint != null && updateCompletedStops(preset, plan, playerPoint, farmingLevel))
		{
			stops = orderedStops(preset, remainingStops(), playerPoint);
			plan = RunPlan.of(stops, patches, composted, protectedTrees, justPlanted, options, carried, contract);
		}
		RunPlan wanted = options.equals(wantedOptions)
			? plan
			: RunPlan.of(stops, patches, composted, protectedTrees, justPlanted, wantedOptions, carried, contract);

		bankFilter.setSpec(bankFilterSpec(preset, settings, stops, contract));

		// Patch data is per account and unavailable between worlds, so only judge the run while logged in
		if (runActive && loggedIn)
		{
			if (stops.isEmpty())
			{
				endRun();
				client.addChatMessage(ChatMessageType.GAMEMESSAGE, "",
					preset.getName() + " complete.", null);
			}
			else if (playerPoint != null && stops.get(0).isNear(playerPoint, 0) && plan.hasSteps(stops.get(0)))
			{
				// At a stop with work left: its patches are highlighted, and a path to where the player already
				// is can only mislead, such as in Lletya, where Shortest Path would teleport there again
				if (route != null)
				{
					router.clear();
					route = null;
				}
			}
			else
			{
				Route wantedRoute = route(preset, stops, plan, settings, carried);
				if (!wantedRoute.equals(route))
				{
					router.show(wantedRoute);
					route = wantedRoute;
				}
			}
		}

		List<ChecklistItem> checklist = List.copyOf(Checklist.build(wanted, patches, settings, carried,
			new Checklist.Options(preset, preset.isCompost(), preset.isProtect(), preset.isVolcanicAsh(),
				preset.isGraceful() ? gracefulPieces : List.of()),
			contract));

		updateHighlights(plan, patches, contract);
		itemHighlightOverlay.setItems(itemHighlights(plan, patches, playerPoint));
		checklistOverlay.setContent(overlayContent(preset, loggedIn, plan, patches, stops, checklist, playerPoint, options, contract));

		Set<RunOption> runOptions = EnumSet.noneOf(RunOption.class);
		for (RunOption option : RunOption.values())
		{
			if (option.isOn(preset))
			{
				runOptions.add(option);
			}
		}

		List<StopView> stopViews = stopViews(preset, stops, plan, patches, settings, playerPoint, options, contract);
		List<PanelState.PresetChoice> presetChoices = new ArrayList<>();
		for (RunPreset each : presets)
		{
			presetChoices.add(new PanelState.PresetChoice(each.getId(), each.getName()));
		}
		PanelState state = new PanelState(loggedIn, runActive, preset, presetChoices, patches, settings, new LinkedHashSet<>(stops), stopViews, checklist,
			runOptions, contract == null ? null : contract.seedLabel() + " at the Farming Guild", farmingLevel, now / 60,
			timeTrackingActive, shortestPathActive, bankTagsActive, config.alwaysShow(), config.neverShow());
		if (!state.equals(lastPanelState))
		{
			lastPanelState = state;
			SwingUtilities.invokeLater(() -> panel.update(state));
		}
	}

	/**
	 * The player's Farming level, boosts included, as a boost is enough to get into the Farming Guild's
	 * wings. Without a player to ask, every patch counts as reachable.
	 */
	private int farmingLevel()
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return Integer.MAX_VALUE;
		}
		return Math.max(client.getRealSkillLevel(Skill.FARMING), client.getBoostedSkillLevel(Skill.FARMING));
	}

	/**
	 * The enabled locations whose anchor patches need attention, which are the stops a run would make.
	 */
	private static Set<FarmLocation> stopsFor(RunPreset preset, Map<FarmPatch, PatchState> patches,
		Map<FarmLocation, LocationSettings> settings, int farmingLevel)
	{
		Set<FarmLocation> stops = EnumSet.noneOf(FarmLocation.class);
		for (FarmLocation location : preset.locations())
		{
			if (!settings.get(location).isEnabled())
			{
				continue;
			}
			for (FarmPatch anchor : preset.anchors(location, farmingLevel))
			{
				if (patches.getOrDefault(anchor, PatchState.UNKNOWN).getStatus().needsVisit())
				{
					stops.add(location);
				}
			}
		}
		return stops;
	}

	private Set<FarmLocation> remainingStops()
	{
		Set<FarmLocation> remaining = EnumSet.noneOf(FarmLocation.class);
		remaining.addAll(runStops);
		remaining.removeAll(completedStops);
		return remaining;
	}

	/**
	 * The stops in the recommended order, but with the one the player is at first, as that is where
	 * seeds are used first and where the route leads while there is something to do.
	 */
	static List<FarmLocation> orderedStops(RunPreset preset, Collection<FarmLocation> stops, @Nullable WorldPoint playerPoint)
	{
		List<FarmLocation> ordered = new ArrayList<>();
		for (FarmLocation location : preset.recommendedOrder())
		{
			if (!stops.contains(location))
			{
				continue;
			}
			if (playerPoint != null && location.isNear(playerPoint, LEAVING_SLACK))
			{
				ordered.add(0, location);
			}
			else
			{
				ordered.add(location);
			}
		}
		return ordered;
	}

	/**
	 * A stop is done once its anchor patches are, and either nothing else there has needed doing for
	 * a few ticks or the player has moved on.
	 *
	 * @return whether any stop was completed
	 */
	private boolean updateCompletedStops(RunPreset preset, RunPlan plan, WorldPoint playerPoint, int farmingLevel)
	{
		int tick = client.getTickCount();
		boolean changed = false;
		for (FarmLocation location : remainingStops())
		{
			if (location.isNear(playerPoint, 0))
			{
				reachedStops.add(location);
			}

			if (plan.hasSteps(location))
			{
				idleSince.remove(location);
			}
			else
			{
				idleSince.putIfAbsent(location, tick);
			}

			boolean there = location.isNear(playerPoint, 0);
			if (there && plan.hasSteps(location))
			{
				workedStops.add(location);
			}

			boolean anchorsDone = preset.anchors(location, farmingLevel).stream().noneMatch(plan.getSteps()::containsKey);
			boolean left = reachedStops.contains(location) && !location.isNear(playerPoint, LEAVING_SLACK);
			// Finishing the last task here moves on at once; a stop that looks done the moment the
			// player arrives waits a few ticks, as the game can show a patch wrongly for a moment then
			boolean finishedHere = workedStops.contains(location) && !plan.hasSteps(location);
			boolean settled = idleSince.containsKey(location) && tick - idleSince.get(location) >= SETTLE_TICKS;
			if (anchorsDone && (finishedHere || settled || left))
			{
				log.debug("{} done: {}", location, finishedHere ? "tasks finished" : left ? "left it" : "nothing to do");
				completedStops.add(location);
				idleSince.remove(location);
				changed = true;
			}
		}
		return changed;
	}

	/**
	 * Makes the location the player is at a stop of the run, if its anchor patches need attention and
	 * it isn't one yet. The run's stops are the ones predicted when it started, but a patch can turn
	 * ready sooner than predicted, and the player knows best where they want to go.
	 */
	private void joinStopAt(RunPreset preset, Map<FarmPatch, PatchState> patches, Map<FarmLocation, LocationSettings> settings,
		WorldPoint playerPoint, int farmingLevel)
	{
		for (FarmLocation location : preset.locations())
		{
			if (runStops.contains(location) || !settings.get(location).isEnabled() || !location.isNear(playerPoint, 0))
			{
				continue;
			}
			for (FarmPatch anchor : preset.anchors(location, farmingLevel))
			{
				if (patches.getOrDefault(anchor, PatchState.UNKNOWN).getStatus().needsVisit())
				{
					log.debug("{} joins the run: {} needs attention", location, anchor);
					runStops.add(location);
					break;
				}
			}
		}
	}

	/**
	 * A planted patch is otherwise done, but can still be composted and its gardener paid. So a patch
	 * planted on this run keeps those steps for as long as the player stays beside it.
	 */
	private void updateJustPlanted(Map<FarmPatch, PatchState> patches, @Nullable WorldPoint playerPoint)
	{
		if (!runActive || playerPoint == null)
		{
			justPlanted.clear();
			if (playerPoint == null)
			{
				previousStatuses.clear();
			}
		}

		for (Map.Entry<FarmPatch, PatchState> entry : patches.entrySet())
		{
			PatchStatus status = entry.getValue().getStatus();
			PatchStatus previous = playerPoint == null ? null : previousStatuses.put(entry.getKey(), status);
			if (runActive && status == PatchStatus.GROWING && previous != null && previous.leadsToReplanting())
			{
				justPlanted.add(entry.getKey());
			}
		}

		justPlanted.removeIf(patch ->
			patches.get(patch).getStatus() != PatchStatus.GROWING
				|| playerPoint == null
				|| !patch.getLocation().isNear(playerPoint, LEAVING_SLACK));
	}

	@Nullable
	private FarmingContract contract()
	{
		// A finished contract only waits to be handed in
		if (client.getVarbitValue(VarbitID.FARMGUILD_CONTRACT_COMPLETE) > 0)
		{
			return null;
		}
		return FarmingContract.fromVarbit(client.getVarbitValue(VarbitID.FARMGUILD_CONTRACT_TYPE));
	}

	private Route route(RunPreset preset, List<FarmLocation> stops, RunPlan plan, Map<FarmLocation, LocationSettings> settings,
		Carried carried)
	{
		boolean requireSeeds = config.bankDetourForSeeds() && !carried.hasAny(RunItems.SEED_BOXES);

		Map<FarmLocation, List<Map<Integer, Integer>>> routeStops = new LinkedHashMap<>();
		boolean useHouse = false;
		boolean useSpells = false;
		boolean useFairyRings = false;
		for (FarmLocation location : stops)
		{
			// The anchors' seeds are what make the stop worth it; a contract patch needs the contract's
			List<Map<Integer, Integer>> seeds = new ArrayList<>();
			Set<PatchType> covered = EnumSet.noneOf(PatchType.class);
			for (FarmPatch anchor : preset.anchors(location))
			{
				if (requireSeeds && plan.getToPlant().contains(anchor) && covered.add(anchor.getType()))
				{
					Integer contractSeed = plan.getContractSeeds().get(anchor);
					seeds.add(contractSeed != null ? Map.of(contractSeed, 1) : anySeed(anchor.getType()));
				}
			}
			routeStops.put(location, seeds);

			Teleport choice = settings.get(location).getTeleport();
			useHouse |= choice.isViaHouse();
			useSpells |= choice.isSpell();
			useFairyRings |= choice == Teleport.FAIRY_RING;
		}

		// Shortest Path looks at the inventory once per search, so the route notes what it would find
		Set<Integer> routeItems = new HashSet<>();
		for (int itemId : carried.getItems().keySet())
		{
			boolean anchorSeed = RunPreset.TYPES.stream().anyMatch(type -> preset.role(type) == PatchRole.VISIT && type.getSeeds().contains(itemId));
			if (Teleport.ALL_ITEM_IDS.contains(itemId) || anchorSeed)
			{
				routeItems.add(itemId);
			}
		}

		if (config.stopOrder() == PlugAndPlantConfig.StopOrder.RECOMMENDED && !routeStops.isEmpty())
		{
			// Only the next stop, which is the one the player is at if they are at one
			Map.Entry<FarmLocation, List<Map<Integer, Integer>>> next = routeStops.entrySet().iterator().next();
			routeStops = Map.of(next.getKey(), next.getValue());
		}

		return new Route(preset, routeStops, config.carriedTeleportsOnly(), useHouse, useSpells, useFairyRings, routeItems);
	}

	/**
	 * Shortest Path's "any one of these items, at least this many": any seed of the kind, enough for a patch.
	 */
	private static Map<Integer, Integer> anySeed(PatchType type)
	{
		Map<Integer, Integer> seeds = new HashMap<>();
		for (int seed : type.getSeeds())
		{
			seeds.put(seed, type.getSeedsPerPatch());
		}
		return seeds;
	}

	private BankFilterSpec bankFilterSpec(RunPreset preset, Map<FarmLocation, LocationSettings> settings,
		Collection<FarmLocation> stops, @Nullable FarmingContract contract)
	{
		Map<BankSection, List<Integer>> sections = new EnumMap<>(BankSection.class);
		List<Integer> equipment = new ArrayList<>(RunItems.EQUIPMENT);
		if (preset.includes(PatchType.HERB))
		{
			sections.put(BankSection.HERB_SEEDS, RunItems.HERB_SEEDS);
		}
		if (preset.includes(PatchType.ALLOTMENT))
		{
			sections.put(BankSection.ALLOTMENT_SEEDS, RunItems.ALLOTMENT_SEEDS);
		}
		if (preset.includes(PatchType.FLOWER))
		{
			sections.put(BankSection.FLOWER_SEEDS, RunItems.FLOWER_SEEDS);
		}
		if (preset.includes(PatchType.TREE))
		{
			sections.put(BankSection.TREE_SAPLINGS, RunItems.TREE_SAPLINGS);
		}
		if (preset.includes(PatchType.FRUIT_TREE))
		{
			sections.put(BankSection.FRUIT_TREE_SAPLINGS, RunItems.FRUIT_TREE_SAPLINGS);
		}
		if (preset.includesTrees())
		{
			if (preset.isProtect())
			{
				Set<Integer> payments = new LinkedHashSet<>();
				for (Sapling sapling : Sapling.values())
				{
					payments.add(sapling.getPayment());
				}
				sections.put(BankSection.PAYMENTS, List.copyOf(payments));
			}
			equipment.addAll(RunItems.TREE_EQUIPMENT);
			sections.put(BankSection.GROWING, RunItems.SAPLINGS_TO_GROW);
		}

		// A contract's seed is shown even when the player doesn't grow that kind of crop otherwise
		if (contract != null && RunPlan.covers(preset, contract.getType()))
		{
			BankSection section = contract.getType() == PatchType.ALLOTMENT ? BankSection.ALLOTMENT_SEEDS
				: contract.getType() == PatchType.FLOWER ? BankSection.FLOWER_SEEDS : null;
			if (section != null)
			{
				sections.putIfAbsent(section, List.of(contract.getSeed()));
			}
		}

		if (preset.isGraceful())
		{
			gracefulPieces.forEach(equipment::addAll);
		}
		if (preset.isVolcanicAsh())
		{
			equipment.add(ItemID.FOSSIL_VOLCANIC_ASH);
		}
		sections.put(BankSection.EQUIPMENT, equipment);

		// The teleports to bring for the stops left
		if (config.bankTeleports())
		{
			Set<Integer> teleportItems = new LinkedHashSet<>();
			for (FarmLocation location : stops)
			{
				Teleport choice = settings.get(location).getTeleport();
				for (Teleport teleport : location.teleportsFor(choice))
				{
					teleportItems.addAll(location.itemsOnly(choice) ? teleport.getItemIds() : teleport.bankItemIds());
				}
			}
			sections.put(BankSection.TELEPORTS, List.copyOf(teleportItems));
		}

		// Anything that can be worn goes on top, to be taken and put on first
		Set<Integer> wearables = new LinkedHashSet<>();
		for (List<Integer> items : sections.values())
		{
			for (int itemId : items)
			{
				if (isEquipable(itemId))
				{
					wearables.add(itemId);
				}
			}
		}
		sections.put(BankSection.WEARABLES, List.copyOf(wearables));

		return new BankFilterSpec(sections);
	}

	private static String stepLabel(FarmPatch patch, Step step, RunPlan plan, Map<FarmPatch, PatchState> patches,
		@Nullable FarmingContract contract)
	{
		switch (step)
		{
			case PLANT:
				return contract != null && plan.getContractSeeds().containsKey(patch) ? "Plant " + contract.getName() : step.getLabel();
			case CURE:
				// Trees are pruned rather than cured
				return patch.getType().isTree() ? "Prune" : step.getLabel();
			case PAY:
				Sapling tree = patches.get(patch).getCrop();
				return tree == null ? "Pay gardener" : "Pay " + tree.paymentLabel();
			default:
				return step.getLabel();
		}
	}

	private void updateHighlights(RunPlan plan, Map<FarmPatch, PatchState> patches, @Nullable FarmingContract contract)
	{
		if (!runActive || !config.highlightPatches())
		{
			patchOverlay.setHighlights(List.of(), List.of(), List.of());
			return;
		}

		List<PatchOverlay.Highlight> highlights = new ArrayList<>();
		Set<FarmPatch> labelled = new HashSet<>();
		for (Map.Entry<GameObject, FarmPatch> entry : patchObjects.get().entrySet())
		{
			FarmPatch patch = entry.getValue();
			Step step = plan.getSteps().get(patch);
			if (step != null)
			{
				// A patch can be several objects; box them all but label only one
				String label = labelled.add(patch) ? stepLabel(patch, step, plan, patches, contract) : null;
				highlights.add(new PatchOverlay.Highlight(entry.getKey(), step.getColor(), label));
			}
		}

		// The gardener takes the payment, as soon as the tree is in, and can take down a grown tree for coins
		List<PatchOverlay.NpcHighlight> npcHighlights = new ArrayList<>();
		for (Map.Entry<NPC, FarmPatch> entry : gardeners.get().entrySet())
		{
			FarmPatch patch = entry.getValue();
			Step step = plan.getSteps().get(patch);
			if (plan.getPayable().contains(patch))
			{
				npcHighlights.add(new PatchOverlay.NpcHighlight(entry.getKey(), Step.PAY.getColor(),
					stepLabel(patch, Step.PAY, plan, patches, contract)));
			}
			else if (step == Step.REMOVE)
			{
				npcHighlights.add(new PatchOverlay.NpcHighlight(entry.getKey(), step.getColor(), "Remove for " + Checklist.REMOVAL_COINS + " coins"));
			}
		}
		// A patch with something to do whose object hasn't been recognised gets a box on its known tile
		List<PatchOverlay.TileHighlight> tileHighlights = new ArrayList<>();
		WorldPoint playerPoint = client.getLocalPlayer() == null ? null : Positions.of(client, client.getLocalPlayer());
		for (Map.Entry<FarmPatch, Step> entry : plan.getSteps().entrySet())
		{
			FarmPatch patch = entry.getKey();
			boolean anchor = patch.getType() == PatchType.HERB || patch.getType().isTree();
			if (anchor && !labelled.contains(patch) && playerPoint != null && patch.getLocation().isNear(playerPoint, 0))
			{
				tileHighlights.add(new PatchOverlay.TileHighlight(patch.getPoint(), entry.getValue().getColor(),
					stepLabel(patch, entry.getValue(), plan, patches, contract)));
			}
		}
		patchOverlay.setHighlights(highlights, npcHighlights, tileHighlights);
	}

	/**
	 * The items the patches the player is at need next, in the colour of the step that uses them.
	 */
	private Map<Integer, Color> itemHighlights(RunPlan plan, Map<FarmPatch, PatchState> patches, @Nullable WorldPoint playerPoint)
	{
		if (!config.highlightItems() || playerPoint == null)
		{
			return Map.of();
		}

		Map<Integer, Color> items = new HashMap<>();
		// While packing for a run, or on one: what could be worn to make room
		if (runActive || bankFilter.isOpen())
		{
			for (int itemId : wearable())
			{
				items.put(itemId, ItemHighlightOverlay.WEAR_COLOR);
			}
		}
		if (!runActive)
		{
			return items;
		}

		for (Map.Entry<FarmPatch, Step> entry : plan.getSteps().entrySet())
		{
			FarmPatch patch = entry.getKey();
			Step step = entry.getValue();
			if (!patch.getLocation().isNear(playerPoint, LEAVING_SLACK))
			{
				continue;
			}

			List<Integer> needed;
			switch (step)
			{
				case PLANT:
					Integer contractSeed = plan.getContractSeeds().get(patch);
					needed = contractSeed != null ? List.of(contractSeed) : patch.getType().getSeeds();
					break;
				case COMPOST:
					needed = new ArrayList<>(RunItems.COMPOST_BUCKETS);
					needed.add(ItemID.BOTTOMLESS_COMPOST_BUCKET_FILLED);
					break;
				case CURE:
					needed = patch.getType().isTree()
						? List.of(ItemID.FAIRY_ENCHANTED_SECATEURS, ItemID.SECATEURS)
						: List.of(ItemID.PLANT_CURE);
					break;
				case RAKE:
					needed = List.of(ItemID.RAKE);
					break;
				case CLEAR:
				case DIG:
					needed = List.of(ItemID.SPADE);
					break;
				case REMOVE:
					needed = List.of(ItemID.COINS);
					break;
				case PAY:
					Sapling tree = patches.get(patch).getCrop();
					needed = tree == null ? List.of() : List.of(tree.getPayment());
					break;
				case HARVEST:
					// Crops are harvested with a spade
					needed = List.of(ItemID.SPADE);
					break;
				default:
					needed = List.of();
					break;
			}

			// What the stop needs next comes before what could be worn
			for (int itemId : needed)
			{
				items.put(itemId, step.getColor());
			}
		}
		return items;
	}

	/**
	 * Each patch tended at a stop and what it needs next, with the first thing to do marked.
	 */
	private static List<StopView.Task> tasks(RunPreset preset, FarmLocation location, RunPlan plan,
		Map<FarmPatch, PatchState> patches, RunPlan.Options options, @Nullable FarmingContract contract)
	{
		Set<FarmPatch> contractPatches = contract == null || !RunPlan.covers(preset, contract.getType())
			? Set.of()
			: new HashSet<>(contract.patches(preset.includes(contract.getType())));

		List<StopView.Task> tasks = new ArrayList<>();
		boolean nextFound = false;
		for (FarmPatch patch : location.getPatches())
		{
			if (!options.includes(patch) && !contractPatches.contains(patch))
			{
				continue;
			}

			Step step = plan.getSteps().get(patch);
			if (step != null)
			{
				tasks.add(new StopView.Task(shortName(patch), stepLabel(patch, step, plan, patches, contract), step.getColor(), !nextFound));
				nextFound = true;
			}
			else if (plan.getWithoutSeeds().contains(patch))
			{
				tasks.add(new StopView.Task(shortName(patch), "No seeds", ColorScheme.MEDIUM_GRAY_COLOR, false));
			}
			else
			{
				boolean growing = patches.get(patch).getStatus() == PatchStatus.GROWING;
				tasks.add(new StopView.Task(shortName(patch), growing ? "Done" : "-", ColorScheme.LIGHT_GRAY_COLOR, false));
			}
		}
		return tasks;
	}

	/**
	 * The run's stops for the side panel, in the order of the run's guide: those finished, the one the
	 * player is at or headed to, and those still to come.
	 */
	private List<StopView> stopViews(RunPreset preset, List<FarmLocation> stops, RunPlan plan, Map<FarmPatch, PatchState> patches,
		Map<FarmLocation, LocationSettings> settings, @Nullable WorldPoint playerPoint, RunPlan.Options options,
		@Nullable FarmingContract contract)
	{
		if (!runActive)
		{
			return List.of();
		}

		FarmLocation current = currentStop(stops, playerPoint);
		List<StopView> views = new ArrayList<>();
		for (FarmLocation location : preset.recommendedOrder())
		{
			if (!runStops.contains(location))
			{
				continue;
			}

			StopView.Stage stage = completedStops.contains(location) ? StopView.Stage.DONE
				: location == current ? StopView.Stage.CURRENT : StopView.Stage.UPCOMING;
			int tasksLeft = (int) plan.getSteps().keySet().stream().filter(patch -> patch.getLocation() == location).count();
			Teleport teleport = settings.get(location).getTeleport();
			String teleportLabel = teleport == Teleport.ANY || teleport == Teleport.NONE ? null : teleport.getLabel();
			views.add(new StopView(location, stage,
				stage == StopView.Stage.CURRENT ? tasks(preset, location, plan, patches, options, contract) : List.of(),
				tasksLeft, teleportLabel));
		}
		return views;
	}

	/**
	 * The stop the player is at, or else the one the route leads to.
	 */
	@Nullable
	private FarmLocation currentStop(List<FarmLocation> stops, @Nullable WorldPoint playerPoint)
	{
		if (stops.isEmpty())
		{
			return null;
		}

		// The stops are ordered with the one the player is at first
		FarmLocation first = stops.get(0);
		if (playerPoint != null && first.isNear(playerPoint, LEAVING_SLACK))
		{
			return first;
		}
		if (config.stopOrder() == PlugAndPlantConfig.StopOrder.RECOMMENDED)
		{
			return first;
		}

		// Nearest next: whichever stop Shortest Path picked, once it has said
		FarmLocation headingTo = router.headingTo();
		return headingTo != null && stops.contains(headingTo) ? headingTo : first;
	}

	@Nullable
	private ChecklistOverlay.Content overlayContent(RunPreset preset, boolean loggedIn, RunPlan plan, Map<FarmPatch, PatchState> patches,
		List<FarmLocation> stops, List<ChecklistItem> checklist, @Nullable WorldPoint playerPoint, RunPlan.Options options,
		@Nullable FarmingContract contract)
	{
		PlugAndPlantConfig.ChecklistOverlayMode mode = config.checklistOverlay();
		boolean show = mode == PlugAndPlantConfig.ChecklistOverlayMode.ALWAYS
			|| (mode == PlugAndPlantConfig.ChecklistOverlayMode.DURING_RUN && runActive);
		if (!loggedIn || !show)
		{
			return null;
		}

		// At the stop the player is at: each patch they tend there and what it needs next
		List<ChecklistOverlay.Line> steps = new ArrayList<>();
		boolean atStop = false;
		for (FarmLocation location : stops)
		{
			if (playerPoint == null || !location.isNear(playerPoint, 0))
			{
				continue;
			}

			atStop = true;
			for (StopView.Task task : tasks(preset, location, plan, patches, options, contract))
			{
				steps.add(new ChecklistOverlay.Line(task.getPatch(), task.getLabel(), task.getColor()));
			}
		}

		// Somewhere the run doesn't stop, say how its patches are doing rather than nothing at all
		if (!atStop && playerPoint != null)
		{
			long now = Instant.now().getEpochSecond();
			for (FarmLocation location : preset.locations())
			{
				if (!location.isNear(playerPoint, 0))
				{
					continue;
				}
				for (FarmPatch patch : preset.anchors(location))
				{
					steps.add(new ChecklistOverlay.Line(shortName(patch), describe(patches.getOrDefault(patch, PatchState.UNKNOWN), now),
						ColorScheme.LIGHT_GRAY_COLOR));
				}
			}
		}

		// What to bring is for the bank; elsewhere it would only crowd the steps
		boolean atBank = playerPoint != null && (banks.isNear(playerPoint) || bankFilter.isBankOpen());
		List<ChecklistOverlay.Line> lines = new ArrayList<>();
		for (ChecklistItem item : checklist)
		{
			if (atBank && (!config.checklistMissingOnly() || item.getState() != ChecklistItem.State.GOOD))
			{
				lines.add(new ChecklistOverlay.Line(item.getLabel(), item.getValue(), ChecklistOverlay.color(item.getState())));
			}
		}

		int left = stops.size();
		String title = runActive ? preset.getName() + ": " + left + (left == 1 ? " stop left" : " stops left") : preset.getName();
		return new ChecklistOverlay.Content(title, steps, lines);
	}

	/**
	 * Run items in the inventory that could be worn in a free slot instead, to make room.
	 */
	private Set<Integer> wearable()
	{
		ItemContainer inventory = client.getItemContainer(InventoryID.INV);
		if (inventory == null)
		{
			return Set.of();
		}
		ItemContainer worn = client.getItemContainer(InventoryID.WORN);

		Set<Integer> wearable = new HashSet<>();
		for (Item item : inventory.getItems())
		{
			int itemId = item.getId();
			if (itemId <= 0 || !isWearableRunItem(itemId))
			{
				continue;
			}

			if (!isEquipable(itemId))
			{
				continue;
			}
			ItemStats stats = itemManager.getItemStats(itemId);

			int slot = stats.getEquipment().getSlot();
			boolean free = worn == null || worn.getItem(slot) == null;
			// A two-handed weapon also takes the shield slot
			if (stats.getEquipment().isTwoHanded() && worn != null && worn.getItem(EquipmentInventorySlot.SHIELD.getSlotIdx()) != null)
			{
				free = false;
			}
			if (free)
			{
				wearable.add(itemId);
			}
		}
		return wearable;
	}

	private boolean isEquipable(int itemId)
	{
		ItemStats stats = itemManager.getItemStats(itemId);
		return stats != null && stats.isEquipable() && stats.getEquipment() != null;
	}

	/**
	 * Teleport items, graceful, magic secateurs, axes and staves: the run items worth wearing.
	 */
	private boolean isWearableRunItem(int itemId)
	{
		if (Teleport.ALL_ITEM_IDS.contains(itemId) || itemId == ItemID.FAIRY_ENCHANTED_SECATEURS || RunItems.AXES.contains(itemId))
		{
			return true;
		}
		if (!Runes.suppliedBy(ItemVariationMapping.map(itemId)).isEmpty())
		{
			return true;
		}
		return gracefulPieces.stream().anyMatch(piece -> piece.contains(itemId));
	}

	/**
	 * A patch's name without the "patch", to fit beside its task.
	 */
	private static String shortName(FarmPatch patch)
	{
		return patch.getName().replace(" patch", "");
	}

	/**
	 * A patch's state in a few words, with how long a growing one has left.
	 */
	static String describe(PatchState state, long now)
	{
		if (state.getStatus() != PatchStatus.GROWING)
		{
			return state.getStatus().getLabel();
		}
		long minutes = Math.max(1, (state.getReadyAt() - now + 59) / 60);
		return "Growing, " + (minutes >= 60 ? minutes / 60 + "h " + minutes % 60 + "m" : minutes + "m");
	}

	private void endRun()
	{
		if (!runActive)
		{
			return;
		}

		runActive = false;
		runStops.clear();
		completedStops.clear();
		reachedStops.clear();
		idleSince.clear();
		workedStops.clear();
		route = null;
		justPlanted.clear();
		router.clear();
		bankFilter.close();
	}

	/**
	 * Whether the preset stops at each location, and the teleport the player picked for it.
	 */
	private Map<FarmLocation, LocationSettings> locationSettings(RunPreset preset)
	{
		Map<FarmLocation, LocationSettings> settings = new EnumMap<>(FarmLocation.class);
		for (FarmLocation location : FarmLocation.values())
		{
			settings.put(location, new LocationSettings(preset.isEnabled(location), presetStore.teleport(location)));
		}
		return settings;
	}

	/**
	 * What the player has on them, with noted items counted as the items themselves, as gardeners
	 * take payments noted, and what they keep with the tool leprechaun.
	 */
	private Carried carried()
	{
		Map<Integer, Integer> items = new HashMap<>();
		Set<Integer> unlimitedRunes = new HashSet<>();
		for (int containerId : new int[]{InventoryID.INV, InventoryID.WORN})
		{
			ItemContainer container = client.getItemContainer(containerId);
			if (container != null)
			{
				for (Item item : container.getItems())
				{
					if (item.getId() > 0)
					{
						items.merge(itemManager.canonicalize(item.getId()), item.getQuantity(), Integer::sum);
						if (containerId == InventoryID.WORN)
						{
							unlimitedRunes.addAll(Runes.suppliedBy(ItemVariationMapping.map(item.getId())));
						}
					}
				}
			}
		}
		if (items.isEmpty())
		{
			return client.getGameState() == GameState.LOGGED_IN
				? new Carried(Map.of(), Map.of(), Set.of(), leprechaun(), false)
				: Carried.NOTHING;
		}

		// The game only sends a seed box's contents once it has been looked in; until then they're unknown
		boolean seedBoxUnknown = false;
		if (RunItems.SEED_BOXES.stream().anyMatch(items::containsKey))
		{
			ItemContainer seedBox = client.getItemContainer(InventoryID.SEED_BOX);
			if (seedBox == null)
			{
				seedBoxUnknown = true;
			}
			else
			{
				for (Item item : seedBox.getItems())
				{
					if (item.getId() > 0)
					{
						items.merge(item.getId(), item.getQuantity(), Integer::sum);
					}
				}
			}
		}

		Map<Integer, Integer> pouchRunes = new HashMap<>();
		if (Teleport.RUNE_POUCHES.stream().anyMatch(items::containsKey))
		{
			EnumComposition runeItems = client.getEnum(EnumID.RUNEPOUCH_RUNE);
			for (int slot = 0; slot < RUNE_POUCH_TYPES.length; slot++)
			{
				int type = client.getVarbitValue(RUNE_POUCH_TYPES[slot]);
				int quantity = client.getVarbitValue(RUNE_POUCH_QUANTITIES[slot]);
				if (type != 0 && quantity > 0)
				{
					pouchRunes.merge(runeItems.getIntValue(type), quantity, Integer::sum);
				}
			}
		}
		return new Carried(items, pouchRunes, unlimitedRunes, leprechaun(), seedBoxUnknown);
	}

	/**
	 * The tools and compost stored with the tool leprechaun, combined from its varbits as the game's
	 * own script for it does.
	 */
	private Map<Integer, Integer> leprechaun()
	{
		Map<Integer, Integer> stored = new HashMap<>();
		stored.put(ItemID.RAKE, 2 * varbit(VarbitID.FARMING_TOOLS_EXTRARAKES) + varbit(VarbitID.FARMING_TOOLS_RAKE));
		stored.put(ItemID.DIBBER, 2 * varbit(VarbitID.FARMING_TOOLS_EXTRADIBBERS) + varbit(VarbitID.FARMING_TOOLS_DIBBER));
		stored.put(ItemID.SPADE, 2 * varbit(VarbitID.FARMING_TOOLS_EXTRASPADES) + varbit(VarbitID.FARMING_TOOLS_SPADE));
		int secateurs = 2 * varbit(VarbitID.FARMING_TOOLS_EXTRASECATEURS) + varbit(VarbitID.FARMING_TOOLS_SECATEURS);
		stored.put(varbit(VarbitID.FARMING_TOOLS_FAIRYSECATEURS) == 1 ? ItemID.FAIRY_ENCHANTED_SECATEURS : ItemID.SECATEURS, secateurs);
		stored.put(ItemID.PLANT_CURE, varbit(VarbitID.FARMING_TOOLS_PLANTCURE));
		stored.put(ItemID.BUCKET_COMPOST, 256 * varbit(VarbitID.FARMING_TOOLS_EXTRACOMPOST) + varbit(VarbitID.FARMING_TOOLS_COMPOST));
		stored.put(ItemID.BUCKET_SUPERCOMPOST, 256 * varbit(VarbitID.FARMING_TOOLS_EXTRASUPERCOMPOST) + varbit(VarbitID.FARMING_TOOLS_SUPERCOMPOST));
		stored.put(ItemID.BUCKET_ULTRACOMPOST, varbit(VarbitID.FARMING_TOOLS_ULTRACOMPOST));
		// 1 is an empty bottomless bucket, more than that a filled one
		if (varbit(VarbitID.FARMING_TOOLS_BOTTOMLESS_BUCKET_TYPE) > 1)
		{
			stored.put(ItemID.BOTTOMLESS_COMPOST_BUCKET_FILLED, 1);
		}
		stored.values().removeIf(count -> count <= 0);
		return stored;
	}

	private int varbit(int varbitId)
	{
		return client.getVarbitValue(varbitId);
	}

	private void checkPlugins()
	{
		timeTrackingActive = false;
		shortestPathActive = false;
		for (Plugin plugin : pluginManager.getPlugins())
		{
			if (TIME_TRACKING_PLUGIN.equals(plugin.getName()))
			{
				timeTrackingActive = pluginManager.isPluginActive(plugin);
			}
			else if (SHORTEST_PATH_PLUGIN.equals(plugin.getName()))
			{
				shortestPathActive = pluginManager.isPluginActive(plugin);
			}
		}
		bankTagsActive = bankFilter.isAvailable();
	}
}
