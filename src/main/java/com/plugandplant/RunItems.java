package com.plugandplant;

import java.util.List;
import java.util.Set;
import net.runelite.api.gameval.ItemID;

/**
 * The items a farm run uses. Lists are in the order the bank filter shows them.
 */
final class RunItems
{
	static final List<Integer> HERB_SEEDS = List.of(
		ItemID.GUAM_SEED,
		ItemID.MARRENTILL_SEED,
		ItemID.TARROMIN_SEED,
		ItemID.HARRALANDER_SEED,
		ItemID.RANARR_SEED,
		ItemID.TOADFLAX_SEED,
		ItemID.IRIT_SEED,
		ItemID.AVANTOE_SEED,
		ItemID.KWUARM_SEED,
		ItemID.HUASCA_SEED,
		ItemID.SNAPDRAGON_SEED,
		ItemID.CADANTINE_SEED,
		ItemID.LANTADYME_SEED,
		ItemID.DWARF_WEED_SEED,
		ItemID.TORSTOL_SEED
	);

	static final List<Integer> ALLOTMENT_SEEDS = List.of(
		ItemID.POTATO_SEED,
		ItemID.ONION_SEED,
		ItemID.CABBAGE_SEED,
		ItemID.TOMATO_SEED,
		ItemID.SWEETCORN_SEED,
		ItemID.STRAWBERRY_SEED,
		ItemID.WATERMELON_SEED,
		ItemID.SNAPE_GRASS_SEED
	);

	static final List<Integer> FLOWER_SEEDS = List.of(
		ItemID.MARIGOLD_SEED,
		ItemID.ROSEMARY_SEED,
		ItemID.NASTURTIUM_SEED,
		ItemID.WOAD_SEED,
		ItemID.LIMPWURT_SEED,
		ItemID.WHITE_LILY_SEED
	);

	/**
	 * Saplings, then watered seedlings: a watered seedling grows into a sapling in a few minutes, so
	 * it will almost certainly be one by the time it reaches a patch, and counts as one.
	 */
	static final List<Integer> TREE_SAPLINGS = List.of(
		ItemID.PLANTPOT_OAK_SAPLING,
		ItemID.PLANTPOT_WILLOW_SAPLING,
		ItemID.PLANTPOT_MAPLE_SAPLING,
		ItemID.PLANTPOT_YEW_SAPLING,
		ItemID.PLANTPOT_MAGIC_TREE_SAPLING,
		ItemID.PLANTPOT_ACORN_WATERED,
		ItemID.PLANTPOT_WILLOW_SEED_WATERED,
		ItemID.PLANTPOT_MAPLE_SEED_WATERED,
		ItemID.PLANTPOT_YEW_SEED_WATERED,
		ItemID.PLANTPOT_MAGIC_TREE_SEED_WATERED
	);

	static final List<Integer> FRUIT_TREE_SAPLINGS = List.of(
		ItemID.PLANTPOT_APPLE_SAPLING,
		ItemID.PLANTPOT_BANANA_SAPLING,
		ItemID.PLANTPOT_ORANGE_SAPLING,
		ItemID.PLANTPOT_CURRY_SAPLING,
		ItemID.PLANTPOT_PINEAPPLE_SAPLING,
		ItemID.PLANTPOT_PAPAYA_SAPLING,
		ItemID.PLANTPOT_PALM_SAPLING,
		ItemID.PLANTPOT_DRAGONFRUIT_SAPLING,
		ItemID.PLANTPOT_APPLE_SEED_WATERED,
		ItemID.PLANTPOT_BANANA_SEED_WATERED,
		ItemID.PLANTPOT_ORANGE_SEED_WATERED,
		ItemID.PLANTPOT_CURRY_SEED_WATERED,
		ItemID.PLANTPOT_PINEAPPLE_SEED_WATERED,
		ItemID.PLANTPOT_PAPAYA_SEED_WATERED,
		ItemID.PLANTPOT_PALM_SEED_WATERED,
		ItemID.PLANTPOT_DRAGONFRUIT_SEED_WATERED
	);

	/**
	 * Compost that is used up one bucket per patch.
	 */
	static final List<Integer> COMPOST_BUCKETS = List.of(
		ItemID.BUCKET_ULTRACOMPOST,
		ItemID.BUCKET_SUPERCOMPOST,
		ItemID.BUCKET_COMPOST
	);

	static final Set<Integer> SEED_BOXES = Set.of(
		ItemID.SEED_BOX,
		ItemID.SEED_BOX_OPEN
	);

	/**
	 * The base item of each piece of the graceful outfit; recolours are variations of these.
	 */
	static final List<Integer> GRACEFUL_PIECES = List.of(
		ItemID.GRACEFUL_HOOD,
		ItemID.GRACEFUL_CAPE,
		ItemID.GRACEFUL_TOP,
		ItemID.GRACEFUL_LEGS,
		ItemID.GRACEFUL_GLOVES,
		ItemID.GRACEFUL_BOOTS
	);

