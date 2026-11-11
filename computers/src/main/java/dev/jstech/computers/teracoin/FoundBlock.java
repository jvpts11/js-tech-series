/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.teracoin;

/**
 * A block of the chain: its height, when it was found, what it paid, the difficulty it was found at, who found it
 * and for which pool, and whether it brought a rare item. Pure.
 *
 * @param height     its number on the chain, from 1
 * @param tick       when it was found, in game ticks
 * @param dayTime    the world's time of day then
 * @param reward     what it paid, in hundred-millionths of a TRC
 * @param difficulty the difficulty it was found at
 * @param machine    the machine whose work found it, as the world knows it
 * @param finder     that machine's name
 * @param address    the address its finder paid
 * @param pool       the pool it was found for, or empty for a miner alone
 * @param rare       whether it brought a rare item
 */
public record FoundBlock(int height, long tick, long dayTime, long reward, long difficulty, String machine,
                         String finder, String address, String pool, boolean rare) {
}
