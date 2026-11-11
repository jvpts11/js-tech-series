/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.computers;

import dev.jstech.computers.blockentity.AbstractComputerBlockEntity;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.machine.MachinePrograms;
import dev.jstech.computers.sigma.SigmaCompiler;
import dev.jstech.computers.sigma.SourceFile;
import dev.jstech.computers.teracoin.FoundBlock;
import dev.jstech.computers.teracoin.MinerSettings;
import dev.jstech.computers.teracoin.TeracoinChain;
import dev.jstech.computers.teracoin.TeracoinState;
import dev.jstech.computers.teracoin.Trc;
import dev.jstech.computers.teracoin.WalletFiles;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.computers.TestWorldBuilder;
import java.util.List;
import java.util.Locale;
import java.util.random.RandomGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * A program reaching Teracoin through System.Teracoin on a real computer: it reads a wallet file's address and balance
 * and sends from it as the Wallet program does, reads the chain, and sets its machine's miner as the miner command
 * does.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class SigmaTeracoinGameTests {

    private static final String ARENA = "empty";
    private static final int SETTLE = 10;
    private static final BlockPos AT = new BlockPos(2, 2, 2);
    /** The namespaces a program brings in, on one line so its source keeps its line numbers. */
    private static final String PRELUDE = "using System.*; using System.IO.*; using System.Teracoin.*; "
            + "namespace Programs; ";
    private static final RandomGenerator RANDOM = RandomGenerator.getDefault();

    private SigmaTeracoinGameTests() {
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void wallet_aProgramReadsAWalletFileAndSendsFromIt(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = TestWorldBuilder.forGameTest(helper).placeRunningPersonalComputer(AT);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final TeracoinChain chain = TeracoinState.of(helper.getLevel().getServer()).chain();
                    final TeracoinChain.Keys from = chain.createWallet(RANDOM);
                    final String path = WalletFiles.write(pc.services().files(), WalletFiles.folder(pc), "spending",
                            from, (char) 0);
                    helper.assertTrue(path != null, "the wallet's file is written");
                    credit(helper, from.address(), Trc.of(5));
                    final String to = chain.createWallet(RANDOM).address();
                    final String written = path.replace("\\", "\\\\");
                    helper.assertValueEqual(run(helper, pc, """
                            class Payer {
                                static void Main() {
                                    string from = Wallet.Address("%s");
                                    Console.PrintLine(from == "%s" ? "address read" : "address wrong");
                                    Console.PrintLine("" + Wallet.Balance(from));
                                    Console.PrintLine(Wallet.Send("%s", "%s", 200000000, "rent") ? "sent" : "not sent");
                                    Console.PrintLine(Wallet.Send("%s", "%s", 900000000, "all") ? "sent" : "not sent");
                                    Console.PrintLine("" + Wallet.Balance(from));
                                    string none = Wallet.Address("nowhere.wallet");
                                    Console.PrintLine(none == "" ? "no wallet" : "a wallet");
                                    try {
                                        Wallet.Balance("trc1qnot");
                                    } catch (ArgumentException failed) {
                                        Console.PrintLine("not an address");
                                    }
                                }
                            }
                            """.formatted(written, from.address(), written, to, written, to)),
                            List.of("address read", String.valueOf(Trc.of(5)), "sent", "not sent",
                                    String.valueOf(Trc.of(3)), "no wallet", "not an address"),
                            "what the program said");
                    helper.assertValueEqual(chain.balance(from.address()), Trc.of(3), "the chain moved the two TRC");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void chainAndMiner_aProgramReadsTheChainAndSetsItsMiner(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = TestWorldBuilder.forGameTest(helper).placeRunningPersonalComputer(AT);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final TeracoinChain chain = TeracoinState.of(helper.getLevel().getServer()).chain();
                    final String address = chain.createWallet(RANDOM).address();
                    final List<String> said = run(helper, pc, """
                            class Miners {
                                static void Main() {
                                    Console.PrintLine(Chain.Height + " " + Chain.Reward);
                                    Console.PrintLine(Miner.Status);
                                    Console.PrintLine(Miner.Start() ? "started" : "no address");
                                    Miner.SetAddress("%s");
                                    Miner.UsePool("Deep");
                                    Console.PrintLine(Miner.Start() ? "started" : "no address");
                                    Console.PrintLine(Miner.Background ? "in the background" : "in front");
                                    Miner.MineAlone();
                                    Miner.Stop();
                                    Console.PrintLine(Miner.Background ? "in the background" : "in front");
                                    Console.PrintLine(Miner.Address == "%s" ? "pays its address" : "pays another");
                                    Console.PrintLine(Pool.Names().Count + " pools");
                                    Console.PrintLine(Pool.Of("") == null ? "no pool" : "a pool");
                                }
                            }
                            """.formatted(address.toUpperCase(Locale.ROOT), address));
                    helper.assertValueEqual(said, List.of(chain.height() + " " + chain.nextReward(), "off",
                            "no address", "started", "in the background", "in front", "pays its address", "0 pools",
                            "no pool"), "what the program said");
                    final MinerSettings miner = pc.console().miner();
                    helper.assertTrue(miner.alone() && !miner.background() && miner.address().equals(address),
                            "the machine keeps what the program set");
                })
                .thenSucceed();
    }

    private static void credit(final GameTestHelper helper, final String address, final long units) {
        final TeracoinState state = TeracoinState.of(helper.getLevel().getServer());
        state.chain().creditAlone(new FoundBlock(state.chain().height(), helper.getLevel().getGameTime(), 0L, units,
                1L, "test", "test", address, "", false));
        state.setDirty();
    }

    /* Runs {@code source} on {@code computer} to its end and gives back what it printed. */
    private static List<String> run(final GameTestHelper helper, final AbstractComputerBlockEntity computer,
                                    final String source) {
        final SigmaCompiler.Result built = SigmaCompiler.compile(List.of(new SourceFile("Script.sgs",
                PRELUDE + source)));
        helper.assertTrue(built.ok(), "the script compiles: " + String.join("\n", built.lines()));
        final MachinePrograms.Started started = computer.programs().start("payer.asm", built.assembly(), 1,
                computer);
        helper.assertTrue(started.ok(), "the program starts: " + started.message());
        computer.programs().tick(100000);
        return computer.programs().byId(started.id()).process().console();
    }
}
