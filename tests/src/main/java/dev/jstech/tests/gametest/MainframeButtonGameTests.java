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
import dev.jstech.computers.block.MainframeBlock;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.menu.MainframeMenu;
import dev.jstech.computers.os.media.EjectButton;
import dev.jstech.core.content.BlockEntry;
import dev.jstech.tests.JsTests;
import java.util.List;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jetbrains.annotations.Nullable;

/**
 * The Mainframe's power button, on the sill in the middle of its cabinet: a click on it switches the machine on and
 * off in every era, and a click anywhere else on the cabinet's front still opens its assembly screen.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class MainframeButtonGameTests {

    private static final String ARENA = "empty";
    /** The controller, the middle of the cabinet's bottom front row, with room for the cabinet round it. */
    private static final BlockPos WHERE = new BlockPos(3, 2, 2);
    private static final List<BlockEntry<MainframeBlock>> ERAS = List.of(ComputingModule.VINTAGE_MAINFRAME,
            ComputingModule.LEGACY_MAINFRAME, ComputingModule.TRANSITION_MAINFRAME, ComputingModule.MAINFRAME,
            ComputingModule.ADVANCED_MAINFRAME);

    private MainframeButtonGameTests() {
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void powerButton_switchesTheMainframeInEveryEra(final GameTestHelper helper) {
        final FakePlayer player = FakePlayerFactory.getMinecraft(helper.getLevel());
        player.getInventory().clearContent();
        for (final BlockEntry<MainframeBlock> era : ERAS) {
            // The last era's cabinet goes first, so each is a cabinet of its own.
            helper.setBlock(WHERE, Blocks.AIR);
            helper.setBlock(WHERE, era.get().defaultBlockState());
            final MainframeBlockEntity mainframe = helper.getBlockEntity(WHERE);
            click(helper, player, MainframeBlock.POWER_BUTTON.middleX(), MainframeBlock.POWER_BUTTON.middleY());
            helper.assertTrue(mainframe.isManualOn(), "the button switches the " + era.getId().getPath() + " on");
            click(helper, player, MainframeBlock.POWER_BUTTON.middleX(), MainframeBlock.POWER_BUTTON.middleY());
            helper.assertFalse(mainframe.isManualOn(), "and off again");
        }
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void clickOffTheButton_opensTheAssemblyScreen(final GameTestHelper helper) {
        helper.setBlock(WHERE, ComputingModule.MAINFRAME.get().defaultBlockState());
        final MainframeBlockEntity mainframe = helper.getBlockEntity(WHERE);
        final MenuWatcher player = new MenuWatcher(helper.getLevel());
        // The middle of the controller's front, above the sill.
        click(helper, player, 32, 20);
        final AbstractContainerMenu menu = player.opened == null ? null
                : player.opened.createMenu(1, player.getInventory(), player);
        helper.assertTrue(menu instanceof MainframeMenu, "a click above the sill opens the assembly screen: " + menu);
        helper.assertFalse(mainframe.isManualOn(), "and leaves the machine off");
        helper.succeed();
    }

    /* Clicks the controller's front at (x, y), in sixty-fourths from its top left as seen. */
    private static void click(final GameTestHelper helper, final Player player, final double x, final double y) {
        final BlockPos at = helper.absolutePos(WHERE);
        final Direction front = helper.getLevel().getBlockState(at).getValue(HorizontalDirectionalBlock.FACING);
        final Direction right = front.getCounterClockWise();
        final double across = x / EjectButton.FRONT;
        final double fromLow = right.getAxisDirection() == Direction.AxisDirection.POSITIVE ? across : 1.0 - across;
        final double hx = right.getAxis() == Direction.Axis.X ? fromLow : front.getStepX() > 0 ? 1.0 : 0.0;
        final double hz = right.getAxis() == Direction.Axis.Z ? fromLow : front.getStepZ() > 0 ? 1.0 : 0.0;
        final Vec3 hit = new Vec3(at.getX() + hx, at.getY() + 1.0 - y / EjectButton.FRONT, at.getZ() + hz);
        helper.useBlock(WHERE, player, new BlockHitResult(hit, front, at, false));
    }

    /* A fake player who keeps what a click would open, which a fake player otherwise never opens. */
    private static final class MenuWatcher extends FakePlayer {

        @Nullable
        private MenuProvider opened;

        MenuWatcher(final ServerLevel level) {
            super(level, new GameProfile(UUID.randomUUID(), "mainframe-clicker"));
        }

        @Override
        public OptionalInt openMenu(@Nullable final MenuProvider provider,
                                    final Consumer<RegistryFriendlyByteBuf> extraData) {
            opened = provider;
            return OptionalInt.empty();
        }
    }
}
