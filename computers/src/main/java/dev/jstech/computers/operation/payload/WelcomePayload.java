/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload;

import dev.jstech.computers.os.boot.WelcomeFacts;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * Server to client: what this machine is, for the welcome its system puts up the first time it comes up.
 *
 * <p>Read off the machine when it is asked for, never written in advance: the name it answers to, the processor
 * in it, the memory counted over its modules, the disk its system is on, the other systems it could boot and the
 * network it is cabled to. The tips travel with it because two of them are only true on some machines.
 *
 * @param systemName    the system that booted, by the name a person reads
 * @param systemSlot    the disk that system is on
 * @param systemDisk    that drive, as it is written on it
 * @param others        the other systems on this machine's other disks
 * @param networkHost   the network this machine is on, by the Mainframe's name; empty when it is on none
 * @param mirrorHost    the Mirror answering it, empty when none does
 * @param showAtStartup whether the welcome is still wanted on later starts
 */
public record WelcomePayload(BlockPos hostPos, String machineName, String cpuName, int memoryMb, String systemName,
                             int systemSlot, String systemDisk, List<WelcomeFacts.Other> others, String networkHost,
                             String mirrorHost, boolean showAtStartup,
                             List<String> tips) implements CustomPacketPayload {

    /** The longest a name travels, wider than any the game gives a machine, a processor or a drive. */
    public static final int MAX_NAME = 64;

    /** The longest a tip travels, which is a sentence and a half. */
    public static final int MAX_TIP = 180;

    /** More systems than any machine has disks. */
    public static final int MOST_OTHERS = 16;

    public static final CustomPacketPayload.Type<WelcomePayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "welcome"));

    public static final StreamCodec<RegistryFriendlyByteBuf, WelcomePayload> STREAM_CODEC =
            StreamCodec.of(WelcomePayload::encode, WelcomePayload::decode);

    @Override
    public CustomPacketPayload.Type<WelcomePayload> type() {
        return TYPE;
    }

    /** Whether this machine is on a network at all, which decides one of the things a welcome offers to do. */
    public boolean networked() {
        return !this.networkHost.isEmpty();
    }

    /** Whether a Mirror answers, which decides where the welcome says software comes from. */
    public boolean mirrorAnswers() {
        return !this.mirrorHost.isEmpty();
    }

    private static void encode(final RegistryFriendlyByteBuf buf, final WelcomePayload p) {
        buf.writeBlockPos(p.hostPos());
        buf.writeUtf(cut(p.machineName(), MAX_NAME), MAX_NAME);
        buf.writeUtf(cut(p.cpuName(), MAX_NAME), MAX_NAME);
        buf.writeVarInt(p.memoryMb());
        buf.writeUtf(cut(p.systemName(), MAX_NAME), MAX_NAME);
        buf.writeVarInt(p.systemSlot() + 1);
        buf.writeUtf(cut(p.systemDisk(), MAX_NAME), MAX_NAME);
        final int others = Math.min(p.others().size(), MOST_OTHERS);
        buf.writeVarInt(others);
        for (int i = 0; i < others; i++) {
            buf.writeUtf(cut(p.others().get(i).system(), MAX_NAME), MAX_NAME);
            buf.writeVarInt(p.others().get(i).slot());
        }
        buf.writeUtf(cut(p.networkHost(), MAX_NAME), MAX_NAME);
        buf.writeUtf(cut(p.mirrorHost(), MAX_NAME), MAX_NAME);
        buf.writeBoolean(p.showAtStartup());
        final int tips = Math.min(p.tips().size(), WelcomeFacts.MOST_TIPS);
        buf.writeVarInt(tips);
        for (int i = 0; i < tips; i++) {
            buf.writeUtf(cut(p.tips().get(i), MAX_TIP), MAX_TIP);
        }
    }

    private static WelcomePayload decode(final RegistryFriendlyByteBuf buf) {
        final BlockPos host = buf.readBlockPos();
        final String machineName = buf.readUtf(MAX_NAME);
        final String cpuName = buf.readUtf(MAX_NAME);
        final int memoryMb = buf.readVarInt();
        final String systemName = buf.readUtf(MAX_NAME);
        final int systemSlot = buf.readVarInt() - 1;
        final String systemDisk = buf.readUtf(MAX_NAME);
        final int otherCount = Math.min(buf.readVarInt(), MOST_OTHERS);
        final List<WelcomeFacts.Other> others = new ArrayList<>(otherCount);
        for (int i = 0; i < otherCount; i++) {
            others.add(new WelcomeFacts.Other(buf.readUtf(MAX_NAME), buf.readVarInt()));
        }
        final String networkHost = buf.readUtf(MAX_NAME);
        final String mirrorHost = buf.readUtf(MAX_NAME);
        final boolean showAtStartup = buf.readBoolean();
        final int tipCount = Math.min(buf.readVarInt(), WelcomeFacts.MOST_TIPS);
        final List<String> tips = new ArrayList<>(tipCount);
        for (int i = 0; i < tipCount; i++) {
            tips.add(buf.readUtf(MAX_TIP));
        }
        return new WelcomePayload(host, machineName, cpuName, memoryMb, systemName, systemSlot, systemDisk,
                others, networkHost, mirrorHost, showAtStartup, tips);
    }

    /** A string trimmed to what the wire takes, since writing one that is too long fails outright. */
    private static String cut(final String text, final int most) {
        if (text == null) {
            return "";
        }
        return text.length() <= most ? text : text.substring(0, most);
    }
}
