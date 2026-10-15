/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.material;

import dev.jstech.core.text.GameText;
import java.util.Objects;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * A material's item in one form, iron dust: named from its form's name with the material's in it, so a material named
 * later, or renamed by whoever discovered it, names all its things at once.
 */
public class MaterialFormItem extends Item {

    private final MaterialThing thing;

    public MaterialFormItem(final Properties properties, final MaterialThing thing) {
        super(properties);
        this.thing = Objects.requireNonNull(thing, "thing");
    }

    /** The material and the form it is. */
    public MaterialThing thing() {
        return this.thing;
    }

    @Override
    public Component getName(final ItemStack stack) {
        return GameText.component(this.thing.form().name().with(this.thing.material().name()));
    }
}
