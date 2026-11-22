package com.ironmanfarmrun;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.runelite.api.gameval.ItemID;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class BankAndRouteTest
{
	@Test
	public void bankFilterOnlyShowsTheSeedsOfPatchesThePlayerTends()
	{
		BankFilterSpec herbsOnly = BankFilterSpec.HERBS_ONLY;
		assertEquals(BankSection.HERB_SEEDS, herbsOnly.sectionOf(ItemID.RANARR_SEED));
		assertNull(herbsOnly.sectionOf(ItemID.WATERMELON_SEED));
		assertNull(herbsOnly.sectionOf(ItemID.LIMPWURT_SEED));
		assertEquals(BankSection.EQUIPMENT, herbsOnly.sectionOf(ItemID.SPADE));
		assertNull(herbsOnly.sectionOf(ItemID.ECTOPHIAL));
		assertNull(herbsOnly.sectionOf(ItemID.FOSSIL_VOLCANIC_ASH));

		BankFilterSpec everything = new BankFilterSpec(true, true, -1, List.of(ItemID.FOSSIL_VOLCANIC_ASH), List.of(ItemID.ECTOPHIAL));
		assertEquals(BankSection.ALLOTMENT_SEEDS, everything.sectionOf(ItemID.WATERMELON_SEED));
		assertEquals(BankSection.FLOWER_SEEDS, everything.sectionOf(ItemID.LIMPWURT_SEED));
		assertEquals(BankSection.TELEPORTS, everything.sectionOf(ItemID.ECTOPHIAL));
		assertEquals(BankSection.EQUIPMENT, everything.sectionOf(ItemID.FOSSIL_VOLCANIC_ASH));

		// A contract's seed is shown in its section even without the rest of that section
		BankFilterSpec contract = new BankFilterSpec(false, false, ItemID.WATERMELON_SEED, List.of(), List.of());
		assertEquals(BankSection.ALLOTMENT_SEEDS, contract.sectionOf(ItemID.WATERMELON_SEED));
		assertNull(contract.sectionOf(ItemID.POTATO_SEED));
	}

	@Test
	public void bankItemsAreArrangedInSectionsUnderAHeadingRow()
	{
		BankFilterSpec spec = new BankFilterSpec(true, false, -1, List.of(), List.of(ItemID.ECTOPHIAL));
		// In bank order, which isn't the order they should be shown in; coins aren't part of the filter
		List<Integer> bank = List.of(ItemID.ECTOPHIAL, ItemID.TORSTOL_SEED, ItemID.COINS, ItemID.DIBBER, ItemID.GUAM_SEED, ItemID.SPADE, ItemID.POTATO_SEED);

		BankFilterSpec.Arrangement arrangement = spec.arrange(bank);

		int[] expected = new int[8 * 8];
		java.util.Arrays.fill(expected, -1);
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
		BankFilterSpec.Arrangement arrangement = BankFilterSpec.HERBS_ONLY.arrange(
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
		BankFilterSpec.Arrangement arrangement = BankFilterSpec.HERBS_ONLY.arrange(List.of(ItemID.COINS));

		assertEquals(0, arrangement.getSlots().length);
		assertTrue(arrangement.getHeadingRows().isEmpty());
	}

	private static Route route(Map<FarmLocation, Map<Integer, Integer>> stops, boolean carriedOnly, boolean house, boolean spells)
	{
		return new Route(stops, carriedOnly, house, spells, Set.of());
	}

	@Test
	public void pathMessageTargetsPlainPointsWithoutSeedRequirements()
	{
		Map<FarmLocation, Map<Integer, Integer>> stops = new LinkedHashMap<>();
		stops.put(FarmLocation.FALADOR, Map.of());
		stops.put(FarmLocation.WEISS, Map.of());

		Map<String, Object> data = ShortestPathRouter.pathMessage(route(stops, false, false, false));

		assertEquals(List.of(FarmLocation.FALADOR.getPoint(), FarmLocation.WEISS.getPoint()), data.get("target"));
		assertNull(data.get("config"));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void pathMessagePassesEachStopsSeedRequirement()
	{
		Map<FarmLocation, Map<Integer, Integer>> stops = new LinkedHashMap<>();
		stops.put(FarmLocation.FARMING_GUILD, Map.of(ItemID.IRIT_SEED, 1));
		stops.put(FarmLocation.WEISS, Map.of());

		List<Object> targets = (List<Object>) ShortestPathRouter.pathMessage(route(stops, false, false, false)).get("target");

		assertEquals(2, targets.size());
		Map<String, Object> guild = (Map<String, Object>) targets.get(0);
		assertEquals(FarmLocation.FARMING_GUILD.getPoint(), guild.get("point"));
		assertEquals(Map.of(ItemID.IRIT_SEED, 1), guild.get("requirements"));
		assertEquals(FarmLocation.WEISS.getPoint(), targets.get(1));
	}

	@Test
	public void pathMessageOnlyOverridesTheShortestPathSettingsItNeeds()
	{
		Map<FarmLocation, Map<Integer, Integer>> stops = Map.of(FarmLocation.FALADOR, Map.of());

		assertEquals(Map.of("useTeleportationItems", "Inventory"),
			ShortestPathRouter.pathMessage(route(stops, true, false, false)).get("config"));
		assertEquals(Map.of("usePoh", true, "useTeleportationSpells", true),
			ShortestPathRouter.pathMessage(route(stops, false, true, true)).get("config"));
	}

	@Test
	public void contractVarbitValuesMatchTheirCrops()
	{
		assertEquals(FarmingContract.IRIT, FarmingContract.fromVarbit(26));
		assertEquals(FarmingContract.WATERMELON, FarmingContract.fromVarbit(46));
		// A tree contract isn't one a run grows
		assertNull(FarmingContract.fromVarbit(9));
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
			assertEquals(PatchType.HERB, location.herbPatch().getType());
			for (FarmPatch patch : location.getPatches())
			{
				assertTrue(patch.toString(), keys.add(patch.timeTrackingKey()));
			}
		}
		// Seven locations with two allotments and a flower patch, Harmony with one allotment, and two with a herb patch alone
		assertEquals(7 * 4 + 2 + 2, keys.size());
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
	public void spellShowsItsRunesAndRunePouchesInTheBank()
	{
		Collection<Integer> items = Teleport.FALADOR_SPELL.bankItemIds();
		assertTrue(items.containsAll(Set.of(ItemID.LAWRUNE, ItemID.AIRRUNE, ItemID.WATERRUNE, ItemID.BH_RUNE_POUCH)));
		assertFalse(items.contains(ItemID.FIRERUNE));
		assertEquals(Set.of(ItemID.TELETAB_FENK), Teleport.FENKENSTRAIN_TAB.bankItemIds());
	}
}
