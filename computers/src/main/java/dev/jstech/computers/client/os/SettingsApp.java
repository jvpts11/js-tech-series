/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.audio.SoundOutput;
import dev.jstech.computers.hardware.DiskSpec;
import dev.jstech.computers.operation.payload.RequestFirmwarePayload;
import dev.jstech.computers.operation.payload.RequestSettingsPayload;
import dev.jstech.computers.operation.payload.SetSettingPayload;
import dev.jstech.computers.operation.payload.SettingsSnapshotPayload;
import dev.jstech.computers.operation.payload.TestSoundPayload;
import dev.jstech.computers.operation.payload.UninstallProgramPayload;
import dev.jstech.computers.gui.layout.SettingsLayout;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.os.ProgramSpec;
import dev.jstech.computers.program.Accents;
import dev.jstech.computers.program.ThemePreset;
import dev.jstech.core.client.gui.component.Button;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.gui.component.Label;
import dev.jstech.core.client.gui.component.ListView;
import dev.jstech.core.client.gui.component.Panel;
import dev.jstech.core.client.gui.component.ProgressBar;
import dev.jstech.core.client.gui.component.ScrollPanel;
import dev.jstech.core.client.gui.component.TextField;
import dev.jstech.core.client.gui.component.UiComponent;
import dev.jstech.core.client.gui.component.UiContext;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextKey;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;

/**
 * The Settings desktop app: one per-computer control panel, drawn through the running OS skin so its
 * form changes with the OS (a basic bevel on Frames 95, the richest layout on Frames 11). A left nav
 * lists eight pages, seven live and one placeholder, and the right pane edits or shows each one.
 *
 * <p>All editable knobs round-trip through the server: opening the app requests a
 * {@link SettingsSnapshotPayload}, and every change sends a {@link SetSettingPayload} and rebuilds the
 * page from the refreshed snapshot the server replies with.
 */
public final class SettingsApp implements IDesktopApp {

    private static final List<TextKey> NAV = List.of(SettingsTexts.PERSONALIZE, SettingsTexts.SYSTEM,
            SettingsTexts.NETWORK, SettingsTexts.STORAGE, SettingsTexts.DISPLAY, SettingsTexts.PROGRAMS,
            SettingsTexts.SOUND, SettingsTexts.USERS);
    private static final int FIRST_SOON = 7;
    /** The most speakers the Sound page lists by name; past that it says how many more there are. */
    private static final int SPEAKER_ROWS = 2;
    private static final int NAV_W = 78;
    private static final int NAV_ROW_H = 15;
    private static final int BTN_H = 13;
    private static final int NAME_MAX = 24;

    private final BlockPos host;
    private OsSkin skin = OsSkin.fallback();
    @Nullable
    private SettingsSnapshotPayload data;
    private int page;
    private int snapshots;
    private int lastMouseX;
    private int lastMouseY;

    private static SettingsApp active;

    /** The monitor this desktop runs on (the firmware restart reopens setup there); null when unknown. */
    @Nullable
    private BlockPos monitorPos;

    // components
    private final Panel root = new Panel();
    private final ListView<TextKey> nav;
    private final Panel pagePanel = new Panel();
    /** The Personalize page's own content, taller than its window once enough wallpapers are offered. */
    private final ScrollPanel personalizeScroll = new ScrollPanel();
    private final Label loadingLabel;
    /** What the page was last built for; a change in any part rebuilds it. */
    private String builtFor = "";
    @Nullable
    private TextField nameField;
    @Nullable
    private Font lastFont;

    /** The Network page's sharing controls, kept so the client tests can find them after a rebuild. */
    private static final int SHARE_ROWS = 4;
    private static final int SHARE_ROW_H = 12;
    private static final int SHARE_PATH_MAX = 96;
    private static final float SMALL = 0.75f;
    private final List<Button> shareRowRead = new ArrayList<>();
    private final List<Button> shareRowWrite = new ArrayList<>();
    private final List<Button> shareRowRemove = new ArrayList<>();
    @Nullable
    private TextField shareField;
    @Nullable
    private Button shareReadOnly;
    @Nullable
    private Button shareForWriting;
    @Nullable
    private Button remoteAllowedButton;
    @Nullable
    private Button remoteRefusedButton;
    private boolean remoteAllowed = true;
    /** The Sound page's Test button, kept so the client tests can find it after a rebuild. */
    @Nullable
    private Button testButton;
    /** The Personalize page's dark-mode button, kept so a client test can prove scrolling brings it into view. */
    @Nullable
    private Button appearanceDarkButton;

    /** A wallpaper style or an accent colour as a small square that a click chooses. */
    private final class Swatch extends UiComponent {

        @Nullable
        private final String style;
        private final int color;
        private final BooleanSupplier on;
        private final Runnable onClick;

        Swatch(@Nullable final String style, final int color, final BooleanSupplier on, final Runnable onClick) {
            this.style = style;
            this.color = color;
            this.on = on;
            this.onClick = onClick;
        }

