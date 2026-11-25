package com.plugandplant;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import lombok.Getter;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.VarbitID;

/**
 * The farming locations runs stop at: those with a herb patch, each with the allotments and flower
 * patch beside it, and those with tree patches of any kind. Regions and varbits are the ones the
 * Time Tracking plugin files each patch under.
 */
@Getter
public enum FarmLocation
{
	FALADOR("Falador", 20,
		new Patches(12083).herb(VarbitID.FARMING_TRANSMIT_D, new WorldPoint(3058, 3311, 0))
			.allotment("North-west allotment", VarbitID.FARMING_TRANSMIT_A)
			.allotment("South-east allotment", VarbitID.FARMING_TRANSMIT_B)
			.flower(VarbitID.FARMING_TRANSMIT_C),
		Teleport.EXPLORERS_RING, Teleport.FALADOR_TELEPORT, Teleport.DRAYNOR_MANOR_TELEPORT, Teleport.RING_OF_WEALTH,
		Teleport.SKILLS_NECKLACE, Teleport.AMULET_OF_GLORY, Teleport.RING_OF_THE_ELEMENTS),
	CATHERBY("Catherby", 20,
		new Patches(11062).herb(VarbitID.FARMING_TRANSMIT_D, new WorldPoint(2813, 3463, 0))
			.allotment("North allotment", VarbitID.FARMING_TRANSMIT_A)
			.allotment("South allotment", VarbitID.FARMING_TRANSMIT_B)
			.flower(VarbitID.FARMING_TRANSMIT_C),
		Teleport.CATHERBY_TELEPORT, Teleport.CAMELOT_TELEPORT),
	ARDOUGNE("Ardougne", 20,
		new Patches(10548).herb(VarbitID.FARMING_TRANSMIT_D, new WorldPoint(2670, 3374, 0))
			.allotment("North allotment", VarbitID.FARMING_TRANSMIT_A)
			.allotment("South allotment", VarbitID.FARMING_TRANSMIT_B)
			.flower(VarbitID.FARMING_TRANSMIT_C),
		Teleport.ARDOUGNE_CLOAK, Teleport.ARDOUGNE_TELEPORT, Teleport.FISHING_GUILD_TELEPORT,
		Teleport.SKILLS_NECKLACE, Teleport.FISHING_CAPE, Teleport.QUEST_POINT_CAPE, Teleport.COMBAT_BRACELET,
		Teleport.FAIRY_RING),
	MORYTANIA("Morytania", 20,
		new Patches(14391).herb(VarbitID.FARMING_TRANSMIT_D, new WorldPoint(3605, 3529, 0))
			.allotment("North-west allotment", VarbitID.FARMING_TRANSMIT_A)
			.allotment("South-east allotment", VarbitID.FARMING_TRANSMIT_B)
			.flower(VarbitID.FARMING_TRANSMIT_C),
		Teleport.ECTOPHIAL, Teleport.FENKENSTRAINS_CASTLE_TELEPORT, Teleport.KHARYRLL_TELEPORT, Teleport.FAIRY_RING),
	HOSIDIUS("Hosidius", 20,
		new Patches(6967).herb(VarbitID.FARMING_TRANSMIT_D, new WorldPoint(1738, 3550, 0))
			.allotment("North-east allotment", VarbitID.FARMING_TRANSMIT_A)
			.allotment("South-west allotment", VarbitID.FARMING_TRANSMIT_B)
			.flower(VarbitID.FARMING_TRANSMIT_C)
			.tree(PatchType.SPIRIT_TREE, VarbitID.FARMING_TRANSMIT_F, new WorldPoint(1693, 3542, 0),
				NpcID.FARMING_GARDENER_SPIRIT_TREE_4),
		Teleport.XERICS_TALISMAN, Teleport.HOSIDIUS_TAB, Teleport.CONSTRUCTION_CAPE, Teleport.KHAREDSTS_MEMOIRS,
		Teleport.SKILLS_NECKLACE, Teleport.FAIRY_RING),
	// The guild's patches are spread out, hence the wider radius. Its wings take a Farming level to
	// enter: 45 for the allotments and flowers, 65 for the herb and tree, 85 for the fruit tree and
	// the special trees.
	FARMING_GUILD("Farming Guild", 45,
		new Patches(4922).smallPatchesFromLevel(45)
			.herb(VarbitID.FARMING_TRANSMIT_E, new WorldPoint(1238, 3726, 0), 65)
			.allotment("North allotment", VarbitID.FARMING_TRANSMIT_C)
			.allotment("South allotment", VarbitID.FARMING_TRANSMIT_D)
			.flower(VarbitID.FARMING_TRANSMIT_H)
			.tree(VarbitID.FARMING_TRANSMIT_G, new WorldPoint(1232, 3736, 0), NpcID.FARMING_GARDENER_FARMGUILD_T2, 65)
			.fruitTree(VarbitID.FARMING_TRANSMIT_K, new WorldPoint(1242, 3758, 0), NpcID.FARMING_GARDENER_FARMGUILD_T3, 85)
			.tree(PatchType.SPIRIT_TREE, VarbitID.FARMING_TRANSMIT_A, new WorldPoint(1253, 3750, 0),
				NpcID.FARMING_GARDENER_SPIRIT_TREE_5, 85)
			.tree(PatchType.CELASTRUS, VarbitID.FARMING_TRANSMIT_L, new WorldPoint(1244, 3750, 0),
				NpcID.FARMING_GARDENER_FARMGUILD_CELASTRUS, 85)
			.tree(PatchType.REDWOOD, VarbitID.FARMING_TRANSMIT_I, new WorldPoint(1229, 3755, 0),
				NpcID.FARMING_GARDENER_FARMGUILD_REDWOOD, 85),
		Teleport.FARMING_CAPE, Teleport.SKILLS_NECKLACE, Teleport.BATTLEFRONT_TELEPORT, Teleport.FAIRY_RING),
	CIVITAS("Ortus Farm", 20,
		new Patches(6192).herb(VarbitID.FARMING_TRANSMIT_D, new WorldPoint(1582, 3094, 0))
			.allotment("North allotment", VarbitID.FARMING_TRANSMIT_A)
			.allotment("South allotment", VarbitID.FARMING_TRANSMIT_B)
			.flower(VarbitID.FARMING_TRANSMIT_C),
		Teleport.QUETZAL_WHISTLE, Teleport.HUNTER_CAPE, Teleport.CIVITAS_TELEPORT, Teleport.FAIRY_RING),
	TROLL_STRONGHOLD("Troll Stronghold", 20,
		new Patches(11321).herb(VarbitID.FARMING_TRANSMIT_A, new WorldPoint(2826, 3694, 0)),
		Teleport.STONY_BASALT, Teleport.TROLLHEIM_TELEPORT),
	WEISS("Weiss", 20,
		new Patches(11325).herb(VarbitID.FARMING_TRANSMIT_A, new WorldPoint(2848, 3934, 0)),
		Teleport.ICY_BASALT, Teleport.RING_OF_SHADOWS, Teleport.FAIRY_RING),
	HARMONY("Harmony Island", 20,
		new Patches(15148).herb(VarbitID.FARMING_TRANSMIT_B, new WorldPoint(3789, 2837, 0))
			.allotment("Allotment", VarbitID.FARMING_TRANSMIT_A),
		Teleport.HARMONY_TELEPORT, Teleport.MOS_LEHARMLESS_TELEPORT, Teleport.ECTOPHIAL),

