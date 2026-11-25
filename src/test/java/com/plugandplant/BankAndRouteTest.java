package com.plugandplant;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class BankAndRouteTest
{
	private static BankFilterSpec spec(Object... sectionsAndItems)
	{
		Map<BankSection, List<Integer>> sections = new LinkedHashMap<>();
		for (int i = 0; i < sectionsAndItems.length; i += 2)
		{
			@SuppressWarnings("unchecked")
			List<Integer> items = (List<Integer>) sectionsAndItems[i + 1];
			sections.put((BankSection) sectionsAndItems[i], items);
		}
		return new BankFilterSpec(sections);
	}

	@Test
	public void bankFilterShowsEachItemInItsSection()
	{
		BankFilterSpec spec = spec(
			BankSection.HERB_SEEDS, RunItems.HERB_SEEDS,
			BankSection.EQUIPMENT, RunItems.EQUIPMENT,
			BankSection.TELEPORTS, List.of(ItemID.ECTOPHIAL));

		assertEquals(BankSection.HERB_SEEDS, spec.sectionOf(ItemID.RANARR_SEED));
		assertEquals(BankSection.EQUIPMENT, spec.sectionOf(ItemID.SPADE));
		assertEquals(BankSection.TELEPORTS, spec.sectionOf(ItemID.ECTOPHIAL));
		assertNull(spec.sectionOf(ItemID.WATERMELON_SEED));
		assertNull(spec.sectionOf(ItemID.COINS));
	}

	@Test
	public void itemListedInTwoSectionsGoesInTheFirst()
	{
		// Coconuts pay for magic trees; a coconut is also a teleport nowhere, but say it were
		BankFilterSpec spec = spec(BankSection.PAYMENTS, List.of(ItemID.COCONUT), BankSection.TELEPORTS, List.of(ItemID.COCONUT));

		assertEquals(BankSection.PAYMENTS, spec.sectionOf(ItemID.COCONUT));
	}

	@Test
	public void wearablesComeFirstAndLeaveTheirOtherSection()
	{
		BankFilterSpec spec = spec(
			BankSection.WEARABLES, List.of(ItemID.ARDY_CAPE_EASY),
			BankSection.EQUIPMENT, RunItems.EQUIPMENT,
			BankSection.TELEPORTS, List.of(ItemID.ARDY_CAPE_EASY, ItemID.ECTOPHIAL));

		BankFilterSpec.Arrangement arrangement = spec.arrange(List.of(ItemID.ECTOPHIAL, ItemID.SPADE, ItemID.ARDY_CAPE_EASY));

		assertEquals(BankSection.WEARABLES, spec.sectionOf(ItemID.ARDY_CAPE_EASY));
		assertEquals(ItemID.ARDY_CAPE_EASY, arrangement.getSlots()[8]);
		assertEquals(List.of(BankSection.WEARABLES, BankSection.EQUIPMENT, BankSection.TELEPORTS),
			List.copyOf(arrangement.getHeadingRows().keySet()));
	}

	@Test
	public void everyTreeTypeListsTheSaplingsAndSeedsOfItsCrops()
	{
		for (PatchType type : PatchType.values())
		{
			if (!type.isTree())
			{
				continue;
			}
			Set<Sapling> crops = new HashSet<>();
			for (Sapling crop : type.getCrops())
			{
				if (crop != null)
				{
					crops.add(crop);
				}
			}
			// Each crop's sapling and watered seedling can be planted; watered seedlings count as saplings
			assertEquals(type.name(), crops.size() * 2, type.getSeeds().size());
			for (Sapling crop : crops)
			{
				assertTrue(type.name(), type.getSeeds().contains(crop.getSapling()));
				assertTrue(type.name(), type.getSeeds().contains(crop.getWateredSeedling()));
				assertFalse(type.name(), type.getSaplingSeeds().contains(crop.getWateredSeedling()));
			}
			// A seed and its unwatered seedling for each crop
			assertEquals(type.name(), crops.size() * 2, Set.copyOf(type.getSaplingSeeds()).size());
		}
		assertEquals(3 + 2 + 9, Set.copyOf(RunItems.GROWING_TOOLS).size());
	}

	@Test
	public void bankItemsAreArrangedInSectionsUnderAHeadingRow()
	{
		BankFilterSpec spec = spec(
			BankSection.HERB_SEEDS, RunItems.HERB_SEEDS,
			BankSection.ALLOTMENT_SEEDS, RunItems.ALLOTMENT_SEEDS,
			BankSection.EQUIPMENT, RunItems.EQUIPMENT,
			BankSection.TELEPORTS, List.of(ItemID.ECTOPHIAL));
		// In bank order, which isn't the order they should be shown in; coins aren't part of the filter
		List<Integer> bank = List.of(ItemID.ECTOPHIAL, ItemID.TORSTOL_SEED, ItemID.COINS, ItemID.DIBBER, ItemID.GUAM_SEED, ItemID.SPADE, ItemID.POTATO_SEED);

		BankFilterSpec.Arrangement arrangement = spec.arrange(bank);

		int[] expected = new int[8 * 8];
		Arrays.fill(expected, -1);
		expected[8] = ItemID.SPADE;
		expected[9] = ItemID.DIBBER;
		expected[24] = ItemID.GUAM_SEED;
		expected[25] = ItemID.TORSTOL_SEED;
		expected[40] = ItemID.POTATO_SEED;
		expected[56] = ItemID.ECTOPHIAL;
		assertArrayEquals(expected, arrangement.getSlots());

		Map<BankSection, Integer> headings = new LinkedHashMap<>();
		headings.put(BankSection.EQUIPMENT, 0);
		headings.put(BankSection.HERB_SEEDS, 2);
		headings.put(BankSection.ALLOTMENT_SEEDS, 4);
		headings.put(BankSection.TELEPORTS, 6);
		assertEquals(headings, arrangement.getHeadingRows());
	}

	@Test
	public void sectionLongerThanARowPushesTheNextOneDown()
	{
		BankFilterSpec spec = spec(BankSection.HERB_SEEDS, RunItems.HERB_SEEDS, BankSection.TELEPORTS, List.of(ItemID.ECTOPHIAL));
		BankFilterSpec.Arrangement arrangement = spec.arrange(
			List.of(ItemID.GUAM_SEED, ItemID.MARRENTILL_SEED, ItemID.TARROMIN_SEED, ItemID.HARRALANDER_SEED, ItemID.RANARR_SEED,
				ItemID.TOADFLAX_SEED, ItemID.IRIT_SEED, ItemID.AVANTOE_SEED, ItemID.KWUARM_SEED, ItemID.ECTOPHIAL));

		assertEquals(Integer.valueOf(0), arrangement.getHeadingRows().get(BankSection.HERB_SEEDS));
		// Nine herb seeds take two rows, after the heading row
		assertEquals(Integer.valueOf(3), arrangement.getHeadingRows().get(BankSection.TELEPORTS));
		assertEquals(ItemID.KWUARM_SEED, arrangement.getSlots()[16]);
		assertEquals(ItemID.ECTOPHIAL, arrangement.getSlots()[32]);
	}

	@Test
	public void emptyBankGivesAnEmptyLayout()
	{
		BankFilterSpec.Arrangement arrangement = spec(BankSection.HERB_SEEDS, RunItems.HERB_SEEDS).arrange(List.of(ItemID.COINS));

		assertEquals(0, arrangement.getSlots().length);
		assertTrue(arrangement.getHeadingRows().isEmpty());
	}

	private static Route route(RunPreset preset, Map<FarmLocation, List<Map<Integer, Integer>>> stops, boolean carriedOnly,
		boolean house, boolean spells)
	{
		return new Route(preset, stops, carriedOnly, house, spells, false, Set.of());
	}

	@Test
	public void pathMessageTargetsPlainPointsWithoutSeedRequirements()
	{
		Map<FarmLocation, List<Map<Integer, Integer>>> stops = new LinkedHashMap<>();
		stops.put(FarmLocation.FALADOR, List.of());
		stops.put(FarmLocation.WEISS, List.of());

		Map<String, Object> data = ShortestPathRouter.pathMessage(route(Presets.HERBS, stops, false, false, false));

		assertEquals(List.of(new WorldPoint(3058, 3311, 0), new WorldPoint(2848, 3934, 0)), data.get("target"));
		assertNull(data.get("config"));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void pathMessagePassesEachStopsSeedRequirements()
	{
		Map<FarmLocation, List<Map<Integer, Integer>>> stops = new LinkedHashMap<>();
		stops.put(FarmLocation.FARMING_GUILD, List.of(Map.of(ItemID.IRIT_SEED, 1)));
		stops.put(FarmLocation.WEISS, List.of());

		List<Object> targets = (List<Object>) ShortestPathRouter.pathMessage(route(Presets.HERBS, stops, false, false, false)).get("target");

		assertEquals(2, targets.size());
		Map<String, Object> guild = (Map<String, Object>) targets.get(0);
		assertEquals(FarmLocation.FARMING_GUILD.patches(PatchType.HERB).get(0).getPoint(), guild.get("point"));
		assertEquals(List.of(Map.of(ItemID.IRIT_SEED, 1)), guild.get("requirements"));
		assertEquals(FarmLocation.WEISS.patches(PatchType.HERB).get(0).getPoint(), targets.get(1));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void treeRunGoesToTheGuildsTreeRatherThanItsHerbPatch()
	{
		Map<FarmLocation, List<Map<Integer, Integer>>> stops = Map.of(FarmLocation.FARMING_GUILD, List.of());

		List<Object> targets = (List<Object>) ShortestPathRouter.pathMessage(route(Presets.TREES, stops, false, false, false)).get("target");

		assertEquals(List.of(new WorldPoint(1232, 3736, 0)), targets);
	}

	@Test
	public void shortestPathsAnswerIsMatchedToTheStopItLeadsTo()
	{
		Map<FarmLocation, List<Map<Integer, Integer>>> stops = new LinkedHashMap<>();
		stops.put(FarmLocation.LUMBRIDGE, List.of());
		stops.put(FarmLocation.FARMING_GUILD, List.of());
		Route route = route(Presets.TREES, stops, false, false, false);

		assertEquals(FarmLocation.FARMING_GUILD, ShortestPathRouter.stopAt(route, new WorldPoint(1232, 3736, 0)));
		// A tile beside the patch, when the patch itself can't be stood on
		assertEquals(FarmLocation.LUMBRIDGE, ShortestPathRouter.stopAt(route, new WorldPoint(3194, 3232, 0)));
		assertNull(ShortestPathRouter.stopAt(route, new WorldPoint(3000, 3000, 0)));
	}

	@Test
	public void pathMessageOnlyOverridesTheShortestPathSettingsItNeeds()
	{
		Map<FarmLocation, List<Map<Integer, Integer>>> stops = Map.of(FarmLocation.FALADOR, List.of());

		assertEquals(Map.of("useTeleportationItems", "Inventory"),
			ShortestPathRouter.pathMessage(route(Presets.HERBS, stops, true, false, false)).get("config"));
		assertEquals(Map.of("usePoh", true, "useTeleportationSpells", true),
			ShortestPathRouter.pathMessage(route(Presets.HERBS, stops, false, true, true)).get("config"));
	}

	@Test
	public void contractVarbitValuesMatchTheirCrops()
	{
		assertEquals(FarmingContract.IRIT, FarmingContract.fromVarbit(26));
		assertEquals(FarmingContract.WATERMELON, FarmingContract.fromVarbit(46));
		assertEquals(FarmingContract.MAGIC, FarmingContract.fromVarbit(11));
		assertEquals(FarmingContract.DRAGONFRUIT, FarmingContract.fromVarbit(19));
		// A bush contract isn't one a run grows
		assertNull(FarmingContract.fromVarbit(1));
		for (FarmingContract contract : FarmingContract.values())
		{
			assertTrue(contract.getType().getSeeds().contains(contract.getSeed()));
		}
	}

	@Test
	public void everyPatchHasItsOwnTimeTrackingKey()
	{
		Set<String> keys = new HashSet<>();
		for (FarmLocation location : FarmLocation.values())
		{
			for (FarmPatch patch : location.getPatches())
			{
				assertTrue(patch.toString(), keys.add(patch.timeTrackingKey()));
			}
		}
		// Herb runs: seven locations with two allotments and a flower patch, Harmony with one allotment,
		// and two with a herb patch alone. Tree runs: seven trees and seven fruit trees. Special trees:
		// four hardwood, three calquat, five spirit tree, and a celastrus, redwood and crystal tree patch.
		assertEquals(7 * 4 + 2 + 2 + 7 + 7 + 4 + 3 + 5 + 3, keys.size());
	}

	@Test
	public void eachRunHasItsLocations()
	{
		assertEquals(10, Presets.HERBS.locations().size());
		// The guild and the Gnome Stronghold each have a tree and a fruit tree
		assertEquals(12, Presets.TREES.locations().size());
		assertTrue(Presets.TREES.locations().contains(FarmLocation.FARMING_GUILD));
		assertFalse(Presets.HERBS.locations().contains(FarmLocation.LUMBRIDGE));
		for (FarmLocation location : Presets.TREES.locations())
		{
			for (FarmPatch tree : Presets.TREES.anchors(location))
			{
				assertTrue(tree.toString(), tree.getGardener() > 0);
			}
		}
	}

	@Test
	public void teleportChoicesAreAnyThenTheLocationsOwnThenTheHouseThenNone()
	{
		for (FarmLocation location : FarmLocation.values())
		{
			List<Teleport> choices = location.teleportChoices();
			assertEquals(Teleport.ANY, choices.get(0));
			assertEquals(Teleport.NONE, choices.get(choices.size() - 1));
			assertEquals(location.getTeleports(), choices.subList(1, 1 + location.getTeleports().size()));
			assertEquals(Teleport.HOUSE, choices.get(choices.size() - 2));
			assertEquals(choices.size(), new HashSet<>(choices).size());
		}
	}

	@Test
	public void anyTeleportMeansTheLocationsItemsButNotItsSpells()
	{
		// Items, a spell's tablet among them, but not a fairy ring, which may not need anything
		List<Teleport> anyItem = List.of(Teleport.ECTOPHIAL, Teleport.FENKENSTRAINS_CASTLE_TELEPORT, Teleport.KHARYRLL_TELEPORT);
		assertEquals(anyItem, FarmLocation.MORYTANIA.teleportsFor(Teleport.ANY));
		assertTrue(FarmLocation.MORYTANIA.itemsOnly(Teleport.ANY));
		assertEquals(List.of(Teleport.FENKENSTRAINS_CASTLE_TELEPORT), FarmLocation.MORYTANIA.teleportsFor(Teleport.FENKENSTRAINS_CASTLE_TELEPORT));
		assertFalse(FarmLocation.MORYTANIA.itemsOnly(Teleport.FENKENSTRAINS_CASTLE_TELEPORT));
		assertEquals(List.of(Teleport.HOUSE), FarmLocation.MORYTANIA.teleportsFor(Teleport.HOUSE));
		assertEquals(List.of(), FarmLocation.MORYTANIA.teleportsFor(Teleport.NONE));
		// A teleport that doesn't go here counts as no preference
		assertEquals(anyItem, FarmLocation.MORYTANIA.teleportsFor(Teleport.ICY_BASALT));
	}

	@Test
	public void recommendedOrderCoversEachRunsLocationsOnce()
	{
		for (RunPreset preset : List.of(Presets.HERBS, Presets.TREES))
		{
			List<FarmLocation> order = preset.recommendedOrder();
			assertEquals(order.size(), new HashSet<>(order).size());
			assertEquals(FarmLocation.values().length, order.size());
		}
		// Each run starts where its guide does
		assertEquals(FarmLocation.FALADOR, Presets.HERBS.locations().get(0));
		assertEquals(FarmLocation.GNOME_STRONGHOLD, Presets.TREES.locations().get(0));
	}

	@Test
	public void stopsFollowTheRecommendedOrderWithTheOneThePlayerIsAtFirst()
	{
		Set<FarmLocation> stops = Set.of(FarmLocation.HARMONY, FarmLocation.CATHERBY, FarmLocation.FALADOR, FarmLocation.HOSIDIUS);

		assertEquals(List.of(FarmLocation.FALADOR, FarmLocation.CATHERBY, FarmLocation.HOSIDIUS, FarmLocation.HARMONY),
			PlugAndPlantPlugin.orderedStops(Presets.HERBS, stops, null));
		// Standing at the Hosidius herb patch
		assertEquals(List.of(FarmLocation.HOSIDIUS, FarmLocation.FALADOR, FarmLocation.CATHERBY, FarmLocation.HARMONY),
			PlugAndPlantPlugin.orderedStops(Presets.HERBS, stops, new WorldPoint(1738, 3550, 0)));
	}

	@Test
	public void brimhavenCanBeReachedThroughArdougne()
	{
		assertTrue(FarmLocation.BRIMHAVEN.getTeleports().contains(Teleport.ARDOUGNE_TELEPORT));
	}

	@Test
	public void spellShowsItsRunesAndRunePouchesInTheBank()
	{
		Collection<Integer> items = Teleport.FALADOR_TELEPORT.bankItemIds();
		assertTrue(items.containsAll(Set.of(ItemID.POH_TABLET_FALADORTELEPORT, ItemID.LAWRUNE, ItemID.AIRRUNE, ItemID.WATERRUNE,
			ItemID.BH_RUNE_POUCH)));
		assertFalse(items.contains(ItemID.FIRERUNE));
		assertEquals(Set.of(ItemID.ECTOPHIAL, ItemID.ECTOPHIAL_EMPTY), Teleport.ECTOPHIAL.bankItemIds());
	}

	@Test
	public void choicesSavedByEarlierVersionsAreKept()
	{
		assertEquals(Teleport.FALADOR_TELEPORT, Teleport.fromSaved("FALADOR_TAB"));
		assertEquals(Teleport.FALADOR_TELEPORT, Teleport.fromSaved("FALADOR_SPELL"));
		assertEquals(Teleport.HOUSE, Teleport.fromSaved("HOUSE_CAPE"));
		assertEquals(Teleport.ECTOPHIAL, Teleport.fromSaved("ECTOPHIAL"));
		assertNull(Teleport.fromSaved("SOMETHING_NEW"));
	}

	@Test
	public void everyLocationHasAWayThere()
	{
		for (FarmLocation location : FarmLocation.values())
		{
			assertFalse(location + " has no teleports", location.getTeleports().isEmpty());
		}
	}
}
