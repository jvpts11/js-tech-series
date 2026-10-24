/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.recipeview;

import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.guide.GuideClient;
import dev.jstech.core.client.multiblock.MultiblockPreview;
import dev.jstech.core.client.multiblock.MultiblockShows;
import dev.jstech.core.multiblock.MultiblockViewLayout;
import dev.jstech.core.multiblock.MultiblockViewTexts;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;

/**
 * One multiblock drawn in a recipe viewer, everything of it but its slots, which the viewer draws: the 3D preview,
 * turned by a drag where the viewer passes drags on and by a click where it does not; the layer's buttons; the choice
 * of a place; how it may stand; its count and size; and the buttons that show it in the world and open its page. One
 * for each time a viewer shows the multiblock, which keeps how it is turned and the layer shown.
 */
public final class MultiblockCard {

    private final MultiblockEntry entry;
    private final MultiblockPreview preview = new MultiblockPreview();
    private final boolean drags;
    private final boolean hasPage;
    private int layer;

    private static final ResourceLocation BUTTON = ResourceLocation.withDefaultNamespace("widget/button");
    private static final ResourceLocation BUTTON_LIT = ResourceLocation.withDefaultNamespace(
            "widget/button_highlighted");
    private static final ResourceLocation BUTTON_OFF = ResourceLocation.withDefaultNamespace("widget/button_disabled");
    /* Words over the preview stand in front of its blocks, which reach toward the viewer. */
    private static final float OVER_PREVIEW = 300.0F;

    /**
     * @param entry the multiblock
     * @param drags whether the viewer passes drags on, so a drag turns the preview; a click turns it otherwise
     */
    public MultiblockCard(final MultiblockEntry entry, final boolean drags) {
        this.entry = entry;
        this.drags = drags;
        this.hasPage = !entry.terminal().isEmpty() && GuideClient.hasPage(entry.terminal());
    }

    /** Draws it, the mouse at ({@code mouseX}, {@code mouseY}) from its top left. */
    public void draw(final GuiGraphics g, final double mouseX, final double mouseY) {
        final MultiblockViewPalette.Colours c = MultiblockViewPalette.get();
        final Font font = Minecraft.getInstance().font;
        final int w = MultiblockViewLayout.PREVIEW_WIDTH;
        final int h = MultiblockViewLayout.PREVIEW_HEIGHT;
        g.fill(0, 0, w, h, c.shade());
        g.fill(1, 1, w, h, c.light());
        g.fill(1, 1, w - 1, h - 1, c.ground());
        this.preview.draw(g, 1, 1, w - 2, h - 2, this.entry.preview(), this.layer);
        g.pose().pushPose();
        g.pose().translate(0.0F, 0.0F, OVER_PREVIEW);
        final int inset = MultiblockViewLayout.PREVIEW_INSET;
        final List<FormattedCharSequence> name = font.split(this.entry.name(), w - 2 * inset);
        if (!name.isEmpty()) {
            Draw.text(g, font, name.getFirst(), inset, inset, c.text());
        }
        Draw.text(g, font, GameText.component(this.drags ? MultiblockViewTexts.DRAG : MultiblockViewTexts.CLICK),
                inset, MultiblockViewLayout.HINT_Y, c.hint());
        g.pose().popPose();
        drawSide(g, font, c, mouseX, mouseY);
        Draw.text(g, font, GameText.component(MultiblockViewTexts.MATERIALS), 0,
                MultiblockViewLayout.MATERIALS_LABEL_Y, c.text());
        Draw.text(g, font, GameText.component(MultiblockViewTexts.BLOCKS.with(this.entry.preview().blocks())),
                MultiblockViewLayout.COUNT_X, MultiblockViewLayout.BLOCKS_Y, c.text());
        Draw.text(g, font, GameText.component(MultiblockViewTexts.SIZE.with(this.entry.preview().sizeX(),
                        this.entry.preview().sizeY(), this.entry.preview().sizeZ())), MultiblockViewLayout.COUNT_X,
                MultiblockViewLayout.SIZE_Y, c.text());
        final boolean shown = MultiblockShows.shownInWorld().filter(this.entry.id()::equals).isPresent();
        button(g, font, c, 0, MultiblockViewLayout.FOOT_Y, MultiblockViewLayout.FOOT_WIDTH,
                MultiblockViewLayout.FOOT_HEIGHT, shown ? MultiblockViewTexts.HIDE : MultiblockViewTexts.SHOW,
                MultiblockShows.canShowInWorld(), MultiblockViewLayout.onShow(mouseX, mouseY));
        button(g, font, c, MultiblockViewLayout.PAGE_X, MultiblockViewLayout.FOOT_Y, MultiblockViewLayout.FOOT_WIDTH,
                MultiblockViewLayout.FOOT_HEIGHT, MultiblockViewTexts.PAGE, this.hasPage,
                MultiblockViewLayout.onPage(mouseX, mouseY));
    }

