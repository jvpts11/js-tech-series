/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.JsComputers;
import dev.jstech.computers.audio.ComputingSounds;
import dev.jstech.computers.audio.SoundOutput;
import dev.jstech.computers.blockentity.AbstractComputerBlockEntity;
import dev.jstech.computers.blockentity.CraftingComputerBlockEntity;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.client.os.DesktopScreen;
import dev.jstech.computers.client.os.DesktopWindow;
import dev.jstech.computers.client.os.SettingsApp;
import dev.jstech.computers.gui.layout.CdeFrontPanelLayout;
import dev.jstech.computers.hardware.DiskSize;
import dev.jstech.computers.hardware.StorageTier;
import dev.jstech.computers.program.ComputerSettings;
import dev.jstech.core.client.audio.AudioMixer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * The volume control every desktop opens from the speaker on its panel, each in its own form, and what it turns: the
 * machine's volume, its mute and where its sound goes. The newest Frames is gone through end to end, its menu and
 * the Sound settings it leads to included; every other form is opened, turned once and pictured; and CDE, which has
 * no panel speaker, sets the same three from its Style Manager.
 */
public final class VolumeClientTests {

    private static final int SETTLE = 4;
    private static final int SCREEN_WAIT = 80;
    private static final int BOOT_WAIT = 400;
    private static final long RECENT_MILLIS = 60_000L;

    private static final BlockPos COMPUTER = new BlockPos(5, 2, 2);
    private static final BlockPos MONITOR = new BlockPos(6, 2, 2);
    private static final BlockPos LEFT_SPEAKER = new BlockPos(5, 2, 1);
    private static final BlockPos RIGHT_SPEAKER = new BlockPos(5, 2, 3);
    private static final BlockPos PLAYER_AT_MONITOR = new BlockPos(8, 2, 2);
    private static final String SETTINGS = "Settings";
    private static final String FRAMES_11_CHIME = ComputingSounds.FRAMES_11_STARTUP.id().toString();

    private VolumeClientTests() {
    }

    @ClientTest(timeoutTicks = 2400)
    public static void frames11_turnsItsSoundFromThePanelAndLeadsToTheSoundSettings(final ClientTestContext ctx) {
        final AbstractComputerBlockEntity[] machine = new AbstractComputerBlockEntity[1];
        booted(ctx, machine, "frames_11", null, false, true)
                .then(SETTLE, () -> clickAt(ctx, desktop(ctx).speakerPoint()))
                .thenAssert(2, () -> desktop(ctx).volumeControlOpen()
                        && desktop(ctx).volumeLook().equals("QUICK"), "the speaker opens Frames 11's quick settings")
                .then(2, () -> clickAt(ctx, desktop(ctx).volumePoint("chevron", 0)))
                .thenScreenshot(4, "frames11-quick-settings")
                .then(2, () -> clickAt(ctx, desktop(ctx).volumePoint("output", 0)))
                .thenWaitUntilServer(level -> settings(machine).soundOutput() == SoundOutput.MONITOR, SCREEN_WAIT,
                        "choosing Monitor sends the machine's sound out of its monitor alone",
                        level -> "output " + settings(machine).soundOutput())
                .then(2, () -> clickAt(ctx, desktop(ctx).volumePoint("track", 40)))
                .thenWaitUntilServer(level -> Math.abs(settings(machine).volume() - 40) <= 3, SCREEN_WAIT,
                        "a click on the slider sets the volume where it lands",
                        level -> "volume " + settings(machine).volume())
                .then(2, () -> clickAt(ctx, desktop(ctx).volumePoint("mute", 0)))
                .thenWaitUntilServer(level -> settings(machine).muted(), SCREEN_WAIT,
                        "a click on the speaker beside the slider mutes the system", level -> "not muted")
                .thenAssert(2, () -> desktop(ctx).mutedShown(), "and the panel's speaker shows it muted")
                .then(0, () -> ctx.key(GLFW.GLFW_KEY_ESCAPE))
                .thenAssert(2, () -> !desktop(ctx).volumeControlOpen(), "Escape puts the control away")
                .then(SETTLE, () -> rightClickAt(ctx, desktop(ctx).speakerPoint()))
                .thenAssert(2, () -> desktop(ctx).volumeMenuOpen(), "the right button opens the speaker's menu")
                .then(2, () -> clickAt(ctx, desktop(ctx).volumePoint("entry", 0)))
                .thenWaitUntil(() -> settingsApp(ctx) != null && settingsApp(ctx).page() == SettingsApp.PAGE_SOUND
                                && settingsApp(ctx).testSoundCenter()[0] > 0, SCREEN_WAIT,
                        "its entry opens the Settings window on the Sound page")
                .thenScreenshot(4, "settings-sound-page")
                .thenServer(0, level -> settings(machine).setMuted(false))
                .then(SETTLE, () -> ctx.clickDesktop(settingsApp(ctx).testSoundCenter()))
                .thenWaitUntil(() -> AudioMixer.recent(RECENT_MILLIS).contains(FRAMES_11_CHIME), SCREEN_WAIT,
                        "Test plays the system's chime", () -> "heard " + AudioMixer.recent(RECENT_MILLIS));
    }

