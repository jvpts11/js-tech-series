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
import dev.jstech.computers.block.AbstractSmallComputerBlock;
import dev.jstech.computers.blockentity.AbstractSmallComputerBlockEntity;
import dev.jstech.computers.hardware.CaseFrontLayout;
import dev.jstech.computers.menu.PersonalComputerMenu;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jetbrains.annotations.Nullable;

/**
 * A click on the front of a small computer's case, in every case of every age and on every machine: the power button
 * switches the machine on and off, whatever the hand holds, and a click anywhere else on the case opens its assembly
 * screen, as it always did.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class CaseFrontClickGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos WHERE = new BlockPos(2, 2, 2);
    /** Low on the front, in sixty-fourths from its top left: plain on every case. */
    private static final double PLAIN_X = 32;
    private static final double PLAIN_Y = 56;
    private static final List<BlockEntry<? extends AbstractSmallComputerBlock<?>>> CASES = List.of(
            ComputingModule.VINTAGE_PERSONAL_COMPUTER, ComputingModule.LEGACY_PERSONAL_COMPUTER,
            ComputingModule.TRANSITION_PERSONAL_COMPUTER, ComputingModule.PERSONAL_COMPUTER,
            ComputingModule.HIGH_PERFORMANCE_PERSONAL_COMPUTER, ComputingModule.AESTHETIC_PERSONAL_COMPUTER,
            ComputingModule.ADVANCED_PERSONAL_COMPUTER, ComputingModule.ADVANCED_HIGH_PERFORMANCE_PERSONAL_COMPUTER,
            ComputingModule.ADVANCED_AESTHETIC_PERSONAL_COMPUTER,
            ComputingModule.VINTAGE_CRAFTING_COMPUTER, ComputingModule.LEGACY_CRAFTING_COMPUTER,
            ComputingModule.TRANSITION_CRAFTING_COMPUTER, ComputingModule.CRAFTING_COMPUTER,
            ComputingModule.HIGH_PERFORMANCE_CRAFTING_COMPUTER, ComputingModule.AESTHETIC_CRAFTING_COMPUTER,
            ComputingModule.ADVANCED_CRAFTING_COMPUTER, ComputingModule.ADVANCED_HIGH_PERFORMANCE_CRAFTING_COMPUTER,
            ComputingModule.ADVANCED_AESTHETIC_CRAFTING_COMPUTER,
            ComputingModule.VINTAGE_CLUSTER_MANAGEMENT_COMPUTER, ComputingModule.LEGACY_CLUSTER_MANAGEMENT_COMPUTER,
            ComputingModule.TRANSITION_CLUSTER_MANAGEMENT_COMPUTER, ComputingModule.CLUSTER_MANAGEMENT_COMPUTER,
            ComputingModule.HIGH_PERFORMANCE_CLUSTER_MANAGEMENT_COMPUTER,
            ComputingModule.AESTHETIC_CLUSTER_MANAGEMENT_COMPUTER, ComputingModule.ADVANCED_CLUSTER_MANAGEMENT_COMPUTER,
            ComputingModule.ADVANCED_HIGH_PERFORMANCE_CLUSTER_MANAGEMENT_COMPUTER,
            ComputingModule.ADVANCED_AESTHETIC_CLUSTER_MANAGEMENT_COMPUTER);

    private CaseFrontClickGameTests() {
    }

    @GameTest(template = ARENA)
    public static void powerButton_switchesTheMachineInEveryCaseWhicheverWayItFaces(final GameTestHelper helper) {
        final FakePlayer player = player(helper);
        for (int i = 0; i < CASES.size(); i++) {
            final AbstractSmallComputerBlock<?> block = CASES.get(i).get();
            final AbstractSmallComputerBlockEntity computer = place(helper, block, Direction.from2DDataValue(i % 4));
            final CaseFrontLayout.Area power = block.frontLayout().power();
            final String name = CASES.get(i).getId().getPath();
            clickFront(helper, player, middleX(power), middleY(power));
            helper.assertTrue(computer.isManualOn(), "its power button switches the " + name + " on");
            clickFront(helper, player, middleX(power), middleY(power));
            helper.assertFalse(computer.isManualOn(), "and off again");
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void powerButton_answersWhateverTheHandHolds(final GameTestHelper helper) {
        final FakePlayer player = player(helper);
        final AbstractSmallComputerBlock<?> block = ComputingModule.LEGACY_PERSONAL_COMPUTER.get();
        final AbstractSmallComputerBlockEntity computer = place(helper, block, Direction.SOUTH);
        final CaseFrontLayout.Area power = block.frontLayout().power();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ComputingModule.CD_ROM.get()));
        try {
            clickFront(helper, player, middleX(power), middleY(power));
        } finally {
            player.getInventory().clearContent();
        }
        helper.assertTrue(computer.isManualOn(), "a disc in the hand does not keep the button from going in");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void clickOffTheButton_opensTheAssemblyScreenAndLeavesThePowerAlone(final GameTestHelper helper) {
        final MenuWatcher player = new MenuWatcher(helper.getLevel());
        final AbstractSmallComputerBlockEntity computer = place(helper,
                ComputingModule.TRANSITION_PERSONAL_COMPUTER.get(), Direction.WEST);
        clickFront(helper, player, PLAIN_X, PLAIN_Y);
        final AbstractContainerMenu menu = player.opened == null ? null
                : player.opened.createMenu(1, player.getInventory(), player);
        helper.assertTrue(menu instanceof PersonalComputerMenu,
                "a click low on the front opens the assembly screen; opened: " + menu);
        helper.assertFalse(computer.isManualOn(), "and leaves the machine off");
        helper.succeed();
    }

    /* The shared fake player, empty-handed and standing. */
    private static FakePlayer player(final GameTestHelper helper) {
        final FakePlayer player = FakePlayerFactory.getMinecraft(helper.getLevel());
        player.getInventory().clearContent();
        player.setShiftKeyDown(false);
        return player;
    }

    /* The machine of that case at WHERE, its front turned to face. */
    private static AbstractSmallComputerBlockEntity place(final GameTestHelper helper,
                                                          final AbstractSmallComputerBlock<?> block,
                                                          final Direction face) {
        helper.setBlock(WHERE, block.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, face));
        return helper.getBlockEntity(WHERE);
    }

    /* Clicks the front of the case at WHERE at (x, y), in sixty-fourths of the front from its top left as seen. */
    private static void clickFront(final GameTestHelper helper, final Player player, final double x, final double y) {
        final BlockPos at = helper.absolutePos(WHERE);
        final Direction front = helper.getLevel().getBlockState(at).getValue(HorizontalDirectionalBlock.FACING);
        final Direction right = front.getCounterClockWise();
        final double across = x / EjectButton.FRONT;
        // From the front's left edge as seen, along the viewer's right; the face itself, on its outer plane.
        final double fromLow = right.getAxisDirection() == Direction.AxisDirection.POSITIVE ? across : 1.0 - across;
        final double hx = right.getAxis() == Direction.Axis.X ? fromLow : front.getStepX() > 0 ? 1.0 : 0.0;
        final double hz = right.getAxis() == Direction.Axis.Z ? fromLow : front.getStepZ() > 0 ? 1.0 : 0.0;
        final Vec3 hit = new Vec3(at.getX() + hx, at.getY() + 1.0 - y / EjectButton.FRONT, at.getZ() + hz);
        helper.useBlock(WHERE, player, new BlockHitResult(hit, front, at, false));
    }

    private static double middleX(final CaseFrontLayout.Area area) {
        return (area.left() + area.right()) / 2.0;
    }

    private static double middleY(final CaseFrontLayout.Area area) {
        return (area.top() + area.bottom()) / 2.0;
    }

    /* A fake player who keeps what a click would open, which a fake player otherwise never opens. */
    private static final class MenuWatcher extends FakePlayer {

        @Nullable
        private MenuProvider opened;

        MenuWatcher(final ServerLevel level) {
            super(level, new GameProfile(UUID.randomUUID(), "case-clicker"));
        }

        @Override
        public OptionalInt openMenu(@Nullable final MenuProvider provider,
                                    final Consumer<RegistryFriendlyByteBuf> extraData) {
            opened = provider;
            return OptionalInt.empty();
        }
    }
}
