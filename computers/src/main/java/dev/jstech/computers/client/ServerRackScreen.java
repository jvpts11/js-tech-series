/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.computers.client.assembly.AssemblyPainter;
import dev.jstech.computers.client.theme.MonitorFrameStyle;
import dev.jstech.computers.gui.layout.AssemblyGeometry.SeatKind;
import dev.jstech.computers.gui.layout.ServerRackLayout;
import dev.jstech.computers.backup.TapeContents;
import dev.jstech.computers.backup.TapeDrive;
import dev.jstech.computers.backup.TapeFormat;
import dev.jstech.computers.hardware.ComputerBuild;
import dev.jstech.computers.hardware.DiskSpec;
import dev.jstech.computers.hardware.EraStorage;
import dev.jstech.computers.hardware.ThermalModel;
import dev.jstech.computers.item.RackUnitItem;
import dev.jstech.computers.item.ServerItem;
import dev.jstech.computers.item.TapeItem;
import dev.jstech.computers.menu.ServerRackMenu;
import dev.jstech.computers.operation.payload.RackBayPowerPayload;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.power.UpsRating;
import dev.jstech.computers.power.UpsReading;
import dev.jstech.computers.rack.IMountableRackUnit;
import dev.jstech.computers.rack.RackChassis;
import dev.jstech.computers.rack.RackLayout;
import dev.jstech.core.client.gui.component.Texts;
import dev.jstech.core.format.UnitFormatter;
import dev.jstech.core.jstechseries.energy.JsEnergy;
import dev.jstech.core.jstechseries.theme.client.JsTechTheme;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Screen for the Server Rack, drawn as the cabinet seen from the front: the rack-unit ruler, the two posts with their
 * holes, and between them each mounted unit as its faceplate, with its own slot at the left end, its drive caddies
 * along it, vents over what its chassis does not use and, on a computer, its power button with its lamp at
 * the right end; an empty unit is a blank panel. The card on the right is the unit picked by clicking its faceplate: its
 * name, its state, what it boots, how it is cabled and what a linked monitor shows. Physical only, by design: no console
 * and no terminal live here, software access always goes through a monitor cabled to the rack.
 */
@PaletteHolder
public class ServerRackScreen extends AbstractComputerScreen<ServerRackMenu> {

    /* The unit the card describes: the top row of a mounted computer, picked by clicking its faceplate. */
    private int selected;

    private static final int ROWS = ServerRackLayout.ROWS;
    private static final int PITCH = ServerRackLayout.ROW_PITCH;
    /** The post's holes, three to a rack unit, from the unit's top. */
    private static final int[] POST_HOLES = {3, 9, 15};
    private static final int GHOST_ALPHA = 110;

    /**
     * The cabinet's metal and its units' faceplates, per era: the metal its posts and its back are made of, the colour
     * of that era's case, and the faceplates the units wear, dark in every era but the newest; and the light a
     * faceplate's top edge and its vents catch.
     */
    private static final Palette<Cabinet> CABINET = Palettes.declare(JsComputers.MODID, "screen/rack_cabinet",
            new Cabinet(0xFF061A08, 0xFF9AA0A8, 0xFF3A3E46, 0xFF2E3035, 0xFFDFE2E6,
                    0xFF1B1D21, 0xFF1B1D21, 0xFF1B1D21, 0xFF1B1D21, 0xFF141518, 0xFFFFFFFF));

