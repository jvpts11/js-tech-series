/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.blockentity.PatternEncoderBlockEntity;
import dev.jstech.computers.machine.DiscCopies;
import dev.jstech.computers.operation.payload.DiscCopyActionPayload;
import dev.jstech.computers.operation.payload.DiscCopyStatePayload;
import dev.jstech.computers.os.media.MediaFormat;
import dev.jstech.core.client.gui.component.AmountStepper;
import dev.jstech.core.client.gui.component.Button;
import dev.jstech.core.client.gui.component.ContextMenu;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.gui.component.Panel;
import dev.jstech.core.client.gui.component.UiContext;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * Roxa's Easy Disc Creator (computers/docs/design/peripherals.md): the Disc Copier, which copies the medium in one of
 * the computer's drives onto a blank one of the same format or a larger one, at the blank's own speed, and a music
 * disc through the Vinyl RW Drive. It chooses the drive to copy from and the one to record to, and how many copies;
 * while it copies it shows each step and how far, and asks for a blank when the copy needs one.
 */
@PaletteHolder
public final class EasyDiscCreatorApp implements IDesktopApp {

    private static final Palette<Brand> BRAND = Palettes.declare(JsComputers.MODID, "app/easy_disc_creator",
            new Brand(0xfffbf8fd, 0xffe8dcef, 0xff6a2c8a, 0xffffffff, 0xff4b2066, 0xff7a6a84));
    private static final int WIDTH = 280;
    private static final int HEIGHT = 196;
    private static final int BANNER_H = 34;
    private static final int MARGIN = 10;
    private static final int LABEL_W = 62;
    private static final int ROW_H = 13;
    private static final int MENU_ROW = 12;
    private static final int BUTTON_W = 54;
    private static final int TICKS_A_SECOND = 20;
    private static final int SECONDS_A_MINUTE = 60;
    private static final long LOOK_EVERY = 500L;
    private static final long BYTES_A_MEGABYTE = 1_048_576L;
    private static final long BYTES_A_KILOBYTE = 1_024L;
    private static final int WHOLE = 1000;
    @Nullable
    private static EasyDiscCreatorApp active;

    private final BlockPos host;
    private final Panel root = new Panel();
    private final ContextMenu menu = new ContextMenu(WIDTH - LABEL_W - 2 * MARGIN, MENU_ROW);
    private final Button from;
    private final Button to;
    private final AmountStepper copies;
    private final Button copy;
    private final Button close;
    private final Button cancel;
    private OsSkin skin = OsSkin.fallback();
    @Nullable
    private DiscCopyStatePayload state;
    private long source;
    private long target;
    private long nextLookAt;
    /* The question or the refusal last put to the player, so each is asked once. */
    private Text asked = Text.EMPTY;
    private int lastX;
    private int lastY;

    public EasyDiscCreatorApp(final BlockPos host) {
        this.host = host;
        this.from = root.add(new Button(() -> placeName(source), () -> choose(true)));
        this.to = root.add(new Button(() -> placeName(target), () -> choose(false)));
        this.copies = root.add(new AmountStepper().setRange(1, DiscCopies.MOST_COPIES).setAmount(1));
        this.copy = root.add(new Button(GameText.resolve(EasyDiscCreatorTexts.COPY), this::start).setPrimary(true));
        this.close = root.add(new Button(GameText.resolve(EasyDiscCreatorTexts.CLOSE),
                () -> ActiveDesktop.closeWindowFor(this)));
        this.cancel = root.add(new Button(GameText.resolve(EasyDiscCreatorTexts.CANCEL),
                () -> send(DiscCopyActionPayload.STOP)));
        active = this;
        send(DiscCopyActionPayload.LOOK);
    }

    /** The computer's word on its drives and its copy, for the open window. */
    public static void accept(final DiscCopyStatePayload payload) {
        final EasyDiscCreatorApp app = active;
        if (app == null || !app.host.equals(payload.hostPos())) {
            return;
        }
        app.state = payload;
        if (!payload.answer().isEmpty()) {
            EasyDiscCreatorPrompt.open(app, payload.answer(), false);
        }
        final Text ask = payload.copy().ask();
        if (!ask.isEmpty() && !GameText.resolve(ask).equals(GameText.resolve(app.asked))) {
            app.asked = ask;
            EasyDiscCreatorPrompt.open(app, ask, true);
        } else if (ask.isEmpty()) {
            app.asked = Text.EMPTY;
        }
    }

