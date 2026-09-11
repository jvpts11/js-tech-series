/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dan200.computercraft.api.filesystem.Mount;
import dan200.computercraft.api.filesystem.MountConstants;
import dan200.computercraft.api.filesystem.WritableMount;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.PeripheralCapability;
import dan200.computercraft.api.peripheral.WorkMonitor;
import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.block.NetworkGatewayBlock;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.NetworkGatewayBlockEntity;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.gateway.GatewayPermissions;
import dev.jstech.computers.gateway.GatewayStats;
import dev.jstech.computers.integration.computercraft.GatewayPeripheral;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestWorldBuilder;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The shared folders as ComputerCraft computers get them through a Gateway: mounted on attach under
 * {@code jsc/<computer>/<share>}, read-only or writable as the Gateway and the sharing computer allow,
 * remounted when the permission changes, gone when files are off; and a mount reads, writes, makes,
 * renames and deletes through the sharing computer's own rules.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class GatewayMountGameTests {

    private GatewayMountGameTests() {
    }

    private static final String ARENA = "empty";
    private static final int SETTLE = 4;
    private static final BlockPos GATEWAY = new BlockPos(6, 2, 2);
    private static final int CC_ID = 7;
    private static final String LAB_PUB = "jsc/lab/pub";
    private static final String DESK_SCRIPTS = "jsc/desk/scripts";

    /** A ComputerCraft computer as the peripheral sees it, keeping what was mounted on it and how. */
    private static final class FakeComputer implements IComputerAccess {

        private final int id;
        private final Map<String, Mount> mounts = new LinkedHashMap<>();
        private final Map<String, Boolean> writable = new LinkedHashMap<>();

        private FakeComputer(final int id) {
            this.id = id;
        }

        @Override
        public String mount(final String desiredLocation, final Mount mount, final String driveName) {
            mounts.put(desiredLocation, mount);
            writable.put(desiredLocation, false);
            return desiredLocation;
        }

        @Override
        public String mountWritable(final String desiredLocation, final WritableMount mount, final String driveName) {
            mounts.put(desiredLocation, mount);
            writable.put(desiredLocation, true);
            return desiredLocation;
        }

        @Override
        public void unmount(final String location) {
            mounts.remove(location);
            writable.remove(location);
        }

        @Override
        public int getID() {
            return id;
        }

        @Override
        public void queueEvent(final String event, final Object... arguments) {
        }

        @Override
        public String getAttachmentName() {
            return "left";
        }

        @Override
        public Map<String, IPeripheral> getAvailablePeripherals() {
            return Map.of();
        }

        @Override
        public IPeripheral getAvailablePeripheral(final String name) {
            return null;
        }

        @Override
        public WorkMonitor getMainThreadMonitor() {
            return new WorkMonitor() {
                @Override
                public boolean canWork() {
                    return true;
                }

                @Override
                public boolean shouldWork() {
                    return true;
                }

                @Override
                public void trackWork(final long time, final TimeUnit unit) {
                }
            };
        }

        WritableMount at(final String location) {
            return (WritableMount) mounts.get(location);
        }

        boolean writableAt(final String location) {
            return Boolean.TRUE.equals(writable.get(location));
        }
    }

    private record Fleet(MainframeBlockEntity mainframe, PersonalComputerBlockEntity host, PersonalComputerBlockEntity lab) {
    }

    private static Fleet wire(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final MainframeBlockEntity mainframe = world.placeRunningMainframe(new BlockPos(1, 2, 2));
        world.setBlock(new BlockPos(2, 2, 2), ComputingModule.HBW_CABLE.get());
        world.placeSeededRack(new BlockPos(2, 2, 1));
        world.setBlock(new BlockPos(3, 2, 2), ComputingModule.PERSONAL_ROUTER.get());
        world.setBlock(new BlockPos(4, 2, 2), ComputingModule.ETHERNET_CABLE.get());
        final PersonalComputerBlockEntity host = world.placeRunningPersonalComputer(new BlockPos(5, 2, 2));
        host.console().setComputerName("desk");
        world.setBlock(new BlockPos(4, 2, 3), ComputingModule.ETHERNET_CABLE.get());
        final PersonalComputerBlockEntity lab = world.placeRunningPersonalComputer(new BlockPos(5, 2, 3));
        lab.console().setComputerName("lab");
        helper.setBlock(GATEWAY, ComputingModule.NETWORK_GATEWAY.get().defaultBlockState()
                .setValue(NetworkGatewayBlock.FACING, Direction.EAST));
        return new Fleet(mainframe, host, lab);
    }

    private static NetworkGatewayBlockEntity gateway(final GameTestHelper helper) {
        if (helper.getBlockEntity(GATEWAY) instanceof NetworkGatewayBlockEntity be) {
            return be;
        }
        throw new IllegalStateException("no Gateway at " + GATEWAY);
    }

    private static GatewayPeripheral peripheral(final GameTestHelper helper) {
        final IPeripheral found = helper.getLevel().getCapability(PeripheralCapability.get(),
                helper.absolutePos(GATEWAY), Direction.EAST);
        if (!(found instanceof GatewayPeripheral peripheral)) {
            throw new IllegalStateException("the Gateway's front is not a jsc_gateway peripheral: " + found);
        }
        return peripheral;
    }

    /** Shares a writable folder with a file on lab and a read-only one on desk. */
    private static void share(final GameTestHelper helper, final Fleet fleet) {
        final ServerCliComputer lab = new ServerCliComputer(fleet.lab(), helper.getLevel());
        helper.assertTrue(lab.makeDir("C:\\pub").ok(), "lab's folder is made");
        helper.assertTrue(lab.writeFile("C:\\pub\\note.txt", "hi").ok(), "lab's file is written");
        helper.assertTrue(lab.setConfig("share", "C:\\pub write").ok(), "lab shares it for writing");
        final ServerCliComputer desk = new ServerCliComputer(fleet.host(), helper.getLevel());
        helper.assertTrue(desk.makeDir("C:\\scripts").ok() && desk.setConfig("share", "C:\\scripts").ok(),
                "desk shares a folder read-only");
    }

    private static String readAll(final SeekableByteChannel channel) throws IOException {
        final ByteBuffer buffer = ByteBuffer.allocate((int) channel.size() + 16);
        while (channel.read(buffer) > 0) {
            // keep reading
        }
        channel.close();
        buffer.flip();
        return StandardCharsets.UTF_8.decode(buffer).toString();
    }

    private static String failure(final IoCall call) {
        try {
            call.run();
            return "";
        } catch (final IOException error) {
            return error.getMessage() == null ? "failed" : error.getMessage();
        }
    }

    private interface IoCall {
        void run() throws IOException;
    }

    private static boolean readOnly(final WritableMount mount, final String path) {
        try {
            return mount.isReadOnly(path);
        } catch (final IOException error) {
            throw new IllegalStateException(error.getMessage(), error);
        }
    }

    @GameTest(template = ARENA)
    public static void mounts_followTheSharesAndTheFilesPermission(final GameTestHelper helper) {
        final Fleet fleet = wire(helper);
        final FakeComputer cc = new FakeComputer(CC_ID);
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 6, () -> {
                    share(helper, fleet);
                    peripheral(helper).attach(cc);
                    helper.assertTrue(cc.mounts.containsKey(LAB_PUB) && cc.mounts.containsKey(DESK_SCRIPTS),
                            "both shares are mounted on attach; got " + cc.mounts.keySet());
                    helper.assertTrue(!cc.writableAt(LAB_PUB) && !cc.writableAt(DESK_SCRIPTS),
                            "with files at read, every mount is read-only");
                    helper.assertTrue(readOnly(cc.at(LAB_PUB), "note.txt"), "and says so");

                    gateway(helper).setPermissions(GatewayPermissions.DEFAULT.withFiles(GatewayPermissions.FileAccess.READ_WRITE),
                            "desk", "set files read & write");
                    helper.assertTrue(cc.writableAt(LAB_PUB), "a writable share is remounted writable with files at read & write");
                    helper.assertTrue(!cc.writableAt(DESK_SCRIPTS), "a read-only share stays read-only");
                    helper.assertTrue(!readOnly(cc.at(LAB_PUB), "note.txt"), "and the mount says so");

                    gateway(helper).setPermissions(GatewayPermissions.DEFAULT.withFiles(GatewayPermissions.FileAccess.OFF),
                            "desk", "set files off");
                    helper.assertTrue(cc.mounts.isEmpty(), "with files off nothing stays mounted; got " + cc.mounts.keySet());

                    gateway(helper).setPermissions(GatewayPermissions.DEFAULT, "desk", "set files read");
                    helper.assertTrue(cc.mounts.size() == 2, "back to read, both come back");
                    final ServerCliComputer desk = new ServerCliComputer(fleet.host(), helper.getLevel());
                    helper.assertTrue(desk.setConfig("unshare", "scripts").ok(), "desk closes its share");
                    gateway(helper).bridge().refreshMounts();
                    helper.assertTrue(cc.mounts.containsKey(LAB_PUB) && !cc.mounts.containsKey(DESK_SCRIPTS),
                            "a closed share is unmounted on the next refresh; got " + cc.mounts.keySet());
                    peripheral(helper).detach(cc);
                    helper.assertTrue(cc.mounts.isEmpty(), "detaching unmounts everything");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void mount_readsAndWritesThroughTheSharingComputersRules(final GameTestHelper helper) {
        final Fleet fleet = wire(helper);
        final FakeComputer cc = new FakeComputer(CC_ID);
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 6, () -> {
                    share(helper, fleet);
                    gateway(helper).setPermissions(GatewayPermissions.DEFAULT.withFiles(GatewayPermissions.FileAccess.READ_WRITE),
                            "desk", "set files read & write");
                    peripheral(helper).attach(cc);
                    final WritableMount pub = cc.at(LAB_PUB);
                    final ServerCliComputer lab = new ServerCliComputer(fleet.lab(), helper.getLevel());
                    try {
                        final List<String> names = new ArrayList<>();
                        pub.list("", names);
                        helper.assertTrue(names.contains("note.txt"), "the mount lists the share's files; got " + names);
                        helper.assertTrue(pub.exists("note.txt") && !pub.isDirectory("note.txt") && pub.isDirectory(""),
                                "exists and isDirectory answer for a file and the root");
                        helper.assertTrue(!pub.exists("nothing.txt"), "a missing file does not exist");
                        helper.assertTrue(pub.getSize("note.txt") == 2L, "the size is the file's bytes; got " + pub.getSize("note.txt"));
                        helper.assertTrue("hi".equals(readAll(pub.openForRead("note.txt"))), "the file reads whole");
                        helper.assertTrue(pub.getAttributes("note.txt").isRegularFile(), "attributes come from the listing");

                        final SeekableByteChannel out = pub.openFile("report.txt", MountConstants.WRITE_OPTIONS);
                        out.write(ByteBuffer.wrap("from cc".getBytes(StandardCharsets.UTF_8)));
                        out.close();
                        helper.assertTrue("from cc".equals(lab.readFile("C:\\pub\\report.txt").message()),
                                "a file written on the mount lands on lab; got " + lab.readFile("C:\\pub\\report.txt").message());
                        final SeekableByteChannel more = pub.openFile("report.txt", MountConstants.APPEND_OPTIONS);
                        more.write(ByteBuffer.wrap(" and more".getBytes(StandardCharsets.UTF_8)));
                        more.close();
                        helper.assertTrue("from cc and more".equals(lab.readFile("C:\\pub\\report.txt").message()),
                                "append keeps what was there; got " + lab.readFile("C:\\pub\\report.txt").message());
                        // A Lua program a ComputerCraft computer drops in a share is a file lab keeps, and runs.
                        final SeekableByteChannel program = pub.openFile("prog.lua", MountConstants.WRITE_OPTIONS);
                        program.write(ByteBuffer.wrap("print('hi')".getBytes(StandardCharsets.UTF_8)));
                        program.close();
                        helper.assertTrue("print('hi')".equals(lab.readFile("C:\\pub\\prog.lua").message()),
                                "a Lua file written on the mount lands on lab; got " + lab.readFile("C:\\pub\\prog.lua").message());

                        pub.makeDirectory("out");
                        helper.assertTrue(pub.isDirectory("out") && lab.listDisk("C:\\pub\\out").ok(), "a folder is made on lab");
                        pub.rename("report.txt", "out/moved.txt");
                        helper.assertTrue(!pub.exists("report.txt") && pub.exists("out/moved.txt")
                                && "from cc and more".equals(readAll(pub.openForRead("out/moved.txt"))), "rename moves the file whole");
                        pub.delete("out/moved.txt");
                        helper.assertTrue(!pub.exists("out/moved.txt"), "delete removes it");
                        helper.assertTrue(pub.getRemainingSpace() > 0L && pub.getCapacity() > pub.getRemainingSpace(),
                                "free space and capacity come from lab's disk; free " + pub.getRemainingSpace() + " of " + pub.getCapacity());
                    } catch (final IOException error) {
                        throw new IllegalStateException(error.getMessage(), error);
                    }
                    final String odd = failure(() -> pub.openFile("image.png", MountConstants.WRITE_OPTIONS).close());
                    helper.assertTrue(odd.contains("unknown file type"), "a file type lab does not take is refused; got " + odd);
                    final WritableMount scripts = cc.at(DESK_SCRIPTS);
                    final String denied = failure(() -> scripts.makeDirectory("x"));
                    helper.assertTrue(denied.contains(MountConstants.ACCESS_DENIED), "a read-only mount refuses writes; got " + denied);
                    helper.assertTrue(failure(() -> scripts.openFile("x.txt", MountConstants.WRITE_OPTIONS)).contains(MountConstants.ACCESS_DENIED),
                            "and opening for writing");
                    helper.assertTrue(gateway(helper).stats().lastMinute(GatewayStats.Kind.FILE, helper.getLevel().getGameTime()) >= 6,
                            "the file work is counted");
                    helper.assertTrue(fleet.host().cannon().owed() > 0, "and paid for by the host");
                })
                .thenSucceed();
    }
}
