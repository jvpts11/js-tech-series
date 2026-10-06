/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.sigma.sem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.computers.sigma.LanguageLevel;
import dev.jstech.computers.sigma.SigmaCompiler;
import dev.jstech.computers.sigma.SigmaVersions;
import dev.jstech.computers.sigma.SourceFile;
import dev.jstech.computers.vm.listing.AsmProgram;
import dev.jstech.computers.vm.listing.AsmReader;
import dev.jstech.computers.vm.program.IHost;
import dev.jstech.computers.vm.program.Process;
import dev.jstech.computers.vm.program.ProgramImage;
import dev.jstech.computers.vm.program.Values;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * The calls written with no type in front of them: each old name is the library's own call written the short way,
 * the listing is the long way's to the byte, and a name the program has for itself stays the program's.
 */
class BareFunctionsTest {

    private static final long ROOM = 64L * 1024;
    private static final int PLENTY = 1_000_000;

    private static final String PRELUDE = "using Standard.*; namespace Tests; ";
    private static final String SHARP_PRELUDE = "using System.*; using Standard.*; namespace Tests; ";

    /** Each old name beside the call a player would have written the long way, in a method body. */
    private static final List<List<String>> SAME_LISTINGS = List.of(
            List.of("puts(\"hi\");", "Console.PrintLine(\"hi\");"),
            List.of("putchar('x');", "Console.Print('x');"),
            List.of("string s = gets();", "string s = Console.ReadLine();"),
            List.of("int c = getchar();", "int c = Console.Read();"),
            List.of("int n; int got = scanf(\"%d\", out n); double x; scanf(\" %lf \", out x);",
                    "int n; int got = Console.Scan(out n); double x; Console.Scan(out x);"),
            List.of("exit(1);", "Program.Exit(1);"),
            List.of("int a = abs(-3); double b = abs(-2.5);", "int a = Math.Abs(-3); double b = Math.Abs(-2.5);"),
            List.of("double r = sqrt(16);", "double r = Math.Sqrt(16);"),
            List.of("double p = pow(2, 10);", "double p = Math.Pow(2, 10);"),
            List.of("double f = floor(2.7);", "double f = Math.Floor(2.7);"),
            List.of("int m = min(3, 4); double n = max(1.5, 2);",
                    "int m = Math.Min(3, 4); double n = Math.Max(1.5, 2);"),
            List.of("double d = atof(\"2.5\");", "double d = Convert.ToDouble(\"2.5\");"),
            List.of("int n = 42; string t = itoa(n);", "int n = 42; string t = Convert.ToString(n);"),
            List.of("srand(7);", "Random.Seed(7);"),
            List.of("int r = rand();", "int r = Random.Next(32768);"),
            List.of("string s = \"abc\"; int n = strlen(s);", "string s = \"abc\"; int n = s.Length;"),
            List.of("string s = \"abc\"; int i = strstr(s, \"b\");", "string s = \"abc\"; int i = s.IndexOf(\"b\");"),
            List.of("int n = 3; string t = sprintf(\"%d items\", n);", "int n = 3; string t = \"\" + n + \" items\";"),
            List.of("int c = strcmp(\"a\", \"b\");", "int c = string.Compare(\"a\", \"b\");"),
            List.of("char u = toupper('a'); char l = tolower('B');",
                    "char u = char.ToUpper('a'); char l = char.ToLower('B');"),
            List.of("bool d = isdigit('7'); bool a = isalpha('x'); bool w = isspace(' ');",
                    "bool d = char.IsDigit('7'); bool a = char.IsLetter('x'); bool w = char.IsWhiteSpace(' ');"),
            List.of("string h = itoa(255, 16);", "string h = Convert.ToString(255, 16);"),
            List.of("int n = atoi(\"12\");", "int n = Convert.ToInt(\"12\", 0);"),
            List.of("string d; strcpy(out d, \"abc\");", "string d; d = \"abc\";"),
            List.of("string d = \"a\"; strcat(ref d, \"b\");", "string d = \"a\"; d = d + \"b\";"),
            List.of("strcpy(out string e, \"x\"); puts(e);", "string e; e = \"x\"; puts(e);"),
            List.of("FILE f = fopen(\"a.txt\", \"r\"); fclose(f);", "FILE f = File.Open(\"a.txt\", \"r\"); f.Close();"),
            List.of("FILE f = fopen(\"a.txt\", \"r+\"); string l; bool got = fgets(out l, f); fputs(\"x\", f); "
                            + "int c = fgetc(f); fputc('y', f); bool e = feof(f); rewind(f); fseek(f, 3); "
                            + "int p = ftell(f);",
                    "FILE f = File.Open(\"a.txt\", \"r+\"); string l; bool got = f.ReadLine(out l); f.Write(\"x\"); "
                            + "int c = f.Read(); f.Write('y'); bool e = f.AtEnd; f.Seek(0); f.Seek(3); "
                            + "int p = f.Position;"),
            List.of("FILE f = fopen(\"a.txt\", \"w\"); int n = 4; fprintf(f, \"%d\", n); fscanf(f, \"%d\", out n);",
                    "FILE f = File.Open(\"a.txt\", \"w\"); int n = 4; f.Write(\"\" + n); f.Scan(out n);"),
            List.of("bool r = remove(\"a.txt\"); bool m = rename(\"a.txt\", \"b.txt\");",
                    "bool r = File.Delete(\"a.txt\"); bool m = File.Move(\"a.txt\", \"b.txt\");"));

