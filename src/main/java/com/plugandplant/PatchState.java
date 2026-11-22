package com.plugandplant;

import javax.annotation.Nullable;
import lombok.Value;

@Value
public class PatchState
{
	static final PatchState UNKNOWN = new PatchState(PatchStatus.UNKNOWN, 0);

	PatchStatus status;
	/**
	 * Unix time in seconds the crop finishes growing; 0 unless the status is {@link PatchStatus#GROWING}.
	 */
	long readyAt;
	/**
	 * The tree in the patch, for tree patches with one in.
	 */
	@Nullable
	Sapling crop;

	PatchState(PatchStatus status, long readyAt, @Nullable Sapling crop)
	{
		this.status = status;
		this.readyAt = readyAt;
		this.crop = crop;
	}

	PatchState(PatchStatus status, long readyAt)
	{
		this(status, readyAt, null);
	}
}
