package com.ironmanfarmrun;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import lombok.Value;

/**
 * What is left to do at a run's stops: the next step at each patch that needs one.
 *
 * A location is a stop because its herb patch needs attention. Allotments and flower patches are
 * tended along the way while the player carries the seeds to replant them.
 */
@Value
class RunPlan
{
	/**
	 * The player's choices that shape the plan.
	 */
	@Value
	static class Options
	{
		boolean allotments;
		boolean flowers;
		/**
		 * Whether composting is a step, which takes the player carrying compost.
		 */
		boolean compost;

		boolean includes(PatchType type)
		{
			switch (type)
			{
				case ALLOTMENT:
					return allotments;
				case FLOWER:
					return flowers;
				default:
					return true;
			}
		}
	}

	/**
	 * Patches with something to do, stop by stop.
	 */
	Map<FarmPatch, Step> steps;
	/**
	 * The patches among {@link #steps} that still need seeds, now or after the current step.
	 */
	Set<FarmPatch> toPlant;
	/**
	 * The patches among {@link #steps} that still get compost, now or after the current step.
	 */
	Set<FarmPatch> toCompost;
	/**
	 * Patches that have to be planted with one seed in particular: the crop of a farming contract.
	 */
	Map<FarmPatch, Integer> contractSeeds;
	/**
	 * Allotment and flower patches left out because the player doesn't carry the seeds to replant them.
	 */
	Set<FarmPatch> withoutSeeds;

	/**
	 * The stops with something left to do.
	 */
	Set<FarmLocation> targets()
	{
		Set<FarmLocation> targets = EnumSet.noneOf(FarmLocation.class);
		for (FarmPatch patch : steps.keySet())
		{
			targets.add(patch.getLocation());
		}
		return targets;
	}

	boolean hasSteps(FarmLocation location)
	{
		for (FarmPatch patch : steps.keySet())
		{
			if (patch.getLocation() == location)
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * How many seeds of a kind the rest of the run takes.
	 */
	int seedsNeeded(PatchType type)
	{
		int patches = 0;
		for (FarmPatch patch : toPlant)
		{
			if (patch.getType() == type)
			{
				patches++;
			}
		}
		return patches * type.getSeedsPerPatch();
	}

	boolean has(Step step)
	{
		return steps.containsValue(step);
	}

	/**
	 * Works out the plan.
	 *
	 * @param stops           the locations to plan for, in the order seeds are handed out: the one
	 *                        the player is at first
	 * @param states          every patch's state
	 * @param composted       patches whose current crop, or bare soil, has been treated with compost
	 * @param awaitingCompost patches planted earlier on this run that the player is still standing by
	 * @param carried         what the player has on them, which decides the allotments and flowers they can replant
	 * @param contract        the player's farming contract, if it is for a crop a run grows
	 */
	static RunPlan of(List<FarmLocation> stops, Map<FarmPatch, PatchState> states, Set<FarmPatch> composted,
		Set<FarmPatch> awaitingCompost, Options options, Carried carried, @Nullable FarmingContract contract)
	{
		Map<FarmPatch, Step> steps = new LinkedHashMap<>();
		Set<FarmPatch> toPlant = new LinkedHashSet<>();
		Set<FarmPatch> toCompost = new LinkedHashSet<>();
		Map<FarmPatch, Integer> contractSeeds = new HashMap<>();
		Set<FarmPatch> withoutSeeds = new LinkedHashSet<>();

		Set<FarmPatch> contractPatches = contract == null
			? Set.of()
			: new HashSet<>(contract.patches(options.includes(contract.getType())));
		// A seed box's contents can't be seen, so with one the player is taken to have whatever they need
		boolean seedBox = carried.hasAny(RunItems.SEED_BOXES);
		Map<Integer, Integer> seedsLeft = new HashMap<>(carried.getItems());

		for (FarmLocation location : stops)
		{
			for (FarmPatch patch : location.getPatches())
			{
				boolean contractPatch = contractPatches.contains(patch);
				if (!contractPatch && !options.includes(patch.getType()))
				{
					continue;
				}

				PatchStatus status = states.getOrDefault(patch, PatchState.UNKNOWN).getStatus();
				// Everything but a cure or a late compost ends in planting
				boolean replant = status == PatchStatus.READY || status == PatchStatus.DEAD
					|| status == PatchStatus.WEEDS || status == PatchStatus.EMPTY;

				if (replant)
				{
					List<Integer> seeds = contractPatch ? List.of(contract.getSeed()) : patch.getType().getSeeds();
					boolean gotSeeds = take(seedsLeft, seeds, patch.getType().getSeedsPerPatch());
					// The herb patch is why the location is a stop, and a contract is a commitment, so
					// those are planned whatever is carried; other patches only while seeds last
					if (!gotSeeds && !seedBox && patch.getType() != PatchType.HERB && !contractPatch)
					{
						withoutSeeds.add(patch);
						continue;
					}
				}

				boolean needsCompost = options.isCompost() && !composted.contains(patch);
				Step step = step(status, needsCompost, awaitingCompost.contains(patch));
				if (step == null)
				{
					continue;
				}

				steps.put(patch, step);
				if (replant)
				{
					toPlant.add(patch);
				}
				if (needsCompost && status != PatchStatus.DISEASED)
				{
					toCompost.add(patch);
				}
				if (contractPatch)
				{
					contractSeeds.put(patch, contract.getSeed());
				}
			}
		}

		return new RunPlan(
			Collections.unmodifiableMap(steps),
			Collections.unmodifiableSet(toPlant),
			Collections.unmodifiableSet(toCompost),
			Collections.unmodifiableMap(contractSeeds),
			Collections.unmodifiableSet(withoutSeeds));
	}

	/**
	 * Sets aside enough of any one kind of seed for a patch.
	 *
	 * @return whether there were enough
	 */
	private static boolean take(Map<Integer, Integer> seedsLeft, List<Integer> seeds, int perPatch)
	{
		for (int seed : seeds)
		{
			int left = seedsLeft.getOrDefault(seed, 0);
			if (left >= perPatch)
			{
				seedsLeft.put(seed, left - perPatch);
				return true;
			}
		}
		return false;
	}

	@Nullable
	private static Step step(PatchStatus status, boolean needsCompost, boolean awaitingCompost)
	{
		switch (status)
		{
			case READY:
				return Step.HARVEST;
			case DEAD:
				return Step.CLEAR;
			case DISEASED:
				return Step.CURE;
			case WEEDS:
				return Step.RAKE;
			case EMPTY:
				return needsCompost ? Step.COMPOST : Step.PLANT;
			case GROWING:
				// Compost can also go on after planting, which is the order many players use
				return needsCompost && awaitingCompost ? Step.COMPOST : null;
			default:
				return null;
		}
	}
}
