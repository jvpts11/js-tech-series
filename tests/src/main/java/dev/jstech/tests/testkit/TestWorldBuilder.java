/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.testkit;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.blockentity.CraftingComputerBlockEntity;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.computers.HardwareItems;
import dev.jstech.computers.hardware.DiskSize;
import dev.jstech.computers.hardware.StorageTier;
import dev.jstech.computers.operation.NetworkStorage;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.install.SetupRunner;
import dev.jstech.computers.os.install.SetupTiming;
import dev.jstech.core.cable.CableBlock;
import dev.jstech.core.gametest.ScenarioBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

/**
 * Builds the standard test scenarios (a booted Mainframe, running computers, a seeded Server Rack, the
 * crafting network) directly in a {@link ServerLevel}, so the same fixtures serve both the headless
 * GameTests and the client-driven tests. Positions handed to this builder are relative: a GameTest maps
 * them through its arena, a client test through the origin of the area it owns. What any mod's tests need
 * (blocks, cables, placing as a player does) is the Core's {@link ScenarioBuilder}; this adds the series'
 * machines.
 */
public final class TestWorldBuilder extends ScenarioBuilder {

    /** The Network OS id installed on every test Mainframe: the minimal OS that enables orchestration. */
    public static final ResourceLocation NETWORK_OS = ResourceLocation.fromNamespaceAndPath("jsc", "mc_net");

    /**
     * What the base's desktop machines run: Frames 11, put straight on the disk. Their boards are Standard ones, on
     * which a player's installer offers Frames 10 and keeps 11 for an Advanced machine; the tests reach the newest
     * desktop without going through the installer, which is tested on its own.
     */
    public static final ResourceLocation DESKTOP_OS = ResourceLocation.fromNamespaceAndPath("jsc", "frames_11");

    /** The system a Standard machine's installer offers, for the tests that hold a machine to its own age. */
    public static final ResourceLocation STANDARD_OS = ResourceLocation.fromNamespaceAndPath("jsc", "frames_10");

    private TestWorldBuilder(final ServerLevel level, final UnaryOperator<BlockPos> toAbsolute) {
        super(level, toAbsolute);
    }

    /** A builder whose relative positions are the GameTest arena's, exactly like {@code helper.setBlock}. */
    public static TestWorldBuilder forGameTest(final GameTestHelper helper) {
        return new TestWorldBuilder(helper.getLevel(), helper::absolutePos);
    }

    /** A builder whose relative positions are offsets from {@code origin}. */
    public static TestWorldBuilder at(final ServerLevel level, final BlockPos origin) {
        return new TestWorldBuilder(level, origin::offset);
    }

    // Hardware builds

    /**
     * Runs whatever {@code host} is setting up to its end, tick by tick, the way waiting would.
     *
     * <p>A setup takes seconds of game time so a player sees it; a test that only cares what the
     * machine looks like afterwards ticks the job itself rather than sleeping through the bar.
     */
    public static void finishSetup(final IOsHost host, final ServerLevel level,
                                   final BlockPos pos) {
        final int most = SetupTiming.MAX_SECONDS * SetupTiming.TICKS_PER_SECOND + 1;
        for (int i = 0; i < most && host.console() != null && host.console().setup() != null; i++) {
            SetupRunner.tick(host, level, pos);
        }
    }

    /**
     * Installs the minimal valid Mainframe build: board, CPU, RAM, PSU, a disk large enough for the Network
     * OS footprint, and the Network OS itself so Operations get dispatched. A valid build never powers on by
     * itself; callers toggle power when they want the machine running.
     */
    public static void installMainframeBuild(final MainframeBlockEntity be) {
        /*
         * The Network OS is 8 MB, one item of a 500 GB HDD's 2 000 at 256 MB the item. The disk must be in
         * place before installOs() so the footprint check passes.
         */
        installMainframeBuild(be, new ItemStack(ComputingModule.disk(StorageTier.HDD, DiskSize.GB_500)));
        be.installOs(NETWORK_OS);
    }

    /**
     * Sets the parts of a working Mainframe into its inventory, with a 500 GB disk of {@code disk}'s kind and a
     * graphics card when {@code withGpu} (which is what gives it the peripheral ports a monitor links to). No
     * system is installed and the machine is not powered, so a caller can pick its own system first.
     */
    public static void installMainframeParts(final MainframeBlockEntity be, final StorageTier disk,
                                             final boolean withGpu) {
        installMainframeBuild(be, new ItemStack(ComputingModule.disk(disk, DiskSize.GB_500)));
        if (withGpu) {
            be.getInventory().setStackInSlot(MainframeBlockEntity.GPU_SLOTS_START,
                    new ItemStack(ComputingModule.GPU_HD_7970.get()));
        }
    }

