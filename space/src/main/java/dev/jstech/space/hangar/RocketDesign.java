/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.hangar;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.space.rocket.RocketStack;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.Objects;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * A rocket as a Rocket Hangar's design screen holds it: its name and its parts as they stand, from the bottom up. A
 * hangar keeps the one on its screen, those saved as its plans, and the one sent to its bay to be built.
 *
 * @param name  what the player called it; empty for a rocket not named yet
 * @param stack its parts, from the bottom up
 */
public record RocketDesign(String name, RocketStack stack) {

    /** The longest name a design takes. */
    public static final int LONGEST_NAME = 24;
    /** The most parts a design stacks, the most boosters a part holds at its side, the most segments a booster. */
    public static final int MOST_PARTS = 64;
    public static final int MOST_BOOSTERS = 8;
    public static final int MOST_SEGMENTS = 16;
    /** A design with no parts and no name, as a hangar's screen starts. */
    public static final RocketDesign EMPTY = new RocketDesign("", new RocketStack(List.of()));
    public static final Codec<RocketDesign> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.string(0, LONGEST_NAME).optionalFieldOf("name", "").forGetter(RocketDesign::name),
            RocketStack.CODEC.fieldOf("parts").forGetter(RocketDesign::stack)
    ).apply(instance, RocketDesign::new));
    public static final StreamCodec<ByteBuf, RocketDesign> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    public RocketDesign {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(stack, "stack");
    }

    /** The same design, its parts {@code parts}. */
    public RocketDesign with(final RocketStack parts) {
        return new RocketDesign(this.name, parts);
    }

    /** The same design, called {@code called}. */
    public RocketDesign named(final String called) {
        return new RocketDesign(called, this.stack);
    }

    /** Whether it keeps within the limits a hangar holds a design to: its name, its parts, its boosters. */
    public boolean bounded() {
        if (this.name.length() > LONGEST_NAME || this.stack.sections().size() > MOST_PARTS) {
            return false;
        }
        for (final RocketStack.Section section : this.stack.sections()) {
            if (section.boosters().size() > MOST_BOOSTERS) {
                return false;
            }
            for (final List<RocketStack.Part> booster : section.boosters()) {
                if (booster.isEmpty() || booster.size() > MOST_SEGMENTS) {
                    return false;
                }
            }
        }
        return true;
    }
}
