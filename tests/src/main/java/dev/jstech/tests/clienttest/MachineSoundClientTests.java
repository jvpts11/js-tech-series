/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.HardwareItems;
import dev.jstech.computers.audio.ComputingSounds;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.computers.blockentity.SpeakerBlockEntity;
import dev.jstech.computers.client.SpeakerScreen;
import dev.jstech.computers.gui.layout.SpeakerLayout;
import dev.jstech.computers.hardware.DiskSize;
import dev.jstech.computers.hardware.StorageTier;
import dev.jstech.core.audio.SoundKey;
import dev.jstech.core.client.audio.AudioEngine;
import dev.jstech.core.client.audio.AudioMixer;
import dev.jstech.core.client.audio.CapturingAudioSink;
import dev.jstech.core.client.audio.SoundDirector;
import dev.jstech.tests.testkit.TestWorldBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.lwjgl.glfw.GLFW;

/**
 * The sounds of the machines, heard where the player stands: a computer's hard drive turning while it runs and
 * stopping when it goes off, the servers of a rack heard by their fans until five of them run close together, when
 * the room is heard instead, a system's chime out of its monitor when its desktop comes up, and the screen a speaker
 * is named on.
 */
public final class MachineSoundClientTests {

    /** On the ground, as every client test stands: relative height 2 is the first air above it. */
    private static final BlockPos STAND = new BlockPos(0, 2, 0);
    private static final BlockPos COMPUTER = new BlockPos(2, 2, 2);
    private static final BlockPos RACK = new BlockPos(3, 2, 0);
    private static final int SETTLE = 5;
    /** A hard drive's spin-up and the look after it, with room to spare. */
    private static final int SPIN_UP_WAIT = 400;
    private static final int ROOM_SERVERS = 5;
    private static final long RECENT_MILLIS = 60_000L;
    private static final String FRAMES_11_CHIME = ComputingSounds.FRAMES_11_STARTUP.id().toString();

    private MachineSoundClientTests() {
    }

    @ClientTest(timeoutTicks = 700)
    public static void computer_hardDriveTurnsWhileItRuns(final ClientTestContext ctx) {
        final CapturingAudioSink sink = new CapturingAudioSink();
        final PersonalComputerBlockEntity[] pc = new PersonalComputerBlockEntity[1];
        ctx.then(0, () -> AudioEngine.useSink(sink))
                .thenTeleport(SETTLE, STAND, Direction.SOUTH)
                .thenBuild(SETTLE, builder -> pc[0] = legacyWithHardDrive(builder))
                .thenWaitUntil(() -> playing(ComputingSounds.HARD_DRIVE_IDLE), SPIN_UP_WAIT,
                        "the running computer's hard drive is heard turning once it has spun up")
                .thenServer(0, level -> pc[0].togglePower())
                .thenWaitUntil(() -> !playing(ComputingSounds.HARD_DRIVE_IDLE), 100,
                        "and it stops being heard when the computer goes off")
                .then(0, AudioEngine::restoreSink);
    }

    @ClientTest(timeoutTicks = 500)
    public static void rack_serversAreHeardByTheirFansThenAsTheRoom(final ClientTestContext ctx) {
        final CapturingAudioSink sink = new CapturingAudioSink();
        final ServerRackBlockEntity[] rack = new ServerRackBlockEntity[1];
        ctx.then(0, () -> AudioEngine.useSink(sink))
                .thenTeleport(SETTLE, STAND, Direction.SOUTH)
                .thenBuild(SETTLE, builder -> rack[0] = builder.placeSeededRack(RACK))
                .thenWaitUntil(() -> playing(ComputingSounds.SERVER_FAN), 200,
                        "a running server is heard by its fans")
                .thenServer(0, level -> {
                    for (int slot = 1; slot < ROOM_SERVERS; slot++) {
                        TestWorldBuilder.mountDefaultServer(rack[0], slot);
                    }
                })
                .thenWaitUntilServer(level -> runningBays(rack[0], level) == (1 << ROOM_SERVERS) - 1, 200,
                        "the rack tells the client its five servers run",
                        level -> "running bays " + Integer.toBinaryString(runningBays(rack[0], level)))
                .thenWaitUntil(() -> playingRoom() && !playing(ComputingSounds.SERVER_FAN), 200,
                        "five running servers close together are heard as the room, and their fans no more",
                        () -> "playing " + SoundDirector.playing() + ", rooms " + SoundDirector.stats().rooms())
                .then(0, AudioEngine::restoreSink);
    }

