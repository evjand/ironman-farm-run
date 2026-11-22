package com.plugandplant;

import java.util.List;
import javax.annotation.Nullable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PatchType
{
	HERB("Herb seeds", 20, PatchStateTables.HERB, null, 1, RunItems.HERB_SEEDS),
	ALLOTMENT("Allotment seeds", 10, PatchStateTables.ALLOTMENT, null, 3, RunItems.ALLOTMENT_SEEDS),
	FLOWER("Flower seeds", 5, PatchStateTables.FLOWER, null, 1, RunItems.FLOWER_SEEDS),
	TREE("Tree saplings", 40, PatchStateTables.TREE, PatchStateTables.TREE_CROPS, 1, RunItems.TREE_SAPLINGS),
	FRUIT_TREE("Fruit tree saplings", 160, PatchStateTables.FRUIT_TREE, PatchStateTables.FRUIT_TREE_CROPS, 1, RunItems.FRUIT_TREE_SAPLINGS);

	private final String seedLabel;
	/**
	 * Minutes between growth ticks.
	 */
	private final int tickMinutes;
	/**
	 * What each varbit value means; see {@link PatchStateTables}.
	 */
	private final int[] stateTable;
	/**
	 * Which tree grows at each varbit value; null for patches that aren't trees.
	 */
	@Nullable
	private final Sapling[] crops;
	private final int seedsPerPatch;
	/**
	 * Any of these can be planted, so the player picks what to grow.
	 */
	private final List<Integer> seeds;

	boolean isTree()
	{
		return crops != null;
	}

	/**
	 * What a crop is once it has finished growing: ready to harvest, or for a tree, waiting for its
	 * health check.
	 */
	PatchStatus grownStatus()
	{
		return isTree() ? PatchStatus.CHECK_HEALTH : PatchStatus.READY;
	}
}
