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
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.client.CommandPromptScreen;
import dev.jstech.computers.client.os.DesktopScreen;
import dev.jstech.computers.client.os.IDesktopApp;
import dev.jstech.computers.client.os.ShellApp;
import dev.jstech.computers.hardware.DiskSize;
import dev.jstech.computers.hardware.StorageTier;
import dev.jstech.computers.os.FilesystemKind;
import dev.jstech.computers.os.fs.DiskFilesystem;
import dev.jstech.computers.os.fs.FileType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.lwjgl.glfw.GLFW;

/**
 * A program at a terminal asking the way the old consoles asked: its question printed without ending the line stands
 * where the prompt was, and the answer typed goes on the same line, drawn once, at a desktop's terminal and at the
 * prompt of a machine with no desktop alike.
 */
public final class SigmaConsoleClientTests {

    private static final int SETTLE = 4;
    private static final int SCREEN_WAIT = 80;
    private static final int BOOT_WAIT = 1_200;

    private static final BlockPos COMPUTER = new BlockPos(5, 2, 2);
    private static final BlockPos MONITOR = new BlockPos(6, 2, 2);
    private static final BlockPos PLAYER_AT_MONITOR = new BlockPos(8, 2, 2);
    private static final ResourceLocation FRAMES_XP =
            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "frames_xp");
    private static final ResourceLocation MC_DOS =
            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "mc_dos");
    private static final String TERMINAL = "Command Prompt";

    private static final String QUESTION = "How many ingots?";
    private static final String ANSWERED = QUESTION + " 16";
    private static final String WENT_ON = "Iron Ingot    16";

    /** The program both terminals run, the same in both languages. */
    private static final String ASK = """
            using Standard.*;
            namespace Ask;
            class Ask {
                static void Main() {
                    int n;
                    printf("How many ingots? ");
                    scanf("%d", out n);
                    printf("Iron Ingot %5d\\n", n);
                }
            }
            """;

    private SigmaConsoleClientTests() {
    }

    @ClientTest(timeoutTicks = 3000)
    public static void openLine_theQuestionStandsWhereThePromptWasAndTheAnswerGoesOnItsLine(
            final ClientTestContext ctx) {
        ctx.thenBuild(0, world -> {
                    final CraftingComputerBlockEntity computer = world.placeRunningCraftingComputer(COMPUTER);
                    computer.installOs(FRAMES_XP);
                    for (final String id : new String[] {"sgsc", "sigma"}) {
                        computer.console().install(program(id).toString());
                    }
                    DiskFilesystem.write(computer.systemDisk(), "progs/ask.sgs", FileType.SGS, ASK,
                            Long.MAX_VALUE, FilesystemKind.HIERARCHICAL);
                    world.placeMonitor(MONITOR, Direction.EAST);
                })
                .thenTeleport(SETTLE, PLAYER_AT_MONITOR, Direction.WEST)
                .thenRightClick(SETTLE, MONITOR)
                .thenAwaitScreen(DesktopScreen.class, BOOT_WAIT)
                .thenWaitUntil(() -> ctx.screen(DesktopScreen.class).launcherLabels().contains(TERMINAL),
                        SCREEN_WAIT, "the terminal to be listed in Start")
                .then(0, () -> launch(ctx))
                .thenWaitUntil(() -> shell(ctx) != null, SCREEN_WAIT, "the terminal window to open")
                .then(SETTLE, () -> ctx.type("sigma run progs/ask.sgs"))
                .then(1, () -> ctx.key(GLFW.GLFW_KEY_ENTER))
                .thenWaitUntil(() -> QUESTION.equals(shell(ctx).promptShown()), SCREEN_WAIT * 3,
                        "the question to stand where the prompt was")
                .thenAssert(0, () -> !shell(ctx).scrollbackText().contains(QUESTION + "\n"),
                        "and not as a line of its own before it is answered")
                .thenScreenshot(2, "open-line-asking")
                .then(SETTLE, () -> ctx.type("16"))
                .then(1, () -> ctx.key(GLFW.GLFW_KEY_ENTER))
                .thenWaitUntil(() -> shell(ctx).scrollbackText().contains(WENT_ON), SCREEN_WAIT,
                        "the program to go on with the answer")
                .thenAssert(0, () -> shell(ctx).scrollbackText().contains(ANSWERED + "\n"),
                        "the question and its answer are one line")
                .thenAssert(0, () -> !shell(ctx).scrollbackText().contains("\n16\n"), "and the answer is drawn once")
                .thenScreenshot(2, "open-line-answered");
    }

    @ClientTest(timeoutTicks = 3600)
    public static void openLine_aMachineWithNoDesktopAsksTheSameWay(final ClientTestContext ctx) {
        ctx.thenBuild(0, world -> {
                    world.setBlock(COMPUTER, ComputingModule.VINTAGE_PERSONAL_COMPUTER.get());
                    final PersonalComputerBlockEntity computer =
                            world.blockEntity(COMPUTER, PersonalComputerBlockEntity.class);
                    buildVintage(computer);
                    computer.installOs(MC_DOS);
                    for (final String id : new String[] {"scc", "sigma"}) {
                        computer.console().install(program(id).toString());
                    }
                    DiskFilesystem.write(computer.systemDisk(), "progs/ask.sg", FileType.SG, ASK,
                            Long.MAX_VALUE, FilesystemKind.HIERARCHICAL);
                    computer.togglePower();
                    // A monitor of its own era: a Standard screen wants more than its VGA card's memory.
                    world.placeMonitor(MONITOR, Direction.EAST, ComputingModule.VINTAGE_MONITOR.get());
                })
                .thenTeleport(SETTLE, PLAYER_AT_MONITOR, Direction.WEST)
                .thenRightClick(SETTLE, MONITOR)
                .thenAwaitScreen(CommandPromptScreen.class, BOOT_WAIT)
                .then(SETTLE, () -> ctx.type("sigma run progs/ask.sg"))
                .then(1, () -> ctx.key(GLFW.GLFW_KEY_ENTER))
                .thenWaitUntil(() -> QUESTION.equals(prompt(ctx).promptShown().stripTrailing()), SCREEN_WAIT * 3,
                        "the question to stand where the prompt was")
                .thenScreenshot(2, "open-line-asking-dos")
                .then(SETTLE, () -> ctx.type("16"))
                .then(1, () -> ctx.key(GLFW.GLFW_KEY_ENTER))
                .thenWaitUntil(() -> prompt(ctx).scrollbackText().stream().anyMatch(l -> l.contains(WENT_ON)),
                        SCREEN_WAIT, "the program to read the line typed at the prompt and go on")
                .thenAssert(0, () -> prompt(ctx).scrollbackText().stream().anyMatch(l -> l.startsWith(ANSWERED)),
                        "the question and its answer are one line")
                .thenAssert(0, () -> prompt(ctx).scrollbackText().stream().noneMatch(l -> l.strip().equals("16")),
                        "and the answer is drawn once")
                .thenScreenshot(2, "open-line-answered-dos");
    }

    private static ResourceLocation program(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, path);
    }

    private static void launch(final ClientTestContext ctx) {
        final DesktopScreen desktop = ctx.screen(DesktopScreen.class);
        ctx.click(desktop.startButtonX(), desktop.startButtonY());
        final int item = desktop.launcherLabels().indexOf(TERMINAL);
        ctx.click(desktop.startMenuItemX(item), desktop.startMenuItemY(item));
    }

    private static ShellApp shell(final ClientTestContext ctx) {
        final DesktopScreen desktop = ctx.screen(DesktopScreen.class);
        final var window = desktop == null ? null : desktop.windowFor(TERMINAL);
        final IDesktopApp app = window == null ? null : window.app();
        return app instanceof ShellApp s ? s : null;
    }

    private static CommandPromptScreen<?> prompt(final ClientTestContext ctx) {
        return ctx.screen(CommandPromptScreen.class);
    }

    /** A Vintage machine's parts: its board, a CPU, memory, a supply, a VGA card for its monitor and a disk. */
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
