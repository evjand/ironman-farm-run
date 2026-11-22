package com.plugandplant;

import java.util.Set;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * The kinds of run. Each has the patch types that make a location a stop; a herb run also tends
 * allotments and flowers at its stops if the player wants.
 */
@Getter
@RequiredArgsConstructor
public enum RunType
{
	HERB("Herbs", "herb run", Set.of(PatchType.HERB)),
	TREE("Trees", "tree run", Set.of(PatchType.TREE, PatchType.FRUIT_TREE));

	private final String label;
	private final String noun;
	/**
	 * A location is a stop when one of its patches of these types needs attention.
	 */
	private final Set<PatchType> anchorTypes;

	static RunType of(PatchType type)
	{
		return type == PatchType.TREE || type == PatchType.FRUIT_TREE ? TREE : HERB;
	}
}
