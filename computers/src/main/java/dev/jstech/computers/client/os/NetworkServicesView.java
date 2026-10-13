/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.engine.EngineSwap;
import dev.jstech.computers.gui.layout.NetworkServicesLayout;
import dev.jstech.computers.operation.payload.EngineActionPayload;
import dev.jstech.computers.operation.payload.NetworkServicesPayload;
import dev.jstech.computers.operation.payload.RequestNetworkServicesPayload;
import dev.jstech.core.client.gui.component.Button;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.gui.component.Grounds;
import dev.jstech.core.client.gui.component.Popup;
import dev.jstech.core.client.gui.component.ProgressBar;
import dev.jstech.core.client.gui.component.ScrollPanel;
import dev.jstech.core.client.gui.component.Texts;
import dev.jstech.core.client.gui.component.UiComponent;
import dev.jstech.core.client.gui.component.UiContext;
import dev.jstech.core.client.gui.theme.JsTechTheme;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * The Network Manager's Services tab: the engine that plans the network's work, with Stop, Start and Replace; the
 * engines installed beside it; the Subframes, marked where one takes no work; and the other services. Replacing the
 * engine asks first, with what will happen, then shows the steps as the new engine comes up.
 *
 * <p>The tab scrolls, and every position on it is read from {@link NetworkServicesLayout}, the same numbers its test
 * checks. The two dialogs are drawn in the window's modal pass.
 */
@PaletteHolder
final class NetworkServicesView {

    private final BlockPos host;
    private final ScrollPanel body = new ScrollPanel();
    private final Sheet sheet = new Sheet();
    private final Button configure;
    private final Button stop;
    private final Button start;
    private final Button replace;
    private final Popup chooser;
    private final Options options = new Options();
    private final Button chooserCancel;
    private final Button chooserConfirm;
    private final Popup progress;
    private final Steps steps = new Steps();
    private final ProgressBar bar;
    private OsSkin skin = OsSkin.fallback();
    @Nullable
    private Font font;
    @Nullable
    private NetworkServicesPayload data;
    /** The client's game time when {@link #data} arrived, from which a replacement's progress runs on. */
    private long receivedAt;
    /** Whether the dialog starts an engine rather than replacing the running one. */
    private boolean starting;
    /** The engine chosen in the dialog, by its package, or empty. */
    private String chosen = "";
    /** Whether the player put away the steps of the replacement under way; they stay away until it is over. */
    private boolean progressDismissed;
    private int frame;
    private int lastX;
    private int lastY;
    private int lastW;
    private int lastH;

    private static final int REFRESH_FRAMES = 100;
    private static final int REPLACING_REFRESH_FRAMES = 20;
    private static final int CHOOSER_W = 260;
    private static final int PROGRESS_W = 270;
    private static final int OPTION_H = 12;
    private static final int STEP_H = 10;
    private static final int DIALOG_BUTTON_H = 13;
    private static final int TICKS_PER_SECOND = 20;
    private static final int TICKS_PER_MINUTE = 60 * TICKS_PER_SECOND;
    private static final int MINUTES_PER_DAY = 24 * 60;

    /** The engines' and Subframes' squares, the states' colours and the note's ground: app/network_services. */
    private static final Palette<Colours> PALETTE = Palettes.declare(JsComputers.MODID, "app/network_services",
            new Colours(0xFF3A6AE0, 0xFF7B52C9, 0xFF9A9A9A, 0xFF2EA043, 0xFFE0A020, 0x30E0A020, 0xFFE8C98A,
                    0x22E0A020, 0xB0000000));

    NetworkServicesView(final BlockPos host) {
        this.host = host;
        body.add(sheet);
        configure = body.add(new Button(GameText.resolve(NetworkServicesTexts.CONFIGURE), () -> { }));
        // What the engine's settings are is not decided yet, so the button is there and does nothing.
        configure.setEnabled(false);
        stop = body.add(new Button(GameText.resolve(NetworkServicesTexts.STOP),
                () -> send(EngineActionPayload.STOP, "")));
        start = body.add(new Button(GameText.resolve(NetworkServicesTexts.START), () -> openChooser(true))
                .setPrimary(true));
        replace = body.add(new Button(GameText.resolve(NetworkServicesTexts.REPLACE), () -> openChooser(false))
                .setPrimary(true));

        chooser = new Popup(() -> GameText.resolve(starting ? NetworkServicesTexts.START_TITLE
                : NetworkServicesTexts.REPLACE_TITLE), CHOOSER_W, 120).setDim(colours().dialogDim())
                .setLayouter(this::layoutChooser);
        chooser.add(options);
        chooserCancel = chooser.add(new Button(GameText.resolve(NetworkServicesTexts.CANCEL), chooser::close));
        chooserConfirm = chooser.add(new Button(() -> GameText.resolve(starting ? NetworkServicesTexts.START_BUTTON
                : NetworkServicesTexts.REPLACE_BUTTON), this::confirmChooser).setPrimary(true));

        progress = new Popup(this::progressTitle, PROGRESS_W,
                22 + EngineSwap.Step.values().length * STEP_H + 26).setDim(colours().dialogDim())
                .setLayouter(this::layoutProgress).setOnClose(() -> progressDismissed = replacementUnderWay());
        progress.add(steps);
        bar = progress.add(new ProgressBar(this::percentDone));
    }

