/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * What the series promises a mod built on it, and nothing more.
 *
 * <p>Everything reachable from an {@code api} package is the promise; everything else in the series is its
 * own business and changes without a word. The danger is a promise made by accident: an API method that
 * hands back or takes something from the inside drags that thing into the promise, and the next release
 * that changes it breaks a mod without anybody having decided to.
 *
 * <p>So the types an API signature is allowed to name are counted, and they are these: the API's own, the
 * few of ours that are the API even though they live where they belong, the language's, and the game's. A
 * new name in a signature fails here until somebody says which of those it is, which is the point: adding
 * to the promise is a decision, not a side effect.
 *
 * <p>The promise is also kept as a photo, one file per mod in {@code src/test/resources/api}: every type and
 * member a mod can reach, each with the version of the API that brought it in the shape written there. The
 * API changing without its photo fails, and so does the photo moving on without the mod's API version, and so
 * does a part newer than the photo's stable version without its experimental mark, or an older one still
 * carrying it. A failing run writes what the photo would have to say to {@code build/api-photo}, to be read
 * and copied over.
 *
 * <p>The list below is the same one {@code docs/API.md} names, and the two are meant to be read together.
 */
class ApiSurfaceTest {

    /**
     * Ours that are part of the promise though they do not live in an api package.
     *
     * <p>Each is something an addon has to hold to do what the API is for: a registry it adds to, or a
     * description of a thing it is adding.
     */
    private static final Set<String> PROMISED = Set.of(
            // the registries an addon adds to
            "LanguageRegistry", "OperationTypeRegistry",
            // what it adds to them
            "IProgrammingLanguage", "OperationType", "IOperationArgs",
            // the computers: what a machine is, and what can be installed on one
            "IsaSpec", "KernelDef", "OsDef", "ProgramSpec", "DesktopEnvironmentDef", "OperatingSpaceDef",
            // the engines a Mainframe can run to plan its network's work, and what they may offer
            "EngineDef", "EngineCapability",
            // the former name of IsaSpec, deprecated for one cycle before it goes
            "ArchitectureSpec");

    /** What java.lang brings in, which a signature may name with no import at all. */
    private static final Set<String> JAVA_LANG = Set.of("String", "Object", "Integer", "Long", "Double", "Float",
            "Boolean", "Character", "Number", "CharSequence", "Iterable", "Comparable", "Class", "Enum", "Record",
            "Runnable", "Exception", "RuntimeException", "Throwable", "Void");

    /** Whose names an API signature may always say: the language's own and the game's. */
    private static final List<String> ELSEWHERE = List.of("java.", "javax.", "net.minecraft.", "net.neoforged.",
            "com.mojang.", "org.slf4j.", "org.jetbrains.");

    /** A member anything outside the mod can reach, with what it gives back and what it takes. */
    private static final Pattern PUBLIC_MEMBER = Pattern.compile("^\\s{4}public\\s+"
            + "(?:static\\s+|final\\s+|abstract\\s+)*([A-Za-z0-9_.<>\\[\\], ?]+?)\\s+(\\w+)\\s*\\(([^)]*)\\)");

    /** A public field or constant, which is reachable the same way a method is. */
    private static final Pattern PUBLIC_FIELD = Pattern.compile("^\\s{4}public\\s+"
            + "(?:static\\s+)?(?:final\\s+)?([A-Za-z0-9_.<>\\[\\], ?]+?)\\s+(\\w+)\\s*[=;]");

    /** Where the photos are kept, relative to the Core's folder: one file per mod, named after the mod's folder. */
    private static final Path PHOTOS = Path.of("src", "test", "resources", "api");

    /** Where a failing run writes what a photo would have to say, for a person to read and copy over. */
    private static final Path PROPOSALS = Path.of("build", "api-photo");

    /** A line of a photo: the version, then the type the line is about, then what it says of it. */
    private static final Pattern PHOTO_LINE = Pattern.compile("^(\\d+) (\\S+)  (.+)$");

    /** The photo's line naming the newest version whose parts are no longer marked experimental. */
    private static final Pattern STABLE_LINE = Pattern.compile("^stable (\\d+)$");

    /** How a mod's API class says the number of its shape. */
    private static final Pattern VERSION = Pattern.compile("\\bint\\s+VERSION\\s*=\\s*(\\d+)\\s*;");