    @ClientTest(timeoutTicks = 700)
    public static void standardPc_chimesOutOfItsMonitorWhenItsDesktopComesUp(final ClientTestContext ctx) {
        ctx.thenTeleport(SETTLE, STAND, Direction.SOUTH)
                .thenBuild(SETTLE, builder -> {
                    builder.placeRunningPersonalComputer(COMPUTER);
                    builder.placeMonitor(COMPUTER.east(), Direction.EAST);
                })
                .thenWaitUntil(() -> AudioMixer.recent(RECENT_MILLIS).contains(FRAMES_11_CHIME), 600,
                        "the Frames 11 chime is heard when the desktop comes up",
                        () -> "heard " + AudioMixer.recent(RECENT_MILLIS));
    }

    @ClientTest(timeoutTicks = 600)
    public static void speakerScreens_nameThemAndRefuseANameTaken(final ClientTestContext ctx) {
        final BlockPos north = COMPUTER.north();
        final BlockPos south = COMPUTER.south();
        final SpeakerBlockEntity[] speakers = new SpeakerBlockEntity[2];
        ctx.thenTeleport(SETTLE, STAND, Direction.SOUTH)
                .thenBuild(SETTLE, builder -> {
                    builder.placeRunningPersonalComputer(COMPUTER);
                    builder.placeMonitor(COMPUTER.east(), Direction.EAST);
                    builder.setBlock(north, ComputingModule.SPEAKER.get());
                    builder.setBlock(south, ComputingModule.LEGACY_SPEAKER.get());
                    speakers[0] = builder.blockEntity(north, SpeakerBlockEntity.class);
                    speakers[1] = builder.blockEntity(south, SpeakerBlockEntity.class);
                })
                .thenRightClick(20, north)
                .thenAwaitScreen(SpeakerScreen.class, 40)
                .then(2, () -> ctx.clickGui(SpeakerLayout.NAME_X + 20, SpeakerLayout.NAME_Y + 6))
                .then(2, () -> ctx.type("Desk left"))
                .then(2, () -> ctx.key(GLFW.GLFW_KEY_E, 0))
                .thenAssert(2, () -> ctx.mc().screen instanceof SpeakerScreen,
                        "the inventory key pressed while a name is typed does not close the screen")
                .thenScreenshot(10, "speaker_standard_named")
                .then(0, () -> ctx.player().closeContainer())
                .thenWaitUntilServer(level -> "Desk left".equals(speakers[0].name()), 40,
                        "the Cobble takes the name typed when its screen closes",
                        level -> "named " + speakers[0].name())
                .thenRightClick(5, south)
                .thenAwaitScreen(SpeakerScreen.class, 40)
                .then(2, () -> ctx.clickGui(SpeakerLayout.NAME_X + 20, SpeakerLayout.NAME_Y + 6))
                .then(2, () -> ctx.type("desk left"))
                .thenAssert(20, () -> ctx.screen(SpeakerScreen.class).getMenu().nameClashes(),
                        "the ToneWorks is told its computer already has a speaker by that name")
                .thenScreenshot(2, "speaker_legacy_clash")
                .then(0, () -> ctx.player().closeContainer())
                .thenAssert(10, () -> speakers[1].name().isEmpty(), "and closing it keeps the name it had");
    }

    private static PersonalComputerBlockEntity legacyWithHardDrive(final TestWorldBuilder builder) {
        builder.setBlock(COMPUTER, ComputingModule.LEGACY_PERSONAL_COMPUTER.get());
        final PersonalComputerBlockEntity pc = builder.blockEntity(COMPUTER, PersonalComputerBlockEntity.class);
        final ItemStackHandler hardware = pc.getHardware();
        hardware.setStackInSlot(PersonalComputerBlockEntity.MOTHERBOARD_SLOT,
                new ItemStack(HardwareItems.MOTHERBOARD_ATX_LEGACY_LGA775.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.CPU_SLOT,
                new ItemStack(HardwareItems.CPU_INTEGRA_DUO_E4300.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.RAM_SLOTS_START,
                new ItemStack(HardwareItems.RAM_DDR2_2048.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.PSU_SLOT, new ItemStack(HardwareItems.PSU_500B.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.DISK_SLOTS_START,
                new ItemStack(ComputingModule.disk(StorageTier.HDD, DiskSize.GB_500)));
        pc.togglePower();
        return pc;
    }

    /** The bays the rack tells the client are running, one bit each. */
    private static int runningBays(final ServerRackBlockEntity rack, final ServerLevel level) {
        return rack.getUpdateTag(level.registryAccess()).getInt("RunningBays");
    }

    /** Whether a source of its own plays {@code sound}; a room's bed is named after its first member and is not one. */
    private static boolean playing(final SoundKey sound) {
        final String suffix = "|" + sound.id();
        return SoundDirector.playing().stream().anyMatch(key -> !key.startsWith("bed|") && key.endsWith(suffix));
    }

    private static boolean playingRoom() {
        final String prefix = "bed|" + ComputingSounds.SERVER_ROOM_FIELD.id() + "|";
        return SoundDirector.playing().stream().anyMatch(key -> key.startsWith(prefix));
    }
}