    /** OK to what the program asked: the copy goes on. */
    void goOn() {
        send(DiscCopyActionPayload.GO_ON);
    }

    /** Cancel to what the program asked: the copy stops. */
    void stop() {
        send(DiscCopyActionPayload.STOP);
    }

    @Override
    public String title() {
        return GameText.resolve(EasyDiscCreatorTexts.TITLE);
    }

    @Override
    public int defaultWidth() {
        return WIDTH;
    }

    @Override
    public int defaultHeight() {
        return HEIGHT;
    }

    @Override
    public void applySkin(final OsSkin value) {
        this.skin = value;
    }

    @Override
    public void onRestored() {
        active = this;
    }

    @Override
    public void onClosed() {
        if (active == this) {
            active = null;
        }
    }

    @Override
    public boolean wantsEscape() {
        return menu.isOpen();
    }

    @Override
    public void renderContent(final GuiGraphics g, final Font font, final int x, final int y, final int width,
                              final int height, final int mouseX, final int mouseY, final float partialTick) {
        this.lastX = x;
        this.lastY = y;
        if (Util.getMillis() >= nextLookAt) {
            nextLookAt = Util.getMillis() + LOOK_EVERY;
            send(DiscCopyActionPayload.LOOK);
        }
        final Brand brand = BRAND.get();
        g.fill(x, y, x + width, y + height, skin.windowBg());
        g.fill(x, y, x + width, y + BANNER_H, brand.banner());
        g.fill(x, y + BANNER_H - 1, x + width, y + BANNER_H, brand.line());
        logo(g, x + MARGIN, y + 8, brand);
        Draw.text(g, font, GameText.resolve(EasyDiscCreatorTexts.BRAND), x + MARGIN + 22, y + 10, brand.logo());
        Draw.text(g, font, GameText.resolve(EasyDiscCreatorTexts.NAME), x + MARGIN + 60, y + 6, brand.name());
        Draw.text(g, font, GameText.resolve(EasyDiscCreatorTexts.SUBTITLE), x + MARGIN + 60, y + 18, brand.sub());
        final int top = y + BANNER_H + 6;
        Draw.text(g, font, GameText.resolve(EasyDiscCreatorTexts.TAB), x + MARGIN + 4, top, skin.text());
        g.fill(x + MARGIN, top + 10, x + width - MARGIN, top + 11, skin.edge());
        final DiscCopyStatePayload.Copy now = state == null ? DiscCopyStatePayload.Copy.NONE : state.copy();
        final DiscCopies.Phase phase = DiscCopies.Phase.byId(now.phase());
        final boolean copying = phase != null && phase != DiscCopies.Phase.DONE;
        from.setVisible(!copying);
        to.setVisible(!copying);
        copies.setVisible(!copying);
        copy.setVisible(!copying);
        close.setVisible(!copying);
        cancel.setVisible(copying);
        if (copying) {
            renderCopying(g, font, x, top + 16, width, now, phase);
        } else {
            renderChoosing(g, font, x, top + 16, width, now);
        }
        final int by = y + height - ROW_H - 6;
        copy.setBounds(x + width - MARGIN - 2 * BUTTON_W - 4, by, BUTTON_W, ROW_H);
        close.setBounds(x + width - MARGIN - BUTTON_W, by, BUTTON_W, ROW_H);
        cancel.setBounds(x + width - MARGIN - BUTTON_W, by, BUTTON_W, ROW_H);
        copy.setEnabled(source != 0L && target != 0L);
        root.render(g, new UiContext(skin, font, mouseX, mouseY, partialTick));
        if (menu.isOpen()) {
            menu.render(g, new UiContext(skin, font, mouseX, mouseY, partialTick));
        }
    }

