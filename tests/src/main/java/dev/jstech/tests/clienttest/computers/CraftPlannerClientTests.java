/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.computers;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.JsComputers;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.blockentity.PrinterBlockEntity;
import dev.jstech.computers.client.os.CraftPlannerApp;
import dev.jstech.computers.client.os.DesktopScreen;
import dev.jstech.computers.client.printer.PrintedPaperScreen;
import dev.jstech.computers.program.Programs;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import dev.jstech.tests.testkit.computers.CraftFiles;
import dev.jstech.tests.testkit.computers.CraftingFixtures;
import dev.jstech.tests.testkit.computers.TestWorldBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

/**
 * The Craft Planner on Frames 10 with a plan the network falls one log short of
 * (computers/docs/design/autocrafting.md): sixty-four oak planks want sixteen logs and the network holds fifteen, so the
 * plan shows the red Missing pill, names the log that is short and keeps Craft greyed; Print sends the shopping list to
 * a laser printer beside the computer, and the sheet that comes out reads the stages, the ingredients and, on a grey
 * band, what is missing.
 */
public final class CraftPlannerClientTests {

    private static final int SETTLE = 4;
    private static final int SCREEN_WAIT = 200;
    private static final int BOOT_WAIT = 1_200;
    private static final int PRINT_WAIT = 600;
    private static final BlockPos COMPUTER = new BlockPos(5, 2, 3);
    private static final BlockPos COMPUTER_CABLE = new BlockPos(4, 2, 3);
    private static final BlockPos MONITOR = new BlockPos(6, 2, 3);
    private static final BlockPos PRINTER = new BlockPos(5, 2, 4);
    private static final BlockPos PLAYER_AT_MONITOR = new BlockPos(8, 2, 3);
    private static final ResourceLocation FRAMES_10 = ResourceLocation.fromNamespaceAndPath(JsComputers.MODID,
            "frames_10");
    private static final String PLANNER = Programs.CRAFT_PLANNER.toString();
    private static final String PLANKS = "Oak Planks";
    /* The name the printed list says it came from. */
    private static final String COMPUTER_NAME = "Office PC";
    /* A log short of sixty-four planks: four planks a log, so sixteen logs wanted. */
    private static final int LOGS = 15;
    /* From one plank to sixty-four: fifteen steps of one, then six of eight. */
    private static final int PLUS_CLICKS = 21;

    private CraftPlannerClientTests() {
    }

    @ClientTest(timeoutTicks = 4000)
    public static void planner_showsWhatIsMissingAndPrintsTheList(final ClientTestContext ctx) {
        ctx.thenBuild(0, world -> {
                    final TestWorldBuilder.CraftingNetwork net = world.buildCraftingNetwork();
                    net.cc().loadPattern(CraftFiles.oakPlanks());
                    net.cc().loadPattern(CraftingFixtures.sticksPattern());
                    net.cc().loadPattern(CraftingFixtures.nuggetsToIngot());
                    net.seed(Items.OAK_LOG, LOGS);
                    world.setBlock(COMPUTER_CABLE, ComputingModule.ETHERNET_CABLE);
                    final PersonalComputerBlockEntity pc = world.placeRunningPersonalComputer(COMPUTER, FRAMES_10);
                    pc.console().install(PLANNER);
                    pc.console().setComputerName(COMPUTER_NAME);
                    world.placeMonitor(MONITOR, Direction.EAST);
                    world.setBlock(PRINTER, ComputingModule.PRINTER.get());
                })
                .thenServer(SETTLE, level -> printer(ctx, level).paper().setStackInSlot(0, new ItemStack(Items.PAPER,
                        16)))
                .thenTeleport(SETTLE, PLAYER_AT_MONITOR, Direction.WEST)
                .thenRightClick(SETTLE, MONITOR)
                .thenAwaitScreen(DesktopScreen.class, BOOT_WAIT)
                .then(SETTLE * 4, () -> DesktopScreen.requestOpen(PLANNER))
                .thenWaitUntil(() -> planner(ctx) != null && planner(ctx).catalogRowPoint(PLANKS) != null,
                        SCREEN_WAIT, "the Craft Planner to list oak planks")
                .then(SETTLE, () -> ctx.clickDesktop(planner(ctx).catalogRowPoint(PLANKS)))
                .thenWaitUntil(() -> planner(ctx).planStages() > 0, SCREEN_WAIT, "the plan of one plank");
        for (int i = 0; i < PLUS_CLICKS; i++) {
            ctx.then(1, () -> ctx.clickDesktop(planner(ctx).plusPoint()));
        }
        ctx.thenWaitUntil(() -> planner(ctx).planStages() > 0, SCREEN_WAIT, "the plan of sixty-four planks")
                .thenAssert(SETTLE, () -> !planner(ctx).planFeasible(), "the network is a log short of the plan")
                .thenAssert(0, () -> planner(ctx).shortIngredients() == 1, "the plan names the one ingredient short")
                .then(SETTLE * 4, () -> ctx.mc().getToasts().clear())
                .thenScreenshot(SETTLE, "craft-planner-missing")
                .then(SETTLE, () -> ctx.clickDesktop(planner(ctx).printPoint()))
                .thenWaitUntil(() -> planner(ctx).printDialog().isOpen()
                                && !planner(ctx).printDialog().printerNames().isEmpty(), SCREEN_WAIT,
                        "the Print window to open and list the printer")
                .then(SETTLE, () -> planner(ctx).printDialog().confirm())
                .thenWaitUntilServer(level -> !printer(ctx, level).output().getStackInSlot(0).isEmpty(), PRINT_WAIT,
                        "the list to come out of the printer", level -> "queue " + printer(ctx, level).jobs().size())
                .then(SETTLE, () -> leave(ctx))
                .thenAwaitNoScreen(SCREEN_WAIT)
                .thenServer(SETTLE, level -> ctx.hold(printer(ctx, level).output().getStackInSlot(0).copy()))
                .then(SETTLE, () -> ctx.mc().gameMode.useItem(ctx.player(), InteractionHand.MAIN_HAND))
                .thenAwaitScreen(PrintedPaperScreen.class, SCREEN_WAIT)
                .thenScreenshot(SETTLE, "craft-plan-sheet")
                .then(SETTLE, () -> ctx.key(GLFW.GLFW_KEY_ESCAPE))
                .thenAwaitNoScreen(SCREEN_WAIT)
                .thenServer(0, level -> ctx.serverPlayer().getInventory().clearContent());
    }

    private static PrinterBlockEntity printer(final ClientTestContext ctx, final ServerLevel level) {
        return (PrinterBlockEntity) level.getBlockEntity(ctx.abs(PRINTER));
    }

    private static CraftPlannerApp planner(final ClientTestContext ctx) {
        return DesktopSteps.app(ctx, PLANNER, CraftPlannerApp.class);
    }

    /* Out of the desktop: a press of Escape may first close a window, so as many as it takes. */
    private static void leave(final ClientTestContext ctx) {
        for (int i = 0; i < 3 && ctx.mc().screen != null; i++) {
            ctx.key(GLFW.GLFW_KEY_ESCAPE);
        }
    }
}
