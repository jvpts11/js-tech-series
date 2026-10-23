/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui.layout;

import dev.jstech.computers.hardware.CaseBays;
import dev.jstech.core.gui.layout.GuiLayout;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Where everything stands on an assembly screen, which is drawn as the machine: on the left the case seen from its open
 * side, with the board on its tray and every part's seat where that part sits on a real board (the back panel with the
 * supply and the cards' brackets, the socket at the top, the memory beside it, the cards stacked under it, the bays and
 * the disk cage at the front); on the right the machine's front panel, its power button over its readouts; under them
 * the inventory, beside it the service switches and the lamps.
 *
 * <p>The three towers (Personal, Crafting and Cluster Management Computers) share one case, whose bays and supply stand
 * where their era's case has them. The Mainframe and the rack server carry four sockets, eight banks and six cards, which
 * no tower board does: their cards stand in two I/O cages of three risers. The server has no front, its drives being the
 * rack's, but a wall of fans.
 *
 * <p>Pure: the screens draw from it and the menus seat their slots from it, so the two cannot disagree, and the tests
 * check it without the game.
 */
public final class AssemblyGeometry {

    /** Which case the screen draws. */
    public enum Kind {

        /** The three towers' case. */
        TOWER,

        /** The Mainframe's cabinet. */
        MAINFRAME,

        /** A rack server's chassis. */
        SERVER
    }

    /** What a seat takes. */
    public enum SeatKind {
        BOARD, CPU, RAM, CARD, PSU, BAY, DISK
    }

    /**
     * One seat: a part's slot where the part sits.
     *
     * @param kind  what it takes
     * @param index which of its kind it is, from 0
     * @param x     its left, window-relative
     * @param y     its top, window-relative
     */
    public record Seat(SeatKind kind, int index, int x, int y) {
    }

    /** A box on the screen, window-relative. */
    public record Box(int x, int y, int w, int h) {
    }

    private final Kind kind;
    private final int bays;
    private final boolean supplyAtTop;
    private final int readouts;
    private final boolean operations;
    private final int switches;
    private final int width;
    private final int height;
    private final int trayRight;
    private final int dimmX;
    private final int rightX;
    private final int frontX;
    private final int caseRight;
    private final int caseBottom;
    private final int inventoryY;
    private final List<Seat> seats;

    /** Between two seats, a slot and the gap after it. */
    public static final int PITCH = 20;
    /** The top row of seats. */
    public static final int TOP = 32;
    /** The back panel's seats: the supply. */
    public static final int REAR_X = 10;
    /** The board's own seat and the cards' column. */
    public static final int BOARD_X = 32;
    /** The first processor's seat. */
    public static final int CPU_X = 54;
    /** Where the tray, and the board on it, begins. */
    public static final int TRAY_LEFT = 30;
    /** The case's rectangle: its left and top. */
    public static final int CASE_LEFT = 8;
    public static final int CASE_TOP = 30;
    /** The rail of captions over the case. */
    public static final int RAIL_Y = 23;
    /** The header strip: its left, top and the inset of its title. */
    public static final int HEADER_X = 6;
    public static final int HEADER_Y = 6;
    public static final int HEADER_H = 16;
    public static final int TITLE_X = 10;
    public static final int TITLE_Y = 10;
    /** The name well on the header, its top and bottom. */
    public static final int WELL_TOP = 7;
    public static final int WELL_BOTTOM = 21;
    /** The big machines' row of risers. */
    public static final int CAGE_Y = TOP + 2 * PITCH + 10;
    /** The risers of the two cages, three to a cage. */
    public static final int[] CAGE_X = {36, 56, 76, 110, 130, 150};
    /** The front panel's width, its power button's height and a readout's. */
    public static final int RIGHT_W = 72;
    public static final int POWER_Y = 26;
    public static final int POWER_H = 16;
    public static final int READOUT_H = 18;
    public static final int READOUT_PITCH = 20;
    /** The operations strip under the Mainframe's case. */
    public static final int OPERATIONS_H = 18;
    /** The service panel beside the inventory. */
    public static final int SERVICE_X = 176;
    public static final int SWITCH_H = 13;
    public static final int LAMPS_H = 20;
    /** The inventory's left. */
    public static final int INVENTORY_X = 8;
    /** Where a card's picture lies along its slot, from the seat: right of it, its bottom the slot's. */
    public static final int CARD_DX = 18;
    public static final int CARD_DY = -3;
    public static final int CARD_W = 52;
    public static final int CARD_H = 20;
    /** A card's bracket on the back panel. */
    public static final int BRACKET_X = 10;
    public static final int BRACKET_W = 18;
    public static final int BRACKET_H = 13;

