/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import dev.jstech.core.energy.EnergyPorts;
import dev.jstech.core.energy.EnergyProtection;
import dev.jstech.core.energy.EnergyStore;
import dev.jstech.core.energy.IEnergyPort;
import dev.jstech.core.energy.WrongClass;
import dev.jstech.core.jstechseries.Element;
import dev.jstech.core.jstechseries.Elements;
import dev.jstech.core.machine.ComparatorReads;
import dev.jstech.core.machine.IMachineParameters;
import dev.jstech.core.machine.MachineHealth;
import dev.jstech.core.machine.MachineHistory;
import dev.jstech.core.machine.MachineStatus;
import dev.jstech.core.machine.RedstoneMode;
import dev.jstech.core.material.MatterState;
import dev.jstech.core.matter.IMatterHandler;
import dev.jstech.core.matter.MatterCapabilities;
import dev.jstech.core.matter.MatterStack;
import dev.jstech.core.signal.Reading;
import dev.jstech.core.signal.Signals;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestEnergies;
import dev.jstech.tests.TestFuses;
import dev.jstech.tests.TestKitMachine;
import dev.jstech.tests.TestKitMachine.KitMachineBlockEntity;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.entity.ComparatorBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The Core's machine kit, on a machine running on energy of a class with a tank of gas: its port and its tanks offered
 * to lines; its state always told (waiting, without energy, working, output full); its switch; its answer to
 * redstone, one recipe a pulse included; a comparator reading what the player picks; an explosion that damages it
 * rather than breaks it, slowing it; a repair kit that mends it over a few seconds; a burnt port that stops it until it
 * is repaired; a fuse that blows and shuts it down unharmed; undervoltage; its rest until something changes; the values
 * it lets the outside read and set; and its owner, whoever placed it.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class MachineKitGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos MACHINE = new BlockPos(4, 2, 4);
    private static final int TICKS_A_SECOND = 20;

    private MachineKitGameTests() {
    }

    @GameTest(template = ARENA)
    public static void machine_offersItsPortAndItsTanksToLines(final GameTestHelper helper) {
        place(helper);
        final BlockPos at = helper.absolutePos(MACHINE);
        final IEnergyPort port = helper.getLevel().getCapability(EnergyPorts.PORT, at, Direction.NORTH);
        helper.assertTrue(port != null && port.portClass() == TestEnergies.LOW, "a port of the low class");
        final IMatterHandler gas = helper.getLevel().getCapability(MatterCapabilities.GAS, at, Direction.UP);
        helper.assertTrue(gas != null && gas.insert(new MatterStack(Elements.of(Element.HYDROGEN), 500L), false)
                == 500L, "its gas input takes hydrogen");
        helper.assertTrue(gas.extract(Elements.of(Element.HYDROGEN), 100L, true) == 0L,
                "and nothing is taken from an input");
        helper.assertTrue(helper.getLevel().getCapability(IMachineParameters.CAPABILITY, at, null) != null
                && helper.getLevel().getCapability(Signals.OUTPUT, at, null) != null,
                "its values and its readings are offered too");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 120)
    public static void machine_alwaysSaysWhatItIsDoing(final GameTestHelper helper) {
        final KitMachineBlockEntity machine = place(helper);
        helper.startSequence()
                .thenExecuteAfter(2, () -> helper.assertTrue(machine.status() == MachineStatus.WAITING,
                        "empty, it waits for input: " + machine.status()))
                .thenExecute(() -> machine.getInventory().setStackInSlot(0, new ItemStack(Items.COBBLESTONE, 4)))
                .thenExecuteAfter(2, () -> helper.assertTrue(machine.status() == MachineStatus.NO_ENERGY,
                        "with no energy, it says so: " + machine.status()))
                .thenExecute(() -> store(machine).setStored(TestKitMachine.CAPACITY))
                .thenExecuteAfter(2, () -> helper.assertTrue(machine.status() == MachineStatus.WORKING,
                        "with energy it works: " + machine.status()))
                .thenExecute(() -> {
                    machine.getInventory().setStackInSlot(TestKitMachine.OUT, new ItemStack(Items.DIRT, 64));
                    machine.getInventory().setStackInSlot(TestKitMachine.OUT + 1, new ItemStack(Items.DIRT, 64));
                })
                .thenExecuteAfter(2, () -> helper.assertTrue(machine.status() == MachineStatus.OUTPUT_FULL,
                        "with no room for its work, its output is full: " + machine.status()))
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void history_tellsWhatItDidAndWhatJammedIt(final GameTestHelper helper) {
        // Eight stones of work, a second each: it works, runs out and waits, and its first reading comes after.
        final KitMachineBlockEntity machine = loaded(helper);
        helper.runAfterDelay(MachineHistory.READING_SECONDS * TICKS_A_SECOND + 2 * TICKS_A_SECOND, () -> {
            final MachineHistory history = machine.history();
            helper.assertTrue(history.made().size() == 1 && history.mostMade() == 8,
                    "its first reading has what it made a minute: " + history.made());
            helper.assertTrue(history.averagePower() > 0L, "and what it drew: " + history.power());
            helper.assertTrue(history.efficiency() > 0 && history.efficiency() < 100,
                    "it worked part of the time: " + history.efficiency());
            helper.assertTrue(history.workedTicks() >= 8 * TestKitMachine.TICKS,
                    "as long as its eight stones took: " + history.workedTicks());
            helper.assertTrue(history.stop().orElse(null) == MachineStatus.WAITING,
                    "running out jammed it: " + history.stoppedBy());
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 120)
    public static void history_leavesOutWhatItsPlayerStopped(final GameTestHelper helper) {
        final KitMachineBlockEntity machine = loaded(helper);
        helper.runAfterDelay(TestKitMachine.TICKS / 2, () -> {
            helper.assertTrue(machine.running(), "it works before it is switched off");
            machine.control().setOn(false);
            helper.runAfterDelay(2 * TICKS_A_SECOND, () -> {
                helper.assertTrue(machine.history().stop().isEmpty(),
                        "switched off by hand is no jam: " + machine.history().stoppedBy());
                helper.succeed();
            });
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void switch_turnsItOffByHand(final GameTestHelper helper) {
        final KitMachineBlockEntity machine = loaded(helper);
        machine.control().setOn(false);
        helper.runAfterDelay(TestKitMachine.TICKS + 5, () -> {
            helper.assertTrue(machine.status() == MachineStatus.SWITCHED_OFF, "switched off: " + machine.status());
            helper.assertTrue(machine.getInventory().getStackInSlot(TestKitMachine.OUT).isEmpty(), "it made nothing");
            machine.control().setOn(true);
            helper.runAfterDelay(TestKitMachine.TICKS + 5, () -> {
                helper.assertTrue(machine.getInventory().getStackInSlot(TestKitMachine.OUT).is(Items.STONE),
                        "switched on, it works again");
                helper.succeed();
            });
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void redstone_runsItOnlyWithASignal(final GameTestHelper helper) {
        final KitMachineBlockEntity machine = loaded(helper);
        machine.control().setMode(RedstoneMode.HIGH);
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(machine.status() == MachineStatus.OFF_BY_REDSTONE, "no signal, no work: "
                    + machine.status());
            helper.setBlock(MACHINE.west(), Blocks.REDSTONE_BLOCK);
            helper.runAfterDelay(3, () -> {
                helper.assertTrue(machine.control().signal() == Reading.FULL, "it hears the signal");
                helper.assertTrue(machine.status() == MachineStatus.WORKING, "and works: " + machine.status());
                helper.succeed();
            });
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void pulse_runsOneRecipeForEachPulse(final GameTestHelper helper) {
        final KitMachineBlockEntity machine = loaded(helper);
        machine.control().setMode(RedstoneMode.PULSE);
        helper.startSequence()
                .thenExecuteAfter(2, () -> helper.setBlock(MACHINE.west(), Blocks.REDSTONE_BLOCK))
                .thenExecuteAfter(2, () -> helper.setBlock(MACHINE.west(), Blocks.AIR))
                .thenExecuteAfter(TestKitMachine.TICKS * 3, () -> {
                    final ItemStack made = machine.getInventory().getStackInSlot(TestKitMachine.OUT);
                    helper.assertTrue(made.is(Items.STONE) && made.getCount() == 1, "one pulse, one recipe: " + made);
                    helper.assertTrue(machine.status() == MachineStatus.OFF_BY_REDSTONE,
                            "and it waits for the next pulse: " + machine.status());
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 60)
    public static void comparator_readsWhatThePlayerPicks(final GameTestHelper helper) {
        final KitMachineBlockEntity machine = place(helper);
        final BlockPos comparatorAt = MACHINE.south();
        helper.setBlock(comparatorAt, Blocks.COMPARATOR.defaultBlockState().setValue(ComparatorBlock.FACING,
                Direction.NORTH));
        machine.control().setReads(ComparatorReads.INPUT);
        // One of its two inputs full: half full.
        machine.getInventory().setStackInSlot(0, new ItemStack(Items.COBBLESTONE, 64));
        helper.runAfterDelay(6, () -> {
            helper.assertTrue(machine.strength() == 8, "half full, it speaks 8 of 15: " + machine.strength());
            final ComparatorBlockEntity comparator = (ComparatorBlockEntity) helper.getLevel().getBlockEntity(
                    helper.absolutePos(comparatorAt));
            helper.assertTrue(comparator.getOutputSignal() == 8, "the comparator reads it: "
                    + comparator.getOutputSignal());
            machine.control().setReads(ComparatorReads.ENERGY);
            store(machine).setStored(TestKitMachine.CAPACITY);
            helper.runAfterDelay(6, () -> {
                helper.assertTrue(comparator.getOutputSignal() == Reading.FULL,
                        "set to its energy, it reads the full store: " + comparator.getOutputSignal());
                helper.succeed();
            });
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 60)
    public static void explosion_damagesItRatherThanBreakingIt(final GameTestHelper helper) {
        final KitMachineBlockEntity machine = loaded(helper);
        final Vec3 at = Vec3.atCenterOf(helper.absolutePos(MACHINE.east()));
        helper.getLevel().explode(null, at.x, at.y, at.z, 2.0F, Level.ExplosionInteraction.BLOCK);
        helper.runAfterDelay(3, () -> {
            helper.assertBlockPresent(TestKitMachine.BLOCK.get(), MACHINE);
            helper.assertTrue(machine.health().health() == MachineHealth.FULL - 50, "half its health is gone: "
                    + machine.health().health());
            helper.assertTrue(machine.status() == MachineStatus.DAMAGED, "it works, but says it is damaged");
            helper.assertTrue(machine.getMaxProgress() > TestKitMachine.TICKS, "and works slower: "
                    + machine.getMaxProgress());
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void repairKit_mendsItOverAFewSeconds(final GameTestHelper helper) {
        final KitMachineBlockEntity machine = place(helper);
        machine.damage(50);
        final FakePlayer player = FakePlayerFactory.getMinecraft(helper.getLevel());
        final Vec3 stand = Vec3.atCenterOf(helper.absolutePos(MACHINE.north(2)));
        player.moveTo(stand.x, stand.y - 1.0, stand.z);
        player.lookAt(EntityAnchorArgument.Anchor.EYES, Vec3.atCenterOf(helper.absolutePos(MACHINE)));
        final ItemStack kit = new ItemStack(TestKitMachine.REPAIR_KIT.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, kit);
        final BlockPos at = helper.absolutePos(MACHINE);
        kit.getItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(
                Vec3.atCenterOf(at), Direction.NORTH, at, false)));
        // A second of holding it gives back what the kit gives a second.
        for (int left = 72_000; left > 72_000 - 20; left--) {
            kit.getItem().onUseTick(helper.getLevel(), player, kit, left);
        }
        helper.assertTrue(machine.health().health() == 50 + TestKitMachine.KIT_PER_SECOND,
                "a second of the kit gives back its rate: " + machine.health().health());
        helper.assertTrue(kit.getDamageValue() == TestKitMachine.KIT_PER_SECOND, "and spends as much of it");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 60)
    public static void burnt_stopsItUntilItIsRepaired(final GameTestHelper helper) {
        final KitMachineBlockEntity machine = loaded(helper);
        store(machine).meetWrongClass(WrongClass.BURNS, TestEnergies.HIGH, helper.getLevel().getGameTime());
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(machine.power().burnt() && machine.status() == MachineStatus.DAMAGED,
                    "a line too high burns its port: " + machine.status());
            helper.assertTrue(machine.health().damaged(), "and damages it");
            machine.repair(MachineHealth.FULL);
            helper.runAfterDelay(3, () -> {
                helper.assertTrue(!machine.power().burnt() && machine.status() == MachineStatus.WORKING,
                        "repaired whole, its port mends and it works: " + machine.status());
                helper.succeed();
            });
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 60)
    public static void fuse_blowsAndShutsItDownUnharmed(final GameTestHelper helper) {
        final KitMachineBlockEntity machine = loaded(helper);
        final EnergyProtection protection = machine.power().protection();
        protection.slots().setStackInSlot(EnergyProtection.FUSE, new ItemStack(TestFuses.ANY_CLASS.get()));
        store(machine).meetWrongClass(WrongClass.BURNS, TestEnergies.MEDIUM, helper.getLevel().getGameTime());
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(machine.status() == MachineStatus.FUSE_BLOWN, "its fuse blew: " + machine.status());
            helper.assertTrue(!machine.health().damaged() && !machine.power().burnt(), "and it is unharmed");
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 60)
    public static void undervoltage_keepsItFromStarting(final GameTestHelper helper) {
        final KitMachineBlockEntity machine = loaded(helper);
        helper.onEachTick(() -> store(machine).meetWrongClass(WrongClass.UNDERVOLTAGE, TestEnergies.LOW,
                helper.getLevel().getGameTime()));
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(machine.status() == MachineStatus.UNDERVOLTAGE, "a line below its class: "
                    + machine.status());
            helper.assertTrue(machine.getProgress() == 0, "and it does not start");
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 60)
    public static void machine_restsUntilSomethingChanges(final GameTestHelper helper) {
        final KitMachineBlockEntity machine = place(helper);
        store(machine).setStored(TestKitMachine.CAPACITY);
        helper.runAfterDelay(4, () -> {
            helper.assertTrue(machine.asleep(), "with nothing to do it rests");
            machine.getInventory().setStackInSlot(0, new ItemStack(Items.COBBLESTONE));
            helper.assertTrue(!machine.asleep(), "something put in wakes it");
            helper.runAfterDelay(TestKitMachine.TICKS + 3, () -> {
                helper.assertTrue(machine.getInventory().getStackInSlot(TestKitMachine.OUT).is(Items.STONE),
                        "and it works");
                helper.succeed();
            });
        });
    }

    @GameTest(template = ARENA)
    public static void parameters_areReadAndSetFromOutside(final GameTestHelper helper) {
        final KitMachineBlockEntity machine = place(helper);
        final IMachineParameters values = helper.getLevel().getCapability(IMachineParameters.CAPABILITY,
                helper.absolutePos(MACHINE), null);
        helper.assertTrue(values.read("health").orElse(-1) == MachineHealth.FULL, "every machine's own are read");
        helper.assertTrue(values.write("on", 0) && !machine.control().on(), "its switch is set from outside");
        helper.assertTrue(values.write("dial", 50) && machine.dial() == 10,
                "its own value is set, held to its span: " + machine.dial());
        helper.assertTrue(!values.write("progress", 1) && !values.write("nothing", 1),
                "a value only read, or none, is not set");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void owner_isWhoeverPlacedIt(final GameTestHelper helper) {
        final KitMachineBlockEntity machine = place(helper);
        final FakePlayer player = FakePlayerFactory.getMinecraft(helper.getLevel());
        final BlockPos at = helper.absolutePos(MACHINE);
        TestKitMachine.BLOCK.get().setPlacedBy(helper.getLevel(), at, helper.getLevel().getBlockState(at), player,
                ItemStack.EMPTY);
        helper.assertTrue(machine.ownership() != null && machine.ownership().owner().equals(player.getUUID()),
                "whoever placed it owns it");
        helper.succeed();
    }

    /* The machine, empty, with no energy. */
    private static KitMachineBlockEntity place(final GameTestHelper helper) {
        helper.setBlock(MACHINE, TestKitMachine.BLOCK.get());
        return (KitMachineBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(MACHINE));
    }

    /* The machine with cobblestone to work and a full store. */
    private static KitMachineBlockEntity loaded(final GameTestHelper helper) {
        final KitMachineBlockEntity machine = place(helper);
        machine.getInventory().setStackInSlot(0, new ItemStack(Items.COBBLESTONE, 8));
        store(machine).setStored(TestKitMachine.CAPACITY);
        return machine;
    }

    private static EnergyStore store(final KitMachineBlockEntity machine) {
        return machine.power().portStore();
    }
}
