/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.machine;

import com.mojang.blaze3d.platform.InputConstants;
import dev.jstech.core.machine.FacePicture;
import dev.jstech.core.machine.MachineBlockEntity;
import dev.jstech.core.machine.MachineMenu;
import dev.jstech.core.machine.MachineScreenLayout;
import dev.jstech.core.machine.MachineScreenTexts;
import dev.jstech.core.machine.MachineStatus;
import dev.jstech.core.text.GameText;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;

/**
 * A machine's face as a picture of its own (see {@link FacePicture}): the machine drawn at work by its mod, its frames
 * turning while it works and its idle frame while it does not, and over it what the game draws live where the picture
 * says: the plates and the lamps, the panel meter, the LCD, the switch with its words, the sight glasses with what
 * they hold, and the words engraved on its parts, each as its mod says it (translated, or read from the machine). The
 * menu places its slots in the picture's ports. A click on the switch turns the machine on or off, on the LCD turns it
 * to its page of statistics, as on the standard face.
 */
@ApiStatus.Experimental
public final class PictureFace implements IMachineFace {

    private final ResourceLocation frames;
    private final FacePicture picture;
    private final BiFunction<MachineBlockEntity, String, String> say;
    /* What it shares with the standard face: the slots' locks drawn over the items. */
    private final StandardFace standard = new StandardFace();
    private long peak;
    private boolean statistics;

    private static final double MILLIS_A_TICK = 50.0;
    /* How far the name plate keeps from the LCD's bezel. */
    private static final int PLATE_GAP = 4;
    /* What a still's glass with nothing in it writes under it. */
    private static final String EMPTY_GLASS = "-";

    /**
     * @param frames  its frames, laid out {@link FacePicture#COLUMNS} to a row, the idle one first, each as large as
     *                the face
     * @param picture where on it what the game draws stands
     * @param say     what a word of the picture says on the machine: its translation, or a reading of the machine
     */
    public PictureFace(final ResourceLocation frames, final FacePicture picture,
                       final BiFunction<MachineBlockEntity, String, String> say) {
        this.frames = frames;
        this.picture = picture;
        this.say = say;
    }

    @Override
    public void draw(final MachineView v) {
        final MachineBlockEntity machine = v.machine();
        final int frame = v.working() ? loopFrame(this.picture, v.tick() * MILLIS_A_TICK) : 0;
        frame(v.g(), v.x(0), v.y(0), this.frames, this.picture, frame);
        // The plates: the name its picture writes on them, else the screen's title, as far as its LCD leaves room.
        final Screen open = Minecraft.getInstance().screen;
        final String name = this.picture.plate().map(plate -> this.say.apply(machine, plate)).orElseGet(() ->
                MachineWords.upper(GameText.of(open != null ? open.getTitle()
                        : machine.getBlockState().getBlock().getName())));
        MachineChrome.plates(v, name, plateRoom(this.picture));
        MachineChrome.lampsOnDark(v);
        meter(v);
        final FacePicture.Box lcd = this.picture.lcd();
        StandardFace.lcd(v, this.statistics, lcd.x(), lcd.y(), lcd.w(), lcd.h());
        toggle(v.g(), v.font(), v.x(0), v.y(0), this.picture, machine.control().on());
        for (final FacePicture.Glass glass : this.picture.glasses()) {
            glass(v, glass);
        }
        words(v.g(), v.font(), v.x(0), v.y(0), this.picture, frame, written -> this.say.apply(machine, written));
    }

