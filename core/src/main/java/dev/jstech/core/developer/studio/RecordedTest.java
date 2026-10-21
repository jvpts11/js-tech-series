/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.developer.studio;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * A client test kept from what a player did on the screens, as the studio's "Record a test" keeps it: the block used to
 * open a screen, each click on a component by its name (or a point, on a screen not declared in a file), what was
 * typed, the keys pressed, the waits, the pictures taken and the screen closed. Written as a file of its mod's,
 * {@code assets/<mod>/jstech/client_tests/<name>.json}, which the test kit runs among the mod's client tests.
 *
 * @param mod   the mod whose test it is
 * @param name  its name, lower case letters, digits and underscores
 * @param steps what it does, in order
 */
public record RecordedTest(String mod, String name, List<Step> steps) {

    /** Where a mod's recorded tests are, in its assets. */
    public static final String FOLDER = "jstech/client_tests";

    private static final Pattern NAME = Pattern.compile("[a-z0-9_]+");
    private static final String MOD = "mod";
    private static final String TEST = "name";
    private static final String STEPS = "steps";
    private static final String USE = "use";
    private static final String CLICK = "click";
    private static final String CLICK_AT = "click_at";
    private static final String TYPE = "type";
    private static final String KEY = "key";
    private static final String WAIT = "wait";
    private static final String PICTURE = "picture";
    private static final String CLOSE = "close";

    public RecordedTest {
        Objects.requireNonNull(mod, "mod");
        if (!NAME.matcher(name).matches()) {
            throw new IllegalArgumentException("a recorded test's name is lower case letters, digits and underscores;"
                    + " got " + name);
        }
        steps = List.copyOf(steps);
    }

    /** Whether {@code name} may name a recorded test. */
    public static boolean validName(final String name) {
        return NAME.matcher(name).matches();
    }

    /** The test as its file writes it. */
    public JsonObject toJson() {
        final JsonObject out = new JsonObject();
        out.addProperty(MOD, this.mod);
        out.addProperty(TEST, this.name);
        final JsonArray written = new JsonArray();
        for (final Step step : this.steps) {
            written.add(step.toJson());
        }
        out.add(STEPS, written);
        return out;
    }

    /**
     * The test its file writes.
     *
     * @throws IllegalArgumentException when the file is not of that form, naming what is wrong
     */
    public static RecordedTest fromJson(final JsonObject json) {
        if (!json.has(MOD) || !json.has(TEST) || !(json.get(STEPS) instanceof JsonArray steps)) {
            throw new IllegalArgumentException("a recorded test has a mod, a name and its steps");
        }
        final List<Step> read = new ArrayList<>();
        for (final JsonElement step : steps) {
            if (!(step instanceof JsonObject object) || object.size() != 1) {
                throw new IllegalArgumentException("each step of a recorded test is an object of one field: " + step);
            }
            read.add(stepOf(object));
        }
        return new RecordedTest(json.get(MOD).getAsString(), json.get(TEST).getAsString(), read);
    }

    private static Step stepOf(final JsonObject step) {
        final String kind = step.keySet().iterator().next();
        final JsonElement value = step.get(kind);
        return switch (kind) {
            case USE -> new Use(value.getAsString());
            case CLICK -> new Click(value.getAsString());
            case CLICK_AT -> new ClickAt(value.getAsJsonArray().get(0).getAsInt(),
                    value.getAsJsonArray().get(1).getAsInt());
            case TYPE -> new Type(value.getAsString());
            case KEY -> new Key(value.getAsInt());
            case WAIT -> new Wait(value.getAsInt());
            case PICTURE -> new Picture(value.getAsString());
            case CLOSE -> new Close();
            default -> throw new IllegalArgumentException("a recorded test has no step " + kind);
        };
    }

    private static JsonObject one(final String kind, final JsonElement value) {
        final JsonObject out = new JsonObject();
        out.add(kind, value);
        return out;
    }

    private static JsonObject one(final String kind, final String value) {
        final JsonObject out = new JsonObject();
        out.addProperty(kind, value);
        return out;
    }

    private static JsonObject one(final String kind, final int value) {
        final JsonObject out = new JsonObject();
        out.addProperty(kind, value);
        return out;
    }

    /** One thing a recorded test does. */
    public sealed interface Step permits Use, Click, ClickAt, Type, Key, Wait, Picture, Close {

        /** The step as its file writes it: an object of one field, its kind. */
        JsonObject toJson();
    }

    /**
     * Uses a block of that id, put in front of the player, as a right click does: how a machine's screen opens.
     *
     * @param block the block's id
     */
    public record Use(String block) implements Step {

        @Override
        public JsonObject toJson() {
            return one(USE, this.block);
        }
    }

    /**
     * Clicks the component of that name on a screen declared in a file.
     *
     * @param component its name
     */
    public record Click(String component) implements Step {

        @Override
        public JsonObject toJson() {
            return one(CLICK, this.component);
        }
    }

    /**
     * Clicks a point of a screen not declared in a file, from the screen's middle, in the GUI's pixels.
     *
     * @param dx how far right of the middle
     * @param dy how far down
     */
    public record ClickAt(int dx, int dy) implements Step {

        @Override
        public JsonObject toJson() {
            final JsonArray at = new JsonArray();
            at.add(this.dx);
            at.add(this.dy);
            return one(CLICK_AT, at);
        }
    }

    /**
     * Types that text into what has the keyboard.
     *
     * @param text what is typed
     */
    public record Type(String text) implements Step {

        @Override
        public JsonObject toJson() {
            return one(TYPE, this.text);
        }
    }

    /**
     * Presses a key, by the game's code of it: Enter, Tab, Backspace, an arrow.
     *
     * @param key the key's code
     */
    public record Key(int key) implements Step {

        @Override
        public JsonObject toJson() {
            return one(KEY, this.key);
        }
    }

    /**
     * Waits that many ticks.
     *
     * @param ticks how long
     */
    public record Wait(int ticks) implements Step {

        public Wait {
            if (ticks < 1) {
                throw new IllegalArgumentException("a wait is a tick at least; got " + ticks);
            }
        }

        @Override
        public JsonObject toJson() {
            return one(WAIT, this.ticks);
        }
    }

    /**
     * Takes the picture of that name, which the test compares with its mod's reference picture.
     *
     * @param name the picture's name
     */
    public record Picture(String name) implements Step {

        @Override
        public JsonObject toJson() {
            return one(PICTURE, this.name);
        }
    }

    /** Closes the screen. */
    public record Close() implements Step {

        @Override
        public JsonObject toJson() {
            final JsonObject out = new JsonObject();
            out.addProperty(CLOSE, true);
            return out;
        }
    }
}
