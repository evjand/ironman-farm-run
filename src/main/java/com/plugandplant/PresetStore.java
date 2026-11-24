package com.plugandplant;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;

/**
 * Saves the player's run presets and teleport choices, per account, as accounts differ in what they
 * have unlocked. The first time, a herb run and a tree run are made from the settings of the versions
 * before presets.
 */
@Slf4j
@Singleton
class PresetStore
{
	private static final String PRESETS_KEY = "presets";
	private static final String SELECTED_KEY = "preset";
	private static final String TELEPORT_KEY_PREFIX = "teleport.";

	// The settings of the versions before presets, read once to make the first presets
	private static final String LEGACY_ENABLED_KEY_PREFIX = "patch.";
	private static final String LEGACY_TREE_KEY_PREFIX = "tree.";

	private final ConfigManager configManager;
	private final Gson gson;

	/**
	 * How a preset is saved: plain names, so a later version can read what an earlier one wrote.
	 */
	private static class Stored
	{
		String id;
		String name;
		Map<String, String> roles;
		List<String> disabledLocations;
		Map<String, List<String>> excludedTypes;
		boolean compost;
		boolean protect;
		boolean graceful;
		boolean volcanicAsh;
	}

	@Inject
	PresetStore(ConfigManager configManager, Gson gson)
	{
		this.configManager = configManager;
		this.gson = gson;
	}

	/**
	 * The player's presets; empty while no account is logged in.
	 */
	List<RunPreset> load()
	{
		if (configManager.getRSProfileKey() == null)
		{
			return List.of();
		}

		String json = configManager.getRSProfileConfiguration(PlugAndPlantConfig.GROUP, PRESETS_KEY);
		if (json != null)
		{
			try
			{
				List<Stored> stored = gson.fromJson(json, new TypeToken<List<Stored>>()
				{
				}.getType());
				List<RunPreset> presets = new ArrayList<>();
				for (Stored each : stored)
				{
					presets.add(fromStored(each));
				}
				if (!presets.isEmpty())
				{
					return presets;
				}
			}
			catch (JsonParseException | IllegalStateException e)
			{
				log.warn("Couldn't read the saved run presets, starting over", e);
			}
		}

		List<RunPreset> presets = fromLegacySettings();
		save(presets);
		return presets;
	}

	void save(List<RunPreset> presets)
	{
		if (configManager.getRSProfileKey() == null)
		{
			return;
		}
		List<Stored> stored = new ArrayList<>();
		for (RunPreset preset : presets)
		{
			stored.add(toStored(preset));
		}
		configManager.setRSProfileConfiguration(PlugAndPlantConfig.GROUP, PRESETS_KEY, gson.toJson(stored));
	}

	@Nullable
	String selectedId()
	{
		String id = configManager.getRSProfileConfiguration(PlugAndPlantConfig.GROUP, SELECTED_KEY);
		if (id == null)
		{
			// Before presets, the side panel remembered the kind of run
			return "TREE".equals(configManager.getConfiguration(PlugAndPlantConfig.GROUP, PlugAndPlantConfig.RUN_TYPE))
				? RunPreset.TREE_ID : RunPreset.HERB_ID;
		}
		return id;
	}

	void select(String id)
	{
		configManager.setRSProfileConfiguration(PlugAndPlantConfig.GROUP, SELECTED_KEY, id);
	}

	/**
	 * The teleport the player picked for a location, shared by every preset.
	 */
	Teleport teleport(FarmLocation location)
	{
		String saved = configManager.getRSProfileConfiguration(PlugAndPlantConfig.GROUP, TELEPORT_KEY_PREFIX + location.name());
		if (saved == null)
		{
			// Before presets, tree runs kept their own
			saved = configManager.getRSProfileConfiguration(PlugAndPlantConfig.GROUP,
				LEGACY_TREE_KEY_PREFIX + TELEPORT_KEY_PREFIX + location.name());
		}
		// Earlier versions' choices are read as what they became; one this version doesn't know is any
		Teleport teleport = saved == null ? null : Teleport.fromSaved(saved);
		return teleport != null && location.teleportChoices().contains(teleport) ? teleport : Teleport.ANY;
	}

	void setTeleport(FarmLocation location, Teleport teleport)
	{
		if (configManager.getRSProfileKey() != null)
		{
			configManager.setRSProfileConfiguration(PlugAndPlantConfig.GROUP, TELEPORT_KEY_PREFIX + location.name(), teleport.name());
		}
	}

