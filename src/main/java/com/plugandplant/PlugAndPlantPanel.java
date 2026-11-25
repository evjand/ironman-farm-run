package com.plugandplant;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

class PlugAndPlantPanel extends PluginPanel
{
	interface Listener
	{
		void startRun();

		void stopRun();

		void reroute();

		void setLocationSettings(FarmLocation location, LocationSettings settings);

		void setRunOption(RunOption option, boolean on);

		void setPatchRole(PatchType type, PatchRole role);

		void setPatchExcluded(FarmLocation location, PatchType type, boolean excluded);

		void selectPreset(String id);

		/**
		 * Adds a copy of the preset shown, and shows it.
		 */
		void newPreset();

		void renamePreset(String name);

		void deletePreset();

		void skipStop(FarmLocation location);

		/**
		 * @param key {@link PlugAndPlantConfig#ALWAYS_SHOW} or {@link PlugAndPlantConfig#NEVER_SHOW}
		 */
		void setBankItems(String key, String text);
	}

	private static final int HELP_WIDTH = 150;
	private static final String BANK_ITEMS_HELP = "One name or item ID per line. * matches anything, like Ring of dueling*.";

	private static final Color GOOD = ColorScheme.PROGRESS_COMPLETE_COLOR;
	private static final Color WARN = ColorScheme.PROGRESS_INPROGRESS_COLOR;
	private static final Color BAD = ColorScheme.PROGRESS_ERROR_COLOR;
	private static final Color NEUTRAL = ColorScheme.LIGHT_GRAY_COLOR;
	private static final Color FAINT = ColorScheme.MEDIUM_GRAY_COLOR;

	/**
	 * The controls for one location.
	 */
	private static class LocationRow
	{
		private final JPanel block;
		private final JCheckBox enabled;
		private final JLabel status = new JLabel();
		private final JComboBox<Teleport> teleport;
		/**
		 * A switch for each kind of patch the preset covers here, when there is more than one.
		 */
		private final JPanel types = new JPanel(new GridLayout(0, 2, 4, 0));
		private final Map<PatchType, JCheckBox> typeBoxes = new EnumMap<>(PatchType.class);
		private List<PatchType> shownTypes = List.of();

		private LocationRow(FarmLocation location, JPanel block)
		{
			this.block = block;
			enabled = new JCheckBox(location.getDisplayName());
			teleport = new JComboBox<>(location.teleportChoices().toArray(new Teleport[0]));
		}

		private LocationSettings settings()
		{
			return new LocationSettings(enabled.isSelected(), (Teleport) teleport.getSelectedItem());
		}
	}

	private final JLabel warnings = new JLabel();
	private final JLabel summary = new JLabel();
	private final JLabel contract = new JLabel();
	private final Map<FarmLocation, LocationRow> rows = new EnumMap<>(FarmLocation.class);
	private final Map<RunOption, JCheckBox> runOptions = new EnumMap<>(RunOption.class);
	private final Map<PatchType, JComboBox<PatchRole>> roles = new EnumMap<>(PatchType.class);
	private final JComboBox<PanelState.PresetChoice> presetBox = new JComboBox<>();
	private final JButton deletePreset = linkButton("Delete");
	private final JPanel checklist = new JPanel(new GridLayout(0, 1, 0, 2));
	private final JButton startStop = new JButton();
	private final JButton reroute = new JButton("Show path");
	private final JPanel buttons = new JPanel(new GridLayout(1, 0, 4, 0));
	/**
	 * The run options that apply to the preset shown; rebuilt when they change, as a grid keeps room for hidden ones.
	 */
	private final JPanel options = card();
	private final JTextArea alwaysShow = new JTextArea(4, 0);
	private final JTextArea neverShow = new JTextArea(4, 0);

