/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.computers;

import static dev.jstech.tests.testkit.computers.ChainMachineSite.formed;
import static dev.jstech.tests.testkit.computers.ChainMachineSite.machine;
import static dev.jstech.tests.testkit.computers.ChainMachineSite.machineAt;
import static dev.jstech.tests.testkit.computers.ChainMachineSite.made;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.CoolingItems;
import dev.jstech.computers.HardwareItems;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.chain.machine.ComputerKit;
import dev.jstech.computers.chain.machine.Preinstalls;
import dev.jstech.computers.hardware.DiskSize;
import dev.jstech.computers.hardware.StorageTier;
import dev.jstech.computers.item.ServerItem;
import dev.jstech.computers.os.BoardFirmware;
import dev.jstech.computers.os.OsDisks;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.os.media.InstallerStacks;
import dev.jstech.computers.registry.ComputingComponents;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.computers.TestWorldBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * The two machines that turn parts into working computers (computers/docs/design/survival.md): the Prebuilder puts a
 * case and its parts together by the assembly window's rules, a Personal Computer or a rack server, and says why a
 * kit does not go together; the Preinstaller writes a blank board's firmware, a system from its media onto a loose
 * disk, both onto an assembled computer, which then starts, and says when there is nothing to write. Its media stay.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ComputerMakingGameTests {

    private static final String ARENA = "empty";
    private static final int GIVEN = 20;
    /* Two machine steps of ten seconds, and the room a test takes to see what it made. */
    private static final int LONG = 600;
    /* A job that writes a system: its step and a second a hundred megabytes of Frames XP, with room to spare. */
    private static final int SYSTEM_JOB = 1_000;

    private ComputerMakingGameTests() {
    }

    @GameTest(template = ARENA, timeoutTicks = LONG)
    public static void prebuilder_buildsAPersonalComputerFromItsKit(final GameTestHelper helper) {
        final BlockPos terminal = formed(helper, machine("prebuilder"));
        helper.runAfterDelay(GIVEN, () -> give(helper, terminal, personalComputerKit()));
        helper.succeedWhen(() -> {
            final ItemStack built = made(machineAt(helper, terminal), ComputingModule.PERSONAL_COMPUTER.get().asItem());
            helper.assertTrue(!built.isEmpty(), "the computer comes out");
            helper.assertTrue(inside(built).size() == 7, "with its seven parts inside: " + inside(built));
            helper.assertTrue(built.has(ComputingComponents.NEVER_STARTED.get()), "never started");
        });
    }

    @GameTest(template = ARENA, timeoutTicks = LONG)
    public static void prebuilder_refusesAPartThatDoesNotFitSayingWhy(final GameTestHelper helper) {
        final BlockPos terminal = formed(helper, machine("prebuilder"));
        helper.runAfterDelay(GIVEN, () -> give(helper, terminal, List.of(
                new ItemStack(ComputingModule.PERSONAL_COMPUTER.get()),
                new ItemStack(HardwareItems.MOTHERBOARD_ATX_STANDARD_LGA1150.get()),
                new ItemStack(HardwareItems.CPU_INTEGRA_486SX.get()))));
        helper.runAfterDelay(GIVEN * 4, () -> {
            helper.assertTrue(!machineAt(helper, terminal).note().isEmpty(), "it says why");
            helper.assertTrue(made(machineAt(helper, terminal), ComputingModule.PERSONAL_COMPUTER.get().asItem())
                    .isEmpty(), "and builds nothing");
            helper.assertTrue(machineAt(helper, terminal).getInventory().getStackInSlot(0).is(
                    ComputingModule.PERSONAL_COMPUTER.get().asItem()), "the case stays where it was");
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = LONG)
    public static void prebuilder_buildsARackServer(final GameTestHelper helper) {
        final BlockPos terminal = formed(helper, machine("prebuilder"));
        helper.runAfterDelay(GIVEN, () -> give(helper, terminal, List.of(new ItemStack(ComputingModule.SERVER.get()),
                new ItemStack(ComputingModule.MOTHERBOARD_EEB_S_2011.get()),
                new ItemStack(ComputingModule.CPU_SERVO_2620.get()),
                new ItemStack(ComputingModule.RAM_DDR3_8192.get()), new ItemStack(ComputingModule.PSU_650G.get()))));
        helper.succeedWhen(() -> {
            final ItemStack built = made(machineAt(helper, terminal), ComputingModule.SERVER.get());
            helper.assertTrue(!built.isEmpty(), "the server comes out");
            final List<ItemStack> parts = new ArrayList<>();
            ServerItem.hardware(built).nonEmptyItems().forEach(parts::add);
            helper.assertTrue(parts.size() == 4, "with its four parts inside: " + parts);
        });
    }

    @GameTest(template = ARENA, timeoutTicks = LONG)
    public static void preinstaller_writesABlankBoardsFirmware(final GameTestHelper helper) {
        final BlockPos terminal = formed(helper, machine("preinstaller"));
        helper.runAfterDelay(GIVEN, () -> give(helper, terminal, List.of(BoardFirmware.blanked(
                new ItemStack(HardwareItems.MOTHERBOARD_ATX_STANDARD_LGA1150.get())))));
        helper.succeedWhen(() -> {
            final ItemStack board = made(machineAt(helper, terminal),
                    HardwareItems.MOTHERBOARD_ATX_STANDARD_LGA1150.get());
            helper.assertTrue(!board.isEmpty() && !BoardFirmware.blank(board), "the board comes out written");
        });
    }

    @GameTest(template = ARENA, timeoutTicks = SYSTEM_JOB)
    public static void preinstaller_writesASystemOntoALooseDiskKeepingItsMedia(final GameTestHelper helper) {
        final BlockPos terminal = formed(helper, machine("preinstaller"));
        final Item ssd = ComputingModule.disk(StorageTier.SSD, DiskSize.GB_500);
        final List<ItemStack> given = new ArrayList<>(List.of(new ItemStack(ssd)));
        given.addAll(media());
        helper.runAfterDelay(GIVEN, () -> give(helper, terminal, given));
        helper.succeedWhen(() -> {
            final ItemStack disk = made(machineAt(helper, terminal), ssd);
            helper.assertTrue(!disk.isEmpty() && TestWorldBuilder.LEGACY_DESKTOP_OS.equals(OsDisks.systemOn(disk)),
                    "the disk comes out with the system on it");
            helper.assertTrue(Preinstalls.isMedium(machineAt(helper, terminal).getInventory().getStackInSlot(1)),
                    "its medium stays");
        });
    }

    @GameTest(template = ARENA, timeoutTicks = SYSTEM_JOB)
    public static void preinstaller_preparesAnAssembledComputerThatThenStarts(final GameTestHelper helper) {
        final BlockPos terminal = formed(helper, machine("preinstaller"));
        final List<ItemStack> kit = personalComputerKit();
        kit.set(1, BoardFirmware.blanked(kit.get(1)));
        final ComputerKit.Built prebuilt = ComputerKit.assemble(kit.getFirst(), kit.subList(1, kit.size()));
        helper.assertTrue(prebuilt.built(), "the kit goes together: " + prebuilt.refusal());
        final List<ItemStack> given = new ArrayList<>(List.of(prebuilt.computer()));
        given.addAll(media());
        helper.runAfterDelay(GIVEN, () -> give(helper, terminal, given));
        helper.succeedWhen(() -> {
            final ItemStack ready = made(machineAt(helper, terminal), ComputingModule.PERSONAL_COMPUTER.get().asItem());
            helper.assertTrue(!ready.isEmpty(), "the computer comes out");
            final BlockPos at = new BlockPos(8, 2, 8);
            helper.setBlock(at, ComputingModule.PERSONAL_COMPUTER.get());
            final PersonalComputerBlockEntity pc = (PersonalComputerBlockEntity) helper.getBlockEntity(at);
            pc.applyComponentsFromItemStack(ready);
            helper.assertTrue(!pc.firmwareMissing(), "its board's firmware written");
            helper.assertTrue(pc.installedOs() != null, "its system on its disk");
            pc.togglePower();
            helper.assertTrue(pc.isRunning(), "and it starts");
        });
    }

    @GameTest(template = ARENA, timeoutTicks = LONG)
    public static void preinstaller_saysWhenThereIsNothingToWrite(final GameTestHelper helper) {
        final BlockPos terminal = formed(helper, machine("preinstaller"));
        helper.runAfterDelay(GIVEN, () -> give(helper, terminal, List.of(
                new ItemStack(HardwareItems.MOTHERBOARD_ATX_STANDARD_LGA1150.get()))));
        helper.runAfterDelay(GIVEN * 4, () -> {
            helper.assertTrue(machineAt(helper, terminal).note().equals(Preinstalls.NOTHING_TO_WRITE.text()),
                    "it says there is nothing to write: " + machineAt(helper, terminal).note());
            helper.succeed();
        });
    }

    /* The case and the parts of a Standard Personal Computer, the case first, the memory in one stack of two. */
    private static List<ItemStack> personalComputerKit() {
        return new ArrayList<>(List.of(new ItemStack(ComputingModule.PERSONAL_COMPUTER.get()),
                new ItemStack(HardwareItems.MOTHERBOARD_ATX_STANDARD_LGA1150.get()),
                new ItemStack(HardwareItems.CPU_INTEGRA_CENTRO_C7_4790K.get()),
                new ItemStack(CoolingItems.STANDARD_STOCK_COOLER.get()),
                new ItemStack(ComputingModule.RAM_DDR3_8192.get(), 2), new ItemStack(ComputingModule.PSU_650G.get()),
                new ItemStack(ComputingModule.disk(StorageTier.SSD, DiskSize.GB_500))));
    }

    /* Every disk of the installer of a desktop system a Standard computer takes, small enough to write quickly. */
    private static List<ItemStack> media() {
        return InstallerStacks.disksOf(OsRegistry.getOs(TestWorldBuilder.LEGACY_DESKTOP_OS));
    }

    /* Puts {@code stacks} into the machine's inputs, one slot each, in order. */
    private static void give(final GameTestHelper helper, final BlockPos terminal, final List<ItemStack> stacks) {
        final IItemHandler inputs = machineAt(helper, terminal).getInventory();
        for (int i = 0; i < stacks.size(); i++) {
            inputs.insertItem(i, stacks.get(i).copy(), false);
        }
    }

    private static List<ItemStack> inside(final ItemStack computer) {
        final List<ItemStack> out = new ArrayList<>();
        computer.getOrDefault(ComputingComponents.ASSEMBLED_HARDWARE.get(), ItemContainerContents.EMPTY)
                .nonEmptyItems().forEach(out::add);
        return out;
    }
}
