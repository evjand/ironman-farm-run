package com.plugandplant;

import javax.annotation.Nullable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.gameval.ItemID;

/**
 * The trees and fruit trees a tree run plants, each with what its gardener asks to protect it.
 * Gardeners take the payment noted too.
 */
@Getter
@RequiredArgsConstructor
public enum Sapling
{
	OAK("Oak", ItemID.PLANTPOT_OAK_SAPLING, ItemID.BASKET_TOMATO_5, 1, "basket of tomatoes"),
	WILLOW("Willow", ItemID.PLANTPOT_WILLOW_SAPLING, ItemID.BASKET_APPLE_5, 1, "basket of apples"),
	MAPLE("Maple", ItemID.PLANTPOT_MAPLE_SAPLING, ItemID.BASKET_ORANGE_5, 1, "basket of oranges"),
	YEW("Yew", ItemID.PLANTPOT_YEW_SAPLING, ItemID.CACTUS_SPINE, 10, "cactus spines"),
	MAGIC("Magic", ItemID.PLANTPOT_MAGIC_TREE_SAPLING, ItemID.COCONUT, 25, "coconuts"),

	APPLE("Apple", ItemID.PLANTPOT_APPLE_SAPLING, ItemID.SWEETCORN, 9, "sweetcorn"),
	BANANA("Banana", ItemID.PLANTPOT_BANANA_SAPLING, ItemID.BASKET_APPLE_5, 4, "baskets of apples"),
	ORANGE("Orange", ItemID.PLANTPOT_ORANGE_SAPLING, ItemID.BASKET_STRAWBERRY_5, 3, "baskets of strawberries"),
	CURRY("Curry", ItemID.PLANTPOT_CURRY_SAPLING, ItemID.BASKET_BANANA_5, 5, "baskets of bananas"),
	PINEAPPLE("Pineapple", ItemID.PLANTPOT_PINEAPPLE_SAPLING, ItemID.WATERMELON, 10, "watermelons"),
	PAPAYA("Papaya", ItemID.PLANTPOT_PAPAYA_SAPLING, ItemID.PINEAPPLE, 10, "pineapples"),
	PALM("Palm", ItemID.PLANTPOT_PALM_SAPLING, ItemID.PAPAYA, 15, "papaya fruit"),
	DRAGONFRUIT("Dragonfruit", ItemID.PLANTPOT_DRAGONFRUIT_SAPLING, ItemID.COCONUT, 15, "coconuts");

	private final String name;
	private final int sapling;
	private final int payment;
	private final int paymentAmount;
	private final String paymentName;

	@Nullable
	static Sapling bySapling(int itemId)
	{
		for (Sapling sapling : values())
		{
			if (sapling.sapling == itemId)
			{
				return sapling;
			}
		}
		return null;
	}

	/**
	 * What to hand the gardener, as the checklist puts it.
	 */
	String paymentLabel()
	{
		return paymentAmount + " " + paymentName;
	}
}
