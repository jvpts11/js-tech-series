/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.blockentity;

import dev.jstech.computers.menu.DesktopMenu;
import dev.jstech.computers.operation.payload.CommandOutputPayload;
import dev.jstech.computers.operation.payload.DesktopBalloonPayload;
import dev.jstech.computers.operation.payload.TerminalKeyboard;
import dev.jstech.computers.operation.payload.WireLine;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.Platform;
import dev.jstech.computers.os.install.Installers;
import dev.jstech.computers.power.UpsReading;
import dev.jstech.computers.power.UpsTexts;
import dev.jstech.computers.program.cli.CliStyle;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * What a machine's system says when its UPS takes over from a line that went (computers/docs/design/power.md), each in
 * its own way: Frames raises its notice from the battery in its tray, the other desktops their power manager's notice,
 * each with the battery under its words; MC-DOS and MC-NET print the UPS's two lines on the console, and a Unix at its
 * console gets the UPS daemon's broadcast.
 */
@TextHolder
final class UpsNotices {

    /** What the network's log says of a machine its UPS shut down cleanly at a tenth of its battery. */
    static final TextKey LOG_SHUTDOWN = TextKey.of("jsc.event_log.ups_shutdown",
            "Shut down by its UPS: the battery was down to 10%");

    private static final TextKey CARRIES = TextKey.of("jsc.ups.carries", "The UPS carries the PC");
    private static final TextKey POWER_MANAGEMENT = TextKey.of("jsc.ups.power_management", "Power Management");
    private static final TextKey RUNNING_ON = TextKey.of("jsc.ups.running_on", "Running on the UPS battery");
    private static final TextKey DOS_OUT = TextKey.of("jsc.ups.dos_out",
            "UPS: the power is out. Running on the battery: %s%%, about %s min.");
    private static final TextKey DOS_SAVE = TextKey.of("jsc.ups.dos_save",
            "UPS: save your work. At 10%% the system shuts itself down.");
    /* The UPS daemon of the Unixes broadcasts on every terminal, in its own words as every daemon writes. */
    private static final String BROADCAST = "Broadcast message from nut@%s:";
    private static final String UPS_ON_BATTERY = "UPS ups@localhost on battery";

    private UpsNotices() {
    }

    /**
     * Tells whoever watches {@code host}, the machine at {@code hostPos}, that its UPS has taken over: {@code viewers}
     * are the players at its monitor, {@code keyboard} who has the console's keyboard, {@code reading} how the UPS
     * stands.
     */
    static void onBattery(final BlockPos hostPos, final IOsHost host, final List<ServerPlayer> viewers,
                          final TerminalKeyboard keyboard, final UpsReading reading) {
        final OsDef system = host.installedOs();
        if (system == null || viewers.isEmpty()) {
            return;
        }
        if (host.bootedDesktopId() != null) {
            // The words lead; each desktop shows the battery under them in its own way.
            final boolean frames = system.platform() == Platform.FRAMES;
            final DesktopBalloonPayload notice = new DesktopBalloonPayload(hostPos,
                    (frames ? UpsTexts.ON_BATTERY : POWER_MANAGEMENT).text(), (frames ? CARRIES : RUNNING_ON).text(),
                    "", reading.percent(), reading.minutesLeft());
            for (final ServerPlayer viewer : viewers) {
                PacketDistributor.sendToPlayer(viewer, notice);
            }
            return;
        }
        final List<WireLine> lines = switch (system.platform()) {
            case MC_DOS, MC_NET -> List.of(new WireLine(Text.EMPTY, CliStyle.PLAIN.id()),
                    new WireLine(DOS_OUT.with(reading.percent(), reading.minutesLeft()), CliStyle.PLAIN.id()),
                    new WireLine(DOS_SAVE.text(), CliStyle.PLAIN.id()));
            default -> List.of(new WireLine(Text.EMPTY, CliStyle.PLAIN.id()),
                    new WireLine(String.format(BROADCAST, Installers.hostName(host)), CliStyle.PLAIN.id()),
                    new WireLine(UPS_ON_BATTERY, CliStyle.PLAIN.id()));
        };
        final CommandOutputPayload said = new CommandOutputPayload("", lines, keyboard);
        for (final ServerPlayer viewer : viewers) {
            if (!(viewer.containerMenu instanceof DesktopMenu)) {
                PacketDistributor.sendToPlayer(viewer, said);
            }
        }
    }
}
