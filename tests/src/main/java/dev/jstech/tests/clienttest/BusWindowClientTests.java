/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.block.part.AbstractBusPart;
import dev.jstech.computers.block.part.ComputingParts;
import dev.jstech.computers.bus.BusAbilities;
import dev.jstech.computers.bus.BusActivity;
import dev.jstech.computers.bus.BusCondition;
import dev.jstech.computers.bus.BusSettings;
import dev.jstech.computers.client.bus.AbstractBusScreen;
import dev.jstech.computers.gui.layout.BusLayout;
import dev.jstech.core.cable.CableBlockEntity;
import dev.jstech.core.multipart.PartType;
import dev.jstech.core.tier.HardwareEra;
import dev.jstech.tests.testkit.TestWorldBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.function.Supplier;

/**
 * The bus windows as a player sees them: a bus of each era opened on a cable beside a running Mainframe, each of its
 * three tabs shot in its era's skin, a stepper pressed and a condition written by clicks, both reaching the bus.
 */
public final class BusWindowClientTests {

    private static final int SETTLE = 4;
    private static final int SCREEN_WAIT = 120;
    private static final BlockPos MAINFRAME = new BlockPos(1, 2, 2);
    private static final BlockPos PLAYER = new BlockPos(4, 2, 5);
    private static final List<Supplier<? extends PartType<? extends AbstractBusPart>>> BUSES = List.of(
            ComputingParts.VINTAGE_IMPORT, ComputingParts.LEGACY_IMPORT, ComputingParts.TRANSITION_IMPORT,
            ComputingParts.IMPORT, ComputingParts.ADVANCED_IMPORT, ComputingParts.VINTAGE_EXPORT,
            ComputingParts.INPUT, ComputingParts.VINTAGE_EXTERNAL, ComputingParts.LEGACY_EXTERNAL,
            ComputingParts.ADVANCED_EXTERNAL);

    private BusWindowClientTests() {
    }

