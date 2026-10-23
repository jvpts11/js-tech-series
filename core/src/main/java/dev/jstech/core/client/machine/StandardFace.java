/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.machine;

import static dev.jstech.core.machine.MachineScreenLayout.GLASS_HEIGHT;
import static dev.jstech.core.machine.MachineScreenLayout.GLASS_WIDTH;
import static dev.jstech.core.machine.MachineScreenLayout.GLASS_X;
import static dev.jstech.core.machine.MachineScreenLayout.GLASS_Y;
import static dev.jstech.core.machine.MachineScreenLayout.LANE_PITCH;
import static dev.jstech.core.machine.MachineScreenLayout.LCD_HEIGHT;
import static dev.jstech.core.machine.MachineScreenLayout.LCD_WIDTH;
import static dev.jstech.core.machine.MachineScreenLayout.LCD_X;
import static dev.jstech.core.machine.MachineScreenLayout.LCD_Y;
import static dev.jstech.core.machine.MachineScreenLayout.METER_HEIGHT;
import static dev.jstech.core.machine.MachineScreenLayout.METER_WIDTH;
import static dev.jstech.core.machine.MachineScreenLayout.METER_X;
import static dev.jstech.core.machine.MachineScreenLayout.METER_Y;
import static dev.jstech.core.machine.MachineScreenLayout.PROCESS_BOTTOM;
import static dev.jstech.core.machine.MachineScreenLayout.PROCESS_RIGHT;
import static dev.jstech.core.machine.MachineScreenLayout.PROCESS_X;
import static dev.jstech.core.machine.MachineScreenLayout.PROCESS_Y;
import static dev.jstech.core.machine.MachineScreenLayout.SWITCH_HEIGHT;
import static dev.jstech.core.machine.MachineScreenLayout.SWITCH_WIDTH;
import static dev.jstech.core.machine.MachineScreenLayout.SWITCH_X;
import static dev.jstech.core.machine.MachineScreenLayout.SWITCH_Y;

import com.mojang.blaze3d.platform.InputConstants;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.energy.Energy;
import dev.jstech.core.format.UnitFormatter;
import dev.jstech.core.machine.MachineBlockEntity;
import dev.jstech.core.machine.MachineHistory;
import dev.jstech.core.machine.MachineMenu;
import dev.jstech.core.machine.MachineScreenLayout;
import dev.jstech.core.machine.MachineScreenTexts;
import dev.jstech.core.machine.MachineStatus;
import dev.jstech.core.machine.ProcessingMachineBlockEntity;
import dev.jstech.core.material.MatterState;
import dev.jstech.core.matter.MatterStack;
import dev.jstech.core.matter.MatterTank;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/**
 * The face every machine shows on its Main tab unless its mod gave it one of its own, as the approved design draws
 * its parts: a panel meter of the energy it draws, ranging itself to the most it has drawn; an LCD of what it made the
 * last minute, how far through its work it is and what it draws, or what keeps it waiting; the process window with
 * its slots, each lane's inputs, an arrow lit as far as its work has gone, and its outputs; its switch; and a sight
 * glass of its first tank, or of its stored energy.
 *
 * <p>A click on the LCD turns it to its page of statistics and back: how much of the last minute the machine worked,
 * and a chart of its last ten minutes, what it made a minute as green columns and what it drew as amber dots, told in
 * full, with the last thing that jammed it, in the LCD's tooltip.
 *
 * <p>An input slot locked to an item shows the item faded while it is empty; Ctrl and a click locks it to what it
 * holds, or unlocks it.
 */
public final class StandardFace implements IMachineFace {

    private long peak;
    /* Whether the LCD shows its page of statistics, the last ten minutes, rather than its readings. */
    private boolean statistics;