    /**
     * The standard Mainframe hardware (board, CPU, RAM, power supply) with {@code disk} in the first disk slot, or
     * no disk when it is null. No system is installed, so a test can put its own on the disk afterwards.
     */
    public static void installMainframeBuild(final MainframeBlockEntity be, final ItemStack disk) {
        final ItemStackHandler inv = be.getInventory();
        inv.setStackInSlot(MainframeBlockEntity.MOTHERBOARD_SLOT,
                new ItemStack(ComputingModule.MOTHERBOARD_MTX_S_2011.get()));
        inv.setStackInSlot(MainframeBlockEntity.CPU_SLOTS_START,
                new ItemStack(ComputingModule.CPU_SERVO_2620.get()));
        inv.setStackInSlot(MainframeBlockEntity.RAM_SLOTS_START,
                new ItemStack(ComputingModule.RAM_DDR3_8192.get()));
        inv.setStackInSlot(MainframeBlockEntity.PSU_SLOT,
                new ItemStack(ComputingModule.PSU_650G.get()));
        if (disk != null) {
            inv.setStackInSlot(MainframeBlockEntity.DISK_SLOTS_START, disk);
        }
    }

    /** Places a Mainframe controller, installs the valid build and powers it on. */
    public MainframeBlockEntity placeRunningMainframe(final BlockPos relative) {
        setBlock(relative, ComputingModule.MAINFRAME.get());
        final MainframeBlockEntity be = blockEntity(relative, MainframeBlockEntity.class);
        installMainframeBuild(be);
        be.togglePower();
        return be;
    }

    /** Places a Mainframe with the valid build and a 500 GB hard drive, powers it on and installs {@code os}. */
    public MainframeBlockEntity placeMainframeWithSystem(final BlockPos relative, final ResourceLocation os) {
        setBlock(relative, ComputingModule.MAINFRAME.get());
        final MainframeBlockEntity be = blockEntity(relative, MainframeBlockEntity.class);
        installMainframeBuild(be, new ItemStack(ComputingModule.disk(StorageTier.HDD, DiskSize.GB_500)));
        be.togglePower();
        if (!be.installOs(os)) {
            throw new IllegalStateException("could not install " + os + " on the test Mainframe");
        }
        return be;
    }

    /** Places a Personal Computer next to a cable (rear toward it), installs its hardware and powers it on. */
    public PersonalComputerBlockEntity placeRunningPersonalComputer(final BlockPos relative) {
        return placeRunningPersonalComputer(relative, DESKTOP_OS);
    }

    /** The same, with that system on its disk rather than the default desktop one. */
    public PersonalComputerBlockEntity placeRunningPersonalComputer(final BlockPos relative,
                                                                    final ResourceLocation os) {
        setBlock(relative, ComputingModule.PERSONAL_COMPUTER.get());
        faceRearTowardCable(relative);
        final PersonalComputerBlockEntity be = blockEntity(relative, PersonalComputerBlockEntity.class);
        final ItemStackHandler hw = be.getHardware();
        hw.setStackInSlot(PersonalComputerBlockEntity.MOTHERBOARD_SLOT,
                new ItemStack(HardwareItems.MOTHERBOARD_ATX_STANDARD_LGA1150.get()));
        hw.setStackInSlot(PersonalComputerBlockEntity.CPU_SLOT,
                new ItemStack(HardwareItems.CPU_INTEGRA_CENTRO_C7_4790K.get()));
        hw.setStackInSlot(PersonalComputerBlockEntity.RAM_SLOTS_START,
                new ItemStack(ComputingModule.RAM_DDR3_8192.get()));
        hw.setStackInSlot(PersonalComputerBlockEntity.PSU_SLOT,
                new ItemStack(ComputingModule.PSU_650G.get()));
        hw.setStackInSlot(PersonalComputerBlockEntity.GPU_SLOTS_START,
                new ItemStack(ComputingModule.GPU_HD_7970.get()));
        /*
         * A machine on a real base has a disk with a system on it. Without one the computer powers on into
         * its firmware with nothing to boot, which is not what the base is meant to demonstrate.
         */
        /*
         * On a solid-state drive, because how long a system takes to come up is read off the disk it sits on:
         * a machine of this generation running this system would have one, and a test that sits through the
         * twenty seconds a mechanical disk costs is only testing the wait.
         */
        hw.setStackInSlot(PersonalComputerBlockEntity.DISK_SLOTS_START,
                new ItemStack(ComputingModule.disk(StorageTier.SSD, DiskSize.GB_500)));
        be.installOs(os);
        be.togglePower();
        return be;
    }

