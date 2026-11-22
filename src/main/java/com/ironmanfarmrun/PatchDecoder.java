package com.ironmanfarmrun;

import javax.annotation.Nullable;

/**
 * Turns what the Time Tracking plugin last saw at a patch into its state now.
 *
 * Time Tracking stores each patch as "varbitValue:unixSeconds". The growth-tick maths is mirrored
 * from its FarmingTracker so both plugins agree on when a crop is done.
 */
final class PatchDecoder
{
	private PatchDecoder()
	{
	}

	/**
	 * @param stored              Time Tracking's stored value for the patch, or null if it has none
	 * @param now                 unix time in seconds
	 * @param offsetPrecisionMins Time Tracking's farmTickOffsetPrecision for the account, if known
	 * @param offsetMins          Time Tracking's farmTickOffset for the account, if known
	 */
	static PatchState predict(PatchType type, @Nullable String stored, long now,
		@Nullable Integer offsetPrecisionMins, @Nullable Integer offsetMins)
	{
		if (stored == null)
		{
			return PatchState.UNKNOWN;
		}

		String[] parts = stored.split(":");
		if (parts.length != 2)
		{
			return PatchState.UNKNOWN;
		}

		int value;
		long seenAt;
		try
		{
			value = Integer.parseInt(parts[0]);
			seenAt = Long.parseLong(parts[1]);
		}
		catch (NumberFormatException e)
		{
			return PatchState.UNKNOWN;
		}

		int[] table = type.getStateTable();
		if (seenAt <= 0 || value < 0 || value >= table.length)
		{
			return PatchState.UNKNOWN;
		}

		int code = table[value];
		Sapling crop = type.getCrops() == null ? null : type.getCrops()[value];
		if (code >= 0)
		{
			// A healthy crop with that many growth ticks to go, counted from the tick it was seen in
			int tick = type.getTickMinutes();
			long seenTick = tickTime(tick, 0, seenAt, offsetPrecisionMins, offsetMins);
			long readyAt = tickTime(tick, code, seenTick, offsetPrecisionMins, offsetMins);
			return readyAt <= now
				? new PatchState(type.grownStatus(), 0, crop)
				: new PatchState(PatchStatus.GROWING, readyAt, crop);
		}

		PatchStatus status = settledStatus(code);
		return new PatchState(status, 0, status == PatchStatus.WEEDS || status == PatchStatus.EMPTY ? null : crop);
	}

	private static PatchStatus settledStatus(int code)
	{
		switch (code)
		{
			case PatchStateTables.CLEAR:
				return PatchStatus.EMPTY;
			case PatchStateTables.WEEDS:
				return PatchStatus.WEEDS;
			case PatchStateTables.READY:
				return PatchStatus.READY;
			case PatchStateTables.DISEASED:
				return PatchStatus.DISEASED;
			case PatchStateTables.DEAD:
				return PatchStatus.DEAD;
			case PatchStateTables.CHECK_HEALTH:
				return PatchStatus.CHECK_HEALTH;
			case PatchStateTables.GROWN:
				return PatchStatus.GROWN;
			case PatchStateTables.STUMP:
				return PatchStatus.STUMP;
			default:
				return PatchStatus.UNKNOWN;
		}
	}

	/**
	 * The time of the growth tick {@code ticks} after the one containing {@code time}, in unix seconds.
	 */
	static long tickTime(int tickRateMins, int ticks, long time,
		@Nullable Integer offsetPrecisionMins, @Nullable Integer offsetMins)
	{
		// Offsets are negative but are stored as positive
		long offset = 0;
		if (offsetPrecisionMins != null && offsetMins != null
			&& (offsetPrecisionMins >= tickRateMins || offsetPrecisionMins >= 40))
		{
			offset = (offsetMins % tickRateMins) * 60L;
		}

		long shifted = time + offset;
		long tickSeconds = tickRateMins * 60L;
		long currentTick = shifted - (shifted % tickSeconds);
		return currentTick + ticks * tickSeconds - offset;
	}
}
