/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.energy.client;

import dev.jstech.core.JsCore;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.gui.screen.CoreContainerScreen;
import dev.jstech.core.energy.Energies;
import dev.jstech.core.format.MeterScale;
import dev.jstech.core.jstechseries.energy.ConverterLimit;
import dev.jstech.core.jstechseries.energy.ConverterView;
import dev.jstech.core.jstechseries.energy.EnergyConverterBlockEntity;
import dev.jstech.core.jstechseries.energy.EnergyConverterLayout;
import dev.jstech.core.jstechseries.energy.EnergyConverterMenu;
import dev.jstech.core.jstechseries.energy.EnergyConverters;
import dev.jstech.core.jstechseries.energy.JsEnergy;
import dev.jstech.core.jstechseries.energy.VoltageClass;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextKey;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * A converter's screen, as the approved design draws it: the converter cabinet's steel face over the player's
 * inventory. Its plates name it and its class; the display reads what crosses it; the lamps say it runs, is held at its
 * limit or is stopped by a fault; the FE meter on the left and the J's Energy meter on the right swing with what
 * crosses, against scales printed for its limit; through the window the bridge's busbars carry the energy's pulses,
 * red as FE and gold as JE, the way it works. Below, the class keys of a configurable converter (the ceiling of a fixed
 * one), the limit's thumbwheels and the direction's knob, which set it as on the cabinet itself, and the switch.
 */
@PaletteHolder
public final class EnergyConverterScreen extends CoreContainerScreen<EnergyConverterMenu> {

    /** The face and the inventory, drawn as the approved design draws them, with no words; the sprites below them. */
    public static final ResourceLocation PICTURE = ResourceLocation.fromNamespaceAndPath(JsCore.MODID,
            "textures/gui/energy_converter.png");
    /** The face's colours. */
    public static final Palette<Colours> PALETTE = Palettes.declare(JsCore.MODID, "energy_converter", new Colours(
            0xFF << 24 | 0x373A3D, 0xFF << 24 | 0xAFB3B8, 0xFF << 24 | 0xC9CCD0, 0xFF << 24 | 0x34373C,
            0xFF << 24 | 0xE4E5E7, 0xFF << 24 | 0xE8C547, 0xFF << 24 | 0x3C2C00, 0xFF << 24 | 0xF3E2A3,
            0xFF << 24 | 0x7CFF7C, 0xFF << 24 | 0x182418, 0xFF << 24 | 0xC41E18, 0xFF << 24 | 0x282828,
            0xFF << 24 | 0x323232, 0xFF << 24 | 0xECECE6, 0xFF << 24 | 0xDCDCD6, 0xFF << 24 | 0x282C32,
            0xFF << 24 | 0xFFECA0, 0xFF << 24 | 0x3C2800, 0xFF << 24 | 0x969696, 0xFF << 24 | 0xDC2C28,
            0xFF << 24 | 0xFFC83C, 0xFF << 24 | 0x5FE07A, 0xFF << 24 | 0x1E3C24, 0xFF << 24 | 0x404040,
            0x46 << 24, 0x32 << 24, 0xFF << 24 | 0xFFFFFF, 0xFF << 24));
    private static final int FRAME_MILLIS = 80;
    private static final int PULSES = 4;
    private static final int PULSE_GAP = 6;
    private static final int LED_CYCLE = 6;
    private static final int LED_LIT = 4;
    private static final int LED_STAGGER = 4;
    private static final int LIMIT_BLINK = 4;
    private static final int METER_MARKS = 3;
    private static final int NEEDLE_FROM_FOOT = 3;
    private static final int NEEDLE_TRAVEL = EnergyConverterLayout.METER_HEIGHT - 6;
    private static final int KNOB_POINTER_FROM = 2;
    private static final int KNOB_DOT_REACH = 3;
    private static final double TO_FE_ANGLE = Math.PI * 1.25;
    private static final double TO_JE_ANGLE = Math.PI * 1.75;
    private static final int ARROW_LENGTH = 5;

