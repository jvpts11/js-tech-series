/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.variant;

import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import java.util.Objects;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.block.Block;

/** A variant's block when its kind asks for no other: a plain block, named after the kind and the material. */
public class VariantBlock extends Block {

    private final Text name;

    public VariantBlock(final Properties properties, final Text name) {
        super(properties);
        this.name = Objects.requireNonNull(name, "name");
    }

    /** What it is called: "Oak Frame". */
    public Text displayName() {
        return this.name;
    }

    @Override
    public MutableComponent getName() {
        return GameText.component(this.name);
    }
}
