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
	description = "Herb and tree run helper: patch states from Time Tracking, routes from Shortest Path, a bank filter and step-by-step patch highlights",
	tags = {"farming", "farm run", "herb run", "tree run", "ironman", "herbs", "trees", "allotment", "flower", "contract"}
)
@PluginDependency(BankTagsPlugin.class)
public class IronmanFarmRunPlugin extends Plugin implements IronmanFarmRunPanel.Listener
{
	private static final String TIME_TRACKING_PLUGIN = "Time Tracking";
	private static final String SHORTEST_PATH_PLUGIN = "Shortest Path";

	// Per-account choices for a location; each key ends in the location's enum name, and those for
	// tree runs start with the tree prefix
	private static final String ENABLED_KEY_PREFIX = "patch.";
	private static final String TELEPORT_KEY_PREFIX = "teleport.";
	private static final String TREE_KEY_PREFIX = "tree.";

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
	private IronmanFarmRunConfig config;

	@Inject
	private PatchTracker patchTracker;

	@Inject
	private PatchObjects patchObjects;

	@Inject
	private Gardeners gardeners;

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
	private RunType activeRunType = RunType.HERB;
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
		eventBus.register(gardeners);
		eventBus.register(bankFilter);
		eventBus.register(router);

		clientThread.invokeLater(() ->
		{
			bankFilter.register();
			patchObjects.scan();
			gardeners.scan();
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
		eventBus.unregister(bankFilter);
		eventBus.unregister(router);

		clientThread.invokeLater(() ->
		{
			endRun();
			bankFilter.unregister();
			patchObjects.clear();
			gardeners.clear();
			patchOverlay.setHighlights(List.of(), List.of());
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

			RunType runType = config.runType();
			Set<FarmLocation> stops = stopsFor(runType, patchTracker.readStates(Instant.now().getEpochSecond()),
				locationSettings(runType), farmingLevel());
			if (stops.isEmpty())
			{
				return;
			}

			runActive = true;
			activeRunType = runType;
			runStops.clear();
			runStops.addAll(stops);
			completedStops.clear();
			reachedStops.clear();
			idleSince.clear();
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
			if (configManager.getRSProfileKey() == null)
			{
				return;
			}

			// Saved per account, as accounts differ in what they have unlocked.
			// The config changes these post trigger the refresh.
			String prefix = keyPrefix(runType());
			String name = location.name();
			configManager.setRSProfileConfiguration(IronmanFarmRunConfig.GROUP, prefix + ENABLED_KEY_PREFIX + name, settings.isEnabled());
			configManager.setRSProfileConfiguration(IronmanFarmRunConfig.GROUP, prefix + TELEPORT_KEY_PREFIX + name, settings.getTeleport().name());
		});
	}

	@Override
	public void setRunOption(RunOption option, boolean on)
	{
		// The config change this posts triggers the refresh
		configManager.setConfiguration(IronmanFarmRunConfig.GROUP, option.getConfigKey(), on);
	}

	@Override
	public void setRunType(RunType runType)
	{
		configManager.setConfiguration(IronmanFarmRunConfig.GROUP, IronmanFarmRunConfig.RUN_TYPE, runType);
	}

	/**
	 * The kind of run in progress, or the one the player has picked.
	 */
	private RunType runType()
	{
		return runActive ? activeRunType : config.runType();
	}

	/**
	 * Re-reads everything, moves the run along, and updates the overlays and panel.
	 */
	private void refresh()
	{
		RunType runType = runType();
		boolean loggedIn = client.getGameState() == GameState.LOGGED_IN;
		long now = Instant.now().getEpochSecond();
		Map<FarmPatch, PatchState> patches = patchTracker.readStates(now);
		Set<FarmPatch> composted = patchTracker.readComposted();
		Set<FarmPatch> protectedTrees = patchTracker.readProtected();
		Map<FarmLocation, LocationSettings> settings = locationSettings(runType);
		Carried carried = carried();
		WorldPoint playerPoint = loggedIn && client.getLocalPlayer() != null ? client.getLocalPlayer().getWorldLocation() : null;
		FarmingContract contract = loggedIn ? contract() : null;
		int farmingLevel = farmingLevel();

		updateJustPlanted(patches, playerPoint);

		// The checklist goes by what the player wants; the steps by what they can do with what they carry
		RunPlan.Options wantedOptions = new RunPlan.Options(runType, config.allotments(), config.flowers(),
			config.compostStep(), config.protectTrees(), farmingLevel);
		boolean carryingCompost = carried.has(ItemID.BOTTOMLESS_COMPOST_BUCKET_FILLED) || carried.hasAny(RunItems.COMPOST_BUCKETS);
		RunPlan.Options options = new RunPlan.Options(runType, config.allotments(), config.flowers(),
			config.compostStep() && carryingCompost, config.protectTrees(), farmingLevel);

		List<FarmLocation> stops = orderedStops(runType, runActive ? remainingStops() : stopsFor(runType, patches, settings, farmingLevel), playerPoint);
		RunPlan plan = RunPlan.of(stops, patches, composted, protectedTrees, justPlanted, options, carried, contract);
		if (runActive && playerPoint != null && updateCompletedStops(runType, plan, playerPoint, farmingLevel))
		{
			stops = orderedStops(runType, remainingStops(), playerPoint);
			plan = RunPlan.of(stops, patches, composted, protectedTrees, justPlanted, options, carried, contract);
		}
		RunPlan wanted = options.equals(wantedOptions)
			? plan
			: RunPlan.of(stops, patches, composted, protectedTrees, justPlanted, wantedOptions, carried, contract);

		bankFilter.setSpec(bankFilterSpec(runType, settings, stops, contract));

		// Patch data is per account and unavailable between worlds, so only judge the run while logged in
		if (runActive && loggedIn)
		{
			if (stops.isEmpty())
			{
				endRun();
				client.addChatMessage(ChatMessageType.GAMEMESSAGE, "",
					Character.toUpperCase(runType.getNoun().charAt(0)) + runType.getNoun().substring(1) + " complete.", null);
			}
			else
			{
				Route wantedRoute = route(runType, stops, plan, settings, carried);
				if (!wantedRoute.equals(route))
				{
					router.show(wantedRoute);
					route = wantedRoute;
				}
			}
		}

		List<ChecklistItem> checklist = Checklist.build(wanted, patches, settings, carried,
			new Checklist.Options(runType, config.allotments(), config.flowers(), config.compostStep(), config.protectTrees(),
				config.volcanicAsh(), config.graceful() ? gracefulPieces : List.of()),
			contract);

		updateHighlights(plan, patches, contract);
		itemHighlightOverlay.setItems(itemHighlights(plan, patches, playerPoint));
		checklistOverlay.setContent(overlayContent(runType, loggedIn, plan, patches, stops, checklist, playerPoint, options, contract));

		Set<RunOption> runOptions = EnumSet.noneOf(RunOption.class);
		for (RunOption option : RunOption.values())
		{
			if (option.isOn(config))
			{
				runOptions.add(option);
			}
		}

		List<StopView> stopViews = stopViews(runType, stops, plan, patches, settings, playerPoint, options, contract);
		PanelState state = new PanelState(loggedIn, runActive, runType, patches, settings, new LinkedHashSet<>(stops), stopViews, checklist,
			runOptions, contract == null ? null : contract.seedLabel() + " at the Farming Guild", farmingLevel, now / 60,
			timeTrackingActive, shortestPathActive, bankTagsActive);
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
	private static Set<FarmLocation> stopsFor(RunType runType, Map<FarmPatch, PatchState> patches,
		Map<FarmLocation, LocationSettings> settings, int farmingLevel)
	{
		Set<FarmLocation> stops = EnumSet.noneOf(FarmLocation.class);
		for (FarmLocation location : FarmLocation.of(runType))
		{
			if (!settings.get(location).isEnabled())
			{
				continue;
			}
			for (FarmPatch anchor : location.anchors(runType, farmingLevel))
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
	static List<FarmLocation> orderedStops(RunType runType, Collection<FarmLocation> stops, @Nullable WorldPoint playerPoint)
	{
		List<FarmLocation> ordered = new ArrayList<>();
		for (FarmLocation location : FarmLocation.recommendedOrder(runType))
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
	private boolean updateCompletedStops(RunType runType, RunPlan plan, WorldPoint playerPoint, int farmingLevel)
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

			boolean anchorsDone = location.anchors(runType, farmingLevel).stream().noneMatch(plan.getSteps()::containsKey);
			boolean left = reachedStops.contains(location) && !location.isNear(playerPoint, LEAVING_SLACK);
			boolean settled = idleSince.containsKey(location) && tick - idleSince.get(location) >= SETTLE_TICKS;
			if (anchorsDone && (settled || left))
			{
				completedStops.add(location);
				idleSince.remove(location);
				changed = true;
			}
		}
		return changed;
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

	private Route route(RunType runType, List<FarmLocation> stops, RunPlan plan, Map<FarmLocation, LocationSettings> settings,
		Carried carried)
	{
		boolean requireSeeds = config.bankDetourForSeeds() && !carried.hasAny(RunItems.SEED_BOXES);

		Map<FarmLocation, List<Map<Integer, Integer>>> routeStops = new LinkedHashMap<>();
		boolean useHouse = false;
		boolean useSpells = false;
		for (FarmLocation location : stops)
		{
			// The anchors' seeds are what make the stop worth it; a contract patch needs the contract's
			List<Map<Integer, Integer>> seeds = new ArrayList<>();
			Set<PatchType> covered = EnumSet.noneOf(PatchType.class);
			for (FarmPatch anchor : location.anchors(runType))
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
		}

		// Shortest Path looks at the inventory once per search, so the route notes what it would find
		Set<Integer> routeItems = new HashSet<>();
		for (int itemId : carried.getItems().keySet())
		{
			boolean anchorSeed = runType.getAnchorTypes().stream().anyMatch(type -> type.getSeeds().contains(itemId));
			if (Teleport.ALL_ITEM_IDS.contains(itemId) || anchorSeed)
			{
				routeItems.add(itemId);
			}
		}

		if (config.stopOrder() == IronmanFarmRunConfig.StopOrder.RECOMMENDED && !routeStops.isEmpty())
		{
			// Only the next stop, which is the one the player is at if they are at one
			Map.Entry<FarmLocation, List<Map<Integer, Integer>>> next = routeStops.entrySet().iterator().next();
			routeStops = Map.of(next.getKey(), next.getValue());
		}

		return new Route(runType, routeStops, config.carriedTeleportsOnly(), useHouse, useSpells, routeItems);
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

	private BankFilterSpec bankFilterSpec(RunType runType, Map<FarmLocation, LocationSettings> settings,
		Collection<FarmLocation> stops, @Nullable FarmingContract contract)
	{
		Map<BankSection, List<Integer>> sections = new EnumMap<>(BankSection.class);
		List<Integer> equipment = new ArrayList<>(RunItems.EQUIPMENT);
		if (runType == RunType.HERB)
		{
			sections.put(BankSection.HERB_SEEDS, RunItems.HERB_SEEDS);
			if (config.allotments())
			{
				sections.put(BankSection.ALLOTMENT_SEEDS, RunItems.ALLOTMENT_SEEDS);
			}
			if (config.flowers())
			{
				sections.put(BankSection.FLOWER_SEEDS, RunItems.FLOWER_SEEDS);
			}
		}
		else
		{
			sections.put(BankSection.TREE_SAPLINGS, RunItems.TREE_SAPLINGS);
			sections.put(BankSection.FRUIT_TREE_SAPLINGS, RunItems.FRUIT_TREE_SAPLINGS);
			if (config.protectTrees())
			{
				Set<Integer> payments = new LinkedHashSet<>();
				for (Sapling sapling : Sapling.values())
				{
					payments.add(sapling.getPayment());
				}
				sections.put(BankSection.PAYMENTS, List.copyOf(payments));
			}
			equipment.addAll(RunItems.TREE_EQUIPMENT);
		}

		// A contract's seed is shown even when the player doesn't grow that kind of crop otherwise
		if (contract != null && RunType.of(contract.getType()) == runType)
		{
			BankSection section = contract.getType() == PatchType.ALLOTMENT ? BankSection.ALLOTMENT_SEEDS
				: contract.getType() == PatchType.FLOWER ? BankSection.FLOWER_SEEDS : null;
			if (section != null)
			{
				sections.putIfAbsent(section, List.of(contract.getSeed()));
			}
		}

		if (config.graceful())
		{
			gracefulPieces.forEach(equipment::addAll);
		}
		if (config.volcanicAsh())
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
				for (Teleport teleport : location.teleportsFor(settings.get(location).getTeleport()))
				{
					teleportItems.addAll(teleport.bankItemIds());
				}
			}
			sections.put(BankSection.TELEPORTS, List.copyOf(teleportItems));
		}

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
			patchOverlay.setHighlights(List.of(), List.of());
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

		// The gardener takes the payment, and can take down a grown tree for coins
		List<PatchOverlay.NpcHighlight> npcHighlights = new ArrayList<>();
		for (Map.Entry<NPC, FarmPatch> entry : gardeners.get().entrySet())
		{
			FarmPatch patch = entry.getValue();
			Step step = plan.getSteps().get(patch);
			if (step == Step.PAY)
			{
				npcHighlights.add(new PatchOverlay.NpcHighlight(entry.getKey(), step.getColor(), stepLabel(patch, step, plan, patches, contract)));
			}
			else if (step == Step.REMOVE)
			{
				npcHighlights.add(new PatchOverlay.NpcHighlight(entry.getKey(), step.getColor(), "Remove for " + Checklist.REMOVAL_COINS + " coins"));
			}
		}
		patchOverlay.setHighlights(highlights, npcHighlights);
	}

	/**
	 * The items the patches the player is at need next, in the colour of the step that uses them.
	 */
	private Map<Integer, Color> itemHighlights(RunPlan plan, Map<FarmPatch, PatchState> patches, @Nullable WorldPoint playerPoint)
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
					// Herbs and flowers are picked by hand; allotment crops are dug up
					needed = patch.getType() == PatchType.ALLOTMENT ? List.of(ItemID.SPADE) : List.of();
					break;
				default:
					needed = List.of();
					break;
			}

			for (int itemId : needed)
			{
				items.putIfAbsent(itemId, step.getColor());
			}
		}
		return items;
	}

