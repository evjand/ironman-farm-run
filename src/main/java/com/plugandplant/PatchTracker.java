package com.plugandplant;

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
	private static final String PROTECTED_SUFFIX = ".protected";

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
		return readMarked(COMPOST_SUFFIX);
	}

	/**
	 * The trees Time Tracking has seen a gardener paid to protect. It notes this when the payment is
	 * made and forgets it when the tree is harvested or dies.
	 */
	Set<FarmPatch> readProtected()
	{
		return readMarked(PROTECTED_SUFFIX);
	}

	private Set<FarmPatch> readMarked(String suffix)
	{
		Set<FarmPatch> marked = new HashSet<>();
		for (FarmLocation location : FarmLocation.values())
		{
			for (FarmPatch patch : location.getPatches())
			{
				String value = configManager.getRSProfileConfiguration(TIME_TRACKING_GROUP, patch.timeTrackingKey() + suffix);
				if (value != null && !"false".equals(value))
				{
					marked.add(patch);
				}
			}
		}
		return marked;
	}
}
