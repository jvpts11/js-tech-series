/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core Test Kit.
 */
package dev.jstech.testkit.check;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.jetbrains.annotations.Nullable;

/**
 * What a mod uses of the mods it is built on, held to their promise: every class, field and method its compiled
 * classes reach in them has to be in their API photos, and nothing may come from a mod it is never to reach. Where
 * {@link ApiSurface} keeps a mod's promise from growing by accident, this keeps a mod built on it inside the promise.
 *
 * <p>It reads class files, so it sees what the compiler settled on rather than what the source says. A method called
 * through the mod's own class is followed up to the class that declares it, a method the mod overrides is followed to
 * the one it overrides, and whatever the language or the game declares is the mod's to use. A member the compiler
 * writes without the source saying it, a record's accessors and constructor, an enum's {@code values()} and
 * {@code valueOf}, the constructor of a class that declares none, comes with its promised type.
 *
 * <pre>{@code
 * @Test
 * void mod_usesOnlyWhatTheCorePromises() {
 *     final ApiUse use = ApiUse.builder()
 *             .own("com.example.mymod")
 *             .photoOf("core")
 *             .build();
 *     assertEquals(List.of(), use.problems(Path.of("build", "classes", "java", "main")));
 * }
 * }</pre>
 */
public final class ApiUse {

    private final String own;
    private final String series;
    private final List<String> forbidden;
    private final Map<String, List<String>> promised;
    private final ClassLoader loader;
    private final Map<String, Optional<ClassReferences>> read = new HashMap<>();

    /** Where in its jar a mod carries its API photo, one file named after its folder. */
    public static final String PHOTOS = "META-INF/jstech/api/";
    private static final String CONSTRUCTOR = "<init>";
    private static final String CLASS = ".class";
    /* The words a photo's line may open with before the member it promises. */
    private static final Pattern LEADING = Pattern.compile(
            "^(deprecated|protected|abstract|default|static|final|synchronized|native|strictfp) ");
    /* The word naming what kind of type a photo's type line is, after which its type variables open. */
    private static final Pattern KIND = Pattern.compile("(class|interface|enum|record|@interface)");
    /* The methods every object has from the language, which a record or an enum declares again. */
    private static final Set<String> OBJECT_METHODS = Set.of("equals(Ljava/lang/Object;)Z", "hashCode()I",
            "toString()Ljava/lang/String;");
    private static final Map<Character, String> PRIMITIVES = Map.of('B', "byte", 'C', "char", 'D', "double",
            'F', "float", 'I', "int", 'J', "long", 'S', "short", 'Z', "boolean", 'V', "void");

