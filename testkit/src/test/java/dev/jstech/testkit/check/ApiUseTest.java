/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core Test Kit.
 */
package dev.jstech.testkit.check;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ApiUseTest {

    @TempDir
    Path folder;

    private Path classes;
    private Path photo;

    @BeforeEach
    void compileTheDemoMods() throws IOException {
        final Path sources = folder.resolve("src");
        write(sources, "com/demo/kit/Promised.java", """
                package com.demo.kit;

                import java.util.List;

                public class Promised {
                    private final String name;
                    public static final List<String> NAMES = List.of("a");

                    public Promised(final String name) {
                        this.name = name;
                    }

                    public int size() {
                        return name.length();
                    }

                    public int hidden() {
                        return 0;
                    }

                    public <T> T same(final T value, final List<String> notes) {
                        return value;
                    }

                    protected void work(final String what) {
                    }
                }
                """);
        write(sources, "com/demo/kit/Shape.java", """
                package com.demo.kit;

                public record Shape(int width, String name) {
                }
                """);
        write(sources, "com/demo/kit/Mode.java", """
                package com.demo.kit;

                public enum Mode {
                    ON, OFF
                }
                """);
        write(sources, "com/demo/kit/Plain.java", """
                package com.demo.kit;

                public class Plain {
                    public int one() {
                        return 1;
                    }
                }
                """);
        write(sources, "com/demo/kit/Unpromised.java", """
                package com.demo.kit;

                public final class Unpromised {
                    public static int one() {
                        return 1;
                    }
                }
                """);
        write(sources, "com/demo/forbidden/Secret.java", """
                package com.demo.forbidden;

                public final class Secret {
                    public static int value() {
                        return 3;
                    }
                }
                """);
        write(sources, "com/demo/good/GoodUser.java", """
                package com.demo.good;

                import com.demo.kit.Mode;
                import com.demo.kit.Plain;
                import com.demo.kit.Promised;
                import com.demo.kit.Shape;
                import java.util.List;

                public final class GoodUser extends Promised {
                    public GoodUser() {
                        super("good");
                    }

                    public int all(final Mode mode) {
                        final Shape shape = new Shape(size(), "square");
                        final int fromMode = switch (mode) {
                            case ON -> 1;
                            case OFF -> 0;
                        };
                        return shape.width() + NAMES.size() + same(shape, List.of()).width() + fromMode
                                + Mode.values().length + (shape.equals(new Shape(1, "a")) ? 1 : 0) + new Plain().one()
                                + Mode.valueOf("ON").ordinal() + shape.name().length();
                    }

                    @Override
                    protected void work(final String what) {
                        super.work(what);
                    }
                }
                """);
        write(sources, "com/demo/bad/BadUser.java", """
                package com.demo.bad;

                import com.demo.forbidden.Secret;
                import com.demo.kit.Promised;
                import com.demo.kit.Unpromised;

                public final class BadUser extends Promised {
                    public BadUser() {
                        super("bad");
                    }

                    public int all() {
                        return new Promised("other").hidden() + Unpromised.one() + Secret.value();
                    }

                    @Override
                    public int hidden() {
                        return 1;
                    }
                }
                """);
        classes = folder.resolve("classes");
        compile(sources, classes);
        photo = folder.resolve("kit.txt");
        Files.writeString(photo, """
                stable 0
                1 com.demo.kit.Mode  type enum
                1 com.demo.kit.Mode  OFF
                1 com.demo.kit.Mode  ON
                1 com.demo.kit.Plain  type class
                1 com.demo.kit.Plain  int one()
                1 com.demo.kit.Promised  type class
                1 com.demo.kit.Promised  <T> T same(T, List<String>)
                1 com.demo.kit.Promised  int size()
                1 com.demo.kit.Promised  new(String)
                1 com.demo.kit.Promised  protected void work(String)
                1 com.demo.kit.Promised  static final List<String> NAMES
                1 com.demo.kit.Shape  type record(int width, String name)
                """, StandardCharsets.UTF_8);
    }

    @Test
    void problems_noneForAModThatUsesOnlyThePromise() throws IOException {
        assertEquals(List.of(), check("com.demo.good").problems(classes));
    }

    @Test
    void problems_nameWhatIsUnpromisedAndWhatIsForbidden() throws IOException {
        final String user = "com.demo.bad.BadUser";
        assertEquals(List.of(
                user + " names com.demo.forbidden.Secret, from a mod it must never reach",
                user + " names com.demo.kit.Unpromised, which no API photo promises",
                user + " overrides com.demo.kit.Promised.hidden(), which no API photo promises",
                user + " uses com.demo.forbidden.Secret.value(), from a mod it must never reach",
                user + " uses com.demo.kit.Promised.hidden(), which no API photo promises",
                user + " uses com.demo.kit.Unpromised.one(), which no API photo promises"),
                check("com.demo.bad").problems(classes));
    }

    @Test
    void problems_sayWhenThereIsNoClassOfTheModToRead() throws IOException {
        assertEquals(List.of("no class of com.demo.none is in " + classes.toAbsolutePath()),
                check("com.demo.none").problems(classes));
    }

    @Test
    void photoOf_readsThePhotoAModCarriesInItsJar() throws IOException {
        final Path carried = classes.resolve(ApiUse.PHOTOS).resolve("kit.txt");
        Files.createDirectories(carried.getParent());
        Files.copy(photo, carried);
        final ApiUse.Builder builder = ApiUse.builder().own("com.demo.good").series("com.demo")
                .classes(new URLClassLoader(new URL[] {classes.toUri().toURL()}, null));

        assertEquals(List.of(), builder.photoOf("kit").build().problems(classes));
        assertThrows(IllegalArgumentException.class, () -> builder.photoOf("missing"));
    }

    @Test
    void photo_refusesAFileThatIsNotThere() {
        assertThrows(IllegalArgumentException.class, () -> ApiUse.builder().photo(folder.resolve("missing.txt")));
    }

    private ApiUse check(final String own) throws IOException {
        return ApiUse.builder().own(own).series("com.demo").forbid("com.demo.forbidden").photo(photo)
                .classes(new URLClassLoader(new URL[] {classes.toUri().toURL()}, null)).build();
    }

    private static void write(final Path sources, final String path, final String text) throws IOException {
        final Path file = sources.resolve(path);
        Files.createDirectories(file.getParent());
        Files.writeString(file, text, StandardCharsets.UTF_8);
    }

    private static void compile(final Path sources, final Path into) throws IOException {
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        final List<String> arguments = new ArrayList<>(List.of("-d", into.toString(), "--release", "21"));
        try (Stream<Path> walk = Files.walk(sources)) {
            walk.filter(path -> path.toString().endsWith(".java")).forEach(path -> arguments.add(path.toString()));
        }
        assertEquals(0, compiler.run(null, null, null, arguments.toArray(String[]::new)), "the demo mods compile");
    }
}