    /** The scrolling tab, which the window adds to its own components. */
    UiComponent component() {
        return body;
    }

    /** Takes what the server says the tab shows, if it is about this window's Mainframe. */
    void accept(final NetworkServicesPayload payload) {
        if (!payload.hostPos().equals(host)) {
            return;
        }
        data = payload;
        receivedAt = clientTime();
        if (!payload.replacement().underWay()) {
            progressDismissed = false;
            progress.close();
        } else if (!progressDismissed && !progress.isOpen()) {
            progress.open();
            progress.placeIn(lastX, lastY, lastW, lastH);
        }
    }

    /** Asks the server what the tab shows. */
    void request() {
        PacketDistributor.sendToServer(new RequestNetworkServicesPayload(host));
    }

    /** Whether the server has answered yet. */
    boolean hasState() {
        return data != null;
    }

    /** The state of the engine on the card, one of the payload's {@code ENGINE_} states. */
    byte engineState() {
        return data == null ? NetworkServicesPayload.ENGINE_NONE : data.engine().state();
    }

    /** The step a replacement under way is on, or {@code null} when none is. */
    @Nullable
    EngineSwap.Step replacementStep() {
        if (!replacementUnderWay()) {
            return null;
        }
        return EngineSwap.stepAt(elapsed(), data.replacement().ticks());
    }

    boolean chooserOpen() {
        return chooser.isOpen();
    }

    boolean progressOpen() {
        return progress.isOpen();
    }

    /** The centre of a card button, where a test clicks it: {@code stop}, {@code start} or {@code replace}. */
    int[] buttonPoint(final String which) {
        return switch (which) {
            case "stop" -> stop.center();
            case "start" -> start.center();
            default -> replace.center();
        };
    }

    /** The centre of the dialog's line for {@code program}, where a test clicks to choose it. */
    int[] optionPoint(final String program) {
        final List<NetworkServicesPayload.EngineRow> rows = rows();
        for (int i = 0; i < rows.size(); i++) {
            if (rows.get(i).program().equals(program)) {
                return new int[] {options.x() + options.width() / 2, options.y() + i * OPTION_H + OPTION_H / 2};
            }
        }
        return options.center();
    }

    int[] confirmPoint() {
        return chooserConfirm.center();
    }

    /** Lays the tab out in the rectangle the window gives it, and keeps it fresh while it shows. */
    void layout(final int x, final int y, final int width, final int height, final boolean shown, final OsSkin osSkin,
                final Font measure) {
        skin = osSkin;
        font = measure;
        lastX = x;
        lastY = y;
        lastW = width;
        lastH = height;
        body.setVisible(shown && data != null);
        if (!shown) {
            return;
        }
        frame++;
        if (frame % (replacementUnderWay() ? REPLACING_REFRESH_FRAMES : REFRESH_FRAMES) == 0) {
            request();
        }
        if (data == null) {
            return;
        }
        body.setBounds(x, y, width, height);
        final int contentW = width - NetworkServicesLayout.THUMB_ROOM;
        final NetworkServicesLayout.Sections at = sections();
        body.setContentHeight(at.height());
        sheet.setBounds(x, body.contentY(0), contentW, at.height());

        final byte state = data.engine().state();
        final boolean running = state == NetworkServicesPayload.ENGINE_RUNNING;
        final boolean replacing = state == NetworkServicesPayload.ENGINE_REPLACING;
        final List<Button> shownButtons = new ArrayList<>();
        if (running || replacing) {
            shownButtons.add(configure);
            shownButtons.add(stop);
            shownButtons.add(replace);
        } else {
            shownButtons.add(start);
        }
        configure.setVisible(shownButtons.contains(configure));
        stop.setVisible(shownButtons.contains(stop));
        replace.setVisible(shownButtons.contains(replace));
        start.setVisible(shownButtons.contains(start));
        stop.setEnabled(running);
        replace.setEnabled(running);
        start.setEnabled(!data.installed().isEmpty());
        final int[] widths = new int[shownButtons.size()];
        for (int i = 0; i < widths.length; i++) {
            widths[i] = measure.width(shownButtons.get(i).label()) + 12;
        }
        final int[] xs = NetworkServicesLayout.buttonsX(contentW, widths);
        for (int i = 0; i < xs.length; i++) {
            shownButtons.get(i).setBounds(x + xs[i], body.contentY(NetworkServicesLayout.CARD_ACTIONS_Y), widths[i],
                    NetworkServicesLayout.BUTTON_H);
        }
    }

    boolean modalOpen() {
        return chooser.isOpen() || progress.isOpen();
    }

    /** Closes both dialogs, as switching tabs does. */
    void closeDialogs() {
        chooser.close();
        progress.close();
    }

    void renderModal(final GuiGraphics g, final UiContext ctx, final int x, final int y, final int width,
                     final int height) {
        if (chooser.isOpen()) {
            chooser.renderIn(g, ctx, x, y, width, height);
        } else if (progress.isOpen()) {
            if (replacementUnderWay() && elapsed() >= data.replacement().ticks()
                    && frame % REPLACING_REFRESH_FRAMES == 0) {
                request();
            }
            progress.renderIn(g, ctx, x, y, width, height);
        }
    }

