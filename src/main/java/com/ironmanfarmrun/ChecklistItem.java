package com.ironmanfarmrun;

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
		 * Not found on the player, but they may well have it covered some way this plugin can't see,
		 * such as the tool leprechaun, a seed box or a staff.
		 */
		UNSURE
	}

	String label;
	String value;
	State state;
}
