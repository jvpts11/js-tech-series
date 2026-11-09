/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.operation.payload.program.StorePayloads;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.os.ProgramSpec;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * The Quince Store (computers/docs/design/quince.md), from the Standard on: the programs the network's Mirror serves
 * for this machine, every one of them free, got with GET the way {@code keg} gets them at the Terminal.
 */
@TextHolder
public final class QuinceStoreApp implements IDesktopApp {

    private final BlockPos host;
    private final QuinceLook look;
    /** What the Mirror last said it serves, or null before it answered. */
    @Nullable
    private StorePayloads.Listing listing;
    private int tab;
    /** How many rows of the grid are scrolled past. */
    private int scroll;
    /** The GET buttons as last drawn, each {@code {x, y, w, h}}, with the program each gets. */
    private final List<int[]> gets = new ArrayList<>();
    private final List<String> getting = new ArrayList<>();
    /** The tabs as last drawn, each {@code {left, right}}. */
    private final List<int[]> tabs = new ArrayList<>();

    private static final TextKey FEATURED = TextKey.of("jsc.quince.store.featured", "Featured");
    private static final TextKey TOP_CHARTS = TextKey.of("jsc.quince.store.top_charts", "Top Charts");
    private static final TextKey CATEGORIES = TextKey.of("jsc.quince.store.categories", "Categories");
    private static final TextKey PURCHASED = TextKey.of("jsc.quince.store.purchased", "Purchased");
    private static final TextKey UPDATES = TextKey.of("jsc.quince.store.updates", "Updates");
    private static final List<TextKey> TABS = List.of(FEATURED, TOP_CHARTS, CATEGORIES, PURCHASED, UPDATES);
    private static final TextKey HEADING = TextKey.of("jsc.quince.store.heading", "Programs for your Quince");
    private static final TextKey GET = TextKey.of("jsc.quince.store.get", "GET");
    private static final TextKey INSTALLED = TextKey.of("jsc.quince.store.installed", "INSTALLED");
    private static final TextKey SERVED = TextKey.of("jsc.quince.store.served", "Served by the Mirror of your network.");
    private static final TextKey NO_MIRROR = TextKey.of("jsc.quince.store.no_mirror",
            "No Mirror answers on this network, so the Store has nothing to show.");
    private static final TextKey UP_TO_DATE = TextKey.of("jsc.quince.store.up_to_date",
            "Every program here is up to date.");
    private static final int ROW_H = 44;
    private static final int TAB_H = 14;

    /** The open Store that takes the Mirror's answers, or null when none is up. */
    @Nullable
    private static QuinceStoreApp active;

    QuinceStoreApp(final BlockPos host, final ResourceLocation desktopId) {
        this.host = host;
        final QuinceLook face = QuinceLook.of(desktopId, null);
        this.look = face == null ? QuinceLook.FLAT : face;
        active = this;
        PacketDistributor.sendToServer(new StorePayloads.Ask(host));
    }

    /** Hands the open Store what the Mirror serves. */
    public static void accept(final StorePayloads.Listing payload) {
        if (active != null && active.host.equals(payload.host())) {
            active.listing = payload;
        }
    }

    @Override
    public String title() {
        return QuinceAboutApp.programName("quince_store");
    }

    @Override
    public int defaultWidth() {
        return 340;
    }

