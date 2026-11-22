package com.ironmanfarmrun;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import lombok.Getter;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.VarbitID;

/**
 * The farming locations runs stop at: those with a herb patch, each with the allotments and flower
 * patch beside it, and those with tree or fruit tree patches. Regions and varbits are the ones the
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
		Teleport.EXPLORERS_RING, Teleport.FALADOR_TAB, Teleport.FALADOR_SPELL),
	CATHERBY("Catherby", 20,
		new Patches(11062).herb(VarbitID.FARMING_TRANSMIT_D, new WorldPoint(2813, 3463, 0))
			.allotment("North allotment", VarbitID.FARMING_TRANSMIT_A)
			.allotment("South allotment", VarbitID.FARMING_TRANSMIT_B)
			.flower(VarbitID.FARMING_TRANSMIT_C),
		Teleport.CATHERBY_TAB, Teleport.CATHERBY_SPELL, Teleport.CAMELOT_TAB, Teleport.CAMELOT_SPELL),
	ARDOUGNE("Ardougne", 20,
		new Patches(10548).herb(VarbitID.FARMING_TRANSMIT_D, new WorldPoint(2670, 3374, 0))
			.allotment("North allotment", VarbitID.FARMING_TRANSMIT_A)
			.allotment("South allotment", VarbitID.FARMING_TRANSMIT_B)
			.flower(VarbitID.FARMING_TRANSMIT_C),
		Teleport.ARDOUGNE_CLOAK, Teleport.ARDOUGNE_TAB, Teleport.ARDOUGNE_SPELL),
	MORYTANIA("Morytania", 20,
		new Patches(14391).herb(VarbitID.FARMING_TRANSMIT_D, new WorldPoint(3605, 3529, 0))
			.allotment("North-west allotment", VarbitID.FARMING_TRANSMIT_A)
			.allotment("South-east allotment", VarbitID.FARMING_TRANSMIT_B)
			.flower(VarbitID.FARMING_TRANSMIT_C),
		Teleport.ECTOPHIAL, Teleport.FENKENSTRAIN_TAB, Teleport.FENKENSTRAIN_SPELL),
	HOSIDIUS("Hosidius", 20,
		new Patches(6967).herb(VarbitID.FARMING_TRANSMIT_D, new WorldPoint(1738, 3550, 0))
			.allotment("North-east allotment", VarbitID.FARMING_TRANSMIT_A)
			.allotment("South-west allotment", VarbitID.FARMING_TRANSMIT_B)
			.flower(VarbitID.FARMING_TRANSMIT_C),
		Teleport.XERICS_TALISMAN, Teleport.HOSIDIUS_TAB),
	// The guild's patches are spread out, hence the wider radius. Its wings take a Farming level to
	// enter: 45 for the allotments and flowers, 65 for the herb and tree, 85 for the fruit tree.
	FARMING_GUILD("Farming Guild", 45,
		new Patches(4922).smallPatchesFromLevel(45)
			.herb(VarbitID.FARMING_TRANSMIT_E, new WorldPoint(1238, 3726, 0), 65)
			.allotment("North allotment", VarbitID.FARMING_TRANSMIT_C)
			.allotment("South allotment", VarbitID.FARMING_TRANSMIT_D)
			.flower(VarbitID.FARMING_TRANSMIT_H)
			.tree(VarbitID.FARMING_TRANSMIT_G, new WorldPoint(1232, 3736, 0), NpcID.FARMING_GARDENER_FARMGUILD_T2, 65)
			.fruitTree(VarbitID.FARMING_TRANSMIT_K, new WorldPoint(1242, 3758, 0), NpcID.FARMING_GARDENER_FARMGUILD_T3, 85),
		Teleport.SKILLS_NECKLACE, Teleport.FARMING_CAPE, Teleport.BATTLEFRONT_SPELL),
	CIVITAS("Civitas illa Fortis", 20,
		new Patches(6192).herb(VarbitID.FARMING_TRANSMIT_D, new WorldPoint(1582, 3094, 0))
			.allotment("North allotment", VarbitID.FARMING_TRANSMIT_A)
			.allotment("South allotment", VarbitID.FARMING_TRANSMIT_B)
			.flower(VarbitID.FARMING_TRANSMIT_C),
		Teleport.QUETZAL_WHISTLE, Teleport.CIVITAS_TAB, Teleport.CIVITAS_SPELL),
	TROLL_STRONGHOLD("Troll Stronghold", 20,
		new Patches(11321).herb(VarbitID.FARMING_TRANSMIT_A, new WorldPoint(2826, 3694, 0)),
		Teleport.STONY_BASALT, Teleport.TROLLHEIM_TAB, Teleport.TROLLHEIM_SPELL),
	WEISS("Weiss", 20,
		new Patches(11325).herb(VarbitID.FARMING_TRANSMIT_A, new WorldPoint(2848, 3934, 0)),
		Teleport.ICY_BASALT),
	HARMONY("Harmony Island", 20,
		new Patches(15148).herb(VarbitID.FARMING_TRANSMIT_B, new WorldPoint(3789, 2837, 0))
			.allotment("Allotment", VarbitID.FARMING_TRANSMIT_A),
		Teleport.HARMONY_TAB, Teleport.HARMONY_SPELL, Teleport.ECTOPHIAL),

	LUMBRIDGE("Lumbridge", 20,
		new Patches(12594).tree(VarbitID.FARMING_TRANSMIT_A, new WorldPoint(3193, 3231, 0), NpcID.FARMING_GARDENER_TREE_4),
		Teleport.LUMBRIDGE_TAB, Teleport.LUMBRIDGE_SPELL),
	VARROCK("Varrock", 20,
		new Patches(12854).tree(VarbitID.FARMING_TRANSMIT_A, new WorldPoint(3229, 3459, 0), NpcID.FARMING_GARDENER_TREE_3_02),
		Teleport.VARROCK_TAB, Teleport.VARROCK_SPELL),
	FALADOR_PARK("Falador park", 20,
		new Patches(11828).tree(VarbitID.FARMING_TRANSMIT_A, new WorldPoint(3004, 3373, 0), NpcID.FARMING_GARDENER_TREE_2),
		Teleport.RING_OF_WEALTH, Teleport.FALADOR_TAB, Teleport.FALADOR_SPELL),
	TAVERLEY("Taverley", 20,
		new Patches(11573).tree(VarbitID.FARMING_TRANSMIT_A, new WorldPoint(2936, 3438, 0), NpcID.FARMING_GARDENER_TREE_1),
		Teleport.TAVERLEY_TAB, Teleport.FALADOR_TAB, Teleport.FALADOR_SPELL),
	GNOME_STRONGHOLD("Gnome Stronghold", 20,
		new Patches(9781).tree(VarbitID.FARMING_TRANSMIT_A, new WorldPoint(2436, 3415, 0), NpcID.FARMING_GARDENER_TREE_GNOME)
			.fruitTree(VarbitID.FARMING_TRANSMIT_B, new WorldPoint(2476, 3446, 0), NpcID.FARMING_GARDENER_FRUIT_1),
		Teleport.ROYAL_SEED_POD),
	AUBURNVALE("Auburnvale", 20,
		new Patches(5427).tree(VarbitID.FARMING_TRANSMIT_A, new WorldPoint(1367, 3322, 0), NpcID.FARMING_GARDENER_TREE_7),
		Teleport.PENDANT_OF_ATES),
	TREE_GNOME_VILLAGE("Tree Gnome Village", 20,
		new Patches(9777).fruitTree(VarbitID.FARMING_TRANSMIT_A, new WorldPoint(2490, 3180, 0), NpcID.FARMING_GARDENER_FRUIT_2)),
	BRIMHAVEN("Brimhaven", 20,
		new Patches(11058).fruitTree(VarbitID.FARMING_TRANSMIT_A, new WorldPoint(2765, 3213, 0), NpcID.GARTH),
		// Brimhaven is also a boat ride from Ardougne
		Teleport.BRIMHAVEN_TAB, Teleport.ARDOUGNE_TAB, Teleport.ARDOUGNE_SPELL),
	CATHERBY_ORCHARD("Catherby", 20,
		new Patches(11317).fruitTree(VarbitID.FARMING_TRANSMIT_A, new WorldPoint(2860, 3433, 0), NpcID.FARMING_GARDENER_FRUIT_4),
		Teleport.CATHERBY_TAB, Teleport.CATHERBY_SPELL, Teleport.CAMELOT_TAB, Teleport.CAMELOT_SPELL),
	LLETYA("Lletya", 20,
		new Patches(9265).fruitTree(VarbitID.FARMING_TRANSMIT_A, new WorldPoint(2347, 3162, 0), NpcID.FARMING_GARDENER_FRUIT_TREE_5),
		Teleport.TELEPORT_CRYSTAL),
	KASTORI("Kastori", 20,
		new Patches(5423).fruitTree(VarbitID.FARMING_TRANSMIT_B, new WorldPoint(1350, 3057, 0), NpcID.FARMING_GARDENER_FRUIT_7),
		Teleport.PENDANT_OF_ATES);

	private static final List<Teleport> HOUSE_TELEPORTS = List.of(Teleport.HOUSE_TAB, Teleport.HOUSE_SPELL, Teleport.HOUSE_CAPE);

	private final String displayName;
	/**
	 * How far from its patches the location reaches, in tiles.
	 */
	private final int radius;
	/**
	 * The herb patch first, then allotments, the flower patch, the tree and the fruit tree.
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
			built.add(new FarmPatch(this, tree.type, tree.type == PatchType.TREE ? "Tree patch" : "Fruit tree patch",
				patches.region, tree.varbit, tree.point, tree.gardener, tree.level));
		}
		this.patches = List.copyOf(built);
	}

	@Nullable
	FarmPatch herbPatch()
	{
		List<FarmPatch> herbs = patches(PatchType.HERB);
		return herbs.isEmpty() ? null : herbs.get(0);
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
	 * The patches that make this location a stop on a run of the type: its herb patch, or its trees.
	 */
	List<FarmPatch> anchors(RunType runType)
	{
		return anchors(runType, Integer.MAX_VALUE);
	}

	/**
	 * The anchor patches a player with the Farming level can get to.
	 */
	List<FarmPatch> anchors(RunType runType, int farmingLevel)
	{
		List<FarmPatch> anchors = new ArrayList<>();
		for (FarmPatch patch : patches)
		{
			if (runType.getAnchorTypes().contains(patch.getType()) && patch.getFarmingLevel() <= farmingLevel)
			{
				anchors.add(patch);
			}
		}
		return anchors;
	}

	boolean has(RunType runType)
	{
		return !anchors(runType).isEmpty();
	}

	/**
	 * Where a run of the type goes to at this location.
	 */
	WorldPoint pointFor(RunType runType)
	{
		List<FarmPatch> anchors = anchors(runType);
		return (anchors.isEmpty() ? patches : anchors).get(0).getPoint();
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
	 * A run's locations in the order the OSRS Wiki's farming run guides visit them.
	 */
	static List<FarmLocation> recommendedOrder(RunType runType)
	{
		if (runType == RunType.TREE)
		{
			return List.of(GNOME_STRONGHOLD, TREE_GNOME_VILLAGE, FARMING_GUILD, LUMBRIDGE, VARROCK, FALADOR_PARK, TAVERLEY,
				CATHERBY_ORCHARD, BRIMHAVEN, LLETYA, KASTORI, AUBURNVALE);
		}
		return List.of(FALADOR, MORYTANIA, ARDOUGNE, CATHERBY, HOSIDIUS, FARMING_GUILD, CIVITAS, TROLL_STRONGHOLD, WEISS, HARMONY);
	}

	static List<FarmLocation> of(RunType runType)
	{
		List<FarmLocation> locations = new ArrayList<>();
		for (FarmLocation location : values())
		{
			if (location.has(runType))
			{
				locations.add(location);
			}
		}
		return locations;
	}

	/**
	 * What the player can pick as this location's teleport: any item that goes here, one teleport
	 * in particular, a way into their house, or none.
	 */
	List<Teleport> teleportChoices()
	{
		List<Teleport> choices = new ArrayList<>();
		choices.add(Teleport.ANY);
		choices.addAll(teleports);
		choices.addAll(HOUSE_TELEPORTS);
		choices.add(Teleport.NONE);
		return choices;
	}

	/**
	 * The teleports that satisfy a choice: carrying any one of them is enough. A choice that doesn't
	 * go to this location is treated as {@link Teleport#ANY}, which is any of this location's
	 * teleport items but not its spells.
	 */
	List<Teleport> teleportsFor(Teleport choice)
	{
		if (choice == Teleport.NONE)
		{
			return List.of();
		}
		if (teleports.contains(choice) || HOUSE_TELEPORTS.contains(choice))
		{
			return List.of(choice);
		}

		List<Teleport> items = new ArrayList<>();
		for (Teleport teleport : teleports)
		{
			if (!teleport.isSpell())
			{
				items.add(teleport);
			}
		}
		return items;
	}

	/**
	 * Collects a location's patches while its enum constant is being declared.
	 */
	static final class Patches
	{
		private static final class Tree
		{
			private final PatchType type;
			private final int varbit;
			private final WorldPoint point;
			private final int gardener;
			private final int level;

			private Tree(PatchType type, int varbit, WorldPoint point, int gardener, int level)
			{
				this.type = type;
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
			trees.add(new Tree(PatchType.TREE, varbit, point, gardener, level));
			return this;
		}

		Patches fruitTree(int varbit, WorldPoint point, int gardener)
		{
			return fruitTree(varbit, point, gardener, 1);
		}

		Patches fruitTree(int varbit, WorldPoint point, int gardener, int level)
		{
			trees.add(new Tree(PatchType.FRUIT_TREE, varbit, point, gardener, level));
			return this;
		}
	}
}
