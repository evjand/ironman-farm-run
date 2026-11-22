package com.plugandplant;

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
	/**
	 * The kind of run shown: the one in progress, or the one the player has picked.
	 */
	RunType runType;
	Map<FarmPatch, PatchState> patches;
	/**
	 * The player's choices per location, for the run type shown.
	 */
	Map<FarmLocation, LocationSettings> settings;
	/**
	 * The stops left on the current run, or the ones the next run would make.
	 */
	Set<FarmLocation> targets;
	/**
	 * The stops of the run in progress, finished ones included; empty without a run.
	 */
	List<StopView> runStops;
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
	 * The player's Farming level, boosts included, which decides the Farming Guild patches they can reach.
	 */
	int farmingLevel;
	/**
	 * Unix time in whole minutes, so the state only changes when a countdown would.
	 */
	long nowMinutes;
	boolean timeTrackingActive;
	boolean shortestPathActive;
	boolean bankTagsActive;
}
