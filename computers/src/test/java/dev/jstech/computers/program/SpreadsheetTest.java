/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Locale;

class SpreadsheetTest {

    private Spreadsheet sheet;

    /** A machine that says what a small network is holding, so the asking cells have something to read. */
    private static final Spreadsheet.INetworkFacts FACTS = new Spreadsheet.INetworkFacts() {

        @Override
        public long quantity(final String item) {
            return switch (item) {
                case "iron_ingot" -> 14208L;
                case "steel_ingot" -> 6512L;
                default -> -1L;
            };
        }

        @Override
        public long free() {
            return 831744L;
        }

        @Override
        public long servers() {
            return 12L;
        }
    };

    @BeforeEach
    void setUp() {
        sheet = new Spreadsheet();
        sheet.setFacts(FACTS);
    }

    @Test
    void aPlainValue_showsItself() {
        sheet.set(0, 0, "Item");
        assertEquals("Item", sheet.display(0, 0));
        assertEquals("Item", sheet.raw(0, 0));
    }

    @Test
    void arithmetic_worksWithTheOrdinaryPrecedence() {
        sheet.set(0, 0, "=2+3*4");
        assertEquals("14", sheet.display(0, 0));
        sheet.set(0, 1, "=(2+3)*4");
        assertEquals("20", sheet.display(0, 1));
        sheet.set(0, 2, "=10/4");
        assertEquals("2.50", sheet.display(0, 2));
        sheet.set(0, 3, "=-5+2");
        assertEquals("-3", sheet.display(0, 3));
    }

    @Test
    void aFormula_canReadAnotherCell() {
        sheet.set(0, 0, "8000");
        sheet.set(1, 0, "2000");
        sheet.set(2, 0, "=A1+A2");
        assertEquals("10,000", sheet.display(2, 0));
    }

    @Test
    void aFormula_canReadACellThatIsItselfAFormula() {
        sheet.set(0, 0, "10");
        sheet.set(0, 1, "=A1*2");
        sheet.set(0, 2, "=B1+5");
        assertEquals("25", sheet.display(0, 2));
    }

    @Test
    void aCellThatReadsItself_saysSoRatherThanHanging() {
        sheet.set(0, 0, "=A1+1");
        assertEquals(Spreadsheet.CIRCULAR, sheet.display(0, 0));
    }

    @Test
    void aRingOfCells_isCaughtTheSameWay() {
        sheet.set(0, 0, "=B1");
        sheet.set(0, 1, "=C1");
        sheet.set(0, 2, "=A1");
        assertEquals(Spreadsheet.CIRCULAR, sheet.display(0, 0));
    }

    @Test
    void aColumnThatReadsTheRowBelowItTwice_isStillOnePassDown() {
        /*
         * Each row adds the one under it to itself, so without holding what a cell came to, working out the
         * top row costs twice what the row below it did, so a column of them is a sheet nobody ever gets an
         * answer out of. Twenty-six rows is enough to take tens of seconds the old way and no time at all
         * this way, which is what the second of grace below is measuring.
         */
        final int rows = 26;
        sheet.set(rows, 0, "1");
        for (int row = rows - 1; row >= 0; row--) {
            sheet.set(row, 0, "=A" + (row + 2) + "+A" + (row + 2));
        }
        final long started = System.nanoTime();
        final String top = sheet.display(0, 0);
        final long took = System.nanoTime() - started;
        assertEquals(number(Math.pow(2, rows)), top);
        assertTrue(took < 1_000_000_000L, "working the sheet out took " + took / 1_000_000L + " ms");
    }

    /** The same grouping the sheet writes a whole number with, so the expectation is written once. */
    private static String number(final double value) {
        return String.format(Locale.ROOT, "%,d", (long) value);
    }

    @Test
    void whatACellCameTo_isForgottenAsSoonAsAnythingIsTyped() {
        sheet.set(1, 0, "2");
        sheet.set(0, 0, "=A2*10");
        assertEquals("20", sheet.display(0, 0));
        sheet.set(1, 0, "5");
        assertEquals("50", sheet.display(0, 0), "the sheet answered from what it held before");
    }

    @Test
    void whatACellCameTo_isForgottenWhenTheMachineSaysSomethingNew() {
        sheet.set(0, 0, "=QTY(\"iron_ingot\")");
        assertEquals("14,208", sheet.display(0, 0));
        sheet.setFacts(new Spreadsheet.INetworkFacts() {

            @Override
            public long quantity(final String item) {
                return 128L;
            }

            @Override
            public long free() {
                return -1L;
            }

            @Override
            public long servers() {
                return -1L;
            }
        });
        assertEquals("128", sheet.display(0, 0), "the sheet answered from what the machine said before");
    }

