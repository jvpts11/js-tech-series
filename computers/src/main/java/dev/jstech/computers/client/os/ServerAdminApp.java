/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.operation.payload.EngineActionPayload;
import dev.jstech.computers.operation.payload.NetworkServicesPayload;
import dev.jstech.computers.operation.payload.RequestNetworkServicesPayload;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.os.ProgramSpec;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * Server Admin on Quince OS X Server (computers/docs/design/quince.md): the Mainframe's services down its left, the
 * Mirror, the engine that plans the network's work and the Automation Engine, and on its right the one chosen, whether
 * it runs, and the button that stops or starts it.
 */
@TextHolder
public final class ServerAdminApp implements IDesktopApp {

    private final BlockPos host;
    private final QuinceLook look;
    /** What the Mainframe last said it runs, or null before it answered. */
    @Nullable
    private NetworkServicesPayload data;
    private int chosen;
    private long askedAt = Long.MIN_VALUE / 2;
    /* Where the list and the button were last drawn. */
    private int listX;
    private int listY;
    private int listW;
    private int[] button = new int[4];

    private static final TextKey SERVERS = TextKey.of("jsc.quince.admin.servers", "SERVERS");
    private static final TextKey OVERVIEW = TextKey.of("jsc.quince.admin.overview", "Overview");
    private static final TextKey IS = TextKey.of("jsc.quince.admin.is", "%s is:");
    private static final TextKey RUNNING = TextKey.of("jsc.quince.admin.running", "Running");
    private static final TextKey STOPPED = TextKey.of("jsc.quince.admin.stopped", "Stopped");
    private static final TextKey NOT_INSTALLED = TextKey.of("jsc.quince.admin.not_installed", "Not installed");
    private static final TextKey VERSION = TextKey.of("jsc.quince.admin.version", "Version:");
    private static final TextKey MADE_BY = TextKey.of("jsc.quince.admin.made_by", "Made by:");
    private static final TextKey STOP = TextKey.of("jsc.quince.admin.stop", "Stop %s");
    private static final TextKey START = TextKey.of("jsc.quince.admin.start", "Start %s");
    private static final TextKey TITLE = TextKey.of("jsc.quince.admin.title", "%s: Server Admin");
    private static final long ASK_EVERY = 40L;
    private static final int ROW = 12;
    private static final int SIDE_W = 104;

    /** The open window that takes the Mainframe's answers, or null when none is up. */
    @Nullable
    private static ServerAdminApp active;

    ServerAdminApp(final BlockPos host, final ResourceLocation desktopId) {
        this.host = host;
        final QuinceLook face = QuinceLook.of(desktopId, null);
        this.look = face == null ? QuinceLook.FLAT : face;
        active = this;
    }

    /** Hands the open window what the Mainframe runs. */
    public static void accept(final NetworkServicesPayload payload) {
        if (active != null && active.host.equals(payload.hostPos())) {
            active.data = payload;
        }
    }

    @Override
    public String title() {
        return GameText.resolve(TITLE.with(ActiveDesktop.machineName()));
    }

    @Override
    public int defaultWidth() {
        return 340;
    }

    @Override
    public int defaultHeight() {
        return 190;
    }

    @Override
    public void onClosed() {
        if (active == this) {
            active = null;
        }
    }

    @Override
    public void renderContent(final GuiGraphics g, final Font font, final int x, final int y, final int width,
                              final int height, final int mouseX, final int mouseY, final float partialTick) {
        ask();
        final QuinceLook.Colours c = this.look.colours();
        g.fill(x, y, x + SIDE_W, y + height, c.sidebar());
        g.fill(x + SIDE_W, y, x + SIDE_W + 1, y + height, c.shade());
        g.fill(x + SIDE_W + 1, y, x + width, y + height, c.light());
        Draw.text(g, font, GameText.resolve(SERVERS), x + 4, y + 4, c.heading());
        Draw.text(g, font, Component.literal(ActiveDesktop.machineName()).withStyle(ChatFormatting.BOLD), x + 4,
                y + 16, c.ink());
        final List<Service> services = services();
        this.chosen = Math.max(0, Math.min(this.chosen, services.size() - 1));
        this.listX = x;
        this.listY = y + 28;
        this.listW = SIDE_W;
        for (int i = 0; i < services.size(); i++) {
            final Service one = services.get(i);
            final int ry = this.listY + i * ROW;
            if (i == this.chosen) {
                g.fill(x + 1, ry - 1, x + SIDE_W - 1, ry + ROW - 1, c.select());
            }
            QuinceArt.disc(g, x + 7, ry + 4, 2, one.running() ? QuinceArt.colours().zoom()
                    : QuinceArt.colours().idleLight(), 0);
            Draw.text(g, font, one.name(), x + 13, ry + 1, i == this.chosen ? c.selectInk() : c.ink());
        }
        // The pane: Overview, the chosen service's state and what it is.
        final int px = x + SIDE_W + 8;
        Draw.text(g, font, GameText.resolve(OVERVIEW), px, y + 4, c.ink());
        if (services.isEmpty()) {
            return;
        }
        final Service one = services.get(this.chosen);
        final int labels = px + 70;
        int ry = y + 22;
        row(g, font, GameText.resolve(IS.with(one.name())), Component.literal(GameText.resolve(one.installed()
                ? one.running() ? RUNNING : STOPPED : NOT_INSTALLED)).withStyle(ChatFormatting.BOLD), labels, ry, c);
        ry += 12;
        row(g, font, GameText.resolve(VERSION), Component.literal(one.version()), labels, ry, c);
        ry += 12;
        row(g, font, GameText.resolve(MADE_BY), Component.literal(one.vendor()), labels, ry, c);
        if (one.installed()) {
            final String label = GameText.resolve((one.running() ? STOP : START).with(one.name()));
            final int bw = font.width(label) + 16;
            this.button = new int[] {x + 4, y + height - 16, bw, 12};
            QuinceArt.button(g, this.look, this.button[0], this.button[1], bw, 12, false, false);
            Draw.text(g, font, label, this.button[0] + 8, this.button[1] + 2, c.ink());
        } else {
            this.button = new int[4];
        }
    }

