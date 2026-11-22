package com.plugandplant;

import java.awt.Color;
import java.util.List;
import javax.annotation.Nullable;
import lombok.Value;

/**
 * A stop of the run in progress, as the side panel lists it.
 */
@Value
class StopView
{
	enum Stage
	{
		DONE,
		/**
		 * Where the player is, or where the route leads.
		 */
		CURRENT,
		UPCOMING
	}

	/**
	 * A patch at the current stop and what to do there.
	 */
	@Value
	static class Task
	{
		String patch;
		String label;
		Color color;
		/**
		 * The first thing to do at the stop.
		 */
		boolean next;
	}

	FarmLocation location;
	Stage stage;
	/**
	 * Every patch tended at the stop, for the current stop; empty for the others.
	 */
	List<Task> tasks;
	/**
	 * How many patches still have something to do.
	 */
	int tasksLeft;
	/**
	 * The teleport the player picked for the stop, if a particular one.
	 */
	@Nullable
	String teleport;
}
