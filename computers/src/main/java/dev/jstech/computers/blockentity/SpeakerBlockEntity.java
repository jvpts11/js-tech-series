/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.blockentity;

import com.mojang.serialization.Codec;
import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.PeripheralLinks;
import dev.jstech.computers.block.SpeakerBlock;
import dev.jstech.computers.menu.SpeakerMenu;
import dev.jstech.core.audio.FrequencyResponse;
import dev.jstech.core.audio.StereoSide;
import dev.jstech.core.blockentity.BoolField;
import dev.jstech.core.blockentity.DerivedInt;
import dev.jstech.core.blockentity.SyncedBlockEntity;
import dev.jstech.core.blockentity.ValueField;
import dev.jstech.core.peripheral.IPeripheralEndpoint;
import dev.jstech.core.peripheral.IPeripheralOwner;
import dev.jstech.core.peripheral.PeripheralCableType;
import dev.jstech.core.peripheral.PeripheralLink;
import dev.jstech.core.peripheral.PortKind;
import dev.jstech.core.tier.HardwareEra;
import dev.jstech.core.util.Loaded;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Optional;

/**
 * A speaker's own state: the computer it is linked to over the peripheral cable, and the name a player gave it, by
 * which a program finds it among its computer's speakers. A name is unique among one computer's speakers: typed on its
 * screen, it is taken when the screen closes, and one another speaker already has is refused, the speaker keeping the
 * name it had.
 */
public class SpeakerBlockEntity extends SyncedBlockEntity implements IPeripheralEndpoint {

    private final PeripheralLink link = new PeripheralLink(fields(), PeripheralCableType.COMPUTING,
            PeripheralLinks.COMPUTING);
    /** The name a player gave it; empty until one is given, when it is called by the word for a speaker. */
    private final ValueField<String> name = fields().value("SpeakerName", Codec.STRING, "").save();
    /** Whether the last name asked for is one another speaker of its computer already has; its screen shows it. */
    private final BoolField clash = fields().flag("NameClash", false).toMenu();
    /** Which side it plays for its computer, as its screen shows it. */
    private final DerivedInt channel = fields().derived("Channel", () -> workOutChannel()).toMenu();
    /** Whether its computer has a subwoofer against a Transition satellite, as its screen shows it. */
    private final DerivedInt subwoofer = fields().derived("Subwoofer", () -> workOutSubwoofer()).toMenu();
    /** The name being typed on its screen, taken when the screen closes; null while nothing is being typed. */
    @Nullable
    private String asked;

    /** The longest name a speaker takes, the same as a computer's. */
    public static final int MAX_NAME = 32;
    /** Not linked to a computer, so it plays nothing. */
    public static final int CHANNEL_NONE = 0;
    /** The computer's only speaker, which plays both sides. */
    public static final int CHANNEL_ALONE = 1;
    /** One of several, standing where it plays both sides. */
    public static final int CHANNEL_BOTH = 2;
    public static final int CHANNEL_LEFT = 3;
    public static final int CHANNEL_RIGHT = 4;
    /** Linked, but its computer's system plays only out of the monitor. */
    public static final int CHANNEL_OFF = 5;

    public SpeakerBlockEntity(final BlockPos pos, final BlockState state) {
        super(ComputingModule.SPEAKER_BE.get(), pos, state);
    }

    public static void serverTick(final Level level, final BlockPos pos, final BlockState state,
                                  final SpeakerBlockEntity speaker) {
        if (level instanceof ServerLevel server) {
            speaker.link.tick(server, pos);
        }
    }

    @Override
    public PeripheralCableType cableType() {
        return link.cableType();
    }

    /** A speaker takes half of an audio output, which drives a pair. */
    @Override
    public PortKind portKind() {
        return PortKind.AUDIO;
    }

    @Override
    public Optional<Long> linkedOwner() {
        return link.linkedOwner();
    }

    @Override
    public void onOwnerLinked(final long ownerPos) {
        link.linked(ownerPos);
    }

    @Override
    public void onOwnerUnlinked() {
        link.unlinked();
        clash.set(false);
    }

    /** The computer it is linked to, or null. */
    @Nullable
    public BlockPos ownerPos() {
        return link.ownerPos();
    }

    /** The name a player gave it; empty while it has none. */
    public String name() {
        return name.get();
    }