        @Override
        public void render(final GuiGraphics g, final UiContext ctx) {
            if (style != null) {
                WallpaperStyle.swatch(g, x(), y(), width(), height(), style);
                if (on.getAsBoolean()) {
                    Draw.outline(g, x() - 1, y() - 1, width() + 2, height() + 2, ctx.skin().accent());
                } else {
                    Draw.outline(g, x(), y(), width(), height(), ctx.skin().edge());
                }
                return;
            }
            g.fill(x(), y(), right(), bottom(), color);
            final int grow = on.getAsBoolean() ? 1 : 0;
            Draw.outline(g, x() - grow, y() - grow, width() + grow * 2, height() + grow * 2,
                    on.getAsBoolean() ? ctx.skin().text() : ctx.skin().edge());
        }

        @Override
        public boolean mouseClicked(final double mx, final double my, final int button) {
            if (button != 0) {
                return false;
            }
            onClick.run();
            return true;
        }
    }

    /** The desktop form: knows its monitor, so "Restart to firmware" can reopen the setup on it. */
    public SettingsApp(final BlockPos host, @Nullable final BlockPos monitorPos) {
        this(host);
        this.monitorPos = monitorPos;
    }

    public SettingsApp(final BlockPos host) {
        this.host = host;
        nav = root.add(new ListView<TextKey>(() -> NAV, NAV_ROW_H, this::renderNavRow).setOnClick(this::navClicked));
        loadingLabel = root.add(new Label(GameText.resolve(SettingsTexts.LOADING), Label.Tone.DIM));
        root.add(pagePanel);
        active = this;
        PacketDistributor.sendToServer(new RequestSettingsPayload(host));
    }

    @Override
    public void onRestored() {
        active = this;
        PacketDistributor.sendToServer(new RequestSettingsPayload(host));
    }

    /** Routes a settings snapshot reply to the open Settings window. */
    public static void accept(final SettingsSnapshotPayload payload) {
        if (active != null && active.host.equals(payload.hostPos())) {
            active.data = payload;
            active.snapshots++;
            // Reflect accent, brightness, clock, wallpaper, taskbar layout and dark mode on the live desktop now.
            DesktopScreen.applyLivePrefs(payload.accent(), payload.brightness(), payload.clock12h(),
                    payload.wallpaper(), payload.taskbarCentered(), payload.darkMode(), payload.guiScale());
        }
    }

    @Override
    public String title() {
        return ProgramClient.nameOf("settings");
    }

    @Override
    public int defaultWidth() {
        return 262;
    }

    @Override
    public int defaultHeight() {
        return 224;
    }

    @Override
    public int minWidth() {
        return 236;
    }

    @Override
    public int minHeight() {
        return 160;
    }

    @Override
    public void applySkin(final OsSkin osSkin) {
        this.skin = osSkin;
    }

    private void set(final String key, final String value) {
        PacketDistributor.sendToServer(new SetSettingPayload(host, key, value));
    }

    private void navClicked(final int index, final int button, final double mx, final double my) {
        if (button == 0 && index >= 0 && index < NAV.size()) {
            page = index;
        }
    }

    private void renderNavRow(final GuiGraphics g, final UiContext ctx, final TextKey item, final int index,
                              final int x, final int y, final int w, final int h, final boolean hovered,
                              final boolean selected) {
        final boolean sel = page == index;
        ctx.skin().listRow(g, x, y, w, h, hovered, sel);
        final int tc = sel ? ctx.skin().listRowText(true) : (index >= FIRST_SOON ? ctx.skin().dim() : ctx.skin().text());
        g.drawString(ctx.font(), ctx.font().plainSubstrByWidth(GameText.resolve(item), w - 7), x + 5, y + 4, tc,
                false);
    }

    // rendering

    @Override
    public void renderContent(final GuiGraphics g, final Font font, final int x, final int y,
                              final int width, final int height, final int mouseX, final int mouseY,
                              final float partialTick) {
        lastFont = font;
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        final UiContext ctx = new UiContext(skin, font, mouseX, mouseY, partialTick);
        g.fill(x, y, x + width, y + height, skin.windowBg());
        nav.setBounds(x + 3, y + 4, NAV_W, NAV.size() * NAV_ROW_H);
        g.fill(x + NAV_W + 5, y + 3, x + NAV_W + 6, y + height - 3, skin.edge());

        final int px = x + NAV_W + 11;
        final int py = y + 6;
        final int pw = width - NAV_W - 15;
        final int ph = height - 12;
        loadingLabel.setVisible(data == null);
        loadingLabel.setBounds(px, y + 8, pw, 8);
        pagePanel.setBounds(px, py, pw, ph);
        ensurePage(px, py, pw, ph, font);
        root.render(g, ctx);
    }

