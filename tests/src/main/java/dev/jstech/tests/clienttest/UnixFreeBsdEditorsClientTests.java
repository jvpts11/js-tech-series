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
import dev.jstech.computers.client.BootMenuScreen;
import dev.jstech.computers.client.CommandPromptScreen;
import dev.jstech.computers.hardware.DiskSize;
import dev.jstech.computers.hardware.StorageTier;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.lwjgl.glfw.GLFW;

/**
 * {@code vi} and {@code ee}, met at the console of the two systems that bundle them: FreeBSD's nvi and
 * UNIX System V's original over the one engine, and FreeBSD's own {@code ee} on top of its keys.
 */
public final class UnixFreeBsdEditorsClientTests {

    private static final int SETTLE = 4;
    private static final int SCREEN_WAIT = 80;
    private static final int BOOT_WAIT = 1_200;

    private static final BlockPos MACHINE = new BlockPos(5, 2, 2);
    private static final BlockPos MONITOR = new BlockPos(6, 2, 2);
    private static final BlockPos PLAYER_AT_MONITOR = new BlockPos(8, 2, 2);

    private static final BlockPos DOS_MACHINE = new BlockPos(5, 2, 8);
    private static final BlockPos DOS_MONITOR = new BlockPos(6, 2, 8);
    private static final BlockPos PLAYER_AT_DOS_MONITOR = new BlockPos(8, 2, 8);

