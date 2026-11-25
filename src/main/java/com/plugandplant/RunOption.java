package com.plugandplant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * A run preset's options, chosen in the side panel before a run.
 */
@Getter
@RequiredArgsConstructor
enum RunOption
{
	COMPOST("Compost", "Compost each patch you plant, while you carry compost"),
	PROTECT("Pay gardeners",
		"Pay each gardener to protect the tree you plant. The checklist and bank filter show the payment each sapling takes."),
	NOTE_HERBS("Note herbs",
		"Once the herb patch is replanted, use your herbs on the tool leprechaun to note them before the next patch"),
	NOTE_ALLOTMENTS_AND_FLOWERS("Note allotments & flowers",
		"Note allotment and flower produce at the tool leprechaun too. Leave it off to compost them instead."),
	GRACEFUL("Graceful", "Add the graceful outfit to the checklist and the bank filter"),
	VOLCANIC_ASH("Volcanic ash", "Add volcanic ash to the checklist and the bank filter");

	private final String label;
	private final String description;

	/**
	 * Whether the option means anything for the run: paying gardeners takes trees, noting herbs takes herbs.
	 */
	boolean appliesTo(RunPreset preset)
	{
		switch (this)
		{
			case PROTECT:
				return preset.includesTrees();
			case NOTE_HERBS:
				return preset.includes(PatchType.HERB);
			case NOTE_ALLOTMENTS_AND_FLOWERS:
				return preset.includes(PatchType.ALLOTMENT) || preset.includes(PatchType.FLOWER);
			default:
				return true;
		}
	}

	boolean isOn(RunPreset preset)
	{
		switch (this)
		{
			case COMPOST:
				return preset.isCompost();
			case PROTECT:
				return preset.isProtect();
			case NOTE_HERBS:
				return preset.isNoteHerbs();
			case NOTE_ALLOTMENTS_AND_FLOWERS:
				return preset.isNoteAllotmentsAndFlowers();
			case GRACEFUL:
				return preset.isGraceful();
			default:
				return preset.isVolcanicAsh();
		}
	}

	RunPreset set(RunPreset preset, boolean on)
	{
		switch (this)
		{
			case COMPOST:
				return preset.withCompost(on);
			case PROTECT:
				return preset.withProtect(on);
			case NOTE_HERBS:
				return preset.withNoteHerbs(on);
			case NOTE_ALLOTMENTS_AND_FLOWERS:
				return preset.withNoteAllotmentsAndFlowers(on);
			case GRACEFUL:
				return preset.withGraceful(on);
			default:
				return preset.withVolcanicAsh(on);
		}
	}
}
