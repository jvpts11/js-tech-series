/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.operation.payload.SettingsSnapshotPayload;
import dev.jstech.computers.os.install.InstallerFlow;
import dev.jstech.tests.JsTests;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/**
 * The settings snapshot carries names the player chose: a running program is named by its path, and a
 * project under its solution's folder makes a path longer than any cap. A cap on the wire is a hard
 * failure that drops the connection, so every string is cut to its cap before it is written, and a
 * snapshot with a long name still reaches the client, shortened.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class SettingsPayloadGameTests {

    private SettingsPayloadGameTests() {
    }

    private static final String ARENA = "empty";

    @GameTest(template = ARENA)
    public static void settingsSnapshot_cutsALongProcessNameInsteadOfDroppingTheConnection(final GameTestHelper helper) {
        final String longName = "progs/MyVeryLongSolutionName/MyVeryLongSolutionName/build/MyVeryLongSolutionName.asm";
        final String longDisk = "A removable disk whose label runs well past the forty-eight characters";
        helper.assertTrue(longName.length() > SettingsSnapshotPayload.LABEL_MAX, "the name under test is over the cap");
        final SettingsSnapshotPayload snapshot = new SettingsSnapshotPayload(new BlockPos(1, 2, 3),
                "win11", "A computer name that is also far longer than the cap allows it to be", 0, false, 75, 100,
                "C", true, "", true, false, 0, "1 CPU", 100, "x86-64, 64-bit", 256, 0, "frames_11", "Frames",
                List.of("jsc:sgsc"),
                List.of(new SettingsSnapshotPayload.DiskUse(longDisk, 500, 12, true)),
                40, List.of(new SettingsSnapshotPayload.RamUse(longName, 12, "PROCESS", 7)),
                List.of(new SettingsSnapshotPayload.ShareRow("pub", "C:\\pub", true)), false);
        final RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        SettingsSnapshotPayload.STREAM_CODEC.encode(buf, snapshot);
        final SettingsSnapshotPayload back = SettingsSnapshotPayload.STREAM_CODEC.decode(buf);
        helper.assertTrue(back.ramUses().size() == 1
                        && back.ramUses().get(0).label().equals(longName.substring(0, SettingsSnapshotPayload.LABEL_MAX))
                        && back.ramUses().get(0).id() == 7,
                "the process arrives with its name cut to the cap and its number intact; got " + back.ramUses());
        helper.assertTrue(back.disks().get(0).label().length() == SettingsSnapshotPayload.LABEL_MAX
                        && back.disks().get(0).capMb() == 500,
                "the disk label is cut the same way; got " + back.disks());
        /*
         * A computer's name is not one of the labels cut to the panel's width: it may be as long as a name may
         * be, which is longer than anything else on this packet, and it is cut at that length instead.
         */
        helper.assertTrue(back.computerName().equals(snapshot.computerName())
                        && back.installed().equals(List.of("jsc:sgsc")) && back.guiScale() == 75,
                "everything else travels whole; got the name as " + back.computerName());
        helper.assertTrue(back.cpuArch().equals("x86-64, 64-bit"),
                "the architecture the screens show travels whole; got " + back.cpuArch());
        // And a name past even that length is cut to it rather than refused, which would drop the connection.
        final String pastTheLimit = "n".repeat(InstallerFlow.MOST_NAME_LETTERS + 40);
        final SettingsSnapshotPayload named = new SettingsSnapshotPayload(new BlockPos(1, 2, 3),
                "win11", pastTheLimit, 0, false, 75, 100,
                "C", true, "", true, false, 0, "1 CPU", 100, "x86-64, 64-bit", 256, 0, "frames_11", "Frames",
                List.of(), List.of(), 40, List.of(), List.of(), false);
        final RegistryFriendlyByteBuf longBuf =
                new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        SettingsSnapshotPayload.STREAM_CODEC.encode(longBuf, named);
        final SettingsSnapshotPayload cut = SettingsSnapshotPayload.STREAM_CODEC.decode(longBuf);
        helper.assertTrue(cut.computerName().length() == InstallerFlow.MOST_NAME_LETTERS,
                "a name past the limit arrives cut to it; got " + cut.computerName().length());
        helper.assertTrue(back.shares().size() == 1 && back.shares().get(0).path().equals("C:\\pub")
                        && back.shares().get(0).writable() && !back.remoteAllowed(),
                "the shares and the remote switch travel too; got " + back.shares());
        helper.succeed();
    }
}
