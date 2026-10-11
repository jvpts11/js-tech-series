/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.client.assembly.AssemblyPainter;
import dev.jstech.computers.gui.layout.AssemblyGeometry;
import dev.jstech.computers.gui.layout.AssemblyGeometry.Box;
import dev.jstech.computers.gui.layout.AssemblyGeometry.Seat;
import dev.jstech.computers.gui.layout.AssemblyGeometry.SeatKind;
import dev.jstech.core.client.gui.theme.JsTechTheme;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.text.GameText;
import dev.jstech.core.tier.HardwareEra;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;

/**
 * An assembly screen drawn as the machine it assembles: the open case on the left with every part's seat where the part
 * sits, the front panel on the right with the power button over the readouts, the inventory under them and the service
 * switches and lamps beside it. Where everything stands is the {@link AssemblyGeometry}'s, which the menu seats its
 * slots from too; what each machine shows in its readouts, switches and lamps is its own screen's.
 */
@PaletteHolder
public abstract class AbstractMachineAssemblyScreen<T extends AbstractContainerMenu> extends AbstractAssemblyScreen<T> {

    /**
     * One instrument of the front panel.
     *
     * @param label   its label, small, over the value
     * @param value   its value
     * @param colour  the value's colour
     * @param tooltip what it says when the mouse rests on it, empty for nothing
     */
    protected record Readout(String label, String value, int colour, List<Component> tooltip) {

        public Readout(final String label, final String value, final int colour) {
            this(label, value, colour, List.of());
        }
    }

    /**
     * One rocker of the service panel.
     *
     * @param caption its caption
     * @param on      whether it is thrown on
     * @param button  the menu button a click on it presses
     */
    protected record ServiceSwitch(String caption, boolean on, int button) {
    }

    /**
     * The machine's name in its header, which the player edits there.
     *
     * @param hint      what the empty field says
     * @param value     the name now
     * @param maxLength the longest name it takes
     * @param rename    what a change to it sends
     */
    protected record NameField(Component hint, String value, int maxLength, Consumer<String> rename) {
    }

    /**
     * The Mainframe's operations strip, its three counts.
     *
     * @param queued  how many wait
     * @param running how many run
     * @param done    how many are done
     */
    protected record Operations(String queued, String running, String done) {
    }

    private final Map<Seat, Slot> seatSlots = new HashMap<>();
    @Nullable
    private AssemblyGeometry geometry;

    /** The board's silkscreen, white ink on each era's board. */
    private static final Palette<Silk> SILK = Palettes.declare(JsComputers.MODID, "screen/assembly_silk",
            new Silk(0xFFA3AEAB, 0xFFACC6B8, 0xFFA8B3C5, 0xFFA7AAAE, 0xFFA6A9AD));

    protected AbstractMachineAssemblyScreen(final T menu, final Inventory playerInventory, final Component title) {
        super(menu, playerInventory, title);
        this.titleLabelX = OFF_SCREEN;
        this.inventoryLabelY = OFF_SCREEN;
    }

