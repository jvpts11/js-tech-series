/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.HardwareItems;
import dev.jstech.computers.JsComputers;
import dev.jstech.computers.blockentity.CraftingComputerBlockEntity;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.client.BootMenuScreen;
import dev.jstech.computers.client.CommandPromptScreen;
import dev.jstech.computers.client.ComputerTerminalScreen;
import dev.jstech.computers.hardware.DiskSize;
import dev.jstech.computers.hardware.StorageTier;
import dev.jstech.computers.menu.ComputerTerminalMenu;
import dev.jstech.core.gui.TextScreen;
import dev.jstech.tests.testkit.TestWorldBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * The manuals at a terminal, in each system's own reader: MC-DOS's and MC-NET's full-screen HELP in the sixteen
 * colours, info on a Linux distribution, and man paging an entry on FreeBSD and on UNIX.
 */
public final class HelpTerminalClientTests {

    private static final int SETTLE = 4;
    private static final int SCREEN_WAIT = 100;
    private static final int BOOT_WAIT = 1_200;
    private static final BlockPos MACHINE = new BlockPos(5, 2, 2);
    private static final BlockPos MONITOR = new BlockPos(6, 2, 2);
    private static final BlockPos PLAYER_AT_MONITOR = new BlockPos(8, 2, 2);
    private static final String CARDS = "Graphics cards";

    private HelpTerminalClientTests() {
    }

    /** MC-DOS's HELP opens on the entry typed after it, then on its contents, and Escape gives the prompt back. */
    @ClientTest(timeoutTicks = 3600)
    public static void mcDos_helpShowsTheManualsInSixteenColours(final ClientTestContext ctx) {
        atMcDos(ctx)
                .then(SETTLE, () -> ctx.type("help graphics-cards"))
                .then(1, () -> ctx.key(GLFW.GLFW_KEY_ENTER))
                .thenWaitUntil(() -> shows(prompt(ctx).editorScreen(), "MC-DOS Help:")
                        && shows(prompt(ctx).editorScreen(), CARDS), SCREEN_WAIT, "HELP to open on the entry")
                .thenAssert(0, () -> prompt(ctx).editorScreen().ground(5, 5) == TextScreen.cga(TextScreen.BLUE),
                        "the topic on blue")
                .thenScreenshot(2, "help-dos-entry")
                .then(SETTLE, () -> ctx.key(GLFW.GLFW_KEY_C, GLFW.GLFW_MOD_ALT))
                .thenWaitUntil(() -> shows(prompt(ctx).editorScreen(), "MC-DOS Help: Contents")
                        && shows(prompt(ctx).editorScreen(), "<1 The Series>"), SCREEN_WAIT,
                        "Alt+C to show the contents, the manual's chapters first")
                .then(SETTLE, () -> ctx.key(GLFW.GLFW_KEY_END))
                .thenWaitUntil(() -> shows(prompt(ctx).editorScreen(), "<dir>"), SCREEN_WAIT,
                        "End to bring the machine's commands up, under the manuals")
                .thenScreenshot(2, "help-dos-commands")
                .thenScreenshot(2, "help-dos-contents")
                .then(SETTLE, () -> ctx.key(GLFW.GLFW_KEY_ESCAPE))
                .thenWaitUntil(() -> !prompt(ctx).editing(), SCREEN_WAIT, "Escape to give the prompt back");
    }

