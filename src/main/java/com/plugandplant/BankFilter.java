package com.plugandplant;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.FontID;
import net.runelite.api.ScriptEvent;
import net.runelite.api.ScriptID;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.events.ScriptPreFired;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.SoundEffectID;
import net.runelite.api.gameval.SpriteID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.JavaScriptCallback;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetType;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.plugins.banktags.BankTagsPlugin;
import net.runelite.client.plugins.banktags.BankTagsService;
import net.runelite.client.plugins.banktags.TagManager;
import net.runelite.client.plugins.banktags.tabs.LayoutManager;

/**
 * Filters the bank down to farm run items by way of a Bank Tags tag, places them in sections with a
 * heading over each, and adds a button to the bank to turn it on and off. All methods run on the
 * client thread.
 *
 * The items are placed by moving their widgets after the bank is built, rather than through a Bank
 * Tags layout, because layout plugins such as Bank Tag Layouts take over the layout of every tag.
 */
@Singleton
class BankFilter
{
	/**
	 * Also searchable in the bank as "tag:plug-and-plant". Bank Tags merges a player's own tag of the
	 * same name into the filter, so the name is specific to this plugin.
	 */
	static final String TAG = "plug-and-plant";

	private static final String BANK_TAGS_GROUP = "banktags";
	/**
	 * The Bank Tags setting that remembers the open tag, to reopen it the next time the bank opens.
	 */
	private static final String REMEMBERED_TAB_KEY = "tab";
	private static final int OPEN_OPTIONS = BankTagsService.OPTION_HIDE_TAG_NAME | BankTagsService.OPTION_NO_LAYOUT;
	/**
	 * Layout plugins run at -1 to come after Bank Tags; this comes after them.
	 */
	private static final float AFTER_LAYOUT_PLUGINS = -2f;

	private static final int ROW_HEIGHT = BankTagsPlugin.BANK_ITEM_HEIGHT + BankTagsPlugin.BANK_ITEM_Y_PADDING;
	private static final int COLUMN_WIDTH = BankTagsPlugin.BANK_ITEM_WIDTH + BankTagsPlugin.BANK_ITEM_X_PADDING;
	private static final int ROW_WIDTH = BankTagsPlugin.BANK_ITEMS_PER_ROW * COLUMN_WIDTH;
	private static final int HEADING_HEIGHT = 15;
	private static final int HEADING_COLOR = 0xE4D8A2;

	// The button sits left of where Quest Helper puts its own, so the two don't overlap
	private static final int BUTTON_SIZE = 25;
	private static final int BUTTON_X = 378;
	private static final int BUTTON_Y = 5;
	private static final String BUTTON_ACTION = "Toggle farm run items";

	private final Client client;
	private final ClientThread clientThread;
	private final ConfigManager configManager;
	private final TagManager tagManager;
	private final BankTagsService bankTagsService;
	private final LayoutManager layoutManager;
	private final ItemManager itemManager;
	private final PluginManager pluginManager;

	private BankFilterSpec spec = BankFilterSpec.EMPTY;
	/**
	 * Canonical item id to bank slot, worked out once per bank visit so items stay put as they are
	 * withdrawn; null until the filter is first shown on this visit.
	 */
	@Nullable
	private Map<Integer, Integer> slots;
	/**
	 * Where the headings go.
	 */
	private Map<BankSection, Integer> headingRows = Map.of();
	/**
	 * Heading widgets added to the bank's item container, kept to be reused each time the bank is rebuilt.
	 */
	private final List<Widget> headingWidgets = new ArrayList<>();
	@Nullable
	private Widget buttonBackground;
	@Nullable
	private Widget buttonIcon;
	/**
	 * Open the filter once the bank has been built.
	 */
	private boolean openRequested;

	@Inject
	BankFilter(Client client, ClientThread clientThread, ConfigManager configManager, TagManager tagManager,
		BankTagsService bankTagsService, LayoutManager layoutManager, ItemManager itemManager, PluginManager pluginManager)
	{
		this.client = client;
		this.clientThread = clientThread;
		this.configManager = configManager;
		this.tagManager = tagManager;
		this.bankTagsService = bankTagsService;
		this.layoutManager = layoutManager;
		this.itemManager = itemManager;
		this.pluginManager = pluginManager;
	}

	void register()
	{
		tagManager.registerTag(TAG, this::shows);
		// An earlier version saved a Bank Tags layout for the tag, which would now get in the way
		layoutManager.removeLayout(TAG);
		if (client.getWidget(InterfaceID.Bankmain.UNIVERSE) != null)
		{
			createButton();
		}
	}

	void unregister()
	{
		close();
		tagManager.unregisterTag(TAG);
		headingRows = Map.of();
		drawHeadings();
		for (Widget widget : new Widget[]{buttonBackground, buttonIcon})
		{
			if (widget != null)
			{
				widget.setHidden(true);
			}
		}
		buttonBackground = buttonIcon = null;
	}

