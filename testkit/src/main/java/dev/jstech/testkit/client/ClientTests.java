/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core Test Kit.
 */
package dev.jstech.testkit.client;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

/**
 * The client tests of every mod, registered from each mod's client code by suite: a mod's classes of tests marked
 * {@link ClientTest}, each test a {@code public static void name(ClientTestHelper helper)} method, read once, with no
 * scan of the class path.
 *
 * <pre>{@code
 * ClientTests.register("mymod", "machines", List.of(KilnClientTests.class, PressClientTests.class));
 * }</pre>
 *
 * <p>A mod with steps of its own registers its tests with a helper of its own, built on {@link ClientTestHelper}; and
 * what a mod keeps between tests that the next one must not inherit is put back by {@link #beforeEach}.
 */
public final class ClientTests {

    private static final List<Suite> SUITES = new CopyOnWriteArrayList<>();
    private static final List<Runnable> BEFORE_EACH = new CopyOnWriteArrayList<>();

    private ClientTests() {
    }

    /**
     * Registers the client tests of {@code classes}, marked {@link ClientTest} and taking a {@link ClientTestHelper},
     * as the suite {@code suite} of the mod {@code modid}.
     *
     * @throws IllegalStateException when a marked method is not public, static and of one helper
     */
    public static void register(final String modid, final String suite, final List<Class<?>> classes) {
        final List<Test> tests = new ArrayList<>();
        for (final Class<?> type : classes) {
            for (final Method method : type.getDeclaredMethods()) {
                final ClientTest marked = method.getAnnotation(ClientTest.class);
                if (marked != null) {
                    tests.add(testOf(type, method, marked.timeoutTicks(), ClientTestHelper.class));
                }
            }
        }
        register(modid, suite, tests, ClientTestHelper::new);
    }

    /** Registers {@code tests}, which a mod read itself, run with the helpers {@code helpers} makes. */
    public static void register(final String modid, final String suite, final List<Test> tests,
                                final IHelperFactory helpers) {
        SUITES.add(new Suite(Objects.requireNonNull(modid, "modid"), Objects.requireNonNull(suite, "suite"),
                List.copyOf(tests), Objects.requireNonNull(helpers, "helpers")));
    }

    /** Runs {@code reset} before every test, so what a mod keeps between tests starts as a fresh game has it. */
    public static void beforeEach(final Runnable reset) {
        BEFORE_EACH.add(Objects.requireNonNull(reset, "reset"));
    }

    /**
     * The test {@code method} of {@code type}, checked to be one: public, static, and taking one {@code helper}.
     *
     * @throws IllegalStateException when it is not
     */
    public static Test testOf(final Class<?> type, final Method method, final int timeoutTicks,
                              final Class<? extends ClientTestHelper> helper) {
        if (!Modifier.isStatic(method.getModifiers()) || !Modifier.isPublic(method.getModifiers())
                || method.getParameterCount() != 1 || method.getParameterTypes()[0] != helper) {
            throw new IllegalStateException("the client test " + type.getSimpleName() + "." + method.getName()
                    + " must be public static and take a single " + helper.getSimpleName());
        }
        return new Test(type.getSimpleName() + "." + method.getName(), helperOf -> method.invoke(null, helperOf),
                timeoutTicks);
    }

    /** Every test registered, sorted by name and numbered, the number placing its area of the world. */
    public static List<Planned> all() {
        final List<Planned> found = new ArrayList<>();
        final List<Suite> suites = new ArrayList<>(SUITES);
        suites.addAll(RecordedClientTests.suites());
        for (final Suite suite : suites) {
            for (final Test test : suite.tests()) {
                found.add(new Planned(test, 0, suite));
            }
        }
        found.sort(Comparator.comparing(Planned::name));
        final List<Planned> numbered = new ArrayList<>(found.size());
        for (int i = 0; i < found.size(); i++) {
            numbered.add(new Planned(found.get(i).test(), i, found.get(i).suite()));
        }
        return numbered;
    }

    /** The names of the suites registered, in the order they were, and then those of the tests kept as files. */
    public static Set<String> suites() {
        final Set<String> names = new LinkedHashSet<>();
        SUITES.forEach(suite -> names.add(suite.name()));
        RecordedClientTests.suites().forEach(suite -> names.add(suite.name()));
        return names;
    }

    static List<Runnable> resets() {
        return List.copyOf(BEFORE_EACH);
    }

    /** What makes the helper a test is run with. */
    @FunctionalInterface
    public interface IHelperFactory {

        ClientTestHelper create(Minecraft mc, Planned test, BlockPos origin);
    }

    /**
     * One test as a mod registers it.
     *
     * @param name         its name, its class's and its method's, or a recorded test's own
     * @param body         what builds its steps
     * @param timeoutTicks how long it may take
     */
    public record Test(String name, IBody body, int timeoutTicks) {
    }

    /** What builds a test's steps on its helper: its method, or the steps of a test kept as a file. */
    @FunctionalInterface
    public interface IBody {

        void build(ClientTestHelper helper) throws IllegalAccessException, InvocationTargetException;
    }

    /**
     * A suite of a mod's tests.
     *
     * @param modid   the mod, in whose resources its reference pictures are
     * @param name    its name, as a run asks for it
     * @param tests   its tests
     * @param helpers what makes the helper each is run with
     */
    public record Suite(String modid, String name, List<Test> tests, IHelperFactory helpers) {
    }

    /**
     * One test of a run, numbered among them all.
     *
     * @param test  the test
     * @param index its number in the whole run, by name, which places its area of the world
     * @param suite its suite
     */
    public record Planned(Test test, int index, Suite suite) {

        public String name() {
            return test.name();
        }

        public String modid() {
            return suite.modid();
        }
    }
}
