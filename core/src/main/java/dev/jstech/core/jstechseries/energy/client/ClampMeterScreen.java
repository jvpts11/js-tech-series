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
import dev.jstech.core.energy.Energies;
import dev.jstech.core.energy.Energy;
import dev.jstech.core.energy.EnergyClass;
import dev.jstech.core.energy.EnergyReadings;
import dev.jstech.core.jstechseries.energy.ClampMeter;
import dev.jstech.core.jstechseries.energy.ClampMeterLayout;
import dev.jstech.core.jstechseries.energy.MeterReading;
import dev.jstech.core.jstechseries.energy.MeterState;
import dev.jstech.core.jstechseries.energy.MeterStatePayload;
import dev.jstech.core.jstechseries.energy.MeterView;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextKey;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The clamp meter held up before the player over the darkened world, as the game's book is held: its jaws, its orange
 * holster, its display with the reading, and its keys and dial, which set it as on the real instrument. No inventory.
 */
@PaletteHolder
public final class ClampMeterScreen extends Screen {

    private final InteractionHand hand;
    private MeterState state;
    private int left;
    private int top;

    /** The instrument's picture, drawn as the approved design draws it, with no words. */
    public static final ResourceLocation PICTURE = ResourceLocation.fromNamespaceAndPath(JsCore.MODID,
            "textures/gui/clamp_meter.png");
    /** The instrument's colours. */
    public static final Palette<Colours> PALETTE = Palettes.declare(JsCore.MODID, "clamp_meter", new Colours(
            0xFF << 24 | 0xD6DAE0, 0xFF << 24 | 0xE0E4E8, 0xFF << 24 | 0x828A96, 0xFF << 24 | 0xFFBE3C,
            0xFF << 24 | 0x5FE07A, 0xFF << 24 | 0x007680, 0xFF << 24 | 0xC8F0F0, 0xFF << 24 | 0xFFFFFF,
            0xFF << 24 | 0x321E00, 0xFF << 24 | 0x001E22, 0xFF << 24 | 0x1E4628, 0xFF << 24 | 0x16202C,
            0x60 << 24));
    private static final int POINTER_FROM = 3;
    private static final int HEADER_GAP = 4;
    private static final int LABEL_HALF_HEIGHT = 3;
    private static final int TICKS_PER_SECOND = 20;
    private static final int SECONDS_PER_MINUTE = 60;

    /** The meter in {@code hand}, held up. */
    public ClampMeterScreen(final InteractionHand hand) {
        super(GameText.component(ClampMeter.TITLE));
        this.hand = hand;
        this.state = ClampMeter.state(held());
    }

    @Override
    protected void init() {
        this.left = (this.width - ClampMeterLayout.WIDTH) / 2;
        this.top = (this.height - ClampMeterLayout.HEIGHT) / 2;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        if (!held().is(ClampMeter.ITEM.get())) {
            onClose();
        }
    }

    @Override
    public void render(final GuiGraphics g, final int mouseX, final int mouseY, final float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        final Colours c = PALETTE.get();
        g.blit(PICTURE, this.left, this.top, 0, 0, ClampMeterLayout.WIDTH, ClampMeterLayout.HEIGHT,
                ClampMeterLayout.WIDTH, ClampMeterLayout.HEIGHT);
        final MeterReading reading = MeterView.shown(this.state.hold());
        final boolean clamped = held().get(ClampMeter.CLAMP.get()) != null;
        small(g, ClampMeter.TITLE.text(), ClampMeterLayout.TITLE_X, ClampMeterLayout.TITLE_Y, c.silk());
        g.fill(x(ClampMeterLayout.LAMP_X), y(ClampMeterLayout.LAMP_Y),
                x(ClampMeterLayout.LAMP_X + ClampMeterLayout.LAMP_SIZE),
                y(ClampMeterLayout.LAMP_Y + ClampMeterLayout.LAMP_SIZE),
                clamped && reading.present() ? c.green() : c.lampOff());
        smallRight(g, ClampMeter.CLAMPED.text(), ClampMeterLayout.CLAMPED_RIGHT, ClampMeterLayout.TITLE_Y, c.silk());
        display(g, c, reading, clamped);
        keys(g, c);
        dial(g, c);
    }

