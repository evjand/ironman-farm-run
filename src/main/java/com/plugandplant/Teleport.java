package com.plugandplant;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import lombok.Getter;
import net.runelite.api.gameval.ItemID;

/**
 * A way to get to a farming location that the player prepares for by bringing something, named after
 * the item or spell. A spell with a tablet of the same name is one teleport: either one will do.
 * {@link #ANY} and {@link #NONE} are the two choices that aren't a particular teleport.
 */
@Getter
public enum Teleport
{
	ANY("Any teleport item"),
	NONE("No teleport"),

	/**
	 * Any way into the player's house, to go on from there by its jewellery box, portals, fairy ring or spirit tree.
	 */
	HOUSE("House", items(ItemID.POH_TABLET_TELEPORTTOHOUSE, ItemID.SKILLCAPE_CONSTRUCTION, ItemID.SKILLCAPE_CONSTRUCTION_TRIMMED),
		runes(ItemID.LAWRUNE, 1, ItemID.AIRRUNE, 1, ItemID.EARTHRUNE, 1), true, false),
	/**
	 * Takes a dramen or lunar staff, unless the elite Lumbridge & Draynor diary is done, so going without isn't missing anything.
	 */
	FAIRY_RING("Fairy ring", items(ItemID.DRAMEN_STAFF, ItemID.LUNAR_MOONCLAN_LIMINAL_STAFF), Map.of(), false, true),

	// Spells, with their tablets
	FALADOR_TELEPORT("Falador Teleport", items(ItemID.POH_TABLET_FALADORTELEPORT),
		runes(ItemID.LAWRUNE, 1, ItemID.AIRRUNE, 3, ItemID.WATERRUNE, 1)),
	DRAYNOR_MANOR_TELEPORT("Draynor Manor Teleport", items(ItemID.TELETAB_DRAYNOR),
		runes(ItemID.LAWRUNE, 1, ItemID.EARTHRUNE, 1, ItemID.WATERRUNE, 1)),
	CATHERBY_TELEPORT("Catherby Teleport", items(ItemID.LUNAR_TABLET_CATHERBY_TELEPORT),
		runes(ItemID.LAWRUNE, 3, ItemID.ASTRALRUNE, 3, ItemID.WATERRUNE, 10)),
	CAMELOT_TELEPORT("Camelot Teleport", items(ItemID.POH_TABLET_CAMELOTTELEPORT),
		runes(ItemID.LAWRUNE, 1, ItemID.AIRRUNE, 5)),
	ARDOUGNE_TELEPORT("Ardougne Teleport", items(ItemID.POH_TABLET_ARDOUGNETELEPORT),
		runes(ItemID.LAWRUNE, 2, ItemID.WATERRUNE, 2)),
	FISHING_GUILD_TELEPORT("Fishing Guild Teleport", items(ItemID.LUNAR_TABLET_FISHING_GUILD_TELEPORT),
		runes(ItemID.LAWRUNE, 3, ItemID.ASTRALRUNE, 3, ItemID.WATERRUNE, 10)),
	FENKENSTRAINS_CASTLE_TELEPORT("Fenkenstrain's Castle Teleport", items(ItemID.TELETAB_FENK),
		runes(ItemID.LAWRUNE, 1, ItemID.SOULRUNE, 1, ItemID.EARTHRUNE, 1)),
	KHARYRLL_TELEPORT("Kharyrll Teleport", items(ItemID.TABLET_KHARYLL),
		runes(ItemID.LAWRUNE, 2, ItemID.BLOODRUNE, 1)),
	BATTLEFRONT_TELEPORT("Battlefront Teleport", items(ItemID.TELETAB_BATTLEFRONT),
		runes(ItemID.LAWRUNE, 1, ItemID.EARTHRUNE, 1, ItemID.FIRERUNE, 1)),
	CIVITAS_TELEPORT("Civitas illa Fortis Teleport", items(ItemID.POH_TABLET_FORTISTELEPORT),
		runes(ItemID.LAWRUNE, 2, ItemID.EARTHRUNE, 1, ItemID.FIRERUNE, 1)),
	TROLLHEIM_TELEPORT("Trollheim Teleport", items(ItemID.NZONE_TELETAB_TROLLHEIM),
		runes(ItemID.LAWRUNE, 2, ItemID.FIRERUNE, 2)),
	HARMONY_TELEPORT("Harmony Island Teleport", items(ItemID.TELETAB_HARMONY),
		runes(ItemID.LAWRUNE, 1, ItemID.NATURERUNE, 1, ItemID.SOULRUNE, 1)),
	LUMBRIDGE_TELEPORT("Lumbridge Teleport", items(ItemID.POH_TABLET_LUMBRIDGETELEPORT),
		runes(ItemID.LAWRUNE, 1, ItemID.AIRRUNE, 3, ItemID.EARTHRUNE, 1)),
	VARROCK_TELEPORT("Varrock Teleport", items(ItemID.POH_TABLET_VARROCKTELEPORT),
		runes(ItemID.LAWRUNE, 1, ItemID.AIRRUNE, 3, ItemID.FIRERUNE, 1)),

