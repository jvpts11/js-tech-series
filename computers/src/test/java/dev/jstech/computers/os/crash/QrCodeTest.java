/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.crash;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * The codes are checked module for module against those a reference encoder made of the same text in byte mode at
 * level M under the same mask, each row written as the hexadecimal of its modules (dark is 1), padded on the right to
 * a whole digit. Two of the texts end on a byte boundary right after the terminator, where the standard adds no
 * padding bits before the pad codewords; that case is covered on purpose.
 */
class QrCodeTest {

    private static final String CRASH_TEXT = "lol there is nothing here. 67.";
    /* The crash text at version 3, under each of the eight masks in turn. */
    private static final String[] CRASH_UNDER_MASK = {
            "fe14b3f882ebf208ba268ae8ba0f22e8ba8212e8826c1208feaaabf8001ed000aa0b4890cd0b47586f140328d9e96d10"
                    + "a658ef5030258e18a6e3cdb80c36cf88bed37e1015eb4e78b21c24b854895310aaa0ff9000adf898fe43daf88206c898"
                    + "baabefd0ba7f71c0ba92c488820cf810feaffb98",
            "fec1e3f8823ea208baf3dae8ba5a72e8ba5742e882b94208feaaabf8004b8000a35e1928985e12083a4156788cbc3840"
                    + "f30dba006570db48f3b698e859639ad8eb862b4040be1b28e74971e801dc0640fff5afc000f8a8c8fe968aa8825398c8"
                    + "ba7ebf80ba2a2490bac791d88259ad40fefaaec8",
            "fe773bf882778208bac502e8ba9352e8bae19ae882f06208feaaabf80082a000be68c3e00817369857f78d101cf51cd0"
                    + "9ebb6168f539ffd89e004380c92abe488630f028d0f73fb88affaa80919522d092437fa800b188d8fe205ac0829ab8d8"
                    + "bac86fe8bae30000baf14ab0821089d0fecc75a0",
            "fef73bf882acea08ba28b2e8ba9352e8ba3af2e8821dd208feaaabf800d9c800b705725808173698e32ce0a0c598aa08"
                    + "9ebb616841e29268476df558c92abe4832eb9d98099a89608affaa80254e4f604b2ecff000b188d8fefb3af082f70880"
                    + "ba486fe8bab86db0ba9cfc68821089d0fe971810",
            "feb023f882309a08ba7de2e8baabb2e8baa682e882b77a08feaaabf800ba40008bafdfc879d02ae8dbcf6e9890cdff58"
                    + "ef7c7d1884fee3a81238a00845125dc0f7f7ec58a13023c806c749081dadc158e3846fd800f698a8fe98bac8822258d0"
                    + "ba8f7f98ba241c70ba49a93882286a58fe8b69d0",
            "fe41e3f882b68208bac502e8baf0dae8ba619ae882316208feaaabf800c3a00082e8c67030f4b8a057f78d100cb418c0"
                    + "f30dba00e578fbc89e004380f1c930708630f028c0b63ba8e74971e881d426c092437fa800d208e0fe205ac0825bb8c8"
                    + "ba7ebf80ba220410ba714ab0827307e8fecc75a0",
            "fec1e3f882b09a08bae192e8ba70dae8baf3d2e88201a208feaaabf80045b8009fcc54b830f4b8a07365c4300084dbc8"
                    + "f30dba0084fee3a8d724d1c8f1c93070a2a2b908cc86f8a0e74971e8e0523ea0db67efe000d208e0feb21ae082eb78c0"
                    + "bafebf80baa41c70ba55d8f8827307e8fede3c80",
            "fe14b3f8824f6208ba34c2e8ba0f22e8ba2682e882fe5a08feaaabf8003a400096990500cd0b475826309160fd7b2430"
                    + "a658ef5079011c50827184980c36cf88f7f7ec5831790758b21c24b81dadc1588e32bfb000adf898fe674ab0829488b8"
                    + "ba2befd0badbe388ba008da8820cf810fe8b69d0"};
    /* "67" in byte mode, at version 1 under mask 3. */
    private static final String SHORT =
            "fea3f882e208ba3ae8bacae8ba62e8822208feabf800c000b7425808297863afa81dec18e254b0008530fec6b082be48"
            + "ba1248bae490baf4e0827ac8fef940";
    /* A sentence of 111 bytes, at version 7 under mask 6: the first version that carries its version information. */
    private static final String LONG_TEXT = "The cooperative kernel cannot recover. Frames will restart, and every"
            + " program that was open is closed for good.";
    private static final String LONG =
            "fef0db2b4bf882e9f9809208ba85eb33d2e8ba7d708d9ae8baf25fd6bae88232e8940208feaaaaaaabf80047688b5800"
            + "9fac2fee34b82997f2e67f60cfc32a14b9382c29fb0bfc20fa1b7478dac8b4eb012eda60daba3ba00a2040f7bbe72e38"
            + "a30929a3d640c55dc20f6c682a1912d51888604f1d6b08b03fbf3f8b6f8888ec88ae78805ae19ae8eaf858d748d1b8a8"
            + "cfc99faacfd8fcc522b24128a367b6684940f10617a43df04e36b191f5d070cf05cb7fe856f956cc4be87da478eb1c78"
            + "c7647bca4d10012bd0eef1a00b23a345e2f878c418e3e2b09bee4fc8dfd800f6689658e0feb5cad0dae08287b8c238a8"
            + "babbcfa78fc8ba870c13f108ba6d1ed00c888261f72d1378feef858c2ac0";

