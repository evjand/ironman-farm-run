package com.plugandplant;

/**
 * The presets the tests plan with.
 */
final class Presets
{
	static final RunPreset HERBS = herbs(false, false);
	static final RunPreset TREES = RunPreset.treeRun();

	private Presets()
	{
	}

	/**
	 * A herb run, tending allotments and flowers while there if asked to.
	 */
	static RunPreset herbs(boolean allotments, boolean flowers)
	{
		return RunPreset.herbRun()
			.withRole(PatchType.ALLOTMENT, allotments ? PatchRole.TEND : PatchRole.OFF)
			.withRole(PatchType.FLOWER, flowers ? PatchRole.TEND : PatchRole.OFF);
	}
}