    /**
     * Places a Crafting Computer next to a cable (rear toward it), installs its hardware including a
     * Crafting Card, and powers it on.
     */
    public CraftingComputerBlockEntity placeRunningCraftingComputer(final BlockPos relative) {
        return placeRunningCrafting(relative, new CraftingParts(ComputingModule.CRAFTING_COMPUTER.get(),
                HardwareItems.MOTHERBOARD_ATX_STANDARD_LGA1150.get(), HardwareItems.CPU_INTEGRA_CENTRO_C7_4790K.get(),
                ComputingModule.RAM_DDR3_8192.get(), ComputingModule.GPU_HD_7970.get(),
                ComputingModule.PSU_650G.get(),
                // Solid state for the same reason as the personal computer above: the disk decides how long it takes.
                StorageTier.SSD), DESKTOP_OS);
    }

    /**
     * Places a Legacy-era Crafting Computer next to a cable, gives it hardware of its own generation, and
     * powers it on.
     *
     * <p>A machine of an earlier era is not the same machine in another colour: its chassis takes only
     * boards of its own generation, and what it wears follows from that. The period desktop chrome, the
     * panel built out of raised studs and sunken wells rather than a flat band, is only reachable on one of
     * these, so anything that wants to see it has to build one.
     *
     * <p>Legacy rather than Vintage because a Vintage machine has no PCIe slot for a Crafting Card, and
     * every caller so far wants a machine that can also do something.
     */
    public CraftingComputerBlockEntity placeRunningLegacyCraftingComputer(final BlockPos relative) {
        return placeRunningCrafting(relative, new CraftingParts(ComputingModule.LEGACY_CRAFTING_COMPUTER.get(),
                HardwareItems.MOTHERBOARD_ATX_LEGACY_LGA775.get(), HardwareItems.CPU_INTEGRA_PENTIX_4_560.get(),
                HardwareItems.RAM_DDR_1024.get(), HardwareItems.GPU_VERTEX_6600_GT.get(),
                HardwareItems.PSU_500B.get(), StorageTier.SSD), DESKTOP_OS);
    }

    /**
     * A Transition-era crafting computer, switched on with {@code os} installed: what the Unix desktops wear their
     * Transition faces on, KDE 4 and GNOME 2.
     */
    public CraftingComputerBlockEntity placeRunningTransitionCraftingComputer(final BlockPos relative,
                                                                              final ResourceLocation os) {
        return placeRunningCrafting(relative, new CraftingParts(ComputingModule.TRANSITION_CRAFTING_COMPUTER.get(),
                HardwareItems.MOTHERBOARD_ATX_TRANSITION_775.get(), HardwareItems.CPU_INTEGRA_CENTRO_2_DUO_E6600.get(),
                HardwareItems.RAM_DDR2_2048.get(), HardwareItems.GPU_VERTEX_8600_GT.get(),
                HardwareItems.PSU_450B.get(), StorageTier.HDD), os);
    }

    /** Seeds slot 0 of the rack at {@code relative} with the default server and its bay drives. */
    public ServerRackBlockEntity seedServer(final BlockPos relative) {
        final ServerRackBlockEntity rack = blockEntity(relative, ServerRackBlockEntity.class);
        mountDefaultServer(rack, 0);
        return rack;
    }

    /**
     * Mounts the default server at {@code slot} and slots the default pair of NVMe drives into the
     * bay it claims, since storage lives on the rack's front-panel drives, not on the Server item, so a
     * fixture that needs network storage must populate the bay too.
     */
    public static void mountDefaultServer(final ServerRackBlockEntity rack, final int slot) {
        rack.getServers().setStackInSlot(slot, ServerStacks.defaultServer());
        rack.insertDrive(slot, new ItemStack(ComputingModule.disk(StorageTier.NVME, DiskSize.TB_1)));
        rack.insertDrive(slot, new ItemStack(ComputingModule.disk(StorageTier.NVME, DiskSize.TB_1)));
    }

