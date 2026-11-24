package com.plugandplant;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Value;
import lombok.With;
import net.runelite.api.coords.WorldPoint;

/**
 * A run the player has set up: what it does with each kind of patch, which locations and patches it
 * leaves out, and its options. A herb run and a tree run are made for every player to start with.
 */
@Value
@With
class RunPreset
{
	static final String HERB_ID = "herb";
	static final String TREE_ID = "tree";

	/**
	 * The patch types a run can cover, in the order the side panel lists them.
	 */
	static final List<PatchType> TYPES = List.of(PatchType.HERB, PatchType.FLOWER, PatchType.ALLOTMENT, PatchType.TREE,
		PatchType.FRUIT_TREE);

	String id;
	String name;
	Map<PatchType, PatchRole> roles;
	/**
	 * Locations the player has left out of the run entirely.
	 */
	Set<FarmLocation> disabledLocations;
	/**
	 * Kinds of patch left out at a particular location, such as the fruit tree at the Farming Guild.
	 */
	Map<FarmLocation, Set<PatchType>> excludedTypes;
	boolean compost;
	boolean protect;
	boolean graceful;
	boolean volcanicAsh;

	static RunPreset herbRun()
	{
		return new RunPreset(HERB_ID, "Herb run", roles(PatchType.HERB, PatchRole.VISIT), Set.of(), Map.of(),
			true, true, false, false);
	}

	static RunPreset treeRun()
	{
		Map<PatchType, PatchRole> roles = roles(PatchType.TREE, PatchRole.VISIT);
		roles.put(PatchType.FRUIT_TREE, PatchRole.VISIT);
		return new RunPreset(TREE_ID, "Tree run", Collections.unmodifiableMap(roles), Set.of(), Map.of(),
			true, true, false, false);
	}

	private static Map<PatchType, PatchRole> roles(PatchType type, PatchRole role)
	{
		Map<PatchType, PatchRole> roles = new EnumMap<>(PatchType.class);
		for (PatchType each : TYPES)
		{
			roles.put(each, PatchRole.OFF);
		}
		roles.put(type, role);
		return roles;
	}

	/**
	 * What the run does with a kind of patch, wherever it is.
	 */
	PatchRole role(PatchType type)
	{
		return roles.getOrDefault(type, PatchRole.OFF);
	}

	/**
	 * What the run does with a particular patch, taking the location's own choices into account.
	 */
	PatchRole role(FarmPatch patch)
	{
		return isExcluded(patch.getLocation(), patch.getType()) ? PatchRole.OFF : role(patch.getType());
	}

	boolean isExcluded(FarmLocation location, PatchType type)
	{
		return excludedTypes.getOrDefault(location, Set.of()).contains(type);
	}

	boolean includes(PatchType type)
	{
		return role(type) != PatchRole.OFF;
	}

	boolean includesTrees()
	{
		return includes(PatchType.TREE) || includes(PatchType.FRUIT_TREE);
	}

	/**
	 * Whether the run covers patches that take seeds rather than saplings.
	 */
	boolean includesSeedPatches()
	{
		return includes(PatchType.HERB) || includes(PatchType.FLOWER) || includes(PatchType.ALLOTMENT);
	}

	boolean isEnabled(FarmLocation location)
	{
		return !disabledLocations.contains(location);
	}

	/**
	 * The patches that make the location a stop when they need attention, within the player's reach.
	 */
	List<FarmPatch> anchors(FarmLocation location, int farmingLevel)
	{
		List<FarmPatch> anchors = new ArrayList<>();
		for (FarmPatch patch : location.getPatches())
		{
			if (role(patch) == PatchRole.VISIT && patch.getFarmingLevel() <= farmingLevel)
			{
				anchors.add(patch);
			}
		}
		return anchors;
	}

	List<FarmPatch> anchors(FarmLocation location)
	{
		return anchors(location, Integer.MAX_VALUE);
	}

	/**
	 * The kinds of patch the run covers at a location, before the location's own choices.
	 */
	List<PatchType> typesAt(FarmLocation location)
	{
		List<PatchType> types = new ArrayList<>();
		for (PatchType type : TYPES)
		{
			if (includes(type) && location.getPatches().stream().anyMatch(patch -> patch.getType() == type))
			{
				types.add(type);
			}
		}
		return types;
	}

	/**
	 * The locations with a patch of a kind the run covers, in the order the run visits them.
	 */
	List<FarmLocation> locations()
	{
		List<FarmLocation> locations = new ArrayList<>();
		for (FarmLocation location : recommendedOrder())
		{
			if (!typesAt(location).isEmpty())
			{
				locations.add(location);
			}
		}
		return locations;
	}

	/**
	 * Every location in the order of the OSRS Wiki's farming run guides: the tree run's order for a
	 * run that only visits for trees, otherwise the herb run's, with the tree run's other locations after.
	 */
	List<FarmLocation> recommendedOrder()
	{
		boolean treesOnly = includesTrees() && role(PatchType.HERB) != PatchRole.VISIT
			&& role(PatchType.FLOWER) != PatchRole.VISIT && role(PatchType.ALLOTMENT) != PatchRole.VISIT;
		Set<FarmLocation> order = new LinkedHashSet<>();
		if (treesOnly)
		{
			order.addAll(FarmLocation.TREE_ORDER);
			order.addAll(FarmLocation.HERB_ORDER);
		}
		else
		{
			order.addAll(FarmLocation.HERB_ORDER);
			order.addAll(FarmLocation.TREE_ORDER);
		}
		for (FarmLocation location : FarmLocation.values())
		{
			order.add(location);
		}
		return new ArrayList<>(order);
	}

	/**
	 * Where the run goes to at a location: its first anchor patch.
	 */
	WorldPoint pointFor(FarmLocation location)
	{
		List<FarmPatch> anchors = anchors(location);
		return (anchors.isEmpty() ? location.getPatches() : anchors).get(0).getPoint();
	}

	/**
	 * The run as a lower-case noun for messages, such as "herb run".
	 */
	String noun()
	{
		return name.isEmpty() ? "run" : Character.toLowerCase(name.charAt(0)) + name.substring(1);
	}

	RunPreset withRole(PatchType type, PatchRole role)
	{
		Map<PatchType, PatchRole> changed = new EnumMap<>(PatchType.class);
		changed.putAll(roles);
		changed.put(type, role);
		return withRoles(Collections.unmodifiableMap(changed));
	}

	RunPreset withLocationEnabled(FarmLocation location, boolean enabled)
	{
		Set<FarmLocation> changed = EnumSet.noneOf(FarmLocation.class);
		changed.addAll(disabledLocations);
		if (enabled)
		{
			changed.remove(location);
		}
		else
		{
			changed.add(location);
		}
		return withDisabledLocations(Collections.unmodifiableSet(changed));
	}

	RunPreset withExcluded(FarmLocation location, PatchType type, boolean excluded)
	{
		Map<FarmLocation, Set<PatchType>> changed = new EnumMap<>(FarmLocation.class);
		changed.putAll(excludedTypes);
		Set<PatchType> types = EnumSet.noneOf(PatchType.class);
		types.addAll(excludedTypes.getOrDefault(location, Set.of()));
		if (excluded)
		{
			types.add(type);
		}
		else
		{
			types.remove(type);
		}
		if (types.isEmpty())
		{
			changed.remove(location);
		}
		else
		{
			changed.put(location, Collections.unmodifiableSet(types));
		}
		return withExcludedTypes(Collections.unmodifiableMap(changed));
	}
}