    @Test
    void encode_matchesTheReferenceUnderEveryMask() {
        for (int mask = 0; mask < 8; mask++) {
            assertEquals(CRASH_UNDER_MASK[mask], hex(QrCode.encode(CRASH_TEXT, mask)), "mask " + mask);
        }
    }

    @Test
    void encode_crashTextIsVersionThree() {
        assertEquals(29, QrCode.encode(CRASH_TEXT).length);
    }

    @Test
    void encode_shortTextIsVersionOne() {
        assertEquals(SHORT, hex(QrCode.encode("67", 3)));
    }

    @Test
    void encode_versionSevenCarriesItsVersionInformation() {
        assertEquals(LONG, hex(QrCode.encode(LONG_TEXT, 6)));
    }

    @Test
    void encode_picksTheMaskThePenaltyRulesScoreLowest() {
        int best = 0;
        for (int mask = 1; mask < 8; mask++) {
            if (QrCode.penalty(QrCode.encode(CRASH_TEXT, mask)) < QrCode.penalty(QrCode.encode(CRASH_TEXT, best))) {
                best = mask;
            }
        }
        assertEquals(hex(QrCode.encode(CRASH_TEXT, best)), hex(QrCode.encode(CRASH_TEXT)));
    }

    @Test
    void versionFor_refusesTextPastVersionTen() {
        assertEquals(10, QrCode.versionFor(QrCode.MOST_BYTES));
        assertThrows(IllegalArgumentException.class, () -> QrCode.versionFor(QrCode.MOST_BYTES + 1));
    }

    @Test
    void encode_refusesAMaskOutOfRange() {
        assertThrows(IllegalArgumentException.class, () -> QrCode.encode(CRASH_TEXT, 8));
    }

    private static String hex(final boolean[][] modules) {
        final StringBuilder out = new StringBuilder();
        for (final boolean[] row : modules) {
            final int digits = (row.length + 3) / 4;
            for (int d = 0; d < digits; d++) {
                int nibble = 0;
                for (int b = 0; b < 4; b++) {
                    final int x = d * 4 + b;
                    nibble = nibble << 1 | (x < row.length && row[x] ? 1 : 0);
                }
                out.append(Character.forDigit(nibble, 16));
            }
        }
        return out.toString();
    }
}