	// Tablets with no spell of their own: house tablets redirected to a portal, and clue scrolls
	HOSIDIUS_TAB("Hosidius teleport", ItemID.NZONE_TELETAB_KOUREND),
	TAVERLEY_TAB("Taverley teleport", ItemID.NZONE_TELETAB_TAVERLEY),
	BRIMHAVEN_TAB("Brimhaven teleport", ItemID.NZONE_TELETAB_BRIMHAVEN),
	MOS_LEHARMLESS_TELEPORT("Mos le'harmless teleport", ItemID.TELEPORTSCROLL_MOSLES),
	TAI_BWO_WANNAI_TELEPORT("Tai bwo wannai teleport", ItemID.TELEPORTSCROLL_TAIBWO),

	// Items
	EXPLORERS_RING("Explorer's ring", ItemID.LUMBRIDGE_RING_MEDIUM, ItemID.LUMBRIDGE_RING_HARD, ItemID.LUMBRIDGE_RING_ELITE),
	RING_OF_THE_ELEMENTS("Ring of the elements", ItemID.RING_OF_ELEMENTS_CHARGED),
	AMULET_OF_GLORY("Amulet of glory",
		ItemID.AMULET_OF_GLORY_1, ItemID.AMULET_OF_GLORY_2, ItemID.AMULET_OF_GLORY_3, ItemID.AMULET_OF_GLORY_4,
		ItemID.AMULET_OF_GLORY_5, ItemID.AMULET_OF_GLORY_6, ItemID.AMULET_OF_GLORY_INF,
		ItemID.TRAIL_AMULET_OF_GLORY_1, ItemID.TRAIL_AMULET_OF_GLORY_2, ItemID.TRAIL_AMULET_OF_GLORY_3,
		ItemID.TRAIL_AMULET_OF_GLORY_4, ItemID.TRAIL_AMULET_OF_GLORY_5, ItemID.TRAIL_AMULET_OF_GLORY_6),
	SKILLS_NECKLACE("Skills necklace",
		ItemID.JEWL_NECKLACE_OF_SKILLS_1, ItemID.JEWL_NECKLACE_OF_SKILLS_2, ItemID.JEWL_NECKLACE_OF_SKILLS_3,
		ItemID.JEWL_NECKLACE_OF_SKILLS_4, ItemID.JEWL_NECKLACE_OF_SKILLS_5, ItemID.JEWL_NECKLACE_OF_SKILLS_6),
	RING_OF_WEALTH("Ring of wealth",
		ItemID.RING_OF_WEALTH_1, ItemID.RING_OF_WEALTH_2, ItemID.RING_OF_WEALTH_3, ItemID.RING_OF_WEALTH_4, ItemID.RING_OF_WEALTH_5,
		ItemID.RING_OF_WEALTH_I1, ItemID.RING_OF_WEALTH_I2, ItemID.RING_OF_WEALTH_I3, ItemID.RING_OF_WEALTH_I4, ItemID.RING_OF_WEALTH_I5),
	COMBAT_BRACELET("Combat bracelet",
		ItemID.JEWL_BRACELET_OF_COMBAT_1, ItemID.JEWL_BRACELET_OF_COMBAT_2, ItemID.JEWL_BRACELET_OF_COMBAT_3,
		ItemID.JEWL_BRACELET_OF_COMBAT_4, ItemID.JEWL_BRACELET_OF_COMBAT_5, ItemID.JEWL_BRACELET_OF_COMBAT_6),
	GAMES_NECKLACE("Games necklace",
		ItemID.NECKLACE_OF_MINIGAMES_1, ItemID.NECKLACE_OF_MINIGAMES_2, ItemID.NECKLACE_OF_MINIGAMES_3, ItemID.NECKLACE_OF_MINIGAMES_4,
		ItemID.NECKLACE_OF_MINIGAMES_5, ItemID.NECKLACE_OF_MINIGAMES_6, ItemID.NECKLACE_OF_MINIGAMES_7, ItemID.NECKLACE_OF_MINIGAMES_8),
	SLAYER_RING("Slayer ring",
		ItemID.SLAYER_RING_1, ItemID.SLAYER_RING_2, ItemID.SLAYER_RING_3, ItemID.SLAYER_RING_4, ItemID.SLAYER_RING_5,
		ItemID.SLAYER_RING_6, ItemID.SLAYER_RING_7, ItemID.SLAYER_RING_8, ItemID.SLAYER_RING_ETERNAL),
	NECKLACE_OF_PASSAGE("Necklace of passage",
		ItemID.NECKLACE_OF_PASSAGE_1, ItemID.NECKLACE_OF_PASSAGE_2, ItemID.NECKLACE_OF_PASSAGE_3, ItemID.NECKLACE_OF_PASSAGE_4,
		ItemID.NECKLACE_OF_PASSAGE_5),
	ARDOUGNE_CLOAK("Ardougne cloak", ItemID.ARDY_CAPE_MEDIUM, ItemID.ARDY_CAPE_HARD, ItemID.ARDY_CAPE_ELITE),
	KARAMJA_GLOVES("Karamja gloves", ItemID.ATJUN_GLOVES_HARD, ItemID.ATJUN_GLOVES_ELITE),
	ECTOPHIAL("Ectophial", ItemID.ECTOPHIAL, ItemID.ECTOPHIAL_EMPTY),
	XERICS_TALISMAN("Xeric's talisman", ItemID.XERIC_TALISMAN),
	KHAREDSTS_MEMOIRS("Kharedst's memoirs", ItemID.VEOS_KHAREDSTS_MEMOIRS, ItemID.BOOK_OF_THE_DEAD),
	FARMING_CAPE("Farming cape", ItemID.SKILLCAPE_FARMING, ItemID.SKILLCAPE_FARMING_TRIMMED),
	FISHING_CAPE("Fishing cape", ItemID.SKILLCAPE_FISHING, ItemID.SKILLCAPE_FISHING_TRIMMED),
	HUNTER_CAPE("Hunter cape", ItemID.SKILLCAPE_HUNTING, ItemID.SKILLCAPE_HUNTING_TRIMMED),
	QUEST_POINT_CAPE("Quest point cape", ItemID.SKILLCAPE_QP, ItemID.SKILLCAPE_QP_TRIMMED),
	ACHIEVEMENT_DIARY_CAPE("Achievement diary cape", ItemID.SKILLCAPE_AD, ItemID.SKILLCAPE_AD_TRIMMED),
	/**
	 * Its teleports to the house portals, which are this cape's own; {@link #HOUSE} is for going via the house.
	 */
	CONSTRUCTION_CAPE("Construct. cape", ItemID.SKILLCAPE_CONSTRUCTION, ItemID.SKILLCAPE_CONSTRUCTION_TRIMMED),
	QUETZAL_WHISTLE("Quetzal whistle",
		ItemID.HG_QUETZALWHISTLE_BASIC, ItemID.HG_QUETZALWHISTLE_ENHANCED, ItemID.HG_QUETZALWHISTLE_PERFECTED),
	PENDANT_OF_ATES("Pendant of ates", ItemID.PENDANT_OF_ATES),
	STONY_BASALT("Stony basalt", ItemID.STRONGHOLD_TELEPORT_BASALT),
	ICY_BASALT("Icy basalt", ItemID.WEISS_TELEPORT_BASALT),
	RING_OF_SHADOWS("Ring of shadows", ItemID.RING_OF_SHADOWS),
	DIGSITE_PENDANT("Digsite pendant", ItemID.NECKLACE_OF_DIGSITE_1, ItemID.NECKLACE_OF_DIGSITE_2,
		ItemID.NECKLACE_OF_DIGSITE_3, ItemID.NECKLACE_OF_DIGSITE_4, ItemID.NECKLACE_OF_DIGSITE_5),
	ROYAL_SEED_POD("Royal seed pod", ItemID.MM2_ROYAL_SEED_POD),
	GRAND_SEED_POD("Grand seed pod", ItemID.ALUFT_SEED_POD),
	TELEPORT_CRYSTAL("Teleport crystal",
		ItemID.MOURNING_TELEPORT_CRYSTAL_1, ItemID.MOURNING_TELEPORT_CRYSTAL_2, ItemID.MOURNING_TELEPORT_CRYSTAL_3,
		ItemID.MOURNING_TELEPORT_CRYSTAL_4, ItemID.MOURNING_TELEPORT_CRYSTAL_5),
	ETERNAL_TELEPORT_CRYSTAL("Eternal teleport crystal", ItemID.PRIF_TELEPORT_CRYSTAL);

