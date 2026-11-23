package com.plugandplant;

import lombok.Value;

/**
 * One line of the "what to bring" checklist.
 */
@Value
class ChecklistItem
{
	enum State
	{
		GOOD,
		MISSING,
		/**
		 * Not on the player, but kept with the tool leprechaun, so it can be taken from there or the bank.
		 */
		STORED,
		/**
		 * A suggestion rather than something to fetch, such as wearing an item to free a slot.
		 */
		HINT,
		/**
		 * Not found on the player, but they may well have it covered some way this plugin can't see,
		 * such as a seed box it hasn't been shown into, or an axe instead of coins.
		 */
		UNSURE
	}

	String label;
	String value;
	State state;
}
