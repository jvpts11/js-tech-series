/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.sigma.edit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.computers.sigma.LanguageLevel;
import dev.jstech.computers.sigma.SigmaSemantics;
import dev.jstech.computers.sigma.SourceFile;
import dev.jstech.computers.sigma.sem.BuiltIns;
import dev.jstech.computers.sigma.sem.SemanticModel;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SigmaCompletionsTest {

    private BuiltIns builtIns;

    /** What every file starts with, on one line so the sources keep their line numbers. */
    private static final String PRELUDE = "using System.*; using System.IO.*; using System.Collections.*; "
            + "using System.Utils.*; using System.Machine.*; using System.Network.*; using System.Operations.*; "
            + "namespace Tests; ";

    @BeforeEach
    void setUp() {
        this.builtIns = new BuiltIns();
    }

    private static List<String> labels(final List<SigmaCompletions.Item> items) {
        return items.stream().map(SigmaCompletions.Item::label).toList();
    }

    private static SigmaCompletions.Item named(final List<SigmaCompletions.Item> items, final String label) {
        return items.stream().filter(i -> i.label().equals(label)).findFirst().orElse(null);
    }

    @Test
    void members_listsWhatTheNetworkOffersOnItsStaticSide() {
        final List<SigmaCompletions.Item> items =
                SigmaCompletions.members(this.builtIns, null, "Network", "Watch", true);
        assertEquals(List.of("Watch", "WatchAbove", "WatchBelow"), labels(items));
    }

    @Test
    void members_writesAMethodWithWhatItTakesAndGivesBack() {
        final List<SigmaCompletions.Item> items =
                SigmaCompletions.members(this.builtIns, null, "Network", "WatchBelow", true);
        assertEquals(1, items.size());
        assertEquals("WatchBelow(string, long, Action<StockEvent>) : Subscription", items.get(0).signature());
        assertEquals(SigmaCompletions.Sort.METHOD, items.get(0).sort());
        assertEquals("Network", items.get(0).owner());
    }

    @Test
    void members_matchesIgnoringCase() {
        assertEquals(List.of("Watch", "WatchAbove", "WatchBelow"),
                labels(SigmaCompletions.members(this.builtIns, null, "Network", "watch", true)));
    }

    @Test
    void members_listsEverythingWhenThePrefixIsEmpty() {
        final List<SigmaCompletions.Item> items =
                SigmaCompletions.members(this.builtIns, null, "Network", "", true);
        assertTrue(items.size() > 3);
        assertTrue(labels(items).contains("Find"));
        assertTrue(labels(items).contains("Servers"));
    }

    @Test
    void members_keepsTheStaticAndInstanceSidesApart() {
        assertTrue(SigmaCompletions.members(this.builtIns, null, "Network", "Watch", false).isEmpty());
        final List<SigmaCompletions.Item> onInstance =
                SigmaCompletions.members(this.builtIns, null, "Subscription", "", false);
        assertEquals(List.of("Id", "Item"), labels(onInstance));
    }

    @Test
    void members_writesAPropertyWithItsType() {
        final SigmaCompletions.Item item =
                named(SigmaCompletions.members(this.builtIns, null, "StockEvent", "", false), "Total");
        assertEquals("Total : long", item.signature());
        assertEquals(SigmaCompletions.Sort.PROPERTY, item.sort());
    }

    @Test
    void members_returnsNothingForATypeThatDoesNotExist() {
        assertTrue(SigmaCompletions.members(this.builtIns, null, "Nowhere", "", true).isEmpty());
        assertTrue(SigmaCompletions.members(this.builtIns, null, "", "", true).isEmpty());
        assertTrue(SigmaCompletions.members(this.builtIns, null, null, "", true).isEmpty());
    }

    @Test
    void members_readsTheProgramsOwnTypes() {
        final SemanticModel model = SigmaSemantics.check(List.of(new SourceFile("Monitor.sgs", PRELUDE + """
                class Monitor : IScript {
                    private int floor = 512;
                    public void OnInit() { }
                    public void OnTick() { }
                    public void OnDestroy() { }
                }
                """))).model();
        final List<String> onInstance =
                labels(SigmaCompletions.members(this.builtIns, model, "Monitor", "On", false));
        assertEquals(List.of("OnDestroy", "OnInit", "OnTick"), onInstance);
    }

    @Test
    void members_reachesWhatAProgramsTypeInheritsFromWhatItImplements() {
        final SemanticModel model = SigmaSemantics.check(List.of(new SourceFile("Monitor.sgs", PRELUDE + """
                class Monitor : IScript {
                    public void OnInit() { }
                    public void OnTick() { }
                    public void OnDestroy() { }
                }
                """))).model();
        assertFalse(SigmaCompletions.members(this.builtIns, model, "Monitor", "", false).isEmpty());
    }

    @Test
    void types_listsTheLanguagesOwnTypes() {
        final List<String> found = labels(SigmaCompletions.types(this.builtIns, null, "Net"));
        assertEquals(List.of("Network"), found);
    }

    @Test
    void types_offersAProgramsOwnTypeOnceWhenItIsNamedAfterOneOfTheLanguages() {
        /*
         * A program may call a type Console in its own namespace, and then that is the Console its
         * names reach: the list offers exactly one, the program's, and not the language's beside it.
         */
        final SemanticModel model = SigmaSemantics.check(List.of(new SourceFile("Monitor.sgs", PRELUDE + """
                class Console {
                    public int Value;
                }
                class Monitor : IScript {
                    public void OnInit() { }
                    public void OnTick() { }
                    public void OnDestroy() { }
                }
                """))).model();
        final List<SigmaCompletions.Item> items = SigmaCompletions.types(this.builtIns, model, "Console");
        assertEquals(1, items.size());
        assertEquals("this program", items.get(0).owner());
    }

    @Test
    void types_listsTheProgramsOwnTypesBesideTheLanguages() {
        final SemanticModel model = SigmaSemantics.check(List.of(new SourceFile("Monitor.sgs", PRELUDE + """
                class Monitor : IScript {
                    public void OnInit() { }
                    public void OnTick() { }
                    public void OnDestroy() { }
                }
                """))).model();
        final List<String> found = labels(SigmaCompletions.types(this.builtIns, model, "Mo"));
        assertEquals(List.of("Monitor"), found);
        assertEquals("this program",
                named(SigmaCompletions.types(this.builtIns, model, "Mo"), "Monitor").owner());
    }

    @Test
    void types_isSortedByName() {
        final List<String> found = labels(SigmaCompletions.types(this.builtIns, null, ""));
        final List<String> sorted = found.stream().sorted().toList();
        assertEquals(sorted, found);
    }

    /* A program the way an editor sees it: the line the caret is on is broken. */

    private static final String FARM = PRELUDE + """
            class Counter {
                public int Count;
                public void Bump() { this.Count = this.Count + 1; }
            }
            class Farm : IScript {
                private Counter counter = new Counter();
                public void OnInit() {
                    int total = 0;
                    counter.
                }
                public void OnTick() { string name = "x"; }
                public void OnDestroy() { }
            }
            """;

    /** The line the broken statement is on, counting from one, with the prelude on the first line. */
    private static final int BROKEN_LINE = 9;

    private record Read(SemanticModel model, SigmaCompletions.Scope scope) {
    }

    /** Checks one file the way an editor does and finds the scope at {@code line}, counting from one. */
    private static Read readAt(final String text, final int line) {
        final SigmaSemantics.Result result =
                SigmaSemantics.checkTolerant(List.of(new SourceFile("Farm.sgs", text)));
        return new Read(result.model(),
                SigmaCompletions.scopeAt(result.model(), result.unit("Farm.sgs"), "Farm.sgs", line));
    }

    private static List<String> names(final SigmaCompletions.Scope scope) {
        return scope.variables().stream().map(v -> v.name()).toList();
    }

    @Test
    void scopeAt_findsTheTypeAroundTheCaretAndTheLocalsAboveItEvenWhenTheLineIsBroken() {
        final Read read = readAt(FARM, BROKEN_LINE);
        assertEquals("Farm", read.scope().enclosing().name());
        assertTrue(names(read.scope()).contains("total"));
        assertFalse(names(read.scope()).contains("name"), "a local of another method is out of reach");
    }

    @Test
    void scopeAt_doesNotSeeALocalDeclaredBelowTheCaret() {
        assertFalse(names(readAt(FARM, 7).scope()).contains("total"));
    }

    @Test
    void scopeAt_isNowhereOutsideEveryType() {
        final Read read = readAt(PRELUDE + "\n\nclass Farm { }", 2);
        assertEquals(SigmaCompletions.Scope.NONE, read.scope());
    }

    @Test
    void resolve_readsAFieldOfTheTypeAroundTheCaretAndListsWhatItsTypeHas() {
        final Read read = readAt(FARM, BROKEN_LINE);
        final SigmaCompletions.Target target =
                SigmaCompletions.resolve(this.builtIns, read.model(), read.scope(), List.of("counter"));
        assertEquals("Tests.Counter", target.type().describe());
        assertFalse(target.staticSide());
        assertEquals(List.of("Bump", "Count"), labels(SigmaCompletions.members(target, "")));
    }

    @Test
    void resolve_readsALocalAboveTheCaret() {
        final Read read = readAt(FARM, BROKEN_LINE);
        final SigmaCompletions.Target target =
                SigmaCompletions.resolve(this.builtIns, read.model(), read.scope(), List.of("total"));
        assertEquals("int", target.type().describe());
    }

    /** An array offers how many places it has, the one thing it answers to. */
    @Test
    void members_ofAnArrayOffersItsLength() {
        final String text = PRELUDE + """
                class Tool {
                    static int Main(string[] args) {
                        args.
                    }
                }
                """;
        final Read read = readAt(text, 3);
        final SigmaCompletions.Target target =
                SigmaCompletions.resolve(this.builtIns, read.model(), read.scope(), List.of("args"));
        assertEquals("string[]", target.type().describe());
        assertEquals(List.of("Length : int"),
                SigmaCompletions.members(target, "le").stream().map(SigmaCompletions.Item::signature).toList());
        assertTrue(SigmaCompletions.members(target, "x").isEmpty());
    }

    @Test
    void resolve_readsThisAndThenAChainThroughIt() {
        final Read read = readAt(FARM, BROKEN_LINE);
        assertEquals("Tests.Farm", SigmaCompletions.resolve(this.builtIns, read.model(), read.scope(),
                List.of("this")).type().describe());
        assertEquals("Tests.Counter", SigmaCompletions.resolve(this.builtIns, read.model(), read.scope(),
                List.of("this", "counter")).type().describe());
    }

    @Test
    void resolve_readsATypeOnItsStaticSide() {
        final Read read = readAt(FARM, BROKEN_LINE);
        final SigmaCompletions.Target target =
                SigmaCompletions.resolve(this.builtIns, read.model(), read.scope(), List.of("Network"));
        assertTrue(target.staticSide());
        assertTrue(labels(SigmaCompletions.members(target, "Watch")).contains("Watch"));
    }

    @Test
    void resolve_givesNothingForANameThatMeansNothing() {
        final Read read = readAt(FARM, BROKEN_LINE);
        assertNull(SigmaCompletions.resolve(this.builtIns, read.model(), read.scope(), List.of("nowhere")));
        assertNull(SigmaCompletions.resolve(this.builtIns, read.model(), read.scope(),
                List.of("counter", "nowhere")));
        assertNull(SigmaCompletions.resolve(this.builtIns, read.model(), read.scope(), List.of()));
    }

    @Test
    void resolve_worksWithoutAScopeForATypeName() {
        final SigmaCompletions.Target target = SigmaCompletions.resolve(this.builtIns, null,
                SigmaCompletions.Scope.NONE, List.of("Network"));
        assertEquals("Network", target.type().describe());
    }

    @Test
    void names_offersTheVariablesInReachFirstAndThenTheTypesOwnMembers() {
        final Read read = readAt(FARM, BROKEN_LINE);
        final List<SigmaCompletions.Item> items =
                SigmaCompletions.names(this.builtIns, read.model(), read.scope(), "");
        assertEquals("total", items.get(0).label());
        assertEquals(SigmaCompletions.Sort.VARIABLE, items.get(0).sort());
        assertEquals(List.of("OnDestroy", "OnInit", "OnTick"),
                labels(SigmaCompletions.names(this.builtIns, read.model(), read.scope(), "On")));
        assertEquals("Farm", named(items, "counter").owner());
    }

    @Test
    void names_stillOffersTheTypesWhenThereIsNoScope() {
        final List<String> found =
                labels(SigmaCompletions.names(this.builtIns, null, SigmaCompletions.Scope.NONE, "Net"));
        assertEquals(List.of("Network"), found);
    }

    @Test
    void scopeAt_descendsIntoATypeNestedInAnother() {
        final String text = PRELUDE + """
                class Outer {
                    public int Width;
                    class Inner {
                        public int Depth;
                        public void Go() {
                            int d = 1;
                            Depth.
                        }
                    }
                }
                """;
        final Read read = readAt(text, 7);
        assertEquals("Inner", read.scope().enclosing().name());
        assertTrue(names(read.scope()).contains("d"));
        assertEquals("int", SigmaCompletions.resolve(this.builtIns, read.model(), read.scope(),
                List.of("Depth")).type().describe());
    }

    @Test
    void resolve_readsATypeDeclaredInAnotherFileOfTheProgram() {
        final SigmaSemantics.Result result = SigmaSemantics.checkTolerant(List.of(
                new SourceFile("Farm.sgs", PRELUDE + """
                        class Farm : IScript {
                            private Silo silo = new Silo();
                            public void OnInit() { silo. }
                            public void OnTick() { }
                            public void OnDestroy() { }
                        }
                        """),
                new SourceFile("Silo.sgs", PRELUDE + """
                        class Silo {
                            public long Stored;
                            public void Fill(long amount) { this.Stored = this.Stored + amount; }
                        }
                        """)));
        final SigmaCompletions.Scope scope =
                SigmaCompletions.scopeAt(result.model(), result.unit("Farm.sgs"), "Farm.sgs", 3);
        final SigmaCompletions.Target target =
                SigmaCompletions.resolve(this.builtIns, result.model(), scope, List.of("silo"));
        assertEquals(List.of("Fill", "Stored"), labels(SigmaCompletions.members(target, "")));
    }

    @Test
    void namespaces_offersTheRootsOnABareUsing() {
        final List<String> roots = labels(SigmaCompletions.namespaces(this.builtIns, null, "", ""));
        assertEquals(List.of("Standard", "System"), roots);
    }

    /** The smaller language opens one namespace and nothing else, so that is all a using line offers it. */
    @Test
    void within_theSmallerLanguage_aUsingOffersItsOneNamespace() {
        final List<SigmaCompletions.Item> roots = SigmaCompletions.within(LanguageLevel.SIGMA,
                SigmaCompletions.namespaces(this.builtIns, null, "", ""));
        assertEquals(List.of("Standard"), labels(roots));
    }

    /** Only the handful of types its library has, each listed under the namespace this language knows it by. */
    @Test
    void within_theSmallerLanguage_onlyTheTypesOfItsLibraryAreOffered() {
        final List<SigmaCompletions.Item> types = SigmaCompletions.within(LanguageLevel.SIGMA,
                SigmaCompletions.types(this.builtIns, null, ""));
        assertEquals(List.of("Computer", "Console", "Convert", "FILE", "File", "Math", "Program", "Random", "Script",
                "Sound", "Speaker", "Time"), labels(types));
        assertEquals("Standard", named(types, "Console").owner());
        assertTrue(labels(SigmaCompletions.types(this.builtIns, null, "")).contains("Network"),
                "which the full language still has");
    }

    /** A version of the language offers none of the library's types that came after it, in either language. */
    @Test
    void within_theFirstVersion_offersNoTypeThatCameLater() {
        final List<String> first = labels(SigmaCompletions.within(LanguageLevel.SIGMA, 1,
                SigmaCompletions.types(this.builtIns, null, "")));
        assertFalse(first.contains("Sound") || first.contains("Speaker"), () -> "Σ 1 offered " + first);
        assertTrue(first.contains("Console"));
        final List<String> sharp = labels(SigmaCompletions.within(LanguageLevel.SIGMA_SHARP, 1,
                SigmaCompletions.types(this.builtIns, null, "")));
        assertFalse(sharp.contains("Sound"), () -> "Σ# 1 offered " + sharp);
        assertTrue(labels(SigmaCompletions.within(LanguageLevel.SIGMA_SHARP, 2,
                SigmaCompletions.types(this.builtIns, null, ""))).contains("Sound"), "Σ# 2 has it");
    }

    /** Random came to the smaller language's library in the second version; the full one always had it. */
    @Test
    void within_theFirstVersion_offersRandomOnlyToTheFullLanguage() {
        final List<SigmaCompletions.Item> types = SigmaCompletions.types(this.builtIns, null, "Ran");
        assertEquals(List.of(), labels(SigmaCompletions.within(LanguageLevel.SIGMA, 1, types)));
        assertEquals(List.of("Random"), labels(SigmaCompletions.within(LanguageLevel.SIGMA, 2, types)));
        assertEquals(List.of("Random"), labels(SigmaCompletions.within(LanguageLevel.SIGMA_SHARP, 1, types)));
    }

    /** A member added to a type the first version had is not offered there, nor a type that came later. */
    @Test
    void within_theFirstVersion_offersNoMemberThatCameLaterOnAnOlderType() {
        final List<SigmaCompletions.Item> compare =
                SigmaCompletions.members(this.builtIns, null, "string", "Comp", true);
        assertEquals(List.of("Compare"), labels(compare));
        assertTrue(SigmaCompletions.within(LanguageLevel.SIGMA_SHARP, 1, compare).isEmpty());
        assertEquals(compare, SigmaCompletions.within(LanguageLevel.SIGMA_SHARP, 2, compare));
        final List<String> toString = SigmaCompletions.within(LanguageLevel.SIGMA_SHARP, 1,
                SigmaCompletions.members(this.builtIns, null, "Convert", "ToString", true)).stream()
                .map(SigmaCompletions.Item::signature).toList();
        assertEquals(List.of("ToString(object) : string"), toString);
        assertFalse(labels(SigmaCompletions.within(LanguageLevel.SIGMA_SHARP, 1,
                SigmaCompletions.types(this.builtIns, null, "ch"))).contains("char"));
    }

    /** The calls written with no type in front of them are offered from their table, each way each is written. */
    @Test
    void names_offersTheCallsWrittenWithNoTypeInFrontOfThem() {
        final List<String> signatures = SigmaCompletions.names(this.builtIns, null, SigmaCompletions.Scope.NONE,
                "p").stream().map(SigmaCompletions.Item::signature).toList();
        assertTrue(signatures.containsAll(List.of("printf(string format, ...) : void", "puts(string text) : void",
                "pow(double x, double y) : double")), () -> "got " + signatures);
        final List<SigmaCompletions.Item> abs =
                SigmaCompletions.names(this.builtIns, null, SigmaCompletions.Scope.NONE, "abs");
        assertEquals(List.of("abs(int n) : int", "abs(double x) : double"),
                abs.stream().map(SigmaCompletions.Item::signature).toList());
    }

    /** The old names came in the second version; printf has been there since the first. */
    @Test
    void within_theFirstVersion_offersPrintfAndNoneOfTheOldNames() {
        final List<SigmaCompletions.Item> offered =
                SigmaCompletions.names(this.builtIns, null, SigmaCompletions.Scope.NONE, "p");
        final List<String> first = labels(SigmaCompletions.within(LanguageLevel.SIGMA, 1, offered));
        assertTrue(first.contains("printf"), () -> "Σ 1 offered " + first);
        assertFalse(first.contains("puts") || first.contains("pow"), () -> "Σ 1 offered " + first);
        assertTrue(labels(SigmaCompletions.within(LanguageLevel.SIGMA, 2, offered)).contains("puts"));
    }

    /** A method of the program's own under one of those names is the one offered, as it is the one called. */
    @Test
    void names_aMethodOfTheProgramsOwnHidesTheOldNameItShares() {
        final String text = PRELUDE + """
                class Farm : IScript {
                    public void puts(int n) { }
                    public void OnInit() {
                        pu
                    }
                    public void OnTick() { }
                    public void OnDestroy() { }
                }
                """;
        final Read read = readAt(text, 4);
        final List<String> puts = SigmaCompletions.names(this.builtIns, read.model(), read.scope(), "pu").stream()
                .filter(item -> item.label().equals("puts")).map(SigmaCompletions.Item::signature).toList();
        assertEquals(List.of("puts(int) : void"), puts);
    }

    /** One costs what the library's call it stands for costs; one that stands for a value of text costs nothing. */
    @Test
    void costOf_pricesAnOldNameAsTheCallItStandsFor() {
        for (final SigmaCompletions.Item abs
                : SigmaCompletions.names(this.builtIns, null, SigmaCompletions.Scope.NONE, "abs")) {
            assertEquals("costs free", SigmaCompletions.costOf(abs).english(), abs.signature());
        }
        final SigmaCompletions.Item puts =
                named(SigmaCompletions.names(this.builtIns, null, SigmaCompletions.Scope.NONE, "puts"), "puts");
        assertEquals("costs free", SigmaCompletions.costOf(puts).english());
        final SigmaCompletions.Item strlen =
                named(SigmaCompletions.names(this.builtIns, null, SigmaCompletions.Scope.NONE, "strlen"), "strlen");
        assertNull(SigmaCompletions.costOf(strlen));
    }

    /** Nor what such a type has, reached through its name. */
    @Test
    void within_theFirstVersion_offersNothingOfATypeThatCameLater() {
        final List<SigmaCompletions.Item> sound = SigmaCompletions.members(this.builtIns, null, "Sound", "", true);
        assertFalse(sound.isEmpty());
        assertTrue(SigmaCompletions.within(LanguageLevel.SIGMA_SHARP, 1, sound).isEmpty());
        assertEquals(sound, SigmaCompletions.within(LanguageLevel.SIGMA_SHARP, 2, sound));
    }

    /** A type of the library offers what the smaller version of it has, and nothing the compiler would refuse. */
    @Test
    void within_theSmallerLanguage_aTypeOffersOnlyTheMembersItKept() {
        final List<SigmaCompletions.Item> all = SigmaCompletions.members(this.builtIns, null, "Console", "", true);
        final List<String> kept = labels(SigmaCompletions.within(LanguageLevel.SIGMA, all));
        assertEquals(List.of("Clear", "Print", "PrintLine", "Read", "ReadBool", "ReadInt", "ReadLine", "Scan"),
                kept.stream().distinct().toList());
        assertTrue(all.size() > kept.size(), "the full language's Console has more than that");
        assertEquals(all, SigmaCompletions.within(LanguageLevel.SIGMA_SHARP, all), "and the full one loses none");
    }

    /** What the program itself declares is the program's, whatever language it is written in. */
    @Test
    void within_theSmallerLanguage_whatTheProgramDeclaresStays() {
        final String source = "using Standard.*; namespace Tests; class Silo { public int Stored; "
                + "public int Room() { return 0; } } class M { static void Main() { Silo silo = new Silo(); } }";
        final SigmaSemantics.Result result =
                SigmaSemantics.checkTolerant(List.of(new SourceFile("a.sg", source)));
        final List<SigmaCompletions.Item> types = SigmaCompletions.within(LanguageLevel.SIGMA,
                SigmaCompletions.types(this.builtIns, result.model(), "Si"));
        assertEquals(List.of("Silo"), labels(types));
        final List<SigmaCompletions.Item> members = SigmaCompletions.within(LanguageLevel.SIGMA,
                SigmaCompletions.members(this.builtIns, result.model(), "Silo", "", false));
        assertEquals(List.of("Room", "Stored"), labels(members));
    }

    @Test
    void namespaces_offersWhatIsUnderTheOneBeingReachedInto() {
        final List<String> under = labels(SigmaCompletions.namespaces(this.builtIns, null, "System", ""));
        assertTrue(under.contains("IO"), () -> "System.IO is offered; got " + under);
        assertTrue(under.contains("Collections"), () -> "and System.Collections; got " + under);
        assertTrue(under.contains("*"), () -> "with the star that opens the whole of it; got " + under);
        // The types in that namespace belong on a using too, since a using may name one.
        assertTrue(under.contains("Console"), () -> "and the types it holds; got " + under);
    }

    @Test
    void namespaces_narrowsByWhatHasBeenTypedAndPutsTheNamespacesFirst() {
        final List<String> matching = labels(SigmaCompletions.namespaces(this.builtIns, null, "System", "i"));
        assertEquals("IO", matching.getFirst(), () -> "the namespace leads; got " + matching);
        assertTrue(matching.contains("IScript"), () -> "the types that match follow it; got " + matching);
        assertTrue(labels(SigmaCompletions.namespaces(this.builtIns, null, "System", "zz")).isEmpty());
    }

    @Test
    void costOf_pricesACallTheWayTheListWritesIt() {
        final List<SigmaCompletions.Item> calls =
                SigmaCompletions.members(this.builtIns, null, "Gateway", "Call", true);
        assertEquals(4, calls.size());
        final SigmaCompletions.Item handingOne = calls.stream()
                .filter(item -> item.signature().equals("Call(string, string, object) : object"))
                .findFirst().orElseThrow();
        assertEquals("costs 105", SigmaCompletions.costOf(handingOne).english());
    }

    @Test
    void costOf_saysWhatWritingAWidgetsValueCosts() {
        final SigmaCompletions.Item text =
                named(SigmaCompletions.members(this.builtIns, null, "Button", "Text", false), "Text");
        assertEquals("costs free to read and 50 to write", SigmaCompletions.costOf(text).english());
    }

    @Test
    void costOf_pricesTheFreeCallsTooAndLeavesTheLanguagesOwnMembersUnpriced() {
        final SigmaCompletions.Item floor =
                named(SigmaCompletions.members(this.builtIns, null, "Math", "Floor", true), "Floor");
        assertEquals("costs free", SigmaCompletions.costOf(floor).english());
        final SigmaCompletions.Item length =
                named(SigmaCompletions.members(this.builtIns, null, "string", "Length", false), "Length");
        assertNull(SigmaCompletions.costOf(length));
    }
}
