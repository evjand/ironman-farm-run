package com.plugandplant;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.runelite.api.gameval.ItemID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Test;

public class ChecklistTest
{
	private static final Checklist.Options PLAIN = new Checklist.Options(RunType.HERB, false, false, false, false, false, List.of());

	private final Map<FarmPatch, PatchState> states = new HashMap<>();
	private final Map<FarmLocation, LocationSettings> settings = new EnumMap<>(FarmLocation.class);
	private final Map<Integer, Integer> items = new HashMap<>();
	private final Map<Integer, Integer> pouchRunes = new HashMap<>();
	private final Map<Integer, Integer> stored = new HashMap<>();
	private final Set<Integer> unlimitedRunes = new HashSet<>();
	private boolean seedBoxUnknown;
	private final Set<FarmPatch> justPlanted = new HashSet<>();

	private void ready(FarmPatch patch)
	{
		ready(patch, PatchStatus.READY);
	}

	private void ready(FarmPatch patch, PatchStatus status)
	{
		states.put(patch, new PatchState(status, 0));
	}

	private List<ChecklistItem> build(Checklist.Options options)
	{
		return build(options, null);
	}

	private List<ChecklistItem> build(Checklist.Options options, FarmingContract contract)
	{
		RunType runType = options.getRunType();
		List<FarmLocation> stops = new ArrayList<>();
		for (FarmLocation location : FarmLocation.of(runType))
		{
			if (location.anchors(runType).stream()
				.anyMatch(patch -> states.getOrDefault(patch, PatchState.UNKNOWN).getStatus().needsVisit()))
			{
				stops.add(location);
			}
		}
		Carried carried = new Carried(items, pouchRunes, unlimitedRunes, stored, seedBoxUnknown);
		RunPlan plan = RunPlan.of(stops, states, Set.of(), Set.of(), justPlanted,
			new RunPlan.Options(runType, options.isAllotments(), options.isFlowers(), options.isCompost(), options.isProtect(), 99),
			carried, contract);
		return Checklist.build(plan, states, settings, carried, options, contract);
	}

	private static ChecklistItem find(List<ChecklistItem> checklist, String label)
	{
		return checklist.stream().filter(item -> item.getLabel().equals(label)).findFirst().orElse(null);
	}

	@Test
	public void seedsOfAKindAreCountedTogether()
	{
		ready(FarmLocation.FALADOR.patches(PatchType.HERB).get(0));
		ready(FarmLocation.CATHERBY.patches(PatchType.HERB).get(0));
		ready(FarmLocation.ARDOUGNE.patches(PatchType.HERB).get(0));
		items.put(ItemID.RANARR_SEED, 1);
		items.put(ItemID.TORSTOL_SEED, 1);

		assertEquals(new ChecklistItem("Herb seeds", "2 / 3", ChecklistItem.State.MISSING), find(build(PLAIN), "Herb seeds"));

		items.put(ItemID.GUAM_SEED, 5);
		assertEquals(new ChecklistItem("Herb seeds", "7 / 3", ChecklistItem.State.GOOD), find(build(PLAIN), "Herb seeds"));
		assertNull(find(build(PLAIN), "Allotment seeds"));
	}

	@Test
	public void seedBoxNotYetLookedInMakesAShortfallUnsure()
	{
		ready(FarmLocation.FALADOR.patches(PatchType.HERB).get(0));
		items.put(ItemID.SEED_BOX_OPEN, 1);
		seedBoxUnknown = true;

		assertEquals(new ChecklistItem("Herb seeds", "0 / 1 + box", ChecklistItem.State.UNSURE), find(build(PLAIN), "Herb seeds"));

		// Once its contents are known they are counted like any other seeds
		seedBoxUnknown = false;
		assertEquals(new ChecklistItem("Herb seeds", "0 / 1", ChecklistItem.State.MISSING), find(build(PLAIN), "Herb seeds"));
	}

	@Test
	public void allotmentAndFlowerSeedsShowHowManyPatchesTheyPlant()
	{
		Checklist.Options both = new Checklist.Options(RunType.HERB, true, true, false, false, false, List.of());
		ready(FarmLocation.FALADOR.patches(PatchType.HERB).get(0));
		items.put(ItemID.WATERMELON_SEED, 7);
		items.put(ItemID.POTATO_SEED, 3);

		List<ChecklistItem> checklist = build(both);
		assertEquals(new ChecklistItem("Allotment seeds", "3 patches", ChecklistItem.State.GOOD), find(checklist, "Allotment seeds"));
		assertEquals(new ChecklistItem("Flower seeds", "None", ChecklistItem.State.UNSURE), find(checklist, "Flower seeds"));
		assertNull(find(build(PLAIN), "Allotment seeds"));
	}

