/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload;

import dev.jstech.computers.os.install.InstallerFlow;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client to server: an answer given to the installer, or a page turned in it.
 *
 * <p>The screen never decides anything by itself. It says what the player did and the machine works out what
 * that means, which is why closing the monitor halfway through changes nothing: the answers were the machine's
 * as soon as they were given.
 *
 * @param action what the player did, one of the constants here
 * @param value  the disk, the desktop or the page the action is about, where it is about one
 * @param text   the name typed, where the action is about one
 */
public record InstallerActionPayload(BlockPos hostPos, BlockPos monitorPos, int action, int value,
                                     String text) implements CustomPacketPayload {

    /** On to the next page, once the one it is on has what it needs. */
    public static final int ACTION_NEXT = 1;

    /** Back a page, which is only possible while nothing has been written. */
    public static final int ACTION_BACK = 2;

    /** The disk the system goes on. */
    public static final int ACTION_SELECT_DISK = 3;

    /** The name the computer takes. */
    public static final int ACTION_NAME = 4;

    /** The desktop that comes with the system, or none for the terminal alone. */
    public static final int ACTION_DESKTOP = 5;

    /** Erase a disk, already confirmed on the screen; it happens when the work starts, not before. */
    public static final int ACTION_ERASE = 6;

    /** Straight to a page, which is how the installer that lists its questions reaches one. */
    public static final int ACTION_GO_TO = 7;

    /** Leave the installer with nothing written, which only the pages before the work allow. */
    public static final int ACTION_QUIT = 8;

    /** Restart into what was just installed. */
    public static final int ACTION_REBOOT = 9;

    public static final CustomPacketPayload.Type<InstallerActionPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "installer_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, InstallerActionPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, InstallerActionPayload::hostPos,
                    BlockPos.STREAM_CODEC, InstallerActionPayload::monitorPos,
                    ByteBufCodecs.VAR_INT, InstallerActionPayload::action,
                    ByteBufCodecs.VAR_INT, InstallerActionPayload::value,
                    ByteBufCodecs.stringUtf8(InstallerFlow.MOST_NAME_LETTERS), InstallerActionPayload::text,
                    InstallerActionPayload::new);

    /** An action about nothing in particular: a page turned, or the installer left. */
    public static InstallerActionPayload of(final BlockPos hostPos, final BlockPos monitorPos, final int action) {
        return new InstallerActionPayload(hostPos, monitorPos, action, 0, "");
    }

    /** An action about a disk, a desktop or a page. */
    public static InstallerActionPayload of(final BlockPos hostPos, final BlockPos monitorPos, final int action,
                                            final int value) {
        return new InstallerActionPayload(hostPos, monitorPos, action, value, "");
    }

    /** The name typed for the computer, trimmed to what a name may be before it is ever sent. */
    public static InstallerActionPayload named(final BlockPos hostPos, final BlockPos monitorPos,
                                               final String name) {
        final String trimmed = name == null ? "" : name.strip();
        return new InstallerActionPayload(hostPos, monitorPos, ACTION_NAME, 0,
                trimmed.length() <= InstallerFlow.MOST_NAME_LETTERS ? trimmed
                        : trimmed.substring(0, InstallerFlow.MOST_NAME_LETTERS));
    }

    @Override
    public CustomPacketPayload.Type<InstallerActionPayload> type() {
        return TYPE;
    }
}
