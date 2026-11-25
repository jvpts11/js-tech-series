/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.machine;

import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.ApiStatus;

/**
 * A machine's face as a picture of its own (its mod drew it: the machine at work, its parts moving) and where on it
 * what the game draws itself stands, in the face's pixels from its top left: its ports, the slots the menu places
 * and the picture lets into the body; its panel meter, its LCD, its switch and the words beside it, its sight glasses;
 * and the words engraved on its parts; and the things of its work it shows moving, which the game draws as the work
 * makes them. The picture's frames are laid out {@link #COLUMNS} to a row, the machine idle first, then its working
 * loop. Both sides read it: the menu for its slots, the screen for the rest.
 *
 * @param frames  how many frames its working loop has, after its idle one
 * @param millis  how long each frame of the loop shows, in milliseconds
 * @param plate   the machine's name as its name plate writes it, short, when the picture gives one; the screen's
 *                title otherwise
 * @param ports   its ports, each with the slot it shows
 * @param meter   its panel meter
 * @param lcd     its LCD
 * @param toggle  its switch and the words on and off
 * @param glasses its sight glasses, in the order of its tanks
 * @param words   the words engraved on its parts
 * @param things  where what the work makes stands on each frame, drawn by the game
 * @param tray    the row of outputs under the face, a sorter's bins, when the machine has one
 */