    @ClientTest(timeoutTicks = 3000)
    public static void busWindows_showEachErasTabsAndTakeClicks(final ClientTestContext ctx) {
        ctx.thenBuild(0, BusWindowClientTests::build)
                .thenServer(SETTLE, level -> mount(ctx, level))
                .thenTeleport(SETTLE, PLAYER, Direction.NORTH);
        final String[] names = {"vintage", "legacy", "transition", "standard", "advanced", "vintage-export",
                "crafting-input", "vintage-external", "legacy-external", "advanced-external"};
        for (int i = 0; i < BUSES.size(); i++) {
            final BlockPos cable = cableOf(i);
            final String name = names[i];
            open(ctx, cable)
                    .thenAssert(0, () -> screen(ctx).getMenu().state().linked(),
                            "the " + name + " bus's lamp tells it reaches the network")
                    .thenScreenshot(SETTLE, name + "-configure")
                    .then(SETTLE, () -> clickTab(ctx, BusLayout.TAB_ACTIVITY))
                    .thenAssert(SETTLE, () -> screen(ctx).tab() == BusLayout.TAB_ACTIVITY, "the Activity tab opens")
                    .thenScreenshot(SETTLE, name + "-activity")
                    .then(SETTLE, () -> clickTab(ctx, BusLayout.TAB_SOFTWARE))
                    .thenScreenshot(SETTLE, name + "-software")
                    .then(SETTLE, () -> clickTab(ctx, BusLayout.TAB_CONFIGURE))
                    .then(SETTLE, () -> ctx.key(GLFW.GLFW_KEY_ESCAPE))
                    .thenAwaitNoScreen(SCREEN_WAIT);
        }

        // The Legacy keep's plus, pressed in the window, raises the keep a program set and takes it back by hand.
        final BusLayout.Row keep = row(HardwareEra.LEGACY, BusLayout.Kind.KEEP, 0);
        open(ctx, cableOf(1))
                .then(SETTLE, () -> ctx.clickGui(BusLayout.PLUS_X + 4, BusLayout.CONFIGURE_VIEW_Y + keep.y() + 4))
                .thenWaitUntilServer(level -> bus(ctx, level, 1).keep() == 17
                                && bus(ctx, level, 1).setBy(BusSettings.KEEP).isEmpty(), SCREEN_WAIT,
                        "the Legacy bus to keep 17, by hand", level -> "keep " + bus(ctx, level, 1).keep()
                                + " set by " + bus(ctx, level, 1).setBy(BusSettings.KEEP))
                .then(SETTLE, () -> ctx.key(GLFW.GLFW_KEY_ESCAPE))
                .thenAwaitNoScreen(SCREEN_WAIT);

        // The Legacy External Storage Bus made read only by a click on its access.
        final BusLayout.Row access = BusLayout.rows(new BusLayout.Shape(BusAbilities.external(HardwareEra.LEGACY),
                BusLayout.Window.EXTERNAL, 2, 0, 1, false, 1, 0, false, 2)).stream()
                .filter(r -> r.kind() == BusLayout.Kind.ACCESS).findFirst().orElseThrow();
        open(ctx, cableOf(8))
                .then(SETTLE, () -> ctx.clickGui(120, BusLayout.CONFIGURE_VIEW_Y + access.y() + 4))
                .thenWaitUntilServer(level -> bus(ctx, level, 8).access() == BusSettings.READ_ONLY, SCREEN_WAIT,
                        "the Legacy External Storage Bus to be read only", level -> "access "
                                + bus(ctx, level, 8).access())
                .thenScreenshot(SETTLE, "legacy-external-read-only")
                .then(SETTLE, () -> ctx.key(GLFW.GLFW_KEY_ESCAPE))
                .thenAwaitNoScreen(SCREEN_WAIT);

        // A condition written by clicks on the Standard bus: scroll down, "+ condition", HOURS, ADD.
        open(ctx, cableOf(3))
                .then(SETTLE, () -> {
                    for (int i = 0; i < 6; i++) {
                        ctx.scrollGui(40, BusLayout.CONFIGURE_VIEW_Y + 10, -1.0);
                    }
                })
                .then(SETTLE, () -> clickRow(ctx, BusLayout.Kind.ADD_CONDITION, 40, 4))
                .then(SETTLE, () -> {
                    for (int i = 0; i < 6; i++) {
                        ctx.scrollGui(40, BusLayout.CONFIGURE_VIEW_Y + 10, -1.0);
                    }
                })
                .thenScreenshot(SETTLE, "standard-writing-a-condition")
                .then(SETTLE, () -> clickRow(ctx, BusLayout.Kind.CONDITION_EDITOR, 50, 4))
                .then(SETTLE, () -> clickRow(ctx, BusLayout.Kind.CONDITION_EDITOR, BusLayout.EDIT_ADD_X + 4,
                        2 * BusLayout.ROW + 4))
                .thenWaitUntilServer(level -> bus(ctx, level, 3).conditions().contains(BusCondition.hours(18, 6)),
                        SCREEN_WAIT, "the Standard bus to wait for the hours from 18:00 to 06:00",
                        level -> "conditions " + bus(ctx, level, 3).conditions())
                .thenScreenshot(SETTLE, "standard-with-the-hours")
                .then(SETTLE, () -> ctx.key(GLFW.GLFW_KEY_ESCAPE))
                .thenAwaitNoScreen(SCREEN_WAIT);
    }

    /* A running Mainframe with a line of cables east of it, a chest south of each. */
    private static void build(final TestWorldBuilder world) {
        world.placeRunningMainframe(MAINFRAME);
        for (int i = 0; i < BUSES.size(); i++) {
            world.setBlock(cableOf(i), ComputingModule.HBW_CABLE);
            world.setBlock(cableOf(i).south(), Blocks.CHEST);
        }
    }

