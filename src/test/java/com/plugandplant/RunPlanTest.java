package com.plugandplant;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.runelite.api.gameval.ItemID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class RunPlanTest
{
	private static final FarmPatch FALADOR_HERB = FarmLocation.FALADOR.patches(PatchType.HERB).get(0);
	private static final FarmPatch FALADOR_ALLOTMENT_1 = FarmLocation.FALADOR.patches(PatchType.ALLOTMENT).get(0);
	private static final FarmPatch FALADOR_ALLOTMENT_2 = FarmLocation.FALADOR.patches(PatchType.ALLOTMENT).get(1);
	private static final FarmPatch FALADOR_FLOWER = FarmLocation.FALADOR.patches(PatchType.FLOWER).get(0);
	private static final FarmPatch GUILD_HERB = FarmLocation.FARMING_GUILD.patches(PatchType.HERB).get(0);
	private static final FarmPatch GUILD_ALLOTMENT = FarmLocation.FARMING_GUILD.patches(PatchType.ALLOTMENT).get(0);

	private static final RunPlan.Options HERBS = new RunPlan.Options(RunType.HERB, false, false, false, false, 99);
	private static final RunPlan.Options EVERYTHING = new RunPlan.Options(RunType.HERB, true, true, false, false, 99);
	private static final RunPlan.Options COMPOST = new RunPlan.Options(RunType.HERB, false, false, true, false, 99);
	private static final RunPlan.Options TREES = new RunPlan.Options(RunType.TREE, true, true, false, false, 99);
	private static final RunPlan.Options TREES_PROTECTED = new RunPlan.Options(RunType.TREE, false, false, true, true, 99);

	private static final FarmPatch LUMBRIDGE_TREE = FarmLocation.LUMBRIDGE.patches(PatchType.TREE).get(0);
	private static final FarmPatch GUILD_TREE = FarmLocation.FARMING_GUILD.patches(PatchType.TREE).get(0);
	private static final FarmPatch GUILD_FRUIT_TREE = FarmLocation.FARMING_GUILD.patches(PatchType.FRUIT_TREE).get(0);

	private final Map<FarmPatch, PatchState> states = new HashMap<>();
	private final Map<Integer, Integer> carried = new HashMap<>();
	private boolean seedBoxUnknown;

	private void set(FarmPatch patch, PatchStatus status)
	{
		states.put(patch, new PatchState(status, status == PatchStatus.GROWING ? 123 : 0));
	}

	private RunPlan plan(List<FarmLocation> stops, RunPlan.Options options)
	{
		return plan(stops, options, Set.of(), Set.of(), null);
	}

	private RunPlan plan(List<FarmLocation> stops, RunPlan.Options options, Set<FarmPatch> composted,
		Set<FarmPatch> justPlanted, FarmingContract contract)
	{
		return plan(stops, options, composted, Set.of(), justPlanted, contract);
	}

	private RunPlan plan(List<FarmLocation> stops, RunPlan.Options options, Set<FarmPatch> composted,
		Set<FarmPatch> protectedTrees, Set<FarmPatch> justPlanted, FarmingContract contract)
	{
		return RunPlan.of(stops, states, composted, protectedTrees, justPlanted, options,
			new Carried(carried, Map.of(), Set.of(), Map.of(), seedBoxUnknown), contract);
	}

	@Test
	public void eachStatusHasItsNextStep()
	{
		set(FarmLocation.FALADOR.patches(PatchType.HERB).get(0), PatchStatus.READY);
		set(FarmLocation.CATHERBY.patches(PatchType.HERB).get(0), PatchStatus.DEAD);
		set(FarmLocation.ARDOUGNE.patches(PatchType.HERB).get(0), PatchStatus.DISEASED);
		set(FarmLocation.MORYTANIA.patches(PatchType.HERB).get(0), PatchStatus.WEEDS);
		set(FarmLocation.HOSIDIUS.patches(PatchType.HERB).get(0), PatchStatus.EMPTY);
		set(FarmLocation.FARMING_GUILD.patches(PatchType.HERB).get(0), PatchStatus.GROWING);

		RunPlan plan = plan(FarmLocation.of(RunType.HERB), HERBS);

		assertEquals(Step.HARVEST, plan.getSteps().get(FarmLocation.FALADOR.patches(PatchType.HERB).get(0)));
		assertEquals(Step.CLEAR, plan.getSteps().get(FarmLocation.CATHERBY.patches(PatchType.HERB).get(0)));
		assertEquals(Step.CURE, plan.getSteps().get(FarmLocation.ARDOUGNE.patches(PatchType.HERB).get(0)));
		assertEquals(Step.RAKE, plan.getSteps().get(FarmLocation.MORYTANIA.patches(PatchType.HERB).get(0)));
		assertEquals(Step.PLANT, plan.getSteps().get(FarmLocation.HOSIDIUS.patches(PatchType.HERB).get(0)));
		assertNull(plan.getSteps().get(FarmLocation.FARMING_GUILD.patches(PatchType.HERB).get(0)));
		assertNull(plan.getSteps().get(FarmLocation.WEISS.patches(PatchType.HERB).get(0)));
	}

	@Test
	public void onlyTheGivenStopsArePlanned()
	{
		set(FALADOR_HERB, PatchStatus.READY);
		set(FarmLocation.CATHERBY.patches(PatchType.HERB).get(0), PatchStatus.READY);

		assertEquals(EnumSet.of(FarmLocation.CATHERBY), plan(List.of(FarmLocation.CATHERBY), HERBS).targets());
	}

	@Test
	public void herbPatchIsPlannedEvenWithoutSeeds()
	{
		set(FALADOR_HERB, PatchStatus.READY);
		set(FarmLocation.ARDOUGNE.patches(PatchType.HERB).get(0), PatchStatus.DISEASED);

		RunPlan plan = plan(List.of(FarmLocation.FALADOR, FarmLocation.ARDOUGNE), HERBS);

		assertEquals(Step.HARVEST, plan.getSteps().get(FALADOR_HERB));
		// A cured crop keeps growing, so it takes no seed
		assertEquals(1, plan.seedsNeeded(PatchType.HERB));
	}

	@Test
	public void allotmentsAndFlowersAreTendedOnlyWhileSeedsLast()
	{
		for (FarmPatch patch : FarmLocation.FALADOR.getPatches())
		{
			set(patch, PatchStatus.READY);
		}

		assertEquals(Set.of(FALADOR_HERB), plan(List.of(FarmLocation.FALADOR), EVERYTHING).getSteps().keySet());

		// Three of a kind plant one allotment; two odd seeds plant nothing
		carried.put(ItemID.POTATO_SEED, 4);
		carried.put(ItemID.ONION_SEED, 2);
		carried.put(ItemID.MARIGOLD_SEED, 1);
		RunPlan plan = plan(List.of(FarmLocation.FALADOR), EVERYTHING);
		assertEquals(Set.of(FALADOR_HERB, FALADOR_ALLOTMENT_1, FALADOR_FLOWER), plan.getSteps().keySet());
		assertEquals(Set.of(FALADOR_ALLOTMENT_2), plan.getWithoutSeeds());

		carried.put(ItemID.ONION_SEED, 3);
		assertEquals(4, plan(List.of(FarmLocation.FALADOR), EVERYTHING).getSteps().size());

		// Turned off, they aren't tended whatever is carried
		assertEquals(Set.of(FALADOR_HERB), plan(List.of(FarmLocation.FALADOR), HERBS).getSteps().keySet());
	}

	@Test
	public void seedsGoToTheFirstStopFirst()
	{
		set(FALADOR_HERB, PatchStatus.READY);
		set(FALADOR_ALLOTMENT_1, PatchStatus.READY);
		set(FarmLocation.CATHERBY.patches(PatchType.HERB).get(0), PatchStatus.READY);
		FarmPatch catherbyAllotment = FarmLocation.CATHERBY.patches(PatchType.ALLOTMENT).get(0);
		set(catherbyAllotment, PatchStatus.READY);
		carried.put(ItemID.POTATO_SEED, 3);

		assertTrue(plan(List.of(FarmLocation.CATHERBY, FarmLocation.FALADOR), EVERYTHING).getSteps().containsKey(catherbyAllotment));
		assertFalse(plan(List.of(FarmLocation.CATHERBY, FarmLocation.FALADOR), EVERYTHING).getSteps().containsKey(FALADOR_ALLOTMENT_1));
		assertTrue(plan(List.of(FarmLocation.FALADOR, FarmLocation.CATHERBY), EVERYTHING).getSteps().containsKey(FALADOR_ALLOTMENT_1));
	}

	@Test
	public void seedBoxNotYetLookedInCountsAsHavingSeeds()
	{
		set(FALADOR_HERB, PatchStatus.READY);
		set(FALADOR_ALLOTMENT_1, PatchStatus.READY);
		carried.put(ItemID.SEED_BOX_OPEN, 1);
		seedBoxUnknown = true;

		assertTrue(plan(List.of(FarmLocation.FALADOR), EVERYTHING).getSteps().containsKey(FALADOR_ALLOTMENT_1));

		// Known to hold no allotment seeds
		seedBoxUnknown = false;
		assertFalse(plan(List.of(FarmLocation.FALADOR), EVERYTHING).getSteps().containsKey(FALADOR_ALLOTMENT_1));
	}

	@Test
	public void diseasedAllotmentIsCuredWithoutSeeds()
	{
		set(FALADOR_HERB, PatchStatus.READY);
		set(FALADOR_ALLOTMENT_1, PatchStatus.DISEASED);

		assertEquals(Step.CURE, plan(List.of(FarmLocation.FALADOR), EVERYTHING).getSteps().get(FALADOR_ALLOTMENT_1));
	}

	@Test
	public void emptyPatchGetsCompostBeforeItsSeed()
	{
		set(FALADOR_HERB, PatchStatus.EMPTY);

		RunPlan uncomposted = plan(List.of(FarmLocation.FALADOR), COMPOST);
		assertEquals(Step.COMPOST, uncomposted.getSteps().get(FALADOR_HERB));
		assertEquals(Set.of(FALADOR_HERB), uncomposted.getToCompost());
		assertEquals(Set.of(FALADOR_HERB), uncomposted.getToPlant());

		RunPlan composted = plan(List.of(FarmLocation.FALADOR), COMPOST, Set.of(FALADOR_HERB), Set.of(), null);
		assertEquals(Step.PLANT, composted.getSteps().get(FALADOR_HERB));
		assertTrue(composted.getToCompost().isEmpty());
	}

	@Test
	public void patchPlantedFirstStillGetsItsCompost()
	{
		set(FALADOR_HERB, PatchStatus.GROWING);

		assertFalse(plan(List.of(FarmLocation.FALADOR), COMPOST).getSteps().containsKey(FALADOR_HERB));

		RunPlan awaiting = plan(List.of(FarmLocation.FALADOR), COMPOST, Set.of(), Set.of(FALADOR_HERB), null);
		assertEquals(Step.COMPOST, awaiting.getSteps().get(FALADOR_HERB));
		assertTrue(awaiting.getToPlant().isEmpty());

		assertFalse(plan(List.of(FarmLocation.FALADOR), COMPOST, Set.of(FALADOR_HERB), Set.of(FALADOR_HERB), null)
			.getSteps().containsKey(FALADOR_HERB));
	}

	@Test
	public void herbContractNeedsItsSeedAtTheGuild()
	{
		set(GUILD_HERB, PatchStatus.READY);
		set(FALADOR_HERB, PatchStatus.READY);

		RunPlan plan = plan(List.of(FarmLocation.FALADOR, FarmLocation.FARMING_GUILD), HERBS, Set.of(), Set.of(), FarmingContract.IRIT);

		assertEquals(Map.of(GUILD_HERB, ItemID.IRIT_SEED), plan.getContractSeeds());
		assertEquals(Step.HARVEST, plan.getSteps().get(GUILD_HERB));
	}

	@Test
	public void allotmentContractIsTendedAtTheGuildEvenWithAllotmentsOffAndNoSeeds()
	{
		set(GUILD_HERB, PatchStatus.READY);
		for (FarmPatch patch : FarmLocation.FARMING_GUILD.patches(PatchType.ALLOTMENT))
		{
			set(patch, PatchStatus.EMPTY);
		}

		RunPlan off = plan(List.of(FarmLocation.FARMING_GUILD), HERBS, Set.of(), Set.of(), FarmingContract.WATERMELON);
		// One patch is enough for the contract
		assertEquals(Map.of(GUILD_ALLOTMENT, ItemID.WATERMELON_SEED), off.getContractSeeds());
		assertEquals(Step.PLANT, off.getSteps().get(GUILD_ALLOTMENT));
		assertEquals(2, off.getSteps().size());

		RunPlan on = plan(List.of(FarmLocation.FARMING_GUILD), EVERYTHING, Set.of(), Set.of(), FarmingContract.WATERMELON);
		assertEquals(2, on.getContractSeeds().size());
	}

	@Test
	public void contractOnlyAppliesWhenTheGuildIsAStop()
	{
		set(FALADOR_HERB, PatchStatus.READY);

		assertTrue(plan(List.of(FarmLocation.FALADOR), HERBS, Set.of(), Set.of(), FarmingContract.IRIT).getContractSeeds().isEmpty());
	}

	@Test
	public void treesGoFromHealthCheckToPlanting()
	{
		set(LUMBRIDGE_TREE, PatchStatus.CHECK_HEALTH);
		set(FarmLocation.VARROCK.patches(PatchType.TREE).get(0), PatchStatus.GROWN);
		set(FarmLocation.FALADOR_PARK.patches(PatchType.TREE).get(0), PatchStatus.STUMP);
		set(FarmLocation.TAVERLEY.patches(PatchType.TREE).get(0), PatchStatus.DISEASED);

		RunPlan plan = plan(FarmLocation.of(RunType.TREE), TREES);

		assertEquals(Step.CHECK_HEALTH, plan.getSteps().get(LUMBRIDGE_TREE));
		assertEquals(Step.REMOVE, plan.getSteps().get(FarmLocation.VARROCK.patches(PatchType.TREE).get(0)));
		assertEquals(Step.DIG, plan.getSteps().get(FarmLocation.FALADOR_PARK.patches(PatchType.TREE).get(0)));
		assertEquals(Step.CURE, plan.getSteps().get(FarmLocation.TAVERLEY.patches(PatchType.TREE).get(0)));
		// Each but the pruned tree makes way for a new one, whether or not a sapling is carried
		assertEquals(3, plan.seedsNeeded(PatchType.TREE));
	}

	@Test
	public void treeRunLeavesHerbPatchesAloneAndTheOtherWayRound()
	{
		set(GUILD_HERB, PatchStatus.READY);
		set(GUILD_ALLOTMENT, PatchStatus.READY);
		set(GUILD_TREE, PatchStatus.CHECK_HEALTH);
		set(GUILD_FRUIT_TREE, PatchStatus.CHECK_HEALTH);
		carried.put(ItemID.POTATO_SEED, 3);

		assertEquals(Set.of(GUILD_TREE, GUILD_FRUIT_TREE), plan(List.of(FarmLocation.FARMING_GUILD), TREES).getSteps().keySet());
		assertEquals(Set.of(GUILD_HERB, GUILD_ALLOTMENT), plan(List.of(FarmLocation.FARMING_GUILD), EVERYTHING).getSteps().keySet());
	}

	@Test
	public void treePlantedOnTheRunGetsCompostThenThePayment()
	{
		states.put(LUMBRIDGE_TREE, new PatchState(PatchStatus.GROWING, 123, Sapling.WILLOW));
		carried.put(ItemID.BASKET_APPLE_5, 1);
		List<FarmLocation> stops = List.of(FarmLocation.LUMBRIDGE);

		assertEquals(Step.COMPOST, plan(stops, TREES_PROTECTED, Set.of(), Set.of(), Set.of(LUMBRIDGE_TREE), null).getSteps().get(LUMBRIDGE_TREE));

		RunPlan composted = plan(stops, TREES_PROTECTED, Set.of(LUMBRIDGE_TREE), Set.of(), Set.of(LUMBRIDGE_TREE), null);
		assertEquals(Step.PAY, composted.getSteps().get(LUMBRIDGE_TREE));
		assertEquals(Set.of(LUMBRIDGE_TREE), composted.getToPay());

		assertNull(plan(stops, TREES_PROTECTED, Set.of(LUMBRIDGE_TREE), Set.of(LUMBRIDGE_TREE), Set.of(LUMBRIDGE_TREE), null)
			.getSteps().get(LUMBRIDGE_TREE));
		// A tree that was already growing before the run is left alone
		assertNull(plan(stops, TREES_PROTECTED).getSteps().get(LUMBRIDGE_TREE));
	}

	@Test
	public void treeToReplantCountsTowardsPaymentsWithTheSaplingSetAside()
	{
		set(LUMBRIDGE_TREE, PatchStatus.CHECK_HEALTH);
		carried.put(ItemID.PLANTPOT_MAGIC_TREE_SAPLING, 1);

		RunPlan plan = plan(List.of(FarmLocation.LUMBRIDGE), TREES_PROTECTED);

		assertEquals(Set.of(LUMBRIDGE_TREE), plan.getToPay());
		assertEquals(Integer.valueOf(ItemID.PLANTPOT_MAGIC_TREE_SAPLING), plan.getSeedsFor().get(LUMBRIDGE_TREE));
	}

	@Test
	public void treeContractIsPlantedOnTreeRunsOnly()
	{
		set(GUILD_HERB, PatchStatus.READY);
		set(GUILD_TREE, PatchStatus.CHECK_HEALTH);

		assertEquals(Map.of(GUILD_TREE, ItemID.PLANTPOT_MAGIC_TREE_SAPLING),
			plan(List.of(FarmLocation.FARMING_GUILD), TREES, Set.of(), Set.of(), FarmingContract.MAGIC).getContractSeeds());
		assertTrue(plan(List.of(FarmLocation.FARMING_GUILD), HERBS, Set.of(), Set.of(), FarmingContract.MAGIC).getContractSeeds().isEmpty());
	}

	@Test
	public void gardenerIsOnlyPaidWithThePaymentToHand()
	{
		states.put(LUMBRIDGE_TREE, new PatchState(PatchStatus.GROWING, 123, Sapling.MAGIC));
		carried.put(ItemID.COCONUT, 24);
		List<FarmLocation> stops = List.of(FarmLocation.LUMBRIDGE);

		RunPlan shortOne = plan(stops, TREES_PROTECTED, Set.of(LUMBRIDGE_TREE), Set.of(), Set.of(LUMBRIDGE_TREE), null);
		assertNull(shortOne.getSteps().get(LUMBRIDGE_TREE));
		// Still on the checklist as wanted
		assertEquals(Set.of(LUMBRIDGE_TREE), shortOne.getToPay());

		carried.put(ItemID.COCONUT, 25);
		assertEquals(Step.PAY, plan(stops, TREES_PROTECTED, Set.of(LUMBRIDGE_TREE), Set.of(), Set.of(LUMBRIDGE_TREE), null)
			.getSteps().get(LUMBRIDGE_TREE));
	}

	@Test
	public void guildPatchesAboveThePlayersFarmingLevelAreLeftOut()
	{
		set(GUILD_TREE, PatchStatus.CHECK_HEALTH);
		set(GUILD_FRUIT_TREE, PatchStatus.WEEDS);
		set(GUILD_HERB, PatchStatus.READY);
		set(GUILD_ALLOTMENT, PatchStatus.READY);
		carried.put(ItemID.POTATO_SEED, 3);
		List<FarmLocation> guild = List.of(FarmLocation.FARMING_GUILD);

		assertEquals(Set.of(GUILD_TREE), plan(guild, new RunPlan.Options(RunType.TREE, false, false, false, false, 80)).getSteps().keySet());
		assertEquals(Set.of(GUILD_TREE, GUILD_FRUIT_TREE), plan(guild, new RunPlan.Options(RunType.TREE, false, false, false, false, 85)).getSteps().keySet());
		assertTrue(plan(guild, new RunPlan.Options(RunType.TREE, false, false, false, false, 64)).getSteps().isEmpty());
		assertEquals(Set.of(GUILD_ALLOTMENT), plan(guild, new RunPlan.Options(RunType.HERB, true, false, false, false, 50)).getSteps().keySet());

		assertEquals(List.of(GUILD_TREE), FarmLocation.FARMING_GUILD.anchors(RunType.TREE, 80));
	}
}
