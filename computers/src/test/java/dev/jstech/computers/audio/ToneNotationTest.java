/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.audio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.jstech.core.audio.pcm.Tone;
import dev.jstech.core.audio.pcm.Waveform;
import java.util.List;
import org.junit.jupiter.api.Test;

class ToneNotationTest {

    private static final double CLOSE = 0.01;

    @Test
    void parts_readsNamesPitchesAndRestsInOnePart() {
        final List<List<Tone>> parts = ToneNotation.parts("A4:200 440 R C4:100", Waveform.SINE);
        assertEquals(1, parts.size());
        final List<Tone> part = parts.getFirst();
        assertEquals(440.0, part.get(0).frequency(), CLOSE);
        assertEquals(200, part.get(0).millis());
        assertEquals(440.0, part.get(1).frequency(), CLOSE);
        assertEquals(200, part.get(1).millis(), "an item that does not say lasts as long as the one before");
        assertEquals(0.0, part.get(2).frequency(), CLOSE);
        assertEquals(261.63, part.get(3).frequency(), CLOSE);
        assertEquals(Waveform.SINE, part.get(0).wave());
    }

    @Test
    void parts_readsSharpsAndFlats() {
        final List<Tone> part = ToneNotation.parts("C#4 Db4 Bb3 B3", Waveform.SQUARE).getFirst();
        assertEquals(277.18, part.get(0).frequency(), CLOSE);
        assertEquals(277.18, part.get(1).frequency(), CLOSE, "a sharp and the flat above it are one pitch");
        assertEquals(233.08, part.get(2).frequency(), CLOSE);
        assertEquals(246.94, part.get(3).frequency(), CLOSE);
    }

    @Test
    void parts_givesEachNoteOfAChordAPartOfItsOwn() {
        final List<List<Tone>> parts = ToneNotation.parts("C4:300 C4+E4+G4:500", Waveform.SQUARE);
        assertEquals(3, parts.size(), "as many parts as the widest chord has notes");
        assertEquals(329.63, parts.get(1).get(1).frequency(), CLOSE);
        assertEquals(0.0, parts.get(1).get(0).frequency(), CLOSE, "a part rests while the tune plays one note");
        assertEquals(800L, ToneNotation.millisOf(parts));
    }

    @Test
    void parts_startsAtAQuarterOfASecond() {
        assertEquals(ToneNotation.DEFAULT_MILLIS, ToneNotation.parts("E5", Waveform.SQUARE).getFirst().getFirst()
                .millis());
    }

    @Test
    void parts_refusesWhatIsNotANoteNamingIt() {
        final IllegalArgumentException wrong = assertThrows(IllegalArgumentException.class,
                () -> ToneNotation.parts("C4 H2 E4", Waveform.SQUARE));
        assertEquals("H2", wrong.getMessage());
        assertThrows(IllegalArgumentException.class, () -> ToneNotation.parts("C9", Waveform.SQUARE));
        assertThrows(IllegalArgumentException.class, () -> ToneNotation.parts("C4:0", Waveform.SQUARE));
        assertThrows(IllegalArgumentException.class, () -> ToneNotation.parts("30000", Waveform.SQUARE));
    }

    @Test
    void parts_ofNothingIsNoPart() {
        assertEquals(List.of(), ToneNotation.parts("   ", Waveform.SQUARE));
    }
}