	LUMBRIDGE("Lumbridge", 20,
		new Patches(12594).tree(VarbitID.FARMING_TRANSMIT_A, new WorldPoint(3193, 3231, 0), NpcID.FARMING_GARDENER_TREE_4),
		Teleport.LUMBRIDGE_TELEPORT, Teleport.ACHIEVEMENT_DIARY_CAPE),
	VARROCK("Varrock", 20,
		new Patches(12854).tree(VarbitID.FARMING_TRANSMIT_A, new WorldPoint(3229, 3459, 0), NpcID.FARMING_GARDENER_TREE_3_02),
		Teleport.VARROCK_TELEPORT, Teleport.RING_OF_WEALTH, Teleport.SKILLS_NECKLACE, Teleport.ACHIEVEMENT_DIARY_CAPE),
	FALADOR_PARK("Falador park", 20,
		new Patches(11828).tree(VarbitID.FARMING_TRANSMIT_A, new WorldPoint(3004, 3373, 0), NpcID.FARMING_GARDENER_TREE_2),
		Teleport.RING_OF_WEALTH, Teleport.FALADOR_TELEPORT, Teleport.SKILLS_NECKLACE, Teleport.AMULET_OF_GLORY),
	TAVERLEY("Taverley", 20,
		new Patches(11573).tree(VarbitID.FARMING_TRANSMIT_A, new WorldPoint(2936, 3438, 0), NpcID.FARMING_GARDENER_TREE_1),
		Teleport.TAVERLEY_TAB, Teleport.FALADOR_TELEPORT, Teleport.CONSTRUCTION_CAPE, Teleport.GAMES_NECKLACE,
		Teleport.COMBAT_BRACELET),
	GNOME_STRONGHOLD("Gnome Stronghold", 20,
		new Patches(9781).tree(VarbitID.FARMING_TRANSMIT_A, new WorldPoint(2436, 3415, 0), NpcID.FARMING_GARDENER_TREE_GNOME)
			.fruitTree(VarbitID.FARMING_TRANSMIT_B, new WorldPoint(2476, 3446, 0), NpcID.FARMING_GARDENER_FRUIT_1),
		Teleport.ROYAL_SEED_POD, Teleport.GRAND_SEED_POD, Teleport.SLAYER_RING, Teleport.NECKLACE_OF_PASSAGE,
		Teleport.ACHIEVEMENT_DIARY_CAPE),
	AUBURNVALE("Nemus Retreat", 20,
		new Patches(5427).tree(VarbitID.FARMING_TRANSMIT_A, new WorldPoint(1367, 3322, 0), NpcID.FARMING_GARDENER_TREE_7),
		Teleport.PENDANT_OF_ATES, Teleport.QUETZAL_WHISTLE, Teleport.FAIRY_RING),
	TREE_GNOME_VILLAGE("Tree Gnome Village", 20,
		new Patches(9777).fruitTree(VarbitID.FARMING_TRANSMIT_A, new WorldPoint(2490, 3180, 0), NpcID.FARMING_GARDENER_FRUIT_2),
		Teleport.FAIRY_RING),
	BRIMHAVEN("Brimhaven", 20,
		new Patches(11058).fruitTree(VarbitID.FARMING_TRANSMIT_A, new WorldPoint(2765, 3213, 0), NpcID.GARTH)
			.tree(PatchType.SPIRIT_TREE, VarbitID.FARMING_TRANSMIT_B, new WorldPoint(2802, 3203, 0),
				NpcID.FARMING_GARDENER_SPIRIT_TREE_3),
		Teleport.BRIMHAVEN_TAB, Teleport.CONSTRUCTION_CAPE, Teleport.KARAMJA_GLOVES, Teleport.AMULET_OF_GLORY,
		Teleport.ARDOUGNE_TELEPORT),
	CATHERBY_ORCHARD("East of Catherby", 20,
		new Patches(11317).fruitTree(VarbitID.FARMING_TRANSMIT_A, new WorldPoint(2860, 3433, 0), NpcID.FARMING_GARDENER_FRUIT_4),
		Teleport.CATHERBY_TELEPORT, Teleport.CAMELOT_TELEPORT),
	LLETYA("Lletya", 20,
		new Patches(9265).fruitTree(VarbitID.FARMING_TRANSMIT_A, new WorldPoint(2347, 3162, 0), NpcID.FARMING_GARDENER_FRUIT_TREE_5),
		Teleport.TELEPORT_CRYSTAL, Teleport.ETERNAL_TELEPORT_CRYSTAL),
	KASTORI("Kastori", 20,
		new Patches(5423).fruitTree(VarbitID.FARMING_TRANSMIT_B, new WorldPoint(1350, 3057, 0), NpcID.FARMING_GARDENER_FRUIT_7)
			.tree(PatchType.CALQUAT, VarbitID.FARMING_TRANSMIT_A, new WorldPoint(1367, 3032, 0),
				NpcID.FARMING_GARDENER_CALQUAT_2),
		Teleport.PENDANT_OF_ATES, Teleport.QUETZAL_WHISTLE),

