/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import static dev.jstech.computers.client.FirmwareScreenTexts.CORE_COUNT_MANY;
import static dev.jstech.computers.client.FirmwareScreenTexts.CORE_COUNT_ONE;
import static dev.jstech.computers.client.FirmwareScreenTexts.DISK;
import static dev.jstech.computers.client.FirmwareScreenTexts.DISK_BOOT_FAILURE;
import static dev.jstech.computers.client.FirmwareScreenTexts.DISK_NAMED;
import static dev.jstech.computers.client.FirmwareScreenTexts.ENTERING_SETUP_NOW;
import static dev.jstech.computers.client.FirmwareScreenTexts.ENTRY;
import static dev.jstech.computers.client.FirmwareScreenTexts.INSERT_MEDIA;
import static dev.jstech.computers.client.FirmwareScreenTexts.KEY_BOOT_MENU;
import static dev.jstech.computers.client.FirmwareScreenTexts.KEY_SETUP;
import static dev.jstech.computers.client.FirmwareScreenTexts.NON_SYSTEM_DISK;
import static dev.jstech.computers.client.FirmwareScreenTexts.NOTHING_ATTACHED;
import static dev.jstech.computers.client.FirmwareScreenTexts.NO_BOOTABLE;
import static dev.jstech.computers.client.FirmwareScreenTexts.NO_BOOTABLE_FOUND;
import static dev.jstech.computers.client.FirmwareScreenTexts.PRESS_ANY_KEY_SETUP;
import static dev.jstech.computers.client.FirmwareScreenTexts.REPAIR;
import static dev.jstech.computers.client.FirmwareScreenTexts.REPLACE_DISK;
import static dev.jstech.computers.client.FirmwareScreenTexts.SUMMARY;
import static dev.jstech.computers.client.FirmwareScreenTexts.THE_MEDIUM;
import static dev.jstech.computers.client.FirmwareScreenTexts.of;

import com.mojang.blaze3d.platform.InputConstants;
import dev.jstech.computers.JsComputers;
import dev.jstech.computers.gui.MonitorGlass;
import dev.jstech.computers.operation.payload.FirmwareActionPayload;
import dev.jstech.computers.operation.payload.FirmwareStatePayload;
import dev.jstech.computers.operation.payload.PostCompletePayload;
import dev.jstech.computers.operation.payload.RequestFirmwareStatePayload;
import dev.jstech.computers.menu.MonitorSessionMenu;
import dev.jstech.computers.os.FirmwareKind;
import dev.jstech.core.gui.Phosphor;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.text.Text;
import dev.jstech.core.tier.HardwareEra;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The power-on self-test: what the monitor shows between switching the computer on (or a reboot) and the
 * boot target opening.
 *
 * <p>This holds the machine's place in its own self-test and the keys a player can press during one; what is
 * drawn belongs elsewhere. The wall of text the first two ages printed is {@link PostWall}, the one-time boot
 * menu in each firmware's own shape is {@link FirmwareBootMenus}, and the modern machines' picture is here
 * because it is a picture and not a page.
 *
 * <p>DEL at any point enters the firmware setup instead. When the sequence ends the client reports
 * {@link PostCompletePayload} and the server swaps this screen for whatever boots.
 */
@PaletteHolder
public final class BootSequenceScreen extends AbstractComputerScreen<MonitorSessionMenu> {

    private static final int W = MonitorGlass.WIDTH;
    private static final int H = MonitorGlass.HEIGHT;

    /** How long a self-test is drawn for when the machine did not say, which only a stale packet leaves. */
    private static final int FALLBACK_TICKS = 70;

    /*
     * The self-test in each firmware's look: {@code jsc:firmware/post_cli} (written in greys the Vintage tube's
     * phosphor lights), {@code jsc:firmware/post_bios} and {@code jsc:firmware/post_uefi}. A Legacy machine posts on
     * black with the maker's badge, the way the boards of that time did; only its setup was ever blue.
     */
    private static final Palette<Post> POST_CLI = Palettes.declare(JsComputers.MODID, "firmware/post_cli",
            new Post(0xFF000000, 0xFFB8B8B8, 0xFF707070, 0xFFE8E8E8, 0xFFA8B2C6, 0xFF2A2D3E, 0xFF2A2D3E,
                    0xFF3A4060, 0xFFF0B23A));
    private static final Palette<Post> POST_BIOS = Palettes.declare(JsComputers.MODID, "firmware/post_bios",
            new Post(0xFF000000, 0xFFBDBDBD, 0xFF8A8A8A, 0xFFFFFFFF, 0xFFA8B2C6, 0xFF2A2D3E, 0xFF2A2D3E,
                    0xFF3A4060, 0xFFF0B23A));
    private static final Palette<Post> POST_UEFI = Palettes.declare(JsComputers.MODID, "firmware/post_uefi",
            new Post(0xFF10121C, 0xFFE6ECF6, 0xFF8090A8, 0xFF5FA8D3, 0xFFA8B2C6, 0xFF2A2D3E, 0xFF2A2D3E,
                    0xFF3A4060, 0xFFF0B23A));

