/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core Test Kit.
 */
package dev.jstech.testkit.check;

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
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * What a mod promises those who build on it, and nothing more: the test of its API's surface, as J's Core keeps its
 * own, offered to every mod. Pure: it reads sources, so it runs in a unit test.
 *
 * <p>The promise is what sits in an {@code api} package, in a package the mod's API class lists as promised
 * ({@code PROMISED_PACKAGES = List.of(...)}), or in one of the few promised types that live where they belong. The
 * danger is a promise made by accident: a promised method that hands back or takes something from the inside drags
 * that thing into the promise, and the next release that changes it breaks a mod without anybody having decided to.
 * So every name a promised public or protected face says is followed to where it comes from, and it may only be the
 * promise's own, the language's or the game's.
 *
 * <p>The promise is kept as a photo, one file a mod: every type and member that can be reached, each with the version
 * of the API that brought it in the shape written there. The API changing without its photo is a problem, and so is
 * the photo moving on without the mod's API {@code VERSION}, and a part newer than the photo's stable version without
 * its experimental mark, or an older one still carrying it. A failing check writes what the photo would have to say
 * to the folder of proposals, to be read and copied over.
 *
 * <pre>{@code
 * class MyModApiTest {
 *     private static final ApiSurface SURFACE = ApiSurface.builder()
 *             .mod("mymod", Path.of("src", "main", "java"))
 *             .photos(Path.of("src", "test", "resources", "api"))
 *             .build();
 *
 *     @Test
 *     void api_keepsItsPromise() {
 *         assertEquals(List.of(), SURFACE.problems());
 *     }
 * }
 * }</pre>
 */
public final class ApiSurface {

    private final List<Root> roots;
    private final Set<String> promised;
    private final Set<String> knownLeaks;
    private final List<String> elsewhere;
    private final Pattern apiClass;
    private final Path photos;
    private final Path proposals;

    /** Whose names a signature may always say: the language's own, and the game's with its own libraries. */
    public static final List<String> GAME = List.of("java.", "javax.", "net.minecraft.", "net.neoforged.",
            "com.mojang.", "org.slf4j.", "org.jetbrains.", "io.netty.");

    private static final Pattern PROMISED_PACKAGES =
            Pattern.compile("\\bPROMISED_PACKAGES\\s*=\\s*List\\.of\\(([^)]*)\\)");
    private static final Pattern QUOTED = Pattern.compile("\"([\\w.]+)\"");
    private static final Pattern VERSION = Pattern.compile("\\bint\\s+VERSION\\s*=\\s*(\\d+)\\s*;");
    private static final String JAVA = ".java";

