/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests;

import dev.jstech.core.content.PartEntry;
import dev.jstech.core.content.PartLook;
import dev.jstech.core.multipart.IFacePart;
import dev.jstech.core.multipart.IPartHost;
import dev.jstech.core.multipart.PartType;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;

/**
 * A kind of part declared the Core's way, to prove it: named once, drawn as a plate the data generation writes, its
 * item mounting it on cables, spaces of parts and the faces of other blocks.
 */
public final class TestParts {

    /** A plain plate of iron with a stone rim, which does nothing but stand on its face. */
    public static final PartEntry<PlatePart> PLATE = TestSounds.CONTENT.part("plate_part", PlatePart::new)
            .named("Test Plate Part").look(PartLook.plate("minecraft:block/iron_block", "minecraft:block/stone"))
            .tab(TestItems.ITEMS).register();

    private TestParts() {
    }

    /** Declares the part, from the test mod's constructor, before its content is registered. */
    public static void declare() {
        // Loading the class declares it.
    }

    /** A plate that does nothing, gives its item back and never ticks. */
    public static final class PlatePart implements IFacePart {

        @Override
        public PartType<?> type() {
            return PLATE.get();
        }

        @Override
        public void attach(final IPartHost host, final Direction face) {
        }

        @Override
        public boolean ticks() {
            return false;
        }

        @Override
        public ItemStack partItem() {
            return new ItemStack(PLATE.item().get());
        }
    }
}