    @Test
    void anEmptyCell_countsAsNothingInASum() {
        sheet.set(0, 0, "=A5+7");
        assertEquals("7", sheet.display(0, 0));
    }

    @Test
    void wordsInACell_countAsNothingInASum() {
        sheet.set(0, 0, "iron");
        sheet.set(1, 0, "=A1+3");
        assertEquals("3", sheet.display(1, 0));
    }

    @Test
    void qty_asksTheNetworkWhatItHolds() {
        sheet.set(0, 0, "=QTY(\"iron_ingot\")");
        assertEquals("14,208", sheet.display(0, 0));
    }

    @Test
    void qty_ofSomethingUnknownSaysSo() {
        sheet.set(0, 0, "=QTY(\"nothing_like_this\")");
        assertEquals(Spreadsheet.UNKNOWN, sheet.display(0, 0));
    }

    @Test
    void free_andServers_askTheNetworkToo() {
        sheet.set(0, 0, "=FREE()");
        sheet.set(0, 1, "=SERVERS()");
        assertEquals("831,744", sheet.display(0, 0));
        assertEquals("12", sheet.display(0, 1));
    }

    @Test
    void anAskingCell_canBePartOfASum() {
        sheet.set(0, 0, "10000");
        sheet.set(0, 1, "=A1-QTY(\"steel_ingot\")");
        assertEquals("3,488", sheet.display(0, 1));
    }

    @Test
    void withNothingKnown_anAskingCellSaysSoRatherThanZero() {
        final Spreadsheet alone = new Spreadsheet();
        alone.set(0, 0, "=QTY(\"iron_ingot\")");
        assertEquals(Spreadsheet.UNKNOWN, alone.display(0, 0),
                "a machine on no network must not pretend the answer is nothing");
    }

    @Test
    void isLive_picksOutTheCellsThatAskTheNetwork() {
        sheet.set(0, 0, "=QTY(\"iron_ingot\")");
        sheet.set(0, 1, "=1+1");
        sheet.set(0, 2, "plain");
        sheet.set(0, 3, "=FREE()");
        assertTrue(sheet.isLive(0, 0));
        assertFalse(sheet.isLive(0, 1));
        assertFalse(sheet.isLive(0, 2));
        assertTrue(sheet.isLive(0, 3));
        assertEquals(2, sheet.liveCells());
    }

    @Test
    void sum_walksARangeOfCells() {
        for (int row = 0; row < 5; row++) {
            sheet.set(row, 0, String.valueOf((row + 1) * 10));
        }
        sheet.set(6, 0, "=SUM(A1:A5)");
        assertEquals("150", sheet.display(6, 0));
    }

    @Test
    void theOtherFunctionsOverARange_answerWhatTheyShould() {
        for (int row = 0; row < 4; row++) {
            sheet.set(row, 0, String.valueOf((row + 1) * 10));
        }
        sheet.set(0, 1, "=MIN(A1:A4)");
        sheet.set(1, 1, "=MAX(A1:A4)");
        sheet.set(2, 1, "=AVG(A1:A4)");
        sheet.set(3, 1, "=COUNT(A1:A4)");
        assertEquals("10", sheet.display(0, 1));
        assertEquals("40", sheet.display(1, 1));
        assertEquals("25", sheet.display(2, 1));
        assertEquals("4", sheet.display(3, 1));
    }

    @Test
    void aRange_worksWhicheverCornerItIsGivenFrom() {
        sheet.set(0, 0, "5");
        sheet.set(1, 0, "6");
        sheet.set(0, 1, "=SUM(A2:A1)");
        assertEquals("11", sheet.display(0, 1));
    }

    @Test
    void nonsenseInAFormula_saysSoRatherThanGuessing() {
        sheet.set(0, 0, "=2+");
        assertEquals(Spreadsheet.ERROR, sheet.display(0, 0));
        sheet.set(0, 1, "=WHAT(A1)");
        assertEquals(Spreadsheet.ERROR, sheet.display(0, 1));
        sheet.set(0, 2, "=(1+2");
        assertEquals(Spreadsheet.ERROR, sheet.display(0, 2));
        sheet.set(0, 3, "=1/0");
        assertEquals(Spreadsheet.ERROR, sheet.display(0, 3));
    }

    @Test
    void aCellOutsideTheSheet_isRefusedRatherThanThrowing() {
        sheet.set(Spreadsheet.ROWS, 0, "x");
        sheet.set(0, Spreadsheet.COLUMNS, "x");
        sheet.set(-1, -1, "x");
        assertEquals("", sheet.raw(Spreadsheet.ROWS, 0));
        assertEquals("", sheet.display(-1, -1));
    }