    private static final List<String> MARKS = List.of("0", "1", "2", "3", "4");
    private static final int TILE = 8;
    private static final int OVER_ITEMS = 200;
    private static final int GHOST = 0x90 << 24 | 0x8B8B8B;
    private static final int DEEP = 0;
    private static final int DEEPEST = 64;
    private static final int PERCENT = 100;
    private static final int LABEL_ROOM = 22;
    private static final int METER_LABEL_ROOM = 36;
    private static final int SECONDS_A_MINUTE = 60;
    private static final int MINUTES = MachineHistory.READINGS * MachineHistory.READING_SECONDS / SECONDS_A_MINUTE;
    private static final int TICKS_A_SECOND = 20;
    private static final int RGB = 0xFFFFFF;

    @Override
    public void draw(final MachineView v) {
        final GuiGraphics g = v.g();
        final MachineBlockEntity machine = v.machine();
        final Energy energy = machine.power().energy();
        final long drawn = machine.drawn();
        this.peak = Math.max(this.peak, drawn);
        final long mark = MachineScreenLayout.meterMark(this.peak);
        MachineArt.panelMeter(g, v.font(), v.x(METER_X), v.y(METER_Y), METER_WIDTH, METER_HEIGHT,
                drawn / (double) (MachineScreenLayout.METER_MARKS * mark), MARKS, true);
        MachineArt.engrave(g, v.font(), MachineArt.fit(v.font(), GameText.resolve(MachineScreenTexts.METER_RANGE.with(
                energy.rate(mark))), METER_LABEL_ROOM), v.x(METER_X + METER_WIDTH / 2), v.y(METER_Y + METER_HEIGHT + 8),
                true);
        if (this.statistics) {
            final MachineHistory history = v.menu().history();
            MachineArt.lcdChart(g, v.font(), v.x(LCD_X), v.y(LCD_Y), LCD_WIDTH, LCD_HEIGHT,
                    GameText.resolve(MachineScreenTexts.LCD_EFFICIENCY.with(history.efficiency())),
                    GameText.resolve(MachineScreenTexts.LCD_SPAN.with(MINUTES)), history.made(), history.power());
        } else {
            MachineArt.lcd(g, v.font(), v.x(LCD_X), v.y(LCD_Y), LCD_WIDTH, LCD_HEIGHT, lcdLines(machine),
                    machine.status() == MachineStatus.WORKING, true);
        }
        processWindow(v);
        MachineArt.rocker(g, v.x(SWITCH_X), v.y(SWITCH_Y), machine.control().on());
        MachineArt.engrave(g, v.font(), GameText.resolve(MachineScreenTexts.ON), v.x(SWITCH_X + SWITCH_WIDTH + 4),
                v.y(SWITCH_Y + 4), false);
        MachineArt.engrave(g, v.font(), GameText.resolve(MachineScreenTexts.OFF), v.x(SWITCH_X + SWITCH_WIDTH + 4),
                v.y(SWITCH_Y + SWITCH_HEIGHT - 4), false);
        glass(v);
    }

    @Override
    public void drawOver(final MachineView v) {
        if (!(v.machine() instanceof ProcessingMachineBlockEntity processing)) {
            return;
        }
        for (final Slot slot : v.menu().faceSlots()) {
            final ItemStack locked = slot.getContainerSlot() < processing.locks().size()
                    ? processing.locks().locked(slot.getContainerSlot()) : ItemStack.EMPTY;
            if (locked.isEmpty() || slot.hasItem()) {
                continue;
            }
            final int x = v.left() + slot.x;
            final int y = v.top() + slot.y;
            v.g().renderItem(locked, x, y);
            v.g().pose().pushPose();
            v.g().pose().translate(0, 0, OVER_ITEMS);
            v.g().fill(x, y, x + MachineScreenLayout.SLOT - 2, y + MachineScreenLayout.SLOT - 2, GHOST);
            v.g().pose().popPose();
        }
    }

    @Override
    public boolean clicked(final MachineView v, final double x, final double y, final int button) {
        if (button == InputConstants.MOUSE_BUTTON_LEFT
                && MachineScreenLayout.on(x, y, SWITCH_X, SWITCH_Y, SWITCH_WIDTH, SWITCH_HEIGHT)) {
            v.press().accept(MachineMenu.SWITCH);
            return true;
        }
        if (button == InputConstants.MOUSE_BUTTON_LEFT && onLcd(x, y)) {
            this.statistics = !this.statistics;
            return true;
        }
        return false;
    }