    /** The open dialog, which takes every click and key while it is open; {@code null} when none is. */
    @Nullable
    Popup openDialog() {
        return chooser.isOpen() ? chooser : progress.isOpen() ? progress : null;
    }

    // what the tab reads

    private boolean replacementUnderWay() {
        return data != null && data.replacement().underWay();
    }

    private List<NetworkServicesPayload.EngineRow> rows() {
        return data == null ? List.of() : data.installed();
    }

    private NetworkServicesLayout.Sections sections() {
        return NetworkServicesLayout.sections(data.installed().size(), data.subframes().size(), mismatch() != null,
                data.services().size());
    }

    /** The first Subframe that takes no work, which the note under the list speaks of; {@code null} when none. */
    @Nullable
    private NetworkServicesPayload.SubframeRow mismatch() {
        if (data == null) {
            return null;
        }
        for (final NetworkServicesPayload.SubframeRow row : data.subframes()) {
            if (!row.takesWork()) {
                return row;
            }
        }
        return null;
    }

    /** How far the replacement under way has gone, run on from what the server said by the client's own clock. */
    private long elapsed() {
        final NetworkServicesPayload.Replacement replacement = data.replacement();
        return Math.min(replacement.ticks(), replacement.elapsed() + Math.max(0L, clientTime() - receivedAt));
    }

    private int percentDone() {
        return replacementUnderWay() ? EngineSwap.permille(elapsed(), data.replacement().ticks()) / 10 : 100;
    }

    private static long clientTime() {
        final Minecraft minecraft = Minecraft.getInstance();
        return minecraft.level == null ? 0L : minecraft.level.getGameTime();
    }

    private void send(final byte action, final String program) {
        PacketDistributor.sendToServer(new EngineActionPayload(host, action, program));
    }

    // the dialog that replaces or starts an engine

    private void openChooser(final boolean toStart) {
        if (data == null) {
            return;
        }
        starting = toStart;
        chosen = "";
        for (final NetworkServicesPayload.EngineRow row : rows()) {
            final boolean pick = toStart ? row.state() == NetworkServicesPayload.ROW_STOPPED
                    : row.state() == NetworkServicesPayload.ROW_INSTALLED;
            if (pick && chosen.isEmpty()) {
                chosen = row.program();
            }
        }
        if (toStart && chosen.isEmpty() && !rows().isEmpty()) {
            chosen = rows().getFirst().program();
        }
        chooser.open();
        chooser.placeIn(lastX, lastY, lastW, lastH);
    }

    private void confirmChooser() {
        if (!chooserReady()) {
            return;
        }
        send(starting ? EngineActionPayload.START : EngineActionPayload.REPLACE, chosen);
        progressDismissed = false;
        chooser.close();
    }

    /** Whether the engine chosen in the dialog is one it can start or replace with. */
    private boolean chooserReady() {
        final NetworkServicesPayload.EngineRow row = chosenRow();
        return row != null && (starting || row.state() != NetworkServicesPayload.ROW_ACTIVE);
    }

    @Nullable
    private NetworkServicesPayload.EngineRow chosenRow() {
        for (final NetworkServicesPayload.EngineRow row : rows()) {
            if (row.program().equals(chosen)) {
                return row;
            }
        }
        return null;
    }

    /** What the dialog says will happen, as lines wrapped to {@code width}. */
    private List<String> chooserNote(final int width) {
        final List<String> sentences = new ArrayList<>();
        final NetworkServicesPayload.EngineRow row = chosenRow();
        if (starting) {
            sentences.add(GameText.resolve(NetworkServicesTexts.START_NOTE));
        } else if (rows().size() < 2) {
            sentences.add(GameText.resolve(NetworkServicesTexts.REPLACE_NOTHING_ELSE));
        } else if (row != null && row.state() != NetworkServicesPayload.ROW_ACTIVE) {
            final String to = nameAndVersion(row.name(), row.version());
            sentences.add(GameText.resolve(NetworkServicesTexts.REPLACE_NEW_REQUESTS.with(to)));
            sentences.add(GameText.resolve(NetworkServicesTexts.REPLACE_IN_FLIGHT.with(data.engine().inFlight())));
            if (!data.engine().vendor().isEmpty()) {
                sentences.add(GameText.resolve(NetworkServicesTexts.REPLACE_SCRIPTS.with(data.engine().vendor())));
            }
            final List<String> left = new ArrayList<>();
            for (final NetworkServicesPayload.SubframeRow subframe : data.subframes()) {
                if (!subframe.software().isEmpty() && !subframe.software().equals(row.program())) {
                    left.add(subframe.name());
                }
            }
            if (!left.isEmpty()) {
                sentences.add(GameText.resolve(NetworkServicesTexts.REPLACE_SUBFRAMES.with(String.join(", ", left),
                        row.name())));
            }
        }
        return wrap(String.join(" ", sentences), width);
    }