	TAI_BWO_WANNAI("Tai Bwo Wannai", 20,
		new Patches(11056).tree(PatchType.CALQUAT, VarbitID.FARMING_TRANSMIT_A, new WorldPoint(2796, 3101, 0),
			NpcID.FARMING_GARDENER_CALQUAT),
		Teleport.TAI_BWO_WANNAI_TELEPORT, Teleport.FAIRY_RING),
	// Reached through the Great Conch, which takes part of Troubled Tortugans
	SUMMER_SHORE("The Summer Shore", 20,
		new Patches(12581).tree(PatchType.CALQUAT, VarbitID.FARMING_TRANSMIT_C, new WorldPoint(3128, 2405, 0),
			NpcID.FARMING_GARDENER_CALQUAT_3),
		Teleport.FAIRY_RING),
	FOSSIL_ISLAND("Fossil Island", 20,
		new Patches(14651)
			.tree(PatchType.HARDWOOD_TREE, "East hardwood patch", VarbitID.FARMING_TRANSMIT_A, new WorldPoint(3715, 3835, 0),
				NpcID.FOSSIL_SQUIRREL_GARDENER1, 1)
			.tree(PatchType.HARDWOOD_TREE, "Middle hardwood patch", VarbitID.FARMING_TRANSMIT_B, new WorldPoint(3709, 3835, 0),
				NpcID.FOSSIL_SQUIRREL_GARDENER2, 1)
			.tree(PatchType.HARDWOOD_TREE, "West hardwood patch", VarbitID.FARMING_TRANSMIT_C, new WorldPoint(3702, 3835, 0),
				NpcID.FOSSIL_SQUIRREL_GARDENER3, 1),
		Teleport.DIGSITE_PENDANT),
	// Takes The Ribbiting Tale of a Lily Pad Labour Dispute
	LOCUS_OASIS("Locus Oasis", 20,
		new Patches(6702).tree(PatchType.HARDWOOD_TREE, VarbitID.FARMING_TRANSMIT_A, new WorldPoint(1687, 2972, 0),
			NpcID.FROG_QUEST_MARCELLUS),
		Teleport.QUETZAL_WHISTLE, Teleport.FAIRY_RING),
	PORT_SARIM("Port Sarim", 20,
		new Patches(12082).tree(PatchType.SPIRIT_TREE, VarbitID.FARMING_TRANSMIT_A, new WorldPoint(3060, 3258, 0),
			NpcID.FARMING_GARDENER_SPIRIT_TREE_1),
		Teleport.EXPLORERS_RING, Teleport.AMULET_OF_GLORY),
	ETCETERIA("Etceteria", 20,
		new Patches(10300).tree(PatchType.SPIRIT_TREE, VarbitID.FARMING_TRANSMIT_B, new WorldPoint(2613, 3857, 0),
			NpcID.FARMING_GARDENER_SPIRIT_TREE_2),
		Teleport.RING_OF_WEALTH, Teleport.FAIRY_RING),
	PRIFDDINAS("Prifddinas", 20,
		new Patches(13151).tree(PatchType.CRYSTAL_TREE, VarbitID.FARMING_TRANSMIT_E, new WorldPoint(3292, 6119, 0), -1),
		Teleport.TELEPORT_CRYSTAL, Teleport.ETERNAL_TELEPORT_CRYSTAL);