	/**
	 * A herb run and a tree run with the options, allotments, flowers and locations the player had
	 * chosen before presets.
	 */
	private List<RunPreset> fromLegacySettings()
	{
		boolean compost = legacyFlag(PlugAndPlantConfig.COMPOST_STEP, true);
		boolean protect = legacyFlag(PlugAndPlantConfig.PROTECT_TREES, true);
		boolean graceful = legacyFlag(PlugAndPlantConfig.GRACEFUL, false);
		boolean volcanicAsh = legacyFlag(PlugAndPlantConfig.VOLCANIC_ASH, false);

		RunPreset herbs = RunPreset.herbRun()
			.withRole(PatchType.FLOWER, legacyFlag(PlugAndPlantConfig.FLOWERS, false) ? PatchRole.TEND : PatchRole.OFF)
			.withRole(PatchType.ALLOTMENT, legacyFlag(PlugAndPlantConfig.ALLOTMENTS, false) ? PatchRole.TEND : PatchRole.OFF)
			.withCompost(compost).withProtect(protect).withGraceful(graceful).withVolcanicAsh(volcanicAsh);
		herbs = herbs.withDisabledLocations(legacyDisabled(""));

		RunPreset trees = RunPreset.treeRun()
			.withCompost(compost).withProtect(protect).withGraceful(graceful).withVolcanicAsh(volcanicAsh)
			.withDisabledLocations(legacyDisabled(LEGACY_TREE_KEY_PREFIX));

		return List.of(herbs, trees);
	}

	private boolean legacyFlag(String key, boolean otherwise)
	{
		String value = configManager.getConfiguration(PlugAndPlantConfig.GROUP, key);
		return value == null ? otherwise : Boolean.parseBoolean(value);
	}

	private Set<FarmLocation> legacyDisabled(String prefix)
	{
		Set<FarmLocation> disabled = EnumSet.noneOf(FarmLocation.class);
		for (FarmLocation location : FarmLocation.values())
		{
			String value = configManager.getRSProfileConfiguration(PlugAndPlantConfig.GROUP,
				prefix + LEGACY_ENABLED_KEY_PREFIX + location.name());
			if ("false".equals(value))
			{
				disabled.add(location);
			}
		}
		return Collections.unmodifiableSet(disabled);
	}

	private static Stored toStored(RunPreset preset)
	{
		Stored stored = new Stored();
		stored.id = preset.getId();
		stored.name = preset.getName();
		stored.roles = new java.util.LinkedHashMap<>();
		preset.getRoles().forEach((type, role) -> stored.roles.put(type.name(), role.name()));
		stored.disabledLocations = new ArrayList<>();
		preset.getDisabledLocations().forEach(location -> stored.disabledLocations.add(location.name()));
		stored.excludedTypes = new java.util.LinkedHashMap<>();
		preset.getExcludedTypes().forEach((location, types) ->
		{
			List<String> names = new ArrayList<>();
			types.forEach(type -> names.add(type.name()));
			stored.excludedTypes.put(location.name(), names);
		});
		stored.compost = preset.isCompost();
		stored.protect = preset.isProtect();
		stored.graceful = preset.isGraceful();
		stored.volcanicAsh = preset.isVolcanicAsh();
		return stored;
	}

	/**
	 * Names this version doesn't know, from a later one, are skipped.
	 */
	static RunPreset fromStored(Stored stored)
	{
		Map<PatchType, PatchRole> roles = new EnumMap<>(PatchType.class);
		for (PatchType type : RunPreset.TYPES)
		{
			roles.put(type, PatchRole.OFF);
		}
		if (stored.roles != null)
		{
			stored.roles.forEach((type, role) ->
			{
				PatchType patchType = parse(PatchType.class, type);
				PatchRole patchRole = parse(PatchRole.class, role);
				if (patchType != null && patchRole != null)
				{
					roles.put(patchType, patchRole);
				}
			});
		}

		Set<FarmLocation> disabled = EnumSet.noneOf(FarmLocation.class);
		if (stored.disabledLocations != null)
		{
			for (String name : stored.disabledLocations)
			{
				FarmLocation location = parse(FarmLocation.class, name);
				if (location != null)
				{
					disabled.add(location);
				}
			}
		}

		Map<FarmLocation, Set<PatchType>> excluded = new EnumMap<>(FarmLocation.class);
		if (stored.excludedTypes != null)
		{
			stored.excludedTypes.forEach((name, types) ->
			{
				FarmLocation location = parse(FarmLocation.class, name);
				if (location == null || types == null)
				{
					return;
				}
				Set<PatchType> patchTypes = EnumSet.noneOf(PatchType.class);
				for (String type : types)
				{
					PatchType patchType = parse(PatchType.class, type);
					if (patchType != null)
					{
						patchTypes.add(patchType);
					}
				}
				if (!patchTypes.isEmpty())
				{
					excluded.put(location, Collections.unmodifiableSet(patchTypes));
				}
			});
		}

		return new RunPreset(stored.id == null ? "preset" : stored.id, stored.name == null ? "Run" : stored.name,
			Collections.unmodifiableMap(roles), Collections.unmodifiableSet(disabled), Collections.unmodifiableMap(excluded),
			stored.compost, stored.protect, stored.graceful, stored.volcanicAsh);
	}

	@Nullable
	private static <E extends Enum<E>> E parse(Class<E> type, @Nullable String name)
	{
		if (name == null)
		{
			return null;
		}
		try
		{
			return Enum.valueOf(type, name);
		}
		catch (IllegalArgumentException e)
		{
			return null;
		}
	}

	/**
	 * For tests: a preset saved and read back.
	 */
	static RunPreset roundTrip(Gson gson, RunPreset preset)
	{
		return fromStored(gson.fromJson(gson.toJson(toStored(preset)), Stored.class));
	}
}