    @ClientTest(timeoutTicks = 2400)
    public static void frames95_opensItsUprightPopup(final ClientTestContext ctx) {
        opensItsOwn(ctx, "frames_95", null, false, "CLASSIC", "frames95-volume");
    }

    @ClientTest(timeoutTicks = 2400)
    public static void framesXp_opensItsUprightPopup(final ClientTestContext ctx) {
        opensItsOwn(ctx, "frames_xp", null, false, "CLASSIC", "xp-volume");
    }

    @ClientTest(timeoutTicks = 2400)
    public static void kde_opensItsAudioVolumeApplet(final ClientTestContext ctx) {
        opensItsOwn(ctx, "ubuntu", "jsc:kde_plasma", false, "PLASMA", "kde-volume");
    }

    @ClientTest(timeoutTicks = 2400)
    public static void gnome_dropsItsSystemMenuFromTheTopBar(final ClientTestContext ctx) {
        opensItsOwn(ctx, "ubuntu", "jsc:gnome", false, "SYSTEM_MENU", "gnome-volume");
    }

    @ClientTest(timeoutTicks = 2400)
    public static void cinnamon_opensItsSoundApplet(final ClientTestContext ctx) {
        opensItsOwn(ctx, "ubuntu", "jsc:cinnamon", false, "APPLET", "cinnamon-volume");
    }

    @ClientTest(timeoutTicks = 2400)
    public static void period_opensTheMixerPopupOnALegacyMachine(final ClientTestContext ctx) {
        opensItsOwn(ctx, "ubuntu", "jsc:kde_plasma", true, "PERIOD", "period-volume");
    }

    /** CDE keeps no speaker on its panel: its Style Manager has an Audio page that sets the same three. */
    @ClientTest(timeoutTicks = 3600)
    public static void cde_setsTheSoundFromItsStyleManager(final ClientTestContext ctx) {
        final AbstractComputerBlockEntity[] machine = new AbstractComputerBlockEntity[1];
        ctx.thenBuild(0, world -> {
                    world.setBlock(COMPUTER, ComputingModule.MAINFRAME.get());
                    final MainframeBlockEntity mainframe = world.blockEntity(COMPUTER, MainframeBlockEntity.class);
                    final ItemStackHandler inv = mainframe.getInventory();
                    inv.setStackInSlot(MainframeBlockEntity.MOTHERBOARD_SLOT,
                            new ItemStack(ComputingModule.MOTHERBOARD_MTX_P.get()));
                    inv.setStackInSlot(MainframeBlockEntity.CPU_SLOTS_START,
                            new ItemStack(ComputingModule.CPU_SERVO_2620.get()));
                    inv.setStackInSlot(MainframeBlockEntity.RAM_SLOTS_START,
                            new ItemStack(ComputingModule.RAM_DDR3_8192.get()));
                    inv.setStackInSlot(MainframeBlockEntity.PSU_SLOT, new ItemStack(ComputingModule.PSU_650G.get()));
                    inv.setStackInSlot(MainframeBlockEntity.GPU_SLOTS_START,
                            new ItemStack(ComputingModule.GPU_HD_7970.get()));
                    inv.setStackInSlot(MainframeBlockEntity.DISK_SLOTS_START,
                            new ItemStack(ComputingModule.disk(StorageTier.SSD, DiskSize.GB_500)));
                    mainframe.installOs(jsc("unix"));
                    mainframe.console().install(jsc("cde").toString());
                    mainframe.togglePower();
                    world.placeMonitor(MONITOR, Direction.EAST);
                    machine[0] = mainframe;
                })
                .thenTeleport(SETTLE, PLAYER_AT_MONITOR, Direction.WEST)
                .thenRightClick(SETTLE, MONITOR)
                .thenAwaitScreen(DesktopScreen.class, BOOT_WAIT)
                .then(SETTLE, () -> clickAt(ctx, desktop(ctx).frontPanelPoint(CdeFrontPanelLayout.Control.STYLE)))
                .thenWaitUntil(() -> desktop(ctx).styleManagerPagePoint("Audio") != null, SCREEN_WAIT,
                        "the Style Manager to open with an Audio page")
                .then(SETTLE, () -> clickAt(ctx, desktop(ctx).styleManagerPagePoint("Audio")))
                .thenWaitUntil(() -> desktop(ctx).styleAudioPoint("ok", 0) != null, SCREEN_WAIT,
                        "the Audio page to open")
                .then(2, () -> clickAt(ctx, desktop(ctx).styleAudioPoint("scale", 30)))
                .then(2, () -> clickAt(ctx, desktop(ctx).styleAudioPoint("speakers", 0)))
                .then(2, () -> clickAt(ctx, desktop(ctx).styleAudioPoint("mute", 0)))
                .thenScreenshot(4, "cde-style-audio")
                .thenAssert(0, () -> !settings(machine).muted(), "nothing is sent before OK")
                .then(2, () -> clickAt(ctx, desktop(ctx).styleAudioPoint("ok", 0)))
                .thenWaitUntilServer(level -> settings(machine).muted()
                                && settings(machine).soundOutput() == SoundOutput.MONITOR
                                && Math.abs(settings(machine).volume() - 30) <= 3, SCREEN_WAIT,
                        "OK sets the volume, the mute and the monitor alone as the page stood",
                        level -> "volume " + settings(machine).volume() + ", muted " + settings(machine).muted()
                                + ", output " + settings(machine).soundOutput());
    }

