/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload;

import dev.jstech.computers.os.install.InstallerFlow;
import dev.jstech.computers.os.install.InstallerStyle;
import dev.jstech.core.id.StableNames;
import dev.jstech.core.text.TextBounds;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * Server to client: the installer this machine is in, on the page it has reached.
 *
 * <p>What travels is the installer itself, not a picture of it: the machine sends the system, the disks it is
 * offering, the desktops a Mirror could serve, the answers so far and how much of the work is done, and the
 * screen builds the same installer from them. Both sides then read the pages out of the same place, so what a
 * player sees is what the machine is holding rather than a second telling of it.
 *
 * <p>It is sent when the installer opens, when a monitor is opened on a machine already in one, and whenever the
 * page changes. Not every tick: the screen walks the same clock and stops where the machine stops.
 */
public record OpenInstallerPayload(BlockPos hostPos, BlockPos monitorPos, InstallerStyle style, String systemId,
                                   String systemName, int footprintMb, int copyTicks, List<InstallerFlow.Disk> disks,
                                   List<InstallerFlow.Desktop> desktops, String mirrorHost, int stageIndex,
                                   int targetSlot, String computerName, String desktopId, int eraseSlot,
                                   int ticksDone, boolean portsSelected, boolean useMirror, boolean cronEnabled,
                                   boolean sshdEnabled) implements CustomPacketPayload {

    /** The longest a drive's name or the system on it may be, which is wider than any the game ships. */
    public static final int MAX_LABEL = 64;

    /** The longest an id may be, which is wider than any namespace and path together. */
    public static final int MAX_ID = 128;

    public static final CustomPacketPayload.Type<OpenInstallerPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "open_installer"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenInstallerPayload> STREAM_CODEC =
            StreamCodec.of(OpenInstallerPayload::encode, OpenInstallerPayload::decode);

    /** The installer as it stands on that machine, with the work it has done. */
    public static OpenInstallerPayload of(final BlockPos hostPos, final BlockPos monitorPos,
                                          final InstallerFlow flow, final int ticksDone) {
        return new OpenInstallerPayload(hostPos, monitorPos, flow.style(), flow.systemId(), flow.systemName(),
                flow.footprintMb(), flow.copyTicks(), flow.disks(), flow.desktops(), flow.mirrorHost(),
                flow.stageIndex(), flow.targetSlot(), flow.computerName(), flow.desktopId(), flow.eraseSlot(),
                ticksDone, flow.portsSelected(), flow.useMirror(), flow.cronEnabled(), flow.sshdEnabled());
    }

    /* Copied on the way in, so what the client is handed cannot change under it after it arrives. */
    public OpenInstallerPayload {
        disks = List.copyOf(disks);
        desktops = List.copyOf(desktops);
    }

    @Override
    public CustomPacketPayload.Type<OpenInstallerPayload> type() {
        return TYPE;
    }

    /** The installer this describes, built back on the side that receives it. */
    public InstallerFlow flow() {
        return InstallerFlow.restored(this.style, this.systemId, this.systemName, this.footprintMb, this.copyTicks,
                this.disks, this.desktops, this.mirrorHost, this.stageIndex, this.targetSlot, this.computerName,
                this.desktopId, this.eraseSlot, this.portsSelected, this.useMirror, this.cronEnabled,
                this.sshdEnabled);
    }

    private static void encode(final RegistryFriendlyByteBuf buf, final OpenInstallerPayload p) {
        buf.writeBlockPos(p.hostPos());
        buf.writeBlockPos(p.monitorPos());
        buf.writeUtf(p.style().serializedName(), MAX_LABEL);
        buf.writeUtf(TextBounds.clip(p.systemId(), MAX_ID), MAX_ID);
        buf.writeUtf(TextBounds.clip(p.systemName(), MAX_LABEL), MAX_LABEL);
        buf.writeVarInt(p.footprintMb());
        buf.writeVarInt(p.copyTicks());
        buf.writeVarInt(Math.min(p.disks().size(), InstallerFlow.MOST_DISKS));
        for (int i = 0; i < Math.min(p.disks().size(), InstallerFlow.MOST_DISKS); i++) {
            final InstallerFlow.Disk disk = p.disks().get(i);
            buf.writeVarInt(disk.slot());
            buf.writeUtf(TextBounds.clip(disk.label(), MAX_LABEL), MAX_LABEL);
            buf.writeVarInt(disk.sizeMb());
            buf.writeVarInt(disk.freeMb());
            buf.writeUtf(TextBounds.clip(disk.holds(), MAX_LABEL), MAX_LABEL);
            buf.writeVarInt(disk.speed());
        }
        buf.writeVarInt(Math.min(p.desktops().size(), InstallerFlow.MOST_DESKTOPS));
        for (int i = 0; i < Math.min(p.desktops().size(), InstallerFlow.MOST_DESKTOPS); i++) {
            final InstallerFlow.Desktop desktop = p.desktops().get(i);
            buf.writeUtf(TextBounds.clip(desktop.id(), MAX_ID), MAX_ID);
            buf.writeUtf(TextBounds.clip(desktop.name(), MAX_LABEL), MAX_LABEL);
            buf.writeVarInt(desktop.sizeMb());
            buf.writeVarInt(desktop.ticks());
        }
        buf.writeUtf(TextBounds.clip(p.mirrorHost(), MAX_LABEL), MAX_LABEL);
        buf.writeVarInt(p.stageIndex());
        buf.writeVarInt(p.targetSlot() + 1);
        buf.writeUtf(TextBounds.clip(p.computerName(), InstallerFlow.MOST_NAME_LETTERS),
                InstallerFlow.MOST_NAME_LETTERS);
        buf.writeUtf(TextBounds.clip(p.desktopId(), MAX_ID), MAX_ID);
        buf.writeVarInt(p.eraseSlot() + 1);
        buf.writeVarInt(p.ticksDone());
        buf.writeBoolean(p.portsSelected());
        buf.writeBoolean(p.useMirror());
        buf.writeBoolean(p.cronEnabled());
        buf.writeBoolean(p.sshdEnabled());
    }

    private static OpenInstallerPayload decode(final RegistryFriendlyByteBuf buf) {
        final BlockPos host = buf.readBlockPos();
        final BlockPos monitor = buf.readBlockPos();
        final InstallerStyle style = StableNames.of(InstallerStyle.class).find(buf.readUtf(MAX_LABEL));
        final String systemId = buf.readUtf(MAX_ID);
        final String systemName = buf.readUtf(MAX_LABEL);
        final int footprintMb = buf.readVarInt();
        final int copyTicks = buf.readVarInt();
        final int diskCount = Math.min(buf.readVarInt(), InstallerFlow.MOST_DISKS);
        final List<InstallerFlow.Disk> disks = new ArrayList<>(diskCount);
        for (int i = 0; i < diskCount; i++) {
            disks.add(new InstallerFlow.Disk(buf.readVarInt(), buf.readUtf(MAX_LABEL), buf.readVarInt(),
                    buf.readVarInt(), buf.readUtf(MAX_LABEL), buf.readVarInt()));
        }
        final int desktopCount = Math.min(buf.readVarInt(), InstallerFlow.MOST_DESKTOPS);
        final List<InstallerFlow.Desktop> desktops = new ArrayList<>(desktopCount);
        for (int i = 0; i < desktopCount; i++) {
            desktops.add(new InstallerFlow.Desktop(buf.readUtf(MAX_ID), buf.readUtf(MAX_LABEL), buf.readVarInt(),
                    buf.readVarInt()));
        }
        final String mirrorHost = buf.readUtf(MAX_LABEL);
        final int stageIndex = buf.readVarInt();
        final int targetSlot = buf.readVarInt() - 1;
        final String computerName = buf.readUtf(InstallerFlow.MOST_NAME_LETTERS);
        final String desktopId = buf.readUtf(MAX_ID);
        final int eraseSlot = buf.readVarInt() - 1;
        final int ticksDone = buf.readVarInt();
        final boolean portsSelected = buf.readBoolean();
        final boolean useMirror = buf.readBoolean();
        final boolean cronEnabled = buf.readBoolean();
        final boolean sshdEnabled = buf.readBoolean();
        return new OpenInstallerPayload(host, monitor, style == null ? InstallerStyle.PLAIN : style, systemId,
                systemName, footprintMb, copyTicks, disks, desktops, mirrorHost, stageIndex, targetSlot,
                computerName, desktopId, eraseSlot, ticksDone, portsSelected, useMirror, cronEnabled, sshdEnabled);
    }
}
