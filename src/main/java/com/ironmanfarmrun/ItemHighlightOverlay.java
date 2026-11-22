package com.ironmanfarmrun;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.WidgetItemOverlay;

/**
 * Highlights the inventory items the patches the player is at need next, in the colour of the step:
 * seeds when a patch needs planting, compost when it needs compost, and so on.
 */
@Singleton
class ItemHighlightOverlay extends WidgetItemOverlay
{
	private static final int FILL_ALPHA = 40;

	private final ItemManager itemManager;
	/**
	 * Item id to highlight colour. Worked out by the plugin whenever the plan changes.
	 */
	private Map<Integer, Color> items = Map.of();

	@Inject
	ItemHighlightOverlay(ItemManager itemManager)
	{
		this.itemManager = itemManager;
		showOnInventory();
	}

	void setItems(Map<Integer, Color> items)
	{
		this.items = items;
	}

	@Override
	public void renderItemOverlay(Graphics2D graphics, int itemId, WidgetItem widgetItem)
	{
		Color color = items.get(itemId);
		if (color == null)
		{
			return;
		}

		Rectangle bounds = widgetItem.getCanvasBounds();
		graphics.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), FILL_ALPHA));
		graphics.fill(bounds);

		BufferedImage outline = itemManager.getItemOutline(itemId, widgetItem.getQuantity(), color);
		graphics.drawImage(outline, (int) bounds.getX(), (int) bounds.getY(), null);
	}
}
