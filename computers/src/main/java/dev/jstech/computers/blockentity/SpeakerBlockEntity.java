/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.blockentity;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.PeripheralLinks;
import dev.jstech.computers.block.SpeakerBlock;
import dev.jstech.computers.menu.SpeakerMenu;
import dev.jstech.core.audio.FrequencyResponse;
import dev.jstech.core.audio.StereoSide;
import dev.jstech.core.peripheral.IPeripheralEndpoint;
import dev.jstech.core.peripheral.IPeripheralOwner;
import dev.jstech.core.peripheral.PeripheralCableType;
import dev.jstech.core.peripheral.PeripheralLinkValidator;
import dev.jstech.core.tier.HardwareEra;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
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
public class SpeakerBlockEntity extends BlockEntity implements IPeripheralEndpoint {

    @Nullable
    private Long linkedOwner;
    /** The name a player gave it; empty until one is given, when it is called by the word for a speaker. */
    private String name = "";
    /** Whether the last name asked for is one another speaker of its computer already has. */
    private boolean clash;
    /** The name being typed on its screen, taken when the screen closes; null while nothing is being typed. */
    @Nullable
    private String asked;
    /** What its screen reads while it is open: whether the name clashes, and which side it plays. */
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(final int index) {
            return switch (index) {
                case DATA_CLASH -> clash ? 1 : 0;
                case DATA_CHANNEL -> channel();
                default -> 0;
            };
        }

        @Override
        public void set(final int index, final int value) {
            // Read only: the server works both out.
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    /** The longest name a speaker takes, the same as a computer's. */
    public static final int MAX_NAME = 32;
    public static final int DATA_CLASH = 0;
    public static final int DATA_CHANNEL = 1;
    public static final int DATA_COUNT = 2;
    /** Not linked to a computer, so it plays nothing. */
    public static final int CHANNEL_NONE = 0;
    /** The computer's only speaker, which plays both sides. */
    public static final int CHANNEL_ALONE = 1;
    /** One of several, standing where it plays both sides. */
    public static final int CHANNEL_BOTH = 2;
    public static final int CHANNEL_LEFT = 3;
    public static final int CHANNEL_RIGHT = 4;
    private static final String NBT_LINKED_OWNER = "LinkedOwner";
    private static final String NBT_NAME = "SpeakerName";

    public SpeakerBlockEntity(final BlockPos pos, final BlockState state) {
        super(ComputingModule.SPEAKER_BE.get(), pos, state);
    }

    public static void serverTick(final Level level, final BlockPos pos, final BlockState state,
                                  final SpeakerBlockEntity be) {
        if (level instanceof ServerLevel serverLevel) {
            be.tick(serverLevel);
        }
    }

    @Override
    public PeripheralCableType cableType() {
        return PeripheralCableType.COMPUTING;
    }

    @Override
    public Optional<Long> linkedOwner() {
        return Optional.ofNullable(linkedOwner);
    }

    @Override
    public void onOwnerLinked(final long ownerPos) {
        linkedOwner = ownerPos;
        setChanged();
    }

    @Override
    public void onOwnerUnlinked() {
        linkedOwner = null;
        clash = false;
        setChanged();
    }

    /** The computer it is linked to, or null. */
    @Nullable
    public BlockPos ownerPos() {
        return linkedOwner == null ? null : BlockPos.of(linkedOwner);
    }

    /** The name a player gave it; empty while it has none. */
    public String name() {
        return name;
    }

    /** The era of its model, which decides how well it plays. */
    public HardwareEra era() {
        return getBlockState().getBlock() instanceof SpeakerBlock speaker ? speaker.era() : HardwareEra.STANDARD;
    }

    /** What it reproduces of a recording. */
    public FrequencyResponse response() {
        return getBlockState().getBlock() instanceof SpeakerBlock speaker ? speaker.response()
                : FrequencyResponse.FULL;
    }

    public ContainerData dataAccess() {
        return data;
    }

    /**
     * The name being typed on its screen, looked at as it is typed: its screen says at once whether another speaker
     * of its computer is already called that, whatever the case of its letters. Nothing is taken until the screen
     * closes, so the names a player passes through on the way to one are never given.
     */
    public void ask(final ServerLevel level, final String requested) {
        final String stripped = requested.strip();
        asked = stripped.length() > MAX_NAME ? stripped.substring(0, MAX_NAME) : stripped;
        clash = !asked.isEmpty() && anotherSpeakerIsCalled(level, asked);
    }

    /**
     * Takes the name asked for when its screen closes, unless it clashes; then the speaker keeps the name it had. An
     * empty name gives it back its default and never clashes.
     */
    public void takeAskedName() {
        if (asked != null && !clash && !asked.equals(name)) {
            name = asked;
            setChanged();
        }
        asked = null;
        clash = false;
    }

    /** What its screen opens with: its name, and the computer it plays for. */
    public SpeakerMenu.Opening opening(final ServerLevel level) {
        String computerName = "";
        String computerKind = "";
        if (linkedOwner != null && level.getBlockEntity(BlockPos.of(linkedOwner))
                instanceof AbstractComputerBlockEntity computer) {
            computerName = computer.customName();
            computerKind = computer.getBlockState().getBlock().getDescriptionId();
        }
        return new SpeakerMenu.Opening(worldPosition, name, computerName, computerKind, era());
    }

    /**
     * Breaks the peripheral link from this speaker's side, notifying the owner so it frees the port.
     * Safe to call when no link exists (no-op).
     */
    public void unlink(final ServerLevel level) {
        if (linkedOwner != null
                && level.getBlockEntity(BlockPos.of(linkedOwner)) instanceof IPeripheralOwner owner) {
            owner.onEndpointUnlinked(worldPosition.asLong());
        }
        onOwnerUnlinked();
    }

    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (linkedOwner != null) {
            tag.putLong(NBT_LINKED_OWNER, linkedOwner);
        }
        if (!name.isEmpty()) {
            tag.putString(NBT_NAME, name);
        }
    }

