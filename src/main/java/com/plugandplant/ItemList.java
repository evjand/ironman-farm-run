package com.plugandplant;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import lombok.EqualsAndHashCode;
import net.runelite.client.util.WildcardMatcher;

/**
 * Items the player has listed in the settings, by name or item id, separated by commas or new lines.
 * Names ignore case, and * in one matches any text, so "Ring of dueling*" covers every charge.
 */
@EqualsAndHashCode
final class ItemList
{
	static final ItemList EMPTY = new ItemList(Set.of(), List.of());

	private final Set<Integer> ids;
	private final List<String> names;

	private ItemList(Set<Integer> ids, List<String> names)
	{
		this.ids = ids;
		this.names = names;
	}

	static ItemList parse(@Nullable String text)
	{
		if (text == null || text.isBlank())
		{
			return EMPTY;
		}

		Set<Integer> ids = new HashSet<>();
		List<String> names = new ArrayList<>();
		for (String entry : text.split("[,\\n]"))
		{
			String trimmed = entry.trim();
			if (trimmed.isEmpty())
			{
				continue;
			}
			if (trimmed.matches("\\d+"))
			{
				ids.add(Integer.parseInt(trimmed));
			}
			else
			{
				names.add(trimmed);
			}
		}
		return new ItemList(Set.copyOf(ids), List.copyOf(names));
	}

	boolean isEmpty()
	{
		return ids.isEmpty() && names.isEmpty();
	}

	/**
	 * @param name the item's name, looked up only when the list has names to compare it with
	 */
	boolean contains(int itemId, Supplier<String> name)
	{
		if (ids.contains(itemId))
		{
			return true;
		}
		if (names.isEmpty())
		{
			return false;
		}
		String itemName = name.get();
		if (itemName == null)
		{
			return false;
		}
		for (String pattern : names)
		{
			if (WildcardMatcher.matches(pattern, itemName))
			{
				return true;
			}
		}
		return false;
	}
}