    @Override
    public List<Component> tooltip(final MachineView v, final double x, final double y) {
        final MachineBlockEntity machine = v.machine();
        final Energy energy = machine.power().energy();
        if (MachineScreenLayout.on(x, y, METER_X - 4, METER_Y - 4, METER_WIDTH + 8, METER_HEIGHT + 8)) {
            return List.of(GameText.component(MachineScreenTexts.TIP_METER.with(energy.rate(machine.drawn()),
                    energy.rate(MachineScreenLayout.meterMark(this.peak)))));
        }
        if (onLcd(x, y)) {
            return this.statistics ? statisticsTooltip(v.menu().history(), machine) : List.of(
                    GameText.component(MachineBlockEntity.STATUS_LINE.with(machine.status().text())),
                    GameText.component(MachineScreenTexts.TIP_LCD_STATISTICS.with(MINUTES))
                            .withStyle(ChatFormatting.DARK_GRAY));
        }
        if (MachineScreenLayout.on(x, y, SWITCH_X, SWITCH_Y, SWITCH_WIDTH, SWITCH_HEIGHT)) {
            return List.of(GameText.component(MachineScreenTexts.TIP_SWITCH.with(
                    (machine.control().on() ? MachineScreenTexts.ON : MachineScreenTexts.OFF).text())));
        }
        if (MachineScreenLayout.on(x, y, GLASS_X - 3, GLASS_Y - 4, GLASS_WIDTH + 6, GLASS_HEIGHT + 8)) {
            return glassTooltip(machine);
        }
        return List.of();
    }

    /*
     * The page of statistics told in full: what it makes a minute and the most it made, what it draws now and on the
     * whole, how much of the last minute it worked, and what last jammed it, amber.
     */
    private static List<Component> statisticsTooltip(final MachineHistory history, final MachineBlockEntity machine) {
        final Energy energy = machine.power().energy();
        final UnitFormatter format = UnitFormatter.forCurrentLocale();
        final List<Component> lines = new ArrayList<>();
        lines.add(GameText.component(MachineScreenTexts.TIP_STATISTICS.with(MINUTES)));
        if (history.made().isEmpty()) {
            lines.add(GameText.component(MachineScreenTexts.TIP_NO_READING.with(MachineHistory.READING_SECONDS))
                    .withStyle(ChatFormatting.GRAY));
        } else {
            lines.add(GameText.component(MachineScreenTexts.TIP_MADE.with(format.compact(machine.perMinute()),
                    format.compact(Math.max(machine.perMinute(), history.mostMade()))))
                    .withStyle(ChatFormatting.GRAY));
            lines.add(GameText.component(MachineScreenTexts.TIP_POWER.with(energy.rate(history.lastPower()),
                    energy.rate(history.averagePower()))).withStyle(ChatFormatting.GRAY));
        }
        lines.add(GameText.component(MachineScreenTexts.TIP_EFFICIENCY.with(history.efficiency(),
                history.workedTicks() / TICKS_A_SECOND)).withStyle(ChatFormatting.GRAY));
        history.stop().ifPresent(stop -> lines.add(GameText.component(jam(history.stoppedSecondsAgo(), stop))
                .withColor(MachineArt.PALETTE.get().lampWait() & RGB)));
        lines.add(GameText.component(MachineScreenTexts.TIP_LCD_READINGS).withStyle(ChatFormatting.DARK_GRAY));
        return lines;
    }

