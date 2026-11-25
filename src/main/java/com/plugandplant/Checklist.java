package com.plugandplant;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import lombok.Value;
import net.runelite.api.gameval.ItemID;

/**
 * Builds the list of what to bring for the rest of a run, checked off against what the player carries.
 */
final class Checklist
{
	/**
	 * What a gardener charges to take down a grown tree.
	 */
	static final int REMOVAL_COINS = 200;

	/**
	 * The optional parts of the checklist.
	 */
	@Value
	static class Options
	{
		RunPreset preset;
		boolean compost;
		boolean protect;
		boolean volcanicAsh;
		/**
		 * Every graceful piece's item ids, one list per piece, or empty to leave graceful out.
		 */
		List<List<Integer>> gracefulPieces;
	}

	private Checklist()
	{
	}

	/**
	 * @param carried what the player has on them, with noted items counted as the items themselves
	 */
	static List<ChecklistItem> build(RunPlan plan, Map<FarmPatch, PatchState> states, Map<FarmLocation, LocationSettings> settings,
		Carried carried, Options options, @Nullable FarmingContract contract)
	{
		List<ChecklistItem> items = new ArrayList<>();
		boolean seedBox = carried.isSeedBoxUnknown();
		RunPreset preset = options.getPreset();
		boolean trees = preset.includesTrees();

		// The seeds of what the run visits for are counted against what the stops need
		for (PatchType type : RunPreset.TYPES)
		{
			if (preset.role(type) != PatchRole.VISIT)
			{
				continue;
			}
			int needed = plan.seedsNeeded(type);
			if (needed == 0)
			{
				continue;
			}

			int have = carried.count(type.getSeeds());
			if (have >= needed)
			{
				items.add(new ChecklistItem(type.getSeedLabel(), have + " / " + needed, ChecklistItem.State.GOOD));
			}
			else if (seedBox && !type.isTree())
			{
				// A seed box's contents can't be read
				items.add(new ChecklistItem(type.getSeedLabel(), have + " / " + needed + " + box", ChecklistItem.State.UNSURE));
			}
			else
			{
				items.add(new ChecklistItem(type.getSeedLabel(), have + " / " + needed, ChecklistItem.State.MISSING));
			}
		}

		// What is tended while there takes whatever seeds the player brings, so these show how far they go
		for (PatchType type : RunPreset.TYPES)
		{
			if (preset.role(type) == PatchRole.TEND)
			{
				items.add(seedsFor(type, type.getSeedLabel(), carried, seedBox));
			}
		}

		if (contract != null)
		{
			boolean planting = plan.getContractSeeds().keySet().stream().anyMatch(plan.getToPlant()::contains);
			if (planting)
			{
				String label = "Contract: " + contract.seedLabel();
				int needed = contract.getType().getSeedsPerPatch();
				if (carried.count(contract.getSeed()) >= needed)
				{
					items.add(new ChecklistItem(label, "Yes", ChecklistItem.State.GOOD));
				}
				else
				{
					boolean unsure = seedBox && !contract.getType().isTree();
					items.add(new ChecklistItem(label, unsure ? "Check seed box" : "Missing",
						unsure ? ChecklistItem.State.UNSURE : ChecklistItem.State.MISSING));
				}
			}
		}

		if (trees && options.isProtect())
		{
			items.addAll(payments(plan, states, carried));
		}

		if (trees)
		{
			// A grown tree is taken down by its gardener for coins, or chopped, which needs an axe instead
			int removals = 0;
			for (Map.Entry<FarmPatch, Step> step : plan.getSteps().entrySet())
			{
				boolean removal = step.getValue() == Step.CHECK_HEALTH || step.getValue() == Step.REMOVE;
				if (removal && step.getKey().getType().isRemovedForCoins())
				{
					removals++;
				}
			}
			if (removals > 0)
			{
				int needed = removals * REMOVAL_COINS;
				int coins = carried.count(ItemID.COINS);
				if (coins >= needed)
				{
					items.add(new ChecklistItem("Coins", coins + " / " + needed, ChecklistItem.State.GOOD));
				}
				else if (carried.hasAny(RunItems.AXES))
				{
					items.add(new ChecklistItem("Coins", coins + " / " + needed + ", axe", ChecklistItem.State.GOOD));
				}
				else
				{
					items.add(new ChecklistItem("Coins", coins + " / " + needed + " or axe", ChecklistItem.State.MISSING));
				}
			}
		}

		if (options.isCompost())
		{
			int needed = plan.getToCompost().size();
			int buckets = carried.count(RunItems.COMPOST_BUCKETS);
			int storedBuckets = carried.stored(RunItems.COMPOST_BUCKETS);
			if (carried.has(ItemID.BOTTOMLESS_COMPOST_BUCKET_FILLED))
			{
				items.add(new ChecklistItem("Compost", "Bottomless", ChecklistItem.State.GOOD));
			}
			else if (buckets >= needed && (needed > 0 || buckets > 0))
			{
				items.add(new ChecklistItem("Compost", buckets + " / " + needed, ChecklistItem.State.GOOD));
			}
			else if (needed > 0 && carried.stored(ItemID.BOTTOMLESS_COMPOST_BUCKET_FILLED) > 0)
			{
				items.add(new ChecklistItem("Compost", "Bottomless at leprechaun", ChecklistItem.State.STORED));
			}
			else if (needed > 0 && buckets + storedBuckets >= needed)
			{
				items.add(new ChecklistItem("Compost", buckets + " / " + needed + ", " + storedBuckets + " at leprechaun",
					ChecklistItem.State.STORED));
			}
			else if (needed > 0)
			{
				// The player asked to compost, so coming up short is worth a red line
				items.add(new ChecklistItem("Compost", buckets + " / " + needed, ChecklistItem.State.MISSING));
			}
		}

		// Tools may be with the tool leprechaun, so one that isn't carried is unsure rather than missing
		items.add(tool("Spade", carried, List.of(ItemID.SPADE), true));
		boolean seedPatches = preset.includesSeedPatches();
		if (seedPatches)
		{
			items.add(tool("Seed dibber", carried, List.of(ItemID.DIBBER), true));
		}
		if (plan.has(Step.RAKE))
		{
			items.add(tool("Rake", carried, List.of(ItemID.RAKE), true));
		}
		// Trees are pruned rather than cured
		if (plan.getSteps().entrySet().stream().anyMatch(step -> step.getValue() == Step.CURE && step.getKey().getType().isTree()))
		{
			items.add(tool("Secateurs", carried, List.of(ItemID.SECATEURS, ItemID.FAIRY_ENCHANTED_SECATEURS), true));
		}
		if (plan.getSteps().entrySet().stream().anyMatch(step -> step.getValue() == Step.CURE && !step.getKey().getType().isTree()))
		{
			items.add(tool("Plant cure", carried, List.of(ItemID.PLANT_CURE), true));
		}
		// Celastrus bark and crystal shards are cut from the tree
		boolean chopping = plan.getSteps().entrySet().stream().anyMatch(step ->
			(step.getValue() == Step.CHECK_HEALTH || step.getValue() == Step.HARVEST)
				&& step.getKey().getType().harvestItems() == RunItems.AXES);
		if (chopping)
		{
			items.add(tool("Axe", carried, RunItems.AXES, true));
		}
		if (seedPatches)
		{
			// Only a bonus to the yield, so going without isn't missing anything
			items.add(tool("Magic secateurs", carried, List.of(ItemID.FAIRY_ENCHANTED_SECATEURS), false));
		}

		for (FarmLocation location : plan.targets())
		{
			Teleport choice = settings.getOrDefault(location, LocationSettings.DEFAULT).getTeleport();
			List<Teleport> teleports = location.teleportsFor(choice);
			if (teleports.isEmpty())
			{
				continue;
			}

			boolean particular = teleports.size() == 1 && teleports.get(0) == choice;
			boolean itemsOnly = location.itemsOnly(choice);
			String label = particular
				? location.getDisplayName() + ": " + choice.getLabel()
				: location.getDisplayName() + " teleport";
			if (teleports.stream().anyMatch(teleport -> carried.has(teleport, itemsOnly)))
			{
				items.add(new ChecklistItem(label, "Yes", ChecklistItem.State.GOOD));
			}
			else if (particular && !choice.isSpell() && !choice.isOptional())
			{
				items.add(new ChecklistItem(label, "Missing", ChecklistItem.State.MISSING));
			}
			else
			{
				// Runes may come from a staff, a fairy ring may not need one, and with no particular teleport
				// picked there may be another way there
				String value = choice.isOptional() ? "No staff" : choice.isSpell() ? "Not found" : "None";
				items.add(new ChecklistItem(label, value, ChecklistItem.State.UNSURE));
			}
		}

		if (!options.getGracefulPieces().isEmpty())
		{
			int pieces = 0;
			for (List<Integer> piece : options.getGracefulPieces())
			{
				if (carried.hasAny(piece))
				{
					pieces++;
				}
			}
			int all = options.getGracefulPieces().size();
			items.add(new ChecklistItem("Graceful", pieces + " / " + all,
				pieces == all ? ChecklistItem.State.GOOD : ChecklistItem.State.MISSING));
		}

		if (options.isVolcanicAsh())
		{
			int ash = carried.count(ItemID.FOSSIL_VOLCANIC_ASH);
			items.add(new ChecklistItem("Volcanic ash", ash > 0 ? Integer.toString(ash) : "None",
				ash > 0 ? ChecklistItem.State.GOOD : ChecklistItem.State.MISSING));
		}

		return items;
	}

