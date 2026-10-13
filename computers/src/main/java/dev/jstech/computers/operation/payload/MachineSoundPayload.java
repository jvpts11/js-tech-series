/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload;

import dev.jstech.computers.audio.IMachineCue;
import dev.jstech.computers.audio.ProgramCue;
import dev.jstech.computers.audio.SystemSound;
import dev.jstech.core.id.IStableName;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Something happened on a desktop that only the player's screen knows of: an error box, a notice, a click that went
 * nowhere, a move in the minefield. The desktop tells the machine, and the machine plays the sound out of its
 * monitors for everyone near it to hear.
 *
 * <p>Only the sounds a screen raises travel this way; the machine alone raises the rest (coming up, going down, a
 * device plugged in), so a screen cannot play them at will.
 *
 * @param hostPos the machine whose desktop raised it
 * @param sound   the sound's stable name
 */
public record MachineSoundPayload(BlockPos hostPos, String sound) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<MachineSoundPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "machine_sound"));

    /** The longest stable name one of the sounds a screen raises has, with room to spare. */
    public static final int MAX_NAME = 32;

    public static final StreamCodec<RegistryFriendlyByteBuf, MachineSoundPayload> STREAM_CODEC =
            StreamCodec.composite(BlockPos.STREAM_CODEC, MachineSoundPayload::hostPos,
                    ByteBufCodecs.stringUtf8(MAX_NAME), MachineSoundPayload::sound,
                    MachineSoundPayload::new);

    /** The sounds a screen may raise, by their stable names. */
    private static final Map<String, IMachineCue> RAISED_BY_SCREENS = raisedByScreens();

    /** The sound a desktop raises on that machine. */
    public MachineSoundPayload(final BlockPos hostPos, final SystemSound sound) {
        this(hostPos, sound.serializedName());
    }

    /** The sound a program of the series raises on that machine. */
    public MachineSoundPayload(final BlockPos hostPos, final ProgramCue sound) {
        this(hostPos, sound.serializedName());
    }

    /** The sound this asks for, or null when it is not one a screen may raise. */
    @Nullable
    public IMachineCue cue() {
        return RAISED_BY_SCREENS.get(sound);
    }

    @Override
    public CustomPacketPayload.Type<MachineSoundPayload> type() {
        return TYPE;
    }

    private static Map<String, IMachineCue> raisedByScreens() {
        final Map<String, IMachineCue> out = new HashMap<>();
        for (final SystemSound one : new SystemSound[]{SystemSound.ERROR, SystemSound.NOTIFY, SystemSound.BEEP}) {
            put(out, one);
        }
        for (final ProgramCue one : ProgramCue.values()) {
            put(out, one);
        }
        return Map.copyOf(out);
    }

    private static <T extends IMachineCue & IStableName> void put(final Map<String, IMachineCue> out, final T one) {
        out.put(one.serializedName(), one);
    }
}
