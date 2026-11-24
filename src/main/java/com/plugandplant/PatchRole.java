package com.plugandplant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * What a run does with a kind of patch.
 */
@Getter
@RequiredArgsConstructor
enum PatchRole
{
	OFF("Off"),
	/**
	 * Tended at the stops the run makes anyway, for as long as the player has seeds for it.
	 */
	TEND("Tend while there"),
	/**
	 * Makes a location a stop when it needs attention.
	 */
	VISIT("Visit for");

	private final String label;

	@Override
	public String toString()
	{
		return label;
	}
}