    /** Rebuilds the page's components when the page, the snapshot, the skin or the space changed. */
    private void ensurePage(final int px, final int py, final int pw, final int ph, final Font font) {
        /*
         * The scroll offset is part of the key: the Personalize page's rows are placed once, at build time,
         * through personalizeScroll.contentY(...), and a plain scroll (which only moves the thumb, not the
         * children) would leave them exactly where they were. Rebuilding on every scroll re-places them at
         * their new position instead.
         */
        final String key = page + "|" + snapshots + "|" + skin.form() + "|" + (monitorPos != null) + "|"
                + px + "," + py + "," + pw + "," + ph + "|" + personalizeScroll.scroll();
        if (key.equals(builtFor)) {
            return;
        }
        if (nameField != null && nameField.isFocused() && key.startsWith(builtFor.substring(0, Math.min(builtFor.length(), 1)))) {
            // A name being typed survives a resize; the snapshot that follows its commit rebuilds the page.
            if (!builtFor.isEmpty() && builtFor.split("\\|")[1].equals(String.valueOf(snapshots)) && page == 1) {
                return;
            }
        }
        builtFor = key;
        pagePanel.clear();
        nameField = null;
        if (data == null) {
            return;
        }
        switch (page) {
            case 0 -> personalize(px, py, pw, ph, font);
            case 1 -> system(px, py, pw, font);
            case 2 -> network(px, py, pw, font);
            case 3 -> storage(px, py, pw, font);
            case 4 -> display(px, py, pw, font);
            case 5 -> programs(px, py, pw, font);
            case PAGE_SOUND -> sound(px, py, pw, font);
            default -> comingSoon(px, py, pw, ph);
        }
    }

    private Label heading(final TextKey title, final int x, final int y, final int w) {
        return heading(pagePanel, title, x, y, w);
    }

    private Label heading(final Panel target, final TextKey title, final int x, final int y, final int w) {
        final Label label = target.add(new Label(GameText.resolve(title)));
        label.setBounds(x, y, w, 8);
        return label;
    }

    private Label caption(final Text text, final int x, final int y, final int w) {
        return caption(pagePanel, text, x, y, w);
    }

    private Label caption(final Panel target, final Text text, final int x, final int y, final int w) {
        final Label label = target.add(new Label(GameText.resolve(text), Label.Tone.DIM));
        label.setBounds(x, y, w, 8);
        return label;
    }

    private Label caption(final TextKey text, final int x, final int y, final int w) {
        return caption(text.text(), x, y, w);
    }

    private Label caption(final Panel target, final TextKey text, final int x, final int y, final int w) {
        return caption(target, text.text(), x, y, w);
    }

    /**
     * The Personalize page: the wallpaper grid, then the accent/theme/clock/taskbar/appearance rows below it, all
     * placed from {@link SettingsLayout}'s own offsets so the page's geometry is the one that class's own test
     * proves clean at the worst case. The content scrolls, since a growing table of wallpapers can outgrow even
     * the tallest window this app is ever given: a fixed panel would silently clip whatever ran past its bottom.
     */
    private void personalize(final int x, final int top, final int w, final int h, final Font font) {
        final SettingsSnapshotPayload d = data;
        appearanceDarkButton = null;
        // The desktop's own wallpaper first, then every style a player may hang instead.
        final List<String> styles = new ArrayList<>();
        styles.add("");
        for (final WallpaperStyle offered : WallpaperStyle.offered()) {
            styles.add(offered.id());
        }
        final boolean richSkin = skin.form() != OsSkin.Form.BEVEL;
        final boolean flatSkin = skin.form() == OsSkin.Form.FLAT;
        final SettingsLayout.Offsets o = SettingsLayout.of(w, styles.size(), richSkin, flatSkin);

        personalizeScroll.clear();
        pagePanel.add(personalizeScroll);
        personalizeScroll.setStep(SettingsLayout.SWATCH_H).setContentHeight(o.contentHeight());
        personalizeScroll.setBounds(x, top, w, h);

        heading(personalizeScroll, SettingsTexts.PERSONALIZE, x, personalizeScroll.contentY(0), w);
        caption(personalizeScroll, SettingsTexts.WALLPAPER, x, personalizeScroll.contentY(o.wallpaperCaptionY()), w);
        for (int i = 0; i < styles.size(); i++) {
            final String style = styles.get(i);
            final int sx = x + SettingsLayout.swatchX(i, o.gridColumns());
            final int sy = personalizeScroll.contentY(SettingsLayout.swatchY(o.gridY(), i, o.gridColumns()));
            final Swatch swatch =
                    new Swatch(style, 0, () -> style.equals(d.wallpaper()), () -> set("wallpaper", style));
            personalizeScroll.add(swatch).setBounds(sx, sy, SettingsLayout.SWATCH_W, SettingsLayout.SWATCH_H);
        }

        // Accent and theme only on the richer skins (scales with the OS).
        if (richSkin) {
            caption(personalizeScroll, SettingsTexts.ACCENT, x, personalizeScroll.contentY(o.accentCaptionY()), w);
            // Each swatch offers the colour it shows, which is the one a resource pack gives it.
            final List<Integer> accents = Accents.PALETTE.get().all();
            final int accentY = personalizeScroll.contentY(o.accentY());
            for (int i = 0; i < accents.size(); i++) {
                final int argb = accents.get(i);
                personalizeScroll.add(new Swatch(null, argb, () -> (d.accent() & 0xFFFFFF) == (argb & 0xFFFFFF),
                        () -> set("accent", String.format(Locale.ROOT, "%06X", argb & 0xFFFFFF))))
                        .setBounds(x + i * 18, accentY, 14, 14);
            }
            caption(personalizeScroll, SettingsTexts.THEME, x, personalizeScroll.contentY(o.themeCaptionY()), w);
            int tx = x;
            final int themeY = personalizeScroll.contentY(o.themeY());
            final ThemePreset current = ThemePreset.byId(d.themePreset());
            for (final ThemePreset preset : ThemePreset.values()) {
                final String theme = preset.id();
                final int bw = font.width(theme) + 12;
                personalizeScroll.add(new Button(theme, () -> set("theme", theme)).setPrimary(preset == current))
                        .setBounds(tx, themeY, bw, BTN_H);
                tx += bw + 4;
            }
        }
        caption(personalizeScroll, SettingsTexts.CLOCK, x, personalizeScroll.contentY(o.clockCaptionY()), w);
        toggleButtons(personalizeScroll, x, personalizeScroll.contentY(o.clockY()), font, SettingsTexts.HOUR_24,
                SettingsTexts.HOUR_12, !d.clock12h(), () -> set("clock", "24h"), () -> set("clock", "12h"));

        // Taskbar alignment and dark mode are Frames 11 concepts only, so they appear exclusively on the flat skin.
        if (flatSkin) {
            caption(personalizeScroll, SettingsTexts.TASKBAR, x, personalizeScroll.contentY(o.taskbarCaptionY()), w);
            toggleButtons(personalizeScroll, x, personalizeScroll.contentY(o.taskbarY()), font, SettingsTexts.CENTER,
                    SettingsTexts.LEFT, d.taskbarCentered(), () -> set("taskbar", "center"),
                    () -> set("taskbar", "left"));
            caption(personalizeScroll, SettingsTexts.APPEARANCE, x,
                    personalizeScroll.contentY(o.appearanceCaptionY()), w);
            appearanceDarkButton = toggleButtons(personalizeScroll, x, personalizeScroll.contentY(o.appearanceY()),
                    font, SettingsTexts.LIGHT, SettingsTexts.DARK, !d.darkMode(), () -> set("darkmode", "off"),
                    () -> set("darkmode", "on"));
        }
    }

