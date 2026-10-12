/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.blockentity;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/**
 * Why a part stays out of a machine's slot, said to the player holding it over the slot: a slot that only refuses
 * leaves a new player guessing which of its rules the part broke.
 */
@TextHolder
public final class PartRefusals {

    public static final TextKey NOT_THIS_SLOT = TextKey.of("jsc.part_refusal.not_this_slot",
            "That part does not go in this slot");
    public static final TextKey NO_BOARD = TextKey.of("jsc.part_refusal.no_board",
            "Seat a board first: its sockets take the parts");
    public static final TextKey NOT_ON_THE_BOARD = TextKey.of("jsc.part_refusal.not_on_the_board",
            "The board has no socket here: a board with more slots opens it");
    public static final TextKey BOARD = TextKey.of("jsc.part_refusal.board",
            "This case takes only a board of its own size and era");
    public static final TextKey CPU = TextKey.of("jsc.part_refusal.cpu",
            "This processor needs a board of its socket and its era");
    public static final TextKey RAM = TextKey.of("jsc.part_refusal.ram",
            "The board takes no memory of this generation and era");
    public static final TextKey CARD_BUS = TextKey.of("jsc.part_refusal.card_bus",
            "This card's bus does not fit the board's slots");
    public static final TextKey SOUND_ERA = TextKey.of("jsc.part_refusal.sound_era",
            "A sound card goes only on a board of its own era");
    public static final TextKey ONE_SOUND_CARD = TextKey.of("jsc.part_refusal.one_sound_card",
            "The machine already has a sound card");
    public static final TextKey WORKSHOP_CARD = TextKey.of("jsc.part_refusal.workshop_card",
            "Only a Personal Computer runs the personal-use cards");
    public static final TextKey PHI = TextKey.of("jsc.part_refusal.phi",
            "Only a supercomputer node seats a Phi co-processor");
    public static final TextKey ONE_PHI = TextKey.of("jsc.part_refusal.one_phi",
            "A node seats one Phi co-processor");
    public static final TextKey ONE_CLUSTER_CARD = TextKey.of("jsc.part_refusal.one_cluster_card",
            "The machine already has a cluster interface card");
    public static final TextKey SERVER_CARD = TextKey.of("jsc.part_refusal.server_card",
            "A server's slots take a graphics card, a storage card or a network card");
    public static final TextKey DISK_TOO_NEW = TextKey.of("jsc.part_refusal.disk_too_new",
            "This disk is of a later era than the machine");
    public static final TextKey NO_BAY = TextKey.of("jsc.part_refusal.no_bay",
            "The case has no bay here");
    public static final TextKey DRIVE_BAY = TextKey.of("jsc.part_refusal.drive_bay",
            "A drive bay takes only a disk");

    private PartRefusals() {
    }
}