	/**
	 * Takes effect the next time the filter is opened.
	 */
	void setSpec(BankFilterSpec spec)
	{
		this.spec = spec;
	}

	boolean isAvailable()
	{
		return bankTagsPlugin() != null;
	}

	boolean isOpen()
	{
		return isAvailable() && TAG.equals(bankTagsService.getActiveTag());
	}

	boolean isBankOpen()
	{
		return client.getWidget(InterfaceID.Bankmain.ITEMS) != null;
	}

	/**
	 * Opens the filter once the bank that is opening has been built.
	 */
	void openWhenBuilt()
	{
		openRequested = true;
	}

	/**
	 * Shows the filter, arranged afresh.
	 */
	void open()
	{
		openRequested = false;
		if (!isBankOpen() || !isAvailable())
		{
			return;
		}

		slots = null;
		// Leave a vanilla tab or the potion store, as Bank Tags' own tabs do
		client.setVarbit(VarbitID.BANK_CURRENTTAB, 0);
		bankTagsService.openBankTag(TAG, OPEN_OPTIONS);
	}

	/**
	 * Closes the filter, unless the player has since moved to another tag.
	 */
	void close()
	{
		openRequested = false;
		if (isOpen())
		{
			bankTagsService.closeBankTag();
		}
		// With the bank shut there is no open tag to close, but Bank Tags may still mean to reopen this one
		if (TAG.equals(configManager.getConfiguration(BANK_TAGS_GROUP, REMEMBERED_TAB_KEY)))
		{
			configManager.setConfiguration(BANK_TAGS_GROUP, REMEMBERED_TAB_KEY, "");
		}
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded event)
	{
		if (event.getGroupId() == InterfaceID.BANKMAIN)
		{
			slots = null;
			createButton();
		}
	}

	@Subscribe(priority = AFTER_LAYOUT_PLUGINS)
	public void onScriptPreFired(ScriptPreFired event)
	{
		if (event.getScriptId() != ScriptID.BANKMAIN_FINISHBUILDING || !isOpen())
		{
			return;
		}

		// The bank has decided which items are shown, but not yet how far it scrolls
		if (slots == null)
		{
			arrange();
		}

		int lastSlot = slots.values().stream().max(Integer::compare).orElse(0);
		// The script's arguments are still on the stack; the ninth from the end is the scroll height
		client.getIntStack()[client.getIntStackSize() - 9] = (lastSlot / BankTagsPlugin.BANK_ITEMS_PER_ROW + 1) * ROW_HEIGHT;
	}

	@Subscribe(priority = AFTER_LAYOUT_PLUGINS)
	public void onScriptPostFired(ScriptPostFired event)
	{
		// The bank is fully built, and any layout plugin has placed the items its way
		if (event.getScriptId() != ScriptID.BANKMAIN_BUILD)
		{
			return;
		}

		if (openRequested)
		{
			// Not from inside a script, which can't start another
			openRequested = false;
			clientThread.invokeLater(this::open);
		}

		if (isOpen())
		{
			if (slots == null)
			{
				arrange();
			}
			placeItems();
		}
		drawHeadings();
		updateButton();
	}

	/**
	 * Works out where each item the filter shows goes.
	 */
	private void arrange()
	{
		Set<Integer> shown = new LinkedHashSet<>();
		for (Widget widget : shownItems())
		{
			shown.add(itemManager.canonicalize(widget.getItemId()));
		}

		BankFilterSpec.Arrangement arrangement = spec.arrange(shown);
		Map<Integer, Integer> arranged = new HashMap<>();
		int[] layout = arrangement.getSlots();
		for (int slot = 0; slot < layout.length; slot++)
		{
			if (layout[slot] != -1)
			{
				arranged.put(layout[slot], slot);
			}
		}
		slots = arranged;
		headingRows = arrangement.getHeadingRows();
	}

	private void placeItems()
	{
		int nextFree = slots.values().stream().max(Integer::compare).orElse(-1) + 1;
		for (Widget widget : shownItems())
		{
			int itemId = itemManager.canonicalize(widget.getItemId());
			Integer slot = slots.get(itemId);
			if (slot == null)
			{
				// Deposited since the filter was arranged: keep it at the end, where it stays
				slot = nextFree++;
				slots.put(itemId, slot);
			}

			widget.setOriginalX((slot % BankTagsPlugin.BANK_ITEMS_PER_ROW) * COLUMN_WIDTH + BankTagsPlugin.BANK_ITEM_START_X);
			widget.setOriginalY((slot / BankTagsPlugin.BANK_ITEMS_PER_ROW) * ROW_HEIGHT);
			widget.revalidate();
		}
	}