    /**
     * A face as a still of its machine at work, for a page with no machine behind it (a recipe viewer's): the frame of
     * its loop the clock is on, its top left at ({@code x}, {@code y}) on the screen, and over it what {@code still}
     * says its plates, LCD, meter and sight glasses show; its switch on and its RUN lamp lit.
     *
     * @param say what a word of the picture says: its translation
     */
    public static void drawStill(final GuiGraphics g, final Font font, final int x, final int y,
                                 final ResourceLocation frames, final FacePicture picture, final Still still,
                                 final UnaryOperator<String> say) {
        final long now = System.currentTimeMillis();
        final int frame = loopFrame(picture, now);
        frame(g, x, y, frames, picture, frame);
        MachineChrome.plates(g, font, x, y, still.name(), plateRoom(picture), still.rating());
        MachineChrome.lamps(g, font, x, y, MachineStatus.Lamp.RUN, true);
        final FacePicture.Meter meter = picture.meter();
        MachineArt.panelMeter(g, font, x + meter.x(), y + meter.y(), meter.w(), meter.h(), still.meter(),
                StandardFace.MARKS, true);
        final FacePicture.Box lcd = picture.lcd();
        MachineArt.lcd(g, font, x + lcd.x(), y + lcd.y(), lcd.w(), lcd.h(), still.lcd(), true, true);
        toggle(g, font, x, y, picture, true);
        final float tick = (float) (now / MILLIS_A_TICK);
        for (int i = 0; i < picture.glasses().size(); i++) {
            final FacePicture.Glass glass = picture.glasses().get(i);
            final Optional<Fill> fill = i < still.fills().size() ? still.fills().get(i) : Optional.empty();
            final double share = fill.map(Fill::share).orElse(0.0);
            final int colour = fill.map(Fill::colour).orElse(MachineArt.PALETTE.get().water());
            if (glass.slim()) {
                MachineArt.slimGlass(g, x + glass.x(), y + glass.y(), glass.h(), share, colour, tick, true);
            } else {
                MachineArt.sightGlass(g, x + glass.x(), y + glass.y(), glass.h(), share, colour, tick, true);
            }
            glass.label().ifPresent(label -> MachineArt.textCentred(g, font, fill.map(Fill::symbol)
                    .orElse(EMPTY_GLASS), x + label.x(), y + label.y(), label.argb()));
        }
        words(g, font, x, y, picture, frame, say);
    }

    @Override
    public void drawOver(final MachineView v) {
        this.standard.drawOver(v);
    }

    /* Its plates are drawn over its picture, the name it gives them as wide as its LCD leaves room. */
    @Override
    public boolean drawsPlates() {
        return true;
    }

    @Override
    public boolean clicked(final MachineView v, final double x, final double y, final int button) {
        if (button != InputConstants.MOUSE_BUTTON_LEFT) {
            return false;
        }
        if (onSwitch(x, y)) {
            v.press().accept(MachineMenu.SWITCH);
            return true;
        }
        if (onLcd(x, y)) {
            this.statistics = !this.statistics;
            return true;
        }
        return false;
    }

    @Override
    public List<Component> tooltip(final MachineView v, final double x, final double y) {
        final MachineBlockEntity machine = v.machine();
        final FacePicture.Meter meter = this.picture.meter();
        if (MachineScreenLayout.on(x, y, meter.x() - 4, meter.y() - 4, meter.w() + 8, meter.h() + 8)) {
            return List.of(GameText.component(MachineScreenTexts.TIP_METER.with(machine.power().energy().rate(
                    machine.drawn()), machine.power().energy().rate(MachineScreenLayout.meterMark(this.peak)))));
        }
        if (onLcd(x, y)) {
            return this.statistics ? StandardFace.statisticsTooltip(v.menu().history(), machine) : List.of(
                    GameText.component(MachineBlockEntity.STATUS_LINE.with(machine.status().text())),
                    GameText.component(MachineScreenTexts.TIP_LCD_STATISTICS.with(StandardFace.MINUTES))
                            .withStyle(ChatFormatting.DARK_GRAY));
        }
        if (onSwitch(x, y)) {
            return List.of(GameText.component(MachineScreenTexts.TIP_SWITCH.with(
                    (machine.control().on() ? MachineScreenTexts.ON : MachineScreenTexts.OFF).text())));
        }
        for (final FacePicture.Glass glass : this.picture.glasses()) {
            if (MachineScreenLayout.on(x, y, glass.x() - 3, glass.y() - 4, glass.w() + 6, glass.h() + 8)) {
                return StandardFace.glassTooltip(machine);
            }
        }
        return List.of();
    }

    /* The panel meter, its range engraved where the picture writes it, or under it. */
    private void meter(final MachineView v) {
        final FacePicture.Meter meter = this.picture.meter();
        final int labelX = meter.label().map(FacePicture.Place::x).orElse(meter.x() + meter.w() / 2);
        final int labelY = meter.label().map(FacePicture.Place::y).orElse(meter.y() + meter.h() + 8);
        this.peak = StandardFace.meter(v, this.peak, meter.x(), meter.y(), meter.w(), meter.h(), labelX, labelY);
    }

    /* The frame of the loop {@code millis} into it. */
    private static int loopFrame(final FacePicture picture, final double millis) {
        return 1 + (int) (millis / picture.millis()) % picture.frames();
    }

    /* Frame {@code frame} of the picture, the face's top left at ({@code x}, {@code y}). */
    private static void frame(final GuiGraphics g, final int x, final int y, final ResourceLocation frames,
                              final FacePicture picture, final int frame) {
        g.blit(frames, x, y, frame % FacePicture.COLUMNS * MachineScreenLayout.FACE_WIDTH,
                frame / FacePicture.COLUMNS * MachineScreenLayout.FACE_HEIGHT, MachineScreenLayout.FACE_WIDTH,
                MachineScreenLayout.FACE_HEIGHT, FacePicture.COLUMNS * MachineScreenLayout.FACE_WIDTH,
                picture.rows() * MachineScreenLayout.FACE_HEIGHT);
    }

