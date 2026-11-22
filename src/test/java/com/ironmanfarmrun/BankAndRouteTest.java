package com.ironmanfarmrun;

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
		expected[8] = ItemID.GUAM_SEED;
		expected[9] = ItemID.TORSTOL_SEED;
		expected[24] = ItemID.POTATO_SEED;
		expected[40] = ItemID.SPADE;
		expected[41] = ItemID.DIBBER;
		expected[56] = ItemID.ECTOPHIAL;
		assertArrayEquals(expected, arrangement.getSlots());

		Map<BankSection, Integer> headings = new LinkedHashMap<>();
		headings.put(BankSection.HERB_SEEDS, 0);
		headings.put(BankSection.ALLOTMENT_SEEDS, 2);
		headings.put(BankSection.EQUIPMENT, 4);
		headings.put(BankSection.TELEPORTS, 6);
		assertEquals(headings, arrangement.getHeadingRows());
	}

	@Test
	public void sectionLongerThanARowPushesTheNextOneDown()
	{
		BankFilterSpec spec = spec(BankSection.HERB_SEEDS, RunItems.HERB_SEEDS, BankSection.EQUIPMENT, RunItems.EQUIPMENT);
		BankFilterSpec.Arrangement arrangement = spec.arrange(
			List.of(ItemID.GUAM_SEED, ItemID.MARRENTILL_SEED, ItemID.TARROMIN_SEED, ItemID.HARRALANDER_SEED, ItemID.RANARR_SEED,
				ItemID.TOADFLAX_SEED, ItemID.IRIT_SEED, ItemID.AVANTOE_SEED, ItemID.KWUARM_SEED, ItemID.RAKE));

		assertEquals(Integer.valueOf(0), arrangement.getHeadingRows().get(BankSection.HERB_SEEDS));
		// Nine herb seeds take two rows, after the heading row
		assertEquals(Integer.valueOf(3), arrangement.getHeadingRows().get(BankSection.EQUIPMENT));
		assertEquals(ItemID.KWUARM_SEED, arrangement.getSlots()[16]);
		assertEquals(ItemID.RAKE, arrangement.getSlots()[32]);
	}

	@Test
	public void emptyBankGivesAnEmptyLayout()
	{
		BankFilterSpec.Arrangement arrangement = spec(BankSection.HERB_SEEDS, RunItems.HERB_SEEDS).arrange(List.of(ItemID.COINS));

		assertEquals(0, arrangement.getSlots().length);
		assertTrue(arrangement.getHeadingRows().isEmpty());
	}

	private static Route route(RunType runType, Map<FarmLocation, List<Map<Integer, Integer>>> stops, boolean carriedOnly,
		boolean house, boolean spells)
	{
		return new Route(runType, stops, carriedOnly, house, spells, Set.of());
	}

	@Test
	public void pathMessageTargetsPlainPointsWithoutSeedRequirements()
	{
		Map<FarmLocation, List<Map<Integer, Integer>>> stops = new LinkedHashMap<>();
		stops.put(FarmLocation.FALADOR, List.of());
		stops.put(FarmLocation.WEISS, List.of());

		Map<String, Object> data = ShortestPathRouter.pathMessage(route(RunType.HERB, stops, false, false, false));

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

		List<Object> targets = (List<Object>) ShortestPathRouter.pathMessage(route(RunType.HERB, stops, false, false, false)).get("target");

		assertEquals(2, targets.size());
		Map<String, Object> guild = (Map<String, Object>) targets.get(0);
		assertEquals(FarmLocation.FARMING_GUILD.herbPatch().getPoint(), guild.get("point"));
		assertEquals(List.of(Map.of(ItemID.IRIT_SEED, 1)), guild.get("requirements"));
		assertEquals(FarmLocation.WEISS.herbPatch().getPoint(), targets.get(1));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void treeRunGoesToTheGuildsTreeRatherThanItsHerbPatch()
	{
		Map<FarmLocation, List<Map<Integer, Integer>>> stops = Map.of(FarmLocation.FARMING_GUILD, List.of());

		List<Object> targets = (List<Object>) ShortestPathRouter.pathMessage(route(RunType.TREE, stops, false, false, false)).get("target");

		assertEquals(List.of(new WorldPoint(1232, 3736, 0)), targets);
	}

	@Test
	public void pathMessageOnlyOverridesTheShortestPathSettingsItNeeds()
	{
		Map<FarmLocation, List<Map<Integer, Integer>>> stops = Map.of(FarmLocation.FALADOR, List.of());

		assertEquals(Map.of("useTeleportationItems", "Inventory"),
			ShortestPathRouter.pathMessage(route(RunType.HERB, stops, true, false, false)).get("config"));
		assertEquals(Map.of("usePoh", true, "useTeleportationSpells", true),
			ShortestPathRouter.pathMessage(route(RunType.HERB, stops, false, true, true)).get("config"));
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
		// and two with a herb patch alone. Tree runs: seven trees and seven fruit trees.
		assertEquals(7 * 4 + 2 + 2 + 7 + 7, keys.size());
	}

	@Test
	public void eachRunHasItsLocations()
	{
		assertEquals(10, FarmLocation.of(RunType.HERB).size());
		// The guild and the Gnome Stronghold each have a tree and a fruit tree
		assertEquals(12, FarmLocation.of(RunType.TREE).size());
		assertTrue(FarmLocation.of(RunType.TREE).contains(FarmLocation.FARMING_GUILD));
		assertFalse(FarmLocation.of(RunType.HERB).contains(FarmLocation.LUMBRIDGE));
		for (FarmLocation location : FarmLocation.of(RunType.TREE))
		{
			for (FarmPatch tree : location.anchors(RunType.TREE))
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
			assertTrue(choices.containsAll(List.of(Teleport.HOUSE_TAB, Teleport.HOUSE_SPELL, Teleport.HOUSE_CAPE)));
			assertEquals(choices.size(), new HashSet<>(choices).size());
		}
	}

	@Test
	public void anyTeleportMeansTheLocationsItemsButNotItsSpells()
	{
		assertEquals(List.of(Teleport.ECTOPHIAL, Teleport.FENKENSTRAIN_TAB), FarmLocation.MORYTANIA.teleportsFor(Teleport.ANY));
		assertEquals(List.of(Teleport.FENKENSTRAIN_SPELL), FarmLocation.MORYTANIA.teleportsFor(Teleport.FENKENSTRAIN_SPELL));
		assertEquals(List.of(Teleport.HOUSE_SPELL), FarmLocation.MORYTANIA.teleportsFor(Teleport.HOUSE_SPELL));
		assertEquals(List.of(), FarmLocation.MORYTANIA.teleportsFor(Teleport.NONE));
		// A teleport that doesn't go here counts as no preference
		assertEquals(List.of(Teleport.ECTOPHIAL, Teleport.FENKENSTRAIN_TAB), FarmLocation.MORYTANIA.teleportsFor(Teleport.ICY_BASALT));
	}

	@Test
	public void brimhavenCanBeReachedThroughArdougne()
	{
		assertTrue(FarmLocation.BRIMHAVEN.getTeleports().containsAll(List.of(Teleport.ARDOUGNE_TAB, Teleport.ARDOUGNE_SPELL)));
	}

	@Test
	public void spellShowsItsRunesAndRunePouchesInTheBank()
	{
		Collection<Integer> items = Teleport.FALADOR_SPELL.bankItemIds();
		assertTrue(items.containsAll(Set.of(ItemID.LAWRUNE, ItemID.AIRRUNE, ItemID.WATERRUNE, ItemID.BH_RUNE_POUCH)));
		assertFalse(items.contains(ItemID.FIRERUNE));
		assertEquals(Set.of(ItemID.TELETAB_FENK), Teleport.FENKENSTRAIN_TAB.bankItemIds());
	}
}
