package com.plugandplant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum PatchStatus
{
	READY("Ready", true),
	/**
	 * A tree that has finished growing and waits for its health check.
	 */
	CHECK_HEALTH("Check health", true),
	/**
	 * A tree whose health has been checked, still standing.
	 */
	GROWN("Grown", true),
	STUMP("Stump", true),
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

	/**
	 * Whether whatever is in the patch makes way for a new crop: everything that needs a visit but
	 * a crop that only needs curing.
	 */
	boolean leadsToReplanting()
	{
		return needsVisit && this != DISEASED;
	}
}