    /*
     * A bus of each era on its cable's south face, named, with a filter, a log, and a program's marks, so every tab
     * has something to show.
     */
    private static void mount(final ClientTestContext ctx, final ServerLevel level) {
        for (int i = 0; i < BUSES.size(); i++) {
            if (!(level.getBlockEntity(ctx.abs(cableOf(i))) instanceof CableBlockEntity cable)) {
                continue;
            }
            final AbstractBusPart bus = BUSES.get(i).get().create();
            cable.addPart(Direction.SOUTH, bus);
            bus.setName("Ore in");
            bus.setFilterSlot(0, new ItemStack(Items.IRON_ORE), "");
            bus.setFilterSlot(1, new ItemStack(Items.COAL), "");
            bus.setItemQuantities(0, 16, 64, "");
            bus.setKeep(16, "Stock keeper");
            bus.setMax(64, "");
            bus.setPriority(5, "Night shift");
            bus.addCondition(BusCondition.stock("minecraft:iron_ore", 512), "");
            bus.addTag(ResourceLocation.parse("c:ores"), "");
            bus.setFuzzy(true, "");
            bus.activity().moved(level.getGameTime() - 200L, "item|minecraft:iron_ore", 64L, false);
            bus.activity().moved(level.getGameTime() - 120L, "item|minecraft:coal", 12L, true);
            bus.activity().held(level.getGameTime() - 60L, "item|minecraft:iron_ore", BusActivity.KEEPS, 16L);
            bus.activity().held(level.getGameTime() - 20L, "item|minecraft:iron_ore", BusActivity.FULL, 0L);
        }
    }

    private static ClientTestContext open(final ClientTestContext ctx, final BlockPos cable) {
        return ctx.thenServer(SETTLE, level -> {
                    if (level.getBlockEntity(ctx.abs(cable)) instanceof CableBlockEntity c
                            && c.getPart(Direction.SOUTH) instanceof AbstractBusPart bus) {
                        bus.use(ctx.serverPlayer());
                    }
                })
                .thenAwaitScreen(AbstractBusScreen.class, SCREEN_WAIT)
                .thenWaitUntil(() -> screen(ctx).getMenu().state() != null, SCREEN_WAIT, "the bus's state to arrive");
    }

    private static void clickTab(final ClientTestContext ctx, final int tab) {
        ctx.clickGui(BusLayout.tabX(tab) + 4, BusLayout.TAB_Y + 4);
    }

    /* Clicks at (x, dy) inside the first row of that kind of the Standard window, where it is scrolled to now. */
    private static void clickRow(final ClientTestContext ctx, final BusLayout.Kind kind, final int x, final int dy) {
        final AbstractBusScreen<?> screen = screen(ctx);
        final BusLayout.Row row = BusLayout.rows(shape(HardwareEra.STANDARD, screen.getMenu().settings().conditions()
                .size(), kind == BusLayout.Kind.CONDITION_EDITOR)).stream().filter(r -> r.kind() == kind)
                .findFirst().orElseThrow();
        final int content = BusLayout.contentHeight(shape(HardwareEra.STANDARD,
                screen.getMenu().settings().conditions().size(), kind == BusLayout.Kind.CONDITION_EDITOR));
        final int scroll = Math.max(0, content - BusLayout.configureView(BusAbilities.of(HardwareEra.STANDARD),
                BusLayout.Window.MOVER));
        ctx.clickGui(x, BusLayout.CONFIGURE_VIEW_Y + row.y() - scroll + dy);
    }

    @Nullable
    private static BusLayout.Row row(final HardwareEra era, final BusLayout.Kind kind, final int index) {
        return BusLayout.rows(shape(era, 1, false)).stream().filter(r -> r.kind() == kind && r.index() == index)
                .findFirst().orElse(null);
    }

    private static BusLayout.Shape shape(final HardwareEra era, final int conditions, final boolean editing) {
        return new BusLayout.Shape(BusAbilities.of(era), BusLayout.Window.MOVER, 2, 2, 1, false, 1, conditions,
                editing, 1);
    }

    private static AbstractBusScreen<?> screen(final ClientTestContext ctx) {
        return ctx.screen(AbstractBusScreen.class);
    }

    private static AbstractBusPart bus(final ClientTestContext ctx, final ServerLevel level, final int index) {
        return level.getBlockEntity(ctx.abs(cableOf(index))) instanceof CableBlockEntity c
                && c.getPart(Direction.SOUTH) instanceof AbstractBusPart bus ? bus : null;
    }

    private static BlockPos cableOf(final int index) {
        return MAINFRAME.east(1 + index);
    }
}
