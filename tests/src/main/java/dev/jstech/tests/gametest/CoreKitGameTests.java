/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.blockentity.CraftingComputerBlockEntity;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.blockentity.SpeakerBlockEntity;
import dev.jstech.computers.blockentity.TankBlockEntity;
import dev.jstech.computers.crafting.CraftingPattern;
import dev.jstech.core.material.MaterialForm;
import dev.jstech.core.material.MaterialItems;
import dev.jstech.core.material.ModMaterial;
import dev.jstech.core.peripheral.PortKind;
import dev.jstech.industrial.IndustrialModule;
import dev.jstech.industrial.blockentity.CoalGeneratorBlockEntity;
import dev.jstech.industrial.blockentity.CompressorBlockEntity;
import dev.jstech.industrial.blockentity.ProcessingMachineBlockEntity;
import dev.jstech.industrial.menu.ProcessingMachineMenu;
import dev.jstech.tests.JsTests;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The Core's construction kit as the machines use it: fields declared once and saved, loaded, sent and shown to the
 * menu from the declaration; exposed inventories, energy and tanks offered to every side; values that may hold
 * nothing, and parts that write themselves; a device block that spills its inventories and frees a peripheral's
 * place as it is broken; and a menu whose shift-clicks follow its declared routes and that closes once its machine
 * is gone.
 * That using a machine opens its menu is shown by a client test, with a real player.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class CoreKitGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos MACHINE = new BlockPos(2, 2, 2);
    /** Where a peripheral's owner stands, beside the machine. */
    private static final BlockPos OWNER = new BlockPos(4, 2, 2);
    /** Where a player stands to use the machine: in front of it, within reach. */
    private static final Vec3 IN_FRONT = new Vec3(2.5, 2, 4.5);

    private CoreKitGameTests() {
    }

    @GameTest(template = ARENA)
    public static void fields_saveAndLoadWhatIsDeclaredSaved(final GameTestHelper helper) {
        final CompressorBlockEntity machine = compressor(helper);
        machine.getInventory().setStackInSlot(ProcessingMachineBlockEntity.INPUT_SLOT,
                new ItemStack(Items.IRON_INGOT, 3));
        machine.getEnergy().setEnergyStored(5_000);
        final HolderLookup.Provider registries = helper.getLevel().registryAccess();

        final CompoundTag saved = machine.saveWithFullMetadata(registries);
        final BlockEntity loaded = BlockEntity.loadStatic(machine.getBlockPos(), machine.getBlockState(), saved,
                registries);

        helper.assertTrue(loaded instanceof CompressorBlockEntity, "the save loads back into a Compressor");
        final CompressorBlockEntity copy = (CompressorBlockEntity) loaded;
        helper.assertTrue(copy.getInventory().getStackInSlot(ProcessingMachineBlockEntity.INPUT_SLOT).getCount() == 3,
                "the inventory is saved");
        helper.assertTrue(copy.getEnergy().getEnergyStored() == 5_000, "the stored energy is saved");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void fields_sendThePlayersOnlyWhatIsDeclaredForThem(final GameTestHelper helper) {
        final CompressorBlockEntity machine = compressor(helper);
        machine.getInventory().setStackInSlot(ProcessingMachineBlockEntity.INPUT_SLOT,
                new ItemStack(Items.IRON_INGOT, 3));

        final CompoundTag update = machine.getUpdateTag(helper.getLevel().registryAccess());

        helper.assertTrue(update.isEmpty(), "a machine that declares nothing for the players sends them nothing, got "
                + update);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void fields_offerTheExposedInventoryAndEnergyOnEverySide(final GameTestHelper helper) {
        final CompressorBlockEntity machine = compressor(helper);
        final BlockPos at = helper.absolutePos(MACHINE);
        for (final Direction side : Direction.values()) {
            helper.assertTrue(helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, at, side)
                    == machine.getInventory(), "the inventory is offered on the " + side + " side");
            helper.assertTrue(helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, at, side)
                    == machine.getEnergy(), "the energy is offered on the " + side + " side");
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void menuData_carriesTheFieldsBothWays(final GameTestHelper helper) {
        final CompressorBlockEntity machine = compressor(helper);
        final ContainerData data = machine.fields().menuData();
        machine.getEnergy().setEnergyStored(1_234);

        helper.assertTrue(data.getCount() == 3, "energy, progress and the processing time, got " + data.getCount());
        helper.assertTrue(data.get(0) == 1_234, "the menu reads the stored energy");
        data.set(0, 77);
        helper.assertTrue(machine.getEnergy().getEnergyStored() == 77,
                "a value the menu receives is written into the field, as on the client");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void deviceBlock_spillsTheInventoryWhenBroken(final GameTestHelper helper) {
        helper.setBlock(MACHINE, IndustrialModule.MACERATOR.get());
        final ProcessingMachineBlockEntity machine = machine(helper, ProcessingMachineBlockEntity.class);
        machine.getInventory().setStackInSlot(ProcessingMachineBlockEntity.INPUT_SLOT,
                new ItemStack(Items.COBBLESTONE, 5));

        helper.setBlock(MACHINE, Blocks.AIR);

        helper.assertItemEntityPresent(Items.COBBLESTONE, MACHINE, 2.0);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void coreMenu_shiftClickFollowsTheDeclaredRoutes(final GameTestHelper helper) {
        final CompressorBlockEntity machine = compressor(helper);
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.getInventory().setItem(0, new ItemStack(Items.IRON_INGOT, 5));
        final ProcessingMachineMenu menu = new ProcessingMachineMenu(IndustrialModule.COMPRESSOR_MENU.get(), 1,
                player.getInventory(), machine);
        // The input, the output, the 27 of the main grid, then the hotbar, whose first slot is index 29.
        final int firstHotbarSlot = 29;

        menu.quickMoveStack(player, firstHotbarSlot);
        helper.assertTrue(machine.getInventory().getStackInSlot(ProcessingMachineBlockEntity.INPUT_SLOT)
                .getCount() == 5, "a shift-click in the player's inventory sends the stack to the input");

        machine.getInventory().setStackInSlot(ProcessingMachineBlockEntity.OUTPUT_SLOT,
                new ItemStack(Items.STONE, 2));
        menu.quickMoveStack(player, ProcessingMachineBlockEntity.OUTPUT_SLOT);
        helper.assertTrue(machine.getInventory().getStackInSlot(ProcessingMachineBlockEntity.OUTPUT_SLOT).isEmpty()
                && player.getInventory().countItem(Items.STONE) == 2,
                "a shift-click on the output sends it to the player");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void coreMenu_staysOpenOnlyWhileItsMachineStands(final GameTestHelper helper) {
        final CompressorBlockEntity machine = compressor(helper);
        // A player left out of the world: one in it would be sent the packets of every other test.
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.moveTo(helper.absoluteVec(IN_FRONT));
        final ProcessingMachineMenu menu = new ProcessingMachineMenu(IndustrialModule.COMPRESSOR_MENU.get(), 1,
                player.getInventory(), machine);

        helper.assertTrue(menu.stillValid(player), "the menu stays open beside its machine");
        helper.setBlock(MACHINE, Blocks.AIR);
        helper.assertFalse(menu.stillValid(player), "the menu closes once its machine is gone");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void nullableValue_isLeftOutWhileItHoldsNothing(final GameTestHelper helper) {
        helper.setBlock(MACHINE, ComputingModule.SPEAKER.get());
        final SpeakerBlockEntity speaker = machine(helper, SpeakerBlockEntity.class);
        final HolderLookup.Provider registries = helper.getLevel().registryAccess();
        final long owner = helper.absolutePos(OWNER).asLong();

        helper.assertFalse(speaker.saveWithoutMetadata(registries).contains("LinkedOwner"),
                "a speaker linked to nothing saves no owner");
        speaker.onOwnerLinked(owner);
        final CompoundTag saved = speaker.saveWithoutMetadata(registries);
        helper.assertTrue(saved.getLong("LinkedOwner") == owner, "a linked speaker saves its owner");

        final BlockEntity loaded = BlockEntity.loadStatic(speaker.getBlockPos(), speaker.getBlockState(),
                speaker.saveWithFullMetadata(registries), registries);
        helper.assertTrue(loaded instanceof SpeakerBlockEntity copy && copy.ownerPos() != null
                && copy.ownerPos().asLong() == owner, "the owner loads back");
        speaker.loadWithComponents(new CompoundTag(), registries);
        helper.assertTrue(speaker.ownerPos() == null, "an owner missing from the save reads back as none");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void fields_offerTheExposedTankOnEverySideAndSaveIt(final GameTestHelper helper) {
        helper.setBlock(MACHINE, ComputingModule.TANK.get());
        final TankBlockEntity tank = machine(helper, TankBlockEntity.class);
        final BlockPos at = helper.absolutePos(MACHINE);
        for (final Direction side : Direction.values()) {
            helper.assertTrue(helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, at, side)
                    == tank.fluidHandler(), "the tank is offered on the " + side + " side");
        }
        tank.fluidHandler().fill(new FluidStack(Fluids.WATER, 3_000), IFluidHandler.FluidAction.EXECUTE);
        final HolderLookup.Provider registries = helper.getLevel().registryAccess();

        final BlockEntity loaded = BlockEntity.loadStatic(tank.getBlockPos(), tank.getBlockState(),
                tank.saveWithFullMetadata(registries), registries);
        helper.assertTrue(loaded instanceof TankBlockEntity copy && copy.fluid().getAmount() == 3_000
                && copy.fluid().is(Fluids.WATER), "the fluid is saved");
        helper.assertTrue(tank.getUpdateTag(registries).contains("Tank"), "the players are sent the fluid");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void partField_savesAndSendsWhatItWrites(final GameTestHelper helper) {
        helper.setBlock(MACHINE, ComputingModule.CRAFTING_COMPUTER.get());
        final CraftingComputerBlockEntity computer = machine(helper, CraftingComputerBlockEntity.class);
        computer.getHardware().setStackInSlot(CraftingComputerBlockEntity.PCIE_SLOTS_START,
                new ItemStack(ComputingModule.CRAFTING_CARD_T2.get()));
        final List<ItemStack> grid = new ArrayList<>(Collections.nCopies(CraftingPattern.GRID_SIZE, ItemStack.EMPTY));
        grid.set(0, new ItemStack(Items.OAK_LOG));
        helper.assertTrue(computer.loadPattern(new CraftingPattern(grid, new ItemStack(Items.OAK_PLANKS, 4))),
                "a pattern goes into the card");
        final HolderLookup.Provider registries = helper.getLevel().registryAccess();

        final BlockEntity loaded = BlockEntity.loadStatic(computer.getBlockPos(), computer.getBlockState(),
                computer.saveWithFullMetadata(registries), registries);
        helper.assertTrue(loaded instanceof CraftingComputerBlockEntity copy && copy.romUsed() == 1,
                "the hardware is saved by the part that writes it, the card's ROM with it");
        final CompoundTag update = computer.getUpdateTag(registries);
        helper.assertTrue(update.contains("Networked"), "the players are sent what the attachment part writes");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void peripheralLink_freesItsPlaceTheMomentItsBlockIsBroken(final GameTestHelper helper) {
        helper.setBlock(OWNER, ComputingModule.PERSONAL_COMPUTER.get());
        helper.setBlock(MACHINE, ComputingModule.SPEAKER.get());
        if (!(helper.getBlockEntity(OWNER) instanceof PersonalComputerBlockEntity computer)) {
            helper.fail("no computer at " + OWNER);
            return;
        }
        final long speakerAt = helper.absolutePos(MACHINE).asLong();
        computer.onEndpointLinked(speakerAt, PortKind.AUDIO);
        machine(helper, SpeakerBlockEntity.class).onOwnerLinked(helper.absolutePos(OWNER).asLong());

        helper.setBlock(MACHINE, Blocks.AIR);

        helper.assertFalse(computer.linkedEndpoints().contains(speakerAt),
                "the computer frees the speaker's place as the speaker is broken, not a tick later");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void processingMachine_pressesAnIngotIntoAPlate(final GameTestHelper helper) {
        final CompressorBlockEntity machine = compressor(helper);
        machine.getInventory().setStackInSlot(ProcessingMachineBlockEntity.INPUT_SLOT,
                new ItemStack(Items.IRON_INGOT));
        machine.getEnergy().setEnergyStored(machine.getEnergy().getMaxEnergyStored());

        helper.succeedWhen(() -> helper.assertTrue(machine.getInventory()
                .getStackInSlot(ProcessingMachineBlockEntity.OUTPUT_SLOT)
                .is(MaterialItems.get(ModMaterial.IRON, MaterialForm.PLATE).get()),
                "the ingot comes out a plate"));
    }

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void coalGenerator_burnsFuelIntoEnergy(final GameTestHelper helper) {
        helper.setBlock(MACHINE, IndustrialModule.COAL_GENERATOR.get());
        final CoalGeneratorBlockEntity generator = machine(helper, CoalGeneratorBlockEntity.class);
        generator.getInventory().setStackInSlot(CoalGeneratorBlockEntity.FUEL_SLOT, new ItemStack(Items.COAL));

        helper.succeedWhen(() -> helper.assertTrue(generator.isBurning()
                && generator.getEnergy().getEnergyStored() > 0, "the coal burns into energy"));
    }

    private static CompressorBlockEntity compressor(final GameTestHelper helper) {
        helper.setBlock(MACHINE, IndustrialModule.COMPRESSOR.get());
        return machine(helper, CompressorBlockEntity.class);
    }

    private static <E extends BlockEntity> E machine(final GameTestHelper helper, final Class<E> type) {
        final BlockEntity found = helper.getBlockEntity(MACHINE);
        if (!type.isInstance(found)) {
            helper.fail("no " + type.getSimpleName() + " at " + MACHINE);
        }
        return type.cast(found);
    }
}
