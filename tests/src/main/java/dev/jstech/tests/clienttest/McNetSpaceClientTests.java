/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.blockentity.CraftingComputerBlockEntity;
import dev.jstech.computers.client.ComputerTerminalScreen;
import dev.jstech.computers.client.NetTerminalScreen;
import dev.jstech.computers.gui.layout.ComputerTerminalLayout;
import dev.jstech.computers.menu.ComputerTerminalMenu;
import dev.jstech.tests.testkit.TestWorldBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

/**
 * The operating space a network machine draws, met the way a player meets it: by looking at the monitor.
 *
 * <p>What is being held to account is the thing that was decided about MC-NET. Its interface is the whole
 * glass and not a window inside it; the rail is built from what the machine is, so a machine that can be
 * taught a recipe shows Patterns and one that cannot does not; the prompt is a heading inside the space and
 * no longer a separate window thrown over it; and the space is a package, so a machine with it taken off
 * comes up at a bare prompt and says how to put one back.
 */
public final class McNetSpaceClientTests {

    private McNetSpaceClientTests() {
    }

    private static final int SETTLE = 4;
    private static final int SCREEN_WAIT = 200;
    private static final int BOOT_WAIT = 400;

    private static final BlockPos COMPUTER = new BlockPos(5, 2, 2);
    private static final BlockPos MONITOR = new BlockPos(6, 2, 2);
    private static final BlockPos PLAYER_AT_MONITOR = new BlockPos(8, 2, 2);

    /*
     * The middle of the panel's Get button, worked out from the layout the screen itself draws from. It
     * used to be the same numbers written out again here, and they went stale the moment the button moved.
     */
    private static final int PANE_GET_X =
            ComputerTerminalLayout.PANE_GET_X + ComputerTerminalLayout.PANE_BTN_W / 2;
    private static final int PANE_BTN_Y =
            ComputerTerminalLayout.PANE_BTN_Y + ComputerTerminalLayout.PANE_BTN_H / 2;