	/**
	 * Hold runes for spells, so they are worth showing next to them in the bank.
	 */
	static final Set<Integer> RUNE_POUCHES = Set.of(
		ItemID.BH_RUNE_POUCH, ItemID.BH_RUNE_POUCH_TROUVER, ItemID.DIVINE_RUNE_POUCH, ItemID.DIVINE_RUNE_POUCH_TROUVER);

	/**
	 * Every item that plays a part in some teleport.
	 */
	static final Set<Integer> ALL_ITEM_IDS;

	/**
	 * Choices saved by earlier versions, which kept a spell, its tablet and each way into the house apart.
	 */
	private static final Map<String, Teleport> LEGACY = Map.ofEntries(
		Map.entry("FALADOR_TAB", FALADOR_TELEPORT), Map.entry("FALADOR_SPELL", FALADOR_TELEPORT),
		Map.entry("CATHERBY_TAB", CATHERBY_TELEPORT), Map.entry("CATHERBY_SPELL", CATHERBY_TELEPORT),
		Map.entry("CAMELOT_TAB", CAMELOT_TELEPORT), Map.entry("CAMELOT_SPELL", CAMELOT_TELEPORT),
		Map.entry("ARDOUGNE_TAB", ARDOUGNE_TELEPORT), Map.entry("ARDOUGNE_SPELL", ARDOUGNE_TELEPORT),
		Map.entry("FENKENSTRAIN_TAB", FENKENSTRAINS_CASTLE_TELEPORT), Map.entry("FENKENSTRAIN_SPELL", FENKENSTRAINS_CASTLE_TELEPORT),
		Map.entry("BATTLEFRONT_SPELL", BATTLEFRONT_TELEPORT),
		Map.entry("CIVITAS_TAB", CIVITAS_TELEPORT), Map.entry("CIVITAS_SPELL", CIVITAS_TELEPORT),
		Map.entry("TROLLHEIM_TAB", TROLLHEIM_TELEPORT), Map.entry("TROLLHEIM_SPELL", TROLLHEIM_TELEPORT),
		Map.entry("HARMONY_TAB", HARMONY_TELEPORT), Map.entry("HARMONY_SPELL", HARMONY_TELEPORT),
		Map.entry("LUMBRIDGE_TAB", LUMBRIDGE_TELEPORT), Map.entry("LUMBRIDGE_SPELL", LUMBRIDGE_TELEPORT),
		Map.entry("VARROCK_TAB", VARROCK_TELEPORT), Map.entry("VARROCK_SPELL", VARROCK_TELEPORT),
		Map.entry("HOUSE_TAB", HOUSE), Map.entry("HOUSE_SPELL", HOUSE), Map.entry("HOUSE_CAPE", HOUSE));

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
	 * Any one of these is this teleport, such as each charge of a necklace, or the tablet of a spell.
	 */
	private final Set<Integer> itemIds;
	/**
	 * Rune id to the amount one cast takes. Empty unless this is a spell.
	 */
	private final Map<Integer, Integer> runes;
	/**
	 * Not carrying its items doesn't mean the player can't use it.
	 */
	private final boolean optional;

