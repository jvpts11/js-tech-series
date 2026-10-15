/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.machine;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * A machine's setup carried from one machine to others, as a configuration card carries it: copied from one, pasted
 * into another of the same kind whole, into a compatible one with what fits, or into every machine of a box at once.
 * Only machines the player may set up take it.
 */
public final class MachineSetup {

    /** The most blocks a box pasted over at once may hold. */
    public static final int AREA_MOST = 4_096;
    public static final Codec<Copied> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("machine").forGetter(Copied::machine),
            CompoundTag.CODEC.fieldOf("setup").forGetter(Copied::setup)
    ).apply(instance, Copied::new));
    public static final StreamCodec<ByteBuf, Copied> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, Copied::machine, ByteBufCodecs.COMPOUND_TAG, Copied::setup, Copied::new);

    private MachineSetup() {
    }

    /** The setup of {@code machine}, named by its block. */
    public static Copied copy(final MachineBlockEntity machine) {
        return new Copied(BuiltInRegistries.BLOCK.getKey(machine.getBlockState().getBlock()),
                machine.copySetup(Objects.requireNonNull(machine.getLevel(), "level").registryAccess()));
    }

    /**
     * Pastes {@code copied} into {@code machine} for {@code player}: whole into a machine of the same block, what fits
     * into another. Whether it took anything; nothing for a player who may not set it up.
     */
    public static boolean paste(final ServerPlayer player, final Copied copied, final MachineBlockEntity machine) {
        if (!machine.maySetUp(player) || machine.getLevel() == null) {
            return false;
        }
        final boolean sameKind = BuiltInRegistries.BLOCK.getKey(machine.getBlockState().getBlock())
                .equals(copied.machine());
        return machine.pasteSetup(copied.setup(), sameKind, machine.getLevel().registryAccess());
    }

    /**
     * Pastes {@code copied} into every machine in the box between {@code from} and {@code to}, both included, that
     * takes anything of it from {@code player}: how many took it, or -1 when the box holds more than
     * {@link #AREA_MOST} blocks and nothing is pasted.
     */
    public static int pasteOver(final ServerLevel level, final ServerPlayer player, final Copied copied,
                                final BlockPos from, final BlockPos to) {
        final long volume = (long) (Math.abs(from.getX() - to.getX()) + 1) * (Math.abs(from.getY() - to.getY()) + 1)
                * (Math.abs(from.getZ() - to.getZ()) + 1);
        if (volume > AREA_MOST) {
            return -1;
        }
        int pasted = 0;
        for (final BlockPos pos : BlockPos.betweenClosed(from, to)) {
            if (level.isLoaded(pos) && level.getBlockEntity(pos) instanceof MachineBlockEntity machine
                    && paste(player, copied, machine)) {
                pasted++;
            }
        }
        return pasted;
    }

    /**
     * A machine's setup as copied.
     *
     * @param machine the block of the machine it was copied from
     * @param setup   what was copied
     */
    public record Copied(ResourceLocation machine, CompoundTag setup) {

        public Copied {
            Objects.requireNonNull(machine, "machine");
            setup = setup.copy();
        }

        @Override
        public CompoundTag setup() {
            return setup.copy();
        }
    }
}
