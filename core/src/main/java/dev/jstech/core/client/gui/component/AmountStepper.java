/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.gui.component;

import dev.jstech.core.client.gui.logic.StepperMath;
import net.minecraft.client.gui.GuiGraphics;

import java.util.function.LongConsumer;

/**
 * An amount with buttons to step it by one and by a factor of two either way, and the amount itself a field that
 * takes digits: {@code -  /2  [value]  x2  +}. The value stays inside its range and the buttons that would leave it
 * are disabled.
 */
public final class AmountStepper extends Panel {

    private static final int BUTTON_W = 18;
    private static final int GAP = 2;
    /* Enough digits for any amount a long holds. */
    private static final int DIGITS = 19;

    private final Button minus;
    private final Button halve;
    private final TextField value;
    private final Button twice;
    private final Button plus;
    private long amount;
    private long min = 1;
    private long max = Long.MAX_VALUE / 4;
    private LongConsumer onChange = v -> { };

    public AmountStepper() {
        minus = add(new Button("-", () -> set(StepperMath.decrement(amount, min, max))));
        halve = add(new Button("/2", () -> set(StepperMath.halved(amount, min, max))));
        value = add(new DigitField().setOnEdit(this::typed));
        twice = add(new Button("x2", () -> set(StepperMath.doubled(amount, min, max))));
        plus = add(new Button("+", () -> set(StepperMath.increment(amount, min, max))));
    }

    /** The amount, what is being typed into it included. */
    public long amount() {
        return amount;
    }

    /** Sets the amount from outside, without firing the change callback. */
    public AmountStepper setAmount(final long v) {
        amount = StepperMath.clamp(v, min, max);
        return this;
    }

    public AmountStepper setRange(final long minimum, final long maximum) {
        min = minimum;
        max = Math.max(minimum, maximum);
        amount = StepperMath.clamp(amount, min, max);
        return this;
    }

    public AmountStepper setOnChange(final LongConsumer action) {
        onChange = action;
        return this;
    }

    /** Hands the keyboard to the amount with all of it selected, so the digits typed next replace it. */
    public AmountStepper editAmount() {
        value.set(Long.toString(amount));
        focus(value);
        value.selectAll();
        return this;
    }

    @Override
    public void render(final GuiGraphics g, final UiContext ctx) {
        // The field shows the amount whenever it is not being typed into, however the amount last changed.
        value.sync(Long.toString(amount));
        super.render(g, ctx);
    }

    @Override
    public UiComponent setBounds(final int x, final int y, final int width, final int height) {
        super.setBounds(x, y, width, height);
        final FlowLayout left = FlowLayout.row(x, y, GAP);
        left.place(minus, BUTTON_W, height);
        left.place(halve, BUTTON_W, height);
        plus.setBounds(x + width - BUTTON_W, y, BUTTON_W, height);
        twice.setBounds(x + width - BUTTON_W * 2 - GAP, y, BUTTON_W, height);
        final int valueX = left.x();
        value.setBounds(valueX, y, twice.x() - GAP - valueX, height);
        minus.setEnabled(amount > min);
        halve.setEnabled(amount > min);
        twice.setEnabled(amount < max);
        plus.setEnabled(amount < max);
        return this;
    }

    private void set(final long v) {
        if (v != amount) {
            amount = v;
            onChange.accept(amount);
        }
    }

    /*
     * Every digit typed is the amount at once, kept inside the range, so a button pressed while the field still has
     * the keyboard reads what was typed. An empty field leaves the amount as it was until a digit comes.
     */
    private void typed() {
        final String digits = value.edit();
        if (!digits.isEmpty()) {
            set(StepperMath.typed(digits, min, max));
        }
    }

    /* The amount's field: it takes digits and nothing else. */
    private static final class DigitField extends TextField {

        DigitField() {
            super(DIGITS);
        }

        @Override
        protected boolean accepts(final char c) {
            return c >= '0' && c <= '9';
        }
    }
}