	Teleport(String label, Integer... itemIds)
	{
		this(label, Set.of(itemIds), Map.of(), false, false);
	}

	Teleport(String label, Set<Integer> itemIds, Map<Integer, Integer> runes)
	{
		this(label, itemIds, runes, false, false);
	}

	Teleport(String label, Set<Integer> itemIds, Map<Integer, Integer> runes, boolean viaHouse, boolean optional)
	{
		this.label = label;
		this.viaHouse = viaHouse;
		this.itemIds = itemIds;
		this.runes = runes;
		this.optional = optional;
	}

	/**
	 * Whether it can be cast, rather than only used from an item.
	 */
	boolean isSpell()
	{
		return !runes.isEmpty();
	}

	/**
	 * The items to show in the bank for this teleport: its items, and a spell's runes.
	 */
	Set<Integer> bankItemIds()
	{
		if (!isSpell())
		{
			return itemIds;
		}

		Set<Integer> ids = new HashSet<>(itemIds);
		ids.addAll(runes.keySet());
		ids.addAll(RUNE_POUCHES);
		return ids;
	}

	/**
	 * A saved choice, including those earlier versions saved; null for one this version doesn't know.
	 */
	@Nullable
	static Teleport fromSaved(String name)
	{
		try
		{
			return valueOf(name);
		}
		catch (IllegalArgumentException e)
		{
			return LEGACY.get(name);
		}
	}

	@Override
	public String toString()
	{
		return label;
	}

	private static Set<Integer> items(Integer... ids)
	{
		return Set.of(ids);
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
