/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.gui.tooltip;

import com.mojang.datafixers.util.Either;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;

/**
 * What one mod says in one item's tooltip, as its {@link ITooltipSection} writes it: what is shown always, and the
 * two parts that wait for a key, Shift for what the item does and Ctrl for its numbers. A line written as a
 * {@link Text} is drawn in the game's grey, as tooltips' lines are.
 *
 * <pre>{@code
 * tooltip.piece(TooltipBar.of(stored, most, TooltipBar.Paint.ENERGY, energyLine))
 *         .line(DrillTexts.AREA.text());
 * tooltip.shift().line(DrillTexts.WHAT_IT_DOES.text());
 * tooltip.ctrl().line(DrillTexts.USES.with(200));
 * }</pre>
 */
public final class ItemTooltip {

    private final ItemStack stack;
    private final Item.TooltipContext context;
    private final @Nullable Player player;
    private final TooltipFlag flags;
    private final Part always = new Part();
    private final Part shift = new Part();
    private final Part ctrl = new Part();

    ItemTooltip(final ItemStack stack, final Item.TooltipContext context, final @Nullable Player player,
                final TooltipFlag flags) {
        this.stack = stack;
        this.context = context;
        this.player = player;
        this.flags = flags;
    }

    /** The item. */
    public ItemStack stack() {
        return stack;
    }

    /** What the game gives the tooltip to read registries and the world's data by. */
    public Item.TooltipContext context() {
        return context;
    }

    /** The player looking, when there is one. */
    public @Nullable Player player() {
        return player;
    }

    /** Whether the player asked for the game's advanced tooltips, and whether this is the creative tab's. */
    public TooltipFlag flags() {
        return flags;
    }

    /** Adds a line shown always. */
    public ItemTooltip line(final Component line) {
        always.line(line);
        return this;
    }

    /** Adds a line shown always, in the game's grey. */
    public ItemTooltip line(final Text line) {
        always.line(line);
        return this;
    }

    /** Adds a piece shown always: a bar, a picture, a 3D preview, what the item holds, or a mod's own. */
    public ItemTooltip piece(final TooltipComponent piece) {
        always.piece(piece);
        return this;
    }

    /** What is shown only with Shift held: what the item does. */
    public Part shift() {
        return shift;
    }

    /** What is shown only with Ctrl held: the item's numbers. */
    public Part ctrl() {
        return ctrl;
    }

    Part always() {
        return always;
    }

    /** Whether nothing was written. */
    boolean isEmpty() {
        return always.isEmpty() && shift.isEmpty() && ctrl.isEmpty();
    }

    /** A part of a tooltip: its lines and pieces, in order. */
    public static final class Part {

        private final List<Either<Component, TooltipComponent>> parts = new ArrayList<>();

        private Part() {
        }

        /** Adds a line. */
        public Part line(final Component line) {
            parts.add(Either.left(line));
            return this;
        }

        /** Adds a line in the game's grey. */
        public Part line(final Text line) {
            return line(GameText.component(line).copy().withStyle(ChatFormatting.GRAY));
        }

        /** Adds a piece: a bar, a picture, a 3D preview, what the item holds, or a mod's own. */
        public Part piece(final TooltipComponent piece) {
            parts.add(Either.right(piece));
            return this;
        }

        boolean isEmpty() {
            return parts.isEmpty();
        }

        List<Either<Component, TooltipComponent>> parts() {
            return parts;
        }
    }
}
