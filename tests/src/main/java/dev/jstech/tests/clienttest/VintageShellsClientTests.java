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
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.client.CommandPromptScreen;
import dev.jstech.computers.client.os.DosShellKeys;
import dev.jstech.computers.client.os.PaceKeys;
import dev.jstech.computers.hardware.DiskSize;
import dev.jstech.computers.hardware.StorageTier;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.terminal.IComputerTerminalHost;
import dev.jstech.core.gui.TextScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * The text-mode shells of the Vintage systems: the MC-DOS Shell with its menus, its file actions run as MC-DOS's own
 * commands and its task switcher, and PACE on UNIX System V with its frames in cascade and its UNIX System that exits
 * back to it.
 */
public final class VintageShellsClientTests {

    private static final int SETTLE = 4;
    private static final int SCREEN_WAIT = 80;
    private static final int BOOT_WAIT = 1_200;
    /** How many items down the File menu Create Directory is from Open, the greyed Print passed over. */
    private static final int TO_CREATE_DIRECTORY = 8;

    private static final BlockPos DOS_MACHINE = new BlockPos(5, 2, 2);
    private static final BlockPos DOS_MONITOR = new BlockPos(6, 2, 2);
    private static final BlockPos PLAYER_AT_DOS_MONITOR = new BlockPos(8, 2, 2);
    private static final BlockPos UNIX_MACHINE = new BlockPos(5, 2, 8);
    private static final BlockPos UNIX_MONITOR = new BlockPos(6, 2, 8);
    private static final BlockPos PLAYER_AT_UNIX_MONITOR = new BlockPos(8, 2, 8);