	@Test
	public void contractSeedIsAskedForWhenTheGuildIsAStop()
	{
		ready(FarmLocation.FARMING_GUILD.patches(PatchType.HERB).get(0));
		items.put(ItemID.RANARR_SEED, 1);

		assertEquals(new ChecklistItem("Contract: Irit seed", "Missing", ChecklistItem.State.MISSING),
			find(build(PLAIN, FarmingContract.IRIT), "Contract: Irit seed"));

		items.put(ItemID.IRIT_SEED, 1);
		assertEquals(ChecklistItem.State.GOOD, find(build(PLAIN, FarmingContract.IRIT), "Contract: Irit seed").getState());
	}

	@Test
	public void contractIsLeftOffWhenTheGuildIsNotAStop()
	{
		ready(FarmLocation.FALADOR.patches(PatchType.HERB).get(0));

		assertNull(find(build(PLAIN, FarmingContract.IRIT), "Contract: Irit seed"));
	}

	@Test
	public void compostIsCountedPerPatchUnlessBottomless()
	{
		ready(FarmLocation.FALADOR.patches(PatchType.HERB).get(0));
		ready(FarmLocation.CATHERBY.patches(PatchType.HERB).get(0));
		Checklist.Options withCompost = new Checklist.Options(RunType.HERB, false, false, true, false, false, List.of());
		items.put(ItemID.BUCKET_ULTRACOMPOST, 1);

		assertEquals(new ChecklistItem("Compost", "1 / 2", ChecklistItem.State.UNSURE), find(build(withCompost), "Compost"));
		assertNull(find(build(PLAIN), "Compost"));

		items.put(ItemID.BOTTOMLESS_COMPOST_BUCKET_FILLED, 1);
		assertEquals(new ChecklistItem("Compost", "Bottomless", ChecklistItem.State.GOOD), find(build(withCompost), "Compost"));
	}

	@Test
	public void toolsAreCarriedAtTheLeprechaunOrMissing()
	{
		ready(FarmLocation.FALADOR.patches(PatchType.HERB).get(0));
		items.put(ItemID.SPADE, 1);
		stored.put(ItemID.DIBBER, 1);

		List<ChecklistItem> checklist = build(PLAIN);
		assertEquals(ChecklistItem.State.GOOD, find(checklist, "Spade").getState());
		assertEquals(new ChecklistItem("Seed dibber", "At leprechaun", ChecklistItem.State.STORED), find(checklist, "Seed dibber"));
		// Magic secateurs only add to the yield, so they aren't missing
		assertEquals(ChecklistItem.State.UNSURE, find(checklist, "Magic secateurs").getState());

		stored.clear();
		assertEquals(ChecklistItem.State.MISSING, find(build(PLAIN), "Seed dibber").getState());
		// Only asked for when a patch needs it
		assertNull(find(checklist, "Rake"));
		assertNull(find(checklist, "Plant cure"));
	}

	@Test
	public void particularTeleportIsMissingUntilCarried()
	{
		ready(FarmLocation.MORYTANIA.patches(PatchType.HERB).get(0));
		settings.put(FarmLocation.MORYTANIA, new LocationSettings(true, Teleport.ECTOPHIAL));

		assertEquals(new ChecklistItem("Morytania: Ectophial", "Missing", ChecklistItem.State.MISSING),
			find(build(PLAIN), "Morytania: Ectophial"));

		items.put(ItemID.ECTOPHIAL_EMPTY, 1);
		assertEquals(ChecklistItem.State.GOOD, find(build(PLAIN), "Morytania: Ectophial").getState());
	}

	@Test
	public void anyTeleportIsSatisfiedByAnyOfTheLocationsItems()
	{
		ready(FarmLocation.MORYTANIA.patches(PatchType.HERB).get(0));

		assertEquals(new ChecklistItem("Morytania teleport", "None", ChecklistItem.State.UNSURE), find(build(PLAIN), "Morytania teleport"));

		items.put(ItemID.TELETAB_FENK, 2);
		assertEquals(ChecklistItem.State.GOOD, find(build(PLAIN), "Morytania teleport").getState());
	}

	@Test
	public void noTeleportLeavesTheLocationOffTheChecklist()
	{
		ready(FarmLocation.MORYTANIA.patches(PatchType.HERB).get(0));
		settings.put(FarmLocation.MORYTANIA, new LocationSettings(true, Teleport.NONE));

		assertNull(find(build(PLAIN), "Morytania teleport"));
	}

