/*
 * The tables in this file are derived from the Time Tracking plugin of RuneLite
 * (PatchImplementation and Produce), which is distributed under the following license:
 *
 * Copyright (c) 2018 Abex
 * Copyright (c) 2019 Abex
 * Copyright (c) 2018, NotFoxtrot <https://github.com/NotFoxtrot>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.ironmanfarmrun;

/**
 * What each value of a patch's varbit means, per kind of patch. Time Tracking keeps the classes that
 * interpret its saved data package-private, so its tables are mirrored here.
 *
 * An entry of zero or more is a healthy crop with that many growth ticks left before it is fully
 * grown. A negative entry is one of the constants below. For trees, the crop tables say which tree
 * is growing, which decides the gardener's payment.
 */
final class PatchStateTables
{
	static final int UNKNOWN = -1;
	/**
	 * Raked and ready to plant.
	 */
	static final int CLEAR = -2;
	static final int WEEDS = -3;
	static final int READY = -4;
	static final int DISEASED = -5;
	static final int DEAD = -6;
	/**
	 * A tree that has finished growing and waits for its health check.
	 */
	static final int CHECK_HEALTH = -7;
	/**
	 * A tree whose health has been checked, still standing.
	 */
	static final int GROWN = -8;
	static final int STUMP = -9;

	private PatchStateTables()
	{
	}

	/**
	 * Growth tick: 20 minutes.
	 */
	static final int[] HERB = {
		-3, -3, -3, -2, 4, 3, 2, 1, -4, -4, -4, 4, 3, 2, 1, -4,
		-4, -4, 4, 3, 2, 1, -4, -4, -4, 4, 3, 2, 1, -4, -4, -4,
		4, 3, 2, 1, -4, -4, -4, 4, 3, 2, 1, -4, -4, -4, 4, 3,
		2, 1, -4, -4, -4, 4, 3, 2, 1, -4, -4, -4, 4, 3, 2, 1,
		-4, -4, -4, -3, 4, 3, 2, 1, -4, -4, -4, 4, 3, 2, 1, -4,
		-4, -4, 4, 3, 2, 1, -4, -4, -4, 4, 3, 2, 1, -4, -4, -4,
		4, 3, 2, 1, -4, -4, -4, 4, 3, 2, 1, -4, -4, -4, -1, -1,
		-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
		-5, -5, -5, -5, -5, -5, -5, -5, -5, -5, -5, -5, -5, -5, -5, -5,
		-5, -5, -5, -5, -5, -5, -5, -5, -5, -5, -5, -5, -5, -5, -5, -5,
		-5, -5, -5, -5, -5, -5, -5, -5, -5, -5, -6, -6, -6, -5, -5, -5,
		-3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3,
		4, 3, 2, 1, -4, -4, -5, -5, -5, -6, -6, -6, -3, -3, -3, -3,
		-3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -1, -3, -3, -3,
		-3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3,
		-3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3,
	};

	/**
	 * Growth tick: 10 minutes.
	 */
	static final int[] ALLOTMENT = {
		-3, -3, -3, -2, -3, -3, 4, 3, 2, 1, -4, -4, -4, 4, 3, 2,
		1, -4, -4, -4, 4, 3, 2, 1, -4, -4, -4, 4, 3, 2, 1, -4,
		-4, -4, 6, 5, 4, 3, 2, 1, -4, -4, -4, 6, 5, 4, 3, 2,
		1, -4, -4, -4, 8, 7, 6, 5, 4, 3, 2, 1, -4, -4, -4, 7,
		6, 5, 4, 3, 2, 1, 4, 3, 2, 1, -3, -3, -3, 4, 3, 2,
		1, -3, -3, -3, 4, 3, 2, 1, -3, -3, -3, 4, 3, 2, 1, -3,
		-3, -3, 6, 5, 4, 3, 2, 1, -3, -3, -3, 6, 5, 4, 3, 2,
		1, -3, -3, -3, 8, 7, 6, 5, 4, 3, 2, 1, -3, -3, -3, -3,
		7, 6, 5, 4, 3, 2, 1, -5, -5, -5, -4, -4, -4, -3, -5, -5,
		-5, -3, -3, -3, -3, -5, -5, -5, -3, -3, -3, -3, -5, -5, -5, -3,
		-3, -3, -3, -5, -5, -5, -5, -5, -3, -3, -3, -3, -5, -5, -5, -5,
		-5, -3, -3, -3, -3, -5, -5, -5, -5, -5, -5, -5, -3, -3, -3, -3,
		-3, -6, -6, -6, -5, -5, -5, -6, -6, -6, -5, -5, -5, -3, -6, -6,
		-6, -6, -6, -6, -3, -6, -6, -6, -3, -3, -3, -3, -6, -6, -6, -3,
		-3, -3, -3, -6, -6, -6, -6, -6, -3, -3, -3, -3, -6, -6, -6, -6,
		-6, -3, -3, -3, -3, -6, -6, -6, -6, -6, -6, -6, -3, -3, -3, -3,
	};

