/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.menu;

import dev.jstech.core.tier.HardwareEra;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;

/**
 * What the screen of a named device that hangs off a computer opens with, and how it travels: the one wire format the
 * Redstone Interface and the speaker share. The longest name a device holds differs per device, so it is passed in.
 *
 * @param pos          where the device stands
 * @param name         the name a player gave it, empty for none
 * @param computerName the name a player gave its computer, empty for none
 * @param computerKind the translation key of its computer's block, empty when it is not linked to one
 * @param era          the era of its housing, whose skin the screen wears
 */
public record NamedDeviceOpening(BlockPos pos, String name, String computerName, String computerKind,
                                 HardwareEra era) {

    public void write(final RegistryFriendlyByteBuf buf, final int maxName) {
        buf.writeBlockPos(pos);
        buf.writeUtf(name, maxName);
        buf.writeUtf(computerName, maxName);
        buf.writeUtf(computerKind);
        buf.writeVarInt(era.id());
    }

    public static NamedDeviceOpening read(final RegistryFriendlyByteBuf buf, final int maxName) {
        final BlockPos pos = buf.readBlockPos();
        final String name = buf.readUtf(maxName);
        final String computerName = buf.readUtf(maxName);
        final String computerKind = buf.readUtf();
        final HardwareEra era = HardwareEra.find(buf.readVarInt());
        return new NamedDeviceOpening(pos, name, computerName, computerKind, era == null ? HardwareEra.STANDARD : era);
    }
}
