/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.fluid;

import dev.jstech.core.JsCore;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;

/**
 * The marks a fluid carries that a pipe has to be made for: a gas needs a pipe that holds it in, and a corrosive fluid
 * one that stands it. A pipe that is not made for a mark carries no fluid that has it. They are tags, so a datapack can
 * mark the fluids of any mod.
 */
@TextHolder
public final class CoreFluidTags {

    /** Fluids that are gases. */
    public static final TagKey<Fluid> GASES = tag("gases");
    /** Fluids that eat through what is not made to stand them. */
    public static final TagKey<Fluid> CORROSIVE = tag("corrosive");
    /** What {@link #GASES} is called, under the key the recipe viewers read a tag's name from. */
    public static final TextKey GASES_NAME = TextKey.of("tag.fluid.jscore.gases", "Gases");
    /** What {@link #CORROSIVE} is called. */
    public static final TextKey CORROSIVE_NAME = TextKey.of("tag.fluid.jscore.corrosive", "Corrosive Fluids");

    private CoreFluidTags() {
    }

    private static TagKey<Fluid> tag(final String name) {
        return TagKey.create(Registries.FLUID, ResourceLocation.fromNamespaceAndPath(JsCore.MODID, name));
    }
}