    private void layoutChooser(final Popup p) {
        final int noteW = p.width() - 20;
        final int noteLines = font == null ? 3 : chooserNote(noteW).size();
        final int optionsH = Math.max(1, rows().size()) * OPTION_H;
        // The dialog grows with the engines listed and the note; the size is taken up on the next frame.
        p.setPreferredSize(CHOOSER_W, 18 + optionsH + 6 + noteLines * NetworkServicesLayout.LINE_H + 8 + 6
                + DIALOG_BUTTON_H + 6);
        options.setBounds(p.x() + 6, p.contentTop() + 2, p.width() - 12, optionsH);
        final int confirmW = (font == null ? 40 : font.width(chooserConfirm.label())) + 14;
        final int cancelW = (font == null ? 40 : font.width(chooserCancel.label())) + 14;
        chooserConfirm.setBounds(p.right() - 6 - confirmW, p.bottom() - 6 - DIALOG_BUTTON_H, confirmW,
                DIALOG_BUTTON_H);
        chooserCancel.setBounds(chooserConfirm.x() - 4 - cancelW, chooserConfirm.y(), cancelW, DIALOG_BUTTON_H);
        chooserConfirm.setEnabled(chooserReady());
    }

    private void renderChooserNote(final GuiGraphics g, final Font f) {
        final int x = chooser.x() + 6;
        final int y = options.bottom() + 6;
        final int w = chooser.width() - 12;
        final List<String> lines = chooserNote(w - 8);
        final int h = lines.size() * NetworkServicesLayout.LINE_H + 6;
        Grounds.fill(g, x, y, x + w, y + h, colours().noteGround());
        Draw.outline(g, x, y, w, h, colours().noteEdge());
        for (int i = 0; i < lines.size(); i++) {
            Draw.text(g, f, lines.get(i), x + 4, y + 4 + i * NetworkServicesLayout.LINE_H, skin.text());
        }
    }

    // a replacement under way

    private String progressTitle() {
        if (!replacementUnderWay()) {
            return "";
        }
        final NetworkServicesPayload.Replacement replacement = data.replacement();
        final String title = GameText.resolve(replacement.from().isEmpty()
                ? NetworkServicesTexts.REPLACING_FROM_NONE.with(replacement.to())
                : NetworkServicesTexts.REPLACING_TITLE.with(replacement.from(), replacement.to()));
        return font == null ? title : Texts.clip(font, title, progress.width() - 10);
    }

    private void layoutProgress(final Popup p) {
        final int stepsH = EngineSwap.Step.values().length * STEP_H;
        steps.setBounds(p.x() + 8, p.contentTop() + 2, p.width() - 16, stepsH);
        bar.setBounds(p.x() + 8, steps.bottom() + 4, p.width() - 16, 6);
    }

    private String stepLabel(final EngineSwap.Step step) {
        final NetworkServicesPayload.Replacement replacement = data.replacement();
        return GameText.resolve(switch (step) {
            case STOP_NEW_PLANS -> NetworkServicesTexts.STEP_STOP_NEW_PLANS.text();
            case KEEP_IN_FLIGHT -> NetworkServicesTexts.STEP_KEEP_IN_FLIGHT.with(replacement.inFlight());
            case STOP_OLD -> replacement.from().isEmpty() ? NetworkServicesTexts.STEP_STOP_NONE.text()
                    : NetworkServicesTexts.STEP_STOP_OLD.with(replacement.from());
            case START_NEW -> NetworkServicesTexts.STEP_START_NEW.with(replacement.to());
            case DISCOVER -> NetworkServicesTexts.STEP_DISCOVER.text();
            case BUILD_INDEXES -> NetworkServicesTexts.STEP_BUILD_INDEXES.text();
            case PUBLISH -> NetworkServicesTexts.STEP_PUBLISH.text();
            case READY -> NetworkServicesTexts.STEP_READY.text();
        });
    }

    // drawing the tab