    private static final int TOWER_W = 268;
    private static final int BIG_W = 292;
    private static final int TOWER_TRAY_RIGHT = 159;
    private static final int BIG_TRAY_RIGHT = 183;
    private static final int TOWER_DIMM_X = 78;
    private static final int BIG_DIMM_X = 100;
    private static final int TOWER_RIGHT_X = 188;
    private static final int BIG_RIGHT_X = 212;
    private static final int TOWER_RAM = 4;
    private static final int TOWER_CARDS = 4;
    private static final int TOWER_DISKS = 2;
    private static final int BIG_CPUS = 4;
    private static final int BIG_RAM = 8;
    private static final int BIG_CARDS = 6;
    private static final int MAINFRAME_DISKS = 4;
    /** The rack server's wall of fans, five high. */
    private static final int SERVER_FANS = 5;

    /**
     * @param kind        which case
     * @param bays        its 5.25-inch bays (the towers only)
     * @param supplyAtTop whether its supply sits at the top of the back panel, as in the old towers
     * @param readouts    how many readouts its front panel shows
     */
    public AssemblyGeometry(final Kind kind, final int bays, final boolean supplyAtTop, final int readouts) {
        this.kind = kind;
        this.bays = kind == Kind.TOWER ? bays : 0;
        this.supplyAtTop = kind == Kind.TOWER && supplyAtTop;
        this.readouts = readouts;
        this.operations = kind == Kind.MAINFRAME;
        // A server has no switches of its own: it is switched on and off in its rack.
        this.switches = kind == Kind.SERVER ? 0 : 2;
        final boolean tower = kind == Kind.TOWER;
        this.width = tower ? TOWER_W : BIG_W;
        this.trayRight = tower ? TOWER_TRAY_RIGHT : BIG_TRAY_RIGHT;
        this.dimmX = tower ? TOWER_DIMM_X : BIG_DIMM_X;
        this.rightX = tower ? TOWER_RIGHT_X : BIG_RIGHT_X;
        this.frontX = trayRight + 2;
        this.caseRight = frontX + 21;
        this.seats = Collections.unmodifiableList(seatMap());
        int bottom = 0;
        for (final Seat seat : seats) {
            bottom = Math.max(bottom, seat.y() + (seat.kind() == SeatKind.CARD && !tower ? 22 : 18));
        }
        if (kind == Kind.SERVER) {
            bottom = Math.max(bottom, TOP + (SERVER_FANS - 1) * PITCH + 18);
        }
        this.caseBottom = bottom + 8;
        final int rightBottom = POWER_Y + POWER_H + 4 + readouts * READOUT_PITCH;
        this.inventoryY = Math.max(caseBottom + (operations ? OPERATIONS_H + 2 : 0) + 6, rightBottom + 6);
        this.height = inventoryY + 84;
    }

    /**
     * A tower's geometry for its era's case: its bays, and its supply at the top of the back panel in the towers up to
     * the Transition's, at the foot in the newer ones.
     */
    public static AssemblyGeometry tower(final HardwareEra era, final int readouts) {
        return new AssemblyGeometry(Kind.TOWER, CaseBays.of(era), era.isAtMost(HardwareEra.TRANSITION), readouts);
    }

    /** The Mainframe's geometry. */
    public static AssemblyGeometry mainframe(final int readouts) {
        return new AssemblyGeometry(Kind.MAINFRAME, 0, false, readouts);
    }

    /** A rack server's geometry. */
    public static AssemblyGeometry server(final int readouts) {
        return new AssemblyGeometry(Kind.SERVER, 0, false, readouts);
    }

    public Kind kind() {
        return kind;
    }

