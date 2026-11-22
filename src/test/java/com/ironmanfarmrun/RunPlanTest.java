package com.ironmanfarmrun;

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
	private static final FarmPatch FALADOR_HERB = FarmLocation.FALADOR.herbPatch();
	private static final FarmPatch FALADOR_ALLOTMENT_1 = FarmLocation.FALADOR.patches(PatchType.ALLOTMENT).get(0);
	private static final FarmPatch FALADOR_ALLOTMENT_2 = FarmLocation.FALADOR.patches(PatchType.ALLOTMENT).get(1);
	private static final FarmPatch FALADOR_FLOWER = FarmLocation.FALADOR.patches(PatchType.FLOWER).get(0);
	private static final FarmPatch GUILD_HERB = FarmLocation.FARMING_GUILD.herbPatch();
	private static final FarmPatch GUILD_ALLOTMENT = FarmLocation.FARMING_GUILD.patches(PatchType.ALLOTMENT).get(0);

	private static final RunPlan.Options HERBS = new RunPlan.Options(false, false, false);
	private static final RunPlan.Options EVERYTHING = new RunPlan.Options(true, true, false);
	private static final RunPlan.Options COMPOST = new RunPlan.Options(false, false, true);

	private final Map<FarmPatch, PatchState> states = new HashMap<>();
	private final Map<Integer, Integer> carried = new HashMap<>();

	private void set(FarmPatch patch, PatchStatus status)
	{
		states.put(patch, new PatchState(status, status == PatchStatus.GROWING ? 123 : 0));
	}

	private RunPlan plan(List<FarmLocation> stops, RunPlan.Options options)
	{
		return plan(stops, options, Set.of(), Set.of(), null);
	}

	private RunPlan plan(List<FarmLocation> stops, RunPlan.Options options, Set<FarmPatch> composted,
		Set<FarmPatch> awaitingCompost, FarmingContract contract)
	{
		return RunPlan.of(stops, states, composted, awaitingCompost, options, new Carried(carried, Map.of()), contract);
	}

	@Test
	public void eachStatusHasItsNextStep()
	{
		set(FarmLocation.FALADOR.herbPatch(), PatchStatus.READY);
		set(FarmLocation.CATHERBY.herbPatch(), PatchStatus.DEAD);
		set(FarmLocation.ARDOUGNE.herbPatch(), PatchStatus.DISEASED);
		set(FarmLocation.MORYTANIA.herbPatch(), PatchStatus.WEEDS);
		set(FarmLocation.HOSIDIUS.herbPatch(), PatchStatus.EMPTY);
		set(FarmLocation.FARMING_GUILD.herbPatch(), PatchStatus.GROWING);

		RunPlan plan = plan(List.of(FarmLocation.values()), HERBS);

		assertEquals(Step.HARVEST, plan.getSteps().get(FarmLocation.FALADOR.herbPatch()));
		assertEquals(Step.CLEAR, plan.getSteps().get(FarmLocation.CATHERBY.herbPatch()));
		assertEquals(Step.CURE, plan.getSteps().get(FarmLocation.ARDOUGNE.herbPatch()));
		assertEquals(Step.RAKE, plan.getSteps().get(FarmLocation.MORYTANIA.herbPatch()));
		assertEquals(Step.PLANT, plan.getSteps().get(FarmLocation.HOSIDIUS.herbPatch()));
		assertNull(plan.getSteps().get(FarmLocation.FARMING_GUILD.herbPatch()));
		assertNull(plan.getSteps().get(FarmLocation.WEISS.herbPatch()));
	}

	@Test
	public void onlyTheGivenStopsArePlanned()
	{
		set(FALADOR_HERB, PatchStatus.READY);
		set(FarmLocation.CATHERBY.herbPatch(), PatchStatus.READY);

		assertEquals(EnumSet.of(FarmLocation.CATHERBY), plan(List.of(FarmLocation.CATHERBY), HERBS).targets());
	}

	@Test
	public void herbPatchIsPlannedEvenWithoutSeeds()
	{
		set(FALADOR_HERB, PatchStatus.READY);
		set(FarmLocation.ARDOUGNE.herbPatch(), PatchStatus.DISEASED);

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
		set(FarmLocation.CATHERBY.herbPatch(), PatchStatus.READY);
		FarmPatch catherbyAllotment = FarmLocation.CATHERBY.patches(PatchType.ALLOTMENT).get(0);
		set(catherbyAllotment, PatchStatus.READY);
		carried.put(ItemID.POTATO_SEED, 3);

		assertTrue(plan(List.of(FarmLocation.CATHERBY, FarmLocation.FALADOR), EVERYTHING).getSteps().containsKey(catherbyAllotment));
		assertFalse(plan(List.of(FarmLocation.CATHERBY, FarmLocation.FALADOR), EVERYTHING).getSteps().containsKey(FALADOR_ALLOTMENT_1));
		assertTrue(plan(List.of(FarmLocation.FALADOR, FarmLocation.CATHERBY), EVERYTHING).getSteps().containsKey(FALADOR_ALLOTMENT_1));
	}

	@Test
	public void seedBoxCountsAsHavingSeeds()
	{
		set(FALADOR_HERB, PatchStatus.READY);
		set(FALADOR_ALLOTMENT_1, PatchStatus.READY);
		carried.put(ItemID.SEED_BOX_OPEN, 1);

		assertTrue(plan(List.of(FarmLocation.FALADOR), EVERYTHING).getSteps().containsKey(FALADOR_ALLOTMENT_1));
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
}
