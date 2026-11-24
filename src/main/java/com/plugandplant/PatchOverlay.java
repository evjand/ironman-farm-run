package com.plugandplant;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Stroke;
import java.util.List;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Value;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.NPC;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

/**
 * Draws a box in the colour of its next step over each patch that has something to do, and labels
 * it. A gardener to pay gets one too.
 */
@Singleton
class PatchOverlay extends Overlay
{
	private static final Stroke BORDER = new BasicStroke(2);
	private static final int FILL_ALPHA = 50;
	private static final int LABEL_HEIGHT = 60;
	private static final int NPC_LABEL_OFFSET = 40;

	@Value
	static class Highlight
	{
		GameObject object;
		Color color;
		/**
		 * What to do, on one object of each patch; null on the patch's other objects.
		 */
		@Nullable
		String label;
	}

	/**
	 * A patch whose object hasn't been recognised in the scene, boxed on its known tile instead so
	 * there is always something to see.
	 */
	@Value
	static class TileHighlight
	{
		WorldPoint point;
		Color color;
		String label;
	}

	@Value
	static class NpcHighlight
	{
		NPC npc;
		Color color;
		String label;
	}

	private final Client client;
	/**
	 * Worked out by the plugin whenever the plan changes, so that drawing a frame is only drawing.
	 */
	private List<Highlight> highlights = List.of();
	private List<NpcHighlight> npcHighlights = List.of();
	private List<TileHighlight> tileHighlights = List.of();

	@Inject
	PatchOverlay(Client client)
	{
		this.client = client;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	void setHighlights(List<Highlight> highlights, List<NpcHighlight> npcHighlights, List<TileHighlight> tileHighlights)
	{
		this.highlights = highlights;
		this.npcHighlights = npcHighlights;
		this.tileHighlights = tileHighlights;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		for (Highlight highlight : highlights)
		{
			GameObject object = highlight.getObject();
			Color color = highlight.getColor();
			Polygon area = Perspective.getCanvasTileAreaPoly(client, object.getLocalLocation(),
				object.sizeX(), object.sizeY(), object.getPlane(), 0);
			if (area != null)
			{
				OverlayUtil.renderPolygon(graphics, area, color, fill(color), BORDER);
			}

			if (highlight.getLabel() != null)
			{
				Point location = object.getCanvasTextLocation(graphics, highlight.getLabel(), LABEL_HEIGHT);
				if (location != null)
				{
					OverlayUtil.renderTextLocation(graphics, location, highlight.getLabel(), color);
				}
			}
		}

		for (TileHighlight highlight : tileHighlights)
		{
			LocalPoint local = Positions.toScene(client, highlight.getPoint());
			if (local == null)
			{
				continue;
			}
			Color color = highlight.getColor();
			Polygon tile = Perspective.getCanvasTileAreaPoly(client, local, 2);
			if (tile != null)
			{
				OverlayUtil.renderPolygon(graphics, tile, color, fill(color), BORDER);
			}
			Point location = Perspective.getCanvasTextLocation(client, graphics, local, highlight.getLabel(), LABEL_HEIGHT);
			if (location != null)
			{
				OverlayUtil.renderTextLocation(graphics, location, highlight.getLabel(), color);
			}
		}

		for (NpcHighlight highlight : npcHighlights)
		{
			NPC npc = highlight.getNpc();
			// A gardener that has just left the scene can't be drawn
			if (npc.getComposition() == null || npc.getTransformedComposition() == null)
			{
				continue;
			}
			Color color = highlight.getColor();
			Polygon tile = npc.getCanvasTilePoly();
			if (tile != null)
			{
				OverlayUtil.renderPolygon(graphics, tile, color, fill(color), BORDER);
			}

			Point location = npc.getCanvasTextLocation(graphics, highlight.getLabel(), npc.getLogicalHeight() + NPC_LABEL_OFFSET);
			if (location != null)
			{
				OverlayUtil.renderTextLocation(graphics, location, highlight.getLabel(), color);
			}
		}
		return null;
	}

	private static Color fill(Color color)
	{
		return new Color(color.getRed(), color.getGreen(), color.getBlue(), FILL_ALPHA);
	}
}
