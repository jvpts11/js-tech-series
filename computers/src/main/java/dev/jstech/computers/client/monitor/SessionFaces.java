/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.monitor;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.client.AbstractComputerScreen;
import dev.jstech.computers.client.BootMenuScreen;
import dev.jstech.computers.client.BootSequenceScreen;
import dev.jstech.computers.client.FirmwareScreen;
import dev.jstech.computers.client.InstallerScreen;
import dev.jstech.computers.client.OsInstallScreen;
import dev.jstech.computers.client.SystemBootScreen;
import dev.jstech.computers.gui.MonitorGlass;
import dev.jstech.computers.menu.MonitorSessionMenu;
import dev.jstech.computers.monitor.IMonitorPicture;
import dev.jstech.computers.operation.payload.OpenBootMenuPayload;
import dev.jstech.computers.operation.payload.OpenComputerUiPayload;
import dev.jstech.computers.operation.payload.OpenInstallDonePayload;
import dev.jstech.computers.operation.payload.OpenInstallerPayload;
import dev.jstech.computers.operation.payload.OpenPostPayload;
import dev.jstech.computers.operation.payload.OpenSystemBootPayload;
import dev.jstech.computers.operation.payload.OsInstallProgressPayload;
import dev.jstech.computers.os.FirmwareKind;
import dev.jstech.core.client.live.LiveGraphics;
import dev.jstech.core.text.GameText;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.jetbrains.annotations.Nullable;

/**
 * The screens drawing monitors' faces in the world while their machines are in a session (testing themselves, at the
 * boot manager, coming up or going down, installing, in the firmware setup): for each monitor, the very screen that
 * session opens for a player at it, built from the same opening, moved on tick by tick as an open one is, and drawn
 * onto the face. So the face and the open screen are one picture, not a picture and its summary.
 */
@EventBusSubscriber(modid = JsComputers.MODID, value = Dist.CLIENT)
public final class SessionFaces {

    /** The faces drawn by screens at once, the least recently seen let go past that. */
    private static final int KEPT = 8;
    /* What a monitor's glass keeps clear round itself, which a screen drawing a face is laid out with. */
    private static final int AROUND_X = MonitorGlass.BESIDE;
    private static final int AROUND_Y = MonitorGlass.ABOVE_AND_BELOW;
    private static final Map<BlockPos, Shown> SHOWN = new LinkedHashMap<>(16, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(final Map.Entry<BlockPos, Shown> eldest) {
            return size() > KEPT;
        }
    };

    private SessionFaces() {
    }

    /** Draws the face of the monitor at {@code monitor}, whose machine is in the session {@code picture} says. */
    public static void paint(final GuiGraphics g, final IMonitorPicture.Session picture, final BlockPos monitor,
                             final float partialTick) {
        Shown shown = SHOWN.get(monitor);
        if (shown == null || !shown.picture().equals(picture)) {
            final AbstractComputerScreen<?> screen = build(picture, monitor);
            if (screen == null) {
                return;
            }
            shown = new Shown(picture, screen);
            SHOWN.put(monitor.immutable(), shown);
        }
        // The screen lays itself out round the glass, as on a game window; the glass falls on the picture.
        if (g instanceof LiveGraphics live) {
            live.shift(-AROUND_X / 2, -AROUND_Y / 2);
        }
        shown.screen().paintFace(g, partialTick);
    }

    /** Lets go of a monitor's face once it shows anything but a session. */
    public static void forget(final BlockPos monitor) {
        SHOWN.remove(monitor);
    }

    /** The screen drawing the face of the monitor at {@code monitor}, or null while it draws no session. */
    @Nullable
    public static AbstractComputerScreen<?> screenOf(final BlockPos monitor) {
        final Shown shown = SHOWN.get(monitor);
        return shown == null ? null : shown.screen();
    }

    @SubscribeEvent
    public static void onClientTick(final ClientTickEvent.Post event) {
        if (SHOWN.isEmpty() || Minecraft.getInstance().isPaused()) {
            return;
        }
        for (final Shown shown : SHOWN.values()) {
            shown.screen().tickFace();
        }
    }

    @SubscribeEvent
    public static void onLeave(final ClientPlayerNetworkEvent.LoggingOut event) {
        SHOWN.clear();
    }

