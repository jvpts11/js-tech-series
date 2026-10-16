/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.content;

import java.util.Objects;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * How a part declared with {@link ModContent#part} looks: a model written by hand, or a plate the data generation
 * writes, twelve pixels across and five deep against its face, its front one texture and the rest another. Each part's
 * model faces north, as the Core turns it to the face it stands on; its item shows the same model.
 */
public sealed interface PartLook permits PartLook.Handmade, PartLook.Plate {

    /** A model written by hand in the mod's resources, the same whether the part is at work or not. */
    static PartLook handmade(final ResourceLocation model) {
        return new Handmade(model, model);
    }

    /** A model written by hand, and another for while the part is at work. */
    static PartLook handmade(final ResourceLocation model, final ResourceLocation busy) {
        return new Handmade(model, busy);
    }

    /** A plate whose front wears {@code front} and the rest {@code side}: {@code "block/part/sensor_front"}. */
    static PartLook plate(final String front, final String side) {
        return new Plate(front, null, side);
    }

    /** A plate whose front wears {@code front}, or {@code busyFront} while the part is at work. */
    static PartLook plate(final String front, final String busyFront, final String side) {
        return new Plate(front, busyFront, side);
    }

    /** The model the part of {@code id} of {@code modid} is drawn with while idle. */
    ResourceLocation model(String modid, String id);

    /** The model it is drawn with while at work. */
    ResourceLocation busyModel(String modid, String id);

    /**
     * A model written by hand.
     *
     * @param model its model, idle
     * @param busy  its model at work
     */
    record Handmade(ResourceLocation model, ResourceLocation busy) implements PartLook {

        public Handmade {
            Objects.requireNonNull(model, "model");
            Objects.requireNonNull(busy, "busy");
        }

        @Override
        public ResourceLocation model(final String modid, final String id) {
            return this.model;
        }

        @Override
        public ResourceLocation busyModel(final String modid, final String id) {
            return this.busy;
        }
    }

    /**
     * A plate the data generation writes, as {@code block/part/<id>}, and {@code block/part/<id>_busy} with a front of
     * its own at work.
     *
     * @param front     the front's texture
     * @param busyFront the front's texture at work, or none for the same
     * @param side      the rest's texture
     */
    record Plate(String front, @Nullable String busyFront, String side) implements PartLook {

        public Plate {
            Objects.requireNonNull(front, "front");
            Objects.requireNonNull(side, "side");
        }

        @Override
        public ResourceLocation model(final String modid, final String id) {
            return ResourceLocation.fromNamespaceAndPath(modid, "block/part/" + id);
        }

        @Override
        public ResourceLocation busyModel(final String modid, final String id) {
            return this.busyFront == null ? model(modid, id)
                    : ResourceLocation.fromNamespaceAndPath(modid, "block/part/" + id + "_busy");
        }
    }
}
