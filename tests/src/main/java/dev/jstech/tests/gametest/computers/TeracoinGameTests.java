/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.computers;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.HardwareItems;
import dev.jstech.computers.block.MiningComputerTexts;
import dev.jstech.computers.blockentity.AsicBlockEntity;
import dev.jstech.computers.blockentity.MiningComputerBlockEntity;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.blockentity.Printer3DBlockEntity;
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.computers.blockentity.ServerServices;
import dev.jstech.computers.machine.FileService;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.program.Programs;
import dev.jstech.computers.teracoin.AutoPrints;
import dev.jstech.computers.teracoin.FoundBlock;
import dev.jstech.computers.teracoin.MiningPools;
import dev.jstech.computers.teracoin.TeracoinChain;
import dev.jstech.computers.teracoin.TeracoinSettings;
import dev.jstech.computers.teracoin.TeracoinState;
import dev.jstech.computers.teracoin.Trc;
import dev.jstech.computers.teracoin.WalletFiles;
import dev.jstech.core.time.GameCalendar;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.computers.TestWorldBuilder;
import java.util.Optional;
import java.util.random.RandomGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Teracoin in a world (computers/docs/design/teracoin.md): the chain saved with the world, a wallet that is a file on a
 * computer, a 3D printer paid item by item from that file, the wallet's Auto print, a Mining Computer's cards on its
 * risers, an ASIC that mines and a Mining Pool on a rack server. The chain's rules, the pools' shares and the
 * printers' motion are plain JUnit.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class TeracoinGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos COMPUTER = new BlockPos(4, 2, 2);
    private static final BlockPos PRINTER = COMPUTER.south();
    private static final BlockPos MACHINE = new BlockPos(2, 2, 2);
    private static final int SETTLE = 10;
    /* More than a Rattan takes to print an item and put it out. */
    private static final int ONE_ITEM = 400;
    /* What a Forge A2 takes from the line each tick, and more. */
    private static final int ENERGY_A_TICK = 1_000;
    private static final RandomGenerator RANDOM = RandomGenerator.getDefault();

    private TeracoinGameTests() {
    }

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void chain_keepsItsWalletsAndBalancesAcrossASave(final GameTestHelper helper) {
        final TeracoinState state = TeracoinState.of(helper.getLevel().getServer());
        helper.assertTrue(state == TeracoinState.of(helper.getLevel().getServer()),
                "there is one chain for the whole server");
        final TeracoinChain.Keys keys = state.chain().createWallet(RANDOM);
        credit(helper, keys.address(), Trc.of(50));
        final CompoundTag saved = state.save(new CompoundTag(), helper.getLevel().registryAccess());
        final TeracoinState read = TeracoinState.factory().deserializer().apply(saved,
                helper.getLevel().registryAccess());
        helper.assertTrue(read.chain().balance(keys.address()) == Trc.of(50),
                "the balance comes back from the save; got " + read.chain().balance(keys.address()));
        helper.assertTrue(read.chain().owns(keys.address(), keys.key()), "and the key still opens the address");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 1600)
    public static void miningPool_onAServerNobodyNamed_goesByTheServersNetworkName(final GameTestHelper helper) {
        final ServerRackBlockEntity rack = TestWorldBuilder.forGameTest(helper).buildCraftingNetwork().rack();
        final IOsHost server = rack.unitHost(0);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> helper.assertTrue(server.installOs(
                        ResourceLocation.fromNamespaceAndPath("jsc", "debian")), "Debian installs on the server"))
                .thenWaitUntil(() -> helper.assertTrue(server.isRunning() && !server.booting() && !server.needsPost(),
                        "the server comes up"))
                .thenExecute(() -> rack.consoleOf(0).install(Programs.MINING_POOL.toString()))
                .thenWaitUntil(() -> helper.assertTrue(!MiningPools.on(helper.getLevel(), rack.networkUuid())
                        .isEmpty(), "the network finds the pool on its server"))
                .thenExecute(() -> {
                    final ServerServices.Host pool = MiningPools.on(helper.getLevel(), rack.networkUuid()).getFirst();
                    helper.assertTrue(pool.name().isEmpty(), "nobody named the server");
                    helper.assertTrue(MiningPools.server(pool).equals(pool.hostname())
                                    && MiningPools.name(pool).equals(pool.hostname()) && !pool.hostname().isEmpty(),
                            "the pool and its server go by the server's network name; got "
                                    + MiningPools.name(pool) + " on " + MiningPools.server(pool));
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void wallet_isAFileOnTheComputerThatKeepsItsAddress(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = TestWorldBuilder.forGameTest(helper).placeRunningPersonalComputer(
                COMPUTER);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final TeracoinChain.Keys keys = TeracoinState.of(helper.getLevel().getServer()).chain()
                            .createWallet(RANDOM);
                    final FileService files = pc.services().files();
                    final String path = WalletFiles.write(files, WalletFiles.folder(pc), "main", keys, (char) 0);
                    helper.assertTrue(path != null, "the wallet's file is written in the wallets folder");
                    final Optional<WalletFiles.Wallet> wallet = WalletFiles.read(files, path);
                    helper.assertTrue(wallet.isPresent() && wallet.get().address().equals(keys.address()),
                            "and read back, it holds the address; got " + wallet);
                    helper.assertTrue(WalletFiles.all(files, pc).stream()
                            .anyMatch(one -> one.path().equals(path)), "the computer lists it among its wallets");
                    helper.assertTrue(WalletFiles.write(files, WalletFiles.folder(pc), "main", keys, (char) 0) == null,
                            "a second wallet of the same name is refused");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 600)
    public static void printer3d_paysTheItemAsItStartsBurnsTheTrcAndPutsTheItemOut(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = TestWorldBuilder.forGameTest(helper).placeRunningPersonalComputer(
                COMPUTER);
        helper.setBlock(PRINTER, ComputingModule.RATTAN_LAB_X1.get());
        final Printer3DBlockEntity printer = (Printer3DBlockEntity) helper.getBlockEntity(PRINTER);
        final long price = TeracoinSettings.priceOf(Items.DIAMOND);
        final String[] wallet = new String[2];
        final long[] burned = new long[1];
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    helper.assertTrue(price > 0L, "a diamond is on the server's list");
                    final TeracoinChain chain = TeracoinState.of(helper.getLevel().getServer()).chain();
                    burned[0] = chain.burned();
                    wallet[0] = walletOn(helper, pc, "printing");
                    wallet[1] = WalletFiles.read(pc.services().files(), wallet[0]).orElseThrow().address();
                    credit(helper, wallet[1], price + Trc.of(3));
                    helper.assertTrue(printer.submit(Items.DIAMOND, 1, pc.getBlockPos(), wallet[0]) != null,
                            "the job goes into the queue");
                })
                .thenWaitUntil(() -> helper.assertTrue(printer.phase() == Printer3DBlockEntity.PRINTING,
                        "it starts printing; its phase is " + printer.phase()))
                .thenExecute(() -> {
                    final TeracoinChain chain = TeracoinState.of(helper.getLevel().getServer()).chain();
                    helper.assertTrue(chain.balance(wallet[1]) == Trc.of(3),
                            "the item is paid the moment it starts; left " + chain.balance(wallet[1]));
                    // The world has one chain, which the Auto print test beside this one burns into too.
                    helper.assertTrue(chain.burned() >= burned[0] + price, "and the TRC paid leave circulation");
                })
                .thenWaitUntil(() -> helper.assertTrue(printer.output().getStackInSlot(0).is(Items.DIAMOND),
                        "a diamond comes out"))
                .thenExecute(() -> helper.assertTrue(printer.jobs().isEmpty() && printer.printed() == 1,
                        "the queue is done, one item printed"))
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void printer3d_waitsForTrcAndPaysNothingShortOfThePrice(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = TestWorldBuilder.forGameTest(helper).placeRunningPersonalComputer(
                COMPUTER);
        helper.setBlock(PRINTER, ComputingModule.PROSA_I3_MK3.get());
        final Printer3DBlockEntity printer = (Printer3DBlockEntity) helper.getBlockEntity(PRINTER);
        final String[] address = new String[1];
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final String path = walletOn(helper, pc, "short");
                    address[0] = WalletFiles.read(pc.services().files(), path).orElseThrow().address();
                    credit(helper, address[0], Trc.of(1));
                    printer.submit(Items.DIAMOND, 2, pc.getBlockPos(), path);
                })
                .thenWaitUntil(() -> helper.assertTrue(printer.phase() == Printer3DBlockEntity.WAITING_TRC,
                        "short of the price it waits for TRC; its phase is " + printer.phase()))
                .thenExecute(() -> {
                    final long left = TeracoinState.of(helper.getLevel().getServer()).chain().balance(address[0]);
                    helper.assertTrue(left == Trc.of(1), "and nothing is taken; left " + left);
                    printer.cancelCurrent();
                    helper.assertTrue(printer.jobs().isEmpty(), "cancelling takes the job off the queue");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void autoPrint_queuesWhatTheBalancePaysPastWhatTheWalletKeeps(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = TestWorldBuilder.forGameTest(helper).placeRunningPersonalComputer(
                COMPUTER);
        helper.setBlock(PRINTER, ComputingModule.PROSA_I3_MK3.get());
        final Printer3DBlockEntity printer = (Printer3DBlockEntity) helper.getBlockEntity(PRINTER);
        final long price = TeracoinSettings.priceOf(Items.DIAMOND);
        final BlockPos printerAt = helper.absolutePos(PRINTER);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(pc.enabledEndpoints().contains(printerAt.asLong()),
                        "the 3D printer links to the computer"))
                .thenExecute(() -> {
                    final String path = walletOn(helper, pc, "auto");
                    final String address = WalletFiles.read(pc.services().files(), path).orElseThrow().address();
                    // It keeps 3 TRC and pays for two items and a little more: two are queued, no third.
                    credit(helper, address, Trc.of(3) + 2 * price + Trc.of(1));
                    helper.assertTrue(pc.console().autoPrints().set(new AutoPrints.Auto(path, "minecraft:diamond",
                            printerAt.asLong(), Trc.of(3), 0L, 0, 0)), "the wallet's Auto print is set");
                })
                .thenWaitUntil(() -> helper.assertTrue(!printer.jobs().isEmpty(),
                        "within a second the computer queues what the balance pays"))
                .thenExecute(() -> {
                    final Printer3DBlockEntity.Job job = printer.jobs().getFirst();
                    helper.assertTrue(job.item().equals("minecraft:diamond") && job.copies() == 2,
                            "two diamonds, what passes what it keeps; got " + job);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void miningComputer_seatsACardOnlyOnARiserOfItsEraOrNewer(final GameTestHelper helper) {
        helper.setBlock(MACHINE, ComputingModule.MINING_COMPUTER_TRANSITION.get());
        final MiningComputerBlockEntity rig = (MiningComputerBlockEntity) helper.getBlockEntity(MACHINE);
        rig.getHardware().setStackInSlot(MiningComputerBlockEntity.MOTHERBOARD_SLOT,
                new ItemStack(HardwareItems.MOTHERBOARD_MNG_TRANSITION.get()));
        rig.risers().setStackInSlot(0, new ItemStack(HardwareItems.MINING_RISER_TRANSITION.get()));
        final ItemStack transition = new ItemStack(HardwareItems.GPU_VERTEX_7600_GT.get());
        final ItemStack standard = new ItemStack(HardwareItems.GPU_VERTEX_GT_730.get());
        helper.assertTrue(rig.switchedOff(), "a rig just placed is off");
        helper.assertTrue(rig.seatRefusal(1, transition) == MiningComputerTexts.NO_RISER,
                "a card needs a riser in the slot");
        helper.assertTrue(rig.seatRefusal(0, standard) == MiningComputerTexts.TOO_NEW_FOR_RISER,
                "a Transition riser does not carry a Standard card");
        helper.assertTrue(rig.seatRefusal(0, new ItemStack(Items.DIAMOND)) == MiningComputerTexts.ONLY_GRAPHICS_CARDS,
                "only a graphics card stands on a riser");
        helper.assertTrue(rig.seat(0, transition) && transition.isEmpty(), "a Transition card seats on it");
        helper.assertTrue(rig.cardOn(0).is(HardwareItems.GPU_VERTEX_7600_GT.get()) && rig.seatedCards() == 1,
                "and stands there");
        helper.assertTrue(rig.seatRefusal(0, new ItemStack(HardwareItems.GPU_VERTEX_7600_GT.get()))
                == MiningComputerTexts.RISER_TAKEN, "a riser carries one card");
        helper.assertTrue(rig.unseat(0).is(HardwareItems.GPU_VERTEX_7600_GT.get()) && rig.seatedCards() == 0,
                "and gives it back, the rig off");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void asic_minesAloneWithItsEnergyAndPaysItsAddress(final GameTestHelper helper) {
        helper.setBlock(MACHINE, ComputingModule.ASIC_FORGE_A2.get());
        final AsicBlockEntity asic = (AsicBlockEntity) helper.getBlockEntity(MACHINE);
        final TeracoinChain.Keys keys = TeracoinState.of(helper.getLevel().getServer()).chain()
                .createWallet(RANDOM);
        helper.assertTrue(asic.setAddress(keys.address()), "its window takes the address");
        asic.setAlone();
        final IEnergyStorage line = helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK,
                helper.absolutePos(MACHINE), null);
        helper.assertTrue(line != null, "it takes energy");
        helper.onEachTick(() -> line.receiveEnergy(ENERGY_A_TICK, false));
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(asic.mining(), "with its watts it mines; " + asic.status()))
                .thenWaitUntil(() -> {
                    final TeracoinChain chain = TeracoinState.of(helper.getLevel().getServer()).chain();
                    helper.assertTrue(chain.balance(keys.address()) > 0L, "a Forge A2 alone soon finds a block and is"
                            + " paid; status " + asic.status() + ", found " + asic.found() + ", chain at "
                            + chain.height() + ", difficulty " + chain.difficulty(helper.getLevel().getDayTime()
                            / GameCalendar.DAY_TICKS));
                })
                .thenExecute(() -> helper.assertTrue(asic.found() > 0, "and counts the block it found"))
                .thenSucceed();
    }

    /* Credits {@code units} to {@code address}, as a block found alone pays it. */
    private static void credit(final GameTestHelper helper, final String address, final long units) {
        final TeracoinState state = TeracoinState.of(helper.getLevel().getServer());
        state.chain().creditAlone(new FoundBlock(state.chain().height(), helper.getLevel().getGameTime(), 0L, units,
                1L, "test", "test", address, "", false));
        state.setDirty();
    }

    /* A new wallet named {@code name} on {@code pc}, its path. */
    private static String walletOn(final GameTestHelper helper, final PersonalComputerBlockEntity pc,
                                   final String name) {
        final TeracoinChain.Keys keys = TeracoinState.of(helper.getLevel().getServer()).chain()
                .createWallet(RANDOM);
        final String path = WalletFiles.write(pc.services().files(), WalletFiles.folder(pc), name, keys, (char) 0);
        if (path == null) {
            throw new IllegalStateException("the wallet " + name + " could not be written");
        }
        return path;
    }
}
