package com.ironmanfarmrun;

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
}
