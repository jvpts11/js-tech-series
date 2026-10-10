/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.monitor;

import dev.jstech.computers.operation.payload.DesktopWindowsPayload;
import dev.jstech.computers.operation.payload.FirmwareStatePayload;
import dev.jstech.computers.operation.payload.OpenBootMenuPayload;
import dev.jstech.computers.operation.payload.OpenComputerUiPayload;
import dev.jstech.computers.operation.payload.OpenInstallDonePayload;
import dev.jstech.computers.operation.payload.OpenInstallerPayload;
import dev.jstech.computers.operation.payload.OpenPostPayload;
import dev.jstech.computers.operation.payload.OpenSystemBootPayload;
import dev.jstech.computers.operation.payload.OsInstallProgressPayload;
import dev.jstech.computers.operation.payload.WireLine;
import dev.jstech.computers.os.Platform;
import dev.jstech.core.text.TextBounds;
import dev.jstech.core.tier.HardwareEra;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * What a monitor's face shows in the world, described for the players who can see it: the server says what the
 * machine shows, and each viewer's game draws it.
 *
 * <p>The description is what the machine holds, never a picture: a machine that is off is dark glass; one testing
 * itself, coming up or going down, standing at its boot manager, in its firmware setup or installing is in a session,
 * drawn by the very screen that session opens; one at a prompt shows its console; one with a desktop shows that
 * desktop with the windows it has open. Two equal descriptions are the same picture, so the server sends one only
 * when it changes.
 */
