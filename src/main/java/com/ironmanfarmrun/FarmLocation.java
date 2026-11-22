package com.ironmanfarmrun;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.VarbitID;

/**
 * The farming locations with a herb patch, each with the allotments and flower patch beside it.
 * Regions and varbits are the ones the Time Tracking plugin files each patch under.
 */
@Getter
public enum FarmLocation
{
	FALADOR("Falador", 12083, new WorldPoint(3058, 3311, 0), 20,
		new Patches(VarbitID.FARMING_TRANSMIT_D)
			.allotment("North-west allotment", VarbitID.FARMING_TRANSMIT_A)
			.allotment("South-east allotment", VarbitID.FARMING_TRANSMIT_B)
			.flower(VarbitID.FARMING_TRANSMIT_C),
		Teleport.EXPLORERS_RING, Teleport.FALADOR_TAB, Teleport.FALADOR_SPELL),
	CATHERBY("Catherby", 11062, new WorldPoint(2813, 3463, 0), 20,
		new Patches(VarbitID.FARMING_TRANSMIT_D)
			.allotment("North allotment", VarbitID.FARMING_TRANSMIT_A)
			.allotment("South allotment", VarbitID.FARMING_TRANSMIT_B)
			.flower(VarbitID.FARMING_TRANSMIT_C),
		Teleport.CATHERBY_TAB, Teleport.CATHERBY_SPELL, Teleport.CAMELOT_TAB, Teleport.CAMELOT_SPELL),
	ARDOUGNE("Ardougne", 10548, new WorldPoint(2670, 3374, 0), 20,
		new Patches(VarbitID.FARMING_TRANSMIT_D)
			.allotment("North allotment", VarbitID.FARMING_TRANSMIT_A)
			.allotment("South allotment", VarbitID.FARMING_TRANSMIT_B)
			.flower(VarbitID.FARMING_TRANSMIT_C),
		Teleport.ARDOUGNE_CLOAK, Teleport.ARDOUGNE_TAB, Teleport.ARDOUGNE_SPELL),
	MORYTANIA("Morytania", 14391, new WorldPoint(3605, 3529, 0), 20,
		new Patches(VarbitID.FARMING_TRANSMIT_D)
			.allotment("North-west allotment", VarbitID.FARMING_TRANSMIT_A)
			.allotment("South-east allotment", VarbitID.FARMING_TRANSMIT_B)
			.flower(VarbitID.FARMING_TRANSMIT_C),
		Teleport.ECTOPHIAL, Teleport.FENKENSTRAIN_TAB, Teleport.FENKENSTRAIN_SPELL),
	HOSIDIUS("Hosidius", 6967, new WorldPoint(1738, 3550, 0), 20,
		new Patches(VarbitID.FARMING_TRANSMIT_D)
			.allotment("North-east allotment", VarbitID.FARMING_TRANSMIT_A)
			.allotment("South-west allotment", VarbitID.FARMING_TRANSMIT_B)
			.flower(VarbitID.FARMING_TRANSMIT_C),
		Teleport.XERICS_TALISMAN, Teleport.HOSIDIUS_TAB),
	// The guild's herb patch is a walk away from its allotments, hence the wider radius
	FARMING_GUILD("Farming Guild", 4922, new WorldPoint(1238, 3726, 0), 45,
		new Patches(VarbitID.FARMING_TRANSMIT_E)
			.allotment("North allotment", VarbitID.FARMING_TRANSMIT_C)
			.allotment("South allotment", VarbitID.FARMING_TRANSMIT_D)
			.flower(VarbitID.FARMING_TRANSMIT_H),
		Teleport.SKILLS_NECKLACE, Teleport.FARMING_CAPE, Teleport.BATTLEFRONT_SPELL),
	CIVITAS("Civitas illa Fortis", 6192, new WorldPoint(1582, 3094, 0), 20,
		new Patches(VarbitID.FARMING_TRANSMIT_D)
			.allotment("North allotment", VarbitID.FARMING_TRANSMIT_A)
			.allotment("South allotment", VarbitID.FARMING_TRANSMIT_B)
			.flower(VarbitID.FARMING_TRANSMIT_C),
		Teleport.QUETZAL_WHISTLE, Teleport.CIVITAS_TAB, Teleport.CIVITAS_SPELL),
	TROLL_STRONGHOLD("Troll Stronghold", 11321, new WorldPoint(2826, 3694, 0), 20,
		new Patches(VarbitID.FARMING_TRANSMIT_A),
		Teleport.STONY_BASALT, Teleport.TROLLHEIM_TAB, Teleport.TROLLHEIM_SPELL),
	WEISS("Weiss", 11325, new WorldPoint(2848, 3934, 0), 20,
		new Patches(VarbitID.FARMING_TRANSMIT_A),
		Teleport.ICY_BASALT),
	HARMONY("Harmony Island", 15148, new WorldPoint(3789, 2837, 0), 20,
		new Patches(VarbitID.FARMING_TRANSMIT_B)
			.allotment("Allotment", VarbitID.FARMING_TRANSMIT_A),
		Teleport.HARMONY_TAB, Teleport.HARMONY_SPELL, Teleport.ECTOPHIAL);

	private static final List<Teleport> HOUSE_TELEPORTS = List.of(Teleport.HOUSE_TAB, Teleport.HOUSE_SPELL, Teleport.HOUSE_CAPE);

	private final String displayName;
	private final int regionId;
	/**
	 * The herb patch, which is where routes to this location lead.
	 */
	private final WorldPoint point;
	/**
	 * How far from {@link #point} this location's patches reach, in tiles.
	 */
	private final int radius;
	/**
	 * The herb patch first, then allotments, then the flower patch.
	 */
	private final List<FarmPatch> patches;
	/**
	 * The teleports that land near this location.
	 */
	private final List<Teleport> teleports;

	FarmLocation(String displayName, int regionId, WorldPoint point, int radius, Patches patches, Teleport... teleports)
	{
		this.displayName = displayName;
		this.regionId = regionId;
		this.point = point;
		this.radius = radius;
		this.teleports = List.of(teleports);

		List<FarmPatch> built = new ArrayList<>();
		built.add(new FarmPatch(this, PatchType.HERB, "Herb patch", patches.herb));
		for (int i = 0; i < patches.allotmentNames.size(); i++)
		{
			built.add(new FarmPatch(this, PatchType.ALLOTMENT, patches.allotmentNames.get(i), patches.allotmentVarbits.get(i)));
		}
		if (patches.flower != -1)
		{
			built.add(new FarmPatch(this, PatchType.FLOWER, "Flower patch", patches.flower));
		}
		this.patches = List.copyOf(built);
	}

	FarmPatch herbPatch()
	{
		return patches.get(0);
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
	 * Whether a point is close enough to be at this location.
	 */
	boolean isNear(WorldPoint other, int slack)
	{
		return other.getPlane() == point.getPlane() && other.distanceTo2D(point) <= radius + slack;
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
		private final int herb;
		private final List<String> allotmentNames = new ArrayList<>();
		private final List<Integer> allotmentVarbits = new ArrayList<>();
		private int flower = -1;

		Patches(int herbVarbit)
		{
			this.herb = herbVarbit;
		}

		Patches allotment(String name, int varbit)
		{
			allotmentNames.add(name);
			allotmentVarbits.add(varbit);
			return this;
		}

		Patches flower(int varbit)
		{
			this.flower = varbit;
			return this;
		}
	}
}