    /* What last jammed it, {@code seconds} ago, in seconds under a minute and in minutes after. */
    private static Text jam(final int seconds, final MachineStatus stop) {
        if (seconds < SECONDS_A_MINUTE) {
            return MachineScreenTexts.TIP_JAM_SECONDS.with(seconds, stop.text());
        }
        final int minutes = seconds / SECONDS_A_MINUTE;
        return minutes == 1 ? MachineScreenTexts.TIP_JAM_MINUTE.with(stop.text())
                : MachineScreenTexts.TIP_JAM_MINUTES.with(minutes, stop.text());
    }

    /* Whether ({@code x}, {@code y}) is on the LCD or its bezel. */
    private static boolean onLcd(final double x, final double y) {
        return MachineScreenLayout.on(x, y, LCD_X - 3, LCD_Y - 3, LCD_WIDTH + 6, LCD_HEIGHT + 6);
    }

    /* The LCD's two lines: what it made the last minute, then how far its work is and what it draws, or its state. */
    private static List<String> lcdLines(final MachineBlockEntity machine) {
        final String count = machine.perMinute() > 0 || machine.running()
                ? GameText.resolve(MachineScreenTexts.PER_MINUTE.with(UnitFormatter.forCurrentLocale()
                        .compact(machine.perMinute())))
                : GameText.resolve(MachineScreenTexts.NO_COUNT);
        if (machine.status() != MachineStatus.WORKING) {
            return List.of(count, GameText.resolve(lcdWord(machine.status())));
        }
        final int percent = machine instanceof ProcessingMachineBlockEntity processing
                && processing.getMaxProgress() > 0
                ? processing.getProgress() * PERCENT / processing.getMaxProgress() : 0;
        return List.of(count, GameText.resolve(MachineScreenTexts.PROGRESS.with(percent)) + "  "
                + GameText.resolve(machine.power().energy().rate(machine.drawn())));
    }

    /* The LCD's short word for what keeps a machine from working. */
    private static TextKey lcdWord(final MachineStatus status) {
        return switch (status) {
            case WORKING -> MachineScreenTexts.LCD_WORKING;
            case NOT_FORMED -> MachineScreenTexts.LCD_NOT_FORMED;
            case WAITING -> MachineScreenTexts.LCD_WAITING;
            case LOCKED -> MachineScreenTexts.LCD_LOCKED;
            case OUTPUT_FULL -> MachineScreenTexts.LCD_OUTPUT_FULL;
            case NO_ENERGY -> MachineScreenTexts.LCD_NO_ENERGY;
            case UNDERVOLTAGE -> MachineScreenTexts.LCD_UNDERVOLTAGE;
            case OVERVOLTAGE -> MachineScreenTexts.LCD_OVERVOLTAGE;
            case FUSE_BLOWN -> MachineScreenTexts.LCD_FUSE_BLOWN;
            case TOO_SLOW -> MachineScreenTexts.LCD_TOO_SLOW;
            case OVERHEATED -> MachineScreenTexts.LCD_OVERHEATED;
            case DAMAGED -> MachineScreenTexts.LCD_DAMAGED;
            case OFF_BY_REDSTONE -> MachineScreenTexts.LCD_OFF_BY_REDSTONE;
            case SWITCHED_OFF -> MachineScreenTexts.LCD_SWITCHED_OFF;
        };
    }