public sealed interface IMonitorPicture
        permits IMonitorPicture.Dark, IMonitorPicture.Console, IMonitorPicture.Desktop, IMonitorPicture.Session {

    /** The glass with nothing on it. */
    Dark DARK = new Dark();

    /** The most lines a console carries. */
    int MAX_LINES = 24;

    /** The longest prompt carried. */
    int PROMPT_MAX = 256;

    /** The most windows a desktop picture carries. */
    int WINDOWS_MAX = 32;

    /* Which opening a session's picture carries, on the wire. */
    byte OPENING_POST = 1;
    byte OPENING_BOOT_MENU = 2;
    byte OPENING_SYSTEM_BOOT = 3;
    byte OPENING_INSTALLER = 4;
    byte OPENING_COPY = 5;
    byte OPENING_INSTALLED = 6;
    byte OPENING_FIRMWARE = 7;

    StreamCodec<RegistryFriendlyByteBuf, IMonitorPicture> STREAM_CODEC =
            StreamCodec.of(IMonitorPicture::write, IMonitorPicture::read);

    /** Nothing on the glass: a machine that is off, or one with nothing to show yet. */
    record Dark() implements IMonitorPicture {
    }

    /**
     * A machine at its prompt: the last lines its console printed, the prompt waiting under them, the family of its
     * system, whose console blinks its cursor to its own beat (null where there is no system yet), and the machine's
     * display scale in percent (0 for the default), which its text is drawn at as on the open prompt.
     */
    record Console(HardwareEra era, List<WireLine> lines, String prompt, @Nullable Platform platform,
                   int scalePercent) implements IMonitorPicture {

        public Console {
            lines = List.copyOf(lines.size() > MAX_LINES ? lines.subList(lines.size() - MAX_LINES, lines.size())
                    : lines);
        }
    }

    /**
     * A machine at its desktop: what the desktop is drawn from (the machine, its system and its desktop, its memory),
     * and the windows it has open, front-most last.
     */
    record Desktop(BlockPos host, ResourceLocation osId, ResourceLocation desktopId, int ramTotalMb,
                   int ramReservedMb, List<DesktopWindowsPayload.WireWindow> windows, int workspace)
            implements IMonitorPicture {

        public Desktop {
            windows = List.copyOf(windows);
        }
    }

    /**
     * A machine in one of its sessions, a screen of its own that a player opening the monitor is put in front of (the
     * self-test, the boot manager, a system coming up or going down, an install, the firmware setup): the very opening
     * that session is sent, which the screen it opens also draws the face from, so the face and the open screen are
     * one picture.
     *
     * <p>A counter the opening carries (what is left of the self-test, of the boot manager's wait, of a system coming
     * up or going down, of a copy) is kept at nought, and the game time it runs out at goes beside it instead: the
     * description then stays the same while the machine works through the session, and is sent once.
     *
     * @param era     the machine's generation, which the screen's look follows; null where it cannot say
     * @param opening what the session is sent: one of the openings {@link SessionOpenings} makes
     * @param state   the machine's parts as its firmware reads them, for the screens that list them; null otherwise
     * @param endsAt  the game time the opening's counter runs out at, 0 where it has none or a key stopped it
     */
    record Session(@Nullable HardwareEra era, CustomPacketPayload opening, @Nullable FirmwareStatePayload state,
                   long endsAt) implements IMonitorPicture {

        /** What is left of the opening's counter at the game time {@code now}, at least one tick while it runs. */
        public int remaining(final long now) {
            return endsAt == 0L ? 0 : (int) Math.max(1L, Math.min(Integer.MAX_VALUE, endsAt - now));
        }
    }

    private static void write(final RegistryFriendlyByteBuf buf, final IMonitorPicture picture) {
        switch (picture) {
            case Dark dark -> buf.writeByte(0);
            case Console console -> {
                buf.writeByte(2);
                buf.writeEnum(console.era());
                WireLine.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_LINES)).encode(buf, console.lines());
                buf.writeUtf(clip(console.prompt()), PROMPT_MAX);
                buf.writeUtf(console.platform() == null ? "" : console.platform().serializedName());
                buf.writeVarInt(console.scalePercent());
            }
            case Desktop desktop -> {
                buf.writeByte(3);
                BlockPos.STREAM_CODEC.encode(buf, desktop.host());
                buf.writeResourceLocation(desktop.osId());
                buf.writeResourceLocation(desktop.desktopId());
                buf.writeVarInt(desktop.ramTotalMb());
                buf.writeVarInt(desktop.ramReservedMb());
                final List<DesktopWindowsPayload.WireWindow> windows = desktop.windows();
                DesktopWindowsPayload.WireWindow.STREAM_CODEC.apply(ByteBufCodecs.list(WINDOWS_MAX))
                        .encode(buf, windows.size() > WINDOWS_MAX ? windows.subList(0, WINDOWS_MAX) : windows);
                buf.writeVarInt(desktop.workspace());
            }
            case Session session -> {
                buf.writeByte(4);
                buf.writeVarInt(session.era() == null ? -1 : session.era().id());
                writeOpening(buf, session.opening());
                buf.writeBoolean(session.state() != null);
                if (session.state() != null) {
                    FirmwareStatePayload.STREAM_CODEC.encode(buf, session.state());
                }
                buf.writeVarLong(session.endsAt());
            }
        }
    }

    /* An opening, preceded by which of them it is. */
    private static void writeOpening(final RegistryFriendlyByteBuf buf, final CustomPacketPayload opening) {
        switch (opening) {
            case OpenPostPayload post -> {
                buf.writeByte(OPENING_POST);
                OpenPostPayload.STREAM_CODEC.encode(buf, post);
            }
            case OpenBootMenuPayload menu -> {
                buf.writeByte(OPENING_BOOT_MENU);
                OpenBootMenuPayload.STREAM_CODEC.encode(buf, menu);
            }
            case OpenSystemBootPayload boot -> {
                buf.writeByte(OPENING_SYSTEM_BOOT);
                OpenSystemBootPayload.STREAM_CODEC.encode(buf, boot);
            }
            case OpenInstallerPayload installer -> {
                buf.writeByte(OPENING_INSTALLER);
                OpenInstallerPayload.STREAM_CODEC.encode(buf, installer);
            }
            case OsInstallProgressPayload copy -> {
                buf.writeByte(OPENING_COPY);
                OsInstallProgressPayload.STREAM_CODEC.encode(buf, copy);
            }
            case OpenInstallDonePayload done -> {
                buf.writeByte(OPENING_INSTALLED);
                OpenInstallDonePayload.STREAM_CODEC.encode(buf, done);
            }
            case OpenComputerUiPayload firmware -> {
                buf.writeByte(OPENING_FIRMWARE);
                OpenComputerUiPayload.STREAM_CODEC.encode(buf, firmware);
            }
            default -> throw new IllegalArgumentException("no session opens with " + opening.type().id());
        }
    }

    private static CustomPacketPayload readOpening(final RegistryFriendlyByteBuf buf) {
        return switch (buf.readByte()) {
            case OPENING_POST -> OpenPostPayload.STREAM_CODEC.decode(buf);
            case OPENING_BOOT_MENU -> OpenBootMenuPayload.STREAM_CODEC.decode(buf);
            case OPENING_SYSTEM_BOOT -> OpenSystemBootPayload.STREAM_CODEC.decode(buf);
            case OPENING_INSTALLER -> OpenInstallerPayload.STREAM_CODEC.decode(buf);
            case OPENING_COPY -> OsInstallProgressPayload.STREAM_CODEC.decode(buf);
            case OPENING_INSTALLED -> OpenInstallDonePayload.STREAM_CODEC.decode(buf);
            case OPENING_FIRMWARE -> OpenComputerUiPayload.STREAM_CODEC.decode(buf);
            default -> throw new IllegalArgumentException("unknown session opening");
        };
    }

    private static IMonitorPicture read(final RegistryFriendlyByteBuf buf) {
        return switch (buf.readByte()) {
            case 2 -> new Console(buf.readEnum(HardwareEra.class),
                    WireLine.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_LINES)).decode(buf), buf.readUtf(PROMPT_MAX),
                    Platform.byName(buf.readUtf()), buf.readVarInt());
            case 3 -> new Desktop(BlockPos.STREAM_CODEC.decode(buf), buf.readResourceLocation(),
                    buf.readResourceLocation(), buf.readVarInt(), buf.readVarInt(),
                    new ArrayList<>(DesktopWindowsPayload.WireWindow.STREAM_CODEC
                            .apply(ByteBufCodecs.list(WINDOWS_MAX)).decode(buf)), buf.readVarInt());
            case 4 -> new Session(HardwareEra.find(buf.readVarInt()), readOpening(buf),
                    buf.readBoolean() ? FirmwareStatePayload.STREAM_CODEC.decode(buf) : null, buf.readVarLong());
            default -> DARK;
        };
    }

    private static String clip(final String prompt) {
        return TextBounds.clip(prompt, PROMPT_MAX);
    }
}
