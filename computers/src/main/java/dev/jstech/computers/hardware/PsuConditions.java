/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.hardware;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.computers.config.ComputersServerConfig;
import dev.jstech.computers.registry.ComputingComponents;
import dev.jstech.computers.registry.HardwareRegistry;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * A power supply's {@link PsuCondition} as its item carries it: how it is saved and sent, and how it is read off a
 * stack. With the server's {@code hardware.psu_failures} off, no supply wears, warns or fails.
 */
@ApiStatus.Experimental
public final class PsuConditions {

    /** The most units a supply's condition is sent with: more than any supply has. */
    private static final int MOST_UNITS = 8;

    public static final Codec<PsuCondition> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.FLOAT.optionalFieldOf("life", 0f).forGetter(PsuCondition::life),
            Codec.FLOAT.listOf().optionalFieldOf("wear", List.of()).forGetter(PsuCondition::wear)
    ).apply(i, PsuCondition::new));

    public static final StreamCodec<ByteBuf, PsuCondition> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, PsuCondition::life,
            ByteBufCodecs.FLOAT.apply(ByteBufCodecs.list(MOST_UNITS)), PsuCondition::wear,
            PsuCondition::new);

    private PsuConditions() {
    }

    /** How worn the supply {@code stack} is; a supply never turned on, or with failures off, is new. */
    public static PsuCondition of(final ItemStack stack) {
        if (!ComputersServerConfig.psuFailures()) {
            return PsuCondition.NEW;
        }
        final PsuCondition condition = stack.get(ComputingComponents.PSU_CONDITION.get());
        return condition == null ? PsuCondition.NEW : condition;
    }

    /** Writes {@code condition} onto the supply {@code stack}. */
    public static void set(final ItemStack stack, final PsuCondition condition) {
        stack.set(ComputingComponents.PSU_CONDITION.get(), condition);
    }

    /** Whether {@code stack} is a power supply whose every unit has failed, so it gives nothing. */
    public static boolean failed(final ItemStack stack) {
        final PsuSpec spec = spec(stack);
        return spec != null && of(stack).failed(spec);
    }

    /** The specification of the supply {@code stack}, or null for anything else. */
    @Nullable
    public static PsuSpec spec(final ItemStack stack) {
        return stack.isEmpty() ? null : HardwareRegistry.psu(stack);
    }
}
