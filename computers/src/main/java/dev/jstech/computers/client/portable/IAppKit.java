/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.portable;

import dev.jstech.computers.portable.MobileApp;
import dev.jstech.computers.portable.MobileSystem;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * How a family of mobile systems draws the inside of its apps (computers/docs/design/wireless.md): the K0.11 apps page
 * drew each app in one family's look, and each family draws every app with its own parts, in its year's look: the
 * bar with the app's name, the tabs, the lists and their rows, the buttons, the switches, the fields typed into. An app
 * says what it shows and what each part does; the kit says how it looks and marks what can be pressed.
 *
 * <p>Frames Pocket keeps its tabs and its actions in the Menu of its right soft key, as the Pocket PC did; the others
 * draw them on the page.
 */
public sealed interface IAppKit permits FramesPocketKit, QosKit, DroidixKit {

    /** The kit of {@code system}. */
    static IAppKit of(final MobileSystem system) {
        return switch (system.family()) {
            case FRAMES_POCKET -> new FramesPocketKit(system);
            case QOS -> new QosKit(system);
            case DROIDIX -> new DroidixKit(system);
        };
    }

    /** Whether {@code app}'s page is dark in this look, which its status bar is drawn over. */
    boolean dark(MobileApp app);

    /** Fills the page of {@code app} with its ground. */
    void ground(MobileCanvas c, MobileApp app);

    /** The colour of a tone in this look. */
    int ink(Tone tone);

    /**
     * Draws the app's bar from {@code top}: its title, an arrow back when {@code back}, and {@code right} at its end;
     * returns where the page under it starts.
     */
    float bar(MobileCanvas c, MobileApp app, float top, String title, @Nullable String right, boolean back);

    /**
     * Draws the tabs {@code labels} from {@code y}, {@code on} chosen; returns where the page under them starts. On
     * Frames Pocket they go in the Menu, and nothing is drawn.
     */
    float tabs(MobileCanvas c, float y, List<String> labels, int on, List<MobileTap> taps);

    /**
     * The heads of a list's two columns, as Frames Pocket's lists have them; nothing on the others. Returns where
     * the list starts.
     */
    float columns(MobileCanvas c, float y, String left, String right);

    /** A section's name, in capitals; returns where what is under it starts. */
    float section(MobileCanvas c, float y, String label);

    /**
     * A row of a list from {@code y}: an item's icon when there is one, its name and its value on the right in
     * {@code tone}; {@code chosen} marks the one picked. Returns where the next row starts.
     */
    float row(MobileCanvas c, float y, @Nullable ItemStack icon, String name, String value, Tone tone,
              @Nullable MobileTap tap, boolean chosen);

    /** A button {@code w} wide at {@code (x, y)}, filled when {@code primary}; returns its foot. */
    float button(MobileCanvas c, float x, float y, float w, String label, boolean primary, MobileTap tap);

    /** A row with a switch at its end, on or off; returns where the next row starts. */
    float toggle(MobileCanvas c, float y, String label, boolean on, MobileTap tap);

    /** A field typed into, {@code w} wide, showing its words or, when none, what goes in it; returns its foot. */
    float field(MobileCanvas c, float x, float y, float w, String typed, String placeholder, boolean focused,
                MobileTap tap);

    /** A bar {@code w} wide filled to {@code share}, from 0 to 1, in {@code tone}. */
    void meter(MobileCanvas c, float x, float y, float w, float share, Tone tone);

    /** The ground of a card from {@code (x, y)}, {@code w} by {@code h}: a panel of its own where the look has one. */
    void card(MobileCanvas c, float x, float y, float w, float h);

    /** A line of small words, centred across the page. */
    void note(MobileCanvas c, float y, String words, Tone tone);

    /** Where an app's page ends, above what {@link #keys} draws at its foot. */
    float bottom(MobileCanvas c);

    /**
     * The foot of the page: Frames Pocket's soft keys, the left one doing {@code left}, the right one opening the
     * Menu of {@code menu}, {@code chosen} of it marked; the home bar where a qOS device has no home button; nothing
     * on the others.
     */
    void keys(MobileCanvas c, @Nullable Entry left, List<Entry> menu, int chosen, boolean menuOpen,
              MobileTap toggleMenu);

    /** How far apart the rows of a list are. */
    float rowPitch();

    /**
     * Whether a tap on a list's row only picks it, the left soft key opening what is picked, as Frames Pocket's lists
     * worked; false where a tap opens the row at once.
     */
    boolean picksBeforeOpening();

    /** The tones a page's parts are drawn in. */
    enum Tone {
        /** What a page says. */
        TEXT,
        /** What is said under it, or beside it, quieter. */
        DIM,
        /** The look's own colour: links, what is chosen. */
        ACCENT,
        /** What is well. */
        GOOD,
        /** What wants looking at. */
        WARN,
        /** What is wrong. */
        BAD,
        /** The rules between rows. */
        LINE,
        /** What a card or a bubble is filled with, over the page's ground. */
        SURFACE
    }

    /** A soft key's word, or an entry of the Menu, and what pressing it does. */
    record Entry(String label, MobileTap tap) {
    }
}