@ApiStatus.Experimental
public record FacePicture(int frames, int millis, Optional<String> plate, List<Port> ports, Meter meter, Box lcd,
                          Toggle toggle, List<Glass> glasses, List<Word> words, List<Thing> things,
                          Optional<Tray> tray) {

    /** How many frames stand in a row of the picture, so no side of it grows past what every graphics card takes. */
    public static final int COLUMNS = 8;

    /** A point of the face. */
    public record Place(int x, int y) {

        public static final Codec<Place> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("x").forGetter(Place::x), Codec.INT.fieldOf("y").forGetter(Place::y))
                .apply(i, Place::new));
    }

    /** A port: the top left of the slot let into the body there, and the slot of the machine it shows. */
    public record Port(int x, int y, int slot) {

        public static final Codec<Port> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("x").forGetter(Port::x), Codec.INT.fieldOf("y").forGetter(Port::y),
                Codec.INT.fieldOf("slot").forGetter(Port::slot)).apply(i, Port::new));
    }

    /**
     * A row of outputs the screen shows under the face, as a sorter's bins, on a band of the face's own plate: the
     * translation key of what the row is called, written at its left; the machine's slots in it, from the left; the
     * mark written under each bin, a letter or a sign, when anything is; and the band's colour and its letters'
     * (RRGGBB), taken from the face's picture.
     */
    public record Tray(String label, List<Integer> slots, List<String> marks, String colour, String ink) {

        public static final Codec<Tray> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("label").forGetter(Tray::label),
                Codec.INT.listOf().fieldOf("slots").forGetter(Tray::slots),
                Codec.STRING.listOf().optionalFieldOf("marks", List.of()).forGetter(Tray::marks),
                Codec.STRING.fieldOf("colour").forGetter(Tray::colour),
                Codec.STRING.fieldOf("ink").forGetter(Tray::ink)).apply(i, Tray::new));

        public Tray {
            slots = List.copyOf(slots);
            marks = List.copyOf(marks);
        }

        /** The band's colour, opaque ARGB. */
        public int argb() {
            return 0xFF << 24 | Integer.parseInt(this.colour, 16);
        }

        /** Its letters' colour, opaque ARGB. */
        public int inkArgb() {
            return 0xFF << 24 | Integer.parseInt(this.ink, 16);
        }
    }

    /** A box of the face, its top left and its size. */
    public record Box(int x, int y, int w, int h) {

        public static final Codec<Box> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("x").forGetter(Box::x), Codec.INT.fieldOf("y").forGetter(Box::y),
                Codec.INT.fieldOf("w").forGetter(Box::w), Codec.INT.fieldOf("h").forGetter(Box::h))
                .apply(i, Box::new));
    }

    /** The panel meter's face, and where the middle and the top of its range engraved stand, when the picture says. */
    public record Meter(int x, int y, int w, int h, Optional<Place> label) {

        public static final Codec<Meter> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("x").forGetter(Meter::x), Codec.INT.fieldOf("y").forGetter(Meter::y),
                Codec.INT.fieldOf("w").forGetter(Meter::w), Codec.INT.fieldOf("h").forGetter(Meter::h),
                Place.CODEC.optionalFieldOf("label").forGetter(Meter::label)).apply(i, Meter::new));
    }

    /** The switch's top left, and where its words on and off stand, when the picture says. */
    public record Toggle(int x, int y, Optional<Place> on, Optional<Place> off) {

        public static final Codec<Toggle> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("x").forGetter(Toggle::x), Codec.INT.fieldOf("y").forGetter(Toggle::y),
                Place.CODEC.optionalFieldOf("on").forGetter(Toggle::on),
                Place.CODEC.optionalFieldOf("off").forGetter(Toggle::off)).apply(i, Toggle::new));
    }

    /** Where the middle and the top of a word stand, and its colour (RRGGBB). */
    public record Label(int x, int y, String colour) {

        public static final Codec<Label> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("x").forGetter(Label::x), Codec.INT.fieldOf("y").forGetter(Label::y),
                Codec.STRING.fieldOf("colour").forGetter(Label::colour)).apply(i, Label::new));

        /** Its colour, opaque ARGB. */
        public int argb() {
            return 0xFF << 24 | Integer.parseInt(this.colour, 16);
        }
    }

    /**
     * A sight glass: its left, its top, its width (a slim one, of a bank of them, narrower than the bracketed one) and
     * its height; the machine's tank it shows (its liquid tanks in, then out, then each state of matter's in turn, in
     * then out); where what it holds is written, when the picture writes it; and, for a machine whose ports fill
     * whichever of its tanks is free, the liquid it shows, by its id (or a few joined by '|'), in whichever of its
     * liquid tanks holds it, among
     * its tanks in when its tank is one of those and its tanks out otherwise, empty while none holds it or has room.
     */
    public record Glass(int x, int y, int w, int h, int tank, Optional<Label> label, Optional<String> fluid) {

        /** How wide a slim glass is, of a bank of them. */
        public static final int SLIM = 6;

        public static final Codec<Glass> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("x").forGetter(Glass::x), Codec.INT.fieldOf("y").forGetter(Glass::y),
                Codec.INT.fieldOf("w").forGetter(Glass::w), Codec.INT.fieldOf("h").forGetter(Glass::h),
                Codec.INT.fieldOf("tank").forGetter(Glass::tank),
                Label.CODEC.optionalFieldOf("label").forGetter(Glass::label),
                Codec.STRING.optionalFieldOf("fluid").forGetter(Glass::fluid)).apply(i, Glass::new));

        /** Whether it is a slim glass, of a bank of them. */
        public boolean slim() {
            return this.w <= SLIM;
        }
    }

    /** Frames {@code first} to {@code last} of the picture, both shown; the idle frame is 0. */
    public record Run(int first, int last) {

        public static final Codec<Run> CODEC = Codec.INT.listOf().comapFlatMap(
                pair -> pair.size() == 2 ? DataResult.success(new Run(pair.get(0), pair.get(1)))
                        : DataResult.error(() -> "a run of frames is its first and its last: " + pair),
                run -> List.of(run.first(), run.last()));
    }

    /**
     * A word engraved on a part, small: its text (which its mod may translate, or read live), its top left, its colour
     * (RRGGBB), and the frames it shows on, all of them when none are given.
     */
    public record Word(String text, double x, double y, String colour, List<Run> frames) {

        public static final Codec<Word> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("text").forGetter(Word::text), Codec.DOUBLE.fieldOf("x").forGetter(Word::x),
                Codec.DOUBLE.fieldOf("y").forGetter(Word::y), Codec.STRING.fieldOf("colour").forGetter(Word::colour),
                Run.CODEC.listOf().optionalFieldOf("frames", List.of()).forGetter(Word::frames))
                .apply(i, Word::new));

        public Word {
            frames = List.copyOf(frames);
        }

        /** Its colour, opaque ARGB. */
        public int argb() {
            return 0xFF << 24 | Integer.parseInt(this.colour, 16);
        }

        /** Whether it shows on frame {@code frame} of the picture. */
        public boolean showsOn(final int frame) {
            return shows(this.frames, frame);
        }
    }

    /**
     * What the work makes, drawn by the game over the picture as an item is drawn, its top left at ({@code x},
     * {@code y}) and {@code size} pixels wide (an item's own being 16), on the frames it shows on, all of them when
     * none are given: the part a moulder ejects, its own on every recipe, where the picture leaves it out.
     */
    public record Thing(double x, double y, double size, List<Run> frames) {

        public static final Codec<Thing> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.DOUBLE.fieldOf("x").forGetter(Thing::x), Codec.DOUBLE.fieldOf("y").forGetter(Thing::y),
                Codec.DOUBLE.fieldOf("size").forGetter(Thing::size),
                Run.CODEC.listOf().optionalFieldOf("frames", List.of()).forGetter(Thing::frames))
                .apply(i, Thing::new));

        public Thing {
            frames = List.copyOf(frames);
        }

        /** Whether it shows on frame {@code frame} of the picture. */
        public boolean showsOn(final int frame) {
            return shows(this.frames, frame);
        }
    }

    public static final Codec<FacePicture> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("frames").forGetter(FacePicture::frames),
            Codec.INT.fieldOf("millis").forGetter(FacePicture::millis),
            Codec.STRING.optionalFieldOf("plate").forGetter(FacePicture::plate),
            Port.CODEC.listOf().fieldOf("ports").forGetter(FacePicture::ports),
            Meter.CODEC.fieldOf("meter").forGetter(FacePicture::meter),
            Box.CODEC.fieldOf("lcd").forGetter(FacePicture::lcd),
            Toggle.CODEC.fieldOf("switch").forGetter(FacePicture::toggle),
            Glass.CODEC.listOf().fieldOf("glasses").forGetter(FacePicture::glasses),
            Word.CODEC.listOf().fieldOf("words").forGetter(FacePicture::words),
            Thing.CODEC.listOf().optionalFieldOf("things", List.of()).forGetter(FacePicture::things),
            Tray.CODEC.optionalFieldOf("tray").forGetter(FacePicture::tray)).apply(i, FacePicture::new));

    public FacePicture {
        if (frames < 1 || millis < 1) {
            throw new IllegalArgumentException("a face's loop has frames, each shown a while: " + frames + " of "
                    + millis + " ms");
        }
        ports = List.copyOf(ports);
        glasses = List.copyOf(glasses);
        words = List.copyOf(words);
        things = List.copyOf(things);
    }

    /** How many rows of frames the picture has. */
    public int rows() {
        return (this.frames + 1 + COLUMNS - 1) / COLUMNS;
    }

    /**
     * The picture described at {@code path} on the classpath of {@code owner}'s jar (the mod's own resources, which a
     * server holds as a player's game does), or none when there is none there.
     *
     * @throws IllegalStateException when the file is there but says no picture
     */
    public static Optional<FacePicture> read(final Class<?> owner, final String path) {
        try (InputStream in = owner.getResourceAsStream(path)) {
            if (in == null) {
                return Optional.empty();
            }
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                return Optional.of(CODEC.parse(JsonOps.INSTANCE, JsonParser.parseReader(reader)).getOrThrow(
                        problem -> new IllegalStateException("the face " + path + " says no picture: " + problem)));
            }
        } catch (final IOException e) {
            throw new IllegalStateException("the face " + path + " could not be read", e);
        }
    }

    /* Whether a part shown on {@code runs} shows on frame {@code frame}: on every frame when there are none. */
    private static boolean shows(final List<Run> runs, final int frame) {
        if (runs.isEmpty()) {
            return true;
        }
        for (final Run run : runs) {
            if (frame >= run.first() && frame <= run.last()) {
                return true;
            }
        }
        return false;
    }
}
