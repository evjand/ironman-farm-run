package com.plugandplant;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import lombok.Value;

/**
 * A snapshot of what the player has on them, and of what they keep with the tool leprechaun.
 */
@Value
class Carried
{
	static final Carried NOTHING = new Carried(Map.of(), Map.of());

	/**
	 * Item id to quantity, over the inventory, worn equipment and a seed box whose contents are known.
	 */
	Map<Integer, Integer> items;
	/**
	 * Rune item id to quantity, for the runes in a carried rune pouch.
	 */
	Map<Integer, Integer> pouchRunes;
	/**
	 * Runes the equipped staff or tome supplies without end.
	 */
	Set<Integer> unlimitedRunes;
	/**
	 * Item id to quantity, for the tools and compost stored with the tool leprechaun.
	 */
	Map<Integer, Integer> stored;
	/**
	 * Whether the player carries a seed box the game hasn't shown the contents of.
	 */
	boolean seedBoxUnknown;
	/**
	 * Item id to quantity, for the inventory's items that aren't noted. {@link #items} counts noted
	 * items as the items themselves, so it can't tell.
	 */
	Map<Integer, Integer> unnoted;

	Carried(Map<Integer, Integer> items, Map<Integer, Integer> pouchRunes, Set<Integer> unlimitedRunes,
		Map<Integer, Integer> stored, boolean seedBoxUnknown, Map<Integer, Integer> unnoted)
	{
		this.items = items;
		this.pouchRunes = pouchRunes;
		this.unlimitedRunes = unlimitedRunes;
		this.stored = stored;
		this.seedBoxUnknown = seedBoxUnknown;
		this.unnoted = unnoted;
	}

	Carried(Map<Integer, Integer> items, Map<Integer, Integer> pouchRunes, Set<Integer> unlimitedRunes,
		Map<Integer, Integer> stored, boolean seedBoxUnknown)
	{
		this(items, pouchRunes, unlimitedRunes, stored, seedBoxUnknown, Map.of());
	}

	Carried(Map<Integer, Integer> items, Map<Integer, Integer> pouchRunes)
	{
		this(items, pouchRunes, Set.of(), Map.of(), false);
	}

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
	 * How many of the items the inventory holds unnoted.
	 */
	int unnoted(Collection<Integer> itemIds)
	{
		int total = 0;
		for (int itemId : itemIds)
		{
			total += unnoted.getOrDefault(itemId, 0);
		}
		return total;
	}

	int stored(int itemId)
	{
		return stored.getOrDefault(itemId, 0);
	}

	int stored(Collection<Integer> itemIds)
	{
		int total = 0;
		for (int itemId : itemIds)
		{
			total += stored(itemId);
		}
		return total;
	}

	/**
	 * Whether the player can cast or use the teleport with what they carry. Runes are counted in the
	 * inventory and rune pouch, combination runes count towards both their elements, and an equipped
	 * staff or tome covers its elements.
	 */
	boolean has(Teleport teleport)
	{
		return has(teleport, false);
	}

	/**
	 * @param itemsOnly whether only the teleport's items count, not runes to cast it
	 */
	boolean has(Teleport teleport, boolean itemsOnly)
	{
		if (hasAny(teleport.getItemIds()))
		{
			return true;
		}
		if (itemsOnly || !teleport.isSpell())
		{
			return false;
		}

		for (Map.Entry<Integer, Integer> rune : teleport.getRunes().entrySet())
		{
			if (!unlimitedRunes.contains(rune.getKey()) && runes(rune.getKey()) < rune.getValue())
			{
				return false;
			}
		}
		return true;
	}

	/**
	 * The runes of a kind the player carries, combination runes that stand in for it included.
	 */
	private int runes(int rune)
	{
		int total = count(rune) + pouchRunes.getOrDefault(rune, 0);
		for (Map.Entry<Integer, Set<Integer>> combination : Runes.COMBINATIONS.entrySet())
		{
			if (combination.getValue().contains(rune))
			{
				total += count(combination.getKey()) + pouchRunes.getOrDefault(combination.getKey(), 0);
			}
		}
		return total;
	}
}