    @Test
    void api_namesNothingThatIsNotPartOfThePromise() {
        final List<Path> sources = apiSources();
        final Set<String> api = new LinkedHashSet<>();
        for (final Path file : sources) {
            api.add(file.getFileName().toString().replace(".java", ""));
        }
        final List<String> leaked = new ArrayList<>();
        for (final Path file : sources) {
            leaked.addAll(leakedIn(file, api));
        }

        assertEquals(List.of(), leaked, () -> String.join("\n", leaked));
    }

    @Test
    void api_isWhereItIsSaidToBe() {
        final List<Path> found = apiSources();

        assertTrue(found.stream().anyMatch(path -> path.endsWith("JsCoreApi.java")), () -> "found " + found);
        assertTrue(found.stream().anyMatch(path -> path.endsWith("CoreRegisterEvent.java")));
        assertTrue(found.stream().anyMatch(path -> path.endsWith("JsComputersApi.java")));
        assertTrue(found.stream().anyMatch(path -> path.endsWith("ComputersRegisterEvent.java")));
    }

    @Test
    void api_isWhatItsPhotoSays() {
        final List<String> wrong = new ArrayList<>();
        for (final Map.Entry<String, List<Promise>> mod : promisesByMod().entrySet()) {
            final Photo photo = photoOf(mod.getKey());
            final Set<String> now = new LinkedHashSet<>();
            for (final Promise promise : mod.getValue()) {
                now.add(promise.key());
            }
            final List<String> added = now.stream().filter(key -> !photo.versions().containsKey(key)).toList();
            final List<String> gone = photo.versions().keySet().stream().filter(key -> !now.contains(key)).toList();
            if (added.isEmpty() && gone.isEmpty()) {
                continue;
            }
            final Path proposal = propose(mod.getKey(), photo, mod.getValue());
            wrong.add(mod.getKey() + ": the API is not what its photo says.\n  new: " + String.join("\n       ", added)
                    + "\n  gone: " + String.join("\n        ", gone)
                    + "\n  What the photo would have to say is in " + proposal.toAbsolutePath() + ". Something new"
                    + " raises the mod's API VERSION to the number the proposal writes against it; something gone"
                    + " is a change a mod may break on, and the changelog's API section says so.");
        }

        assertEquals(List.of(), wrong, () -> String.join("\n", wrong));
    }

    @Test
    void api_versionIsTheNewestNumberInItsPhoto() {
        for (final String mod : promisesByMod().keySet()) {
            final Photo photo = photoOf(mod);

            assertEquals(photo.newest(), versionOf(mod), () -> mod + ": the API VERSION is not the newest number"
                    + " in the photo, so something was added without raising it, or it was raised with nothing added");
        }
    }

    @Test
    void api_marksExperimentalExactlyWhatIsNewerThanTheStableVersion() {
        final List<String> wrong = new ArrayList<>();
        for (final Map.Entry<String, List<Promise>> mod : promisesByMod().entrySet()) {
            final Photo photo = photoOf(mod.getKey());
            final Map<String, Promise> types = new HashMap<>();
            for (final Promise promise : mod.getValue()) {
                if (promise.isType()) {
                    types.put(promise.owner(), promise);
                }
            }
            for (final Promise promise : mod.getValue()) {
                final Integer version = photo.versions().get(promise.key());
                if (version == null) {
                    // Not in the photo yet: the photo test says so, and what it is new at is not known until then.
                    continue;
                }
                final boolean expected = newerThanStable(promise, photo, types);
                if (expected && !promise.experimental()) {
                    wrong.add(promise.key() + " came in version " + version + ", after the stable " + photo.stable()
                            + ", and has to carry @ApiStatus.Experimental (on itself or on a type it is in)");
                } else if (!expected && promise.experimental()) {
                    wrong.add(promise.key() + " is as old as the stable version " + photo.stable()
                            + " and has been through a release: its @ApiStatus.Experimental mark comes off");
                }
            }
        }

        assertEquals(List.of(), wrong, () -> String.join("\n", wrong));
    }