    private ApiUse(final Builder from) {
        this.own = internal(Objects.requireNonNull(from.own, "a check knows whose classes it reads"));
        this.series = internal(from.series);
        this.forbidden = from.forbidden.stream().map(ApiUse::internal).toList();
        this.promised = Map.copyOf(from.promised);
        this.loader = from.loader;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Each thing the mod's classes in {@code classes} reach that they may not, one line each, sorted: a type or a
     * member the photos do not promise, and anything from a mod it must never reach.
     */
    public List<String> problems(final Path classes) {
        final Map<String, ClassReferences> mine = new TreeMap<>();
        try (Stream<Path> walk = Files.walk(classes)) {
            for (final Path file : walk.filter(path -> path.toString().endsWith(CLASS)).toList()) {
                final ClassReferences references = ClassReferences.read(Files.readAllBytes(file));
                if (references.name().startsWith(own)) {
                    mine.put(references.name(), references);
                }
            }
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
        if (mine.isEmpty()) {
            return List.of("no class of " + dotted(own) + " is in " + classes.toAbsolutePath());
        }
        mine.forEach((name, references) -> read.put(name, Optional.of(references)));
        final Set<String> wrong = new TreeSet<>();
        for (final ClassReferences references : mine.values()) {
            final String user = dotted(references.name());
            for (final String type : references.types()) {
                typeProblem(user, type).ifPresent(wrong::add);
            }
            for (final ClassReferences.Member member : references.members()) {
                memberProblem(user, member).ifPresent(wrong::add);
            }
            for (final ClassReferences.Declared declared : references.declared()) {
                overrideProblem(references, declared).ifPresent(wrong::add);
            }
        }
        return List.copyOf(wrong);
    }

    private Optional<String> typeProblem(final String user, final String type) {
        if (!type.startsWith(series) || type.startsWith(own)) {
            return Optional.empty();
        }
        if (isForbidden(type)) {
            return Optional.of(user + " names " + dotted(type) + ", from a mod it must never reach");
        }
        final List<String> lines = promised.get(dotted(type));
        if (lines == null || lines.stream().noneMatch(line -> line.startsWith(ApiPromise.TYPE))) {
            return Optional.of(user + " names " + dotted(type) + ", which no API photo promises");
        }
        return Optional.empty();
    }

    private Optional<String> memberProblem(final String user, final ClassReferences.Member member) {
        if (!inSight(member.owner())) {
            return Optional.empty();
        }
        final String declarer = member.field() ? fieldDeclarer(member.owner(), member.name())
                : methodDeclarer(member.owner(), member.name(), member.descriptor());
        return promiseProblem(user + " uses", declarer, member.name(), member.descriptor(), member.field());
    }

    /* A method of the mod's that takes the place of one of the series', which has to be promised to be taken. */
    private Optional<String> overrideProblem(final ClassReferences references, final ClassReferences.Declared declared) {
        if (declared.field() || declared.isStatic() || !declared.visible() || declared.name().startsWith("<")) {
            return Optional.empty();
        }
        final List<String> above = new ArrayList<>(references.interfaces());
        if (references.superName() != null) {
            above.add(0, references.superName());
        }
        for (final String start : above) {
            final String declarer = methodDeclarer(start, declared.name(), declared.descriptor());
            if (declarer != null) {
                return promiseProblem(dotted(references.name()) + " overrides", declarer, declared.name(),
                        declared.descriptor(), false);
            }
        }
        return Optional.empty();
    }

    /* What is wrong with using a member declared in declarer, or nothing when it is the mod's own or promised. */
    private Optional<String> promiseProblem(final String doing, @Nullable final String declarer, final String name,
                                            final String descriptor, final boolean field) {
        if (declarer == null || declarer.startsWith(own)) {
            return Optional.empty();
        }
        final String shown = shown(declarer, name, descriptor, field);
        if (isForbidden(declarer)) {
            return Optional.of(doing + " " + shown + ", from a mod it must never reach");
        }
        if (!promises(dotted(declarer), name, descriptor, field)) {
            return Optional.of(doing + " " + shown + ", which no API photo promises");
        }
        return Optional.empty();
    }

    /*
     * The class declaring a field as the virtual machine finds it: the class named, then what it implements, then
     * what it extends; none when the search leaves the series and the mod first, for the game's or the language's.
     */
    @Nullable
    private String fieldDeclarer(@Nullable final String at, final String name) {
        if (at == null || !inSight(at)) {
            return null;
        }
        final ClassReferences references = find(at);
        if (references.declares(name, "", true)) {
            return at;
        }
        for (final String implemented : references.interfaces()) {
            final String found = fieldDeclarer(implemented, name);
            if (found != null) {
                return found;
            }
        }
        return fieldDeclarer(references.superName(), name);
    }

    /*
     * The class declaring a method as the virtual machine finds it: up the classes extended first, then through what
     * they implement; none when the search leaves the series and the mod first, for the game's or the language's.
     */
    @Nullable
    private String methodDeclarer(final String start, final String name, final String descriptor) {
        final List<String> implemented = new ArrayList<>();
        for (String at = start; at != null && inSight(at); at = find(at).superName()) {
            final ClassReferences references = find(at);
            if (references.declares(name, descriptor, false)) {
                return at;
            }
            implemented.addAll(references.interfaces());
        }
        final Set<String> seen = new HashSet<>();
        while (!implemented.isEmpty()) {
            final String at = implemented.remove(0);
            if (!inSight(at) || !seen.add(at)) {
                continue;
            }
            final ClassReferences references = find(at);
            if (references.declares(name, descriptor, false)) {
                return at;
            }
            implemented.addAll(references.interfaces());
        }
        return null;
    }

    /* Whether the photos promise the member of the type, the members the compiler writes for it included. */
    private boolean promises(final String type, final String name, final String descriptor, final boolean field) {
        final List<String> lines = promised.getOrDefault(type, List.of());
        final String typeLine = lines.stream().filter(line -> line.startsWith(ApiPromise.TYPE)).findFirst().orElse("");
        if (typeLine.isEmpty()) {
            return false;
        }
        if (field) {
            return lines.stream().anyMatch(line -> !line.startsWith(ApiPromise.TYPE) && !line.contains("(")
                    && line.substring(line.lastIndexOf(' ') + 1).equals(name));
        }
        if (OBJECT_METHODS.contains(name + descriptor)) {
            return true;
        }
        final String kind = kindOf(typeLine);
        final List<Erased> parameters = parameters(descriptor);
        if (kind.equals("enum") && (name.equals("values") && parameters.isEmpty()
                || name.equals("valueOf") && descriptor.startsWith("(Ljava/lang/String;)"))) {
            return true;
        }
        final Set<String> variables = typeVariables(type);
        if (kind.equals("record")) {
            final List<String> components = topLevelParts(between(typeLine.substring(typeLine.indexOf("record")),
                    '(', ')'));
            if (name.equals(CONSTRUCTOR) && matches(components.stream().map(ApiUse::withoutName).toList(),
                    parameters, variables)) {
                return true;
            }
            if (parameters.isEmpty() && components.stream().anyMatch(part -> lastWord(part).equals(name))) {
                return true;
            }
        }
        boolean anyConstructor = false;
        for (final String line : lines) {
            final int open = line.indexOf('(');
            if (line.startsWith(ApiPromise.TYPE) || open < 0) {
                continue;
            }
            final String head = line.substring(0, open);
            final String lineName = lastWord(head).equals("new") ? CONSTRUCTOR : lastWord(head);
            anyConstructor |= lineName.equals(CONSTRUCTOR);
            if (!lineName.equals(name)) {
                continue;
            }
            final Set<String> all = new HashSet<>(variables);
            all.addAll(variablesOf(leadingVariables(head)));
            if (matches(topLevelParts(line.substring(open + 1, line.lastIndexOf(')'))), parameters, all)) {
                return true;
            }
        }
        // A class that declares no constructor has the one the compiler gives it, as public as the class.
        return name.equals(CONSTRUCTOR) && !anyConstructor && parameters.isEmpty() && kind.equals("class");
    }

    /* The type variables a member of the type can say: the type's own and those of the types around it. */
    private Set<String> typeVariables(final String type) {
        final Set<String> found = new HashSet<>();
        for (String at = type; at != null; at = ApiReach.outerOf(at)) {
            for (final String line : promised.getOrDefault(at, List.of())) {
                if (line.startsWith(ApiPromise.TYPE)) {
                    final Matcher kind = KIND.matcher(line);
                    if (kind.find() && kind.end() < line.length() && line.charAt(kind.end()) == '<') {
                        found.addAll(variablesOf(between(line.substring(kind.end()), '<', '>')));
                    }
                }
            }
        }
        return found;
    }

    private ClassReferences find(final String name) {
        return read.computeIfAbsent(name, missing -> {
            try (InputStream in = loader.getResourceAsStream(missing + CLASS)) {
                if (in == null) {
                    throw new IllegalStateException(missing + CLASS + " is on no class path the check can read");
                }
                return Optional.of(ClassReferences.read(in.readAllBytes()));
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
        }).orElseThrow();
    }

    /* Whether a class is the series' or the mod's own, whose members the check follows; the rest are the game's. */
    private boolean inSight(final String name) {
        return name.startsWith(series) || name.startsWith(own);
    }

    private boolean isForbidden(final String name) {
        return forbidden.stream().anyMatch(name::startsWith);
    }

    /* Whether the parameters a photo writes are those the compiler erased them to, a type variable fitting any. */
    private static boolean matches(final List<String> written, final List<Erased> erased, final Set<String> variables) {
        if (written.size() != erased.size()) {
            return false;
        }
        for (int i = 0; i < written.size(); i++) {
            String type = withoutGenerics(written.get(i)).strip();
            int dimensions = 0;
            if (type.endsWith("...")) {
                dimensions++;
                type = type.substring(0, type.length() - 3).strip();
            }
            while (type.endsWith("[]")) {
                dimensions++;
                type = type.substring(0, type.length() - 2).strip();
            }
            final String simple = type.substring(type.lastIndexOf('.') + 1);
            final Erased actual = erased.get(i);
            if (dimensions != actual.dimensions() || !variables.contains(simple) && !simple.equals(actual.simple())) {
                return false;
            }
        }
        return true;
    }

    /* The parameters of a method's descriptor, each as its simple name and how many arrays deep it is. */
    private static List<Erased> parameters(final String descriptor) {
        final List<Erased> found = new ArrayList<>();
        int at = descriptor.indexOf('(') + 1;
        while (descriptor.charAt(at) != ')') {
            int dimensions = 0;
            while (descriptor.charAt(at) == '[') {
                dimensions++;
                at++;
            }
            if (descriptor.charAt(at) == 'L') {
                final int end = descriptor.indexOf(';', at);
                found.add(new Erased(simpleOf(descriptor.substring(at + 1, end)), dimensions));
                at = end + 1;
            } else {
                found.add(new Erased(PRIMITIVES.get(descriptor.charAt(at)), dimensions));
                at++;
            }
        }
        return found;
    }

    /* A member as a problem shows it: its type's whole name, then its own with its parameters' simple names. */
    private static String shown(final String declarer, final String name, final String descriptor,
                                final boolean field) {
        if (field) {
            return dotted(declarer) + "." + name;
        }
        final List<String> types = new ArrayList<>();
        for (final Erased parameter : parameters(descriptor)) {
            types.add(parameter.simple() + "[]".repeat(parameter.dimensions()));
        }
        return dotted(declarer) + "." + (name.equals(CONSTRUCTOR) ? "new" : name) + "(" + String.join(", ", types)
                + ")";
    }

    private static String kindOf(final String typeLine) {
        final Matcher kind = KIND.matcher(typeLine);
        return kind.find() ? kind.group(1) : "";
    }

    /* The type variables a photo's member line opens with, after its modifiers, or none. */
    private static String leadingVariables(final String head) {
        String rest = head.strip();
        Matcher word = LEADING.matcher(rest);
        while (word.find()) {
            rest = rest.substring(word.end()).strip();
            word = LEADING.matcher(rest);
        }
        return rest.startsWith("<") ? between(rest, '<', '>') : "";
    }

    /* The names of the type variables a list declares, as in "M extends Menu, A". */
    private static List<String> variablesOf(final String declared) {
        final List<String> names = new ArrayList<>();
        for (final String part : topLevelParts(declared)) {
            final String name = part.strip().split("\\s+")[0];
            if (!name.isEmpty()) {
                names.add(name);
            }
        }
        return names;
    }

    /* What stands between the first open character and the one that closes it, nested ones and all. */
    private static String between(final String text, final char open, final char close) {
        final int start = text.indexOf(open);
        if (start < 0) {
            return "";
        }
        int depth = 0;
        for (int i = start; i < text.length(); i++) {
            if (text.charAt(i) == open) {
                depth++;
            } else if (text.charAt(i) == close && --depth == 0) {
                return text.substring(start + 1, i);
            }
        }
        return text.substring(start + 1);
    }

    /* The parts of a list split at its commas outside angle brackets, parentheses and square brackets. */
    private static List<String> topLevelParts(final String list) {
        final List<String> parts = new ArrayList<>();
        int depth = 0;
        int from = 0;
        for (int i = 0; i < list.length(); i++) {
            final char c = list.charAt(i);
            if (c == '<' || c == '(' || c == '[') {
                depth++;
            } else if (c == '>' || c == ')' || c == ']') {
                depth--;
            } else if (c == ',' && depth == 0) {
                parts.add(list.substring(from, i).strip());
                from = i + 1;
            }
        }
        final String last = list.substring(from).strip();
        if (!last.isEmpty()) {
            parts.add(last);
        }
        return parts;
    }

    private static String withoutGenerics(final String type) {
        final StringBuilder out = new StringBuilder();
        int depth = 0;
        for (int i = 0; i < type.length(); i++) {
            final char c = type.charAt(i);
            if (c == '<') {
                depth++;
            } else if (c == '>') {
                depth--;
            } else if (depth == 0) {
                out.append(c);
            }
        }
        return out.toString();
    }

    /* A record's component as its type alone, without the name after it. */
    private static String withoutName(final String component) {
        final String bare = component.strip();
        final int space = bare.lastIndexOf(' ');
        return space < 0 ? bare : bare.substring(0, space);
    }

    private static String lastWord(final String text) {
        final String bare = text.strip();
        return bare.substring(bare.lastIndexOf(' ') + 1);
    }

    private static String simpleOf(final String binary) {
        final String afterPackage = binary.substring(binary.lastIndexOf('/') + 1);
        return afterPackage.substring(afterPackage.lastIndexOf('$') + 1);
    }

    /* A package or a class in the binary form a class file writes it in, a package ending in its slash. */
    private static String internal(final String dottedName) {
        return dottedName.replace('.', '/') + "/";
    }

    private static String dotted(final String binary) {
        final String name = binary.endsWith("/") ? binary.substring(0, binary.length() - 1) : binary;
        return name.replace('/', '.').replace('$', '.');
    }

    /* A parameter as a descriptor erases it: the simple name of its type, and how many arrays deep. */
    private record Erased(String simple, int dimensions) {
    }

    /** How a check is set up: whose classes it reads, what is promised to them, and what they must never reach. */
    public static final class Builder {

        private final List<String> forbidden = new ArrayList<>();
        private final Map<String, List<String>> promised = new LinkedHashMap<>();
        private String own;
        private String series = "dev.jstech";
        private ClassLoader loader = Thread.currentThread().getContextClassLoader() != null
                ? Thread.currentThread().getContextClassLoader() : ApiUse.class.getClassLoader();

        private Builder() {
        }

        /** The package the mod's own classes are in, as {@code com.example.mymod}: what is checked. */
        public Builder own(final String pkg) {
            own = pkg;
            return this;
        }

        /** The package whose classes are held to the photos; {@code dev.jstech}, the series', unless set. */
        public Builder series(final String pkg) {
            series = pkg;
            return this;
        }

        /** Packages of mods the mod must never reach, promised or not, as {@code dev.jstech.computers}. */
        public Builder forbid(final String... packages) {
            forbidden.addAll(List.of(packages));
            return this;
        }

        /** A photo of what a mod the mod is built on promises, as its API surface test keeps it. */
        public Builder photo(final Path file) {
            if (!Files.isRegularFile(file)) {
                throw new IllegalArgumentException("there is no API photo at " + file.toAbsolutePath());
            }
            return taking(ApiPhoto.read(file));
        }

        /**
         * The photo the mod {@code modFolder} carries in its jar, {@value ApiUse#PHOTOS}{@code <modFolder>.txt},
         * read from the class path the builder reads classes from: J's Core's is {@code photoOf("core")}, and it is
         * the photo of the very Core the mod is built against.
         */
        public Builder photoOf(final String modFolder) {
            final String resource = PHOTOS + modFolder + ".txt";
            try (InputStream in = loader.getResourceAsStream(resource)) {
                if (in == null) {
                    throw new IllegalArgumentException("no API photo of " + modFolder + " on the class path, at "
                            + resource);
                }
                return taking(ApiPhoto.parse(new String(in.readAllBytes(), StandardCharsets.UTF_8)));
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        /** Where the classes of the mods it is built on are read from; the test's own class path unless set. */
        public Builder classes(final ClassLoader from) {
            loader = from;
            return this;
        }

        public ApiUse build() {
            return new ApiUse(this);
        }

        private Builder taking(final ApiPhoto photo) {
            for (final String key : photo.versions().keySet()) {
                final int split = key.indexOf("  ");
                promised.computeIfAbsent(key.substring(0, split), type -> new ArrayList<>())
                        .add(key.substring(split + 2));
            }
            return this;
        }
    }
}
