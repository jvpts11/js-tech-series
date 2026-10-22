/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.script;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

/**
 * The scripts' view of the series written for an editor: a TypeScript declaration file of every script binding's
 * calls ({@code JsCore.teamOf(...)}) and of every group of script events with what each event holds
 * ({@code JsCoreEvents.knowledgeGained(event => ...)}), so a script editor completes them and checks their names.
 * Read from the classes themselves, so it never falls behind them; {@code /jstech dump typings} writes it. What the
 * scripts see of the game's own classes is left to the script mod's own typings, as {@code any} here.
 */
public final class ScriptTypings {

    /** The groups of script events a script mod declares, handed over by its plugin. */
    private static final List<Supplier<List<EventGroup>>> EVENT_GROUPS = new CopyOnWriteArrayList<>();
    private static final Map<Class<?>, String> SIMPLE = simple();
    private static final String ANY = "any";
    private static final String GETTER = "get";
    private static final String IS = "is";
    private static final String INDENT = "    ";

    private ScriptTypings() {
    }

    /** Hands over the groups of script events a script mod's plugin declares, read when the typings are written. */
    public static void events(final Supplier<List<EventGroup>> groups) {
        EVENT_GROUPS.add(groups);
    }

    /** Every group of events handed over. */
    public static List<EventGroup> events() {
        final List<EventGroup> out = new ArrayList<>();
        for (final Supplier<List<EventGroup>> groups : EVENT_GROUPS) {
            out.addAll(groups.get());
        }
        return out;
    }

    /** The declaration file of {@code bindings}, by their names in the scripts, and of {@code groups}. */
    public static String declarations(final Map<String, Class<?>> bindings, final List<EventGroup> groups) {
        final StringBuilder out = new StringBuilder();
        out.append("// The J's Tech Series' script bindings and events, for an editor to complete scripts with.\n")
                .append("// Written by /jstech dump typings from the classes themselves.\n\n")
                .append("interface JavaOptional<T> {\n").append(INDENT).append("isPresent(): boolean;\n")
                .append(INDENT).append("isEmpty(): boolean;\n").append(INDENT).append("get(): T;\n")
                .append(INDENT).append("orElse(other: T): T;\n}\n");
        for (final Map.Entry<String, Class<?>> binding : sorted(bindings)) {
            out.append("\ndeclare const ").append(binding.getKey()).append(": {\n");
            for (final Method method : methods(binding.getValue(), true)) {
                out.append(INDENT).append(signature(method)).append(";\n");
            }
            out.append("};\n");
        }
        for (final EventGroup group : groups) {
            out.append("\ndeclare namespace ").append(group.name()).append(" {\n");
            final List<Class<?>> written = new ArrayList<>();
            for (final Event event : group.events()) {
                if (!written.contains(event.type())) {
                    written.add(event.type());
                    out.append(eventInterface(event.type()));
                }
            }
            for (final Event event : group.events()) {
                out.append(INDENT).append("function ").append(event.name()).append("(handler: (event: ")
                        .append(event.type().getSimpleName()).append(") => void): void;\n");
            }
            out.append("}\n");
        }
        return out.toString();
    }

    /** The TypeScript type of {@code type}: the scripts' own kinds of value, lists, maps and optionals of them. */
    public static String typeOf(final Type type) {
        if (type instanceof Class<?> plain) {
            if (plain.isArray()) {
                return typeOf(plain.getComponentType()) + "[]";
            }
            return SIMPLE.getOrDefault(plain, Collection.class.isAssignableFrom(plain) ? "any[]"
                    : Map.class.isAssignableFrom(plain) ? "Record<string, any>" : ANY);
        }
        if (type instanceof ParameterizedType generic && generic.getRawType() instanceof Class<?> raw) {
            final Type[] args = generic.getActualTypeArguments();
            if (Collection.class.isAssignableFrom(raw) && args.length == 1) {
                return element(args[0]) + "[]";
            }
            if (Map.class.isAssignableFrom(raw) && args.length == 2) {
                return "Record<" + (args[0] == String.class ? "string" : ANY) + ", " + typeOf(args[1]) + ">";
            }
            if (raw == Optional.class && args.length == 1) {
                return "JavaOptional<" + typeOf(args[0]) + ">";
            }
            return typeOf(raw);
        }
        return ANY;
    }

    /* An event's interface: each getter, and the property a script reads it by. */
    private static String eventInterface(final Class<?> type) {
        final StringBuilder out = new StringBuilder();
        out.append(INDENT).append("interface ").append(type.getSimpleName()).append(" {\n");
        for (final Method method : methods(type, false)) {
            out.append(INDENT).append(INDENT).append(signature(method)).append(";\n");
            property(method).ifPresent(name -> out.append(INDENT).append(INDENT).append("readonly ").append(name)
                    .append(": ").append(typeOf(method.getGenericReturnType())).append(";\n"));
        }
        return out.append(INDENT).append("}\n").toString();
    }