    private static List<String> leakedIn(final Path file, final Set<String> api) {
        final String text = read(file);
        final List<String> leaked = new ArrayList<>();
        for (final String line : text.split("\n")) {
            final Matcher method = PUBLIC_MEMBER.matcher(line);
            final Matcher field = PUBLIC_FIELD.matcher(line);
            final Set<String> named = new LinkedHashSet<>();
            if (method.find()) {
                named.addAll(typesIn(method.group(1)));
                for (final String parameter : method.group(3).split(",")) {
                    named.addAll(typesIn(parameter.replace("final ", "").trim()));
                }
            } else if (field.find()) {
                named.addAll(typesIn(field.group(1)));
            } else {
                continue;
            }
            for (final String type : named) {
                if (!allowed(type, text, api)) {
                    leaked.add(file.getFileName() + ": " + type + " is named by the API but is not part of it");
                }
            }
        }
        return leaked;
    }

    /** The type names written in a piece of a signature, with the generics and the arrays taken apart. */
    private static Set<String> typesIn(final String written) {
        final Set<String> found = new LinkedHashSet<>();
        final Matcher names = Pattern.compile("\\b([A-Z][A-Za-z0-9_]*)\\b").matcher(written);
        while (names.find()) {
            found.add(names.group(1));
        }
        return found;
    }

    /**
     * Whether an API signature may name that type.
     *
     * <p>What decides it is where THAT type comes from, not what else the file happens to import. Getting
     * that wrong is how this test came to pass a deliberate leak the first time it was tried.
     */
    private static boolean allowed(final String type, final String text, final Set<String> api) {
        if (PROMISED.contains(type) || api.contains(type) || JAVA_LANG.contains(type)) {
            return true;
        }
        final String imported = importOf(type, text);
        if (imported != null) {
            for (final String outside : ELSEWHERE) {
                if (imported.startsWith(outside)) {
                    return true;
                }
            }
            // Imported from inside the series and not spoken for: a promise nobody decided to make.
            return false;
        }
        // Named without an import: something this very file declares, or something java.lang brings in.
        return text.contains("class " + type) || text.contains("interface " + type)
                || text.contains("record " + type) || text.contains("enum " + type);
    }

    /** The whole name the file imports that type as, or null when it imports no such type. */
    private static String importOf(final String type, final String text) {
        for (final String line : text.split("\n")) {
            final Matcher match = Pattern.compile("^\\s*import\\s+([A-Za-z0-9_.]+\\." + type + ");").matcher(line);
            if (match.find()) {
                return match.group(1);
            }
        }
        return null;
    }

