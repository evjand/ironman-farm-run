package com.ironmanfarmrun;

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
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

/**
 * Draws a box in the colour of its next step over each patch that has something to do, and labels it.
 */
@Singleton
class PatchOverlay extends Overlay
{
	private static final Stroke BORDER = new BasicStroke(2);
	private static final int FILL_ALPHA = 50;
	private static final int LABEL_HEIGHT = 60;

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

	private final Client client;
	/**
	 * Worked out by the plugin whenever the plan changes, so that drawing a frame is only drawing.
	 */
	private List<Highlight> highlights = List.of();

	@Inject
	PatchOverlay(Client client)
	{
		this.client = client;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	void setHighlights(List<Highlight> highlights)
	{
		this.highlights = highlights;
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
				OverlayUtil.renderPolygon(graphics, area, color,
					new Color(color.getRed(), color.getGreen(), color.getBlue(), FILL_ALPHA), BORDER);
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
		return null;
	}
}