	/**
	 * Growth tick: 5 minutes.
	 */
	static final int[] FLOWER = {
		-3, -3, -3, -2, -3, -3, -3, -3, 4, 3, 2, 1, -4, 4, 3, 2,
		1, -4, 4, 3, 2, 1, -4, 4, 3, 2, 1, -4, 4, 3, 2, 1,
		-4, 1, 2, 3, 3, 4, 3, 2, 1, -4, -3, -3, -3, -3, -3, -3,
		-3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3,
		-3, -3, -3, -3, -3, -3, -3, -3, 4, 3, 2, 1, -3, 4, 3, 2,
		1, -3, 4, 3, 2, 1, -3, 4, 3, 2, 1, -3, 4, 3, 2, 1,
		-3, -3, -3, -3, -3, 4, 3, 2, 1, -3, -3, -3, -3, -3, -3, -3,
		-3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3,
		-3, -3, -3, -3, -3, -3, -3, -3, -3, -5, -5, -5, -3, -3, -5, -5,
		-5, -3, -3, -5, -5, -5, -3, -3, -5, -5, -5, -3, -3, -5, -5, -5,
		-3, -3, -3, -3, -3, -3, -5, -5, -5, -3, -3, -3, -3, -3, -3, -3,
		-3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3,
		-3, -3, -3, -3, -3, -3, -3, -3, -3, -6, -6, -6, -6, -3, -6, -6,
		-6, -6, -3, -6, -6, -6, -6, -3, -6, -6, -6, -6, -3, -6, -6, -6,
		-6, -3, -3, -3, -3, -3, -6, -6, -6, -6, -3, -3, -3, -3, -3, -3,
		-3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3,
	};

	/**
	 * Growth tick: 40 minutes.
	 */
	static final int[] TREE = {
		-3, -3, -3, -2, -3, -3, -3, -3, 4, 3, 2, 1, -7, -8, -9, 6,
		5, 4, 3, 2, 1, -7, -8, -9, 8, 7, 6, 5, 4, 3, 2, 1,
		-7, -8, -9, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1, -7, -8, -9,
		12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1, -7, -8, -9, -3,
		-3, -3, -3, -3, -3, -3, -3, -3, -3, -5, -5, -5, -1, -5, -3, -3,
		-5, -5, -5, -5, -5, -1, -5, -3, -3, -5, -5, -5, -5, -5, -5, -5,
		-1, -5, -3, -3, -5, -5, -5, -5, -5, -5, -5, -5, -5, -1, -5, -3,
		-3, -5, -5, -5, -5, -5, -5, -5, -5, -5, -5, -5, -1, -5, -3, -3,
		-3, -3, -3, -3, -3, -3, -3, -3, -3, -6, -6, -6, -1, -6, -3, -3,
		-6, -6, -6, -6, -6, -1, -6, -3, -3, -6, -6, -6, -6, -6, -6, -6,
		-1, -6, -3, -3, -6, -6, -6, -6, -6, -6, -6, -6, -6, -1, -6, -3,
		-3, -6, -6, -6, -6, -6, -6, -6, -6, -6, -6, -6, -1, -6, -3, -3,
		-8, -8, -8, -8, -8, -8, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3,
		-3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3,
		-3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3,
		-3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3,
	};

