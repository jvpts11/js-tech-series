/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.computers;

import com.mojang.authlib.GameProfile;
import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.block.MonitorBlock;
import dev.jstech.computers.block.MonitorKind;
import dev.jstech.computers.block.MonitorPanel;
import dev.jstech.computers.blockentity.MonitorBlockEntity;
import dev.jstech.computers.menu.IMonitorMenu;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.computers.TestWorldBuilder;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

/**
 * The touch monitors (computers/docs/design/peripherals.md): the Standard's and the Advanced's flat panels with a touch
 * layer; a touch on one's glass opens what its computer shows for the player who touched it, as a click on a monitor
 * does, and a touch reaches nothing on a monitor without one; touch monitors join only their own kind.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class TouchMonitorGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos PC = new BlockPos(2, 2, 3);

    private TouchMonitorGameTests() {
    }

    @GameTest(template = ARENA)
    public static void kinds_areTheFlatPanelsOfTheirEraWithATouchLayer(final GameTestHelper helper) {
        final MonitorKind standard = kind(ComputingModule.TOUCH_MONITOR.get());
        final MonitorKind advanced = kind(ComputingModule.ADVANCED_TOUCH_MONITOR.get());
        helper.assertTrue(standard == MonitorKind.TOUCH_STANDARD && advanced == MonitorKind.TOUCH_ADVANCED,
                "each touch monitor is its kind");
        helper.assertTrue(standard.touch() && advanced.touch() && standard.flat() && advanced.flat(),
                "both are flat panels with a touch layer");
        helper.assertTrue(standard.era() == HardwareEra.STANDARD && advanced.era() == HardwareEra.ADVANCED,
                "of the Standard and the Advanced");
        helper.assertTrue(!MonitorKind.STANDARD.touch() && !MonitorKind.COLOR.touch(),
                "the plain flat panels are not touch screens");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void touchMonitors_joinOnlyTheirOwnKind(final GameTestHelper helper) {
        final BlockPos left = new BlockPos(2, 2, 2);
        helper.setBlock(left, facing(ComputingModule.TOUCH_MONITOR.get(), Direction.SOUTH));
        helper.setBlock(left.east(), facing(ComputingModule.MONITOR.get(), Direction.SOUTH));
        final BlockPos pair = new BlockPos(2, 4, 2);
        helper.setBlock(pair, facing(ComputingModule.TOUCH_MONITOR.get(), Direction.SOUTH));
        helper.setBlock(pair.east(), facing(ComputingModule.TOUCH_MONITOR.get(), Direction.SOUTH));
        helper.startSequence()
                .thenExecuteAfter(5, () -> {
                    helper.assertTrue(entity(helper, left).panel() == null && entity(helper, left.east()).panel()
                            == null, "a touch monitor beside a plain one joins nothing");
                    final MonitorPanel joined = entity(helper, pair).panel();
                    helper.assertTrue(joined != null && joined.width() == 2 && joined.height() == 1,
                            "two touch monitors side by side make one screen");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void touch_opensWhatTheComputerShowsForThePlayer(final GameTestHelper helper) {
        TestWorldBuilder.forGameTest(helper).placeRunningPersonalComputer(PC);
        final BlockPos touchAt = PC.east();
        final BlockPos plainAt = PC.west();
        helper.setBlock(touchAt, facing(ComputingModule.TOUCH_MONITOR.get(), Direction.WEST));
        helper.setBlock(plainAt, facing(ComputingModule.MONITOR.get(), Direction.EAST));
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(entity(helper, touchAt).lit(), "the touch monitor lights"))
                .thenExecute(() -> {
                    // The player is only in the world while the touches are tried, and leaves it whatever they show.
                    final ServerPlayer player = player(helper, touchAt.east(2));
                    try {
                        MonitorBlock.touched(player, helper.getLevel(), helper.absolutePos(plainAt));
                        helper.assertTrue(player.containerMenu == player.inventoryMenu,
                                "a touch reaches nothing on a monitor with no touch layer");
                        MonitorBlock.touched(player, helper.getLevel(), helper.absolutePos(touchAt));
                        helper.assertTrue(player.containerMenu instanceof IMonitorMenu open
                                        && open.monitorPos().equals(helper.absolutePos(touchAt)),
                                "a touch on the glass opens the computer's screen on that monitor: "
                                        + player.containerMenu);
                    } finally {
                        helper.getLevel().getServer().getPlayerList().remove(player);
                    }
                })
                .thenSucceed();
    }

    private static MonitorKind kind(final Block block) {
        return ((MonitorBlock) block).kind();
    }

    private static BlockState facing(final Block block, final Direction direction) {
        return block.defaultBlockState().setValue(MonitorBlock.FACING, direction);
    }

    private static MonitorBlockEntity entity(final GameTestHelper helper, final BlockPos relative) {
        return (MonitorBlockEntity) helper.getBlockEntity(relative);
    }

    /* A player on the server with a connection that takes what is sent, standing at {@code at}. */
    private static ServerPlayer player(final GameTestHelper helper, final BlockPos at) {
        final ServerLevel level = helper.getLevel();
        final CommonListenerCookie cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(),
                "toucher"), false);
        final ServerPlayer player = new ServerPlayer(level.getServer(), level, cookie.gameProfile(),
                cookie.clientInformation());
        final Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        NetworkRegistry.configureMockConnection(connection);
        level.getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
        for (int id = 0; id <= 8; id++) {
            player.connection.handleAcceptTeleportPacket(new ServerboundAcceptTeleportationPacket(id));
        }
        player.setGameMode(GameType.SURVIVAL);
        final Vec3 there = helper.absoluteVec(Vec3.atBottomCenterOf(at));
        player.moveTo(there.x, there.y, there.z, 0, 0);
        return player;
    }
}
