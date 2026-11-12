/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.computers;

import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.program.cli.CliCommands;
import dev.jstech.computers.program.cli.CliLine;
import dev.jstech.computers.program.cli.CliShell;
import dev.jstech.computers.program.tty.ITtyProcess;
import dev.jstech.computers.program.tty.ITtySink;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.computers.TestWorldBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Linux's fsck (computers/docs/design/storage-devices.md): it checks the disk through the machine's own repair and
 * prints its passes as the repair goes.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class FsckGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos AT = new BlockPos(4, 2, 4);
    private static final ResourceLocation UBUNTU = ResourceLocation.fromNamespaceAndPath("jsc", "ubuntu");

    private FsckGameTests() {
    }

    /** fsck says what it checks the moment it starts, where it waited and printed nothing, ever. */
    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void fsck_opensAtOnceAndPrintsItsPasses(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = TestWorldBuilder.forGameTest(helper).placeRunningPersonalComputer(AT,
                UBUNTU);
        helper.startSequence()
                .thenExecute(() -> {
                    final ServerCliComputer cli = new ServerCliComputer(pc, helper.getLevel());
                    final CliShell shell = CliCommands.shellFor(cli, 80);
                    final ITtyProcess fsck = shell.run("fsck -y", cli).started();
                    helper.assertTrue(fsck != null, "fsck takes the terminal over");
                    final List<String> said = new ArrayList<>();
                    final ITtySink sink = new ITtySink() {
                        @Override
                        public void line(final CliLine line) {
                            said.add(line.text());
                        }

                        @Override
                        public void redraw(final CliLine line) {
                            said.add(line.text());
                        }
                    };
                    fsck.begin(0L);
                    fsck.advance(0L, sink);
                    helper.assertFalse(said.isEmpty(), "it opens with what it checks");
                })
                .thenSucceed();
    }
}
