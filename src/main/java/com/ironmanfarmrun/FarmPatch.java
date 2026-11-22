package com.ironmanfarmrun;

import lombok.Getter;
import net.runelite.api.coords.WorldPoint;

/**
 * One patch at a {@link FarmLocation}. There is a single instance per patch, so patches compare by identity.
 */
@Getter
public final class FarmPatch
{
	private final FarmLocation location;
	private final PatchType type;
	private final String name;
	/**
	 * The region Time Tracking files the patch under.
	 */
	private final int regionId;
	/**
	 * The varbit that holds this patch's state while the player is nearby.
	 */
	private final int varbit;
	/**
	 * Roughly where the patch is; exact for herb and tree patches, which routes lead to.
	 */
	private final WorldPoint point;
	/**
	 * The NPC that looks after the patch for payment, or -1.
	 */
	private final int gardener;
	/**
	 * The Farming level it takes to reach the patch, for the Farming Guild's wings; 1 elsewhere.
	 */
	private final int farmingLevel;

	FarmPatch(FarmLocation location, PatchType type, String name, int regionId, int varbit, WorldPoint point, int gardener,
		int farmingLevel)
	{
		this.location = location;
		this.type = type;
		this.name = name;
		this.regionId = regionId;
		this.varbit = varbit;
		this.point = point;
		this.gardener = gardener;
		this.farmingLevel = farmingLevel;
	}

	/**
	 * The key Time Tracking stores this patch's last seen state under.
	 */
	String timeTrackingKey()
	{
		return regionId + "." + varbit;
	}

	@Override
	public String toString()
	{
		return location.getDisplayName() + " " + name;
	}
}
