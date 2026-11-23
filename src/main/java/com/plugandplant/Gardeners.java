package com.plugandplant;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.NpcChanged;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.client.eventbus.Subscribe;

/**
 * Keeps track of the gardeners in the scene who look after tree patches, so they can be highlighted
 * when it is time to pay them.
 *
 * A gardener is recognised by the NPC id Time Tracking knows them by, or else as an NPC offering to
 * be paid right beside a tree patch: some gardeners change ids with quests and game updates.
 */
@Singleton
class Gardeners
{
	/**
	 * How far from their tree a gardener without a known id may stand and still be taken for its gardener.
	 */
	private static final int GARDENER_RANGE = 12;

	private final Client client;
	private final Map<NPC, FarmPatch> gardeners = new HashMap<>();

	@Inject
	Gardeners(Client client)
	{
		this.client = client;
	}

	/**
	 * The gardeners in the scene, with the patch each looks after.
	 */
	Map<NPC, FarmPatch> get()
	{
		return Collections.unmodifiableMap(gardeners);
	}

	/**
	 * Finds the gardeners already in the scene. Only needed when tracking starts while logged in, as
	 * spawn events cover everything after that.
	 */
	void scan()
	{
		gardeners.clear();
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		for (NPC npc : client.getTopLevelWorldView().npcs())
		{
			update(npc);
		}
	}

	void clear()
	{
		gardeners.clear();
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned event)
	{
		update(event.getNpc());
	}

	@Subscribe
	public void onNpcChanged(NpcChanged event)
	{
		update(event.getNpc());
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned event)
	{
		gardeners.remove(event.getNpc());
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		// Unlike objects, NPCs still in view aren't spawned again after the map loads a new area, so the
		// gardeners are kept through loading and checked again once it is done
		switch (event.getGameState())
		{
			case LOGGED_IN:
				scan();
				break;
			case LOADING:
				break;
			default:
				gardeners.clear();
				break;
		}
	}

	private void update(NPC npc)
	{
		FarmPatch patch = patchOf(npc);
		if (patch != null)
		{
			gardeners.put(npc, patch);
		}
		else
		{
			gardeners.remove(npc);
		}
	}

	@Nullable
	private static FarmPatch patchOf(NPC npc)
	{
		WorldPoint point = npc.getWorldLocation();
		FarmPatch nearest = null;
		for (FarmLocation location : FarmLocation.values())
		{
			if (!location.isNear(point, 0))
			{
				continue;
			}

			for (FarmPatch patch : location.getPatches())
			{
				if (!patch.getType().isTree() || point.getPlane() != patch.getPoint().getPlane())
				{
					continue;
				}
				if (patch.getGardener() == npc.getId())
				{
					return patch;
				}
				if (point.distanceTo2D(patch.getPoint()) <= GARDENER_RANGE
					&& (nearest == null || point.distanceTo2D(patch.getPoint()) < point.distanceTo2D(nearest.getPoint())))
				{
					nearest = patch;
				}
			}
		}
		return nearest != null && offersPayment(npc) ? nearest : null;
	}

	private static boolean offersPayment(NPC npc)
	{
		NPCComposition composition = npc.getTransformedComposition();
		if (composition == null)
		{
			composition = npc.getComposition();
		}
		if (composition == null || composition.getActions() == null)
		{
			return false;
		}

		for (String action : composition.getActions())
		{
			if (action != null && action.startsWith("Pay"))
			{
				return true;
			}
		}
		return false;
	}
}