    private void renderCard(final GuiGraphics g, final Font f, final int x, final int y, final int w) {
        final NetworkServicesPayload.Engine engine = data.engine();
        skin.panel(g, x, y, w, NetworkServicesLayout.CARD_H);
        final int nameY = y + NetworkServicesLayout.CARD_NAME_Y;
        final int nameX = x + NetworkServicesLayout.NAME_X;
        final boolean live = engine.chosen() && engine.state() != NetworkServicesPayload.ENGINE_STOPPED;
        g.fill(x + NetworkServicesLayout.PAD, nameY + 1, x + NetworkServicesLayout.PAD + NetworkServicesLayout.SQUARE,
                nameY + 1 + NetworkServicesLayout.SQUARE, live ? colours().engine() : colours().idle());
        final int actionsY = y + NetworkServicesLayout.CARD_ACTIONS_Y + 2;
        final int buttonsLeft = leftmostButton() - 3;
        if (!live) {
            Draw.text(g, f, Texts.clip(f, GameText.resolve(NetworkServicesTexts.NONE_RUNNING),
                    w - NetworkServicesLayout.NAME_X - NetworkServicesLayout.PAD), nameX, nameY, skin.text());
            final List<String> note = wrap(GameText.resolve(data.installed().isEmpty()
                    ? NetworkServicesTexts.NONE_INSTALLED : NetworkServicesTexts.NONE_NOTE),
                    w - 2 * NetworkServicesLayout.PAD);
            for (int i = 0; i < note.size() && i < NetworkServicesLayout.OFF_NOTE_LINES; i++) {
                Draw.text(g, f, note.get(i), x + NetworkServicesLayout.PAD,
                        y + NetworkServicesLayout.CARD_FACTS_Y + i * NetworkServicesLayout.LINE_H, skin.dim());
            }
            return;
        }
        final String title = nameAndVersion(engine.name(), engine.version());
        final String chip = GameText.resolve(switch (engine.state()) {
            case NetworkServicesPayload.ENGINE_REPLACING -> NetworkServicesTexts.REPLACING;
            case NetworkServicesPayload.ENGINE_RUNNING -> NetworkServicesTexts.RUNNING;
            default -> NetworkServicesTexts.STOPPED;
        });
        final int chipW = f.width(chip) + 6;
        final String shownTitle = Texts.clip(f, title, w - NetworkServicesLayout.NAME_X - chipW - 8);
        Draw.text(g, f, shownTitle, nameX, nameY, skin.text());
        final int chipX = nameX + f.width(shownTitle) + 5;
        final int chipColour = engine.state() == NetworkServicesPayload.ENGINE_RUNNING ? colours().good()
                : engine.state() == NetworkServicesPayload.ENGINE_REPLACING ? skin.accent() : skin.dim();
        Draw.outline(g, chipX, nameY - 2, chipW, 11, chipColour);
        Draw.text(g, f, chip, chipX + 3, nameY, chipColour);
        Draw.text(g, f, Texts.clip(f, GameText.resolve(NetworkServicesTexts.ON_HOST.with(engine.host())),
                buttonsLeft - nameX), nameX, actionsY, skin.dim());

        final int factsY = y + NetworkServicesLayout.CARD_FACTS_Y;
        final int second = x + NetworkServicesLayout.secondFactX(w);
        final int valueRoom = NetworkServicesLayout.secondFactX(w) - NetworkServicesLayout.PAD
                - NetworkServicesLayout.factValueOffset() - 4;
        fact(g, f, x + NetworkServicesLayout.PAD, factsY, NetworkServicesTexts.DIALECT, engine.dialect(), valueRoom);
        fact(g, f, second, factsY, NetworkServicesTexts.MEMORY,
                GameText.resolve(NetworkServicesTexts.MEGABYTES.with(engine.memoryMb())), valueRoom);
        final int line2 = factsY + NetworkServicesLayout.LINE_H;
        fact(g, f, x + NetworkServicesLayout.PAD, line2, NetworkServicesTexts.UP, upTime(engine.upTicks()), valueRoom);
        fact(g, f, second, line2, NetworkServicesTexts.PLANS_TODAY, JsTechTheme.fmt(engine.plansToday()), valueRoom);
        final int wholeRoom = w - 2 * NetworkServicesLayout.PAD - NetworkServicesLayout.factValueOffset();
        fact(g, f, x + NetworkServicesLayout.PAD, line2 + NetworkServicesLayout.LINE_H, NetworkServicesTexts.INDEXES,
                GameText.resolve(NetworkServicesTexts.INDEXES_VALUE.with(JsTechTheme.fmt(engine.itemTypes()),
                        engine.servers())), wholeRoom);
        fact(g, f, x + NetworkServicesLayout.PAD, line2 + 2 * NetworkServicesLayout.LINE_H,
                NetworkServicesTexts.CAPABILITIES, capabilities(engine), wholeRoom);
    }

    private void fact(final GuiGraphics g, final Font f, final int x, final int y, final TextKey label,
                      final String value, final int room) {
        Draw.text(g, f, Texts.clip(f, GameText.resolve(label), NetworkServicesLayout.factValueOffset() - 4), x, y,
                skin.dim());
        Draw.text(g, f, Texts.clip(f, value, room), x + NetworkServicesLayout.factValueOffset(), y, skin.text());
    }