    public ServerRackScreen(final ServerRackMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title);
        this.imageWidth = ServerRackLayout.WIDTH;
        this.imageHeight = ServerRackLayout.HEIGHT;
        this.titleLabelX = OFF_SCREEN;
        this.inventoryLabelY = OFF_SCREEN;
    }

    /**
     * The plain panel, with no monitor bezel: this screen is a physical cabinet panel, not a monitor, so a recipe viewer
     * placing its own panel beside it should sit against the panel's edge rather than an unclaimed bezel.
     */
    @Override
    public MonitorFrameStyle.Geometry frameBounds() {
        return new MonitorFrameStyle.Geometry(leftPos, topPos, imageWidth, imageHeight, topPos + imageHeight);
    }

    /*
     * The rack GUI stays physical: it REPORTS the array's health but never configures it. A RAID controller is set up
     * in its own firmware setup at power-on, like the real thing; see the machine's STORAGE page in the BIOS.
     */

    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        final int relX = (int) Math.floor(mouseX - leftPos);
        final int relY = (int) Math.floor(mouseY - topPos);
        final int row = rowAt(relX, relY);
        final int top = row < 0 ? -1 : menu.unitTopOf(row);
        if (selectable(top)) {
            // A click anywhere on a unit's faceplate picks it for the card, and still reaches the slot under it.
            selected = top;
            if (isComputer(top) && button == 0 && onPower(top, relX, relY)) {
                send(new RackBayPowerPayload(menu.rackPos(), top));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(final GuiGraphics g, final int mouseX, final int mouseY, final float partialTick) {
        // The base binds the cabinet's era skin for the whole pass and renders the hovered slot's own tooltip.
        super.render(g, mouseX, mouseY, partialTick);
        renderRackTooltip(g, mouseX, mouseY);
    }

    /** The cabinet's own era, so its screen (labels included) wears that decade's skin. */
    @Override
    protected Era screenEra() {
        return menu.rackEra();
    }

    @Override
    protected void renderBg(final GuiGraphics g, final float partialTick, final int mouseX, final int mouseY) {
        final int x = leftPos;
        final int y = topPos;
        final Era era = menu.rackEra();
        final int metal = CABINET.get().metal(era);
        final int face = CABINET.get().face(era);
        JsTechTheme.window(g, x, y, imageWidth, imageHeight);
        drawHeader(g, x, y);
        drawCabinet(g, x, y, metal);
        int row = 0;
        while (row < ROWS) {
            final int ry = y + ServerRackLayout.rowY(row);
            JsTechTheme.textS(g, font, GameText.resolve(ServerRackTexts.UNIT.with(row + 1)),
                    x + ServerRackLayout.RULER_X, ry + 5, JsTechTheme.dim());
            final IMountableRackUnit unit = IMountableRackUnit.of(menu.serverInBay(row));
            if (unit == null) {
                // An empty unit: a blank panel between the posts, nothing to click.
                g.fill(x + ServerRackLayout.RAIL_L, ry, x + ServerRackLayout.RAIL_R - 2, ry + PITCH - 1,
                        AssemblyPainter.mix(JsTechTheme.screen(), metal, 0.2f));
                g.fill(x + ServerRackLayout.RAIL_L, ry, x + ServerRackLayout.RAIL_R - 2, ry + 1,
                        AssemblyPainter.mix(JsTechTheme.line(), metal, 0.5f));
                row++;
                continue;
            }
            final int heightU = Math.max(1, Math.min(unit.heightU(), ROWS - row));
            drawUnit(g, x, y, row, heightU, unit, face, mouseX, mouseY);
            row += heightU;
        }
        // The unit the card is about, framed over the gap the units leave round each other.
        final int top = selectedUnit();
        if (top >= 0) {
            final int h = menu.unitHeightAt(top) * PITCH;
            final int ry = y + ServerRackLayout.rowY(top);
            AssemblyPainter.frame(g, x + ServerRackLayout.RAIL_L - 1, ry - 1, x + ServerRackLayout.RAIL_R - 1, ry + h,
                    JsTechTheme.accent());
        }
        drawCard(g, x, y, top);
        /*
         * The player inventory, drawn from the menu's own slots (everything past the rack's server and front slots),
         * so a moved slot always draws its frame with it.
         */
        for (int i = ServerRackMenu.RACK_SLOTS + ServerRackMenu.FRONT_SLOT_COUNT; i < menu.slots.size(); i++) {
            final Slot slot = menu.slots.get(i);
            if (slot.isActive()) {
                JsTechTheme.slot(g, x + slot.x, y + slot.y);
            }
        }
    }

    @Override
    protected void renderLabels(final GuiGraphics g, final int mouseX, final int mouseY) {
        // Every word is drawn with the cabinet in renderBg; the game's own title and inventory caption are not shown.
    }

    /**
     * A front slot still holding what went in before its unit came out shows the stack and why the slot no longer
     * takes anything, so the player knows to take it back out.
     */
    @Override
    protected void renderTooltip(final GuiGraphics g, final int mouseX, final int mouseY) {
        final Slot slot = hoveredSlot;
        final int index = slot == null ? -1 : frontIndex(slot);
        // A unit or a drive held over a row or a bay that keeps it out says why, rather than nothing happening.
        final ItemStack carried = menu.getCarried();
        if (slot != null && !carried.isEmpty() && !slot.mayPlace(carried)) {
            final RackLayout.SlotRole role = index >= 0 ? menu.frontSlotRole(index) : null;
            final Component why = role == RackLayout.SlotRole.BLOCKED_NO_UNIT
                    ? GameText.component(ServerRackTexts.BLOCKED_NO_UNIT)
                    : role == RackLayout.SlotRole.BLOCKED_BUDGET ? GameText.component(ServerRackTexts.BLOCKED_BUDGET)
                    : menu.refusal(slot, carried);
            if (why != null) {
                g.renderComponentTooltip(font, List.of(why), mouseX, mouseY);
            }
            return;
        }
        if (index >= 0 && slot.hasItem() && menu.getCarried().isEmpty()) {
            final RackLayout.SlotRole role = menu.frontSlotRole(index);
            if (role == RackLayout.SlotRole.BLOCKED_NO_UNIT || role == RackLayout.SlotRole.BLOCKED_BUDGET) {
                final List<Component> lines = new ArrayList<>(getTooltipFromContainerItem(slot.getItem()));
                lines.add(GameText.component(role == RackLayout.SlotRole.BLOCKED_NO_UNIT
                        ? ServerRackTexts.BLOCKED_NO_UNIT : ServerRackTexts.BLOCKED_BUDGET).withStyle(ChatFormatting.RED));
                g.renderComponentTooltip(font, lines, mouseX, mouseY);
                return;
            }
        }
        super.renderTooltip(g, mouseX, mouseY);
    }

    /* The header: the cabinet's name, and beside the link lamp the rack units in use and the cabinet's state. */
    private void drawHeader(final GuiGraphics g, final int x, final int y) {
        JsTechTheme.headerBar(g, x + ServerRackLayout.HEADER_X, y + ServerRackLayout.HEADER_Y, ServerRackLayout.HEADER_W);
        JsTechTheme.text(g, font, title.getString().toUpperCase(Locale.ROOT), x + ServerRackLayout.TITLE_X,
                y + ServerRackLayout.TITLE_Y, JsTechTheme.accent());
        final int usedU = RackLayout.usedU(menu.mountedUnits());
        /*
         * A cabinet past its thermal budget says so where the link state would sit: it is the thing the player most
         * needs to know about this rack right now.
         */
        final int throttle = menu.throttlePercent();
        final boolean linked = menu.networkLinked();
        final UpsReading.Mode ups = menu.upsReading().mode();
        // A rack its UPS carries, or one it no longer protects, says so first: the power is the most urgent of all.
        final String pill;
        final int colour;
        if (ups == UpsReading.Mode.ON_BATTERY || ups == UpsReading.Mode.BYPASS) {
            final boolean battery = ups == UpsReading.Mode.ON_BATTERY;
            pill = GameText.resolve((battery ? ServerRackTexts.ON_BATTERY_PILL : ServerRackTexts.BYPASS_PILL)
                    .with(usedU, ROWS));
            colour = battery ? JsTechTheme.amber() : JsTechTheme.red();
        } else {
            pill = GameText.resolve(throttle < 100
                    ? ServerRackTexts.THROTTLED.with(usedU, ROWS, menu.temperature(), throttle)
                    : (linked ? ServerRackTexts.LINKED : ServerRackTexts.OFFLINE).with(usedU, ROWS));
            colour = throttle < 100 ? JsTechTheme.amber() : linked ? JsTechTheme.green() : JsTechTheme.red();
        }
        final int pillX = x + ServerRackLayout.HEADER_X + ServerRackLayout.HEADER_W - 4 - font.width(pill);
        JsTechTheme.text(g, font, pill, pillX, y + ServerRackLayout.TITLE_Y, colour);
        AssemblyPainter.drawLamp(g, pillX - 7, y + ServerRackLayout.TITLE_Y + 3, colour, true);
    }

    /* The cabinet's back between the posts, and the two posts with their holes. */
    private void drawCabinet(final GuiGraphics g, final int x, final int y, final int metal) {
        final int top = y + ServerRackLayout.ROW_Y0;
        final int bottom = top + ROWS * PITCH;
        g.fill(x + ServerRackLayout.CABINET_X, top - 2, x + ServerRackLayout.RAIL_R + 2, bottom,
                AssemblyPainter.mix(JsTechTheme.screen(), metal, 0.4f));
        final int post = AssemblyPainter.mix(metal, JsTechTheme.text(), 0.1f);
        for (final int postX : new int[] {ServerRackLayout.RAIL_L - ServerRackLayout.POST_W,
                ServerRackLayout.RAIL_R - 2}) {
            g.fill(x + postX, top - 2, x + postX + ServerRackLayout.POST_W, bottom, post);
            for (int row = 0; row < ROWS; row++) {
                for (final int dy : POST_HOLES) {
                    final int hy = top + row * PITCH + dy;
                    g.fill(x + postX + 1, hy, x + postX + 3, hy + 2, JsTechTheme.screen());
                }
            }
        }
    }

    /*
     * One mounted unit's faceplate: its own slot, the drive caddies its chassis cables, the vents over
     * the rest, and on a computer the power button with the bay's lamp and, while its array rebuilds, the progress.
     */
    private void drawUnit(final GuiGraphics g, final int x, final int y, final int row, final int heightU,
                          final IMountableRackUnit unit, final int face, final int mouseX, final int mouseY) {
        final int ry = y + ServerRackLayout.rowY(row);
        final int h = heightU * PITCH;
        final int left = x + ServerRackLayout.RAIL_L;
        final int right = x + ServerRackLayout.RAIL_R - 2;
        final int sheen = CABINET.get().sheen();
        g.fill(left, ry, right, ry + h - 1, face);
        g.fill(left, ry, right, ry + 1, AssemblyPainter.mix(face, sheen, 0.18f));
        JsTechTheme.slot(g, x + ServerRackLayout.serverItemX(), y + ServerRackLayout.itemY(row));
        final boolean computer = isComputer(row);
        final RackUnitItem.Kind kind = RackUnitItem.kindOf(menu.serverInBay(row));
        // A drive shelf's bays serve the computer above it, so they light with that computer's power.
        final int owner = kind == RackUnitItem.Kind.DRIVE_SHELF ? menu.extendedBy(row) : row;
        final boolean on = owner >= 0 && isComputer(owner) && menu.bayPowerOn(owner);
        // The vents across what the chassis does not cable, between its bays and what stands at its right end.
        final int cabled = unit.driveSlots();
        final int ventLeft = x + ServerRackLayout.frontItemX(Math.min(ServerRackLayout.FRONT_SLOTS, cabled)) + 2;
        final int ventRight = x + ventEnd(kind);
        if (kind == RackUnitItem.Kind.VENT_PANEL) {
            // A Vent Panel is perforated all across its face, its holes staggered row to row.
            final int hole = AssemblyPainter.mix(face, JsTechTheme.screen(), 0.6f);
            for (int vy = ry + 3, line = 0; vy < ry + h - 3; vy += 2, line++) {
                for (int vx = ventLeft + line % 2; vx < ventRight; vx += 2) {
                    g.fill(vx, vy, vx + 1, vy + 1, hole);
                }
            }
        } else if (ventLeft < ventRight - 4) {
            final int vent = AssemblyPainter.mix(face, sheen, 0.12f);
            for (int vy = ry + 3; vy < ry + h - 3; vy += 2) {
                g.fill(ventLeft, vy, ventRight, vy + 1, vent);
            }
            final int rebuild = computer ? menu.rebuildPermille(row) : 0;
            if (rebuild > 0) {
                g.fill(ventLeft, ry + h - 4, ventRight, ry + h - 2, JsTechTheme.track());
                g.fill(ventLeft, ry + h - 4, ventLeft + (ventRight - ventLeft) * rebuild / 1000, ry + h - 2,
                        JsTechTheme.amber());
            }
        }
        for (int r = row; r < row + heightU; r++) {
            for (int column = 0; column < ServerRackLayout.FRONT_SLOTS; column++) {
                drawBay(g, x, y, r, column, face, on);
            }
        }
        if (computer) {
            final int px = x + ServerRackLayout.PWR_X;
            final int py = y + ServerRackLayout.pwrY(row, heightU);
            JsTechTheme.button(g, px, py, ServerRackLayout.PWR_W, ServerRackLayout.PWR_H,
                    hover(mouseX, mouseY, ServerRackLayout.PWR_X, ServerRackLayout.pwrY(row, heightU),
                            ServerRackLayout.PWR_W, ServerRackLayout.PWR_H));
            final UnitState state = stateOf(row);
            AssemblyPainter.drawLamp(g, px + ServerRackLayout.PWR_LAMP_DX, py + 4, state.lamp(), on);
            JsTechTheme.textS(g, font, GameText.resolve(ServerRackTexts.POWER), px + ServerRackLayout.PWR_LABEL_DX,
                    py + 3, JsTechTheme.text());
        } else if (kind != null) {
            drawEquipment(g, x, y, row, heightU, kind, face);
        }
    }

    /* Where a unit's vents end: before its readout or its cells, or before a computer's power button. */
    private static int ventEnd(@Nullable final RackUnitItem.Kind kind) {
        if (kind == null) {
            return ServerRackLayout.PWR_X - 2;
        }
        return switch (kind) {
            case RACK_UPS -> ServerRackLayout.DISPLAY_X - 3;
            case UPS_BATTERY -> ServerRackLayout.CELLS_X - 3;
            // A PDU's faceplate is its outlets, a shelf's its bays: neither has vents.
            case PDU, DRIVE_SHELF -> 0;
            // A Vent Panel is all holes, to the rail.
            case VENT_PANEL -> ServerRackLayout.RAIL_R - 4;
            default -> ServerRackLayout.PWR_X - 2;
        };
    }

    /*
     * The faces of the rack's own power and storage: the Rack UPS's charge on its readout, a battery expansion's cells,
     * the PDU's outlets lit for the units drawing and the rack's whole draw, and the badge that says which server a
     * drive shelf extends. The KVM switch, the Cooling Unit and the fibre interface show only their vents.
     */
    private void drawEquipment(final GuiGraphics g, final int x, final int y, final int row, final int heightU,
                               final RackUnitItem.Kind kind, final int face) {
        final UpsReading ups = menu.upsReading();
        final int upsColour = upsColour(ups);
        switch (kind) {
            case RACK_UPS -> drawReadout(g, x, y, row, heightU, ups.present()
                    ? GameText.resolve(ServerRackTexts.PERCENT.with(ups.percent())) : "--", upsColour, true);
            case UPS_BATTERY -> {
                final int lit = ups.present() ? (ups.percent() * ServerRackLayout.CELLS + 99) / 100 : 0;
                final int cy = y + ServerRackLayout.rowY(row) + (heightU * PITCH - ServerRackLayout.CELL_H) / 2;
                for (int cell = 0; cell < ServerRackLayout.CELLS; cell++) {
                    final int cx = x + ServerRackLayout.CELLS_X + cell * ServerRackLayout.CELL_PITCH;
                    g.fill(cx, cy, cx + ServerRackLayout.CELL_W, cy + ServerRackLayout.CELL_H, cell < lit ? upsColour
                            : AssemblyPainter.mix(upsColour, JsTechTheme.screen(), 0.75f));
                }
            }
            case PDU -> {
                final int drawing = unitsDrawing();
                final int oy = y + ServerRackLayout.rowY(row) + (heightU * PITCH - ServerRackLayout.OUTLET_H) / 2 + 1;
                for (int index = 0; index < ServerRackLayout.OUTLETS; index++) {
                    final int ox = x + ServerRackLayout.outletX(index);
                    g.fill(ox, oy, ox + ServerRackLayout.OUTLET_W, oy + ServerRackLayout.OUTLET_H,
                            AssemblyPainter.mix(face, JsTechTheme.screen(), 0.5f));
                    AssemblyPainter.frame(g, ox, oy, ox + ServerRackLayout.OUTLET_W, oy + ServerRackLayout.OUTLET_H,
                            AssemblyPainter.mix(face, JsTechTheme.text(), 0.25f));
                    g.fill(ox + 2, oy + 2, ox + 3, oy + 4, JsTechTheme.dim());
                    g.fill(ox + 5, oy + 2, ox + 6, oy + 4, JsTechTheme.dim());
                    g.fill(ox + 3, oy - 3, ox + 5, oy - 2, index < drawing ? JsTechTheme.green()
                            : AssemblyPainter.mix(JsTechTheme.green(), JsTechTheme.screen(), 0.75f));
                }
                final UnitFormatter.Scaled watts = UnitFormatter.forCurrentLocale().si(menu.drawnWatts());
                drawReadout(g, x, y, row, heightU, GameText.resolve(ServerRackTexts.WATTS_SHORT.with(watts.number(),
                        watts.prefix())), JsTechTheme.amber(), false);
            }
            case DRIVE_SHELF -> {
                final int px = x + ServerRackLayout.PWR_X;
                final int py = y + ServerRackLayout.pwrY(row, heightU);
                final int server = menu.extendedBy(row);
                final int colour = server >= 0 ? JsTechTheme.accent() : JsTechTheme.dim();
                JsTechTheme.button(g, px, py, ServerRackLayout.PWR_W, ServerRackLayout.PWR_H, false);
                // An arrow up at the server whose bays it extends.
                g.fill(px + 4, py + 3, px + 5, py + 9, colour);
                g.fill(px + 3, py + 4, px + 6, py + 5, colour);
                JsTechTheme.textS(g, font, server >= 0 ? GameText.resolve(ServerRackTexts.UNIT.with(server + 1))
                        : "--", px + ServerRackLayout.PWR_LABEL_DX, py + 3, colour);
            }
            case TAPE_LIBRARY -> {
                // The drive at the right end: its lamp amber while it winds, green while it reads or writes.
                final int px = x + ServerRackLayout.PWR_X;
                final int py = y + ServerRackLayout.pwrY(row, heightU);
                final TapeDrive.State state = menu.tapeDrive(row);
                JsTechTheme.button(g, px, py, ServerRackLayout.PWR_W, ServerRackLayout.PWR_H, false);
                AssemblyPainter.drawLamp(g, px + ServerRackLayout.PWR_LAMP_DX, py + 4, driveColour(state),
                        state != TapeDrive.State.IDLE);
                JsTechTheme.textS(g, font, GameText.resolve(ServerRackTexts.TAPE_DRIVE),
                        px + ServerRackLayout.PWR_LABEL_DX, py + 3, JsTechTheme.text());
            }
            default -> {
            }
        }
    }

    /* The colour a tape drive's lamp and card line take: amber winding, green at work, dim with nothing to do. */
    private static int driveColour(final TapeDrive.State state) {
        return switch (state) {
            case IDLE -> JsTechTheme.dim();
            case WINDING -> JsTechTheme.amber();
            case READING, WRITING -> JsTechTheme.green();
        };
    }

    /* A readout at a unit's right end: its words in {@code colour}, and a lamp of the same beside them. */
    private void drawReadout(final GuiGraphics g, final int x, final int y, final int row, final int heightU,
                             final String text, final int colour, final boolean lamp) {
        final int dx = x + ServerRackLayout.DISPLAY_X;
        final int dy = y + ServerRackLayout.displayY(row, heightU);
        final int right = dx + ServerRackLayout.DISPLAY_W;
        g.fill(dx, dy, right, dy + ServerRackLayout.DISPLAY_H, JsTechTheme.track());
        AssemblyPainter.frame(g, dx, dy, right, dy + ServerRackLayout.DISPLAY_H, JsTechTheme.line());
        JsTechTheme.textS(g, font, text, dx + 3, dy + 3, colour);
        if (lamp) {
            g.fill(right - 6, dy + 3, right - 2, dy + 7, colour);
        }
    }

    /* How many of the rack's units draw from the line now: its powered machines, and its UPS while it charges. */
    private int unitsDrawing() {
        int drawing = 0;
        for (int row = 0; row < ROWS; row++) {
            if (isComputer(row) && menu.bayWatts(row) > 0) {
                drawing++;
            }
        }
        final UpsReading ups = menu.upsReading();
        return drawing + (ups.present() && !ups.onBattery() && ups.permille() < 1_000 ? 1 : 0);
    }

    /* The colour a UPS's state is shown in: green on the line, amber on its battery, red in bypass, dim for none. */
    private static int upsColour(final UpsReading ups) {
        return switch (ups.mode()) {
            case ON_LINE -> JsTechTheme.green();
            case ON_BATTERY -> JsTechTheme.amber();
            case BYPASS -> JsTechTheme.red();
            case NONE -> JsTechTheme.dim();
        };
    }

    /*
     * One front-panel bay of a unit: a drive caddy with its handle and its lamp, the shape of a drive while it is
     * empty; a bay the chassis does not cable shows only while it still holds
     * something, with a red lamp.
     */
    private void drawBay(final GuiGraphics g, final int x, final int y, final int row, final int column,
                         final int face, final boolean on) {
        final int index = row * RackLayout.SLOTS_PER_U + column;
        final int cx = x + ServerRackLayout.frontItemX(column);
        final int cy = y + ServerRackLayout.itemY(row);
        final ItemStack stack = menu.frontSlotStack(index);
        switch (menu.frontSlotRole(index)) {
            case DRIVE -> {
                JsTechTheme.slot(g, cx, cy);
                if (stack.isEmpty()) {
                    AssemblyPainter.drawGhost(g, cx, cy, SeatKind.DISK, JsTechTheme.dim(), GHOST_ALPHA);
                }
                g.fill(cx, cy + 14, cx + 16, cy + 16, AssemblyPainter.mix(face, JsTechTheme.text(), 0.2f));
                final boolean lit = on && !stack.isEmpty();
                g.fill(cx + 13, cy + 1, cx + 15, cy + 3,
                        lit ? JsTechTheme.green() : AssemblyPainter.mix(JsTechTheme.green(), JsTechTheme.screen(), 0.75f));
            }
            // A Tape Library's slot is a plain slot for a tape: its drive, not the slot, shows what it does.
            case TAPE -> JsTechTheme.slot(g, cx, cy);
            case BLOCKED_NO_UNIT, BLOCKED_BUDGET -> {
                if (!stack.isEmpty()) {
                    JsTechTheme.slot(g, cx, cy);
                    g.fill(cx + 13, cy + 1, cx + 15, cy + 3, JsTechTheme.red());
                }
            }
        }
    }

    /* The card of the selected unit: its rack unit, its name, then a line each for what the player reads off it. */
    private void drawCard(final GuiGraphics g, final int x, final int y, final int top) {
        final int cx = x + ServerRackLayout.CARD_X;
        final int cy = y + ServerRackLayout.CARD_Y;
        final int w = ServerRackLayout.CARD_W;
        JsTechTheme.panel(g, cx, cy, w, ServerRackLayout.CARD_H);
        JsTechTheme.textS(g, font, GameText.resolve(ServerRackTexts.UNIT.with(Math.max(top, 0) + 1)), cx + 3, cy + 3,
                JsTechTheme.dim());
        final String name = top < 0 ? GameText.resolve(ServerRackTexts.NO_UNIT) : unitName(top);
        JsTechTheme.text(g, font, Texts.clip(font, name, w - 6), cx + 3, cy + 10,
                top < 0 ? JsTechTheme.dim() : JsTechTheme.text());
        g.fill(cx + 3, cy + 21, cx + w - 3, cy + 22, JsTechTheme.line());
        final List<CardLine> lines = cardLines(top);
        for (int i = 0; i < lines.size(); i++) {
            final CardLine line = lines.get(i);
            final int ly = y + ServerRackLayout.cardLineY(i);
            final String label = GameText.resolve(line.label());
            JsTechTheme.textS(g, font, JsTechTheme.widthS(font, label) <= w - 6 ? label
                    : Texts.clip(font, label, (int) ((w - 6) / JsTechTheme.small())), cx + 3, ly, JsTechTheme.dim());
            if (font.width(line.value()) <= w - 6) {
                JsTechTheme.textRight(g, font, line.value(), cx + w - 3, ly + 7, line.colour());
            } else {
                final String small = JsTechTheme.widthS(font, line.value()) <= w - 6 ? line.value()
                        : Texts.clip(font, line.value(), (int) ((w - 6) / JsTechTheme.small()));
                JsTechTheme.textSRight(g, font, small, cx + w - 3, ly + 8, line.colour());
            }
        }
    }

    /* What the card says of the unit topped at top, or its empty lines when no unit is picked. */
    private List<CardLine> cardLines(final int top) {
        if (top < 0) {
            final int text = JsTechTheme.text();
            return List.of(new CardLine(ServerRackTexts.CARD_STATE, "--", text),
                    new CardLine(ServerRackTexts.CARD_BOOT, "--", text),
                    new CardLine(ServerRackTexts.CARD_SYSTEM, "--", text),
                    new CardLine(ServerRackTexts.CARD_NETWORK, "--", text));
        }
        final RackUnitItem.Kind kind = RackUnitItem.kindOf(menu.serverInBay(top));
        if (kind != null) {
            return switch (kind) {
                case RACK_UPS -> upsLines();
                case UPS_BATTERY -> expansionLines();
                case PDU -> pduLines();
                case DRIVE_SHELF -> shelfLines(top);
                case COOLING_UNIT, VENT_PANEL -> coolingLines(top);
                case TAPE_LIBRARY -> tapeLines(top);
                default -> List.of();
            };
        }
        final UnitState state = stateOf(top);
        final int disk = menu.bootDisk(top);
        final OsDef system = menu.system(top);
        final boolean linked = menu.networkLinked();
        return List.of(
                new CardLine(ServerRackTexts.CARD_STATE, GameText.resolve(state.word()), state.colour()),
                new CardLine(ServerRackTexts.CARD_BOOT, disk < 0 ? "--" : GameText.resolve(ServerRackTexts.BOOT_DISK
                        .with(disk)), disk < 0 ? JsTechTheme.dim() : JsTechTheme.text()),
                new CardLine(ServerRackTexts.CARD_SYSTEM, system == null ? GameText.resolve(ServerRackTexts.NONE)
                        : system.displayName(), system == null ? JsTechTheme.dim() : JsTechTheme.text()),
                new CardLine(ServerRackTexts.CARD_NETWORK, linked ? GameText.resolve(ServerRackTexts.NETWORK_LINKED)
                        : "--", linked ? JsTechTheme.accent() : JsTechTheme.dim()),
                raidLine(top),
                // A hot cabinet's temperature is what its machines most need said: it takes the KVM's line.
                menu.temperature() >= ThermalModel.FULL_UP_TO ? tempLine() : kvmLine(top));
    }

    /*
     * A Cooling Unit or a Vent Panel: what it adds to the budget, the cabinet's budget against what its machines make,
     * its one temperature, and the share of their clock that leaves its machines
     * (computers/docs/design/temperature.md).
     */
    private List<CardLine> coolingLines(final int top) {
        final int throttle = menu.throttlePercent();
        final int load = menu.thermalLoadWatts();
        final int budget = menu.thermalBudgetWatts();
        return List.of(
                new CardLine(ServerRackTexts.CARD_ADDS, GameText.resolve(ServerRackTexts.ADDS_WATTS.with(
                        JsTechTheme.fmt(RackUnitItem.coolingWattsOf(menu.serverInBay(top))))), JsTechTheme.green()),
                new CardLine(ServerRackTexts.CARD_BUDGET, watts(budget), JsTechTheme.text()),
                new CardLine(ServerRackTexts.CARD_LOAD, watts(load), load > budget ? JsTechTheme.amber()
                        : JsTechTheme.text()),
                new CardLine(ServerRackTexts.CARD_TEMP, GameText.resolve(ServerRackTexts.DEGREES.with(
                        menu.temperature())), tempColour(menu.temperature())),
                new CardLine(ServerRackTexts.CARD_MACHINES, GameText.resolve(ServerRackTexts.MACHINES_AT.with(
                        throttle)), throttle < 100 ? JsTechTheme.amber() : JsTechTheme.text()));
    }

    /* The cabinet's temperature on a server's card, and the share of its clock it leaves while it throttles. */
    private CardLine tempLine() {
        final int throttle = menu.throttlePercent();
        return new CardLine(ServerRackTexts.CARD_TEMP, GameText.resolve(throttle < 100
                ? ServerRackTexts.DEGREES_AT.with(menu.temperature(), throttle)
                : ServerRackTexts.DEGREES.with(menu.temperature())), tempColour(menu.temperature()));
    }

    /* The colour of a temperature: green up to 80 °C, amber to 90, red from there. */
    private static int tempColour(final int celsius) {
        return celsius >= ThermalModel.THROTTLE_AT ? JsTechTheme.red()
                : celsius >= ThermalModel.FULL_UP_TO ? JsTechTheme.amber() : JsTechTheme.green();
    }

    /* The Rack UPS: where the power comes from, its charge, what it holds, the load, how long it lasts, its end. */
    private List<CardLine> upsLines() {
        final UpsReading ups = menu.upsReading();
        final UpsRating rating = menu.upsRating();
        final int colour = upsColour(ups);
        final TextKey state = switch (ups.mode()) {
            case ON_BATTERY -> ServerRackTexts.UPS_ON_BATTERY;
            case BYPASS -> ServerRackTexts.UPS_BYPASS;
            default -> ServerRackTexts.UPS_ON_LINE;
        };
        final int load = menu.drawnWatts();
        return List.of(
                new CardLine(ServerRackTexts.CARD_STATE, ups.present() ? GameText.resolve(state) : "--", colour),
                new CardLine(ServerRackTexts.CARD_CHARGE, GameText.resolve(ServerRackTexts.PERCENT.with(ups.percent())),
                        colour),
                new CardLine(ServerRackTexts.CARD_HOLDS, rating == null ? "--"
                        : GameText.resolve(JsEnergy.ENERGY.amount(rating.batteryJoules())), JsTechTheme.text()),
                new CardLine(ServerRackTexts.CARD_LOAD, watts(load), JsTechTheme.text()),
                new CardLine(ServerRackTexts.CARD_LASTS, load <= 0 ? "--"
                        : GameText.resolve(ServerRackTexts.MINUTES.with(ups.minutesLeft())), JsTechTheme.text()),
                new CardLine(ServerRackTexts.CARD_SHUTDOWN, GameText.resolve(ServerRackTexts.SHUTDOWN_AT),
                        JsTechTheme.dim()));
    }

    /* A UPS Battery Expansion: what it adds, the Rack UPS it adds to, and that UPS's charge. */
    private List<CardLine> expansionLines() {
        final UpsReading ups = menu.upsReading();
        final int upsRow = firstOf(RackUnitItem.Kind.RACK_UPS);
        return List.of(
                new CardLine(ServerRackTexts.CARD_ADDS, GameText.resolve(JsEnergy.ENERGY.amount(
                        UpsRating.EXPANSION_JOULES)), JsTechTheme.text()),
                new CardLine(ServerRackTexts.CARD_UPS, upsRow < 0 ? GameText.resolve(ServerRackTexts.NONE)
                        : GameText.resolve(ServerRackTexts.UNIT.with(upsRow + 1)),
                        upsRow < 0 ? JsTechTheme.amber() : JsTechTheme.accent()),
                new CardLine(ServerRackTexts.CARD_CHARGE, ups.present()
                        ? GameText.resolve(ServerRackTexts.PERCENT.with(ups.percent())) : "--", upsColour(ups)));
    }

    /*
     * The PDU: the rack's whole draw, then what each unit draws (the UPS by whether it charges), as many as the card
     * holds, and who switches the bays.
     */
    private List<CardLine> pduLines() {
        final List<CardLine> units = new ArrayList<>();
        final int upsRow = firstOf(RackUnitItem.Kind.RACK_UPS);
        if (upsRow >= 0) {
            final UpsReading ups = menu.upsReading();
            final TextKey charging = ups.onBattery() ? ServerRackTexts.UPS_DRAINING
                    : ups.permille() >= 1_000 ? ServerRackTexts.UPS_CHARGED : ServerRackTexts.UPS_CHARGING;
            units.add(new CardLine(ServerRackTexts.UNIT_NAMED.with(upsRow + 1, ServerRackTexts.CARD_UPS.text()),
                    GameText.resolve(charging), upsColour(ups)));
        }
        for (int row = 0; row < ROWS; row++) {
            if (isComputer(row)) {
                final boolean on = menu.bayPowerOn(row);
                units.add(new CardLine(ServerRackTexts.UNIT_NAMED.with(row + 1, unitName(row)),
                        on ? watts(menu.bayWatts(row)) : GameText.resolve(ServerRackTexts.OFF),
                        on ? JsTechTheme.text() : JsTechTheme.dim()));
            }
        }
        final List<CardLine> lines = new ArrayList<>();
        lines.add(new CardLine(ServerRackTexts.CARD_TOTAL, watts(menu.drawnWatts()), JsTechTheme.text()));
        final int room = ServerRackLayout.CARD_LINES - 2;
        if (units.size() <= room) {
            lines.addAll(units);
        } else {
            lines.addAll(units.subList(0, room - 1));
            lines.add(new CardLine(ServerRackTexts.MORE_UNITS.with(units.size() - room + 1), "", JsTechTheme.dim()));
        }
        lines.add(new CardLine(ServerRackTexts.CARD_POWER, GameText.resolve(ServerRackTexts.IQL_JOBS),
                JsTechTheme.accent()));
        return lines;
    }

    /*
     * A drive shelf: the server it extends, or nothing; that server's bays and its own; the drives in its own; and the
     * array they join.
     */
    private List<CardLine> shelfLines(final int top) {
        final int server = menu.extendedBy(top);
        final int bays = menu.unitHeightAt(top) * RackLayout.SLOTS_PER_U;
        final String drives = GameText.resolve(ServerRackTexts.OF_TOTAL.with(menu.drivesIn(top), bays));
        if (server < 0) {
            return List.of(new CardLine(ServerRackTexts.CARD_EXTENDS, GameText.resolve(ServerRackTexts.NOTHING),
                            JsTechTheme.amber()),
                    new CardLine(ServerRackTexts.CARD_BAYS, String.valueOf(bays), JsTechTheme.text()),
                    new CardLine(ServerRackTexts.CARD_DRIVES, drives, JsTechTheme.text()));
        }
        final RackChassis chassis = ServerItem.chassisOf(menu.serverInBay(server));
        final int own = chassis == null ? 0 : chassis.driveSlots();
        final boolean array = menu.raidLabel(server) != null;
        return List.of(
                new CardLine(ServerRackTexts.CARD_EXTENDS, GameText.resolve(ServerRackTexts.UNIT_NAMED.with(server + 1,
                        unitName(server))), JsTechTheme.accent()),
                new CardLine(ServerRackTexts.CARD_BAYS, GameText.resolve(ServerRackTexts.BAYS_PLUS.with(own, bays)),
                        JsTechTheme.text()),
                new CardLine(ServerRackTexts.CARD_DRIVES, drives, JsTechTheme.text()),
                new CardLine(ServerRackTexts.CARD_ARRAY, GameText.resolve(array
                        ? ServerRackTexts.WITH_UNIT.with(server + 1) : ServerRackTexts.NONE.text()),
                        array ? JsTechTheme.text() : JsTechTheme.dim()));
    }

    /*
     * A Tape Library: its tapes against its slots, what they hold and what is written on them, how many backups, what
     * its drive is doing, and how many of the network's items the latest backup on them keeps.
     */
    private List<CardLine> tapeLines(final int top) {
        final List<ItemStack> slots = menu.tapesIn(top);
        int tapes = 0;
        long holdsMb = 0L;
        long usedMb = 0L;
        int backups = 0;
        TapeContents.Spot latest = null;
        for (final ItemStack stack : slots) {
            final TapeFormat format = TapeItem.formatOf(stack);
            if (format == null) {
                continue;
            }
            final TapeContents contents = TapeItem.contents(stack);
            tapes++;
            holdsMb += format.capacityMb();
            usedMb += contents.used() * EraStorage.mbPerItem(format.era());
            backups += contents.spots().size();
            for (final TapeContents.Spot spot : contents.spots()) {
                if (spot.items() > 0L && (latest == null || spot.madeAt() > latest.madeAt())) {
                    latest = spot;
                }
            }
        }
        final TapeDrive.State drive = menu.tapeDrive(top);
        final int text = JsTechTheme.text();
        return List.of(
                new CardLine(ServerRackTexts.CARD_TAPES, GameText.resolve(ServerRackTexts.OF_TOTAL.with(tapes,
                        slots.size())), text),
                new CardLine(ServerRackTexts.CARD_HOLDS, tapes == 0 ? "--" : DiskSpec.sizeLabel(holdsMb), text),
                new CardLine(ServerRackTexts.CARD_USED, tapes == 0 ? "--" : DiskSpec.sizeLabel(usedMb), text),
                new CardLine(ServerRackTexts.CARD_BACKUPS, String.valueOf(backups), text),
                new CardLine(ServerRackTexts.CARD_DRIVE, GameText.resolve(ServerRackTexts.driveWord(drive)),
                        driveColour(drive)),
                new CardLine(ServerRackTexts.CARD_ITEMS, latest == null ? "--"
                        : String.format(Locale.ROOT, "%,d", latest.items()),
                        latest == null ? JsTechTheme.dim() : text));
    }

    /* The top row of the first unit of {@code kind} in the rack, or -1 when it has none. */
    private int firstOf(final RackUnitItem.Kind kind) {
        for (int row = 0; row < ROWS; row++) {
            if (RackUnitItem.is(menu.serverInBay(row), kind)) {
                return row;
            }
        }
        return -1;
    }

    /* Watts as the card writes them, grouped: "1,240 W". */
    private static String watts(final int watts) {
        return GameText.resolve(ServerRackTexts.WATTS.with(UnitFormatter.forCurrentLocale().decimal(watts)));
    }

    /* The array: rebuilding with its progress, its level and health, or none. */
    private CardLine raidLine(final int top) {
        final int rebuild = menu.rebuildPermille(top);
        if (rebuild > 0) {
            return new CardLine(ServerRackTexts.CARD_RAID, GameText.resolve(ServerRackTexts.REBUILD.with(rebuild / 10)),
                    JsTechTheme.amber());
        }
        final Text raid = menu.raidLabel(top);
        if (raid == null) {
            return new CardLine(ServerRackTexts.CARD_RAID, GameText.resolve(ServerRackTexts.NONE), JsTechTheme.dim());
        }
        final int colour = switch (menu.raidHealth(top)) {
            case FAILED -> JsTechTheme.red();
            case DEGRADED -> JsTechTheme.amber();
            case NONE, HEALTHY -> JsTechTheme.green();
        };
        return new CardLine(ServerRackTexts.CARD_RAID, GameText.resolve(raid), colour);
    }

    /* What a linked monitor shows: this unit, another, or nothing when two machines share the rack with no switch. */
    private CardLine kvmLine(final int top) {
        final int screen = menu.screenRow();
        if (screen == top) {
            return new CardLine(ServerRackTexts.CARD_KVM, GameText.resolve(ServerRackTexts.THIS_UNIT), JsTechTheme.text());
        }
        if (screen < 0) {
            return new CardLine(ServerRackTexts.CARD_KVM, GameText.resolve(ServerRackTexts.NO_SWITCH),
                    JsTechTheme.amber());
        }
        return new CardLine(ServerRackTexts.CARD_KVM, GameText.resolve(ServerRackTexts.SHOWS_UNIT.with(screen + 1)),
                JsTechTheme.dim());
    }

    /* A computer's state as its lamp and the card show it: unbuilt, switched off, on the network, or up alone. */
    private UnitState stateOf(final int top) {
        final ComputerBuild build = ServerItem.build(menu.serverInBay(top));
        if (build == null) {
            return new UnitState(ServerRackTexts.INCOMPLETE, JsTechTheme.red(), JsTechTheme.red());
        }
        if (!menu.bayPowerOn(top)) {
            return new UnitState(ServerRackTexts.OFF, JsTechTheme.dim(), JsTechTheme.amber());
        }
        // A cabinet that holds its machines at 90 °C slows each of them: the server says so first.
        if (menu.temperature() >= ThermalModel.THROTTLE_AT && menu.throttlePercent() < 100) {
            return new UnitState(ServerRackTexts.THROTTLED_STATE, JsTechTheme.amber(), JsTechTheme.amber());
        }
        return menu.networkLinked() ? new UnitState(ServerRackTexts.ONLINE, JsTechTheme.green(), JsTechTheme.green())
                : new UnitState(ServerRackTexts.READY, JsTechTheme.amber(), JsTechTheme.amber());
    }

    /* The name the player gave the machine, else its item's. */
    private String unitName(final int top) {
        final ItemStack server = menu.serverInBay(top);
        final String custom = ServerItem.customName(server);
        return custom.isEmpty() ? server.getHoverName().getString() : custom;
    }

    private void renderRackTooltip(final GuiGraphics g, final int mouseX, final int mouseY) {
        if (hoveredSlot != null && hoveredSlot.hasItem()) {
            return; // the item's own tooltip already shows
        }
        final int relX = mouseX - leftPos;
        final int relY = mouseY - topPos;
        if (relX < ServerRackLayout.CABINET_X || relX >= ServerRackLayout.RAIL_R + 2 || relY < ServerRackLayout.ROW_Y0
                || relY >= ServerRackLayout.ROW_Y0 + ROWS * PITCH) {
            return;
        }
        /*
         * A chassis this cabinet does not seat says so the moment it is carried over the cabinet, instead of the slot
         * silently refusing it, so the player learns which cabinet it belongs in before letting go.
         */
        final ItemStack carried = menu.getCarried();
        if (!carried.isEmpty()) {
            final RackChassis carriedChassis = ServerItem.chassisOf(carried);
            final ServerRackBlockEntity rack = Minecraft.getInstance().level != null
                    && Minecraft.getInstance().level.getBlockEntity(menu.rackPos()) instanceof ServerRackBlockEntity found
                    ? found : null;
            if (carriedChassis != null && rack != null && !rack.acceptsChassis(carried)) {
                // The right kind of cabinet refuses only a chassis of a later era than its own.
                final TextKey belongs = carriedChassis.rackType() == rack.rackType() ? ServerRackTexts.NEWER_THAN_RACK
                        : switch (carriedChassis.rackType()) {
                            case SERVER -> ServerRackTexts.BELONGS_IN_SERVER_RACK;
                            case SUPERCOMPUTER -> ServerRackTexts.BELONGS_IN_SUPERCOMPUTER_RACK;
                            case AI -> ServerRackTexts.BELONGS_IN_AI_RACK;
                        };
                g.renderTooltip(font, GameText.component(belongs), mouseX, mouseY);
            }
            return;
        }
        // A drive shelf with no server right above it says so, and where it has to go.
        final int hoveredRow = rowAt(relX, relY);
        final int hoveredTop = hoveredRow < 0 ? -1 : menu.unitTopOf(hoveredRow);
        if (hoveredTop >= 0 && RackUnitItem.is(menu.serverInBay(hoveredTop), RackUnitItem.Kind.DRIVE_SHELF)
                && menu.extendedBy(hoveredTop) < 0) {
            g.renderComponentTooltip(font, List.of(
                    GameText.component(ServerRackTexts.SHELF_ALONE).withStyle(ChatFormatting.GOLD),
                    GameText.component(ServerRackTexts.SHELF_MOUNT)), mouseX, mouseY);
            return;
        }
        // An empty bay explains itself: what goes in it, and whether it belongs to an array.
        final int index = hoveredSlot == null ? -1 : frontIndex(hoveredSlot);
        if (index >= 0) {
            final RackLayout.SlotRole role = menu.frontSlotRole(index);
            if (role == RackLayout.SlotRole.DRIVE) {
                final int top = menu.unitTopOf(index / RackLayout.SLOTS_PER_U);
                final Text raid = top < 0 ? null : menu.raidLabel(top);
                g.renderTooltip(font, GameText.component(raid != null ? ServerRackTexts.ARRAY_MEMBER.with(raid)
                        : ServerRackTexts.DRIVE_BAY.text()), mouseX, mouseY);
            } else if (role == RackLayout.SlotRole.TAPE) {
                g.renderTooltip(font, GameText.component(ServerRackTexts.TAPE_SLOT), mouseX, mouseY);
            }
            return;
        }
        // The rest of a computer's faceplate shows the machine's summary.
        final int row = rowAt(relX, relY);
        final int top = row < 0 ? -1 : menu.unitTopOf(row);
        if (hoveredSlot != null || !isComputer(top) || onPower(top, relX, relY)) {
            return;
        }
        final ItemStack server = menu.serverInBay(top);
        final List<Component> lines = new ArrayList<>();
        final UUID uuid = ServerItem.nodeUuid(server);
        lines.add(GameText.component(uuid != null ? ServerRackTexts.NODE.with(uuid.toString().substring(0, 8))
                : ServerRackTexts.UNASSIGNED_NODE.text()));
        final RackChassis chassis = ServerItem.chassisOf(server);
        if (chassis != null) {
            lines.add(GameText.component(ServerRackTexts.CHASSIS.with(chassis.heightU(), chassis.driveSlots()))
                    .withStyle(ChatFormatting.GRAY));
        }
        if (ServerItem.build(server) == null) {
            lines.add(GameText.component(ServerRackTexts.NEEDS_BOARD).withStyle(ChatFormatting.RED));
        } else if (!menu.networkLinked()) {
            lines.add(GameText.component(ServerRackTexts.NOT_ON_NETWORK).withStyle(ChatFormatting.YELLOW));
        }
        g.renderComponentTooltip(font, lines, mouseX, mouseY);
    }

    /*
     * The selected unit, or, when the selection holds none any more, the topmost computer, else the topmost unit the
     * card describes; -1 when the rack has none.
     */
    private int selectedUnit() {
        if (selectable(selected)) {
            return selected;
        }
        int first = -1;
        for (int row = 0; row < ROWS; row++) {
            if (isComputer(row)) {
                selected = row;
                return row;
            }
            if (first < 0 && selectable(row)) {
                first = row;
            }
        }
        if (first >= 0) {
            selected = first;
        }
        return first;
    }

    /* Whether the card describes the unit topped at row: a computer, or the rack's power, storage and cooling. */
    private boolean selectable(final int row) {
        if (isComputer(row)) {
            return true;
        }
        final RackUnitItem.Kind kind = row < 0 || row >= ROWS ? null : RackUnitItem.kindOf(menu.serverInBay(row));
        return kind == RackUnitItem.Kind.RACK_UPS || kind == RackUnitItem.Kind.UPS_BATTERY
                || kind == RackUnitItem.Kind.PDU || kind == RackUnitItem.Kind.DRIVE_SHELF
                || kind == RackUnitItem.Kind.COOLING_UNIT || kind == RackUnitItem.Kind.VENT_PANEL
                || kind == RackUnitItem.Kind.TAPE_LIBRARY;
    }

    /* Whether a computer is mounted with its top at row. */
    private boolean isComputer(final int row) {
        return row >= 0 && row < ROWS && menu.serverInBay(row).getItem() instanceof ServerItem;
    }

    /* The rack unit under a window-relative point between the posts, or -1. */
    private static int rowAt(final int relX, final int relY) {
        if (relX < ServerRackLayout.RAIL_L || relX >= ServerRackLayout.RAIL_R - 2 || relY < ServerRackLayout.ROW_Y0) {
            return -1;
        }
        final int row = (relY - ServerRackLayout.ROW_Y0) / PITCH;
        return row < ROWS ? row : -1;
    }

    /* Whether a window-relative point is on the power button of the computer topped at top. */
    private boolean onPower(final int top, final int relX, final int relY) {
        final int py = ServerRackLayout.pwrY(top, menu.unitHeightAt(top));
        return relX >= ServerRackLayout.PWR_X && relX < ServerRackLayout.PWR_X + ServerRackLayout.PWR_W
                && relY >= py && relY < py + ServerRackLayout.PWR_H;
    }

    /* The front-panel index of one of the menu's slots, or -1 when it is not a front slot. */
    private static int frontIndex(final Slot slot) {
        final int index = slot.index - ServerRackMenu.RACK_SLOTS;
        return index >= 0 && index < ServerRackMenu.FRONT_SLOT_COUNT ? index : -1;
    }

    /**
     * The cabinet's metal and its units' faceplates, one of each per era, and the light their edges catch.
     */
    private record Cabinet(int vintageMetal, int legacyMetal, int transitionMetal, int standardMetal, int advancedMetal,
                           int vintageFace, int legacyFace, int transitionFace, int standardFace, int advancedFace,
                           int sheen) {

        int metal(final Era era) {
            return HardwareEra.pick(era, vintageMetal, legacyMetal, transitionMetal, standardMetal, advancedMetal);
        }

        int face(final Era era) {
            return HardwareEra.pick(era, vintageFace, legacyFace, transitionFace, standardFace, advancedFace);
        }
    }

    /** One line of the card: its label, and its value in its colour. */
    private record CardLine(Text label, String value, int colour) {

        CardLine(final TextKey label, final String value, final int colour) {
            this(label.text(), value, colour);
        }
    }

    /** A computer's state: its word on the card and that word's colour, and its power lamp's colour. */
    private record UnitState(TextKey word, int colour, int lamp) {
    }
}
