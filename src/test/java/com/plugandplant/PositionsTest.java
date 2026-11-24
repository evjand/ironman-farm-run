package com.plugandplant;

import net.runelite.api.coords.WorldPoint;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class PositionsTest
{
	@Test
	public void theCopyOfLletyaIsLletya()
	{
		// The fruit tree patch, as the game shows it in region 11103
		WorldPoint inCopy = new WorldPoint(2347 + 448, 3162 + 2944, 0);
		assertEquals(11103, inCopy.getRegionID());

		WorldPoint original = Positions.original(inCopy);

		assertEquals(new WorldPoint(2347, 3162, 0), original);
		assertTrue(FarmLocation.LLETYA.isNear(original, 0));
	}

	@Test
	public void elsewhereIsLeftAlone()
	{
		WorldPoint catherby = new WorldPoint(2860, 3433, 0);
		assertEquals(catherby, Positions.original(catherby));
	}
}