    @Override
    public int defaultHeight() {
        return 196;
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
        final QuinceLook.Colours c = this.look.colours();
        // The tabs along the top, the one up picked out.
        this.tabs.clear();
        int tx = x + 16;
        for (int i = 0; i < TABS.size(); i++) {
            final String label = GameText.resolve(TABS.get(i));
            final int w = font.width(label);
            if (i == this.tab) {
                QuinceArt.roundRect(g, tx - 4, y + 1, tx + w + 4, y + TAB_H - 1, 2, c.track());
            }
            Draw.text(g, font, label, tx, y + 3, c.ink());
            this.tabs.add(new int[] {tx - 4, tx + w + 4});
            tx += w + 16;
        }
        g.fill(x, y + TAB_H, x + width, y + TAB_H + 1, c.shade());
        g.fill(x, y + TAB_H + 1, x + width, y + height, c.light());
        this.gets.clear();
        this.getting.clear();
        final int top = y + TAB_H + 6;
        final StorePayloads.Listing shown = this.listing;
        if (shown == null) {
            return;
        }
        if (!shown.reachable()) {
            Draw.text(g, font, GameText.resolve(NO_MIRROR), x + 10, top, c.dim());
            return;
        }
        Draw.text(g, font, Component.literal(GameText.resolve(HEADING)).withStyle(ChatFormatting.BOLD), x + 10, top,
                c.ink());
        final List<StorePayloads.Row> rows = rows(shown);
        if (rows.isEmpty()) {
            Draw.text(g, font, GameText.resolve(UP_TO_DATE), x + 10, top + 14, c.dim());
        }
        final int gridTop = top + 12;
        final int visible = Math.max(1, (height - TAB_H - 40) / ROW_H) * 2;
        this.scroll = Math.max(0, Math.min(this.scroll, Math.max(0, (rows.size() - visible + 1) / 2)));
        for (int i = 0; i < visible && this.scroll * 2 + i < rows.size(); i++) {
            final StorePayloads.Row row = rows.get(this.scroll * 2 + i);
            final int cx = x + 10 + (i % 2) * (width / 2);
            final int cy = gridTop + (i / 2) * ROW_H;
            g.fill(cx, cy, cx + 22, cy + 22, c.face());
            ProgramIcons.draw(g, cx + 3, cy + 3, 16, 16, idOf(row.program()), "quince_os_x");
            Draw.text(g, font, Component.literal(row.name()).withStyle(ChatFormatting.BOLD), cx + 28, cy, c.ink());
            Draw.text(g, font, houseOf(row.program()), cx + 28, cy + 10, c.dim());
            final String label = GameText.resolve(row.installed() ? INSTALLED : GET);
            final int bw = font.width(label) + 10;
            QuinceArt.roundRect(g, cx + 28, cy + 21, cx + 28 + bw, cy + 31, 4, c.select());
            Draw.text(g, font, label, cx + 33, cy + 22, c.selectInk());
            if (!row.installed()) {
                this.gets.add(new int[] {cx + 28, cy + 21, bw, 10});
                this.getting.add(row.program());
            }
        }
        Draw.text(g, font, GameText.resolve(SERVED), x + 10, y + height - 12, c.dim());
    }

    @Override
    public void mouseClicked(final DesktopWindow window, final double mouseX, final double mouseY, final int button) {
        for (int i = 0; i < this.tabs.size(); i++) {
            final int[] span = this.tabs.get(i);
            if (mouseX >= span[0] && mouseX < span[1] && mouseY < window.y() + 30) {
                this.tab = i;
                this.scroll = 0;
                return;
            }
        }
        for (int i = 0; i < this.gets.size(); i++) {
            final int[] box = this.gets.get(i);
            if (mouseX >= box[0] && mouseX < box[0] + box[2] && mouseY >= box[1] && mouseY < box[1] + box[3]) {
                PacketDistributor.sendToServer(new StorePayloads.Get(this.host, this.getting.get(i)));
                return;
            }
        }
    }

    @Override
    public boolean mouseScrolled(final double delta) {
        this.scroll = Math.max(0, this.scroll - (int) Math.signum(delta));
        return true;
    }

    /** The rows of the tab that is up: all of them, by name for the charts, what is installed, or none to update. */
    private List<StorePayloads.Row> rows(final StorePayloads.Listing shown) {
        final List<StorePayloads.Row> rows = new ArrayList<>(shown.rows());
        switch (this.tab) {
            case 1, 2 -> rows.sort(Comparator.comparing(StorePayloads.Row::name));
            case 3 -> rows.removeIf(row -> !row.installed());
            case 4 -> rows.clear();
            default -> {
            }
        }
        return rows;
    }

    /** The program a Store row gets, by the name it is got by, for its icon and its house. */
    private static ResourceLocation idOf(final String command) {
        for (final ProgramSpec spec : OsRegistry.programs()) {
            if (spec.commandName().equals(command)) {
                return spec.id();
            }
        }
        return ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "generic");
    }

    private static String houseOf(final String command) {
        final ProgramSpec spec = OsRegistry.getProgram(idOf(command));
        return spec == null ? "" : spec.house().name();
    }
}