	/**
	 * The crop growing at each tree varbit value, or null.
	 */
	static final Sapling[] TREE_CROPS = {
		null, null, null, null, null, null, null, null,
		Sapling.OAK, Sapling.OAK, Sapling.OAK, Sapling.OAK, Sapling.OAK, Sapling.OAK, Sapling.OAK, Sapling.WILLOW,
		Sapling.WILLOW, Sapling.WILLOW, Sapling.WILLOW, Sapling.WILLOW, Sapling.WILLOW, Sapling.WILLOW, Sapling.WILLOW, Sapling.WILLOW,
		Sapling.MAPLE, Sapling.MAPLE, Sapling.MAPLE, Sapling.MAPLE, Sapling.MAPLE, Sapling.MAPLE, Sapling.MAPLE, Sapling.MAPLE,
		Sapling.MAPLE, Sapling.MAPLE, Sapling.MAPLE, Sapling.YEW, Sapling.YEW, Sapling.YEW, Sapling.YEW, Sapling.YEW,
		Sapling.YEW, Sapling.YEW, Sapling.YEW, Sapling.YEW, Sapling.YEW, Sapling.YEW, Sapling.YEW, Sapling.YEW,
		Sapling.MAGIC, Sapling.MAGIC, Sapling.MAGIC, Sapling.MAGIC, Sapling.MAGIC, Sapling.MAGIC, Sapling.MAGIC, Sapling.MAGIC,
		Sapling.MAGIC, Sapling.MAGIC, Sapling.MAGIC, Sapling.MAGIC, Sapling.MAGIC, Sapling.MAGIC, Sapling.MAGIC, null,
		null, null, null, null, null, null, null, null,
		null, Sapling.OAK, Sapling.OAK, Sapling.OAK, null, Sapling.OAK, null, null,
		Sapling.WILLOW, Sapling.WILLOW, Sapling.WILLOW, Sapling.WILLOW, Sapling.WILLOW, null, Sapling.WILLOW, null,
		null, Sapling.MAPLE, Sapling.MAPLE, Sapling.MAPLE, Sapling.MAPLE, Sapling.MAPLE, Sapling.MAPLE, Sapling.MAPLE,
		null, Sapling.MAPLE, null, null, Sapling.YEW, Sapling.YEW, Sapling.YEW, Sapling.YEW,
		Sapling.YEW, Sapling.YEW, Sapling.YEW, Sapling.YEW, Sapling.YEW, null, Sapling.YEW, null,
		null, Sapling.MAGIC, Sapling.MAGIC, Sapling.MAGIC, Sapling.MAGIC, Sapling.MAGIC, Sapling.MAGIC, Sapling.MAGIC,
		Sapling.MAGIC, Sapling.MAGIC, Sapling.MAGIC, Sapling.MAGIC, null, Sapling.MAGIC, null, null,
		null, null, null, null, null, null, null, null,
		null, Sapling.OAK, Sapling.OAK, Sapling.OAK, null, Sapling.OAK, null, null,
		Sapling.WILLOW, Sapling.WILLOW, Sapling.WILLOW, Sapling.WILLOW, Sapling.WILLOW, null, Sapling.WILLOW, null,
		null, Sapling.MAPLE, Sapling.MAPLE, Sapling.MAPLE, Sapling.MAPLE, Sapling.MAPLE, Sapling.MAPLE, Sapling.MAPLE,
		null, Sapling.MAPLE, null, null, Sapling.YEW, Sapling.YEW, Sapling.YEW, Sapling.YEW,
		Sapling.YEW, Sapling.YEW, Sapling.YEW, Sapling.YEW, Sapling.YEW, null, Sapling.YEW, null,
		null, Sapling.MAGIC, Sapling.MAGIC, Sapling.MAGIC, Sapling.MAGIC, Sapling.MAGIC, Sapling.MAGIC, Sapling.MAGIC,
		Sapling.MAGIC, Sapling.MAGIC, Sapling.MAGIC, Sapling.MAGIC, null, Sapling.MAGIC, null, null,
		Sapling.WILLOW, Sapling.WILLOW, Sapling.WILLOW, Sapling.WILLOW, Sapling.WILLOW, Sapling.WILLOW, null, null,
		null, null, null, null, null, null, null, null,
		null, null, null, null, null, null, null, null,
		null, null, null, null, null, null, null, null,
		null, null, null, null, null, null, null, null,
		null, null, null, null, null, null, null, null,
		null, null, null, null, null, null, null, null,
		null, null, null, null, null, null, null, null,
	};