    private void renderLists(final GuiGraphics g, final Font f, final int x, final int w) {
        final NetworkServicesLayout.Sections at = sections();
        final int top = sheet.y();
        final int vendorX = x + NetworkServicesLayout.secondColumnX(w);
        final int versionX = x + NetworkServicesLayout.versionX(w);
        heading(g, f, x, top + at.installedHeading(),
                GameText.resolve(NetworkServicesTexts.INSTALLED_ON.with(data.engine().host())));
        final int columnsY = top + at.installedColumns();
        Draw.text(g, f, GameText.resolve(NetworkServicesTexts.ENGINE_COLUMN), x + NetworkServicesLayout.PAD,
                columnsY + 2, skin.dim());
        Draw.text(g, f, GameText.resolve(NetworkServicesTexts.VENDOR_COLUMN), vendorX, columnsY + 2, skin.dim());
        Draw.text(g, f, GameText.resolve(NetworkServicesTexts.VERSION_COLUMN), versionX, columnsY + 2, skin.dim());
        rightText(g, f, GameText.resolve(NetworkServicesTexts.STATE_COLUMN), x + w, columnsY + 2, skin.dim());
        g.fill(x, columnsY + NetworkServicesLayout.COLUMNS_H - 1, x + w, columnsY + NetworkServicesLayout.COLUMNS_H,
                skin.edge());
        final List<NetworkServicesPayload.EngineRow> engines = data.installed();
        for (int i = 0; i < engines.size(); i++) {
            final NetworkServicesPayload.EngineRow row = engines.get(i);
            final int rowY = top + at.installedRows() + i * NetworkServicesLayout.ROW_H;
            final int textY = rowY + 2;
            Draw.text(g, f, Texts.clip(f, row.name(), vendorX - x - 8), x + NetworkServicesLayout.PAD, textY,
                    skin.text());
            Draw.text(g, f, Texts.clip(f, row.vendor(), versionX - vendorX - 4), vendorX, textY, skin.text());
            Draw.text(g, f, row.version(), versionX, textY, skin.text());
            rightText(g, f, GameText.resolve(rowWord(row.state())), x + w, textY, rowColour(row.state()));
            rule(g, x, rowY + NetworkServicesLayout.ROW_H - 1, w);
        }
        if (engines.isEmpty()) {
            Draw.text(g, f, GameText.resolve(NetworkServicesTexts.NONE_INSTALLED), x + NetworkServicesLayout.PAD,
                    top + at.installedRows() + 2, skin.dim());
        }

        heading(g, f, x, top + at.subframesHeading(), GameText.resolve(NetworkServicesTexts.SUBFRAMES));
        final List<NetworkServicesPayload.SubframeRow> subframes = data.subframes();
        for (int i = 0; i < subframes.size(); i++) {
            final NetworkServicesPayload.SubframeRow row = subframes.get(i);
            final int rowY = top + at.subframeRows() + i * NetworkServicesLayout.ROW_H;
            if (!row.takesWork()) {
                Grounds.fill(g, x, rowY, x + w, rowY + NetworkServicesLayout.ROW_H - 1, colours().hotRow());
            }
            final int squareX = x + NetworkServicesLayout.PAD;
            g.fill(squareX, rowY + 3, squareX + NetworkServicesLayout.SQUARE, rowY + 3 + NetworkServicesLayout.SQUARE,
                    colours().subframe());
            Draw.text(g, f, row.name(), x + NetworkServicesLayout.NAME_X, rowY + 2, skin.text());
            final String engine = row.engine().isEmpty()
                    ? GameText.resolve(NetworkServicesTexts.FOLLOWS_MAINFRAME.with(data.engine().host()))
                    : row.engine();
            Draw.text(g, f, Texts.clip(f, engine, w - (vendorX - x) - 70), vendorX, rowY + 2,
                    row.engine().isEmpty() ? skin.dim() : skin.text());
            rightText(g, f, GameText.resolve(row.takesWork() ? NetworkServicesTexts.MATCHES
                    : NetworkServicesTexts.TAKES_NO_WORK), x + w, rowY + 2,
                    row.takesWork() ? colours().good() : colours().warn());
            rule(g, x, rowY + NetworkServicesLayout.ROW_H - 1, w);
        }
        if (subframes.isEmpty()) {
            Draw.text(g, f, GameText.resolve(NetworkServicesTexts.NO_SUBFRAMES), x + NetworkServicesLayout.PAD,
                    top + at.subframeRows() + 2, skin.dim());
        }
        final NetworkServicesPayload.SubframeRow odd = mismatch();
        if (odd != null) {
            renderMismatchNote(g, f, odd, x, top + at.note(), w);
        }

        heading(g, f, x, top + at.servicesHeading(), GameText.resolve(NetworkServicesTexts.OTHER_SERVICES));
        final List<NetworkServicesPayload.ServiceRow> services = data.services();
        for (int i = 0; i < services.size(); i++) {
            final NetworkServicesPayload.ServiceRow row = services.get(i);
            final int rowY = top + at.serviceRows() + i * NetworkServicesLayout.ROW_H;
            Draw.text(g, f, Texts.clip(f, row.name(), vendorX - x - 8), x + NetworkServicesLayout.PAD, rowY + 2,
                    skin.text());
            Draw.text(g, f, Texts.clip(f, row.vendor(), versionX - vendorX - 4), vendorX, rowY + 2, skin.text());
            Draw.text(g, f, row.version(), versionX, rowY + 2, skin.text());
            final boolean on = row.state() == NetworkServicesPayload.SERVICE_RUNNING;
            rightText(g, f, GameText.resolve(on ? NetworkServicesTexts.SERVICE_RUNNING
                    : NetworkServicesTexts.SERVICE_ABSENT), x + w, rowY + 2, on ? colours().good() : skin.dim());
            rule(g, x, rowY + NetworkServicesLayout.ROW_H - 1, w);
        }
    }