    @Override
    public void mouseClicked(final DesktopWindow window, final double mouseX, final double mouseY, final int button) {
        if (menu.isOpen()) {
            menu.mouseClicked(mouseX, mouseY, button);
            return;
        }
        root.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(final int key, final int scanCode, final int modifiers) {
        return menu.isOpen() ? menu.keyPressed(key, scanCode, modifiers) : root.keyPressed(key, scanCode, modifiers);
    }

    /* The two drives to copy between, the blank's speed and the copies. */
    private void renderChoosing(final GuiGraphics g, final Font font, final int x, final int y, final int width,
                                final DiscCopyStatePayload.Copy last) {
        final int field = x + MARGIN + LABEL_W;
        final int w = width - 2 * MARGIN - LABEL_W;
        Draw.text(g, font, GameText.resolve(EasyDiscCreatorTexts.COPY_FROM), x + MARGIN, y + 3, skin.text());
        from.setBounds(field, y, w, ROW_H);
        Draw.text(g, font, GameText.resolve(EasyDiscCreatorTexts.RECORD_TO), x + MARGIN, y + 25, skin.text());
        to.setBounds(field, y + 22, w, ROW_H);
        Draw.text(g, font, GameText.resolve(EasyDiscCreatorTexts.SPEED), x + MARGIN, y + 48, skin.text());
        Draw.text(g, font, speedLine(), field, y + 48, skin.text());
        Draw.text(g, font, GameText.resolve(EasyDiscCreatorTexts.COPIES), x + MARGIN, y + 66, skin.text());
        copies.setBounds(field, y + 64, 100, 11);
        if (source != 0L && source == target) {
            Draw.text(g, font, GameText.resolve(EasyDiscCreatorTexts.ONE_DRIVE), x + MARGIN, y + 84, skin.dim());
        }
        if (!last.note().isEmpty()) {
            Draw.text(g, font, GameText.resolve(last.note()), x + MARGIN, y + 98, skin.dim());
        }
    }

    /* The copy's steps, each done, under way or waiting, and its bar. */
    private void renderCopying(final GuiGraphics g, final Font font, final int x, final int y, final int width,
                               final DiscCopyStatePayload.Copy now, final DiscCopies.Phase phase) {
        Draw.text(g, font, GameText.resolve(EasyDiscCreatorTexts.COPYING.with(now.source(), now.target())),
                x + MARGIN, y, skin.text());
        final List<Step> steps = phase == DiscCopies.Phase.RECORDING || phase == DiscCopies.Phase.CUTTING
                ? List.of(new Step(EasyDiscCreatorTexts.RECORDING.text(), DiscCopies.Phase.RECORDING),
                        new Step(EasyDiscCreatorTexts.CUTTING.text(), DiscCopies.Phase.CUTTING))
                : List.of(new Step(EasyDiscCreatorTexts.READING.text(), DiscCopies.Phase.READING),
                        new Step(EasyDiscCreatorTexts.WRITING.with(now.target()), DiscCopies.Phase.WRITING),
                        new Step(EasyDiscCreatorTexts.CHECKING.text(), DiscCopies.Phase.CHECKING));
        int sy = y + 16;
        for (final Step step : steps) {
            final int order = step.phase().id() - phase.id();
            final String state = order < 0 ? GameText.resolve(EasyDiscCreatorTexts.DONE)
                    : order == 0 ? GameText.resolve(EasyDiscCreatorTexts.PERCENT.with(now.done() / 10))
                    : GameText.resolve(EasyDiscCreatorTexts.WAITING);
            final int ink = order == 0 ? skin.text() : skin.dim();
            Draw.text(g, font, GameText.resolve(step.words()), x + MARGIN + 6, sy, ink);
            Draw.text(g, font, state, x + width - MARGIN - 6 - font.width(state), sy, ink);
            sy += 12;
        }
        final int barY = sy + 4;
        g.fill(x + MARGIN, barY, x + width - MARGIN, barY + 9, skin.edge());
        g.fill(x + MARGIN + 1, barY + 1, x + width - MARGIN - 1, barY + 8, skin.fieldBg());
        g.fill(x + MARGIN + 1, barY + 1, x + MARGIN + 1 + (width - 2 * MARGIN - 2) * now.done() / WHOLE, barY + 8,
                skin.accent());
        final String size = GameText.resolve(EasyDiscCreatorTexts.SIZE_OF.with(megabytes(now.bytes() * now.done()
                / WHOLE), megabytes(now.bytes())));
        Draw.text(g, font, size, x + MARGIN, barY + 13, skin.text());
        final String left = GameText.resolve(EasyDiscCreatorTexts.TIME_LEFT.with(GameText.resolve(time(now.left()))));
        Draw.text(g, font, left, x + width - MARGIN - font.width(left), barY + 13, skin.text());
        Draw.text(g, font, GameText.resolve(EasyDiscCreatorTexts.NOTE_1), x + MARGIN, barY + 30, skin.dim());
        Draw.text(g, font, GameText.resolve(EasyDiscCreatorTexts.NOTE_2), x + MARGIN, barY + 40, skin.dim());
    }

    /* Opens the list of the computer's drives under the button of the one being chosen. */
    private void choose(final boolean ofSource) {
        if (state == null) {
            return;
        }
        final List<ContextMenu.Item> items = new ArrayList<>();
        for (final DiscCopyStatePayload.Place place : state.places()) {
            final String line = GameText.resolve(EasyDiscCreatorTexts.PLACE_HOLDS.with(place.letter(),
                    GameText.resolve(place.drive()), GameText.resolve(place.medium())));
            items.add(new ContextMenu.Item(line, true, () -> {
                if (ofSource) {
                    source = place.pos();
                    if (target == 0L || place.vinyl()) {
                        target = place.pos();
                    }
                } else {
                    target = place.pos();
                }
            }));
        }
        final Button under = ofSource ? from : to;
        menu.open(items, under.x(), under.bottom(), lastX, lastY, WIDTH, HEIGHT);
    }

    private void start() {
        PacketDistributor.sendToServer(new DiscCopyActionPayload(host, DiscCopyActionPayload.START, source, target,
                (int) copies.amount()));
    }

    private void send(final int action) {
        PacketDistributor.sendToServer(new DiscCopyActionPayload(host, action, source, target,
                (int) copies.amount()));
    }

    /* A drive as the buttons name it: its letter, kind and what it holds. */
    private String placeName(final long pos) {
        if (state == null || state.places().isEmpty()) {
            return GameText.resolve(EasyDiscCreatorTexts.NO_DRIVES);
        }
        for (final DiscCopyStatePayload.Place place : state.places()) {
            if (place.pos() == pos) {
                return GameText.resolve(EasyDiscCreatorTexts.PLACE.with(place.letter(),
                        GameText.resolve(place.drive())));
            }
        }
        return "";
    }

    /* The blank's speed: the record-to drive's medium's own, in kilobytes a second. */
    private String speedLine() {
        final MediaFormat format = targetFormat();
        if (format == null) {
            return GameText.resolve(EasyDiscCreatorTexts.OWN_SPEED);
        }
        final int perTick = PatternEncoderBlockEntity.bytesPerTick(format);
        return GameText.resolve(perTick == Integer.MAX_VALUE ? EasyDiscCreatorTexts.SPEED_AT_ONCE.text()
                : EasyDiscCreatorTexts.SPEED_KB.with(String.format(Locale.ROOT, "%,d",
                        (long) perTick * TICKS_A_SECOND / BYTES_A_KILOBYTE)));
    }

    @Nullable
    private MediaFormat targetFormat() {
        if (state == null) {
            return null;
        }
        for (final DiscCopyStatePayload.Place place : state.places()) {
            if (place.pos() == target && place.blank()) {
                for (final MediaFormat format : MediaFormat.values()) {
                    if (GameText.resolve(place.medium()).contains(GameText.resolve(format.text()))) {
                        return format;
                    }
                }
            }
        }
        return null;
    }

    private static Text time(final int ticks) {
        final int seconds = Math.max(1, ticks / TICKS_A_SECOND);
        return seconds < SECONDS_A_MINUTE ? EasyDiscCreatorTexts.SECONDS.with(seconds)
                : EasyDiscCreatorTexts.MINUTES.with(Math.max(1, Math.round(seconds / (float) SECONDS_A_MINUTE)));
    }

    private static String megabytes(final long bytes) {
        return String.format(Locale.ROOT, "%,d MB", Math.max(0L, bytes / BYTES_A_MEGABYTE));
    }

    /* Roxa's mark: a purple ring with a white hole, as a disc. */
    private static void logo(final GuiGraphics g, final int x, final int y, final Brand brand) {
        g.fill(x + 2, y, x + 14, y + 16, brand.logo());
        g.fill(x, y + 2, x + 16, y + 14, brand.logo());
        g.fill(x + 6, y + 6, x + 10, y + 10, brand.logoInk());
    }

    /* A step of the copy: what it says and the phase it is. */
    private record Step(Text words, DiscCopies.Phase phase) {
    }

    /**
     * Roxa's colours.
     *
     * @param banner  the band at the top
     * @param line    the line under it
     * @param logo    the mark and the house's name
     * @param logoInk the hole of the mark
     * @param name    the program's name
     * @param sub     its subtitle
     */
    private record Brand(int banner, int line, int logo, int logoInk, int name, int sub) {
    }
}