    /* The process window: its bolted bezel, the dark behind it, its slots, and each lane's arrow. */
    private static void processWindow(final MachineView v) {
        final GuiGraphics g = v.g();
        final int x0 = v.x(PROCESS_X);
        final int y0 = v.y(PROCESS_Y);
        final int x1 = v.x(PROCESS_RIGHT);
        final int y1 = v.y(PROCESS_BOTTOM);
        MachineArt.bezel(g, x0, y0, x1, y1, true);
        for (int y = y0 + 3; y < y1 - 3; y++) {
            MachineArt.hline(g, x0 + 3, x1 - 3, y, MachineArt.mix(MachineArt.rgb(34, 38, 44),
                    MachineArt.rgb(16, 18, 22), (y - y0 - 3) / (double) (y1 - y0 - 6)));
        }
        for (final Slot slot : v.menu().faceSlots()) {
            MachineArt.slot(g, v.left() + slot.x - 1, v.top() + slot.y - 1);
        }
        if (!(v.machine() instanceof ProcessingMachineBlockEntity processing)) {
            return;
        }
        final ProcessingMachineBlockEntity.Layout layout = processing.layout();
        final int lit = MachineArt.PALETTE.get().lampRun();
        if (MachineScreenLayout.fits(layout.lanes(), layout.inputs(), layout.outputs())) {
            for (int lane = 0; lane < layout.lanes(); lane++) {
                MachineArt.arrow(g, v.x(MachineScreenLayout.arrowX(layout.inputs())),
                        v.y(MachineScreenLayout.laneY(lane, layout.lanes()) + LANE_PITCH / 2 - 1),
                        share(processing.progress(lane), processing.maxProgress(lane)), lit);
            }
            return;
        }
        final int barTop = y1 - 3 - 4;
        MachineArt.fill(g, x0 + 6, barTop, x1 - 6, barTop + 2, MachineArt.rgb(46, 50, 56));
        MachineArt.fill(g, x0 + 6, barTop, x0 + 6 + (int) Math.round((x1 - x0 - 12)
                * share(processing.getProgress(), processing.getMaxProgress())), barTop + 2, lit);
    }

    /* The sight glass, of the machine's first tank, or of its stored energy for a machine with no tank. */
    private static void glass(final MachineView v) {
        final GuiGraphics g = v.g();
        final MachineBlockEntity machine = v.machine();
        final int x = v.x(GLASS_X);
        final int y = v.y(GLASS_Y);
        final Optional<FluidTank> tank = firstTank(machine);
        final Optional<MatterTank> matter = firstMatter(machine);
        final String label;
        if (tank.isPresent()) {
            final FluidStack held = tank.get().getFluid();
            MachineArt.glassBrackets(g, x, y, GLASS_HEIGHT);
            final int top = MachineArt.glassTop(y, GLASS_HEIGHT, share(held.getAmount(), tank.get().getCapacity()));
            final int tint = held.isEmpty() ? MachineArt.PALETTE.get().water() : fluidTiles(g, held, x, top,
                    y + GLASS_HEIGHT);
            MachineArt.glassFront(g, x, y, GLASS_HEIGHT, top, tint, v.tick(), v.working());
            label = held.isEmpty() ? "" : held.getHoverName().getString();
        } else if (matter.isPresent()) {
            final Optional<MatterStack> held = matter.get().held();
            final int colour = held.map(stack -> 0xFF << 24 | stack.material().colour())
                    .orElse(MachineArt.PALETTE.get().water());
            MachineArt.sightGlass(g, x, y, GLASS_HEIGHT, share(held.map(MatterStack::amount).orElse(0L),
                    matter.get().capacity()), colour, v.tick(), v.working());
            label = held.map(stack -> GameText.resolve(stack.material().name())).orElse("");
        } else {
            MachineArt.sightGlass(g, x, y, GLASS_HEIGHT, share(machine.power().stored(),
                    machine.capacityShown()), MachineArt.PALETTE.get().lampWait(), v.tick(), v.working());
            label = GameText.resolve(machine.power().energy().unit().symbol());
        }
        if (!label.isEmpty()) {
            MachineArt.engrave(g, v.font(), MachineArt.fit(v.font(), label.toUpperCase(Locale.ROOT), LABEL_ROOM),
                    v.x(GLASS_X + GLASS_WIDTH / 2), v.y(GLASS_Y + GLASS_HEIGHT + 11), true);
        }
    }