    private static SigmaCompiler.Result built(final String members, final String body, final LanguageLevel level,
                                              final int version) {
        final String source = (level.full() ? SHARP_PRELUDE : PRELUDE) + "class Says : Script { " + members
                + " public override void OnTick() { " + body + " } }";
        return SigmaCompiler.compile(List.of(new SourceFile(level.full() ? "Says.sgs" : "Says.sg", source)),
                level.full() ? AsmProgram.DEFAULT_ISA : "jsc:ia_16", level, version);
    }

    private static String listing(final String body, final LanguageLevel level) {
        final SigmaCompiler.Result built = built("", body, level, SigmaVersions.NEWEST);
        assertTrue(built.ok(), () -> body + "\n" + String.join("\n", built.lines()));
        return built.assembly();
    }

    /** What the program printed, a line to an entry. */
    private static List<String> printed(final String members, final String body, final LanguageLevel level,
                                        final int version) {
        final SigmaCompiler.Result built = built(members, body, level, version);
        assertTrue(built.ok(), () -> String.join("\n", built.lines()));
        final AsmReader reader = new AsmReader(built.assembly());
        final ProgramImage program = ProgramImage.of(reader.read());
        assertFalse(reader.hasProblems());
        final Process process = new Process(program, ROOM, IHost.still());
        final Values.Obj self = process.create(program.entryPoint());
        assertNotNull(self);
        process.begin(self, "OnTick");
        process.step(PLENTY);
        assertEquals(Process.State.FINISHED, process.state(), () -> String.valueOf(process.message()));
        return process.console();
    }

    private static String refusal(final String body, final LanguageLevel level, final int version) {
        final SigmaCompiler.Result built = built("", body, level, version);
        assertFalse(built.ok(), "it was taken");
        return String.join("\n", built.lines());
    }

    @Test
    void all_printfCameWithTheLanguagesAndTheOldNamesInTheSecondVersion() {
        for (final BareFunctions.Function function : BareFunctions.all()) {
            assertFalse(function.forms().isEmpty(), function.name());
            assertTrue(function.in(LanguageLevel.SIGMA) && function.in(LanguageLevel.SIGMA_SHARP), function.name());
            assertEquals("printf".equals(function.name()) ? SigmaVersions.FIRST : 2, function.since(),
                    function.name());
        }
    }

    @Test
    void named_findsOneByItsNameAndNothingForAnyOtherName() {
        assertEquals(BareFunctions.Shape.READ_OFF_THE_FIRST, BareFunctions.named("strlen").shape());
        assertNull(BareFunctions.named("Strlen"));
        assertEquals(List.of("printf", "puts", "putchar", "pow"),
                BareFunctions.startingWith("p").stream().map(BareFunctions.Function::name).toList());
    }

    /** Nothing of the short way is left in what is written down, in either language. */
    @Test
    void everyOldName_writesTheListingTheLongWayWrites() {
        for (final LanguageLevel level : LanguageLevel.values()) {
            for (final List<String> pair : SAME_LISTINGS) {
                assertEquals(listing(pair.get(1), level), listing(pair.getFirst(), level),
                        () -> level + ": " + pair.getFirst());
            }
        }
    }