	/**
	 * Growth tick: 160 minutes.
	 */
	static final int[] FRUIT_TREE = {
		-3, -3, -3, -2, -3, -3, -3, -3, 6, 5, 4, 3, 2, 1, -8, -8,
		-8, -8, -8, -8, -8, -5, -5, -5, -5, -5, -5, -6, -6, -6, -6, -6,
		-6, -9, -7, 6, 5, 4, 3, 2, 1, -8, -8, -8, -8, -8, -8, -8,
		-5, -5, -5, -5, -5, -5, -6, -6, -6, -6, -6, -6, -9, -7, -3, -3,
		-3, -3, -3, -3, -3, -3, -3, -3, 6, 5, 4, 3, 2, 1, -8, -8,
		-8, -8, -8, -8, -8, -5, -5, -5, -5, -5, -5, -6, -6, -6, -6, -6,
		-6, -9, -7, 6, 5, 4, 3, 2, 1, -8, -8, -8, -8, -8, -8, -8,
		-5, -5, -5, -5, -5, -5, -6, -6, -6, -6, -6, -6, -9, -7, -3, -3,
		-3, -3, -3, -3, -3, -3, -3, -3, 6, 5, 4, 3, 2, 1, -8, -8,
		-8, -8, -8, -8, -8, -5, -5, -5, -5, -5, -5, -6, -6, -6, -6, -6,
		-6, -9, -7, 6, 5, 4, 3, 2, 1, -8, -8, -8, -8, -8, -8, -8,
		-5, -5, -5, -5, -5, -5, -6, -6, -6, -6, -6, -6, -9, -7, -3, -3,
		-3, -3, -3, -3, -3, -3, -3, -3, 6, 5, 4, 3, 2, 1, -8, -8,
		-8, -8, -8, -8, -8, -5, -5, -5, -5, -5, -5, -6, -6, -6, -6, -6,
		-6, -9, -7, 6, 5, 4, 3, 2, 1, -8, -8, -8, -8, -8, -8, -8,
		-5, -5, -5, -5, -5, -5, -6, -6, -6, -6, -6, -6, -9, -7, -3, -3,
	};

