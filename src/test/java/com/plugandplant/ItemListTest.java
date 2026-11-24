package com.plugandplant;

import java.util.List;
import net.runelite.api.gameval.ItemID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class ItemListTest
{
	@Test
	public void namesAndIdsAcrossCommasAndLines()
	{
		ItemList list = ItemList.parse(" Ring of dueling* ,\n" + ItemID.ECTOPHIAL + "\n\nexplorer's ring 4,");

		assertTrue(list.contains(ItemID.RING_OF_DUELING_8, () -> "Ring of dueling(8)"));
		assertTrue(list.contains(ItemID.ECTOPHIAL, () -> "Ectophial"));
		assertTrue(list.contains(1, () -> "Explorer's ring 4"));
		assertFalse(list.contains(2, () -> "Explorer's ring 3"));
	}

	@Test
	public void namesAreOnlyLookedUpWhenNeeded()
	{
		ItemList ids = ItemList.parse("123");

		assertFalse(ids.contains(456, () ->
		{
			throw new AssertionError("looked up a name for a list of ids");
		}));
	}

	@Test
	public void blankIsEmpty()
	{
		assertTrue(ItemList.parse(" ,\n ").isEmpty());
		assertTrue(ItemList.parse(null).isEmpty());
		assertEquals(ItemList.parse("a, 1"), ItemList.parse("a\n1"));
	}

	@Test
	public void listedItemsGetTheirOwnSection()
	{
		BankFilterSpec spec = new BankFilterSpec(java.util.Map.of(BankSection.EQUIPMENT, List.of(ItemID.SPADE)))
			.with(BankSection.ALWAYS_SHOWN, List.of(ItemID.ECTOPHIAL));

		assertEquals(BankSection.ALWAYS_SHOWN, spec.sectionOf(ItemID.ECTOPHIAL));
		assertEquals(BankSection.EQUIPMENT, spec.sectionOf(ItemID.SPADE));
	}
}