    /** MC-NET's prompt is a heading of its space, and HELP takes that heading's glass in the same colours. */
    @ClientTest(timeoutTicks = 3600)
    public static void mcNet_helpTakesThePromptsGlass(final ClientTestContext ctx) {
        ctx.thenBuild(0, world -> {
                    final CraftingComputerBlockEntity computer = world.placeRunningCraftingComputer(MACHINE);
                    computer.togglePower();
                    computer.formatDisk(0);
                    computer.installOs(ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "mc_net"));
                    computer.togglePower();
                    world.placeMonitor(MONITOR, Direction.EAST);
                })
                .thenTeleport(SETTLE, PLAYER_AT_MONITOR, Direction.WEST)
                .thenRightClick(SETTLE, MONITOR)
                .thenAwaitScreen(ComputerTerminalScreen.class, BOOT_WAIT)
                .then(2, () -> ctx.clickGui(space(ctx).headingPoint("Console")[0],
                        space(ctx).headingPoint("Console")[1]))
                .thenWaitUntil(() -> space(ctx).activeHeading() == ComputerTerminalMenu.TAB_CONSOLE
                        && space(ctx).consoleText().contains("MC-NET"), SCREEN_WAIT, "the prompt to greet")
                .then(SETTLE, () -> ctx.type("help mainframes"))
                .then(1, () -> ctx.key(GLFW.GLFW_KEY_ENTER))
                .thenWaitUntil(() -> shows(space(ctx).consoleScreen(), "MC-NET Help:")
                        && shows(space(ctx).consoleScreen(), "Mainframes"), SCREEN_WAIT,
                        "HELP to open on the entry, in MC-NET's own name")
                .thenScreenshot(2, "help-mcnet-entry")
                .then(SETTLE, () -> ctx.key(GLFW.GLFW_KEY_ESCAPE))
                .thenWaitUntil(() -> space(ctx).consoleScreen() == null, SCREEN_WAIT, "Escape to leave HELP");
    }

    /** info on a Linux distribution: the entry as a node, n to the next one, q to leave. */
    @ClientTest(timeoutTicks = 3600)
    public static void linux_infoReadsTheManualsNodeByNode(final ClientTestContext ctx) {
        ctx.thenBuild(0, world -> {
                    world.setBlock(MACHINE, ComputingModule.MAINFRAME.get());
                    final MainframeBlockEntity machine = world.blockEntity(MACHINE, MainframeBlockEntity.class);
                    build(machine);
                    machine.togglePower();
                    machine.installOs(ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "gentoo"));
                    world.placeMonitor(MONITOR, Direction.EAST);
                })
                .thenTeleport(SETTLE, PLAYER_AT_MONITOR, Direction.WEST)
                .thenRightClick(SETTLE, MONITOR)
                .thenAwaitScreen(CommandPromptScreen.class, BOOT_WAIT)
                .then(SETTLE, () -> ctx.type("info graphics-cards"))
                .then(1, () -> ctx.key(GLFW.GLFW_KEY_ENTER))
                .thenWaitUntil(() -> shows(prompt(ctx).editorScreen(), "Up: ")
                        && shows(prompt(ctx).editorScreen(), "-----Info: (technical-reference)" + CARDS),
                        SCREEN_WAIT, "info to open the entry as a node")
                .thenScreenshot(2, "help-info-entry")
                .then(SETTLE, () -> ctx.type("n"))
                .thenWaitUntil(() -> !shows(prompt(ctx).editorScreen(), "-----Info: (technical-reference)" + CARDS),
                        SCREEN_WAIT, "n to go to the next node")
                .then(SETTLE, () -> ctx.type("q"))
                .thenWaitUntil(() -> !prompt(ctx).editing(), SCREEN_WAIT, "q to leave info");
    }

    /** man on FreeBSD pages an entry of the manuals as a page of section 7. */
    @ClientTest(timeoutTicks = 3600)
    public static void freeBsd_manPagesAnEntry(final ClientTestContext ctx) {
        unixMachine(ctx, "freebsd")
                .thenAwaitScreen(BootMenuScreen.class, BOOT_WAIT)
                .then(SETTLE, () -> ctx.key(GLFW.GLFW_KEY_ENTER))
                .thenAwaitScreen(CommandPromptScreen.class, BOOT_WAIT)
                .thenWaitUntil(() -> said(ctx, "Welcome to FreeBSD, player."), SCREEN_WAIT, "the console to greet")
                .then(SETTLE, () -> ctx.type("man graphics-cards"))
                .then(1, () -> ctx.key(GLFW.GLFW_KEY_ENTER))
                .thenWaitUntil(() -> prompt(ctx).editorText().startsWith("GRAPHICS_CARDS(7)")
                        && prompt(ctx).editorText().contains("Items: "), SCREEN_WAIT, "man to page the entry")
                .thenScreenshot(2, "help-freebsd-man")
                .then(SETTLE, () -> ctx.type("q"))
                .thenWaitUntil(() -> !prompt(ctx).editing(), SCREEN_WAIT, "q to leave the pager");
    }

    /** man on UNIX does the same, by the entry's title. */
    @ClientTest(timeoutTicks = 3600)
    public static void unix_manPagesAnEntryByItsTitle(final ClientTestContext ctx) {
        unixMachine(ctx, "unix")
                .thenAwaitScreen(CommandPromptScreen.class, BOOT_WAIT)
                .thenWaitUntil(() -> said(ctx, "Console Login: player"), SCREEN_WAIT, "the console to sign in")
                .then(SETTLE, () -> ctx.type("man mainframes"))
                .then(1, () -> ctx.key(GLFW.GLFW_KEY_ENTER))
                .thenWaitUntil(() -> prompt(ctx).editorText().startsWith("MAINFRAMES(7)"), SCREEN_WAIT,
                        "man to page the entry")
                .thenScreenshot(2, "help-unix-man")
                .then(SETTLE, () -> ctx.type("q"))
                .thenWaitUntil(() -> !prompt(ctx).editing(), SCREEN_WAIT, "q to leave the pager");
    }

    /** A Vintage Personal Computer running MC-DOS on a colour monitor, at its prompt. */
    private static ClientTestContext atMcDos(final ClientTestContext ctx) {
        return ctx.thenBuild(0, world -> {
                    world.setBlock(MACHINE, ComputingModule.VINTAGE_PERSONAL_COMPUTER.get());
                    final PersonalComputerBlockEntity computer =
                            world.blockEntity(MACHINE, PersonalComputerBlockEntity.class);
                    final ItemStackHandler hardware = computer.getHardware();
                    hardware.setStackInSlot(PersonalComputerBlockEntity.MOTHERBOARD_SLOT,
                            new ItemStack(HardwareItems.MOTHERBOARD_BABYAT_VINTAGE.get()));
                    hardware.setStackInSlot(PersonalComputerBlockEntity.CPU_SLOT,
                            new ItemStack(HardwareItems.CPU_INTEGRA_486SX.get()));
                    hardware.setStackInSlot(PersonalComputerBlockEntity.RAM_SLOTS_START,
                            new ItemStack(HardwareItems.RAM_SIMM_4.get()));
                    hardware.setStackInSlot(PersonalComputerBlockEntity.PSU_SLOT,
                            new ItemStack(HardwareItems.PSU_300.get()));
                    hardware.setStackInSlot(PersonalComputerBlockEntity.GPU_SLOTS_START,
                            new ItemStack(HardwareItems.GPU_VGA_256.get()));
                    hardware.setStackInSlot(PersonalComputerBlockEntity.DISK_SLOTS_START,
                            new ItemStack(ComputingModule.disk(StorageTier.HDD, DiskSize.GB_500)));
                    computer.installOs(ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "mc_dos"));
                    computer.togglePower();
                    // The colour adapter's monitor, which shows HELP in its sixteen colours.
                    world.placeMonitor(MONITOR, Direction.EAST, ComputingModule.CGA_MONITOR.get());
                })
                .thenTeleport(SETTLE, PLAYER_AT_MONITOR, Direction.WEST)
                .thenRightClick(SETTLE, MONITOR)
                .thenAwaitScreen(CommandPromptScreen.class, BOOT_WAIT);
    }

    /** A Mainframe running that Unix system at a monitor, switched on. */
    private static ClientTestContext unixMachine(final ClientTestContext ctx, final String system) {
        return ctx.thenBuild(0, world -> {
                    world.setBlock(MACHINE, ComputingModule.MAINFRAME.get());
                    final MainframeBlockEntity machine = world.blockEntity(MACHINE, MainframeBlockEntity.class);
                    build(machine);
                    machine.installOs(ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, system));
                    machine.togglePower();
                    world.placeMonitor(MONITOR, Direction.EAST);
                })
                .thenTeleport(SETTLE, PLAYER_AT_MONITOR, Direction.WEST)
                .thenRightClick(SETTLE, MONITOR);
    }

    private static void build(final MainframeBlockEntity machine) {
        TestWorldBuilder.installMainframeParts(machine, StorageTier.SSD, true);
    }

    private static CommandPromptScreen<?> prompt(final ClientTestContext ctx) {
        return ctx.screen(CommandPromptScreen.class);
    }

    private static ComputerTerminalScreen space(final ClientTestContext ctx) {
        return ctx.screen(ComputerTerminalScreen.class);
    }

    private static boolean said(final ClientTestContext ctx, final String what) {
        final CommandPromptScreen<?> screen = prompt(ctx);
        return screen != null && screen.scrollbackText().stream().anyMatch(line -> line.contains(what));
    }

    private static boolean shows(@Nullable final TextScreen screen, final String words) {
        return screen != null && screen.text().contains(words);
    }
}
