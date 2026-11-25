package com.plugandplant;

import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.gameval.ItemID;

@Getter
@RequiredArgsConstructor
public enum PatchType
{
	HERB("Herbs", "Herb patch", "Herb seeds", 20, PatchStateTables.HERB, null, 1, RunItems.HERB_SEEDS, List.of(), 1),
	ALLOTMENT("Allotments", "Allotment", "Allotment seeds", 10, PatchStateTables.ALLOTMENT, null, 3, RunItems.ALLOTMENT_SEEDS,
		List.of(), 1),
	FLOWER("Flowers", "Flower patch", "Flower seeds", 5, PatchStateTables.FLOWER, null, 1, RunItems.FLOWER_SEEDS, List.of(), 1),
	TREE("Trees", "Tree patch", "Tree saplings", 40, PatchStateTables.TREE, PatchStateTables.TREE_CROPS, 1,
		RunItems.TREE_SAPLINGS, RunItems.TREE_SEEDS, 1),
	FRUIT_TREE("Fruit trees", "Fruit tree patch", "Fruit tree saplings", 160, PatchStateTables.FRUIT_TREE,
		PatchStateTables.FRUIT_TREE_CROPS, 1, RunItems.FRUIT_TREE_SAPLINGS, RunItems.FRUIT_TREE_SEEDS, 1),
	HARDWOOD_TREE("Hardwood trees", "Hardwood patch", "Hardwood saplings", 640, PatchStateTables.HARDWOOD_TREE,
		PatchStateTables.HARDWOOD_TREE_CROPS, 1, RunItems.HARDWOOD_SAPLINGS, RunItems.HARDWOOD_SEEDS, 35),
	CALQUAT("Calquats", "Calquat patch", "Calquat saplings", 160, PatchStateTables.CALQUAT, PatchStateTables.CALQUAT_CROPS,
		1, RunItems.CALQUAT_SAPLINGS, RunItems.CALQUAT_SEEDS, 72),
	SPIRIT_TREE("Spirit trees", "Spirit tree patch", "Spirit saplings", 320, PatchStateTables.SPIRIT_TREE,
		PatchStateTables.SPIRIT_TREE_CROPS, 1, RunItems.SPIRIT_SAPLINGS, RunItems.SPIRIT_SEEDS, 83),
	CELASTRUS("Celastrus", "Celastrus patch", "Celastrus saplings", 160, PatchStateTables.CELASTRUS,
		PatchStateTables.CELASTRUS_CROPS, 1, RunItems.CELASTRUS_SAPLINGS, RunItems.CELASTRUS_SEEDS, 85),
	REDWOOD("Redwoods", "Redwood patch", "Redwood saplings", 640, PatchStateTables.REDWOOD, PatchStateTables.REDWOOD_CROPS,
		1, RunItems.REDWOOD_SAPLINGS, RunItems.REDWOOD_SEEDS, 90),
	CRYSTAL_TREE("Crystal trees", "Crystal tree patch", "Crystal saplings", 80, PatchStateTables.CRYSTAL_TREE,
		PatchStateTables.CRYSTAL_TREE_CROPS, 1, RunItems.CRYSTAL_SAPLINGS, RunItems.CRYSTAL_SEEDS, 74);

	/**
	 * What the side panel calls patches of this kind, such as "Fruit trees".
	 */
	private final String pluralName;
	/**
	 * What a patch of this kind is called when it is the only one at its location.
	 */
	private final String patchName;
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
	/**
	 * The seeds and unwatered seedlings that grow into this kind's saplings.
	 */
	private final List<Integer> saplingSeeds;
	/**
	 * The Farming level it takes to plant anything here; 1 for kinds whose lowest crop is low enough
	 * not to matter.
	 */
	private final int plantLevel;

	boolean isTree()
	{
		return crops != null;
	}

	/**
	 * Whether the patch is one of the special tree patches, beyond trees and fruit trees.
	 */
	boolean isSpecialTree()
	{
		return isTree() && this != TREE && this != FRUIT_TREE;
	}

	/**
	 * Whether a gardener can be paid to protect what grows here; crystal trees can't get diseased.
	 */
	boolean isProtectable()
	{
		return isTree() && this != CRYSTAL_TREE;
	}

	/**
	 * What a crop is once it has finished growing: ready to harvest, or for a tree, waiting for its
	 * health check.
	 */
	PatchStatus grownStatus()
	{
		return isTree() ? PatchStatus.CHECK_HEALTH : PatchStatus.READY;
	}

	/**
	 * Whether a grown tree here is taken down by paying the gardener (or chopping it). Calquats and
	 * spirit trees are cleared with a spade instead, and a crystal tree is gone once harvested.
	 */
	boolean isRemovedForCoins()
	{
		return isTree() && this != CALQUAT && this != SPIRIT_TREE && this != CRYSTAL_TREE;
	}

	/**
	 * What the patch yields that the tool leprechaun can note; nothing for trees.
	 */
	Set<Integer> produce()
	{
		switch (this)
		{
			case HERB:
				return RunItems.HERBS_TO_NOTE;
			case ALLOTMENT:
				return RunItems.ALLOTMENT_PRODUCE;
			case FLOWER:
				return RunItems.FLOWER_PRODUCE;
			default:
				return Set.of();
		}
	}

	/**
	 * What taking the crop takes: an axe for celastrus bark and crystal shards, a spade for the rest.
	 */
	List<Integer> harvestItems()
	{
		return this == CELASTRUS || this == CRYSTAL_TREE ? RunItems.AXES : List.of(ItemID.SPADE);
	}

	/**
	 * What taking down a grown tree here takes.
	 */
	List<Integer> removalItems()
	{
		return isRemovedForCoins() ? List.of(ItemID.COINS) : List.of(ItemID.SPADE);
	}
}