	private final String displayName;
	/**
	 * How far from its patches the location reaches, in tiles.
	 */
	private final int radius;
	/**
	 * The herb patch first, then allotments, the flower patch and the trees.
	 */
	private final List<FarmPatch> patches;
	/**
	 * The teleports that land near this location.
	 */
	private final List<Teleport> teleports;

	FarmLocation(String displayName, int radius, Patches patches, Teleport... teleports)
	{
		this.displayName = displayName;
		this.radius = radius;
		this.teleports = List.of(teleports);

		List<FarmPatch> built = new ArrayList<>();
		WorldPoint herb = patches.herbPoint;
		if (herb != null)
		{
			built.add(new FarmPatch(this, PatchType.HERB, "Herb patch", patches.region, patches.herbVarbit, herb, -1,
				patches.herbLevel));
			for (int i = 0; i < patches.allotmentNames.size(); i++)
			{
				// Allotments and flowers sit beside the herb patch
				built.add(new FarmPatch(this, PatchType.ALLOTMENT, patches.allotmentNames.get(i), patches.region,
					patches.allotmentVarbits.get(i), herb, -1, patches.smallPatchLevel));
			}
			if (patches.flowerVarbit != -1)
			{
				built.add(new FarmPatch(this, PatchType.FLOWER, "Flower patch", patches.region, patches.flowerVarbit, herb, -1,
					patches.smallPatchLevel));
			}
		}
		for (Patches.Tree tree : patches.trees)
		{
			// Nobody gets to a patch to no end: the run leaves out what the player can't plant yet
			built.add(new FarmPatch(this, tree.type, tree.name, patches.region, tree.varbit, tree.point, tree.gardener,
				Math.max(tree.level, tree.type.getPlantLevel())));
		}
		this.patches = List.copyOf(built);
	}

