package com.plugandplant;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.WidgetItemOverlay;

/**
 * Highlights the inventory items the patches the player is at need next, in the colour of the step:
 * seeds when a patch needs planting, compost when it needs compost, and so on. Run items that could
 * be worn in a free slot are highlighted too, to make room.
 */
@Singleton
class ItemHighlightOverlay extends WidgetItemOverlay
{
	private static final int FILL_ALPHA = 40;
	/**
	 * For run items that could be worn in a free slot to make room in the inventory.
	 */
	static final Color WEAR_COLOR = Color.WHITE;

	private final ItemManager itemManager;
	/**
	 * Canonical item id to highlight colour. Worked out by the plugin whenever the plan changes.
	 */
	private Map<Integer, Color> items = Map.of();
	/**
	 * Canonical ids among {@link #items} to highlight only when not noted: produce to take to the leprechaun.
	 */
	private Set<Integer> unnotedOnly = Set.of();

	@Inject
	ItemHighlightOverlay(ItemManager itemManager)
	{
		this.itemManager = itemManager;
		showOnInventory();
	}

	void setItems(Map<Integer, Color> items, Set<Integer> unnotedOnly)
	{
		this.items = items;
		this.unnotedOnly = unnotedOnly;
	}

	@Override
	public void renderItemOverlay(Graphics2D graphics, int itemId, WidgetItem widgetItem)
	{
		// Payments can be noted, but produce to note is only highlighted until it is
		int canonical = itemManager.canonicalize(itemId);
		Color color = items.get(canonical);
		if (color == null || (canonical != itemId && unnotedOnly.contains(canonical)))
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
