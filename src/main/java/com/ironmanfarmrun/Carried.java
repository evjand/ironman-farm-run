package com.ironmanfarmrun;

import java.util.Collection;
import java.util.Map;
import lombok.Value;

/**
 * A snapshot of what the player has on them.
 */
@Value
class Carried
{
	static final Carried NOTHING = new Carried(Map.of(), Map.of());

	/**
	 * Item id to quantity, over the inventory and worn equipment.
	 */
	Map<Integer, Integer> items;
	/**
	 * Rune item id to quantity, for the runes in a carried rune pouch.
	 */
	Map<Integer, Integer> pouchRunes;

	int count(int itemId)
	{
		return items.getOrDefault(itemId, 0);
	}

	int count(Collection<Integer> itemIds)
	{
		int total = 0;
		for (int itemId : itemIds)
		{
			total += count(itemId);
		}
		return total;
	}

	boolean has(int itemId)
	{
		return count(itemId) > 0;
	}

	boolean hasAny(Collection<Integer> itemIds)
	{
		return count(itemIds) > 0;
	}

	/**
	 * Whether the player can use the teleport with what they carry. Runes are only looked for as
	 * themselves, in the inventory and rune pouch: staves and combination runes aren't counted.
	 */
	boolean has(Teleport teleport)
	{
		if (!teleport.isSpell())
		{
			return hasAny(teleport.getItemIds());
		}

		for (Map.Entry<Integer, Integer> rune : teleport.getRunes().entrySet())
		{
			if (count(rune.getKey()) + pouchRunes.getOrDefault(rune.getKey(), 0) < rune.getValue())
			{
				return false;
			}
		}
		return true;
	}
}
