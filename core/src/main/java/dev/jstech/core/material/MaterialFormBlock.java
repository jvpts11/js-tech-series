/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.material;

import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

/**
 * A material's block placed in the world: a block of tin, or tin's ore in one rock. Named from its form's name, or its
 * rock's, with the material's in it.
 */
public class MaterialFormBlock extends Block {

    private final MaterialThing thing;
    private final @Nullable HostRock rock;

    public MaterialFormBlock(final Properties properties, final MaterialThing thing, @Nullable final HostRock rock) {
        super(properties);
        this.thing = Objects.requireNonNull(thing, "thing");
        this.rock = rock;
    }

    /** The material and the form it is. */
    public MaterialThing thing() {
        return this.thing;
    }

    /** The rock it is the ore in, for an ore. */
    public Optional<HostRock> rock() {
        return Optional.ofNullable(this.rock);
    }

    /** What it is called: "Block of Tin", "Deepslate Tin Ore". */
    public Text displayName() {
        final Text material = this.thing.material().name();
        return this.rock != null ? this.rock.oreName(material) : this.thing.form().name().with(material);
    }

    @Override
    public MutableComponent getName() {
        return GameText.component(displayName());
    }
}
