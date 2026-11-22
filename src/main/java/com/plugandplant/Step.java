package com.plugandplant;

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
	CHECK_HEALTH("Check health", new Color(0x4C, 0xD9, 0x64)),
	/**
	 * Take down a grown tree: chop it, or pay the gardener to.
	 */
	REMOVE("Remove", new Color(0xB0, 0x8C, 0xF0)),
	DIG("Dig up", new Color(0xE0, 0x5A, 0x4E)),
	CLEAR("Clear", new Color(0xE0, 0x5A, 0x4E)),
	CURE("Cure", new Color(0xE0, 0x5A, 0x4E)),
	RAKE("Rake", new Color(0xE6, 0xC8, 0x3C)),
	COMPOST("Compost", new Color(0xD9, 0x8E, 0x3A)),
	PLANT("Plant", new Color(0x4C, 0xC3, 0xF0)),
	/**
	 * Pay the gardener to protect a newly planted tree.
	 */
	PAY("Pay", new Color(0xFF, 0x6E, 0xC7));

	private final String label;
	private final Color color;
}
