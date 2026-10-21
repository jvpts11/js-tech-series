/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.studio;

import dev.jstech.core.client.gui.component.Button;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.mcp.McpGame;
import dev.jstech.core.client.mcp.McpTexts;
import dev.jstech.core.developer.mcp.McpCallLog;
import dev.jstech.core.developer.mcp.McpPermission;
import dev.jstech.core.developer.mcp.McpServer;
import dev.jstech.core.developer.mcp.McpSide;
import dev.jstech.core.developer.mcp.McpTools;
import dev.jstech.core.developer.mcp.McpWords;
import dev.jstech.core.developer.mcp.PairedClients;
import dev.jstech.core.gui.layout.StudioLayout;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;

/**
 * The studio's MCP server: where it listens and its switch, which is the key "MCP server", on for this run of the game
 * only; the clients let in, what each may do, since when and how many calls, with its key taken back; the tools of
 * other mods, off until turned on; and every call, who made it, what it called and what became of it.
 */
final class AgentsPage extends StudioPage {

    private final Button server;
    private final Button cut;
    private final Button takeBack;
    private final Map<String, Button> mods = new LinkedHashMap<>();
    private int chosen;

    private static final int CLIENT_ROW = 34;
    private static final int CALL_ROW = 13;
    private static final int MOD_ROW = 18;
    private static final String TIME = "HH:mm:ss";

    AgentsPage(final StudioScreen studio) {
        super(studio);
        this.server = this.panel.add(new Button(() -> words(McpGame.running().isPresent() ? ToolTexts.SERVER_IS_ON
                : ToolTexts.SERVER_IS_OFF), () -> this.studio.say(GameText.resolve(McpGame.turn(
                        McpGame.running().isEmpty())))));
        this.cut = this.panel.add(new Button(() -> words(ToolTexts.CUT_THEM_ALL), () -> this.studio.say(
                GameText.resolve(McpTexts.CUT.with(McpGame.cutAll())))));
        this.takeBack = this.panel.add(new Button(() -> words(ToolTexts.TAKE_KEY_BACK), this::takeBack));
        for (final String mod : McpTools.mods(McpSide.GAME)) {
            this.mods.put(mod, this.panel.add(new Button(() -> words(McpTools.isOn(mod) ? ToolTexts.TOOLS_ON
                    : ToolTexts.TOOLS_OFF), () -> McpTools.turn(mod, !McpTools.isOn(mod)))));
        }
    }

    @Override
    TextKey tab() {
        return StudioTool.AGENTS.title();
    }

    @Override
    List<String> crumbs() {
        final List<PairedClients.PairedClient> clients = this.clients();
        return this.chosen < clients.size() ? List.of(words(this.tab()), clients.get(this.chosen).name())
                : List.of(words(this.tab()));
    }

    @Override
    List<String> lines() {
        return this.clients().stream().map(PairedClients.PairedClient::toString).toList();
    }

    @Override
    void layout(final StudioLayout.Regions laid) {
        super.layout(laid);
        final StudioLayout.Rect body = laid.body();
        place(this.cut, new StudioLayout.Rect(body.right() - 78, body.y() - 1, 78, 14));
        place(this.server, new StudioLayout.Rect(body.right() - 78 - StudioLayout.GAP / 2 - 82, body.y() - 1, 82,
                14));
        final StudioLayout.Rect left = this.columns().get(0);
        final int x = left.x() + StudioDraw.INSET;
        final int wide = left.width() - 2 * StudioDraw.INSET;
        place(this.takeBack, new StudioLayout.Rect(x, this.buttonY(left), wide, 18));
        int y = this.modsY(left) + 12;
        for (final Button button : this.mods.values()) {
            place(button, new StudioLayout.Rect(left.right() - StudioDraw.INSET - 64, y, 64, 14));
            y += MOD_ROW;
        }
    }