    private ApiSurface(final Builder from) {
        this.roots = List.copyOf(from.roots);
        this.promised = Set.copyOf(from.promised);
        this.knownLeaks = Set.copyOf(from.knownLeaks);
        this.elsewhere = List.copyOf(from.elsewhere);
        this.apiClass = from.apiClass;
        this.photos = Objects.requireNonNull(from.photos, "a surface keeps its photos somewhere");
        this.proposals = from.proposals;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Runs every check over one mod, for a build, and ends with a code that fails it when any says something:
     * {@code ApiSurface <mod> <its java sources> <photos folder> <proposals folder> [<API class's file pattern>]}.
     */
    public static void main(final String[] args) {
        if (args.length < 4) {
            System.err.println("ApiSurface <mod> <its java sources> <photos folder> <proposals folder>"
                    + " [<API class's file pattern>]");
            System.exit(2);
        }
        final Builder builder = builder().mod(args[0], Path.of(args[1])).photos(Path.of(args[2]))
                .proposals(Path.of(args[3]));
        if (args.length > 4) {
            builder.apiClass(args[4]);
        }
        final List<String> problems = builder.build().problems();
        if (problems.isEmpty()) {
            System.out.println("The API of " + args[0] + " keeps its promise.");
            return;
        }
        System.err.println("The API of " + args[0] + " does not keep its promise:");
        problems.forEach(problem -> System.err.println("  " + problem));
        System.exit(1);
    }

    /**
     * Every source in an api package, or in one nested under it. A client half is a package of its own because a
     * dedicated server must not load a screen, not because it is any less of a promise.
     */
    public List<Path> apiSources() {
        final List<Path> found = new ArrayList<>();
        for (final Root root : roots) {
            found.addAll(javaUnder(root.main()).stream().filter(ApiSurface::insideApi).toList());
        }
        return found;
    }

    /**
     * Each name a promised face says that is the mod's own and promised by nobody, as "a > b"; each one written down
     * as a known debt that is no longer reached; and each promised type not found exactly once.
     */
    public List<String> leaks() {
        final List<String> wrong = new ArrayList<>();
        final Set<String> promisedTypes = new LinkedHashSet<>();
        final Map<Path, List<ApiPromise>> byFile = new LinkedHashMap<>();
        for (final Path file : promisedSources(wrong)) {
            final List<ApiPromise> promises = new ApiSourceReader(read(file)).read();
            byFile.put(file, promises);
            for (final ApiPromise promise : promises) {
                if (promise.isType()) {
                    promisedTypes.add(promise.owner());
                }
            }
        }
        final Set<String> leaks = new LinkedHashSet<>();
        for (final Map.Entry<Path, List<ApiPromise>> file : byFile.entrySet()) {
            leaks.addAll(new ApiReach(read(file.getKey()), file.getValue(), promisedTypes, elsewhere).leaks());
        }
        for (final String leak : leaks) {
            if (!knownLeaks.contains(leak)) {
                wrong.add(leak + ": not promised, so promise it too or keep it out of the public face");
            }
        }
        for (final String leak : knownLeaks) {
            if (!leaks.contains(leak)) {
                wrong.add(leak + ": no longer reached, so it comes off the known leaks");
            }
        }
        return wrong;
    }

    /** Each promised package that is not the mod's own beside its api ones, holds no type, or is not explained. */
    public List<String> packageProblems() {
        final List<String> wrong = new ArrayList<>();
        for (final Root root : roots) {
            final Path api = apiClassOf(root.main());
            final String own = api == null ? "" : packageOf(root.main(), api.getParent()).replaceFirst("\\.api$", "");
            final Set<String> seen = new LinkedHashSet<>();
            for (final String pkg : promisedPackagesOf(root.main())) {
                Path folder = root.main();
                for (final String part : pkg.split("\\.")) {
                    folder = folder.resolve(part);
                }
                if (!seen.add(pkg)) {
                    wrong.add(pkg + " is declared twice");
                } else if (!pkg.startsWith(own + ".") || Set.of(pkg.split("\\.")).contains("api")) {
                    wrong.add(pkg + " is not one of " + own + "'s own packages beside its api ones");
                } else if (typesIn(folder).isEmpty()) {
                    wrong.add(pkg + " holds no type");
                } else if (!Files.isRegularFile(folder.resolve("package-info.java"))
                        || !read(folder.resolve("package-info.java")).contains("/**")) {
                    wrong.add(pkg + " has no package-info.java explaining it to a reader who knows nothing of it");
                }
            }
        }
        return wrong;
    }

    /** Each mod whose API is not what its photo says, with what is new and gone, and where the proposal is. */
    public List<String> photoProblems() {
        final List<String> wrong = new ArrayList<>();
        for (final Map.Entry<String, List<ApiPromise>> mod : promisesByMod().entrySet()) {
            final ApiPhoto photo = photoOf(mod.getKey());
            final Set<String> now = new LinkedHashSet<>();
            for (final ApiPromise promise : mod.getValue()) {
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
        return wrong;
    }

    /** Each mod whose API class's {@code VERSION} is not the newest number in its photo. */
    public List<String> versionProblems() {
        final List<String> wrong = new ArrayList<>();
        for (final String mod : promisesByMod().keySet()) {
            final int newest = photoOf(mod).newest();
            final Integer version = versionOf(mod, wrong);
            if (version != null && version != newest) {
                wrong.add(mod + ": the API VERSION is " + version + " and the newest number in the photo " + newest
                        + ", so something was added without raising it, or it was raised with nothing added");
            }
        }
        return wrong;
    }

    /** Each part marked experimental that is as old as the stable version, and each newer one not marked. */
    public List<String> experimentalProblems() {
        final List<String> wrong = new ArrayList<>();
        for (final Map.Entry<String, List<ApiPromise>> mod : promisesByMod().entrySet()) {
            final ApiPhoto photo = photoOf(mod.getKey());
            final Map<String, ApiPromise> types = new HashMap<>();
            for (final ApiPromise promise : mod.getValue()) {
                if (promise.isType()) {
                    types.put(promise.owner(), promise);
                }
            }
            for (final ApiPromise promise : mod.getValue()) {
                final Integer version = photo.versions().get(promise.key());
                if (version == null) {
                    // Not in the photo yet: the photo's check says so, and what it is new at is not known until then.
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
        return wrong;
    }

    /** Every problem of every check. */
    public List<String> problems() {
        final List<String> all = new ArrayList<>(leaks());
        all.addAll(packageProblems());
        all.addAll(photoProblems());
        all.addAll(versionProblems());
        all.addAll(experimentalProblems());
        return all;
    }

    /** Every promise of every mod that makes one, by the mod's name: what its promised sources say. */
    public Map<String, List<ApiPromise>> promisesByMod() {
        final Map<String, List<ApiPromise>> byMod = new TreeMap<>();
        for (final Path file : promisedSources(new ArrayList<>())) {
            byMod.computeIfAbsent(modOf(file), mod -> new ArrayList<>())
                    .addAll(new ApiSourceReader(read(file)).read());
        }
        return byMod;
    }

    /*
     * Every source whose public face is part of the promise: what sits in an api package, what sits in a package its
     * mod's API class declares as promised, and the promised types that live where they belong; a promised type not
     * found exactly once is said in wrong.
     */
    private List<Path> promisedSources(final List<String> wrong) {
        final List<Path> found = new ArrayList<>();
        final Map<String, Integer> promisedFound = new HashMap<>();
        for (final Root root : roots) {
            final List<String> packages = promisedPackagesOf(root.main());
            for (final Path file : javaUnder(root.main())) {
                final String type = file.getFileName().toString().replace(JAVA, "");
                if (type.equals("package-info")) {
                    continue;
                }
                final boolean isPromised = promised.contains(type);
                if (isPromised) {
                    promisedFound.merge(type, 1, Integer::sum);
                }
                if (isPromised || insideApi(file) || packages.contains(packageOf(root.main(), file.getParent()))) {
                    found.add(file);
                }
            }
        }
        for (final String type : promised) {
            final int times = promisedFound.getOrDefault(type, 0);
            if (times != 1) {
                wrong.add("the promised type " + type + " is found " + times + " times in the sources, not once");
            }
        }
        return found;
    }

    /* The packages a mod's API class declares as promised beside its api ones, or none when it declares none. */
    private List<String> promisedPackagesOf(final Path main) {
        final Path api = apiClassOf(main);
        if (api == null) {
            return List.of();
        }
        final Matcher declared = PROMISED_PACKAGES.matcher(read(api));
        if (!declared.find()) {
            return List.of();
        }
        final List<String> packages = new ArrayList<>();
        final Matcher name = QUOTED.matcher(declared.group(1));
        while (name.find()) {
            packages.add(name.group(1));
        }
        return packages;
    }

    /* A mod's API class, the one saying its VERSION, or null for a mod that makes no promise. */
    private Path apiClassOf(final Path main) {
        for (final Path file : apiSources()) {
            if (file.startsWith(main) && apiClass.matcher(file.getFileName().toString()).matches()) {
                return file;
            }
        }
        return null;
    }

    private String modOf(final Path file) {
        for (final Root root : roots) {
            if (file.startsWith(root.main())) {
                return root.mod();
            }
        }
        throw new IllegalStateException(file + " is in no mod's source tree");
    }

    /* The mod's API version, as the number its API class says, or none with the reason in wrong. */
    private Integer versionOf(final String mod, final List<String> wrong) {
        for (final Root root : roots) {
            final Path file = root.mod().equals(mod) ? apiClassOf(root.main()) : null;
            if (file != null) {
                final Matcher version = VERSION.matcher(read(file));
                if (!version.find()) {
                    wrong.add(file + " says no VERSION");
                    return null;
                }
                return Integer.parseInt(version.group(1));
            }
        }
        wrong.add(mod + " makes promises but has no API class saying their version");
        return null;
    }

    private ApiPhoto photoOf(final String mod) {
        return ApiPhoto.read(photos.resolve(mod + ".txt"));
    }

    /*
     * Writes what the photo would have to say for the API as it is: what it already said kept at its version, a line
     * that only became deprecated kept at the version it had, and what is new at the next number.
     */
    private Path propose(final String mod, final ApiPhoto photo, final List<ApiPromise> promises) {
        final int next = photo.newest() + 1;
        final Map<String, Integer> undeprecated = new HashMap<>();
        for (final Map.Entry<String, Integer> kept : photo.versions().entrySet()) {
            undeprecated.put(undeprecated(kept.getKey()), kept.getValue());
        }
        final List<ApiPromise> sorted = new ArrayList<>(promises);
        sorted.sort(Comparator.comparing(ApiPromise::owner).thenComparing(promise -> !promise.isType())
                .thenComparing(ApiPromise::what));
        final List<String> lines = new ArrayList<>();
        for (final ApiPromise promise : sorted) {
            Integer version = photo.versions().get(promise.key());
            if (version == null) {
                version = undeprecated.get(undeprecated(promise.key()));
            }
            lines.add((version == null ? next : version) + " " + promise.key());
        }
        final Path file = proposals.resolve(mod + ".txt");
        try {
            Files.createDirectories(proposals);
            Files.writeString(file, ApiPhoto.text(mod, photo.stable(), lines), StandardCharsets.UTF_8);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
        return file;
    }

    /* A photo's key with its deprecation taken off, which is how a line that only became deprecated is known. */
    private static String undeprecated(final String key) {
        return key.replaceFirst("  (" + ApiPromise.TYPE + ")?" + ApiPromise.DEPRECATED, "  $1");
    }

    /*
     * Whether a promise is newer than its photo's stable version: itself, or through a type it sits in, since
     * everything in a type that came later came with it.
     */
    private static boolean newerThanStable(final ApiPromise promise, final ApiPhoto photo,
                                           final Map<String, ApiPromise> types) {
        if (newer(promise, photo)) {
            return true;
        }
        // The types around it, from the nearest out; the dots past the outermost one are its package's.
        String around = promise.isType() ? ApiReach.outerOf(promise.owner()) : promise.owner();
        while (around != null) {
            final ApiPromise type = types.get(around);
            if (type != null && newer(type, photo)) {
                return true;
            }
            around = ApiReach.outerOf(around);
        }
        return false;
    }

    private static boolean newer(final ApiPromise promise, final ApiPhoto photo) {
        final Integer version = photo.versions().get(promise.key());
        return version != null && version > photo.stable();
    }

    private static String read(final Path file) {
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /* The types a folder holds itself, not in the folders under it, or none when there is no such folder. */
    private static List<Path> typesIn(final Path folder) {
        if (!Files.isDirectory(folder)) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(folder)) {
            return files.filter(path -> path.toString().endsWith(JAVA))
                    .filter(path -> !path.getFileName().toString().equals("package-info.java")).toList();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static List<Path> javaUnder(final Path main) {
        try (Stream<Path> walk = Files.walk(main)) {
            return walk.filter(path -> path.toString().endsWith(JAVA)).sorted().toList();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /* Whether a source file sits in an api package or in one nested under it. */
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

    /* The package a folder of a source tree holds, in Java's dotted words. */
    private static String packageOf(final Path main, final Path folder) {
        return main.relativize(folder).toString().replace('\\', '.').replace('/', '.');
    }

    /* A mod's sources: its name, and the folder its packages start in. */
    private record Root(String mod, Path main) {
    }

    /** How a surface is set up: whose sources, what they promise beside their api packages, where the photos are. */
    public static final class Builder {

        private final List<Root> roots = new ArrayList<>();
        private final Set<String> promised = new LinkedHashSet<>();
        private final Set<String> knownLeaks = new LinkedHashSet<>();
        private final List<String> elsewhere = new ArrayList<>(GAME);
        private Pattern apiClass = Pattern.compile("\\w+Api\\.java");
        private Path photos;
        private Path proposals = Path.of("build", "api-photo");

        private Builder() {
        }

        /** The mod {@code name}, whose packages start in {@code main}, as {@code src/main/java}. */
        public Builder mod(final String name, final Path main) {
            roots.add(new Root(name, main.toAbsolutePath().normalize()));
            return this;
        }

        /** Every mod under {@code folder}: each folder in it with a {@code src/main/java}, named after the folder. */
        public Builder modsUnder(final Path folder) {
            try (Stream<Path> modules = Files.list(folder)) {
                modules.sorted().map(module -> module.resolve("src").resolve("main").resolve("java"))
                        .filter(Files::isDirectory)
                        .forEach(main -> mod(main.getParent().getParent().getParent().getFileName().toString(), main));
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
            return this;
        }

        /** Types of the mod, by their simple names, promised though they live outside an api package. */
        public Builder promised(final Set<String> types) {
            promised.addAll(types);
            return this;
        }

        /** The debts of the promise as it stands, each "a promised type > a type it names unpromised". */
        public Builder knownLeaks(final Set<String> leaks) {
            knownLeaks.addAll(leaks);
            return this;
        }

        /** More starts of names a signature may always say, besides the language's and the game's. */
        public Builder alsoAllowed(final List<String> prefixes) {
            elsewhere.addAll(prefixes);
            return this;
        }

        /** How a mod's API class's file is named, the class that says its {@code VERSION}; any {@code *Api.java}. */
        public Builder apiClass(final String fileNamePattern) {
            apiClass = Pattern.compile(fileNamePattern);
            return this;
        }

        /** The folder of the photos, one file a mod, named after it. */
        public Builder photos(final Path folder) {
            photos = folder;
            return this;
        }

        /** The folder a failing check writes the photos it proposes to; {@code build/api-photo} unless set. */
        public Builder proposals(final Path folder) {
            proposals = folder;
            return this;
        }

        public ApiSurface build() {
            return new ApiSurface(this);
        }
    }
}