    private static final ResourceLocation FREEBSD =
            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "freebsd");
    private static final ResourceLocation UNIX = ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "unix");
    private static final ResourceLocation MC_DOS =
            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "mc_dos");

    private UnixFreeBsdEditorsClientTests() {
    }

    /**
     * FreeBSD's {@code vi} is nvi, bundled with no install: opening a new name says so bare, typing and
     * writing leave the file on the disk, and the prompt is whole again once it is closed.
     */
    @ClientTest(timeoutTicks = 3600)
    public static void vi_opensAsNviOnFreeBsdAndWritesTheFile(final ClientTestContext ctx) {
        atFreeBsdConsole(ctx)
                .then(SETTLE, () -> ctx.type("vi notes.txt"))
                .then(1, () -> ctx.key(GLFW.GLFW_KEY_ENTER))
                .thenWaitUntil(() -> editing(ctx), SCREEN_WAIT, "nvi to take the console over a bare new name")
                .thenAssert(0, () -> status(ctx).equals("notes.txt: new file: line 1"),
                        "nvi's own opening message, not Vim's status line")
                .thenScreenshot(2, "freebsd-vi-open")
                .then(SETTLE, () -> ctx.type("i"))
                .thenAssert(1, () -> status(ctx).isEmpty(), "no Vim status line once insert mode begins")
                .then(1, () -> ctx.type("Shopping for the base:"))
                .then(1, () -> ctx.key(GLFW.GLFW_KEY_ESCAPE))
                .then(1, () -> ctx.type(":wq"))
                .then(1, () -> ctx.key(GLFW.GLFW_KEY_ENTER))
                .thenWaitUntil(() -> !editing(ctx), SCREEN_WAIT, "nvi to give the console back")
                .then(SETTLE, () -> ctx.type("cat notes.txt"))
                .then(1, () -> ctx.key(GLFW.GLFW_KEY_ENTER))
                .thenWaitUntil(() -> said(ctx, "Shopping for the base:"), SCREEN_WAIT,
                        "what was typed to be on the disk")
                .thenScreenshot(2, "freebsd-vi-written");
    }

    /**
     * UNIX's {@code vi} is System V's original, the same engine wearing its own voice: the messages are
     * its own, and the file it writes is the same file the shell reads back.
     */
    @ClientTest(timeoutTicks = 3600)
    public static void vi_opensAsSystemVOnUnixAndWritesTheFile(final ClientTestContext ctx) {
        atUnixConsole(ctx)
                .then(SETTLE, () -> ctx.type("vi notes.txt"))
                .then(1, () -> ctx.key(GLFW.GLFW_KEY_ENTER))
                .thenWaitUntil(() -> editing(ctx), SCREEN_WAIT, "System V's vi to take the console")
                .thenAssert(0, () -> status(ctx).equals("\"notes.txt\" [New file]"),
                        "System V's own opening message, not Vim's status line")
                .thenScreenshot(2, "unix-vi-open")
                .then(SETTLE, () -> ctx.type("i"))
                .thenAssert(1, () -> status(ctx).isEmpty(), "no Vim status line once insert mode begins")
                .then(1, () -> ctx.type("Shopping for the base:"))
                .then(1, () -> ctx.key(GLFW.GLFW_KEY_ESCAPE))
                .then(1, () -> ctx.type(":wq"))
                .then(1, () -> ctx.key(GLFW.GLFW_KEY_ENTER))
                .thenWaitUntil(() -> !editing(ctx), SCREEN_WAIT, "vi to give the console back")
                .then(SETTLE, () -> ctx.type("cat notes.txt"))
                .then(1, () -> ctx.key(GLFW.GLFW_KEY_ENTER))
                .thenWaitUntil(() -> said(ctx, "Shopping for the base:"), SCREEN_WAIT,
                        "what was typed to be on the disk");
    }

    /**
     * {@code ee} writes its five rows of shortcuts across the top and its own row under them naming where
     * the caret stands; Escape opens its menu over the text, and leaving through it, with Enter twice at
     * the defaults it starts on, saves and gives the console back.
     */
    @ClientTest(timeoutTicks = 3600)
    public static void ee_opensWithItsKeysOnTopAndItsMenuSavesOnTheWayOut(final ClientTestContext ctx) {
        atFreeBsdConsole(ctx)
                .then(SETTLE, () -> ctx.type("ee notes.txt"))
                .then(1, () -> ctx.key(GLFW.GLFW_KEY_ENTER))
                .thenWaitUntil(() -> editing(ctx), SCREEN_WAIT, "ee to take the console over a bare new name")
                .thenAssert(0, () -> editorKeysOnTop(ctx), "ee writes its five rows of shortcuts above the text")
                .thenScreenshot(2, "freebsd-ee-open")
                .then(SETTLE, () -> ctx.type("Shopping for the base:"))
                .thenAssert(1, () -> editorPosition(ctx).startsWith("=====line 1 col 22"),
                        "ee's position row follows the caret")
                .then(1, () -> ctx.key(GLFW.GLFW_KEY_ESCAPE))
                .thenScreenshot(2, "freebsd-ee-menu")
                // "leave editor" is the menu's default; Enter opens the question it asks.
                .then(1, () -> ctx.key(GLFW.GLFW_KEY_ENTER))
                .thenScreenshot(2, "freebsd-ee-leave-question")
                // "save changes" is that question's default.
                .then(1, () -> ctx.key(GLFW.GLFW_KEY_ENTER))
                .thenWaitUntil(() -> !editing(ctx), SCREEN_WAIT, "ee to give the console back")
                .then(SETTLE, () -> ctx.type("cat notes.txt"))
                .then(1, () -> ctx.key(GLFW.GLFW_KEY_ENTER))
                .thenWaitUntil(() -> said(ctx, "Shopping for the base:"), SCREEN_WAIT,
                        "what was typed to be on the disk");
    }

    /** UNIX never had {@code ee}: FreeBSD's own gift to newcomers stays FreeBSD's alone. */
    @ClientTest(timeoutTicks = 3600)
    public static void ee_isNotOnUnix(final ClientTestContext ctx) {
        atUnixConsole(ctx)
                .then(SETTLE, () -> ctx.type("ee notes.txt"))
                .then(1, () -> ctx.key(GLFW.GLFW_KEY_ENTER))
                .thenWaitUntil(() -> said(ctx, "not found"), SCREEN_WAIT, "UNIX to have never heard of ee");
    }

    /** "leave editor" answered "no save" (b) leaves what was typed off the disk. */
    @ClientTest(timeoutTicks = 3600)
    public static void ee_leavingWithNoSaveDropsWhatWasTyped(final ClientTestContext ctx) {
        atFreeBsdConsole(ctx)
                .then(SETTLE, () -> ctx.type("ee notes.txt"))
                .then(1, () -> ctx.key(GLFW.GLFW_KEY_ENTER))
                .thenWaitUntil(() -> editing(ctx), SCREEN_WAIT, "ee to take the console over a bare new name")
                .then(SETTLE, () -> ctx.type("Shopping for the base:"))
                .then(1, () -> ctx.key(GLFW.GLFW_KEY_ESCAPE))
                .then(1, () -> ctx.type("a"))
                .then(1, () -> ctx.type("b"))
                .thenWaitUntil(() -> !editing(ctx), SCREEN_WAIT, "ee to give the console back with no save")
                .then(SETTLE, () -> ctx.type("cat notes.txt"))
                .then(1, () -> ctx.key(GLFW.GLFW_KEY_ENTER))
                .thenAssert(SCREEN_WAIT, () -> !said(ctx, "Shopping for the base:"),
                        "no save left the typed words off the disk");
    }

    /**
     * The Esc menu's other letters are real, not dead: {@code c b} writes without leaving, {@code f} opens
     * the same search prompt Control and Y does, and {@code b} shows a page of keys that closes back onto
     * the same file, untouched.
     */
    @ClientTest(timeoutTicks = 3600)
    public static void ee_menuOffersWriteSearchAndHelp(final ClientTestContext ctx) {
        atFreeBsdConsole(ctx)
                .then(SETTLE, () -> ctx.type("ee notes.txt"))
                .then(1, () -> ctx.key(GLFW.GLFW_KEY_ENTER))
                .thenWaitUntil(() -> editing(ctx), SCREEN_WAIT, "ee to take the console over a bare new name")
                .then(SETTLE, () -> ctx.type("Shopping for the base:"))
                // Esc, c) file operations, b) save file: saves without leaving the editor.
                .then(1, () -> ctx.key(GLFW.GLFW_KEY_ESCAPE))
                .then(1, () -> ctx.type("c"))
                .then(1, () -> ctx.type("b"))
                .thenAssert(1, () -> editing(ctx) && status(ctx).equals("\"notes.txt\" 1 lines, 22 characters"),
                        "file operations' save writes without leaving the editor")
                // Esc, f) search: opens the same prompt Control and Y does, rather than doing nothing.
                .then(1, () -> ctx.key(GLFW.GLFW_KEY_ESCAPE))
                .then(1, () -> ctx.type("f"))
                .thenAssert(1, () -> "Search: ".equals(status(ctx)), "search opens the same prompt ^Y does")
                .then(1, () -> ctx.key(GLFW.GLFW_KEY_ESCAPE))
                // Esc, b) help: a page of keys, the file still there and untouched once it closes.
                .then(1, () -> ctx.key(GLFW.GLFW_KEY_ESCAPE))
                .then(1, () -> ctx.type("b"))
                .thenScreenshot(2, "freebsd-ee-help")
                .thenAssert(1, () -> "(press any key to continue)".equals(status(ctx)),
                        "the message row says how to leave the help page while it is up")
                .then(1, () -> ctx.key(GLFW.GLFW_KEY_ESCAPE))
                .thenAssert(1, () -> editorText(ctx).contains("Shopping for the base:"),
                        "help closes back onto the same file, untouched")
                // A Control chord types nothing, and closes the help page like any other key.
                .then(1, () -> ctx.key(GLFW.GLFW_KEY_ESCAPE))
                .then(1, () -> ctx.type("b"))
                .then(1, () -> ctx.key(GLFW.GLFW_KEY_C, GLFW.GLFW_MOD_CONTROL))
                .thenAssert(1, () -> !"(press any key to continue)".equals(status(ctx))
                                && editorText(ctx).equals("Shopping for the base:"),
                        "^c closes the help page and leaves the file as it was")
                // a) leave editor, a) save changes: gives the console back.
                .then(1, () -> ctx.key(GLFW.GLFW_KEY_ESCAPE))
                .then(1, () -> ctx.type("a"))
                .then(1, () -> ctx.type("a"))
                .thenWaitUntil(() -> !editing(ctx), SCREEN_WAIT, "ee to give the console back");
    }

    /**
     * A printable key on the help page reaches the editor as a key press and then a typed character, the
     * way the real keyboard sends one: the key press alone must not close the page and let the character
     * that follows fall through onto the file.
     */
    @ClientTest(timeoutTicks = 3600)
    public static void ee_help_spaceKeyClosesWithoutTypingIntoTheFile(final ClientTestContext ctx) {
        atFreeBsdConsole(ctx)
                .then(SETTLE, () -> ctx.type("ee notes.txt"))
                .then(1, () -> ctx.key(GLFW.GLFW_KEY_ENTER))
                .thenWaitUntil(() -> editing(ctx), SCREEN_WAIT, "ee to take the console over a bare new name")
                .then(SETTLE, () -> ctx.type("Shopping for the base:"))
                .then(1, () -> ctx.key(GLFW.GLFW_KEY_ESCAPE))
                .then(1, () -> ctx.type("b"))
                .then(1, () -> ctx.key(GLFW.GLFW_KEY_SPACE))
                .then(1, () -> ctx.type(" "))
                .thenAssert(1, () -> editorText(ctx).equals("Shopping for the base:"),
                        "the space that closed the help page never reached the file")
                .then(1, () -> ctx.type("!"))
                .thenAssert(1, () -> editorText(ctx).equals("Shopping for the base:!"),
                        "help is closed, so ordinary typing reaches the file again");
    }

    /** A second Tab in a row on an empty line lists every command the console has, as sh and bash do. */
    @ClientTest(timeoutTicks = 3600)
    public static void tabTwice_onAnEmptyLineListsEveryCommand(final ClientTestContext ctx) {
        final int[] before = new int[1];
        atFreeBsdConsole(ctx)
                .then(0, () -> before[0] = scrollbackSize(ctx))
                .then(1, () -> ctx.key(GLFW.GLFW_KEY_TAB))
                .then(1, () -> ctx.key(GLFW.GLFW_KEY_TAB))
                .thenAssert(1, () -> {
                    final String added = String.join(" ", newScrollbackLines(ctx, before[0]));
                    return hasWord(added, "ls") && hasWord(added, "man") && hasWord(added, "apropos");
                }, "a second Tab in a row lists what the machine can run, as the welcome promises");
    }

    /** MC-DOS never had bash's habit of listing every command on a second Tab: it prints nothing new. */
    @ClientTest(timeoutTicks = 3600)
    public static void tabTwice_onMcDosPrintsNothing(final ClientTestContext ctx) {
        final int[] before = new int[1];
        atMcDosConsole(ctx)
                .then(0, () -> before[0] = scrollbackSize(ctx))
                .then(1, () -> ctx.key(GLFW.GLFW_KEY_TAB))
                .then(1, () -> ctx.key(GLFW.GLFW_KEY_TAB))
                .thenAssert(SCREEN_WAIT, () -> scrollbackSize(ctx) == before[0],
                        "a second Tab on MC-DOS, which never had this habit, must add no line");
    }

    private static int scrollbackSize(final ClientTestContext ctx) {
        final CommandPromptScreen<?> prompt = ctx.screen(CommandPromptScreen.class);
        return prompt == null ? 0 : prompt.scrollbackText().size();
    }

    /** The scrollback lines added since {@code before}, so a check can read only what a step just printed. */
    private static List<String> newScrollbackLines(final ClientTestContext ctx, final int before) {
        final CommandPromptScreen<?> prompt = ctx.screen(CommandPromptScreen.class);
        final List<String> all = prompt == null ? List.of() : prompt.scrollbackText();
        return before >= all.size() ? List.of() : all.subList(before, all.size());
    }

    /** Whether {@code word} appears in {@code line} as a whole token, not merely as a substring of another. */
    private static boolean hasWord(final String line, final String word) {
        for (final String token : line.split("\\s+")) {
            if (token.equals(word)) {
                return true;
            }
        }
        return false;
    }

    private static boolean editing(final ClientTestContext ctx) {
        final CommandPromptScreen<?> prompt = ctx.screen(CommandPromptScreen.class);
        return prompt != null && prompt.editing();
    }

    private static String status(final ClientTestContext ctx) {
        final CommandPromptScreen<?> prompt = ctx.screen(CommandPromptScreen.class);
        return prompt == null ? "" : prompt.editorStatus();
    }

    private static String editorText(final ClientTestContext ctx) {
        final CommandPromptScreen<?> prompt = ctx.screen(CommandPromptScreen.class);
        return prompt == null ? "" : prompt.editorText();
    }

    private static String editorPosition(final ClientTestContext ctx) {
        final CommandPromptScreen<?> prompt = ctx.screen(CommandPromptScreen.class);
        return prompt == null ? "" : prompt.editorPosition();
    }

    private static boolean editorKeysOnTop(final ClientTestContext ctx) {
        final CommandPromptScreen<?> prompt = ctx.screen(CommandPromptScreen.class);
        return prompt != null && prompt.editorKeysOnTop();
    }

    private static boolean said(final ClientTestContext ctx, final String words) {
        final CommandPromptScreen<?> prompt = ctx.screen(CommandPromptScreen.class);
        return prompt != null && prompt.scrollbackText().stream().anyMatch(line -> line.contains(words));
    }

    /** A FreeBSD Mainframe at a monitor, taken from the loader straight to its console. */
    private static ClientTestContext atFreeBsdConsole(final ClientTestContext ctx) {
        return ctx.thenBuild(0, world -> {
                    world.setBlock(MACHINE, ComputingModule.MAINFRAME.get());
                    final MainframeBlockEntity machine = world.blockEntity(MACHINE, MainframeBlockEntity.class);
                    build(machine);
                    machine.installOs(FREEBSD);
                    machine.togglePower();
                    world.placeMonitor(MONITOR, Direction.EAST);
                })
                .thenTeleport(SETTLE, PLAYER_AT_MONITOR, Direction.WEST)
                .thenRightClick(SETTLE, MONITOR)
                .thenAwaitScreen(BootMenuScreen.class, BOOT_WAIT)
                .then(SETTLE, () -> ctx.key(GLFW.GLFW_KEY_ENTER))
                .thenAwaitScreen(CommandPromptScreen.class, BOOT_WAIT)
                .thenWaitUntil(() -> said(ctx, "Welcome to FreeBSD, player."), SCREEN_WAIT, "the console to greet");
    }

    /** A UNIX Mainframe at a monitor: no loader on the way, straight to its console. */
    private static ClientTestContext atUnixConsole(final ClientTestContext ctx) {
        return ctx.thenBuild(0, world -> {
                    world.setBlock(MACHINE, ComputingModule.MAINFRAME.get());
                    final MainframeBlockEntity machine = world.blockEntity(MACHINE, MainframeBlockEntity.class);
                    build(machine);
                    machine.installOs(UNIX);
                    machine.togglePower();
                    world.placeMonitor(MONITOR, Direction.EAST);
                })
                .thenTeleport(SETTLE, PLAYER_AT_MONITOR, Direction.WEST)
                .thenRightClick(SETTLE, MONITOR)
                .thenAwaitScreen(CommandPromptScreen.class, BOOT_WAIT)
                .thenWaitUntil(() -> said(ctx, "Console Login: player"), SCREEN_WAIT, "the console to sign in");
    }

    /** A Vintage Personal Computer at a monitor, MC-DOS installed, straight to its prompt. */
    private static ClientTestContext atMcDosConsole(final ClientTestContext ctx) {
        return ctx.thenBuild(0, world -> {
                    world.setBlock(DOS_MACHINE, ComputingModule.VINTAGE_PERSONAL_COMPUTER.get());
                    final PersonalComputerBlockEntity computer =
                            world.blockEntity(DOS_MACHINE, PersonalComputerBlockEntity.class);
                    buildVintage(computer);
                    computer.installOs(MC_DOS);
                    computer.togglePower();
                    world.placeMonitor(DOS_MONITOR, Direction.EAST);
                })
                .thenTeleport(SETTLE, PLAYER_AT_DOS_MONITOR, Direction.WEST)
                .thenRightClick(SETTLE, DOS_MONITOR)
                .thenAwaitScreen(CommandPromptScreen.class, BOOT_WAIT);
    }

    /** The Mainframe build both systems install on: a board, a CPU, RAM, a PSU, a GPU for the monitor and a disk. */
    private static void build(final MainframeBlockEntity machine) {
        final ItemStackHandler inv = machine.getInventory();
        inv.setStackInSlot(MainframeBlockEntity.MOTHERBOARD_SLOT,
                new ItemStack(ComputingModule.MOTHERBOARD_MTX_P.get()));
        inv.setStackInSlot(MainframeBlockEntity.CPU_SLOTS_START, new ItemStack(ComputingModule.CPU_SERVO_2620.get()));
        inv.setStackInSlot(MainframeBlockEntity.RAM_SLOTS_START, new ItemStack(ComputingModule.RAM_DDR3_8192.get()));
        inv.setStackInSlot(MainframeBlockEntity.PSU_SLOT, new ItemStack(ComputingModule.PSU_650G.get()));
        inv.setStackInSlot(MainframeBlockEntity.GPU_SLOTS_START, new ItemStack(ComputingModule.GPU_HD_7970.get()));
        inv.setStackInSlot(MainframeBlockEntity.DISK_SLOTS_START,
                new ItemStack(ComputingModule.disk(StorageTier.SSD, DiskSize.GB_500)));
    }

    /** The Vintage Personal Computer build MC-DOS installs on: a period board, CPU, RAM, PSU and a disk. */
    private static void buildVintage(final PersonalComputerBlockEntity computer) {
        final ItemStackHandler hardware = computer.getHardware();
        hardware.setStackInSlot(PersonalComputerBlockEntity.MOTHERBOARD_SLOT,
                new ItemStack(HardwareItems.MOTHERBOARD_BABYAT_VINTAGE.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.CPU_SLOT,
                new ItemStack(HardwareItems.CPU_INTEGRA_486SX.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.RAM_SLOTS_START,
                new ItemStack(HardwareItems.RAM_SIMM_4.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.PSU_SLOT, new ItemStack(HardwareItems.PSU_300.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.DISK_SLOTS_START,
                new ItemStack(ComputingModule.disk(StorageTier.HDD, DiskSize.GB_500)));
    }
}
