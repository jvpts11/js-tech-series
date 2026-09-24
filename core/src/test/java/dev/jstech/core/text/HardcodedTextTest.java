/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.text;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * No fixed words a player reads are written into the code.
 *
 * <p>Every sentence a player reads is declared as a {@link TextKey} and translated. Any other fixed text fails the
 * build, except in the files listed as data below, each with the number of pieces it holds and why they are not
 * words to translate: the names of the fictional software, the files of a virtual disk, what a player types.
 *
 * <p>What counts is a string literal that is either handed to something that puts words in front of a player (a
 * screen drawing, a message, a terminal line, the answer a command gives, the label of a button or a checkbox) and
 * has a word in it, two letters or more, or that reads as a sentence, two words or more. What does not count is
 * what is not for a player, or is data on purpose: a log line, an exception's message, an annotation, a sentence's
 * own English where it is declared (a {@code TextKey}, or the name and description something is declared with,
 * {@code .named(...)} and {@code .described(...)} chained on it), and words wrapped in {@code Text.literal}, which
 * is how data is marked so it can be found.
 */
class HardcodedTextTest {

    /** The mods whose text a player reads; the test mod is for development only. */
    private static final Set<String> MODULES = Set.of("core", "computers", "industrial");

    /** Methods that put what they are handed in front of a player. */
    private static final Set<String> SINKS = Set.of("drawString", "drawCenteredString", "drawWordWrap",
            "text", "textS", "textRight", "textCenter", "textSRight", "textSCenter",
            "line", "error", "ok", "fail", "warn", "info", "dim", "accent", "header", "row", "entry", "say", "plain",
            "showBalloon", "raise", "displayClientMessage", "sendSystemMessage", "broadcastSystemMessage",
            "sendConsoleLine", "setTooltip", "withTooltip", "setPlaceholder", "submenu");

    /** What a declaration chains on to say its English: a thing's name, what it does, a sound's subtitle. */
    private static final Set<String> DECLARATIONS = Set.of("named", "described", "subtitle");

    /** The toolkit's controls whose first words are the label a player reads on them. */
    private static final Set<String> LABELLED = Set.of("Button", "Label", "Checkbox", "Popup");

    /** What reads or writes a map or a tag by key. */
    private static final Set<String> KEY_ACCESSORS = Set.of("get", "getOrDefault", "getString", "getInt", "getLong",
            "getBoolean", "getCompound", "getList", "put", "putString", "putInt", "putLong", "putBoolean", "contains",
            "containsKey", "remove");

    /** A class that is an exception or an error, whose message goes to whoever reads the log. */
    private static final Pattern THROWABLE_CLASS = Pattern.compile("extends\\s+\\w*(Exception|Error)\\b");

    private static final String COMPUTERS = "computers/src/main/java/dev/jstech/computers/";
    private static final String INDUSTRIAL = "industrial/src/main/java/dev/jstech/industrial/";

    private static final String ORIGIN = "the name an Operation's origin is logged under, an identifier of what asked";
    private static final String LOGO = "a product's name and maker, drawn as part of its logo";
    private static final String QUERY = "statements of the query language, which read the same in every language";
    private static final String COMMANDS = "commands a player types, or that a program types at the shell";
    private static final String BUILD_LOG = "a data generator's name, for the build log";
    private static final String PRODUCTS = "names of fictional products, systems and makers";
    private static final String FILES = "folder names and the contents of files on a virtual disk";
    private static final String SIGNATURES = "signatures of the programming language's own calls";
    private static final String SAVE_TAG = "the tag a machine's parts are saved under";