	/**
	 * Holds the location blocks of the preset shown, in its recommended order.
	 */
	private final JPanel locations = new JPanel();
	/**
	 * Picking and managing presets, before a run.
	 */
	private final JPanel presetBar = new JPanel();
	private final JPanel setup = new JPanel();
	/**
	 * The stops of the run in progress. Shown only during a run.
	 */
	private final JPanel run = new JPanel();
	private final JPanel stopList = new JPanel();
	private final Listener listener;
	private List<FarmLocation> shownLocations = List.of();
	private Set<RunOption> shownOptions = Set.of();
	private List<PanelState.PresetChoice> shownPresets = List.of();
	private boolean runActive;
	/**
	 * Set while {@link #update} is changing checkboxes and dropdowns, so those changes aren't reported as the player's.
	 */
	private boolean updating;

	PlugAndPlantPanel(Listener listener)
	{
		this.listener = listener;
		setLayout(new BorderLayout());
		setBorder(new EmptyBorder(8, 8, 8, 8));

		JPanel content = new JPanel();
		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

		JLabel title = new JLabel("Plug & Plant Farming");
		title.setFont(FontManager.getRunescapeBoldFont());
		title.setForeground(Color.WHITE);
		title.setBorder(new EmptyBorder(0, 0, 8, 0));
		content.add(leftAligned(title));

		presetBar.setLayout(new BoxLayout(presetBar, BoxLayout.Y_AXIS));
		presetBox.setToolTipText("The run to plan. Each keeps its own patches, locations and options.");
		presetBox.addActionListener(e ->
		{
			PanelState.PresetChoice choice = (PanelState.PresetChoice) presetBox.getSelectedItem();
			if (!updating && choice != null)
			{
				listener.selectPreset(choice.getId());
			}
		});
		presetBar.add(leftAligned(presetBox));

		JButton newPreset = linkButton("New");
		newPreset.setToolTipText("Make a new run, starting as a copy of this one");
		newPreset.addActionListener(e -> listener.newPreset());
		JButton renamePreset = linkButton("Rename");
		renamePreset.addActionListener(e ->
		{
			PanelState.PresetChoice choice = (PanelState.PresetChoice) presetBox.getSelectedItem();
			String name = (String) JOptionPane.showInputDialog(this, "Name this run:", "Rename run", JOptionPane.PLAIN_MESSAGE,
				null, null, choice == null ? "" : choice.getName());
			if (name != null)
			{
				listener.renamePreset(name);
			}
		});
		deletePreset.addActionListener(e ->
		{
			PanelState.PresetChoice choice = (PanelState.PresetChoice) presetBox.getSelectedItem();
			if (choice != null && JOptionPane.showConfirmDialog(this, "Delete " + choice.getName() + "?", "Delete run",
				JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION)
			{
				listener.deletePreset();
			}
		});
		JPanel presetActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
		presetActions.setBorder(new EmptyBorder(4, 0, 0, 0));
		presetActions.add(newPreset);
		presetActions.add(renamePreset);
		presetActions.add(deletePreset);
		presetBar.add(leftAligned(presetActions));
		content.add(leftAligned(presetBar));

		warnings.setForeground(WARN);
		warnings.setFont(FontManager.getRunescapeSmallFont());
		warnings.setBorder(new EmptyBorder(8, 0, 0, 0));
		content.add(leftAligned(warnings));

		summary.setForeground(NEUTRAL);
		summary.setBorder(new EmptyBorder(8, 0, 0, 0));
		content.add(leftAligned(summary));

		contract.setForeground(NEUTRAL);
		contract.setFont(FontManager.getRunescapeSmallFont());
		contract.setBorder(new EmptyBorder(2, 0, 0, 0));
		content.add(leftAligned(contract));

		buttons.setBorder(new EmptyBorder(8, 0, 0, 0));
		startStop.addActionListener(e ->
		{
			if (runActive)
			{
				listener.stopRun();
			}
			else
			{
				listener.startRun();
			}
		});
		reroute.setToolTipText("Ask Shortest Path for the route to the nearest remaining location again");
		reroute.addActionListener(e -> listener.reroute());
		buttons.add(startStop);
		content.add(leftAligned(buttons));

		run.setLayout(new BoxLayout(run, BoxLayout.Y_AXIS));
		run.add(leftAligned(sectionTitle("Stops")));
		stopList.setLayout(new BoxLayout(stopList, BoxLayout.Y_AXIS));
		run.add(leftAligned(stopList));
		content.add(leftAligned(run));

		setup.setLayout(new BoxLayout(setup, BoxLayout.Y_AXIS));
		content.add(leftAligned(setup));
		setup.add(leftAligned(sectionTitle("Patches")));
		JPanel typesCard = card();
		typesCard.setLayout(new GridLayout(0, 1, 0, 2));
		for (PatchType type : RunPreset.TYPES)
		{
			JComboBox<PatchRole> role = new JComboBox<>(PatchRole.values());
			role.setFont(FontManager.getRunescapeSmallFont());
			role.setToolTipText("<html>Visit for: a location is a stop when this needs attention.<br>"
				+ "Tend while there: done at the stops the run makes anyway, while your seeds last.</html>");
			role.addActionListener(e ->
			{
				if (!updating)
				{
					listener.setPatchRole(type, (PatchRole) role.getSelectedItem());
				}
			});
			roles.put(type, role);
			JPanel line = new JPanel(new BorderLayout(6, 0));
			line.setBackground(ColorScheme.DARKER_GRAY_COLOR);
			line.add(name(type.getPluralName()), BorderLayout.CENTER);
			line.add(role, BorderLayout.EAST);
			typesCard.add(line);
		}
		setup.add(leftAligned(typesCard));

		setup.add(leftAligned(sectionTitle("Run options")));
		options.setLayout(new GridLayout(0, 1, 0, 2));
		for (RunOption option : RunOption.values())
		{
			JCheckBox box = new JCheckBox(option.getLabel());
			box.setBackground(ColorScheme.DARKER_GRAY_COLOR);
			box.setToolTipText(option.getDescription());
			box.addActionListener(e ->
			{
				if (!updating)
				{
					listener.setRunOption(option, box.isSelected());
				}
			});
			runOptions.put(option, box);
		}
		setup.add(leftAligned(options));

		setup.add(leftAligned(sectionTitle("Locations")));
		locations.setLayout(new BoxLayout(locations, BoxLayout.Y_AXIS));
		setup.add(leftAligned(locations));
		for (FarmLocation location : FarmLocation.values())
		{
			JPanel block = card();
			block.setLayout(new BoxLayout(block, BoxLayout.Y_AXIS));
			LocationRow row = new LocationRow(location, block);
			Runnable changed = () ->
			{
				if (!updating)
				{
					listener.setLocationSettings(location, row.settings());
				}
			};

			row.enabled.setBackground(ColorScheme.DARKER_GRAY_COLOR);
			row.enabled.setToolTipText("Stop at " + location.getDisplayName() + " on runs");
			row.enabled.addActionListener(e -> changed.run());
			row.status.setFont(FontManager.getRunescapeSmallFont());
			block.add(leftAligned(row(row.enabled, row.status)));

			// A quiet second line: the teleport is set once and rarely changed
			row.teleport.setToolTipText("What to bring to get to " + location.getDisplayName()
				+ ". The checklist and bank filter show it, and the route uses it if you carry it.");
			row.teleport.setFont(FontManager.getRunescapeSmallFont());
			row.teleport.setForeground(NEUTRAL);
			row.teleport.setBackground(ColorScheme.DARKER_GRAY_COLOR);
			row.teleport.setBorder(new EmptyBorder(0, 0, 0, 0));
			row.teleport.addActionListener(e -> changed.run());
			block.add(leftAligned(indented(row.teleport)));

			row.types.setBackground(ColorScheme.DARKER_GRAY_COLOR);
			row.types.setBorder(new EmptyBorder(2, 24, 0, 0));
			for (PatchType type : RunPreset.TYPES)
			{
				JCheckBox box = new JCheckBox(type.getPluralName());
				box.setFont(FontManager.getRunescapeSmallFont());
				box.setBackground(ColorScheme.DARKER_GRAY_COLOR);
				box.setToolTipText("Untick to leave " + type.getPluralName().toLowerCase() + " at " + location.getDisplayName()
					+ " out of this run");
				box.addActionListener(e ->
				{
					if (!updating)
					{
						listener.setPatchExcluded(location, type, !box.isSelected());
					}
				});
				row.typeBoxes.put(type, box);
			}
			block.add(leftAligned(row.types));

			rows.put(location, row);
			leftAligned(block);
		}

		content.add(leftAligned(sectionTitle("Checklist")));
		checklist.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		checklist.setBorder(new EmptyBorder(6, 8, 6, 8));
		content.add(leftAligned(checklist));

		// Out of the way until wanted: a heading that opens the player's own bank filter lists
		JPanel bankItems = new JPanel();
		bankItems.setLayout(new BoxLayout(bankItems, BoxLayout.Y_AXIS));
		bankItems.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		bankItems.setBorder(new EmptyBorder(6, 8, 8, 8));
		bankItems.setVisible(false);
		// A width for the text to wrap at, as a label otherwise keeps to one line
		JLabel help = new JLabel("<html><body style='width:" + HELP_WIDTH + "px'>" + BANK_ITEMS_HELP + "</body></html>");
		help.setForeground(FAINT);
		bankItems.add(leftAligned(help));
		bankItems.add(leftAligned(itemListLabel("Always show")));
		bankItems.add(leftAligned(itemList(alwaysShow, PlugAndPlantConfig.ALWAYS_SHOW)));
		bankItems.add(leftAligned(itemListLabel("Never show")));
		bankItems.add(leftAligned(itemList(neverShow, PlugAndPlantConfig.NEVER_SHOW)));

		JLabel bankItemsTitle = sectionTitle("Bank filter items \u25B8");
		bankItemsTitle.setToolTipText("Add items to the bank filter, or leave them out");
		bankItemsTitle.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		bankItemsTitle.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				bankItems.setVisible(!bankItems.isVisible());
				bankItemsTitle.setText("Bank filter items " + (bankItems.isVisible() ? "\u25BE" : "\u25B8"));
				revalidate();
				repaint();
			}
		});
		content.add(leftAligned(bankItemsTitle));
		content.add(leftAligned(bankItems));

		add(content, BorderLayout.NORTH);
	}

	/**
	 * Must be called on the Swing event thread.
	 */
	void update(PanelState state)
	{
		runActive = state.isRunActive();
		int stops = state.getTargets().size();
		RunPreset preset = state.getPreset();

		warnings.setText(warningText(state));
		warnings.setVisible(!warnings.getText().isEmpty());

		if (!state.isLoggedIn())
		{
			summary.setText("Log in to see your patches.");
		}
		else if (runActive)
		{
			long done = state.getRunStops().stream().filter(stop -> stop.getStage() == StopView.Stage.DONE).count();
			summary.setText(preset.getName() + ": " + done + " of " + state.getRunStops().size() + " stops done");
		}
		else if (stops == 0)
		{
			summary.setText("No stops need attention for the " + preset.noun() + ".");
		}
		else
		{
			summary.setText(stops == 1 ? "1 stop needs attention." : stops + " stops need attention.");
		}

		contract.setText(state.getContract() == null ? "" : "Contract: " + state.getContract());
		contract.setVisible(state.getContract() != null);

		// Setting up is for before a run; during one the panel follows its stops
		presetBar.setVisible(!runActive);
		setup.setVisible(!runActive);
		run.setVisible(runActive);
		if (runActive)
		{
			showStops(state.getRunStops());
		}

		// A run follows the patches through Time Tracking, so it can't get anywhere without it
		startStop.setEnabled(runActive || (state.isLoggedIn() && stops > 0 && state.isTimeTrackingActive()));
		// Side by side during a run; GridLayout keeps room for hidden buttons, so they're taken out instead
		boolean showReroute = runActive && state.isShortestPathActive();
		if (showReroute != (reroute.getParent() == buttons))
		{
			if (showReroute)
			{
				buttons.add(reroute);
			}
			else
			{
				buttons.remove(reroute);
			}
		}
		startStop.setText(runActive ? "Stop run" : "Start " + preset.noun());

		updating = true;
		if (!state.getPresets().equals(shownPresets))
		{
			shownPresets = state.getPresets();
			presetBox.removeAllItems();
			for (PanelState.PresetChoice choice : shownPresets)
			{
				presetBox.addItem(choice);
			}
		}
		for (PanelState.PresetChoice choice : shownPresets)
		{
			if (choice.getId().equals(preset.getId()))
			{
				presetBox.setSelectedItem(choice);
			}
		}
		presetBox.setEnabled(!runActive && !shownPresets.isEmpty());
		deletePreset.setEnabled(shownPresets.size() > 1);

		for (Map.Entry<PatchType, JComboBox<PatchRole>> role : roles.entrySet())
		{
			role.getValue().setSelectedItem(preset.role(role.getKey()));
			role.getValue().setEnabled(state.isLoggedIn());
		}

		Set<RunOption> applicable = EnumSet.noneOf(RunOption.class);
		for (RunOption option : RunOption.values())
		{
			if (option.appliesTo(preset))
			{
				applicable.add(option);
			}
		}
		if (!applicable.equals(shownOptions))
		{
			shownOptions = applicable;
			options.removeAll();
			for (RunOption option : applicable)
			{
				options.add(runOptions.get(option));
			}
		}
		for (Map.Entry<RunOption, JCheckBox> option : runOptions.entrySet())
		{
			option.getValue().setSelected(state.getRunOptions().contains(option.getKey()));
			option.getValue().setEnabled(state.isLoggedIn());
		}

		// The locations with something the preset covers, in the order it visits them
		List<FarmLocation> presetLocations = preset.locations();
		if (!presetLocations.equals(shownLocations))
		{
			shownLocations = presetLocations;
			locations.removeAll();
			for (FarmLocation location : presetLocations)
			{
				locations.add(rows.get(location).block);
			}
		}
		for (FarmLocation location : presetLocations)
		{
			LocationRow row = rows.get(location);
			LocationSettings settings = state.getSettings().getOrDefault(location, LocationSettings.DEFAULT);

			row.enabled.setSelected(settings.isEnabled());
			row.enabled.setEnabled(state.isLoggedIn());
			row.teleport.setSelectedItem(settings.getTeleport());
			row.teleport.setEnabled(state.isLoggedIn());

			List<PatchType> types = preset.typesAt(location);
			if (!types.equals(row.shownTypes))
			{
				row.shownTypes = types;
				row.types.removeAll();
				for (PatchType type : types)
				{
					row.types.add(row.typeBoxes.get(type));
				}
				row.types.setVisible(types.size() > 1);
			}
			for (PatchType type : types)
			{
				JCheckBox box = row.typeBoxes.get(type);
				box.setSelected(!preset.isExcluded(location, type));
				box.setEnabled(state.isLoggedIn() && settings.isEnabled());
			}

			// The anchor patches decide whether a location is a stop; the tooltip has the rest
			PatchState shown = anchorState(location, preset, state);
			row.status.setText(statusText(shown, state.getNowMinutes()));
			row.status.setForeground(statusColor(shown.getStatus()));
			List<String> lines = new ArrayList<>();
			for (FarmPatch patch : location.getPatches())
			{
				if (!preset.includes(patch.getType()))
				{
					continue;
				}
				lines.add(patch.getName() + ": " + statusText(state.getPatches().getOrDefault(patch, PatchState.UNKNOWN), state.getNowMinutes()));
			}
			row.status.setToolTipText("<html>" + String.join("<br>", lines) + "</html>");
		}
		updating = false;

		// What is left to fetch first; what is already packed below, out of the way
		checklist.removeAll();
		List<ChecklistItem> packed = new ArrayList<>();
		for (ChecklistItem item : state.getChecklist())
		{
			if (item.getState() == ChecklistItem.State.GOOD)
			{
				packed.add(item);
				continue;
			}
			JLabel value = new JLabel(item.getValue());
			value.setForeground(stateColor(item.getState()));
			checklist.add(row(name(item.getLabel()), value));
		}
		if (checklist.getComponentCount() == 0)
		{
			JLabel ready = new JLabel("Everything is packed");
			ready.setForeground(GOOD);
			checklist.add(ready);
		}
		if (!packed.isEmpty())
		{
			JLabel heading = new JLabel("Packed");
			heading.setForeground(FAINT);
			heading.setFont(FontManager.getRunescapeSmallFont());
			heading.setBorder(new EmptyBorder(6, 0, 0, 0));
			checklist.add(heading);
			for (ChecklistItem item : packed)
			{
				JLabel label = name(item.getLabel());
				label.setForeground(FAINT);
				JLabel value = new JLabel(item.getValue());
				value.setForeground(FAINT);
				checklist.add(row(label, value));
			}
		}

		// Not while the player is typing in it
		if (!alwaysShow.hasFocus() && !alwaysShow.getText().equals(state.getAlwaysShow()))
		{
			alwaysShow.setText(state.getAlwaysShow());
		}
		if (!neverShow.hasFocus() && !neverShow.getText().equals(state.getNeverShow()))
		{
			neverShow.setText(state.getNeverShow());
		}

		revalidate();
		repaint();
	}

	private static JLabel itemListLabel(String text)
	{
		JLabel label = name(text);
		label.setBorder(new EmptyBorder(8, 0, 2, 0));
		return label;
	}

	/**
	 * A box for one of the player's item lists, saved when they leave it.
	 */
	private JScrollPane itemList(JTextArea area, String key)
	{
		area.setLineWrap(true);
		area.setWrapStyleWord(true);
		area.setBorder(new EmptyBorder(4, 4, 4, 4));
		area.addFocusListener(new FocusAdapter()
		{
			@Override
			public void focusLost(FocusEvent e)
			{
				listener.setBankItems(key, area.getText());
			}
		});
		JScrollPane scroll = new JScrollPane(area, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.setBorder(new MatteBorder(1, 1, 1, 1, ColorScheme.MEDIUM_GRAY_COLOR));
		return scroll;
	}

	/**
	 * Lists the run's stops: finished ones on a single line, the current one with each patch's task
	 * and the next one marked, and those to come on a line with how much is left to do there.
	 */
	private void showStops(List<StopView> views)
	{
		stopList.removeAll();
		for (StopView view : views)
		{
			String name = view.getLocation().getDisplayName();
			switch (view.getStage())
			{
				case DONE:
					stopList.add(leftAligned(compactLine(label(name, FAINT), label("Done", GOOD))));
					break;
				case UPCOMING:
					int left = view.getTasksLeft();
					stopList.add(leftAligned(compactLine(label(name, NEUTRAL), label(left == 1 ? "1 task" : left + " tasks", NEUTRAL))));
					break;
				default:
					stopList.add(leftAligned(currentStop(view)));
					break;
			}
		}
		stopList.revalidate();
	}

	private JPanel currentStop(StopView view)
	{
		JPanel block = new JPanel(new GridLayout(0, 1, 0, 2));
		block.setBackground(ColorScheme.DARKER_GRAY_HOVER_COLOR);
		block.setBorder(new CompoundBorder(
			new CompoundBorder(new MatteBorder(4, 0, 4, 0, ColorScheme.DARK_GRAY_COLOR), new MatteBorder(0, 3, 0, 0, ColorScheme.BRAND_ORANGE)),
			new EmptyBorder(5, 8, 5, 8)));

		JLabel title = label(view.getLocation().getDisplayName(), Color.WHITE);
		title.setFont(FontManager.getRunescapeBoldFont());
		JLabel teleport = label(view.getTeleport() == null ? "" : view.getTeleport(), NEUTRAL);
		teleport.setFont(FontManager.getRunescapeSmallFont());
		teleport.setToolTipText("The teleport you picked for this stop");
		block.add(line(title, teleport, ColorScheme.DARKER_GRAY_HOVER_COLOR));

		for (StopView.Task task : view.getTasks())
		{
			JLabel patch = label(task.getPatch(), task.isNext() ? Color.WHITE : NEUTRAL);
			JLabel step = label(task.isNext() ? "> " + task.getLabel() : task.getLabel(), task.getColor());
			if (task.isNext())
			{
				patch.setFont(FontManager.getRunescapeBoldFont());
				step.setFont(FontManager.getRunescapeBoldFont());
			}
			block.add(line(patch, step, ColorScheme.DARKER_GRAY_HOVER_COLOR));
		}

		// A quiet way out, for a stop that can't be finished
		JButton skip = new JButton("Skip stop");
		skip.setFont(FontManager.getRunescapeSmallFont());
		skip.setForeground(NEUTRAL);
		skip.setContentAreaFilled(false);
		skip.setBorderPainted(false);
		skip.setFocusPainted(false);
		skip.setBorder(new EmptyBorder(0, 0, 0, 0));
		skip.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		skip.setToolTipText("Leave " + view.getLocation().getDisplayName() + " out of the rest of this run, for when it can't be finished");
		skip.addActionListener(e -> listener.skipStop(view.getLocation()));
		skip.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseEntered(MouseEvent e)
			{
				skip.setForeground(Color.WHITE);
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				skip.setForeground(NEUTRAL);
			}
		});
		JPanel skipLine = new JPanel(new BorderLayout());
		skipLine.setBackground(ColorScheme.DARKER_GRAY_HOVER_COLOR);
		skipLine.add(skip, BorderLayout.EAST);
		block.add(skipLine);
		return block;
	}

	private static JPanel compactLine(JLabel left, JLabel right)
	{
		JPanel line = line(left, right, ColorScheme.DARKER_GRAY_COLOR);
		line.setBorder(new CompoundBorder(new MatteBorder(0, 0, 1, 0, ColorScheme.DARK_GRAY_COLOR), new EmptyBorder(4, 11, 4, 8)));
		return line;
	}

	private static JPanel line(JLabel left, JLabel right, Color background)
	{
		JPanel line = new JPanel(new BorderLayout(6, 0));
		line.setBackground(background);
		line.add(left, BorderLayout.CENTER);
		line.add(right, BorderLayout.EAST);
		return line;
	}

	private static JLabel label(String text, Color color)
	{
		JLabel label = new JLabel(text);
		label.setForeground(color);
		return label;
	}

	/**
	 * The state of the location's anchor patch that most needs the player: one with something to do,
	 * otherwise the one that is ready soonest.
	 */
	private static PatchState anchorState(FarmLocation location, RunPreset preset, PanelState state)
	{
		PatchState shown = null;
		for (FarmPatch patch : preset.anchors(location, state.getFarmingLevel()))
		{
			PatchState patchState = state.getPatches().getOrDefault(patch, PatchState.UNKNOWN);
			if (shown == null || rank(patchState) < rank(shown))
			{
				shown = patchState;
			}
		}
		return shown == null ? PatchState.UNKNOWN : shown;
	}

	private static long rank(PatchState state)
	{
		if (state.getStatus().needsVisit())
		{
			return 0;
		}
		return state.getStatus() == PatchStatus.GROWING ? state.getReadyAt() : Long.MAX_VALUE;
	}

	private static String warningText(PanelState state)
	{
		List<String> lines = new ArrayList<>();
		if (!state.isTimeTrackingActive())
		{
			lines.add("Turn on the Time Tracking plugin: runs follow your patches through it, so they can't start without it.");
		}
		if (!state.isShortestPathActive())
		{
			lines.add("Install the Shortest Path plugin to get routes between stops. Runs work without it.");
		}
		if (!state.isBankTagsActive())
		{
			lines.add("Turn on the Bank Tags plugin to filter the bank for the run.");
		}
		return lines.isEmpty() ? "" : "<html>" + String.join("<br><br>", lines) + "</html>";
	}

	private static String statusText(PatchState state, long nowMinutes)
	{
		if (state.getStatus() != PatchStatus.GROWING)
		{
			return state.getStatus().getLabel();
		}

		// Round up, so a patch never shows 0m while it is still growing
		long minutes = Math.max(1, (state.getReadyAt() + 59) / 60 - nowMinutes);
		return minutes >= 60 ? minutes / 60 + "h " + minutes % 60 + "m" : minutes + "m";
	}

	private static Color statusColor(PatchStatus status)
	{
		switch (status)
		{
			case READY:
			case CHECK_HEALTH:
			case GROWN:
				return GOOD;
			case WEEDS:
			case EMPTY:
			case STUMP:
			case DISEASED:
				return WARN;
			case DEAD:
				return BAD;
			case GROWING:
				return NEUTRAL;
			default:
				return FAINT;
		}
	}

	private static Color stateColor(ChecklistItem.State state)
	{
		switch (state)
		{
			case GOOD:
				return GOOD;
			case MISSING:
				return BAD;
			case STORED:
				return ChecklistOverlay.STORED_COLOR;
			default:
				return FAINT;
		}
	}

	private static JLabel sectionTitle(String text)
	{
		JLabel label = new JLabel(text);
		label.setFont(FontManager.getRunescapeBoldFont());
		label.setForeground(Color.WHITE);
		label.setBorder(new EmptyBorder(14, 0, 6, 0));
		return label;
	}

	/**
	 * A dark block with room around its contents, set a little apart from the next one.
	 */
	private static JPanel card()
	{
		JPanel panel = new JPanel();
		panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		panel.setBorder(new CompoundBorder(new MatteBorder(0, 0, 4, 0, ColorScheme.DARK_GRAY_COLOR), new EmptyBorder(5, 6, 5, 8)));
		return panel;
	}

	private static JLabel name(String text)
	{
		JLabel label = new JLabel(text);
		label.setForeground(NEUTRAL);
		return label;
	}

	private static JPanel row(Component left, JLabel right)
	{
		JPanel row = new JPanel(new BorderLayout(6, 0));
		row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		row.add(left, BorderLayout.CENTER);
		row.add(right, BorderLayout.EAST);
		return row;
	}

	private static JPanel indented(Component component)
	{
		JPanel panel = new JPanel(new BorderLayout());
		panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		panel.setBorder(new EmptyBorder(0, 24, 0, 0));
		panel.add(component, BorderLayout.CENTER);
		return panel;
	}

	/**
	 * A small text button, for actions that shouldn't draw the eye.
	 */
	private static JButton linkButton(String text)
	{
		JButton button = new JButton(text);
		button.setFont(FontManager.getRunescapeSmallFont());
		button.setForeground(NEUTRAL);
		button.setContentAreaFilled(false);
		button.setBorderPainted(false);
		button.setFocusPainted(false);
		button.setBorder(new EmptyBorder(0, 0, 0, 0));
		button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		button.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseEntered(MouseEvent e)
			{
				if (button.isEnabled())
				{
					button.setForeground(Color.WHITE);
				}
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				button.setForeground(NEUTRAL);
			}
		});
		return button;
	}

	/**
	 * BoxLayout centres components with the default alignment; pin them left so the column lines up.
	 */
	private static <T extends JComponent> T leftAligned(T component)
	{
		component.setAlignmentX(Component.LEFT_ALIGNMENT);
		return component;
	}
}
