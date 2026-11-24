package com.plugandplant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * A run preset's options, chosen in the side panel before a run.
 */
@Getter
@RequiredArgsConstructor
enum RunOption
{
	COMPOST("Compost", "Compost each patch you plant, while you carry compost", false),
	PROTECT("Pay gardeners",
		"Pay each gardener to protect the tree you plant. The checklist and bank filter show the payment each sapling takes.",
		true),
	GRACEFUL("Graceful", "Add the graceful outfit to the checklist and the bank filter", false),
	VOLCANIC_ASH("Volcanic ash", "Add volcanic ash to the checklist and the bank filter", false);

	private final String label;
	private final String description;
	/**
	 * Only for runs with trees.
	 */
	private final boolean treesOnly;

	boolean appliesTo(RunPreset preset)
	{
		return !treesOnly || preset.includesTrees();
	}

	boolean isOn(RunPreset preset)
	{
		switch (this)
		{
			case COMPOST:
				return preset.isCompost();
			case PROTECT:
				return preset.isProtect();
			case GRACEFUL:
				return preset.isGraceful();
			default:
				return preset.isVolcanicAsh();
		}
	}

	RunPreset set(RunPreset preset, boolean on)
	{
		switch (this)
		{
			case COMPOST:
				return preset.withCompost(on);
			case PROTECT:
				return preset.withProtect(on);
			case GRACEFUL:
				return preset.withGraceful(on);
			default:
				return preset.withVolcanicAsh(on);
		}
	}
}
