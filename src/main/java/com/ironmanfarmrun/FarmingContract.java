package com.ironmanfarmrun;

import java.util.List;
import javax.annotation.Nullable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.gameval.ItemID;

/**
 * A Farming Guild contract for a crop grown in one of the patch types a run tends. Contracts for
 * trees, bushes and the like aren't part of a run and have no constant here.
 */
@Getter
@RequiredArgsConstructor
public enum FarmingContract
{
	GUAM(20, "Guam", PatchType.HERB, ItemID.GUAM_SEED),
	MARRENTILL(21, "Marrentill", PatchType.HERB, ItemID.MARRENTILL_SEED),
	TARROMIN(22, "Tarromin", PatchType.HERB, ItemID.TARROMIN_SEED),
	HARRALANDER(23, "Harralander", PatchType.HERB, ItemID.HARRALANDER_SEED),
	RANARR(24, "Ranarr", PatchType.HERB, ItemID.RANARR_SEED),
	TOADFLAX(25, "Toadflax", PatchType.HERB, ItemID.TOADFLAX_SEED),
	IRIT(26, "Irit", PatchType.HERB, ItemID.IRIT_SEED),
	AVANTOE(27, "Avantoe", PatchType.HERB, ItemID.AVANTOE_SEED),
	KWUARM(28, "Kwuarm", PatchType.HERB, ItemID.KWUARM_SEED),
	SNAPDRAGON(29, "Snapdragon", PatchType.HERB, ItemID.SNAPDRAGON_SEED),
	CADANTINE(30, "Cadantine", PatchType.HERB, ItemID.CADANTINE_SEED),
	LANTADYME(31, "Lantadyme", PatchType.HERB, ItemID.LANTADYME_SEED),
	DWARF_WEED(32, "Dwarf weed", PatchType.HERB, ItemID.DWARF_WEED_SEED),
	TORSTOL(33, "Torstol", PatchType.HERB, ItemID.TORSTOL_SEED),
	MARIGOLD(34, "Marigold", PatchType.FLOWER, ItemID.MARIGOLD_SEED),
	ROSEMARY(35, "Rosemary", PatchType.FLOWER, ItemID.ROSEMARY_SEED),
	NASTURTIUM(36, "Nasturtium", PatchType.FLOWER, ItemID.NASTURTIUM_SEED),
	WOAD(37, "Woad", PatchType.FLOWER, ItemID.WOAD_SEED),
	LIMPWURT(38, "Limpwurt", PatchType.FLOWER, ItemID.LIMPWURT_SEED),
	WHITE_LILY(39, "White lily", PatchType.FLOWER, ItemID.WHITE_LILY_SEED),
	POTATO(40, "Potato", PatchType.ALLOTMENT, ItemID.POTATO_SEED),
	ONION(41, "Onion", PatchType.ALLOTMENT, ItemID.ONION_SEED),
	CABBAGE(42, "Cabbage", PatchType.ALLOTMENT, ItemID.CABBAGE_SEED),
	TOMATO(43, "Tomato", PatchType.ALLOTMENT, ItemID.TOMATO_SEED),
	SWEETCORN(44, "Sweetcorn", PatchType.ALLOTMENT, ItemID.SWEETCORN_SEED),
	STRAWBERRY(45, "Strawberry", PatchType.ALLOTMENT, ItemID.STRAWBERRY_SEED),
	WATERMELON(46, "Watermelon", PatchType.ALLOTMENT, ItemID.WATERMELON_SEED),
	SNAPE_GRASS(47, "Snape grass", PatchType.ALLOTMENT, ItemID.SNAPE_GRASS_SEED);

	/**
	 * The value of the contract type varbit for this crop, as Time Tracking's Produce has it.
	 */
	private final int varbitValue;
	private final String name;
	private final PatchType type;
	private final int seed;

	@Nullable
	static FarmingContract fromVarbit(int value)
	{
		for (FarmingContract contract : values())
		{
			if (contract.varbitValue == value)
			{
				return contract;
			}
		}
		return null;
	}

	String seedLabel()
	{
		return name + " seed";
	}

	/**
	 * The guild patches to grow the crop in. Where the run tends that kind of patch anyway, all of
	 * them get the contract crop; otherwise one is enough to fulfil the contract.
	 */
	List<FarmPatch> patches(boolean typeIncluded)
	{
		List<FarmPatch> patches = FarmLocation.FARMING_GUILD.patches(type);
		return typeIncluded || patches.isEmpty() ? patches : patches.subList(0, 1);
	}
}
