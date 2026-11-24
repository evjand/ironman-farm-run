package com.plugandplant;

import java.util.List;
import javax.annotation.Nullable;
import lombok.Value;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.TileObject;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;

/**
 * Where things in the scene are on the world map, as the patches know it. Instanced scenes are turned
 * back into the world map's coordinates, and so are places the game keeps more than one copy of.
 */
final class Positions
{
	/**
	 * A copy of a place elsewhere on the world map, which Time Tracking treats as the same place.
	 */
	@Value
	static class Copy
	{
		int regionId;
		int dx;
		int dy;
	}

	/**
	 * Lletya, which the game shows from a second copy of its map in region 11103 (Time Tracking counts
	 * that region as Lletya too). Its patches are where they are in the original, shifted.
	 */
	static final List<Copy> COPIES = List.of(new Copy(11103, 448, 2944));

	private Positions()
	{
	}

	static WorldPoint of(Client client, Actor actor)
	{
		return original(WorldPoint.fromLocalInstance(client, actor.getLocalLocation(), actor.getWorldLocation().getPlane()));
	}

	static WorldPoint of(Client client, TileObject object)
	{
		return original(WorldPoint.fromLocalInstance(client, object.getLocalLocation(), object.getPlane()));
	}

	/**
	 * A point in a copy of a place, moved to where it is in the original.
	 */
	static WorldPoint original(WorldPoint point)
	{
		for (Copy copy : COPIES)
		{
			if (point.getRegionID() == copy.getRegionId())
			{
				return point.dx(-copy.getDx()).dy(-copy.getDy());
			}
		}
		return point;
	}

	/**
	 * Where a world map point is in the scene, if it is loaded, here or in a copy of its place.
	 */
	@Nullable
	static LocalPoint toScene(Client client, WorldPoint point)
	{
		WorldView worldView = client.getTopLevelWorldView();
		LocalPoint local = toScene(worldView, point);
		for (Copy copy : COPIES)
		{
			if (local != null)
			{
				break;
			}
			WorldPoint copied = point.dx(copy.getDx()).dy(copy.getDy());
			if (copied.getRegionID() == copy.getRegionId())
			{
				local = toScene(worldView, copied);
			}
		}
		return local;
	}

	@Nullable
	private static LocalPoint toScene(WorldView worldView, WorldPoint point)
	{
		for (WorldPoint scenePoint : WorldPoint.toLocalInstance(worldView, point))
		{
			LocalPoint local = LocalPoint.fromWorld(worldView, scenePoint);
			if (local != null)
			{
				return local;
			}
		}
		return null;
	}
}
