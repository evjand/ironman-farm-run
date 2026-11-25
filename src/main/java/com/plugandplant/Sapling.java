package com.plugandplant;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import lombok.Getter;
import lombok.Value;
import net.runelite.api.gameval.ItemID;

/**
 * The trees runs plant, each with what its gardener asks to protect it. Gardeners take the payment
 * noted too.
 */
@Getter
public enum Sapling
{
	OAK("Oak", ItemID.PLANTPOT_OAK_SAPLING, ItemID.PLANTPOT_ACORN_WATERED, pay(ItemID.BASKET_TOMATO_5, 1, "basket of tomatoes")),
	WILLOW("Willow", ItemID.PLANTPOT_WILLOW_SAPLING, ItemID.PLANTPOT_WILLOW_SEED_WATERED, pay(ItemID.BASKET_APPLE_5, 1, "basket of apples")),
	MAPLE("Maple", ItemID.PLANTPOT_MAPLE_SAPLING, ItemID.PLANTPOT_MAPLE_SEED_WATERED, pay(ItemID.BASKET_ORANGE_5, 1, "basket of oranges")),
	YEW("Yew", ItemID.PLANTPOT_YEW_SAPLING, ItemID.PLANTPOT_YEW_SEED_WATERED, pay(ItemID.CACTUS_SPINE, 10, "cactus spines")),
	MAGIC("Magic", ItemID.PLANTPOT_MAGIC_TREE_SAPLING, ItemID.PLANTPOT_MAGIC_TREE_SEED_WATERED, pay(ItemID.COCONUT, 25, "coconuts")),

	APPLE("Apple", ItemID.PLANTPOT_APPLE_SAPLING, ItemID.PLANTPOT_APPLE_SEED_WATERED, pay(ItemID.SWEETCORN, 9, "sweetcorn")),
	BANANA("Banana", ItemID.PLANTPOT_BANANA_SAPLING, ItemID.PLANTPOT_BANANA_SEED_WATERED, pay(ItemID.BASKET_APPLE_5, 4, "baskets of apples")),
	ORANGE("Orange", ItemID.PLANTPOT_ORANGE_SAPLING, ItemID.PLANTPOT_ORANGE_SEED_WATERED, pay(ItemID.BASKET_STRAWBERRY_5, 3, "baskets of strawberries")),
	CURRY("Curry", ItemID.PLANTPOT_CURRY_SAPLING, ItemID.PLANTPOT_CURRY_SEED_WATERED, pay(ItemID.BASKET_BANANA_5, 5, "baskets of bananas")),
	PINEAPPLE("Pineapple", ItemID.PLANTPOT_PINEAPPLE_SAPLING, ItemID.PLANTPOT_PINEAPPLE_SEED_WATERED, pay(ItemID.WATERMELON, 10, "watermelons")),
	PAPAYA("Papaya", ItemID.PLANTPOT_PAPAYA_SAPLING, ItemID.PLANTPOT_PAPAYA_SEED_WATERED, pay(ItemID.PINEAPPLE, 10, "pineapples")),
	PALM("Palm", ItemID.PLANTPOT_PALM_SAPLING, ItemID.PLANTPOT_PALM_SEED_WATERED, pay(ItemID.PAPAYA, 15, "papaya fruit")),
	DRAGONFRUIT("Dragonfruit", ItemID.PLANTPOT_DRAGONFRUIT_SAPLING, ItemID.PLANTPOT_DRAGONFRUIT_SEED_WATERED, pay(ItemID.COCONUT, 15, "coconuts")),