    /** Places a Server Rack (default orientation) and seeds it with the default server. */
    public ServerRackBlockEntity placeSeededRack(final BlockPos relative) {
        setBlock(relative, ComputingModule.SERVER_RACK.get());
        return seedServer(relative);
    }

    /** Places a Server Rack facing {@code facing} (cables attach through its rear) and seeds it. */
    public ServerRackBlockEntity placeSeededRack(final BlockPos relative, final Direction facing) {
        setBlock(relative, ComputingModule.SERVER_RACK.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, facing));
        return seedServer(relative);
    }

    /**
     * Turns a just-placed computer so its rear (its only data port) meets an adjacent horizontal cable.
     * Computers connect through the back face alone, so a fixture that drops one beside a cable must orient
     * it. Blocks without a facing property are left alone.
     */
    public void faceRearTowardCable(final BlockPos relative) {
        final BlockState state = getBlockState(relative);
        if (!state.hasProperty(HorizontalDirectionalBlock.FACING)) {
            return;
        }
        for (final Direction d : Direction.Plane.HORIZONTAL) {
            if (getBlockState(relative.relative(d)).getBlock() instanceof CableBlock) {
                setBlock(relative, state.setValue(HorizontalDirectionalBlock.FACING, d.getOpposite()));
                return;
            }
        }
    }

    /**
     * Gives a Crafting Computer a disk, installs the desktop OS {@code os} on it and installs {@code programs}
     * (by id, e.g. {@code jsc:crafting_manager}). Do this before powering the computer so it boots into the OS.
     */
    public static void installDesktop(final CraftingComputerBlockEntity cc, final ResourceLocation os,
                                      final ResourceLocation... programs) {
        cc.getHardware().setStackInSlot(CraftingComputerBlockEntity.DISK_SLOTS_START,
                new ItemStack(ComputingModule.disk(StorageTier.SSD, DiskSize.GB_500)));
        if (!cc.installOs(os)) {
            throw new IllegalStateException("could not install " + os + " on the Crafting Computer");
        }
        for (final ResourceLocation program : programs) {
            cc.console().install(program.toString());
        }
    }

    /**
     * Places an unpowered Crafting Computer with the plain hardware a program test needs (a standard board, a
     * 4790K, 8 GB of DDR3, a 650 W supply and a 500 GB hard drive, no Crafting Card) and installs {@code os} on it.
     */
    public CraftingComputerBlockEntity placeCraftingComputer(final BlockPos relative, final ResourceLocation os) {
        setBlock(relative, ComputingModule.CRAFTING_COMPUTER.get());
        final CraftingComputerBlockEntity be = blockEntity(relative, CraftingComputerBlockEntity.class);
        final ItemStackHandler hw = be.getHardware();
        hw.setStackInSlot(CraftingComputerBlockEntity.MOTHERBOARD_SLOT,
                new ItemStack(HardwareItems.MOTHERBOARD_ATX_STANDARD_LGA1150.get()));
        hw.setStackInSlot(CraftingComputerBlockEntity.CPU_SLOT,
                new ItemStack(HardwareItems.CPU_INTEGRA_CENTRO_C7_4790K.get()));
        hw.setStackInSlot(CraftingComputerBlockEntity.RAM_SLOTS_START,
                new ItemStack(ComputingModule.RAM_DDR3_8192.get()));
        hw.setStackInSlot(CraftingComputerBlockEntity.PSU_SLOT, new ItemStack(ComputingModule.PSU_650G.get()));
        hw.setStackInSlot(CraftingComputerBlockEntity.DISK_SLOTS_START,
                new ItemStack(ComputingModule.disk(StorageTier.HDD, DiskSize.GB_500)));
        if (!be.installOs(os)) {
            throw new IllegalStateException("could not install " + os + " on the Crafting Computer");
        }
        return be;
    }

    /** Places a Monitor facing {@code facing}; it links itself to an adjacent computer within a few ticks. */
    public void placeMonitor(final BlockPos relative, final Direction facing) {
        placeMonitor(relative, facing, ComputingModule.MONITOR.get());
    }

    /** Places that monitor facing {@code facing}; it links itself to an adjacent computer within a few ticks. */
    public void placeMonitor(final BlockPos relative, final Direction facing, final Block monitor) {
        BlockState state = monitor.defaultBlockState();
        if (state.hasProperty(HorizontalDirectionalBlock.FACING)) {
            state = state.setValue(HorizontalDirectionalBlock.FACING, facing);
        }
        setBlock(relative, state);
    }

    // Composite scenarios

    /**
     * The assembled crafting network, with handles on the parts assertions need: a booted Mainframe, a rack
     * holding one server (the network's storage) and a running Crafting Computer.
     */
    public record CraftingNetwork(MainframeBlockEntity mainframe, ServerRackBlockEntity rack,
                                  CraftingComputerBlockEntity cc) {

        /** Puts {@code count} of {@code item} straight into the server's store. */
        public void seed(final Item item, final int count) {
            rack.getServerStorage(0).insert(item, count);
        }

        public NetworkStorage storage(final ServerLevel level) {
            return NetworkStorage.of(level, mainframe.networkUuid());
        }
    }

    /**
     * Builds the standard crafting network along the x axis at y=2: Mainframe (1,2,2) to HBW cable (2,2,2)
     * with the rack beside it at (2,2,1) to Personal Router (3,2,2) to Ethernet cable (4,2,2) to Crafting
     * Computer (5,2,2), rear toward the cable.
     */
    public CraftingNetwork buildCraftingNetwork() {
        final MainframeBlockEntity mainframe = placeRunningMainframe(new BlockPos(1, 2, 2));
        setBlock(new BlockPos(2, 2, 2), ComputingModule.HBW_CABLE);
        final ServerRackBlockEntity rack = placeSeededRack(new BlockPos(2, 2, 1));
        setBlock(new BlockPos(3, 2, 2), ComputingModule.PERSONAL_ROUTER.get());
        setBlock(new BlockPos(4, 2, 2), ComputingModule.ETHERNET_CABLE);
        final CraftingComputerBlockEntity cc = placeRunningCraftingComputer(new BlockPos(5, 2, 2));
        return new CraftingNetwork(mainframe, rack, cc);
    }

    /**
     * Stacks {@code count} more running Crafting Computers over the standard network's one, each at (5,2+i,2) with
     * its rear on an Ethernet cable at (4,2+i,2) that climbs from the network's, for a test that needs more bench
     * recipes than one computer's cards keep.
     */
    public List<CraftingComputerBlockEntity> stackCraftingComputers(final int count) {
        final List<CraftingComputerBlockEntity> computers = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            setBlock(new BlockPos(4, 2 + i, 2), ComputingModule.ETHERNET_CABLE);
            computers.add(placeRunningCraftingComputer(new BlockPos(5, 2 + i, 2)));
        }
        return computers;
    }

    /** Places a crafting computer of {@code parts} with a Crafting Card, installs {@code os} and powers it on. */
    private CraftingComputerBlockEntity placeRunningCrafting(final BlockPos relative, final CraftingParts parts,
                                                             final ResourceLocation os) {
        setBlock(relative, parts.block());
        faceRearTowardCable(relative);
        final CraftingComputerBlockEntity be = blockEntity(relative, CraftingComputerBlockEntity.class);
        final ItemStackHandler hw = be.getHardware();
        hw.setStackInSlot(CraftingComputerBlockEntity.MOTHERBOARD_SLOT, new ItemStack(parts.board()));
        hw.setStackInSlot(CraftingComputerBlockEntity.CPU_SLOT, new ItemStack(parts.cpu()));
        hw.setStackInSlot(CraftingComputerBlockEntity.RAM_SLOTS_START, new ItemStack(parts.ram()));
        hw.setStackInSlot(CraftingComputerBlockEntity.PCIE_SLOTS_START,
                new ItemStack(ComputingModule.CRAFTING_CARD_T2.get()));
        hw.setStackInSlot(CraftingComputerBlockEntity.PCIE_SLOTS_START + 1, new ItemStack(parts.gpu()));
        hw.setStackInSlot(CraftingComputerBlockEntity.PSU_SLOT, new ItemStack(parts.psu()));
        hw.setStackInSlot(CraftingComputerBlockEntity.DISK_SLOTS_START,
                new ItemStack(ComputingModule.disk(parts.disk(), DiskSize.GB_500)));
        be.installOs(os);
        be.togglePower();
        return be;
    }

    /* What one crafting computer is built from, apart from the Crafting Card every one of them carries. */
    private record CraftingParts(Block block, ItemLike board, ItemLike cpu, ItemLike ram, ItemLike gpu, ItemLike psu,
                                 StorageTier disk) {
    }
}