    private void system(final int x, final int top, final int w, final Font font) {
        final SettingsSnapshotPayload d = data;
        int y = top;
        heading(SettingsTexts.SYSTEM, x, y, w);
        y += 13;
        caption(SettingsTexts.COMPUTER_NAME, x, y, w);
        y += 10;
        final TextField field = pagePanel.add(new TextField(NAME_MAX)
                .setPlaceholder(GameText.resolve(SettingsTexts.UNNAMED))
                .setOnCommit(name -> set("name", name)));
        field.sync(d.computerName());
        field.setBounds(x, y, Math.min(w, 130), 13);
        nameField = field;
        y += 20;

        heading(SettingsTexts.ABOUT, x, y, w);
        y += 12;
        y = specRow(x, y, w, SettingsTexts.PROCESSOR, SettingsTexts.PROCESSOR_VALUE.with(d.cpuLabel(), d.cpuMhz()));
        if (!d.cpuArch().isEmpty()) {
            y = specRow(x, y, w, SettingsTexts.ARCHITECTURE, d.cpuArch());
        }
        if (d.ramMb() > 0) {
            y = specRow(x, y, w, SettingsTexts.MEMORY, SettingsTexts.MEGABYTES.with(group(d.ramMb())));
        }
        if (d.vramMb() > 0) {
            y = specRow(x, y, w, SettingsTexts.GRAPHICS, SettingsTexts.VRAM.with(d.vramMb()));
        }
        y = specRow(x, y, w, SettingsTexts.SYSTEM, Text.literal(d.osLabel()));
        y = specRow(x, y, w, SettingsTexts.PLATFORM, Text.literal(d.platform()));
        // Restart into the firmware setup (the boot manager): the way to reach it once an OS is installed.
        final BlockPos monitor = monitorPos;
        if (monitor != null) {
            y += 4;
            final String label = GameText.resolve(SettingsTexts.RESTART_TO_FIRMWARE);
            pagePanel.add(new Button(label, () -> PacketDistributor.sendToServer(new RequestFirmwarePayload(host, monitor))))
                    .setBounds(x, y, font.width(label) + 12, BTN_H);
        }
    }

