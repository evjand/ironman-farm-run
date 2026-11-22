package com.plugandplant;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class PlugAndPlantPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(PlugAndPlantPlugin.class);
		RuneLite.main(args);
	}
}
