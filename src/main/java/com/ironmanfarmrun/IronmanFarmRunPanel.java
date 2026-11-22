package com.ironmanfarmrun;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

class IronmanFarmRunPanel extends PluginPanel
{
	interface Listener
	{
		void startRun();

		void stopRun();

		void reroute();

		void setLocationSettings(FarmLocation location, LocationSettings settings);

		void setRunOption(RunOption option, boolean on);
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
		private final JCheckBox enabled;
		private final JLabel status = new JLabel();
		private final JComboBox<Teleport> teleport;

		private LocationRow(FarmLocation location)
		{
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
	private final JPanel checklist = new JPanel(new GridLayout(0, 1, 0, 2));
	private final JButton startStop = new JButton();
	private final JButton reroute = new JButton("Show path again");

	private boolean runActive;
	/**
	 * Set while {@link #update} is changing checkboxes and dropdowns, so those changes aren't reported as the player's.
	 */
	private boolean updating;

	IronmanFarmRunPanel(Listener listener)
	{
		setLayout(new BorderLayout());
		setBorder(new EmptyBorder(8, 8, 8, 8));

		JPanel content = new JPanel();
		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

		JLabel title = new JLabel("Farm run");
		title.setFont(FontManager.getRunescapeBoldFont());
		title.setForeground(Color.WHITE);
		content.add(leftAligned(title));

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

		content.add(leftAligned(sectionTitle("Run options")));
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
		content.add(leftAligned(options));

		content.add(leftAligned(sectionTitle("Locations")));
		for (FarmLocation location : FarmLocation.values())
		{
			LocationRow row = new LocationRow(location);
			Runnable changed = () ->
			{
				if (!updating)
				{
					listener.setLocationSettings(location, row.settings());
				}
			};

			JPanel block = section();
			row.enabled.setBackground(ColorScheme.DARKER_GRAY_COLOR);
			row.enabled.setToolTipText("Stop at " + location.getDisplayName() + " on runs");
			row.enabled.addActionListener(e -> changed.run());
			block.add(row(row.enabled, row.status));

			row.teleport.setToolTipText("What to bring to get to " + location.getDisplayName()
				+ ". The checklist and bank filter show it, and the route uses it if you carry it.");
			row.teleport.addActionListener(e -> changed.run());
			block.add(indented(row.teleport));

			rows.put(location, row);
			content.add(leftAligned(block));
			content.add(Box.createVerticalStrut(4));
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

		warnings.setText(warningText(state));
		warnings.setVisible(!warnings.getText().isEmpty());

		if (!state.isLoggedIn())
		{
			summary.setText("Log in to see your patches.");
		}
		else if (runActive)
		{
			summary.setText(stops == 1 ? "Run active: 1 stop left." : "Run active: " + stops + " stops left.");
		}
		else if (stops == 0)
		{
			summary.setText("No herb patches need attention.");
		}
		else
		{
			summary.setText(stops == 1 ? "1 herb patch needs attention." : stops + " herb patches need attention.");
		}

		contract.setText(state.getContract() == null ? "" : "Contract: " + state.getContract());
		contract.setVisible(state.getContract() != null);

		startStop.setText(runActive ? "Stop run" : "Start run");
		startStop.setEnabled(runActive || (state.isLoggedIn() && stops > 0));
		reroute.setVisible(runActive);

		updating = true;
		for (Map.Entry<RunOption, JCheckBox> option : runOptions.entrySet())
		{
			option.getValue().setSelected(state.getRunOptions().contains(option.getKey()));
		}
		for (FarmLocation location : FarmLocation.values())
		{
			LocationRow row = rows.get(location);
			LocationSettings settings = state.getSettings().getOrDefault(location, LocationSettings.DEFAULT);

			row.enabled.setSelected(settings.isEnabled());
			row.enabled.setEnabled(state.isLoggedIn());
			row.teleport.setSelectedItem(settings.getTeleport());
			row.teleport.setEnabled(state.isLoggedIn());

			// The herb patch decides whether a location is a stop; the tooltip has the rest
			PatchState herb = state.getPatches().getOrDefault(location.herbPatch(), PatchState.UNKNOWN);
			row.status.setText(statusText(herb, state.getNowMinutes()));
			row.status.setForeground(statusColor(herb.getStatus()));
			List<String> lines = new ArrayList<>();
			for (FarmPatch patch : location.getPatches())
			{
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

	private static String warningText(PanelState state)
	{
		List<String> lines = new ArrayList<>();
		if (!state.isTimeTrackingActive())
		{
			lines.add("Turn on the Time Tracking plugin to keep patch states up to date.");
		}
		if (!state.isShortestPathActive())
		{
			lines.add("Install the Shortest Path plugin to get routes.");
		}
		if (!state.isBankTagsActive())
		{
			lines.add("Turn on the Bank Tags plugin to filter the bank.");
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
				return GOOD;
			case WEEDS:
			case EMPTY:
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