    /** The self-test the machine last reported, kept until the session that shows it is built. */
    @Nullable
    private static Testing pending;

    private final BlockPos computerPos;
    private final BlockPos monitorPos;
    private final FirmwareKind kind;
    private final String machineName;

    /** What the machine found wrong with the system on its disk, in that system's words; empty when nothing. */
    private final String complaint;
    /** What the machine said was left of its self-test when this screen opened. */
    private final int postTicks;

    /** Whether the machine is standing at the end of a self-test that refused to go on. */
    private final boolean halted;

    private static BootSequenceScreen active;

    private FirmwareStatePayload state;
    private int ticks;
    private boolean completed;
    private boolean setupRequested;
    /** Whether F12 opened the one-time boot menu over the self-test. */
    private boolean bootMenu;
    /** Which entry the menu is on. */
    private int menuAt;

    public BootSequenceScreen(final MonitorSessionMenu session, final Inventory inventory,
                              final Component title) {
        super(session, inventory, title);
        this.imageWidth = W;
        this.imageHeight = H;
        this.titleLabelX = OFF_SCREEN;
        this.inventoryLabelY = OFF_SCREEN;
        this.computerPos = session.hostPos();
        this.monitorPos = session.monitorPos();
        final Testing testing = pending != null ? pending
                : new Testing(FirmwareKind.forEra(HardwareEra.STANDARD), "", FALLBACK_TICKS, false, "");
        this.kind = testing.kind();
        this.machineName = testing.machineName();
        this.complaint = testing.complaint();
        this.halted = testing.halted();
        this.postTicks = testing.remainingTicks() > 0 ? testing.remainingTicks() : FALLBACK_TICKS;
        /*
         * A machine standing at a self-test that found nothing to boot opens at the end of that test rather
         * than playing one through: the test is over, and what is on the glass is the failure it ended on.
         */
        if (testing.halted()) {
            this.ticks = this.postTicks;
            this.completed = true;
        }
    }

    /** The self-test the machine is in, said before the session that shows it is opened. */
    public static void expect(final FirmwareKind kind, final String machineName, final int remainingTicks,
                              final boolean halted, final String complaint) {
        pending = new Testing(kind, machineName, remainingTicks, halted, complaint);
    }

    /**
     * One machine testing itself: which firmware it wears, what it is called, where it has got to, and what
     * it found wrong with the system on its disk, if the disk had one at all.
     */
    private record Testing(FirmwareKind kind, String machineName, int remainingTicks, boolean halted,
                           String complaint) {
    }

    /** The machine's own generation, so the bezel is the monitor that machine would really have. */
    @Override
    @Nullable
    protected HardwareEra screenEra() {
        return this.getMenu().hardwareEra();
    }

    /** The same, never null, for the wording a firmware of that age would really have put up. */
    private HardwareEra bezelEra() {
        final HardwareEra era = this.screenEra();
        return era == null ? HardwareEra.STANDARD : era;
    }

    @Override
    protected void init() {
        super.init();
        active = this;
        PacketDistributor.sendToServer(new RequestFirmwareStatePayload(computerPos));
    }

    @Override
    public void removed() {
        if (active == this) {
            active = null;
        }
        super.removed();
    }

