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

	static final List<Integer> TREE_SAPLINGS = List.of(
		ItemID.PLANTPOT_OAK_SAPLING,
		ItemID.PLANTPOT_WILLOW_SAPLING,
		ItemID.PLANTPOT_MAPLE_SAPLING,
		ItemID.PLANTPOT_YEW_SAPLING,
		ItemID.PLANTPOT_MAGIC_TREE_SAPLING
	);

	static final List<Integer> FRUIT_TREE_SAPLINGS = List.of(
		ItemID.PLANTPOT_APPLE_SAPLING,
		ItemID.PLANTPOT_BANANA_SAPLING,
		ItemID.PLANTPOT_ORANGE_SAPLING,
		ItemID.PLANTPOT_CURRY_SAPLING,
		ItemID.PLANTPOT_PINEAPPLE_SAPLING,
		ItemID.PLANTPOT_PAPAYA_SAPLING,
		ItemID.PLANTPOT_PALM_SAPLING,
		ItemID.PLANTPOT_DRAGONFRUIT_SAPLING
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
	 * Tools and consumables a tree run uses on top of the shared ones: coins to have a gardener
	 * remove a grown tree.
	 */
	static final List<Integer> TREE_EQUIPMENT = List.of(
		ItemID.COINS
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