	/**
	 * The item widgets the bank shows right now.
	 */
	private List<Widget> shownItems()
	{
		List<Widget> shown = new ArrayList<>();
		Widget container = client.getWidget(InterfaceID.Bankmain.ITEMS);
		if (container == null || container.getDynamicChildren() == null)
		{
			return shown;
		}

		for (Widget widget : container.getDynamicChildren())
		{
			// The game fills empty slots with a blank item
			if (!widget.isHidden() && widget.getItemId() >= 0 && widget.getItemId() != ItemID.BLANKOBJECT)
			{
				shown.add(widget);
			}
		}
		return shown;
	}

	private void toggle(ScriptEvent event)
	{
		if (event.getOp() != 2)
		{
			return;
		}

		if (isOpen())
		{
			close();
		}
		else
		{
			open();
		}
		client.playSoundEffect(SoundEffectID.INTERFACE_SELECT1);
	}

	private boolean shows(int itemId)
	{
		return spec.sectionOf(itemManager.canonicalize(itemId)) != null;
	}

	private void createButton()
	{
		Widget parent = client.getWidget(InterfaceID.Bankmain.UNIVERSE);
		if (parent == null)
		{
			return;
		}

		buttonBackground = parent.createChild(-1, WidgetType.GRAPHIC);
		buttonBackground.setOriginalX(BUTTON_X);
		buttonBackground.setOriginalY(BUTTON_Y);
		buttonBackground.setOriginalWidth(BUTTON_SIZE);
		buttonBackground.setOriginalHeight(BUTTON_SIZE);
		buttonBackground.setSpriteId(SpriteID.Miscgraphics3.UNKNOWN_BUTTON_SQUARE_SMALL);
		buttonBackground.setAction(1, BUTTON_ACTION);
		buttonBackground.setName("");
		buttonBackground.setHasListener(true);
		buttonBackground.setOnOpListener((JavaScriptCallback) this::toggle);
		buttonBackground.revalidate();

		buttonIcon = parent.createChild(-1, WidgetType.GRAPHIC);
		buttonIcon.setOriginalX(BUTTON_X + 3);
		buttonIcon.setOriginalY(BUTTON_Y + 3);
		buttonIcon.setOriginalWidth(BUTTON_SIZE - 6);
		buttonIcon.setOriginalHeight(BUTTON_SIZE - 6);
		buttonIcon.setSpriteId(SpriteID.Staticons2.FARMING);
		buttonIcon.revalidate();

		updateButton();
	}

	private void updateButton()
	{
		if (buttonBackground == null || buttonIcon == null)
		{
			return;
		}

		boolean available = isAvailable();
		buttonBackground.setHidden(!available);
		buttonIcon.setHidden(!available);
		buttonBackground.setSpriteId(isOpen()
			? SpriteID.Miscgraphics3.UNKNOWN_BUTTON_SQUARE_SMALL_SELECTED
			: SpriteID.Miscgraphics3.UNKNOWN_BUTTON_SQUARE_SMALL);
	}

	private void drawHeadings()
	{
		Widget container = client.getWidget(InterfaceID.Bankmain.ITEMS);
		if (container == null)
		{
			headingWidgets.clear();
			return;
		}

		// Widgets from before the bank was last closed are gone from the container
		headingWidgets.removeIf(widget -> container.getChild(widget.getIndex()) != widget);

		int used = 0;
		if (isOpen() && slots != null)
		{
			for (Map.Entry<BankSection, Integer> heading : headingRows.entrySet())
			{
				if (used == headingWidgets.size())
				{
					Widget created = container.createChild(-1, WidgetType.TEXT);
					created.setFontId(FontID.PLAIN_11);
					created.setTextColor(HEADING_COLOR);
					created.setTextShadowed(true);
					created.setOriginalX(BankTagsPlugin.BANK_ITEM_START_X);
					created.setOriginalWidth(ROW_WIDTH);
					created.setOriginalHeight(HEADING_HEIGHT);
					headingWidgets.add(created);
				}

				Widget widget = headingWidgets.get(used++);
				widget.setText(heading.getKey().getTitle());
				// At the foot of the empty row, just above the section's items
				widget.setOriginalY(heading.getValue() * ROW_HEIGHT + ROW_HEIGHT - HEADING_HEIGHT - 2);
				widget.setHidden(false);
				widget.revalidate();
			}
		}

		for (int i = used; i < headingWidgets.size(); i++)
		{
			headingWidgets.get(i).setHidden(true);
		}
	}

	@Nullable
	private BankTagsPlugin bankTagsPlugin()
	{
		for (Plugin plugin : pluginManager.getPlugins())
		{
			if (plugin instanceof BankTagsPlugin)
			{
				return pluginManager.isPluginActive(plugin) ? (BankTagsPlugin) plugin : null;
			}
		}
		return null;
	}
}