    /* The public methods of {@code type}, static ones or those of an instance, in a steady order. */
    private static List<Method> methods(final Class<?> type, final boolean statics) {
        return Arrays.stream(type.getMethods()).filter(method -> Modifier.isStatic(method.getModifiers()) == statics)
                .filter(method -> method.getDeclaringClass() != Object.class && !method.isSynthetic())
                .sorted(Comparator.comparing(Method::getName).thenComparingInt(Method::getParameterCount)
                        .thenComparing(Method::toString)).toList();
    }

    /* A method as TypeScript writes it: its name, its parameters named after their kinds, and what it gives. */
    private static String signature(final Method method) {
        final StringBuilder out = new StringBuilder(method.getName()).append('(');
        final Parameter[] parameters = method.getParameters();
        final Map<String, Integer> used = new HashMap<>();
        for (int i = 0; i < parameters.length; i++) {
            if (i > 0) {
                out.append(", ");
            }
            out.append(name(parameters[i], used)).append(": ").append(typeOf(parameters[i].getParameterizedType()));
        }
        return out.append("): ").append(typeOf(method.getGenericReturnType())).toString();
    }

    /* A parameter's name: its own when the class kept it, else its kind's, numbered when the kind comes again. */
    private static String name(final Parameter parameter, final Map<String, Integer> used) {
        final String base = parameter.isNamePresent() ? parameter.getName() : decapitalised(parameter.getType()
                .getSimpleName().replace("[]", "s"));
        final int seen = used.merge(base, 1, Integer::sum);
        return seen == 1 ? base : base + seen;
    }

    /* The property a getter is read by: {@code getTeam} as {@code team}, {@code isCancelled} as {@code cancelled}. */
    private static Optional<String> property(final Method method) {
        final String name = method.getName();
        if (method.getParameterCount() != 0 || method.getReturnType() == void.class) {
            return Optional.empty();
        }
        if (name.startsWith(GETTER) && name.length() > GETTER.length()) {
            return Optional.of(decapitalised(name.substring(GETTER.length())));
        }
        if (name.startsWith(IS) && name.length() > IS.length() && method.getReturnType() == boolean.class) {
            return Optional.of(decapitalised(name.substring(IS.length())));
        }
        return Optional.empty();
    }

    private static String element(final Type type) {
        final String one = typeOf(type);
        return one.contains(" ") ? "(" + one + ")" : one;
    }

    /* A name with its first word in lower case, a run of capitals as one: {@code UUID} as {@code uuid}. */
    private static String decapitalised(final String word) {
        int capitals = 0;
        while (capitals < word.length() && Character.isUpperCase(word.charAt(capitals))) {
            capitals++;
        }
        if (capitals == 0) {
            return word;
        }
        // In UUIDList the run's last capital starts the next word.
        final int lower = capitals == word.length() || capitals == 1 ? capitals : capitals - 1;
        return word.substring(0, lower).toLowerCase(Locale.ROOT) + word.substring(lower);
    }

    private static List<Map.Entry<String, Class<?>>> sorted(final Map<String, Class<?>> bindings) {
        return bindings.entrySet().stream().sorted(Map.Entry.comparingByKey()).toList();
    }

    private static Map<Class<?>, String> simple() {
        final Map<Class<?>, String> out = new HashMap<>();
        for (final Class<?> number : List.of(int.class, long.class, double.class, float.class, short.class, byte.class,
                Integer.class, Long.class, Double.class, Float.class, Short.class, Byte.class, Number.class)) {
            out.put(number, "number");
        }
        out.put(boolean.class, "boolean");
        out.put(Boolean.class, "boolean");
        out.put(String.class, "string");
        out.put(CharSequence.class, "string");
        out.put(char.class, "string");
        out.put(Character.class, "string");
        out.put(void.class, "void");
        out.put(Void.class, "void");
        return Map.copyOf(out);
    }

    /**
     * A group of script events, as scripts write it ({@code JsCoreEvents}).
     *
     * @param name   its name in the scripts
     * @param events its events, in order
     */
    public record EventGroup(String name, List<Event> events) {

        public EventGroup {
            events = List.copyOf(events);
        }
    }

    /**
     * One event of a group.
     *
     * @param name its name in the scripts ({@code knowledgeGained})
     * @param type the class a handler is given
     */
    public record Event(String name, Class<?> type) {
    }
}
