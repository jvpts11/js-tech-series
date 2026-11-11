/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.blockentity;

import dev.jstech.computers.hardware.DiskSpec;
import dev.jstech.computers.registry.HardwareRegistry;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

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
    public static final TextKey FOUR_WAN_CARDS = TextKey.of("jsc.part_refusal.four_wan_cards",
            "The gateway has four ports: a WAN Interface Card for each");
    public static final TextKey SERVER_CARD = TextKey.of("jsc.part_refusal.server_card",
            "A server's slots take a graphics card or a storage card");
    public static final TextKey DISK_TOO_NEW = TextKey.of("jsc.part_refusal.disk_too_new",
            "This disk is of a later era than the machine");
    public static final TextKey NO_BAY = TextKey.of("jsc.part_refusal.no_bay",
            "The case has no bay here");
    public static final TextKey DRIVE_BAY = TextKey.of("jsc.part_refusal.drive_bay",
            "A drive bay takes only a disk");
    public static final TextKey TAPE_SLOT = TextKey.of("jsc.part_refusal.tape_slot",
            "A Tape Library's slot takes only a tape");
    public static final TextKey TAPE_OF_ITS_ERA = TextKey.of("jsc.part_refusal.tape_of_its_era",
            "A Tape Library reads only the tapes of its own era");
    public static final TextKey COOLER_TOO_NEW = TextKey.of("jsc.part_refusal.cooler_too_new",
            "This cooler is of a later era than the case: its mount has nothing to hold it");
    public static final TextKey NO_RADIATOR_ROOM = TextKey.of("jsc.part_refusal.no_radiator_room",
            "Its radiator needs two free fan seats, on the top or at the front");
    public static final TextKey OWN_HEAT_SINK = TextKey.of("jsc.part_refusal.own_heat_sink",
            "The processor here carries its own heat sink: no cooler goes over it");
    public static final TextKey COOLER_IN_ITS_PLACE = TextKey.of("jsc.part_refusal.cooler_in_its_place",
            "This processor carries its own heat sink, and a cooler sits where it would go");
    public static final TextKey FAN_TOO_NEW = TextKey.of("jsc.part_refusal.fan_too_new",
            "This fan is of a later era than the case");
    public static final TextKey NO_FAN_SEAT = TextKey.of("jsc.part_refusal.no_fan_seat",
            "The case has no fan seat here");
    public static final TextKey UNDER_RADIATOR = TextKey.of("jsc.part_refusal.under_radiator",
            "A water cooler's radiator sits in this seat");
    /* A disk newer than the machine: the disk's name, then that it is too new, then the era it asks for. */
    public static final TextKey TOO_NEW_FOR_COMPUTER = TextKey.of("jsc.part_refusal.too_new_for_computer",
            "Too new for this computer:");
    public static final TextKey DISK_NEEDS_COMPUTER = TextKey.of("jsc.part_refusal.disk_needs_computer",
            "a %s disk needs a %s computer or a newer one");
    public static final TextKey TOO_NEW_FOR_SERVER = TextKey.of("jsc.part_refusal.too_new_for_server",
            "Too new for this server:");
    public static final TextKey DISK_NEEDS_SERVER = TextKey.of("jsc.part_refusal.disk_needs_server",
            "a %s disk needs a %s server or a newer one");
    public static final TextKey TOO_NEW_FOR_DOCK = TextKey.of("jsc.part_refusal.too_new_for_dock",
            "Too new for this Dock Station:");
    public static final TextKey DOCK_TAKES = TextKey.of("jsc.part_refusal.dock_takes",
            "a %s dock takes disks of its era or older");

    private PartRefusals() {
    }

    /**
     * What is said over a slot that keeps {@code stack} out for {@code why}: a disk newer than the machine names
     * itself, says it is too new and the era it asks for, of a computer or of a {@code server}; any other refusal is
     * its one sentence.
     */
    public static List<Component> lines(final TextKey why, final ItemStack stack, final boolean server) {
        if (why == DISK_TOO_NEW && HardwareRegistry.disk(stack) instanceof DiskSpec disk) {
            return tooNew(stack, server ? TOO_NEW_FOR_SERVER : TOO_NEW_FOR_COMPUTER,
                    (server ? DISK_NEEDS_SERVER : DISK_NEEDS_COMPUTER).with(disk.era().text(), disk.era().text()));
        }
        return List.of(GameText.component(why));
    }

    /** What a Dock Station of {@code dockEra} says of {@code disk}, a disk newer than itself held over a tray. */
    public static List<Component> dockTooNew(final ItemStack disk, final Era dockEra) {
        return tooNew(disk, TOO_NEW_FOR_DOCK, DOCK_TAKES.with(dockEra.text()));
    }

    private static List<Component> tooNew(final ItemStack disk, final TextKey title, final Text reason) {
        return List.of(disk.getHoverName().copy().withStyle(ChatFormatting.WHITE),
                GameText.component(title).withStyle(ChatFormatting.RED),
                GameText.component(reason).withStyle(ChatFormatting.GRAY));
    }
}
