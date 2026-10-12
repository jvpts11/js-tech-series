/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.gui.MonitorGlass;
import dev.jstech.computers.gui.layout.CrashLayout;
import dev.jstech.computers.menu.MonitorSessionMenu;
import dev.jstech.computers.operation.payload.crash.CrashKeyPayload;
import dev.jstech.computers.operation.payload.crash.OpenCrashPayload;
import dev.jstech.computers.os.crash.CrashCause;
import dev.jstech.computers.os.crash.CrashPage;
import dev.jstech.computers.os.crash.CrashStyle;
import dev.jstech.computers.os.crash.QrCode;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.tier.HardwareEra;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * The page a system crashed onto: the blue (or, for the hardware, red) page of each edition of Frames, or a kernel's
 * panic on its console.
 *
 * <p>The machine went down and is keeping the time: this joins the page where it stands and waits. Nothing on it
 * answers the keyboard or the mouse, as nothing on a real one did, except a kernel that offers to stop its automatic
 * reboot, which takes a key for that and the next one to reboot. Escape still takes the player away from the desk.
 */
@PaletteHolder
public final class CrashScreen extends AbstractComputerScreen<MonitorSessionMenu> {

    private final BlockPos host;
    private final CrashPage page;
    private final int totalTicks;
    private final boolean endsDark;
    private boolean held;
    private int ticks;

    private static final int W = MonitorGlass.WIDTH;
    private static final int H = MonitorGlass.HEIGHT;
    /** Drawn for this long when the machine did not say, which only a stale packet leaves. */
    private static final int FALLBACK_TICKS = 60;
    /**
     * What the code on the newer pages reads as when a phone is pointed at it, and all it reads as: the code is made
     * from this line by {@link QrCode} and carries nothing else, so nobody is sent anywhere by reading it.
     */
    private static final String QR_TEXT = "lol there is nothing here. 67.";
    /** Where the newer pages send whoever wants to know more, which goes nowhere: it is only words on the page. */
    private static final String STOP_CODE_ADDRESS = "https://www.midsoft.mc/stopcode";
    /** Frames 95's title in its grey band, the edition's own name for itself. */
    private static final String F95_TITLE = " Frames ";
    /** What FreeBSD prints once a key has stopped its automatic reboot, in its own words as every kernel line is. */
    private static final String FREEBSD_HELD = "--> Press a key on the console to reboot <--";

    /**
     * The pages' colours, {@code jsc:boot/crash}: the blue of Frames 95, of XP and 7, of 10 and of 11; the red of 95,
     * of XP and 7 and of 10 and 11; the grey of 95's title and the white of the words; and a console's ground, its
     * text and the panic's own line.
     */
    private static final Palette<Colours> PALETTE = Palettes.declare(JsComputers.MODID, "boot/crash",
            new Colours(0xFF0000AA, 0xFF000082, 0xFF0078D7, 0xFF0067C0, 0xFFAA0000, 0xFFAA0000, 0xFFB3261E,
                    0xFFAAAAAA, 0xFFFFFFFF, 0xFF000000, 0xFFC8C8C8, 0xFFFF5555));

    /** What the machine last said it crashed onto, kept until the screen showing it is built. */
    @Nullable
    private static OpenCrashPayload pending;
    /** What a screen being built to draw a monitor's face shows, handed to it alone and taken at once. */
    @Nullable
    private static OpenCrashPayload forFace;
    /** The code the newer pages show, made once: it is the same line every time. */
    @Nullable
    private static boolean[][] code;