    /** The screen's width and height. */
    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    /** The case's 5.25-inch bays. */
    public int bays() {
        return bays;
    }

    /** Every seat, in the order the machine's slots run. */
    public List<Seat> seats() {
        return seats;
    }

    /** The seats of one kind, in order. */
    public List<Seat> seats(final SeatKind of) {
        final List<Seat> out = new ArrayList<>();
        for (final Seat seat : seats) {
            if (seat.kind() == of) {
                out.add(seat);
            }
        }
        return out;
    }

    /** The one seat of that kind (the board's or the supply's). */
    public Seat seat(final SeatKind of) {
        return seats(of).getFirst();
    }

    /** The case's rectangle, which its pictures cover. */
    public Box caseBox() {
        return new Box(CASE_LEFT, CASE_TOP, caseRight - CASE_LEFT, caseBottom - CASE_TOP);
    }

    /** Where the tray ends and the front strip begins. */
    public int trayRight() {
        return trayRight;
    }

    /** The front strip's seats' left. */
    public int frontX() {
        return frontX;
    }

    /** Where the memory's caption goes on the rail. */
    public int dimmX() {
        return dimmX;
    }

    /** Whether the case has a front with drives: every machine but the rack server. */
    public boolean hasFront() {
        return kind != Kind.SERVER;
    }

    /** Where the board's bus is printed: between the first two slots of a tower, over the cages otherwise. */
    public int busX() {
        return kind == Kind.TOWER ? BOARD_X + 20 : CAGE_X[0];
    }

    public int busY() {
        return kind == Kind.TOWER ? seats(SeatKind.CARD).get(1).y() + 17 : CAGE_Y - 8;
    }

    /** Where the disk cage's caption goes, under the bays: only on a tower that has bays. */
    public int diskCaptionY() {
        return TOP + bays * PITCH + 2;
    }

    /** The header strip. */
    public Box header() {
        return new Box(HEADER_X, HEADER_Y, width - 2 * HEADER_X, HEADER_H);
    }

    /** Where the name well ends, short of the front panel's column. */
    public int wellRight() {
        return rightX - 6;
    }

    /** The front panel's column. */
    public int rightX() {
        return rightX;
    }

    /** The power button. */
    public Box power() {
        return new Box(rightX, POWER_Y, RIGHT_W, POWER_H);
    }

    /** The readouts under the power button. */
    public Box readout(final int index) {
        return new Box(rightX, POWER_Y + POWER_H + 4 + index * READOUT_PITCH, RIGHT_W, READOUT_H);
    }

    public int readouts() {
        return readouts;
    }

    /** The Mainframe's operations strip under its case, or null on a machine that has none. */
    public Box operations() {
        return operations ? new Box(CASE_LEFT, caseBottom + 2, caseRight - CASE_LEFT, OPERATIONS_H) : null;
    }

    /** The inventory's top. */
    public int inventoryY() {
        return inventoryY;
    }

    /** How many service switches the machine has: auto start, and the side panel or the failover; none on a server. */
    public int switches() {
        return switches;
    }

    /** A service switch beside the inventory. */
    public Box serviceSwitch(final int index) {
        return new Box(SERVICE_X, inventoryY - 1 + index * 16, width - SERVICE_X - 8, SWITCH_H);
    }

    /** The lamps' panel under the switches, or in their place on a machine with none. */
    public Box lamps() {
        final int top = switches == 0 ? inventoryY - 1 : inventoryY - 1 + switches * 16 + 3;
        return new Box(SERVICE_X, top, width - SERVICE_X - 8, LAMPS_H);
    }

    /** Whether a card's seat shares its row of the back panel with the supply, which hides that row's bracket. */
    public boolean bracketHidden(final Seat card) {
        final Seat supply = seat(SeatKind.PSU);
        return supply.y() <= card.y() && card.y() < supply.y() + 18;
    }

