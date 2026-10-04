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
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.client.CommandPromptScreen;
import dev.jstech.computers.hardware.DiskSize;
import dev.jstech.computers.hardware.StorageTier;
import dev.jstech.computers.program.cli.msd.MsdScreen;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.lwjgl.glfw.GLFW;

/**
 * The Vintage systems' diagnostics, worked as a player works them at an MC-DOS prompt: {@code msd} takes the glass, L
 * opens the list of the ports, the arrows reach the floppy drive, D disables it on the machine, Enter puts the list
 * away and F3 gives the prompt back.
 */
public final class MsdClientTests {

    private static final int SETTLE = 4;
    private static final int SCREEN_WAIT = 80;
    private static final int BOOT_WAIT = 1_200;
    private static final BlockPos COMPUTER = new BlockPos(5, 2, 2);
    private static final BlockPos MONITOR = new BlockPos(6, 2, 2);
    private static final BlockPos DRIVE = new BlockPos(5, 2, 3);
    private static final BlockPos PLAYER_AT_MONITOR = new BlockPos(8, 2, 2);
    private static final ResourceLocation MC_DOS = ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "mc_dos");
    private static final String FLOPPY = "Floppy Drive";

    private MsdClientTests() {
    }

    @ClientTest(timeoutTicks = 3600)
    public static void mcDos_msdDisablesTheFloppyDriveFromTheListOfPorts(final ClientTestContext ctx) {
        final PersonalComputerBlockEntity[] pc = new PersonalComputerBlockEntity[1];
        ctx.thenBuild(0, world -> {
                    world.setBlock(COMPUTER, ComputingModule.VINTAGE_PERSONAL_COMPUTER.get());
                    pc[0] = world.blockEntity(COMPUTER, PersonalComputerBlockEntity.class);
                    buildVintage(pc[0]);
                    pc[0].installOs(MC_DOS);
                    pc[0].togglePower();
                    // A monitor of its own era: a Standard screen wants more than its VGA card's memory.
                    world.placeMonitor(MONITOR, Direction.EAST, ComputingModule.VINTAGE_MONITOR.get());
                    world.setBlock(DRIVE, ComputingModule.FLOPPY_DRIVE.get());
                })
                .thenTeleport(SETTLE, PLAYER_AT_MONITOR, Direction.WEST)
                .thenRightClick(SETTLE, MONITOR)
                .thenAwaitScreen(CommandPromptScreen.class, BOOT_WAIT)
                .then(SETTLE, () -> ctx.type("msd"))
                .then(1, () -> ctx.key(GLFW.GLFW_KEY_ENTER))
                .thenWaitUntil(() -> prompt(ctx).editing() && glass(ctx).contains("Midsoft Diagnostics 2.01")
                                && glass(ctx).contains("COM Ports..."), SCREEN_WAIT,
                        "the diagnostics to take the glass")
                .thenScreenshot(2, "msd-main")
                .then(SETTLE, () -> ctx.type("l"))
                .thenWaitUntil(() -> glass(ctx).contains("LPT and COM Ports") && floppyRow(ctx) >= 0, SCREEN_WAIT,
                        "L to open the list of the ports with the drive on one")
                .then(SETTLE, () -> {
                    final int delta = floppyRow(ctx) - MsdScreen.pickedSaid(lines(ctx));
                    for (int i = 0; i < Math.abs(delta); i++) {
                        ctx.key(delta > 0 ? GLFW.GLFW_KEY_DOWN : GLFW.GLFW_KEY_UP);
                    }
                })
                .thenWaitUntil(() -> MsdScreen.pickedSaid(lines(ctx)) == floppyRow(ctx), SCREEN_WAIT,
                        "the arrows to reach the drive")
                .then(SETTLE, () -> ctx.type("d"))
                .thenWaitUntilServer(level -> pc[0].isDisabled(ctx.abs(DRIVE).asLong()), SCREEN_WAIT,
                        "the machine to disable the drive", level -> "still enabled")
                .thenWaitUntil(() -> lines(ctx).get(MsdScreen.PORTS_TOP + floppyRow(ctx)).contains("Off (disabled)"),
                        SCREEN_WAIT, "the list to say the drive is off")
                .thenScreenshot(2, "msd-disabled")
                .then(SETTLE, () -> ctx.key(GLFW.GLFW_KEY_ENTER))
                .thenWaitUntil(() -> !glass(ctx).contains("LPT and COM Ports"), SCREEN_WAIT,
                        "Enter to put the list away")
                .then(SETTLE, () -> ctx.key(GLFW.GLFW_KEY_F3))
                .thenWaitUntil(() -> !prompt(ctx).editing(), SCREEN_WAIT, "F3 to give the prompt back");
    }

    private static CommandPromptScreen<?> prompt(final ClientTestContext ctx) {
        return ctx.screen(CommandPromptScreen.class);
    }

    private static String glass(final ClientTestContext ctx) {
        final CommandPromptScreen<?> screen = prompt(ctx);
        return screen == null ? "" : screen.editorText();
    }

    private static List<String> lines(final ClientTestContext ctx) {
        return List.of(glass(ctx).split("\n", -1));
    }

    /* Which port row of the list names the floppy drive, or -1 while none does. */
    private static int floppyRow(final ClientTestContext ctx) {
        final List<String> screen = lines(ctx);
        for (int i = 0; i < MsdScreen.portsSaid(screen); i++) {
            if (screen.get(MsdScreen.PORTS_TOP + i).contains(FLOPPY)) {
                return i;
            }
        }
        return -1;
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
