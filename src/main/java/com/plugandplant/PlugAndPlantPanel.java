package com.plugandplant;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
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

		void setRunType(RunType runType);

		void skipStop(FarmLocation location);
	}

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
	private final Map<RunType, JToggleButton> runTypes = new EnumMap<>(RunType.class);
	private final JPanel checklist = new JPanel(new GridLayout(0, 1, 0, 2));
	private final JButton startStop = new JButton();
	private final JButton reroute = new JButton("Show path again");

	/**
	 * Holds the location blocks of the run type shown, in its recommended order.
	 */
	private final JPanel locations = new JPanel();
	/**
	 * What is set up before a run: the kind of run, its options and the locations. Hidden during a run.
	 */
	private final JPanel types = new JPanel(new GridLayout(1, 0, 4, 0));
	private final JPanel setup = new JPanel();
	/**
	 * The stops of the run in progress. Shown only during a run.
	 */
	private final JPanel run = new JPanel();
	private final JPanel stopList = new JPanel();
	private final Listener listener;
	@Nullable
	private RunType shownRunType;
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
		content.add(leftAligned(title));

		types.setBorder(new EmptyBorder(6, 0, 0, 0));
		ButtonGroup typeGroup = new ButtonGroup();
		for (RunType type : RunType.values())
		{
			JToggleButton button = new JToggleButton(type.getLabel());
			button.setToolTipText("Plan a " + type.getNoun());
			button.addActionListener(e ->
			{
				if (!updating)
				{
					listener.setRunType(type);
				}
			});
			typeGroup.add(button);
			runTypes.put(type, button);
			types.add(button);
		}
		content.add(leftAligned(types));

		warnings.setForeground(WARN);
		warnings.setBorder(new EmptyBorder(6, 0, 0, 0));
		content.add(leftAligned(warnings));

		summary.setForeground(NEUTRAL);
		summary.setBorder(new EmptyBorder(6, 0, 6, 0));
		content.add(leftAligned(summary));

		contract.setForeground(NEUTRAL);
		contract.setBorder(new EmptyBorder(0, 0, 6, 0));
		content.add(leftAligned(contract));

		JPanel buttons = new JPanel(new GridLayout(0, 1, 0, 4));
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
		reroute.setToolTipText("Ask Shortest Path for the route to the nearest remaining location");
		reroute.addActionListener(e -> listener.reroute());
		buttons.add(startStop);
		buttons.add(reroute);
		content.add(leftAligned(buttons));

		run.setLayout(new BoxLayout(run, BoxLayout.Y_AXIS));
		run.add(leftAligned(sectionTitle("Stops")));
		stopList.setLayout(new BoxLayout(stopList, BoxLayout.Y_AXIS));
		run.add(leftAligned(stopList));
		content.add(leftAligned(run));

		setup.setLayout(new BoxLayout(setup, BoxLayout.Y_AXIS));
		content.add(leftAligned(setup));
		setup.add(leftAligned(sectionTitle("Run options")));
		JPanel options = section();
		options.setLayout(new GridLayout(0, 2, 4, 2));
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
			options.add(box);
		}
		setup.add(leftAligned(options));

		setup.add(leftAligned(sectionTitle("Locations")));
		locations.setLayout(new BoxLayout(locations, BoxLayout.Y_AXIS));
		setup.add(leftAligned(locations));
		for (FarmLocation location : FarmLocation.values())
		{
			JPanel block = section();
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
			block.add(row(row.enabled, row.status));

			row.teleport.setToolTipText("What to bring to get to " + location.getDisplayName()
				+ ". The checklist and bank filter show it, and the route uses it if you carry it.");
			row.teleport.addActionListener(e -> changed.run());
			block.add(indented(row.teleport));

			// Space below each location, inside the block so it hides with it
			block.setBorder(new CompoundBorder(new MatteBorder(0, 0, 4, 0, ColorScheme.DARK_GRAY_COLOR), block.getBorder()));
			rows.put(location, row);
			leftAligned(block);
		}

		content.add(leftAligned(sectionTitle("Checklist")));
		checklist.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		checklist.setBorder(new EmptyBorder(6, 6, 6, 6));
		content.add(leftAligned(checklist));

		add(content, BorderLayout.NORTH);
	}

	/**
	 * Must be called on the Swing event thread.
	 */
	void update(PanelState state)
	{
		runActive = state.isRunActive();
		int stops = state.getTargets().size();
		RunType runType = state.getRunType();

		warnings.setText(warningText(state));
		warnings.setVisible(!warnings.getText().isEmpty());

		if (!state.isLoggedIn())
		{
			summary.setText("Log in to see your patches.");
		}
		else if (runActive)
		{
			long done = state.getRunStops().stream().filter(stop -> stop.getStage() == StopView.Stage.DONE).count();
			summary.setText(capitalised(runType.getNoun()) + ": " + done + " of " + state.getRunStops().size() + " stops done");
		}
		else if (stops == 0)
		{
			summary.setText("No stops need attention for a " + runType.getNoun() + ".");
		}
		else
		{
			summary.setText(stops == 1 ? "1 stop needs attention." : stops + " stops need attention.");
		}

		contract.setText(state.getContract() == null ? "" : "Contract: " + state.getContract());
		contract.setVisible(state.getContract() != null);

		// Setting up is for before a run; during one the panel follows its stops
		types.setVisible(!runActive);
		setup.setVisible(!runActive);
		run.setVisible(runActive);
		if (runActive)
		{
			showStops(state.getRunStops());
		}

		startStop.setText(runActive ? "Stop " + runType.getNoun() : "Start " + runType.getNoun());
		// A run follows the patches through Time Tracking, so it can't get anywhere without it
		startStop.setEnabled(runActive || (state.isLoggedIn() && stops > 0 && state.isTimeTrackingActive()));
		reroute.setVisible(runActive && state.isShortestPathActive());

		if (runType != shownRunType)
		{
			// The locations of this kind of run, in the order its guide visits them
			shownRunType = runType;
			locations.removeAll();
			for (FarmLocation location : FarmLocation.recommendedOrder(runType))
			{
				locations.add(rows.get(location).block);
			}
		}

		updating = true;
		for (Map.Entry<RunType, JToggleButton> type : runTypes.entrySet())
		{
			type.getValue().setSelected(type.getKey() == runType);
			// The run in progress decides what the panel shows
			type.getValue().setEnabled(!runActive);
		}
		for (Map.Entry<RunOption, JCheckBox> option : runOptions.entrySet())
		{
			option.getValue().setSelected(state.getRunOptions().contains(option.getKey()));
			option.getValue().setVisible(option.getKey().getRunTypes().contains(runType));
		}
		for (FarmLocation location : FarmLocation.values())
		{
			LocationRow row = rows.get(location);
			if (!location.has(runType))
			{
				continue;
			}
			LocationSettings settings = state.getSettings().getOrDefault(location, LocationSettings.DEFAULT);

			row.enabled.setSelected(settings.isEnabled());
			row.enabled.setEnabled(state.isLoggedIn());
			row.teleport.setSelectedItem(settings.getTeleport());
			row.teleport.setEnabled(state.isLoggedIn());

			// The anchor patches decide whether a location is a stop; the tooltip has the rest
			PatchState shown = anchorState(location, runType, state);
			row.status.setText(statusText(shown, state.getNowMinutes()));
			row.status.setForeground(statusColor(shown.getStatus()));
			List<String> lines = new ArrayList<>();
			for (FarmPatch patch : location.getPatches())
			{
				if (RunType.of(patch.getType()) != runType)
				{
					continue;
				}
				lines.add(patch.getName() + ": " + statusText(state.getPatches().getOrDefault(patch, PatchState.UNKNOWN), state.getNowMinutes()));
			}
			row.status.setToolTipText("<html>" + String.join("<br>", lines) + "</html>");
		}
		updating = false;

		checklist.removeAll();
		for (ChecklistItem item : state.getChecklist())
		{
			JLabel value = new JLabel(item.getValue());
			value.setForeground(stateColor(item.getState()));
			checklist.add(row(name(item.getLabel()), value));
		}

		revalidate();
		repaint();
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
			new EmptyBorder(4, 6, 4, 6)));

		JLabel title = label(view.getLocation().getDisplayName(), Color.WHITE);
		title.setFont(FontManager.getRunescapeBoldFont());
		JLabel teleport = label(view.getTeleport() == null ? "" : view.getTeleport(), NEUTRAL);
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

		JButton skip = new JButton("Skip stop");
		skip.setToolTipText("Leave " + view.getLocation().getDisplayName() + " out of the rest of this run, for when it can't be finished");
		skip.addActionListener(e -> listener.skipStop(view.getLocation()));
		block.add(skip);
		return block;
	}

	private static JPanel compactLine(JLabel left, JLabel right)
	{
		JPanel line = line(left, right, ColorScheme.DARKER_GRAY_COLOR);
		line.setBorder(new CompoundBorder(new MatteBorder(0, 0, 1, 0, ColorScheme.DARK_GRAY_COLOR), new EmptyBorder(3, 9, 3, 6)));
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
	private static PatchState anchorState(FarmLocation location, RunType runType, PanelState state)
	{
		PatchState shown = null;
		for (FarmPatch patch : location.anchors(runType, state.getFarmingLevel()))
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

	private static String capitalised(String text)
	{
		return Character.toUpperCase(text.charAt(0)) + text.substring(1);
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
		label.setBorder(new EmptyBorder(12, 0, 4, 0));
		return label;
	}

	private static JPanel section()
	{
		JPanel panel = new JPanel(new GridLayout(0, 1, 0, 2));
		panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		panel.setBorder(new EmptyBorder(4, 6, 4, 6));
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
		panel.setBorder(new EmptyBorder(0, 20, 0, 0));
		panel.add(component, BorderLayout.CENTER);
		return panel;
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
