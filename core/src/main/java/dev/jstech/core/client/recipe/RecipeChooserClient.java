/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.recipe;

import com.mojang.datafixers.util.Either;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.recipe.RecipeChoice;
import dev.jstech.core.recipe.RecipeChoiceTexts;
import dev.jstech.core.recipe.RecipeChoicesPayload;
import dev.jstech.core.recipe.RecipeChooserLayout;
import dev.jstech.core.recipe.RecipeChosenPayload;
import dev.jstech.core.text.GameText;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * The chooser of recipes on a player's game: while the server says more than one recipe fits what is in the open
 * window's grid or furnace, a small upright chooser stands beside the result, an arrow up, the count and an arrow
 * down; a click shows the next recipe's result, a right-click the one before, and its tooltip names every recipe that
 * fits, the chosen one marked. Drawn on the crafting table's window, the furnaces' and any like them whose result has
 * room beside it.
 */
public final class RecipeChooserClient {

    private static final float SMALL = 0.75f;
    /** How far in front it is drawn: over the window's slots and items, under the tooltip. */
    private static final int OVER_ITEMS = 300;
    private static final int LEFT_BUTTON = 0;
    private static final int RIGHT_BUTTON = 1;
    /** What the server last said fits the open window; none when nothing is to choose. On the render thread. */
    private static @Nullable RecipeChoicesPayload offer;

    private RecipeChooserClient() {
    }

    /** Hands the buses what the chooser answers, once, from the Core's client constructor. */
    @ApiStatus.Internal
    public static void listen(final IEventBus modBus) {
        modBus.addListener(RegisterClientTooltipComponentFactoriesEvent.class,
                event -> event.register(RecipeChoiceRow.class, row -> row));
        NeoForge.EVENT_BUS.addListener(ScreenEvent.Render.Post.class, RecipeChooserClient::onRender);
        NeoForge.EVENT_BUS.addListener(ScreenEvent.Init.Post.class, event -> {
            if (event.getScreen() instanceof AbstractContainerScreen<?> screen) {
                event.addListener(new Clicks(screen));
            }
        });
        NeoForge.EVENT_BUS.addListener(ScreenEvent.Closing.class, event -> offer = null);
    }

    /** The server told what fits the open window. */
    public static void told(final RecipeChoicesPayload payload) {
        offer = payload.recipes().size() < 2 ? null : payload;
    }

    /** What the server last said fits the open window, when more than one recipe does. */
    public static Optional<RecipeChoicesPayload> offer() {
        return Optional.ofNullable(offer);
    }

    /**
     * Where the chooser stands on {@code screen}, as its left and top on the screen, when it is shown there: a
     * window the server spoke of, whose result has room beside it.
     */
    public static Optional<int[]> place(final AbstractContainerScreen<?> screen) {
        final RecipeChoicesPayload now = offer;
        if (now == null || screen.getMenu().containerId != now.containerId()) {
            return Optional.empty();
        }
        final Slot result = resultOf(screen.getMenu(), now.furnace());
        if (result == null || !RecipeChooserLayout.fits(result.x, screen.getXSize())) {
            return Optional.empty();
        }
        return Optional.of(new int[] {screen.getGuiLeft() + RecipeChooserLayout.x(result.x),
                screen.getGuiTop() + RecipeChooserLayout.y(result.y)});
    }

    /** Steps the chooser on {@code screen} to the next recipe, or the one before, and tells the server. */
    public static boolean step(final AbstractContainerScreen<?> screen, final boolean forward) {
        final RecipeChoicesPayload now = offer;
        if (now == null || place(screen).isEmpty()) {
            return false;
        }
        final int at = RecipeChoice.step(now.chosen(), now.recipes().size(), forward);
        offer = new RecipeChoicesPayload(now.containerId(), now.recipes(), now.results(), at, now.furnace(),
                now.input());
        PacketDistributor.sendToServer(new RecipeChosenPayload(now.containerId(), now.recipes().get(at)));
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
        return true;
    }

    /** The colour of a recipe's name in the tooltip: white for the chosen one, grey for the rest. */
    static int rowColour(final boolean chosen) {
        final Integer colour = (chosen ? ChatFormatting.WHITE : ChatFormatting.GRAY).getColor();
        return colour == null ? -1 : colour | (0xFF << 24);
    }

