package com.ironmanfarmrun;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.events.PluginMessage;

/**
 * Asks the Shortest Path plugin to draw routes, using its plugin message API. Hub plugins can't share
 * classes, so the messages are plain maps; they go nowhere if Shortest Path isn't installed.
 */
@Singleton
class ShortestPathRouter
{
	private static final String NAMESPACE = "shortestpath";

	private final EventBus eventBus;
	private boolean routed;
	private boolean overridingSettings;

	@Inject
	ShortestPathRouter(EventBus eventBus)
	{
		this.eventBus = eventBus;
	}

	/**
	 * Shows the path to whichever of the route's stops is cheapest to reach from where the player is.
	 */
	void show(Route route)
	{
		Map<String, Object> message = pathMessage(route);
		boolean overrides = message.containsKey("config");
		if (overridingSettings && !overrides)
		{
			// A path message can replace setting overrides but not remove them; only a clear does that
			clear();
		}

		eventBus.post(new PluginMessage(NAMESPACE, "path", message));
		routed = true;
		overridingSettings = overrides;
	}

	/**
	 * Removes the path and setting overrides, if this plugin asked for them.
	 */
	void clear()
	{
		if (routed)
		{
			eventBus.post(new PluginMessage(NAMESPACE, "clear", new HashMap<>()));
			routed = false;
			overridingSettings = false;
		}
	}

	static Map<String, Object> pathMessage(Route route)
	{
		List<Object> targets = new ArrayList<>();
		for (Map.Entry<FarmLocation, Map<Integer, Integer>> stop : route.getStops().entrySet())
		{
			if (stop.getValue().isEmpty())
			{
				targets.add(stop.getKey().getPoint());
			}
			else
			{
				Map<String, Object> target = new HashMap<>();
				target.put("point", stop.getKey().getPoint());
				// A map of item id to quantity is "any one of these items, at least this many"
				target.put("requirements", stop.getValue());
				targets.add(target);
			}
		}

		Map<String, Object> data = new HashMap<>();
		data.put("target", targets);

		// Setting names and option values as Shortest Path's settings panel has them
		Map<String, Object> settings = new HashMap<>();
		if (route.isCarriedTeleportsOnly())
		{
			settings.put("useTeleportationItems", "Inventory");
		}
		if (route.isUseHouse())
		{
			settings.put("usePoh", true);
		}
		if (route.isUseSpells())
		{
			settings.put("useTeleportationSpells", true);
		}
		if (!settings.isEmpty())
		{
			data.put("config", settings);
		}
		return data;
	}
}
