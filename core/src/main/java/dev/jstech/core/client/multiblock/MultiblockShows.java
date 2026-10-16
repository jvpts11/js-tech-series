/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.multiblock;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Where the views that show a multiblock before it is built meet what shows it in the world, on the player's side:
 * the items that show multiblocks, which the recipe viewers list beside every multiblock, and the one way a multiblock
 * is shown in front of the player from a recipe viewer's button, which whatever draws multiblocks in the world gives.
 */
public final class MultiblockShows {

    /* The items that show multiblocks, in the order given. */
    private static final List<Supplier<ItemStack>> TOOLS = new CopyOnWriteArrayList<>();

    @Nullable
    private static volatile IWorldShow world;

    private MultiblockShows() {
    }

    /** Adds an item that shows multiblocks, listed beside every multiblock in the recipe viewers. */
    public static void addTool(final Supplier<ItemStack> tool) {
        TOOLS.add(tool);
    }

    /** The items that show multiblocks, those with nothing to show left out. */
    public static List<ItemStack> tools() {
        return TOOLS.stream().map(Supplier::get).filter(stack -> !stack.isEmpty()).toList();
    }

    /** Makes {@code show} what shows a multiblock in front of the player; the last given is the one used. */
    public static void showsInWorld(final IWorldShow show) {
        world = show;
    }

    /** Whether anything can show a multiblock in front of the player. */
    public static boolean canShowInWorld() {
        return world != null;
    }

    /**
     * Shows the multiblock of {@code id} in front of the player, or stops showing it when it is the one shown.
     *
     * @return whether anything shows multiblocks in the world
     */
    public static boolean toggle(final ResourceLocation id) {
        final IWorldShow show = world;
        if (show == null) {
            return false;
        }
        show.show(show.shown().filter(id::equals).isPresent() ? null : id);
        return true;
    }

    /** The multiblock shown in front of the player now, or none. */
    public static Optional<ResourceLocation> shownInWorld() {
        final IWorldShow show = world;
        return show == null ? Optional.empty() : show.shown();
    }

    /** What shows a multiblock in front of the player. */
    public interface IWorldShow {

        /** Shows the multiblock of {@code id} in front of the player in place of any shown, or none for none. */
        void show(@Nullable ResourceLocation id);

        /** The multiblock it shows now, or none. */
        Optional<ResourceLocation> shown();
    }
}