    @Override
    void render(final GuiGraphics g, final int mouseX, final int mouseY, final List<Component> tip) {
        final StudioPalette.Colours colours = StudioPalette.get();
        final StudioLayout.Rect body = this.regions.body();
        final Optional<McpServer> running = McpGame.running();
        StudioDraw.text(g, font(), running.map(server -> GameText.resolve(ToolTexts.AGENTS_HEAD.with(server.port())))
                .orElse(words(ToolTexts.AGENTS_HEAD_OFF)), body.x() + 2, body.y() + 2,
                this.server.x() - body.x() - StudioLayout.GAP, colours.text());
        final List<StudioLayout.Rect> columns = this.columns();
        this.clientsPanel(g, columns.get(0), colours);
        this.callsPanel(g, columns.get(1), running, colours);
        this.cut.setEnabled(!this.clients().isEmpty());
        this.takeBack.setEnabled(this.chosen < this.clients().size());
    }

    @Override
    boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        final StudioLayout.Rect left = this.columns().get(0);
        final int top = left.y() + StudioDraw.HEAD + 2;
        if (!left.contains(mouseX, mouseY) || mouseY < top || mouseY >= this.buttonY(left)) {
            return false;
        }
        final int row = (int) ((mouseY - top) / CLIENT_ROW);
        if (row < this.clients().size()) {
            this.chosen = row;
            return true;
        }
        return false;
    }

    private List<StudioLayout.Rect> columns() {
        return StudioLayout.columns(StudioLayout.below(this.regions.body(), 16), 0.36);
    }

    private List<PairedClients.PairedClient> clients() {
        return McpGame.running().map(server -> server.protocol().clients().all()).orElse(List.of());
    }

    /* The button that takes a key back stands under the clients, two of them shown at least. */
    private int buttonY(final StudioLayout.Rect left) {
        return left.y() + StudioDraw.HEAD + 4 + Math.max(2, this.clients().size()) * CLIENT_ROW;
    }

    /* The tools of other mods stand under the note on keys, as many lines as it takes in the player's language. */
    private int modsY(final StudioLayout.Rect left) {
        final int lines = font().split(FormattedText.of(words(ToolTexts.KEY_NOTE)), (int) ((left.width()
                - 2 * StudioDraw.INSET) / StudioDraw.SMALL)).size();
        return this.buttonY(left) + 22 + lines * (StudioDraw.SMALL_ROW - 3) + 10;
    }

    private void takeBack() {
        final List<PairedClients.PairedClient> clients = this.clients();
        McpGame.running().ifPresent(server -> {
            if (this.chosen < clients.size()) {
                server.protocol().revoke(clients.get(this.chosen));
                this.chosen = 0;
            }
        });
    }

    private void clientsPanel(final GuiGraphics g, final StudioLayout.Rect area,
                              final StudioPalette.Colours colours) {
        StudioDraw.panel(g, font(), area, words(ToolTexts.CLIENTS_PAIRED));
        final List<PairedClients.PairedClient> clients = this.clients();
        final int x = area.x() + StudioDraw.INSET;
        if (clients.isEmpty()) {
            StudioDraw.small(g, font(), words(ToolTexts.NO_CLIENTS), x, area.y() + StudioDraw.HEAD + 4,
                    area.width() - 2 * StudioDraw.INSET, colours.dim());
        }
        final SimpleDateFormat clock = new SimpleDateFormat(TIME);
        for (int i = 0; i < clients.size(); i++) {
            final PairedClients.PairedClient client = clients.get(i);
            final int y = area.y() + StudioDraw.HEAD + 4 + i * CLIENT_ROW;
            if (y + CLIENT_ROW > this.buttonY(area)) {
                break;
            }
            if (i == this.chosen) {
                g.fill(area.x() + 3, y - 3, area.right() - 3, y + CLIENT_ROW - 5, colours.selected());
                Draw.outline(g, area.x() + 3, y - 3, area.width() - 6, CLIENT_ROW - 2, colours.outline());
            }
            final boolean acts = client.permission() == McpPermission.ACT;
            g.fill(x, y + 1, x + 5, y + 6, acts ? colours.warning() : colours.fine());
            StudioDraw.text(g, font(), client.toString(), x + 10, y, area.width() - 26,
                    i == this.chosen ? colours.text() : colours.label());
            StudioDraw.small(g, font(), words(acts ? McpWords.READS_AND_ACTS : McpWords.READS), x + 10, y + 10,
                    area.width() - 26, acts ? colours.items() : colours.fine());
            StudioDraw.small(g, font(), GameText.resolve(ToolTexts.SINCE.with(clock.format(new Date(
                    client.since())), client.calls())), x + 10, y + 18, area.width() - 26, colours.dim());
        }
        final int noteY = this.buttonY(area) + 22;
        StudioDraw.smallWrapped(g, font(), words(ToolTexts.KEY_NOTE), x, noteY, area.width() - 2 * StudioDraw.INSET,
                colours.dim());
        final int modsY = this.modsY(area);
        StudioDraw.small(g, font(), words(ToolTexts.OTHER_TOOLS), x, modsY, area.width(), colours.text());
        if (this.mods.isEmpty()) {
            StudioDraw.small(g, font(), words(ToolTexts.NO_OTHER_TOOLS), x, modsY + 14,
                    area.width() - 2 * StudioDraw.INSET, colours.dim());
        }
        int y = modsY + 12;
        for (final String mod : this.mods.keySet()) {
            StudioDraw.text(g, font(), StudioThings.modName(mod), x, y + 3, area.width() - 80, colours.label());
            y += MOD_ROW;
        }
    }

    private void callsPanel(final GuiGraphics g, final StudioLayout.Rect area, final Optional<McpServer> running,
                            final StudioPalette.Colours colours) {
        StudioDraw.panel(g, font(), area, words(ToolTexts.EVERY_CALL));
        final int[] at = {area.x() + StudioDraw.INSET, area.x() + area.width() * 14 / 100,
            area.x() + area.width() * 36 / 100, area.x() + area.width() * 84 / 100};
        final List<TextKey> heads = List.of(ToolTexts.WHEN, ToolTexts.CLIENT, ToolTexts.TOOL_CALLED,
                ToolTexts.ANSWER);
        for (int i = 0; i < heads.size(); i++) {
            StudioDraw.small(g, font(), words(heads.get(i)), at[i], area.y() + StudioDraw.HEAD + 2,
                    area.width() / 4, colours.text());
        }
        final List<McpCallLog.Entry> entries = running.map(server -> server.protocol().log().entries())
                .orElse(List.of());
        if (entries.isEmpty()) {
            StudioDraw.small(g, font(), words(ToolTexts.NO_CALLS), at[0], area.y() + StudioDraw.HEAD + 16,
                    area.width(), colours.dim());
            return;
        }
        final SimpleDateFormat clock = new SimpleDateFormat(TIME);
        final List<McpCallLog.Entry> shown = new ArrayList<>(entries);
        McpCallLog.Entry refused = null;
        for (int i = 0; i < shown.size(); i++) {
            final McpCallLog.Entry entry = shown.get(i);
            if (refused == null && entry.answer() == McpWords.REFUSED && entry.why() != null) {
                refused = entry;
            }
            final int y = area.y() + StudioDraw.HEAD + 16 + i * CALL_ROW;
            if (y + CALL_ROW > area.bottom() - 16) {
                continue;
            }
            StudioDraw.small(g, font(), clock.format(new Date(entry.millis())), at[0], y, at[1] - at[0] - 4,
                    colours.dim());
            StudioDraw.small(g, font(), entry.client(), at[1], y, at[2] - at[1] - 4, colours.label());
            StudioDraw.small(g, font(), (entry.tool() + " " + entry.what()).strip(), at[2], y, at[3] - at[2] - 4,
                    colours.label());
            StudioDraw.small(g, font(), words(entry.answer()), at[3], y, area.right() - at[3] - 4,
                    colourOf(entry.answer(), colours));
        }
        if (refused != null) {
            StudioDraw.small(g, font(), GameText.resolve(ToolTexts.REFUSED_WHY.with(refused.why())), at[0],
                    area.bottom() - 12, area.width() - 2 * StudioDraw.INSET, colours.dim());
        }
    }

    private static int colourOf(final TextKey answer, final StudioPalette.Colours colours) {
        if (answer == McpWords.REFUSED || answer == McpWords.FAILED) {
            return colours.error();
        }
        if (answer == McpWords.ACTING || answer == McpWords.TAKEN_BACK) {
            return colours.warning();
        }
        return colours.fine();
    }
}