    private static final ResourceLocation UNIX = ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "unix");
    private static final ResourceLocation MC_DOS =
            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "mc_dos");

    private VintageShellsClientTests() {
    }

    @ClientTest(timeoutTicks = 3600)
    public static void dosShell_opensOnTheRootWithItsAreasAndMenus(final ClientTestContext ctx) {
        atDosShell(ctx)
                .thenAssert(0, () -> shows(ctx, "MC-DOS Shell") && shows(ctx, "Directory Tree")
                        && shows(ctx, "Active Task List") && shows(ctx, "Command Prompt"),
                        "the title, the tree, the task list and the Main group's prompt")
                .thenScreenshot(2, "dosshell-main")
                .then(SETTLE, () -> ctx.key(GLFW.GLFW_KEY_F, GLFW.GLFW_MOD_ALT))
                .thenAssert(1, () -> shows(ctx, "View File Contents"), "Alt+F drops the File menu")
                .thenAssert(0, () -> greyed(ctx, "Print"), "Print is greyed with no printer linked")
                .thenScreenshot(2, "dosshell-file-menu")
                .then(1, () -> ctx.key(GLFW.GLFW_KEY_ESCAPE))
                .thenAssert(1, () -> !shows(ctx, "View File Contents"), "Escape puts the menu away");
    }

    @ClientTest(timeoutTicks = 3600)
    public static void dosShell_viewsAFileWithType(final ClientTestContext ctx) {
        atDosShell(ctx)
                .then(SETTLE, () -> ctx.key(GLFW.GLFW_KEY_TAB))
                .then(1, () -> ctx.key(GLFW.GLFW_KEY_F9))
                .thenWaitUntil(() -> shows(ctx, "File View") && shows(ctx, "@ECHO OFF"), SCREEN_WAIT,
                        "AUTOEXEC.BAT shown as TYPE prints it")
                .thenScreenshot(2, "dosshell-view")
                .then(SETTLE, () -> ctx.key(GLFW.GLFW_KEY_ESCAPE))
                .thenAssert(1, () -> !shows(ctx, "File View"), "Escape goes back to the shell");
    }

    @ClientTest(timeoutTicks = 3600)
    public static void dosShell_createsADirectoryWithMkdir(final ClientTestContext ctx) {
        ClientTestContext chain = atDosShell(ctx)
                .then(SETTLE, () -> ctx.key(GLFW.GLFW_KEY_F, GLFW.GLFW_MOD_ALT));
        for (int i = 0; i < TO_CREATE_DIRECTORY; i++) {
            chain = chain.then(1, () -> ctx.key(GLFW.GLFW_KEY_DOWN));
        }
        chain.thenAssert(1, () -> shows(ctx, "Makes a folder in the selected one"), "Create Directory is picked")
                .then(1, () -> ctx.key(GLFW.GLFW_KEY_ENTER))
                .thenAssert(1, () -> shows(ctx, "New directory name"), "the Create Directory dialog")
                .thenScreenshot(2, "dosshell-mkdir")
                .then(1, () -> ctx.type("GAMES"))
                .then(1, () -> ctx.key(GLFW.GLFW_KEY_ENTER))
                .thenWaitUntilServer(level -> level.getBlockEntity(ctx.abs(DOS_MACHINE))
                                instanceof IComputerTerminalHost host && new ServerCliComputer(host, level)
                                .listDisk("C:\\").entries().stream()
                                .anyMatch(e -> e.isDir() && e.name().equalsIgnoreCase("GAMES")),
                        SCREEN_WAIT, "MKDIR to make the folder on the disk", level -> "it is not on C:\\")
                .thenWaitUntil(() -> shows(ctx, "GAMES"), SCREEN_WAIT, "the tree to show the new folder");
    }

    @ClientTest(timeoutTicks = 3600)
    public static void dosShell_keepsAPromptAsATaskAndSwitchesWithAltTab(final ClientTestContext ctx) {
        atDosShell(ctx)
                .then(SETTLE, () -> ctx.key(GLFW.GLFW_KEY_F9, GLFW.GLFW_MOD_SHIFT))
                .thenWaitUntil(() -> shellIs(ctx, true) && shows(ctx, "C:\\>"), SCREEN_WAIT,
                        "Shift+F9 to bring a command prompt up as a task")
                .then(SETTLE, () -> ctx.type("dir"))
                .then(1, () -> ctx.key(GLFW.GLFW_KEY_ENTER))
                .thenWaitUntil(() -> shows(ctx, "AUTOEXEC"), SCREEN_WAIT, "DIR to list the root in the task")
                .thenScreenshot(2, "dosshell-task")
                .then(SETTLE, () -> ctx.key(GLFW.GLFW_KEY_TAB, GLFW.GLFW_MOD_ALT))
                .thenAssert(0, () -> "MC-DOS Shell".equals(shellBanner(ctx)), "Alt+Tab names the shell")
                .thenWaitUntil(() -> shellIs(ctx, false) && shows(ctx, "Active Task List"), SCREEN_WAIT,
                        "letting go of Alt to bring the shell back")
                .thenAssert(0, () -> shellTasks(ctx) == 1, "the prompt is in the Active Task List")
                .thenScreenshot(2, "dosshell-task-list")
                .then(SETTLE, () -> ctx.key(GLFW.GLFW_KEY_TAB, GLFW.GLFW_MOD_ALT))
                .thenAssert(0, () -> "Command Prompt".equals(shellBanner(ctx)), "Alt+Tab names the task")
                .thenWaitUntil(() -> shellIs(ctx, true) && shows(ctx, "AUTOEXEC"), SCREEN_WAIT,
                        "the task back with its screen as it was")
                .then(SETTLE, () -> ctx.type("exit"))
                .then(1, () -> ctx.key(GLFW.GLFW_KEY_ENTER))
                .thenWaitUntil(() -> shellIs(ctx, false) && shellTasks(ctx) == 0, SCREEN_WAIT,
                        "EXIT to end the task and bring the shell back")
                .then(SETTLE, () -> ctx.key(GLFW.GLFW_KEY_F3))
                .thenWaitUntil(() -> !prompt(ctx).editing(), SCREEN_WAIT, "F3 to leave the shell for the prompt");
    }

    @ClientTest(timeoutTicks = 3600)
    public static void pace_opensItsFramesInCascade(final ClientTestContext ctx) {
        atPace(ctx)
                .thenAssert(0, () -> shows(ctx, "1  PACE") && shows(ctx, "Bellwether Labs UNIX")
                        && shows(ctx, "CMD-MENU") && shows(ctx, "-->"),
                        "the PACE frame, the maker, the function keys and the command line")
                .thenScreenshot(2, "pace-main")
                .then(SETTLE, () -> ctx.key(GLFW.GLFW_KEY_ENTER))
                .thenWaitUntil(() -> shows(ctx, "2  Office of player") && shows(ctx, "Filecabinet"), SCREEN_WAIT,
                        "the Office of the player over the PACE frame")
                .then(SETTLE, () -> ctx.key(GLFW.GLFW_KEY_ENTER))
                .thenWaitUntil(() -> shows(ctx, "3  Filecabinet  /usr/player") && shows(ctx, "Directory"),
                        SCREEN_WAIT, "the Filecabinet with the home's folders and their types")
                .thenScreenshot(2, "pace-filecabinet")
                .then(SETTLE, () -> ctx.key(GLFW.GLFW_KEY_F5))
                .thenAssert(1, () -> !shows(ctx, "3  Filecabinet"), "CANCEL closes the frame on top");
    }

    @ClientTest(timeoutTicks = 3600)
    public static void pace_unixSystemIsAShellThatExitsBackToPace(final ClientTestContext ctx) {
        atPace(ctx)
                .then(SETTLE, () -> ctx.key(GLFW.GLFW_KEY_DOWN))
                .then(1, () -> ctx.key(GLFW.GLFW_KEY_DOWN))
                .then(1, () -> ctx.key(GLFW.GLFW_KEY_DOWN))
                .then(1, () -> ctx.key(GLFW.GLFW_KEY_ENTER))
                .thenWaitUntil(() -> paceIs(ctx, true), SCREEN_WAIT, "the UNIX System over the frames")
                .then(SETTLE, () -> ctx.type("exit"))
                .then(1, () -> ctx.key(GLFW.GLFW_KEY_ENTER))
                .thenWaitUntil(() -> paceIs(ctx, false) && shows(ctx, "1  PACE"), SCREEN_WAIT,
                        "exit to bring PACE back")
                .then(SETTLE, () -> ctx.key(GLFW.GLFW_KEY_DOWN))
                .then(1, () -> ctx.key(GLFW.GLFW_KEY_ENTER))
                .thenWaitUntil(() -> !prompt(ctx).editing(), SCREEN_WAIT, "Exit PACE to give the terminal back");
    }

    /** MC-DOS at its prompt, the shell started on it, showing the root's files. */
    private static ClientTestContext atDosShell(final ClientTestContext ctx) {
        return ctx.thenBuild(0, world -> {
                    world.setBlock(DOS_MACHINE, ComputingModule.VINTAGE_PERSONAL_COMPUTER.get());
                    final PersonalComputerBlockEntity computer =
                            world.blockEntity(DOS_MACHINE, PersonalComputerBlockEntity.class);
                    buildVintage(computer);
                    computer.installOs(MC_DOS);
                    computer.togglePower();
                    // The colour adapter's monitor, which shows the shell in its sixteen colours.
                    world.placeMonitor(DOS_MONITOR, Direction.EAST, ComputingModule.CGA_MONITOR.get());
                })
                .thenTeleport(SETTLE, PLAYER_AT_DOS_MONITOR, Direction.WEST)
                .thenRightClick(SETTLE, DOS_MONITOR)
                .thenAwaitScreen(CommandPromptScreen.class, BOOT_WAIT)
                .then(SETTLE, () -> ctx.type("dosshell"))
                .then(1, () -> ctx.key(GLFW.GLFW_KEY_ENTER))
                .thenWaitUntil(() -> shows(ctx, "AUTOEXEC"), SCREEN_WAIT, "the shell with the root's files");
    }

    /** UNIX System V at its console, PACE started on it. */
    private static ClientTestContext atPace(final ClientTestContext ctx) {
        return ctx.thenBuild(0, world -> {
                    world.setBlock(UNIX_MACHINE, ComputingModule.MAINFRAME.get());
                    final MainframeBlockEntity machine = world.blockEntity(UNIX_MACHINE, MainframeBlockEntity.class);
                    build(machine);
                    machine.installOs(UNIX);
                    machine.togglePower();
                    world.placeMonitor(UNIX_MONITOR, Direction.EAST);
                })
                .thenTeleport(SETTLE, PLAYER_AT_UNIX_MONITOR, Direction.WEST)
                .thenRightClick(SETTLE, UNIX_MONITOR)
                .thenAwaitScreen(CommandPromptScreen.class, BOOT_WAIT)
                .thenWaitUntil(() -> prompt(ctx).scrollbackText().stream()
                        .anyMatch(line -> line.contains("Console Login: player")), SCREEN_WAIT,
                        "the console to sign in")
                .then(SETTLE, () -> ctx.type("pace"))
                .then(1, () -> ctx.key(GLFW.GLFW_KEY_ENTER))
                .thenWaitUntil(() -> shows(ctx, "Office of player"), SCREEN_WAIT, "PACE to take the console");
    }

    private static CommandPromptScreen<?> prompt(final ClientTestContext ctx) {
        return ctx.screen(CommandPromptScreen.class);
    }

    /* The shell's keys, or null while the console is in another editor, so a wait polls instead of failing. */
    @Nullable
    private static DosShellKeys shell(final ClientTestContext ctx) {
        final CommandPromptScreen<?> prompt = ctx.openScreen(CommandPromptScreen.class);
        return prompt != null && prompt.editorKeys() instanceof DosShellKeys keys ? keys : null;
    }

    @Nullable
    private static PaceKeys pace(final ClientTestContext ctx) {
        final CommandPromptScreen<?> prompt = ctx.openScreen(CommandPromptScreen.class);
        return prompt != null && prompt.editorKeys() instanceof PaceKeys keys ? keys : null;
    }

    /* Whether the shell is up and its task is in front (or, with false, up and behind another). */
    private static boolean shellIs(final ClientTestContext ctx, final boolean inFront) {
        final DosShellKeys shell = shell(ctx);
        return shell != null && shell.taskInFront() == inFront;
    }

    private static boolean paceIs(final ClientTestContext ctx, final boolean inFront) {
        final PaceKeys pace = pace(ctx);
        return pace != null && pace.taskInFront() == inFront;
    }

    private static String shellBanner(final ClientTestContext ctx) {
        final DosShellKeys shell = shell(ctx);
        return shell == null ? "" : shell.banner();
    }

    private static int shellTasks(final ClientTestContext ctx) {
        final DosShellKeys shell = shell(ctx);
        return shell == null ? -1 : shell.taskCount();
    }

    @Nullable
    private static TextScreen screenOf(final ClientTestContext ctx) {
        final CommandPromptScreen<?> prompt = ctx.openScreen(CommandPromptScreen.class);
        return prompt == null ? null : prompt.editorScreen();
    }

    private static boolean shows(final ClientTestContext ctx, final String words) {
        final TextScreen screen = screenOf(ctx);
        return screen != null && screen.text().contains(words);
    }

    /** Whether {@code word} stands on the screen in the dark grey of something that cannot be picked. */
    private static boolean greyed(final ClientTestContext ctx, final String word) {
        final TextScreen screen = screenOf(ctx);
        if (screen == null) {
            return false;
        }
        for (int row = 0; row < screen.rows(); row++) {
            final int at = screen.text(row).indexOf(word);
            if (at >= 0) {
                return screen.ink(at, row) == TextScreen.cga(TextScreen.DARK_GREY);
            }
        }
        return false;
    }

    private static void build(final MainframeBlockEntity machine) {
        final ItemStackHandler inv = machine.getInventory();
        inv.setStackInSlot(MainframeBlockEntity.MOTHERBOARD_SLOT,
                new ItemStack(ComputingModule.MOTHERBOARD_MTX_S_2011.get()));
        inv.setStackInSlot(MainframeBlockEntity.CPU_SLOTS_START, new ItemStack(ComputingModule.CPU_SERVO_2620.get()));
        inv.setStackInSlot(MainframeBlockEntity.RAM_SLOTS_START, new ItemStack(ComputingModule.RAM_DDR3_8192.get()));
        inv.setStackInSlot(MainframeBlockEntity.PSU_SLOT, new ItemStack(ComputingModule.PSU_650G.get()));
        inv.setStackInSlot(MainframeBlockEntity.GPU_SLOTS_START, new ItemStack(ComputingModule.GPU_HD_7970.get()));
        inv.setStackInSlot(MainframeBlockEntity.DISK_SLOTS_START,
                new ItemStack(ComputingModule.disk(StorageTier.SSD, DiskSize.GB_500)));
    }

    private static void buildVintage(final PersonalComputerBlockEntity computer) {
        final ItemStackHandler hardware = computer.getHardware();
        hardware.setStackInSlot(PersonalComputerBlockEntity.MOTHERBOARD_SLOT,
                new ItemStack(HardwareItems.MOTHERBOARD_BABYAT_VINTAGE.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.CPU_SLOT,
                new ItemStack(HardwareItems.CPU_INTEGRA_486SX.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.RAM_SLOTS_START,
                new ItemStack(HardwareItems.RAM_SIMM_4.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.PSU_SLOT, new ItemStack(HardwareItems.PSU_300.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.GPU_SLOTS_START,
                new ItemStack(HardwareItems.GPU_VGA_256.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.DISK_SLOTS_START,
                new ItemStack(ComputingModule.disk(StorageTier.HDD, DiskSize.GB_500)));
    }
}
