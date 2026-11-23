package com.plugandplant;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.List;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Value;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

/**
 * An in-game box with the steps at the location the player is at and the checklist of what to bring.
 */
@Singleton
class ChecklistOverlay extends OverlayPanel
{
	private static final int WIDTH = 190;
	/**
	 * For items kept with the tool leprechaun: not on the player, but not missing either.
	 */
	static final Color STORED_COLOR = new Color(0x6C, 0xB4, 0xEE);
	/**
	 * For suggestions, such as wearing an item to make room.
	 */
	static final Color HINT_COLOR = new Color(0xE6, 0xC8, 0x6E);

	@Value
	static class Line
	{
		String left;
		String right;
		Color rightColor;
	}

	@Value
	static class Content
	{
		String title;
		/**
		 * The patches at the location the player is at, with the next step at each; empty elsewhere.
		 */
		List<Line> steps;
		List<Line> checklist;
	}

	/**
	 * Null while the overlay is hidden.
	 */
	@Nullable
	private Content content;

	@Inject
	ChecklistOverlay()
	{
		setPosition(OverlayPosition.TOP_LEFT);
		panelComponent.setPreferredSize(new Dimension(WIDTH, 0));
	}

	void setContent(@Nullable Content content)
	{
		this.content = content;
	}

	static Color color(ChecklistItem.State state)
	{
		switch (state)
		{
			case GOOD:
				return ColorScheme.PROGRESS_COMPLETE_COLOR;
			case MISSING:
				return ColorScheme.PROGRESS_ERROR_COLOR;
			case STORED:
				return STORED_COLOR;
			case HINT:
				return HINT_COLOR;
			default:
				return ColorScheme.LIGHT_GRAY_COLOR;
		}
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		Content shown = content;
		if (shown == null)
		{
			return null;
		}

		panelComponent.getChildren().add(TitleComponent.builder().text(shown.getTitle()).build());
		for (Line line : shown.getSteps())
		{
			add(line);
		}
		if (!shown.getSteps().isEmpty() && !shown.getChecklist().isEmpty())
		{
			panelComponent.getChildren().add(LineComponent.builder().build());
		}
		for (Line line : shown.getChecklist())
		{
			add(line);
		}
		return super.render(graphics);
	}

	private void add(Line line)
	{
		panelComponent.getChildren().add(LineComponent.builder()
			.left(line.getLeft())
			.right(line.getRight())
			.rightColor(line.getRightColor())
			.build());
	}
}
