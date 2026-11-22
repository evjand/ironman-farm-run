package com.ironmanfarmrun;

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
		RunType runType;
		boolean allotments;
		boolean flowers;
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
		boolean seedBox = carried.hasAny(RunItems.SEED_BOXES);
		boolean trees = options.getRunType() == RunType.TREE;

		// The anchors' seeds are counted against what the stops need
		for (PatchType type : options.getRunType().getAnchorTypes().contains(PatchType.HERB)
			? List.of(PatchType.HERB) : List.of(PatchType.TREE, PatchType.FRUIT_TREE))
		{
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

		// Allotments and flowers take whatever seeds the player brings, so these show how far they go
		if (!trees && options.isAllotments())
		{
			items.add(seedsFor(PatchType.ALLOTMENT, "Allotment seeds", carried, seedBox));
		}
		if (!trees && options.isFlowers())
		{
			items.add(seedsFor(PatchType.FLOWER, "Flower seeds", carried, seedBox));
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
			int removals = plan.count(Step.CHECK_HEALTH) + plan.count(Step.REMOVE);
			if (removals > 0)
			{
				int needed = removals * REMOVAL_COINS;
				int coins = carried.count(ItemID.COINS);
				items.add(new ChecklistItem("Coins to remove trees", coins + " / " + needed,
					coins >= needed ? ChecklistItem.State.GOOD : ChecklistItem.State.UNSURE));
			}
		}

		if (options.isCompost())
		{
			int needed = plan.getToCompost().size();
			int buckets = carried.count(RunItems.COMPOST_BUCKETS);
			if (carried.has(ItemID.BOTTOMLESS_COMPOST_BUCKET_FILLED))
			{
				items.add(new ChecklistItem("Compost", "Bottomless", ChecklistItem.State.GOOD));
			}
			else if (needed > 0 || buckets > 0)
			{
				items.add(new ChecklistItem("Compost", buckets + " / " + needed,
					buckets >= needed ? ChecklistItem.State.GOOD : ChecklistItem.State.UNSURE));
			}
		}

		// Tools may be with the tool leprechaun, so one that isn't carried is unsure rather than missing
		items.add(tool("Spade", carried.has(ItemID.SPADE)));
		if (!trees)
		{
			items.add(tool("Seed dibber", carried.has(ItemID.DIBBER)));
		}
		if (plan.has(Step.RAKE))
		{
			items.add(tool("Rake", carried.has(ItemID.RAKE)));
		}
		if (plan.has(Step.CURE))
		{
			// Trees are pruned rather than cured
			items.add(trees
				? tool("Secateurs", carried.has(ItemID.SECATEURS) || carried.has(ItemID.FAIRY_ENCHANTED_SECATEURS))
				: tool("Plant cure", carried.has(ItemID.PLANT_CURE)));
		}
		if (!trees)
		{
			items.add(tool("Magic secateurs", carried.has(ItemID.FAIRY_ENCHANTED_SECATEURS)));
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
			String label = particular
				? location.getDisplayName() + ": " + shortLabel(choice)
				: location.getDisplayName() + " teleport";
			if (teleports.stream().anyMatch(carried::has))
			{
				items.add(new ChecklistItem(label, "Yes", ChecklistItem.State.GOOD));
			}
			else if (particular && !choice.isSpell())
			{
				items.add(new ChecklistItem(label, "Missing", ChecklistItem.State.MISSING));
			}
			else
			{
				// Runes may come from a staff, and with no particular teleport picked there may be another way there
				items.add(new ChecklistItem(label, choice.isSpell() ? "Runes not found" : "None", ChecklistItem.State.UNSURE));
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
	 * What each tree to be paid for will cost: the tree already in the patch, or the sapling set
	 * aside for it. Saplings that share a payment are added together.
	 */
	private static List<ChecklistItem> payments(RunPlan plan, Map<FarmPatch, PatchState> states, Carried carried)
	{
		Map<Integer, Integer> needed = new LinkedHashMap<>();
		Map<Integer, String> names = new LinkedHashMap<>();
		int unknown = 0;
		for (FarmPatch patch : plan.getToPay())
		{
			Sapling tree = plan.getToPlant().contains(patch)
				? (plan.getSeedsFor().containsKey(patch) ? Sapling.bySapling(plan.getSeedsFor().get(patch)) : null)
				: states.getOrDefault(patch, PatchState.UNKNOWN).getCrop();
			if (tree == null)
			{
				unknown++;
				continue;
			}
			needed.merge(tree.getPayment(), tree.getPaymentAmount(), Integer::sum);
			names.putIfAbsent(tree.getPayment(), tree.getPaymentName());
		}

		List<ChecklistItem> items = new ArrayList<>();
		for (Map.Entry<Integer, Integer> payment : needed.entrySet())
		{
			int have = carried.count(payment.getKey());
			String name = names.get(payment.getKey());
			items.add(new ChecklistItem("Pay: " + name, have + " / " + payment.getValue(),
				have >= payment.getValue() ? ChecklistItem.State.GOOD : ChecklistItem.State.MISSING));
		}
		if (unknown > 0)
		{
			// The payment depends on the tree, which isn't known until the player brings its sapling
			items.add(new ChecklistItem("Pay: for " + unknown + (unknown == 1 ? " more tree" : " more trees"),
				"Bring saplings", ChecklistItem.State.UNSURE));
		}
		return items;
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

	private static ChecklistItem tool(String label, boolean carried)
	{
		return new ChecklistItem(label, carried ? "Yes" : "Not carried",
			carried ? ChecklistItem.State.GOOD : ChecklistItem.State.UNSURE);
	}

	/**
	 * House teleports are labelled "House: tab" for the dropdown; next to a location name "House tab" reads better.
	 */
	private static String shortLabel(Teleport teleport)
	{
		return teleport.getLabel().replace("House: ", "House ");
	}
}