	/**
	 * What protecting the trees will cost: every sapling the player brings is a tree they mean to
	 * plant and protect, and a tree planted on this run but not paid for yet still needs its payment.
	 * Saplings that share a payment are added together.
	 */
	private static List<ChecklistItem> payments(RunPlan plan, Map<FarmPatch, PatchState> states, Carried carried)
	{
		Map<Integer, Integer> needed = new LinkedHashMap<>();
		Map<Integer, String> names = new LinkedHashMap<>();
		for (Sapling sapling : Sapling.values())
		{
			int saplings = carried.count(sapling.getSapling()) + carried.count(sapling.getWateredSeedling());
			if (saplings > 0)
			{
				add(needed, names, sapling, saplings);
			}
		}

		int withoutSapling = 0;
		for (FarmPatch patch : plan.getToPay())
		{
			if (plan.getToPlant().contains(patch))
			{
				// Still to plant: paid for above if its sapling is carried
				if (!plan.getSeedsFor().containsKey(patch))
				{
					withoutSapling++;
				}
				continue;
			}

			Sapling tree = states.getOrDefault(patch, PatchState.UNKNOWN).getCrop();
			if (tree != null)
			{
				add(needed, names, tree, 1);
			}
		}

		List<ChecklistItem> items = new ArrayList<>();
		for (Map.Entry<Integer, Integer> payment : needed.entrySet())
		{
			int have = carried.count(payment.getKey());
			String name = names.get(payment.getKey());
			items.add(new ChecklistItem("Pay: " + name, have + " / " + payment.getValue(),
				have >= payment.getValue() ? ChecklistItem.State.GOOD : ChecklistItem.State.MISSING));
		}
		if (withoutSapling > 0)
		{
			// The payment depends on the tree, which isn't known until the player brings its sapling
			items.add(new ChecklistItem("Pay: for " + withoutSapling + (withoutSapling == 1 ? " more tree" : " more trees"),
				"Bring saplings", ChecklistItem.State.UNSURE));
		}
		return items;
	}