    /* One desktop's own control: it opens from the speaker, a click on its slider sets the volume, and it is drawn. */
    private static void opensItsOwn(final ClientTestContext ctx, final String os, @Nullable final String desktopPackage,
                                    final boolean period, final String look, final String picture) {
        final AbstractComputerBlockEntity[] machine = new AbstractComputerBlockEntity[1];
        booted(ctx, machine, os, desktopPackage, period, false)
                .then(SETTLE, () -> clickAt(ctx, desktop(ctx).speakerPoint()))
                .thenAssert(2, () -> desktop(ctx).volumeControlOpen() && desktop(ctx).volumeLook().equals(look),
                        "the speaker opens the desktop's own control, " + look)
                .then(2, () -> clickAt(ctx, desktop(ctx).volumePoint("track", 30)))
                .thenWaitUntilServer(level -> Math.abs(settings(machine).volume() - 30) <= 3, SCREEN_WAIT,
                        "a click on its slider sets the volume", level -> "volume " + settings(machine).volume())
                // The game's own toasts stand in the top right corner, over GNOME's menu: they go before the picture.
                .then(0, () -> ctx.mc().getToasts().clear())
                .thenScreenshot(4, picture)
                .then(0, () -> ctx.key(GLFW.GLFW_KEY_ESCAPE))
                .thenAssert(2, () -> !desktop(ctx).volumeControlOpen(), "Escape puts it away");
    }

    /**
     * A crafting computer at its desktop: {@code os} installed, with {@code desktopPackage} on top of it for a Linux,
     * a Legacy one when {@code period}, and two speakers beside it when {@code speakers}.
     */
    private static ClientTestContext booted(final ClientTestContext ctx, final AbstractComputerBlockEntity[] machine,
                                            final String os, @Nullable final String desktopPackage,
                                            final boolean period, final boolean speakers) {
        return ctx.thenBuild(0, world -> {
                    final CraftingComputerBlockEntity computer = period
                            ? world.placeRunningLegacyCraftingComputer(COMPUTER)
                            : world.placeRunningCraftingComputer(COMPUTER);
                    computer.installOs(jsc(os));
                    if (desktopPackage != null) {
                        computer.console().install(desktopPackage);
                    }
                    world.placeMonitor(MONITOR, Direction.EAST);
                    if (speakers) {
                        world.setBlock(LEFT_SPEAKER, ComputingModule.SPEAKER.get());
                        world.setBlock(RIGHT_SPEAKER, ComputingModule.SPEAKER.get());
                    }
                    machine[0] = computer;
                })
                .thenTeleport(SETTLE, PLAYER_AT_MONITOR, Direction.WEST)
                .thenRightClick(SETTLE, MONITOR)
                .thenAwaitScreen(DesktopScreen.class, BOOT_WAIT)
                .thenWaitUntil(() -> desktop(ctx).volumeShown() == 100, SCREEN_WAIT,
                        "the desktop to know the machine's sound");
    }

    private static ComputerSettings settings(final AbstractComputerBlockEntity[] machine) {
        return machine[0].console().settings();
    }

    @Nullable
    private static SettingsApp settingsApp(final ClientTestContext ctx) {
        final DesktopWindow window = desktop(ctx).windowFor(SETTINGS);
        return window != null && window.app() instanceof SettingsApp app ? app : null;
    }

    private static DesktopScreen desktop(final ClientTestContext ctx) {
        return ctx.screen(DesktopScreen.class);
    }

    private static void clickAt(final ClientTestContext ctx, final int[] at) {
        ctx.click(at[0] + 0.5, at[1] + 0.5);
    }

    private static void rightClickAt(final ClientTestContext ctx, final int[] at) {
        ctx.rightClick(at[0] + 0.5, at[1] + 0.5);
    }

    private static ResourceLocation jsc(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, path);
    }
}
