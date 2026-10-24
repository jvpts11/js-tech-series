/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.industrial;

import com.mojang.authlib.GameProfile;
import dev.jstech.core.connect.RelativeFace;
import dev.jstech.core.energy.EnergyProtection;
import dev.jstech.core.inventory.FaceMode;
import dev.jstech.core.jstechseries.tools.SharedTools;
import dev.jstech.core.machine.MachineResource;
import dev.jstech.core.machine.MachineWrench;
import dev.jstech.core.team.Ownership;
import dev.jstech.industrial.IndustrialModule;
import dev.jstech.industrial.blockentity.CompressorBlockEntity;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestIndustrialEnergy;
import dev.jstech.tests.TestOvenBattery;
import dev.jstech.tests.TestUpgrades;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The wrench on J's Industrial's machines, any wrench of the common tag: sneaking, it takes a machine away whole, its
 * contents, upgrades, battery, energy and settings in its item, and a multiblock's terminal with what it holds, the
 * structure forming again once the terminal is back.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class IndustrialWrenchGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos AT = new BlockPos(2, 2, 2);
    private static final BlockPos TERMINAL = new BlockPos(2, 2, 1);
    private static final long STORED = 1_234L;

    private IndustrialWrenchGameTests() {
    }

    @GameTest(template = ARENA)
    public static void wrench_takesAMachineAwayWithItsContentsAndItComesBackAsItWas(final GameTestHelper helper) {
        final FakePlayer owner = player(helper, "industrial_wrench_taker");
        owner.getInventory().clearContent();
        helper.setBlock(AT, IndustrialModule.COMPRESSOR.get());
        final CompressorBlockEntity machine = helper.getBlockEntity(AT);
        machine.own(Ownership.of(owner.getUUID()));
        machine.getInventory().setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 7));
        machine.getInventory().setStackInSlot(machine.layout().firstUpgrade(),
                new ItemStack(TestUpgrades.MOTOR.get(), 2));
        Objects.requireNonNull(machine.power().protection()).slots().setStackInSlot(EnergyProtection.BATTERY,
                new ItemStack(TestIndustrialEnergy.BATTERY.get()));
        Objects.requireNonNull(machine.power().portStore()).setStored(STORED);
        machine.sides().set(MachineResource.ITEMS, RelativeFace.BACK, FaceMode.OUT);
        final ItemStack taken = takeWhole(helper, owner, AT, IndustrialModule.COMPRESSOR.get().asItem());
        helper.assertItemEntityNotPresent(Items.IRON_INGOT, AT, 3.0);
        placeBack(helper, owner, taken, AT);
        final CompressorBlockEntity back = helper.getBlockEntity(AT);
        helper.assertTrue(back.getInventory().getStackInSlot(0).getCount() == 7, "placed again, it holds its ingots");
        helper.assertTrue(back.upgradesOf(TestUpgrades.MOTOR.get()) == 2, "its upgrades");
        helper.assertTrue(Objects.requireNonNull(back.power().protection()).slots()
                .getStackInSlot(EnergyProtection.BATTERY).is(TestIndustrialEnergy.BATTERY.get()), "its battery");
        helper.assertTrue(back.power().stored() == STORED, "its energy: " + back.power().stored());
        helper.assertTrue(back.sides().mode(MachineResource.ITEMS, RelativeFace.BACK) == FaceMode.OUT,
                "and its sides");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void wrench_takesATerminalAwayWithWhatItHoldsAndTheStructureFormsAgain(final GameTestHelper helper) {
        final FakePlayer owner = player(helper, "industrial_terminal_taker");
        owner.getInventory().clearContent();
        build(helper);
        final TestOvenBattery.TerminalBlockEntity terminal = helper.getBlockEntity(TERMINAL);
        terminal.own(Ownership.of(owner.getUUID()));
        terminal.getInventory().setStackInSlot(0, new ItemStack(Items.COBBLESTONE, 9));
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(formed(helper), "the battery forms"))
                .thenExecute(() -> {
                    final ItemStack taken = takeWhole(helper, owner, TERMINAL,
                            TestOvenBattery.TERMINAL.get().asItem());
                    helper.assertItemEntityNotPresent(Items.COBBLESTONE, TERMINAL, 3.0);
                    placeBack(helper, owner, taken, TERMINAL);
                })
                .thenWaitUntil(() -> helper.assertTrue(formed(helper), "placed again, the battery forms again"))
                .thenExecute(() -> {
                    final TestOvenBattery.TerminalBlockEntity back = helper.getBlockEntity(TERMINAL);
                    // Unpowered, it bakes nothing: what it holds is what it held.
                    helper.assertTrue(back.getInventory().getStackInSlot(0).is(Items.COBBLESTONE)
                            && back.getInventory().getStackInSlot(0).getCount() == 9,
                            "its terminal holds what it held");
                })
                .thenSucceed();
    }

    /* Takes the machine at {@code at} away whole with a sneaking wrench; the item it gave {@code player}. */
    private static ItemStack takeWhole(final GameTestHelper helper, final FakePlayer player, final BlockPos at,
                                       final Item machine) {
        player.setShiftKeyDown(true);
        final ItemStack wrench = new ItemStack(SharedTools.WRENCH.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, wrench);
        final BlockPos pos = helper.absolutePos(at);
        player.gameMode.useItemOn(player, helper.getLevel(), wrench, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos).relative(Direction.UP, 0.5), Direction.UP, pos, false));
        player.setShiftKeyDown(false);
        helper.assertTrue(helper.getLevel().getBlockState(pos).isAir(), "the wrench takes it away");
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            final ItemStack held = player.getInventory().getItem(slot);
            if (held.is(machine)) {
                helper.assertTrue(held.has(MachineWrench.TAKEN_WHOLE.get()), "its item holds it whole: " + held);
                return held;
            }
        }
        helper.fail("the wrench gave no " + machine);
        return ItemStack.EMPTY;
    }

    /* Places {@code taken} back at {@code at}, as a player does. */
    private static void placeBack(final GameTestHelper helper, final FakePlayer player, final ItemStack taken,
                                  final BlockPos at) {
        player.setItemInHand(InteractionHand.MAIN_HAND, taken);
        final BlockPos ground = helper.absolutePos(at.below());
        taken.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(ground),
                Direction.UP, ground, false)));
    }

    /* A battery of one oven, its terminal at {@link #TERMINAL}, its depth along z. */
    private static void build(final GameTestHelper helper) {
        for (int dz = 0; dz <= 2; dz++) {
            for (int dy = 0; dy <= 1; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    if (dx != 0 || dy != 0 || dz != 0) {
                        helper.setBlock(TERMINAL.offset(dx, dy, dz), TestOvenBattery.BRICKS.get());
                    }
                }
            }
        }
        helper.setBlock(TERMINAL, TestOvenBattery.TERMINAL.get());
    }

    private static boolean formed(final GameTestHelper helper) {
        return helper.getBlockEntity(TERMINAL) instanceof TestOvenBattery.TerminalBlockEntity terminal
                && terminal.structure() != null && terminal.structure().formed();
    }

    private static FakePlayer player(final GameTestHelper helper, final String name) {
        return FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.nameUUIDFromBytes(name.getBytes(
                StandardCharsets.UTF_8)), name));
    }
}
