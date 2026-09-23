/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.datagen.advancement;

import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.Criterion;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.Supplier;

/**
 * One advancement as written down: where it sits in its tab, how it looks, what it is called and how it is earned.
 *
 * <p>The criteria are suppliers because the triggers they stand on exist only once the game's registries do, which
 * is later than the tabs are written down; the words are plain strings because the language file wants them
 * without a game at all.
 *
 * @param name        its name within the tab, which is also the last part of its id and of its translation keys
 * @param parent      the name of the one it hangs from in the same tab, or null for the tab's root
 * @param requiredMod the mod that has to be present for the advancement to exist at all, or null when none does
 */
public record AdvancementSpec(String name, @Nullable String parent, ItemLike icon, AdvancementType frame,
                              boolean hidden, String title, String description,
                              Map<String, Supplier<Criterion<?>>> criteria, @Nullable String requiredMod) {
}