    /* The screen the session opens, handed the opening the picture carries, its clock read off the game's. */
    @Nullable
    private static AbstractComputerScreen<?> build(final IMonitorPicture.Session picture, final BlockPos monitor) {
        final Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return null;
        }
        final long now = mc.level.getGameTime();
        final int left = picture.remaining(now);
        final Inventory inventory = mc.player.getInventory();
        return switch (picture.opening()) {
            case OpenPostPayload post -> {
                BootSequenceScreen.faceWith(FirmwareKind.byId(post.firmwareKind()), post.name(), left, post.halted(),
                        GameText.resolve(post.complaint()), picture.state());
                yield AbstractComputerScreen.face(() -> new BootSequenceScreen(
                        menu(inventory, monitor, post.host(), picture, MonitorSessionMenu.Phase.POST), inventory,
                        Component.empty()), AROUND_X, AROUND_Y);
            }
            case OpenBootMenuPayload menu -> {
                BootMenuScreen.faceWith(menu.menu(), left);
                yield AbstractComputerScreen.face(() -> new BootMenuScreen(
                        menu(inventory, monitor, menu.hostPos(), picture, MonitorSessionMenu.Phase.BOOT_MENU),
                        inventory, Component.empty()), AROUND_X, AROUND_Y);
            }
            case OpenSystemBootPayload boot -> {
                SystemBootScreen.faceWith(boot.sequence(), left, boot.totalTicks(), boot.endsDark(), boot.splash(),
                        boot.who());
                yield AbstractComputerScreen.face(() -> new SystemBootScreen(
                        menu(inventory, monitor, boot.hostPos(), picture, MonitorSessionMenu.Phase.SYSTEM_BOOT),
                        inventory, Component.empty()), AROUND_X, AROUND_Y);
            }
            case OpenInstallerPayload installer -> {
                InstallerScreen.faceWith(installer, picture.state());
                yield AbstractComputerScreen.face(() -> new InstallerScreen(
                        menu(inventory, monitor, installer.hostPos(), picture, MonitorSessionMenu.Phase.INSTALLER),
                        inventory, Component.empty()), AROUND_X, AROUND_Y);
            }
            case OsInstallProgressPayload copy -> {
                OsInstallScreen.faceWorking(FirmwareKind.byId(copy.firmwareKind()), copy.osName(),
                        GameText.resolve(copy.targetLabel()), left, copy.ticksTotal());
                yield AbstractComputerScreen.face(() -> new OsInstallScreen(
                        menu(inventory, monitor, copy.hostPos(), picture, MonitorSessionMenu.Phase.INSTALL_PROGRESS),
                        inventory, Component.empty()), AROUND_X, AROUND_Y);
            }
            case OpenInstallDonePayload done -> {
                OsInstallScreen.faceDone(FirmwareKind.byId(done.firmwareKind()), done.osName(),
                        GameText.resolve(done.targetLabel()), done.targetSlot());
                yield AbstractComputerScreen.face(() -> new OsInstallScreen(
                        menu(inventory, monitor, done.host(), picture, MonitorSessionMenu.Phase.INSTALL_PROGRESS),
                        inventory, Component.empty()), AROUND_X, AROUND_Y);
            }
            case OpenComputerUiPayload firmware -> {
                FirmwareScreen.faceWith(FirmwareKind.byId(firmware.firmwareKind()), firmware.name(),
                        picture.state());
                yield AbstractComputerScreen.face(() -> new FirmwareScreen(
                        menu(inventory, monitor, firmware.host(), picture, MonitorSessionMenu.Phase.FIRMWARE),
                        inventory, Component.empty()), AROUND_X, AROUND_Y);
            }
            default -> null;
        };
    }

    /* The session's menu as this client would be given it, which holds nothing but who is looked at where. */
    private static MonitorSessionMenu menu(final Inventory inventory, final BlockPos monitor, final BlockPos host,
                                           final IMonitorPicture.Session picture,
                                           final MonitorSessionMenu.Phase phase) {
        return new MonitorSessionMenu(0, inventory, monitor, host, picture.era(), phase);
    }

    /** One face drawn by a screen: the session it was built for, and the screen. */
    private record Shown(IMonitorPicture.Session picture, AbstractComputerScreen<?> screen) {
    }
}
