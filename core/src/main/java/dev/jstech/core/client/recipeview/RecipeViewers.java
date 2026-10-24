/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.recipeview;

import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * The recipe viewers running on this game, JEI's, EMI's and REI's when they are installed. Each one is added by the
 * Core's bridge to it as the viewer starts; the Core's screens ask them through here, the first one added answering
 * first. With none installed every question is answered with nothing, and nothing opens.
 *
 * <p>A viewer opened from a screen that is not a container's, such as a manual, comes back to that screen when it
 * closes, whichever viewer it is: EMI's own recipe screen remembers only the container screen it was opened over.
 */
public final class RecipeViewers {

    private static final CopyOnWriteArrayList<IRecipeViewer> VIEWERS = new CopyOnWriteArrayList<>();
    /** The screen a viewer was opened from, to come back to when the viewer closes; on the render thread. */
    private static @Nullable Screen returnTo;

    private RecipeViewers() {
    }

    /** Hands the game's bus the coming back from a viewer, once, from the Core's client constructor. */
    @ApiStatus.Internal
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, event -> comeBack());
    }

    /** Adds a viewer, once however many times its bridge starts. */
    public static void add(final IRecipeViewer viewer) {
        VIEWERS.addIfAbsent(viewer);
    }

    /** Whether a viewer runs on this game, so a click on a recipe has somewhere to open it. */
    public static boolean any() {
        return !VIEWERS.isEmpty();
    }

    /** Opens a viewer on the recipes that make {@code stack}; whether one opened. */
    public static boolean showRecipesOf(final ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        final Screen from = Minecraft.getInstance().screen;
        for (final IRecipeViewer viewer : VIEWERS) {
            if (viewer.showRecipesOf(stack)) {
                opened(from);
                return true;
            }
        }
        return false;
    }

    /** Opens a viewer on the recipes {@code stack} goes into; whether one opened. */
    public static boolean showUsesOf(final ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        final Screen from = Minecraft.getInstance().screen;
        for (final IRecipeViewer viewer : VIEWERS) {
            if (viewer.showUsesOf(stack)) {
                opened(from);
                return true;
            }
        }
        return false;
    }

    /** Opens a viewer on the recipes worked in {@code machine}, a machine's item; whether one opened. */
    public static boolean showRecipesMadeIn(final ItemStack machine) {
        if (machine.isEmpty()) {
            return false;
        }
        final Screen from = Minecraft.getInstance().screen;
        for (final IRecipeViewer viewer : VIEWERS) {
            if (viewer.showRecipesMadeIn(machine)) {
                opened(from);
                return true;
            }
        }
        return false;
    }

    /** The item under the pointer in a viewer's lists and recipe screens, or an empty stack. */
    public static ItemStack hovered() {
        for (final IRecipeViewer viewer : VIEWERS) {
            final ItemStack under = viewer.hovered();
            if (!under.isEmpty()) {
                return under;
            }
        }
        return ItemStack.EMPTY;
    }

    /* A viewer opened over {@code from}: a screen that is not a container's is come back to. */
    private static void opened(@Nullable final Screen from) {
        returnTo = from == null || from instanceof AbstractContainerScreen<?> ? null : from;
    }

    /*
     * Each tick while a viewer may be open over a screen to come back to: the viewer closed to nothing, or to the
     * player's own inventory as EMI's does with no container screen to go back to, so the screen is opened again; the
     * viewer came back to it itself, or the player went on to another container, and it is let go.
     */
    private static void comeBack() {
        final Screen from = returnTo;
        if (from == null) {
            return;
        }
        final Minecraft minecraft = Minecraft.getInstance();
        final Screen now = minecraft.screen;
        if (now == null || now instanceof EffectRenderingInventoryScreen<?>) {
            returnTo = null;
            if (minecraft.level != null) {
                minecraft.setScreen(from);
            }
        } else if (now == from || now instanceof AbstractContainerScreen<?>) {
            returnTo = null;
        }
    }
}
