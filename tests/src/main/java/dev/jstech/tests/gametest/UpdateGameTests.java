/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.engine.CraftRequest;
import dev.jstech.computers.engine.EngineVerb;
import dev.jstech.computers.engine.NetworkOperationsService;
import dev.jstech.computers.operation.NetworkUpdateOperation;
import dev.jstech.computers.operation.payload.OperationRecord;
import dev.jstech.computers.program.IqlEngine;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.storage.ExternalDataPort;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.computers.workshop.UpdateAction;
import dev.jstech.computers.workshop.UpdateDoor;
import dev.jstech.computers.workshop.UpdateDrawer;
import dev.jstech.computers.workshop.UpdateRequest;
import dev.jstech.computers.workshop.Workshop;
import dev.jstech.computers.workshop.WorkshopCard;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestCables;
import dev.jstech.tests.testkit.TestWorldBuilder;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jetbrains.annotations.Nullable;

/**
 * UPDATE, the network's door to a Personal Computer's personal-use cards: an item the network holds goes to the card,
 * the card works on it at the Workshop's rules, and it comes back to its server. What it refuses (anything but a
 * Personal Computer's own cards, a paid action with nobody there to pay, a network with no engine), the drawer the
 * item waits in (seen by no query, saved with the computer, handed to the card's output after a reload, dropped with
 * the computer), the one furnace both doors share, and that no craft can ever use a card.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class UpdateGameTests {

    private static final String ARENA = "empty";
    private static final int SETTLE = 6;
    private static final BlockPos PC = new BlockPos(5, 2, 3);
    private static final BlockPos PC_CABLE = new BlockPos(4, 2, 3);
    /** The card slots after the graphics card in the first. */
    private static final int FIRST_FREE_CARD_SLOT = PersonalComputerBlockEntity.GPU_SLOTS_START + 1;

    private UpdateGameTests() {
    }

    /**
     * A statement of the network's language sent from the computer smelts the raw iron at the Furnace Card and writes
     * the ingots back: the log shows the three steps and the item before and after.
     */
    @GameTest(template = ARENA, timeoutTicks = 900)
    public static void smelt_writesTheIngotsBackAndLogsTheThreeSteps(final GameTestHelper helper) {
        final TestWorldBuilder.CraftingNetwork net = TestWorldBuilder.forGameTest(helper).buildCraftingNetwork();
        final PersonalComputerBlockEntity pc = placePc(helper);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    install(pc, WorkshopCard.FURNACE);
                    net.seed(Items.RAW_IRON, 5);
                })
                .thenWaitUntil(() -> helper.assertTrue(pc.networkUuid() != null && held(net, Items.RAW_IRON) == 5L,
                        "the network holds the raw iron and the computer is on it"))
                .thenExecute(() -> {
                    final IqlEngine.Outcome sent = net.mainframe().networkOperations().query(
                            IqlEngine.viewOf(new ServerCliComputer(pc, helper.getLevel())),
                            "UPDATE 3 raw_iron SET SMELT", 64);
                    helper.assertTrue(sent.ok(), "the statement is taken: " + sent.message());
                })
                .thenWaitUntil(() -> helper.assertTrue(held(net, Items.IRON_INGOT) == 3L,
                        "three ingots come back; got " + held(net, Items.IRON_INGOT)))
                .thenExecute(() -> {
                    helper.assertTrue(held(net, Items.RAW_IRON) == 2L, "two raw iron are left");
                    final OperationRecord row = lastUpdate(net);
                    helper.assertTrue(row != null && row.status() == OperationRecord.STATUS_COMPLETED,
                            "the log holds the UPDATE as done; got " + row);
                    helper.assertTrue(row.subs().stream().map(OperationRecord.SubRow::server).toList()
                                    .equals(List.of("SUB_SELECT", "SUB_UPDATE", "SUB_INSERT")),
                            "with its three steps; got " + row.subs());
                    helper.assertTrue(row.moves().stream().anyMatch(move -> move.from().equals("Raw Iron")
                            && move.to().equals("Iron Ingot") && move.qty() == 3L), "and the before and after");
                    helper.assertTrue(pc.updateDrawer().free(), "the drawer is free again");
                })
                .thenSucceed();
    }

    /** The card's one furnace serves both doors: the UPDATE waits while the Workshop's own smelting runs. */
    @GameTest(template = ARENA, timeoutTicks = 900)
    public static void smelt_waitsWhileTheWorkshopSmelts(final GameTestHelper helper) {
        final TestWorldBuilder.CraftingNetwork net = TestWorldBuilder.forGameTest(helper).buildCraftingNetwork();
        final PersonalComputerBlockEntity pc = placePc(helper);
        final NetworkUpdateOperation[] op = new NetworkUpdateOperation[1];
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    install(pc, WorkshopCard.FURNACE);
                    net.seed(Items.RAW_IRON, 1);
                    pc.workshop().put(Workshop.FURNACE_IN, new ItemStack(Items.RAW_GOLD, 2));
                })
                .thenWaitUntil(() -> helper.assertTrue(held(net, Items.RAW_IRON) == 1L, "the network holds it"))
                .thenExecute(() -> op[0] = net.mainframe().networkOperations().update(request(pc, Items.RAW_IRON, 1,
                        UpdateAction.SMELT, null)))
                .thenExecuteAfter(SETTLE, () -> {
                    helper.assertTrue(op[0] != null && op[0].isWaiting(), "the UPDATE waits its turn");
                    helper.assertTrue(pc.updateDrawer().free(), "and holds nothing while it waits");
                })
                .thenWaitUntil(() -> helper.assertTrue(op[0].isDone()
                        && op[0].status() == OperationRecord.STATUS_COMPLETED, "it runs once the Workshop is done"))
                .thenExecute(() -> helper.assertTrue(pc.workshop().slot(Workshop.FURNACE_OUT).is(Items.GOLD_INGOT)
                        && held(net, Items.IRON_INGOT) == 1L, "both smelted, each where its door sends it"))
                .thenSucceed();
    }

    /** A paid action needs the one who asked to be there: away, the item goes back as it was and nothing is paid. */
    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void enchant_isRefusedWithTheAskerAway(final GameTestHelper helper) {
        final TestWorldBuilder.CraftingNetwork net = TestWorldBuilder.forGameTest(helper).buildCraftingNetwork();
        final PersonalComputerBlockEntity pc = placePc(helper);
        final NetworkUpdateOperation[] op = new NetworkUpdateOperation[1];
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    install(pc, WorkshopCard.ENCHANTING);
                    net.seed(Items.DIAMOND_SWORD, 1);
                })
                .thenWaitUntil(() -> helper.assertTrue(held(net, Items.DIAMOND_SWORD) == 1L, "the network holds it"))
                .thenExecute(() -> {
                    helper.assertTrue(UpdateDoor.refusal(helper.getLevel(), pc, StorageKey.of(Items.DIAMOND_SWORD),
                            UpdateAction.ENCHANT, null) != null, "a door refuses it with nobody asking");
                    op[0] = net.mainframe().networkOperations().update(new UpdateRequest(pc.getBlockPos(),
                            StorageKey.of(Items.DIAMOND_SWORD), 1, UpdateAction.ENCHANT, 2, "", null, null,
                            UUID.randomUUID(), "test"));
                })
                .thenWaitUntil(() -> helper.assertTrue(op[0].isDone(), "the UPDATE settles"))
                .thenExecute(() -> {
                    helper.assertTrue(op[0].status() == OperationRecord.STATUS_FAILED
                                    && op[0].cause().key().equals(NetworkUpdateOperation.NOBODY_TO_PAY.key()),
                            "it fails because nobody was there to pay; got " + op[0].status());
                    helper.assertTrue(held(net, Items.DIAMOND_SWORD) == 1L
                                    && net.storage(helper.getLevel()).query().keySet().stream().noneMatch(key ->
                                    key.item() == Items.DIAMOND_SWORD && key.stack(1).isEnchanted()),
                            "and the sword is back, not enchanted");
                })
                .thenSucceed();
    }

    /**
     * The drawer is seen by no query of the network nor the computer's own storage, is saved with the computer and
     * hands its work to the card's output when it comes back with no UPDATE behind it, and falls out with the
     * computer.
     */
    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void drawer_isUnseenSavedAndFallsOutWithTheComputer(final GameTestHelper helper) {
        final TestWorldBuilder.CraftingNetwork net = TestWorldBuilder.forGameTest(helper).buildCraftingNetwork();
        final PersonalComputerBlockEntity pc = placePc(helper);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    install(pc, WorkshopCard.FURNACE);
                    pc.updateDrawer().hold(UUID.randomUUID(), WorkshopCard.FURNACE, true);
                    pc.updateDrawer().put(UpdateDrawer.WORK, new ItemStack(Items.RAW_COPPER, 7));
                })
                .thenExecuteAfter(SETTLE, () -> {
                    helper.assertTrue(held(net, Items.RAW_COPPER) == 0L, "no query of the network sees it");
                    helper.assertTrue(pc.localStore().count(StorageKey.of(Items.RAW_COPPER)) == 0L,
                            "nor the computer's own storage");
                    final CompoundTag saved = pc.saveWithFullMetadata(helper.getLevel().registryAccess());
                    final BlockEntity loaded = BlockEntity.loadStatic(pc.getBlockPos(), pc.getBlockState(), saved,
                            helper.getLevel().registryAccess());
                    helper.assertTrue(loaded instanceof PersonalComputerBlockEntity, "the computer loads again");
                    final PersonalComputerBlockEntity again = (PersonalComputerBlockEntity) loaded;
                    helper.assertTrue(again.updateDrawer().slot(UpdateDrawer.WORK).getCount() == 7,
                            "with the drawer's work saved in it");
                    helper.assertTrue(again.updateDrawer().recover(again.workshop()).isEmpty()
                                    && again.workshop().slot(Workshop.FURNACE_OUT).getCount() == 7,
                            "and, with no UPDATE behind it, handed to the card's output");
                    helper.assertTrue(pc.workshopDrops().stream().anyMatch(stack -> stack.is(Items.RAW_COPPER)
                            && stack.getCount() == 7), "the drawer falls out with the computer");
                })
                .thenSucceed();
    }

    /**
     * No craft can use a card: the computer exposes nothing a Crafting Interface or a bus could reach, and a craft
     * that would need a smelt finds no way with only the Furnace Card on the network.
     */
    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void cards_areNeverAStageOfACraft(final GameTestHelper helper) {
        final TestWorldBuilder.CraftingNetwork net = TestWorldBuilder.forGameTest(helper).buildCraftingNetwork();
        final PersonalComputerBlockEntity pc = placePc(helper);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    install(pc, WorkshopCard.FURNACE);
                    net.seed(Items.RAW_IRON, 4);
                })
                .thenExecuteAfter(SETTLE, () -> {
                    final BlockPos at = pc.getBlockPos();
                    helper.assertTrue(helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, at, null)
                            == null, "the computer has no inventory to reach");
                    for (final Direction side : Direction.values()) {
                        helper.assertTrue(helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, at, side)
                                == null && ExternalDataPort.at(helper.getLevel(), at, side).isEmpty(),
                                "nothing on its " + side + " side a Crafting Interface would take for a machine");
                    }
                    helper.assertTrue(net.mainframe().networkOperations().craft(CraftRequest.of(
                                    StorageKey.of(Items.IRON_INGOT), 1, false, "test", null)) == null,
                            "a craft of an ingot finds no way to smelt with only the card");
                })
                .thenSucceed();
    }

    /** Only a Personal Computer's own cards: a job of the network's language runs as the Mainframe and has none. */
    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void update_isOnlyAPersonalComputers(final GameTestHelper helper) {
        final TestWorldBuilder.CraftingNetwork net = TestWorldBuilder.forGameTest(helper).buildCraftingNetwork();
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    helper.assertTrue(UpdateDoor.refusal(helper.getLevel(), net.mainframe(),
                            StorageKey.of(Items.RAW_IRON), UpdateAction.SMELT, null) != null,
                            "the Mainframe has no cards of its own to lend");
                    helper.assertTrue(UpdateDoor.refusal(helper.getLevel(), net.cc(),
                            StorageKey.of(Items.RAW_IRON), UpdateAction.SMELT, null) != null,
                            "nor has a Crafting Computer");
                })
                .thenSucceed();
    }

    /** With no engine running, an UPDATE is refused as a craft is: nothing starts, and the service says why. */
    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void update_isRefusedWithNoEngine(final GameTestHelper helper) {
        final TestWorldBuilder.CraftingNetwork net = TestWorldBuilder.forGameTest(helper).buildCraftingNetwork();
        final PersonalComputerBlockEntity pc = placePc(helper);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    install(pc, WorkshopCard.FURNACE);
                    net.seed(Items.RAW_IRON, 2);
                    net.mainframe().setEngineRunning(false);
                })
                .thenExecuteAfter(SETTLE, () -> {
                    final NetworkOperationsService door = net.mainframe().networkOperations();
                    helper.assertTrue(door.refusal(EngineVerb.UPDATE) != null
                                    && door.refusal(EngineVerb.UPDATE).english().equals(
                                    NetworkOperationsService.UNAVAILABLE.english()),
                            "the network says its service is unavailable");
                    helper.assertTrue(door.update(request(pc, Items.RAW_IRON, 1, UpdateAction.SMELT, null)) == null,
                            "and starts nothing");
                    net.mainframe().setEngineRunning(true);
                })
                .thenSucceed();
    }

    /** An UPDATE belongs to its computer: the computer stopping discards it, and what the card held stays with it. */
    @GameTest(template = ARENA, timeoutTicks = 600)
    public static void update_isDiscardedWhenTheComputerStops(final GameTestHelper helper) {
        final TestWorldBuilder.CraftingNetwork net = TestWorldBuilder.forGameTest(helper).buildCraftingNetwork();
        final PersonalComputerBlockEntity pc = placePc(helper);
        final NetworkUpdateOperation[] op = new NetworkUpdateOperation[1];
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    install(pc, WorkshopCard.FURNACE);
                    net.seed(Items.RAW_IRON, 4);
                })
                .thenWaitUntil(() -> helper.assertTrue(held(net, Items.RAW_IRON) == 4L, "the network holds it"))
                .thenExecute(() -> op[0] = net.mainframe().networkOperations().update(request(pc, Items.RAW_IRON, 4,
                        UpdateAction.SMELT, null)))
                .thenWaitUntil(() -> helper.assertTrue(!pc.updateDrawer().slot(UpdateDrawer.WORK).isEmpty(),
                        "the raw iron reaches the card"))
                .thenExecute(pc::togglePower)
                .thenWaitUntil(() -> helper.assertTrue(op[0].isDone()
                        && op[0].status() == OperationRecord.STATUS_DISCARDED, "the UPDATE is discarded"))
                .thenExecute(() -> {
                    final long raw = held(net, Items.RAW_IRON) + countAtOutput(pc, Items.RAW_IRON);
                    final long ingots = held(net, Items.IRON_INGOT) + countAtOutput(pc, Items.IRON_INGOT);
                    helper.assertTrue(raw + ingots == 4L && countAtOutput(pc, Items.RAW_IRON) > 0L,
                            "nothing is lost and what the card held waits at its output; raw " + raw + ", ingots "
                                    + ingots);
                    helper.assertTrue(pc.updateDrawer().free(), "and the drawer is let go");
                })
                .thenSucceed();
    }

    private static PersonalComputerBlockEntity placePc(final GameTestHelper helper) {
        TestCables.lay(helper, PC_CABLE, ComputingModule.ETHERNET_CABLE);
        return TestWorldBuilder.forGameTest(helper).placeRunningPersonalComputer(PC);
    }

    private static UpdateRequest request(final PersonalComputerBlockEntity pc, final Item item, final long quantity,
                                         final UpdateAction action, @Nullable final StorageKey with) {
        return new UpdateRequest(pc.getBlockPos(), StorageKey.of(item), quantity, action, 0, "", with, null, null,
                "test");
    }

    private static long held(final TestWorldBuilder.CraftingNetwork net, final Item item) {
        return net.storage((ServerLevel) net.mainframe().getLevel()).query().getOrDefault(StorageKey.of(item), 0L);
    }

    /* How many of {@code item} wait at the Furnace Card's output: on its slot in the Workshop or in the drawer. */
    private static long countAtOutput(final PersonalComputerBlockEntity pc, final Item item) {
        long count = 0L;
        final ItemStack out = pc.workshop().slot(Workshop.FURNACE_OUT);
        if (out.is(item)) {
            count += out.getCount();
        }
        for (int slot = UpdateDrawer.OUT; slot < UpdateDrawer.SLOTS; slot++) {
            final ItemStack waiting = pc.updateDrawer().slot(slot);
            if (waiting.is(item)) {
                count += waiting.getCount();
            }
        }
        return count;
    }

    @Nullable
    private static OperationRecord lastUpdate(final TestWorldBuilder.CraftingNetwork net) {
        for (final OperationRecord row : net.mainframe().recentOperations()) {
            if (row.type() == OperationRecord.TYPE_UPDATE) {
                return row;
            }
        }
        return null;
    }

    private static void install(final PersonalComputerBlockEntity pc, final WorkshopCard card) {
        final ItemStack stack = UpdateDoor.cardItem(card);
        for (int slot = FIRST_FREE_CARD_SLOT; slot < PersonalComputerBlockEntity.GPU_SLOTS_START
                + PersonalComputerBlockEntity.GPU_SLOTS; slot++) {
            if (pc.getHardware().getStackInSlot(slot).isEmpty()) {
                pc.getHardware().setStackInSlot(slot, stack);
                return;
            }
        }
        throw new IllegalStateException("no free card slot");
    }
}