    private void renderMismatchNote(final GuiGraphics g, final Font f, final NetworkServicesPayload.SubframeRow odd,
                                    final int x, final int y, final int w) {
        final NetworkServicesPayload.Engine engine = data.engine();
        final String text = engine.chosen()
                ? GameText.resolve(NetworkServicesTexts.MISMATCH_NOTE.with(odd.name(), engine.host(), engine.name(),
                        engine.host(), odd.engine()))
                : GameText.resolve(NetworkServicesTexts.MISMATCH_NOTE_NONE.with(odd.name(), odd.engine(),
                        engine.host()));
        Grounds.fill(g, x, y, x + w, y + NetworkServicesLayout.NOTE_H, colours().noteGround());
        Draw.outline(g, x, y, w, NetworkServicesLayout.NOTE_H, colours().noteEdge());
        final List<String> lines = wrap(text, w - 8);
        final int last = NetworkServicesLayout.NOTE_LINES - 1;
        for (int i = 0; i < lines.size() && i <= last; i++) {
            // A note longer than its room ends cut on its last line, with the rest of it marked as cut.
            final String line = i == last && lines.size() > NetworkServicesLayout.NOTE_LINES
                    ? Texts.clip(f, lines.get(i) + " " + lines.get(i + 1), w - 8) : lines.get(i);
            Draw.text(g, f, line, x + 4, y + 3 + i * NetworkServicesLayout.LINE_H, skin.text());
        }
    }

    private void heading(final GuiGraphics g, final Font f, final int x, final int y, final String text) {
        Draw.text(g, f, text, x + 1, y + 1, skin.dim());
    }

    private void rule(final GuiGraphics g, final int x, final int y, final int w) {
        g.fill(x, y, x + w, y + 1, skin.edge());
    }

    private static void rightText(final GuiGraphics g, final Font f, final String text, final int right, final int y,
                                  final int colour) {
        Draw.text(g, f, text, right - NetworkServicesLayout.PAD - f.width(text), y, colour);
    }

    /** The left edge of the card's leftmost button showing, where the host line has to stop. */
    private int leftmostButton() {
        int left = Integer.MAX_VALUE;
        for (final Button button : List.of(configure, stop, start, replace)) {
            if (button.visible()) {
                left = Math.min(left, button.x());
            }
        }
        return left == Integer.MAX_VALUE ? sheet.right() : left;
    }

    private static TextKey rowWord(final byte state) {
        return switch (state) {
            case NetworkServicesPayload.ROW_ACTIVE -> NetworkServicesTexts.ROW_ACTIVE;
            case NetworkServicesPayload.ROW_STOPPED -> NetworkServicesTexts.ROW_STOPPED;
            case NetworkServicesPayload.ROW_STARTING -> NetworkServicesTexts.ROW_STARTING;
            default -> NetworkServicesTexts.ROW_INSTALLED;
        };
    }

    private int rowColour(final byte state) {
        return switch (state) {
            case NetworkServicesPayload.ROW_ACTIVE -> colours().good();
            case NetworkServicesPayload.ROW_STARTING -> skin.accent();
            default -> skin.dim();
        };
    }

    /** What every engine does, then the extras this one offers, in the player's words. */
    private static String capabilities(final NetworkServicesPayload.Engine engine) {
        final List<String> words = new ArrayList<>();
        words.add(GameText.resolve(NetworkServicesTexts.EVERY_ENGINE));
        for (final String name : engine.capabilities()) {
            final TextKey key = switch (name) {
                case "procedures_and_views" -> NetworkServicesTexts.PROCEDURES_AND_VIEWS;
                case "declarative_state" -> NetworkServicesTexts.DECLARATIVE_STATE;
                case "subscriptions" -> NetworkServicesTexts.SUBSCRIPTIONS;
                case "explain" -> NetworkServicesTexts.EXPLAIN;
                case "planner_hints" -> NetworkServicesTexts.PLANNER_HINTS;
                case "extensions" -> NetworkServicesTexts.EXTENSIONS;
                default -> null;
            };
            words.add(key == null ? name : GameText.resolve(key));
        }
        return String.join(", ", words);
    }

    /** How long an engine has been up, in days and hours, hours and minutes, or minutes. */
    private static String upTime(final long ticks) {
        final long minutes = ticks / TICKS_PER_MINUTE;
        if (minutes >= MINUTES_PER_DAY) {
            return GameText.resolve(NetworkServicesTexts.DAYS_HOURS.with(minutes / MINUTES_PER_DAY,
                    minutes % MINUTES_PER_DAY / 60));
        }
        if (minutes >= 60) {
            return GameText.resolve(NetworkServicesTexts.HOURS_MINUTES.with(minutes / 60, minutes % 60));
        }
        return GameText.resolve(NetworkServicesTexts.MINUTES.with(minutes));
    }

    private static String nameAndVersion(final String name, final String version) {
        return version.isEmpty() ? name : name + " " + version;
    }

    /** {@code text} broken into lines no wider than {@code width}, at the spaces. */
    private List<String> wrap(final String text, final int width) {
        final List<String> lines = new ArrayList<>();
        if (font == null || text.isEmpty()) {
            return lines;
        }
        StringBuilder line = new StringBuilder();
        for (final String word : text.split(" ")) {
            final String tried = line.isEmpty() ? word : line + " " + word;
            if (font.width(tried) <= width || line.isEmpty()) {
                line = new StringBuilder(tried);
            } else {
                lines.add(line.toString());
                line = new StringBuilder(word);
            }
        }
        if (!line.isEmpty()) {
            lines.add(line.toString());
        }
        return lines;
    }

    private static Colours colours() {
        return PALETTE.get();
    }