    @Test
    void aReferenceOffTheSheet_isAnError() {
        sheet.set(0, 0, "=A200");
        assertEquals(Spreadsheet.ERROR, sheet.display(0, 0));
    }

    @Test
    void csv_goesOutAndComesBackTheSame() {
        sheet.set(0, 0, "Item");
        sheet.set(0, 1, "Floor");
        sheet.set(1, 0, "iron_ingot");
        sheet.set(1, 1, "8000");
        sheet.set(1, 2, "=QTY(\"iron_ingot\")");
        final String csv = sheet.toCsv();
        final Spreadsheet back = new Spreadsheet();
        back.setFacts(FACTS);
        back.fromCsv(csv);
        assertEquals("Item", back.raw(0, 0));
        assertEquals("8000", back.raw(1, 1));
        assertEquals("=QTY(\"iron_ingot\")", back.raw(1, 2));
        assertEquals("14,208", back.display(1, 2));
    }

    @Test
    void csv_survivesAValueHoldingACommaOrAQuote() {
        sheet.set(0, 0, "one,two");
        sheet.set(0, 1, "a \"quoted\" word");
        final Spreadsheet back = new Spreadsheet();
        back.fromCsv(sheet.toCsv());
        assertEquals("one,two", back.raw(0, 0));
        assertEquals("a \"quoted\" word", back.raw(0, 1));
    }

    @Test
    void fromCsv_clearsWhatWasThereBefore() {
        sheet.set(5, 5, "old");
        sheet.fromCsv("new\n");
        assertEquals("", sheet.raw(5, 5));
        assertEquals("new", sheet.raw(0, 0));
    }

    @Test
    void fromCsv_ofNothingLeavesAnEmptySheet() {
        sheet.set(0, 0, "old");
        sheet.fromCsv("");
        assertEquals("", sheet.raw(0, 0));
        sheet.fromCsv(null);
        assertEquals("", sheet.raw(0, 0));
    }

    @Test
    void fromCsv_ignoresMoreRowsOrColumnsThanTheSheetHas() {
        final StringBuilder wide = new StringBuilder();
        for (int i = 0; i < Spreadsheet.COLUMNS + 5; i++) {
            wide.append(i).append(',');
        }
        sheet.fromCsv(wide + "\n");
        assertEquals("0", sheet.raw(0, 0));
        assertEquals(String.valueOf(Spreadsheet.COLUMNS - 1), sheet.raw(0, Spreadsheet.COLUMNS - 1));
    }

    @Test
    void columnsAndCells_areNamedTheWayAFormulaWritesThem() {
        assertEquals("A", Spreadsheet.columnName(0));
        assertEquals("Z", Spreadsheet.columnName(Spreadsheet.COLUMNS - 1));
        assertEquals("C3", Spreadsheet.cellName(2, 2));
    }

    @Test
    void aCellThatReadsAnother_movesWhenThatOtherIsRetyped() {
        /*
         * The answers are held onto so a sheet drawn sixty times a second is worked out once. This is the
         * thing that has to keep working: a held answer that never moves is worse than a slow one.
         */
        sheet.set(0, 0, "10");
        sheet.set(0, 1, "=A1*2");
        assertEquals("20", sheet.display(0, 1));
        sheet.set(0, 0, "50");
        assertEquals("100", sheet.display(0, 1), "the answer was held on to after its input changed");
    }

    @Test
    void anAskingCell_movesWhenTheMachineSaysSomethingNew() {
        sheet.set(0, 0, "=QTY(\"iron_ingot\")");
        assertEquals("14,208", sheet.display(0, 0));
        sheet.setFacts(new Spreadsheet.INetworkFacts() {

            @Override
            public long quantity(final String item) {
                return 99L;
            }

            @Override
            public long free() {
                return 0L;
            }

            @Override
            public long servers() {
                return 0L;
            }
        });
        assertEquals("99", sheet.display(0, 0), "the answer was held on to after the network changed");
    }

    @Test
    void forget_makesTheSheetWorkItselfOutAgain() {
        sheet.set(0, 0, "=1+1");
        assertEquals("2", sheet.display(0, 0));
        sheet.forget();
        assertEquals("2", sheet.display(0, 0), "forgetting must not change the answer, only re-find it");
    }

    @Test
    void csvRead_dropsTheAnswersOfTheSheetItReplaced() {
        sheet.set(0, 0, "=1+1");
        assertEquals("2", sheet.display(0, 0));
        sheet.fromCsv("=2+2\n");
        assertEquals("4", sheet.display(0, 0));
    }

    @Test
    void aSheet_isTwentySixByNinetyNine() {
        assertEquals(26, Spreadsheet.COLUMNS);
        assertEquals(99, Spreadsheet.ROWS);
    }
}
