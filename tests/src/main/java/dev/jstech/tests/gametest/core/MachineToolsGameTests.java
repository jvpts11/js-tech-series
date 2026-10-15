/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import com.mojang.authlib.GameProfile;
import dev.jstech.core.connect.RelativeFace;
import dev.jstech.core.energy.EnergyProtection;
import dev.jstech.core.inventory.FaceMode;
import dev.jstech.core.jstechseries.tools.SharedTools;
import dev.jstech.core.machine.MachineBlockEntity;
import dev.jstech.core.machine.MachineResource;
import dev.jstech.core.machine.MachineSetup;
import dev.jstech.core.machine.MachineWrench;
import dev.jstech.core.machine.RedstoneMode;
import dev.jstech.core.team.Ownership;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestFuses;
import dev.jstech.tests.TestItems;
import dev.jstech.tests.TestKitMachine;
import dev.jstech.tests.TestKitMachine.KitMachineBlockEntity;
import dev.jstech.tests.TestPress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The tools that set machines up: a wrench of the common tag turns what a face does and takes a machine away whole,
 * its contents going with it and coming back when it is placed again, for its owner only; broken instead, a machine
 * spills its fuse and battery with its items; the Configuration Card copies a setup and pastes it whole into a machine
 * of the same kind, what fits into another, and into every machine of a box; and an installer grows a machine into
 * the next tier of its family in its place, its items moved into the slots of the same use.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class MachineToolsGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos AT = new BlockPos(2, 2, 2);
    private static final long STORED = 5_000L;

    private MachineToolsGameTests() {
    }

    @GameTest(template = ARENA)
    public static void wrench_turnsTheFaceItIsUsedOn(final GameTestHelper helper) {
        final FakePlayer owner = player(helper, "wrench_owner");
        final KitMachineBlockEntity machine = kit(helper, AT, owner);
        final ItemStack wrench = new ItemStack(SharedTools.WRENCH.get());
        helper.assertTrue(MachineWrench.isWrench(wrench), "the Basic Wrench is a wrench of the common tag");
        use(helper, owner, wrench, AT, Direction.NORTH);
        helper.assertTrue(machine.sides().mode(MachineResource.ITEMS, RelativeFace.FRONT) == FaceMode.NONE,
                "its front goes from both to neither: " + machine.sides().mode(MachineResource.ITEMS,
                        RelativeFace.FRONT));
        MachineWrench.step(wrench, machine, true);
        helper.assertTrue(MachineWrench.shown(wrench, machine) == MachineResource.GAS,
                "the wheel shows its next resource, its gas");
        use(helper, owner, wrench, AT, Direction.NORTH);
        helper.assertTrue(machine.sides().mode(MachineResource.GAS, RelativeFace.FRONT) == FaceMode.NONE
                        && machine.sides().mode(MachineResource.ITEMS, RelativeFace.FRONT) == FaceMode.NONE,
                "and the next use turns the gas alone");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void wrench_takesTheMachineAwayWholeAndItComesBack(final GameTestHelper helper) {
        final FakePlayer owner = player(helper, "wrench_taker");
        owner.getInventory().clearContent();
        final KitMachineBlockEntity machine = kit(helper, AT, owner);
        machine.getInventory().setStackInSlot(0, new ItemStack(Items.COBBLESTONE, 5));
        machine.sides().set(MachineResource.ITEMS, RelativeFace.BACK, FaceMode.OUT);
        owner.setShiftKeyDown(true);
        use(helper, owner, new ItemStack(SharedTools.WRENCH.get()), AT, Direction.UP);
        helper.assertBlockNotPresent(TestKitMachine.BLOCK.get(), AT);
        helper.assertItemEntityNotPresent(Items.COBBLESTONE, AT, 3.0);
        ItemStack taken = ItemStack.EMPTY;
        for (int slot = 0; slot < owner.getInventory().getContainerSize(); slot++) {
            if (owner.getInventory().getItem(slot).is(TestKitMachine.BLOCK.get().asItem())) {
                taken = owner.getInventory().getItem(slot);
            }
        }
        helper.assertTrue(taken.has(MachineWrench.TAKEN_WHOLE.get())
                && taken.has(DataComponents.BLOCK_ENTITY_DATA), "its item holds the whole machine: " + taken);
        final BlockPos ground = AT.below();
        owner.setShiftKeyDown(false);
        owner.setItemInHand(InteractionHand.MAIN_HAND, taken);
        taken.useOn(new UseOnContext(owner, InteractionHand.MAIN_HAND, new BlockHitResult(
                Vec3.atCenterOf(helper.absolutePos(ground)), Direction.UP, helper.absolutePos(ground), false)));
        final KitMachineBlockEntity back = helper.getBlockEntity(AT);
        helper.assertTrue(back.getInventory().getStackInSlot(0).getCount() == 5, "placed again, it holds its items");
        helper.assertTrue(back.power().stored() == STORED, "its energy");
        helper.assertTrue(back.sides().mode(MachineResource.ITEMS, RelativeFace.BACK) == FaceMode.OUT,
                "and its sides");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void wrench_ofAStrangerChangesNothing(final GameTestHelper helper) {
        final FakePlayer owner = player(helper, "wrench_keeper");
        final FakePlayer stranger = player(helper, "wrench_stranger");
        final KitMachineBlockEntity machine = kit(helper, AT, owner);
        use(helper, stranger, new ItemStack(SharedTools.WRENCH.get()), AT, Direction.NORTH);
        stranger.setShiftKeyDown(true);
        use(helper, stranger, new ItemStack(SharedTools.WRENCH.get()), AT, Direction.NORTH);
        helper.assertBlockPresent(TestKitMachine.BLOCK.get(), AT);
        helper.assertTrue(machine.sides().mode(MachineResource.ITEMS, RelativeFace.FRONT) == FaceMode.BOTH,
                "a stranger's wrench neither turns a face nor takes the machine");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void broken_spillsTheFuseAndTheBatteryWithItsItems(final GameTestHelper helper) {
        final KitMachineBlockEntity machine = kit(helper, AT, player(helper, "breaker"));
        final EnergyProtection protection = machine.power().protection();
        protection.slots().setStackInSlot(EnergyProtection.FUSE, new ItemStack(TestFuses.ONE_CLASS.get()));
        protection.slots().setStackInSlot(EnergyProtection.BATTERY, new ItemStack(TestItems.FULL.get()));
        machine.getInventory().setStackInSlot(0, new ItemStack(Items.COBBLESTONE, 2));
        helper.getLevel().destroyBlock(helper.absolutePos(AT), false);
        helper.assertItemEntityPresent(TestFuses.ONE_CLASS.get(), AT, 3.0);
        helper.assertItemEntityPresent(TestItems.FULL.get(), AT, 3.0);
        helper.assertItemEntityPresent(Items.COBBLESTONE, AT, 3.0);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void card_copiesASetupAndPastesItWholeIntoTheSameKind(final GameTestHelper helper) {
        final FakePlayer owner = player(helper, "card_owner");
        final KitMachineBlockEntity from = kit(helper, AT, owner);
        final KitMachineBlockEntity into = kit(helper, AT.east(2), owner);
        from.sides().set(MachineResource.ITEMS, RelativeFace.FRONT, FaceMode.OUT);
        from.control().setMode(RedstoneMode.HIGH);
        from.getInventory().setStackInSlot(0, new ItemStack(Items.COBBLESTONE));
        from.locks().lock(0, new ItemStack(Items.COBBLESTONE));
        final ItemStack card = new ItemStack(SharedTools.CARD.get());
        owner.setShiftKeyDown(true);
        firstUse(helper, owner, card, AT);
        helper.assertTrue(card.has(SharedTools.SETUP.get()), "sneaking, the card copies the setup");
        owner.setShiftKeyDown(false);
        firstUse(helper, owner, card, AT.east(2));
        helper.assertTrue(into.sides().mode(MachineResource.ITEMS, RelativeFace.FRONT) == FaceMode.OUT,
                "the other machine takes its sides");
        helper.assertTrue(into.control().mode() == RedstoneMode.HIGH, "its redstone mode");
        helper.assertTrue(into.locks().locked(0).is(Items.COBBLESTONE), "and, of the same kind, its locks");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void card_pastesWhatFitsIntoAnotherKind(final GameTestHelper helper) {
        final FakePlayer owner = player(helper, "card_fitter");
        final KitMachineBlockEntity from = kit(helper, AT, owner);
        from.sides().set(MachineResource.ITEMS, RelativeFace.LEFT, FaceMode.IN);
        from.locks().lock(0, new ItemStack(Items.COBBLESTONE));
        helper.setBlock(AT.east(2), TestKitMachine.FACTORY.get());
        final MachineBlockEntity factory = helper.getBlockEntity(AT.east(2));
        factory.own(Ownership.of(owner.getUUID()));
        helper.assertTrue(MachineSetup.paste(owner, MachineSetup.copy(from), factory), "the factory takes some");
        helper.assertTrue(factory.sides().mode(MachineResource.ITEMS, RelativeFace.LEFT) == FaceMode.IN,
                "the sides of the items both have");
        helper.assertTrue(((TestKitMachine.FactoryBlockEntity) factory).locks().locked(0).isEmpty(),
                "but no locks, which only a machine of the same kind takes");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void card_pastesOverEveryMachineOfABox(final GameTestHelper helper) {
        final FakePlayer owner = player(helper, "card_boxer");
        final KitMachineBlockEntity from = kit(helper, AT, owner);
        from.control().setMode(RedstoneMode.LOW);
        final KitMachineBlockEntity a = kit(helper, new BlockPos(1, 2, 5), owner);
        final KitMachineBlockEntity b = kit(helper, new BlockPos(3, 2, 6), owner);
        helper.setBlock(new BlockPos(1, 2, 4), Blocks.STONE);
        final ItemStack card = new ItemStack(SharedTools.CARD.get());
        card.set(SharedTools.SETUP.get(), MachineSetup.copy(from));
        firstUse(helper, owner, card, new BlockPos(1, 2, 4));
        helper.assertTrue(card.has(SharedTools.CORNER.get()), "a block that is not a machine marks a corner");
        firstUse(helper, owner, card, new BlockPos(4, 3, 7));
        helper.assertTrue(a.control().mode() == RedstoneMode.LOW && b.control().mode() == RedstoneMode.LOW,
                "the other corner pastes into every machine of the box");
        helper.assertTrue(!card.has(SharedTools.CORNER.get()), "and the corner is forgotten");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void installer_growsTheMachineIntoTheNextTierInItsPlace(final GameTestHelper helper) {
        final FakePlayer owner = player(helper, "grower");
        final KitMachineBlockEntity machine = kit(helper, AT, owner);
        machine.getInventory().setStackInSlot(0, new ItemStack(Items.COBBLESTONE, 4));
        machine.getInventory().setStackInSlot(TestKitMachine.OUT, new ItemStack(Items.STONE, 2));
        machine.getInventory().setStackInSlot(TestKitMachine.LAYOUT.firstUpgrade(),
                new ItemStack(TestPress.UPGRADE.get()));
        machine.sides().set(MachineResource.ITEMS, RelativeFace.TOP, FaceMode.IN);
        final ItemStack installer = new ItemStack(TestKitMachine.INSTALLER.get(), 2);
        firstUse(helper, owner, installer, AT);
        helper.assertBlockPresent(TestKitMachine.BLOCK_MK2.get(), AT);
        final KitMachineBlockEntity grown = helper.getBlockEntity(AT);
        helper.assertTrue(grown.getInventory().getStackInSlot(0).getCount() == 4, "its input stays its input");
        helper.assertTrue(grown.getInventory().getStackInSlot(TestKitMachine.OUT_MK2).is(Items.STONE),
                "its output moves to the second tier's output");
        helper.assertTrue(grown.getInventory().getStackInSlot(TestKitMachine.LAYOUT_MK2.firstUpgrade())
                .is(TestPress.UPGRADE.get()), "its upgrade to the second tier's bay");
        helper.assertTrue(grown.power().stored() == STORED, "its energy is kept");
        helper.assertTrue(grown.sides().mode(MachineResource.ITEMS, RelativeFace.TOP) == FaceMode.IN,
                "and its sides");
        helper.assertTrue(installer.getCount() == 1, "one installer is used up");
        firstUse(helper, owner, installer, AT);
        helper.assertTrue(installer.getCount() == 1, "and none on a machine already of its tier");
        helper.succeed();
    }

    private static KitMachineBlockEntity kit(final GameTestHelper helper, final BlockPos pos, final FakePlayer owner) {
        helper.setBlock(pos, TestKitMachine.BLOCK.get());
        final KitMachineBlockEntity machine = helper.getBlockEntity(pos);
        machine.own(Ownership.of(owner.getUUID()));
        machine.power().portStore().setStored(STORED);
        return machine;
    }

    /* Uses {@code stack} on {@code pos}'s {@code face} as a player's click does, events and all. */
    private static void use(final GameTestHelper helper, final FakePlayer player, final ItemStack stack,
                            final BlockPos pos, final Direction face) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        final BlockPos at = helper.absolutePos(pos);
        player.gameMode.useItemOn(player, helper.getLevel(), stack, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(at).relative(face, 0.5), face, at, false));
    }

    /* Uses an item that acts before the block it is used on, as a player's click reaches it first. */
    private static void firstUse(final GameTestHelper helper, final FakePlayer player, final ItemStack stack,
                                 final BlockPos pos) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        final BlockPos at = helper.absolutePos(pos);
        stack.getItem().onItemUseFirst(stack, new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(at), Direction.UP, at, false)));
    }

    private static FakePlayer player(final GameTestHelper helper, final String name) {
        return FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.nameUUIDFromBytes(name.getBytes(
                StandardCharsets.UTF_8)), name));
    }
}