    private void network(final int x, final int top, final int w, final Font font) {
        final SettingsSnapshotPayload d = data;
        int y = top;
        heading(SettingsTexts.NETWORK, x, y, w);
        y += 13;
        caption(SettingsTexts.PUBLIC_SHARE, x, y, w);
        y += 11;
        final int permille = d.netshare();
        stepper(x, y, font, GameText.resolve(SettingsTexts.SHARE_AMOUNT.with(
                        String.format(Locale.ROOT, "%.1f", permille / 10.0), permille)),
                () -> set("netshare", Integer.toString(Math.max(0, permille - 50))),
                () -> set("netshare", Integer.toString(Math.min(1000, permille + 50))));
        y += 20;
        pagePanel.add(new ProgressBar(() -> permille / 10)).setBounds(x, y, Math.min(w, 150), 6);
        y += 16;
        caption(SettingsTexts.LINK, x, y, w);
        y += 12;

        /*
         * The folders this computer shares, as the prompt's "config share" lists them: one row each with its
         * mode, a read / write pair to change it and Remove; then a field and two buttons to share another.
         * The page has room for a few rows; past that the prompt is the place to see them all.
         */
        caption(SettingsTexts.SHARED_FOLDERS, x, y, w);
        y += 10;
        shareRowRead.clear();
        shareRowWrite.clear();
        shareRowRemove.clear();
        final List<SettingsSnapshotPayload.ShareRow> shares = d.shares();
        final int shown = Math.min(shares.size(), SHARE_ROWS);
        for (int i = 0; i < shown; i++) {
            final SettingsSnapshotPayload.ShareRow share = shares.get(i);
            /*
             * The page is narrow: the path takes what the three buttons leave, and the mode is the button
             * that is lit (read or write), not a word of its own.
             */
            pagePanel.add(new Label(share.path(), share.writable() ? Label.Tone.ACCENT : Label.Tone.TEXT).setScale(SMALL))
                    .setBounds(x, y + 2, w - 100, 8);
            shareRowRead.add(pagePanel.add(new Button(GameText.resolve(SettingsTexts.READ),
                    () -> set("share", share.path() + " read")).setPrimary(!share.writable()).setLabelScale(SMALL)));
            shareRowRead.get(i).setBounds(x + w - 96, y, 24, SHARE_ROW_H - 1);
            shareRowWrite.add(pagePanel.add(new Button(GameText.resolve(SettingsTexts.WRITE),
                    () -> set("share", share.path() + " write")).setPrimary(share.writable()).setLabelScale(SMALL)));
            shareRowWrite.get(i).setBounds(x + w - 70, y, 28, SHARE_ROW_H - 1);
            shareRowRemove.add(pagePanel.add(new Button(GameText.resolve(SettingsTexts.REMOVE),
                    () -> set("unshare", share.name())).setLabelScale(SMALL)));
            shareRowRemove.get(i).setBounds(x + w - 40, y, 40, SHARE_ROW_H - 1);
            y += SHARE_ROW_H;
        }
        if (shares.size() > shown) {
            caption(SettingsTexts.MORE_SHARES.with(shares.size() - shown), x, y + 2, w);
            y += 10;
        }
        y += 3;
        final TextField field = pagePanel.add(new TextField(SHARE_PATH_MAX)
                .setPlaceholder(GameText.resolve(SettingsTexts.SHARE_HINT)));
        field.setBounds(x, y, w, 13);
        shareField = field;
        y += 17;
        final String readOnly = GameText.resolve(SettingsTexts.SHARE_READ_ONLY);
        final String forWriting = GameText.resolve(SettingsTexts.SHARE_FOR_WRITING);
        final int readW = Math.round(font.width(readOnly) * SMALL) + 10;
        final int writeW = Math.round(font.width(forWriting) * SMALL) + 10;
        shareReadOnly = pagePanel.add(new Button(readOnly, () -> shareTyped("read")).setLabelScale(SMALL));
        shareReadOnly.setBounds(x, y, readW, BTN_H);
        shareForWriting = pagePanel.add(new Button(forWriting, () -> shareTyped("write")).setLabelScale(SMALL));
        shareForWriting.setBounds(x + readW + 4, y, writeW, BTN_H);
        y += BTN_H + 4;
        final String host = d.computerName().isEmpty() ? "computer" : d.computerName().toLowerCase(Locale.ROOT).replace(' ', '-');
        pagePanel.add(new Label(GameText.resolve(SettingsTexts.REACHED_AS.with(host)), Label.Tone.DIM)
                .setScale(SMALL)).setBounds(x, y, w, 8);
        y += 8;
        pagePanel.add(new Label(GameText.resolve(SettingsTexts.CC_REACHES_AS.with(host)), Label.Tone.DIM)
                .setScale(SMALL)).setBounds(x, y, w, 8);
        y += 13;

        caption(SettingsTexts.REMOTE_PROGRAMS, x, y, w);
        y += 10;
        remoteAllowed = d.remoteAllowed();
        final String allowed = GameText.resolve(SettingsTexts.ALLOWED);
        final String refused = GameText.resolve(SettingsTexts.REFUSED);
        final int allowedW = font.width(allowed) + 12;
        remoteAllowedButton = pagePanel.add(new Button(allowed, () -> set("remote", "on"))
                .setPrimary(d.remoteAllowed()));
        remoteAllowedButton.setBounds(x, y, allowedW, BTN_H);
        remoteRefusedButton = pagePanel.add(new Button(refused, () -> set("remote", "off"))
                .setPrimary(!d.remoteAllowed()));
        remoteRefusedButton.setBounds(x + allowedW + 4, y, font.width(refused) + 12, BTN_H);
    }

    // what the client tests read and click

    public int page() {
        return page;
    }