    @Override
    public void mouseClicked(final DesktopWindow window, final double mouseX, final double mouseY, final int button) {
        if (mouseX >= this.listX && mouseX < this.listX + this.listW && mouseY >= this.listY) {
            final int row = (int) ((mouseY - this.listY) / ROW);
            if (row < services().size()) {
                this.chosen = row;
                return;
            }
        }
        if (this.button[2] > 0 && mouseX >= this.button[0] && mouseX < this.button[0] + this.button[2]
                && mouseY >= this.button[1] && mouseY < this.button[1] + this.button[3]) {
            toggle();
        }
    }

    /** Stops the chosen service, or starts it again: the engine through its own switch, the others as services. */
    private void toggle() {
        final List<Service> services = services();
        if (this.chosen >= services.size()) {
            return;
        }
        final Service one = services.get(this.chosen);
        final byte action = one.engine()
                ? one.running() ? EngineActionPayload.STOP : EngineActionPayload.START
                : one.running() ? EngineActionPayload.SERVICE_STOP : EngineActionPayload.SERVICE_START;
        PacketDistributor.sendToServer(new EngineActionPayload(this.host, action, one.program()));
        this.askedAt = Long.MIN_VALUE / 2;
    }

    /** The Mainframe's services as the window lists them: the Mirror, the engine, and the Automation Engine. */
    private List<Service> services() {
        final List<Service> out = new ArrayList<>();
        final NetworkServicesPayload d = this.data;
        if (d == null) {
            return out;
        }
        for (final NetworkServicesPayload.ServiceRow row : d.services()) {
            if (row.program().endsWith(":mirror")) {
                out.add(service(row));
            }
        }
        final NetworkServicesPayload.Engine engine = d.engine();
        if (!engine.program().isEmpty()) {
            out.add(new Service(engine.program(), engine.name(), engine.version(), engine.vendor(), true,
                    engine.state() == NetworkServicesPayload.ENGINE_RUNNING, true));
        }
        for (final NetworkServicesPayload.ServiceRow row : d.services()) {
            if (!row.program().endsWith(":mirror")) {
                out.add(service(row));
            }
        }
        return out;
    }

    private static Service service(final NetworkServicesPayload.ServiceRow row) {
        final ProgramSpec spec = OsRegistry.getProgram(ResourceLocation.tryParse(row.program()));
        final boolean installed = row.state() != NetworkServicesPayload.SERVICE_ABSENT || spec == null;
        return new Service(row.program(), row.name(), row.version(), row.vendor(), installed,
                row.state() == NetworkServicesPayload.SERVICE_RUNNING, false);
    }

    private void ask() {
        final Minecraft minecraft = Minecraft.getInstance();
        final long now = minecraft.level == null ? 0L : minecraft.level.getGameTime();
        if (now - this.askedAt >= ASK_EVERY) {
            this.askedAt = now;
            PacketDistributor.sendToServer(new RequestNetworkServicesPayload(this.host));
        }
    }

    private static void row(final GuiGraphics g, final Font font, final String label, final Component value,
                            final int labels, final int y, final QuinceLook.Colours c) {
        Draw.text(g, font, label, labels - font.width(label), y, c.ink());
        Draw.text(g, font, value, labels + 4, y, c.ink());
    }

    /**
     * One service the window lists.
     *
     * @param program   its program id, which an action names it by
     * @param name      what it is called
     * @param version   its version
     * @param vendor    who makes it
     * @param installed whether the Mainframe has it at all
     * @param running   whether it runs now
     * @param engine    whether it is the engine that plans the network's work, which has a switch of its own
     */
    private record Service(String program, String name, String version, String vendor, boolean installed,
                           boolean running, boolean engine) {
    }
}
