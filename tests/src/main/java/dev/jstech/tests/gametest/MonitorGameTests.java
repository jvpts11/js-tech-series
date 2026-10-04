/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import com.mojang.authlib.GameProfile;
import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.HardwareItems;
import dev.jstech.computers.block.MonitorBlock;
import dev.jstech.computers.block.MonitorKind;
import dev.jstech.computers.block.MonitorPanel;
import dev.jstech.computers.blockentity.MonitorBlockEntity;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.monitor.IMonitorPicture;
import dev.jstech.computers.monitor.MonitorPictures;
import dev.jstech.core.peripheral.PortKind;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestWorldBuilder;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The monitors of every era: a monitor against its computer needs no cable, flat panels side by side join into one
 * big screen that takes one video output, the power button on the block switches the computer, the three Haswell
 * desktop chips drive a screen with no card, and what a monitor's face shows is described from what its machine
 * holds.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class MonitorGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos PC = new BlockPos(2, 2, 2);
    /* Long enough for a peripheral's next tick to try its link, and a little more. */
    private static final int LINK_TICKS = 10;
    private static final ResourceLocation MC_DOS = ResourceLocation.fromNamespaceAndPath("jsc", "mc_dos");

    private MonitorGameTests() {
    }

    @GameTest(template = ARENA)
    public static void monitor_againstItsComputerLinksWithNoCable(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = cardPc(helper, HardwareItems.CPU_INTEGRA_SERVO_1231_V3.get());
        helper.setBlock(PC.east(), facing(ComputingModule.MONITOR.get(), Direction.WEST));
        final MonitorBlockEntity monitor = entity(helper, PC.east());
        helper.startSequence()
                .thenExecuteAfter(LINK_TICKS, () -> {
                    helper.assertTrue(helper.absolutePos(PC).equals(monitor.ownerPos()),
                            "a monitor standing against its computer links with no cable between them");
                    helper.assertTrue(computer.portsInUse(PortKind.VIDEO) == 1, "and takes one video output");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void haswellChip_givesTheBoardAVideoOutputWithNoCard(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = bareStandardPc(helper,
                HardwareItems.CPU_INTEGRA_CENTRO_C5_4590.get());
        helper.setBlock(PC.east(), ComputingModule.MONITOR.get());
        final MonitorBlockEntity monitor = entity(helper, PC.east());
        helper.startSequence()
                .thenExecuteAfter(LINK_TICKS, () -> {
                    helper.assertTrue(computer.ports(PortKind.VIDEO) == 1,
                            "a Centro c5 4590 gives its board one video output; had " + computer.ports(PortKind.VIDEO));
                    helper.assertTrue(helper.absolutePos(PC).equals(monitor.ownerPos()),
                            "and a monitor links to it with no graphics card in the machine");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void chipWithoutGraphics_leavesTheBoardWithNoVideoOutput(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = bareStandardPc(helper,
                HardwareItems.CPU_INTEGRA_SERVO_1231_V3.get());
        helper.setBlock(PC.east(), ComputingModule.MONITOR.get());
        final MonitorBlockEntity monitor = entity(helper, PC.east());
        helper.startSequence()
                .thenExecuteAfter(LINK_TICKS, () -> {
                    helper.assertTrue(computer.ports(PortKind.VIDEO) == 0, "a Servo carries no graphics");
                    helper.assertTrue(monitor.ownerPos() == null, "so the monitor waits for a card");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void panel_ofFlatMonitorsLinksOnceAndLightsWhole(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = TestWorldBuilder.forGameTest(helper)
                .placeRunningPersonalComputer(PC);
        // A wall three wide and two tall in front of the computer, its screens facing away from it.
        final BlockPos[] wall = wall(PC.north(), 3, 2);
        for (final BlockPos member : wall) {
            helper.setBlock(member, facing(ComputingModule.MONITOR.get(), Direction.SOUTH));
        }
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(lit(helper, wall), "every monitor of the screen lights"))
                .thenExecute(() -> {
                    int linked = 0;
                    for (final BlockPos member : wall) {
                        if (entity(helper, member).ownerPos() != null) {
                            linked++;
                        }
                        final MonitorPanel panel = entity(helper, member).panel();
                        helper.assertTrue(panel != null && panel.width() == 3 && panel.height() == 2,
                                "every monitor knows it is part of one screen three by two");
                    }
                    helper.assertTrue(linked == 1, "one monitor holds the link for the screen; linked " + linked);
                    helper.assertTrue(computer.portsInUse(PortKind.VIDEO) == 1, "the screen takes one video output");
                    int buttons = 0;
                    for (final BlockPos member : wall) {
                        if (helper.getBlockState(member).getValue(MonitorBlock.BUTTON)) {
                            buttons++;
                        }
                    }
                    helper.assertTrue(buttons == 1, "only the corner shows its power button; shown " + buttons);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void panel_picturesTubesNeverJoin(final GameTestHelper helper) {
        helper.setBlock(PC, ComputingModule.LEGACY_MONITOR.get());
        helper.setBlock(PC.east(), ComputingModule.LEGACY_MONITOR.get());
        helper.startSequence()
                .thenExecuteAfter(2, () -> helper.assertTrue(entity(helper, PC).panel() == null
                        && entity(helper, PC.east()).panel() == null, "two picture tubes side by side stay two"))
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void panel_pastEightWideStaysSeparate(final GameTestHelper helper) {
        final BlockPos[] row = wall(new BlockPos(0, 2, 2), 9, 1);
        for (final BlockPos member : row) {
            helper.setBlock(member, ComputingModule.TRANSITION_MONITOR.get());
        }
        helper.startSequence()
                .thenExecuteAfter(2, () -> helper.assertTrue(entity(helper, row[0]).panel() == null,
                        "nine flat monitors in a row are past the limit and stay separate"))
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void powerButton_switchesTheComputerOffAndOn(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = TestWorldBuilder.forGameTest(helper)
                .placeRunningPersonalComputer(PC);
        final BlockPos monitorAt = PC.east();
        helper.setBlock(monitorAt, facing(ComputingModule.MONITOR.get(), Direction.WEST));
        final ServerPlayer player = FakePlayerFactory.get(helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "button-presser"));
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(entity(helper, monitorAt).ownerPos() != null, "it links"))
                .thenExecute(() -> {
                    helper.assertTrue(computer.isRunning(), "the computer starts out running");
                    pressButton(helper, monitorAt, player);
                    helper.assertTrue(!computer.isRunning(), "the monitor's power button switches it off");
                    pressButton(helper, monitorAt, player);
                    helper.assertTrue(computer.isRunning(), "and on again");
                })
                .thenSucceed();
    }

    /* A desktop system takes a while to come up, its self-test and its own start both. */
    @GameTest(template = ARENA, timeoutTicks = 900)
    public static void picture_describesWhatTheMachineShows(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = TestWorldBuilder.forGameTest(helper)
                .placeRunningPersonalComputer(PC);
        helper.setBlock(PC.east(), facing(ComputingModule.MONITOR.get(), Direction.WEST));
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(entity(helper, PC.east()).lit(), "the monitor lights"))
                .thenWaitUntil(() -> helper.assertTrue(!computer.needsPost() && !computer.booting(),
                        "the machine comes up"))
                .thenExecute(() -> {
                    final IMonitorPicture shown = MonitorPictures.describe(helper.getLevel(),
                            entity(helper, PC.east()));
                    helper.assertTrue(shown instanceof IMonitorPicture.Desktop,
                            "a machine at its desktop shows its desktop; showed " + shown);
                    computer.togglePower();
                })
                .thenWaitUntil(() -> helper.assertTrue(!entity(helper, PC.east()).lit(), "the monitor goes dark"))
                .thenExecute(() -> helper.assertTrue(MonitorPictures.describe(helper.getLevel(),
                                entity(helper, PC.east())) == IMonitorPicture.DARK,
                        "a machine that is off shows dark glass"))
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void picture_ofAPromptCarriesItsConsole(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = TestWorldBuilder.forGameTest(helper)
                .placeRunningPersonalComputer(PC, MC_DOS);
        helper.setBlock(PC.east(), facing(ComputingModule.MONITOR.get(), Direction.WEST));
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(entity(helper, PC.east()).lit()
                        && !computer.needsPost() && !computer.booting(), "the machine comes up at its prompt"))
                .thenExecute(() -> {
                    final IMonitorPicture shown = MonitorPictures.describe(helper.getLevel(),
                            entity(helper, PC.east()));
                    helper.assertTrue(shown instanceof IMonitorPicture.Console console
                                    && !console.prompt().isEmpty(),
                            "a machine at its prompt shows its console with the prompt; showed " + shown);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void kinds_comeInTheirEras(final GameTestHelper helper) {
        helper.assertTrue(kind(ComputingModule.MONO_I_MONITOR.get()) == MonitorKind.MONO_I
                && kind(ComputingModule.VINTAGE_MONITOR.get()) == MonitorKind.MONO_II
                && kind(ComputingModule.AMBER_MONITOR.get()) == MonitorKind.AMBER
                && kind(ComputingModule.CGA_MONITOR.get()) == MonitorKind.CGA
                && kind(ComputingModule.LEGACY_MONITOR.get()) == MonitorKind.LEGACY
                && kind(ComputingModule.TRANSITION_MONITOR.get()) == MonitorKind.TRANSITION
                && kind(ComputingModule.MONITOR.get()) == MonitorKind.STANDARD
                && kind(ComputingModule.COLOR_MONITOR.get()) == MonitorKind.COLOR, "each monitor is its kind");
        helper.assertTrue(!MonitorKind.LEGACY.flat() && MonitorKind.TRANSITION.flat() && MonitorKind.COLOR.flat(),
                "only the flat panels join");
        helper.succeed();
    }

    /** Many monitors of a big screen ticking cost next to nothing with nobody near to watch. */
    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void panel_ticksCheaplyWithNobodyNear(final GameTestHelper helper) {
        final BlockPos[] wall = wall(new BlockPos(0, 2, 2), 8, 6);
        for (final BlockPos member : wall) {
            helper.setBlock(member, ComputingModule.MONITOR.get());
        }
        helper.startSequence()
                .thenExecuteAfter(2, () -> {
                    final int rounds = 100;
                    final long start = System.nanoTime();
                    for (int round = 0; round < rounds; round++) {
                        for (final BlockPos member : wall) {
                            final BlockPos at = helper.absolutePos(member);
                            MonitorBlockEntity.serverTick(helper.getLevel(), at, helper.getLevel().getBlockState(at),
                                    entity(helper, member));
                        }
                    }
                    final long perMonitorTick = (System.nanoTime() - start) / ((long) rounds * wall.length);
                    // Generous on purpose: a busy machine running the battery is slower, never this much slower.
                    helper.assertTrue(perMonitorTick < 50_000L,
                            "a monitor of a big screen ticks in under 50 microseconds; took " + perMonitorTick
                                    + " ns");
                })
                .thenSucceed();
    }

    /* Clicks the middle of the monitor's power button, as a player aiming at it would. */
    private static void pressButton(final GameTestHelper helper, final BlockPos relative, final ServerPlayer player) {
        final BlockPos at = helper.absolutePos(relative);
        final BlockState state = helper.getLevel().getBlockState(at);
        final MonitorKind kind = kind(state.getBlock());
        final Direction facing = state.getValue(MonitorBlock.FACING);
        final Direction front = facing.getOpposite();
        final Direction right = facing.getClockWise();
        final double across = (kind.button(0) + kind.button(2)) / 2.0 / MonitorKind.FRONT;
        final double down = (kind.button(1) + kind.button(3)) / 2.0 / MonitorKind.FRONT;
        // From the front's left edge as seen, along the viewer's right; the face itself, on its outer plane.
        final double fromLeft = right.getAxisDirection() == Direction.AxisDirection.POSITIVE ? across : 1.0 - across;
        final double x = right.getAxis() == Direction.Axis.X ? fromLeft : front.getStepX() > 0 ? 1.0 : 0.0;
        final double z = right.getAxis() == Direction.Axis.Z ? fromLeft : front.getStepZ() > 0 ? 1.0 : 0.0;
        final Vec3 hit = new Vec3(at.getX() + x, at.getY() + 1.0 - down, at.getZ() + z);
        state.useWithoutItem(helper.getLevel(), player, new BlockHitResult(hit, front, at, false));
    }

    private static BlockPos[] wall(final BlockPos start, final int wide, final int tall) {
        final BlockPos[] cells = new BlockPos[wide * tall];
        for (int v = 0; v < tall; v++) {
            for (int u = 0; u < wide; u++) {
                cells[v * wide + u] = start.east(u).above(v);
            }
        }
        return cells;
    }

    private static boolean lit(final GameTestHelper helper, final BlockPos[] members) {
        for (final BlockPos member : members) {
            if (!entity(helper, member).lit()) {
                return false;
            }
        }
        return true;
    }

    private static MonitorKind kind(final Block block) {
        return ((MonitorBlock) block).kind();
    }

    private static BlockState facing(final Block block, final Direction facing) {
        return block.defaultBlockState().setValue(MonitorBlock.FACING, facing);
    }

    /* A Standard personal computer with that processor and a graphics card, switched off. */
    private static PersonalComputerBlockEntity cardPc(final GameTestHelper helper, final Item cpu) {
        final PersonalComputerBlockEntity computer = bareStandardPc(helper, cpu);
        computer.getHardware().setStackInSlot(PersonalComputerBlockEntity.GPU_SLOTS_START,
                new ItemStack(ComputingModule.GPU_HD_7970.get()));
        return computer;
    }

    /* A Standard personal computer with its board, that processor, memory and supply, and no card. */
    private static PersonalComputerBlockEntity bareStandardPc(final GameTestHelper helper, final Item cpu) {
        helper.setBlock(PC, ComputingModule.PERSONAL_COMPUTER.get());
        final PersonalComputerBlockEntity computer = (PersonalComputerBlockEntity) helper.getBlockEntity(PC);
        computer.getHardware().setStackInSlot(PersonalComputerBlockEntity.MOTHERBOARD_SLOT,
                new ItemStack(HardwareItems.MOTHERBOARD_ATX_STANDARD_LGA1150.get()));
        computer.getHardware().setStackInSlot(PersonalComputerBlockEntity.CPU_SLOT, new ItemStack(cpu));
        computer.getHardware().setStackInSlot(PersonalComputerBlockEntity.RAM_SLOTS_START,
                new ItemStack(ComputingModule.RAM_DDR3_8192.get()));
        computer.getHardware().setStackInSlot(PersonalComputerBlockEntity.PSU_SLOT,
                new ItemStack(ComputingModule.PSU_650G.get()));
        return computer;
    }

    private static MonitorBlockEntity entity(final GameTestHelper helper, final BlockPos at) {
        if (!(helper.getBlockEntity(at) instanceof MonitorBlockEntity monitor)) {
            throw new IllegalStateException("no monitor at " + at);
        }
        return monitor;
    }
}