    @Override
    public void render(final GuiGraphics g, final int mouseX, final int mouseY, final float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        final AssemblyGeometry geo = geometry();
        final Box power = geo.power();
        if (hover(mouseX, mouseY, power.x(), power.y(), power.w(), power.h())) {
            tooltip(g, powerTooltip(), mouseX, mouseY);
            return;
        }
        final List<Readout> rows = readouts();
        for (int i = 0; i < rows.size(); i++) {
            final Box r = geo.readout(i);
            if (hover(mouseX, mouseY, r.x(), r.y(), r.w(), r.h())) {
                tooltip(g, rows.get(i).tooltip(), mouseX, mouseY);
                return;
            }
        }
    }

    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        if (clickNameBox(mouseX, mouseY, button)) {
            return true;
        }
        if (button == 0) {
            final AssemblyGeometry geo = geometry();
            final Box power = geo.power();
            if (powerButton() >= 0 && powerActive()
                    && hover((int) mouseX, (int) mouseY, power.x(), power.y(), power.w(), power.h())) {
                sendButton(powerButton());
                return true;
            }
            final List<ServiceSwitch> switches = serviceSwitches();
            for (int i = 0; i < switches.size(); i++) {
                final Box s = geo.serviceSwitch(i);
                if (hover((int) mouseX, (int) mouseY, s.x(), s.y(), s.w(), s.h())) {
                    sendButton(switches.get(i).button());
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /** Where everything stands on this machine's screen, worked out once for the machine's era. */
    public final AssemblyGeometry geometry() {
        if (geometry == null) {
            geometry = makeGeometry();
        }
        return geometry;
    }

    @Override
    protected void init() {
        final AssemblyGeometry geo = geometry();
        this.imageWidth = geo.width();
        this.imageHeight = geo.height();
        super.init();
        seatSlots.clear();
        for (final Seat seat : geo.seats()) {
            for (final Slot slot : menu.slots) {
                if (slot.x == seat.x() && slot.y == seat.y()) {
                    seatSlots.put(seat, slot);
                    break;
                }
            }
        }
        final NameField name = nameField();
        if (name != null) {
            final int left = wellLeft();
            setupNameBox(left + 3, AssemblyGeometry.WELL_TOP + 3, geo.wellRight() - left - 6, name.maxLength(),
                    name.hint(), name.value(), name.rename());
        }
    }

    @Override
    protected void renderBg(final GuiGraphics g, final float partialTick, final int mouseX, final int mouseY) {
        final AssemblyGeometry geo = geometry();
        final int x = leftPos;
        final int y = topPos;
        final HardwareEra era = caseEra();
        JsTechTheme.window(g, x, y, imageWidth, imageHeight);
        drawHeader(g, geo, x, y);
        // The case, the board over its tray, and each card along its slot with its bracket on the back panel.
        final boolean boardIn = boardIn();
        AssemblyPainter.drawCase(g, x, y, geo, era, boardIn);
        if (geo.kind() == AssemblyGeometry.Kind.TOWER) {
            for (final Seat seat : geo.seats(SeatKind.CARD)) {
                final Slot slot = seatSlots.get(seat);
                if (slot == null || !slot.hasItem()) {
                    continue;
                }
                AssemblyPainter.drawCard(g, x, y, seat, slot.getItem());
                if (!geo.bracketHidden(seat)) {
                    AssemblyPainter.drawBracket(g, x, y, seat, slot.getItem(), era);
                }
            }
        }
        for (final Seat seat : geo.seats()) {
            final Slot slot = seatSlots.get(seat);
            AssemblyPainter.drawSeat(g, x, y, seat, slot != null && slot.isActive(), slot == null || !slot.hasItem());
        }
        drawCaptions(g, geo, x, y, era, boardIn);
        // The front panel.
        final Box power = geo.power();
        AssemblyPainter.drawPower(g, font, x, y, power, powerWord(),
                hover(mouseX, mouseY, power.x(), power.y(), power.w(), power.h()), powerActive(), powerLamp());
        final List<Readout> rows = readouts();
        for (int i = 0; i < rows.size() && i < geo.readouts(); i++) {
            final Readout r = rows.get(i);
            AssemblyPainter.drawReadout(g, font, x, y, geo.readout(i), r.label(), r.value(), r.colour());
        }
        drawOperations(g, geo, x, y);
        // The inventory, the service switches and the lamps.
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                JsTechTheme.slot(g, x + AssemblyGeometry.INVENTORY_X + col * 18, y + geo.inventoryY() + row * 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            JsTechTheme.slot(g, x + AssemblyGeometry.INVENTORY_X + col * 18, y + geo.inventoryY() + 58);
        }
        final List<ServiceSwitch> switches = serviceSwitches();
        for (int i = 0; i < switches.size(); i++) {
            AssemblyPainter.drawSwitch(g, font, x, y, geo.serviceSwitch(i), switches.get(i).caption(),
                    switches.get(i).on());
        }
        final boolean[] lit = lampsLit();
        AssemblyPainter.drawLamps(g, font, x, y, geo.lamps(),
                new String[] {GameText.resolve(AssemblyTexts.LAMP_POWER), GameText.resolve(AssemblyTexts.LAMP_DISK),
                        GameText.resolve(AssemblyTexts.LAMP_NETWORK)},
                lit, new int[] {JsTechTheme.green(), JsTechTheme.amber(), JsTechTheme.accent()});
    }

    @Override
    protected void renderLabels(final GuiGraphics g, final int mouseX, final int mouseY) {
        // Every word is drawn with the machine in renderBg; the game's own title and inventory caption are not shown.
    }

    @Override
    protected HardwareEra screenEra() {
        return caseEra();
    }

    /** The menu's slot at that seat, or null when the menu seats nothing there. */
    @Nullable
    protected Slot seatSlot(final Seat seat) {
        return seatSlots.get(seat);
    }

    /** The machine's era, whose case and theme the screen wears. */
    protected abstract HardwareEra caseEra();

    /** Where everything stands on this machine's screen. */
    protected abstract AssemblyGeometry makeGeometry();

    /** The machine's short name in the header. */
    protected abstract String machineTitle();

    /** The machine's state in the header, and its colour. */
    protected abstract String stateWord();

    protected abstract int stateColour();

    /** The front panel's readouts, top to bottom. */
    protected abstract List<Readout> readouts();

    /** The service panel's switches, top to bottom. */
    protected abstract List<ServiceSwitch> serviceSwitches();

    /** Whether the power, disk and network lamps are lit. */
    protected abstract boolean[] lampsLit();

    /** Whether a board is in the machine, which puts it over the tray. */
    protected abstract boolean boardIn();

    /** The name field in the header, or null for a machine named nowhere here. */
    @Nullable
    protected NameField nameField() {
        return null;
    }

    /** The power button's word. */
    protected String powerWord() {
        return GameText.resolve(AssemblyTexts.POWER);
    }

    /** The menu button the power button presses, or -1 when it presses none. */
    protected int powerButton() {
        return -1;
    }

    /** Whether the power button answers a click now. */
    protected boolean powerActive() {
        return true;
    }

    /** The power button's lamp. */
    protected abstract int powerLamp();

    /** What the power button says when the mouse rests on it. */
    protected List<Component> powerTooltip() {
        return List.of();
    }

    /** The operations strip's counts, or null for a machine with no strip. */
    @Nullable
    protected Operations operations() {
        return null;
    }

    private void drawHeader(final GuiGraphics g, final AssemblyGeometry geo, final int x, final int y) {
        final Box header = geo.header();
        JsTechTheme.headerBar(g, x + header.x(), y + header.y(), header.w());
        final String title = machineTitle();
        JsTechTheme.text(g, font, title, x + AssemblyGeometry.TITLE_X, y + AssemblyGeometry.TITLE_Y,
                JsTechTheme.accent());
        if (nameField() != null) {
            final int left = x + wellLeft();
            final int right = x + geo.wellRight();
            g.fill(left, y + AssemblyGeometry.WELL_TOP, right, y + AssemblyGeometry.WELL_BOTTOM, JsTechTheme.track());
            AssemblyPainter.frame(g, left, y + AssemblyGeometry.WELL_TOP, right, y + AssemblyGeometry.WELL_BOTTOM,
                    JsTechTheme.line());
        }
        final String state = stateWord();
        final int stateX = x + geo.width() - AssemblyGeometry.TITLE_X - font.width(state);
        JsTechTheme.text(g, font, state, stateX, y + AssemblyGeometry.TITLE_Y, stateColour());
        AssemblyPainter.drawLamp(g, stateX - 7, y + AssemblyGeometry.TITLE_Y + 3, stateColour(), true);
    }

    private void drawCaptions(final GuiGraphics g, final AssemblyGeometry geo, final int x, final int y,
                              final HardwareEra era, final boolean boardIn) {
        final int dim = JsTechTheme.dim();
        final int rail = y + AssemblyGeometry.RAIL_Y;
        JsTechTheme.textS(g, font, GameText.resolve(AssemblyTexts.REAR), x + AssemblyGeometry.REAR_X, rail, dim);
        JsTechTheme.textS(g, font, GameText.resolve(AssemblyTexts.CPU), x + AssemblyGeometry.CPU_X, rail, dim);
        JsTechTheme.textS(g, font, GameText.resolve(AssemblyTexts.DIMM), x + geo.dimmX(), rail, dim);
        if (geo.hasFront()) {
            JsTechTheme.textS(g, font, GameText.resolve(AssemblyTexts.FRONT), x + geo.frontX() - 2, rail, dim);
        }
        if (geo.bays() > 0) {
            JsTechTheme.textS(g, font, GameText.resolve(AssemblyTexts.DISK), x + geo.frontX(),
                    y + geo.diskCaptionY(), dim);
        }
        // The bus, printed on the board in its silkscreen once there is one, and in the case's own grey before.
        final String bus = GameText.resolve(switch (era) {
            case VINTAGE -> AssemblyTexts.BUS_ISA;
            case LEGACY -> AssemblyTexts.BUS_PCI;
            default -> AssemblyTexts.PCIE;
        });
        JsTechTheme.textS(g, font, bus, x + geo.busX(), y + geo.busY(), boardIn ? SILK.get().of(era) : dim);
    }

    private void drawOperations(final GuiGraphics g, final AssemblyGeometry geo, final int x, final int y) {
        final Box box = geo.operations();
        final Operations ops = operations();
        if (box == null || ops == null) {
            return;
        }
        final int left = x + box.x();
        final int top = y + box.y();
        JsTechTheme.panel(g, left, top, box.w(), box.h());
        JsTechTheme.textS(g, font, GameText.resolve(AssemblyTexts.OPERATIONS), left + 4, top + 6, JsTechTheme.dim());
        final int column = (box.w() - 60) / 3;
        final String[] keys = {GameText.resolve(AssemblyTexts.QUEUED), GameText.resolve(AssemblyTexts.RUNNING),
                GameText.resolve(AssemblyTexts.DONE)};
        final String[] values = {ops.queued(), ops.running(), ops.done()};
        for (int i = 0; i < keys.length; i++) {
            final int cx = left + 60 + i * column;
            JsTechTheme.textS(g, font, keys[i], cx, top + 6, JsTechTheme.dim());
            JsTechTheme.textRight(g, font, values[i], cx + column - 6, top + 5, JsTechTheme.text());
        }
    }

    /* The name well starts after the machine's title in the header. */
    private int wellLeft() {
        return AssemblyGeometry.TITLE_X + 6 + font.width(machineTitle());
    }

    private void tooltip(final GuiGraphics g, final List<Component> lines, final int mouseX, final int mouseY) {
        if (!lines.isEmpty() && menu.getCarried().isEmpty()) {
            g.renderComponentTooltip(font, lines, mouseX, mouseY);
        }
    }

    /** The silkscreen's ink on each era's board. */
    private record Silk(int vintage, int legacy, int transition, int standard, int advanced) {

        int of(final HardwareEra era) {
            return switch (era) {
                case VINTAGE -> vintage;
                case LEGACY -> legacy;
                case TRANSITION -> transition;
                case ADVANCED, EXA, SINGULARITY -> advanced;
                default -> standard;
            };
        }
    }
}
