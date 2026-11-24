package com.plugandplant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * The groups the bank filter lays items out in, in the order they are shown.
 */
@Getter
@RequiredArgsConstructor
enum BankSection
{
	/**
	 * Whatever the run uses that can be worn, first so it can all be taken and put on before the rest.
	 */
	WEARABLES("Wear"),
	EQUIPMENT("Equipment"),
	HERB_SEEDS("Herb seeds"),
	ALLOTMENT_SEEDS("Allotment seeds"),
	FLOWER_SEEDS("Flower seeds"),
	TREE_SAPLINGS("Tree saplings"),
	FRUIT_TREE_SAPLINGS("Fruit tree saplings"),
	PAYMENTS("Gardener payments"),
	TELEPORTS("Teleports"),
	/**
	 * Tree seeds on their way to becoming saplings, and what it takes to grow them.
	 */
	GROWING("Saplings to grow");

	private final String title;
}