    @Override
    protected void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        linkedOwner = tag.contains(NBT_LINKED_OWNER) ? tag.getLong(NBT_LINKED_OWNER) : null;
        name = tag.getString(NBT_NAME);
    }

    private void tick(final ServerLevel level) {
        final long self = worldPosition.asLong();
        final PeripheralLinkValidator validator = PeripheralLinks.validator(level);
        if (linkedOwner == null) {
            PeripheralLinks.discoverOwner(level, self)
                    .ifPresent(ownerPos -> validator.tryEstablishLink(ownerPos, self));
        } else {
            final boolean ownerPresent =
                    level.getBlockEntity(BlockPos.of(linkedOwner)) instanceof IPeripheralOwner;
            if (!ownerPresent
                    || !validator.isLinkStillValid(linkedOwner, self, PeripheralCableType.COMPUTING)) {
                unlink(level);
            }
        }
    }

    /* Which side it plays for its computer, as its screen shows it. */
    private int channel() {
        if (linkedOwner == null || !(level instanceof ServerLevel server)) {
            return CHANNEL_NONE;
        }
        if (!(server.getBlockEntity(BlockPos.of(linkedOwner)) instanceof AbstractComputerBlockEntity computer)
                || computer.speakerCount() < 2) {
            return CHANNEL_ALONE;
        }
        final StereoSide side = computer.speakerSide(worldPosition);
        return switch (side) {
            case LEFT -> CHANNEL_LEFT;
            case RIGHT -> CHANNEL_RIGHT;
            case BOTH -> CHANNEL_BOTH;
        };
    }

    private boolean anotherSpeakerIsCalled(final ServerLevel level, final String wanted) {
        if (linkedOwner == null
                || !(level.getBlockEntity(BlockPos.of(linkedOwner)) instanceof IPeripheralOwner owner)) {
            return false;
        }
        final String folded = wanted.toLowerCase(Locale.ROOT);
        for (final long endpoint : owner.linkedEndpoints()) {
            if (endpoint != worldPosition.asLong()
                    && level.getBlockEntity(BlockPos.of(endpoint)) instanceof SpeakerBlockEntity other
                    && other.name.toLowerCase(Locale.ROOT).equals(folded)) {
                return true;
            }
        }
        return false;
    }
}
