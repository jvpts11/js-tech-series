/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.blockentity.PowerShown;
import dev.jstech.computers.client.assembly.AssemblyPainter;
import dev.jstech.computers.config.ComputersServerConfig;
import dev.jstech.computers.gui.layout.AssemblyGeometry;
import dev.jstech.computers.gui.layout.AssemblyGeometry.Box;
import dev.jstech.computers.gui.layout.AssemblyGeometry.Face;
import dev.jstech.computers.gui.layout.AssemblyGeometry.Seat;
import dev.jstech.computers.gui.layout.AssemblyGeometry.SeatKind;
import dev.jstech.computers.hardware.CaseCooling;
import dev.jstech.computers.hardware.CaseFanSpec;
import dev.jstech.computers.hardware.CoolerSpec;
import dev.jstech.computers.hardware.PsuCondition;
import dev.jstech.computers.hardware.PsuConditions;
import dev.jstech.computers.hardware.PsuSpec;
import dev.jstech.computers.hardware.PsuTexts;
import dev.jstech.computers.hardware.PsuWear;
import dev.jstech.computers.hardware.ThermalModel;
import dev.jstech.computers.hardware.ThermalState;
import dev.jstech.computers.registry.HardwareRegistry;
import dev.jstech.core.jstechseries.energy.JsEnergy;
import dev.jstech.core.jstechseries.theme.client.JsTechTheme;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.EraTable;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
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
     * @param gauge   the small gauge beside its label, or none
     */
    protected record Readout(String label, String value, int colour, List<Component> tooltip, @Nullable Gauge gauge) {

        public Readout(final String label, final String value, final int colour, final List<Component> tooltip) {
            this(label, value, colour, tooltip, null);
        }

        public Readout(final String label, final String value, final int colour) {
            this(label, value, colour, List.of(), null);
        }
    }

    /**
     * A small gauge beside a readout's label.
     *
     * @param fraction how full, from 0 to 1
     * @param colour   what it fills with
     * @param marks    where along it a mark stands, each from 0 to 1
     */
    protected record Gauge(double fraction, int colour, double... marks) {
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
    /* The bus printed on each era's board: ISA on the Vintage's, PCI on the Legacy's, PCIe from the Transition on. */
    private static final EraTable<TextKey> BUS_LABELS = EraTable.of(AssemblyTexts.BUS_ISA)
            .from(HardwareEra.LEGACY, AssemblyTexts.BUS_PCI)
            .from(HardwareEra.TRANSITION, AssemblyTexts.PCIE);
    /* Past this load a supply's gauge turns amber; its marks stand where a supply's wear slows and quickens. */
    private static final double HIGH_LOAD = 0.8;
    private static final double[] LOAD_MARKS = {0.5, HIGH_LOAD};
    private static final int PERCENT = 100;
    private static final int TICKS_A_SECOND = 20;
    /* The temperature gauge starts at the plains' surroundings. */
    private static final double GAUGE_FROM = 20.0;
    /* How the cooling face dims the board behind it, and the depth it is drawn at, over the board's parts. */
    private static final int DIM_ALPHA = 178;
    private static final float COOLING_Z = 200f;

    protected AbstractMachineAssemblyScreen(final T menu, final Inventory playerInventory, final Component title) {
        super(menu, playerInventory, title);
        this.titleLabelX = OFF_SCREEN;
        this.inventoryLabelY = OFF_SCREEN;
    }

    @Override
    public void render(final GuiGraphics g, final int mouseX, final int mouseY, final float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        // A part held over a slot that keeps it out says which rule does, rather than the slot refusing in silence.
        final ItemStack carried = menu.getCarried();
        if (!carried.isEmpty() && hoveredSlot != null && !hoveredSlot.mayPlace(carried)) {
            final Component why = refusal(hoveredSlot, carried);
            if (why != null) {
                g.renderComponentTooltip(font, List.of(why), mouseX, mouseY);
            }
            return;
        }
        final AssemblyGeometry geo = geometry();
        final Box power = geo.power();
        if (hover(mouseX, mouseY, power.x(), power.y(), power.w(), power.h())) {
            tooltip(g, powerTooltip(), mouseX, mouseY);
            return;
        }
        final List<Readout> rows = shownReadouts();
        for (int i = 0; i < rows.size(); i++) {
            final Box r = geo.readout(i);
            if (hover(mouseX, mouseY, r.x(), r.y(), r.w(), r.h())) {
                tooltip(g, rows.get(i).tooltip(), mouseX, mouseY);
                return;
            }
        }
    }

    /** Over a cooler's or a fan's seat, what it is and what it takes away, as the cooling face names them. */
    @Override
    protected void renderTooltip(final GuiGraphics g, final int mouseX, final int mouseY) {
        final Slot slot = hoveredSlot;
        if (menu.getCarried().isEmpty() && slot != null && slot.hasItem() && coolingSlot(slot)) {
            final List<Component> lines = coolingPartTooltip(slot);
            if (!lines.isEmpty()) {
                g.renderComponentTooltip(font, lines, mouseX, mouseY);
                return;
            }
        }
        super.renderTooltip(g, mouseX, mouseY);
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
            if (geo.cooled()) {
                for (final Face face : Face.values()) {
                    final Box tab = geo.tab(face);
                    if (hover((int) mouseX, (int) mouseY, tab.x(), tab.y(), tab.w(), tab.h())) {
                        // Turned here at once, and on the server, whose slots follow the same face.
                        showFace(face);
                        sendButton(faceButton(face));
                        return true;
                    }
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
            // The two faces seat parts in the same places (a cooler where its processor sits), so each seat takes its
            // own face's slot.
            for (final Slot slot : menu.slots) {
                if (slot.x == seat.x() && slot.y == seat.y() && coolingSlot(slot) == (seat.face() == Face.COOLING)) {
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
        final Era era = caseEra();
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
        if (shownFace() == Face.BOARD) {
            for (final Seat seat : geo.seats(Face.BOARD)) {
                final Slot slot = seatSlots.get(seat);
                /*
                 * Once a board is seated, a seat that neither it nor the chassis wires is not drawn at all: dimmed, it
                 * looked like a slot only waiting for its part, and took none. Before a board, every seat shows dimmed
                 * where its part will go.
                 */
                if (boardIn && slot != null && !slot.isActive()) {
                    continue;
                }
                // A tower's second row is a two-socket board's alone: it shows only while one is in.
                if (geo.inSecondRow(seat) && (slot == null || !slot.isActive())) {
                    continue;
                }
                AssemblyPainter.drawSeat(g, x, y, seat, slot != null && slot.isActive(),
                        slot == null || !slot.hasItem());
                final PsuSpec psu = seat.kind() == SeatKind.PSU && slot != null ? PsuConditions.spec(slot.getItem())
                        : null;
                if (psu != null) {
                    AssemblyPainter.drawPsuEdge(g, x, y, seat, psu, PsuConditions.of(slot.getItem()));
                }
            }
            drawCaptions(g, geo, x, y, era, boardIn);
        } else {
            drawCoolingFace(g, geo, x, y, era);
        }
        // The front panel.
        final Box power = geo.power();
        AssemblyPainter.drawPower(g, font, x, y, power, powerWord(),
                hover(mouseX, mouseY, power.x(), power.y(), power.w(), power.h()), powerActive(), powerLamp());
        final List<Readout> rows = shownReadouts();
        for (int i = 0; i < rows.size() && i < geo.readouts(); i++) {
            final Readout r = rows.get(i);
            AssemblyPainter.drawReadout(g, font, x, y, geo.readout(i), r.label(), r.value(), r.colour());
            if (r.gauge() != null) {
                AssemblyPainter.drawGauge(g, font, x, y, geo.readout(i), r.label(), r.gauge().fraction(),
                        r.gauge().colour(), r.gauge().marks());
            }
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
        if (geo.cooled()) {
            drawTabs(g, geo, x, y, mouseX, mouseY);
        }
    }

    @Override
    protected void renderLabels(final GuiGraphics g, final int mouseX, final int mouseY) {
        // Every word is drawn with the machine in renderBg; the game's own title and inventory caption are not shown.
        // The supply's wear goes over its icon, as an item's durability does, so it is drawn once the items are.
        if (!ComputersServerConfig.psuFailures() || shownFace() != Face.BOARD) {
            return;
        }
        for (final Seat seat : geometry().seats(SeatKind.PSU)) {
            final Slot slot = seatSlots.get(seat);
            final PsuSpec spec = slot == null || !slot.isActive() ? null : PsuConditions.spec(slot.getItem());
            if (spec != null) {
                AssemblyPainter.drawPsuWear(g, seat.x(), seat.y(), spec, PsuConditions.of(slot.getItem()));
            }
        }
    }

    /**
     * What the power button says of a machine whose supply has failed: why it does not start and what to do; nothing
     * for any other.
     */
    protected static List<Component> psuFailure(final ItemStack psu) {
        if (!PsuConditions.failed(psu)) {
            return List.of();
        }
        return List.of(GameText.component(PsuTexts.PSU_FAILURE).withColor(JsTechTheme.red()),
                GameText.component(PsuTexts.SUPPLY_FAILED.with(psu.getHoverName().getString()))
                        .withColor(JsTechTheme.dim()),
                GameText.component(PsuTexts.REPLACE).withColor(JsTechTheme.dim()));
    }

    @Override
    protected Era screenEra() {
        return caseEra();
    }

    /** The menu's slot at that seat, or null when the menu seats nothing there. */
    @Nullable
    protected Slot seatSlot(final Seat seat) {
        return seatSlots.get(seat);
    }

    /** The machine's era, whose case and theme the screen wears. */
    /**
     * The DRAW readout: what the parts take against the supply's rating, its gauge the supply's load (green, amber past
     * 80%, red past the rating), and its tooltip what that takes from the line and the supply it goes through; NO POWER
     * while no energy reaches a machine that has a supply.
     *
     * @param power what the machine's power draws
     * @param psu   its supply, named in the tooltip
     */
    protected static Readout drawReadout(final PowerShown power, final ItemStack psu) {
        final String label = GameText.resolve(AssemblyTexts.DRAW);
        if (power.psuWatts() == 0) {
            return new Readout(label, GameText.resolve(AssemblyTexts.NO_WATTS), JsTechTheme.dim());
        }
        final double load = power.load();
        final List<Component> tooltip = new ArrayList<>();
        tooltip.add(GameText.component(AssemblyTexts.DRAW_PARTS.with(power.partsWatts(), power.psuWatts(),
                (int) Math.round(load * PERCENT))));
        tooltip.add(GameText.component(AssemblyTexts.DRAW_LINE.with(power.lineWatts(),
                JsEnergy.ENERGY.toFe(Math.round(power.lineWatts() / (double) TICKS_A_SECOND))))
                .withColor(JsTechTheme.accent()));
        final PsuSpec spec = HardwareRegistry.psu(psu);
        final PsuCondition condition = PsuConditions.of(psu);
        if (spec != null) {
            tooltip.add(GameText.component(AssemblyTexts.DRAW_SUPPLY.with(psu.getHoverName().getString(),
                    spec.efficiencyPercent())).withColor(JsTechTheme.dim()));
            if (ComputersServerConfig.psuFailures()) {
                final int colour = condition.failed(spec) ? JsTechTheme.red()
                        : condition.degraded(spec) ? JsTechTheme.amber() : JsTechTheme.dim();
                for (final Text line : PsuTexts.state(spec, condition)) {
                    tooltip.add(GameText.component(line).withColor(colour));
                }
                if (!condition.failed(spec)) {
                    tooltip.add(GameText.component(PsuTexts.pace(PsuWear.unitLoad(spec, condition,
                            power.partsWatts()))).withColor(JsTechTheme.dim()));
                }
            }
            if (condition.failed(spec)) {
                return new Readout(label, GameText.resolve(AssemblyTexts.DRAW_FAILED), JsTechTheme.red(), tooltip,
                        new Gauge(0.0, JsTechTheme.red(), LOAD_MARKS));
            }
        }
        if (!power.energized()) {
            tooltip.add(GameText.component(AssemblyTexts.DRAW_NONE).withColor(JsTechTheme.red()));
            return new Readout(label, GameText.resolve(AssemblyTexts.NO_POWER), JsTechTheme.red(), tooltip,
                    new Gauge(0.0, JsTechTheme.red(), LOAD_MARKS));
        }
        final int gauge = load > 1.0 ? JsTechTheme.red() : load > HIGH_LOAD ? JsTechTheme.amber() : JsTechTheme.green();
        final int value = spec != null && condition.degraded(spec) ? JsTechTheme.amber() : JsTechTheme.text();
        return new Readout(label, GameText.resolve(AssemblyTexts.DRAW_OF.with(power.partsWatts(),
                power.psuWatts())), value, tooltip, new Gauge(load, gauge, LOAD_MARKS));
    }

    /**
     * The TEMP readout of the board's face (computers/docs/design/temperature.md): the degrees, green up to 80, amber
     * to 90, red past, with a gauge from 20 to 100 °C marked at 80 and 90; its tooltip what the heat costs the machine
     * and, when its cooling falls short of what it holds, what is missing and where to fit it. None for a machine with
     * no temperature.
     */
    protected static Readout tempReadout(final ThermalState thermal) {
        final String label = GameText.resolve(AssemblyTexts.TEMP);
        if (!thermal.known()) {
            return new Readout(label, "--", JsTechTheme.dim());
        }
        final List<Component> tooltip = new ArrayList<>();
        tooltip.add(GameText.component(thermal.throttled()
                ? AssemblyTexts.TEMP_THROTTLED.with(thermal.celsius(), thermal.clockPercent())
                : thermal.clockPercent() < PERCENT ? AssemblyTexts.TEMP_FADING.with(thermal.celsius(),
                thermal.clockPercent()) : AssemblyTexts.TEMP_FULL.with(thermal.celsius()))
                .withColor(tempColour(thermal.celsius())));
        tooltip.add(GameText.component(AssemblyTexts.TEMP_LOAD.with(thermal.heatWatts(), thermal.budgetWatts()))
                .withColor(JsTechTheme.dim()));
        final boolean bare = thermal.bareProcessors() > 0;
        final boolean noFans = thermal.fans() == 0 && thermal.fanSeats() > 0;
        if (bare) {
            tooltip.add(GameText.component(thermal.bareProcessors() == 1 ? AssemblyTexts.NO_COOLER.text()
                    : AssemblyTexts.NO_COOLERS.with(thermal.bareProcessors())).withColor(JsTechTheme.red()));
        }
        if (noFans) {
            tooltip.add(GameText.component(AssemblyTexts.NO_FANS).withColor(JsTechTheme.red()));
        }
        if (bare || noFans) {
            tooltip.add(GameText.component(AssemblyTexts.OPEN_COOLING).withColor(JsTechTheme.dim()));
        }
        return new Readout(label, GameText.resolve(AssemblyTexts.DEGREES.with(thermal.celsius())),
                tempColour(thermal.celsius()), tooltip, tempGauge(thermal.celsius()));
    }

    /**
     * What the power button says of a machine still too hot to start after a thermal trip: why, and how cool it has to
     * be; nothing for any other.
     */
    protected static List<Component> tooHot(final ThermalState thermal) {
        if (!thermal.cooling()) {
            return List.of();
        }
        return List.of(GameText.component(AssemblyTexts.TOO_HOT).withColor(JsTechTheme.red()),
                GameText.component(AssemblyTexts.COOLS_TO.with(ComputersServerConfig.restartBelow(),
                        thermal.celsius())).withColor(JsTechTheme.dim()));
    }

    /**
     * The cooling face's readouts: the temperature, the heat the parts make against the budget, the budget, the
     * surroundings and the clock its heat leaves it.
     */
    protected List<Readout> coolingReadouts() {
        final ThermalState thermal = thermal();
        return List.of(tempReadout(thermal), heatReadout(thermal),
                new Readout(GameText.resolve(AssemblyTexts.BUDGET),
                        GameText.resolve(AssemblyTexts.WATTS.with(JsTechTheme.fmt(thermal.budgetWatts()))),
                        JsTechTheme.text()),
                ambientReadout(thermal), clockReadout(thermal));
    }

    /** The HEAT readout: the parts' watts, its gauge how near the budget they come. */
    protected static Readout heatReadout(final ThermalState thermal) {
        final double share = thermal.budgetWatts() <= 0 ? 1.0 : thermal.heatWatts() / (double) thermal.budgetWatts();
        return new Readout(GameText.resolve(AssemblyTexts.HEAT),
                GameText.resolve(AssemblyTexts.WATTS.with(JsTechTheme.fmt(thermal.heatWatts()))), JsTechTheme.text(),
                List.of(), new Gauge(Math.min(1.0, share), tempColour(thermal.celsius()), 1.0));
    }

    /** The AMBIENT readout: the surroundings the machine stands in. */
    protected static Readout ambientReadout(final ThermalState thermal) {
        return new Readout(GameText.resolve(AssemblyTexts.AMBIENT),
                GameText.resolve(AssemblyTexts.DEGREES.with(thermal.ambient())), JsTechTheme.text());
    }

    /** The CLOCK readout: amber below its full clock, red at the floor. */
    protected static Readout clockReadout(final ThermalState thermal) {
        final int clock = thermal.clockPercent();
        final int colour = clock >= PERCENT ? JsTechTheme.text()
                : clock <= (int) Math.round(ThermalModel.FLOOR * PERCENT) ? JsTechTheme.red() : JsTechTheme.amber();
        return new Readout(GameText.resolve(AssemblyTexts.CLOCK), GameText.resolve(AssemblyTexts.PERCENT.with(clock)),
                colour);
    }

    /** The colour of a temperature: green up to 80 °C, amber to 90, red from there. */
    protected static int tempColour(final int celsius) {
        return celsius >= ThermalModel.THROTTLE_AT ? JsTechTheme.red()
                : celsius >= ThermalModel.FULL_UP_TO ? JsTechTheme.amber() : JsTechTheme.green();
    }

    /** The face the screen shows: its board's, or its cooling; a machine whose case does not cool shows its board. */
    protected Face face() {
        return Face.BOARD;
    }

    /** Turns the screen to {@code face}, at once on this side. */
    protected void showFace(final Face face) {
    }

    /** The menu button the tab of {@code face} presses. */
    protected int faceButton(final Face face) {
        return -1;
    }

    /** Whether {@code slot} is one of the cooling face's: a cooler's or a fan's. */
    protected boolean coolingSlot(final Slot slot) {
        return false;
    }

    /** How the machine's heat stands, as the server last told the screen; none for a machine with no temperature. */
    protected ThermalState thermal() {
        return ThermalState.NONE;
    }

    protected abstract Era caseEra();

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

    /** Why {@code carried} stays out of {@code slot}, said over the slot; null when the machine names no rule. */
    @Nullable
    protected Component refusal(final Slot slot, final ItemStack carried) {
        return null;
    }

    /** What the power button says when the mouse rests on it. */
    protected List<Component> powerTooltip() {
        return List.of();
    }

    /** The operations strip's counts, or null for a machine with no strip. */
    @Nullable
    protected Operations operations() {
        return null;
    }

    /* The face drawn: the board's on a machine whose case does not cool. */
    private Face shownFace() {
        return geometry().cooled() ? face() : Face.BOARD;
    }

    /* The readouts of the face shown. */
    private List<Readout> shownReadouts() {
        return shownFace() == Face.COOLING ? coolingReadouts() : readouts();
    }

    /* The gauge of a temperature, from 20 °C to 100 °C, marked at 80 and 90. */
    private static Gauge tempGauge(final int celsius) {
        final double span = ThermalModel.TRIP_AT - GAUGE_FROM;
        return new Gauge((celsius - GAUGE_FROM) / span, tempColour(celsius),
                (ThermalModel.FULL_UP_TO - GAUGE_FROM) / span, (ThermalModel.THROTTLE_AT - GAUGE_FROM) / span);
    }

    /*
     * The cooling face (computers/docs/design/temperature.md): the board and what is on it dim behind the cooling
     * parts, a cooler seat over every processor and the case's fan seats, a water cooler's radiator over the pair of
     * fan seats it takes with its hoses to its pump, and the captions over them. The dim and what is drawn over it go
     * above the board's parts, which the items of the cooling seats then go above.
     */
    private void drawCoolingFace(final GuiGraphics g, final AssemblyGeometry geo, final int x, final int y,
                                 final Era era) {
        for (final Seat seat : geo.seats(Face.BOARD)) {
            final Slot slot = seatSlots.get(seat);
            if (slot != null && slot.hasItem()) {
                g.renderItem(slot.getItem(), x + seat.x(), y + seat.y());
            }
        }
        final Box box = geo.caseBox();
        g.pose().pushPose();
        g.pose().translate(0, 0, COOLING_Z);
        g.fill(x + box.x() + 1, y + box.y() + 1, x + box.x() + box.w() - 1, y + box.y() + box.h() - 1,
                AssemblyPainter.withAlpha(JsTechTheme.screen(), DIM_ALPHA));
        g.pose().translate(0, 0, 1);
        final List<CoolerSpec> coolers = new ArrayList<>();
        for (final Seat seat : geo.seats(SeatKind.COOLER)) {
            final Slot slot = seatSlots.get(seat);
            coolers.add(slot == null ? null : HardwareRegistry.cooler(slot.getItem()));
            if (slot != null && (slot.isActive() || !geo.inSecondRow(seat))) {
                AssemblyPainter.drawSeat(g, x, y, seat, slot.isActive(), !slot.hasItem());
            }
        }
        final Set<Integer> radiators = geo.cooling().radiatorSeats(coolers);
        for (final Seat seat : geo.seats(SeatKind.FAN)) {
            final Slot slot = seatSlots.get(seat);
            if (!radiators.contains(seat.index()) && slot != null) {
                AssemblyPainter.drawSeat(g, x, y, seat, slot.isActive(), !slot.hasItem());
            }
        }
        drawRadiators(g, geo, x, y, era, coolers);
        drawCoolingCaptions(g, geo, x, y);
        g.pose().popPose();
    }

    /* Each water cooler's radiator over the pair of fan seats it takes, and the two hoses from its pump to it. */
    private void drawRadiators(final GuiGraphics g, final AssemblyGeometry geo, final int x, final int y,
                               final Era era, final List<CoolerSpec> coolers) {
        final List<Integer> pairs = geo.cooling().radiatorPairs();
        final List<Seat> fans = geo.seats(SeatKind.FAN);
        final List<Seat> pumps = geo.seats(SeatKind.COOLER);
        int pair = 0;
        for (int i = 0; i < coolers.size() && pair < pairs.size(); i++) {
            final CoolerSpec cooler = coolers.get(i);
            if (cooler == null || cooler.radiatorFans() == 0) {
                continue;
            }
            final Seat first = fans.get(pairs.get(pair));
            final Seat second = fans.get(pairs.get(pair) + 1);
            AssemblyPainter.drawRadiator(g, x + first.x(), y + first.y(), first.y() == second.y(), era,
                    x + pumps.get(i).x(), y + pumps.get(i).y());
            pair++;
        }
    }

    private void drawCoolingCaptions(final GuiGraphics g, final AssemblyGeometry geo, final int x, final int y) {
        final int dim = JsTechTheme.dim();
        final int rail = y + AssemblyGeometry.RAIL_Y;
        if (geo.cooling().rearFans() > 0) {
            JsTechTheme.textS(g, font, GameText.resolve(AssemblyTexts.REAR), x + AssemblyGeometry.REAR_X, rail, dim);
        }
        final boolean many = geo.seats(SeatKind.COOLER).size() > 1;
        JsTechTheme.textS(g, font, GameText.resolve(many ? AssemblyTexts.COOLERS : AssemblyTexts.COOLER),
                x + AssemblyGeometry.CPU_X, rail, dim);
        if (geo.topCaptionX() >= 0) {
            JsTechTheme.textS(g, font, GameText.resolve(AssemblyTexts.TOP), x + geo.topCaptionX(), rail, dim);
        }
        if (geo.cooling().frontFans() > 0) {
            JsTechTheme.textS(g, font, GameText.resolve(AssemblyTexts.FRONT), x + geo.frontX() - 2, rail, dim);
        }
    }

    /*
     * The tabs of the case's two faces under the lamps, the shown one lit; the cooling tab's lamp lights amber when the
     * machine's cooling falls short of what it holds, red when a processor has no cooler or it would trip.
     */
    private void drawTabs(final GuiGraphics g, final AssemblyGeometry geo, final int x, final int y, final int mouseX,
                          final int mouseY) {
        for (final Face face : Face.values()) {
            final Box tab = geo.tab(face);
            final boolean on = face == shownFace();
            if (on) {
                JsTechTheme.selectedTab(g, x + tab.x(), y + tab.y(), tab.w(), tab.h());
            } else {
                JsTechTheme.panel(g, x + tab.x(), y + tab.y(), tab.w(), tab.h());
                if (hover(mouseX, mouseY, tab.x(), tab.y(), tab.w(), tab.h())) {
                    g.fill(x + tab.x() + 1, y + tab.y() + 1, x + tab.x() + tab.w() - 1, y + tab.y() + tab.h() - 1,
                            JsTechTheme.hover());
                }
            }
            JsTechTheme.textSCenter(g, font, GameText.resolve(face == Face.BOARD ? AssemblyTexts.FACE_BOARD
                    : AssemblyTexts.FACE_COOLING), x + tab.x() + tab.w() / 2, y + tab.y() + 3,
                    on ? JsTechTheme.tabLabelOn() : JsTechTheme.dim());
        }
        final ThermalState.Lack lack = thermal().lack();
        if (lack != ThermalState.Lack.NONE) {
            final Box cooling = geo.tab(Face.COOLING);
            g.fill(x + cooling.x() + cooling.w() - 5, y + cooling.y() + 2, x + cooling.x() + cooling.w() - 2,
                    y + cooling.y() + 5, lack == ThermalState.Lack.BARE ? JsTechTheme.red() : JsTechTheme.amber());
        }
    }

    /* What the cooling face says of the cooler or the fan in {@code slot}: its name, where it sits, what it takes. */
    private List<Component> coolingPartTooltip(final Slot slot) {
        final ItemStack stack = slot.getItem();
        final List<Component> lines = new ArrayList<>();
        lines.add(stack.getHoverName());
        final CoolerSpec cooler = HardwareRegistry.cooler(stack);
        final CaseFanSpec fan = HardwareRegistry.caseFan(stack);
        if (cooler != null) {
            final Seat seat = seatOf(slot);
            if (cooler.radiatorFans() > 0 && seat != null) {
                lines.add(GameText.component(radiatorSide(seat.index()) == CaseCooling.Side.TOP
                        ? AssemblyTexts.RADIATOR_TOP : AssemblyTexts.RADIATOR_FRONT).withColor(JsTechTheme.dim()));
            }
            lines.add(GameText.component(AssemblyTexts.TAKES_AWAY.with(cooler.takesAwayWatts()))
                    .withColor(JsTechTheme.accent()));
        } else if (fan != null) {
            final Seat seat = seatOf(slot);
            if (seat != null) {
                final CaseCooling.Side side = geometry().fanSide(seat.index());
                lines.add(GameText.component(side == CaseCooling.Side.REAR ? AssemblyTexts.SEAT_REAR
                        : side == CaseCooling.Side.TOP ? AssemblyTexts.SEAT_TOP : AssemblyTexts.SEAT_FRONT)
                        .withColor(JsTechTheme.dim()));
            }
            lines.add(GameText.component(AssemblyTexts.TAKES_AWAY.with(fan.takesAwayWatts()))
                    .withColor(JsTechTheme.accent()));
        }
        return lines;
    }

    /*
     * Where the radiator of the water cooler over processor {@code index} sits: the pair the water coolers before it
     * leave, the top's first and the front's after.
     */
    private CaseCooling.Side radiatorSide(final int index) {
        int before = 0;
        for (final Seat seat : geometry().seats(SeatKind.COOLER)) {
            final Slot slot = seatSlots.get(seat);
            final CoolerSpec cooler = slot == null ? null : HardwareRegistry.cooler(slot.getItem());
            if (seat.index() < index && cooler != null && cooler.radiatorFans() > 0) {
                before++;
            }
        }
        final List<Integer> pairs = geometry().cooling().radiatorPairs();
        return before < pairs.size() ? geometry().fanSide(pairs.get(before)) : CaseCooling.Side.FRONT;
    }

    /* The seat {@code slot} sits in, or null. */
    @Nullable
    private Seat seatOf(final Slot slot) {
        for (final Map.Entry<Seat, Slot> entry : seatSlots.entrySet()) {
            if (entry.getValue() == slot) {
                return entry.getKey();
            }
        }
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
                              final Era era, final boolean boardIn) {
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
        final String bus = GameText.resolve(BUS_LABELS.get(era));
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

        int of(final Era era) {
            return HardwareEra.pick(era, vintage, legacy, transition, standard, advanced);
        }
    }
}