    public int[] navCenter(final int index) {
        return nav.rowCenter(index);
    }

    /** The shares the Network page shows, each as its path and mode. */
    public List<String> sharesShown() {
        final List<String> out = new ArrayList<>();
        if (data != null) {
            for (final SettingsSnapshotPayload.ShareRow share : data.shares()) {
                out.add(share.path() + " " + (share.writable() ? "write" : "read"));
            }
        }
        return out;
    }

    public boolean remoteAllowedShown() {
        return remoteAllowed;
    }

    public int[] shareFieldCenter() {
        return shareField == null ? new int[] {0, 0} : shareField.center();
    }

    public int[] shareReadOnlyCenter() {
        return shareReadOnly == null ? new int[] {0, 0} : shareReadOnly.center();
    }

    public int[] shareForWritingCenter() {
        return shareForWriting == null ? new int[] {0, 0} : shareForWriting.center();
    }

    public int[] shareRowReadCenter(final int row) {
        return row < shareRowRead.size() ? shareRowRead.get(row).center() : new int[] {0, 0};
    }

    public int[] shareRowRemoveCenter(final int row) {
        return row < shareRowRemove.size() ? shareRowRemove.get(row).center() : new int[] {0, 0};
    }

    public int[] remoteRefusedCenter() {
        return remoteRefusedButton == null ? new int[] {0, 0} : remoteRefusedButton.center();
    }

    public int[] remoteAllowedCenter() {
        return remoteAllowedButton == null ? new int[] {0, 0} : remoteAllowedButton.center();
    }

    public int[] testSoundCenter() {
        return testButton == null ? new int[] {0, 0} : testButton.center();
    }

    /** A point inside the Personalize page's own scrolling content, for a client test to aim the wheel at. */
    public int[] personalizeScrollCenter() {
        return personalizeScroll.center();
    }

    /** The flat skin's dark-mode button on the Personalize page, or the origin before it is ever built. */
    public int[] appearanceDarkCenter() {
        return appearanceDarkButton == null ? new int[] {0, 0} : appearanceDarkButton.center();
    }

    /** Whether the dark-mode button lies whole inside the Personalize page's visible area, not clipped by it. */
    public boolean appearanceDarkFullyShown() {
        return appearanceDarkButton != null && appearanceDarkButton.y() >= personalizeScroll.y()
                && appearanceDarkButton.bottom() <= personalizeScroll.bottom();
    }

    /** The sound the Sound page was last built from, or null before the first snapshot. */
    @Nullable
    public SettingsSnapshotPayload.Sound soundShown() {
        return data == null ? null : data.sound();
    }

    /** Shares the folder typed in the field, {@code mode} being {@code read} or {@code write}; nothing typed, nothing sent. */
    private void shareTyped(final String mode) {
        final String path = shareField == null ? "" : shareField.edit().strip();
        if (!path.isEmpty()) {
            set("share", path + " " + mode);
        }
    }

    private void storage(final int x, final int top, final int w, final Font font) {
        final SettingsSnapshotPayload d = data;
        int y = top;
        heading(SettingsTexts.STORAGE, x, y, w);
        y += 13;
        for (final SettingsSnapshotPayload.DiskUse disk : d.disks()) {
            final String cap = DiskSpec.sizeLabel(disk.capMb());
            pagePanel.add(new Label(GameText.resolve(disk.system() ? SettingsTexts.SYSTEM_DISK.with(disk.label())
                    : disk.label()))).setBounds(x, y, w - font.width(cap) - 4, 8);
            pagePanel.add(new Label(cap, Label.Tone.DIM).setAlign(Label.Align.RIGHT)).setBounds(x, y, w, 8);
            y += 10;
            final int percent = disk.capMb() > 0 ? (int) Math.min(100, 100 * disk.usedMb() / disk.capMb()) : 0;
            pagePanel.add(new ProgressBar(() -> percent)).setBounds(x, y, Math.min(w, 150), 5);
            y += 11;
        }
        if (d.disks().isEmpty()) {
            caption(SettingsTexts.NO_DISKS, x, y, w);
        }
    }

    private void display(final int x, final int top, final int w, final Font font) {
        final SettingsSnapshotPayload d = data;
        int y = top;
        heading(SettingsTexts.DISPLAY, x, y, w);
        y += 13;
        caption(SettingsTexts.BRIGHTNESS, x, y, w);
        y += 10;
        final int b = d.brightness();
        stepper(x, y, font, GameText.resolve(SettingsTexts.PERCENT.with(b)),
                () -> set("brightness", Integer.toString(Math.max(0, b - 10))),
                () -> set("brightness", Integer.toString(Math.min(100, b + 10))));
        y += 20;
        /*
         * How big everything on the glass is drawn. Smaller fits more of a program on the screen at
         * the cost of smaller text, which is a choice for the player and the monitor they sit at.
         */
        caption(SettingsTexts.SCALE, x, y, w);
        y += 10;
        final int scale = d.guiScale() <= 0 ? DesktopViewport.DEFAULT_SCALE : d.guiScale();
        final int at = Math.max(0, SCALES.indexOf(scale));
        stepper(x, y, font, GameText.resolve(SettingsTexts.PERCENT.with(scale)),
                () -> set("guiscale", Integer.toString(SCALES.get(Math.min(SCALES.size() - 1, at + 1)))),
                () -> set("guiscale", Integer.toString(SCALES.get(Math.max(0, at - 1)))));
        y += 20;
        caption(SettingsTexts.MONITOR, x, y, w);
    }

