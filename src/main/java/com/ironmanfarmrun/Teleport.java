package com.ironmanfarmrun;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import lombok.Getter;
import net.runelite.api.gameval.ItemID;

/**
 * A way to get to a farming location that the player prepares for by bringing something: a teleport
 * item, the runes for a spell, or a way into their house. {@link #ANY} and {@link #NONE} are the two
 * choices that aren't a particular teleport.
 */
@Getter
public enum Teleport
{
	ANY("Any teleport item"),
	NONE("No teleport"),

	EXPLORERS_RING("Explorer's ring",
		ItemID.LUMBRIDGE_RING_MEDIUM, ItemID.LUMBRIDGE_RING_HARD, ItemID.LUMBRIDGE_RING_ELITE),
	FALADOR_TAB("Falador tab", ItemID.POH_TABLET_FALADORTELEPORT),
	FALADOR_SPELL("Falador spell", runes(ItemID.LAWRUNE, 1, ItemID.AIRRUNE, 3, ItemID.WATERRUNE, 1)),
	CATHERBY_TAB("Catherby tab", ItemID.LUNAR_TABLET_CATHERBY_TELEPORT),
	CATHERBY_SPELL("Catherby spell", runes(ItemID.LAWRUNE, 3, ItemID.ASTRALRUNE, 3, ItemID.WATERRUNE, 10)),
	CAMELOT_TAB("Camelot tab", ItemID.POH_TABLET_CAMELOTTELEPORT),
	CAMELOT_SPELL("Camelot spell", runes(ItemID.LAWRUNE, 1, ItemID.AIRRUNE, 5)),
	ARDOUGNE_CLOAK("Ardougne cloak",
		ItemID.ARDY_CAPE_MEDIUM, ItemID.ARDY_CAPE_HARD, ItemID.ARDY_CAPE_ELITE),
	ARDOUGNE_TAB("Ardougne tab", ItemID.POH_TABLET_ARDOUGNETELEPORT),
	ARDOUGNE_SPELL("Ardougne spell", runes(ItemID.LAWRUNE, 2, ItemID.WATERRUNE, 2)),
	ECTOPHIAL("Ectophial", ItemID.ECTOPHIAL, ItemID.ECTOPHIAL_EMPTY),
	FENKENSTRAIN_TAB("Fenkenstrain tab", ItemID.TELETAB_FENK),
	FENKENSTRAIN_SPELL("Fenkenstrain spell", runes(ItemID.LAWRUNE, 1, ItemID.SOULRUNE, 1, ItemID.EARTHRUNE, 1)),
	XERICS_TALISMAN("Xeric's talisman", ItemID.XERIC_TALISMAN),
	HOSIDIUS_TAB("Hosidius tab", ItemID.NZONE_TELETAB_KOUREND),
	SKILLS_NECKLACE("Skills necklace",
		ItemID.JEWL_NECKLACE_OF_SKILLS_1, ItemID.JEWL_NECKLACE_OF_SKILLS_2, ItemID.JEWL_NECKLACE_OF_SKILLS_3,
		ItemID.JEWL_NECKLACE_OF_SKILLS_4, ItemID.JEWL_NECKLACE_OF_SKILLS_5, ItemID.JEWL_NECKLACE_OF_SKILLS_6),
	FARMING_CAPE("Farming cape", ItemID.SKILLCAPE_FARMING, ItemID.SKILLCAPE_FARMING_TRIMMED),
	BATTLEFRONT_SPELL("Battlefront spell", runes(ItemID.LAWRUNE, 1, ItemID.EARTHRUNE, 1, ItemID.FIRERUNE, 1)),
	QUETZAL_WHISTLE("Quetzal whistle",
		ItemID.HG_QUETZALWHISTLE_BASIC, ItemID.HG_QUETZALWHISTLE_ENHANCED, ItemID.HG_QUETZALWHISTLE_PERFECTED),
	CIVITAS_TAB("Civitas tab", ItemID.POH_TABLET_FORTISTELEPORT),
	CIVITAS_SPELL("Civitas spell", runes(ItemID.LAWRUNE, 2, ItemID.EARTHRUNE, 1, ItemID.FIRERUNE, 1)),
	STONY_BASALT("Stony basalt", ItemID.STRONGHOLD_TELEPORT_BASALT),
	TROLLHEIM_TAB("Trollheim tab", ItemID.NZONE_TELETAB_TROLLHEIM),
	TROLLHEIM_SPELL("Trollheim spell", runes(ItemID.LAWRUNE, 2, ItemID.FIRERUNE, 2)),
	ICY_BASALT("Icy basalt", ItemID.WEISS_TELEPORT_BASALT),
	HARMONY_TAB("Harmony tab", ItemID.TELETAB_HARMONY),
	HARMONY_SPELL("Harmony spell", runes(ItemID.LAWRUNE, 1, ItemID.NATURERUNE, 1, ItemID.SOULRUNE, 1)),