    /** The tab's content, drawn in one pass: the card and the lists under it. */
    private final class Sheet extends UiComponent {
        @Override
        public void render(final GuiGraphics g, final UiContext ctx) {
            if (data == null) {
                return;
            }
            renderCard(g, ctx.font(), x(), y(), width());
            renderLists(g, ctx.font(), x(), width());
        }
    }

    /** The dialog's engines, one line each, the chosen one marked; a click chooses. */
    private final class Options extends UiComponent {
        @Override
        public void render(final GuiGraphics g, final UiContext ctx) {
            final Font f = ctx.font();
            final List<NetworkServicesPayload.EngineRow> rows = rows();
            for (int i = 0; i < rows.size(); i++) {
                final NetworkServicesPayload.EngineRow row = rows.get(i);
                final int rowY = y() + i * OPTION_H;
                final boolean on = row.program().equals(chosen);
                ctx.skin().listRow(g, x(), rowY, width(), OPTION_H, ctx.over(x(), rowY, width(), OPTION_H), on);
                // The mark has a ground of its own, so it reads the same on a plain line and on the selection.
                g.fill(x() + 3, rowY + 2, x() + 10, rowY + 9, ctx.skin().windowBg());
                Draw.outline(g, x() + 3, rowY + 2, 7, 7, ctx.skin().dim());
                if (on) {
                    g.fill(x() + 5, rowY + 4, x() + 8, rowY + 7, ctx.skin().accent());
                }
                final String name = nameAndVersion(row.name(), row.version());
                Draw.text(g, f, name, x() + 14, rowY + 2, ctx.skin().text());
                final TextKey suffix = switch (row.state()) {
                    case NetworkServicesPayload.ROW_ACTIVE -> NetworkServicesTexts.OPTION_ACTIVE;
                    case NetworkServicesPayload.ROW_STOPPED -> NetworkServicesTexts.OPTION_STOPPED;
                    default -> NetworkServicesTexts.OPTION_INSTALLED;
                };
                // On the chosen line the dim word would be lost on the selection, so it is written in the text colour.
                Draw.text(g, f, GameText.resolve(suffix), x() + 14 + f.width(name) + 5, rowY + 2,
                        on ? ctx.skin().text() : ctx.skin().dim());
            }
            renderChooserNote(g, f);
        }

        @Override
        public boolean mouseClicked(final double mx, final double my, final int button) {
            final int index = (int) ((my - y()) / OPTION_H);
            final List<NetworkServicesPayload.EngineRow> rows = rows();
            if (index >= 0 && index < rows.size()) {
                chosen = rows.get(index).program();
                chooserConfirm.setEnabled(chooserReady());
            }
            return true;
        }
    }

    /** The steps of a replacement under way: those done ticked, the one on now marked, the rest waiting. */
    private final class Steps extends UiComponent {
        @Override
        public void render(final GuiGraphics g, final UiContext ctx) {
            if (!replacementUnderWay()) {
                return;
            }
            final Font f = ctx.font();
            final EngineSwap.Step now = EngineSwap.stepAt(elapsed(), data.replacement().ticks());
            int rowY = y();
            for (final EngineSwap.Step step : EngineSwap.Step.values()) {
                final boolean done = step.before(now);
                final boolean current = step == now;
                final int colour = done ? colours().good() : current ? ctx.skin().accent() : ctx.skin().dim();
                if (done) {
                    // A tick: a short stroke down to the right, a long one up.
                    g.fill(x() + 1, rowY + 4, x() + 2, rowY + 6, colour);
                    g.fill(x() + 2, rowY + 5, x() + 3, rowY + 7, colour);
                    g.fill(x() + 3, rowY + 2, x() + 4, rowY + 6, colour);
                    g.fill(x() + 4, rowY + 1, x() + 5, rowY + 3, colour);
                } else if (current) {
                    g.fill(x() + 1, rowY + 1, x() + 2, rowY + 7, colour);
                    g.fill(x() + 2, rowY + 2, x() + 3, rowY + 6, colour);
                    g.fill(x() + 3, rowY + 3, x() + 4, rowY + 5, colour);
                } else {
                    g.fill(x() + 2, rowY + 3, x() + 4, rowY + 5, colour);
                }
                Draw.text(g, f, Texts.clip(f, stepLabel(step), width() - 10), x() + 9, rowY, colour);
                rowY += STEP_H;
            }
            final long left = (data.replacement().ticks() - elapsed() + TICKS_PER_SECOND - 1) / TICKS_PER_SECOND;
            Draw.text(g, f, Texts.clip(f, GameText.resolve(NetworkServicesTexts.ABOUT_SECONDS.with(left,
                    JsTechTheme.fmt(data.replacement().itemTypes()))), width()), x(), bar.bottom() + 4,
                    ctx.skin().dim());
        }
    }

    /**
     * The tab's colours: an engine's square, a Subframe's, the square when no engine runs, what is fine and what
     * wants the eye, the note's ground and edge, the ground of a Subframe that takes no work, and what dims the window
     * behind a dialog.
     */
    private record Colours(int engine, int subframe, int idle, int good, int warn, int noteGround, int noteEdge,
                           int hotRow, int dialogDim) {
    }
}
