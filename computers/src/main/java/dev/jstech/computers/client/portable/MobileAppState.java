/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.portable;

import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.text.Text;
import org.jetbrains.annotations.Nullable;

/**
 * Where the apps of one portable are on its player's game: the network app's tab, the item or the craft chosen, how
 * many, what the search says and whether the deposit is open; the item the alerts are choosing or setting a level
 * for; the Messenger's room and what is being typed in it; the picture chosen in the gallery; what the camera last
 * did; how near the map is. Kept while the player is in the world, so an app opens where it was left.
 */
public final class MobileAppState {

    private int networkTab;
    @Nullable
    private StorageKey item;
    @Nullable
    private StorageKey picked;
    private long amount = DEFAULT_AMOUNT;
    private String search = "";
    private boolean depositing;
    private boolean pickingWatch;
    @Nullable
    private StorageKey watching;
    private String room = "";
    private String draft = "";
    private int picture = -1;
    private Text cameraNote = Text.EMPTY;
    private int zoom = 1;
    private Field typing = Field.NONE;
    private boolean menuOpen;
    private long askedAt = Long.MIN_VALUE;

    /** The tabs of the network app. */
    public static final int STOCK = 0;
    public static final int DEVICE = 1;
    public static final int CRAFT = 2;
    /** How many a request or a craft starts at. */
    public static final long DEFAULT_AMOUNT = 16L;
    /** The level a new watch starts at. */
    public static final long DEFAULT_LEVEL = 64L;
    /** The longest a search or a message runs. */
    public static final int MOST_TYPED = 200;
    private static final int MOST_ZOOM = 4;

    public int networkTab() {
        return networkTab;
    }

    @Nullable
    public StorageKey item() {
        return item;
    }

    /** The row picked in a list, on a look where a tap picks before the soft key opens it. */
    @Nullable
    public StorageKey picked() {
        return picked;
    }

    /** Picks {@code key} in a list. */
    public void pick(@Nullable final StorageKey key) {
        picked = key;
    }

    public long amount() {
        return amount;
    }

    public String search() {
        return search;
    }

    public boolean depositing() {
        return depositing;
    }

    public boolean pickingWatch() {
        return pickingWatch;
    }

    @Nullable
    public StorageKey watching() {
        return watching;
    }

    public String room() {
        return room;
    }

    public String draft() {
        return draft;
    }

    public int picture() {
        return picture;
    }

    public Text cameraNote() {
        return cameraNote;
    }

    /** How many blocks each pixel of the map covers: 1, 2 or 4. */
    public int zoom() {
        return zoom;
    }

    public Field typing() {
        return typing;
    }

    /** Whether Frames Pocket's Menu is open over the page. */
    public boolean menuOpen() {
        return menuOpen;
    }

    /** Opens the Menu, or closes it. */
    public void toggleMenu() {
        menuOpen = !menuOpen;
    }

    /** Closes the Menu, as choosing from it does. */
    public void closeMenu() {
        menuOpen = false;
    }

    /** Chooses the network app's tab {@code tab}, at its list. */
    public void networkTab(final int tab) {
        networkTab = tab;
        item = null;
        depositing = false;
        typing = Field.NONE;
    }

    /** Chooses {@code key} in the network app's stock or crafts, at the amount it starts at. */
    public void choose(@Nullable final StorageKey key) {
        item = key;
        amount = DEFAULT_AMOUNT;
        typing = Field.NONE;
    }

    /** Changes how many by {@code by}, never below one. */
    public void addAmount(final long by) {
        amount = Math.max(1L, amount + by);
    }

    /** Opens or closes the deposit. */
    public void deposit(final boolean open) {
        depositing = open;
    }

    /** Starts or stops choosing an item to watch. */
    public void pickWatch(final boolean picking) {
        pickingWatch = picking;
        typing = picking ? Field.SEARCH : Field.NONE;
    }

    /** Sets the level of {@code key}, or stops, with null. */
    public void watching(@Nullable final StorageKey key) {
        watching = key;
        pickingWatch = false;
        typing = Field.NONE;
    }

    /** Opens the Messenger's room {@code named}; empty for the list of rooms. */
    public void room(final String named) {
        room = named;
        typing = named.isEmpty() ? Field.NONE : Field.MESSAGE;
    }

    /** What was typed is sent: the draft starts again. */
    public void sent() {
        draft = "";
    }

    /** Chooses the gallery's picture numbered {@code number}; -1 for none. */
    public void picture(final int number) {
        picture = number;
    }

    /** What the camera last did, said under its shutter. */
    public void cameraNote(final Text note) {
        cameraNote = note;
    }

    /** Brings the map nearer, with {@code nearer}, or takes it further. */
    public void zoom(final boolean nearer) {
        zoom = nearer ? Math.max(1, zoom / 2) : Math.min(MOST_ZOOM, zoom * 2);
    }

    /** Types into {@code field}. */
    public void typeInto(final Field field) {
        typing = field;
    }

    /** Adds {@code letter} to what is being typed; false when nothing is. */
    public boolean type(final char letter) {
        switch (typing) {
            case SEARCH -> search = clip(search + letter);
            case MESSAGE -> draft = clip(draft + letter);
            case NONE -> {
                return false;
            }
        }
        return true;
    }

    /** Takes the last letter of what is being typed; false when nothing is. */
    public boolean erase() {
        switch (typing) {
            case SEARCH -> search = search.isEmpty() ? search : search.substring(0, search.length() - 1);
            case MESSAGE -> draft = draft.isEmpty() ? draft : draft.substring(0, draft.length() - 1);
            case NONE -> {
                return false;
            }
        }
        return true;
    }

    /**
     * Goes back a step inside an app, from an item to its list, from the deposit to the device, from a level to the
     * watched items, from a room to the rooms; false when the app is at its first page and the system goes back.
     */
    public boolean back() {
        if (menuOpen) {
            menuOpen = false;
            return true;
        }
        if (item != null || depositing) {
            item = null;
            depositing = false;
            typing = Field.NONE;
            return true;
        }
        if (pickingWatch || watching != null) {
            pickingWatch = false;
            watching = null;
            typing = Field.NONE;
            return true;
        }
        if (!room.isEmpty()) {
            room("");
            return true;
        }
        return false;
    }

    /** Whether it is time to ask again what the open app shows, every {@code every} ticks, at {@code now}. */
    public boolean dueToAsk(final long now, final int every) {
        // Never asked, or asked in another world whose clock is behind this one's: ask now.
        if (askedAt != Long.MIN_VALUE && now >= askedAt && now - askedAt < every) {
            return false;
        }
        askedAt = now;
        return true;
    }

    /** Asks again at the next chance: something was just done that changes what the app shows. */
    public void askSoon() {
        askedAt = Long.MIN_VALUE;
    }

    /* Typed words cut to what one search or message may hold. */
    private static String clip(final String typed) {
        return typed.length() > MOST_TYPED ? typed.substring(0, MOST_TYPED) : typed;
    }

    /** What the keys type into. */
    public enum Field {
        NONE,
        SEARCH,
        MESSAGE
    }
}