    /** Answers a click at ({@code x}, {@code y}) from its top left; whether it did anything. */
    public boolean mouseClicked(final double x, final double y, final int button) {
        if (button != 0) {
            return false;
        }
        final int layers = this.entry.preview().layers();
        final int index = MultiblockViewLayout.layerButtonAt(x, y, layers);
        if (index >= 0) {
            this.layer = MultiblockViewLayout.pressed(index, this.layer, layers);
            click();
            return true;
        }
        if (MultiblockViewLayout.onShow(x, y) && MultiblockShows.canShowInWorld()) {
            click();
            MultiblockShows.toggle(this.entry.id());
            if (MultiblockShows.shownInWorld().isPresent()) {
                Minecraft.getInstance().setScreen(null);
            }
            return true;
        }
        if (MultiblockViewLayout.onPage(x, y) && this.hasPage) {
            click();
            GuideClient.openPageOf(this.entry.terminal(), Minecraft.getInstance().screen);
            return true;
        }
        if (!this.drags && MultiblockViewLayout.onPreview(x, y)) {
            this.preview.step();
            return true;
        }
        return false;
    }

    /** Turns the preview as a drag of ({@code dx}, {@code dy}) from ({@code x}, {@code y}) does; whether it did. */
    public boolean mouseDragged(final double x, final double y, final double dx, final double dy) {
        if (!this.drags || !MultiblockViewLayout.onPreview(x, y)) {
            return false;
        }
        this.preview.drag(dx, dy);
        return true;
    }

    /** What the mouse at ({@code x}, {@code y}) is told of: the button that shows it in the world, a page missing. */
    public List<Component> tooltip(final double x, final double y) {
        if (MultiblockViewLayout.onShow(x, y) && MultiblockShows.canShowInWorld()) {
            return List.of(GameText.component(MultiblockViewTexts.SHOW_HOW));
        }
        if (MultiblockViewLayout.onPage(x, y) && !this.hasPage) {
            return List.of(GameText.component(MultiblockViewTexts.NO_PAGE));
        }
        return List.of();
    }

    /* The right of the preview: the layer's buttons, the choice of a place, and how it may stand. */
    private void drawSide(final GuiGraphics g, final Font font, final MultiblockViewPalette.Colours c,
                          final double mouseX, final double mouseY) {
        final int x = MultiblockViewLayout.SIDE_X;
        Draw.text(g, font, GameText.component(MultiblockViewTexts.LAYER), x, MultiblockViewLayout.LAYER_LABEL_Y,
                c.text());
        final int layers = this.entry.preview().layers();
        for (int index = 0; index < MultiblockViewLayout.layerButtons(layers); index++) {
            final MultiblockViewLayout.Point at = MultiblockViewLayout.layerButton(index);
            final boolean lit = MultiblockViewLayout.lit(index, this.layer, layers)
                    || MultiblockViewLayout.layerButtonAt(mouseX, mouseY, layers) == index;
            button(g, font, c, at.x(), at.y(), MultiblockViewLayout.BUTTON_WIDTH, MultiblockViewLayout.BUTTON_HEIGHT,
                    layerWord(index, layers), lit);
        }
        this.entry.choice().ifPresent(choice -> {
            final Component places = choice.places() == 1 ? GameText.component(MultiblockViewTexts.ONE_PLACE)
                    : GameText.component(MultiblockViewTexts.PLACES.with(choice.places()));
            Draw.text(g, font, places, x, MultiblockViewLayout.CHOICE_LABEL_Y, c.text());
            Draw.text(g, font, GameText.component(MultiblockViewTexts.EITHER), x,
                    MultiblockViewLayout.CHOICE_LABEL_Y + MultiblockViewLayout.LINE, c.text());
        });
        final List<Component> turns = this.entry.turns();
        for (int line = 0; line < Math.min(turns.size(), MultiblockViewLayout.TURN_LINES); line++) {
            Draw.text(g, font, turns.get(line), x, MultiblockViewLayout.TURNS_Y + line * MultiblockViewLayout.LINE,
                    c.text());
        }
    }

    /* What layer button {@code index} says: "All", its layer, or for a tall multiblock the layer shown and arrows. */
    private Component layerWord(final int index, final int layers) {
        if (index == 0) {
            return GameText.component(MultiblockViewTexts.ALL);
        }
        if (!MultiblockViewLayout.stepping(layers)) {
            return Component.literal(String.valueOf(index));
        }
        return switch (index) {
            case 1 -> this.layer == 0 ? GameText.component(MultiblockViewTexts.NONE)
                    : Component.literal(String.valueOf(this.layer));
            case 2 -> GameText.component(MultiblockViewTexts.PREVIOUS);
            default -> GameText.component(MultiblockViewTexts.NEXT);
        };
    }

    private static void button(final GuiGraphics g, final Font font, final MultiblockViewPalette.Colours c,
                               final int x, final int y, final int w, final int h, final Component word,
                               final boolean lit) {
        g.blitSprite(lit ? BUTTON_LIT : BUTTON, x, y, w, h);
        Draw.text(g, font, word, x + (w - font.width(word)) / 2, y + (h - font.lineHeight) / 2 + 1, c.button());
    }

    private static void button(final GuiGraphics g, final Font font, final MultiblockViewPalette.Colours c,
                               final int x, final int y, final int w, final int h, final TextKey word,
                               final boolean active, final boolean hovered) {
        if (!active) {
            g.blitSprite(BUTTON_OFF, x, y, w, h);
            final Component text = GameText.component(word);
            Draw.text(g, font, text, x + (w - font.width(text)) / 2, y + (h - font.lineHeight) / 2 + 1,
                    c.buttonOff());
            return;
        }
        button(g, font, c, x, y, w, h, GameText.component(word), hovered);
    }

    private static void click() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }
}
