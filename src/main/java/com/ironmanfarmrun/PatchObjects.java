package com.ironmanfarmrun;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.ObjectComposition;
import net.runelite.api.Tile;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.eventbus.Subscribe;

/**
 * Keeps track of the patch objects in the scene, so they can be highlighted without searching for
 * them every frame.
 *
 * A patch is recognised by where it is and which varbit its object changes with, the same varbit
 * Time Tracking files it under, rather than by a list of object ids.
 */
@Singleton
class PatchObjects
{
	private final Client client;
	private final Map<GameObject, FarmPatch> objects = new HashMap<>();

	@Inject
	PatchObjects(Client client)
	{
		this.client = client;
	}

	/**
	 * The patch objects in the scene. A patch can be made of more than one object.
	 */
	Map<GameObject, FarmPatch> get()
	{
		return Collections.unmodifiableMap(objects);
	}

	/**
	 * Finds the patches already in the scene. Only needed when tracking starts while logged in, as
	 * spawn events cover everything after that.
	 */
	void scan()
	{
		objects.clear();
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		WorldView worldView = client.getTopLevelWorldView();
		Tile[][] plane = worldView.getScene().getTiles()[worldView.getPlane()];
		for (Tile[] row : plane)
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
	}

	void clear()
	{
		objects.clear();
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned event)
	{
		add(event.getGameObject());
	}

	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned event)
	{
		objects.remove(event.getGameObject());
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		// The scene is rebuilt on loading, and its objects spawn again
		if (event.getGameState() != GameState.LOGGED_IN)
		{
			objects.clear();
		}
	}

	private void add(GameObject object)
	{
		FarmPatch patch = patchOf(object);
		if (patch != null)
		{
			objects.put(object, patch);
		}
	}

	@Nullable
	private FarmPatch patchOf(GameObject object)
	{
		WorldPoint point = object.getWorldLocation();
		for (FarmLocation location : FarmLocation.values())
		{
			if (!location.isNear(point, 0))
			{
				continue;
			}

			// Only now look the object up; nearly every object in the game is ruled out by the distance check
			ObjectComposition composition = client.getObjectDefinition(object.getId());
			if (composition == null || composition.getImpostorIds() == null)
			{
				return null;
			}

			// Patches at a location in different regions can share a varbit; the object is the nearest one's
			FarmPatch nearest = null;
			for (FarmPatch patch : location.getPatches())
			{
				if (patch.getVarbit() == composition.getVarbitId()
					&& (nearest == null || point.distanceTo2D(patch.getPoint()) < point.distanceTo2D(nearest.getPoint())))
				{
					nearest = patch;
				}
			}
			return nearest;
		}
		return null;
	}
}
