/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.audio;

import java.util.List;

/**
 * What the director keeps at one moment, as the game's debug screen shows it.
 *
 * @param shorts      the running sounds loaded whole
 * @param shortBudget how many of those it keeps to
 * @param longs       the running sounds read as they play
 * @param longBudget  how many of those it keeps to
 * @param rooms       the rooms many machines make, one sound each
 * @param walls       for each muffled sound, how many walls are in its way, fewest first
 */
public record AudioStats(int shorts, int shortBudget, int longs, int longBudget, List<Room> rooms,
                         List<Integer> walls) {

    public AudioStats {
        rooms = List.copyOf(rooms);
        walls = List.copyOf(walls);
    }

    /**
     * One room: the field it belongs to, how many machines it stands for, and how loud it plays.
     *
     * @param field   the field's id
     * @param members how many machines it stands for
     * @param volume  how loud it plays, from 0 to 1
     */
    public record Room(String field, int members, double volume) {
    }
}
