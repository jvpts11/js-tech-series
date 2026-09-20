/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli.interac;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.computers.program.cli.ICliComputer;
import dev.jstech.computers.program.iql.IIqlCondition;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InteracViewTest {

    private static final int WIDE = 80;
    private static final int TALL = 19;

    private FakeNetwork network;

    @BeforeEach
    void setUp() {
        this.network = new FakeNetwork();
    }

    private static boolean says(final List<String> screen, final String text) {
        return screen.stream().anyMatch(line -> line.contains(text));
    }

    /** That state drawn on a glass of the size these tests read. */
    private List<String> screen(final InteracState state) {
        return InteracView.screen(this.network, state.on(WIDE, TALL));
    }

    @Test
    void screen_showsWhatTheNetworkHoldsUnderTheHeadingThatIsUp() {
        final List<String> screen = screen(InteracState.OPENING);

        assertEquals(TALL, screen.size(), "a screen is a glass's worth of rows");
        assertTrue(says(screen, "[Network]"), "the heading that is up is marked");
        assertTrue(says(screen, "Cobblestone") && says(screen, "Oak Log"), "and its rows are drawn");
        assertTrue(says(screen, "2 servers"), "the bar says how the network is");
    }

    @Test
    void screen_narrowsToWhatIsBeingSearchedForAndSaysHowManyRowsThatLeaves() {
        final List<String> screen = screen(InteracState.OPENING.searchingFor("log"));

        assertTrue(says(screen, "Oak Log"), "what matches is there");
        assertFalse(says(screen, "Cobblestone"), "and what does not is not");
        assertEquals(2, InteracScreen.rowsSaid(screen), "and the screen says how many are left");
    }

    @Test
    void screen_putsTheThingsBesideTheListThatSayWhatThePickedRowIs() {
        final List<String> screen = screen(InteracState.OPENING);

        assertTrue(says(screen, "minecraft:cobblestone"), "the panel names what is picked");
        assertTrue(says(screen, "storage-1"), "and says who is holding it");
        assertTrue(says(screen, "Used in"), "and what it goes into");
    }

    @Test
    void screen_asksTheQuestionWhileANumberIsStillBeingTyped() {
        final List<String> screen = screen(InteracState.OPENING.asking("get", 42L));

        assertTrue(says(screen, "how many Cobblestone? 42_"), "the question stands at the foot");
        assertTrue(this.network.done.isEmpty(), "and nothing has happened to the network yet");
    }

    @Test
    void screen_carriesOutAnActionOnlyOnceItIsMarkedAsOneToCarryOut() {
        screen(InteracState.OPENING.asking("get" + InteracView.NOW, 42L));

        assertEquals(List.of("hand minecraft:cobblestone 42"), this.network.done);
    }

    @Test
    void screen_worksTheHeadingsThatAreNotThingsTheNetworkHolds() {
        final List<String> servers = screen(InteracState.OPENING.onTab(InteracState.TAB_SERVERS));
        assertTrue(says(servers, "[Servers]") && says(servers, "storage-1"), "the servers are listed");
        assertTrue(says(servers, "% full"), "with how full each one is");

        final List<String> ops = screen(InteracState.OPENING.onTab(InteracState.TAB_OPS));
        assertTrue(says(ops, "[Ops]") && says(ops, "select Cobblestone"), "the work is listed");

        final List<String> starred = screen(InteracState.OPENING.onTab(InteracState.TAB_STARRED));
        assertTrue(says(starred, "[Starred]") && says(starred, "starred"), "and so are the stars");
    }

    @Test
    void screen_callsOffTheOperationThePickedRowStandsFor() {
        screen(InteracState.OPENING.onTab(InteracState.TAB_OPS).asking("stop" + InteracView.NOW, 0L));

        assertEquals(List.of("cancel op-1"), this.network.done);
    }

    @Test
    void screen_saysSoRatherThanBreakingWhenThereIsNothingToShow() {
        this.network.stock.clear();
        final List<String> screen = screen(InteracState.OPENING.picking(40));

        assertEquals(TALL, screen.size(), "the screen is still a screen");
        assertTrue(says(screen, "nothing here"), "and it says there is nothing to show");
        assertEquals(0, InteracScreen.rowsSaid(screen));
    }

    @Test
    void screen_isDrawnToTheSizeOfTheGlassThatAskedForIt() {
        for (final int wide : new int[] {40, 52, 80, 120}) {
            for (final int tall : new int[] {11, 19, 40}) {
                final List<String> screen =
                        InteracView.screen(this.network, InteracState.OPENING.on(wide, tall));
                assertEquals(tall, screen.size(), "at " + wide + " by " + tall);
                for (final String line : screen) {
                    assertEquals(wide, line.length(), "at " + wide + " by " + tall + ": [" + line + "]");
                }
            }
        }
    }

    @Test
    void screen_givesTheListTheWholeGlassWhenThereIsNoRoomForThePanelBesideIt() {
        final List<String> narrow = InteracView.screen(this.network, InteracState.OPENING.on(45, TALL));

        assertFalse(says(narrow, "|"), "no panel is drawn, so no line stands beside the list");
        assertTrue(says(narrow, "Cobblestone") && says(narrow, "storage-1"),
                "and the list keeps the name whole and says where the thing is:\n"
                        + String.join("\n", narrow));
    }

    /** A network of three things on two servers, with one operation in flight and one star. */
    private static final class FakeNetwork implements ICliComputer {

        final List<StoredItem> stock = new ArrayList<>(List.of(
                new StoredItem("Cobblestone", 2048L, "storage-1"),
                new StoredItem("Oak Log", 640L, "storage-1"),
                new StoredItem("Spruce Log", 128L, "storage-2")));

        /** What was asked of the network, in the order it was asked. */
        final List<String> done = new ArrayList<>();

        @Override public String networkId() {
            return "4f1c9b2e";
        }

        @Override public NetSummary network() {
            return new NetSummary(true, 2, 0, 0, 3, true);
        }

        @Override public ServerUse networkUse() {
            return new ServerUse("network", 2816L, 8192L);
        }

        @Override public List<StoredItem> query(final IIqlCondition where, final String server,
                                                final int limit) {
            return List.copyOf(this.stock);
        }

        @Override public List<ServerUse> servers() {
            return List.of(new ServerUse("storage-1", 2688L, 4096L),
                    new ServerUse("storage-2", 128L, 4096L));
        }

        @Override public List<StoredItem> locks() {
            return List.of();
        }

        @Override public List<String> favourites() {
            return List.of("item|minecraft:cobblestone");
        }

        @Override public List<ActiveOp> activeOps() {
            return List.of(new ActiveOp("op-1", "SELECT", "Cobblestone", 30L, 64L, "PROCESSING", "MEDIUM"));
        }

        @Override public List<ItemMatch> matching(final String text) {
            final String wanted = text.toLowerCase(Locale.ROOT);
            final List<ItemMatch> found = new ArrayList<>();
            for (final StoredItem row : this.stock) {
                if (row.name().toLowerCase(Locale.ROOT).contains(wanted)
                        || idOf(row.name()).contains(wanted)) {
                    found.add(new ItemMatch(idOf(row.name()), row.name(), row.quantity()));
                }
            }
            return found;
        }

        @Override public ItemDetail itemDetail(final String id) {
            for (final StoredItem row : this.stock) {
                if (idOf(row.name()).equals(id)) {
                    return new ItemDetail(row.name(), id, row.quantity(),
                            List.of(new Holding(row.detail(), row.quantity())),
                            List.of("Stone, smelted"), List.of("Furnace", "Stone Bricks"));
                }
            }
            return new ItemDetail("", id, 0L, List.of(), List.of(), List.of());
        }

        @Override public OpResult takeToHand(final String item, final long quantity) {
            this.done.add("hand " + item + " " + quantity);
            return OpResult.ok("taken");
        }

        @Override public OpResult cancelOperation(final String id) {
            this.done.add("cancel " + id);
            return OpResult.ok("called off");
        }

        private static String idOf(final String name) {
            return "minecraft:" + name.toLowerCase(Locale.ROOT).replace(' ', '_');
        }
    }
}