	TEAK("Teak", ItemID.PLANTPOT_TEAK_SAPLING, ItemID.PLANTPOT_TEAK_SEED_WATERED, pay(ItemID.LIMPWURT_ROOT, 15, "limpwurt roots")),
	MAHOGANY("Mahogany", ItemID.PLANTPOT_MAHOGANY_SAPLING, ItemID.PLANTPOT_MAHOGANY_SEED_WATERED, pay(ItemID.YANILLIAN_HOPS, 25, "Yanillian hops")),
	CAMPHOR("Camphor", ItemID.PLANTPOT_CAMPHOR_SAPLING, ItemID.PLANTPOT_CAMPHOR_SEED_WATERED, pay(ItemID.WHITE_BERRIES, 10, "white berries")),
	IRONWOOD("Ironwood", ItemID.PLANTPOT_IRONWOOD_SAPLING, ItemID.PLANTPOT_IRONWOOD_SEED_WATERED, pay(ItemID.CURRY_LEAF, 10, "curry leaves")),
	ROSEWOOD("Rosewood", ItemID.PLANTPOT_ROSEWOOD_SAPLING, ItemID.PLANTPOT_ROSEWOOD_SEED_WATERED, pay(ItemID.DRAGONFRUIT, 8, "dragonfruit")),
	CALQUAT("Calquat", ItemID.PLANTPOT_CALQUAT_SAPLING, ItemID.PLANTPOT_CALQUAT_SEED_WATERED, pay(ItemID.POISONIVY_BERRIES, 8, "poison ivy berries")),
	SPIRIT_TREE("Spirit", ItemID.PLANTPOT_SPIRIT_TREE_SAPLING, ItemID.PLANTPOT_SPIRIT_TREE_SEED_WATERED,
		pay(ItemID.MM_MONKEY_NUTS, 5, "monkey nuts"), pay(ItemID.MM_MONKEY_BAR, 1, "monkey bar"),
		pay(ItemID.LUNAR_GROUNDTOOTH, 1, "ground tooth")),
	CELASTRUS("Celastrus", ItemID.PLANTPOT_CELASTRUS_TREE_SAPLING, ItemID.PLANTPOT_CELASTRUS_TREE_SEED_WATERED, pay(ItemID.CACTUS_POTATO, 8, "potato cacti")),
	REDWOOD("Redwood", ItemID.PLANTPOT_REDWOOD_TREE_SAPLING, ItemID.PLANTPOT_REDWOOD_TREE_SEED_WATERED, pay(ItemID.DRAGONFRUIT, 6, "dragonfruit")),
	/**
	 * Can't get diseased, so there is nothing to pay for.
	 */
	CRYSTAL_TREE("Crystal", ItemID.PLANTPOT_CRYSTAL_TREE_SAPLING, ItemID.PLANTPOT_CRYSTAL_TREE_SEED_WATERED);

	private final String name;
	private final int sapling;
	/**
	 * The watered seedling that grows into the sapling, which counts as one.
	 */
	private final int wateredSeedling;
	/**
	 * Everything the gardener asks for, all of it; none for a tree that can't be protected.
	 */
	private final List<Payment> payments;

	/**
	 * Part of a gardener's payment: this many of an item.
	 */
	@Value
	static class Payment
	{
		int item;
		int amount;
		String name;

		String label()
		{
			return amount + " " + name;
		}
	}

	Sapling(String name, int sapling, int wateredSeedling, Payment... payments)
	{
		this.name = name;
		this.sapling = sapling;
		this.wateredSeedling = wateredSeedling;
		this.payments = List.of(payments);
	}

	private static Payment pay(int item, int amount, String name)
	{
		return new Payment(item, amount, name);
	}

	@Nullable
	static Sapling bySapling(int itemId)
	{
		for (Sapling sapling : values())
		{
			if (sapling.sapling == itemId || sapling.wateredSeedling == itemId)
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
		List<String> parts = new ArrayList<>();
		for (Payment payment : payments)
		{
			parts.add(payment.label());
		}
		if (parts.size() < 2)
		{
			return String.join("", parts);
		}
		return String.join(", ", parts.subList(0, parts.size() - 1)) + " and " + parts.get(parts.size() - 1);
	}

	/**
	 * Whether the player carries the whole payment.
	 */
	boolean canPay(Carried carried)
	{
		for (Payment payment : payments)
		{
			if (carried.count(payment.getItem()) < payment.getAmount())
			{
				return false;
			}
		}
		return !payments.isEmpty();
	}
}