    /** The era of its model, which decides how well it plays. */
    public HardwareEra era() {
        return getBlockState().getBlock() instanceof SpeakerBlock speaker ? speaker.era() : HardwareEra.STANDARD;
    }

    /** What it reproduces of a recording, given whether its computer has a subwoofer against a Transition satellite. */
    public FrequencyResponse response(final boolean subwoofer) {
        return getBlockState().getBlock() instanceof SpeakerBlock speaker ? speaker.response(subwoofer)
                : FrequencyResponse.FULL;
    }

    /** Whether the name last asked for is one another speaker of its computer already has. */
    public boolean nameClashes() {
        return clash.get();
    }

    /** Which side it plays for its computer: one of the {@code CHANNEL_} values. */
    public int channel() {
        return channel.getAsInt();
    }

    /** Whether its computer has a subwoofer against a Transition satellite, which gives the satellites the bass. */
    public boolean subwoofer() {
        return subwoofer.isSet();
    }

    /**
     * The name being typed on its screen, looked at as it is typed: its screen says at once whether another speaker
     * of its computer is already called that, whatever the case of its letters. Nothing is taken until the screen
     * closes, so the names a player passes through on the way to one are never given.
     */
    public void ask(final ServerLevel level, final String requested) {
        final String stripped = requested.strip();
        asked = stripped.length() > MAX_NAME ? stripped.substring(0, MAX_NAME) : stripped;
        clash.set(!asked.isEmpty() && anotherSpeakerIsCalled(level, asked));
    }

    /**
     * Takes the name asked for when its screen closes, unless it clashes; then the speaker keeps the name it had. An
     * empty name gives it back its default and never clashes.
     */
    public void takeAskedName() {
        if (asked != null && !clash.get()) {
            name.set(asked);
        }
        asked = null;
        clash.set(false);
    }

    /** What its screen opens with: its name, and the computer it plays for. */
    public SpeakerMenu.Opening opening(final ServerLevel level) {
        String computerName = "";
        String computerKind = "";
        final BlockPos owner = link.ownerPos();
        if (owner != null && Loaded.blockEntity(level, owner) instanceof AbstractComputerBlockEntity computer) {
            computerName = computer.customName();
            computerKind = computer.getBlockState().getBlock().getDescriptionId();
        }
        return new SpeakerMenu.Opening(worldPosition, name.get(), computerName, computerKind, era());
    }

    /* Which side it plays for its computer, worked out on the server. */
    private int workOutChannel() {
        final BlockPos owner = link.ownerPos();
        if (owner == null || !(level instanceof ServerLevel server)) {
            return CHANNEL_NONE;
        }
        if (!(Loaded.blockEntity(server, owner) instanceof AbstractComputerBlockEntity computer)) {
            return CHANNEL_ALONE;
        }
        if (!computer.speakersPlay()) {
            return CHANNEL_OFF;
        }
        if (computer.speakerCount() < 2) {
            return CHANNEL_ALONE;
        }
        final StereoSide side = computer.speakerSide(worldPosition);
        return switch (side) {
            case LEFT -> CHANNEL_LEFT;
            case RIGHT -> CHANNEL_RIGHT;
            case BOTH -> CHANNEL_BOTH;
        };
    }

    /* Whether its computer has a subwoofer against one of its Transition satellites, worked out on the server. */
    private boolean workOutSubwoofer() {
        final BlockPos owner = link.ownerPos();
        return owner != null && level instanceof ServerLevel server
                && Loaded.blockEntity(server, owner) instanceof AbstractComputerBlockEntity computer
                && computer.hasSubwoofer();
    }

    private boolean anotherSpeakerIsCalled(final ServerLevel level, final String wanted) {
        final BlockPos owner = link.ownerPos();
        if (owner == null || !(Loaded.blockEntity(level, owner) instanceof IPeripheralOwner linkedTo)) {
            return false;
        }
        final String folded = wanted.toLowerCase(Locale.ROOT);
        for (final long endpoint : linkedTo.linkedEndpoints()) {
            if (endpoint != worldPosition.asLong()
                    && Loaded.blockEntity(level, BlockPos.of(endpoint)) instanceof SpeakerBlockEntity other
                    && other.name().toLowerCase(Locale.ROOT).equals(folded)) {
                return true;
            }
        }
        return false;
    }
}
