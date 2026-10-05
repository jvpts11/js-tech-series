/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.testkit;

import dev.jstech.computers.api.ComponentKind;
import dev.jstech.computers.api.ComputersRegisterEvent;
import dev.jstech.computers.api.IComponentValidator;
import dev.jstech.tests.JsTests;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;

/**
 * Two kinds of generic component registered the way another mod registers them, through the computers' register
 * event: a dial, which holds an angle and is turned by a player, and a page of the web, which reaches outside the
 * game and so stays off unless the server and the player turn it on.
 */
public final class TestComponents {

    /** A dial: its value is an angle from 0 to 359, and the one thing a player does to it is turn it. */
    public static final ResourceLocation DIAL = ResourceLocation.fromNamespaceAndPath(JsTests.MODID, "dial");
    /** A page of the web, which no test ever shows. */
    public static final ResourceLocation BROWSER = ResourceLocation.fromNamespaceAndPath(JsTests.MODID, "browser");
    /** What a player does to a dial. */
    public static final String TURN = "turn";
    /** The most degrees a dial goes round. */
    private static final int FULL_TURN = 360;

    private TestComponents() {
    }

    /** Registers both kinds while the game loads. */
    public static void register(final IEventBus modEventBus) {
        modEventBus.addListener(ComputersRegisterEvent.class, event -> {
            event.componentKind(new ComponentKind(DIAL, false, new IComponentValidator() {
                @Override
                public boolean data(final Object value) {
                    return value instanceof Integer angle && angle >= 0 && angle < FULL_TURN;
                }

                @Override
                public boolean action(final String name, final Object value) {
                    return TURN.equals(name) && value instanceof Integer by && Math.abs(by) < FULL_TURN;
                }
            }));
            event.componentKind(new ComponentKind(BROWSER, true, IComponentValidator.ANYTHING));
        });
    }
}
