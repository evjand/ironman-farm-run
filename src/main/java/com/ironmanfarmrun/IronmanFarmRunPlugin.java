package com.ironmanfarmrun;

import com.google.inject.Provides;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
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
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
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

@Slf4j
@PluginDescriptor(
	name = "Ironman Farm Run",
	description = "Farm run helper: patch states from Time Tracking, routes from Shortest Path, a bank filter and step-by-step patch highlights",
	tags = {"farming", "farm run", "herb run", "ironman", "herbs", "allotment", "flower", "contract"}
)
@PluginDependency(BankTagsPlugin.class)
public class IronmanFarmRunPlugin extends Plugin implements IronmanFarmRunPanel.Listener
{
	private static final String TIME_TRACKING_PLUGIN = "Time Tracking";
	private static final String SHORTEST_PATH_PLUGIN = "Shortest Path";

	// Per-account choices for a location; each key ends in the location's enum name
	private static final String ENABLED_KEY_PREFIX = "patch.";
	private static final String TELEPORT_KEY_PREFIX = "teleport.";

	/**
	 * Which plugins are on rarely changes and takes a walk over every plugin to find out.
	 */
	private static final int PLUGIN_CHECK_TICKS = 10;
	/**
	 * How far past a location's patches the player can wander and still count as being there.
	 */
	private static final int LEAVING_SLACK = 15;

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
	private IronmanFarmRunConfig config;

	@Inject
	private PatchTracker patchTracker;

	@Inject
	private PatchObjects patchObjects;

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

	private IronmanFarmRunPanel panel;
	private NavigationButton navButton;
	/**
	 * Each graceful piece's item ids across every recolour.
	 */
	private List<List<Integer>> gracefulPieces = List.of();

	// Run state, only touched on the client thread
	private boolean runActive;
	/**
	 * The locations the run visits, fixed when it starts: those whose herb patch needed attention then.
	 * Each is visited once, so a patch that turns ready later doesn't send the player back.
	 */
	private final Set<FarmLocation> runStops = EnumSet.noneOf(FarmLocation.class);
	private final Set<FarmLocation> completedStops = EnumSet.noneOf(FarmLocation.class);
	/**
	 * Stops the player has been at on this run, to notice them leaving.
	 */
	private final Set<FarmLocation> reachedStops = EnumSet.noneOf(FarmLocation.class);
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
	 * Patches planted on this run that haven't had compost yet, while the player is still beside them.
	 */
	private final Set<FarmPatch> awaitingCompost = new HashSet<>();
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

