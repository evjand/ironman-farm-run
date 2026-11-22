package com.ironmanfarmrun;

import java.util.Set;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * What a run includes, chosen in the side panel right before a run. Each is saved as a plugin setting.
 */
@Getter
@RequiredArgsConstructor
enum RunOption
{
	COMPOST(IronmanFarmRunConfig.COMPOST_STEP, "Compost",
		"Compost each patch you plant, while you carry compost", Set.of(RunType.HERB, RunType.TREE)),
	ALLOTMENTS(IronmanFarmRunConfig.ALLOTMENTS, "Allotments",
		"Tend the allotments at each stop, for as long as you carry the seeds to replant them (three of a kind per patch)",
		Set.of(RunType.HERB)),
	FLOWERS(IronmanFarmRunConfig.FLOWERS, "Flowers",
		"Tend the flower patch at each stop, for as long as you carry flower seeds", Set.of(RunType.HERB)),
	PROTECT(IronmanFarmRunConfig.PROTECT_TREES, "Pay gardeners",
		"Pay each gardener to protect the tree you plant. The checklist and bank filter show the payment each sapling takes.",
		Set.of(RunType.TREE)),
	GRACEFUL(IronmanFarmRunConfig.GRACEFUL, "Graceful",
		"Add the graceful outfit to the checklist and the bank filter", Set.of(RunType.HERB, RunType.TREE)),
	VOLCANIC_ASH(IronmanFarmRunConfig.VOLCANIC_ASH, "Volcanic ash",
		"Add volcanic ash to the checklist and the bank filter", Set.of(RunType.HERB, RunType.TREE));

	private final String configKey;
	private final String label;
	private final String description;
	/**
	 * The runs the option applies to.
	 */
	private final Set<RunType> runTypes;

	boolean isOn(IronmanFarmRunConfig config)
	{
		switch (this)
		{
			case COMPOST:
				return config.compostStep();
			case ALLOTMENTS:
				return config.allotments();
			case FLOWERS:
				return config.flowers();
			case PROTECT:
				return config.protectTrees();
			case GRACEFUL:
				return config.graceful();
			default:
				return config.volcanicAsh();
		}
	}
}
