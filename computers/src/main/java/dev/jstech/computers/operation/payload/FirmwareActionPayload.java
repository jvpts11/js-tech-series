/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload;

import dev.jstech.computers.os.media.MediaVolume;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
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
 *                   position (install / boot media), the computer's own for a drive on its front; {@code -1} = any
 *                   installer medium the machine has
 * @param target     the disk slot to install onto, or {@code -1} for the firmware's default target
 * @param osId       the system this action is about, for the actions that are about one; empty otherwise
 * @param bay        for an action on a medium, which of the reader's volumes it is on, as {@link MediaVolume#bay()}
 *                   counts them; {@link MediaVolume#MEDIUM} otherwise
 */
public record FirmwareActionPayload(BlockPos hostPos, BlockPos monitorPos, int action, long ref, int target,
                                    String osId, int bay) implements CustomPacketPayload {

    /** The longest a system's id may be on the wire; longer than any the registry holds. */
    public static final int MAX_ID = 64;

    /** An action about a particular system, on a disk or on a reader's own medium. */
    public FirmwareActionPayload(final BlockPos hostPos, final BlockPos monitorPos, final int action, final long ref,
                                 final int target, final String osId) {
        this(hostPos, monitorPos, action, ref, target, osId, MediaVolume.MEDIUM);
    }

    /** An action that is not about a particular system. */
    public static FirmwareActionPayload of(final BlockPos hostPos, final BlockPos monitorPos, final int action,
                                           final long ref, final int target) {
        return new FirmwareActionPayload(hostPos, monitorPos, action, ref, target, "");
    }

    /** An action on the medium of a boot entry, which names its reader and the volume of the reader it is on. */
    public static FirmwareActionPayload onMedium(final BlockPos hostPos, final BlockPos monitorPos, final int action,
                                                 final FirmwareStatePayload.Entry entry, final int target) {
        return new FirmwareActionPayload(hostPos, monitorPos, action, entry.ref(), target, "", entry.bay());
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

    // Written by hand: composite() tops out at six pairs, and an action carries seven fields.
    public static final StreamCodec<RegistryFriendlyByteBuf, FirmwareActionPayload> STREAM_CODEC =
            StreamCodec.of((buf, p) -> {
                buf.writeBlockPos(p.hostPos());
                buf.writeBlockPos(p.monitorPos());
                buf.writeVarInt(p.action());
                buf.writeVarLong(p.ref());
                buf.writeVarInt(p.target());
                buf.writeUtf(p.osId(), MAX_ID);
                buf.writeInt(p.bay());
            }, buf -> new FirmwareActionPayload(buf.readBlockPos(), buf.readBlockPos(), buf.readVarInt(),
                    buf.readVarLong(), buf.readVarInt(), buf.readUtf(MAX_ID), buf.readInt()));

    @Override
    public CustomPacketPayload.Type<FirmwareActionPayload> type() {
        return TYPE;
    }
}
