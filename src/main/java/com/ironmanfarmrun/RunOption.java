package com.ironmanfarmrun;

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
		"Compost each patch you plant, while you carry compost"),
	ALLOTMENTS(IronmanFarmRunConfig.ALLOTMENTS, "Allotments",
		"Tend the allotments at each stop, for as long as you carry the seeds to replant them (three of a kind per patch)"),
	FLOWERS(IronmanFarmRunConfig.FLOWERS, "Flowers",
		"Tend the flower patch at each stop, for as long as you carry flower seeds"),
	GRACEFUL(IronmanFarmRunConfig.GRACEFUL, "Graceful",
		"Add the graceful outfit to the checklist and the bank filter"),
	VOLCANIC_ASH(IronmanFarmRunConfig.VOLCANIC_ASH, "Volcanic ash",
		"Add volcanic ash to the checklist and the bank filter");

	private final String configKey;
	private final String label;
	private final String description;

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
			case GRACEFUL:
				return config.graceful();
			default:
				return config.volcanicAsh();
		}
	}
}