	/**
	 * Each patch tended at a stop and what it needs next, with the first thing to do marked.
	 */
	private static List<StopView.Task> tasks(RunType runType, FarmLocation location, RunPlan plan,
		Map<FarmPatch, PatchState> patches, RunPlan.Options options, @Nullable FarmingContract contract)
	{
		Set<FarmPatch> contractPatches = contract == null || RunType.of(contract.getType()) != runType
			? Set.of()
			: new HashSet<>(contract.patches(options.includes(contract.getType())));

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
				tasks.add(new StopView.Task(patch.getName(), stepLabel(patch, step, plan, patches, contract), step.getColor(), !nextFound));
				nextFound = true;
			}
			else if (plan.getWithoutSeeds().contains(patch))
			{
				tasks.add(new StopView.Task(patch.getName(), "No seeds", ColorScheme.MEDIUM_GRAY_COLOR, false));
			}
			else
			{
				boolean growing = patches.get(patch).getStatus() == PatchStatus.GROWING;
				tasks.add(new StopView.Task(patch.getName(), growing ? "Done" : "-", ColorScheme.LIGHT_GRAY_COLOR, false));
			}
		}
		return tasks;
	}

	/**
	 * The run's stops for the side panel, in the order of the run's guide: those finished, the one the
	 * player is at or headed to, and those still to come.
	 */
	private List<StopView> stopViews(RunType runType, List<FarmLocation> stops, RunPlan plan, Map<FarmPatch, PatchState> patches,
		Map<FarmLocation, LocationSettings> settings, @Nullable WorldPoint playerPoint, RunPlan.Options options,
		@Nullable FarmingContract contract)
	{
		if (!runActive)
		{
			return List.of();
		}

		FarmLocation current = currentStop(stops, playerPoint);
		List<StopView> views = new ArrayList<>();
		for (FarmLocation location : FarmLocation.recommendedOrder(runType))
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
				stage == StopView.Stage.CURRENT ? tasks(runType, location, plan, patches, options, contract) : List.of(),
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
		if (config.stopOrder() == IronmanFarmRunConfig.StopOrder.RECOMMENDED)
		{
			return first;
		}

		// Nearest next: whichever stop Shortest Path picked, once it has said
		FarmLocation headingTo = router.headingTo();
		return headingTo != null && stops.contains(headingTo) ? headingTo : first;
	}

	@Nullable
	private ChecklistOverlay.Content overlayContent(RunType runType, boolean loggedIn, RunPlan plan, Map<FarmPatch, PatchState> patches,
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
		for (FarmLocation location : stops)
		{
			if (playerPoint == null || !location.isNear(playerPoint, 0))
			{
				continue;
			}

			for (StopView.Task task : tasks(runType, location, plan, patches, options, contract))
			{
				steps.add(new ChecklistOverlay.Line(task.getPatch(), task.getLabel(), task.getColor()));
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
		String noun = Character.toUpperCase(runType.getNoun().charAt(0)) + runType.getNoun().substring(1);
		String title = runActive ? noun + ": " + left + (left == 1 ? " stop left" : " stops left") : noun;
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
		idleSince.clear();
		route = null;
		justPlanted.clear();
		router.clear();
		bankFilter.close();
	}

	private static String keyPrefix(RunType runType)
	{
		// Herb runs came first, so their keys have no prefix
		return runType == RunType.TREE ? TREE_KEY_PREFIX : "";
	}

	private Map<FarmLocation, LocationSettings> locationSettings(RunType runType)
	{
		String prefix = keyPrefix(runType);
		Map<FarmLocation, LocationSettings> settings = new EnumMap<>(FarmLocation.class);
		for (FarmLocation location : FarmLocation.values())
		{
			settings.put(location, new LocationSettings(
				// On unless the player has unticked it
				!"false".equals(savedChoice(prefix + ENABLED_KEY_PREFIX + location.name())),
				savedTeleport(location, prefix)));
		}
		return settings;
	}

	private Teleport savedTeleport(FarmLocation location, String prefix)
	{
		String saved = savedChoice(prefix + TELEPORT_KEY_PREFIX + location.name());
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

	/**
	 * What the player has on them, with noted items counted as the items themselves, as gardeners
	 * take payments noted.
	 */
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
						items.merge(itemManager.canonicalize(item.getId()), item.getQuantity(), Integer::sum);
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