    public CrashScreen(final MonitorSessionMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title);
        this.imageWidth = W;
        this.imageHeight = H;
        this.titleLabelX = OFF_SCREEN;
        this.inventoryLabelY = OFF_SCREEN;
        final OpenCrashPayload shown = forFace != null ? forFace : pending;
        forFace = null;
        this.host = menu.hostPos();
        if (shown == null) {
            this.page = new CrashPage(CrashStyle.CONSOLE, CrashCause.MEMORY, "", "", "", List.of(), FALLBACK_TICKS,
                    false);
            this.totalTicks = FALLBACK_TICKS;
            this.endsDark = false;
            return;
        }
        this.page = shown.page();
        this.totalTicks = Math.max(1, this.page.totalTicks());
        this.endsDark = shown.endsDark();
        this.held = shown.held();
        this.ticks = Math.max(0, this.totalTicks - Math.max(0, shown.remainingTicks()));
    }

    /** The page the machine crashed onto, said before the session that shows it is opened. */
    public static void expect(final OpenCrashPayload crash) {
        pending = crash;
        // A player already on the page is not sent a new session: the page they are on takes what changed.
        if (Minecraft.getInstance().screen instanceof CrashScreen open && open.host.equals(crash.hostPos())) {
            open.held = crash.held();
            open.ticks = Math.max(0, open.totalTicks - Math.max(0, crash.remainingTicks()));
        }
    }

    /** The same, for the next screen built to draw a monitor's face, leaving what a session expects alone. */
    public static void faceWith(final OpenCrashPayload crash, final int remainingTicks) {
        forFace = new OpenCrashPayload(crash.hostPos(), crash.monitorPos(), crash.page(), remainingTicks,
                crash.held(), crash.endsDark());
    }

    /** The page on the glass, for a test to read. */
    public CrashPage page() {
        return this.page;
    }

    /** Whether a key has stopped the automatic reboot, for a test to read. */
    public boolean held() {
        return this.held;
    }

    /** How far through its time the page is, in hundredths: the percentage the newer pages count. */
    public int percent() {
        return Math.min(100, this.ticks * 100 / this.totalTicks);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /**
     * Escape takes the player away from the desk; every other key is the console's, which only a kernel offering to
     * stop its reboot answers.
     */
    @Override
    public boolean keyPressed(final int key, final int scanCode, final int modifiers) {
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            return super.keyPressed(key, scanCode, modifiers);
        }
        if (this.page.holdsOnKey() && !onFace()) {
            PacketDistributor.sendToServer(new CrashKeyPayload(this.host));
        }
        return true;
    }

    @Override
    public boolean charTyped(final char c, final int modifiers) {
        return true;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (this.held) {
            return;
        }
        if (this.ticks < this.totalTicks) {
            this.ticks++;
            return;
        }
        // A machine left with no computer has nothing to start over: the page sees itself out and the monitor is dark.
        if (this.endsDark && Minecraft.getInstance().screen == this) {
            onClose();
        }
    }

    @Override
    protected void renderBg(final GuiGraphics g, final float partialTick, final int mouseX, final int mouseY) {
        final int x = this.leftPos;
        final int y = this.topPos;
        final HardwareEra era = screenEra() == null ? HardwareEra.STANDARD : screenEra();
        MonitorFrame.renderBody(g, x, y, W, H, era, font);
        final Colours c = PALETTE.get();
        g.fill(x, y, x + W, y + H, ground(c));
        g.pose().pushPose();
        g.pose().translate(x, y, 0.0F);
        switch (this.page.style()) {
            case FRAMES_95 -> frames95(g, c);
            case FRAMES_XP, FRAMES_7 -> framesNt(g, c);
            case FRAMES_10, FRAMES_11 -> framesModern(g, c);
            case CONSOLE -> console(g, c);
        }
        g.pose().popPose();
    }

    /** The machine's own generation, so the bezel is the monitor that machine would really have. */
    @Override
    @Nullable
    protected HardwareEra screenEra() {
        return this.getMenu().hardwareEra();
    }

    private int ground(final Colours c) {
        final boolean red = this.page.red();
        return switch (this.page.style()) {
            case FRAMES_95 -> red ? c.red95() : c.blue95();
            case FRAMES_XP, FRAMES_7 -> red ? c.redNt() : c.blueNt();
            case FRAMES_10 -> red ? c.redModern() : c.blue10();
            case FRAMES_11 -> red ? c.redModern() : c.blue11();
            case CONSOLE -> c.consoleGround();
        };
    }

    /* Frames 95: the grey title, the fatal exception, the two points, and the restart under them. */
    private void frames95(final GuiGraphics g, final Colours c) {
        final int titleWidth = TextWall.width(font, F95_TITLE);
        final int bandX = (W - titleWidth) / 2 - CrashLayout.BAND_PAD;
        g.fill(bandX, CrashLayout.BAND_Y - 2, bandX + titleWidth + 2 * CrashLayout.BAND_PAD,
                CrashLayout.BAND_Y - 2 + CrashLayout.BAND_H, c.band());
        TextWall.draw(g, font, F95_TITLE, (W - titleWidth) / 2, CrashLayout.BAND_Y, ground(c));
        final int width = W - 2 * CrashLayout.F95_MARGIN;
        int y = CrashLayout.F95_TEXT_Y;
        for (final String line : wrap(GameText.resolve(CrashTexts.F95_FATAL.with(this.page.stopCode(),
                this.page.stopLine())), width)) {
            TextWall.draw(g, font, line, CrashLayout.F95_MARGIN, y, c.ink());
            y += CrashLayout.ROW;
        }
        y += CrashLayout.ROW;
        final TextKey[] points = switch (this.page.cause()) {
            case HARDWARE -> new TextKey[] {CrashTexts.F95_HARDWARE, CrashTexts.F95_HARDWARE_NEXT};
            case INIT_KILLED -> new TextKey[] {CrashTexts.F95_PROCESS, CrashTexts.F95_PROCESS_NEXT};
            case MEMORY -> new TextKey[] {CrashTexts.F95_MEMORY, CrashTexts.F95_MEMORY_NEXT};
        };
        // Each point keeps its asterisk in the margin and its wrapped lines under its first word, as the real page did.
        for (final TextKey point : points) {
            TextWall.draw(g, font, "*", CrashLayout.F95_MARGIN, y, c.ink());
            for (final String line : wrap(GameText.resolve(point), width - CrashLayout.F95_POINT_INDENT)) {
                TextWall.draw(g, font, line, CrashLayout.F95_MARGIN + CrashLayout.F95_POINT_INDENT, y, c.ink());
                y += CrashLayout.ROW;
            }
        }
        // A machine left with no computer has nothing to restart, so the page does not promise it.
        if (!this.endsDark) {
            TextWall.centered(g, font, GameText.resolve(CrashTexts.F95_RESTARTING), W / 2,
                    y + CrashLayout.F95_RESTART_GAP, c.ink());
        }
    }

    /* Frames XP and 7: the Stop error screen, top to bottom, and the dump of the memory at its foot. */
    private void framesNt(final GuiGraphics g, final Colours c) {
        final TextKey advice = switch (this.page.cause()) {
            case HARDWARE -> CrashTexts.NT_ADVICE_HARDWARE;
            case INIT_KILLED -> CrashTexts.NT_ADVICE_PROCESS;
            case MEMORY -> CrashTexts.NT_ADVICE_MEMORY;
        };
        final List<String> paragraphs = new ArrayList<>(List.of(GameText.resolve(CrashTexts.NT_DETECTED), "",
                this.page.stopCode(), "", GameText.resolve(CrashTexts.NT_FIRST_TIME), "", GameText.resolve(advice), "",
                GameText.resolve(CrashTexts.NT_TECHNICAL), "",
                GameText.resolve(CrashTexts.NT_STOP.with(this.page.stopLine())), ""));
        if (this.page.style() == CrashStyle.FRAMES_7) {
            paragraphs.add(GameText.resolve(CrashTexts.SEVEN_COLLECTING));
            paragraphs.add(GameText.resolve(CrashTexts.SEVEN_INITIALIZING));
            paragraphs.add(GameText.resolve(CrashTexts.SEVEN_BEGIN));
            paragraphs.add(GameText.resolve(CrashTexts.SEVEN_DUMPING.with(String.valueOf(percent()))));
        } else {
            paragraphs.add(GameText.resolve(CrashTexts.XP_DUMP_BEGIN));
            paragraphs.add(GameText.resolve(CrashTexts.XP_DUMP_DONE));
            paragraphs.add(GameText.resolve(CrashTexts.XP_CONTACT));
        }
        int y = CrashLayout.NT_MARGIN;
        for (final String paragraph : paragraphs) {
            for (final String line : wrap(paragraph, W - 2 * CrashLayout.NT_MARGIN)) {
                TextWall.draw(g, font, line, CrashLayout.NT_MARGIN, y, c.ink());
                y += CrashLayout.ROW;
            }
        }
    }

    /* Frames 10 and 11: the sad face, the sentence, how much is collected, the code, and the stop code beside it. */
    private void framesModern(final GuiGraphics g, final Colours c) {
        Draw.text(g, font, ":(", CrashLayout.MODERN_X, CrashLayout.FACE_Y, c.ink(), ground(c), CrashLayout.FACE_SCALE);
        final TextKey sentence = this.page.style() == CrashStyle.FRAMES_10 ? CrashTexts.TEN_SENTENCE
                : CrashTexts.ELEVEN_SENTENCE;
        final List<String> rows = wrapScaled(GameText.resolve(sentence), CrashLayout.SENTENCE_W,
                CrashLayout.SENTENCE_SCALE);
        int y = CrashLayout.SENTENCE_Y;
        for (final String row : rows) {
            Draw.text(g, font, row, CrashLayout.MODERN_X, y, c.ink(), ground(c), CrashLayout.SENTENCE_SCALE);
            y += CrashLayout.SENTENCE_ROW;
        }
        Draw.text(g, font, GameText.resolve(CrashTexts.MODERN_COMPLETE.with(percent() + "%")), CrashLayout.MODERN_X,
                CrashLayout.percentY(rows.size()), c.ink(), ground(c), CrashLayout.SENTENCE_SCALE);
        final boolean[][] modules = code();
        final int qrY = CrashLayout.qrY(rows.size());
        final int side = CrashLayout.qrSide(modules.length);
        g.fill(CrashLayout.MODERN_X, qrY, CrashLayout.MODERN_X + side, qrY + side, c.ink());
        final int inset = CrashLayout.QR_QUIET * CrashLayout.QR_MODULE;
        for (int row = 0; row < modules.length; row++) {
            for (int column = 0; column < modules.length; column++) {
                if (modules[row][column]) {
                    final int mx = CrashLayout.MODERN_X + inset + column * CrashLayout.QR_MODULE;
                    final int my = qrY + inset + row * CrashLayout.QR_MODULE;
                    g.fill(mx, my, mx + CrashLayout.QR_MODULE, my + CrashLayout.QR_MODULE, ground(c));
                }
            }
        }
        final int infoX = CrashLayout.infoX(modules.length);
        final int width = W - CrashLayout.MODERN_X - infoX;
        final List<String> notes = new ArrayList<>(List.of(
                GameText.resolve(CrashTexts.MODERN_MORE.with(STOP_CODE_ADDRESS)), "",
                GameText.resolve(CrashTexts.MODERN_SUPPORT),
                GameText.resolve(CrashTexts.MODERN_STOP_CODE.with(this.page.stopCode()))));
        if (!this.page.failedPart().isEmpty()) {
            notes.add(GameText.resolve(CrashTexts.MODERN_FAILED.with(this.page.failedPart())));
        }
        int ny = qrY + inset;
        for (final String note : notes) {
            for (final String line : wrap(note, width)) {
                TextWall.draw(g, font, line, infoX, ny, c.ink());
                ny += CrashLayout.ROW;
            }
        }
    }

    /* A kernel's panic on its console, wrapped at the glass's edge; the oldest rows go first when it overflows. */
    private void console(final GuiGraphics g, final Colours c) {
        final List<String> lines = new ArrayList<>(this.page.lines());
        if (this.held) {
            lines.add(FREEBSD_HELD);
        }
        final List<String> rows = new ArrayList<>();
        final List<Integer> colours = new ArrayList<>();
        for (final String line : lines) {
            final int colour = headline(line) ? c.consoleHot() : c.consoleInk();
            for (final String row : wrapHard(line, W - 2 * CrashLayout.CONSOLE_MARGIN)) {
                rows.add(row);
                colours.add(colour);
            }
        }
        final int first = Math.max(0, rows.size() - CrashLayout.consoleRows());
        int y = CrashLayout.CONSOLE_MARGIN;
        for (int i = first; i < rows.size(); i++) {
            TextWall.draw(g, font, rows.get(i), CrashLayout.CONSOLE_MARGIN, y, colours.get(i));
            y += CrashLayout.ROW;
        }
    }

    /* Whether a console line is the panic itself, the one line a reader's eye has to land on. */
    private static boolean headline(final String line) {
        final int stamp = line.startsWith("[") ? line.indexOf("] ") : -1;
        final String message = stamp >= 0 ? line.substring(stamp + 2) : line;
        return message.startsWith("Kernel panic - not syncing") || message.startsWith("panic:")
                || message.startsWith("PANIC:");
    }

    private static boolean[][] code() {
        if (code == null) {
            code = QrCode.encode(QR_TEXT);
        }
        return code;
    }

    /* The words of {@code text} in rows no wider than {@code width} at the wall's scale. */
    private List<String> wrap(final String text, final int width) {
        return wrapScaled(text, width, TextWall.SCALE);
    }

    /* The same at any scale; a word wider than the row is cut where it has to be, as a long address is. */
    private List<String> wrapScaled(final String text, final int width, final float scale) {
        final List<String> rows = new ArrayList<>();
        if (text.isEmpty()) {
            rows.add("");
            return rows;
        }
        String row = "";
        for (final String word : text.split(" ")) {
            final String trial = row.isEmpty() ? word : row + " " + word;
            if (font.width(trial) * scale <= width) {
                row = trial;
                continue;
            }
            if (!row.isEmpty()) {
                rows.add(row);
            }
            row = word;
            while (font.width(row) * scale > width && row.length() > 1) {
                int cut = row.length() - 1;
                while (cut > 1 && font.width(row.substring(0, cut)) * scale > width) {
                    cut--;
                }
                rows.add(row.substring(0, cut));
                row = row.substring(cut);
            }
        }
        rows.add(row);
        return rows;
    }

    /* A console line broken where the glass ends, word or no word, the way a text console wraps. */
    private List<String> wrapHard(final String text, final int width) {
        final List<String> rows = new ArrayList<>();
        String rest = text;
        while (TextWall.width(font, rest) > width && rest.length() > 1) {
            int cut = rest.length() - 1;
            while (cut > 1 && TextWall.width(font, rest.substring(0, cut)) > width) {
                cut--;
            }
            rows.add(rest.substring(0, cut));
            rest = rest.substring(cut);
        }
        rows.add(rest);
        return rows;
    }

    /** The pages' colours, as the palette above names them. */
    private record Colours(int blue95, int blueNt, int blue10, int blue11, int red95, int redNt, int redModern,
                           int band, int ink, int consoleGround, int consoleInk, int consoleHot) {
    }
}
