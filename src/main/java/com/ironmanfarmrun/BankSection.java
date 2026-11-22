package com.ironmanfarmrun;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * The groups the bank filter lays items out in, in the order they are shown.
 */
@Getter
@RequiredArgsConstructor
enum BankSection
{
	HERB_SEEDS("Herb seeds"),
	ALLOTMENT_SEEDS("Allotment seeds"),
	FLOWER_SEEDS("Flower seeds"),
	TREE_SAPLINGS("Tree saplings"),
	FRUIT_TREE_SAPLINGS("Fruit tree saplings"),
	PAYMENTS("Gardener payments"),
	EQUIPMENT("Equipment"),
	TELEPORTS("Teleports");

	private final String title;
}
