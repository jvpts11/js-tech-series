/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.ComputingModule;
import dev.jstech.tests.testkit.ServerStacks;
import dev.jstech.computers.HardwareItems;
import dev.jstech.computers.block.ServerRackBlock;
import dev.jstech.computers.blockentity.ClusterManagementComputerBlockEntity;
import dev.jstech.computers.blockentity.PartRefusals;
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.computers.item.GpuItem;
import dev.jstech.computers.item.ServerHardwareHandler;
import dev.jstech.computers.item.ServerItem;
import dev.jstech.computers.rack.RackChassis;
import dev.jstech.computers.rack.RackLayout;
import dev.jstech.computers.rack.RackThermals;
import dev.jstech.core.tier.HardwareEra;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.EraParts;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Servers and cabinets have eras. A case takes only boards of its own era; a cabinet seats servers of
 * its own era or earlier. The cabinet is drawn as one model from what each row holds, so the block
 * entity must report every row, the bay power and the service panel the way the renderer reads them.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class RackEraGameTests {

    private static final String ARENA = "empty";
    private static final int SETTLE = 4;

    private RackEraGameTests() {
    }

    private static ServerRackBlockEntity placeRack(final GameTestHelper helper, final BlockPos pos, final Block block) {
        helper.setBlock(pos, block);
        if (!(helper.getBlockEntity(pos) instanceof ServerRackBlockEntity rack)) {
            throw new IllegalStateException("no rack at " + pos);
        }
        return rack;
    }

    private static ItemStack stack(final Item item) {
        return new ItemStack(item);
    }

    private static boolean fits(final ServerHardwareHandler handler, final int slot, final Item part) {
        return handler.insertItem(slot, stack(part), false).isEmpty();
    }

    /* Mounts each server two rows apart and checks how many of its rows' front slots it cables as drive bays. */
    private static void assertBays(final GameTestHelper helper, final ServerRackBlockEntity rack,
                                   final List<Item> servers, final List<Integer> bays) {
        final ItemStackHandler rows = rack.getServers();
        for (int i = 0; i < servers.size(); i++) {
            rows.setStackInSlot(2 * i, stack(servers.get(i)));
        }
        for (int i = 0; i < servers.size(); i++) {
            int drives = 0;
            for (int slot = 2 * i * RackLayout.SLOTS_PER_U; slot < (2 * i + 2) * RackLayout.SLOTS_PER_U; slot++) {
                if (rack.roleOfFrontSlot(slot) == RackLayout.SlotRole.DRIVE) {
                    drives++;
                }
            }
            helper.assertTrue(drives == bays.get(i), BuiltInRegistries.ITEM.getKey(servers.get(i)) + " cables "
                    + bays.get(i) + " drive bays; got " + drives);
        }
    }

    /**
     * A server's slots take only what its board takes, as a computer's do: the board's socket and era for a processor,
     * its memory, and its bus for a card. A part that cannot go in stays out of its slot.
     */
    @GameTest(template = ARENA)
    public static void serverSlots_takeOnlyWhatTheirBoardTakes(final GameTestHelper helper) {
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, ServerStacks.cpulessServer());
        final ServerHardwareHandler slots = new ServerHardwareHandler(player, InteractionHand.MAIN_HAND);
        helper.assertFalse(slots.isItemValid(ServerHardwareHandler.CPU_START,
                stack(HardwareItems.CPU_VELOCION_EPIC_7251.get())), "a processor of another socket and era stays out");
        helper.assertTrue(slots.isItemValid(ServerHardwareHandler.CPU_START,
                stack(ComputingModule.CPU_SERVO_2620.get())), "the board's own processor goes in");
        helper.assertFalse(slots.isItemValid(ServerHardwareHandler.RAM_START + 1,
                stack(HardwareItems.RAM_DDR4_16384.get())), "memory of another generation stays out");
        helper.assertTrue(slots.isItemValid(ServerHardwareHandler.GPU_START,
                stack(ComputingModule.VAULTIS_ARRAY_PCIE3.get())), "a card of the board's bus goes in");
        helper.succeed();
    }

    /** A server with no board yet is drawn in its chassis's era on its rack's screens, not in the Standard one. */
    @GameTest(template = ARENA)
    public static void rackEra_aServerWithNoBoardWearsItsChassissEra(final GameTestHelper helper) {
        final ServerRackBlockEntity rack = placeRack(helper, new BlockPos(2, 2, 2),
                ComputingModule.ADVANCED_SERVER_RACK.get());
        rack.getServers().setStackInSlot(0, stack(ComputingModule.ADVANCED_SERVER.get()));
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> helper.assertTrue(rack.displayEra() == HardwareEra.ADVANCED,
                        "an empty Advanced server reads as Advanced: " + rack.displayEra()))
                .thenSucceed();
    }

    /**
     * A server's slots and a rack's rows and bays name the rule that keeps a part out, so their screens tell the
     * player which: a board's socket, the cabinet a chassis belongs in, a disk of a later era, what a bay takes.
     */
    @GameTest(template = ARENA)
    public static void refusal_namesTheRuleOfServerSlotsRowsAndBays(final GameTestHelper helper) {
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, ServerStacks.cpulessServer());
        final ServerHardwareHandler slots = new ServerHardwareHandler(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(slots.refusal(ServerHardwareHandler.CPU_START,
                stack(HardwareItems.CPU_VELOCION_EPIC_7251.get())) == PartRefusals.CPU, "the server names the socket");
        helper.assertTrue(slots.refusal(ServerHardwareHandler.GPU_START,
                stack(ComputingModule.CRAFTING_CARD_T2.get())) == PartRefusals.SERVER_CARD,
                "and what its expansion slots take");
        final ServerRackBlockEntity rack = placeRack(helper, new BlockPos(2, 2, 2),
                ComputingModule.LEGACY_SERVER_RACK.get());
        rack.getServers().setStackInSlot(0, stack(ComputingModule.LEGACY_SERVER.get()));
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    helper.assertTrue(rack.rowRefusal(4, stack(ComputingModule.SERVER.get()))
                            == ServerRackBlock.WRONG_CHASSIS, "a Legacy rack says a Standard server belongs elsewhere");
                    int drive = -1;
                    for (int i = 0; i < rack.getFrontSlots().getSlots() && drive < 0; i++) {
                        if (rack.roleOfFrontSlot(i) == RackLayout.SlotRole.DRIVE) {
                            drive = i;
                        }
                    }
                    helper.assertTrue(drive >= 0, "the Legacy server cables a drive bay");
                    helper.assertTrue(rack.bayRefusal(drive, stack(HardwareItems.DISK_LINK_SATA_SSD_64G.get()))
                            == PartRefusals.DISK_TOO_NEW, "the bay names a disk of a later era");
                    helper.assertTrue(rack.bayRefusal(drive, stack(HardwareItems.CPU_VELOCION_EPIC_7251.get()))
                            == PartRefusals.DRIVE_BAY, "and says a drive bay takes a disk");
                })
                .thenSucceed();
    }

    /** A rack's drive bays take a disk of the rack's era or an older one, as a computer's disk slots do. */
    @GameTest(template = ARENA)
    public static void driveBays_takeOnlyDisksOfTheirRacksEraOrOlder(final GameTestHelper helper) {
        final ServerRackBlockEntity rack = placeRack(helper, new BlockPos(2, 2, 2),
                ComputingModule.LEGACY_SERVER_RACK.get());
        rack.getServers().setStackInSlot(0, stack(ComputingModule.LEGACY_SERVER.get()));
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    helper.assertFalse(rack.insertDrive(0, stack(HardwareItems.DISK_LINK_SATA_SSD_64G.get())),
                            "a Legacy rack refuses a Transition disk");
                    helper.assertTrue(rack.insertDrive(0, stack(HardwareItems.DISK_LINK_IDE_4G.get())),
                            "a Legacy rack takes a Legacy disk");
                    helper.assertTrue(rack.insertDrive(0, stack(HardwareItems.DISK_TRENCH_20M.get())),
                            "a Legacy rack takes a Vintage disk");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void rackEra_vintageRackSeatsOnlyVintageServers(final GameTestHelper helper) {
        final ServerRackBlockEntity rack = placeRack(helper, new BlockPos(2, 2, 2), ComputingModule.VINTAGE_SERVER_RACK.get());
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final ItemStackHandler rows = rack.getServers();
                    helper.assertTrue(rows.isItemValid(0, stack(ComputingModule.VINTAGE_SERVER.get())),
                            "a Vintage rack seats a Vintage server");
                    helper.assertFalse(rows.isItemValid(1, stack(ComputingModule.LEGACY_SERVER.get())),
                            "a Vintage rack refuses a Legacy server");
                    helper.assertFalse(rows.isItemValid(2, stack(ComputingModule.SERVER.get())),
                            "a Vintage rack refuses a Standard server");
                    helper.assertTrue(rows.isItemValid(3, stack(ComputingModule.VINTAGE_KVM_SWITCH.get())),
                            "a Vintage rack seats the Vintage KVM Switch");
                    helper.assertFalse(rows.isItemValid(4, stack(ComputingModule.KVM_SWITCH.get())),
                            "but not the Standard one");
                })
                .thenSucceed();
    }

    /** Rack equipment follows the servers' rule: a rack seats its own era's units and the earlier eras'. */
    @GameTest(template = ARENA)
    public static void rackUnits_seatInARackOfTheirEraOrNewer(final GameTestHelper helper) {
        final ServerRackBlockEntity legacy = placeRack(helper, new BlockPos(1, 2, 2),
                ComputingModule.LEGACY_SERVER_RACK.get());
        final ServerRackBlockEntity advanced = placeRack(helper, new BlockPos(5, 2, 2),
                ComputingModule.ADVANCED_SERVER_RACK.get());
        final ItemStack transitionKvm = stack(ComputingModule.TRANSITION_KVM_SWITCH.get());
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final ItemStackHandler rows = legacy.getServers();
                    helper.assertTrue(rows.isItemValid(0, stack(ComputingModule.VINTAGE_RACK_UPS.get())),
                            "a Legacy rack seats a Vintage UPS");
                    helper.assertTrue(rows.isItemValid(1, stack(ComputingModule.LEGACY_COOLING_UNIT.get())),
                            "and its own era's Cooling Unit");
                    helper.assertFalse(rows.isItemValid(2, transitionKvm), "but not a Transition KVM Switch");
                    helper.assertTrue(legacy.rowRefusal(2, transitionKvm) == ServerRackBlock.WRONG_UNIT,
                            "and says the unit belongs in a newer rack");
                    for (final Item unit : List.of(ComputingModule.VINTAGE_KVM_SWITCH.get(),
                            ComputingModule.LEGACY_RACK_UPS.get(), ComputingModule.TRANSITION_COOLING_UNIT.get(),
                            ComputingModule.KVM_SWITCH.get(), ComputingModule.ADVANCED_RACK_UPS.get())) {
                        helper.assertTrue(advanced.getServers().isItemValid(0, stack(unit)),
                                "an Advanced rack seats " + BuiltInRegistries.ITEM.getKey(unit));
                    }
                })
                .thenSucceed();
    }

    /**
     * Each era's storage and compute servers seat in a rack of their era or a newer one, wear their own era's face in
     * the cabinet and count as computers there.
     */
    @GameTest(template = ARENA)
    public static void eraServers_seatInRacksOfTheirEraAndWearTheirFaces(final GameTestHelper helper) {
        final ServerRackBlockEntity vintage = placeRack(helper, new BlockPos(1, 2, 2),
                ComputingModule.VINTAGE_SERVER_RACK.get());
        final ServerRackBlockEntity advanced = placeRack(helper, new BlockPos(5, 2, 2),
                ComputingModule.ADVANCED_SERVER_RACK.get());
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final ItemStackHandler rows = vintage.getServers();
                    helper.assertTrue(rows.insertItem(0, stack(ComputingModule.VINTAGE_STORAGE_SERVER.get()), false)
                            .isEmpty(), "a Vintage rack seats the Vintage Storage Server");
                    helper.assertTrue(rows.insertItem(2, stack(ComputingModule.VINTAGE_COMPUTE_SERVER.get()), false)
                            .isEmpty(), "and the Vintage Compute Server");
                    helper.assertFalse(rows.isItemValid(4, stack(ComputingModule.LEGACY_STORAGE_SERVER.get())),
                            "but not a Legacy one");
                    helper.assertValueEqual(ServerRackBlockEntity.UNIT_BONES[vintage.unitCodeAt(0)],
                            "storage_2u_vintage", "the Vintage storage server's face");
                    helper.assertValueEqual(ServerRackBlockEntity.UNIT_BONES[vintage.unitCodeAt(2)],
                            "compute_2u_vintage", "the Vintage compute server's face");
                    helper.assertTrue(vintage.computerSlots().size() == 2 && vintage.anyComputerSeated(),
                            "both are computers");
                    final ItemStackHandler newer = advanced.getServers();
                    newer.setStackInSlot(0, stack(ComputingModule.LEGACY_COMPUTE_SERVER.get()));
                    newer.setStackInSlot(2, stack(ComputingModule.TRANSITION_STORAGE_SERVER.get()));
                    newer.setStackInSlot(4, stack(ComputingModule.ADVANCED_STORAGE_SERVER.get()));
                    newer.setStackInSlot(6, stack(ComputingModule.ADVANCED_COMPUTE_SERVER.get()));
                    helper.assertValueEqual(ServerRackBlockEntity.UNIT_BONES[advanced.unitCodeAt(0)],
                            "compute_2u_legacy", "an Advanced rack draws an older server in that server's own shape");
                    helper.assertValueEqual(ServerRackBlockEntity.UNIT_BONES[advanced.unitCodeAt(2)],
                            "storage_2u_transition", "the Transition storage server's face");
                    helper.assertValueEqual(ServerRackBlockEntity.UNIT_BONES[advanced.unitCodeAt(4)],
                            "storage_2u_advanced", "the Advanced storage server's face");
                    helper.assertValueEqual(ServerRackBlockEntity.UNIT_BONES[advanced.unitCodeAt(6)],
                            "compute_2u_advanced", "the Advanced compute server's face");
                })
                .thenSucceed();
    }

    /** Each era's storage server cables more drive bays than the last, and each compute server its own few. */
    @GameTest(template = ARENA)
    public static void eraServers_cableTheirErasDriveBays(final GameTestHelper helper) {
        final ServerRackBlockEntity rack = placeRack(helper, new BlockPos(2, 2, 2),
                ComputingModule.ADVANCED_SERVER_RACK.get());
        final List<Item> storage = List.of(ComputingModule.VINTAGE_STORAGE_SERVER.get(),
                ComputingModule.LEGACY_STORAGE_SERVER.get(), ComputingModule.TRANSITION_STORAGE_SERVER.get(),
                ComputingModule.ADVANCED_STORAGE_SERVER.get());
        final List<Item> compute = List.of(ComputingModule.VINTAGE_COMPUTE_SERVER.get(),
                ComputingModule.LEGACY_COMPUTE_SERVER.get(), ComputingModule.TRANSITION_COMPUTE_SERVER.get(),
                ComputingModule.ADVANCED_COMPUTE_SERVER.get());
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    assertBays(helper, rack, storage, List.of(5, 7, 8, 10));
                    assertBays(helper, rack, compute, List.of(1, 2, 2, 3));
                })
                .thenSucceed();
    }

    /**
     * The Vintage and Legacy server boards carry the older bus beside their own, as the boards of their time did: the
     * era's storage cards (ISA on the Vintage, PCI on the Legacy) seat in them beside a card of the board's own bus,
     * and the Legacy servers keep a second slot for the graphics card their board lacks.
     */
    @GameTest(template = ARENA)
    public static void serverBoardsOfTwoBuses_seatTheirErasStorageCards(final GameTestHelper helper) {
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack(ComputingModule.VINTAGE_STORAGE_SERVER.get()));
        final ServerHardwareHandler vintage = new ServerHardwareHandler(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(fits(vintage, ServerHardwareHandler.MOBO, HardwareItems.MOTHERBOARD_EEB_VINTAGE.get()),
                "the Vintage storage server takes the era's server board");
        helper.assertTrue(fits(vintage, ServerHardwareHandler.GPU_START, ComputingModule.VAULTIS_ARRAY_ISA.get()),
                "and its ISA RAID controller in a slot of the older bus");
        player.setItemInHand(InteractionHand.MAIN_HAND, stack(ComputingModule.LEGACY_STORAGE_SERVER.get()));
        final ServerHardwareHandler legacy = new ServerHardwareHandler(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(fits(legacy, ServerHardwareHandler.MOBO, HardwareItems.MOTHERBOARD_EATX_LEGACY_604.get()),
                "the Legacy storage server takes the era's server board");
        helper.assertTrue(fits(legacy, ServerHardwareHandler.GPU_START, ComputingModule.VAULTIS_ARRAY_PCI.get()),
                "its PCI RAID controller in a slot of the older bus");
        helper.assertTrue(fits(legacy, ServerHardwareHandler.GPU_START + 1,
                        EraParts.cardFor(HardwareItems.MOTHERBOARD_EATX_LEGACY_604.get(), GpuItem.class)),
                "and a graphics card of the board's own bus in its second slot, for its monitor");
        helper.assertTrue(RackChassis.LEGACY_STORAGE_SERVER.maxPcie() == 2,
                "the Legacy storage server has the two slots, its board having no video of its own");
        player.setItemInHand(InteractionHand.MAIN_HAND, stack(ComputingModule.LEGACY_SERVER.get()));
        final ServerHardwareHandler legacyServer = new ServerHardwareHandler(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(fits(legacyServer, ServerHardwareHandler.MOBO,
                        HardwareItems.MOTHERBOARD_EATX_LEGACY_604.get()),
                "the Legacy 1U server takes the era's server board");
        helper.assertTrue(fits(legacyServer, ServerHardwareHandler.GPU_START, ComputingModule.VAULTIS_CACHE_PCI.get()),
                "its PCI read cache in a slot of the older bus");
        helper.assertTrue(fits(legacyServer, ServerHardwareHandler.GPU_START + 1,
                        EraParts.cardFor(HardwareItems.MOTHERBOARD_EATX_LEGACY_604.get(), GpuItem.class)),
                "and a graphics card beside it, the 1U server having two slots for the same reason");
        helper.succeed();
    }

    /** A Legacy compute server's case takes a Legacy server board and refuses a Standard one. */
    @GameTest(template = ARENA)
    public static void eraServers_takeTheirErasBoards(final GameTestHelper helper) {
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack(ComputingModule.LEGACY_COMPUTE_SERVER.get()));
        final ServerHardwareHandler slots = new ServerHardwareHandler(player, InteractionHand.MAIN_HAND);
        helper.assertFalse(fits(slots, ServerHardwareHandler.MOBO, ComputingModule.MOTHERBOARD_EEB_S_2011.get()),
                "the Legacy compute server refuses a Standard board");
        helper.assertTrue(fits(slots, ServerHardwareHandler.MOBO, HardwareItems.MOTHERBOARD_EATX_LEGACY_604.get()),
                "and takes the era's server board");
        helper.succeed();
    }

    /** Each Cooling Unit adds its own era's watts, and each era's unit wears its own face in the cabinet. */
    @GameTest(template = ARENA)
    public static void coolingUnits_addTheirErasWattsToTheBudget(final GameTestHelper helper) {
        final ServerRackBlockEntity rack = placeRack(helper, new BlockPos(2, 2, 2),
                ComputingModule.ADVANCED_SERVER_RACK.get());
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final ItemStackHandler rows = rack.getServers();
                    rows.setStackInSlot(0, stack(ComputingModule.VINTAGE_COOLING_UNIT.get()));
                    rows.setStackInSlot(1, stack(ComputingModule.ADVANCED_COOLING_UNIT.get()));
                    rows.setStackInSlot(2, stack(ComputingModule.LEGACY_KVM_SWITCH.get()));
                    helper.assertTrue(rack.thermalBudgetWatts() == RackThermals.PASSIVE_BUDGET_W + 300 + 2_500,
                            "a Vintage and an Advanced Cooling Unit add 300 W and 2,500 W; the budget is "
                                    + rack.thermalBudgetWatts() + " W");
                    helper.assertValueEqual(ServerRackBlockEntity.UNIT_BONES[rack.unitCodeAt(0)],
                            "cooling_unit_vintage", "the Vintage unit's face");
                    helper.assertValueEqual(ServerRackBlockEntity.UNIT_BONES[rack.unitCodeAt(1)],
                            "cooling_unit_advanced", "the Advanced unit's face");
                    helper.assertValueEqual(ServerRackBlockEntity.UNIT_BONES[rack.unitCodeAt(2)],
                            "kvm_switch_legacy", "the Legacy KVM Switch's face");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void rackEra_legacyRackSeatsItsEraAndEarlier(final GameTestHelper helper) {
        final ServerRackBlockEntity rack = placeRack(helper, new BlockPos(2, 2, 2), ComputingModule.LEGACY_SERVER_RACK.get());
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final ItemStackHandler rows = rack.getServers();
                    helper.assertTrue(rows.isItemValid(0, stack(ComputingModule.VINTAGE_SERVER.get())),
                            "a Legacy rack seats a Vintage server");
                    helper.assertTrue(rows.isItemValid(1, stack(ComputingModule.LEGACY_SERVER.get())),
                            "a Legacy rack seats a Legacy server");
                    helper.assertFalse(rows.isItemValid(2, stack(ComputingModule.SERVER.get())),
                            "a Legacy rack refuses a Standard server");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void rackEra_standardRackSeatsEveryEra(final GameTestHelper helper) {
        final ServerRackBlockEntity rack = placeRack(helper, new BlockPos(2, 2, 2), ComputingModule.SERVER_RACK.get());
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final ItemStackHandler rows = rack.getServers();
                    helper.assertTrue(rows.isItemValid(0, stack(ComputingModule.VINTAGE_SERVER.get()))
                                    && rows.isItemValid(1, stack(ComputingModule.LEGACY_SERVER.get()))
                                    && rows.isItemValid(2, stack(ComputingModule.SERVER.get())),
                            "a Standard rack seats every era of server");
                    helper.assertFalse(rows.isItemValid(3, stack(ComputingModule.SUPERCOMPUTER_NODE.get())),
                            "the era rule never overrides the cabinet type: a node still needs its own rack");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void rackEra_transitionRackSeatsItsEraAndEarlier(final GameTestHelper helper) {
        final ServerRackBlockEntity rack = placeRack(helper, new BlockPos(2, 2, 2),
                ComputingModule.TRANSITION_SERVER_RACK.get());
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final ItemStackHandler rows = rack.getServers();
                    helper.assertTrue(rows.isItemValid(0, stack(ComputingModule.VINTAGE_SERVER.get()))
                                    && rows.isItemValid(1, stack(ComputingModule.LEGACY_SERVER.get()))
                                    && rows.isItemValid(2, stack(ComputingModule.TRANSITION_SERVER.get())),
                            "a Transition rack seats the Vintage, Legacy and Transition servers");
                    helper.assertFalse(rows.isItemValid(3, stack(ComputingModule.SERVER.get())),
                            "a Transition rack refuses a Standard server");
                    helper.assertFalse(rows.isItemValid(4, stack(ComputingModule.ADVANCED_SERVER.get())),
                            "a Transition rack refuses an Advanced server");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void rackEra_advancedRackSeatsEveryEra(final GameTestHelper helper) {
        final ServerRackBlockEntity rack = placeRack(helper, new BlockPos(2, 2, 2),
                ComputingModule.ADVANCED_SERVER_RACK.get());
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final ItemStackHandler rows = rack.getServers();
                    helper.assertTrue(rows.isItemValid(0, stack(ComputingModule.VINTAGE_SERVER.get()))
                                    && rows.isItemValid(1, stack(ComputingModule.LEGACY_SERVER.get()))
                                    && rows.isItemValid(2, stack(ComputingModule.TRANSITION_SERVER.get()))
                                    && rows.isItemValid(3, stack(ComputingModule.SERVER.get()))
                                    && rows.isItemValid(4, stack(ComputingModule.ADVANCED_SERVER.get())),
                            "an Advanced rack seats every era of server");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void serverCase_transitionAndAdvancedTakeOnlyBoardsOfTheirEra(final GameTestHelper helper) {
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        final InteractionHand hand = InteractionHand.MAIN_HAND;
        final ItemStack transitionBoard = stack(HardwareItems.MOTHERBOARD_EEB_T_1366.get());
        final ItemStack advancedBoard = stack(HardwareItems.MOTHERBOARD_EEB_A_SP3.get());
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    player.setItemInHand(hand, stack(ComputingModule.TRANSITION_SERVER.get()));
                    final ServerHardwareHandler transition = new ServerHardwareHandler(player, hand);
                    helper.assertTrue(transition.insertItem(ServerHardwareHandler.MOBO, transitionBoard.copy(), true)
                            .isEmpty(), "a Transition case takes a Transition server board");
                    helper.assertFalse(transition.insertItem(ServerHardwareHandler.MOBO, advancedBoard.copy(), true)
                            .isEmpty(), "a Transition case refuses an Advanced board");

                    player.setItemInHand(hand, stack(ComputingModule.ADVANCED_SERVER.get()));
                    final ServerHardwareHandler advanced = new ServerHardwareHandler(player, hand);
                    helper.assertTrue(advanced.insertItem(ServerHardwareHandler.MOBO, advancedBoard.copy(), true)
                            .isEmpty(), "an Advanced case takes an Advanced server board");
                    helper.assertFalse(advanced.insertItem(ServerHardwareHandler.MOBO, transitionBoard.copy(), true)
                            .isEmpty(), "an Advanced case refuses a Transition board");
                })
                .thenSucceed();
    }

    /** The new eras' 1U servers report their own rows, and count as the computers a rack lights its bar for. */
    @GameTest(template = ARENA)
    public static void rackModel_reportsTheTransitionAndAdvancedServers(final GameTestHelper helper) {
        final ServerRackBlockEntity rack = placeRack(helper, new BlockPos(2, 2, 2),
                ComputingModule.ADVANCED_SERVER_RACK.get());
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final ItemStackHandler rows = rack.getServers();
                    rows.setStackInSlot(0, stack(ComputingModule.TRANSITION_SERVER.get()));
                    rows.setStackInSlot(1, stack(ComputingModule.ADVANCED_SERVER.get()));
                    helper.assertTrue(rack.unitCodeAt(0) == ServerRackBlockEntity.UNIT_SERVER_TRANSITION
                                    && rack.unitCodeAt(1) == ServerRackBlockEntity.UNIT_SERVER_ADVANCED,
                            "each row reports the new server it holds");
                    helper.assertTrue(rack.anyComputerSeated(), "the new servers are computers to the rack");
                    final int advanced = ServerRackBlockEntity.UNIT_SERVER_ADVANCED;
                    helper.assertValueEqual(ServerRackBlockEntity.UNIT_BONES[advanced], "server_1u_advanced",
                            "the bone the Advanced server shows in its row");
                })
                .thenSucceed();
    }

    /** The Advanced Supercomputer Rack seats its own node and the Standard one, each shown as itself, and no server. */
    @GameTest(template = ARENA)
    public static void advancedSupercomputerRack_seatsTheNodesOfItsEraAndEarlier(final GameTestHelper helper) {
        final ServerRackBlockEntity cabinet = placeRack(helper, new BlockPos(2, 2, 2),
                ComputingModule.ADVANCED_SUPERCOMPUTER_RACK.get());
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final ItemStackHandler rows = cabinet.getServers();
                    helper.assertTrue(rows.isItemValid(0, stack(ComputingModule.ADVANCED_SUPERCOMPUTER_NODE.get()))
                                    && rows.isItemValid(2, stack(ComputingModule.SUPERCOMPUTER_NODE.get())),
                            "the Advanced cabinet seats its own node and the Standard one");
                    helper.assertFalse(rows.isItemValid(4, stack(ComputingModule.ADVANCED_SERVER.get())),
                            "a supercomputer cabinet never seats a server, whatever its era");
                    rows.setStackInSlot(0, ServerStacks.advancedSupercomputerNode());
                    rows.setStackInSlot(2, ServerStacks.defaultSupercomputerNode());
                    helper.assertTrue(cabinet.unitCodeAt(0) == ServerRackBlockEntity.UNIT_NODE_2U_ADVANCED
                                    && cabinet.unitCodeAt(2) == ServerRackBlockEntity.UNIT_NODE_2U,
                            "each node shows in the cabinet in the shape of its own era");
                    helper.assertTrue(cabinet.anyComputerSeated(), "the Advanced node is a computer to the cabinet");
                    helper.assertValueEqual(ClusterManagementComputerBlockEntity.nodeName(cabinet, 0), "node",
                            "the cluster calls the Advanced node a node");
                })
                .thenSucceed();
    }

    /** The Advanced node seats only in its era's cabinet: too new for the Standard one, the wrong kind for servers. */
    @GameTest(template = ARENA)
    public static void advancedSupercomputerNode_isRefusedByEveryOtherRack(final GameTestHelper helper) {
        final ServerRackBlockEntity standard = placeRack(helper, new BlockPos(1, 2, 2),
                ComputingModule.SUPERCOMPUTER_RACK.get());
        final ServerRackBlockEntity servers = placeRack(helper, new BlockPos(5, 2, 2),
                ComputingModule.ADVANCED_SERVER_RACK.get());
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final ItemStack node = ServerStacks.advancedSupercomputerNode();
                    helper.assertFalse(standard.getServers().isItemValid(0, node),
                            "the Standard Supercomputer Rack refuses the later era's node");
                    helper.assertFalse(servers.getServers().isItemValid(0, node),
                            "an Advanced Server Rack refuses a node, though of its own era");
                })
                .thenSucceed();
    }

    /** The Advanced node is built from the Advanced parts: its era's board, the Phi 9000 and the Tessera cards. */
    @GameTest(template = ARENA)
    public static void advancedSupercomputerNode_assemblesFromTheAdvancedParts(final GameTestHelper helper) {
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        final InteractionHand hand = InteractionHand.MAIN_HAND;
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    player.setItemInHand(hand, stack(ComputingModule.ADVANCED_SUPERCOMPUTER_NODE.get()));
                    final ServerHardwareHandler node = new ServerHardwareHandler(player, hand);
                    helper.assertFalse(node.insertItem(ServerHardwareHandler.MOBO,
                                    stack(ComputingModule.MOTHERBOARD_EEB_S_2011.get()), true).isEmpty(),
                            "the Advanced node refuses a Standard board");
                    final int sleds = ServerHardwareHandler.GPU_START;
                    final boolean fitted =
                            fits(node, ServerHardwareHandler.MOBO, HardwareItems.MOTHERBOARD_EEB_A_SP3.get())
                            && fits(node, ServerHardwareHandler.CPU_START, HardwareItems.CPU_VELOCION_EPIC_7251.get())
                            && fits(node, ServerHardwareHandler.RAM_START, HardwareItems.RAM_DDR4_16384.get())
                            && fits(node, sleds, ComputingModule.PHI_9000.get())
                            && fits(node, sleds + 1, HardwareItems.GPU_TESSERA_V100.get())
                            && fits(node, sleds + 2, HardwareItems.GPU_TESSERA_A100.get())
                            && fits(node, sleds + 3, HardwareItems.GPU_TESSERA_H100.get())
                            && fits(node, ServerHardwareHandler.PSU, HardwareItems.PSU_1200P.get());
                    helper.assertTrue(fitted, "the era's board, an Epic, DDR4, the Phi 9000 and three Tesseras go in");
                    helper.assertFalse(node.insertItem(sleds + 4, stack(ComputingModule.PHI_7290.get()), true)
                            .isEmpty(), "a node takes one co-processor");
                    helper.assertTrue(ServerItem.build(player.getItemInHand(hand)) != null,
                            "the parts make a computer");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void serverCase_takesOnlyBoardsOfItsEra(final GameTestHelper helper) {
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        final InteractionHand hand = InteractionHand.MAIN_HAND;
        final ItemStack vintageBoard = stack(HardwareItems.MOTHERBOARD_EEB_VINTAGE.get());
        final ItemStack legacyBoard = stack(HardwareItems.MOTHERBOARD_EATX_LEGACY_S940.get());
        final ItemStack standardBoard = stack(ComputingModule.MOTHERBOARD_EEB_S_2011.get());
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    player.setItemInHand(hand, stack(ComputingModule.VINTAGE_SERVER.get()));
                    final ServerHardwareHandler vintage = new ServerHardwareHandler(player, hand);
                    helper.assertTrue(vintage.insertItem(ServerHardwareHandler.MOBO, vintageBoard.copy(), true).isEmpty(),
                            "a Vintage case takes a Vintage server board");
                    helper.assertFalse(vintage.insertItem(ServerHardwareHandler.MOBO, standardBoard.copy(), true).isEmpty(),
                            "a Vintage case refuses a Standard board");

                    player.setItemInHand(hand, stack(ComputingModule.LEGACY_SERVER.get()));
                    final ServerHardwareHandler legacy = new ServerHardwareHandler(player, hand);
                    helper.assertTrue(legacy.insertItem(ServerHardwareHandler.MOBO, legacyBoard.copy(), true).isEmpty(),
                            "a Legacy case takes a Legacy server board");
                    helper.assertFalse(legacy.insertItem(ServerHardwareHandler.MOBO, vintageBoard.copy(), true).isEmpty(),
                            "a Legacy case refuses a Vintage board");

                    player.setItemInHand(hand, stack(ComputingModule.SERVER.get()));
                    final ServerHardwareHandler standard = new ServerHardwareHandler(player, hand);
                    helper.assertTrue(standard.insertItem(ServerHardwareHandler.MOBO, standardBoard.copy(), true).isEmpty(),
                            "a Standard case takes a Standard server board");
                    helper.assertFalse(standard.insertItem(ServerHardwareHandler.MOBO, vintageBoard.copy(), true).isEmpty(),
                            "a Standard case refuses a Vintage board");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void rackModel_reportsWhatEachRowSeats(final GameTestHelper helper) {
        final ServerRackBlockEntity rack = placeRack(helper, new BlockPos(2, 2, 2), ComputingModule.SERVER_RACK.get());
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final ItemStackHandler rows = rack.getServers();
                    rows.setStackInSlot(0, ServerStacks.defaultServer().copy());   // a Standard 1U in row 0
                    rows.setStackInSlot(1, stack(ComputingModule.STORAGE_SERVER.get()));   // 2U: rows 1 and 2
                    rows.setStackInSlot(3, stack(ComputingModule.LEGACY_SERVER.get()));
                    rows.setStackInSlot(5, stack(ComputingModule.VINTAGE_SERVER.get()));
                    rows.setStackInSlot(6, stack(ComputingModule.KVM_SWITCH.get()));
                    rows.setStackInSlot(7, stack(ComputingModule.RACK_UPS.get()));
                    helper.assertTrue(rack.unitCodeAt(0) == ServerRackBlockEntity.UNIT_SERVER_STANDARD
                                    && rack.unitCodeAt(1) == ServerRackBlockEntity.UNIT_STORAGE_2U
                                    && rack.unitCodeAt(2) == ServerRackBlockEntity.UNIT_NONE
                                    && rack.unitCodeAt(3) == ServerRackBlockEntity.UNIT_SERVER_LEGACY
                                    && rack.unitCodeAt(4) == ServerRackBlockEntity.UNIT_NONE
                                    && rack.unitCodeAt(5) == ServerRackBlockEntity.UNIT_SERVER_VINTAGE
                                    && rack.unitCodeAt(6) == ServerRackBlockEntity.UNIT_KVM_SWITCH
                                    && rack.unitCodeAt(7) == ServerRackBlockEntity.UNIT_RACK_UPS,
                            "every row reports the unit it starts, and the second row of a 2U reports nothing");
                    helper.assertTrue(rack.anyComputerSeated() && rack.anyBayOn(),
                            "seated computers with their bays on spin the fans and light the bar");
                    final CompoundTag tag = rack.getUpdateTag(helper.getLevel().registryAccess());
                    final byte[] units = tag.getByteArray("Units");
                    helper.assertTrue(units.length == ServerRackBlockEntity.CAPACITY_U
                                    && units[1] == ServerRackBlockEntity.UNIT_STORAGE_2U
                                    && units[6] == ServerRackBlockEntity.UNIT_KVM_SWITCH,
                            "the block update carries one unit code per row for the client's model");
                    for (final int computer : rack.computerSlots()) {
                        rack.toggleBayPower(computer);
                    }
                    helper.assertTrue(!rack.anyBayOn() && rack.anyComputerSeated(),
                            "with every bay off the fans stop and the bar shows seated-but-dark");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void supercomputerRack_servicePanelComesOffOnlyThere(final GameTestHelper helper) {
        final ServerRackBlockEntity cabinet = placeRack(helper, new BlockPos(1, 2, 2), ComputingModule.SUPERCOMPUTER_RACK.get());
        final ServerRackBlockEntity rack = placeRack(helper, new BlockPos(5, 2, 2), ComputingModule.SERVER_RACK.get());
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    helper.assertFalse(cabinet.servicePanelOff(), "the livery panel starts on");
                    cabinet.toggleServicePanel();
                    helper.assertTrue(cabinet.servicePanelOff(), "sneak-use takes the panel off");
                    helper.assertTrue(cabinet.getUpdateTag(helper.getLevel().registryAccess()).getBoolean("ServicePanelOff"),
                            "the panel state travels to the client with the block update");
                    cabinet.toggleServicePanel();
                    helper.assertFalse(cabinet.servicePanelOff(), "and puts it back");
                    rack.toggleServicePanel();
                    helper.assertFalse(rack.servicePanelOff(), "a server rack has no panel to take off");
                    cabinet.getServers().setStackInSlot(2, ServerStacks.defaultSupercomputerNode());
                    helper.assertTrue(cabinet.unitCodeAt(2) == ServerRackBlockEntity.UNIT_NODE_2U
                                    && cabinet.unitCodeAt(3) == ServerRackBlockEntity.UNIT_NONE,
                            "a seated node reports its start row for the chassis bands");
                })
                .thenSucceed();
    }
}
