package com.plugandplant;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class PatchDecoderTest
{
	// 1,000,000 is 400 seconds into both a 20 and a 10 minute growth tick, which started at 999,600
	private static final long SEEN_AT = 1_000_000;
	private static final long SEEN_TICK = 999_600;
	private static final long MINUTE = 60;

	private static final int GROWING = 0;
	private static final int HARVESTABLE = 1;
	private static final int DISEASED = 2;
	private static final int DEAD = 3;
	private static final int WEEDS = 4;
	private static final int RAKED = 5;
	private static final int UNUSED = 6;
	/**
	 * Every herb varbit range from Time Tracking's PatchImplementation.HERB: {first, last, kind}.
	 * Herbs have four growing values, so a growing range's first value has four growth ticks to go.
	 */
	private static final int[][] HERB_RANGES = {
		{0, 2, WEEDS}, {3, 3, RAKED},
		{4, 7, GROWING}, {8, 10, HARVESTABLE},
		{11, 14, GROWING}, {15, 17, HARVESTABLE},
		{18, 21, GROWING}, {22, 24, HARVESTABLE},
		{25, 28, GROWING}, {29, 31, HARVESTABLE},
		{32, 35, GROWING}, {36, 38, HARVESTABLE},
		{39, 42, GROWING}, {43, 45, HARVESTABLE},
		{46, 49, GROWING}, {50, 52, HARVESTABLE},
		{53, 56, GROWING}, {57, 59, HARVESTABLE},
		{60, 63, GROWING}, {64, 66, HARVESTABLE},
		{67, 67, WEEDS},
		{68, 71, GROWING}, {72, 74, HARVESTABLE},
		{75, 78, GROWING}, {79, 81, HARVESTABLE},
		{82, 85, GROWING}, {86, 88, HARVESTABLE},
		{89, 92, GROWING}, {93, 95, HARVESTABLE},
		{96, 99, GROWING}, {100, 102, HARVESTABLE},
		{103, 106, GROWING}, {107, 109, HARVESTABLE},
		{110, 127, UNUSED},
		{128, 169, DISEASED},
		{170, 172, DEAD},
		{173, 175, DISEASED},
		{176, 191, WEEDS},
		{192, 195, GROWING}, {196, 197, HARVESTABLE},
		{198, 200, DISEASED},
		{201, 203, DEAD},
		{204, 219, WEEDS},
		{220, 220, UNUSED},
		{221, 255, WEEDS},
	};

	@Test
	public void everyHerbVarbitValueDecodesAsTimeTrackingDoes()
	{
		int next = 0;
		for (int[] range : HERB_RANGES)
		{
			assertEquals("ranges must be contiguous", next, range[0]);
			for (int value = range[0]; value <= range[1]; value++)
			{
				PatchState state = predict(PatchType.HERB, value + ":" + SEEN_AT, SEEN_AT);
				String message = "value " + value;
				if (range[2] == GROWING)
				{
					int ticksLeft = 4 - (value - range[0]);
					assertEquals(message, new PatchState(PatchStatus.GROWING, SEEN_TICK + ticksLeft * 20 * MINUTE), state);
				}
				else
				{
					assertEquals(message, new PatchState(expectedStatus(range[2]), 0), state);
				}
			}
			next = range[1] + 1;
		}
		assertEquals(256, next);
	}

	private static PatchStatus expectedStatus(int kind)
	{
		switch (kind)
		{
			case HARVESTABLE:
				return PatchStatus.READY;
			case DISEASED:
				return PatchStatus.DISEASED;
			case DEAD:
				return PatchStatus.DEAD;
			case WEEDS:
				return PatchStatus.WEEDS;
			case RAKED:
				return PatchStatus.EMPTY;
			default:
				return PatchStatus.UNKNOWN;
		}
	}

	@Test
	public void growingCropTurnsReadyAtItsLastGrowthTick()
	{
		long readyAt = SEEN_TICK + 4 * 20 * MINUTE;
		// 32 is a ranarr at growth stage 0
		assertEquals(new PatchState(PatchStatus.GROWING, readyAt), predict(PatchType.HERB, "32:" + SEEN_AT, readyAt - 1));
		assertEquals(new PatchState(PatchStatus.READY, 0), predict(PatchType.HERB, "32:" + SEEN_AT, readyAt));
	}

	@Test
	public void allotmentsGrowInTenMinuteTicksAndCropsDifferInStages()
	{
		// 6 is a freshly planted potato, which has four growth ticks to go
		assertEquals(new PatchState(PatchStatus.GROWING, SEEN_TICK + 4 * 10 * MINUTE), predict(PatchType.ALLOTMENT, "6:" + SEEN_AT, SEEN_AT));
		// 52 is a freshly planted watermelon, which has eight
		assertEquals(new PatchState(PatchStatus.GROWING, SEEN_TICK + 8 * 10 * MINUTE), predict(PatchType.ALLOTMENT, "52:" + SEEN_AT, SEEN_AT));
		assertEquals(new PatchState(PatchStatus.GROWING, SEEN_TICK + 10 * MINUTE), predict(PatchType.ALLOTMENT, "59:" + SEEN_AT, SEEN_AT));

		assertEquals(PatchStatus.READY, predict(PatchType.ALLOTMENT, "10:" + SEEN_AT, SEEN_AT).getStatus());
		assertEquals(PatchStatus.READY, predict(PatchType.ALLOTMENT, "60:" + SEEN_AT, SEEN_AT).getStatus());
		assertEquals(PatchStatus.DISEASED, predict(PatchType.ALLOTMENT, "181:" + SEEN_AT, SEEN_AT).getStatus());
		assertEquals(PatchStatus.DEAD, predict(PatchType.ALLOTMENT, "245:" + SEEN_AT, SEEN_AT).getStatus());
		assertEquals(PatchStatus.WEEDS, predict(PatchType.ALLOTMENT, "0:" + SEEN_AT, SEEN_AT).getStatus());
		assertEquals(PatchStatus.EMPTY, predict(PatchType.ALLOTMENT, "3:" + SEEN_AT, SEEN_AT).getStatus());
	}

	@Test
	public void flowersGrowInFiveMinuteTicks()
	{
		// 1,000,000 is 100 seconds into a 5 minute growth tick that started at 999,900; 8 is a freshly planted marigold
		assertEquals(new PatchState(PatchStatus.GROWING, 999_900 + 4 * 5 * MINUTE), predict(PatchType.FLOWER, "8:" + SEEN_AT, SEEN_AT));
		assertEquals(PatchStatus.READY, predict(PatchType.FLOWER, "12:" + SEEN_AT, SEEN_AT).getStatus());
		assertEquals(PatchStatus.DISEASED, predict(PatchType.FLOWER, "137:" + SEEN_AT, SEEN_AT).getStatus());
		assertEquals(PatchStatus.DEAD, predict(PatchType.FLOWER, "201:" + SEEN_AT, SEEN_AT).getStatus());
	}

	@Test
	public void growthTicksShiftByTheAccountsTickOffset()
	{
		// A 5 minute offset moves every growth tick 300 seconds earlier
		PatchState state = PatchDecoder.predict(PatchType.HERB, "32:" + SEEN_AT, SEEN_AT, 20, 5);
		assertEquals(new PatchState(PatchStatus.GROWING, SEEN_TICK - 300 + 4 * 20 * MINUTE), state);
	}

	@Test
	public void tickOffsetTooImpreciseForTheCropIsIgnored()
	{
		PatchState state = PatchDecoder.predict(PatchType.HERB, "32:" + SEEN_AT, SEEN_AT, 5, 3);
		assertEquals(new PatchState(PatchStatus.GROWING, SEEN_TICK + 4 * 20 * MINUTE), state);
	}

	@Test
	public void missingOrMalformedDataIsUnknown()
	{
		assertEquals(PatchState.UNKNOWN, predict(PatchType.HERB, null, SEEN_AT));
		assertEquals(PatchState.UNKNOWN, predict(PatchType.HERB, "", SEEN_AT));
		assertEquals(PatchState.UNKNOWN, predict(PatchType.HERB, "32", SEEN_AT));
		assertEquals(PatchState.UNKNOWN, predict(PatchType.HERB, "x:y", SEEN_AT));
		assertEquals(PatchState.UNKNOWN, predict(PatchType.HERB, "32:0", SEEN_AT));
		assertEquals(PatchState.UNKNOWN, predict(PatchType.HERB, "256:" + SEEN_AT, SEEN_AT));
		assertEquals(PatchState.UNKNOWN, predict(PatchType.HERB, "-1:" + SEEN_AT, SEEN_AT));
	}

	private static PatchState predict(PatchType type, String stored, long now)
	{
		return PatchDecoder.predict(type, stored, now, null, null);
	}

	@Test
	public void treeGrowsToItsHealthCheckThenStandsUntilRemoved()
	{
		// 1,000,000 is 1,600 seconds into a 40 minute growth tick that started at 998,400; 8 is a freshly planted oak
		long treeTick = 998_400;
		assertEquals(new PatchState(PatchStatus.GROWING, treeTick + 4 * 40 * MINUTE, Sapling.OAK),
			predict(PatchType.TREE, "8:" + SEEN_AT, SEEN_AT));
		assertEquals(new PatchState(PatchStatus.CHECK_HEALTH, 0, Sapling.OAK),
			predict(PatchType.TREE, "8:" + SEEN_AT, treeTick + 4 * 40 * MINUTE));
		assertEquals(new PatchState(PatchStatus.CHECK_HEALTH, 0, Sapling.OAK), predict(PatchType.TREE, "12:" + SEEN_AT, SEEN_AT));
		assertEquals(new PatchState(PatchStatus.GROWN, 0, Sapling.OAK), predict(PatchType.TREE, "13:" + SEEN_AT, SEEN_AT));
		assertEquals(new PatchState(PatchStatus.STUMP, 0, Sapling.OAK), predict(PatchType.TREE, "14:" + SEEN_AT, SEEN_AT));
		assertEquals(new PatchState(PatchStatus.CHECK_HEALTH, 0, Sapling.MAGIC), predict(PatchType.TREE, "60:" + SEEN_AT, SEEN_AT));
		assertEquals(PatchStatus.DISEASED, predict(PatchType.TREE, "113:" + SEEN_AT, SEEN_AT).getStatus());
		assertEquals(PatchStatus.DEAD, predict(PatchType.TREE, "177:" + SEEN_AT, SEEN_AT).getStatus());
		assertEquals(new PatchState(PatchStatus.WEEDS, 0), predict(PatchType.TREE, "0:" + SEEN_AT, SEEN_AT));
	}

	@Test
	public void fruitTreeWithFruitCountsAsGrown()
	{
		// 34 is an apple tree waiting for its health check; 14 to 20 are apple trees with 0 to 6 apples
		assertEquals(new PatchState(PatchStatus.CHECK_HEALTH, 0, Sapling.APPLE), predict(PatchType.FRUIT_TREE, "34:" + SEEN_AT, SEEN_AT));
		for (int value = 14; value <= 20; value++)
		{
			assertEquals(new PatchState(PatchStatus.GROWN, 0, Sapling.APPLE), predict(PatchType.FRUIT_TREE, value + ":" + SEEN_AT, SEEN_AT));
		}
		assertEquals(new PatchState(PatchStatus.STUMP, 0, Sapling.APPLE), predict(PatchType.FRUIT_TREE, "33:" + SEEN_AT, SEEN_AT));
		assertEquals(new PatchState(PatchStatus.CHECK_HEALTH, 0, Sapling.DRAGONFRUIT), predict(PatchType.FRUIT_TREE, "253:" + SEEN_AT, SEEN_AT));
		// A fruit tree takes six 160 minute ticks
		assertEquals(PatchStatus.GROWING, predict(PatchType.FRUIT_TREE, "8:" + SEEN_AT, SEEN_AT).getStatus());
	}
}
