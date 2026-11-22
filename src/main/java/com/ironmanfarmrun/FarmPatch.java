package com.ironmanfarmrun;

import lombok.Getter;

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
	 * The varbit that holds this patch's state while the player is at the location.
	 */
	private final int varbit;

	FarmPatch(FarmLocation location, PatchType type, String name, int varbit)
	{
		this.location = location;
		this.type = type;
		this.name = name;
		this.varbit = varbit;
	}

	/**
	 * The key Time Tracking stores this patch's last seen state under.
	 */
	String timeTrackingKey()
	{
		return location.getRegionId() + "." + varbit;
	}

	@Override
	public String toString()
	{
		return location.getDisplayName() + " " + name;
	}
}