    /**
     * The files whose fixed text is data, not words to translate, and how many pieces each holds. The count is exact,
     * so a sentence added next to them is still caught.
     */
    private static final Map<String, Data> DATA = Map.ofEntries(
            data(COMPUTERS + "block/MonitorBlock.java", 1, "a live medium's host name, after the system it boots"),
            data(COMPUTERS + "block/part/ExportBusPart.java", 1, ORIGIN),
            data(COMPUTERS + "block/part/ImportBusPart.java", 1, ORIGIN),
            data(COMPUTERS + "operation/MoveLabels.java", 1, ORIGIN),
            data(COMPUTERS + "client/BootSplashArt.java", 3, LOGO),
            data(COMPUTERS + "client/DesktopSplashArt.java", 2, LOGO),
            data(COMPUTERS + "client/os/CdeSplashArt.java", 1, LOGO),
            data(COMPUTERS + "client/MaintenanceTerminalTab.java", 3, QUERY),
            data(COMPUTERS + "client/NmsApp.java", 4, QUERY),
            data(COMPUTERS + "operation/payload/terminal/TerminalPayloads.java", 3, QUERY),
            data(COMPUTERS + "client/os/CodeWorkspace.java", 2, "lines of code a stub is written with"),
            data(COMPUTERS + "client/os/DeskFiles.java", 1, PRODUCTS),
            data(COMPUTERS + "client/os/FilesApp.java", 1, PRODUCTS),
            data(COMPUTERS + "client/os/ExposureApp.java", 1, COMMANDS),
            data(COMPUTERS + "client/os/VirtualStudioApp.java", 6, COMMANDS + "; and the studio's own name"),
            data(COMPUTERS + "client/os/VirtualStudioCodeApp.java", 7, COMMANDS + "; and the editor's own name"),
            data(COMPUTERS + "blockentity/AbstractComputerBlockEntity.java", 1, SAVE_TAG),
            data(COMPUTERS + "blockentity/MainframeBlockEntity.java", 1, SAVE_TAG),
            data(COMPUTERS + "client/os/ItemCategories.java", 7,
                    "a category's id, which a saved filter keeps; what it reads as is translated"),
            data(COMPUTERS + "client/os/TtyChrome.java", 1, "the mode line of a period editor, as the editor drew it"),
            data(COMPUTERS + "client/theme/MonitorFrameStyle.java", 1, "a monitor's model name on its bezel"),
            data(COMPUTERS + "datagen/JscRecipeMachinesProvider.java", 1, BUILD_LOG),
            data(COMPUTERS + "datagen/advancement/ConditionalAdvancementProvider.java", 1, BUILD_LOG),
            data(COMPUTERS + "hardware/StorageTier.java", 3, PRODUCTS),
            data(COMPUTERS + "integration/jei/JscJeiPlugin.java", 1, "a description for the log"),
            data(COMPUTERS + "machine/MachinePrograms.java", 1, "a word for the log"),
            data(COMPUTERS + "machine/PortsTree.java", 2, FILES),
            data(COMPUTERS + "machine/SourceChains.java", 14, "build flags, as a source build prints and reads them"),
            data(COMPUTERS + "operation/payload/program/ProgramPayloads.java", 1,
                    "a process's name, which the actions on the process address it by"),
            data(COMPUTERS + "os/Branding.java", 2, PRODUCTS),
            data(COMPUTERS + "os/KernelNames.java", 3, "kernel names as a system reports them"),
            data(COMPUTERS + "os/OsBootstrap.java", 5, PRODUCTS),
            data(COMPUTERS + "os/OsRegistry.java", 2, "the kind of entry a registration names in the log"),
            data(COMPUTERS + "os/SoftwareHouse.java", 31, PRODUCTS),
            data(COMPUTERS + "os/edit/NanoWords.java", 1, "an editor's name and version on its title row"),
            data(COMPUTERS + "os/edit/project/ProjectTemplate.java", 6, "the source files a new project starts with"),
            data(COMPUTERS + "os/fs/InstallerLayout.java", 29, FILES),
            data(COMPUTERS + "os/fs/McDosTree.java", 7, FILES),
            data(COMPUTERS + "os/fs/ProgramFilesProjection.java", 10, FILES),
            data(COMPUTERS + "os/fs/SystemLayout.java", 2, FILES),
            data(COMPUTERS + "os/fs/TrashFolder.java", 1, FILES),
            data(COMPUTERS + "program/KnotRepository.java", 1, "a revision's message as the repository stores it"),
            data(COMPUTERS + "program/cli/ConsoleGreeting.java", 3, PRODUCTS),
            data(COMPUTERS + "program/cli/ICliFiles.java", 1, COMMANDS),
            data(COMPUTERS + "program/cli/ICliProcesses.java", 2, COMMANDS),
            data(COMPUTERS + "program/cli/ICliRemote.java", 2, COMMANDS),
            data(COMPUTERS + "program/cli/MachineFacts.java", 3, COMMANDS),
            data(COMPUTERS + "program/cli/interac/InteracCommand.java", 2, COMMANDS),
            data(COMPUTERS + "program/cli/ScreenfetchLogos.java", 16, "logos drawn in characters"),
            data(COMPUTERS + "sigma/LanguageLevel.java", 1, PRODUCTS),
            data(COMPUTERS + "sigma/edit/SigmaCompletions.java", 3,
                    SIGNATURES + ", and the marks for who declares a candidate, which are never drawn"),
            data(COMPUTERS + "vm/program/NumberFunctions.java", 4, SIGNATURES),
            data(COMPUTERS + "vm/system/SystemApi.java", 1, SIGNATURES),
            data("core/src/main/java/dev/jstech/core/config/ConfigValidator.java", 11,
                    "why a configuration value was set aside, written to the log"),
            data(INDUSTRIAL + "client/CoalGeneratorScreen.java", 1, "the energy unit's symbol"),
            data(INDUSTRIAL + "client/CompressorScreen.java", 1, "the energy unit's symbol"),
            data(INDUSTRIAL + "client/ElectricFurnaceScreen.java", 1, "the energy unit's symbol"),
            data(INDUSTRIAL + "client/MaceratorScreen.java", 1, "the energy unit's symbol"));