    /* How wide its name plate may grow: as far as its LCD leaves room. */
    private static int plateRoom(final FacePicture picture) {
        return picture.lcd().x() - PLATE_GAP - MachineScreenLayout.PLATE_X;
    }

    /* The switch, {@code on} or off, and its words on and off where the picture writes them, light on its inset. */
    private static void toggle(final GuiGraphics g, final Font font, final int x, final int y,
                               final FacePicture picture, final boolean on) {
        final FacePicture.Toggle toggle = picture.toggle();
        final int ink = MachineArt.PALETTE.get().insetInk();
        MachineArt.rocker(g, x + toggle.x(), y + toggle.y(), on);
        toggle.on().ifPresent(at -> MachineArt.text(g, font, GameText.resolve(MachineScreenTexts.ON), x + at.x(),
                y + at.y(), ink));
        toggle.off().ifPresent(at -> MachineArt.text(g, font, GameText.resolve(MachineScreenTexts.OFF), x + at.x(),
                y + at.y(), ink));
    }

    /* The words engraved on its parts that show on frame {@code frame}, each as {@code say} says it. */
    private static void words(final GuiGraphics g, final Font font, final int x, final int y,
                              final FacePicture picture, final int frame, final UnaryOperator<String> say) {
        for (final FacePicture.Word word : picture.words()) {
            if (word.showsOn(frame)) {
                MachineArt.text(g, font, say.apply(word.text()), x + (int) Math.round(word.x()),
                        y + (int) Math.round(word.y()), word.argb());
            }
        }
    }

    /*
     * A sight glass of the machine's tank, the one holding its liquid for a glass of a liquid, and what it holds
     * written where the picture writes it. A glass of a liquid no tank holds or has room for stands empty.
     */
    private static void glass(final MachineView v, final FacePicture.Glass glass) {
        final int tank = glass.fluid().map(ids -> StandardFace.tankFor(v.machine(), Arrays.stream(ids.split("\\|"))
                .map(ResourceLocation::parse).collect(Collectors.toSet()), glass.tank())).orElse(glass.tank());
        if (tank < 0) {
            if (glass.slim()) {
                StandardFace.slimGlass(v, glass.x(), glass.y(), glass.h(), Integer.MAX_VALUE);
            }
            return;
        }
        if (glass.slim()) {
            StandardFace.slimGlass(v, glass.x(), glass.y(), glass.h(), tank);
        } else {
            StandardFace.glass(v, glass.x(), glass.y(), glass.h(), tank);
        }
        glass.label().ifPresent(label -> MachineArt.textCentred(v.g(), v.font(),
                StandardFace.symbol(v.machine(), tank), v.x(label.x()), v.y(label.y()), label.argb()));
    }

    /**
     * What a still of a face shows, with no machine behind it.
     *
     * @param name   what its name plate writes
     * @param rating what its class plate writes
     * @param lcd    its LCD's lines
     * @param meter  how far up its scale its meter's needle stands, from 0 to 1
     * @param fills  what each of its sight glasses holds, in the order the picture lists them; none, an empty glass
     */
    public record Still(String name, String rating, List<String> lcd, double meter, List<Optional<Fill>> fills) {

        public Still {
            lcd = List.copyOf(lcd);
            fills = List.copyOf(fills);
        }
    }

    /**
     * What a still's sight glass holds.
     *
     * @param colour its colour, opaque ARGB
     * @param share  how full the glass is, from 0 to 1
     * @param symbol what is written under the glass, its formula or its symbol
     */
    public record Fill(int colour, double share, String symbol) {
    }

    /* Whether ({@code x}, {@code y}) is on its switch. */
    private boolean onSwitch(final double x, final double y) {
        final FacePicture.Toggle toggle = this.picture.toggle();
        return MachineScreenLayout.on(x, y, toggle.x(), toggle.y(), MachineScreenLayout.SWITCH_WIDTH,
                MachineScreenLayout.SWITCH_HEIGHT);
    }

    /* Whether ({@code x}, {@code y}) is on its LCD or the LCD's bezel. */
    private boolean onLcd(final double x, final double y) {
        final FacePicture.Box lcd = this.picture.lcd();
        return MachineScreenLayout.on(x, y, lcd.x() - 3, lcd.y() - 3, lcd.w() + 6, lcd.h() + 6);
    }
}
