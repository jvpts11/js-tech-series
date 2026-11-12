/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.MediaDevices;
import dev.jstech.computers.blockentity.PatternEncoderBlockEntity;
import dev.jstech.computers.machine.DiscCopies;
import dev.jstech.computers.operation.payload.DiscCopyActionPayload;
import dev.jstech.computers.operation.payload.DiscCopyStatePayload;
import dev.jstech.computers.os.media.FormattedMediaItem;
import dev.jstech.computers.os.media.MediaFormat;
import dev.jstech.computers.os.media.MediaSizes;
import dev.jstech.core.client.gui.component.Button;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.gui.component.Dropdown;
import dev.jstech.core.client.gui.component.Panel;
import dev.jstech.core.client.gui.component.TabStrip;
import dev.jstech.core.client.gui.component.Texts;
import dev.jstech.core.client.gui.component.UiContext;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * Roxa's Easy Disc Creator (computers/docs/design/peripherals.md): the Disc Copier, which copies the medium in one of
 * the computer's drives onto a blank one of the same format or a larger one, at the blank's own speed, and a music
 * disc through the Vinyl RW Drive. Under Roxa's band and its Copy tab it chooses the drive to copy from and the one to
 * record to, each list saying what every drive holds and the line under it what the chosen one holds, with the
 * blank's speed, the time it will take and how many copies; while it copies it shows each step and how far, and asks
 * for a blank when the copy needs one.
 */
@PaletteHolder
public final class EasyDiscCreatorApp implements IDesktopApp {

    private final BlockPos host;
    private final Panel root = new Panel();
    private final TabStrip tabs;
    private final Dropdown copiesList;
    private final Button copy;
    private final Button close;
    private final Button cancel;
    private OsSkin skin = OsSkin.fallback();
    @Nullable
    private DiscCopyStatePayload state;
    private long source;
    private long target;
    private int copies = 1;
    /* The drive list open under its field: none, the one copied from, or the one recorded to. */
    private int listOpen = NO_LIST;
    private long nextLookAt;
    /* The question or the refusal last put to the player, so each is asked once. */
    private Text asked = Text.EMPTY;
    /* The song of the music disc a vinyl copy started from, for its line once the disc is taken out. */
    private String copiedSong = "";
    private int lastX;
    private int lastY;

    private static final Palette<Brand> BRAND = Palettes.declare(JsComputers.MODID, "app/easy_disc_creator",
            new Brand(0xffffffff, 0xffe2d0ea, 0xff7b2d8e, 0xff7b2d8e, 0xffe7d2ee, 0xff7b2d8e, 0xff5a3a66,
                    0xff2e7d32));
    /* The icon the desktop gives its disks, beside each drive's field. */
    private static final ResourceLocation DRIVES_ICON = ResourceLocation.fromNamespaceAndPath(JsComputers.MODID,
            "disks");
    private static final int WIDTH = 381;
    private static final int HEIGHT = 263;
    /* Roxa's band: its height, the mark, the house's word at a size and a half, the program's name and subtitle. */
    private static final int BANNER_H = 37;
    private static final int MARK_X = 9;
    private static final int MARK_Y = 10;
    private static final int MARK = 16;
    /* The mark's rows, each half as wide as the disc is there, and the pale ring's. */
    private static final int[] MARK_HALVES = {3, 5, 6, 7, 7, 8, 8, 8, 8, 8, 8, 7, 7, 6, 5, 3};
    private static final int[] RING_HALVES = {2, 3, 3, 3, 3, 2};
    private static final int BRAND_X = 28;
    private static final int BRAND_Y = 12;
    private static final float BRAND_SCALE = 1.5F;
    private static final int NAME_X = 75;
    private static final int NAME_Y = 8;
    private static final int SUB_Y = 23;
    /* The tab over the page, and the page's box. */
    private static final int TAB_Y = 46;
    private static final int TAB_H = 16;
    private static final int TAB_PAD = 22;
    private static final int PAGE_X = 8;
    private static final int PAGE_TOP = 62;
    private static final int PAGE_BOTTOM = 35;
    /* The Copy page: the labels, the drives' icons, the fields and the lines under them. */
    private static final int LABEL_X = 19;
    private static final int ICON_X = 86;
    private static final int ICON = 11;
    private static final int FIELD_X = 99;
    private static final int FIELD_H = 15;
    private static final int FROM_Y = 74;
    private static final int TO_Y = 113;
    private static final int INFO_DY = 19;
    private static final int SPEED_Y = 154;
    private static final int COPIES_Y = 167;
    private static final int COPIES_W = 40;
    private static final int NOTE_Y = 190;
    private static final int ARROW_W = 12;
    /* An open drive list: a row for each drive, its letter and kind, and what it holds in the second column. */
    private static final int LIST_ROW = 13;
    private static final int DETAIL_X = 100;
    /* While it copies: the line saying what, the steps, the bar, the bytes and the time, and the note. */
    private static final int COPYING_Y = 74;
    private static final int STEPS_Y = 94;
    private static final int STEP_PITCH = 15;
    private static final int STEP_INSET = 7;
    private static final int BAR_Y = 149;
    private static final int BAR_H = 11;
    private static final int SIZE_Y = 165;
    private static final int COPY_NOTE_Y = 186;
    private static final int NOTE_PITCH = 11;
    /* The keys under the page, right-aligned. */
    private static final int BUTTON_W = 53;
    private static final int BUTTON_H = 15;
    private static final int BUTTON_GAP = 7;
    private static final int BUTTONS_DY = 27;
    private static final int TICKS_A_SECOND = 20;
    private static final int SECONDS_A_MINUTE = 60;
    private static final long LOOK_EVERY = 500L;
    private static final long BYTES_A_KILOBYTE = 1_024L;
    private static final int WHOLE = 1000;
    private static final int NO_LIST = 0;
    private static final int FROM_LIST = 1;
    private static final int TO_LIST = 2;
    @Nullable
    private static EasyDiscCreatorApp active;

