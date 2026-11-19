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
import dev.jstech.core.text.GameText;
import java.util.List;
import java.util.function.BiFunction;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
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
        final int frame = v.working() ? 1 + (int) (v.tick() * MILLIS_A_TICK / this.picture.millis())
                % this.picture.frames() : 0;
        v.g().blit(this.frames, v.x(0), v.y(0), frame % FacePicture.COLUMNS * MachineScreenLayout.FACE_WIDTH,
                frame / FacePicture.COLUMNS * MachineScreenLayout.FACE_HEIGHT, MachineScreenLayout.FACE_WIDTH,
                MachineScreenLayout.FACE_HEIGHT, FacePicture.COLUMNS * MachineScreenLayout.FACE_WIDTH,
                this.picture.rows() * MachineScreenLayout.FACE_HEIGHT);
        // The plates: the name its picture writes on them, else the screen's title, as far as its LCD leaves room.
        final Screen open = Minecraft.getInstance().screen;
        final String name = this.picture.plate().map(plate -> this.say.apply(machine, plate)).orElseGet(() ->
                MachineWords.upper(GameText.of(open != null ? open.getTitle()
                        : machine.getBlockState().getBlock().getName())));
        MachineChrome.plates(v, name, this.picture.lcd().x() - PLATE_GAP - MachineScreenLayout.PLATE_X);
        MachineChrome.lamps(v);
        meter(v);
        final FacePicture.Box lcd = this.picture.lcd();
        StandardFace.lcd(v, this.statistics, lcd.x(), lcd.y(), lcd.w(), lcd.h());
        toggle(v);
        for (final FacePicture.Glass glass : this.picture.glasses()) {
            glass(v, glass);
        }
        for (final FacePicture.Word word : this.picture.words()) {
            if (word.showsOn(frame)) {
                MachineArt.text(v.g(), v.font(), this.say.apply(machine, word.text()),
                        v.x((int) Math.round(word.x())), v.y((int) Math.round(word.y())), word.argb());
            }
        }
    }

    @Override
    public void drawOver(final MachineView v) {
        this.standard.drawOver(v);
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

    /* The switch, and its words on and off where the picture writes them. */
    private void toggle(final MachineView v) {
        final FacePicture.Toggle toggle = this.picture.toggle();
        MachineArt.rocker(v.g(), v.x(toggle.x()), v.y(toggle.y()), v.machine().control().on());
        toggle.on().ifPresent(at -> MachineArt.text(v.g(), v.font(), GameText.resolve(MachineScreenTexts.ON),
                v.x(at.x()), v.y(at.y()), MachineArt.PALETTE.get().title()));
        toggle.off().ifPresent(at -> MachineArt.text(v.g(), v.font(), GameText.resolve(MachineScreenTexts.OFF),
                v.x(at.x()), v.y(at.y()), MachineArt.PALETTE.get().title()));
    }

    /* A sight glass of the machine's tank, and what it holds written where the picture writes it. */
    private static void glass(final MachineView v, final FacePicture.Glass glass) {
        if (glass.slim()) {
            StandardFace.slimGlass(v, glass.x(), glass.y(), glass.h(), glass.tank());
        } else {
            StandardFace.glass(v, glass.x(), glass.y(), glass.h(), glass.tank());
        }
        glass.label().ifPresent(label -> MachineArt.textCentred(v.g(), v.font(),
                StandardFace.symbol(v.machine(), glass.tank()), v.x(label.x()), v.y(label.y()), label.argb()));
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