    @Override
    public void renderBackground(final GuiGraphics g, final int mouseX, final int mouseY, final float partialTick) {
        // The world stays in view behind the instrument, darkened, as behind the game's book.
        renderTransparentBackground(g);
    }

    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        final double x = mouseX - this.left;
        final double y = mouseY - this.top;
        final MeterState was = this.state;
        if (ClampMeterLayout.onKey(x, y, ClampMeterLayout.HOLD_X, ClampMeterLayout.HOLD_Y)) {
            this.state = this.state.toggleHold();
        } else if (ClampMeterLayout.onKey(x, y, ClampMeterLayout.MAX_MIN_X, ClampMeterLayout.MAX_MIN_Y)) {
            this.state = this.state.toggleMaxMin();
        } else if (ClampMeterLayout.onKey(x, y, ClampMeterLayout.SHOW_X, ClampMeterLayout.SHOW_Y)) {
            this.state = this.state.toggleShow();
        } else if (ClampMeterLayout.onKey(x, y, ClampMeterLayout.LIGHT_X, ClampMeterLayout.LIGHT_Y)) {
            this.state = this.state.toggleLight();
        } else if (ClampMeterLayout.onDial(x, y)) {
            this.state = this.state.turned();
        }
        if (this.state.equals(was)) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        held().set(ClampMeter.STATE.get(), this.state);
        PacketDistributor.sendToServer(new MeterStatePayload(this.hand == InteractionHand.MAIN_HAND, this.state));
        return true;
    }

    /** The meter as the player holds it now. */
    public MeterState state() {
        return this.state;
    }

    private ItemStack held() {
        final Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player == null ? ItemStack.EMPTY : minecraft.player.getItemInHand(this.hand);
    }

    /* The display: dark with the dial off, a word when there is nothing to read, the reading otherwise. */
    private void display(final GuiGraphics g, final Colours c, final MeterReading reading, final boolean clamped) {
        if (this.state.dial() == MeterState.Dial.OFF) {
            return;
        }
        if (this.state.light()) {
            g.fill(x(ClampMeterLayout.DISPLAY_LEFT), y(ClampMeterLayout.DISPLAY_TOP),
                    x(ClampMeterLayout.DISPLAY_RIGHT), y(ClampMeterLayout.DISPLAY_BOTTOM), c.lit());
        }
        if (!clamped || !reading.present()) {
            small(g, (clamped ? ClampMeter.LOST : ClampMeter.NOT_CLAMPED).text(), ClampMeterLayout.TEXT_LEFT,
                    ClampMeterLayout.VALUE_Y, c.dim());
            return;
        }
        final Optional<Energy> energy = Energies.byId(reading.energy());
        if (energy.isEmpty()) {
            return;
        }
        header(g, c, reading, energy.get());
        if (reading.line()) {
            line(g, c, reading, energy.get());
        } else {
            block(g, c, reading, energy.get());
        }
    }

    private void header(final GuiGraphics g, final Colours c, final MeterReading reading, final Energy energy) {
        g.fill(x(ClampMeterLayout.DISPLAY_LEFT), y(ClampMeterLayout.DISPLAY_TOP), x(ClampMeterLayout.DISPLAY_RIGHT),
                y(ClampMeterLayout.DISPLAY_TOP + ClampMeterLayout.HEADER_HEIGHT), c.head());
        final Optional<EnergyClass> lineClass = energy.classNamed(reading.lineClass());
        int ratedWidth = 0;
        if (!reading.lineClass().isEmpty() && lineClass.isPresent()) {
            final Text rated = Text.literal(lineClass.get().ratingKey().map(rating -> GameText.resolve(rating.text()))
                    .orElse(""));
            ratedWidth = width(rated) + HEADER_GAP;
            smallRight(g, rated, ClampMeterLayout.DISPLAY_RIGHT - 3, ClampMeterLayout.DISPLAY_TOP + 3,
                    c.headText());
        }
        // A long name is cut where the class begins, so the two never run into each other.
        final int room = ClampMeterLayout.DISPLAY_RIGHT - 3 - ratedWidth - (ClampMeterLayout.DISPLAY_LEFT + 3);
        final String name = this.font.plainSubstrByWidth(subjectName(reading).toUpperCase(Locale.ROOT), room);
        Draw.text(g, this.font, name, x(ClampMeterLayout.DISPLAY_LEFT + 3), y(ClampMeterLayout.DISPLAY_TOP + 2),
                c.white());
    }

    private void line(final GuiGraphics g, final Colours c, final MeterReading reading, final Energy energy) {
        final boolean fe = this.state.dial() == MeterState.Dial.FE;
        final String value = GameText.resolve(rate(energy, reading, reading.perTick(), fe));
        Draw.textScaled(g, this.font, value, x(ClampMeterLayout.TEXT_LEFT), y(ClampMeterLayout.VALUE_Y), c.white(),
                ClampMeterLayout.VALUE_SCALE);
        reading.toward().ifPresent(face -> small(g, ClampMeter.toward(face).text(), ClampMeterLayout.TEXT_LEFT
                + (int) Math.ceil(this.font.width(value) * ClampMeterLayout.VALUE_SCALE) + 4,
                ClampMeterLayout.VALUE_Y + 4, c.dim()));
        small(g, ClampMeter.EACH_SECOND.with(amount(energy, reading, Energy.perSecond(reading.perTick()), fe)),
                ClampMeterLayout.TEXT_LEFT, ClampMeterLayout.EACH_SECOND_Y, c.text());
        small(g, ClampMeter.SINCE.with(amount(energy, reading, reading.total(), fe), clock(reading.ticks())),
                ClampMeterLayout.TEXT_LEFT, ClampMeterLayout.SINCE_Y, c.text());
        if (reading.ceiling() == Long.MAX_VALUE) {
            small(g, ClampMeter.NO_CEILING.text(), ClampMeterLayout.TEXT_LEFT, ClampMeterLayout.CEILING_Y, c.text());
        } else {
            final Text ceiling = ClampMeter.CEILING.with(rate(energy, reading, reading.ceiling(), fe));
            small(g, ceiling, ClampMeterLayout.TEXT_LEFT, ClampMeterLayout.CEILING_Y, c.text());
            if (reading.perTick() >= reading.ceiling()) {
                small(g, ClampMeter.FULL.text(), ClampMeterLayout.TEXT_LEFT + width(ceiling) + 3,
                        ClampMeterLayout.CEILING_Y, c.amber());
            }
        }
        if (this.state.maxMin()) {
            small(g, ClampMeter.MOST_LEAST.with(rate(energy, reading, reading.most(), fe),
                    rate(energy, reading, reading.least(), fe)), ClampMeterLayout.TEXT_LEFT,
                    ClampMeterLayout.MOST_LEAST_Y, c.dim());
        }
    }

    private void block(final GuiGraphics g, final Colours c, final MeterReading reading, final Energy energy) {
        final boolean fe = this.state.dial() == MeterState.Dial.FE;
        final TextKey trend = reading.perTick() > 0 ? ClampMeter.FILLING
                : reading.perTick() < 0 ? ClampMeter.EMPTYING : ClampMeter.STEADY;
        small(g, ClampMeter.STORED.with(amount(energy, reading, reading.stored(), fe),
                amount(energy, reading, reading.capacity(), fe), trend.text()), ClampMeterLayout.TEXT_LEFT,
                ClampMeterLayout.STORED_Y, c.text());
        int row = 0;
        for (final MeterReading.Face face : reading.faces()) {
            final int y = ClampMeterLayout.FACES_Y + row * ClampMeterLayout.FACE_PITCH;
            final boolean port = face.kind() != EnergyReadings.FaceReading.Kind.NO_PORT;
            small(g, ClampMeter.face(face.face()).text(), ClampMeterLayout.TEXT_LEFT, y, port ? c.text() : c.dim());
            switch (face.kind()) {
                case IN -> {
                    small(g, ClampMeter.IN.text(), ClampMeterLayout.FACE_WAY_X, y, c.green());
                    smallRight(g, rate(energy, reading, face.perTick(), fe), ClampMeterLayout.TEXT_RIGHT, y,
                            c.green());
                }
                case OUT -> {
                    small(g, ClampMeter.OUT.text(), ClampMeterLayout.FACE_WAY_X, y, c.amber());
                    smallRight(g, rate(energy, reading, face.perTick(), fe), ClampMeterLayout.TEXT_RIGHT, y,
                            c.amber());
                }
                case IDLE -> smallRight(g, ClampMeter.IDLE.text(), ClampMeterLayout.TEXT_RIGHT, y, c.dim());
                case NO_PORT -> smallRight(g, ClampMeter.NO_PORT.text(), ClampMeterLayout.TEXT_RIGHT, y, c.dim());
            }
            row++;
        }
    }

    /* The four keys' words, each key sunk a pixel while it is on. */
    private void keys(final GuiGraphics g, final Colours c) {
        key(g, c, ClampMeter.HOLD, ClampMeterLayout.HOLD_X, ClampMeterLayout.HOLD_Y, this.state.hold(),
                c.amberInk());
        key(g, c, ClampMeter.MAX_MIN, ClampMeterLayout.MAX_MIN_X, ClampMeterLayout.MAX_MIN_Y, this.state.maxMin(),
                c.silk());
        key(g, c, ClampMeter.SHOW, ClampMeterLayout.SHOW_X, ClampMeterLayout.SHOW_Y, this.state.show(), c.tealInk());
        key(g, c, ClampMeter.LIGHT, ClampMeterLayout.LIGHT_X, ClampMeterLayout.LIGHT_Y, this.state.light(),
                c.silk());
    }

    private void key(final GuiGraphics g, final Colours c, final TextKey label, final int keyX, final int keyY,
                     final boolean on, final int ink) {
        if (on) {
            g.fill(x(keyX), y(keyY), x(keyX + ClampMeterLayout.KEY_WIDTH), y(keyY + ClampMeterLayout.KEY_HEIGHT),
                    c.pressed());
        }
        final String word = GameText.resolve(label);
        final int w = (int) Math.ceil(this.font.width(word) * ClampMeterLayout.SMALL);
        Draw.textScaled(g, this.font, word, x(keyX + (ClampMeterLayout.KEY_WIDTH - w) / 2),
                y(keyY + (on ? 4 : 3)), ink, ClampMeterLayout.SMALL);
    }

    /* The dial's pointer at its position, and its three positions printed round it, the chosen one lit. */
    private void dial(final GuiGraphics g, final Colours c) {
        final double angle = switch (this.state.dial()) {
            case OFF -> Math.PI;
            case OWN -> Math.PI * 1.5;
            case FE -> 0.0;
        };
        for (int s = POINTER_FROM; s < ClampMeterLayout.DIAL_RADIUS - 2; s++) {
            final int px = ClampMeterLayout.DIAL_X + (int) Math.round(s * Math.cos(angle));
            final int py = ClampMeterLayout.DIAL_Y + (int) Math.round(s * Math.sin(angle));
            g.fill(x(px), y(py), x(px + 1), y(py + 1), c.white());
        }
        position(g, ClampMeter.DIAL_OFF, Math.PI, this.state.dial() == MeterState.Dial.OFF, c);
        position(g, ClampMeter.DIAL_OWN, Math.PI * 1.5, this.state.dial() == MeterState.Dial.OWN, c);
        position(g, ClampMeter.DIAL_FE, 0.0, this.state.dial() == MeterState.Dial.FE, c);
    }

    private void position(final GuiGraphics g, final TextKey label, final double angle, final boolean chosen,
                          final Colours c) {
        final String word = GameText.resolve(label);
        final int w = (int) Math.ceil(this.font.width(word) * ClampMeterLayout.SMALL);
        final int cx = ClampMeterLayout.DIAL_X + (int) Math.round(ClampMeterLayout.DIAL_LABEL_REACH * Math.cos(angle));
        final int cy = ClampMeterLayout.DIAL_Y + (int) Math.round((ClampMeterLayout.DIAL_LABEL_REACH - 2)
                * Math.sin(angle)) - LABEL_HALF_HEIGHT;
        Draw.textScaled(g, this.font, word, x(cx - w / 2), y(cy), chosen ? c.amber() : c.silk(),
                ClampMeterLayout.SMALL);
    }

    private String subjectName(final MeterReading reading) {
        if (reading.line()) {
            return BuiltInRegistries.ITEM.getOptional(reading.subject()).map(item -> new ItemStack(item)
                    .getHoverName().getString()).orElse(reading.subject().toString());
        }
        return BuiltInRegistries.BLOCK.getOptional(reading.subject()).map(block -> block.getName().getString())
                .orElse(reading.subject().toString());
    }

    private void small(final GuiGraphics g, final Text text, final int x, final int y, final int colour) {
        Draw.textScaled(g, this.font, GameText.resolve(text), x(x), y(y), colour, ClampMeterLayout.SMALL);
    }

    private void smallRight(final GuiGraphics g, final Text text, final int right, final int y, final int colour) {
        small(g, text, right - width(text), y, colour);
    }

    private int width(final Text text) {
        return (int) Math.ceil(this.font.width(GameText.resolve(text)) * ClampMeterLayout.SMALL);
    }

    private int x(final int x) {
        return this.left + x;
    }

    private int y(final int y) {
        return this.top + y;
    }

    /* {@code perTick} a tick, in the energy's own unit or in FE as the dial says. */
    private static Text rate(final Energy energy, final MeterReading reading, final long perTick, final boolean fe) {
        return fe ? Energies.FE.rate(reading.ratio().toFe(Math.max(0L, perTick))) : energy.rate(Math.max(0L,
                perTick));
    }

    private static Text amount(final Energy energy, final MeterReading reading, final long amount,
                               final boolean fe) {
        return fe ? Energies.FE.amount(reading.ratio().toFe(Math.max(0L, amount))) : energy.amount(Math.max(0L,
                amount));
    }

    /* {@code ticks} as minutes and seconds: "4:00". */
    private static Text clock(final long ticks) {
        final long seconds = ticks / TICKS_PER_SECOND;
        return Text.literal(seconds / SECONDS_PER_MINUTE + ":" + String.format(Locale.ROOT, "%02d",
                seconds % SECONDS_PER_MINUTE));
    }

    /**
     * The instrument's colours.
     *
     * @param silk     the printed words on its face
     * @param text     the display's words
     * @param dim      the display's quieter words
     * @param amber    a full line, and energy going out
     * @param green    energy going in, and the lamp lit
     * @param head     the display's header
     * @param headText the class beside the header's name
     * @param white    the header's name and the big reading
     * @param amberInk the word on the amber key
     * @param tealInk  the word on the teal key
     * @param lampOff  the lamp unlit
     * @param lit      the display lit
     * @param pressed  the shade over a key that is on
     */
    public record Colours(int silk, int text, int dim, int amber, int green, int head, int headText, int white,
                          int amberInk, int tealInk, int lampOff, int lit, int pressed) {
    }
}
