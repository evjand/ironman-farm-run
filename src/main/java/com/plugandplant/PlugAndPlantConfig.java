package com.plugandplant;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup(PlugAndPlantConfig.GROUP)
public interface PlugAndPlantConfig extends Config
{
	String GROUP = "plugandplant";

	enum StopOrder
	{
		NEAREST("Nearest next"),
		RECOMMENDED("Recommended order");

		private final String label;

		StopOrder(String label)
		{
			this.label = label;
		}

		@Override
		public String toString()
		{
			return label;
		}
	}

	enum ChecklistOverlayMode
	{
		NEVER("Never"),
		DURING_RUN("During a run"),
		ALWAYS("Always");

		private final String label;

		ChecklistOverlayMode(String label)
		{
			this.label = label;
		}

		@Override
		public String toString()
		{
			return label;
		}
	}

	// What a run includes; hidden here as it is set in the side panel, right before a run
	// Settings from before run presets, read once to make the first presets
	String COMPOST_STEP = "compostStep";
	String ALLOTMENTS = "allotments";
	String FLOWERS = "flowers";
	String GRACEFUL = "graceful";
	String VOLCANIC_ASH = "volcanicAsh";
	String PROTECT_TREES = "protectTrees";
	/**
	 * The kind of run the side panel shows.
	 */
	String RUN_TYPE = "runType";
	String ALWAYS_SHOW = "alwaysShow";
	String NEVER_SHOW = "neverShow";

	@ConfigSection(
		name = "Overlays",
		description = "What is drawn in the game",
		position = 1
	)
	String overlaySection = "overlays";

	@ConfigSection(
		name = "Bank",
		description = "The bank filter",
		position = 2
	)
	String bankSection = "bank";

	@ConfigSection(
		name = "Routing",
		description = "How Shortest Path is asked for routes",
		position = 3
	)
	String routingSection = "routing";

	@ConfigItem(
		keyName = "highlightPatches",
		name = "Highlight patches",
		description = "During a run, draw a box over each patch that needs something, coloured and labelled by the next step",
		position = 1,
		section = overlaySection
	)
	default boolean highlightPatches()
	{
		return true;
	}

	@ConfigItem(
		keyName = "highlightItems",
		name = "Highlight inventory items",
		description = "During a run, highlight the inventory items the patches you are at need next, such as seeds to plant or compost",
		position = 2,
		section = overlaySection
	)
	default boolean highlightItems()
	{
		return true;
	}

	@ConfigItem(
		keyName = "checklistOverlay",
		name = "Checklist box",
		description = "When to show the in-game box with the checklist and the steps at your current location",
		position = 3,
		section = overlaySection
	)
	default ChecklistOverlayMode checklistOverlay()
	{
		return ChecklistOverlayMode.DURING_RUN;
	}

	@ConfigItem(
		keyName = "checklistMissingOnly",
		name = "Only list what is not carried",
		description = "Leave items you already carry out of the in-game checklist box, so it shows only what is left to withdraw",
		position = 4,
		section = overlaySection
	)
	default boolean checklistMissingOnly()
	{
		return true;
	}

	@ConfigItem(
		keyName = "filterBank",
		name = "Filter bank during run",
		description = "Show only farm run items, in sections, when you open the bank during a run. Needs the Bank Tags plugin.",
		position = 1,
		section = bankSection
	)
	default boolean filterBank()
	{
		return true;
	}

	@ConfigItem(
		keyName = ALWAYS_SHOW,
		hidden = true,
		name = "Always show",
		description = "Items to always show in the bank filter, by name or item ID, separated by commas or new lines."
			+ " Names ignore case, and * matches any text, so \"Ring of dueling*\" covers every charge.",
		position = 3,
		section = bankSection
	)
	default String alwaysShow()
	{
		return "";
	}

	@ConfigItem(
		keyName = NEVER_SHOW,
		hidden = true,
		name = "Never show",
		description = "Items to leave out of the bank filter even when a run needs them, written the same way as"
			+ " Always show.",
		position = 4,
		section = bankSection
	)
	default String neverShow()
	{
		return "";
	}

	@ConfigItem(
		keyName = "bankTeleports",
		name = "Teleports in bank filter",
		description = "Include the teleports you picked in the panel for the locations left to visit in the bank filter",
		position = 2,
		section = bankSection
	)
	default boolean bankTeleports()
	{
		return true;
	}

	@ConfigItem(
		keyName = "stopOrder",
		name = "Stop order",
		description = "Nearest next: route to whichever stop left is quickest to reach from where you are. "
			+ "Recommended order: visit the stops in the order of the OSRS Wiki's farming run guides.",
		position = 0,
		section = routingSection
	)
	default StopOrder stopOrder()
	{
		return StopOrder.NEAREST;
	}

	@ConfigItem(
		keyName = "bankDetourForSeeds",
		name = "Bank detour for seeds",
		description = "Have Shortest Path route through a bank when you don't carry a herb seed for the next stop. Ignored while you carry a seed box.",
		position = 1,
		section = routingSection
	)
	default boolean bankDetourForSeeds()
	{
		return true;
	}

	@ConfigItem(
		keyName = "carriedTeleportsOnly",
		name = "Route with carried teleports only",
		description = "During a run, have Shortest Path only use teleport items you are carrying, so the route follows the teleports you chose to bring. Turn off to use your own Shortest Path setting.",
		position = 2,
		section = routingSection
	)
	default boolean carriedTeleportsOnly()
	{
		return true;
	}
}
