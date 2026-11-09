/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.operation.payload.RequestSettingsPayload;
import dev.jstech.computers.operation.payload.SettingsSnapshotPayload;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.KernelNames;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.os.ProgramSpec;
import dev.jstech.computers.os.RamLedger;
import dev.jstech.computers.os.WindowKeys;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
 * About This Computer on Quince's systems (computers/docs/design/quince.md). On the classic two it is where the
 * memory is read: the machine's, the largest block left unused, and each program's block as a bar whose dark part is
 * what the program uses of it. From Quince OS X on it names the system, the processor, the memory and the disk the
 * machine started from.
 */
@TextHolder
public final class QuinceAboutApp implements IDesktopApp {

    private final BlockPos host;
    private final QuinceLook look;
    /** What the machine last said of itself, or null before it answered. */
    @Nullable
    private SettingsSnapshotPayload data;
    /** When the machine was last asked, in game ticks, so the bars follow what opens and closes. */
    private long askedAt = Long.MIN_VALUE / 2;

    private static final TextKey BUILT_IN = TextKey.of("jsc.quince.about.built_in", "Built-in Memory:");
    private static final TextKey VIRTUAL = TextKey.of("jsc.quince.about.virtual", "Virtual Memory:");
    private static final TextKey OFF = TextKey.of("jsc.quince.about.off", "Off");
    private static final TextKey LARGEST = TextKey.of("jsc.quince.about.largest", "Largest Unused Block:");
    private static final TextKey BARS = TextKey.of("jsc.quince.about.bars",
            "Each bar is the program's partition; the dark part is in use.");
    private static final TextKey VERSION = TextKey.of("jsc.quince.about.version", "Version %s");
    private static final TextKey PROCESSOR = TextKey.of("jsc.quince.about.processor", "Processor");
    private static final TextKey MEMORY = TextKey.of("jsc.quince.about.memory", "Memory");
    private static final TextKey STARTUP = TextKey.of("jsc.quince.about.startup", "Startup Disk");
    private static final TextKey COPYRIGHT = TextKey.of("jsc.quince.about.copyright", "(c) Quince Inc.");
    private static final ResourceLocation THIS_PC = ResourceLocation.fromNamespaceAndPath("jsc", "this_pc");
    /** How often the machine is asked again while the window is up, in ticks. */
    private static final long ASK_EVERY = 40L;
    private static final int K = 1024;
    private static final int ROW = 16;

    /** The open window that takes the machine's answers, or null when none is up. */
    @Nullable
    private static QuinceAboutApp active;

    QuinceAboutApp(final BlockPos host, final ResourceLocation desktopId) {
        this.host = host;
        final QuinceLook face = QuinceLook.of(desktopId, eraOf(host));
        this.look = face == null ? QuinceLook.PULP : face;
        active = this;
    }

    /** Hands the open window the machine's answer about itself. */
    public static void accept(final SettingsSnapshotPayload payload) {
        if (active != null && active.host.equals(payload.hostPos())) {
            active.data = payload;
        }
    }

    /** The program's own name, which the window's bar writes as Quince calls it: About This Computer. */
    @Override
    public String title() {
        return programName(THIS_PC.getPath());
    }

    /** A program's own name, by its id path, which a window titles itself with; empty for one nobody registered. */
    static String programName(final String path) {
        final ProgramSpec spec = OsRegistry.getProgram(ResourceLocation.fromNamespaceAndPath("jsc", path));
        return spec == null ? "" : spec.displayName();
    }

    @Override
    public int defaultWidth() {
        return 356;
    }

    @Override
    public int defaultHeight() {
        return this.look.classic() ? 150 : 120;
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
        final SettingsSnapshotPayload d = this.data;
        if (d == null) {
            return;
        }
        if (!this.look.classic()) {
            modern(g, font, d, x, y, width, c);
            return;
        }
        QuinceArt.fruit(g, this.look, x + 8, y + 6, false);
        Draw.text(g, font, d.osLabel(), x + 22, y + 6, c.ink());
        if (this.look == QuinceLook.ONE_BIT) {
            Draw.text(g, font, GameText.resolve(COPYRIGHT), x + 22, y + 17, c.ink());
        }
        final int rx = x + 168;
        final int right = x + width - 8;
        int ty = y + 4;
        pair(g, font, GameText.resolve(BUILT_IN), thousands(d.ramMb() * (long) K) + "K", rx, right, ty, c.ink());
        ty += 11;
        if (this.look == QuinceLook.PLATINUM) {
            pair(g, font, GameText.resolve(VIRTUAL), GameText.resolve(OFF), rx, right, ty, c.ink());
            ty += 11;
        }
        final int unused = Math.max(0, d.ramMb() - d.ramUsedMb());
        pair(g, font, GameText.resolve(LARGEST), thousands(unused * (long) K) + "K", rx, right, ty, c.ink());
        final int rule = y + 40;
        g.fill(x + 4, rule, x + width - 4, rule + 1, c.shade());
        g.fill(x + 4, rule + 1, x + width - 4, rule + 2, c.light());
        final List<Row> rows = rows(d);
        int largest = 1;
        for (final Row row : rows) {
            largest = Math.max(largest, row.blockMb());
        }
        int by = rule + 8;
        for (final Row row : rows) {
            if (row.icon() != null) {
                ProgramIcons.draw(g, x + 6, by - 1, 10, 10, row.icon(), iconSet());
            }
            Draw.text(g, font, row.name(), x + 20, by, c.ink());
            final String size = thousands(row.blockMb() * (long) K) + "K";
            Draw.text(g, font, size, x + 146 - font.width(size), by, c.ink());
            final int bx0 = x + 154;
            final int bw = Math.max(8, (x + width - 12 - bx0) * row.blockMb() / largest);
            g.fill(bx0, by, bx0 + bw, by + 9, c.light());
            ChromeShapes.outline(g, bx0, by, bw, 9, c.shade());
            final int used = (bw - 2) * Math.min(row.blockMb(), row.usedMb()) / Math.max(1, row.blockMb());
            g.fill(bx0 + 1, by + 1, bx0 + 1 + used, by + 8, c.select());
            by += ROW;
        }
        Draw.text(g, font, GameText.resolve(BARS), x + 8, by + 2, c.ink());
    }

