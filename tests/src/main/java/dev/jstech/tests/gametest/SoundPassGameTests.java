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
import dev.jstech.computers.audio.ComputingSounds;
import dev.jstech.computers.audio.ProgramCue;
import dev.jstech.computers.audio.SystemSound;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.computers.hardware.DiskSize;
import dev.jstech.computers.hardware.StorageTier;
import dev.jstech.computers.operation.payload.MachineSoundPayload;
import dev.jstech.computers.os.install.SetupJob;
import dev.jstech.computers.os.media.MediaItem;
import dev.jstech.computers.os.media.MediaKind;
import dev.jstech.computers.os.media.MediaReaderBlockEntity;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.program.cli.CliCommands;
import dev.jstech.core.audio.LoopRequest;
import dev.jstech.core.audio.SoundContext;
import dev.jstech.core.audio.SoundCue;
import dev.jstech.core.audio.SoundKey;
import dev.jstech.core.text.Text;
import dev.jstech.core.tier.HardwareEra;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestWorldBuilder;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * The sounds the rest of the sound pass brought: each desktop's own chimes picked by its system, its desktop and its
 * age; what a screen may ask a machine to play; the self-test that fails, the bell and where it rings, a server on
 * its rails, a picture tube going dark, a hard drive at work, a disc turning in its drive and a device plugged in.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class SoundPassGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos WHERE = new BlockPos(2, 2, 2);
    private static final BlockPos BESIDE = new BlockPos(3, 2, 2);
    private static final BlockPos BEHIND = new BlockPos(2, 2, 3);
    private static final int SETTLE = 3;
    /** Long enough for a monitor or a drive beside a computer to link to it. */
    private static final int LINKED = 5;
    /** Past the end of a hard drive's spin-up, when it is heard turning. */
    private static final int SPUN_UP = 180;
    /** Past the moment the heads stop being heard seeking after the last work. */
    private static final int SEEK_OVER = 40;
    /** On a free side of the crafting network's computer, where a drive links to it. */
    private static final BlockPos DRIVE_BESIDE_COMPUTER = new BlockPos(5, 2, 3);
    private static final ResourceLocation LINUX = ResourceLocation.fromNamespaceAndPath("jsc", "ubuntu");
    private static final ResourceLocation CD_PROGRAM = ResourceLocation.fromNamespaceAndPath("jsc", "minesweeper");

    private SoundPassGameTests() {
    }

    @GameTest(template = ARENA)
    public static void systemCues_pickEachDesktopsOwnSoundsByItsAge(final GameTestHelper helper) {
        picks(helper, ComputingSounds.SYSTEM_STARTUP, on("jsc:ubuntu", "jsc:kde_plasma", HardwareEra.LEGACY),
                ComputingSounds.KDE_LEGACY_STARTUP);
        picks(helper, ComputingSounds.SYSTEM_STARTUP, on("jsc:ubuntu", "jsc:kde_plasma", HardwareEra.STANDARD),
                ComputingSounds.KDE_STARTUP);
        picks(helper, ComputingSounds.SYSTEM_STARTUP, on("jsc:debian", "jsc:gnome", HardwareEra.LEGACY),
                ComputingSounds.GNOME_LEGACY_STARTUP);
        picks(helper, ComputingSounds.SYSTEM_STARTUP, on("jsc:debian", "jsc:gnome", HardwareEra.STANDARD), null);
        picks(helper, ComputingSounds.SYSTEM_ERROR, on("jsc:arch", "jsc:cinnamon", HardwareEra.STANDARD),
                ComputingSounds.GNOME_ERROR);
        picks(helper, ComputingSounds.SYSTEM_NOTIFY, on("jsc:frames_95", "jsc:frames_95", HardwareEra.LEGACY),
                ComputingSounds.FRAMES_NOTIFY);
        picks(helper, ComputingSounds.SYSTEM_DEVICE_CONNECT, on("jsc:frames_95", "jsc:frames_95", HardwareEra.LEGACY),
                null);
        picks(helper, ComputingSounds.SYSTEM_DEVICE_CONNECT, on("jsc:frames_xp", "jsc:frames_xp", HardwareEra.LEGACY),
                ComputingSounds.FRAMES_DEVICE_CONNECT);
        picks(helper, ComputingSounds.SYSTEM_DEVICE_CONNECT, on("jsc:fedora", "jsc:kde_plasma", HardwareEra.LEGACY),
                null);
        picks(helper, ComputingSounds.SYSTEM_DEVICE_DISCONNECT,
                on("jsc:fedora", "jsc:kde_plasma", HardwareEra.STANDARD), ComputingSounds.KDE_DEVICE_DISCONNECT);
        picks(helper, ComputingSounds.SYSTEM_BEEP, on("jsc:unix", "jsc:cde", HardwareEra.VINTAGE), null);
        picks(helper, ComputingSounds.SYSTEM_BEEP, on("jsc:debian", "jsc:gnome", HardwareEra.LEGACY),
                ComputingSounds.GNOME_BELL);
        picks(helper, ComputingSounds.SYSTEM_BEEP, on("jsc:frames_11", "jsc:frames_11", HardwareEra.STANDARD),
                ComputingSounds.FRAMES_BEEP);
        helper.succeed();
    }

    /*
     * Frames 7 and 10 start with their own chimes and go down with the same ones; the rest is the family's, with the
     * error each one shared: 7 kept XP's, 10 has 11's.
     */
    @GameTest(template = ARENA)
    public static void systemCues_giveFrames7And10TheirOwnChimesAndTheFamilysRest(final GameTestHelper helper) {
        final SoundContext seven = on("jsc:frames_7", "jsc:frames_7", HardwareEra.TRANSITION);
        final SoundContext ten = on("jsc:frames_10", "jsc:frames_10", HardwareEra.STANDARD);
        picks(helper, ComputingSounds.SYSTEM_STARTUP, seven, ComputingSounds.FRAMES_7_STARTUP);
        picks(helper, ComputingSounds.SYSTEM_SHUTDOWN, seven, ComputingSounds.FRAMES_7_STARTUP);
        picks(helper, ComputingSounds.SYSTEM_STARTUP, ten, ComputingSounds.FRAMES_10_STARTUP);
        picks(helper, ComputingSounds.SYSTEM_SHUTDOWN, ten, ComputingSounds.FRAMES_10_STARTUP);
        picks(helper, ComputingSounds.SYSTEM_ERROR, seven, ComputingSounds.FRAMES_XP_ERROR);
        picks(helper, ComputingSounds.SYSTEM_ERROR, ten, ComputingSounds.FRAMES_11_ERROR);
        for (final SoundContext context : List.of(seven, ten)) {
            picks(helper, ComputingSounds.SYSTEM_NOTIFY, context, ComputingSounds.FRAMES_NOTIFY);
            picks(helper, ComputingSounds.SYSTEM_BEEP, context, ComputingSounds.FRAMES_BEEP);
            picks(helper, ComputingSounds.SYSTEM_DEVICE_CONNECT, context, ComputingSounds.FRAMES_DEVICE_CONNECT);
            picks(helper, ComputingSounds.SYSTEM_DEVICE_DISCONNECT, context,
                    ComputingSounds.FRAMES_DEVICE_DISCONNECT);
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void machineSoundPayload_carriesOnlyWhatAScreenMayRaise(final GameTestHelper helper) {
        final BlockPos host = new BlockPos(1, 2, 3);
        helper.assertTrue(new MachineSoundPayload(host, SystemSound.NOTIFY).cue() == SystemSound.NOTIFY,
                "a desktop raises its notice");
        helper.assertTrue(new MachineSoundPayload(host, SystemSound.BEEP).cue() == SystemSound.BEEP,
                "and rings its bell");
        helper.assertTrue(new MachineSoundPayload(host, ProgramCue.MINESWEEPER_WIN).cue() == ProgramCue.MINESWEEPER_WIN,
                "a program of the series raises its own sounds");
        helper.assertTrue(new MachineSoundPayload(host, SystemSound.STARTUP).cue() == null,
                "a screen cannot play the machine coming up");
        helper.assertTrue(new MachineSoundPayload(host, SystemSound.DEVICE_CONNECT).cue() == null,
                "nor a device the machine never saw");
        helper.assertTrue(new MachineSoundPayload(host, "no_such_sound").cue() == null, "nor a name it made up");
        final MachineSoundPayload sent = new MachineSoundPayload(host, ProgramCue.MINESWEEPER_EXPLODE);
        final RegistryFriendlyByteBuf wire = new RegistryFriendlyByteBuf(Unpooled.buffer(),
                helper.getLevel().registryAccess());
        try {
            MachineSoundPayload.STREAM_CODEC.encode(wire, sent);
            helper.assertTrue(sent.equals(MachineSoundPayload.STREAM_CODEC.decode(wire)),
                    "the machine and the sound arrive as they were sent");
        } finally {
            wire.release();
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void legacyPc_switchedOnWithoutMemory_beepsItsSelfTestFailing(final GameTestHelper helper) {
        final Heard heard = Heard.at(helper, WHERE);
        final PersonalComputerBlockEntity pc = withoutMemory(helper, ComputingModule.LEGACY_PERSONAL_COMPUTER.get(),
                new ItemStack(HardwareItems.MOTHERBOARD_ATX_LEGACY_LGA775.get()),
                new ItemStack(HardwareItems.CPU_INTEGRA_PENTIX_4_560.get()),
                new ItemStack(HardwareItems.PSU_500B.get()));
        pc.togglePower();
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    heard.stop();
                    helper.assertFalse(pc.isRunning(), "parts that do not make a computer do not run");
                    heard.assertPlayed(helper, ComputingSounds.POST_FAIL);
                    heard.assertNotPlayed(helper, ComputingSounds.POST_BEEP);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void standardPc_switchedOnWithoutMemory_failsWithoutABeep(final GameTestHelper helper) {
        final Heard heard = Heard.at(helper, WHERE);
        final PersonalComputerBlockEntity pc = withoutMemory(helper, ComputingModule.PERSONAL_COMPUTER.get(),
                new ItemStack(HardwareItems.MOTHERBOARD_ATX_STANDARD_LGA1150.get()),
                new ItemStack(HardwareItems.CPU_INTEGRA_CENTRO_C7_4790K.get()),
                new ItemStack(ComputingModule.PSU_650G.get()));
        pc.togglePower();
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    heard.stop();
                    heard.assertNotPlayed(helper, ComputingSounds.POST_FAIL);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void bell_ringsTheCaseWithNoMonitorAndTheSystemsBellWithOne(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final Heard heard = Heard.at(helper, WHERE);
        final PersonalComputerBlockEntity pc = world.placeRunningPersonalComputer(WHERE);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(pc.bootedDesktopId() != null, "waiting for the desktop"))
                .thenExecute(() -> {
                    pc.bell(helper.getLevel());
                    helper.assertTrue(heard.count(ComputingSounds.POST_BEEP) == 1,
                            "with no monitor to play through, the bell is the case's speaker; heard " + heard.sounds);
                    world.placeMonitor(BESIDE, Direction.EAST);
                })
                .thenExecuteAfter(LINKED, () -> {
                    pc.bell(helper.getLevel());
                    heard.stop();
                    helper.assertTrue(heard.count(ComputingSounds.POST_BEEP) == 1,
                            "with a monitor the system rings its own bell, not the case's");
                    helper.assertTrue(pc.voices().held(helper.getLevel(), false) > 0,
                            "and it rings through the sound on the board");
                    helper.assertTrue("jsc:frames_11".equals(
                                    pc.audioHost().soundContext().get(ComputingSounds.DESKTOP)),
                            "the desktop it booted is part of what picks the sound; got "
                                    + pc.audioHost().soundContext());
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void unixShell_echoingTheBellCharacter_ringsTheBell(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final Heard heard = Heard.at(helper, WHERE);
        final PersonalComputerBlockEntity pc = world.placeRunningPersonalComputer(WHERE, LINUX);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertFalse(pc.needsPost(), "waiting for the self-test"))
                .thenExecute(() -> {
                    final ServerCliComputer cli = new ServerCliComputer(pc, helper.getLevel());
                    CliCommands.shellFor(cli, 80).run("echo one two", cli);
                    helper.assertTrue(heard.count(ComputingSounds.POST_BEEP) == 0, "plain words ring nothing");
                    CliCommands.shellFor(cli, 80).run("echo -e \"one\\atwo\"", cli);
                    heard.stop();
                    helper.assertTrue(heard.count(ComputingSounds.POST_BEEP) == 1,
                            "the bell character rings the bell, here the case's; heard " + heard.sounds);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void rack_unitsGoingInAndOut_slideOnTheirRails_andAFallingRackIsQuiet(
            final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final Heard heard = Heard.at(helper, WHERE);
        final ServerRackBlockEntity rack = world.placeSeededRack(WHERE);
        final ItemStackHandler servers = rack.getServers();
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    helper.assertTrue(heard.count(ComputingSounds.RACK_SLIDE_IN) == 1,
                            "a server going in is heard on its rails; heard " + heard.sounds);
                    final ItemStack server = servers.extractItem(0, 1, false);
                    helper.assertTrue(heard.count(ComputingSounds.RACK_SLIDE_OUT) == 1, "and coming out");
                    servers.setStackInSlot(0, server);
                    rack.quietly(() -> servers.setStackInSlot(0, ItemStack.EMPTY));
                    heard.stop();
                    helper.assertTrue(heard.count(ComputingSounds.RACK_SLIDE_IN) == 2
                                    && heard.count(ComputingSounds.RACK_SLIDE_OUT) == 1,
                            "a rack coming down drops its servers without sliding them out; heard " + heard.sounds);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void crtMonitor_goingDark_isHeard(final GameTestHelper helper) {
        final Heard heard = Heard.at(helper, BESIDE);
        final PersonalComputerBlockEntity pc = legacyWithHardDrive(helper);
        helper.setBlock(BESIDE, ComputingModule.LEGACY_MONITOR.get());
        pc.togglePower();
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(heard.played(ComputingSounds.MONITOR_POWER_ON),
                        "waiting for the monitor to come on"))
                .thenExecute(pc::togglePower)
                .thenExecuteAfter(SETTLE, () -> {
                    heard.stop();
                    heard.assertPlayed(helper, ComputingSounds.MONITOR_POWER_OFF);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void flatPanel_goingDark_isSilent(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final Heard heard = Heard.at(helper, BESIDE);
        final PersonalComputerBlockEntity pc = world.placeRunningPersonalComputer(WHERE);
        world.placeMonitor(BESIDE, Direction.EAST);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(heard.played(ComputingSounds.MONITOR_POWER_ON),
                        "waiting for the monitor to come on"))
                .thenExecute(pc::togglePower)
                .thenExecuteAfter(SETTLE, () -> {
                    heard.stop();
                    heard.assertNotPlayed(helper, ComputingSounds.MONITOR_POWER_OFF);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void hardDrive_atWork_isHeardSeekingForAMoment(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = legacyWithHardDrive(helper);
        pc.togglePower();
        helper.startSequence()
                .thenExecuteAfter(SPUN_UP, () -> {
                    helper.assertTrue(turning(helper, pc), "the drive has spun up");
                    helper.assertFalse(seeking(helper, pc), "a drive nobody works is only heard turning");
                    pc.diskWorked(helper.getLevel());
                })
                .thenExecuteAfter(1, () -> helper.assertTrue(seeking(helper, pc),
                        "a drive at work is heard seeking"))
                .thenExecuteAfter(SEEK_OVER, () -> helper.assertFalse(seeking(helper, pc),
                        "and falls back to turning a moment after the work"))
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void cdDrive_programInstallingFromItsDisc_isHeardSpinning(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final TestWorldBuilder.CraftingNetwork net = world.buildCraftingNetwork();
        world.setBlock(DRIVE_BESIDE_COMPUTER, ComputingModule.CD_DRIVE.get());
        final MediaReaderBlockEntity drive = world.blockEntity(DRIVE_BESIDE_COMPUTER, MediaReaderBlockEntity.class);
        final ItemStack disc = new ItemStack(ComputingModule.CD_ROM.get());
        MediaItem.setKind(disc, MediaKind.PROGRAM_INSTALL);
        MediaItem.setPayload(disc, CD_PROGRAM);
        drive.mediaSlot().setStackInSlot(0, disc);
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 3, () -> {
                    helper.assertTrue(drive.loops().isEmpty(), "a drive nobody installs from is quiet");
                    net.cc().console().beginSetup(new SetupJob(CD_PROGRAM.toString(), "Minesweeper", "Midsoft",
                            1, Text.literal("CD"), false, 400));
                })
                .thenExecuteAfter(SETTLE, () -> {
                    final List<LoopRequest> loops = drive.loops();
                    helper.assertTrue(loops.size() == 1 && loops.getFirst().sound() == ComputingSounds.OPTICAL_READ,
                            "a disc is heard turning while the program on it installs; got " + loops);
                    net.cc().console().clearSetup();
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void usbDrive_intoARunningDesktop_isADeviceConnecting(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final PersonalComputerBlockEntity pc = world.placeRunningPersonalComputer(WHERE);
        world.placeMonitor(BESIDE, Direction.EAST);
        world.setBlock(BEHIND, ComputingModule.DOCK_STATION.get());
        final MediaReaderBlockEntity dock = world.blockEntity(BEHIND, MediaReaderBlockEntity.class);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(pc.bootedDesktopId() != null
                                && pc.getBlockPos().equals(dock.ownerPos()),
                        "waiting for the desktop and the dock's link"))
                .thenExecute(() -> {
                    helper.assertTrue(pc.voices().held(helper.getLevel(), false) == 0,
                            "nothing rings before the drive goes in");
                    dock.insertMedia(new ItemStack(ComputingModule.USB_FLASH_DRIVE.get()));
                    helper.assertTrue(pc.voices().held(helper.getLevel(), false) > 0,
                            "the system sounds the device plugged in through its sound");
                })
                .thenSucceed();
    }

    /* The context a machine running that system and desktop, of that age, gives its sounds. */
    private static SoundContext on(final String system, final String desktop, final HardwareEra era) {
        return SoundContext.EMPTY.with(ComputingSounds.SYSTEM, system).with(ComputingSounds.DESKTOP, desktop)
                .with(SoundContext.ERA, era.serializedName());
    }

    private static void picks(final GameTestHelper helper, final SoundCue cue, final SoundContext context,
                              @Nullable final SoundKey expected) {
        final String picked = cue.defaults().pick(context);
        final String wanted = expected == null ? null : expected.id().toString();
        helper.assertTrue(wanted == null ? picked == null : wanted.equals(picked),
                cue.id() + " in " + context + " plays " + wanted + "; got " + picked);
    }

    private static PersonalComputerBlockEntity withoutMemory(final GameTestHelper helper, final Block block,
                                                             final ItemStack board, final ItemStack cpu,
                                                             final ItemStack psu) {
        helper.setBlock(WHERE, block);
        if (!(helper.getBlockEntity(WHERE) instanceof PersonalComputerBlockEntity pc)) {
            throw new IllegalStateException("no personal computer at " + WHERE);
        }
        final ItemStackHandler hardware = pc.getHardware();
        hardware.setStackInSlot(PersonalComputerBlockEntity.MOTHERBOARD_SLOT, board);
        hardware.setStackInSlot(PersonalComputerBlockEntity.CPU_SLOT, cpu);
        hardware.setStackInSlot(PersonalComputerBlockEntity.PSU_SLOT, psu);
        return pc;
    }

    private static PersonalComputerBlockEntity legacyWithHardDrive(final GameTestHelper helper) {
        helper.setBlock(WHERE, ComputingModule.LEGACY_PERSONAL_COMPUTER.get());
        if (!(helper.getBlockEntity(WHERE) instanceof PersonalComputerBlockEntity pc)) {
            throw new IllegalStateException("no personal computer at " + WHERE);
        }
        final ItemStackHandler hardware = pc.getHardware();
        hardware.setStackInSlot(PersonalComputerBlockEntity.MOTHERBOARD_SLOT,
                new ItemStack(HardwareItems.MOTHERBOARD_ATX_LEGACY_LGA775.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.CPU_SLOT,
                new ItemStack(HardwareItems.CPU_INTEGRA_PENTIX_4_560.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.RAM_SLOTS_START,
                new ItemStack(HardwareItems.RAM_DDR_1024.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.PSU_SLOT, new ItemStack(HardwareItems.PSU_500B.get()));
        // A graphics card, for the video output a monitor beside it takes.
        hardware.setStackInSlot(PersonalComputerBlockEntity.GPU_SLOTS_START,
                new ItemStack(HardwareItems.GPU_VERTEX_6600_GT.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.DISK_SLOTS_START,
                new ItemStack(ComputingModule.disk(StorageTier.HDD, DiskSize.GB_500)));
        return pc;
    }

    /** Whether the client is told the machine's hard drive is turning, read from what it is sent. */
    private static boolean turning(final GameTestHelper helper, final PersonalComputerBlockEntity pc) {
        return pc.getUpdateTag(helper.getLevel().registryAccess()).getBoolean("DiskTurning");
    }

    /** Whether the client is told the machine's hard drive is seeking, read from what it is sent. */
    private static boolean seeking(final GameTestHelper helper, final PersonalComputerBlockEntity pc) {
        return pc.getUpdateTag(helper.getLevel().registryAccess()).getBoolean("DiskSeeking");
    }

    /** The sounds the server plays at one block, heard from the moment it is made until it is stopped. */
    private static final class Heard implements Consumer<PlayLevelSoundEvent.AtPosition> {

        /** Longer than any test of this class may run, so a listener left behind by a failed test lets go. */
        private static final long LIFETIME_TICKS = 500;

        private final Vec3 at;
        private final ServerLevel level;
        private final long born;
        private final List<ResourceLocation> sounds = new ArrayList<>();

        private Heard(final Vec3 at, final ServerLevel level) {
            this.at = at;
            this.level = level;
            this.born = level.getGameTime();
        }

        static Heard at(final GameTestHelper helper, final BlockPos local) {
            final Heard heard = new Heard(Vec3.atCenterOf(helper.absolutePos(local)), helper.getLevel());
            NeoForge.EVENT_BUS.addListener(heard);
            return heard;
        }

        @Override
        public void accept(final PlayLevelSoundEvent.AtPosition event) {
            // An assertion that fails before stop() would leave this listener on the bus for the rest of the run.
            if (level.getGameTime() - born > LIFETIME_TICKS) {
                stop();
                return;
            }
            if (event.getSound() != null && event.getPosition().distanceToSqr(at) < 0.01) {
                sounds.add(event.getSound().value().getLocation());
            }
        }

        void stop() {
            NeoForge.EVENT_BUS.unregister(this);
        }

        boolean played(final SoundKey sound) {
            return sounds.contains(sound.id());
        }

        int count(final SoundKey sound) {
            return (int) sounds.stream().filter(sound.id()::equals).count();
        }

        void assertPlayed(final GameTestHelper helper, final SoundKey sound) {
            helper.assertTrue(played(sound), sound.id() + " was heard; heard instead: " + sounds);
        }

        void assertNotPlayed(final GameTestHelper helper, final SoundKey sound) {
            helper.assertFalse(played(sound), sound.id() + " was not heard; heard: " + sounds);
        }
    }
}
