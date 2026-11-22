package com.ironmanfarmrun;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.config.ConfigManager;

/**
 * Reads patch states out of the Time Tracking plugin's saved data for the logged in account.
 */
@Singleton
class PatchTracker
{
	static final String TIME_TRACKING_GROUP = "timetracking";
	private static final String FARM_TICK_OFFSET = "farmTickOffset";
	private static final String FARM_TICK_OFFSET_PRECISION = "farmTickOffsetPrecision";
	private static final String COMPOST_SUFFIX = ".compost";

	private final ConfigManager configManager;

	@Inject
	PatchTracker(ConfigManager configManager)
	{
		this.configManager = configManager;
	}

	Map<FarmPatch, PatchState> readStates(long now)
	{
		Integer offsetPrecision = configManager.getRSProfileConfiguration(TIME_TRACKING_GROUP, FARM_TICK_OFFSET_PRECISION, int.class);
		Integer offset = configManager.getRSProfileConfiguration(TIME_TRACKING_GROUP, FARM_TICK_OFFSET, int.class);

		Map<FarmPatch, PatchState> states = new LinkedHashMap<>();
		for (FarmLocation location : FarmLocation.values())
		{
			for (FarmPatch patch : location.getPatches())
			{
				String stored = configManager.getRSProfileConfiguration(TIME_TRACKING_GROUP, patch.timeTrackingKey());
				states.put(patch, PatchDecoder.predict(patch.getType(), stored, now, offsetPrecision, offset));
			}
		}
		return states;
	}

	/**
	 * The patches Time Tracking has seen treated with compost. It notes this when the compost goes on
	 * and forgets it when the crop is harvested or dies.
	 */
	Set<FarmPatch> readComposted()
	{
		Set<FarmPatch> composted = new HashSet<>();
		for (FarmLocation location : FarmLocation.values())
		{
			for (FarmPatch patch : location.getPatches())
			{
				if (configManager.getRSProfileConfiguration(TIME_TRACKING_GROUP, patch.timeTrackingKey() + COMPOST_SUFFIX) != null)
				{
					composted.add(patch);
				}
			}
		}
		return composted;
	}
}