    /** Who a line for the developer is written through. */
    private static final Set<String> LOGGERS = Set.of("LOGGER", "LOG", "logger", "log");

    /**
     * An advancement tab declaring its advancements on itself: the title and description handed in are the English the
     * language file is generated from, under keys the advancement is shown by.
     */
    private static final Set<String> ADVANCEMENT_DECLARATIONS = Set.of("root", "task", "goal", "challenge", "secret",
            "goalWith", "challengeOfAll");

    /** Two runs of letters with something between them: words, not an identifier. */
    private static final Pattern PROSE = Pattern.compile("[A-Za-z]{2,}[^A-Za-z]+[A-Za-z]{2,}");
    /** A word: two letters in a row, where a lone letter is a mark such as a tick's x. */
    private static final Pattern WORD = Pattern.compile("[A-Za-z]{2}");
    /** Capitalised words and nothing else, the shape of a title or a label. */
    private static final Pattern TITLE = Pattern.compile("[A-Z][a-z]+( [A-Za-z]+)*");

    @Test
    void fixedText_livesOnlyInTextKeysAndData() {
        final Map<String, List<Hit>> found = hitsByFile();
        final List<String> wrong = new ArrayList<>();
        found.forEach((file, hits) -> {
            final Data data = DATA.get(file);
            if (data != null && hits.size() == data.count()) {
                return;
            }
            wrong.add(data == null ? file + ": declare each as a TextKey, or wrap data in Text.literal"
                    : file + " holds " + hits.size() + " pieces, where " + data.count() + " are data (" + data.reason()
                            + "); new words a player reads are TextKeys, and fewer means the list is stale");
            for (final Hit hit : hits) {
                wrong.add("    line " + hit.line() + ": \"" + hit.text() + "\"");
            }
        });
        DATA.forEach((file, data) -> {
            if (!found.containsKey(file)) {
                wrong.add(file + " is listed as data but holds none; take it off the list");
            }
        });
        assertTrue(wrong.isEmpty(), () -> "fixed words a player reads are written into the code:\n"
                + String.join("\n", wrong));
    }

    /*
     * A scanner pointed at the wrong place reads nothing and finds none, which would pass for the wrong reason. What
     * is checked is that every mod's sources are there to be read.
     */
    @Test
    void everyModIsRead() {
        final Path root = Path.of("").toAbsolutePath().getParent();
        for (final String module : MODULES) {
            assertTrue(Files.isDirectory(root.resolve(module).resolve("src").resolve("main").resolve("java")),
                    () -> "the sources of " + module + " were not found");
        }
    }

