/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries;

import dev.jstech.core.material.MaterialUses;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;

/**
 * What of the series' shared content each installed mod uses: a material, a fluid, a component, a shared block or a
 * tool. The J's Tech creative tab shows only what some mod uses, and a mod that declares its uses leaves them out of
 * its own tabs, so nothing shows twice. A mod declares its uses once, from its constructor:
 *
 * <pre>{@code
 * SharedUses.declare(MODID, () -> CoreGuide.MANUAL);
 * }</pre>
 *
 * <p>A material in a form is declared with {@link MaterialUses}, which registers its thing as well.
 */
public final class SharedUses {

    private static final List<Use> USES = new CopyOnWriteArrayList<>();

    private SharedUses() {
    }

    /** Declares that the mod {@code modid} uses {@code thing}. */
    public static void declare(final String modid, final Supplier<? extends ItemLike> thing) {
        USES.add(new Use(Objects.requireNonNull(modid, "modid"), Objects.requireNonNull(thing, "thing")));
    }

    /**
     * Whether some installed mod uses {@code item}, declared here or as a use of a material
     * ({@link MaterialUses}). Asked once the game's registries are filled.
     */
    public static boolean used(final Item item) {
        if (MaterialUses.shared(item)) {
            return true;
        }
        for (final Use use : USES) {
            if (use.thing().get().asItem() == item) {
                return true;
            }
        }
        return false;
    }

    /** The mods that use {@code item}. */
    public static Set<String> usersOf(final Item item) {
        final Set<String> out = new LinkedHashSet<>();
        for (final Use use : USES) {
            if (use.thing().get().asItem() == item) {
                out.add(use.modid());
            }
        }
        MaterialUses.thingOf(item).ifPresent(thing -> MaterialUses.all().forEach(use -> {
            if (use.shared() && use.thing().equals(thing)) {
                out.add(use.modid());
            }
        }));
        return out;
    }

    /** The mods that declared any use, of shared content or of a material. */
    public static Set<String> users() {
        final Set<String> out = new LinkedHashSet<>();
        USES.forEach(use -> out.add(use.modid()));
        out.addAll(MaterialUses.users());
        return out;
    }

    /* One mod's use of one thing. */
    private record Use(String modid, Supplier<? extends ItemLike> thing) {
    }
}