    /** Routes the server's hardware state to the running POST, which lists it as detected devices. */
    public static void accept(final FirmwareStatePayload payload) {
        if (active != null && active.computerPos.equals(payload.hostPos())) {
            active.state = payload;
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        ticks++;
        /*
         * The machine is the one that ends its self-test and puts whoever is watching in front of what boots,
         * so this waits rather than reporting. All the screen owes is a way out if nothing ever comes: a
         * machine switched off mid-test leaves this here with nothing else to show.
         */
        if (!completed && ticks >= postTicks) {
            completed = true;
        }
        /*
         * This never sees itself out. It used to, as a way out if the machine never swapped the screen, and that
         * became the reason the machine could not: the session a player holds is how the machine knows who is
         * watching, so closing it is leaving, and leaving guarantees that nothing comes. A self-test ends by the
         * machine handing over, a failed one waits for a key, and a machine switched off mid-test is left on the
         * glass until somebody presses Escape, which is the way out of every other screen here.
         */
    }

    @Override
    public boolean keyPressed(final int keyCode, final int scanCode, final int modifiers) {
        if (bootMenu) {
            return bootMenuKey(keyCode);
        }
        if (keyCode == InputConstants.KEY_F12 && !setupRequested && !bootable().isEmpty()) {
            bootMenu = true;
            menuAt = 0;
            return true;
        }
        /*
         * A self-test that ended with nothing to boot waits here, as a machine does. Any key asks the machine
         * to go on, which with nothing on any disk means its setup.
         */
        if (completed && !setupRequested && bootingFrom().isEmpty()) {
            setupRequested = true;
            PacketDistributor.sendToServer(new PostCompletePayload(computerPos, monitorPos, false));
            return true;
        }
        if (keyCode == InputConstants.KEY_DELETE && !completed && !setupRequested) {
            setupRequested = true;
            PacketDistributor.sendToServer(new PostCompletePayload(computerPos, monitorPos, true));
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // Render

    @Override
    protected void renderBg(final GuiGraphics g, final float partialTick, final int mouseX, final int mouseY) {
        final int x = this.leftPos;
        final int y = this.topPos;
        MonitorFrame.renderBody(g, x, y, W, H, screenEra(), font);

        /*
         * A Legacy machine posts on black with the maker's badge, the way the boards of that time did; the
         * setup it opens with DEL is the blue one. The two are different screens and only setup was ever blue.
         */
        final Post look = post();
        final int bg = look.ground();
        /*
         * The Vintage machine posts on a green-phosphor tube, which has exactly one colour: its text is
         * that green, brighter or dimmer, never the grey-white of a later monitor. Its palette is written in
         * greys, and the phosphor lights them.
         */
        final boolean tube = kind == FirmwareKind.CLI_BIOS;
        final int text = tube ? Phosphor.green(look.text()) : look.text();
        final int dim = tube ? Phosphor.green(look.dim()) : look.dim();
        final int accent = tube ? Phosphor.green(look.accent()) : look.accent();
        g.fill(x, y, x + W, y + H, bg);

        if (kind == FirmwareKind.UEFI) {
            renderUefi(g, x, y, text, dim, accent);
        } else {
            PostWall.draw(g, font, kind, bezelEra(), state, machineName, bootingFrom(), noBootLines(),
                    x, y, W, ticks, completed, text, dim, accent);
            PostWall.hint(g, font, kind, x, y, H, ticks, setupRequested, dim, accent);
        }
        if (kind == FirmwareKind.BLUE_BIOS) {
            /*
             * The badge the boards of that age wore in the corner of their self-test: a framed block, printed
             * on the board and put on the glass, rather than the house's name set in the machine's own font.
             */
            SplashLogos.badge(g, x + W - PostWall.MARGIN, y + 8);
        }
        if (bootMenu) {
            FirmwareBootMenus.draw(g, font, kind, bootable(), menuAt, x, y, W, H, text, dim, accent);
        }
    }

    /** Everything this machine could boot right now, in the order the firmware found it. */
    private List<FirmwareBootMenus.Choice> bootable() {
        if (state == null) {
            return List.of();
        }
        final List<FirmwareBootMenus.Choice> out = new ArrayList<>();
        int slot = 0;
        for (final FirmwareStatePayload.Entry entry : state.entries()) {
            final int here = entry.kind() == FirmwareStatePayload.KIND_DISK ? slot++ : -1;
            if (entry.bootable()) {
                out.add(new FirmwareBootMenus.Choice(entry, here));
            }
        }
        return out;
    }

    /** Up and down walk the menu, Enter boots what is on, and anything else puts the menu away. */
    private boolean bootMenuKey(final int keyCode) {
        final List<FirmwareBootMenus.Choice> choices = bootable();
        if (choices.isEmpty()) {
            bootMenu = false;
            return true;
        }
        switch (keyCode) {
            case InputConstants.KEY_UP -> menuAt = (menuAt - 1 + choices.size()) % choices.size();
            case InputConstants.KEY_DOWN -> menuAt = (menuAt + 1) % choices.size();
            case InputConstants.KEY_RETURN, InputConstants.KEY_NUMPADENTER -> {
                final FirmwareBootMenus.Choice chosen = choices.get(Math.min(menuAt, choices.size() - 1));
                bootMenu = false;
                setupRequested = true;
                /*
                 * A disk boots this once and the saved order stays where it is; a medium boots the way it does
                 * from the setup, since booting one is already a thing that happens once.
                 */
                PacketDistributor.sendToServer(chosen.slot() >= 0
                        ? new FirmwareActionPayload(computerPos, monitorPos,
                                FirmwareActionPayload.ACTION_BOOT_ONCE, chosen.slot(), -1,
                                chosen.entry().osId())
                        : FirmwareActionPayload.of(computerPos, monitorPos,
                                FirmwareActionPayload.ACTION_BOOT_MEDIA, chosen.entry().ref(), -1));
            }
            default -> bootMenu = false;
        }
        return true;
    }

    /**
     * What the machine is about to boot, worded as the firmware would say it ("Disk 0: Frames XP"), or nothing
     * when no disk carries a system and no medium in a drive can boot.
     */
    private String bootingFrom() {
        /*
         * A machine standing at a failed self-test is booting from nothing, whatever the firmware made of the
         * drives. The disk it would reach for is the very disk it has just refused: a system whose loader is
         * gone is still a system as far as the firmware's list is concerned, so the list says bootable and
         * the machine says no. Saying "booting from" it here is the machine contradicting itself, and what a
         * player saw was a self-test that announced a boot and then sat there for ever.
         */
        if (this.halted) {
            return "";
        }
        if (state == null) {
            return "";
        }
        FirmwareStatePayload.Entry chosen = null;
        int slot = 0;
        int chosenSlot = 0;
        for (final FirmwareStatePayload.Entry entry : state.entries()) {
            final int here = entry.kind() == FirmwareStatePayload.KIND_DISK ? slot++ : -1;
            if (!entry.bootable()) {
                continue;
            }
            final boolean preferred = here >= 0 && here == state.bootSlot();
            if (chosen == null || preferred) {
                chosen = entry;
                chosenSlot = here;
            }
            if (preferred) {
                break;
            }
        }
        if (chosen == null) {
            return "";
        }
        return of(ENTRY.with(chosenSlot >= 0 ? DISK.with(chosenSlot) : THE_MEDIUM.text(), chosen.label()));
    }

    /**
     * How this machine says it will not start.
     *
     * <p>A disk with a system on it whose loader has been deleted is not a disk with nothing on it: the
     * firmware found a system and handed over, and what failed was the system. So the system's own words are
     * what goes on the glass, with the way back underneath, and the firmware only speaks when there was
     * really nothing to find.
     */
    private List<String> noBootLines() {
        if (!this.complaint.isEmpty()) {
            return List.of(this.complaint, of(REPAIR));
        }
        return switch (kind) {
            case CLI_BIOS -> List.of(of(NON_SYSTEM_DISK), of(REPLACE_DISK));
            case BLUE_BIOS -> List.of(of(DISK_BOOT_FAILURE));
            case UEFI -> List.of(of(NO_BOOTABLE_FOUND), of(PRESS_ANY_KEY_SETUP));
        };
    }

    /** What the machine is called: the name its owner gave it, else the kind of machine it is. */
    private String machineTitle() {
        final String named = state == null ? "" : of(state.machine().name());
        return named.isEmpty() ? machineName : named;
    }

    /** The machine on one line, the way a modern firmware sums a computer up while it comes awake. */
    private String buildSummary() {
        if (state == null || !state.machine().hasCpu()) {
            return "";
        }
        final FirmwareStatePayload.Machine m = state.machine();
        final String memory = m.ramMb() >= 1024 ? m.ramMb() / 1024 + " GB" : m.ramMb() + " MB";
        return of(SUMMARY.with(m.cpuName(), (m.cores() == 1 ? CORE_COUNT_ONE : CORE_COUNT_MANY).with(m.cores()),
                memory, m.cpuArch()));
    }

    /**
     * The modern machines hide the wall of text: the maker's mark, the machine's own name, a bar under it,
     * and along the foot what it is made of on one side and the two keys on the other.
     */
    private void renderUefi(final GuiGraphics g, final int x, final int y, final int text, final int dim,
                            final int accent) {
        /*
         * The mark is a picture rather than the house's name set in the game's font: it is a lockup with its
         * own lettering, and writing the words out in one size was the thing that made this screen read as a
         * placeholder. No screen inside the fiction names the mod, and the machine in front of the player is
         * the one it names.
         */
        final int cx = x + W / 2;
        SplashLogos.draw(g, SplashLogos.JSC, cx, y + H / 2 - 46);
        wallCentered(g, machineTitle(), cx, y + H / 2 + 8, post().subtitle());
        final int barW = 120;
        final int bx = cx - barW / 2;
        final int by = y + H / 2 + 24;
        g.fill(bx, by, bx + barW, by + 3, post().track());
        final int fill = Math.min(barW, barW * ticks / postTicks);
        g.fill(bx, by, bx + fill, by + 3, accent);
        if (completed && bootingFrom().isEmpty()) {
            renderUefiNoBoot(g, x, y, text, dim);
            return;
        }
        /*
         * Along the foot: what the machine is made of where a board of this age prints it, and the two keys
         * at the other end, blinking as they do until one of them is pressed.
         */
        final int fy = y + H - 16;
        wall(g, buildSummary(), x + PostWall.MARGIN, fy, dim);
        if (setupRequested) {
            wallRight(g, of(ENTERING_SETUP_NOW), x + W - PostWall.MARGIN, fy, accent);
        } else if ((ticks / 10) % 2 == 0) {
            final String setup = of(KEY_SETUP);
            final String bootMenu = of(KEY_BOOT_MENU);
            int tx = x + W - PostWall.MARGIN - wallWidth("DEL" + setup + "F12" + bootMenu);
            tx = PostWall.run(g, font, "DEL", tx, fy, text);
            tx = PostWall.run(g, font, setup, tx, fy, dim);
            tx = PostWall.run(g, font, "F12", tx, fy, text);
            PostWall.run(g, font, bootMenu, tx, fy, dim);
        }
    }

    /**
     * A modern machine with nothing to boot says so in a dialog that names every device and what is on it.
     *
     * <p>Two lines saying no device was found tell a player that something is wrong and nothing about what:
     * the disks are there, they are simply empty, and the one thing worth knowing is which of them is.
     */
    private void renderUefiNoBoot(final GuiGraphics g, final int x, final int y, final int text, final int dim) {
        final List<FirmwareStatePayload.Entry> entries = state == null ? List.of() : state.entries();
        final int listed = Math.min(entries.size(), PostWall.MOST_DRIVES);
        final int boxW = 224;
        final int boxH = 16 + Math.max(1, listed) * WALL_ROW + 22;
        final int bx = x + (W - boxW) / 2;
        final int by = y + (H - boxH) / 2;
        final Post look = post();
        g.fill(bx, by, bx + boxW, by + boxH, look.box());
        g.fill(bx, by, bx + boxW, by + 14, look.boxHead());
        g.fill(bx, by, bx + 2, by + 14, look.alarm());
        /*
         * What is wrong, when the machine knows: "no bootable device" is true of an empty computer and a lie
         * about a wrecked one, whose device is right there and whose system will not start. A player who
         * deleted a file needs to be told which file, not that their disk has gone.
         */
        wall(g, this.complaint.isEmpty() ? of(NO_BOOTABLE) : this.complaint,
                bx + 7, by + 4, look.alarm());
        int ly = by + 18;
        if (entries.isEmpty()) {
            wall(g, of(NOTHING_ATTACHED), bx + 8, ly, dim);
            ly += WALL_ROW;
        }
        int slot = 0;
        for (int i = 0; i < listed; i++) {
            final FirmwareStatePayload.Entry entry = entries.get(i);
            final Text where = entry.kind() == FirmwareStatePayload.KIND_DISK
                    ? DISK_NAMED.with(slot++, entry.device()) : entry.device();
            wall(g, wallClip(of(ENTRY.with(where, entry.label())), boxW - 16), bx + 8, ly, text);
            ly += WALL_ROW;
        }
        wall(g, of(INSERT_MEDIA), bx + 8, ly + 4, dim);
    }

    /* This machine's self-test colours, by the firmware it posts with. */
    private Post post() {
        return switch (kind) {
            case CLI_BIOS -> POST_CLI.get();
            case BLUE_BIOS -> POST_BIOS.get();
            case UEFI -> POST_UEFI.get();
        };
    }

    /**
     * A self-test's colours: the ground, the text, the quiet lines and the accent (the tube's are greys its phosphor
     * lights), the machine's name under the mark and the bar's track, and the box that says nothing will boot
     * with its head and the alarm along it.
     */
    private record Post(int ground, int text, int dim, int accent, int subtitle, int track, int box, int boxHead,
                        int alarm) {
    }
}