	/**
	 * Axes, which take a grown tree down instead of paying its gardener.
	 */
	static final List<Integer> AXES = List.of(
		ItemID.BRONZE_AXE, ItemID.IRON_AXE, ItemID.STEEL_AXE, ItemID.BLACK_AXE, ItemID.MITHRIL_AXE, ItemID.ADAMANT_AXE,
		ItemID.RUNE_AXE, ItemID.DRAGON_AXE, ItemID.INFERNAL_AXE, ItemID.INFERNAL_AXE_EMPTY, ItemID.CRYSTAL_AXE,
		ItemID.CRYSTAL_AXE_INACTIVE, ItemID.TRAILBLAZER_AXE,
		ItemID.BRONZE_AXE_2H, ItemID.IRON_AXE_2H, ItemID.STEEL_AXE_2H, ItemID.BLACK_AXE_2H, ItemID.MITHRIL_AXE_2H,
		ItemID.ADAMANT_AXE_2H, ItemID.RUNE_AXE_2H, ItemID.DRAGON_AXE_2H, ItemID.CRYSTAL_AXE_2H
	);

	/**
	 * Tools and consumables a tree run uses on top of the shared ones: coins to have a gardener
	 * remove a grown tree.
	 */
	static final List<Integer> TREE_EQUIPMENT = List.of(
		ItemID.COINS
	);

	/**
	 * Tree and fruit tree seeds, the seedlings they become in a plant pot until watered, and the pots,
	 * trowel and watering cans it takes to grow them into saplings. Watered seedlings count as saplings.
	 */
	static final List<Integer> SAPLINGS_TO_GROW = List.of(
		ItemID.ACORN, ItemID.WILLOW_SEED, ItemID.MAPLE_SEED, ItemID.YEW_SEED, ItemID.MAGIC_TREE_SEED,
		ItemID.APPLE_TREE_SEED, ItemID.BANANA_TREE_SEED, ItemID.ORANGE_TREE_SEED, ItemID.CURRY_TREE_SEED,
		ItemID.PINEAPPLE_TREE_SEED, ItemID.PAPAYA_TREE_SEED, ItemID.PALM_TREE_SEED, ItemID.DRAGONFRUIT_TREE_SEED,
		ItemID.PLANTPOT_ACORN, ItemID.PLANTPOT_WILLOW_SEED, ItemID.PLANTPOT_MAPLE_SEED, ItemID.PLANTPOT_YEW_SEED,
		ItemID.PLANTPOT_MAGIC_TREE_SEED, ItemID.PLANTPOT_APPLE_SEED, ItemID.PLANTPOT_BANANA_SEED,
		ItemID.PLANTPOT_ORANGE_SEED, ItemID.PLANTPOT_CURRY_SEED, ItemID.PLANTPOT_PINEAPPLE_SEED,
		ItemID.PLANTPOT_PAPAYA_SEED, ItemID.PLANTPOT_PALM_SEED, ItemID.PLANTPOT_DRAGONFRUIT_SEED,
		ItemID.PLANTPOT_EMPTY, ItemID.PLANTPOT_COMPOST, ItemID.PLANTPOT_SUPERCOMPOST,
		ItemID.GARDENING_TROWEL, ItemID.ZEAH_WATERINGCAN,
		ItemID.WATERING_CAN_8, ItemID.WATERING_CAN_7, ItemID.WATERING_CAN_6, ItemID.WATERING_CAN_5,
		ItemID.WATERING_CAN_4, ItemID.WATERING_CAN_3, ItemID.WATERING_CAN_2, ItemID.WATERING_CAN_1,
		ItemID.WATERING_CAN_0
	);

	/**
	 * Tools and consumables, always part of the bank filter.
	 */
	static final List<Integer> EQUIPMENT = List.of(
		ItemID.SPADE,
		ItemID.DIBBER,
		ItemID.RAKE,
		ItemID.FAIRY_ENCHANTED_SECATEURS,
		ItemID.SECATEURS,
		ItemID.SEED_BOX,
		ItemID.SEED_BOX_OPEN,
		ItemID.BOTTOMLESS_COMPOST_BUCKET_FILLED,
		ItemID.BOTTOMLESS_COMPOST_BUCKET,
		ItemID.BUCKET_ULTRACOMPOST,
		ItemID.BUCKET_SUPERCOMPOST,
		ItemID.BUCKET_COMPOST,
		ItemID.PLANT_CURE,
		ItemID.SLAYER_HERB_SACK,
		ItemID.SLAYER_HERB_SACK_OPEN,
		ItemID.SLAYER_HERB_SACK_SILK,
		ItemID.SLAYER_HERB_SACK_SILK_OPEN
	);

	private RunItems()
	{
	}

}