    @Test
    void theScannerTellsDataFromWordsAPlayerReads() {
        final String source = """
                class A {
                    void f() {
                        LOGGER.warn("a log line for developers");
                        throw new IllegalStateException("an exception message here");
                        Objects.requireNonNull(name, "a name is never missing here");
                        ctx.out().line("Shown to the player");
                        ctx.out().line(Text.literal("/usr/ports"));
                        final String label = "Low on memory";
                        tag.getString("History");
                        drawString(font, "Power", 1, 2);
                        // "a comment with words"
                        KEY = TextKey.of("jsc.key", "the English itself");
                        BLOCK = CONTENT.block("x", X::new)
                                .named("Electric Furnace").register();
                        SPEC = ProgramSpec.of(id, "x").described("Keeps the network's files");
                        this.task("first", "root", ICON, "First Steps", "Start the machine", on(EVENT));
                        builder.comment("Whether the machine asks first").define("ask", true);
                        layout.text("title", 4, 4, 15, 1.0f);
                        ok = add(new Button("OK", this::close));
                        twice = add(new Button("x2", this::twice));
                        g.drawString(font, "x", 1, 2, INK, false);
                        field.setPlaceholder("Search");
                        read = add(new Button(GameText.resolve(READ), () -> set("share", path + " read")));
                        items = SavedValue.written(fresh, LOGGER, "what a server was storing here");
                        return DataResult.error(() -> "an empty key cannot be read back");
                        text(stroke.get("Kind"));
                        return "Calculator";
                        return "jsc:calculator";
                    }
                    @Deprecated(since = "the annotation text")
                    void g() { }
                    @Override
                    void h() { ctx.out().error("after a bare annotation"); }
                }
                """;
        final List<String> read = scan(source).stream().map(Hit::text).toList();
        assertTrue(read.equals(List.of("Shown to the player", "Low on memory", "Power", "OK", "Search", "Calculator",
                "after a bare annotation")), () -> "read " + read);
    }

    /** One fixed piece of text: the line it is on and what it says. */
    private record Hit(int line, String text) {
    }

    /** How many pieces of fixed text a data file holds, and why they are data. */
    private record Data(int count, String reason) {
    }

    /**
     * A call the scanner is inside: what it is called, what it is called on, what kind of call it is, which of its
     * arguments the scanner has reached, and whether a logger was handed to it.
     */
    private static final class Frame {

        private final String name;
        private final String receiver;
        private final boolean constructor;
        private final boolean quiet;
        /** Which argument the scanner is in, counted from nought by the commas it has passed. */
        private int argument;
        /** Whether a logger is one of the arguments: the words beside it are what the log line is about. */
        private boolean logged;

        Frame(final String name, final String receiver, final boolean constructor, final boolean quiet) {
            this.name = name;
            this.receiver = receiver;
            this.constructor = constructor;
            this.quiet = quiet;
        }

        boolean excludes() {
            return this.quiet
                    || this.logged
                    || (this.receiver != null && LOGGERS.contains(this.receiver))
                    // A codec's complaint is for the log of whoever loads the data, never for a player.
                    || ("DataResult".equals(this.receiver) && "error".equals(this.name))
                    // A key a map or a tag is read or written by is data, whatever it is handed to.
                    || (this.name != null && KEY_ACCESSORS.contains(this.name))
                    || (this.constructor && this.name != null
                            && (this.name.endsWith("Exception") || this.name.endsWith("Error")))
                    // What a null check says when it fails is an exception's message, for whoever reads the log.
                    || ("Objects".equals(this.receiver) && "requireNonNull".equals(this.name))
                    || ("TextKey".equals(this.receiver) && "of".equals(this.name))
                    || (this.constructor && "TextKey".equals(this.name))
                    // A declaration's name or description, chained on it: the English the language file is made from.
                    || (this.name != null && DECLARATIONS.contains(this.name) && this.receiver == null)
                    || ("this".equals(this.receiver) && ADVANCEMENT_DECLARATIONS.contains(this.name))
                    // A config value's comment is written into the config file above it: a file's words, in English.
                    || "comment".equals(this.name)
                    // A layout's text element is named for its overlap report; the words drawn there come elsewhere.
                    || (("layout".equals(this.receiver) || "l".equals(this.receiver)) && "text".equals(this.name))
                    || ("Text".equals(this.receiver) && "literal".equals(this.name))
                    // A command's example line or switch in its manual: what a player types, which is data.
                    || (this.constructor && ("Example".equals(this.name) || "Option".equals(this.name)));
        }