	List<FarmPatch> patches(PatchType type)
	{
		List<FarmPatch> ofType = new ArrayList<>();
		for (FarmPatch patch : patches)
		{
			if (patch.getType() == type)
			{
				ofType.add(patch);
			}
		}
		return ofType;
	}

	/**
	 * Whether a tool leprechaun stands by the location's herb patch to note herbs. Every herb patch
	 * has one; the Troll Stronghold's lies in a deckchair on the roof.
	 */
	boolean hasToolLeprechaun()
	{
		return !patches(PatchType.HERB).isEmpty();
	}

	/**
	 * Whether a point is close enough to one of the location's patches to be at the location.
	 */
	boolean isNear(WorldPoint other, int slack)
	{
		for (FarmPatch patch : patches)
		{
			WorldPoint point = patch.getPoint();
			if (other.getPlane() == point.getPlane() && other.distanceTo2D(point) <= radius + slack)
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * The OSRS Wiki's farming run guides' orders, for herbs and for trees.
	 */
	static final List<FarmLocation> HERB_ORDER = List.of(FALADOR, MORYTANIA, ARDOUGNE, CATHERBY, HOSIDIUS, FARMING_GUILD,
		CIVITAS, TROLL_STRONGHOLD, WEISS, HARMONY);
	static final List<FarmLocation> TREE_ORDER = List.of(GNOME_STRONGHOLD, TREE_GNOME_VILLAGE, FARMING_GUILD, LUMBRIDGE,
		VARROCK, FALADOR_PARK, TAVERLEY, CATHERBY_ORCHARD, BRIMHAVEN, LLETYA, KASTORI, AUBURNVALE);

	/**
	 * What the player can pick as this location's teleport: any item that goes here, one teleport
	 * in particular, a way into their house, or none.
	 */
	List<Teleport> teleportChoices()
	{
		List<Teleport> choices = new ArrayList<>();
		choices.add(Teleport.ANY);
		choices.addAll(teleports);
		choices.add(Teleport.HOUSE);
		choices.add(Teleport.NONE);
		return choices;
	}

	/**
	 * The teleports that satisfy a choice: carrying any one of them is enough. A choice that doesn't
	 * go to this location is treated as {@link Teleport#ANY}, which is any of this location's
	 * teleport items; only their items count then, not a spell's runes (see {@link #itemsOnly}).
	 */
	List<Teleport> teleportsFor(Teleport choice)
	{
		if (choice == Teleport.NONE)
		{
			return List.of();
		}
		if (teleports.contains(choice) || choice == Teleport.HOUSE)
		{
			return List.of(choice);
		}

		List<Teleport> items = new ArrayList<>();
		for (Teleport teleport : teleports)
		{
			if (!teleport.getItemIds().isEmpty() && !teleport.isOptional())
			{
				items.add(teleport);
			}
		}
		return items;
	}

	/**
	 * Whether only teleport items satisfy the choice, as for {@link Teleport#ANY}: carrying runes that
	 * happen to cast a spell here doesn't mean the player brought them for it.
	 */
	boolean itemsOnly(Teleport choice)
	{
		return choice != Teleport.HOUSE && !teleports.contains(choice);
	}

	/**
	 * Collects a location's patches while its enum constant is being declared.
	 */
	static final class Patches
	{
		private static final class Tree
		{
			private final PatchType type;
			private final String name;
			private final int varbit;
			private final WorldPoint point;
			private final int gardener;
			private final int level;

			private Tree(PatchType type, String name, int varbit, WorldPoint point, int gardener, int level)
			{
				this.type = type;
				this.name = name;
				this.varbit = varbit;
				this.point = point;
				this.gardener = gardener;
				this.level = level;
			}
		}

		private final int region;
		private int herbVarbit = -1;
		@Nullable
		private WorldPoint herbPoint;
		private int herbLevel = 1;
		private int smallPatchLevel = 1;
		private final List<String> allotmentNames = new ArrayList<>();
		private final List<Integer> allotmentVarbits = new ArrayList<>();
		private int flowerVarbit = -1;
		private final List<Tree> trees = new ArrayList<>();

		Patches(int region)
		{
			this.region = region;
		}

		Patches herb(int varbit, WorldPoint point)
		{
			return herb(varbit, point, 1);
		}

		Patches herb(int varbit, WorldPoint point, int level)
		{
			herbVarbit = varbit;
			herbPoint = point;
			herbLevel = level;
			return this;
		}

		/**
		 * The Farming level the location's allotments and flower patch take to reach.
		 */
		Patches smallPatchesFromLevel(int level)
		{
			smallPatchLevel = level;
			return this;
		}

		Patches allotment(String name, int varbit)
		{
			allotmentNames.add(name);
			allotmentVarbits.add(varbit);
			return this;
		}

		Patches flower(int varbit)
		{
			flowerVarbit = varbit;
			return this;
		}

		Patches tree(int varbit, WorldPoint point, int gardener)
		{
			return tree(varbit, point, gardener, 1);
		}

		Patches tree(int varbit, WorldPoint point, int gardener, int level)
		{
			return tree(PatchType.TREE, varbit, point, gardener, level);
		}

		/**
		 * A tree patch of any kind; a gardener of -1 is none.
		 */
		Patches tree(PatchType type, int varbit, WorldPoint point, int gardener)
		{
			return tree(type, varbit, point, gardener, 1);
		}

		Patches tree(PatchType type, int varbit, WorldPoint point, int gardener, int level)
		{
			return tree(type, type.getPatchName(), varbit, point, gardener, level);
		}

		Patches tree(PatchType type, String name, int varbit, WorldPoint point, int gardener, int level)
		{
			trees.add(new Tree(type, name, varbit, point, gardener, level));
			return this;
		}

		Patches fruitTree(int varbit, WorldPoint point, int gardener)
		{
			return fruitTree(varbit, point, gardener, 1);
		}

		Patches fruitTree(int varbit, WorldPoint point, int gardener, int level)
		{
			return tree(PatchType.FRUIT_TREE, varbit, point, gardener, level);
		}
	}
}