    private static String read(final Path file) {
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Every source in an api package of the series, or in one nested under it. The client half is a package of
     * its own because a dedicated server must not load a screen, not because it is any less of a promise: an
     * addon registering an operating space calls it, so it is watched the same way.
     */
    private static List<Path> apiSources() {
        final List<Path> found = new ArrayList<>();
        for (final Path main : mainSourceRoots()) {
            found.addAll(javaUnder(main).stream().filter(ApiSurfaceTest::insideApi).toList());
        }
        return found;
    }

    /** The source folder of every mod of the series, in the order the folders sort. */
    private static List<Path> mainSourceRoots() {
        final Path root = Path.of("").toAbsolutePath().getParent();
        try (Stream<Path> modules = Files.list(root)) {
            return modules.sorted().map(module -> module.resolve("src").resolve("main").resolve("java"))
                    .filter(Files::isDirectory).toList();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static List<Path> javaUnder(final Path main) {
        try (Stream<Path> walk = Files.walk(main)) {
            return walk.filter(path -> path.toString().endsWith(".java")).sorted().toList();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Whether a source file sits in an {@code api} package or in one nested under it. */
    private static boolean insideApi(final Path file) {
        for (Path at = file.getParent(); at != null; at = at.getParent()) {
            final String name = at.getFileName() == null ? "" : at.getFileName().toString();
            if (name.equals("api")) {
                return true;
            }
            if (name.equals("java")) {
                return false;
            }
        }
        return false;
    }

    /** The mod a source file belongs to: the name of the folder its source tree sits in. */
    private static String modOf(final Path main) {
        return main.getParent().getParent().getParent().getFileName().toString();
    }

    /**
     * Every promise of every mod that makes one: what its api packages hold and what its promised types are,
     * keyed by the mod's folder.
     */
    private static Map<String, List<Promise>> promisesByMod() {
        final Map<String, List<Promise>> byMod = new TreeMap<>();
        final Map<String, Integer> promisedFound = new HashMap<>();
        for (final Path main : mainSourceRoots()) {
            for (final Path file : javaUnder(main)) {
                final String type = file.getFileName().toString().replace(".java", "");
                final boolean promised = PROMISED.contains(type);
                if (promised) {
                    promisedFound.merge(type, 1, Integer::sum);
                }
                if (promised || insideApi(file)) {
                    byMod.computeIfAbsent(modOf(main), mod -> new ArrayList<>())
                            .addAll(new SurfaceReader(read(file)).read());
                }
            }
        }
        for (final String type : PROMISED) {
            assertEquals(1, promisedFound.getOrDefault(type, 0),
                    () -> "the promised type " + type + " has to be found exactly once in the series' sources");
        }
        return byMod;
    }

    /**
     * Whether a promise is newer than its photo's stable version: itself, or through a type it sits in, since
     * everything in a type that came later came with it.
     */
    private static boolean newerThanStable(final Promise promise, final Photo photo, final Map<String, Promise> types) {
        if (newer(promise, photo)) {
            return true;
        }
        // The types around it, from the nearest out; the dots past the outermost one are its package's.
        String around = promise.isType() ? outerOf(promise.owner()) : promise.owner();
        while (around != null) {
            final Promise type = types.get(around);
            if (type != null && newer(type, photo)) {
                return true;
            }
            around = outerOf(around);
        }
        return false;
    }

    private static boolean newer(final Promise promise, final Photo photo) {
        final Integer version = photo.versions().get(promise.key());
        return version != null && version > photo.stable();
    }

    /** The name with its last part taken off, or null when there is nothing left to take. */
    private static String outerOf(final String name) {
        final int dot = name.lastIndexOf('.');
        return dot < 0 ? null : name.substring(0, dot);
    }

    /** The mod's API version, as the number its API class says. */
    private static int versionOf(final String mod) {
        for (final Path main : mainSourceRoots()) {
            if (!modOf(main).equals(mod)) {
                continue;
            }
            for (final Path file : apiSources()) {
                if (file.startsWith(main) && file.getFileName().toString().matches("Js\\w+Api\\.java")) {
                    final Matcher version = VERSION.matcher(read(file));
                    assertTrue(version.find(), () -> file + " says no VERSION");
                    return Integer.parseInt(version.group(1));
                }
            }
        }
        throw new AssertionError(mod + " makes promises but has no Js*Api class saying their version");
    }

    /** The photo kept for a mod, or an empty one when it has none yet. */
    private static Photo photoOf(final String mod) {
        final Path file = PHOTOS.resolve(mod + ".txt");
        final Map<String, Integer> versions = new LinkedHashMap<>();
        int stable = 0;
        if (Files.isRegularFile(file)) {
            for (final String line : read(file).split("\n")) {
                final String trimmed = line.strip();
                final Matcher entry = PHOTO_LINE.matcher(trimmed);
                final Matcher stableLine = STABLE_LINE.matcher(trimmed);
                if (entry.matches()) {
                    versions.put(entry.group(2) + "  " + entry.group(3), Integer.parseInt(entry.group(1)));
                } else if (stableLine.matches()) {
                    stable = Integer.parseInt(stableLine.group(1));
                }
            }
        }
        return new Photo(stable, versions);
    }

    /**
     * Writes what the photo would have to say for the API as it is: what it already said kept at its version,
     * a line that only became deprecated kept at the version it had, and what is new at the next number.
     */
    private static Path propose(final String mod, final Photo photo, final List<Promise> promises) {
        final int next = photo.newest() + 1;
        final Map<String, Integer> undeprecated = new HashMap<>();
        for (final Map.Entry<String, Integer> kept : photo.versions().entrySet()) {
            undeprecated.put(undeprecated(kept.getKey()), kept.getValue());
        }
        final List<Promise> sorted = new ArrayList<>(promises);
        sorted.sort(Comparator.comparing(Promise::owner).thenComparing(promise -> !promise.isType())
                .thenComparing(Promise::what));
        final List<String> lines = new ArrayList<>();
        for (final Promise promise : sorted) {
            Integer version = photo.versions().get(promise.key());
            if (version == null) {
                version = undeprecated.get(undeprecated(promise.key()));
            }
            lines.add((version == null ? next : version) + " " + promise.key());
        }
        final Path file = PROPOSALS.resolve(mod + ".txt");
        try {
            Files.createDirectories(PROPOSALS);
            Files.writeString(file, photoText(mod, photo.stable(), lines), StandardCharsets.UTF_8);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
        return file;
    }

    /** A photo's key with its deprecation taken off, which is how a line that only became deprecated is known. */
    private static String undeprecated(final String key) {
        return key.replaceFirst("  (" + Promise.TYPE + ")?" + Promise.DEPRECATED, "  $1");
    }

    /** A photo written out: what it is and its stable version, then its lines, each type before its members. */
    private static String photoText(final String mod, final int stable, final List<String> lines) {
        final StringBuilder out = new StringBuilder();
        out.append("# The public API of the mod in the folder '").append(mod).append("': every type and member a mod\n")
                .append("# built on it can reach. The number before a line is the version of the API that brought\n")
                .append("# it in the shape written there. Whatever came after the stable version carries\n")
                .append("# @ApiStatus.Experimental until the cycle after its first release. Kept by ApiSurfaceTest,\n")
                .append("# which writes to build/api-photo what this file would have to say when the API changes.\n")
                .append("stable ").append(stable).append('\n');
        for (final String line : lines) {
            out.append(line).append('\n');
        }
        return out.toString();
    }

    /** A mod's photo: the newest version already through a release, and the version of every line. */
    private record Photo(int stable, Map<String, Integer> versions) {

        int newest() {
            int newest = 0;
            for (final int version : this.versions.values()) {
                newest = Math.max(newest, version);
            }
            return newest;
        }
    }

    /**
     * One thing a mod promises: a type, or a member of one, in the words the photo writes it with.
     *
     * @param owner        the type's whole name, nested types joined with dots
     * @param what         {@code type ...} for the type itself, or the member as a caller sees it
     * @param experimental whether it, or a type it sits in, carries {@code @ApiStatus.Experimental}
     */
    private record Promise(String owner, String what, boolean experimental) {

        /** How the line declaring a type starts, so it sorts before the type's members and is told from them. */
        static final String TYPE = "type ";
        /** How a member or a type marked as going starts, after the word for a type if it is one. */
        static final String DEPRECATED = "deprecated ";

        String key() {
            return this.owner + "  " + this.what;
        }

        boolean isType() {
            return this.what.startsWith(TYPE);
        }
    }

    /**
     * Reads a source file into the promises it makes: its public types and what anything outside can reach of
     * them, one line each in the photo's words.
     *
     * <p>It reads source rather than classes because a Core test sees every mod's sources but only the Core's
     * classes, and the game's types an API names are on no test's classpath at all. It knows as much Java as an
     * API is written in: types nested in types, records, enums, interfaces whose members are public without
     * saying so, annotations, generics, and the bodies and initializers it only has to step over.
     */
    private static final class SurfaceReader {

        private final String code;
        private final String pkg;
        private final List<Promise> found = new ArrayList<>();
        private int at;

        private static final Set<String> MODIFIERS = Set.of("public", "protected", "private", "static", "final",
                "abstract", "default", "sealed", "non-sealed", "synchronized", "native", "strictfp", "transient",
                "volatile");
        /** The modifiers that change what a caller or an implementer may rely on, in the order they are written. */
        private static final List<String> KEPT = List.of("protected", "abstract", "default", "static", "final",
                "sealed", "non-sealed");
        private static final Pattern TYPE_HEAD =
                Pattern.compile("^(class|interface|enum|record|@interface)\\s+(\\w+)(.*)$", Pattern.DOTALL);
        private static final Pattern ANNOTATION = Pattern.compile("@[\\w.]+(\\s*\\([^)]*\\))?\\s*");
        private static final Pattern MODIFIER = Pattern.compile("^(non-sealed|[a-z]+)\\b");
        private static final Pattern PACKAGE = Pattern.compile("\\bpackage\\s+([\\w.]+)\\s*;");
        private static final String FILE = "file";

        SurfaceReader(final String source) {
            this.code = withoutCommentsOrLiterals(source);
            final Matcher pkgMatch = PACKAGE.matcher(this.code);
            this.pkg = pkgMatch.find() ? pkgMatch.group(1) : "";
        }

        List<Promise> read() {
            body(new Scope(this.pkg, FILE, false, false, false));
            return this.found;
        }

        /** The source with comments gone and every text and character literal emptied, so no brace in them counts. */
        private static String withoutCommentsOrLiterals(final String source) {
            final StringBuilder out = new StringBuilder(source.length());
            int i = 0;
            while (i < source.length()) {
                final char c = source.charAt(i);
                final char next = i + 1 < source.length() ? source.charAt(i + 1) : '\0';
                if (c == '/' && next == '/') {
                    while (i < source.length() && source.charAt(i) != '\n') {
                        i++;
                    }
                } else if (c == '/' && next == '*') {
                    final int end = source.indexOf("*/", i + 2);
                    i = end < 0 ? source.length() : end + 2;
                    out.append(' ');
                } else if (source.startsWith("\"\"\"", i)) {
                    final int end = source.indexOf("\"\"\"", i + 3);
                    i = end < 0 ? source.length() : end + 3;
                    out.append("\"\"");
                } else if (c == '"' || c == '\'') {
                    i = closingQuote(source, i);
                    out.append(c).append(c);
                } else {
                    out.append(c);
                    i++;
                }
            }
            return out.toString();
        }

        /** Where the literal opened by the quote at {@code open} ends, past its closing quote. */
        private static int closingQuote(final String source, final int open) {
            final char quote = source.charAt(open);
            int i = open + 1;
            while (i < source.length()) {
                final char c = source.charAt(i);
                if (c == '\\') {
                    i += 2;
                } else if (c == quote) {
                    return i + 1;
                } else {
                    i++;
                }
            }
            return i;
        }

        /** Reads the members of a type (or of the file) up to its closing brace, which it steps past. */
        private void body(final Scope owner) {
            if (owner.kind().equals("enum")) {
                constants(owner);
            }
            while (true) {
                skipSpace();
                if (this.at >= this.code.length()) {
                    return;
                }
                if (this.code.charAt(this.at) == '}') {
                    this.at++;
                    return;
                }
                final Segment segment = segment();
                if (segment.end() == '}') {
                    this.at++;
                    return;
                }
                if (segment.end() == ';') {
                    member(owner, segment.head(), false);
                    continue;
                }
                // A block: a type of its own, or the body of a method, a constructor or an initializer.
                final Head head = Head.of(segment.head());
                final Matcher type = TYPE_HEAD.matcher(head.rest());
                if (type.matches()) {
                    this.at++;
                    body(type(owner, head, type));
                } else {
                    member(owner, segment.head(), true);
                    skipBlock();
                }
            }
        }

        /** The constants that open an enum's body, up to the semicolon after them or the enum's end. */
        private void constants(final Scope owner) {
            final StringBuilder all = new StringBuilder();
            int depth = 0;
            while (this.at < this.code.length()) {
                final char c = this.code.charAt(this.at);
                if (depth == 0 && (c == ';' || c == '}')) {
                    if (c == ';') {
                        this.at++;
                    }
                    break;
                }
                if (depth == 0 && c == '{') {
                    // A constant with a body of its own, which says nothing about what the constant is called.
                    skipBlock();
                    continue;
                }
                if (c == '(') {
                    depth++;
                } else if (c == ')') {
                    depth--;
                }
                all.append(c);
                this.at++;
            }
            for (final String constant : topLevelParts(all.toString())) {
                final Head head = Head.of(constant);
                final String name = head.rest().replaceAll("\\s*\\(.*$", "").strip();
                if (!name.isEmpty() && owner.visible()) {
                    add(owner, head, name);
                }
            }
        }

        /**
         * Reads up to the end of one member's head: a semicolon, the brace opening its body, or the brace closing
         * the type. A brace inside parentheses (an annotation's array, a lambda handed to a call) is part of the
         * head, and so is one after an equals sign, which opens an initializer the reader steps over.
         */
        private Segment segment() {
            final StringBuilder head = new StringBuilder();
            int depth = 0;
            boolean initializer = false;
            while (this.at < this.code.length()) {
                final char c = this.code.charAt(this.at);
                if (depth == 0) {
                    if (c == ';') {
                        this.at++;
                        return new Segment(head.toString(), ';');
                    }
                    if (c == '}') {
                        return new Segment(head.toString(), '}');
                    }
                    if (c == '{') {
                        if (!initializer) {
                            return new Segment(head.toString(), '{');
                        }
                        skipBlock();
                        head.append("{}");
                        continue;
                    }
                    if (c == '=') {
                        initializer = true;
                    }
                }
                if (c == '(') {
                    depth++;
                } else if (c == ')') {
                    depth--;
                }
                head.append(c);
                this.at++;
            }
            return new Segment(head.toString(), ';');
        }

        /** Steps over the block whose opening brace is under the reader, braces inside it and all. */
        private void skipBlock() {
            int depth = 0;
            while (this.at < this.code.length()) {
                final char c = this.code.charAt(this.at++);
                if (c == '{') {
                    depth++;
                } else if (c == '}' && --depth == 0) {
                    return;
                }
            }
        }

        private void skipSpace() {
            while (this.at < this.code.length() && Character.isWhitespace(this.code.charAt(this.at))) {
                this.at++;
            }
        }

        /** A type declared at the reader, written down when it is part of the promise, and the scope of its body. */
        private Scope type(final Scope owner, final Head head, final Matcher type) {
            final String kind = type.group(1);
            final String name = owner.name().isEmpty() ? type.group(2) : owner.name() + "." + type.group(2);
            final boolean visible = owner.kind().equals(FILE) ? head.has("public") : owner.visible()
                    && visible(owner, head);
            final boolean experimental = owner.experimental() || head.experimental();
            if (visible) {
                final String tail = normal(type.group(3));
                this.found.add(new Promise(name, Promise.TYPE + (head.deprecated() ? Promise.DEPRECATED : "")
                        + kept(head, "") + kind + (tail.isEmpty() || tail.startsWith("(") || tail.startsWith("<")
                        ? tail : " " + tail), experimental));
            }
            return new Scope(name, kind, visible, experimental, !kind.equals("class") || head.has("final"));
        }

        /** A member at the reader: a method, a constructor or a field, written down when it is part of the promise. */
        private void member(final Scope owner, final String text, final boolean hasBody) {
            if (owner.kind().equals(FILE) || !owner.visible()) {
                return;
            }
            final Head head = Head.of(text);
            final String rest = head.rest();
            if (rest.isEmpty() || !visible(owner, head)) {
                return;
            }
            final boolean inInterface = owner.kind().equals("interface") || owner.kind().equals("@interface");
            final int open = topLevel(rest, '(');
            final int equals = topLevel(rest, '=');
            if (open >= 0 && (equals < 0 || open < equals)) {
                final String before = normal(rest.substring(0, open));
                final String parameters = parameterTypes(rest.substring(open + 1, closing(rest, open)));
                final String simple = owner.name().substring(owner.name().lastIndexOf('.') + 1);
                if (before.equals(simple)) {
                    add(owner, head, "new(" + parameters + ")");
                    return;
                }
                final boolean implicitAbstract = inInterface && !hasBody && !head.has("static")
                        && !head.has("default") && !head.has("private");
                add(owner, head, kept(head, implicitAbstract ? "abstract" : "") + before + "(" + parameters + ")");
            } else if (!hasBody) {
                final String declared = normal(equals < 0 ? rest : rest.substring(0, equals));
                final List<String> declarators = topLevelParts(declared);
                final String first = declarators.get(0);
                final int space = first.lastIndexOf(' ');
                if (space < 0) {
                    return;
                }
                final String type = first.substring(0, space);
                final String mods = kept(head, inInterface && !head.has("static") ? "static final" : "");
                add(owner, head, mods + first);
                for (int i = 1; i < declarators.size(); i++) {
                    add(owner, head, mods + type + " " + declarators.get(i).strip());
                }
            }
            // A body with no parentheses before it is a record's compact constructor or an initializer: no promise.
        }

        private void add(final Scope owner, final Head head, final String what) {
            this.found.add(new Promise(owner.name(), (head.deprecated() ? Promise.DEPRECATED : "") + what,
                    owner.experimental() || head.experimental()));
        }

        /** Whether something declared in {@code owner} with that head can be reached from outside the mod. */
        private static boolean visible(final Scope owner, final Head head) {
            if (owner.kind().equals("interface") || owner.kind().equals("@interface")) {
                return !head.has("private");
            }
            return head.has("public") || head.has("protected") && !owner.finalType();
        }

        /** The modifiers a promise keeps, written before what it says, with one it has without writing it. */
        private static String kept(final Head head, final String implied) {
            final StringBuilder out = new StringBuilder();
            for (final String modifier : KEPT) {
                if (head.has(modifier) || (" " + implied + " ").contains(" " + modifier + " ")) {
                    out.append(modifier).append(' ');
                }
            }
            return out.toString();
        }

        /** The types of a parameter list, without the names, which a caller never writes. */
        private static String parameterTypes(final String parameters) {
            final List<String> types = new ArrayList<>();
            for (final String parameter : topLevelParts(parameters)) {
                final String written = normal(parameter);
                if (written.isEmpty()) {
                    continue;
                }
                final int space = written.lastIndexOf(' ');
                types.add(space < 0 ? written : written.substring(0, space));
            }
            return String.join(", ", types);
        }

        /** A piece of a signature as the photo writes it: no annotations, no {@code final}, spaces in one form. */
        private static String normal(final String written) {
            return ANNOTATION.matcher(written).replaceAll("")
                    .replaceAll("\\bfinal\\s+", "")
                    .replaceAll("\\s+", " ")
                    .replaceAll("\\s*<\\s*", "<")
                    .replaceAll("\\s+>", ">")
                    .replaceAll("\\s*,\\s*", ", ")
                    .replaceAll("\\s*\\(\\s*", "(")
                    .replaceAll("\\s*\\)", ")")
                    .replaceAll("\\s*\\[\\s*]", "[]")
                    .strip();
        }

        /** The parts of a list at its own level, split on the commas not inside angle brackets or parentheses. */
        private static List<String> topLevelParts(final String list) {
            final List<String> parts = new ArrayList<>();
            int depth = 0;
            int start = 0;
            for (int i = 0; i < list.length(); i++) {
                final char c = list.charAt(i);
                if (c == '<' || c == '(') {
                    depth++;
                } else if (c == '>' || c == ')') {
                    depth--;
                } else if (c == ',' && depth == 0) {
                    parts.add(list.substring(start, i));
                    start = i + 1;
                }
            }
            parts.add(list.substring(start));
            return parts.stream().map(String::strip).filter(part -> !part.isEmpty()).toList();
        }

        /** Where that character first stands outside angle brackets and parentheses, or -1. */
        private static int topLevel(final String text, final char wanted) {
            int angles = 0;
            int parens = 0;
            for (int i = 0; i < text.length(); i++) {
                final char c = text.charAt(i);
                if (c == wanted && angles == 0 && parens == 0) {
                    return i;
                }
                if (c == '<') {
                    angles++;
                } else if (c == '>') {
                    angles--;
                } else if (c == '(') {
                    parens++;
                } else if (c == ')') {
                    parens--;
                }
            }
            return -1;
        }

        /** Where the parenthesis opened at {@code open} closes. */
        private static int closing(final String text, final int open) {
            int depth = 0;
            for (int i = open; i < text.length(); i++) {
                if (text.charAt(i) == '(') {
                    depth++;
                } else if (text.charAt(i) == ')' && --depth == 0) {
                    return i;
                }
            }
            return text.length();
        }

        /** What the reader has open: a type's body, or the file around the top-level types. */
        private record Scope(String name, String kind, boolean visible, boolean experimental, boolean finalType) {
        }

        /** A member's head read up to its end, with what ended it. */
        private record Segment(String head, char end) {
        }

        /** A head taken apart: its modifiers, the marks the photo cares about, and what is declared. */
        private record Head(Set<String> modifiers, boolean deprecated, boolean experimental, String rest) {

            static Head of(final String text) {
                String rest = text.strip();
                final Set<String> modifiers = new LinkedHashSet<>();
                boolean deprecated = false;
                boolean experimental = false;
                while (!rest.isEmpty()) {
                    if (rest.startsWith("@") && !rest.startsWith("@interface")) {
                        int end = 1;
                        while (end < rest.length() && (Character.isJavaIdentifierPart(rest.charAt(end))
                                || rest.charAt(end) == '.')) {
                            end++;
                        }
                        final String name = rest.substring(1, end);
                        int after = end;
                        while (after < rest.length() && Character.isWhitespace(rest.charAt(after))) {
                            after++;
                        }
                        final int next = after < rest.length() && rest.charAt(after) == '('
                                ? closing(rest, after) + 1 : end;
                        deprecated |= name.equals("Deprecated");
                        experimental |= name.equals("ApiStatus.Experimental") || name.equals("Experimental");
                        rest = rest.substring(Math.min(next, rest.length())).strip();
                        continue;
                    }
                    final Matcher word = MODIFIER.matcher(rest);
                    if (word.find() && MODIFIERS.contains(word.group(1))) {
                        modifiers.add(word.group(1));
                        rest = rest.substring(word.end()).strip();
                        continue;
                    }
                    break;
                }
                return new Head(modifiers, deprecated, experimental, rest.replaceAll("\\s+", " "));
            }

            boolean has(final String modifier) {
                return this.modifiers.contains(modifier);
            }
        }
    }
}
