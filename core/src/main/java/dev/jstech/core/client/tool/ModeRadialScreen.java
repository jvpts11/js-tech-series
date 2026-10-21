/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.tool;

import com.mojang.blaze3d.platform.InputConstants;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.input.KeyActionsClient;
import dev.jstech.core.gui.layout.ToolHudLayout;
import dev.jstech.core.input.CoreKeys;
import dev.jstech.core.item.ItemMode;
import dev.jstech.core.item.ItemModePayload;
import dev.jstech.core.item.ItemStates;
import dev.jstech.core.text.GameText;
import dev.jstech.core.tool.ToolSpec;
import dev.jstech.core.tool.ToolTexts;
import dev.jstech.core.tool.Tools;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The ring of an item's modes, held open by the mode key over a darkened world: a segment to each mode around the item,
 * the first at the top, each with its picture and its name; the one the mouse points at lit, the one it is in marked
 * with a dot. Letting go of the key chooses the mode pointed at; Escape, or letting go in the middle, keeps the one it
 * is in. The world goes on behind it.
 */
public final class ModeRadialScreen extends Screen {

    private final ItemStack held;
    private final List<ItemMode> modes;
    private final int current;
    private final Optional<ToolSpec> spec;
    private int pointed = -1;

    /* The names of the modes are written at this size, as the game's smaller labels. */
    private static final float SMALL = 0.75F;

    /** The ring of the modes of {@code held}, the item in the main hand. */
    public ModeRadialScreen(final ItemStack held) {
        super(Component.empty());
        this.held = held;
        this.modes = ItemStates.of(held).modes();
        this.current = ItemStates.mode(held).map(this.modes::indexOf).orElse(0);
        this.spec = Tools.spec(held);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(final GuiGraphics g, final int mouseX, final int mouseY, final float partialTick) {
        g.fill(0, 0, this.width, this.height, ToolHudPalette.get().dim());
    }

    @Override
    public void render(final GuiGraphics g, final int mouseX, final int mouseY, final float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        final ToolHudPalette.Colours colours = ToolHudPalette.get();
        final int cx = this.width / 2;
        final int cy = this.height / 2 - ToolHudLayout.RAISED;
        final int count = this.modes.size();
        this.pointed = ToolHudLayout.pointedAt(mouseX - cx, mouseY - cy, count);
        final RingMask mask = RingMask.of(count);
        for (int row = 0; row < mask.rows().size(); row++) {
            final int y = cy - ToolHudLayout.OUTER + row;
            for (final RingMask.Run run : mask.rows().get(row)) {
                final boolean lit = run.segment() == this.pointed;
                final int colour = run.edge() ? (lit ? colours.pointedEdge() : colours.segmentEdge())
                        : (lit ? colours.pointed() : colours.segment());
                g.fill(cx + run.from(), y, cx + run.to(), y + 1, colour);
            }
            for (final RingMask.Run run : mask.disc().get(row)) {
                g.fill(cx + run.from(), y, cx + run.to(), y + 1, run.edge() ? colours.segmentEdge() : colours.middle());
            }
        }
        for (int i = 0; i < count; i++) {
            drawMode(g, cx + ToolHudLayout.placeX(i, count), cy + ToolHudLayout.placeY(i, count), i, colours);
        }
        g.pose().pushPose();
        g.pose().translate(cx - ToolHudLayout.ITEM / 2.0F, cy - ToolHudLayout.ITEM / 2.0F, 0);
        g.pose().scale(2.0F, 2.0F, 1.0F);
        g.renderItem(this.held, 0, 0);
        g.pose().popPose();
        final Component name = this.held.getHoverName();
        Draw.text(g, this.font, name, cx - this.font.width(name) / 2, cy + ToolHudLayout.NAME_BELOW,
                colours.current(), true);
        if (this.pointed >= 0) {
            final String hint = GameText.component(ToolTexts.RELEASE_TO_CHOOSE.with(this.modes.get(this.pointed)
                    .name())).getString();
            small(g, hint, cx, cy + ToolHudLayout.HINT_BELOW, colours.hint());
        }
    }

    @Override
    public void tick() {
        /* The key let go while the game was not listening for it: choose as though it had heard. */
        final KeyMapping key = KeyActionsClient.mapping(CoreKeys.CHANGE_ITEM_MODE);
        if (!key.isUnbound() && key.getKey().getType() == InputConstants.Type.KEYSYM
                && !InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(),
                key.getKey().getValue())) {
            choose();
            return;
        }
        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !minecraft.player.getMainHandItem().is(this.held.getItem())) {
            onClose();
        }
    }

    @Override
    public boolean keyReleased(final int keyCode, final int scanCode, final int modifiers) {
        if (KeyActionsClient.mapping(CoreKeys.CHANGE_ITEM_MODE).matches(keyCode, scanCode)) {
            choose();
            return true;
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseReleased(final double mouseX, final double mouseY, final int button) {
        if (KeyActionsClient.mapping(CoreKeys.CHANGE_ITEM_MODE).matchesMouse(button)) {
            choose();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    /** The mode the mouse points at, or -1 while it is in the middle. */
    public int pointed() {
        return this.pointed;
    }

    /** What letting go of the key does: puts the item in the mode pointed at, when it is another, and closes. */
    public void choose() {
        if (this.pointed >= 0 && this.pointed != this.current && this.pointed < this.modes.size()) {
            PacketDistributor.sendToServer(new ItemModePayload(this.modes.get(this.pointed).id()));
        }
        onClose();
    }

    private void drawMode(final GuiGraphics g, final int x, final int y, final int index,
                          final ToolHudPalette.Colours colours) {
        final boolean lit = index == this.pointed;
        if (this.spec.isPresent() && ModeGlyphs.has(this.spec.get())) {
            ModeGlyphs.draw(g, this.spec.get(), this.modes.get(index), x - ToolHudLayout.GLYPH_WIDTH / 2,
                    y - ToolHudLayout.GLYPH_ABOVE, lit ? colours.current() : colours.label());
        }
        final int text = lit ? colours.pointedText() : index == this.current ? colours.current() : colours.label();
        small(g, GameText.component(this.modes.get(index).name().text()).getString(), x,
                y + ToolHudLayout.LABEL_BELOW, text);
        if (index == this.current) {
            final int half = ToolHudLayout.DOT / 2;
            g.fill(x - half, y + ToolHudLayout.DOT_BELOW, x - half + ToolHudLayout.DOT,
                    y + ToolHudLayout.DOT_BELOW + ToolHudLayout.DOT, colours.currentDot());
        }
    }

    /* A line in the smaller labels' size, centred on {@code cx}. */
    private void small(final GuiGraphics g, final String text, final int cx, final int y, final int colour) {
        final int width = Math.round(this.font.width(text) * SMALL);
        Draw.textScaled(g, this.font, text, cx - width / 2, y, colour, SMALL);
    }
}
