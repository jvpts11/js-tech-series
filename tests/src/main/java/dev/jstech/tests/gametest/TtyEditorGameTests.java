/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.JsComputers;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.operation.payload.CommandOutputPayload;
import dev.jstech.computers.operation.payload.DesktopShellOutputPayload;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.program.cli.CliCommands;
import dev.jstech.computers.program.cli.CliLine;
import dev.jstech.computers.program.cli.CliShell;
import dev.jstech.computers.program.cli.ICliCommand;
import dev.jstech.computers.os.ShellFamily;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestWorldBuilder;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Handing a terminal to an editor, over the wire and through the shell.
 *
 * <p>The machine decides whether an editor opens, so the two replies that carry that decision are
 * encoded here at the longest they may be: a cap on a payload string does not cut what is too long, it
 * throws as the packet is sent and takes the player's connection with it.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class TtyEditorGameTests {

    private TtyEditorGameTests() {
    }

    private static final String ARENA = "empty";

    /** The longest a file's path may be on the wire, which is what an editor is opened on. */
    private static final String LONG_PATH = "progs/" + "deep/".repeat(29) + "a.sgs";

    private static final int SETTLE = 4;
    private static final ResourceLocation FREEBSD = ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "freebsd");
    private static final ResourceLocation UNIX = ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "unix");

    private static RegistryFriendlyByteBuf buffer(final GameTestHelper helper) {
        return new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
    }

    /** The desktop terminal's reply, carrying a hand-over on the longest path it could name. */
    @GameTest(template = ARENA)
    public static void desktopShellOutput_carriesAHandOverOverTheWire(final GameTestHelper helper) {
        final DesktopShellOutputPayload payload = new DesktopShellOutputPayload(
                false, false, "C:\\progs>", List.of(), "vim", LONG_PATH);
        final RegistryFriendlyByteBuf buf = buffer(helper);
        try {
            DesktopShellOutputPayload.STREAM_CODEC.encode(buf, payload);
        } catch (final RuntimeException e) {
            helper.fail("a hand-over does not encode: " + e.getMessage());
            return;
        }
        final DesktopShellOutputPayload back = DesktopShellOutputPayload.STREAM_CODEC.decode(buf);
        helper.assertTrue(back.handsOver(), "the reply still says it hands the terminal over");
        helper.assertTrue(back.editor().equals("vim"), "the editor named survives");
        helper.assertTrue(back.editorPath().equals(LONG_PATH), "the file it opens on survives whole");
        helper.succeed();
    }

    /** The same for the terminal of a machine with no desktop at all, which is where it matters most. */
    @GameTest(template = ARENA)
    public static void commandOutput_carriesAHandOverOverTheWire(final GameTestHelper helper) {
        final CommandOutputPayload payload =
                new CommandOutputPayload(false, "C:\\progs>", List.of(), "vim", LONG_PATH);
        final RegistryFriendlyByteBuf buf = buffer(helper);
        try {
            CommandOutputPayload.STREAM_CODEC.encode(buf, payload);
        } catch (final RuntimeException e) {
            helper.fail("a hand-over does not encode: " + e.getMessage());
            return;
        }
        final CommandOutputPayload back = CommandOutputPayload.STREAM_CODEC.decode(buf);
        helper.assertTrue(back.handsOver(), "the reply still says it hands the terminal over");
        helper.assertTrue(back.editorPath().equals(LONG_PATH), "the file it opens on survives whole");
        helper.succeed();
    }

    /** A reply that only printed says so, which is what nearly every command does. */
    @GameTest(template = ARENA)
    public static void aPlainReply_handsNothingOver(final GameTestHelper helper) {
        helper.assertTrue(!new DesktopShellOutputPayload(false, false, "", List.of()).handsOver(),
                "a plain desktop reply hands nothing over");
        helper.assertTrue(!new CommandOutputPayload(false, "", List.of()).handsOver(),
                "a plain prompt reply hands nothing over");
        helper.succeed();
    }

    /**
     * The editors are verbs of the shell, so a machine that has them lists them in help.
     *
     * <p>They are also gated on being installed, which is why the plain shell offers them and a
     * computer without them does not; that half is the machine's own answer at run time.
     */
    @GameTest(template = ARENA)
    public static void theEditors_areCommandsOfEveryShellFamily(final GameTestHelper helper) {
        for (final ShellFamily family : ShellFamily.values()) {
            for (final String verb : List.of("vim", "emacs", "vi", "ee")) {
                boolean found = false;
                for (final ICliCommand command : CliCommands.commandsFor(family)) {
                    if (command.name().equals(verb)) {
                        found = true;
                        helper.assertTrue(command.usage().english().contains("<file>"),
                                verb + " should say it takes a file");
                    }
                }
                helper.assertTrue(found, family + " should know " + verb);
            }
        }
        helper.succeed();
    }

    /**
     * {@code vi} is one engine wearing two systems' own voices: FreeBSD's hands the terminal to nvi and
     * UNIX's to System V's original, chosen from the machine's own platform rather than from what the
     * player typed, which is the same word on both.
     */
    @GameTest(template = ARENA)
    public static void vi_answersToEachSystemsOwnDialectOfOneEngine(final GameTestHelper helper) {
        final MainframeBlockEntity freeBsd = mainframe(helper, new BlockPos(2, 2, 2), FREEBSD);
        final MainframeBlockEntity unix = mainframe(helper, new BlockPos(6, 2, 2), UNIX);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final ServerCliComputer freeBsdCli = new ServerCliComputer(freeBsd, helper.getLevel());
                    final CliShell freeBsdShell = CliCommands.newShell(freeBsdCli.shellFamily(), 52);
                    final CliShell.Response onFreeBsd = freeBsdShell.run("vi notes.txt", freeBsdCli);
                    helper.assertTrue(onFreeBsd.handOver() != null
                                    && onFreeBsd.handOver().editor().equals("vi-freebsd"),
                            "FreeBSD's vi hands over as nvi's own dialect; got " + onFreeBsd.handOver());

                    final ServerCliComputer unixCli = new ServerCliComputer(unix, helper.getLevel());
                    final CliShell unixShell = CliCommands.newShell(unixCli.shellFamily(), 52);
                    final CliShell.Response onUnix = unixShell.run("vi notes.txt", unixCli);
                    helper.assertTrue(onUnix.handOver() != null && onUnix.handOver().editor().equals("vi-unix"),
                            "UNIX's vi hands over as System V's own dialect; got " + onUnix.handOver());
                })
                .thenSucceed();
    }

    /** {@code ee} comes bundled with FreeBSD alone: UNIX never had it, and neither did the Linux family. */
    @GameTest(template = ARENA)
    public static void ee_comesBundledOnFreeBsdAlone(final GameTestHelper helper) {
        final MainframeBlockEntity freeBsd = mainframe(helper, new BlockPos(2, 2, 2), FREEBSD);
        final MainframeBlockEntity unix = mainframe(helper, new BlockPos(6, 2, 2), UNIX);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final ServerCliComputer freeBsdCli = new ServerCliComputer(freeBsd, helper.getLevel());
                    final CliShell freeBsdShell = CliCommands.newShell(freeBsdCli.shellFamily(), 52);
                    final CliShell.Response opened = freeBsdShell.run("ee notes.txt", freeBsdCli);
                    helper.assertTrue(opened.handOver() != null && opened.handOver().editor().equals("ee"),
                            "ee needs no install on FreeBSD, since it comes with the system; got "
                                    + opened.handOver());

                    final ServerCliComputer unixCli = new ServerCliComputer(unix, helper.getLevel());
                    final CliShell unixShell = CliCommands.newShell(unixCli.shellFamily(), 52);
                    helper.assertTrue(text(unixShell.run("ee notes.txt", unixCli)).contains("not found"),
                            "UNIX never had ee, which is FreeBSD's own gift to newcomers");
                })
                .thenSucceed();
    }

    /**
     * vi and ee come with the system, so they show under /usr/bin without ever being installed; the desktop
     * apps a desktop environment would bundle do not, since a console machine with none installed has no
     * desktop to bundle them.
     */
    @GameTest(template = ARENA)
    public static void viAndEe_showUnderUsrBinOnceInstalledNever(final GameTestHelper helper) {
        final MainframeBlockEntity freeBsd = mainframe(helper, new BlockPos(2, 2, 2), FREEBSD);
        final MainframeBlockEntity unix = mainframe(helper, new BlockPos(6, 2, 2), UNIX);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final ServerCliComputer freeBsdCli = new ServerCliComputer(freeBsd, helper.getLevel());
                    final CliShell freeBsdShell = CliCommands.newShell(freeBsdCli.shellFamily(), 52);
                    final List<String> freeBsdBin = names(freeBsdShell.run("ls /usr/bin", freeBsdCli));
                    helper.assertTrue(freeBsdBin.contains("vi") && freeBsdBin.contains("ee"),
                            "FreeBSD's base binaries hold both; got " + freeBsdBin);
                    helper.assertFalse(freeBsdBin.contains("settings") || freeBsdBin.contains("files"),
                            "a console FreeBSD with no desktop bundles none of its apps; got " + freeBsdBin);

                    final ServerCliComputer unixCli = new ServerCliComputer(unix, helper.getLevel());
                    final CliShell unixShell = CliCommands.newShell(unixCli.shellFamily(), 52);
                    final List<String> unixBin = names(unixShell.run("ls /usr/bin", unixCli));
                    helper.assertTrue(unixBin.contains("vi"), "UNIX's own vi is there too; got " + unixBin);
                    helper.assertFalse(unixBin.contains("ee"), "UNIX never had ee; got " + unixBin);
                    helper.assertFalse(unixBin.contains("dtwsinfo"),
                            "UNIX with no CDE bundles no Workstation Info either; got " + unixBin);
                })
                .thenSucceed();
    }

    /** All of a shell response's lines joined with newlines. */
    private static String text(final CliShell.Response response) {
        final StringBuilder out = new StringBuilder();
        for (final CliLine line : response.lines()) {
            out.append(line.text()).append('\n');
        }
        return out.toString();
    }

    /** The whole file names a listing prints, split so one name is never mistaken for a substring of another. */
    private static List<String> names(final CliShell.Response response) {
        final List<String> out = new ArrayList<>();
        for (final String token : text(response).trim().split("\\s+")) {
            if (!token.isEmpty()) {
                out.add(token.endsWith("/") ? token.substring(0, token.length() - 1) : token);
            }
        }
        return out;
    }

    /** A powered Mainframe with a full build and that system installed on it. */
    private static MainframeBlockEntity mainframe(final GameTestHelper helper, final BlockPos pos,
                                                   final ResourceLocation osId) {
        helper.setBlock(pos, ComputingModule.MAINFRAME.get());
        if (!(helper.getBlockEntity(pos) instanceof MainframeBlockEntity mainframe)) {
            throw new IllegalStateException("no MainframeBlockEntity at " + pos);
        }
        TestWorldBuilder.installMainframeBuild(mainframe);
        mainframe.uninstallOs();
        if (!mainframe.installOs(osId)) {
            throw new IllegalStateException("failed to install " + osId + " on the test Mainframe");
        }
        mainframe.togglePower();
        return mainframe;
    }
}
