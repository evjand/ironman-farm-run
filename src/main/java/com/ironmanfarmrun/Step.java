package com.ironmanfarmrun;

import java.awt.Color;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * The next thing to do at a patch.
 */
@Getter
@RequiredArgsConstructor
public enum Step
{
	HARVEST("Harvest", new Color(0x4C, 0xD9, 0x64)),
	CLEAR("Clear", new Color(0xE0, 0x5A, 0x4E)),
	CURE("Cure", new Color(0xE0, 0x5A, 0x4E)),
	RAKE("Rake", new Color(0xE6, 0xC8, 0x3C)),
	COMPOST("Compost", new Color(0xD9, 0x8E, 0x3A)),
	PLANT("Plant", new Color(0x4C, 0xC3, 0xF0));

	private final String label;
	private final Color color;
}