    private static final ResourceLocation MC_NET =
            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "mc_net");
    private static final ResourceLocation INTERACTOR =
            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "interactor");

    private static ComputerTerminalScreen space(final ClientTestContext ctx) {
        return ctx.screen(ComputerTerminalScreen.class);
    }

    private static CraftingComputerBlockEntity machine(final ClientTestContext ctx, final ServerLevel level) {
        return TestWorldBuilder.at(level, ctx.origin())
                .blockEntity(COMPUTER, CraftingComputerBlockEntity.class);
    }

    /** A Crafting Computer running the network system, with a monitor a player is standing at. */
    private static ClientTestContext atTheMachine(final ClientTestContext ctx) {
        return ctx.thenBuild(0, builder -> {
                    final CraftingComputerBlockEntity computer = builder.placeRunningCraftingComputer(COMPUTER);
                    /*
                     * The desktop system it is built with is formatted away first, so what comes up is the
                     * network system and not two systems on one disk with the older one booting.
                     */
                    computer.togglePower();
                    computer.formatDisk(0);
                    computer.installOs(MC_NET);
                    computer.togglePower();
                    builder.placeMonitor(MONITOR, Direction.EAST);
                })
                .thenTeleport(SETTLE, PLAYER_AT_MONITOR, Direction.WEST)
                .thenRightClick(SETTLE, MONITOR);
    }

    /**
     * The space is the whole glass, with the headings this machine offers and the player's own rows on it.
     *
     * <p>A Crafting Computer with a Crafting Card, so Patterns is among them: the heading exists under the
     * same condition the Crafting Manager installs under, and that machine is the one the whole heading was
     * added for.
     */
    @ClientTest(timeoutTicks = 1800)
    public static void theSpace_fillsTheGlassAndOffersWhatTheMachineCanDo(final ClientTestContext ctx) {
        atTheMachine(ctx)
                .thenAwaitScreen(ComputerTerminalScreen.class, BOOT_WAIT)
                .then(2, () -> {
                    ctx.assertEquals(ComputerTerminalLayout.WIDTH, space(ctx).getXSize(),
                            "the space is the width of the glass");
                    ctx.assertEquals(ComputerTerminalLayout.HEIGHT, space(ctx).getYSize(),
                            "and its height");
                })
                .then(1, () -> {
                    final var headings = space(ctx).headings();
                    ctx.assertTrue(headings.contains("Network") && headings.contains("Console"),
                            "every machine offers the network and its own prompt; got " + headings);
                    ctx.assertTrue(headings.contains("Patterns"),
                            "a machine that can be taught a recipe offers Patterns; got " + headings);
                    ctx.assertTrue(!headings.contains("Tasks") && !headings.contains("Upkeep"),
                            "and the orchestrator's headings belong to a Mainframe; got " + headings);
                })
                .then(1, () -> ctx.clickGui(space(ctx).headingPoint("Patterns")[0],
                        space(ctx).headingPoint("Patterns")[1]))
                .thenWaitUntil(() -> space(ctx).activeHeading() == ComputerTerminalMenu.TAB_PATTERNS,
                        SCREEN_WAIT, "the Patterns heading to be the one showing")
                .thenScreenshot(2, "mcnet_space_patterns")
                .then(1, () -> ctx.clickGui(space(ctx).headingPoint("Local")[0],
                        space(ctx).headingPoint("Local")[1]))
                .thenWaitUntil(() -> space(ctx).activeHeading() == ComputerTerminalMenu.TAB_LOCAL,
                        SCREEN_WAIT, "clicking a heading to switch to it")
                .thenScreenshot(2, "mcnet_space_local");
    }

    /**
     * The prompt is a heading inside the space: clicking it changes what is shown and opens nothing.
     *
     * <p>It used to launch the Command Prompt, which threw a window over the screen. The machine is asked a
     * question at it afterwards, because a prompt that is drawn and does not answer is not a prompt.
     */
    @ClientTest(timeoutTicks = 1800)
    public static void theConsole_isAHeadingAndNotAWindow(final ClientTestContext ctx) {
        atTheMachine(ctx)
                .thenAwaitScreen(ComputerTerminalScreen.class, BOOT_WAIT)
                .then(2, () -> ctx.clickGui(space(ctx).headingPoint("Console")[0],
                        space(ctx).headingPoint("Console")[1]))
                .thenWaitUntil(() -> space(ctx).activeHeading() == ComputerTerminalMenu.TAB_CONSOLE,
                        SCREEN_WAIT, "the Console heading to be the one showing")
                .then(2, () -> ctx.assertTrue(ctx.mc().screen instanceof ComputerTerminalScreen,
                        "and nothing was opened over the space; got " + ctx.mc().screen))
                .thenWaitUntil(() -> space(ctx).consoleText().contains("MC-NET"), SCREEN_WAIT,
                        "the prompt to greet in the system's own name")
                .then(1, () -> ctx.assertTrue(space(ctx).consoleText().contains("showcommands"),
                        "and to say where to start in this system's word for it; got "
                                + space(ctx).consoleText()))
                .then(1, () -> ctx.type("listfiles"))
                .then(1, () -> ctx.key(GLFW.GLFW_KEY_ENTER))
                .thenWaitUntil(() -> space(ctx).consoleText().contains("netstart.sys"), SCREEN_WAIT,
                        "the machine to answer its own prompt with what is on its disk")
                /*
                 * The inventory key is a letter at a prompt and nothing else. It closed the whole screen,
                 * because a key the prompt had no use for fell through to the game behind it.
                 */
                .then(2, () -> ctx.key(GLFW.GLFW_KEY_E))
                .then(2, () -> ctx.assertTrue(ctx.mc().screen instanceof ComputerTerminalScreen,
                        "the inventory key is a letter at a prompt; got " + ctx.mc().screen))
                .thenScreenshot(2, "mcnet_space_console");
    }

    /**
     * Asking the network for a thing opens the question, and the question draws.
     *
     * <p>It threw the moment it was opened: the field a quantity is typed into was built with the screen's
     * font before the screen had one, and the first thing that measured a string in it died.
     */
    @ClientTest(timeoutTicks = 2400)
    public static void askingForAThing_opensTheQuestionAndDrawsIt(final ClientTestContext ctx) {
        ctx.thenBuild(0, world -> {
                    final TestWorldBuilder.CraftingNetwork net = world.buildCraftingNetwork();
                    net.cc().togglePower();
                    net.cc().formatDisk(0);
                    net.cc().installOs(MC_NET);
                    net.cc().togglePower();
                    net.seed(Items.IRON_INGOT, 64);
                    world.placeMonitor(MONITOR, Direction.EAST);
                })
                .thenTeleport(SETTLE, PLAYER_AT_MONITOR, Direction.WEST)
                .thenRightClick(SETTLE, MONITOR)
                .thenAwaitScreen(ComputerTerminalScreen.class, BOOT_WAIT)
                .then(2, () -> ctx.clickGui(space(ctx).headingPoint("Network")[0],
                        space(ctx).headingPoint("Network")[1]))
                .thenWaitUntil(() -> space(ctx).activeHeading() == ComputerTerminalMenu.TAB_NETWORK,
                        SCREEN_WAIT, "the Network heading to be the one showing")
                // The first cell of the grid, which is where the one seeded kind lands.
                .then(2, () -> ctx.clickGui(ComputerTerminalLayout.GRID_X + 8,
                        ComputerTerminalLayout.GRID_Y + 8))
                .then(2, () -> ctx.clickGui(PANE_GET_X, PANE_BTN_Y))
                .thenWaitUntil(() -> space(ctx).requestOpen(), SCREEN_WAIT,
                        "the question about taking it out of the network")
                .thenScreenshot(4, "mcnet_space_request");
    }

    /**
     * Take the space off and the machine is a prompt, which says what is missing and how to put one back.
     *
     * <p>The same relationship a Linux has with its desktop. What opens is the bare glass rather than the
     * space, because what a monitor opens is decided by the space installed on the machine.
     */
    @ClientTest(timeoutTicks = 1800)
    public static void withNoSpace_theMachineIsAPromptAndSaysSo(final ClientTestContext ctx) {
        atTheMachine(ctx)
                .thenAwaitScreen(ComputerTerminalScreen.class, BOOT_WAIT)
                .then(2, () -> ctx.key(GLFW.GLFW_KEY_ESCAPE))
                .thenServer(SETTLE, level -> machine(ctx, level).console().uninstall(INTERACTOR.toString()))
                .thenRightClick(SETTLE, MONITOR)
                .thenAwaitScreen(NetTerminalScreen.class, SCREEN_WAIT)
                .thenScreenshot(4, "mcnet_bare_prompt");
    }
}