    public EasyDiscCreatorApp(final BlockPos host) {
        this.host = host;
        this.tabs = root.add(new TabStrip(List.of(GameText.resolve(EasyDiscCreatorTexts.TAB))).fitToLabels(TAB_PAD));
        final List<Supplier<String>> amounts = new ArrayList<>();
        for (int amount = 1; amount <= DiscCopies.MOST_COPIES; amount++) {
            final String words = Integer.toString(amount);
            amounts.add(() -> words);
        }
        this.copiesList = root.add(new Dropdown(amounts, () -> copies - 1, index -> copies = index + 1,
                shown -> shown));
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
        if (app.source == 0L && app.target == 0L) {
            app.chooseFirst(payload.places());
        }
        if (!payload.answer().isEmpty()) {
            EasyDiscCreatorPrompt.open(app, payload.answer(), false, ItemStack.EMPTY);
        }
        final Text ask = payload.copy().ask();
        if (!ask.isEmpty() && !GameText.resolve(ask).equals(GameText.resolve(app.asked))) {
            app.asked = ask;
            EasyDiscCreatorPrompt.open(app, ask, true, app.askedFor(payload.copy()));
        } else if (ask.isEmpty()) {
            app.asked = Text.EMPTY;
        }
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
    public int minWidth() {
        return WIDTH;
    }

    @Override
    public int minHeight() {
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
        return listOpen != NO_LIST || copiesList.isOpen();
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
        g.fill(x, y, x + width, y + height, skin.windowBg());
        drawBanner(g, font, x, y, width);
        tabs.setBounds(x + PAGE_X, y + TAB_Y, width - 2 * PAGE_X, TAB_H);
        final int pageBottom = y + height - PAGE_BOTTOM;
        g.fill(x + PAGE_X, y + PAGE_TOP, x + width - PAGE_X, pageBottom, skin.fieldBg());
        Draw.outline(g, x + PAGE_X, y + PAGE_TOP, width - 2 * PAGE_X, pageBottom - y - PAGE_TOP, skin.edge());
        final DiscCopyStatePayload.Copy now = state == null ? DiscCopyStatePayload.Copy.NONE : state.copy();
        final DiscCopies.Phase phase = DiscCopies.Phase.byId(now.phase());
        final boolean copying = phase != null && phase != DiscCopies.Phase.DONE;
        copiesList.setVisible(!copying);
        copy.setVisible(!copying);
        close.setVisible(!copying);
        cancel.setVisible(copying);
        if (copying) {
            listOpen = NO_LIST;
            renderCopying(g, font, x, y, width, now, phase);
        } else {
            renderChoosing(g, font, x, y, width, now, mouseX, mouseY);
        }
        final int by = y + height - BUTTONS_DY;
        final int right = x + width - PAGE_X;
        close.setBounds(right - BUTTON_W, by, BUTTON_W, BUTTON_H);
        cancel.setBounds(right - BUTTON_W, by, BUTTON_W, BUTTON_H);
        copy.setBounds(right - 2 * BUTTON_W - BUTTON_GAP, by, BUTTON_W, BUTTON_H);
        copy.setEnabled(source != 0L && target != 0L);
        root.render(g, new UiContext(skin, font, mouseX, mouseY, partialTick));
        if (listOpen != NO_LIST) {
            drawList(g, font, x, y, width, mouseX, mouseY);
        }
    }

    @Override
    public void mouseClicked(final DesktopWindow window, final double mouseX, final double mouseY, final int button) {
        if (listOpen != NO_LIST) {
            final int row = listRowAt(mouseX, mouseY);
            if (row >= 0 && state != null && row < state.places().size()) {
                choose(state.places().get(row), listOpen == FROM_LIST);
            }
            listOpen = NO_LIST;
            return;
        }
        if (button == 0 && !copying() && state != null && !state.places().isEmpty()) {
            if (onField(mouseX, mouseY, FROM_Y)) {
                listOpen = FROM_LIST;
                return;
            }
            if (onField(mouseX, mouseY, TO_Y)) {
                listOpen = TO_LIST;
                return;
            }
        }
        root.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(final int key, final int scanCode, final int modifiers) {
        if (listOpen != NO_LIST) {
            listOpen = NO_LIST;
            return true;
        }
        return root.keyPressed(key, scanCode, modifiers);
    }

    /** The middle of the field of the drive copied from, for a test. */
    public int[] fromPoint() {
        return new int[] {lastX + FIELD_X + fieldWidth() / 2, lastY + FROM_Y + FIELD_H / 2};
    }

    /** The middle of the field of the drive recorded to, for a test. */
    public int[] toPoint() {
        return new int[] {lastX + FIELD_X + fieldWidth() / 2, lastY + TO_Y + FIELD_H / 2};
    }

    /** The middle of the Copy key, for a test. */
    public int[] copyPoint() {
        return new int[] {copy.x() + copy.width() / 2, copy.y() + copy.height() / 2};
    }

    /** Whether a drive list is open, for a test. */
    public boolean listShown() {
        return listOpen != NO_LIST;
    }

    /** The middle of the open list's row of the drive lettered {@code letter}, or null, for a test. */
    @Nullable
    public int[] listPoint(final String letter) {
        if (listOpen == NO_LIST || state == null) {
            return null;
        }
        final int top = lastY + (listOpen == FROM_LIST ? FROM_Y : TO_Y) + FIELD_H;
        for (int row = 0; row < state.places().size(); row++) {
            if (state.places().get(row).letter().equals(letter)) {
                return new int[] {lastX + FIELD_X + fieldWidth() / 2, top + 2 + row * LIST_ROW + LIST_ROW / 2};
            }
        }
        return null;
    }

    /** The middle of the OK key of the program's message box up now, or null, for a test. */
    @Nullable
    public static int[] promptOkPoint() {
        return EasyDiscCreatorPrompt.okPointShowing();
    }

    /** The step the copy is on, by its order, or -1 with none under way, for a test. */
    public int phase() {
        return state == null ? -1 : state.copy().phase();
    }

    /** OK to what the program asked: the copy goes on. */
    void goOn() {
        send(DiscCopyActionPayload.GO_ON);
    }

    /** Cancel to what the program asked: the copy stops. */
    void stop() {
        send(DiscCopyActionPayload.STOP);
    }

    /* Roxa's band: the light washing down to its lilac, the line under it, the mark, the program's name. */
    private void drawBanner(final GuiGraphics g, final Font font, final int x, final int y, final int width) {
        final Brand brand = BRAND.get();
        for (int row = 0; row < BANNER_H; row++) {
            g.fill(x, y + row, x + width, y + row + 1, ChromeShapes.blend(brand.bannerTop(), brand.banner(),
                    row / (float) (BANNER_H - 1)));
        }
        g.fill(x, y + BANNER_H, x + width, y + BANNER_H + 1, brand.line());
        logo(g, x + MARK_X, y + MARK_Y, brand);
        Texts.scaled(g, font, GameText.resolve(EasyDiscCreatorTexts.BRAND), x + BRAND_X, y + BRAND_Y, BRAND_SCALE,
                brand.logo());
        TabbedWindowLook.heading(g, font, GameText.resolve(EasyDiscCreatorTexts.NAME), x + NAME_X, y + NAME_Y,
                brand.name());
        Draw.text(g, font, GameText.resolve(EasyDiscCreatorTexts.SUBTITLE), x + NAME_X, y + SUB_Y, brand.sub());
    }

    /* The two drives to copy between, each with what it holds under it, the blank's speed and the copies. */
    private void renderChoosing(final GuiGraphics g, final Font font, final int x, final int y, final int width,
                                final DiscCopyStatePayload.Copy last, final int mouseX, final int mouseY) {
        final DiscCopyStatePayload.Place from = place(source);
        final DiscCopyStatePayload.Place to = place(target);
        driveRow(g, font, x, y + FROM_Y, EasyDiscCreatorTexts.COPY_FROM.text(), from,
                from == null ? "" : info(from), listOpen == FROM_LIST, mouseX, mouseY);
        driveRow(g, font, x, y + TO_Y, EasyDiscCreatorTexts.RECORD_TO.text(), to,
                to == null ? "" : targetInfo(from, to), listOpen == TO_LIST, mouseX, mouseY);
        Draw.text(g, font, GameText.resolve(EasyDiscCreatorTexts.SPEED), x + LABEL_X, y + SPEED_Y, skin.text());
        Draw.text(g, font, Texts.clip(font, speedLine(from, to), width - FIELD_X - LABEL_X), x + FIELD_X,
                y + SPEED_Y, skin.text());
        Draw.text(g, font, GameText.resolve(EasyDiscCreatorTexts.COPIES), x + LABEL_X, y + COPIES_Y + 4,
                skin.text());
        copiesList.setBounds(x + FIELD_X, y + COPIES_Y, COPIES_W, FIELD_H);
        if (!last.note().isEmpty()) {
            Draw.text(g, font, GameText.resolve(last.note()), x + LABEL_X, y + NOTE_Y, skin.dim());
        }
    }

    /* A label, the drives' icon, the field naming the chosen drive with its arrow, and what it holds under it. */
    private void driveRow(final GuiGraphics g, final Font font, final int x, final int y, final Text label,
                          @Nullable final DiscCopyStatePayload.Place chosen, final String info, final boolean open,
                          final int mouseX, final int mouseY) {
        Draw.text(g, font, GameText.resolve(label), x + LABEL_X, y + 4, skin.text());
        ProgramIcons.draw(g, x + ICON_X, y + 2, ICON, ICON, DRIVES_ICON, skin.iconSet());
        final int fx = x + FIELD_X;
        final int fw = fieldWidth();
        skin.field(g, fx, y, fw, FIELD_H, open);
        final boolean over = mouseX >= fx + fw - ARROW_W && mouseX < fx + fw && mouseY >= y && mouseY < y + FIELD_H;
        skin.button(g, font, fx + fw - ARROW_W - 1, y + 1, ARROW_W, FIELD_H - 2, "", over || open, false, false);
        final int ax = fx + fw - ARROW_W / 2 - 1;
        for (int i = 0; i < 3; i++) {
            g.fill(ax - 2 + i, y + 6 + i, ax + 3 - i, y + 7 + i, skin.text());
        }
        final boolean none = state == null || state.places().isEmpty();
        final String shown = none ? GameText.resolve(EasyDiscCreatorTexts.NO_DRIVES)
                : chosen == null ? "" : placeName(chosen);
        Draw.text(g, font, Texts.clip(font, shown, fw - ARROW_W - 8), fx + 4, y + 4, skin.text());
        Draw.text(g, font, Texts.clip(font, info, fw), fx, y + INFO_DY, skin.dim());
    }

    /* The open list under a field: each drive's letter and kind, and what it holds. */
    private void drawList(final GuiGraphics g, final Font font, final int x, final int y, final int width,
                          final int mouseX, final int mouseY) {
        if (state == null) {
            return;
        }
        final List<DiscCopyStatePayload.Place> places = state.places();
        final int lx = x + FIELD_X;
        final int top = y + (listOpen == FROM_LIST ? FROM_Y : TO_Y) + FIELD_H;
        final int lw = fieldWidth();
        final int lh = places.size() * LIST_ROW + 4;
        g.fill(lx, top, lx + lw, top + lh, skin.fieldBg());
        Draw.outline(g, lx, top, lw, lh, skin.text());
        final int hovered = listRowAt(mouseX, mouseY);
        final long chosen = listOpen == FROM_LIST ? source : target;
        for (int row = 0; row < places.size(); row++) {
            final DiscCopyStatePayload.Place place = places.get(row);
            final int ry = top + 2 + row * LIST_ROW;
            final boolean lit = hovered >= 0 ? hovered == row : place.pos() == chosen;
            if (lit) {
                g.fill(lx + 1, ry, lx + lw - 1, ry + LIST_ROW, skin.listSelect());
            }
            Draw.text(g, font, Texts.clip(font, placeName(place), DETAIL_X - 8), lx + 4, ry + 3,
                    skin.listRowText(lit));
            Draw.text(g, font, Texts.clip(font, detail(place), lw - DETAIL_X - 4), lx + DETAIL_X, ry + 3,
                    lit ? skin.listRowText(true) : skin.dim());
        }
    }

    /* The copy's line, its steps each done, under way or waiting, its bar, its bytes and time, and its note. */
    private void renderCopying(final GuiGraphics g, final Font font, final int x, final int y, final int width,
                               final DiscCopyStatePayload.Copy now, final DiscCopies.Phase phase) {
        final Brand brand = BRAND.get();
        final DiscCopyStatePayload.Place from = byLetter(now.source());
        // A vinyl copy waits for its blank between recording and cutting, so its drive says what kind it is.
        final boolean vinyl = phase == DiscCopies.Phase.RECORDING || phase == DiscCopies.Phase.CUTTING
                || from != null && from.vinyl();
        final int px = x + LABEL_X;
        final int pw = width - 2 * LABEL_X;
        Draw.text(g, font, Texts.clip(font, copyingLine(now, vinyl), pw), px, y + COPYING_Y, skin.text());
        final DiscCopyStatePayload.Place to = place(target);
        final List<Step> steps = vinyl
                ? List.of(new Step(EasyDiscCreatorTexts.RECORDING.text(), DiscCopies.Phase.RECORDING),
                        new Step(EasyDiscCreatorTexts.CUTTING.text(), DiscCopies.Phase.CUTTING))
                : List.of(new Step(EasyDiscCreatorTexts.READING.text(), DiscCopies.Phase.READING),
                        new Step(EasyDiscCreatorTexts.WRITING.with(now.target(), rate(formatOf(to))),
                                DiscCopies.Phase.WRITING),
                        new Step(EasyDiscCreatorTexts.CHECKING.text(), DiscCopies.Phase.CHECKING));
        int sy = y + STEPS_Y;
        for (final Step step : steps) {
            final int order = vinyl ? vinylOrder(step.phase(), phase) : Integer.signum(step.phase().id() - phase.id());
            final String how = order < 0 ? GameText.resolve(EasyDiscCreatorTexts.DONE)
                    : order == 0 ? GameText.resolve(EasyDiscCreatorTexts.PERCENT.with(now.done() / 10))
                    : GameText.resolve(EasyDiscCreatorTexts.WAITING);
            final int ink = order < 0 ? brand.done() : order == 0 ? skin.text() : skin.dim();
            Draw.text(g, font, GameText.resolve(step.words()), px + STEP_INSET, sy, ink);
            Draw.text(g, font, how, px + pw - STEP_INSET - font.width(how), sy, ink);
            sy += STEP_PITCH;
        }
        final boolean asking = phase == DiscCopies.Phase.ASKING;
        skin.progress(g, px, y + BAR_Y, pw, BAR_H, asking ? 0.0 : now.done() / (double) WHOLE);
        if (!vinyl) {
            Draw.text(g, font, GameText.resolve(EasyDiscCreatorTexts.SIZE_OF.with(
                    MediaSizes.label(now.bytes() * now.done() / WHOLE), MediaSizes.label(now.bytes()))), px,
                    y + SIZE_Y, skin.text());
        }
        // While it waits on the player there is no time left to tell.
        if (!asking) {
            final String left = GameText.resolve(EasyDiscCreatorTexts.TIME_LEFT.with(
                    GameText.resolve(time(now.left()))));
            Draw.text(g, font, left, px + pw - font.width(left), y + SIZE_Y, skin.text());
        }
        TabbedWindowLook.paragraph(g, font, GameText.resolve(EasyDiscCreatorTexts.NOTE), px, y + COPY_NOTE_Y, pw,
                NOTE_PITCH, skin.dim());
    }

    /*
     * Where a step of a vinyl copy stands while the copy is at {@code phase}: done, under way or waiting. Its question
     * comes between the recording and the cut, though its step comes first among a disc's.
     */
    private static int vinylOrder(final DiscCopies.Phase step, final DiscCopies.Phase phase) {
        if (step == phase) {
            return 0;
        }
        if (step == DiscCopies.Phase.RECORDING) {
            return -1;
        }
        return phase == DiscCopies.Phase.DONE ? -1 : 1;
    }

    /* What the copy under way copies, and onto what, as its first line says it. */
    private String copyingLine(final DiscCopyStatePayload.Copy now, final boolean vinyl) {
        final DiscCopyStatePayload.Place from = byLetter(now.source());
        if (vinyl) {
            // The music disc comes out for the blank, so the song is the one the copy started with.
            final String song = from != null && !from.song().isEmpty() ? song(from) : copiedSong;
            return GameText.resolve(EasyDiscCreatorTexts.COPYING_VINYL.with(song.isEmpty()
                    ? GameText.resolve(EasyDiscCreatorTexts.THE_DISC) : song, now.source()));
        }
        final DiscCopyStatePayload.Place to = byLetter(now.target());
        final String what = from == null ? "" : from.name().isEmpty() ? kind(from) : from.name();
        return GameText.resolve(EasyDiscCreatorTexts.COPYING.with(what, now.source(), to == null ? "" : kind(to),
                now.target()));
    }

    /*
     * What the window opens on: the first drive holding a disc that copies as the one copied from, and the first other
     * drive holding a blank as the one recorded to.
     */
    private void chooseFirst(final List<DiscCopyStatePayload.Place> places) {
        for (final DiscCopyStatePayload.Place place : places) {
            if (source == 0L && !place.vinyl() && !place.blank() && !place.storage() && item(place) != Items.AIR) {
                source = place.pos();
            }
        }
        for (final DiscCopyStatePayload.Place place : places) {
            if (target == 0L && source != 0L && place.pos() != source && place.blank()) {
                target = place.pos();
            }
        }
    }

    /* Chooses {@code chosen} as the drive copied from or recorded to; a vinyl drive is both. */
    private void choose(final DiscCopyStatePayload.Place chosen, final boolean ofSource) {
        if (ofSource) {
            source = chosen.pos();
            if (target == 0L || chosen.vinyl()) {
                target = chosen.pos();
            }
        } else {
            target = chosen.pos();
        }
    }

    private void start() {
        final DiscCopyStatePayload.Place from = place(source);
        copiedSong = from != null && from.vinyl() && !from.song().isEmpty() ? song(from) : "";
        PacketDistributor.sendToServer(new DiscCopyActionPayload(host, DiscCopyActionPayload.START, source, target,
                copies));
    }

    private void send(final int action) {
        PacketDistributor.sendToServer(new DiscCopyActionPayload(host, action, source, target, copies));
    }

    private boolean copying() {
        final DiscCopies.Phase phase = state == null ? null : DiscCopies.Phase.byId(state.copy().phase());
        return phase != null && phase != DiscCopies.Phase.DONE;
    }

    private int fieldWidth() {
        return WIDTH - FIELD_X - LABEL_X;
    }

    private boolean onField(final double mouseX, final double mouseY, final int fieldY) {
        return mouseX >= lastX + FIELD_X && mouseX < lastX + FIELD_X + fieldWidth() && mouseY >= lastY + fieldY
                && mouseY < lastY + fieldY + FIELD_H;
    }

    /* The row of the open list under the pointer, or -1. */
    private int listRowAt(final double mouseX, final double mouseY) {
        if (listOpen == NO_LIST || state == null) {
            return -1;
        }
        final int top = lastY + (listOpen == FROM_LIST ? FROM_Y : TO_Y) + FIELD_H + 2;
        if (mouseX < lastX + FIELD_X || mouseX >= lastX + FIELD_X + fieldWidth() || mouseY < top) {
            return -1;
        }
        final int row = (int) ((mouseY - top) / LIST_ROW);
        return row < state.places().size() ? row : -1;
    }

    @Nullable
    private DiscCopyStatePayload.Place place(final long pos) {
        if (state != null && pos != 0L) {
            for (final DiscCopyStatePayload.Place place : state.places()) {
                if (place.pos() == pos) {
                    return place;
                }
            }
        }
        return null;
    }

    @Nullable
    private DiscCopyStatePayload.Place byLetter(final String letter) {
        if (state != null) {
            for (final DiscCopyStatePayload.Place place : state.places()) {
                if (place.letter().equals(letter)) {
                    return place;
                }
            }
        }
        return null;
    }

    /* What a question asks for, as the box's picture: a blank vinyl, or a blank of the disc being copied. */
    private ItemStack askedFor(final DiscCopyStatePayload.Copy copy) {
        final DiscCopyStatePayload.Place from = byLetter(copy.source());
        if (from == null) {
            return ItemStack.EMPTY;
        }
        return from.vinyl() ? new ItemStack(MediaDevices.BLANK_VINYL_RECORD.get()) : new ItemStack(item(from));
    }

    /* A drive as its field and its list name it: its letter and kind. */
    private static String placeName(final DiscCopyStatePayload.Place place) {
        return GameText.resolve(EasyDiscCreatorTexts.PLACE.with(place.letter(), GameText.resolve(place.drive())));
    }

    /* What a drive holds, as the second column of a list says it. */
    private static String detail(final DiscCopyStatePayload.Place place) {
        if (item(place) == Items.AIR) {
            return GameText.resolve(EasyDiscCreatorTexts.EMPTY);
        }
        if (place.vinyl()) {
            return place.song().isEmpty() ? GameText.resolve(EasyDiscCreatorTexts.BLANK_VINYL) : song(place);
        }
        if (place.storage()) {
            return GameText.resolve(EasyDiscCreatorTexts.STORAGE);
        }
        if (place.blank()) {
            return GameText.resolve(EasyDiscCreatorTexts.BLANK.with(kind(place)));
        }
        return place.name().isEmpty() ? kind(place)
                : GameText.resolve(EasyDiscCreatorTexts.NAMED.with(place.name(), kind(place)));
    }

    /* What a drive holds, as the line under its field says it. */
    private static String info(final DiscCopyStatePayload.Place place) {
        final Item item = item(place);
        if (item == Items.AIR) {
            return GameText.resolve(EasyDiscCreatorTexts.INFO_EMPTY);
        }
        if (place.vinyl()) {
            return place.song().isEmpty() ? GameText.resolve(EasyDiscCreatorTexts.INFO_BLANK_VINYL)
                    : GameText.resolve(EasyDiscCreatorTexts.INFO_SONG.with(song(place), clock(place.seconds())));
        }
        if (place.storage()) {
            return GameText.resolve(EasyDiscCreatorTexts.INFO_STORAGE);
        }
        final String box = item instanceof FormattedMediaItem medium ? medium.soldAs() : "";
        if (place.blank()) {
            return GameText.resolve(EasyDiscCreatorTexts.INFO_BLANK.with(kind(place), box));
        }
        final String used = MediaSizes.label(place.bytes());
        return place.name().isEmpty() ? GameText.resolve(EasyDiscCreatorTexts.INFO_UNNAMED.with(kind(place), used,
                box)) : GameText.resolve(EasyDiscCreatorTexts.INFO_NAMED.with(place.name(), kind(place), used, box));
    }

    /* The line under the field recorded to: what the drive holds, and whether the copy has room on it. */
    private static String targetInfo(@Nullable final DiscCopyStatePayload.Place from,
                                     final DiscCopyStatePayload.Place to) {
        if (from != null && from.pos() == to.pos()) {
            return GameText.resolve(to.vinyl() ? EasyDiscCreatorTexts.SAME_VINYL : EasyDiscCreatorTexts.ONE_DRIVE);
        }
        if (from == null || !to.blank() || !(item(to) instanceof FormattedMediaItem blank)) {
            return info(to);
        }
        final long needs = item(from) instanceof FormattedMediaItem medium ? medium.capacityBytes() : 0L;
        return GameText.resolve((blank.capacityBytes() >= needs ? EasyDiscCreatorTexts.INFO_ROOM
                : EasyDiscCreatorTexts.INFO_TOO_SMALL).with(kind(to), blank.soldAs()));
    }

    /* The blank's speed and how long the copy will take; a vinyl's in its song's own time. */
    private String speedLine(@Nullable final DiscCopyStatePayload.Place from,
                             @Nullable final DiscCopyStatePayload.Place to) {
        if (from != null && from.vinyl()) {
            final String length = clock(from.seconds());
            return GameText.resolve(EasyDiscCreatorTexts.SPEED_SONG.with(length, length));
        }
        final MediaFormat blank = to != null && to.blank() ? formatOf(to) : null;
        if (blank == null) {
            return GameText.resolve(EasyDiscCreatorTexts.OWN_SPEED);
        }
        if (PatternEncoderBlockEntity.bytesPerTick(blank) == Integer.MAX_VALUE) {
            return GameText.resolve(EasyDiscCreatorTexts.SPEED_AT_ONCE);
        }
        final MediaFormat read = from == null ? null : formatOf(from);
        if (read == null) {
            return GameText.resolve(EasyDiscCreatorTexts.SPEED_KB.with(rate(blank)));
        }
        final long ticks = DiscCopies.ticksFor(read, from.bytes())
                + (long) (DiscCopies.ticksFor(blank, from.bytes()) + DiscCopies.CHECK_TICKS) * copies;
        return GameText.resolve(EasyDiscCreatorTexts.SPEED_ABOUT.with(rate(blank),
                GameText.resolve(time((int) Math.min(Integer.MAX_VALUE, ticks)))));
    }

    /* A format's writing pace, in kilobytes a second. */
    private static String rate(@Nullable final MediaFormat format) {
        if (format == null) {
            return "";
        }
        final int perTick = PatternEncoderBlockEntity.bytesPerTick(format);
        return perTick == Integer.MAX_VALUE ? GameText.resolve(EasyDiscCreatorTexts.AT_ONCE)
                : GameText.resolve(EasyDiscCreatorTexts.RATE.with(String.format(Locale.ROOT, "%,d",
                (long) perTick * TICKS_A_SECOND / BYTES_A_KILOBYTE)));
    }

    @Nullable
    private static MediaFormat formatOf(@Nullable final DiscCopyStatePayload.Place place) {
        return place != null && item(place) instanceof FormattedMediaItem medium ? medium.format() : null;
    }

    private static Item item(final DiscCopyStatePayload.Place place) {
        return BuiltInRegistries.ITEM.get(place.item());
    }

    /* The kind of medium in a drive, by its item's name in the player's language: a CD-RW, a floppy disk. */
    private static String kind(final DiscCopyStatePayload.Place place) {
        return item(place).getDescription().getString();
    }

    /* The song of a music disc in a drive, as its jukebox names it ("C418 - cat"), in the player's language. */
    private static String song(final DiscCopyStatePayload.Place place) {
        final ResourceLocation id = ResourceLocation.tryParse(place.song());
        final ClientLevel level = Minecraft.getInstance().level;
        if (id == null || level == null) {
            return place.song();
        }
        return level.registryAccess().registry(Registries.JUKEBOX_SONG).flatMap(songs -> songs.getOptional(id))
                .map(song -> song.description().getString()).orElse(place.song());
    }

    /* A length in minutes and seconds, as a player shows it: 3:05. */
    private static String clock(final int seconds) {
        return String.format(Locale.ROOT, "%d:%02d", seconds / SECONDS_A_MINUTE, seconds % SECONDS_A_MINUTE);
    }

    private static Text time(final int ticks) {
        final int seconds = Math.max(1, ticks / TICKS_A_SECOND);
        if (seconds < SECONDS_A_MINUTE) {
            return seconds == 1 ? EasyDiscCreatorTexts.SECOND.text() : EasyDiscCreatorTexts.SECONDS.with(seconds);
        }
        final int minutes = Math.max(1, Math.round(seconds / (float) SECONDS_A_MINUTE));
        return minutes == 1 ? EasyDiscCreatorTexts.MINUTE.text() : EasyDiscCreatorTexts.MINUTES.with(minutes);
    }

    /* Roxa's mark: a purple disc, a pale ring round its middle, and the band's own light through its hole. */
    private static void logo(final GuiGraphics g, final int x, final int y, final Brand brand) {
        for (int row = 0; row < MARK; row++) {
            final int half = MARK_HALVES[row];
            g.fill(x + MARK / 2 - half, y + row, x + MARK / 2 + half, y + row + 1, brand.logo());
        }
        for (int row = 0; row < RING_HALVES.length; row++) {
            final int half = RING_HALVES[row];
            final int ry = y + (MARK - RING_HALVES.length) / 2 + row;
            g.fill(x + MARK / 2 - half, ry, x + MARK / 2 + half, ry + 1, brand.logoInk());
        }
        g.fill(x + MARK / 2 - 1, y + MARK / 2 - 1, x + MARK / 2 + 1, y + MARK / 2 + 1, brand.bannerTop());
    }

    /* A step of the copy: what it says and the phase it is. */
    private record Step(Text words, DiscCopies.Phase phase) {
    }

    /**
     * Roxa's colours.
     *
     * @param bannerTop the band's light at its top
     * @param banner    the lilac it washes down to
     * @param line      the line under it
     * @param logo      the mark and the house's name
     * @param logoInk   the ring inside the mark
     * @param name      the program's name
     * @param sub       its subtitle
     * @param done      a step of the copy done
     */
    private record Brand(int bannerTop, int banner, int line, int logo, int logoInk, int name, int sub, int done) {
    }
}
