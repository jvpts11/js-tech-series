/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client to server: one action taken in the firmware boot manager.
 *
 * @param hostPos    the computer
 * @param monitorPos the monitor the firmware was opened from (the booted OS opens on it)
 * @param action     one of the {@code ACTION_*} constants
 * @param ref        the entry reference: a disk slot index (boot / boot order) or a media reader's packed
 *                   position (install / boot media); {@code -1} = any linked installer medium
 * @param target     the disk slot to install onto, or {@code -1} for the firmware's default target
 * @param osId       the system this action is about, for the actions that are about one; empty otherwise
 */
public record FirmwareActionPayload(BlockPos hostPos, BlockPos monitorPos, int action, long ref, int target,
                                    String osId) implements CustomPacketPayload {

    /** The longest a system's id may be on the wire; longer than any the registry holds. */
    public static final int MAX_ID = 64;

    /** An action that is not about a particular system. */
    public static FirmwareActionPayload of(final BlockPos hostPos, final BlockPos monitorPos, final int action,
                                           final long ref, final int target) {
        return new FirmwareActionPayload(hostPos, monitorPos, action, ref, target, "");
    }

    /** Boot the OS on disk slot {@code ref} (also makes it the preferred boot disk). */
    public static final int ACTION_BOOT_DISK = 0;
    /** Install the OS from the medium in reader {@code ref} (or any, when -1) onto disk {@code target}. */
    public static final int ACTION_INSTALL = 1;
    /** Make disk slot {@code ref} the preferred boot disk without booting. */
    public static final int ACTION_SET_BOOT = 2;
    /** Boot the medium in reader {@code ref}: a guided installer installs and boots; a live medium opens its shell. */
    public static final int ACTION_BOOT_MEDIA = 3;
    /** Format disk slot {@code ref}: erase its system, files and storage (the client asks twice first). */
    public static final int ACTION_FORMAT = 4;
    /**
     * Set the storage controller's array mode to the {@code ref}-th mode. Configuring an array from
     * the firmware (not from inside a running system) is how a real controller works, and it means
     * a machine with no OS can still have its storage set up.
     */
    public static final int ACTION_RAID_MODE = 5;
    /**
     * Boot the system {@code osId} on disk slot {@code ref} for this boot only.
     *
     * <p>Unlike {@link #ACTION_BOOT_DISK} the order saved in setup is left alone and the machine does not test
     * itself again: it has already done that, and this is the choice of where to go next.
     *
     * <p>The choice travels as the disk and the system it names, rather than as a place in a list. Two menus
     * offer this (the self-test's one-time menu and the boot manager the systems bring with them), they list
     * different things in different orders, and a position meant a different thing in each: one of them was
     * sending a disk slot where the other was sending a row number, and the machine read both the same way.
     */
    public static final int ACTION_BOOT_ONCE = 6;
    /** A key was pressed at the boot manager: stop counting and wait there for a choice. */
    public static final int ACTION_HOLD_BOOT_MENU = 7;
    /** Open the firmware setup from the boot manager, which is one of its entries on the modern machines. */
    public static final int ACTION_OPEN_SETUP = 8;
    /** Start the machine over from the boot manager, for a manager that lists that among what it offers. */
    public static final int ACTION_RESTART_FROM_MENU = 9;

    public static final CustomPacketPayload.Type<FirmwareActionPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "firmware_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, FirmwareActionPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, FirmwareActionPayload::hostPos,
                    BlockPos.STREAM_CODEC, FirmwareActionPayload::monitorPos,
                    ByteBufCodecs.VAR_INT, FirmwareActionPayload::action,
                    ByteBufCodecs.VAR_LONG, FirmwareActionPayload::ref,
                    ByteBufCodecs.VAR_INT, FirmwareActionPayload::target,
                    ByteBufCodecs.stringUtf8(MAX_ID), FirmwareActionPayload::osId,
                    FirmwareActionPayload::new);

    @Override
    public CustomPacketPayload.Type<FirmwareActionPayload> type() {
        return TYPE;
    }
}
