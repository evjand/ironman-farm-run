package com.ironmanfarmrun;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum PatchStatus
{
	READY("Ready", true),
	/**
	 * Weeds that have to be raked before anything can be planted.
	 */
	WEEDS("Weeds", true),
	/**
	 * Raked and ready to plant.
	 */
	EMPTY("Empty", true),
	DISEASED("Diseased", true),
	DEAD("Dead", true),
	GROWING("Growing", false),
	/**
	 * Time Tracking has never seen this patch on this account.
	 */
	UNKNOWN("No data", false);

	@Getter
	private final String label;
	private final boolean needsVisit;

	/**
	 * Whether a run should stop at a patch in this state.
	 */
	boolean needsVisit()
	{
		return needsVisit;
	}
}