    /* The fluid's own texture tiled in the glass from {@code top} down, shaded deeper below; its tint. */
    private static int fluidTiles(final GuiGraphics g, final FluidStack held, final int x, final int top,
                                  final int bottom) {
        final IClientFluidTypeExtensions client = IClientFluidTypeExtensions.of(held.getFluid());
        final TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(client.getStillTexture(held));
        final int tint = client.getTintColor(held);
        Draw.pushScissor(g, x, top, x + TILE, bottom);
        for (int ty = bottom - TILE; ty > top - TILE; ty -= TILE) {
            g.blit(x, ty, 0, TILE, TILE, sprite, (tint >> 16 & 0xFF) / 255F, (tint >> 8 & 0xFF) / 255F,
                    (tint & 0xFF) / 255F, 1F);
        }
        Draw.popScissor(g);
        for (int yy = top; yy < bottom; yy++) {
            MachineArt.hline(g, x, x + TILE, yy, MachineArt.alpha(DEEP, (int) (DEEPEST * (yy - top)
                    / (double) Math.max(1, bottom - top))));
        }
        return 0xFF << 24 | tint;
    }

    /* Every tank's contents, or the stored energy for a machine with none. */
    private static List<Component> glassTooltip(final MachineBlockEntity machine) {
        final List<Component> lines = new ArrayList<>();
        if (machine instanceof ProcessingMachineBlockEntity processing) {
            final ProcessingMachineBlockEntity.Layout layout = processing.layout();
            for (int i = 0; i < layout.inputTanks(); i++) {
                lines.add(tankLine(processing.inputTank(i)));
            }
            for (int i = 0; i < layout.outputTanks(); i++) {
                lines.add(tankLine(processing.outputTank(i)));
            }
            for (final MatterState state : MatterState.values()) {
                for (final MatterTank tank : processing.matter().inputs(state)) {
                    lines.add(matterLine(tank));
                }
                for (final MatterTank tank : processing.matter().outputs(state)) {
                    lines.add(matterLine(tank));
                }
            }
        }
        if (lines.isEmpty()) {
            final Energy energy = machine.power().energy();
            lines.add(GameText.component(MachineScreenTexts.TIP_STORED.with(energy.amount(machine.power().stored()),
                    energy.amount(machine.capacityShown()))));
        }
        return lines;
    }

    private static Component tankLine(final FluidTank tank) {
        final UnitFormatter format = UnitFormatter.forCurrentLocale();
        final FluidStack held = tank.getFluid();
        return held.isEmpty()
                ? GameText.component(MachineScreenTexts.TIP_EMPTY_TANK.with(format.compact(tank.getCapacity())))
                : GameText.component(MachineScreenTexts.TIP_TANK.with(GameText.of(held.getHoverName()),
                        format.compact(held.getAmount()), format.compact(tank.getCapacity())));
    }

    private static Component matterLine(final MatterTank tank) {
        final UnitFormatter format = UnitFormatter.forCurrentLocale();
        final Optional<MatterStack> held = tank.held();
        return held.isEmpty()
                ? GameText.component(MachineScreenTexts.TIP_EMPTY_TANK.with(format.compact(tank.capacity())))
                : GameText.component(MachineScreenTexts.TIP_TANK.with(held.get().material().name(),
                        format.compact(held.get().amount()), format.compact(tank.capacity())));
    }

    private static Optional<FluidTank> firstTank(final MachineBlockEntity machine) {
        if (machine instanceof ProcessingMachineBlockEntity processing) {
            if (processing.layout().inputTanks() > 0) {
                return Optional.of(processing.inputTank(0));
            }
            if (processing.layout().outputTanks() > 0) {
                return Optional.of(processing.outputTank(0));
            }
        }
        return Optional.empty();
    }

    private static Optional<MatterTank> firstMatter(final MachineBlockEntity machine) {
        if (machine instanceof ProcessingMachineBlockEntity processing) {
            for (final MatterState state : MatterState.values()) {
                if (!processing.matter().inputs(state).isEmpty()) {
                    return Optional.of(processing.matter().inputs(state).getFirst());
                }
                if (!processing.matter().outputs(state).isEmpty()) {
                    return Optional.of(processing.matter().outputs(state).getFirst());
                }
            }
        }
        return Optional.empty();
    }

    private static double share(final long part, final long whole) {
        return whole <= 0 ? 0.0 : Math.max(0.0, Math.min(1.0, part / (double) whole));
    }
}
