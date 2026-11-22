package com.ironmanfarmrun;

import lombok.Value;

/**
 * What the player has chosen for one location.
 */
@Value
class LocationSettings
{
	static final LocationSettings DEFAULT = new LocationSettings(true, Teleport.ANY);

	/**
	 * Whether runs stop here at all.
	 */
	boolean enabled;
	Teleport teleport;
}
