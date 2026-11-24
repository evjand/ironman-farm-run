package com.plugandplant;

import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.ObjectComposition;
import net.runelite.api.Tile;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.client.eventbus.Subscribe;

/**
 * Keeps track of the banks in the scene, booths, chests and bankers, to tell when the player is at
 * one and the "what to bring" checklist is worth showing.
 */
@Singleton
class Banks
{
	/**
	 * How close to a bank the player has to be to count as at it.
	 */
	private static final int RANGE = 12;

	private final Client client;
	private final Map<Object, WorldPoint> banks = new HashMap<>();

	@Inject
	Banks(Client client)
	{
		this.client = client;
	}

	boolean isNear(WorldPoint point)
	{
		for (WorldPoint bank : banks.values())
		{
			if (bank.getPlane() == point.getPlane() && bank.distanceTo2D(point) <= RANGE)
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * Finds the banks already in the scene. Only needed when tracking starts while logged in, as
	 * spawn events cover everything after that.
	 */
	void scan()
	{
		banks.clear();
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		WorldView worldView = client.getTopLevelWorldView();
		for (Tile[] row : worldView.getScene().getTiles()[worldView.getPlane()])
		{
			for (Tile tile : row)
			{
				if (tile == null)
				{
					continue;
				}
				for (GameObject object : tile.getGameObjects())
				{
					if (object != null)
					{
						add(object);
					}
				}
			}
		}
		for (NPC npc : worldView.npcs())
		{
			add(npc);
		}
	}

	void clear()
	{
		banks.clear();
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned event)
	{
		add(event.getGameObject());
	}

	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned event)
	{
		banks.remove(event.getGameObject());
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned event)
	{
		add(event.getNpc());
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned event)
	{
		banks.remove(event.getNpc());
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		// Objects spawn again once the map has loaded; bankers still in view don't, so they are kept
		switch (event.getGameState())
		{
			case LOADING:
				banks.keySet().removeIf(bank -> bank instanceof GameObject);
				break;
			case LOGGED_IN:
				break;
			default:
				banks.clear();
				break;
		}
	}

	private void add(GameObject object)
	{
		ObjectComposition composition = client.getObjectDefinition(object.getId());
		if (composition != null && composition.getImpostorIds() != null)
		{
			composition = composition.getImpostor();
		}
		if (composition != null && (offersBank(composition.getActions()) || isBankName(composition.getName())))
		{
			banks.put(object, Positions.of(client, object));
		}
	}

	private void add(NPC npc)
	{
		NPCComposition composition = npc.getTransformedComposition();
		if (composition != null && offersBank(composition.getActions()))
		{
			banks.put(npc, Positions.of(client, npc));
		}
	}

	private static boolean offersBank(String[] actions)
	{
		if (actions == null)
		{
			return false;
		}
		for (String action : actions)
		{
			if ("Bank".equals(action))
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * Bank chests often only offer "Use".
	 */
	private static boolean isBankName(String name)
	{
		return "Bank chest".equals(name) || "Bank booth".equals(name);
	}
}