    @Test
    void theOldNames_doWhatTheLongWayDoes() {
        final List<String> out = printed("", "puts(itoa(strlen(\"abcd\") + strstr(\"hello\", \"ll\"))); "
                + "puts(sprintf(\"%s=%d\", \"n\", abs(-4))); srand(7); int r = rand(); "
                + "if (r >= 0 && r < 32768) { puts(\"in range\"); }", LanguageLevel.SIGMA, SigmaVersions.NEWEST);
        assertEquals(List.of("6", "n=4", "in range"), out);
    }

    /** The small helpers: text compared, characters tested and changed, numbers in a base, text read as 0. */
    @Test
    void theHelpers_doWhatTheirNamesSay() {
        final List<String> out = printed("", "printf(\"%d %d %d\\n\", strcmp(\"apple\", \"banana\"), "
                + "strcmp(\"b\", \"b\"), strcmp(\"b\", \"a\")); printf(\"%c%c\\n\", toupper('a'), tolower('B')); "
                + "if (isdigit('7') && isalpha('x') && isspace(' ') && !isdigit('x')) { puts(\"tested\"); } "
                + "puts(itoa(255, 16) + \" \" + itoa(-1, 16) + \" \" + itoa(5, 2) + \" \" + itoa(35, 36)); "
                + "printf(\"%d %d %d\\n\", atoi(\"12\"), atoi(\" 7 \"), atoi(\"twelve\"));",
                LanguageLevel.SIGMA, SigmaVersions.NEWEST);
        assertEquals(List.of("-1 0 1", "Ab", "tested", "ff ffffffff 101 z", "12 7 0"), out);
    }

    @Test
    void itoa_inABaseThereIsNotStopsTheProgramSayingSo() {
        final SigmaCompiler.Result built = built("", "puts(itoa(5, 40));", LanguageLevel.SIGMA,
                SigmaVersions.NEWEST);
        assertTrue(built.ok(), () -> String.join("\n", built.lines()));
        final ProgramImage program = ProgramImage.of(new AsmReader(built.assembly()).read());
        final Process process = new Process(program, ROOM, IHost.still());
        process.begin(process.create(program.entryPoint()), "OnTick");
        process.step(PLENTY);
        assertEquals(Process.State.HALTED, process.state());
        assertTrue(process.message().english().contains("there is no base 40"), process.message().english());
    }

    /** What came to a type the first version already had is refused there, named with its type. */
    @Test
    void aMemberAddedInTheSecondVersion_isRefusedAtTheFirst() {
        assertTrue(refusal("int c = string.Compare(\"a\", \"b\");", LanguageLevel.SIGMA_SHARP, SigmaVersions.FIRST)
                .contains("'string.Compare' needs Σ# 2; this project is Σ# 1"));
        assertTrue(refusal("string h = Convert.ToString(255, 16);", LanguageLevel.SIGMA_SHARP, SigmaVersions.FIRST)
                .contains("'Convert.ToString' needs Σ# 2"));
        final String character = refusal("bool d = char.IsDigit('1');", LanguageLevel.SIGMA_SHARP,
                SigmaVersions.FIRST);
        assertTrue(character.contains("'char' needs Σ# 2") && !character.contains("'char.IsDigit'"), character);
        assertTrue(built("", "string s = Convert.ToString(255); int n = Convert.ToInt(\"4\");",
                LanguageLevel.SIGMA_SHARP, SigmaVersions.FIRST).ok(), "what was there before is still there");
    }

    /** A program that had a method or a variable under one of the names before they came keeps calling its own. */
    @Test
    void aNameTheProgramHasForItself_staysTheProgramsAtEveryVersion() {
        assertEquals(List.of("7"), printed("static int abs(int n) { return 7; }", "printf(\"%d\\n\", abs(-3));",
                LanguageLevel.SIGMA_SHARP, SigmaVersions.FIRST));
        assertEquals(List.of("9"), printed("", "Func<int, int> abs = n => 9; printf(\"%d\\n\", abs(1));",
                LanguageLevel.SIGMA_SHARP, SigmaVersions.FIRST));
    }

