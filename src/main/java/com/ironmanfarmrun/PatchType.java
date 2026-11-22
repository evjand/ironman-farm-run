package com.ironmanfarmrun;

import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PatchType
{
	HERB("Herb seeds", 20, PatchStateTables.HERB, 1, RunItems.HERB_SEEDS),
	ALLOTMENT("Allotment seeds", 10, PatchStateTables.ALLOTMENT, 3, RunItems.ALLOTMENT_SEEDS),
	FLOWER("Flower seeds", 5, PatchStateTables.FLOWER, 1, RunItems.FLOWER_SEEDS);

	private final String seedLabel;
	/**
	 * Minutes between growth ticks.
	 */
	private final int tickMinutes;
	/**
	 * What each varbit value means; see {@link PatchStateTables}.
	 */
	private final int[] stateTable;
	private final int seedsPerPatch;
	/**
	 * Any of these can be planted, so the player picks what to grow.
	 */
	private final List<Integer> seeds;
}