    public EnergyConverterScreen(final EnergyConverterMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title);
        this.imageWidth = EnergyConverterLayout.WIDTH;
        this.imageHeight = EnergyConverterLayout.HEIGHT;
    }

    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        final double x = mouseX - this.leftPos;
        final double y = mouseY - this.topPos;
        final ConverterView view = this.menu.view();
        final int pressed = pressedButton(view, x, y);
        if (pressed >= 0) {
            sendButton(pressed);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderBg(final GuiGraphics g, final float partialTick, final int mouseX, final int mouseY) {
        final Colours c = PALETTE.get();
        final ConverterView view = this.menu.view();
        final long frame = Util.getMillis() / FRAME_MILLIS;
        Draw.blended(() -> g.blit(PICTURE, this.leftPos, this.topPos, 0, 0, EnergyConverterLayout.WIDTH,
                EnergyConverterLayout.HEIGHT, EnergyConverterLayout.PICTURE_WIDTH,
                EnergyConverterLayout.PICTURE_HEIGHT));
        plates(g, c, view);
        lamps(g, view, frame);
        display(g, c, view);
        meters(g, c, view, frame);
        bridge(g, c, view, frame);
        sprite(g, EnergyConverterLayout.SWITCH_SPRITE_X + (view.on() ? 0 : EnergyConverterLayout.SWITCH_SPRITE_PITCH),
                EnergyConverterLayout.SPRITES_Y, EnergyConverterLayout.SWITCH_X, EnergyConverterLayout.SWITCH_Y,
                EnergyConverterLayout.SWITCH_WIDTH, EnergyConverterLayout.SWITCH_HEIGHT);
        classes(g, c, view);
        thumbwheels(g, c, view);
        knob(g, c, view);
        words(g, c, view);
    }

    /* The inventory's word alone: the converter's name is on its plate. */
    @Override
    protected void renderLabels(final GuiGraphics g, final int mouseX, final int mouseY) {
        Draw.text(g, this.font, this.playerInventoryTitle, EnergyConverterLayout.INVENTORY_WORD_X,
                EnergyConverterLayout.INVENTORY_WORD_Y, PALETTE.get().inventoryTitle());
    }

    /* The button a click at ({@code x}, {@code y}) on the face presses, or -1 for none. */
    private static int pressedButton(final ConverterView view, final double x, final double y) {
        if (EnergyConverterLayout.onSwitch(x, y)) {
            return EnergyConverterMenu.SWITCH;
        }
        if (EnergyConverterLayout.onToFeWord(x, y)) {
            return EnergyConverterMenu.TO_FE;
        }
        if (EnergyConverterLayout.onToJeWord(x, y)) {
            return EnergyConverterMenu.TO_JE;
        }
        if (EnergyConverterLayout.onKnob(x, y)) {
            return view.toJe() ? EnergyConverterMenu.TO_FE : EnergyConverterMenu.TO_JE;
        }
        final int key = view.fixed() ? -1 : EnergyConverterLayout.keyAt(x, y);
        if (key >= 0) {
            return EnergyConverterMenu.FIRST_KEY + key;
        }
        final int drum = EnergyConverterLayout.drumAt(x, y);
        if (drum == ConverterLimit.DRUMS) {
            return EnergyConverterMenu.PREFIX;
        }
        if (drum >= 0) {
            return (EnergyConverterLayout.upperHalf(y) ? EnergyConverterMenu.FIRST_DRUM_UP
                    : EnergyConverterMenu.FIRST_DRUM_DOWN) + drum;
        }
        return -1;
    }

    /* The name plate, and the gold class plate below it: each as wide as its words, which shrink to fit if long. */
    private void plates(final GuiGraphics g, final Colours c, final ConverterView view) {
        final VoltageClass voltage = view.voltageClass();
        final String name = GameText.resolve(view.fixed() ? EnergyConverters.FIXED_PLATE.with(
                EnergyConverters.key(voltage).text()) : EnergyConverters.CONFIGURABLE_PLATE.text());
        plate(g, EnergyConverterLayout.PLATE_X, EnergyConverterLayout.NAME_PLATE_Y, name, c.plate(), c.plateInk(),
                c.plateLip());
        final String rating = JsEnergy.classOf(voltage).ratingKey().map(GameText::resolve).orElse("");
        final String plate = GameText.resolve(EnergyConverters.CLASS_PLATE.with(
                EnergyConverters.word(voltage).text(), Text.literal(rating)));
        plate(g, EnergyConverterLayout.PLATE_X, EnergyConverterLayout.CLASS_PLATE_Y, plate, c.gold(), c.goldInk(),
                c.goldLip());
    }

    private void plate(final GuiGraphics g, final int px, final int py, final String text, final int ground,
                       final int ink, final int lip) {
        final int room = EnergyConverterLayout.PLATE_RIGHT - px - EnergyConverterLayout.PLATE_PADDING * 2;
        final float scale = Math.min(EnergyConverterLayout.SMALL, room / (float) Math.max(1, this.font.width(text)));
        final int width = (int) Math.ceil(this.font.width(text) * scale) + EnergyConverterLayout.PLATE_PADDING * 2;
        final int x0 = this.leftPos + px;
        final int y0 = this.topPos + py;
        final int x1 = x0 + width;
        final int y1 = y0 + EnergyConverterLayout.PLATE_HEIGHT;
        final Colours c = PALETTE.get();
        g.fill(x0 + 1, y1, x1 + 1, y1 + 1, c.shadow());
        g.fill(x1, y0 + 1, x1 + 1, y1 + 1, c.shadow());
        g.fill(x0, y0, x1, y1, ground);
        g.fill(x0, y0, x1, y0 + 1, mix(ground, c.white(), 0.45F));
        g.fill(x0, y0, x0 + 1, y1, mix(ground, c.white(), 0.3F));
        g.fill(x0, y1 - 1, x1, y1, mix(ground, c.black(), 0.35F));
        g.fill(x1 - 1, y0, x1, y1, mix(ground, c.black(), 0.28F));
        for (final int sx : new int[] {x0 + 3, x1 - 5}) {
            g.fill(sx, y0 + 5, sx + 2, y0 + 7, mix(ground, c.black(), 0.45F));
            g.fill(sx, y0 + 5, sx + 1, y0 + 6, mix(ground, c.black(), 0.2F));
        }
        final int tx = x0 + EnergyConverterLayout.PLATE_PADDING;
        final int ty = y0 + (EnergyConverterLayout.PLATE_HEIGHT - Math.round(this.font.lineHeight * scale)) / 2 + 1;
        Draw.textScaled(g, this.font, text, tx, ty + 1, lip, scale);
        Draw.textScaled(g, this.font, text, tx, ty, ink, scale);
    }

    /* The lamps lit: RUN while it moves energy, LIMIT blinking while held at its limit, FAULT while a fault stops it. */
    private void lamps(final GuiGraphics g, final ConverterView view, final long frame) {
        final boolean[] lit = {view.running(), view.limited() && frame / LIMIT_BLINK % 2 == 0, view.fault()};
        for (int i = 0; i < lit.length; i++) {
            if (lit[i]) {
                sprite(g, i * (EnergyConverterLayout.LAMP_SPRITE + 1), EnergyConverterLayout.SPRITES_Y,
                        EnergyConverterLayout.LAMP_X - 2, EnergyConverterLayout.LAMP_Y
                                + i * EnergyConverterLayout.LAMP_PITCH - 2,
                        EnergyConverterLayout.LAMP_SPRITE, EnergyConverterLayout.LAMP_SPRITE);
            }
        }
    }

    /* The display: the power crossing, in watts, and under it the FE a tick, or that it is held at its limit. */
    private void display(final GuiGraphics g, final Colours c, final ConverterView view) {
        final int glow = view.running() ? c.lcd() : mix(c.lcd(), c.lcdGround(), 0.55F);
        final String power = GameText.resolve(JsEnergy.ENERGY.rate(view.movedJ()));
        Draw.text(g, this.font, power, x(EnergyConverterLayout.DISPLAY_TEXT_X), y(EnergyConverterLayout.DISPLAY_LINE_Y),
                glow);
        final Text second = view.limited() ? EnergyConverters.AT_LIMIT.text() : Energies.FE.rate(view.movedFe());
        Draw.textScaled(g, this.font, GameText.resolve(second), x(EnergyConverterLayout.DISPLAY_TEXT_X),
                y(EnergyConverterLayout.DISPLAY_SECOND_Y), mix(glow, c.lcdGround(), 0.25F),
                EnergyConverterLayout.SMALL);
    }

    /* The two meters: their scales printed for the limit, the needle at what crosses, trembling while it runs free. */
    private void meters(final GuiGraphics g, final Colours c, final ConverterView view, final long frame) {
        final double tremble = view.running() && !view.limited() ? 0.02 * Math.sin(frame * Math.PI / 4) : 0.0;
        final MeterScale fe = MeterScale.upTo(view.limitFe());
        meter(g, c, EnergyConverterLayout.FE_METER_X, fe, fe.fraction(view.movedFe()) + tremble);
        unit(g, c, GameText.resolve(EnergyConverters.FE_PER_TICK.with(fe.prefix())), EnergyConverterLayout.FE_UNIT_X);
        final MeterScale je = MeterScale.upTo(view.limitJ() * EnergyConverterBlockEntity.TICKS_PER_SECOND);
        meter(g, c, EnergyConverterLayout.JE_METER_X, je, je.fraction(view.movedJ()
                * EnergyConverterBlockEntity.TICKS_PER_SECOND) + tremble);
        unit(g, c, je.prefix() + GameText.resolve(JsEnergy.WATT_SYMBOL), EnergyConverterLayout.JE_UNIT_X);
    }

    private void meter(final GuiGraphics g, final Colours c, final int mx, final MeterScale scale,
                       final double fraction) {
        final String[] marks = {"0", scale.halfLabel(), scale.fullLabel()};
        final int foot = EnergyConverterLayout.METER_Y + EnergyConverterLayout.METER_HEIGHT - NEEDLE_FROM_FOOT;
        for (int i = 0; i < METER_MARKS; i++) {
            final int ty = foot - i * NEEDLE_TRAVEL / (METER_MARKS - 1);
            Draw.textScaled(g, this.font, marks[i], x(mx + EnergyConverterLayout.METER_NUMBER_X), y(ty - 2),
                    c.meterInk(), EnergyConverterLayout.SMALL);
        }
        final int ny = foot - (int) Math.round(Math.max(0.0, Math.min(1.0, fraction)) * NEEDLE_TRAVEL);
        final int right = mx + EnergyConverterLayout.METER_WIDTH;
        g.fill(x(mx + 1), y(ny + 1), x(right - 2), y(ny + 2), c.needleShadow());
        g.fill(x(mx), y(ny), x(right - 2), y(ny + 1), c.needle());
        g.fill(x(right - 3), y(ny - 1), x(right - 1), y(ny + 2), c.needleHub());
    }

    private void unit(final GuiGraphics g, final Colours c, final String text, final int centre) {
        engrave(g, c, text, centre - width(text) / 2, EnergyConverterLayout.UNIT_Y);
    }

    /* Through the window: the pulses along the busbars, the way it works, and the power modules' lamps, while it runs. */
    private void bridge(final GuiGraphics g, final Colours c, final ConverterView view, final long frame) {
        if (!view.running()) {
            return;
        }
        final int step = (int) (frame % PULSE_GAP);
        final int top = EnergyConverterLayout.BUSBAR_Y;
        for (int k = 0; k < PULSES; k++) {
            final int run = (k * PULSE_GAP + step) % EnergyConverterLayout.BUS_RUN;
            final int off = view.toJe() ? run : EnergyConverterLayout.BUS_RUN - run;
            final int fx = EnergyConverterLayout.FE_BUS_X + off;
            g.fill(x(off == 0 ? fx : fx - 1), y(top), x(fx + 1), y(top + 3), c.feRed());
            final int jx = EnergyConverterLayout.JE_BUS_X + off;
            if (jx < EnergyConverterLayout.JE_BUS_RIGHT) {
                g.fill(x(jx - 1), y(top), x(jx + 1), y(top + 3), c.jeGold());
            }
        }
        for (int k = 0; k < EnergyConverterLayout.MODULES; k++) {
            final int mx = EnergyConverterLayout.MODULE_X + k * EnergyConverterLayout.MODULE_PITCH;
            final boolean lit = (frame / 2 + (long) k * LED_STAGGER) % LED_CYCLE < LED_LIT;
            final int led = mx + EnergyConverterLayout.MODULE_LED_X;
            g.fill(x(led), y(EnergyConverterLayout.MODULE_LED_Y), x(led + 2), y(EnergyConverterLayout.MODULE_LED_Y
                    + 2), lit ? c.ledOn() : c.ledOff());
            if (lit) {
                final int glow = mix(c.feRed(), c.jeGold(), (k + 1) / (float) (EnergyConverterLayout.MODULES + 1));
                g.fill(x(mx + 3), y(EnergyConverterLayout.MODULE_GLOW_Y), x(mx + 15),
                        y(EnergyConverterLayout.MODULE_GLOW_Y + 1), glow);
            }
        }
    }

    /* A configurable converter's five class keys, its own lit; a fixed one's ceiling on a plate. */
    private void classes(final GuiGraphics g, final Colours c, final ConverterView view) {
        if (view.fixed()) {
            final long ceilingJ = ConverterLimit.ceiling(view.voltageClass())
                    / EnergyConverterBlockEntity.TICKS_PER_SECOND;
            plate(g, EnergyConverterLayout.KEY_X, EnergyConverterLayout.KEY_Y,
                    GameText.resolve(JsEnergy.ENERGY.rate(ceilingJ)), c.plate(), c.plateInk(), c.plateLip());
            return;
        }
        for (final VoltageClass each : VoltageClass.values()) {
            final int k = each.rank() - 1;
            final boolean lit = each == view.voltageClass();
            final int kx = EnergyConverterLayout.KEY_X + k * EnergyConverterLayout.KEY_PITCH;
            sprite(g, EnergyConverterLayout.KEY_SPRITE_X + (lit ? EnergyConverterLayout.KEY_SPRITE_PITCH : 0),
                    EnergyConverterLayout.SPRITES_Y, kx - 1, EnergyConverterLayout.KEY_Y - 1,
                    EnergyConverterLayout.KEY_SPRITE_WIDTH, EnergyConverterLayout.KEY_SPRITE_HEIGHT);
            final String legend = GameText.resolve(EnergyConverters.key(each));
            Draw.textScaled(g, this.font, legend, x(kx + (EnergyConverterLayout.KEY_WIDTH - width(legend)) / 2),
                    y(EnergyConverterLayout.KEY_Y + 3), lit ? c.keyInk() : c.keyInkOff(), EnergyConverterLayout.SMALL);
        }
    }

    /* The digits on the drums, and the prefix's letter on the fifth. */
    private void thumbwheels(final GuiGraphics g, final Colours c, final ConverterView view) {
        final ConverterLimit limit = view.limit();
        final String drums = limit.drumsText() + limit.prefixSymbol();
        for (int d = 0; d < EnergyConverterLayout.DRUMS; d++) {
            final String digit = drums.substring(d, d + 1);
            final int dx = EnergyConverterLayout.DRUM_X + d * EnergyConverterLayout.DRUM_PITCH;
            Draw.text(g, this.font, digit, x(dx + (EnergyConverterLayout.DRUM_WIDTH - this.font.width(digit) + 1) / 2),
                    y(EnergyConverterLayout.DRUM_Y + 2), c.pointer());
        }
    }

    /* The knob's pointer at the way it works, and the chosen position's mark lit. */
    private void knob(final GuiGraphics g, final Colours c, final ConverterView view) {
        final double angle = view.toJe() ? TO_JE_ANGLE : TO_FE_ANGLE;
        for (int s = KNOB_POINTER_FROM; s < EnergyConverterLayout.KNOB_RADIUS - 1; s++) {
            dot(g, EnergyConverterLayout.KNOB_X + (int) Math.round(s * Math.cos(angle)),
                    EnergyConverterLayout.KNOB_Y + (int) Math.round(s * Math.sin(angle)), c.pointer());
        }
        final int reach = EnergyConverterLayout.KNOB_RADIUS + KNOB_DOT_REACH;
        for (final boolean toJe : new boolean[] {false, true}) {
            final double at = toJe ? TO_JE_ANGLE : TO_FE_ANGLE;
            dot(g, EnergyConverterLayout.KNOB_X + (int) Math.round(reach * Math.cos(at)),
                    EnergyConverterLayout.KNOB_Y + (int) Math.round(reach * Math.sin(at)),
                    toJe == view.toJe() ? c.markOn() : c.markOff());
        }
    }

    /* The words cut in the steel, and the two ways over the knob, the chosen one lit. */
    private void words(final GuiGraphics g, final Colours c, final ConverterView view) {
        final TextKey[] lamps = {EnergyConverters.RUN, EnergyConverters.LIMIT, EnergyConverters.FAULT};
        for (int i = 0; i < lamps.length; i++) {
            engrave(g, c, GameText.resolve(lamps[i]), EnergyConverterLayout.LAMP_WORD_X,
                    EnergyConverterLayout.LAMP_WORD_Y + i * EnergyConverterLayout.LAMP_PITCH);
        }
        engrave(g, c, GameText.resolve(EnergyConverters.ON), EnergyConverterLayout.ON_WORD_X,
                EnergyConverterLayout.ON_WORD_Y);
        engrave(g, c, GameText.resolve(EnergyConverters.OFF), EnergyConverterLayout.ON_WORD_X,
                EnergyConverterLayout.OFF_WORD_Y);
        engrave(g, c, GameText.resolve(view.fixed() ? EnergyConverters.CEILING : EnergyConverters.CLASS),
                EnergyConverterLayout.CLASS_WORD_X, EnergyConverterLayout.WORD_ROW_Y);
        engrave(g, c, GameText.resolve(EnergyConverters.LIMIT), EnergyConverterLayout.LIMIT_WORD_X,
                EnergyConverterLayout.WORD_ROW_Y);
        engrave(g, c, GameText.resolve(JsEnergy.WATT_SYMBOL), EnergyConverterLayout.WHEEL_UNIT_X,
                EnergyConverterLayout.WHEEL_UNIT_Y);
        final String fe = GameText.resolve(EnergyConverters.FE);
        final String je = GameText.resolve(EnergyConverters.JE);
        way(g, c, EnergyConverterLayout.TO_FE_WORD_X, je, fe, !view.toJe());
        way(g, c, EnergyConverterLayout.TO_JE_WORD_X, fe, je, view.toJe());
    }

    /* One way: its two ends with an arrow between them, lit when it is the way chosen, cut in the steel otherwise. */
    private void way(final GuiGraphics g, final Colours c, final int wx, final String from, final String to,
                     final boolean chosen) {
        final int wy = EnergyConverterLayout.WAY_WORD_Y;
        int tx = wx;
        word(g, c, from, tx, wy, chosen);
        tx += width(from) + 1;
        final int arrow = chosen ? c.wayLit() : c.engraved();
        g.fill(x(tx), y(wy + 2), x(tx + ARROW_LENGTH), y(wy + 3), arrow);
        dot(g, tx + 3, wy + 1, arrow);
        dot(g, tx + 3, wy + 3, arrow);
        tx += ARROW_LENGTH + 1;
        word(g, c, to, tx, wy, chosen);
    }

    private void word(final GuiGraphics g, final Colours c, final String text, final int wx, final int wy,
                      final boolean lit) {
        if (lit) {
            Draw.textScaled(g, this.font, text, x(wx), y(wy), c.wayLit(), EnergyConverterLayout.SMALL);
        } else {
            engrave(g, c, text, wx, wy);
        }
    }

    /* Letters cut into the steel: dark, with the light catching their lower lip. */
    private void engrave(final GuiGraphics g, final Colours c, final String text, final int ex, final int ey) {
        Draw.textScaled(g, this.font, text, x(ex), y(ey + 1), c.lip(), EnergyConverterLayout.SMALL);
        Draw.textScaled(g, this.font, text, x(ex), y(ey), c.engraved(), EnergyConverterLayout.SMALL);
    }

    private void sprite(final GuiGraphics g, final int u, final int v, final int sx, final int sy, final int w,
                        final int h) {
        Draw.blended(() -> g.blit(PICTURE, x(sx), y(sy), u, v, w, h, EnergyConverterLayout.PICTURE_WIDTH,
                EnergyConverterLayout.PICTURE_HEIGHT));
    }

    private void dot(final GuiGraphics g, final int dx, final int dy, final int colour) {
        g.fill(x(dx), y(dy), x(dx + 1), y(dy + 1), colour);
    }

    private int width(final String text) {
        return (int) Math.ceil(this.font.width(text) * EnergyConverterLayout.SMALL);
    }

    private int x(final int x) {
        return this.leftPos + x;
    }

    private int y(final int y) {
        return this.topPos + y;
    }

    /* {@code from} moved {@code toward} the other colour by {@code amount}, its alpha kept. */
    private static int mix(final int from, final int toward, final float amount) {
        final int a = from >>> 24;
        final int r = channel(from >> 16 & 0xFF, toward >> 16 & 0xFF, amount);
        final int gr = channel(from >> 8 & 0xFF, toward >> 8 & 0xFF, amount);
        final int b = channel(from & 0xFF, toward & 0xFF, amount);
        return a << 24 | r << 16 | gr << 8 | b;
    }

    private static int channel(final int from, final int toward, final float amount) {
        return Math.round(from + (toward - from) * amount);
    }

    /**
     * The face's colours.
     *
     * @param engraved      the letters cut in the steel
     * @param lip           the light on their lower lip
     * @param plate         an aluminium plate
     * @param plateInk      its letters
     * @param plateLip      the light under them
     * @param gold          the class plate
     * @param goldInk       its letters
     * @param goldLip       the light under them
     * @param lcd           the display's words, lit
     * @param lcdGround     the display's ground
     * @param needle        a meter's needle
     * @param needleHub     the needle's hub
     * @param meterInk      a meter's numbers
     * @param pointer       the knob's pointer and the drums' digits
     * @param markOn        the knob's chosen position
     * @param markOff       its other position
     * @param wayLit        the way chosen, lit
     * @param keyInk        the legend on the lit class key
     * @param keyInkOff     the legend on the others
     * @param feRed         FE's pulses
     * @param jeGold        J's Energy's pulses
     * @param ledOn         a power module's lamp lit
     * @param ledOff        the same, out
     * @param inventoryTitle the inventory's word, as the game writes it
     * @param shadow        a plate's shadow on the steel
     * @param needleShadow  the needle's shadow on the meter's face
     * @param white         the light a plate's lit edges mix in
     * @param black         the dark its shaded edges and screws mix in
     */
    public record Colours(int engraved, int lip, int plate, int plateInk, int plateLip, int gold, int goldInk,
                          int goldLip, int lcd, int lcdGround, int needle, int needleHub, int meterInk, int pointer,
                          int markOn, int markOff, int wayLit, int keyInk, int keyInkOff, int feRed, int jeGold,
                          int ledOn, int ledOff, int inventoryTitle, int shadow, int needleShadow, int white,
                          int black) {
    }
}
