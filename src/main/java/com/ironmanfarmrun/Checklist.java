package com.ironmanfarmrun;

import java.util.ArrayList;
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
	 * The optional parts of the checklist.
	 */
	@Value
	static class Options
	{
		boolean allotments;
		boolean flowers;
		boolean compost;
		boolean volcanicAsh;
		/**
		 * Every graceful piece's item ids, one list per piece, or empty to leave graceful out.
		 */
		List<List<Integer>> gracefulPieces;
	}

	private Checklist()
	{
	}

	static List<ChecklistItem> build(RunPlan plan, Map<FarmLocation, LocationSettings> settings, Carried carried,
		Options options, @Nullable FarmingContract contract)
	{
		List<ChecklistItem> items = new ArrayList<>();
		boolean seedBox = carried.hasAny(RunItems.SEED_BOXES);

		int herbsNeeded = plan.seedsNeeded(PatchType.HERB);
		if (herbsNeeded > 0)
		{
			int have = carried.count(PatchType.HERB.getSeeds());
			if (have >= herbsNeeded)
			{
				items.add(new ChecklistItem("Herb seeds", have + " / " + herbsNeeded, ChecklistItem.State.GOOD));
			}
			else if (seedBox)
			{
				// A seed box's contents can't be read
				items.add(new ChecklistItem("Herb seeds", have + " / " + herbsNeeded + " + box", ChecklistItem.State.UNSURE));
			}
			else
			{
				items.add(new ChecklistItem("Herb seeds", have + " / " + herbsNeeded, ChecklistItem.State.MISSING));
			}
		}

		// Allotments and flowers take whatever seeds the player brings, so these show how far they go
		if (options.isAllotments())
		{
			items.add(seedsFor(PatchType.ALLOTMENT, "Allotment seeds", carried, seedBox));
		}
		if (options.isFlowers())
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
					items.add(new ChecklistItem(label, seedBox ? "Check seed box" : "Missing",
						seedBox ? ChecklistItem.State.UNSURE : ChecklistItem.State.MISSING));
				}
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
		items.add(tool("Seed dibber", carried.has(ItemID.DIBBER)));
		if (plan.has(Step.RAKE))
		{
			items.add(tool("Rake", carried.has(ItemID.RAKE)));
		}
		if (plan.has(Step.CURE))
		{
			items.add(tool("Plant cure", carried.has(ItemID.PLANT_CURE)));
		}
		items.add(tool("Magic secateurs", carried.has(ItemID.FAIRY_ENCHANTED_SECATEURS)));

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
