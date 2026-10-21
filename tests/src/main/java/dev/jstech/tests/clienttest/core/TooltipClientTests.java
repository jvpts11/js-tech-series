/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.core;

import com.mojang.datafixers.util.Either;
import dev.jstech.core.client.gui.tooltip.ItemTooltips;
import dev.jstech.core.client.gui.tooltip.TooltipTexts;
import dev.jstech.core.energy.ItemEnergyStorage;
import dev.jstech.core.gui.tooltip.TooltipBar;
import dev.jstech.core.gui.tooltip.TooltipContents;
import dev.jstech.core.gui.tooltip.TooltipPicture;
import dev.jstech.core.gui.tooltip.TooltipPreview;
import dev.jstech.core.item.ItemStates;
import dev.jstech.core.text.GameText;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestItems;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.SimpleFluidContent;

/**
 * Tooltips on a player's game: each mod's lines together, the item's own mod first, the parts that wait for Shift
 * and Ctrl shown when they are held and a hint while none is; drawn, the pieces take their places (bars, what an item
 * holds, a 3D preview, a picture) and the item's mod is named last; and the boards of all of them, to be judged by eye.
 */
public final class TooltipClientTests {

    private static final int ENERGY = 5_500;
    private static final int WATER = 3_200;
    private static final int IRON = 64;
    private static final int MORE_IRON = 30;
    private static final int GOLD = 5;
    private static final int WORN = 600;
    private static final int SETTLE = 5;

    private TooltipClientTests() {
    }

    @ClientTest(timeoutTicks = 200)
    public static void sections_keepEachModTogetherAndWaitForTheirKeys(final ClientTestContext ctx) {
        ctx.then(0, () -> {
            final ItemStack full = full();
            final List<String> plain = strings(lines(ctx, full, false, false));
            final int made = plain.indexOf(GameText.resolve(TestTooltips.MADE.text()));
            final int energy = indexStarting(plain, "Energy:");
            ctx.assertTrue(made >= 0 && energy > made,
                    "the test mod's own line comes before the Core's, the item being the test mod's: " + plain);
            ctx.assertTrue(plain.contains(GameText.resolve(TooltipTexts.MORE_BOTH.text())),
                    "with neither key held, the hint names both: " + plain);
            ctx.assertTrue(plain.contains("Holds 3 of 4 stacks"), "how many stacks it holds: " + plain);
            final List<String> shifted = strings(lines(ctx, full, true, false));
            ctx.assertTrue(shifted.contains("94 Iron Ingot") && shifted.contains("5 Gold Ingot")
                    && !shifted.contains(GameText.resolve(TooltipTexts.MORE_BOTH.text())),
                    "with Shift, the whole list of what it holds, each kind once, and no hint: " + shifted);
            final List<String> numbers = strings(lines(ctx, full, false, true));
            ctx.assertTrue(indexStarting(numbers, "Charges at up to") >= 0
                    && indexStarting(numbers, "Gives up to") >= 0 && !numbers.contains("94 Iron Ingot"),
                    "with Ctrl, how fast it charges and gives, and not the list: " + numbers);
            final List<String> ingot = strings(lines(ctx, new ItemStack(Items.IRON_INGOT), false, false));
            ctx.assertTrue(ingot.contains("Symbol: Fe")
                    && ingot.contains(GameText.resolve(TooltipTexts.MORE.text())),
                    "the game's own iron ingot shows its material and that Shift shows more: " + ingot);
        });
    }

    @ClientTest(timeoutTicks = 200)
    public static void drawn_putsThePiecesInTheirPlacesAndNamesTheModLast(final ClientTestContext ctx) {
        ctx.then(0, () -> {
            final List<Either<FormattedText, TooltipComponent>> full = gathered(ctx, full());
            ctx.assertTrue(full.stream().anyMatch(part -> part.right().filter(TooltipBar.class::isInstance)
                    .isPresent()), "the energy and the water are bars");
            ctx.assertTrue(full.stream().anyMatch(part -> part.right().filter(TooltipContents.class::isInstance)
                    .isPresent()), "what it holds is in slots");
            ctx.assertTrue(last(full).equals(ItemTooltips.modName(JsTests.MODID)),
                    "the test mod named last: " + last(full));
            final List<Either<FormattedText, TooltipComponent>> ingot = gathered(ctx,
                    new ItemStack(Items.IRON_INGOT));
            ctx.assertTrue(last(ingot).equals("Minecraft"), "the game's own item named as the game's: "
                    + last(ingot));
            final List<Either<FormattedText, TooltipComponent>> bricks = gathered(ctx,
                    TestTooltips.marked(new ItemStack(Items.BRICKS), TestTooltips.PREVIEW));
            ctx.assertTrue(bricks.stream().anyMatch(part -> part.right().filter(TooltipPreview.class::isInstance)
                    .isPresent()), "the furnace's 3D preview");
            final List<Either<FormattedText, TooltipComponent>> painting = gathered(ctx,
                    TestTooltips.marked(new ItemStack(Items.PAINTING), TestTooltips.PICTURE));
            ctx.assertTrue(painting.stream().anyMatch(part -> part.right().filter(TooltipPicture.class::isInstance)
                    .isPresent()), "the painting's picture");
        });
    }

