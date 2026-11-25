package com.plugandplant;

import java.util.Collections;
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
 * A location is a stop because one of its anchor patches needs attention: the herb patch on a herb
 * run, the trees on a tree run. On a herb run, allotments and flower patches are tended along the
 * way while the player carries the seeds to replant them.
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
		RunPreset preset;
		/**
		 * Whether composting is a step, which takes the player carrying compost.
		 */
		boolean compost;
		/**
		 * Whether paying a gardener to protect a newly planted tree is a step.
		 */
		boolean protect;
		/**
		 * The player's Farming level, boosts included, which decides the Farming Guild patches they can reach.
		 */
		int farmingLevel;
		/**
		 * The kinds of patch whose produce is noted at the tool leprechaun as a step.
		 */
		Set<PatchType> noted;

		Options(RunPreset preset, boolean compost, boolean protect, int farmingLevel, Set<PatchType> noted)
		{
			this.preset = preset;
			this.compost = compost;
			this.protect = protect;
			this.farmingLevel = farmingLevel;
			this.noted = noted;
		}

		Options(RunPreset preset, boolean compost, boolean protect, int farmingLevel)
		{
			this(preset, compost, protect, farmingLevel, Set.of());
		}

		/**
		 * Whether the run tends the patch: one of the kinds it covers, within the player's reach.
		 */
		boolean includes(FarmPatch patch)
		{
			return preset.role(patch) != PatchRole.OFF && patch.getFarmingLevel() <= farmingLevel;
		}
	}

	/**
	 * Whether a contract for the type is grown on the run: one with trees for a tree, otherwise one with
	 * seed patches, even if it doesn't tend that kind of patch itself.
	 */
	static boolean covers(RunPreset preset, PatchType contractType)
	{
		return contractType.isTree() ? preset.includesTrees() : preset.includesSeedPatches();
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
	 * The trees whose gardener still gets paid, now or after the current step: those among
	 * {@link #steps}, and any planted on this run that the player has no payment for.
	 */
	Set<FarmPatch> toPay;
	/**
	 * Trees planted on this run whose gardener can be paid now, with the payment to hand. Paying
	 * doesn't wait on compost, so these are ready for the gardener whatever the patch's own step is.
	 */
	Set<FarmPatch> payable;
	/**
	 * The seed or sapling set aside from what the player carries for each patch in {@link #toPlant}
	 * that got one.
	 */
	Map<FarmPatch, Integer> seedsFor;
	/**
	 * Patches that have to be planted with one seed in particular: the crop of a farming contract.
	 */
	Map<FarmPatch, Integer> contractSeeds;
	/**
	 * Allotment and flower patches left out because the player doesn't carry the seeds to replant them.
	 */
	Set<FarmPatch> withoutSeeds;

	/**
	 * The stops with something left to do, in the order they were planned.
	 */
	Set<FarmLocation> targets()
	{
		Set<FarmLocation> targets = new LinkedHashSet<>();
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

	int count(Step step)
	{
		int count = 0;
		for (Step planned : steps.values())
		{
			if (planned == step)
			{
				count++;
			}
		}
		return count;
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
	 * @param protectedTrees  trees whose gardener has been paid to protect them
	 * @param justPlanted     patches planted earlier on this run that the player is still standing by,
	 *                        which can still be composted and paid for
	 * @param carried         what the player has on them, which decides the allotments and flowers they can replant
	 * @param contract        the player's farming contract, if it is for a crop a run grows
	 */
	static RunPlan of(List<FarmLocation> stops, Map<FarmPatch, PatchState> states, Set<FarmPatch> composted,
		Set<FarmPatch> protectedTrees, Set<FarmPatch> justPlanted, Options options, Carried carried,
		@Nullable FarmingContract contract)
	{
		Map<FarmPatch, Step> steps = new LinkedHashMap<>();
		Set<FarmPatch> toPlant = new LinkedHashSet<>();
		Set<FarmPatch> toCompost = new LinkedHashSet<>();
		Set<FarmPatch> toPay = new LinkedHashSet<>();
		Set<FarmPatch> payable = new LinkedHashSet<>();
		Map<FarmPatch, Integer> seedsFor = new HashMap<>();
		Map<FarmPatch, Integer> contractSeeds = new HashMap<>();
		Set<FarmPatch> withoutSeeds = new LinkedHashSet<>();

		// A contract is grown on the run of its kind
		Set<FarmPatch> contractPatches = new HashSet<>();
		if (contract != null && covers(options.getPreset(), contract.getType()))
		{
			for (FarmPatch patch : contract.patches(options.getPreset().includes(contract.getType())))
			{
				if (patch.getFarmingLevel() <= options.getFarmingLevel())
				{
					contractPatches.add(patch);
				}
			}
		}
		// A seed box the game hasn't shown the contents of is taken to hold whatever the player needs
		boolean seedBox = carried.isSeedBoxUnknown();
		Map<Integer, Integer> seedsLeft = new HashMap<>(carried.getItems());
		int spiritSlots = spiritSlots(states, options.getFarmingLevel());

		for (FarmLocation location : stops)
		{
			for (FarmPatch patch : location.getPatches())
			{
				boolean contractPatch = contractPatches.contains(patch);
				if (!contractPatch && !options.includes(patch))
				{
					continue;
				}

				PatchStatus status = states.getOrDefault(patch, PatchState.UNKNOWN).getStatus();
				if (takesSpiritSlot(patch, status))
				{
					if (spiritSlots <= 0)
					{
						continue;
					}
					spiritSlots--;
				}
				boolean replant = status.leadsToReplanting();
				if (replant)
				{
					List<Integer> seeds = contractPatch ? List.of(contract.getSeed()) : patch.getType().getSeeds();
					Integer seed = take(seedsLeft, seeds, patch.getType().getSeedsPerPatch());
					if (seed != null)
					{
						seedsFor.put(patch, seed);
					}
					// The anchor patches are why the location is a stop, and a contract is a commitment, so
					// those are planned whatever is carried; other patches only while seeds last
					boolean anchor = options.getPreset().role(patch) == PatchRole.VISIT;
					if (seed == null && !seedBox && !anchor && !contractPatch)
					{
						withoutSeeds.add(patch);
						continue;
					}
				}

				boolean needsCompost = options.isCompost() && !composted.contains(patch);
				boolean needsPayment = options.isProtect() && patch.getType().isProtectable() && !protectedTrees.contains(patch);
				// The gardener can only be paid with the payment to hand; without it the tree goes unprotected
				Sapling tree = states.getOrDefault(patch, PatchState.UNKNOWN).getCrop();
				boolean canPay = tree != null && tree.canPay(carried);
				if (needsPayment && canPay && status == PatchStatus.GROWING && justPlanted.contains(patch))
				{
					payable.add(patch);
				}
				Step step = step(status, needsCompost, needsPayment && canPay, justPlanted.contains(patch));
				// A patch's produce is noted as its last step, before going on to the next patch with room to spare
				if (step == null && justPlanted.contains(patch) && options.getNoted().contains(patch.getType())
					&& location.hasToolLeprechaun() && carried.unnoted(patch.getType().produce()) > 0)
				{
					step = Step.NOTE;
				}
				if (step == null)
				{
					// Planted on this run without the payment to hand: nothing to do, but the checklist says what's missing
					if (needsPayment && status == PatchStatus.GROWING && justPlanted.contains(patch))
					{
						toPay.add(patch);
					}
					continue;
				}

				steps.put(patch, step);
				if (replant)
				{
					toPlant.add(patch);
				}
				// Whatever ends in planting, or was just planted, still gets its compost and payment
				boolean fresh = replant || status == PatchStatus.GROWING;
				if (needsCompost && fresh)
				{
					toCompost.add(patch);
				}
				if (needsPayment && fresh)
				{
					toPay.add(patch);
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
			Collections.unmodifiableSet(toPay),
			Collections.unmodifiableSet(payable),
			Collections.unmodifiableMap(seedsFor),
			Collections.unmodifiableMap(contractSeeds),
			Collections.unmodifiableSet(withoutSeeds));
	}

	/**
	 * How many spirit trees the game lets a player have planted at once at a Farming level.
	 */
	static int spiritTreeLimit(int farmingLevel)
	{
		if (farmingLevel >= 99)
		{
			return Integer.MAX_VALUE;
		}
		if (farmingLevel >= 96)
		{
			return 4;
		}
		if (farmingLevel >= 93)
		{
			return 3;
		}
		if (farmingLevel >= 88)
		{
			return 2;
		}
		return farmingLevel >= 83 ? 1 : 0;
	}

	/**
	 * How many more spirit trees the player may plant, as the game caps how many stand at once.
	 */
	static int spiritSlots(Map<FarmPatch, PatchState> states, int farmingLevel)
	{
		int limit = spiritTreeLimit(farmingLevel);
		if (limit == Integer.MAX_VALUE)
		{
			return limit;
		}
		int standing = 0;
		for (Map.Entry<FarmPatch, PatchState> state : states.entrySet())
		{
			if (state.getKey().getType() == PatchType.SPIRIT_TREE && holdsTree(state.getValue().getStatus()))
			{
				standing++;
			}
		}
		return limit - standing;
	}

	/**
	 * Whether tending the patch means planting a spirit tree where none stands, which is only worth
	 * the trip while the player may plant another.
	 */
	static boolean takesSpiritSlot(FarmPatch patch, PatchStatus status)
	{
		return patch.getType() == PatchType.SPIRIT_TREE && status.needsVisit() && !holdsTree(status);
	}

	/**
	 * Whether a living tree stands in the patch, which counts towards the spirit tree limit.
	 */
	private static boolean holdsTree(PatchStatus status)
	{
		switch (status)
		{
			case GROWING:
			case DISEASED:
			case CHECK_HEALTH:
			case GROWN:
				return true;
			default:
				return false;
		}
	}

	/**
	 * Sets aside enough of any one kind of seed for a patch.
	 *
	 * @return the seed set aside, or null if there weren't enough
	 */
	@Nullable
	private static Integer take(Map<Integer, Integer> seedsLeft, List<Integer> seeds, int perPatch)
	{
		for (int seed : seeds)
		{
			int left = seedsLeft.getOrDefault(seed, 0);
			if (left >= perPatch)
			{
				seedsLeft.put(seed, left - perPatch);
				return seed;
			}
		}
		return null;
	}

	@Nullable
	private static Step step(PatchStatus status, boolean needsCompost, boolean needsPayment, boolean justPlanted)
	{
		switch (status)
		{
			case READY:
				return Step.HARVEST;
			case CHECK_HEALTH:
				return Step.CHECK_HEALTH;
			case GROWN:
				return Step.REMOVE;
			case STUMP:
				return Step.DIG;
			case DEAD:
				return Step.CLEAR;
			case DISEASED:
				return Step.CURE;
			case WEEDS:
				return Step.RAKE;
			case EMPTY:
				return needsCompost ? Step.COMPOST : Step.PLANT;
			case GROWING:
				// Compost can also go on after planting, which is the order many players use; the
				// gardener is paid once the tree is in
				if (!justPlanted)
				{
					return null;
				}
				if (needsCompost)
				{
					return Step.COMPOST;
				}
				return needsPayment ? Step.PAY : null;
			default:
				return null;
		}
	}
}
