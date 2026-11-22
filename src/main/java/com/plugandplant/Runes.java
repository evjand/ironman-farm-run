package com.plugandplant;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import net.runelite.api.gameval.ItemID;

/**
 * What stands in for elemental runes: combination runes, and staves and tomes that supply them.
 */
final class Runes
{
	/**
	 * Combination rune to the two elemental runes it counts as.
	 */
	static final Map<Integer, Set<Integer>> COMBINATIONS = Map.of(
		ItemID.MISTRUNE, Set.of(ItemID.AIRRUNE, ItemID.WATERRUNE),
		ItemID.DUSTRUNE, Set.of(ItemID.AIRRUNE, ItemID.EARTHRUNE),
		ItemID.SMOKERUNE, Set.of(ItemID.AIRRUNE, ItemID.FIRERUNE),
		ItemID.MUDRUNE, Set.of(ItemID.WATERRUNE, ItemID.EARTHRUNE),
		ItemID.STEAMRUNE, Set.of(ItemID.WATERRUNE, ItemID.FIRERUNE),
		ItemID.LAVARUNE, Set.of(ItemID.EARTHRUNE, ItemID.FIRERUNE)
	);

	/**
	 * Staff or tome to the runes it supplies while equipped. Ornamented and other variants of these
	 * are mapped to them before looking up.
	 */
	private static final Map<Integer, Set<Integer>> SUPPLIERS = new HashMap<>();

	static
	{
		supplies(Set.of(ItemID.AIRRUNE),
			ItemID.STAFF_OF_AIR, ItemID.AIR_BATTLESTAFF, ItemID.MYSTIC_AIR_STAFF);
		supplies(Set.of(ItemID.WATERRUNE),
			ItemID.STAFF_OF_WATER, ItemID.WATER_BATTLESTAFF, ItemID.MYSTIC_WATER_STAFF, ItemID.KODAI_WAND, ItemID.TOME_OF_WATER);
		supplies(Set.of(ItemID.EARTHRUNE),
			ItemID.STAFF_OF_EARTH, ItemID.EARTH_BATTLESTAFF, ItemID.MYSTIC_EARTH_STAFF, ItemID.TOME_OF_EARTH);
		supplies(Set.of(ItemID.FIRERUNE),
			ItemID.STAFF_OF_FIRE, ItemID.FIRE_BATTLESTAFF, ItemID.MYSTIC_FIRE_STAFF, ItemID.TOME_OF_FIRE);
		supplies(Set.of(ItemID.AIRRUNE, ItemID.WATERRUNE), ItemID.MIST_BATTLESTAFF, ItemID.MYSTIC_MIST_BATTLESTAFF);
		supplies(Set.of(ItemID.AIRRUNE, ItemID.EARTHRUNE), ItemID.DUST_BATTLESTAFF, ItemID.MYSTIC_DUST_BATTLESTAFF);
		supplies(Set.of(ItemID.AIRRUNE, ItemID.FIRERUNE), ItemID.SMOKE_BATTLESTAFF, ItemID.MYSTIC_SMOKE_BATTLESTAFF);
		supplies(Set.of(ItemID.WATERRUNE, ItemID.EARTHRUNE), ItemID.MUD_BATTLESTAFF, ItemID.MYSTIC_MUD_STAFF);
		supplies(Set.of(ItemID.WATERRUNE, ItemID.FIRERUNE), ItemID.STEAM_BATTLESTAFF, ItemID.MYSTIC_STEAM_BATTLESTAFF,
			ItemID.TWINFLAME_STAFF);
		supplies(Set.of(ItemID.EARTHRUNE, ItemID.FIRERUNE), ItemID.LAVA_BATTLESTAFF, ItemID.MYSTIC_LAVA_STAFF);
	}

	private Runes()
	{
	}

	private static void supplies(Set<Integer> runes, int... itemIds)
	{
		for (int itemId : itemIds)
		{
			SUPPLIERS.put(itemId, runes);
		}
	}

	/**
	 * The runes an equipped item supplies.
	 *
	 * @param baseItemId the item, mapped from any variant to its base item
	 */
	static Set<Integer> suppliedBy(int baseItemId)
	{
		return SUPPLIERS.getOrDefault(baseItemId, Set.of());
	}
}
