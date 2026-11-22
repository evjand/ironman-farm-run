package com.ironmanfarmrun;

import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import lombok.Value;

/**
 * Everything the side panel shows. Built on the client thread and handed to the panel as a whole,
 * so the panel never reads game state itself.
 */
@Value
class PanelState
{
	boolean loggedIn;
	boolean runActive;
	Map<FarmPatch, PatchState> patches;
	Map<FarmLocation, LocationSettings> settings;
	/**
	 * The stops left on the current run, or the ones the next run would make.
	 */
	Set<FarmLocation> targets;
	List<ChecklistItem> checklist;
	/**
	 * The run options that are on.
	 */
	Set<RunOption> runOptions;
	/**
	 * The farming contract, described for the panel; null without one a run can help with.
	 */
	@Nullable
	String contract;
	/**
	 * Unix time in whole minutes, so the state only changes when a countdown would.
	 */
	long nowMinutes;
	boolean timeTrackingActive;
	boolean shortestPathActive;
	boolean bankTagsActive;
}