	@Test
	public void spellRunesAreFoundInTheInventoryAndRunePouch()
	{
		ready(FarmLocation.FALADOR.patches(PatchType.HERB).get(0));
		settings.put(FarmLocation.FALADOR, new LocationSettings(true, Teleport.FALADOR_SPELL));

		// Falador Teleport takes a law, three airs and a water
		items.put(ItemID.LAWRUNE, 10);
		items.put(ItemID.AIRRUNE, 2);
		assertEquals(new ChecklistItem("Falador: Falador spell", "Runes not found", ChecklistItem.State.UNSURE),
			find(build(PLAIN), "Falador: Falador spell"));

		pouchRunes.put(ItemID.AIRRUNE, 1);
		pouchRunes.put(ItemID.WATERRUNE, 500);
		assertEquals(ChecklistItem.State.GOOD, find(build(PLAIN), "Falador: Falador spell").getState());
	}

	@Test
	public void houseTeleportIsOfferedEverywhere()
	{
		ready(FarmLocation.WEISS.patches(PatchType.HERB).get(0));
		settings.put(FarmLocation.WEISS, new LocationSettings(true, Teleport.HOUSE_TAB));
		items.put(ItemID.POH_TABLET_TELEPORTTOHOUSE, 3);

		assertEquals(new ChecklistItem("Weiss: House tab", "Yes", ChecklistItem.State.GOOD), find(build(PLAIN), "Weiss: House tab"));
	}

	@Test
	public void gracefulAndVolcanicAshAreOptional()
	{
		ready(FarmLocation.FALADOR.patches(PatchType.HERB).get(0));
		assertNull(find(build(PLAIN), "Graceful"));
		assertNull(find(build(PLAIN), "Volcanic ash"));

		// Two recolours of the hood count as one piece
		List<List<Integer>> pieces = List.of(List.of(ItemID.GRACEFUL_HOOD, 13579), List.of(ItemID.GRACEFUL_TOP));
		Checklist.Options options = new Checklist.Options(RunType.HERB, false, false, false, false, true, pieces);
		items.put(13579, 1);
		items.put(ItemID.FOSSIL_VOLCANIC_ASH, 40);

		List<ChecklistItem> checklist = build(options);
		assertEquals(new ChecklistItem("Graceful", "1 / 2", ChecklistItem.State.MISSING), find(checklist, "Graceful"));
		assertEquals(new ChecklistItem("Volcanic ash", "40", ChecklistItem.State.GOOD), find(checklist, "Volcanic ash"));
	}

	private static final Checklist.Options TREES = new Checklist.Options(RunType.TREE, false, false, false, true, false, List.of());

	@Test
	public void treeRunAsksForSaplingsOfBothKinds()
	{
		ready(FarmLocation.LUMBRIDGE.patches(PatchType.TREE).get(0), PatchStatus.CHECK_HEALTH);
		ready(FarmLocation.GNOME_STRONGHOLD.patches(PatchType.TREE).get(0), PatchStatus.STUMP);
		ready(FarmLocation.GNOME_STRONGHOLD.patches(PatchType.FRUIT_TREE).get(0), PatchStatus.GROWN);
		items.put(ItemID.PLANTPOT_YEW_SAPLING, 2);

		List<ChecklistItem> checklist = build(TREES);
		assertEquals(new ChecklistItem("Tree saplings", "2 / 2", ChecklistItem.State.GOOD), find(checklist, "Tree saplings"));
		assertEquals(new ChecklistItem("Fruit tree saplings", "0 / 1", ChecklistItem.State.MISSING), find(checklist, "Fruit tree saplings"));
		// A health check and a grown tree each take a removal; a stump is dug up
		assertEquals(new ChecklistItem("Coins to remove trees", "0 / 400", ChecklistItem.State.UNSURE), find(checklist, "Coins to remove trees"));
		assertNull(find(checklist, "Seed dibber"));
		assertNull(find(checklist, "Herb seeds"));
	}

	@Test
	public void paymentsFollowTheSaplingsCarried()
	{
		ready(FarmLocation.LUMBRIDGE.patches(PatchType.TREE).get(0), PatchStatus.CHECK_HEALTH);
		ready(FarmLocation.VARROCK.patches(PatchType.TREE).get(0), PatchStatus.CHECK_HEALTH);
		ready(FarmLocation.CATHERBY_ORCHARD.patches(PatchType.FRUIT_TREE).get(0), PatchStatus.CHECK_HEALTH);
		ready(FarmLocation.BRIMHAVEN.patches(PatchType.FRUIT_TREE).get(0), PatchStatus.CHECK_HEALTH);
		// A magic and a yew tree; one dragonfruit tree, which shares coconuts with magic, and one without a sapling
		items.put(ItemID.PLANTPOT_MAGIC_TREE_SAPLING, 1);
		items.put(ItemID.PLANTPOT_YEW_SAPLING, 1);
		items.put(ItemID.PLANTPOT_DRAGONFRUIT_SAPLING, 1);
		items.put(ItemID.COCONUT, 30);

		List<ChecklistItem> checklist = build(TREES);
		assertEquals(new ChecklistItem("Pay: coconuts", "30 / 40", ChecklistItem.State.MISSING), find(checklist, "Pay: coconuts"));
		assertEquals(new ChecklistItem("Pay: cactus spines", "0 / 10", ChecklistItem.State.MISSING), find(checklist, "Pay: cactus spines"));
		assertEquals(new ChecklistItem("Pay: for 1 more tree", "Bring saplings", ChecklistItem.State.UNSURE),
			find(checklist, "Pay: for 1 more tree"));
	}

