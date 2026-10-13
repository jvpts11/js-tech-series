/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.machine;

import dev.jstech.computers.blockentity.SpeakerBlockEntity;
import dev.jstech.computers.vm.program.Halt;
import dev.jstech.computers.vm.program.Values;
import dev.jstech.computers.vm.system.MemberId;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.Map;

/**
 * The calls a program makes on the machine's sound, answered by its {@link SoundService}: a beep, a tune, a recording,
 * and a speaker found by its name.
 *
 * <p>A tune holding something that is no note stops the program, naming what it could not read, as a text that is no
 * number stops a conversion; playing a song the machine cannot play answers false instead, since a disk or a card is
 * not the program's to be sure of.
 */
@TextHolder
final class SoundCalls {

    private static final String STRING = "string";
    private static final String INT = "int";

    private static final TextKey NOT_A_NOTE = TextKey.of("jsc.service.sound.not_a_note", "%s is not a note");

    private SoundCalls() {
    }

    static void bind(final Map<MemberId, MachineCalls.Binding<?>> bindings) {
        sound(bindings, "Beep", (sound, call, target, arguments, line) -> {
            sound.beep(number(arguments, 0), number(arguments, 1));
            return null;
        }, INT, INT);
        sound(bindings, "Tones", (sound, call, target, arguments, line) -> {
            try {
                return sound.tones(text(arguments, 0));
            } catch (final IllegalArgumentException notANote) {
                throw new Halt(Halt.Reason.OUT_OF_RANGE, line, NOT_A_NOTE.with(notANote.getMessage()));
            }
        }, STRING);
        sound(bindings, "Play", (sound, call, target, arguments, line) -> sound.play(text(arguments, 0)), STRING);
        sound(bindings, "Stop", (sound, call, target, arguments, line) -> {
            sound.stop();
            return null;
        });
        MachineCalls.bind(bindings, MachineServices::sound, "Speaker", "Named",
                (sound, call, target, arguments, line) -> {
                    final SpeakerBlockEntity speaker = sound.speaker(text(arguments, 0));
                    if (speaker == null) {
                        return null;
                    }
                    final Values.Obj made = new Values.Obj("Speaker");
                    made.set("Name", speaker.name());
                    return made;
                }, STRING);
        MachineCalls.bind(bindings, MachineServices::sound, "Speaker", "Play",
                (sound, call, target, arguments, line) -> {
                    final String name = target instanceof Values.Obj held && held.get("Name") instanceof String named
                            ? named : "";
                    return sound.playOn(name, text(arguments, 0));
                }, STRING);
    }

    private static void sound(final Map<MemberId, MachineCalls.Binding<?>> bindings, final String name,
                              final MachineCalls.IServiceFunction<SoundService> function, final String... parameters) {
        MachineCalls.bind(bindings, MachineServices::sound, "Sound", name, function, parameters);
    }

    private static String text(final Object[] arguments, final int at) {
        return arguments.length <= at ? "" : String.valueOf(arguments[at]);
    }

    private static int number(final Object[] arguments, final int at) {
        return arguments.length > at && arguments[at] instanceof Number value ? value.intValue() : 0;
    }
}
