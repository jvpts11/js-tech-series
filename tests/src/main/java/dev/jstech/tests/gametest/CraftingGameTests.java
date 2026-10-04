/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.HardwareItems;
import dev.jstech.computers.block.ServerRackBlock;
import dev.jstech.computers.block.ServerRackStructure;
import dev.jstech.computers.blockentity.CraftingComputerBlockEntity;
import dev.jstech.computers.blockentity.HbwInterfaceBlockEntity;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.PatternEncoderBlockEntity;
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.computers.crafting.CraftingPattern;
import dev.jstech.computers.crafting.MultiStagePattern;
import dev.jstech.computers.crafting.NetworkCraftOperation;
import dev.jstech.computers.crafting.NetworkMultiStageOperation;
import dev.jstech.computers.crafting.NetworkProcessingOperation;
import dev.jstech.computers.crafting.NetworkRecipe;
import dev.jstech.computers.crafting.PatternWorkbench;
import dev.jstech.computers.crafting.PendingCraftOperation;
import dev.jstech.computers.crafting.ProcessingPattern;
import dev.jstech.computers.item.CraftingCardItem;
import dev.jstech.computers.menu.CraftingComputerMenu;
import dev.jstech.computers.menu.PatternEncoderMenu;
import dev.jstech.computers.operation.NetworkStorage;
import dev.jstech.computers.operation.payload.OperationRecord;
import dev.jstech.computers.os.FilesystemKind;
import dev.jstech.computers.os.fs.CraftFile;
import dev.jstech.computers.os.fs.DiskFilesystem;
import dev.jstech.computers.os.fs.FileType;
import dev.jstech.computers.os.media.MediaReaderBlockEntity;
import dev.jstech.computers.storage.ExternalDataPort;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestMachineBlockEntity;
import dev.jstech.tests.TestMachines;
import dev.jstech.tests.testkit.CraftingRig;
import dev.jstech.tests.testkit.ServerStacks;
import dev.jstech.tests.testkit.TestCables;
import dev.jstech.tests.testkit.TestWorldBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * GameTests for autocrafting over a real network: the Pattern Encoder writing recipes onto media, the bench recipes
 * the Crafting Cards keep, bench crafts end to end, the Supercomputer's parallel crafts, and processing recipes run
 * on a real machine through a Crafting Interface, with what comes out credited by a Crafting Receiving Bus and every
 * item conserved whatever happens to the job.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class CraftingGameTests {

    private static final String ARENA = "empty";
    private static final int SETTLE = 4;
    private static final String NO_INTERFACE = "jsc.operation.failure.no_interface";

    private CraftingGameTests() {
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void studio_benchDraftBurnsOntoMediaAtTheLinkedEncoder(final GameTestHelper helper) {
        /*
         * The workbench lives on the computer; the encoder beside it is its burner. One oak log resolves to
         * four planks through the recipe book, and the burned file reads back as that pattern.
         */
        final Network net = buildCraftingNetwork(helper);
        final BlockPos encoderPos = new BlockPos(5, 2, 3);
        helper.setBlock(encoderPos, ComputingModule.PATTERN_ENCODER.get());
        if (!(helper.getBlockEntity(encoderPos) instanceof PatternEncoderBlockEntity encoder)) {
            throw new IllegalStateException("no pattern encoder at " + encoderPos);
        }
        encoder.media().setStackInSlot(0, new ItemStack(ComputingModule.DVD_RW.get()));
        final var studio = net.cc.studio();
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    helper.assertTrue(encoder.ownerPos() != null && encoder.ownerPos().equals(net.cc.getBlockPos()),
                            "the encoder links to the adjacent computer; got " + encoder.ownerPos());
                    studio.refreshPreview(helper.getLevel());
                    helper.assertTrue(studio.serialize(PatternWorkbench.Kind.BENCH,
                            helper.getLevel().registryAccess()).isEmpty(), "an empty bench serializes to nothing");
                    studio.setGhost(0, new ItemStack(Items.OAK_LOG));
                    studio.refreshPreview(helper.getLevel());
                    helper.assertTrue(studio.preview().is(Items.OAK_PLANKS) && studio.preview().getCount() == 4,
                            "one log previews four planks");
                    final var content = studio.serialize(PatternWorkbench.Kind.BENCH,
                            helper.getLevel().registryAccess());
                    helper.assertTrue(content.isPresent(), "a resolved bench draft serializes");
                    helper.assertTrue(encoder.queueBurn("oak_planks", content.get()), "the encoder queues the burn");
                })
                .thenExecuteAfter(120, () -> {
                    final ItemStack media = encoder.mediaStack();
                    final List<DiskFilesystem.FileEntry> files =
                            DiskFilesystem.list(media, "", FilesystemKind.HIERARCHICAL);
                    int craftCount = 0;
                    String craftPath = null;
                    for (final DiskFilesystem.FileEntry e : files) {
                        if (e.type() == FileType.CRAFT) {
                            craftCount++;
                            craftPath = e.path();
                        }
                    }
                    helper.assertTrue(craftCount == 1, "exactly one .craft file is burned");
                    final var content = DiskFilesystem.read(media, craftPath);
                    helper.assertTrue(content.isPresent(), "the .craft file is readable");
                    final var parsed = CraftFile.parse(content.get(), helper.getLevel().registryAccess());
                    helper.assertTrue(parsed.isPresent(), "the .craft round-trips back into a pattern");
                    helper.assertTrue(parsed.get().result().is(Items.OAK_PLANKS)
                            && parsed.get().result().getCount() == 4, "the burned pattern produces four planks");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void encoder_requiresWritableMediaOfItsEra(final GameTestHelper helper) {
        final BlockPos pos = new BlockPos(2, 2, 2);
        helper.setBlock(pos, ComputingModule.PATTERN_ENCODER.get());
        if (!(helper.getBlockEntity(pos) instanceof PatternEncoderBlockEntity encoder)) {
            throw new IllegalStateException("no pattern encoder at " + pos);
        }
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    helper.assertFalse(encoder.hasMedia(), "nothing in the bay to write to");
                    helper.assertFalse(encoder.media().isItemValid(0, new ItemStack(ComputingModule.CD_ROM.get())),
                            "read-only media is rejected by the bay");
                    helper.assertFalse(encoder.media().isItemValid(0,
                            new ItemStack(ComputingModule.FLOPPY_DISK.get())), "a Standard encoder refuses a floppy");
                    helper.assertTrue(encoder.media().isItemValid(0, new ItemStack(ComputingModule.DVD_RW.get())),
                            "a Standard encoder takes a DVD-RW");
                })
                .thenSucceed();
    }

    /** The bench recipes live in the cards' ROM: a card keeps as many as its era allows, and a second adds room. */
    @GameTest(template = ARENA)
    public static void recipeRom_capsAtTheCardsRom(final GameTestHelper helper) {
        final CraftingComputerBlockEntity computer = placeComputerWithCard(helper, new BlockPos(2, 2, 2));
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final int size = computer.romSize();
                    helper.assertTrue(size == CraftingCardItem.romSize(
                                    new ItemStack(ComputingModule.CRAFTING_CARD_T2.get())),
                            "one card's ROM is the computer's whole ROM; got " + size);
                    for (int i = 1; i <= size; i++) {
                        helper.assertTrue(computer.loadPattern(planksPattern(i)), "pattern " + i + " fits");
                    }
                    helper.assertFalse(computer.loadPattern(sticksPattern()), "one more is refused");
                    helper.assertTrue(computer.romUsed() == size, "the ROM sits exactly at its size");
                    computer.getHardware().setStackInSlot(CraftingComputerBlockEntity.PCIE_SLOTS_START + 2,
                            new ItemStack(ComputingModule.CRAFTING_CARD_T2.get()));
                    helper.assertTrue(computer.romSize() == 2 * size, "a second card doubles the ROM");
                    helper.assertTrue(computer.loadPattern(sticksPattern()), "the refused pattern now fits");
                    helper.assertFalse(computer.loadPattern(planksPattern(1)), "a pattern already kept is refused");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void recipeRom_persistsThroughNbtRoundTrip(final GameTestHelper helper) {
        final CraftingComputerBlockEntity computer = placeComputerWithCard(helper, new BlockPos(2, 2, 2));
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    computer.loadPattern(planksPattern(1));
                    computer.loadPattern(sticksPattern());
                    final CompoundTag saved = computer.saveWithFullMetadata(helper.getLevel().registryAccess());

                    final CraftingComputerBlockEntity reloaded = new CraftingComputerBlockEntity(
                            computer.getBlockPos(), computer.getBlockState());
                    reloaded.loadWithComponents(saved, helper.getLevel().registryAccess());
                    helper.assertTrue(reloaded.romUsed() == 2, "the ROM survives the NBT round-trip");
                    helper.assertTrue(reloaded.romContains(sticksPattern()),
                            "the reloaded ROM still holds the same recipes");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void craftingPattern_comparesByValueNotIdentity(final GameTestHelper helper) {
        final CraftingPattern a = planksPattern(4);
        final CraftingPattern b = planksPattern(4);
        helper.assertTrue(a.equals(b), "two patterns holding the same recipe must be equal");
        helper.assertTrue(a.hashCode() == b.hashCode(), "equal patterns must share a hash code");
        helper.assertFalse(a.equals(planksPattern(8)), "a different result count is a different pattern");
        helper.assertFalse(a.equals(sticksPattern()), "a different recipe is not equal");
        helper.succeed();
    }

    // Bench crafts, end to end over a real network

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void craft_executesSinglePatternEndToEnd(final GameTestHelper helper) {
        final Network net = buildCraftingNetwork(helper);
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    net.seed(Items.OAK_LOG, 2);
                    net.cc.loadPattern(planksPattern(4));
                })
                .thenExecuteAfter(SETTLE + 2, () -> helper.assertTrue(net.mainframe.submitNetworkCraft(
                        storageKey(Items.OAK_PLANKS), 8, false, "test") != null, "a feasible CRAFT is accepted"))
                .thenExecuteAfter(40, () -> {
                    final NetworkStorage storage = net.storage(helper);
                    helper.assertTrue(storage.count(Items.OAK_PLANKS) == 8,
                            "the network holds the 8 crafted planks; got " + storage.count(Items.OAK_PLANKS));
                    helper.assertTrue(storage.count(Items.OAK_LOG) == 0,
                            "both logs are consumed; got " + storage.count(Items.OAK_LOG));
                    helper.assertFalse(net.cc.craftBusy(), "the computer frees up after the craft");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void craft_recursesAndReturnsSurplus(final GameTestHelper helper) {
        final Network net = buildCraftingNetwork(helper);
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    net.seed(Items.OAK_LOG, 2);
                    net.cc.loadPattern(planksPattern(4));
                    net.cc.loadPattern(sticksPattern());
                })
                .thenExecuteAfter(SETTLE + 2, () -> helper.assertTrue(net.mainframe.submitNetworkCraft(
                        storageKey(Items.STICK), 4, false, "test") != null, "the recursive CRAFT is accepted"))
                .thenExecuteAfter(40, () -> {
                    final NetworkStorage storage = net.storage(helper);
                    helper.assertTrue(storage.count(Items.STICK) == 4,
                            "the network holds the 4 crafted sticks; got " + storage.count(Items.STICK));
                    helper.assertTrue(storage.count(Items.OAK_LOG) == 1,
                            "only one log is needed; got " + storage.count(Items.OAK_LOG));
                    helper.assertTrue(storage.count(Items.OAK_PLANKS) == 2,
                            "the 2 surplus planks return to storage; got " + storage.count(Items.OAK_PLANKS));
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void craft_partialScalesDownAndReportsIt(final GameTestHelper helper) {
        final Network net = buildCraftingNetwork(helper);
        final AtomicReference<NetworkCraftOperation> opHolder = new AtomicReference<>();
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    net.seed(Items.OAK_LOG, 1);
                    net.cc.loadPattern(planksPattern(4));
                })
                .thenExecuteAfter(SETTLE + 2, () -> {
                    helper.assertTrue(net.mainframe.submitNetworkCraft(
                                    storageKey(Items.OAK_PLANKS), 16, false, "test") == null,
                            "an infeasible strict CRAFT is rejected");
                    opHolder.set(net.mainframe.submitNetworkCraft(storageKey(Items.OAK_PLANKS), 16, true, "test"));
                    helper.assertTrue(opHolder.get() != null, "the partial CRAFT is accepted");
                })
                .thenExecuteAfter(40, () -> {
                    final NetworkCraftOperation op = opHolder.get();
                    helper.assertTrue(op.isDone(), "the partial CRAFT settles");
                    helper.assertTrue(op.craftStatus() == OperationRecord.STATUS_PARTIAL,
                            "a scaled-down craft settles as COMPLETED_PARTIAL");
                    helper.assertTrue(op.delivered() == 4, "one log yields 4 planks; got " + op.delivered());
                    helper.assertTrue(net.storage(helper).count(Items.OAK_PLANKS) == 4, "the 4 planks are stored");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void craft_drawsIngredientsAcrossTwoServersAndReleasesLocks(final GameTestHelper helper) {
        final Network net = buildCraftingNetwork(helper);
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    TestWorldBuilder.mountDefaultServer(net.rack, 1);
                    net.cc.loadPattern(planksPattern(4));
                })
                .thenExecuteAfter(SETTLE + 2, () -> {
                    /*
                     * One log on each server, so an 8-plank craft (2 logs) must draw from both, the case
                     * where the lock-order server and the drained server once diverged and left reservations.
                     */
                    net.rack.getServerStorage(0).insert(Items.OAK_LOG, 1);
                    net.rack.getServerStorage(1).insert(Items.OAK_LOG, 1);
                })
                .thenExecuteAfter(SETTLE + 2, () -> helper.assertTrue(
                        net.mainframe.submitNetworkCraft(storageKey(Items.OAK_PLANKS), 8, false, "test") != null,
                        "the two-server craft is accepted"))
                .thenExecuteAfter(40, () -> {
                    final NetworkStorage storage = net.storage(helper);
                    helper.assertTrue(storage.count(Items.OAK_PLANKS) == 8,
                            "8 planks crafted from logs on two servers; got " + storage.count(Items.OAK_PLANKS));
                    helper.assertTrue(storage.count(Items.OAK_LOG) == 0,
                            "both logs are consumed; got " + storage.count(Items.OAK_LOG));
                    helper.assertTrue(net.mainframe.networkIndex().activeLockCount() == 0,
                            "the craft releases every ingredient reservation");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void craft_withoutPatternIsRejected(final GameTestHelper helper) {
        final Network net = buildCraftingNetwork(helper);
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> helper.assertTrue(
                        net.mainframe.submitNetworkCraft(storageKey(Items.PISTON), 1, true, "test") == null,
                        "no pattern on the network produces pistons"))
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void serverRack_faceReflectsInstalledServers(final GameTestHelper helper) {
        final BlockPos rack = new BlockPos(2, 2, 2);
        final Direction facing = Direction.NORTH;
        helper.setBlock(rack, ComputingModule.SERVER_RACK.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, facing));
        ((ServerRackBlock) ComputingModule.SERVER_RACK.get()).setPlacedBy(helper.getLevel(),
                helper.absolutePos(rack), helper.getBlockState(rack), null, ItemStack.EMPTY);
        if (!(helper.getBlockEntity(rack) instanceof ServerRackBlockEntity rackBe)) {
            throw new IllegalStateException("no server rack at " + rack);
        }
        final BlockPos second = new BlockPos(ServerRackStructure.bayBlockPos(rack, facing, 1, 0));
        final BlockPos upper = new BlockPos(ServerRackStructure.bayBlockPos(rack, facing, 0, 1));
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    /*
                     * Each visual bay block covers two rack-unit rows (bay b = rows 2b and 2b+1),
                     * so servers in U0, U2 and U4 light the controller, second column and upper bay.
                     */
                    rackBe.getServers().setStackInSlot(0, ServerStacks.defaultServer());
                    rackBe.getServers().setStackInSlot(2, ServerStacks.defaultServer());
                    rackBe.getServers().setStackInSlot(4, ServerStacks.defaultServer());
                })
                .thenExecuteAfter(SETTLE, () -> {
                    helper.assertTrue(helper.getBlockState(rack).getValue(ServerRackBlock.BAYS) == 3,
                            "the controller bay lights up for its server");
                    helper.assertTrue(helper.getBlockState(second).getValue(ServerRackBlock.BAYS) == 3,
                            "the second column lights up for its server");
                    helper.assertTrue(helper.getBlockState(upper).getValue(ServerRackBlock.BAYS) == 3,
                            "the upper bay lights up for its server");
                    rackBe.getServers().setStackInSlot(2, ItemStack.EMPTY);
                })
                .thenExecuteAfter(SETTLE, () -> {
                    helper.assertTrue(helper.getBlockState(second).getValue(ServerRackBlock.BAYS) == 0,
                            "pulling a Server empties its bay on the face");
                    helper.assertTrue(helper.getBlockState(rack).getValue(ServerRackBlock.BAYS) == 3,
                            "the controller bay keeps its own server");
                })
                .thenSucceed();
    }

    // Supercomputer: Phi slots and parallel orchestration

    @GameTest(template = ARENA)
    public static void cluster_surveyAssignsSlotsAndBudget(final GameTestHelper helper) {
        final BlockPos hub = new BlockPos(2, 2, 2);
        // Three nodes in a row east of the interface: slots 1, 2, 3 by discovery order.
        placeCluster(helper, hub, 3);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    if (!(helper.getBlockEntity(hub) instanceof HbwInterfaceBlockEntity be)) {
                        throw new IllegalStateException("no hbw interface");
                    }
                    // 5100s fit slots 1-2 (8 + 16); the third 5100 is under-rated for slot 3.
                    helper.assertTrue(be.parallelCrafts() == 24, "two rated slots give 24; got "
                            + be.parallelCrafts());
                    final var slots = be.clusterSlots();
                    helper.assertTrue(slots.size() == 3, "three nodes surveyed");
                    helper.assertTrue(slots.get(2).code() == HbwInterfaceBlockEntity.SLOT_UNDER_RATED,
                            "a 5100 in slot 3 is flagged under-rated, never crashes");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void supercomputer_unlocksParallelCrafting(final GameTestHelper helper) {
        final Network net = buildCraftingNetwork(helper);
        final AtomicReference<NetworkCraftOperation> first = new AtomicReference<>();
        final AtomicReference<NetworkCraftOperation> second = new AtomicReference<>();
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    net.seed(Items.OAK_LOG, 8000);
                    net.cc.loadPattern(planksPattern(4));
                })
                .thenExecuteAfter(SETTLE + 2, () -> {
                    // No cluster yet: two long crafts, and the second must wait its turn.
                    first.set(net.mainframe.submitNetworkCraft(storageKey(Items.OAK_PLANKS), 12000, false, "test"));
                    second.set(net.mainframe.submitNetworkCraft(storageKey(Items.OAK_PLANKS), 12000, false, "test"));
                    helper.assertTrue(first.get() != null && second.get() != null, "both CRAFTs are accepted");
                })
                .thenExecuteAfter(2, () -> {
                    helper.assertFalse(first.get().isWaiting(), "the first craft claims the computer");
                    helper.assertTrue(second.get().isWaiting(), "without a Supercomputer the second craft waits");
                    first.get().abandon();
                    second.get().abandon();
                })
                .thenExecuteAfter(SETTLE, () -> placeCluster(helper, new BlockPos(2, 2, 3), 1))
                .thenExecuteAfter(SETTLE + 2, () -> {
                    /*
                     * Smaller than phase one: the first run consumed some logs before being
                     * abandoned, and BOTH locks must still be fully coverable at once.
                     */
                    first.set(net.mainframe.submitNetworkCraft(storageKey(Items.OAK_PLANKS), 8000, false, "test"));
                    second.set(net.mainframe.submitNetworkCraft(storageKey(Items.OAK_PLANKS), 8000, false, "test"));
                    helper.assertTrue(first.get() != null && second.get() != null,
                            "both CRAFTs are accepted with the Supercomputer");
                })
                .thenExecuteAfter(6, () -> {
                    helper.assertFalse(first.get().isWaiting(), "the first craft runs under the cluster");
                    helper.assertFalse(second.get().isWaiting(), "the cluster runs both crafts in parallel");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void supercomputer_fansOneCraftAcrossComputers(final GameTestHelper helper) {
        final Network net = buildCraftingNetwork(helper);
        final BlockPos hub = new BlockPos(2, 2, 3);
        final BlockPos cc2Pos = new BlockPos(4, 2, 1);
        final AtomicReference<NetworkCraftOperation> opHolder = new AtomicReference<>();
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    net.seed(Items.OAK_LOG, 8000);
                    net.cc.loadPattern(planksPattern(4));
                    placeSecondCraftingComputer(helper, cc2Pos).loadPattern(planksPattern(4));
                })
                .thenExecuteAfter(SETTLE + 2, () -> {
                    helper.assertTrue(net.mainframe.craftingComputerPositions().size() >= 2,
                            "both crafting computers join the network; got "
                                    + net.mainframe.craftingComputerPositions().size());
                    placeCluster(helper, hub, 1);
                })
                .thenExecuteAfter(SETTLE + 2, () -> {
                    if (!(helper.getBlockEntity(hub) instanceof HbwInterfaceBlockEntity sc) || !sc.clusterOnline()
                            || sc.parallelCrafts() < 2) {
                        helper.fail("the supercomputer cluster is not online with two slots or more");
                    }
                    helper.assertTrue(net.mainframe.supercomputerPositions().size() >= 1,
                            "the supercomputer is registered on the network");
                    opHolder.set(net.mainframe.submitNetworkCraft(storageKey(Items.OAK_PLANKS), 24000, false,
                            "test"));
                    helper.assertTrue(opHolder.get() != null, "the large craft is accepted");
                })
                .thenExecuteAfter(4, () -> {
                    /*
                     * The single request fanned out: it claimed one computer per supercomputer slot. The executor
                     * list persists after the craft settles, so this is robust to the craft finishing fast.
                     */
                    helper.assertTrue(opHolder.get().executorCount() >= 2,
                            "one large craft fans out across both crafting computers; executors="
                                    + opHolder.get().executorCount());
                })
                .thenSucceed();
    }

    // Recipes for one result, and the recipe a request names

    /** The recipes that make one result come in a stable order: the machine ones first, then the bench ones. */
    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void recipesFor_listsMachineRecipesThenBenchPatternsForTheResult(final GameTestHelper helper) {
        final Network net = buildCraftingNetwork(helper);
        final CraftingRig rig = kilnRig(helper, net);
        final StorageKey ingot = storageKey(Items.IRON_INGOT);
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    helper.assertTrue(rig.hold(NetworkRecipe.ofProcessing(smelt().withName("Blast", ""))),
                            "the processing recipe goes into the interface");
                    helper.assertTrue(rig.hold(NetworkRecipe.ofMultiStage(new MultiStagePattern(List.of(
                                    MultiStagePattern.Stage.proc(smelt()))).withName("Iron line", ""))),
                            "the multi-stage recipe goes into the interface");
                    helper.assertTrue(net.cc.loadPattern(nuggetsToIngot()), "the bench pattern loads");
                    helper.assertTrue(net.cc.loadPattern(new CraftingPattern(grid(Items.OAK_LOG),
                            new ItemStack(Items.OAK_PLANKS, 4))), "an unrelated bench pattern loads");
                })
                .thenExecuteAfter(SETTLE, () -> {
                    final List<NetworkRecipe> recipes = net.mainframe.recipesFor(ingot);
                    helper.assertTrue(recipes.size() == 3, "three recipes make the ingot; got " + recipes.size());
                    helper.assertTrue(recipes.get(0).proc().isPresent()
                                    && recipes.get(0).displayName().equals("Blast"),
                            "the processing recipe comes first; got " + recipes.get(0).displayName());
                    helper.assertTrue(recipes.get(1).multi().isPresent()
                                    && recipes.get(1).displayName().equals("Iron line"),
                            "the multi-stage recipe comes second; got " + recipes.get(1).displayName());
                    helper.assertTrue(recipes.get(2).bench().isPresent(), "the bench pattern comes last");
                    helper.assertTrue(net.mainframe.recipesFor(storageKey(Items.OAK_PLANKS)).size() == 1,
                            "the planks have their one bench pattern");
                    helper.assertTrue(net.mainframe.recipesFor(storageKey(Items.DIAMOND)).isEmpty(),
                            "nothing makes a diamond");
                })
                .thenSucceed();
    }

    /** A craft request that names a recipe runs that recipe: the pipeline, the machine, or the bench pattern's plan. */
    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void submitCraftRequest_runsTheRecipeThePlayerPicked(final GameTestHelper helper) {
        final Network net = buildCraftingNetwork(helper);
        final CraftingRig rig = kilnRig(helper, net);
        final StorageKey ingot = storageKey(Items.IRON_INGOT);
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    net.seed(Items.RAW_IRON, 16);
                    net.seed(Items.IRON_NUGGET, 18);
                    rig.hold(NetworkRecipe.ofProcessing(smelt().withName("Blast", "")));
                    rig.hold(NetworkRecipe.ofMultiStage(new MultiStagePattern(List.of(
                            MultiStagePattern.Stage.proc(smelt()))).withName("Iron line", "")));
                    net.cc.loadPattern(nuggetsToIngot());
                })
                .thenExecuteAfter(SETTLE, () -> {
                    final var multi = net.mainframe.submitCraftRequest(ingot, 1, false, "test", null, 1);
                    helper.assertTrue(multi instanceof NetworkMultiStageOperation,
                            "index 1 runs the pipeline; got " + multi);
                    final var machine = net.mainframe.submitCraftRequest(ingot, 1, false, "test", null, 0);
                    helper.assertTrue(machine instanceof NetworkProcessingOperation,
                            "index 0 runs the machine; got " + machine);
                    final var bench = net.mainframe.submitCraftRequest(ingot, 1, false, "test", null, 2);
                    helper.assertTrue(bench instanceof PendingCraftOperation,
                            "index 2 plans the bench pattern; got " + bench);
                    final var auto = net.mainframe.submitCraftRequest(ingot, 1, false, "test", null, 7);
                    helper.assertTrue(auto instanceof NetworkProcessingOperation,
                            "an index past the list is the machine's own choice, the first machine recipe; got "
                                    + auto);
                })
                .thenExecuteAfter(60, () -> helper.assertTrue(net.storage(helper).count(ingot) >= 4,
                        "the bench plan and the three machine runs make an ingot each; ingots="
                                + net.storage(helper).count(ingot)))
                .thenSucceed();
    }

    // Processing recipes on a real machine

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void processing_deliversInputsToTheMachine(final GameTestHelper helper) {
        final Network net = buildCraftingNetwork(helper);
        final CraftingRig rig = kilnRig(helper, net);
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    net.seed(Items.COBBLESTONE, 64);
                    // Held, the kiln keeps what it is given, so the delivery itself can be seen.
                    kiln(rig).setHeld(true);
                    rig.hold(cobblePattern(200));
                })
                .thenExecuteAfter(SETTLE + 2, () -> helper.assertTrue(
                        net.mainframe.submitNetworkProcessing(cobblePattern(200), 4, "test") != null,
                        "the processing operation is accepted"))
                .thenExecuteAfter(15, () -> helper.assertTrue(kiln(rig).input(0).is(Items.COBBLESTONE)
                                && kiln(rig).input(0).getCount() == 4,
                        "the interface delivered the four lots from the network into the machine; got "
                                + kiln(rig).input(0)))
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void processing_collectsOutputsIntoTheNetwork(final GameTestHelper helper) {
        final Network net = buildCraftingNetwork(helper);
        final CraftingRig rig = kilnRig(helper, net);
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    net.seed(Items.COBBLESTONE, 64);
                    rig.hold(cobblePattern(200));
                })
                .thenExecuteAfter(SETTLE + 2, () -> helper.assertTrue(
                        net.mainframe.submitNetworkProcessing(cobblePattern(200), 4, "test") != null,
                        "the processing operation is accepted"))
                .thenExecuteAfter(60, () -> {
                    final long stone = net.storage(helper).count(storageKey(Items.STONE));
                    helper.assertTrue(stone == 4, "the Receiving Bus credited the four stone made; stone=" + stone);
                    helper.assertTrue(kiln(rig).output().isEmpty(), "nothing the job made is left in the kiln");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void multiStage_runsItsProcessingStage(final GameTestHelper helper) {
        final Network net = buildCraftingNetwork(helper);
        final CraftingRig rig = kilnRig(helper, net);
        final MultiStagePattern multi = new MultiStagePattern(List.of(MultiStagePattern.Stage.proc(
                cobblePattern(200))));
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    net.seed(Items.COBBLESTONE, 64);
                    rig.hold(NetworkRecipe.ofMultiStage(multi));
                })
                .thenExecuteAfter(SETTLE + 2, () -> helper.assertTrue(
                        net.mainframe.submitNetworkMultiStage(multi, 2, "test") != null,
                        "the multi-stage operation is accepted"))
                .thenExecuteAfter(60, () -> {
                    final long stone = net.storage(helper).count(storageKey(Items.STONE));
                    helper.assertTrue(stone >= 2, "the multi-stage ran its processing stage; stone=" + stone);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void craftRequest_runsAMultiStageRecipeTheRecursivePlannerCannotSee(final GameTestHelper helper) {
        final Network net = buildCraftingNetwork(helper);
        final CraftingRig rig = kilnRig(helper, net);
        final StorageKey stone = storageKey(Items.STONE);
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    net.seed(Items.COBBLESTONE, 64);
                    /*
                     * Only a multi-stage recipe for stone. The recursive craft planner unwraps processing patterns
                     * but never multi-stage ones, so it is blind to this recipe, which is why the CLI and IQL,
                     * before they shared the terminal's entry point, could not craft it.
                     */
                    rig.hold(NetworkRecipe.ofMultiStage(new MultiStagePattern(List.of(
                            MultiStagePattern.Stage.proc(cobblePattern(200))))));
                })
                .thenExecuteAfter(SETTLE + 2, () -> {
                    helper.assertTrue(net.mainframe.submitNetworkCraft(stone, 1, true, "test") == null,
                            "the recursive planner is blind to a multi-stage-only recipe");
                    helper.assertTrue(net.mainframe.submitCraftRequest(stone, 1, true, "test (Shell)", null) != null,
                            "the shared craft entry point runs the multi-stage recipe");
                })
                .thenExecuteAfter(60, () -> helper.assertTrue(net.storage(helper).count(stone) > 0,
                        "the multi-stage recipe ran through the shared entry point; stone="
                                + net.storage(helper).count(stone)))
                .thenSucceed();
    }

    /** A recipe no interface holds has no machine to run on: the job times out and nothing leaves the network. */
    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void processing_recipeNoInterfaceHoldsTimesOutAndConservesInputs(final GameTestHelper helper) {
        final Network net = buildCraftingNetwork(helper);
        final AtomicReference<NetworkProcessingOperation> op = new AtomicReference<>();
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> net.seed(Items.COBBLESTONE, 16))
                .thenExecuteAfter(SETTLE + 2, () -> {
                    op.set(net.mainframe.submitNetworkProcessing(cobblePattern(5), 4, "test"));
                    helper.assertTrue(op.get() != null, "the operation is accepted");
                })
                .thenExecuteAfter(20, () -> {
                    helper.assertTrue(op.get().isDone(), "the job timed out instead of hanging");
                    helper.assertTrue(op.get().toRecord().status() == OperationRecord.STATUS_FAILED,
                            "it settled FAILED");
                    helper.assertTrue(op.get().toRecord().cause().key().equals(NO_INTERFACE),
                            "its cause says no interface holds the recipe; got " + op.get().toRecord().cause());
                    helper.assertTrue(net.storage(helper).count(storageKey(Items.COBBLESTONE)) == 16,
                            "no machine ran, so the inputs are all still in the network");
                })
                .thenSucceed();
    }

    /** A paused interface takes no job: the job waits, without a timeout, and its machine is fed nothing. */
    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void processing_pausedInterfaceIsNotFed(final GameTestHelper helper) {
        final Network net = buildCraftingNetwork(helper);
        final CraftingRig rig = kilnRig(helper, net);
        final AtomicReference<NetworkProcessingOperation> op = new AtomicReference<>();
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    net.seed(Items.COBBLESTONE, 64);
                    rig.hold(cobblePattern(10));
                    rig.part().setPaused(true, "");
                })
                .thenExecuteAfter(SETTLE + 2, () -> op.set(net.mainframe.submitNetworkProcessing(cobblePattern(10),
                        4, "test")))
                .thenExecuteAfter(30, () -> {
                    helper.assertTrue(kiln(rig).input(0).isEmpty(), "a paused interface feeds nothing");
                    helper.assertFalse(op.get().isDone(), "the job outlives its timeout while it waits");
                    helper.assertTrue(op.get().isWaiting(), "the job waits for the interface");
                    rig.part().setPaused(false, "");
                })
                .thenExecuteAfter(40, () -> {
                    helper.assertTrue(op.get().isDone() && op.get().toRecord().status()
                            == OperationRecord.STATUS_COMPLETED, "once resumed, the job runs and completes");
                    helper.assertTrue(net.storage(helper).count(storageKey(Items.STONE)) == 4, "four stone made");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void processing_reachesCompletedStatus(final GameTestHelper helper) {
        final Network net = buildCraftingNetwork(helper);
        final CraftingRig rig = kilnRig(helper, net);
        final AtomicReference<NetworkProcessingOperation> op = new AtomicReference<>();
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    net.seed(Items.COBBLESTONE, 64);
                    rig.hold(cobblePattern(200));
                })
                .thenExecuteAfter(SETTLE + 2, () -> {
                    op.set(net.mainframe.submitNetworkProcessing(cobblePattern(200), 1, "test"));
                    final byte live = op.get().liveRecord().status();
                    helper.assertTrue(live == OperationRecord.STATUS_WAITING
                                    || live == OperationRecord.STATUS_PROCESSING,
                            "the live record is in flight before completion; got " + live);
                })
                .thenExecuteAfter(30, () -> {
                    helper.assertTrue(op.get().isDone(), "the job finished");
                    helper.assertTrue(op.get().toRecord().status() == OperationRecord.STATUS_COMPLETED,
                            "its status is COMPLETED after the output it asked for came back");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void network_aggregatesMachineRecipes(final GameTestHelper helper) {
        final Network net = buildCraftingNetwork(helper);
        final CraftingRig rig = kilnRig(helper, net);
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    rig.hold(CraftingRig.pattern(Items.RAW_IRON, Items.IRON_INGOT, 200));
                    rig.hold(CraftingRig.pattern(Items.RAW_COPPER, Items.COPPER_INGOT, 200));
                })
                .thenExecuteAfter(2, () -> helper.assertTrue(net.mainframe.networkMachineRecipes().size() == 2,
                        "the Mainframe lists the recipes its computer's interfaces hold; got "
                                + net.mainframe.networkMachineRecipes().size()))
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void processing_rejectsNonPositiveQuantity(final GameTestHelper helper) {
        final Network net = buildCraftingNetwork(helper);
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    helper.assertTrue(net.mainframe.submitNetworkProcessing(cobblePattern(200), 0, "t") == null,
                            "quantity 0 is rejected");
                    helper.assertTrue(net.mainframe.submitNetworkProcessing(cobblePattern(200), -10, "t") == null,
                            "a negative quantity is rejected");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void processing_maxJobsCapsConcurrentJobsOnAnInterface(final GameTestHelper helper) {
        final Network net = buildCraftingNetwork(helper);
        final CraftingRig rig = kilnRig(helper, net);
        final AtomicReference<NetworkProcessingOperation> op1 = new AtomicReference<>();
        final AtomicReference<NetworkProcessingOperation> op2 = new AtomicReference<>();
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    net.seed(Items.COBBLESTONE, 256);
                    rig.hold(cobblePattern(200));
                    rig.part().setMaxJobs(1, "");
                })
                .thenExecuteAfter(SETTLE + 2, () -> {
                    op1.set(net.mainframe.submitNetworkProcessing(cobblePattern(200), 999, "a"));
                    op2.set(net.mainframe.submitNetworkProcessing(cobblePattern(200), 999, "b"));
                })
                .thenExecuteAfter(10, () -> {
                    int active = 0;
                    if (!op1.get().isWaiting() && !op1.get().isDone()) {
                        active++;
                    }
                    if (!op2.get().isWaiting() && !op2.get().isDone()) {
                        active++;
                    }
                    helper.assertTrue(active == 1, "most jobs 1 keeps exactly one job on the interface; active="
                            + active);
                    helper.assertTrue(op1.get().isWaiting() || op2.get().isWaiting(),
                            "the job over the cap is WAITING");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void processing_malformedPatternSettlesFailed(final GameTestHelper helper) {
        final Network net = buildCraftingNetwork(helper);
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    final StorageKey cobble = storageKey(Items.COBBLESTONE);
                    final StorageKey stone = storageKey(Items.STONE);
                    // No outputs => no result key => the job settles FAILED at construction, never hangs.
                    final ProcessingPattern noOut = new ProcessingPattern(
                            List.of(new ProcessingPattern.ProcessingInput(cobble, 1L)), List.of(), 200);
                    final NetworkProcessingOperation op = net.mainframe.submitNetworkProcessing(noOut, 1, "test");
                    helper.assertTrue(op != null && op.isDone(), "a pattern with no outputs settles immediately");
                    helper.assertTrue(op.toRecord().status() == OperationRecord.STATUS_FAILED,
                            "and its status is FAILED");
                    final ProcessingPattern noIn = new ProcessingPattern(List.of(),
                            List.of(new ProcessingPattern.ProcessingOutput(stone, 1L, 100)), 200);
                    final NetworkProcessingOperation op2 = net.mainframe.submitNetworkProcessing(noIn, 1, "test");
                    helper.assertTrue(op2 != null && op2.isDone()
                            && op2.toRecord().status() == OperationRecord.STATUS_FAILED,
                            "a pattern with no inputs is FAILED too");
                })
                .thenSucceed();
    }

    /** Cobblestone becomes stone one for one: whatever the job does, the two together never change. */
    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void processing_conservesItemsAcrossTheCraft(final GameTestHelper helper) {
        final Network net = buildCraftingNetwork(helper);
        final CraftingRig rig = kilnRig(helper, net);
        final long[] before = new long[1];
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    net.seed(Items.COBBLESTONE, 200);
                    // Stone the kiln already held belongs to the machine and stays there.
                    kiln(rig).preload(new ItemStack(Items.STONE, 16));
                    rig.hold(cobblePattern(200));
                    before[0] = both(helper, net, rig);
                })
                .thenExecuteAfter(SETTLE + 2, () -> net.mainframe.submitNetworkProcessing(cobblePattern(200), 32,
                        "conserve"))
                .thenExecuteAfter(200, () -> {
                    helper.assertTrue(both(helper, net, rig) == before[0],
                            "cobblestone and stone conserved: " + before[0] + " -> " + both(helper, net, rig));
                    helper.assertTrue(net.storage(helper).count(storageKey(Items.STONE)) == 32,
                            "the 32 stone made reached the network; got "
                                    + net.storage(helper).count(storageKey(Items.STONE)));
                    helper.assertTrue(kiln(rig).output().getCount() == 16,
                            "the 16 stone the kiln held before the job are still in it");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void networkStorage_conservesFluidsThroughInsertAndSelect(final GameTestHelper helper) {
        final Network net = buildCraftingNetwork(helper);
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    final NetworkStorage storage = net.storage(helper);
                    final StorageKey water = StorageKey.of(new FluidStack(Fluids.WATER, 1));
                    final long inserted = storage.insert(water, 8000L);
                    helper.assertTrue(inserted > 0, "the network accepts fluid into its mB-eq capacity");
                    helper.assertTrue(storage.count(water) == inserted, "the inserted fluid is counted exactly");
                    final FluidTank tank = new FluidTank(1_000_000);
                    final long moved = storage.select(water, inserted, new ExternalDataPort(null, tank));
                    helper.assertTrue(moved == inserted, "all the fluid moves out of the network");
                    helper.assertTrue(storage.count(water) == 0, "the network fluid is fully drained");
                    helper.assertTrue(tank.getFluidAmount() == inserted,
                            "the sink holds exactly what left, fluid conserved (" + inserted + " mB)");
                })
                .thenSucceed();
    }

    /** A machine that never answers times the job out after it fed it: what it fed is still in the machine. */
    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void processing_conservesItemsOnTimeout(final GameTestHelper helper) {
        final Network net = buildCraftingNetwork(helper);
        final CraftingRig rig = kilnRig(helper, net);
        final AtomicReference<NetworkProcessingOperation> op = new AtomicReference<>();
        final long[] before = new long[1];
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    net.seed(Items.COBBLESTONE, 128);
                    kiln(rig).setHeld(true);
                    rig.hold(cobblePattern(6));
                    before[0] = rig.total(net.storage(helper), Items.COBBLESTONE);
                })
                .thenExecuteAfter(SETTLE + 2, () -> op.set(net.mainframe.submitNetworkProcessing(cobblePattern(6),
                        64, "timeout")))
                .thenExecuteAfter(40, () -> {
                    helper.assertTrue(op.get().isDone() && op.get().toRecord().status()
                            == OperationRecord.STATUS_FAILED, "the job timed out with nothing made");
                    final long after = rig.total(net.storage(helper), Items.COBBLESTONE);
                    helper.assertTrue(after == before[0], "no cobblestone lost on timeout: " + before[0] + " -> "
                            + after);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void processing_manyConcurrentJobsConserveAndDontCrash(final GameTestHelper helper) {
        final Network net = buildCraftingNetwork(helper);
        final CraftingRig rig = kilnRig(helper, net);
        final long[] before = new long[1];
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    net.seed(Items.COBBLESTONE, 1000);
                    rig.hold(cobblePattern(200));
                    before[0] = both(helper, net, rig);
                })
                .thenExecuteAfter(SETTLE + 2, () -> {
                    // Fifty jobs at the one interface at once: the queue must hold, and nothing is made or lost.
                    for (int i = 0; i < 50; i++) {
                        net.mainframe.submitNetworkProcessing(cobblePattern(200), 4, "op" + i);
                    }
                })
                .thenExecuteAfter(120, () -> helper.assertTrue(both(helper, net, rig) == before[0],
                        "cobblestone and stone conserved under 50 jobs: " + before[0] + " -> "
                                + both(helper, net, rig)))
                .thenSucceed();
    }

    /** A job given up on mid-run still collects what its machine goes on making, and nothing is lost. */
    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void processing_conservesItemsOnAbandon(final GameTestHelper helper) {
        final Network net = buildCraftingNetwork(helper);
        final CraftingRig rig = kilnRig(helper, net);
        final AtomicReference<NetworkProcessingOperation> op = new AtomicReference<>();
        final long[] before = new long[1];
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    net.seed(Items.COBBLESTONE, 128);
                    rig.hold(cobblePattern(200));
                    before[0] = both(helper, net, rig);
                })
                .thenExecuteAfter(SETTLE + 2, () -> op.set(net.mainframe.submitNetworkProcessing(
                        cobblePattern(200), 16, "abandon")))
                .thenExecuteAfter(8, () -> op.get().abandon())
                .thenExecuteAfter(100, () -> {
                    helper.assertTrue(op.get().isDone(), "an abandoned job settles");
                    helper.assertTrue(both(helper, net, rig) == before[0],
                            "nothing lost on abandon mid-run: " + before[0] + " -> " + both(helper, net, rig));
                    helper.assertTrue(kiln(rig).output().isEmpty(),
                            "what the kiln made after the job settled still went to the network");
                })
                .thenSucceed();
    }

    /**
     * A machine's output waits in the machine while the network has no room for it. It used to be pulled out
     * whole and stored as far as it fitted, and whatever a full network refused was simply gone.
     */
    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void processing_leavesTheOutputInTheMachineWhenTheNetworkIsFull(final GameTestHelper helper) {
        final Network net = buildCraftingNetwork(helper);
        final CraftingRig rig = kilnRig(helper, net);
        final long[] before = new long[1];
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    net.seed(Items.COBBLESTONE, 32);
                    rig.hold(cobblePattern(200));
                })
                .thenExecuteAfter(SETTLE + 2, () -> {
                    net.mainframe.submitNetworkProcessing(cobblePattern(200), 8, "full");
                    before[0] = both(helper, net, rig);
                })
                // The job has fed its eight lots; now every byte the drives have left goes to dirt.
                .thenExecuteAfter(6, () -> net.rack.getServerStorage(0).insert(Items.DIRT, Long.MAX_VALUE / 4))
                .thenExecuteAfter(60, () -> helper.assertTrue(both(helper, net, rig) == before[0],
                        "nothing the machine made is lost to a full network: " + before[0] + " -> "
                                + both(helper, net, rig)))
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void processing_conservesItemsThroughPowerCycle(final GameTestHelper helper) {
        final Network net = buildCraftingNetwork(helper);
        final CraftingRig rig = kilnRig(helper, net);
        final long[] before = new long[1];
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    net.seed(Items.COBBLESTONE, 128);
                    rig.hold(cobblePattern(200));
                    before[0] = both(helper, net, rig);
                })
                .thenExecuteAfter(SETTLE + 2, () -> net.mainframe.submitNetworkProcessing(cobblePattern(200), 8,
                        "power"))
                .thenExecuteAfter(6, net.mainframe::togglePower)
                .thenExecuteAfter(12, net.mainframe::togglePower)
                .thenExecuteAfter(60, () -> helper.assertTrue(both(helper, net, rig) == before[0],
                        "nothing lost through a power cycle mid-run: " + before[0] + " -> "
                                + both(helper, net, rig)))
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void processing_repeatedSubmitsAreIndependent(final GameTestHelper helper) {
        final Network net = buildCraftingNetwork(helper);
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    final var op1 = net.mainframe.submitNetworkProcessing(cobblePattern(200), 4, "a");
                    final var op2 = net.mainframe.submitNetworkProcessing(cobblePattern(200), 4, "b");
                    final var op3 = net.mainframe.submitNetworkProcessing(cobblePattern(200), 4, "c");
                    helper.assertTrue(op1 != null && op2 != null && op3 != null, "every submit is accepted");
                    helper.assertTrue(op1 != op2 && op2 != op3 && op1 != op3,
                            "repeated identical submits create independent operations, no aliasing");
                    helper.assertTrue(!op1.operationId().equals(op2.operationId())
                                    && !op2.operationId().equals(op3.operationId()),
                            "each operation gets a distinct id");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void mainframe_operationsStateSurvivesReload(final GameTestHelper helper) {
        final Network net = buildCraftingNetwork(helper);
        final CraftingRig rig = kilnRig(helper, net);
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    net.seed(Items.COBBLESTONE, 64);
                    rig.hold(cobblePattern(200));
                })
                .thenExecuteAfter(SETTLE + 2, () -> net.mainframe.submitNetworkProcessing(cobblePattern(200), 1,
                        "reload"))
                .thenExecuteAfter(30, () -> {
                    final long completedBefore = net.mainframe.completedOps();
                    final int logBefore = net.mainframe.recentOperations().size();
                    helper.assertTrue(completedBefore >= 1, "the job completed before the reload");
                    final var reg = helper.getLevel().registryAccess();
                    net.mainframe.loadWithComponents(net.mainframe.saveWithFullMetadata(reg), reg);
                    helper.assertTrue(net.mainframe.completedOps() == completedBefore,
                            "the completed-ops total survives a reload: " + completedBefore + " -> "
                                    + net.mainframe.completedOps());
                    helper.assertTrue(net.mainframe.recentOperations().size() == logBefore,
                            "the operations log survives a reload (" + logBefore + " entries)");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void networkStorage_conservesLargeItemLoadAtScale(final GameTestHelper helper) {
        final Network net = buildCraftingNetwork(helper);
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    net.seed(Items.IRON_INGOT, 30_000);
                    final NetworkStorage storage = net.storage(helper);
                    final StorageKey iron = storageKey(Items.IRON_INGOT);
                    final long total = storage.count(iron);
                    helper.assertTrue(total > 0, "the network holds a large item load (" + total + ")");
                    final ItemStackHandler sink = new ItemStackHandler(1024);
                    final long moved = storage.select(iron, total, new ExternalDataPort(sink, null));
                    helper.assertTrue(moved == total, "the whole load moves out");
                    helper.assertTrue(storage.count(iron) == 0, "the network is fully drained, nothing stuck");
                    long inSink = 0;
                    for (int s = 0; s < sink.getSlots(); s++) {
                        if (sink.getStackInSlot(s).is(Items.IRON_INGOT)) {
                            inSink += sink.getStackInSlot(s).getCount();
                        }
                    }
                    helper.assertTrue(inSink == total,
                            "the sink holds exactly the load, items conserved at scale (" + total + ")");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void networkStorage_conservesLargeFluidLoadAtScale(final GameTestHelper helper) {
        final Network net = buildCraftingNetwork(helper);
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    final NetworkStorage storage = net.storage(helper);
                    final StorageKey water = StorageKey.of(new FluidStack(Fluids.WATER, 1));
                    final long inserted = storage.insert(water, 500_000L);
                    helper.assertTrue(inserted > 0, "the network accepts a large fluid load (" + inserted + " mB)");
                    helper.assertTrue(storage.count(water) == inserted, "the whole load is counted exactly");
                    final FluidTank tank = new FluidTank(4_000_000);
                    final long moved = storage.select(water, inserted, new ExternalDataPort(null, tank));
                    helper.assertTrue(moved == inserted, "the whole large load moves out in one go");
                    helper.assertTrue(storage.count(water) == 0, "the network is fully drained, nothing stuck");
                    helper.assertTrue(tank.getFluidAmount() == inserted,
                            "the sink holds exactly the load, conserved at scale (" + inserted + " mB)");
                })
                .thenSucceed();
    }

    /**
     * A machine taken away mid-run leaves its job waiting, not failed: the interface still holds the recipe. Put back,
     * the machine is the job's again, and the job settles on what it could make.
     */
    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void processing_waitsWhileItsMachineIsGoneAndSettlesOnceItIsBack(final GameTestHelper helper) {
        final Network net = buildCraftingNetwork(helper);
        final CraftingRig rig = kilnRig(helper, net);
        final AtomicReference<NetworkProcessingOperation> op = new AtomicReference<>();
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    net.seed(Items.COBBLESTONE, 64);
                    rig.hold(cobblePattern(20));
                })
                .thenExecuteAfter(SETTLE + 2, () -> op.set(net.mainframe.submitNetworkProcessing(cobblePattern(20),
                        8, "test")))
                .thenExecuteAfter(4, () -> {
                    helper.setBlock(rig.machinePos(), Blocks.AIR);
                    // The computer reads its crafting network again at once, as it would within a second.
                    net.cc.forgetFloor();
                })
                .thenExecuteAfter(40, () -> {
                    helper.assertFalse(op.get().isDone(), "the job outlives its timeout while the machine is gone");
                    helper.assertTrue(op.get().isWaiting(), "the job waits for its machine");
                    helper.setBlock(rig.machinePos(), TestMachines.KILN.get());
                    net.cc.forgetFloor();
                })
                .thenExecuteAfter(80, () -> helper.assertTrue(op.get().isDone(),
                        "once the machine is back the job settles instead of hanging"))
                .thenSucceed();
    }

    /** A machine fed through a router on the interface's own cable, its output collected on the far side. */
    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void processing_routedMachineIsFedThroughItsRouter(final GameTestHelper helper) {
        final Network net = buildCraftingNetwork(helper);
        final CraftingRig rig = CraftingRig.routed(TestWorldBuilder.forGameTest(helper), net.cc,
                TestMachines.KILN.get(), false);
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    net.seed(Items.RAW_COPPER, 32);
                    rig.hold(CraftingRig.pattern(Items.RAW_COPPER, Items.COPPER_INGOT, 200));
                })
                .thenExecuteAfter(SETTLE + 2, () -> helper.assertTrue(net.mainframe.submitNetworkProcessing(
                        CraftingRig.pattern(Items.RAW_COPPER, Items.COPPER_INGOT, 200), 8, "routed") != null,
                        "the routed job is accepted"))
                .thenExecuteAfter(80, () -> {
                    helper.assertTrue(kiln(rig).made() == 8, "the router fed the kiln eight lots; made "
                            + kiln(rig).made());
                    final long stored = net.storage(helper).count(storageKey(Items.COPPER_INGOT));
                    helper.assertTrue(stored == 8, "the ingots reached the network through the bus; got " + stored);
                })
                .thenSucceed();
    }

    /** A machine with sided inputs: each input goes in through the router its filter names, on the face it needs. */
    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void processing_sidedMachineTakesEachInputThroughItsRouter(final GameTestHelper helper) {
        final Network net = buildCraftingNetwork(helper);
        final CraftingRig rig = CraftingRig.routed(TestWorldBuilder.forGameTest(helper), net.cc,
                TestMachines.MIXER.get(), true);
        final ProcessingPattern mix = CraftingRig.mix(Items.DIRT, Items.GRAVEL, Items.COARSE_DIRT, 2);
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    net.seed(Items.DIRT, 16);
                    net.seed(Items.GRAVEL, 16);
                    rig.router(0).getFilterHandler().setStackInSlot(0, new ItemStack(Items.DIRT));
                    rig.router(1).getFilterHandler().setStackInSlot(0, new ItemStack(Items.GRAVEL));
                    rig.hold(mix);
                })
                .thenExecuteAfter(SETTLE + 2, () -> helper.assertTrue(
                        net.mainframe.submitNetworkProcessing(mix, 8, "sided") != null, "the sided job is accepted"))
                .thenExecuteAfter(80, () -> {
                    final long coarse = net.storage(helper).count(storageKey(Items.COARSE_DIRT));
                    helper.assertTrue(coarse == 8, "four mixes made eight coarse dirt; got " + coarse);
                    helper.assertTrue(net.storage(helper).count(storageKey(Items.DIRT)) == 12
                                    && net.storage(helper).count(storageKey(Items.GRAVEL)) == 12,
                            "exactly four of each input left the network");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void multiStage_mixedPipelineRunsProcThenBench(final GameTestHelper helper) {
        /*
         * A two-stage pipeline mixing both stage kinds: a processing stage (cobblestone to stone in the kiln)
         * followed by a bench stage (stone to a stone button). The processing output flows through network storage
         * into the bench stage, and the whole pipeline settles COMPLETED.
         */
        final Network net = buildCraftingNetwork(helper);
        final CraftingRig rig = kilnRig(helper, net);
        final AtomicReference<NetworkMultiStageOperation> opHolder = new AtomicReference<>();
        final MultiStagePattern multi = new MultiStagePattern(List.of(
                MultiStagePattern.Stage.proc(cobblePattern(200)),
                MultiStagePattern.Stage.bench(stoneButtonPattern())));
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    net.seed(Items.COBBLESTONE, 64);
                    // The bench pattern is not in the ROM: a pipeline's bench stage runs from its own pattern.
                    rig.hold(NetworkRecipe.ofMultiStage(multi));
                })
                .thenExecuteAfter(SETTLE + 2, () -> {
                    opHolder.set(net.mainframe.submitNetworkMultiStage(multi, 1, "test"));
                    helper.assertTrue(opHolder.get() != null, "the mixed multi-stage operation is accepted");
                })
                .thenExecuteAfter(100, () -> {
                    helper.assertTrue(opHolder.get().isDone(), "the mixed pipeline settles");
                    helper.assertTrue(opHolder.get().toRecord().status() == OperationRecord.STATUS_COMPLETED,
                            "the mixed pipeline settles COMPLETED; status=" + opHolder.get().toRecord().status());
                    final long buttons = net.storage(helper).count(storageKey(Items.STONE_BUTTON));
                    helper.assertTrue(buttons >= 1,
                            "the bench stage crafts the button from the processing stage's output; got " + buttons);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void multiStage_failedStageFailsThePipelineAndConserves(final GameTestHelper helper) {
        /*
         * Stage 1 is held by no interface, so it times out FAILED. The pipeline fails with it, stage 2 never runs,
         * and the network's inputs are conserved.
         */
        final Network net = buildCraftingNetwork(helper);
        final AtomicReference<NetworkMultiStageOperation> opHolder = new AtomicReference<>();
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> net.seed(Items.COBBLESTONE, 16))
                .thenExecuteAfter(SETTLE + 2, () -> {
                    final MultiStagePattern multi = new MultiStagePattern(List.of(
                            MultiStagePattern.Stage.proc(cobblePattern(5)),
                            MultiStagePattern.Stage.bench(stoneButtonPattern())));
                    opHolder.set(net.mainframe.submitNetworkMultiStage(multi, 1, "test"));
                    helper.assertTrue(opHolder.get() != null, "the operation is accepted");
                })
                .thenExecuteAfter(30, () -> {
                    helper.assertTrue(opHolder.get().isDone(), "the pipeline settles instead of hanging");
                    helper.assertTrue(opHolder.get().toRecord().status() == OperationRecord.STATUS_FAILED,
                            "a failed stage fails the whole pipeline; status=" + opHolder.get().toRecord().status());
                    helper.assertTrue(net.storage(helper).count(storageKey(Items.COBBLESTONE)) == 16,
                            "no machine ran, so the pipeline's inputs are conserved");
                    helper.assertTrue(net.storage(helper).count(storageKey(Items.STONE_BUTTON)) == 0,
                            "the bench stage after the failed stage never runs");
                })
                .thenSucceed();
    }

    /** One feed moves as many lots as the machine takes and the job needs, not one lot every few ticks. */
    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void processing_feedsManyLotsPerCycle(final GameTestHelper helper) {
        final Network net = buildCraftingNetwork(helper);
        final CraftingRig rig = kilnRig(helper, net);
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    net.seed(Items.COBBLESTONE, 64);
                    kiln(rig).setHeld(true);
                    rig.hold(cobblePattern(200));
                })
                .thenExecuteAfter(SETTLE + 2, () -> helper.assertTrue(
                        net.mainframe.submitNetworkProcessing(cobblePattern(200), 64, "fill") != null,
                        "the processing operation is accepted"))
                .thenExecuteAfter(8, () -> {
                    final long delivered = kiln(rig).input(0).getCount();
                    // One lot every four ticks could never pass three items in eight ticks.
                    helper.assertTrue(delivered >= 16, "one feed delivers many lots; delivered=" + delivered);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void journey_encodeLoadAndCraftAcrossTheFullChain(final GameTestHelper helper) {
        /*
         * The player's whole path, server-side: encode a recipe onto a disc at the Pattern Encoder, carry the disc
         * to a drive linked to the Crafting Computer, read and load it into a Crafting Card's ROM (the exact steps
         * the Crafting Manager's Load button runs), then request the craft through the same entry point the
         * terminal's request popup uses, and watch the result land in network storage.
         */
        final Network net = buildCraftingNetwork(helper);
        final BlockPos encoderPos = new BlockPos(5, 2, 1);
        final BlockPos drivePos = new BlockPos(5, 2, 3);
        helper.setBlock(encoderPos, ComputingModule.PATTERN_ENCODER.get());
        helper.setBlock(drivePos, ComputingModule.DVD_DRIVE.get());
        if (!(helper.getBlockEntity(encoderPos) instanceof PatternEncoderBlockEntity encoder)) {
            throw new IllegalStateException("no pattern encoder at " + encoderPos);
        }
        if (!(helper.getBlockEntity(drivePos) instanceof MediaReaderBlockEntity drive)) {
            throw new IllegalStateException("no media reader at " + drivePos);
        }
        encoder.media().setStackInSlot(0, new ItemStack(ComputingModule.DVD_RW.get()));

        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    // 1) Author the pattern on the computer's workbench and send it to the linked encoder.
                    helper.assertTrue(encoder.ownerPos() != null && encoder.ownerPos().equals(net.cc.getBlockPos()),
                            "the encoder links to the adjacent Crafting Computer; got " + encoder.ownerPos());
                    final var studio = net.cc.studio();
                    studio.setGhost(0, new ItemStack(Items.OAK_LOG));
                    studio.refreshPreview(helper.getLevel());
                    final var content = studio.serialize(PatternWorkbench.Kind.BENCH,
                            helper.getLevel().registryAccess());
                    helper.assertTrue(content.isPresent() && encoder.queueBurn("oak_planks", content.get()),
                            "the bench draft is sent to the encoder");
                    final var player = helper.makeMockPlayer(GameType.CREATIVE);
                    final BlockPos absolute = helper.absolutePos(encoderPos);
                    player.setPos(absolute.getX() + 0.5, absolute.getY(), absolute.getZ() + 0.5);
                    final var encoderMenu = new PatternEncoderMenu(1, player.getInventory(), encoder);
                    helper.assertTrue(encoderMenu.stillValid(player), "the Pattern Encoder menu stays open");
                })
                .thenExecuteAfter(120, () -> {
                    helper.assertTrue(encoder.completed() == 1, "the encoder burned the file; completed="
                            + encoder.completed());
                    helper.assertFalse(encoder.locked(), "the bay is free once the job is over");
                    // 2) Carry the disc over: out of the encoder, into the drive next to the computer.
                    final ItemStack disc = encoder.ejectMedia();
                    helper.assertFalse(disc.isEmpty(), "the disc comes out of the encoder");
                    drive.mediaSlot().setStackInSlot(0, disc);
                })
                .thenExecuteAfter(SETTLE + 2, () -> {
                    // 3) The drive links to the adjacent Crafting Computer, which lists the medium as a volume.
                    helper.assertTrue(drive.ownerPos() != null
                                    && drive.ownerPos().equals(helper.absolutePos(new BlockPos(5, 2, 2))),
                            "the DVD drive links to the Crafting Computer; got " + drive.ownerPos());
                    helper.assertTrue(net.cc.linkedEndpoints().contains(helper.absolutePos(drivePos).asLong()),
                            "the Crafting Computer lists the drive as a linked endpoint");
                    // 4) Load the .craft from the linked medium into a card's ROM (the Load button's steps).
                    final ItemStack media = drive.mediaSlot().getStackInSlot(0);
                    String craftPath = null;
                    for (final DiskFilesystem.FileEntry e
                            : DiskFilesystem.list(media, "", FilesystemKind.HIERARCHICAL)) {
                        if (e.type() == FileType.CRAFT) {
                            craftPath = e.path();
                        }
                    }
                    helper.assertTrue(craftPath != null, "the carried disc still holds the .craft file");
                    final var content = DiskFilesystem.read(media, craftPath);
                    final var parsed = CraftFile.parse(content.orElse(""), helper.getLevel().registryAccess());
                    helper.assertTrue(parsed.isPresent(), "the .craft parses back into a pattern");
                    helper.assertTrue(net.cc.loadPattern(parsed.get()), "the pattern loads into the card's ROM");
                    final var player = helper.makeMockPlayer(GameType.CREATIVE);
                    final BlockPos absolute = helper.absolutePos(new BlockPos(5, 2, 2));
                    player.setPos(absolute.getX() + 0.5, absolute.getY(), absolute.getZ() + 0.5);
                    final var ccMenu = new CraftingComputerMenu(1, player.getInventory(), net.cc);
                    helper.assertTrue(ccMenu.stillValid(player), "the Crafting Computer menu stays open");
                    /*
                     * Stock the ingredients now: the craft planner reads the incremental network index,
                     * which needs a tick to absorb a direct store write before the request is planned.
                     */
                    net.seed(Items.OAK_LOG, 8);
                })
                .thenExecuteAfter(SETTLE, () -> helper.assertTrue(net.mainframe.submitNetworkCraft(
                                storageKey(Items.OAK_PLANKS), 4, true, "test (Interactor)") != null,
                        "the craft request is accepted"))
                .thenExecuteAfter(20, () -> {
                    final long planks = net.storage(helper).count(storageKey(Items.OAK_PLANKS));
                    helper.assertTrue(planks >= 4, "the crafted planks land in network storage; got " + planks);
                })
                .thenSucceed();
    }

    // Fixtures

    /** The assembled test network, with handles on the parts the assertions need. */
    private record Network(MainframeBlockEntity mainframe, ServerRackBlockEntity rack, CraftingComputerBlockEntity cc) {

        void seed(final Item item, final int count) {
            rack.getServerStorage(0).insert(item, count);
        }

        NetworkStorage storage(final GameTestHelper helper) {
            return NetworkStorage.of(helper.getLevel(), mainframe.networkUuid());
        }
    }

    private static Network buildCraftingNetwork(final GameTestHelper helper) {
        final TestWorldBuilder.CraftingNetwork net = TestWorldBuilder.forGameTest(helper).buildCraftingNetwork();
        return new Network(net.mainframe(), net.rack(), net.cc());
    }

    /* A test kiln fed by an interface against it, beside the network's Crafting Computer. */
    private static CraftingRig kilnRig(final GameTestHelper helper, final Network net) {
        return CraftingRig.direct(TestWorldBuilder.forGameTest(helper), net.cc, TestMachines.KILN.get());
    }

    private static TestMachineBlockEntity kiln(final CraftingRig rig) {
        final TestMachineBlockEntity kiln = rig.machine();
        if (kiln == null) {
            throw new IllegalStateException("no test kiln at " + rig.machinePos());
        }
        return kiln;
    }

    /* Cobblestone and stone together, in the network and in the kiln. */
    private static long both(final GameTestHelper helper, final Network net, final CraftingRig rig) {
        final NetworkStorage storage = net.storage(helper);
        return rig.total(storage, Items.COBBLESTONE) + rig.total(storage, Items.STONE);
    }

    private static ProcessingPattern cobblePattern(final int timeout) {
        return CraftingRig.pattern(Items.COBBLESTONE, Items.STONE, timeout);
    }

    private static ProcessingPattern smelt() {
        return CraftingRig.pattern(Items.RAW_IRON, Items.IRON_INGOT, 200);
    }

    private static StorageKey storageKey(final Item item) {
        return StorageKey.of(item);
    }

    /* A Crafting Computer with nothing in it but a Crafting Card, enough for its ROM. */
    private static CraftingComputerBlockEntity placeComputerWithCard(final GameTestHelper helper,
                                                                     final BlockPos pos) {
        helper.setBlock(pos, ComputingModule.CRAFTING_COMPUTER.get());
        if (!(helper.getBlockEntity(pos) instanceof CraftingComputerBlockEntity computer)) {
            throw new IllegalStateException("no crafting computer at " + pos);
        }
        computer.getHardware().setStackInSlot(CraftingComputerBlockEntity.PCIE_SLOTS_START,
                new ItemStack(ComputingModule.CRAFTING_CARD_T2.get()));
        return computer;
    }

    private static CraftingComputerBlockEntity placeSecondCraftingComputer(final GameTestHelper helper,
                                                                           final BlockPos pos) {
        helper.setBlock(pos, ComputingModule.CRAFTING_COMPUTER.get());
        TestWorldBuilder.forGameTest(helper).faceRearTowardCable(pos);
        if (!(helper.getBlockEntity(pos) instanceof CraftingComputerBlockEntity ccBe)) {
            throw new IllegalStateException("no second crafting computer");
        }
        final var hw = ccBe.getHardware();
        hw.setStackInSlot(CraftingComputerBlockEntity.MOTHERBOARD_SLOT,
                new ItemStack(HardwareItems.MOTHERBOARD_ATX_STANDARD_LGA1150.get()));
        hw.setStackInSlot(CraftingComputerBlockEntity.CPU_SLOT,
                new ItemStack(HardwareItems.CPU_INTEGRA_CENTRO_C7_4790K.get()));
        hw.setStackInSlot(CraftingComputerBlockEntity.RAM_SLOTS_START,
                new ItemStack(ComputingModule.RAM_DDR3_8192.get()));
        hw.setStackInSlot(CraftingComputerBlockEntity.PCIE_SLOTS_START,
                new ItemStack(ComputingModule.CRAFTING_CARD_T2.get()));
        hw.setStackInSlot(CraftingComputerBlockEntity.PSU_SLOT, new ItemStack(ComputingModule.PSU_650G.get()));
        ccBe.togglePower();
        return ccBe;
    }

    /**
     * A cluster the way a player wires one: the HBW Interface, a run of high-compute cable east of it,
     * and one Supercomputer Rack hanging off each cable block, each rack seating one node. Racks are
     * leaves on the fabric, so they sit beside the cable run rather than in it.
     */
    private static void placeCluster(final GameTestHelper helper, final BlockPos hub, final int nodes) {
        helper.setBlock(hub, ComputingModule.HBW_INTERFACE.get());
        for (int i = 1; i <= nodes; i++) {
            final BlockPos cable = hub.east(i);
            TestCables.lay(helper, cable, ComputingModule.HPC_CABLE);
            // Above the cable, not beside it: the fixtures' computers and cables occupy the row in front.
            final BlockPos rackPos = cable.above();
            helper.setBlock(rackPos, ComputingModule.SUPERCOMPUTER_RACK.get());
            if (helper.getBlockEntity(rackPos) instanceof ServerRackBlockEntity rack) {
                rack.getServers().setStackInSlot(0, ServerStacks.defaultSupercomputerNode());
            }
        }
    }

    private static CraftingPattern planksPattern(final int count) {
        final List<ItemStack> grid = emptyGrid();
        grid.set(0, new ItemStack(Items.OAK_LOG));
        return new CraftingPattern(grid, new ItemStack(Items.OAK_PLANKS, count));
    }

    private static CraftingPattern sticksPattern() {
        final List<ItemStack> grid = emptyGrid();
        grid.set(0, new ItemStack(Items.OAK_PLANKS));
        grid.set(3, new ItemStack(Items.OAK_PLANKS));
        return new CraftingPattern(grid, new ItemStack(Items.STICK, 4));
    }

    private static CraftingPattern stoneButtonPattern() {
        final List<ItemStack> grid = emptyGrid();
        grid.set(0, new ItemStack(Items.STONE));
        return new CraftingPattern(grid, new ItemStack(Items.STONE_BUTTON));
    }

    private static CraftingPattern nuggetsToIngot() {
        final List<ItemStack> grid = new ArrayList<>();
        for (int i = 0; i < CraftingPattern.GRID_SIZE; i++) {
            grid.add(new ItemStack(Items.IRON_NUGGET));
        }
        return new CraftingPattern(grid, new ItemStack(Items.IRON_INGOT));
    }

    private static List<ItemStack> grid(final Item first) {
        final List<ItemStack> grid = emptyGrid();
        grid.set(0, new ItemStack(first));
        return grid;
    }

    private static List<ItemStack> emptyGrid() {
        final List<ItemStack> grid = new ArrayList<>(CraftingPattern.GRID_SIZE);
        for (int i = 0; i < CraftingPattern.GRID_SIZE; i++) {
            grid.add(ItemStack.EMPTY);
        }
        return grid;
    }
}