	// Ways into the player's house, to go on from there with its jewellery box, portals, fairy ring or spirit tree
	HOUSE_TAB("House: tab", true, Set.of(ItemID.POH_TABLET_TELEPORTTOHOUSE), Map.of()),
	HOUSE_SPELL("House: spell", true, Set.of(), runes(ItemID.LAWRUNE, 1, ItemID.AIRRUNE, 1, ItemID.EARTHRUNE, 1)),
	HOUSE_CAPE("House: Construction cape", true,
		Set.of(ItemID.SKILLCAPE_CONSTRUCTION, ItemID.SKILLCAPE_CONSTRUCTION_TRIMMED), Map.of());

	/**
	 * Hold runes for spells, so they are worth showing next to them in the bank.
	 */
	static final Set<Integer> RUNE_POUCHES = Set.of(
		ItemID.BH_RUNE_POUCH, ItemID.BH_RUNE_POUCH_TROUVER, ItemID.DIVINE_RUNE_POUCH, ItemID.DIVINE_RUNE_POUCH_TROUVER);

	/**
	 * Every item that plays a part in some teleport.
	 */
	static final Set<Integer> ALL_ITEM_IDS;

	static
	{
		Set<Integer> all = new HashSet<>();
		for (Teleport teleport : values())
		{
			all.addAll(teleport.bankItemIds());
		}
		ALL_ITEM_IDS = Set.copyOf(all);
	}

	private final String label;
	/**
	 * Goes to the player's house rather than to a patch.
	 */
	private final boolean viaHouse;
	/**
	 * Any one of these is this teleport, such as each charge of a necklace. Empty for a spell.
	 */
	private final Set<Integer> itemIds;
	/**
	 * Rune id to the amount one cast takes. Empty unless this is a spell.
	 */
	private final Map<Integer, Integer> runes;

	Teleport(String label, Integer... itemIds)
	{
		this(label, false, Set.of(itemIds), Map.of());
	}

	Teleport(String label, Map<Integer, Integer> runes)
	{
		this(label, false, Set.of(), runes);
	}

	Teleport(String label, boolean viaHouse, Set<Integer> itemIds, Map<Integer, Integer> runes)
	{
		this.label = label;
		this.viaHouse = viaHouse;
		this.itemIds = itemIds;
		this.runes = runes;
	}

	boolean isSpell()
	{
		return !runes.isEmpty();
	}

	/**
	 * The items to show in the bank for this teleport.
	 */
	Set<Integer> bankItemIds()
	{
		if (!isSpell())
		{
			return itemIds;
		}

		Set<Integer> ids = new HashSet<>(runes.keySet());
		ids.addAll(RUNE_POUCHES);
		return ids;
	}

	@Override
	public String toString()
	{
		return label;
	}

	/**
	 * @param idsAndAmounts rune id, amount, rune id, amount, ...
	 */
	private static Map<Integer, Integer> runes(int... idsAndAmounts)
	{
		Map<Integer, Integer> runes = new LinkedHashMap<>();
		for (int i = 0; i < idsAndAmounts.length; i += 2)
		{
			runes.put(idsAndAmounts[i], idsAndAmounts[i + 1]);
		}
		return runes;
	}
}