    /** The sizes the desktop can be drawn at, the biggest first, as percentages of its own size. */
    private static final List<Integer> SCALES = List.of(100, 90, 80, 75, 66, 50);

    /** The pages, by the index the navigation lists them at, for a menu that opens one directly. */
    public static final int PAGE_PERSONALIZE = 0;
    public static final int PAGE_DISPLAY = 4;
    public static final int PAGE_SOUND = 6;

    /** Opens on {@code index}'s page instead of the first one. */
    public SettingsApp showPage(final int index) {
        if (index >= 0 && index < NAV.size()) {
            page = index;
        }
        return this;
    }

    private void programs(final int x, final int top, final int w, final Font font) {
        final SettingsSnapshotPayload d = data;
        int y = top;
        heading(SettingsTexts.PROGRAMS, x, y, w);
        y += 13;
        if (d.installed().isEmpty()) {
            caption(SettingsTexts.NO_PROGRAMS, x, y, w);
            return;
        }
        final String btn = GameText.resolve(SettingsTexts.UNINSTALL);
        final int bw = font.width(btn) + 8;
        for (final String id : d.installed()) {
            final ResourceLocation rl = ResourceLocation.tryParse(id);
            final ProgramSpec spec = rl == null ? null : OsRegistry.getProgram(rl);
            final String name = spec != null ? spec.displayName() : (id.contains(":") ? id.substring(id.indexOf(':') + 1) : id);
            pagePanel.add(new Label("- " + name)).setBounds(x, y + 1, w - bw - 4, 8);
            // Per-row uninstall: the desktop counterpart of the shell's package removal.
            pagePanel.add(new Button(btn, () -> {
                PacketDistributor.sendToServer(new UninstallProgramPayload(host, id));
                // The uninstall lands before these refreshes are processed (same connection, in order).
                PacketDistributor.sendToServer(new RequestSettingsPayload(host));
                DesktopScreen.refreshActive();
            }).setLabelScale(0.85f)).setBounds(x + w - bw, y - 1, bw, 11);
            y += 12;
        }
    }

    /*
     * The system's sound: its volume and whether it is muted, where it goes, what plays it and the speakers it has,
     * and a button that plays the system's own sound through all of that, for the player to hear the change.
     */
    private void sound(final int x, final int top, final int w, final Font font) {
        final SettingsSnapshotPayload.Sound s = data.sound();
        int y = top;
        heading(SettingsTexts.SOUND, x, y, w);
        y += 13;
        caption(SettingsTexts.VOLUME, x, y, w);
        y += 10;
        final int v = s.volume();
        stepper(x, y, font, GameText.resolve(SettingsTexts.PERCENT.with(v)),
                () -> set("volume", Integer.toString(Math.max(0, v - 10))),
                () -> set("volume", Integer.toString(Math.min(100, v + 10))));
        y += 17;
        pagePanel.add(new ProgressBar(() -> v)).setBounds(x, y, Math.min(w, 150), 5);
        y += 10;
        caption(SettingsTexts.MUTE, x, y, w);
        y += 10;
        toggleButtons(x, y, font, SettingsTexts.ON, SettingsTexts.OFF, s.muted(),
                () -> set("mute", "on"), () -> set("mute", "off"));
        y += 18;
        caption(SettingsTexts.OUTPUT, x, y, w);
        y += 10;
        final SoundOutput chosen = SoundOutput.byId(s.output());
        int bx = x;
        for (final SoundOutput output : SoundOutput.values()) {
            final String label = GameText.resolve(switch (output) {
                case MONITOR -> SettingsTexts.OUTPUT_MONITOR;
                case SPEAKERS -> SettingsTexts.OUTPUT_SPEAKERS;
                case BOTH -> SettingsTexts.OUTPUT_BOTH;
            });
            final int bw = font.width(label) + 12;
            pagePanel.add(new Button(label, () -> set("output", output.id())).setPrimary(output == chosen))
                    .setBounds(bx, y, bw, BTN_H);
            bx += bw + 4;
        }
        y += 18;
        caption(SettingsTexts.SOUND_HARDWARE, x, y, w);
        y += 10;
        pagePanel.add(new Label(s.hardware().isEmpty() ? GameText.resolve(SettingsTexts.NO_SOUND_HARDWARE)
                : GameText.resolve(s.hardware()))).setBounds(x, y, w, 8);
        y += 12;
        caption(SettingsTexts.SPEAKERS, x, y, w);
        y += 10;
        if (s.speakers().isEmpty()) {
            pagePanel.add(new Label(GameText.resolve(SettingsTexts.NO_SPEAKERS), Label.Tone.DIM)).setBounds(x, y, w, 8);
            y += 10;
        }
        for (int i = 0; i < s.speakers().size() && i < SPEAKER_ROWS; i++) {
            final SettingsSnapshotPayload.SpeakerRow speaker = s.speakers().get(i);
            final String name = speaker.name().isEmpty() ? GameText.resolve(SettingsTexts.UNNAMED_SPEAKER)
                    : speaker.name();
            final String side = GameText.resolve(switch (speaker.side()) {
                case LEFT -> SettingsTexts.SIDE_LEFT;
                case RIGHT -> SettingsTexts.SIDE_RIGHT;
                case BOTH -> SettingsTexts.SIDE_BOTH;
            });
            pagePanel.add(new Label(name)).setBounds(x, y, w - font.width(side) - 6, 8);
            pagePanel.add(new Label(side, Label.Tone.DIM).setAlign(Label.Align.RIGHT)).setBounds(x, y, w, 8);
            y += 10;
        }
        if (s.speakers().size() > SPEAKER_ROWS) {
            caption(SettingsTexts.MORE_SPEAKERS.with(s.speakers().size() - SPEAKER_ROWS), x, y, w);
            y += 10;
        }
        y += 3;
        final String test = GameText.resolve(SettingsTexts.TEST);
        testButton = pagePanel.add(new Button(test, () -> PacketDistributor.sendToServer(new TestSoundPayload(host))));
        testButton.setEnabled(s.plays());
        testButton.setBounds(x, y, font.width(test) + 16, BTN_H);
    }

