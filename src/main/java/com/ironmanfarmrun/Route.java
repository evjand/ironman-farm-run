package com.ironmanfarmrun;

import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Value;

/**
 * Everything a path request to Shortest Path depends on. The shown path is only asked for again when
 * one of these changes.
 */
@Value
class Route
{
	/**
	 * Which point of each location the route leads to.
	 */
	RunType runType;
	/**
	 * The locations left to visit, each with the seeds the player needs on arrival. Each map is "any
	 * one of these item ids, at least that many", and every map in the list is needed. An empty list
	 * means no requirement, so no bank detour.
	 */
	Map<FarmLocation, List<Map<Integer, Integer>>> stops;
	/**
	 * Only let the route use teleport items the player is carrying, whatever their Shortest Path
	 * settings say, so it follows the teleports they chose to bring.
	 */
	boolean carriedTeleportsOnly;
	/**
	 * Let the route go through the player's house, because they picked it for a stop.
	 */
	boolean useHouse;
	/**
	 * Let the route use teleport spells, because the player picked one for a stop.
	 */
	boolean useSpells;
	/**
	 * What Shortest Path would find on the player that bears on the route. It only looks once per
	 * search, so a change here means asking again.
	 */
	Set<Integer> carriedRouteItems;
}