	@Test
	public void treePlantedOnTheRunIsPaidForWhatIsGrowing()
	{
		// Planted at the stop the player is at, so it was a stop when the run started
		FarmPatch tree = FarmLocation.LUMBRIDGE.patches(PatchType.TREE).get(0);
		states.put(tree, new PatchState(PatchStatus.GROWING, 123, Sapling.MAPLE));
		justPlanted.add(tree);
		Carried carried = new Carried(items, pouchRunes);
		RunPlan plan = RunPlan.of(List.of(FarmLocation.LUMBRIDGE), states, Set.of(tree), Set.of(), justPlanted,
			new RunPlan.Options(RunType.TREE, false, false, false, true, 99), carried, null);

		List<ChecklistItem> checklist = Checklist.build(plan, states, settings, carried, TREES, null);
		assertEquals(new ChecklistItem("Pay: basket of oranges", "0 / 1", ChecklistItem.State.MISSING),
			find(checklist, "Pay: basket of oranges"));
	}

	@Test
	public void compostAtTheLeprechaunMakesUpTheShortfall()
	{
		ready(FarmLocation.FALADOR.patches(PatchType.HERB).get(0));
		ready(FarmLocation.CATHERBY.patches(PatchType.HERB).get(0));
		ready(FarmLocation.ARDOUGNE.patches(PatchType.HERB).get(0));
		Checklist.Options withCompost = new Checklist.Options(RunType.HERB, false, false, true, false, false, List.of());
		items.put(ItemID.BUCKET_ULTRACOMPOST, 1);
		stored.put(ItemID.BUCKET_SUPERCOMPOST, 5);

		assertEquals(new ChecklistItem("Compost", "1 / 3, 5 at leprechaun", ChecklistItem.State.STORED), find(build(withCompost), "Compost"));

		stored.clear();
		stored.put(ItemID.BOTTOMLESS_COMPOST_BUCKET_FILLED, 1);
		assertEquals(new ChecklistItem("Compost", "Bottomless at leprechaun", ChecklistItem.State.STORED), find(build(withCompost), "Compost"));
	}

	@Test
	public void staffAndCombinationRunesCountTowardsASpell()
	{
		ready(FarmLocation.FALADOR.patches(PatchType.HERB).get(0));
		settings.put(FarmLocation.FALADOR, new LocationSettings(true, Teleport.FALADOR_SPELL));
		// Falador Teleport takes a law, three airs and a water
		items.put(ItemID.LAWRUNE, 1);
		items.put(ItemID.WATERRUNE, 1);
		assertEquals(ChecklistItem.State.UNSURE, find(build(PLAIN), "Falador: Falador spell").getState());

		unlimitedRunes.add(ItemID.AIRRUNE);
		assertEquals(ChecklistItem.State.GOOD, find(build(PLAIN), "Falador: Falador spell").getState());

		// Mist runes count as both air and water
		unlimitedRunes.clear();
		items.remove(ItemID.WATERRUNE);
		pouchRunes.put(ItemID.MISTRUNE, 3);
		assertEquals(ChecklistItem.State.GOOD, find(build(PLAIN), "Falador: Falador spell").getState());
	}

	@Test
	public void staffVariantsAndTomesSupplyTheirRunes()
	{
		assertEquals(Set.of(ItemID.AIRRUNE), Runes.suppliedBy(ItemID.STAFF_OF_AIR));
		assertEquals(Set.of(ItemID.AIRRUNE, ItemID.WATERRUNE), Runes.suppliedBy(ItemID.MYSTIC_MIST_BATTLESTAFF));
		assertEquals(Set.of(ItemID.FIRERUNE), Runes.suppliedBy(ItemID.TOME_OF_FIRE));
		assertEquals(Set.of(), Runes.suppliedBy(ItemID.TOME_OF_FIRE_UNCHARGED));
		assertEquals(Set.of(), Runes.suppliedBy(ItemID.SPADE));
	}
}