    /**
     * The layout the menu seats its slots from and the tests check: every seat a slot under its menu key ({@code mobo},
     * {@code psu}, {@code cpu} or {@code cpu_<i>}, {@code ram_<i>}, {@code <cardKey>_<i>}, {@code bay_<i>},
     * {@code disk_<i>}), the front panel and the service panel as boxes, the inventory, and the captions.
     *
     * @param cardKey what the cards' slots are called: {@code gpu} or {@code pcie}
     */
    public GuiLayout layout(final String cardKey) {
        final GuiLayout l = new GuiLayout(width, height);
        final int cpus = seats(SeatKind.CPU).size();
        for (final Seat seat : seats) {
            final String key = switch (seat.kind()) {
                case BOARD -> "mobo";
                case PSU -> "psu";
                case CPU -> cpus == 1 ? "cpu" : "cpu_" + seat.index();
                case RAM -> "ram_" + seat.index();
                case CARD -> cardKey + "_" + seat.index();
                case BAY -> "bay_" + seat.index();
                case DISK -> "disk_" + seat.index();
            };
            l.slot(key, seat.x(), seat.y());
        }
        final Box power = power();
        l.box("power", power.x(), power.y(), power.w(), power.h());
        for (int i = 0; i < readouts; i++) {
            final Box r = readout(i);
            l.box("readout_" + i, r.x(), r.y(), r.w(), r.h());
        }
        final Box ops = operations();
        if (ops != null) {
            l.box("operations", ops.x(), ops.y(), ops.w(), ops.h());
        }
        for (int i = 0; i < switches; i++) {
            final Box s = serviceSwitch(i);
            l.box("switch_" + i, s.x(), s.y(), s.w(), s.h());
        }
        final Box lamps = lamps();
        l.box("lamps", lamps.x(), lamps.y(), lamps.w(), lamps.h());
        l.playerInventory(INVENTORY_X, inventoryY);
        l.text("rearCaption", REAR_X, RAIL_Y, 4, 0.75f);
        l.text("cpuCaption", CPU_X, RAIL_Y, 3, 0.75f);
        l.text("dimmCaption", dimmX, RAIL_Y, 4, 0.75f);
        if (hasFront()) {
            l.text("frontCaption", frontX - 2, RAIL_Y, 5, 0.75f);
        }
        return l;
    }

    private List<Seat> seatMap() {
        final List<Seat> out = new ArrayList<>();
        out.add(new Seat(SeatKind.BOARD, 0, BOARD_X, TOP));
        if (kind == Kind.TOWER) {
            out.add(new Seat(SeatKind.CPU, 0, CPU_X, TOP));
            for (int i = 0; i < TOWER_RAM; i++) {
                out.add(new Seat(SeatKind.RAM, i, dimmX + i * PITCH, TOP));
            }
            for (int k = 0; k < TOWER_CARDS; k++) {
                out.add(new Seat(SeatKind.CARD, k, BOARD_X, TOP + (k + 1) * PITCH + 4));
            }
            final int lastCard = TOP + TOWER_CARDS * PITCH + 4;
            out.add(new Seat(SeatKind.PSU, 0, REAR_X, supplyAtTop ? TOP : lastCard));
            for (int i = 0; i < bays; i++) {
                out.add(new Seat(SeatKind.BAY, i, frontX, TOP + i * PITCH));
            }
            final int disksTop = TOP + bays * PITCH + (bays > 0 ? 10 : 0);
            for (int i = 0; i < TOWER_DISKS; i++) {
                out.add(new Seat(SeatKind.DISK, i, frontX, disksTop + i * PITCH));
            }
        } else {
            for (int i = 0; i < BIG_CPUS; i++) {
                out.add(new Seat(SeatKind.CPU, i, CPU_X + (i % 2) * PITCH, TOP + (i / 2) * PITCH));
            }
            for (int i = 0; i < BIG_RAM; i++) {
                out.add(new Seat(SeatKind.RAM, i, dimmX + (i % 4) * PITCH, TOP + (i / 4) * PITCH));
            }
            for (int k = 0; k < BIG_CARDS; k++) {
                out.add(new Seat(SeatKind.CARD, k, CAGE_X[k], CAGE_Y));
            }
            out.add(new Seat(SeatKind.PSU, 0, REAR_X, CAGE_Y));
            if (kind == Kind.MAINFRAME) {
                for (int i = 0; i < MAINFRAME_DISKS; i++) {
                    out.add(new Seat(SeatKind.DISK, i, frontX, TOP + i * PITCH));
                }
            }
        }
        return out;
    }
}