    @Test
    void anOldName_atTheFirstVersion_needsTheSecond() {
        assertTrue(refusal("puts(\"x\");", LanguageLevel.SIGMA_SHARP, SigmaVersions.FIRST)
                .contains("'puts' needs Σ# 2; this project is Σ# 1"));
        assertTrue(refusal("int r = rand();", LanguageLevel.SIGMA, SigmaVersions.FIRST)
                .contains("'rand' needs Σ 2; this project is Σ 1"));
        assertEquals(List.of("1"), printed("", "printf(\"%d\\n\", 1);", LanguageLevel.SIGMA, SigmaVersions.FIRST));
    }

    /** A value the name does not take is reported under the name the player wrote. */
    @Test
    void anOldName_givenWhatItDoesNotTake_isRefusedUnderItsOwnName() {
        assertTrue(refusal("int a = abs(\"x\");", LanguageLevel.SIGMA, SigmaVersions.NEWEST)
                .contains("no version of 'abs' takes those arguments"));
        assertTrue(refusal("int r = rand(5);", LanguageLevel.SIGMA, SigmaVersions.NEWEST)
                .contains("no version of 'rand' takes those arguments"));
        assertTrue(refusal("string t = sprintf(\"%d\", 1, 2);", LanguageLevel.SIGMA, SigmaVersions.NEWEST)
                .contains("sprintf: the format has 1 hole and the call gives 2 values"));
    }

    /** strcpy and strcat in C's order: the place first, with out to be written and with ref to be joined onto. */
    @Test
    void strcpyAndStrcat_copyIntoAndJoinOntoThePlaceHandedFirst() {
        assertEquals(List.of("Iron Ingot", "Iron Ingot"), printed("", "string d = \"Iron\"; "
                + "strcat(ref d, \" Ingot\"); string c; strcpy(out c, d); puts(c); puts(strcpy(out c, d));",
                LanguageLevel.SIGMA, SigmaVersions.NEWEST));
        assertTrue(refusal("int x = -2; int y = abs(ref x);", LanguageLevel.SIGMA, SigmaVersions.NEWEST)
                .contains("'ref' is written only for strcat"));
        assertTrue(refusal("string d = \"a\"; strcat(out d, \"b\");", LanguageLevel.SIGMA, SigmaVersions.NEWEST)
                .contains("no version of 'strcat' takes those arguments"));
        assertEquals(List.of("3"), printed("", "int ref = 3; puts(itoa(ref));", LanguageLevel.SIGMA,
                SigmaVersions.NEWEST), "a variable called ref is still a variable");
    }

    /** scanf reads one value a call, of a kind its hole names, into a variable handed with out. */
    @Test
    void scanf_readsOneValueACallIntoAVariableHandedWithOut() {
        assertTrue(refusal("int a; int b; scanf(\"%d %d\", out a, out b);", LanguageLevel.SIGMA,
                SigmaVersions.NEWEST).contains("scanf: one value a call"));
        assertTrue(refusal("int a = 0; scanf(\"%d\", a);", LanguageLevel.SIGMA, SigmaVersions.NEWEST)
                .contains("scanf: one value a call"));
        assertTrue(refusal("int a; scanf(\"%x\", out a);", LanguageLevel.SIGMA, SigmaVersions.NEWEST)
                .contains("'%x' is no hole scanf reads"));
        assertTrue(refusal("string s; scanf(\"%d\", out s);", LanguageLevel.SIGMA, SigmaVersions.NEWEST)
                .contains("scanf: '%d' takes a whole number, and this is string"));
        assertTrue(refusal("int a; scanf(\"%d\", out a);", LanguageLevel.SIGMA_SHARP, SigmaVersions.FIRST)
                .contains("'scanf' needs Σ# 2"));
    }

    /** The smaller language gained Random with rand, and only the two calls rand and srand come down to. */
    @Test
    void random_isInTheSmallerLanguagesLibraryFromTheSecondVersion() {
        assertTrue(refusal("int r = Random.Next(6);", LanguageLevel.SIGMA, SigmaVersions.FIRST)
                .contains("'Random' needs Σ 2; this project is Σ 1"));
        assertTrue(built("", "int r = Random.Next(6);", LanguageLevel.SIGMA_SHARP, SigmaVersions.FIRST).ok(),
                "the full language always had it");
        assertTrue(refusal("double d = Random.NextDouble();", LanguageLevel.SIGMA, SigmaVersions.NEWEST)
                .contains("S3052"));
    }
}
