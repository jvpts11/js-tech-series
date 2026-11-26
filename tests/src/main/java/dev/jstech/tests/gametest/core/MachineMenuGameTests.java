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
import dev.jstech.core.inventory.FaceMode;
import dev.jstech.core.inventory.ItemFilter;
import dev.jstech.core.jstechseries.upgrade.SharedUpgrades;
import dev.jstech.core.machine.ComparatorReads;
import dev.jstech.core.machine.MachineFill;
import dev.jstech.core.machine.MachineMenu;
import dev.jstech.core.machine.MachineResource;
import dev.jstech.core.machine.MachineSides;
import dev.jstech.core.machine.ProcessingRecipe;
import dev.jstech.core.machine.RedstoneMode;
import dev.jstech.core.team.Access;
import dev.jstech.core.team.Ownership;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestKitMachine;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * The machine screen's menu on the server: each control sets its part of the machine for its owner (the switch, the
 * redstone mode, what the comparator reads, what a face does, the pushing and its thumbwheel, an input's lock, the
 * filter from the thing in hand), a stranger sets nothing, only the owner turns the key of who may use it, each tab
 * shows its own slots alone, and a recipe viewer's move fills the inputs with a recipe's ingredients and picks it.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class MachineMenuGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos AT = new BlockPos(2, 2, 2);
    /* A stack of motors, and how many motors of every kind fit in a machine together. */
    private static final int MOTORS = 16;
    private static final int MOST_MOTORS = 4;

    private MachineMenuGameTests() {
    }

    @GameTest(template = ARENA)
    public static void controls_setTheirPartOfTheMachine(final GameTestHelper helper) {
        final FakePlayer owner = player(helper, "machine_owner");
        final TestKitMachine.KitMachineBlockEntity machine = machine(helper, owner);
        final MachineMenu menu = new MachineMenu(0, owner.getInventory(), machine);
        menu.clickMenuButton(owner, MachineMenu.SWITCH);
        menu.clickMenuButton(owner, MachineMenu.FIRST_MODE + 1);
        menu.clickMenuButton(owner, MachineMenu.FIRST_READS + 3);
        menu.clickMenuButton(owner, MachineMenu.sideButton(MachineResource.ITEMS, RelativeFace.FRONT, FaceMode.OUT));
        menu.clickMenuButton(owner, MachineMenu.automationButton(MachineResource.ITEMS, MachineMenu.PUSH));
        menu.clickMenuButton(owner, MachineMenu.automationButton(MachineResource.ITEMS, MachineMenu.PUSH_UP + 1));
        menu.clickMenuButton(owner, MachineMenu.automationButton(MachineResource.ITEMS, MachineMenu.PUSH_DOWN + 2));
        helper.assertTrue(!machine.control().on(), "the switch turns it off");
        helper.assertTrue(machine.control().mode() == RedstoneMode.HIGH, "the second mode runs it with a signal");
        helper.assertTrue(machine.control().reads() == ComparatorReads.ENERGY, "the fourth reading is its energy");
        helper.assertTrue(machine.sides().mode(MachineResource.ITEMS, RelativeFace.FRONT) == FaceMode.OUT,
                "its front gives items out");
        final MachineSides.Automation auto = machine.sides().automation(MachineResource.ITEMS);
        helper.assertTrue(auto.push(), "it pushes items by itself");
        helper.assertTrue(auto.pushRate() == 73, "the tens' drum up and the ones' down make 64 into 73: "
                + auto.pushRate());
        menu.clickMenuButton(owner, MachineMenu.SWITCH);
        helper.assertTrue(machine.control().on(), "and the switch turns it on again");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void lock_locksAnInputToWhatItHoldsAndUnlocksIt(final GameTestHelper helper) {
        final FakePlayer owner = player(helper, "machine_locker");
        final TestKitMachine.KitMachineBlockEntity machine = machine(helper, owner);
        final MachineMenu menu = new MachineMenu(0, owner.getInventory(), machine);
        machine.getInventory().setStackInSlot(0, new ItemStack(Items.COBBLESTONE, 3));
        menu.clickMenuButton(owner, MachineMenu.FIRST_LOCK);
        helper.assertTrue(machine.locks().locked(0).is(Items.COBBLESTONE), "the input is locked to cobblestone");
        machine.getInventory().setStackInSlot(0, ItemStack.EMPTY);
        helper.assertTrue(!machine.locks().allows(0, new ItemStack(Items.SAND)), "and takes nothing else");
        menu.clickMenuButton(owner, MachineMenu.FIRST_LOCK);
        helper.assertTrue(machine.locks().locked(0).isEmpty(), "the same control unlocks it");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void stranger_setsNothingAndOnlyTheOwnerTurnsTheKey(final GameTestHelper helper) {
        final FakePlayer owner = player(helper, "machine_keeper");
        final FakePlayer stranger = player(helper, "machine_stranger");
        final TestKitMachine.KitMachineBlockEntity machine = machine(helper, owner);
        final MachineMenu theirs = new MachineMenu(0, stranger.getInventory(), machine);
        theirs.clickMenuButton(stranger, MachineMenu.SWITCH);
        theirs.clickMenuButton(stranger, MachineMenu.FIRST_ACCESS + 2);
        helper.assertTrue(machine.control().on(), "a stranger does not throw its switch");
        helper.assertTrue(machine.ownership().access() == Access.PRIVATE, "nor turn its key");
        new MachineMenu(0, owner.getInventory(), machine).clickMenuButton(owner, MachineMenu.FIRST_ACCESS + 2);
        helper.assertTrue(machine.ownership().access() == Access.PUBLIC, "its owner opens it to everyone");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void filter_letsOnlyTheThingInHandOrEverything(final GameTestHelper helper) {
        final FakePlayer owner = player(helper, "machine_filter");
        final TestKitMachine.KitMachineBlockEntity machine = machine(helper, owner);
        final MachineMenu menu = new MachineMenu(0, owner.getInventory(), machine);
        final int items = MachineMenu.FIRST_FILTER + MachineResource.round().indexOf(MachineResource.ITEMS);
        menu.setCarried(new ItemStack(Items.IRON_INGOT));
        menu.clickMenuButton(owner, items);
        final ItemFilter only = machine.sides().automation(MachineResource.ITEMS).filter();
        helper.assertTrue(only.mode() == ItemFilter.Mode.ONLY
                && only.rules().equals(List.of(new ItemFilter.Fuzzy("minecraft:iron_ingot", 0L))),
                "an iron ingot in hand lets only iron ingots pass: " + only);
        menu.setCarried(ItemStack.EMPTY);
        menu.clickMenuButton(owner, items);
        helper.assertTrue(machine.sides().automation(MachineResource.ITEMS).filter().equals(ItemFilter.EVERYTHING),
                "an empty hand lets everything pass");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void tabs_showTheirOwnSlotsAlone(final GameTestHelper helper) {
        final FakePlayer owner = player(helper, "machine_tabs");
        final TestKitMachine.KitMachineBlockEntity machine = machine(helper, owner);
        final MachineMenu menu = new MachineMenu(0, owner.getInventory(), machine);
        helper.assertTrue(menu.faceSlots().stream().allMatch(Slot::isActive), "the Main tab shows its slots");
        menu.clickMenuButton(owner, MachineMenu.FIRST_TAB + MachineMenu.UPGRADES);
        helper.assertTrue(menu.tab() == MachineMenu.UPGRADES, "the tab's control shows it");
        helper.assertTrue(menu.faceSlots().stream().noneMatch(Slot::isActive), "the Main tab's slots are gone");
        final long bays = menu.slots.stream().filter(slot -> slot.isActive() && !(slot.container instanceof Inventory))
                .count();
        helper.assertTrue(bays == TestKitMachine.LAYOUT.upgrades(), "and its two bays are there: " + bays);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void fill_movesARecipesIngredientsIntoTheInputs(final GameTestHelper helper) {
        final FakePlayer owner = player(helper, "machine_fill");
        final TestKitMachine.KitMachineBlockEntity machine = machine(helper, owner);
        owner.getInventory().clearContent();
        owner.getInventory().add(new ItemStack(Items.IRON_INGOT, 10));
        owner.getInventory().add(new ItemStack(Items.BLAZE_ROD, 2));
        machine.getInventory().setStackInSlot(1, new ItemStack(Items.DIRT, 2));
        final RecipeHolder<ProcessingRecipe> nuggets = recipe(helper, "test_refining/nuggets");
        final int sets = MachineFill.fill(owner, machine, nuggets, true);
        helper.assertTrue(sets == 10, "ten ingots make ten sets: " + sets);
        helper.assertTrue(machine.getInventory().getStackInSlot(0).is(Items.IRON_INGOT)
                && machine.getInventory().getStackInSlot(0).getCount() == 10, "the ingots fill the first input");
        helper.assertTrue(machine.getInventory().getStackInSlot(1).is(Items.BLAZE_ROD)
                && machine.getInventory().getStackInSlot(1).getCount() == 1, "the catalyst goes once, not used up");
        helper.assertTrue(owner.getInventory().countItem(Items.DIRT) == 2, "what the recipe does not take goes back");
        helper.assertTrue(machine.chosenRecipe().filter(nuggets.id()::equals).isPresent(), "and the recipe is picked");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void fill_refusesAStrangerAndARecipeOfAnotherKind(final GameTestHelper helper) {
        final FakePlayer owner = player(helper, "machine_fill_owner");
        final FakePlayer stranger = player(helper, "machine_fill_stranger");
        final TestKitMachine.KitMachineBlockEntity machine = machine(helper, owner);
        stranger.getInventory().add(new ItemStack(Items.IRON_INGOT, 4));
        stranger.getInventory().add(new ItemStack(Items.BLAZE_ROD));
        owner.getInventory().add(new ItemStack(Items.GRAVEL, 4));
        helper.assertTrue(MachineFill.fill(stranger, machine, recipe(helper, "test_refining/nuggets"), false) == 0,
                "a stranger fills nothing");
        helper.assertTrue(MachineFill.fill(owner, machine, recipe(helper, "test_choosing/flint"), false) == 0,
                "nor does a recipe of a kind the machine does not work");
        helper.assertTrue(machine.getInventory().getStackInSlot(0).isEmpty(), "its inputs stay empty");
        helper.succeed();
    }

    /**
     * A stack of motors dropped on a bay goes in only as far as the motors' shared limit: four motors in all, of
     * whichever kinds, the rest left in hand. A machine took sixteen and used four.
     */
    @GameTest(template = ARENA)
    public static void bays_takeNoMoreMotorsDroppedThanTheirLimit(final GameTestHelper helper) {
        final FakePlayer owner = player(helper, "motor_owner");
        final TestKitMachine.KitMachineBlockEntity machine = machine(helper, owner);
        owner.getInventory().clearContent();
        owner.getInventory().setItem(0, new ItemStack(SharedUpgrades.BRUSHLESS_MOTOR.get(), MOTORS));
        owner.getInventory().setItem(1, new ItemStack(SharedUpgrades.INDUCTION_MOTOR.get(), MOTORS));
        final MachineMenu menu = new MachineMenu(0, owner.getInventory(), machine);
        final int first = TestKitMachine.LAYOUT.firstUpgrade();
        // A stack carried onto each bay, as a player drops it there.
        for (int bay = 0; bay < TestKitMachine.LAYOUT.upgrades(); bay++) {
            menu.setCarried(owner.getInventory().removeItemNoUpdate(bay));
            menu.clicked(machineSlot(menu, machine, first + bay), 0, ClickType.PICKUP, owner);
            owner.getInventory().setItem(bay, menu.getCarried());
            menu.setCarried(ItemStack.EMPTY);
        }
        int motors = 0;
        for (int bay = 0; bay < TestKitMachine.LAYOUT.upgrades(); bay++) {
            motors += machine.getInventory().getStackInSlot(first + bay).getCount();
        }
        final String where = "bays " + machine.getInventory().getStackInSlot(first) + ", "
                + machine.getInventory().getStackInSlot(first + 1) + "; inventory " + owner.getInventory().getItem(0)
                + ", " + owner.getInventory().getItem(1);
        helper.assertTrue(motors == MOST_MOTORS, "four motors in the bays, of both kinds together: " + where);
        helper.assertTrue(owner.getInventory().getItem(0).getCount() + owner.getInventory().getItem(1).getCount()
                == 2 * MOTORS - MOST_MOTORS, "and the rest left in the inventory: " + where);
        helper.succeed();
    }

    /**
     * Emptying a machine by hand leaves its upgrades in their bays: a double click gathering motors on the machine's
     * own tab takes none from the bays of the tab not shown, and a bay is not of the outputs' inventory for moving
     * every alike stack at once.
     */
    @GameTest(template = ARENA)
    public static void handDrain_leavesTheUpgradesInTheirBays(final GameTestHelper helper) {
        final FakePlayer owner = player(helper, "drain_owner");
        final TestKitMachine.KitMachineBlockEntity machine = machine(helper, owner);
        final int bay = TestKitMachine.LAYOUT.firstUpgrade();
        machine.getInventory().setStackInSlot(bay, new ItemStack(SharedUpgrades.BRUSHLESS_MOTOR.get(), MOST_MOTORS));
        machine.getInventory().setStackInSlot(TestKitMachine.OUT, new ItemStack(SharedUpgrades.BRUSHLESS_MOTOR.get(),
                2));
        final MachineMenu menu = new MachineMenu(0, owner.getInventory(), machine);
        final Slot baySlot = menu.slots.get(machineSlot(menu, machine, bay));
        final Slot output = menu.slots.get(machineSlot(menu, machine, TestKitMachine.OUT));
        final Slot input = menu.slots.get(machineSlot(menu, machine, 0));
        helper.assertTrue(!baySlot.isSameInventory(output) && output.isSameInventory(input),
                "a bay is not of the outputs' inventory for moving alike stacks; an input is");
        menu.setCarried(new ItemStack(SharedUpgrades.BRUSHLESS_MOTOR.get()));
        menu.clicked(machineSlot(menu, machine, 0), 0, ClickType.PICKUP_ALL, owner);
        helper.assertTrue(machine.getInventory().getStackInSlot(bay).getCount() == MOST_MOTORS,
                "a double click gathering motors leaves the bay's: " + machine.getInventory().getStackInSlot(bay));
        helper.assertTrue(menu.getCarried().getCount() == 3, "and gathers the output's: " + menu.getCarried());
        helper.succeed();
    }

    /* The menu's slot of the machine's inventory slot {@code index}. */
    private static int machineSlot(final MachineMenu menu, final TestKitMachine.KitMachineBlockEntity machine,
                                   final int index) {
        for (int k = 0; k < menu.slots.size(); k++) {
            if (menu.slots.get(k) instanceof SlotItemHandler held && held.getItemHandler() == machine.getInventory()
                    && held.getSlotIndex() == index) {
                return k;
            }
        }
        throw new IllegalStateException("no slot of the machine's slot " + index);
    }

    @SuppressWarnings("unchecked")
    private static RecipeHolder<ProcessingRecipe> recipe(final GameTestHelper helper, final String path) {
        return (RecipeHolder<ProcessingRecipe>) helper.getLevel().getRecipeManager().byKey(
                ResourceLocation.fromNamespaceAndPath(JsTests.MODID, path)).orElseThrow();
    }

    private static TestKitMachine.KitMachineBlockEntity machine(final GameTestHelper helper, final FakePlayer owner) {
        helper.setBlock(AT, TestKitMachine.BLOCK.get());
        final TestKitMachine.KitMachineBlockEntity machine = helper.getBlockEntity(AT);
        machine.own(Ownership.of(owner.getUUID()));
        return machine;
    }

    private static FakePlayer player(final GameTestHelper helper, final String name) {
        return FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.nameUUIDFromBytes(name.getBytes(
                StandardCharsets.UTF_8)), name));
    }
}
