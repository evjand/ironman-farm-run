package com.ironmanfarmrun;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import lombok.Value;

/**
 * Which items the bank filter shows, and in which section.
 */
@Value
class BankFilterSpec
{
	static final BankFilterSpec HERBS_ONLY = new BankFilterSpec(false, false, -1, List.of(), List.of());
	static final int ITEMS_PER_ROW = 8;

	boolean allotmentSeeds;
	boolean flowerSeeds;
	/**
	 * A farming contract's seed, shown in its section even when the player doesn't grow that kind of
	 * crop otherwise; -1 for none.
	 */
	int contractSeed;
	/**
	 * Shown in the equipment section after the tools: graceful and volcanic ash, when the player wants them.
	 */
	List<Integer> extraEquipment;
	List<Integer> teleportItems;

	/**
	 * @param itemId a canonical item id: not a note or placeholder
	 * @return the section the item belongs in, or null if the filter doesn't show it
	 */
	@Nullable
	BankSection sectionOf(int itemId)
	{
		if (RunItems.HERB_SEEDS.contains(itemId))
		{
			return BankSection.HERB_SEEDS;
		}
		boolean contract = itemId == contractSeed;
		if ((allotmentSeeds || contract) && RunItems.ALLOTMENT_SEEDS.contains(itemId))
		{
			return BankSection.ALLOTMENT_SEEDS;
		}
		if ((flowerSeeds || contract) && RunItems.FLOWER_SEEDS.contains(itemId))
		{
			return BankSection.FLOWER_SEEDS;
		}
		if (RunItems.EQUIPMENT.contains(itemId) || extraEquipment.contains(itemId))
		{
			return BankSection.EQUIPMENT;
		}
		if (teleportItems.contains(itemId))
		{
			return BankSection.TELEPORTS;
		}
		return null;
	}

	/**
	 * Where a section's items go in the layout, and which row its heading sits in.
	 */
	@Value
	static class Arrangement
	{
		/**
		 * Item id per bank slot, eight slots to a row; -1 for an empty slot.
		 */
		int[] slots;
		/**
		 * The row left empty above each section, for its heading.
		 */
		Map<BankSection, Integer> headingRows;
	}

	/**
	 * Lays the given items out section by section, each under an empty row for its heading.
	 *
	 * @param bankItemIds canonical ids of the items in the bank
	 */
	Arrangement arrange(Collection<Integer> bankItemIds)
	{
		Set<Integer> inBank = new LinkedHashSet<>(bankItemIds);
		Map<BankSection, List<Integer>> bySection = new EnumMap<>(BankSection.class);
		for (BankSection section : BankSection.values())
		{
			List<Integer> items = new ArrayList<>();
			// The declared order first, so related items sit together whatever order the bank is in
			for (int itemId : order(section))
			{
				if (inBank.contains(itemId) && sectionOf(itemId) == section)
				{
					items.add(itemId);
				}
			}
			for (int itemId : inBank)
			{
				if (sectionOf(itemId) == section && !items.contains(itemId))
				{
					items.add(itemId);
				}
			}
			if (!items.isEmpty())
			{
				bySection.put(section, items);
			}
		}

		List<Integer> slots = new ArrayList<>();
		Map<BankSection, Integer> headingRows = new EnumMap<>(BankSection.class);
		for (Map.Entry<BankSection, List<Integer>> section : bySection.entrySet())
		{
			headingRows.put(section.getKey(), slots.size() / ITEMS_PER_ROW);
			for (int i = 0; i < ITEMS_PER_ROW; i++)
			{
				slots.add(-1);
			}

			slots.addAll(section.getValue());
			while (slots.size() % ITEMS_PER_ROW != 0)
			{
				slots.add(-1);
			}
		}

		return new Arrangement(slots.stream().mapToInt(Integer::intValue).toArray(), headingRows);
	}

	private List<Integer> order(BankSection section)
	{
		switch (section)
		{
			case HERB_SEEDS:
				return RunItems.HERB_SEEDS;
			case ALLOTMENT_SEEDS:
				return RunItems.ALLOTMENT_SEEDS;
			case FLOWER_SEEDS:
				return RunItems.FLOWER_SEEDS;
			case EQUIPMENT:
				List<Integer> equipment = new ArrayList<>(RunItems.EQUIPMENT);
				equipment.addAll(extraEquipment);
				return equipment;
			default:
				return teleportItems;
		}
	}
}
