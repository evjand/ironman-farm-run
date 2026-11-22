package com.plugandplant;

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
 * Which items the bank filter shows, section by section.
 */
@Value
class BankFilterSpec
{
	static final BankFilterSpec EMPTY = new BankFilterSpec(Map.of());
	static final int ITEMS_PER_ROW = 8;

	/**
	 * The items of each section, in the order they are shown. An item is in the first section that has it.
	 */
	Map<BankSection, List<Integer>> sections;

	/**
	 * @param itemId a canonical item id: not a note or placeholder
	 * @return the section the item belongs in, or null if the filter doesn't show it
	 */
	@Nullable
	BankSection sectionOf(int itemId)
	{
		for (BankSection section : BankSection.values())
		{
			List<Integer> items = sections.get(section);
			if (items != null && items.contains(itemId))
			{
				return section;
			}
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
		List<Integer> slots = new ArrayList<>();
		Map<BankSection, Integer> headingRows = new EnumMap<>(BankSection.class);
		for (BankSection section : BankSection.values())
		{
			// The declared order, so related items sit together whatever order the bank is in
			List<Integer> items = new ArrayList<>();
			for (int itemId : sections.getOrDefault(section, List.of()))
			{
				if (inBank.contains(itemId) && sectionOf(itemId) == section && !items.contains(itemId))
				{
					items.add(itemId);
				}
			}
			if (items.isEmpty())
			{
				continue;
			}

			headingRows.put(section, slots.size() / ITEMS_PER_ROW);
			for (int i = 0; i < ITEMS_PER_ROW; i++)
			{
				slots.add(-1);
			}

			slots.addAll(items);
			while (slots.size() % ITEMS_PER_ROW != 0)
			{
				slots.add(-1);
			}
		}

		return new Arrangement(slots.stream().mapToInt(Integer::intValue).toArray(), headingRows);
	}
}