	/**
	 * The crop growing at each fruit tree varbit value, or null.
	 */
	static final Sapling[] FRUIT_TREE_CROPS = {
		null, null, null, null, null, null, null, null,
		Sapling.APPLE, Sapling.APPLE, Sapling.APPLE, Sapling.APPLE, Sapling.APPLE, Sapling.APPLE, Sapling.APPLE, Sapling.APPLE,
		Sapling.APPLE, Sapling.APPLE, Sapling.APPLE, Sapling.APPLE, Sapling.APPLE, Sapling.APPLE, Sapling.APPLE, Sapling.APPLE,
		Sapling.APPLE, Sapling.APPLE, Sapling.APPLE, Sapling.APPLE, Sapling.APPLE, Sapling.APPLE, Sapling.APPLE, Sapling.APPLE,
		Sapling.APPLE, Sapling.APPLE, Sapling.APPLE, Sapling.BANANA, Sapling.BANANA, Sapling.BANANA, Sapling.BANANA, Sapling.BANANA,
		Sapling.BANANA, Sapling.BANANA, Sapling.BANANA, Sapling.BANANA, Sapling.BANANA, Sapling.BANANA, Sapling.BANANA, Sapling.BANANA,
		Sapling.BANANA, Sapling.BANANA, Sapling.BANANA, Sapling.BANANA, Sapling.BANANA, Sapling.BANANA, Sapling.BANANA, Sapling.BANANA,
		Sapling.BANANA, Sapling.BANANA, Sapling.BANANA, Sapling.BANANA, Sapling.BANANA, Sapling.BANANA, null, null,
		null, null, null, null, null, null, null, null,
		Sapling.ORANGE, Sapling.ORANGE, Sapling.ORANGE, Sapling.ORANGE, Sapling.ORANGE, Sapling.ORANGE, Sapling.ORANGE, Sapling.ORANGE,
		Sapling.ORANGE, Sapling.ORANGE, Sapling.ORANGE, Sapling.ORANGE, Sapling.ORANGE, Sapling.ORANGE, Sapling.ORANGE, Sapling.ORANGE,
		Sapling.ORANGE, Sapling.ORANGE, Sapling.ORANGE, Sapling.ORANGE, Sapling.ORANGE, Sapling.ORANGE, Sapling.ORANGE, Sapling.ORANGE,
		Sapling.ORANGE, Sapling.ORANGE, Sapling.ORANGE, Sapling.CURRY, Sapling.CURRY, Sapling.CURRY, Sapling.CURRY, Sapling.CURRY,
		Sapling.CURRY, Sapling.CURRY, Sapling.CURRY, Sapling.CURRY, Sapling.CURRY, Sapling.CURRY, Sapling.CURRY, Sapling.CURRY,
		Sapling.CURRY, Sapling.CURRY, Sapling.CURRY, Sapling.CURRY, Sapling.CURRY, Sapling.CURRY, Sapling.CURRY, Sapling.CURRY,
		Sapling.CURRY, Sapling.CURRY, Sapling.CURRY, Sapling.CURRY, Sapling.CURRY, Sapling.CURRY, null, null,
		null, null, null, null, null, null, null, null,
		Sapling.PINEAPPLE, Sapling.PINEAPPLE, Sapling.PINEAPPLE, Sapling.PINEAPPLE, Sapling.PINEAPPLE, Sapling.PINEAPPLE, Sapling.PINEAPPLE, Sapling.PINEAPPLE,
		Sapling.PINEAPPLE, Sapling.PINEAPPLE, Sapling.PINEAPPLE, Sapling.PINEAPPLE, Sapling.PINEAPPLE, Sapling.PINEAPPLE, Sapling.PINEAPPLE, Sapling.PINEAPPLE,
		Sapling.PINEAPPLE, Sapling.PINEAPPLE, Sapling.PINEAPPLE, Sapling.PINEAPPLE, Sapling.PINEAPPLE, Sapling.PINEAPPLE, Sapling.PINEAPPLE, Sapling.PINEAPPLE,
		Sapling.PINEAPPLE, Sapling.PINEAPPLE, Sapling.PINEAPPLE, Sapling.PAPAYA, Sapling.PAPAYA, Sapling.PAPAYA, Sapling.PAPAYA, Sapling.PAPAYA,
		Sapling.PAPAYA, Sapling.PAPAYA, Sapling.PAPAYA, Sapling.PAPAYA, Sapling.PAPAYA, Sapling.PAPAYA, Sapling.PAPAYA, Sapling.PAPAYA,
		Sapling.PAPAYA, Sapling.PAPAYA, Sapling.PAPAYA, Sapling.PAPAYA, Sapling.PAPAYA, Sapling.PAPAYA, Sapling.PAPAYA, Sapling.PAPAYA,
		Sapling.PAPAYA, Sapling.PAPAYA, Sapling.PAPAYA, Sapling.PAPAYA, Sapling.PAPAYA, Sapling.PAPAYA, null, null,
		null, null, null, null, null, null, null, null,
		Sapling.PALM, Sapling.PALM, Sapling.PALM, Sapling.PALM, Sapling.PALM, Sapling.PALM, Sapling.PALM, Sapling.PALM,
		Sapling.PALM, Sapling.PALM, Sapling.PALM, Sapling.PALM, Sapling.PALM, Sapling.PALM, Sapling.PALM, Sapling.PALM,
		Sapling.PALM, Sapling.PALM, Sapling.PALM, Sapling.PALM, Sapling.PALM, Sapling.PALM, Sapling.PALM, Sapling.PALM,
		Sapling.PALM, Sapling.PALM, Sapling.PALM, Sapling.DRAGONFRUIT, Sapling.DRAGONFRUIT, Sapling.DRAGONFRUIT, Sapling.DRAGONFRUIT, Sapling.DRAGONFRUIT,
		Sapling.DRAGONFRUIT, Sapling.DRAGONFRUIT, Sapling.DRAGONFRUIT, Sapling.DRAGONFRUIT, Sapling.DRAGONFRUIT, Sapling.DRAGONFRUIT, Sapling.DRAGONFRUIT, Sapling.DRAGONFRUIT,
		Sapling.DRAGONFRUIT, Sapling.DRAGONFRUIT, Sapling.DRAGONFRUIT, Sapling.DRAGONFRUIT, Sapling.DRAGONFRUIT, Sapling.DRAGONFRUIT, Sapling.DRAGONFRUIT, Sapling.DRAGONFRUIT,
		Sapling.DRAGONFRUIT, Sapling.DRAGONFRUIT, Sapling.DRAGONFRUIT, Sapling.DRAGONFRUIT, Sapling.DRAGONFRUIT, Sapling.DRAGONFRUIT, null, null,
	};
}
