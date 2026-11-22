package com.ironmanfarmrun;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class IronmanFarmRunPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(IronmanFarmRunPlugin.class);
		RuneLite.main(args);
	}
}
