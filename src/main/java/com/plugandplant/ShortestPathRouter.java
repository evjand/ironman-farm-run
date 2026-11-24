package com.plugandplant;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.PluginMessage;

/**
 * Asks the Shortest Path plugin to draw routes, using its plugin message API. Hub plugins can't share
 * classes, so the messages are plain maps; they go nowhere if Shortest Path isn't installed.
 */
@Singleton
class ShortestPathRouter
{
	private static final String NAMESPACE = "shortestpath";
	private static final String QUERY_ID = "plug-and-plant";

	private final EventBus eventBus;
	private boolean routed;
	private boolean overridingSettings;
	/**
	 * The query asked alongside the shown path, to learn which stop it leads to; null when none is waiting.
	 */
	@Nullable
	private volatile Route queried;
	/**
	 * The stop the shown path leads to, as Shortest Path last answered; null if not known.
	 */
	@Nullable
	private volatile FarmLocation headingTo;

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

		// The path doesn't say which stop it picked, but the same search as a query does
		headingTo = null;
		queried = route;
		Map<String, Object> query = new HashMap<>();
		query.put("id", QUERY_ID);
		query.put("target", message.get("target"));
		eventBus.post(new PluginMessage(NAMESPACE, "query", query));
	}

	/**
	 * The stop the shown path leads to, once Shortest Path has said; null until then, or without it.
	 */
	@Nullable
	FarmLocation headingTo()
	{
		return headingTo;
	}

	@Subscribe
	public void onPluginMessage(PluginMessage event)
	{
		Route route = queried;
		if (!NAMESPACE.equals(event.getNamespace()) || !"result".equals(event.getName()) || route == null
			|| !QUERY_ID.equals(event.getData().get("id")))
		{
			return;
		}

		Object target = event.getData().get("target");
		if (target instanceof WorldPoint)
		{
			headingTo = stopAt(route, (WorldPoint) target);
		}
	}

	/**
	 * The stop of the route whose point Shortest Path's answer names, or the one nearest it, as the
	 * answer can be a tile beside the point when the point itself can't be stood on.
	 */
	@Nullable
	static FarmLocation stopAt(Route route, WorldPoint target)
	{
		FarmLocation nearest = null;
		int nearestDistance = Integer.MAX_VALUE;
		for (FarmLocation location : route.getStops().keySet())
		{
			WorldPoint point = route.getPreset().pointFor(location);
			int distance = point.getPlane() == target.getPlane() ? point.distanceTo2D(target) : Integer.MAX_VALUE;
			if (distance < nearestDistance)
			{
				nearest = location;
				nearestDistance = distance;
			}
		}
		return nearest != null && nearestDistance <= nearest.getRadius() ? nearest : null;
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
			queried = null;
			headingTo = null;
		}
	}

	static Map<String, Object> pathMessage(Route route)
	{
		List<Object> targets = new ArrayList<>();
		for (Map.Entry<FarmLocation, List<Map<Integer, Integer>>> stop : route.getStops().entrySet())
		{
			WorldPoint point = route.getPreset().pointFor(stop.getKey());
			if (stop.getValue().isEmpty())
			{
				targets.add(point);
			}
			else
			{
				Map<String, Object> target = new HashMap<>();
				target.put("point", point);
				// A list is "all of these"; each map in it is "any one of these items, at least this many"
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
		if (route.isUseFairyRings())
		{
			settings.put("useFairyRings", true);
		}
		if (!settings.isEmpty())
		{
			data.put("config", settings);
		}
		return data;
	}
}