		panel = new IronmanFarmRunPanel(this);
		navButton = NavigationButton.builder()
			.tooltip("Ironman Farm Run")
			.icon(navIcon())
			.priority(7)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);

		overlayManager.add(patchOverlay);
		overlayManager.add(itemHighlightOverlay);
		overlayManager.add(checklistOverlay);
		eventBus.register(patchObjects);
		eventBus.register(bankFilter);

		clientThread.invokeLater(() ->
		{
			bankFilter.register();
			patchObjects.scan();
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
		eventBus.unregister(bankFilter);

		clientThread.invokeLater(() ->
		{
			endRun();
			bankFilter.unregister();
			patchObjects.clear();
			patchOverlay.setHighlights(List.of());
			itemHighlightOverlay.setItems(Map.of());
			checklistOverlay.setContent(null);
			previousStatuses.clear();
			lastPanelState = null;
		});
	}

	@Provides
	IronmanFarmRunConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(IronmanFarmRunConfig.class);
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
		clientThread.invoke(this::refresh);
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		// Time Tracking saves a patch here the moment it sees it change
		if (PatchTracker.TIME_TRACKING_GROUP.equals(event.getGroup()) || IronmanFarmRunConfig.GROUP.equals(event.getGroup()))
		{
			clientThread.invoke(this::refresh);
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

	@Override
	public void startRun()
	{
		clientThread.invoke(() ->
		{
			if (runActive || client.getGameState() != GameState.LOGGED_IN)
			{
				return;
			}

			Set<FarmLocation> stops = herbStops(patchTracker.readStates(Instant.now().getEpochSecond()), locationSettings());
			if (stops.isEmpty())
			{
				return;
			}

			runActive = true;
			runStops.clear();
			runStops.addAll(stops);
			completedStops.clear();
			reachedStops.clear();
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
	public void setLocationSettings(FarmLocation location, LocationSettings settings)
	{
		clientThread.invoke(() ->
		{
			if (configManager.getRSProfileKey() == null)
			{
				return;
			}

			// Saved per account, as accounts differ in what they have unlocked.
			// The config changes these post trigger the refresh.
			String name = location.name();
			configManager.setRSProfileConfiguration(IronmanFarmRunConfig.GROUP, ENABLED_KEY_PREFIX + name, settings.isEnabled());
			configManager.setRSProfileConfiguration(IronmanFarmRunConfig.GROUP, TELEPORT_KEY_PREFIX + name, settings.getTeleport().name());
		});
	}

	@Override
	public void setRunOption(RunOption option, boolean on)
	{
		// The config change this posts triggers the refresh
		configManager.setConfiguration(IronmanFarmRunConfig.GROUP, option.getConfigKey(), on);
	}

	/**
	 * Re-reads everything, moves the run along, and updates the overlays and panel.
	 */
	private void refresh()
	{
		boolean loggedIn = client.getGameState() == GameState.LOGGED_IN;
		long now = Instant.now().getEpochSecond();
		Map<FarmPatch, PatchState> patches = patchTracker.readStates(now);
		Set<FarmPatch> composted = patchTracker.readComposted();
		Map<FarmLocation, LocationSettings> settings = locationSettings();
		Carried carried = carried();
		WorldPoint playerPoint = loggedIn && client.getLocalPlayer() != null ? client.getLocalPlayer().getWorldLocation() : null;
		FarmingContract contract = loggedIn ? contract() : null;

		updateAwaitingCompost(patches, composted, playerPoint);

		// The checklist goes by what the player wants; the steps by what they can do with what they carry
		RunPlan.Options wantedOptions = new RunPlan.Options(config.allotments(), config.flowers(), config.compostStep());
		boolean carryingCompost = carried.has(ItemID.BOTTOMLESS_COMPOST_BUCKET_FILLED) || carried.hasAny(RunItems.COMPOST_BUCKETS);
		RunPlan.Options options = new RunPlan.Options(config.allotments(), config.flowers(), config.compostStep() && carryingCompost);

		List<FarmLocation> stops = orderedStops(runActive ? remainingStops() : herbStops(patches, settings), playerPoint);
		RunPlan plan = RunPlan.of(stops, patches, composted, awaitingCompost, options, carried, contract);
		if (runActive && playerPoint != null && updateCompletedStops(plan, playerPoint))
		{
			stops = orderedStops(remainingStops(), playerPoint);
			plan = RunPlan.of(stops, patches, composted, awaitingCompost, options, carried, contract);
		}
		RunPlan wanted = options.equals(wantedOptions)
			? plan
			: RunPlan.of(stops, patches, composted, awaitingCompost, wantedOptions, carried, contract);

		bankFilter.setSpec(bankFilterSpec(settings, stops, contract));

		// Patch data is per account and unavailable between worlds, so only judge the run while logged in
		if (runActive && loggedIn)
		{
			if (stops.isEmpty())
			{
				endRun();
				client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Farm run complete.", null);
			}
			else
			{
				Route wantedRoute = route(stops, plan, settings, carried);
				if (!wantedRoute.equals(route))
				{
					router.show(wantedRoute);
					route = wantedRoute;
				}
			}
		}

		List<ChecklistItem> checklist = Checklist.build(wanted, settings, carried,
			new Checklist.Options(config.allotments(), config.flowers(), config.compostStep(), config.volcanicAsh(),
				config.graceful() ? gracefulPieces : List.of()),
			contract);

		patchOverlay.setHighlights(highlights(plan, contract));
		itemHighlightOverlay.setItems(itemHighlights(plan, playerPoint));
		checklistOverlay.setContent(overlayContent(loggedIn, plan, patches, stops, checklist, playerPoint, options, contract));

		Set<RunOption> runOptions = EnumSet.noneOf(RunOption.class);
		for (RunOption option : RunOption.values())
		{
			if (option.isOn(config))
			{
				runOptions.add(option);
			}
		}

		PanelState state = new PanelState(loggedIn, runActive, patches, settings, new LinkedHashSet<>(stops), checklist, runOptions,
			contract == null ? null : contract.seedLabel() + " at the Farming Guild", now / 60,
			timeTrackingActive, shortestPathActive, bankTagsActive);
		if (!state.equals(lastPanelState))
		{
			lastPanelState = state;
			SwingUtilities.invokeLater(() -> panel.update(state));
		}
	}

	/**
	 * The enabled locations whose herb patch needs attention, which are the stops a run would make.
	 */
	private static Set<FarmLocation> herbStops(Map<FarmPatch, PatchState> patches, Map<FarmLocation, LocationSettings> settings)
	{
		Set<FarmLocation> stops = EnumSet.noneOf(FarmLocation.class);
		for (FarmLocation location : FarmLocation.values())
		{
			if (settings.get(location).isEnabled()
				&& patches.getOrDefault(location.herbPatch(), PatchState.UNKNOWN).getStatus().needsVisit())
			{
				stops.add(location);
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
	 * The stops with the one the player is at first, as that is where seeds are used first.
	 */
	private static List<FarmLocation> orderedStops(Collection<FarmLocation> stops, @Nullable WorldPoint playerPoint)
	{
		List<FarmLocation> ordered = new ArrayList<>();
		for (FarmLocation location : stops)
		{
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
	 * A stop is done once its herb patch is, and either nothing else there needs doing or the player
	 * has moved on.
	 *
	 * @return whether any stop was completed
	 */
	private boolean updateCompletedStops(RunPlan plan, WorldPoint playerPoint)
	{
		boolean changed = false;
		for (FarmLocation location : remainingStops())
		{
			if (location.isNear(playerPoint, 0))
			{
				reachedStops.add(location);
			}

			boolean herbDone = !plan.getSteps().containsKey(location.herbPatch());
			boolean left = reachedStops.contains(location) && !location.isNear(playerPoint, LEAVING_SLACK);
			if (herbDone && (!plan.hasSteps(location) || left))
			{
				completedStops.add(location);
				changed = true;
			}
		}
		return changed;
	}

	/**
	 * Compost can go on after planting, but a planted patch is otherwise done. So a patch planted on
	 * this run keeps a compost step for as long as the player stays beside it.
	 */
	private void updateAwaitingCompost(Map<FarmPatch, PatchState> patches, Set<FarmPatch> composted, @Nullable WorldPoint playerPoint)
	{
		if (!runActive || playerPoint == null)
		{
			awaitingCompost.clear();
			if (playerPoint == null)
			{
				previousStatuses.clear();
			}
		}

		for (Map.Entry<FarmPatch, PatchState> entry : patches.entrySet())
		{
			PatchStatus status = entry.getValue().getStatus();
			PatchStatus previous = playerPoint == null ? null : previousStatuses.put(entry.getKey(), status);
			boolean justPlanted = status == PatchStatus.GROWING
				&& (previous == PatchStatus.EMPTY || previous == PatchStatus.WEEDS || previous == PatchStatus.READY || previous == PatchStatus.DEAD);
			if (runActive && justPlanted)
			{
				awaitingCompost.add(entry.getKey());
			}
		}

		awaitingCompost.removeIf(patch ->
			patches.get(patch).getStatus() != PatchStatus.GROWING
				|| composted.contains(patch)
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

	private Route route(List<FarmLocation> stops, RunPlan plan, Map<FarmLocation, LocationSettings> settings, Carried carried)
	{
		boolean requireSeeds = config.bankDetourForSeeds() && !carried.hasAny(RunItems.SEED_BOXES);
		Map<Integer, Integer> anyHerbSeed = new HashMap<>();
		for (int seed : PatchType.HERB.getSeeds())
		{
			anyHerbSeed.put(seed, 1);
		}

		Map<FarmLocation, Map<Integer, Integer>> routeStops = new LinkedHashMap<>();
		boolean useHouse = false;
		boolean useSpells = false;
		for (FarmLocation location : stops)
		{
			// The herb seed is what makes the stop worth it; a contract stop needs the contract's seed
			Map<Integer, Integer> seeds = Map.of();
			FarmPatch herb = location.herbPatch();
			if (requireSeeds && plan.getToPlant().contains(herb))
			{
				Integer contractSeed = plan.getContractSeeds().get(herb);
				seeds = contractSeed != null ? Map.of(contractSeed, 1) : anyHerbSeed;
			}
			routeStops.put(location, seeds);

			Teleport choice = settings.get(location).getTeleport();
			useHouse |= choice.isViaHouse();
			useSpells |= choice.isSpell();
		}

		// Shortest Path looks at the inventory once per search, so the route notes what it would find
		Set<Integer> routeItems = new HashSet<>();
		for (int itemId : carried.getItems().keySet())
		{
			if (Teleport.ALL_ITEM_IDS.contains(itemId) || PatchType.HERB.getSeeds().contains(itemId))
			{
				routeItems.add(itemId);
			}
		}

		return new Route(routeStops, config.carriedTeleportsOnly(), useHouse, useSpells, routeItems);
	}

	private BankFilterSpec bankFilterSpec(Map<FarmLocation, LocationSettings> settings, Collection<FarmLocation> stops,
		@Nullable FarmingContract contract)
	{
		List<Integer> extraEquipment = new ArrayList<>();
		if (config.graceful())
		{
			gracefulPieces.forEach(extraEquipment::addAll);
		}
		if (config.volcanicAsh())
		{
			extraEquipment.add(ItemID.FOSSIL_VOLCANIC_ASH);
		}

		// The teleports to bring for the stops left
		Set<Integer> teleportItems = new LinkedHashSet<>();
		if (config.bankTeleports())
		{
			for (FarmLocation location : stops)
			{
				for (Teleport teleport : location.teleportsFor(settings.get(location).getTeleport()))
				{
					teleportItems.addAll(teleport.bankItemIds());
				}
			}
		}

		return new BankFilterSpec(config.allotments(), config.flowers(), contract == null ? -1 : contract.getSeed(),
			extraEquipment, List.copyOf(teleportItems));
	}

	private static String stepLabel(FarmPatch patch, Step step, RunPlan plan, @Nullable FarmingContract contract)
	{
		if (step == Step.PLANT && contract != null && plan.getContractSeeds().containsKey(patch))
		{
			return "Plant " + contract.getName();
		}
		return step.getLabel();
	}

	private List<PatchOverlay.Highlight> highlights(RunPlan plan, @Nullable FarmingContract contract)
	{
		if (!runActive || !config.highlightPatches())
		{
			return List.of();
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
				String label = labelled.add(patch) ? stepLabel(patch, step, plan, contract) : null;
				highlights.add(new PatchOverlay.Highlight(entry.getKey(), step.getColor(), label));
			}
		}
		return highlights;
	}

	/**
	 * The items the patches the player is at need next, in the colour of the step that uses them.
	 */
	private Map<Integer, Color> itemHighlights(RunPlan plan, @Nullable WorldPoint playerPoint)
	{
		if (!runActive || !config.highlightItems() || playerPoint == null)
		{
			return Map.of();
		}

		Map<Integer, Color> items = new HashMap<>();
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
					needed = List.of(ItemID.PLANT_CURE);
					break;
				case RAKE:
					needed = List.of(ItemID.RAKE);
					break;
				case CLEAR:
					needed = List.of(ItemID.SPADE);
					break;
				default:
					// Herbs and flowers are picked by hand; allotment crops are dug up
					needed = patch.getType() == PatchType.ALLOTMENT ? List.of(ItemID.SPADE) : List.of();
					break;
			}

			for (int itemId : needed)
			{
				items.putIfAbsent(itemId, step.getColor());
			}
		}
		return items;
	}

	@Nullable
	private ChecklistOverlay.Content overlayContent(boolean loggedIn, RunPlan plan, Map<FarmPatch, PatchState> patches,
		List<FarmLocation> stops, List<ChecklistItem> checklist, @Nullable WorldPoint playerPoint, RunPlan.Options options,
		@Nullable FarmingContract contract)
	{
		IronmanFarmRunConfig.ChecklistOverlayMode mode = config.checklistOverlay();
		boolean show = mode == IronmanFarmRunConfig.ChecklistOverlayMode.ALWAYS
			|| (mode == IronmanFarmRunConfig.ChecklistOverlayMode.DURING_RUN && runActive);
		if (!loggedIn || !show)
		{
			return null;
		}

		// At the stop the player is at: each patch they tend there and what it needs next
		List<ChecklistOverlay.Line> steps = new ArrayList<>();
		Set<FarmPatch> contractPatches = contract == null ? Set.of() : new HashSet<>(contract.patches(options.includes(contract.getType())));
		for (FarmLocation location : stops)
		{
			if (playerPoint == null || !location.isNear(playerPoint, 0))
			{
				continue;
			}

			for (FarmPatch patch : location.getPatches())
			{
				if (!options.includes(patch.getType()) && !contractPatches.contains(patch))
				{
					continue;
				}

				Step step = plan.getSteps().get(patch);
				if (step != null)
				{
					steps.add(new ChecklistOverlay.Line(patch.getName(), stepLabel(patch, step, plan, contract), step.getColor()));
				}
				else if (plan.getWithoutSeeds().contains(patch))
				{
					steps.add(new ChecklistOverlay.Line(patch.getName(), "No seeds", ColorScheme.MEDIUM_GRAY_COLOR));
				}
				else
				{
					boolean growing = patches.get(patch).getStatus() == PatchStatus.GROWING;
					steps.add(new ChecklistOverlay.Line(patch.getName(), growing ? "Done" : "-", ColorScheme.LIGHT_GRAY_COLOR));
				}
			}
		}

		List<ChecklistOverlay.Line> lines = new ArrayList<>();
		for (ChecklistItem item : checklist)
		{
			if (!config.checklistMissingOnly() || item.getState() != ChecklistItem.State.GOOD)
			{
				lines.add(new ChecklistOverlay.Line(item.getLabel(), item.getValue(), ChecklistOverlay.color(item.getState())));
			}
		}

		int left = stops.size();
		String title = runActive ? "Farm run: " + left + (left == 1 ? " stop left" : " stops left") : "Farm run";
		return new ChecklistOverlay.Content(title, steps, lines);
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
		route = null;
		awaitingCompost.clear();
		router.clear();
		bankFilter.close();
	}

	private Map<FarmLocation, LocationSettings> locationSettings()
	{
		Map<FarmLocation, LocationSettings> settings = new EnumMap<>(FarmLocation.class);
		for (FarmLocation location : FarmLocation.values())
		{
			settings.put(location, new LocationSettings(
				// On unless the player has unticked it
				!"false".equals(savedChoice(ENABLED_KEY_PREFIX + location.name())),
				savedTeleport(location)));
		}
		return settings;
	}

	private Teleport savedTeleport(FarmLocation location)
	{
		String saved = savedChoice(TELEPORT_KEY_PREFIX + location.name());
		if (saved != null)
		{
			try
			{
				Teleport teleport = Teleport.valueOf(saved);
				if (location.teleportChoices().contains(teleport))
				{
					return teleport;
				}
			}
			catch (IllegalArgumentException e)
			{
				// A teleport this version no longer knows; fall back to any
			}
		}
		return Teleport.ANY;
	}

	@Nullable
	private String savedChoice(String key)
	{
		return configManager.getRSProfileConfiguration(IronmanFarmRunConfig.GROUP, key);
	}

	private Carried carried()
	{
		Map<Integer, Integer> items = new HashMap<>();
		for (int containerId : new int[]{InventoryID.INV, InventoryID.WORN})
		{
			ItemContainer container = client.getItemContainer(containerId);
			if (container != null)
			{
				for (Item item : container.getItems())
				{
					if (item.getId() > 0)
					{
						items.merge(item.getId(), item.getQuantity(), Integer::sum);
					}
				}
			}
		}
		if (items.isEmpty())
		{
			return Carried.NOTHING;
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
		return new Carried(items, pouchRunes);
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

	private static BufferedImage navIcon()
	{
		BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		// A leaf on a stem
		g.setColor(new Color(0x5F, 0xA8, 0x3A));
		g.rotate(Math.toRadians(-45), 8, 8);
		g.fillOval(4, 1, 8, 12);
		g.setColor(new Color(0x2F, 0x6B, 0x1E));
		g.setStroke(new BasicStroke(1.2f));
		g.drawLine(8, 3, 8, 15);
		g.dispose();
		return image;
	}
}