    @ClientTest(timeoutTicks = 400)
    public static void boards_showEveryPieceAsTheGameDrawsIt(final ClientTestContext ctx) {
        ctx.afterTest(() -> ctx.mc().setScreen(null));
        final ItemStack worn = new ItemStack(Items.DIAMOND_PICKAXE);
        worn.setDamageValue(WORN);
        ctx.then(0, () -> ctx.mc().setScreen(new Board(full(), withKeys(ctx, full(), false, false))))
                .thenScreenshot(SETTLE, "tooltip-full-tool")
                .then(0, () -> ctx.mc().setScreen(new Board(full(), withKeys(ctx, full(), true, false))))
                .thenScreenshot(SETTLE, "tooltip-full-tool-shift")
                .then(0, () -> ctx.mc().setScreen(new Board(full(), withKeys(ctx, full(), false, true))))
                .thenScreenshot(SETTLE, "tooltip-full-tool-ctrl")
                .then(0, () -> ctx.mc().setScreen(Board.of(ctx, new ItemStack(Items.IRON_INGOT))))
                .thenScreenshot(SETTLE, "tooltip-iron-ingot")
                .then(0, () -> ctx.mc().setScreen(Board.of(ctx, worn)))
                .thenScreenshot(SETTLE, "tooltip-worn-pickaxe")
                .then(0, () -> ctx.mc().setScreen(Board.of(ctx, TestTooltips.marked(new ItemStack(Items.BRICKS),
                        TestTooltips.PREVIEW))))
                .thenScreenshot(SETTLE, "tooltip-preview")
                .then(0, () -> ctx.mc().setScreen(Board.of(ctx, TestTooltips.marked(new ItemStack(Items.PAINTING),
                        TestTooltips.PICTURE))))
                .thenScreenshot(SETTLE, "tooltip-picture");
    }

    /* The full test tool holding energy, water and three stacks of two kinds. */
    private static ItemStack full() {
        final ItemStack stack = new ItemStack(TestItems.FULL.get());
        final ItemEnergyStorage energy = new ItemEnergyStorage(stack, TestItems.ENERGY, TestItems.ENERGY_IN,
                TestItems.ENERGY_OUT);
        // It takes no more than its rate at a time.
        while (energy.stored() < ENERGY) {
            energy.receiveEnergy(ENERGY - (int) energy.stored(), false);
        }
        stack.set(ItemStates.FLUID.get(), SimpleFluidContent.copyOf(new FluidStack(Fluids.WATER, WATER)));
        stack.set(ItemStates.INVENTORY.get(), ItemContainerContents.fromItems(List.of(
                new ItemStack(Items.IRON_INGOT, IRON), new ItemStack(Items.IRON_INGOT, MORE_IRON),
                new ItemStack(Items.GOLD_INGOT, GOLD))));
        return stack;
    }

    /* The item's name and what the sections add with the keys held as said, as the tooltip shows them then. */
    private static List<Component> withKeys(final ClientTestContext ctx, final ItemStack stack, final boolean shift,
                                            final boolean ctrl) {
        final List<Component> said = new ArrayList<>();
        said.add(stack.getHoverName());
        said.addAll(lines(ctx, stack, shift, ctrl));
        return said;
    }

    private static List<Component> lines(final ClientTestContext ctx, final ItemStack stack, final boolean shift,
                                         final boolean ctrl) {
        return ItemTooltips.lines(stack, Item.TooltipContext.of(ctx.mc().level), ctx.player(), TooltipFlag.NORMAL,
                shift, ctrl);
    }

    /* The name, the game's lines and the sections, as the game gathers them to be drawn. */
    private static List<Either<FormattedText, TooltipComponent>> gathered(final ClientTestContext ctx,
                                                                          final ItemStack stack) {
        final List<Either<FormattedText, TooltipComponent>> elements = new ArrayList<>();
        Screen.getTooltipFromItem(ctx.mc(), stack).forEach(line -> elements.add(Either.left(line)));
        final RenderTooltipEvent.GatherComponents event = new RenderTooltipEvent.GatherComponents(stack,
                ctx.mc().getWindow().getGuiScaledWidth(), ctx.mc().getWindow().getGuiScaledHeight(), elements, -1);
        NeoForge.EVENT_BUS.post(event);
        return event.getTooltipElements();
    }

    private static String last(final List<Either<FormattedText, TooltipComponent>> elements) {
        return elements.get(elements.size() - 1).left().map(FormattedText::getString).orElse("");
    }

    private static int indexStarting(final List<String> lines, final String start) {
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).startsWith(start)) {
                return i;
            }
        }
        return -1;
    }

    private static List<String> strings(final List<Component> lines) {
        final List<String> out = new ArrayList<>();
        lines.forEach(line -> out.add(line.getString()));
        return out;
    }

    /* One tooltip on a plain ground, as the game draws it by the pointer. */
    private static final class Board extends Screen {

        private final ItemStack stack;
        private final List<Component> said;

        private static final int GROUND = 0xFF << 24 | 0x30 << 16 | 0x34 << 8 | 0x3A;
        /* Where the pointer stands; the game draws the tooltip a little right of it and up. */
        private static final int POINTER_X = 24;
        private static final int POINTER_Y = 40;

        private Board(final ItemStack stack, final List<Component> said) {
            super(Component.empty());
            this.stack = stack;
            this.said = List.copyOf(said);
        }

        /* The whole tooltip the game shows for {@code stack} with no key held. */
        static Board of(final ClientTestContext ctx, final ItemStack stack) {
            return new Board(stack, Screen.getTooltipFromItem(ctx.mc(), stack));
        }

        @Override
        public void render(final GuiGraphics g, final int mouseX, final int mouseY, final float partialTick) {
            g.fill(0, 0, width, height, GROUND);
            g.renderComponentTooltip(font, said, POINTER_X, POINTER_Y, stack);
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }
    }
}