    /** From Quince OS X on: the system's name and version over the machine's processor, memory and disk. */
    private void modern(final GuiGraphics g, final Font font, final SettingsSnapshotPayload d, final int x,
                        final int y, final int width, final QuinceLook.Colours c) {
        g.pose().pushPose();
        g.pose().translate(x + 12, y + 10, 0.0F);
        g.pose().scale(3.0F, 3.0F, 1.0F);
        QuinceArt.fruit(g, this.look, 0, 0, false);
        g.pose().popPose();
        final int tx = x + 50;
        Draw.text(g, font, Component.literal(d.osLabel()).withStyle(ChatFormatting.BOLD), tx, y + 8, c.ink());
        Draw.text(g, font, GameText.resolve(VERSION.with(KernelNames.WALLACE_RELEASE)), tx, y + 20, c.dim());
        int ty = y + 38;
        for (final String[] pair : new String[][] {
            {GameText.resolve(PROCESSOR), d.cpuMhz() + " MHz " + GameText.resolve(d.cpuLabel())},
            {GameText.resolve(MEMORY), d.ramMb() + " MB"},
            {GameText.resolve(STARTUP), GameText.resolve(DesktopLaunchers.QUINCE_DISK)}}) {
            Draw.text(g, font, Component.literal(pair[0]).withStyle(ChatFormatting.BOLD), tx, ty, c.ink());
            Draw.text(g, font, pair[1], tx + 74, ty, c.ink());
            ty += 12;
        }
    }

    /** The system's own block first, then each program's, as the machine weighs them. */
    private List<Row> rows(final SettingsSnapshotPayload d) {
        final List<Row> rows = new ArrayList<>();
        final Era era = eraOf(this.host);
        for (final SettingsSnapshotPayload.RamUse use : d.ramUses()) {
            if (use.kind().equals(RamLedger.Kind.SYSTEM.serializedName())) {
                rows.add(new Row(use.label(), null, use.mb(), use.mb()));
            } else if (use.kind().equals(RamLedger.Kind.WINDOW.serializedName())) {
                final ProgramSpec spec = WindowKeys.program(use.label());
                if (spec != null) {
                    rows.add(new Row(spec.displayName(), spec.id(), use.mb(), spec.ramMbOn(era, false)));
                }
            }
        }
        return rows;
    }

    private void ask() {
        final Minecraft minecraft = Minecraft.getInstance();
        final long now = minecraft.level == null ? 0L : minecraft.level.getGameTime();
        if (now - this.askedAt >= ASK_EVERY) {
            this.askedAt = now;
            PacketDistributor.sendToServer(new RequestSettingsPayload(this.host));
        }
    }

    private String iconSet() {
        return switch (this.look) {
            case ONE_BIT -> "quince_system";
            case PLATINUM -> "quince_os_9";
            default -> "quince_os_x";
        };
    }

    @Nullable
    private static Era eraOf(final BlockPos host) {
        final Minecraft minecraft = Minecraft.getInstance();
        return minecraft.level != null && minecraft.level.getBlockEntity(host) instanceof IOsHost machine
                ? machine.displayEra() : null;
    }

    private static void pair(final GuiGraphics g, final Font font, final String label, final String value,
                             final int x, final int right, final int y, final int ink) {
        Draw.text(g, font, label, x, y, ink);
        Draw.text(g, font, value, right - font.width(value), y, ink);
    }

    private static String thousands(final long value) {
        return String.format(Locale.ROOT, "%,d", value);
    }

    /**
     * One bar: what holds it, its icon, the block it holds, and what it uses of that block.
     *
     * @param name    what is written beside the bar
     * @param icon    the program's icon, or null for the system's own row
     * @param blockMb the block, in megabytes
     * @param usedMb  what is in use of it, in megabytes
     */
    private record Row(String name, @Nullable ResourceLocation icon, int blockMb, int usedMb) {
    }
}