    private void comingSoon(final int x, final int y, final int w, final int h) {
        pagePanel.add(new Label(GameText.resolve(NAV.get(page))).setAlign(Label.Align.CENTER))
                .setBounds(x, y + h / 2 - 10, w, 8);
        pagePanel.add(new Label(GameText.resolve(SettingsTexts.COMING_SOON), Label.Tone.DIM)
                .setAlign(Label.Align.CENTER)).setBounds(x, y + h / 2 + 2, w, 8);
    }

    // small controls

    private int specRow(final int x, final int y, final int w, final TextKey label, final Text value) {
        pagePanel.add(new Label(GameText.resolve(label), Label.Tone.DIM)).setBounds(x, y, w / 2, 8);
        pagePanel.add(new Label(GameText.resolve(value)).setAlign(Label.Align.RIGHT)).setBounds(x + w / 2, y,
                w - w / 2, 8);
        return y + 11;
    }

    /** Two buttons of which one is lit: the setting's two states. */
    private void toggleButtons(final int x, final int y, final Font font, final TextKey aKey, final TextKey bKey,
                               final boolean aOn, final Runnable onA, final Runnable onB) {
        toggleButtons(pagePanel, x, y, font, aKey, bKey, aOn, onA, onB);
    }

    /** @return the second button, for the rare caller a client test needs to find afterwards */
    private Button toggleButtons(final Panel target, final int x, final int y, final Font font, final TextKey aKey,
                                 final TextKey bKey, final boolean aOn, final Runnable onA, final Runnable onB) {
        final String a = GameText.resolve(aKey);
        final String b = GameText.resolve(bKey);
        final int aw = font.width(a) + 12;
        final int bw = font.width(b) + 12;
        target.add(new Button(a, onA).setPrimary(aOn)).setBounds(x, y, aw, BTN_H);
        final Button bButton = target.add(new Button(b, onB).setPrimary(!aOn));
        bButton.setBounds(x + aw + 4, y, bw, BTN_H);
        return bButton;
    }

    /** A value between a minus and a plus button. */
    private void stepper(final int x, final int y, final Font font, final String value, final Runnable dec, final Runnable inc) {
        pagePanel.add(new Button("-", dec)).setBounds(x, y, 15, BTN_H);
        final int valueW = Math.max(38, font.width(value) + 6);
        pagePanel.add(new Label(value)).setBounds(x + 21, y + 3, valueW, 8);
        pagePanel.add(new Button("+", inc)).setBounds(x + 21 + valueW, y, 15, BTN_H);
    }

    private static String group(final long n) {
        return String.format(Locale.ROOT, "%,d", n);
    }

    // input

    @Override
    public void mouseClicked(final DesktopWindow window, final double mouseX, final double mouseY, final int button) {
        root.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void mouseReleased(final DesktopWindow window, final double mouseX, final double mouseY, final int button) {
        root.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(final double delta) {
        // Only the Personalize page ever nests a panel that scrolls; every other page ignores the wheel.
        return root.mouseScrolled(lastMouseX, lastMouseY, delta);
    }

    @Override
    public boolean charTyped(final char c) {
        return root.charTyped(c);
    }

    @Override
    public boolean keyPressed(final int key, final int scanCode, final int modifiers) {
        if (key == GLFW.GLFW_KEY_ESCAPE && nameField != null && nameField.isFocused()) {
            // Escape drops the edit through the field's own handling and keeps the window open.
            return root.keyPressed(key, scanCode, modifiers);
        }
        return root.keyPressed(key, scanCode, modifiers);
    }
}
