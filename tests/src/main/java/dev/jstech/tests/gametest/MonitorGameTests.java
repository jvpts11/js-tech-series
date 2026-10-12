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
import dev.jstech.computers.menu.DesktopMenu;
import dev.jstech.computers.menu.MonitorSessionMenu;
import dev.jstech.computers.monitor.IMonitorPicture;
import dev.jstech.computers.monitor.MonitorPictures;
import dev.jstech.computers.operation.payload.OpenPostPayload;
import dev.jstech.core.peripheral.PortKind;
import dev.jstech.core.tier.HardwareEra;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestWorldBuilder;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ClientInformation;
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
 * big screen that takes one video output, the power button on the block switches the monitor itself (the whole big
 * screen from its corner, the computer running on, every screen open on it closing), the three Haswell desktop chips
 * drive a screen with no card, and what a monitor's face shows is described from what its machine holds, a start or
 * an install by the screen a player at the machine sees.
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

    /**
     * A computer switched on with parts that do not make a computer only beeps, and its monitor says so rather than
     * calling it powered off, which left a player who had pressed the switch thinking it had not taken.
     */
    @GameTest(template = ARENA)
    public static void monitor_ofAComputerOnWithPartsMissingSaysSo(final GameTestHelper helper) {
        helper.setBlock(PC, ComputingModule.PERSONAL_COMPUTER.get());
        if (!(helper.getBlockEntity(PC) instanceof PersonalComputerBlockEntity computer)) {
            throw new IllegalStateException("no personal computer at " + PC);
        }
        computer.togglePower();
        final List<Component> said = new ArrayList<>();
        final ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "tester"), ClientInformation.createDefault()) {
            @Override
            public void displayClientMessage(final Component message, final boolean actionBar) {
                said.add(message);
            }
        };
        MonitorBlock.openSession(player, helper.getLevel(), helper.absolutePos(PC.east()), helper.absolutePos(PC));
        helper.assertTrue(said.size() == 1 && said.getFirst().getContents() instanceof TranslatableContents line
                && "block.jsc.monitor.build_incomplete".equals(line.getKey()), "the monitor says the parts: " + said);
        helper.succeed();
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
    public static void powerButton_switchesTheMonitorAndLeavesTheComputerRunning(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = TestWorldBuilder.forGameTest(helper)
                .placeRunningPersonalComputer(PC);
        final BlockPos monitorAt = PC.east();
        helper.setBlock(monitorAt, facing(ComputingModule.MONITOR.get(), Direction.WEST));
        final ServerPlayer player = FakePlayerFactory.get(helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "button-presser"));
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(entity(helper, monitorAt).lit(), "the monitor lights"))
                .thenExecute(() -> {
                    helper.assertTrue(entity(helper, monitorAt).switchedOn(), "a monitor comes switched on");
                    pressButton(helper, monitorAt, player);
                    helper.assertTrue(!entity(helper, monitorAt).switchedOn(), "its button switches it off");
                    helper.assertTrue(computer.isRunning(), "and its computer runs on");
                })
                .thenWaitUntil(() -> helper.assertTrue(!entity(helper, monitorAt).lit()
                        && !helper.getBlockState(monitorAt).getValue(MonitorBlock.ON), "it goes dark, its lamp out"))
                .thenExecute(() -> {
                    helper.assertTrue(MonitorPictures.describe(helper.getLevel(), entity(helper, monitorAt))
                            == IMonitorPicture.DARK, "switched off it shows dark glass");
                    pressButton(helper, monitorAt, player);
                    helper.assertTrue(entity(helper, monitorAt).switchedOn(), "a second press switches it on");
                })
                .thenWaitUntil(() -> helper.assertTrue(entity(helper, monitorAt).lit(),
                        "and it lights again with its computer"))
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void switchedOff_closesTheScreensOpenOnItAndOpensNone(final GameTestHelper helper) {
        TestWorldBuilder.forGameTest(helper).placeRunningPersonalComputer(PC);
        final BlockPos monitorAt = PC.east();
        helper.setBlock(monitorAt, facing(ComputingModule.MONITOR.get(), Direction.WEST));
        final ServerPlayer player = FakePlayerFactory.get(helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "screen-watcher"));
        final Vec3 standing = helper.absoluteVec(Vec3.atBottomCenterOf(monitorAt.east(2)));
        player.setPos(standing.x, standing.y, standing.z);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(entity(helper, monitorAt).lit(), "the monitor lights"))
                .thenExecute(() -> {
                    final BlockPos monitor = helper.absolutePos(monitorAt);
                    final BlockPos host = helper.absolutePos(PC);
                    final MonitorSessionMenu session = new MonitorSessionMenu(1, player.getInventory(), monitor, host,
                            HardwareEra.STANDARD, MonitorSessionMenu.Phase.FIRMWARE);
                    final DesktopMenu desktop = new DesktopMenu(2, player.getInventory(), monitor, host,
                            ResourceLocation.fromNamespaceAndPath("jsc", "frames_xp"), "Desk", 8192, 0);
                    helper.assertTrue(session.stillValid(player) && desktop.stillValid(player),
                            "screens open on the monitor hold while it is on");
                    pressButton(helper, monitorAt, player);
                    helper.assertFalse(session.stillValid(player), "switched off, the session on it closes");
                    helper.assertFalse(desktop.stillValid(player), "and so does the desktop");
                    clickGlass(helper, monitorAt, player);
                    helper.assertTrue(player.containerMenu == player.inventoryMenu,
                            "a click on the glass of a monitor switched off opens nothing");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void computerOff_leavesTheMonitorSwitchedOnWithItsLampAsleep(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = TestWorldBuilder.forGameTest(helper)
                .placeRunningPersonalComputer(PC);
        final BlockPos monitorAt = PC.east();
        helper.setBlock(monitorAt, facing(ComputingModule.MONITOR.get(), Direction.WEST));
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(entity(helper, monitorAt).lit(), "the monitor lights"))
                .thenExecute(computer::togglePower)
                .thenWaitUntil(() -> helper.assertTrue(!entity(helper, monitorAt).lit(), "the monitor goes dark"))
                .thenExecute(() -> helper.assertTrue(entity(helper, monitorAt).switchedOn()
                                && helper.getBlockState(monitorAt).getValue(MonitorBlock.ON),
                        "with its computer off the monitor stays switched on, its lamp asleep"))
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void switchedOff_isKeptInTheSaveAndAnOlderSaveComesOn(final GameTestHelper helper) {
        helper.setBlock(PC, ComputingModule.LEGACY_MONITOR.get());
        final MonitorBlockEntity monitor = entity(helper, PC);
        final HolderLookup.Provider registries = helper.getLevel().registryAccess();
        monitor.switchPower(helper.getLevel());
        final CompoundTag saved = monitor.saveWithFullMetadata(registries);
        final MonitorBlockEntity loaded = new MonitorBlockEntity(monitor.getBlockPos(), monitor.getBlockState());
        loaded.loadWithComponents(saved, registries);
        helper.assertFalse(loaded.switchedOn(), "a monitor switched off is still off when its world is loaded");
        // A save from before monitors had a switch holds no key for it.
        saved.remove("SwitchedOn");
        final MonitorBlockEntity older = new MonitorBlockEntity(monitor.getBlockPos(), monitor.getBlockState());
        older.loadWithComponents(saved, registries);
        helper.assertTrue(older.switchedOn(), "a monitor from an older save comes switched on");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void panel_switchesWholeFromItsCornerButton(final GameTestHelper helper) {
        TestWorldBuilder.forGameTest(helper).placeRunningPersonalComputer(PC);
        final BlockPos[] wall = wall(PC.north(), 3, 2);
        for (final BlockPos member : wall) {
            helper.setBlock(member, facing(ComputingModule.MONITOR.get(), Direction.SOUTH));
        }
        final ServerPlayer player = FakePlayerFactory.get(helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "wall-presser"));
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(lit(helper, wall), "every monitor of the screen lights"))
                .thenExecute(() -> {
                    BlockPos corner = null;
                    for (final BlockPos member : wall) {
                        if (helper.getBlockState(member).getValue(MonitorBlock.BUTTON)) {
                            corner = member;
                        }
                    }
                    helper.assertTrue(corner != null, "the screen has its button on a corner");
                    pressButton(helper, corner, player);
                })
                .thenWaitUntil(() -> {
                    for (final BlockPos member : wall) {
                        helper.assertTrue(!entity(helper, member).switchedOn() && !entity(helper, member).lit(),
                                "the corner's button switches off every monitor of the screen");
                    }
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

    /*
     * A machine in its self-test shows the screen a player at it sees, and the same picture a tick later: its clock
     * is told as the game time it ends at, so a face is sent once a phase and not every tick, and a player's game
     * reads the same picture from both, so the face it draws goes on as it is.
     */
    @GameTest(template = ARENA, timeoutTicks = 600)
    public static void picture_ofAMachineInItsSelfTestHoldsStill(final GameTestHelper helper) {
        TestWorldBuilder.forGameTest(helper).placeRunningPersonalComputer(PC);
        helper.setBlock(PC.east(), facing(ComputingModule.MONITOR.get(), Direction.WEST));
        final IMonitorPicture[] first = new IMonitorPicture[1];
        helper.startSequence()
                .thenWaitUntil(() -> {
                    first[0] = MonitorPictures.describe(helper.getLevel(), entity(helper, PC.east()));
                    helper.assertTrue(selfTest(first[0]), "the machine shows its self-test; showed " + first[0]);
                })
                .thenExecuteAfter(1, () -> {
                    final IMonitorPicture next = MonitorPictures.describe(helper.getLevel(),
                            entity(helper, PC.east()));
                    // The self-test may just have ended; otherwise a tick changes nothing in the picture.
                    if (!selfTest(next)) {
                        return;
                    }
                    helper.assertTrue(next.equals(first[0]),
                            "a tick on, the self-test is the same picture; was " + first[0] + ", is " + next);
                    final IMonitorPicture readFirst = acrossTheWire(helper, first[0]);
                    final IMonitorPicture readNext = acrossTheWire(helper, next);
                    helper.assertTrue(readNext.equals(readFirst),
                            "a player's game reads the same picture from both; read " + readFirst + " and " + readNext);
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
        final MonitorKind kind = kind(helper.getLevel().getBlockState(helper.absolutePos(relative)).getBlock());
        clickFront(helper, relative, player, (kind.button(0) + kind.button(2)) / 2.0,
                (kind.button(1) + kind.button(3)) / 2.0);
    }

    /* Clicks the middle of the monitor's glass. */
    private static void clickGlass(final GameTestHelper helper, final BlockPos relative, final ServerPlayer player) {
        final MonitorKind kind = kind(helper.getLevel().getBlockState(helper.absolutePos(relative)).getBlock());
        clickFront(helper, relative, player, (kind.glass(0) + kind.glass(2)) / 2.0,
                (kind.glass(1) + kind.glass(3)) / 2.0);
    }

    /* Clicks the monitor's front at (x, y), in sixty-fourths of the front from its top left as seen. */
    private static void clickFront(final GameTestHelper helper, final BlockPos relative, final ServerPlayer player,
                                   final double x64, final double y64) {
        final BlockPos at = helper.absolutePos(relative);
        final BlockState state = helper.getLevel().getBlockState(at);
        final Direction facing = state.getValue(MonitorBlock.FACING);
        final Direction front = facing.getOpposite();
        final Direction right = facing.getClockWise();
        final double across = x64 / MonitorKind.FRONT;
        final double down = y64 / MonitorKind.FRONT;
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

    private static boolean selfTest(final IMonitorPicture picture) {
        return picture instanceof IMonitorPicture.Session session && session.opening() instanceof OpenPostPayload;
    }

    @GameTest(template = ARENA)
    public static void picture_ofAnUnknownKindFailsToDecode(final GameTestHelper helper) {
        final RegistryFriendlyByteBuf wire = new RegistryFriendlyByteBuf(Unpooled.buffer(),
                helper.getLevel().registryAccess());
        wire.writeByte(99);
        try {
            IMonitorPicture.STREAM_CODEC.decode(wire);
            helper.fail("an unknown picture kind decoded instead of failing");
        } catch (final IllegalArgumentException expected) {
            helper.succeed();
        }
    }

    /* The picture as a player's game reads it: written as the server sends it, and read back. */
    private static IMonitorPicture acrossTheWire(final GameTestHelper helper, final IMonitorPicture picture) {
        final RegistryFriendlyByteBuf wire = new RegistryFriendlyByteBuf(Unpooled.buffer(),
                helper.getLevel().registryAccess());
        IMonitorPicture.STREAM_CODEC.encode(wire, picture);
        return IMonitorPicture.STREAM_CODEC.decode(wire);
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