    private static void onRender(final ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen)) {
            return;
        }
        final Optional<int[]> place = place(screen);
        final RecipeChoicesPayload now = offer;
        if (place.isEmpty() || now == null) {
            return;
        }
        final GuiGraphics graphics = event.getGuiGraphics();
        final int x = place.get()[0];
        final int y = place.get()[1];
        final boolean hovered = inside(x, y, event.getMouseX(), event.getMouseY());
        draw(graphics, x, y, now.chosen() + 1, now.recipes().size(), hovered);
        if (hovered) {
            graphics.renderComponentTooltipFromElements(Minecraft.getInstance().font, tooltip(now),
                    event.getMouseX(), event.getMouseY(), ItemStack.EMPTY);
        }
    }

    private static void draw(final GuiGraphics graphics, final int x, final int y, final int shown, final int total,
                             final boolean hovered) {
        final RecipeChooserPalette.Colours colours = RecipeChooserPalette.get();
        final int right = x + RecipeChooserLayout.WIDTH;
        final int bottom = y + RecipeChooserLayout.HEIGHT;
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, OVER_ITEMS);
        graphics.fill(x, y, right, bottom, hovered ? colours.hovered() : colours.edge());
        graphics.fill(x + 1, y + 1, right - 1, bottom - 1, colours.face());
        graphics.fill(x + 1, y + 1, right - 1, y + 2, colours.light());
        graphics.fill(x + 1, y + 1, x + 2, bottom - 1, colours.light());
        graphics.fill(x + 1, bottom - 2, right - 1, bottom - 1, colours.shade());
        graphics.fill(right - 2, y + 1, right - 1, bottom - 1, colours.shade());
        final int middle = x + RecipeChooserLayout.WIDTH / 2;
        for (int row = 0; row < RecipeChooserLayout.ARROW_ROWS; row++) {
            final int up = y + RecipeChooserLayout.ARROW_TOP + row;
            final int down = bottom - RecipeChooserLayout.ARROW_BOTTOM - row;
            graphics.fill(middle - row - 1, up, middle + row + 1, up + 1, colours.ink());
            graphics.fill(middle - row - 1, down, middle + row + 1, down + 1, colours.ink());
        }
        final Font font = Minecraft.getInstance().font;
        String count = shown + "/" + total;
        if (font.width(count) * SMALL > RecipeChooserLayout.WIDTH - 2) {
            count = String.valueOf(shown);
        }
        final int width = Math.round(font.width(count) * SMALL);
        Draw.textScaled(graphics, font, count, middle - width / 2, y + RecipeChooserLayout.COUNT_Y, colours.ink(),
                SMALL);
        graphics.pose().popPose();
    }

    private static List<Either<FormattedText, TooltipComponent>> tooltip(final RecipeChoicesPayload offer) {
        final List<Either<FormattedText, TooltipComponent>> out = new ArrayList<>();
        out.add(Either.left(GameText.component(RecipeChoiceTexts.FIT.with(offer.recipes().size()))
                .withStyle(ChatFormatting.WHITE)));
        for (int i = 0; i < offer.results().size(); i++) {
            final ItemStack result = offer.results().get(i);
            out.add(Either.right(new RecipeChoiceRow(result, result.getHoverName(), i == offer.chosen())));
        }
        out.add(Either.left(GameText.component(RecipeChoiceTexts.NEXT.text()).withStyle(ChatFormatting.GRAY)));
        out.add(Either.left(GameText.component(RecipeChoiceTexts.BEFORE.text()).withStyle(ChatFormatting.GRAY)));
        out.add(Either.left((offer.furnace() ? GameText.component(RecipeChoiceTexts.KEPT_FURNACE.with(
                GameText.of(offer.input().getHoverName()))) : GameText.component(RecipeChoiceTexts.KEPT_GRID.text()))
                .withStyle(ChatFormatting.DARK_AQUA)));
        return out;
    }

    /* The result's slot of {@code menu}: a furnace's output, or a crafting grid's result. */
    private static @Nullable Slot resultOf(final AbstractContainerMenu menu, final boolean furnace) {
        if (furnace) {
            return menu instanceof AbstractFurnaceMenu ? menu.getSlot(AbstractFurnaceMenu.RESULT_SLOT) : null;
        }
        for (final Slot slot : menu.slots) {
            if (slot instanceof ResultSlot) {
                return slot;
            }
        }
        return null;
    }

    private static boolean inside(final int x, final int y, final double mouseX, final double mouseY) {
        return mouseX >= x && mouseX < x + RecipeChooserLayout.WIDTH && mouseY >= y
                && mouseY < y + RecipeChooserLayout.HEIGHT;
    }

    /*
     * The chooser's clicks, one of the window's own listeners, so a click reaches it as it reaches the window's
     * buttons, before the slots; it answers only while the chooser is shown.
     */
    private static final class Clicks implements GuiEventListener {

        private final AbstractContainerScreen<?> screen;
        private boolean focused;

        Clicks(final AbstractContainerScreen<?> screen) {
            this.screen = screen;
        }

        @Override
        public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
            return (button == LEFT_BUTTON || button == RIGHT_BUTTON) && isMouseOver(mouseX, mouseY)
                    && step(this.screen, button == LEFT_BUTTON);
        }

        @Override
        public boolean isMouseOver(final double mouseX, final double mouseY) {
            final Optional<int[]> place = place(this.screen);
            return place.isPresent() && inside(place.get()[0], place.get()[1], mouseX, mouseY);
        }

        @Override
        public void setFocused(final boolean focused) {
            this.focused = focused;
        }

        @Override
        public boolean isFocused() {
            return this.focused;
        }
    }
}