	private static void add(Map<Integer, Integer> needed, Map<Integer, String> names, Sapling tree, int trees)
	{
		for (Sapling.Payment payment : tree.getPayments())
		{
			needed.merge(payment.getItem(), trees * payment.getAmount(), Integer::sum);
			names.putIfAbsent(payment.getItem(), payment.getName());
		}
	}

	/**
	 * How many patches of a kind the carried seeds would plant. A patch takes several seeds of one kind.
	 */
	static int patchesPlantable(PatchType type, Carried carried)
	{
		int patches = 0;
		for (int seed : type.getSeeds())
		{
			patches += carried.count(seed) / type.getSeedsPerPatch();
		}
		return patches;
	}

	private static ChecklistItem seedsFor(PatchType type, String label, Carried carried, boolean seedBox)
	{
		int patches = patchesPlantable(type, carried);
		if (patches > 0)
		{
			return new ChecklistItem(label, patches == 1 ? "1 patch" : patches + " patches", ChecklistItem.State.GOOD);
		}
		return new ChecklistItem(label, seedBox ? "Seed box" : "None", ChecklistItem.State.UNSURE);
	}

	/**
	 * A tool, carried, kept with the tool leprechaun, or neither.
	 *
	 * @param needed whether the run can't do without it
	 */
	private static ChecklistItem tool(String label, Carried carried, List<Integer> itemIds, boolean needed)
	{
		if (carried.hasAny(itemIds))
		{
			return new ChecklistItem(label, "Yes", ChecklistItem.State.GOOD);
		}
		if (carried.stored(itemIds) > 0)
		{
			return new ChecklistItem(label, "At leprechaun", ChecklistItem.State.STORED);
		}
		return new ChecklistItem(label, "Not carried", needed ? ChecklistItem.State.MISSING : ChecklistItem.State.UNSURE);
	}

	/**
	 * House teleports are labelled "House: tab" for the dropdown; next to a location name "House tab" reads better.
	 */
}