        boolean shows() {
            if (this.name == null) {
                return false;
            }
            if (this.constructor) {
                // A control's label is its first argument; what comes after it is what the control does.
                return LABELLED.contains(this.name) && this.argument == 0;
            }
            if ("literal".equals(this.name)) {
                return "Component".equals(this.receiver);
            }
            // Map.entry builds a table, not a line on a terminal, whatever the two share in name.
            return SINKS.contains(this.name) && !("entry".equals(this.name) && "Map".equals(this.receiver));
        }
    }

    private static Map<String, List<Hit>> hitsByFile() {
        final Map<String, List<Hit>> out = new TreeMap<>();
        final Path root = Path.of("").toAbsolutePath().getParent();
        for (final String module : MODULES) {
            final Path main = root.resolve(module).resolve("src").resolve("main").resolve("java");
            if (!Files.isDirectory(main)) {
                continue;
            }
            try (Stream<Path> walk = Files.walk(main)) {
                for (final Path file : walk.filter(path -> path.toString().endsWith(".java")).sorted().toList()) {
                    final List<Hit> hits = scan(Files.readString(file, StandardCharsets.UTF_8));
                    if (!hits.isEmpty()) {
                        out.put(root.relativize(file).toString().replace('\\', '/'), hits);
                    }
                }
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return out;
    }

    /**
     * The fixed text in one source, read with just enough of Java to know where each literal stands: comments are
     * skipped, and every open call is kept on a stack with its name and what it is called on.
     */
    private static List<Hit> scan(final String source) {
        final List<Hit> hits = new ArrayList<>();
        final Deque<Frame> calls = new ArrayDeque<>();
        final int length = source.length();
        final boolean throwable = THROWABLE_CLASS.matcher(source).find();
        int line = 1;
        String lastWord = null;
        String receiver = null;
        String dotted = null;
        boolean afterNew = false;
        boolean afterAt = false;
        // Whether the last word read is an annotation's name, which only a bracket straight after it opens.
        boolean annotationWord = false;
        boolean wordLast = false;
        int i = 0;
        while (i < length) {
            final char c = source.charAt(i);
            if (c == '\n') {
                line++;
                i++;
                continue;
            }
            if (c == '/' && i + 1 < length && source.charAt(i + 1) == '/') {
                while (i < length && source.charAt(i) != '\n') {
                    i++;
                }
                continue;
            }
            if (c == '/' && i + 1 < length && source.charAt(i + 1) == '*') {
                final int end = source.indexOf("*/", i + 2);
                final int stop = end < 0 ? length : end + 2;
                line += count(source, i, stop);
                i = stop;
                continue;
            }
            if (c == '"') {
                final boolean block = source.startsWith("\"\"\"", i);
                final int start = i + (block ? 3 : 1);
                final int end = block ? source.indexOf("\"\"\"", start) : closing(source, start);
                final int stop = end < 0 ? length : end;
                final String text = source.substring(start, stop);
                // A method that hands back a capitalised word, the way a window's title does, is showing it.
                final boolean returned = wordLast && "return".equals(lastWord) && TITLE.matcher(text).matches()
                        && endsStatement(source, stop + (block ? 3 : 1));
                if (counts(text, calls) || returned && !excluded(calls)) {
                    hits.add(new Hit(line, text.strip()));
                }
                line += count(source, i, stop);
                i = stop + (block ? 3 : 1);
                wordLast = false;
                continue;
            }
            if (c == '\'') {
                i = closingQuote(source, i + 1) + 1;
                wordLast = false;
                continue;
            }
            if (Character.isJavaIdentifierStart(c)) {
                final int start = i;
                while (i < length && Character.isJavaIdentifierPart(source.charAt(i))) {
                    i++;
                }
                final String word = source.substring(start, i);
                if ("new".equals(word)) {
                    afterNew = true;
                    wordLast = false;
                    continue;
                }
                if (LOGGERS.contains(word) && !calls.isEmpty() && handedOver(source, i)) {
                    calls.peek().logged = true;
                }
                receiver = dotted;
                dotted = null;
                lastWord = word;
                annotationWord = afterAt;
                afterAt = false;
                wordLast = true;
                continue;
            }
            if (c == '.') {
                dotted = wordLast ? lastWord : null;
                wordLast = false;
                i++;
                continue;
            }
            if (c == '@') {
                afterAt = true;
                i++;
                continue;
            }
            if (c == '(') {
                // An exception's own message, handed up to the class it extends, is for the log as well.
                final boolean quiet = annotationWord || throwable && "super".equals(lastWord);
                calls.push(wordLast ? new Frame(lastWord, receiver, afterNew, quiet)
                        : new Frame(null, null, false, false));
                afterNew = false;
                annotationWord = false;
                wordLast = false;
                i++;
                continue;
            }
            if (c == ')') {
                if (!calls.isEmpty()) {
                    calls.pop();
                }
            } else if (c == ',' && !calls.isEmpty()) {
                calls.peek().argument++;
            } else if (!Character.isWhitespace(c) && c != '<' && c != '>' && c != ',' && c != '?') {
                // Anything else ends a qualified name and whatever an annotation or a constructor was about to open.
                afterNew = afterNew && (c == '[' || c == ']');
                afterAt = false;
                annotationWord = false;
                dotted = null;
            }
            if (!Character.isWhitespace(c)) {
                wordLast = false;
            }
            i++;
        }
        return hits;
    }

    /** Whether any of those calls keeps what is inside it from being words a player reads. */
    private static boolean excluded(final Deque<Frame> calls) {
        for (final Frame frame : calls) {
            if (frame.excludes()) {
                return true;
            }
        }
        return false;
    }

    /** Whether a literal standing inside those calls is fixed text a player reads. */
    private static boolean counts(final String text, final Deque<Frame> calls) {
        if (!WORD.matcher(text).find()) {
            return false;
        }
        boolean shown = false;
        for (final Frame frame : calls) {
            if (frame.excludes()) {
                return false;
            }
            shown |= frame.shows();
        }
        return shown || PROSE.matcher(text).find() && text.indexOf(' ') >= 0;
    }

    private static Map.Entry<String, Data> data(final String file, final int count, final String reason) {
        return Map.entry(file, new Data(count, reason));
    }

    /** Whether the statement ends at {@code from}: the literal before it is the whole of what is returned. */
    private static boolean endsStatement(final String source, final int from) {
        int i = from;
        while (i < source.length() && Character.isWhitespace(source.charAt(i))) {
            i++;
        }
        return i < source.length() && source.charAt(i) == ';';
    }

    /** Whether the word that ends at {@code end} is handed over as an argument: a comma or a bracket comes next. */
    private static boolean handedOver(final String source, final int end) {
        int i = end;
        while (i < source.length() && Character.isWhitespace(source.charAt(i))) {
            i++;
        }
        return i < source.length() && (source.charAt(i) == ',' || source.charAt(i) == ')');
    }

    private static int closing(final String source, final int from) {
        int i = from;
        while (i < source.length()) {
            final char c = source.charAt(i);
            if (c == '\\') {
                i += 2;
                continue;
            }
            if (c == '"' || c == '\n') {
                return i;
            }
            i++;
        }
        return source.length();
    }

    private static int closingQuote(final String source, final int from) {
        int i = from;
        while (i < source.length() && source.charAt(i) != '\'') {
            i += source.charAt(i) == '\\' ? 2 : 1;
        }
        return Math.min(i, source.length() - 1);
    }

    private static int count(final String source, final int from, final int to) {
        int lines = 0;
        for (int i = from; i < to && i < source.length(); i++) {
            if (source.charAt(i) == '\n') {
                lines++;
            }
        }
        return lines;
    }
}
