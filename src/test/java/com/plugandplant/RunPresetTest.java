package com.plugandplant;

import com.google.gson.Gson;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class RunPresetTest
{
	private static final FarmPatch GUILD_HERB = FarmLocation.FARMING_GUILD.patches(PatchType.HERB).get(0);
	private static final FarmPatch GUILD_FLOWER = FarmLocation.FARMING_GUILD.patches(PatchType.FLOWER).get(0);
	private static final FarmPatch GUILD_TREE = FarmLocation.FARMING_GUILD.patches(PatchType.TREE).get(0);
	private static final FarmPatch GUILD_FRUIT_TREE = FarmLocation.FARMING_GUILD.patches(PatchType.FRUIT_TREE).get(0);

	private static RunPlan plan(RunPreset preset, PatchStatus status, FarmPatch... ready)
	{
		Map<FarmPatch, PatchState> states = new HashMap<>();
		for (FarmPatch patch : ready)
		{
			states.put(patch, new PatchState(status, 0));
		}
		return RunPlan.of(List.of(FarmLocation.FARMING_GUILD), states, Set.of(), Set.of(), Set.of(),
			new RunPlan.Options(preset, false, false, 99), Carried.NOTHING, null);
	}

	@Test
	public void theDefaultsAreAHerbRunAndATreeRun()
	{
		assertEquals(List.of(GUILD_HERB), Presets.HERBS.anchors(FarmLocation.FARMING_GUILD));
		assertEquals(List.of(GUILD_TREE, GUILD_FRUIT_TREE), Presets.TREES.anchors(FarmLocation.FARMING_GUILD));
		assertFalse(Presets.HERBS.includesTrees());
		assertTrue(Presets.TREES.includesTrees());
	}

	@Test
	public void aRunCanVisitForHerbsAndTreesAlike()
	{
		RunPreset both = Presets.HERBS.withRole(PatchType.TREE, PatchRole.VISIT);

		assertEquals(List.of(GUILD_HERB, GUILD_TREE), both.anchors(FarmLocation.FARMING_GUILD));
		assertEquals(Set.of(GUILD_HERB, GUILD_TREE), plan(both, PatchStatus.READY, GUILD_HERB, GUILD_TREE).getSteps().keySet());
		// Trees and herbs together keep the herb run's order, with the tree locations after
		assertEquals(FarmLocation.FALADOR, both.locations().get(0));
		assertTrue(both.locations().contains(FarmLocation.LUMBRIDGE));
	}

	@Test
	public void aKindOfPatchCanBeLeftOutAtOneLocation()
	{
		RunPreset noGuildFruitTree = Presets.TREES.withExcluded(FarmLocation.FARMING_GUILD, PatchType.FRUIT_TREE, true);

		assertEquals(List.of(GUILD_TREE), noGuildFruitTree.anchors(FarmLocation.FARMING_GUILD));
		assertEquals(Set.of(GUILD_TREE), plan(noGuildFruitTree, PatchStatus.CHECK_HEALTH, GUILD_TREE, GUILD_FRUIT_TREE).getSteps().keySet());
		// Only there
		assertEquals(PatchRole.VISIT, noGuildFruitTree.role(FarmLocation.CATHERBY_ORCHARD.patches(PatchType.FRUIT_TREE).get(0)));
		assertEquals(Presets.TREES, noGuildFruitTree.withExcluded(FarmLocation.FARMING_GUILD, PatchType.FRUIT_TREE, false));
	}

	@Test
	public void flowersVisitedForAreAnchorsButTendedOnesAreNot()
	{
		RunPreset tend = Presets.HERBS.withRole(PatchType.FLOWER, PatchRole.TEND);
		RunPreset visit = Presets.HERBS.withRole(PatchType.FLOWER, PatchRole.VISIT);

		assertEquals(List.of(GUILD_HERB), tend.anchors(FarmLocation.FARMING_GUILD));
		assertTrue(visit.anchors(FarmLocation.FARMING_GUILD).contains(GUILD_FLOWER));
		// Tended flowers need seeds; ones visited for are planned whatever is carried
		assertTrue(plan(tend, PatchStatus.READY, GUILD_FLOWER).getSteps().isEmpty());
		assertEquals(Set.of(GUILD_FLOWER), plan(visit, PatchStatus.READY, GUILD_FLOWER).getSteps().keySet());
	}

	@Test
	public void aDisabledLocationIsRememberedPerPreset()
	{
		RunPreset withoutGuild = Presets.HERBS.withLocationEnabled(FarmLocation.FARMING_GUILD, false);

		assertFalse(withoutGuild.isEnabled(FarmLocation.FARMING_GUILD));
		assertTrue(Presets.TREES.isEnabled(FarmLocation.FARMING_GUILD));
		assertEquals(Presets.HERBS, withoutGuild.withLocationEnabled(FarmLocation.FARMING_GUILD, true));
	}

	@Test
	public void presetsAreSavedAndReadBack()
	{
		RunPreset custom = Presets.HERBS.withId("custom-1").withName("Everything")
			.withRole(PatchType.TREE, PatchRole.VISIT)
			.withRole(PatchType.FLOWER, PatchRole.TEND)
			.withLocationEnabled(FarmLocation.WEISS, false)
			.withExcluded(FarmLocation.FARMING_GUILD, PatchType.TREE, true)
			.withGraceful(true);

		assertEquals(custom, PresetStore.roundTrip(new Gson(), custom));
	}

	@Test
	public void optionsFollowThePreset()
	{
		assertFalse(RunOption.PROTECT.appliesTo(Presets.HERBS));
		assertTrue(RunOption.PROTECT.appliesTo(Presets.TREES));
		assertTrue(RunOption.GRACEFUL.isOn(RunOption.GRACEFUL.set(Presets.HERBS, true)));
	}
}
