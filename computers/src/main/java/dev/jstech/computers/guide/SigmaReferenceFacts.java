/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.guide;

import dev.jstech.computers.sigma.LanguageLevel;
import dev.jstech.computers.sigma.SigmaVersions;
import dev.jstech.computers.vm.system.ConstructorSpec;
import dev.jstech.computers.vm.system.EventSpec;
import dev.jstech.computers.vm.system.IMemberSpec;
import dev.jstech.computers.vm.system.MethodSpec;
import dev.jstech.computers.vm.system.PropertySpec;
import dev.jstech.computers.vm.system.SystemApi;
import dev.jstech.computers.vm.system.TypeSpec;
import dev.jstech.core.guide.GuideBlock;
import dev.jstech.core.guide.GuideFacts;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * The tables of the Sigma reference, read from the library itself: for each namespace, every type and each of its
 * calls, what the call gives back, what it costs in credits beyond the instruction that makes it, and the version of
 * the language it came in. Written from the code, they cannot fall behind it.
 */
public final class SigmaReferenceFacts {

    /** The id a namespace's table is read under starts with this, then the namespace in lower case. */
    private static final String PREFIX = "jsc:sigma_library_";

    private SigmaReferenceFacts() {
    }

    /** Says where the manual reads these tables from: once, as the manual is declared. */
    public static void declare() {
        for (final String namespace : namespaces()) {
            GuideFacts.rows(of(namespace), () -> rows(namespace));
        }
    }

    /** The namespaces of the library, in the order it declares them. */
    public static List<String> namespaces() {
        final Set<String> found = new LinkedHashSet<>();
        for (final TypeSpec type : SystemApi.types()) {
            found.add(type.namespace());
        }
        return List.copyOf(found);
    }

    /** The id the table of a namespace is read under. */
    public static String of(final String namespace) {
        return PREFIX + pathOf(namespace);
    }

    /** A namespace as a path the manual's ids can hold: {@code system_io} for {@code System.IO}. */
    public static String pathOf(final String namespace) {
        return namespace.toLowerCase(Locale.ROOT).replace('.', '_');
    }

    /* A row for each call of each type of the namespace: the call, what it gives, its price and its version. */
    private static List<GuideBlock.TableRow> rows(final String namespace) {
        final List<GuideBlock.TableRow> rows = new ArrayList<>();
        for (final TypeSpec type : SystemApi.types()) {
            if (!type.namespace().equals(namespace)) {
                continue;
            }
            for (final IMemberSpec member : type.members()) {
                final String takes = String.join(", ", member.id().parameters());
                final int since = SigmaVersions.sinceMember(LanguageLevel.SIGMA_SHARP, type.name(),
                        member.id().name(), takes);
                rows.add(new GuideBlock.TableRow(List.of(new GuideBlock.GuideValue.Literal(called(type, member)),
                        new GuideBlock.GuideValue.Literal(gives(member)),
                        new GuideBlock.GuideValue.Said(member.cost().text()),
                        new GuideBlock.GuideValue.Literal(String.valueOf(since)))));
            }
        }
        return rows;
    }

    /* How a call is written: Type.Name(what it takes), a value without brackets, a constructor with new. */
    private static String called(final TypeSpec type, final IMemberSpec member) {
        final String takes = String.join(", ", member.id().parameters());
        return switch (member) {
            case ConstructorSpec ignored -> "new " + type.name() + "(" + takes + ")";
            case PropertySpec ignored -> type.name() + "." + member.id().name();
            case EventSpec ignored -> type.name() + "." + member.id().name();
            case MethodSpec ignored -> type.name() + "." + member.id().name() + "(" + takes + ")";
        };
    }

    /* What a call gives back: its type, the type of a value, or the handler an event takes. */
    private static String gives(final IMemberSpec member) {
        return switch (member) {
            case ConstructorSpec constructor -> constructor.id().owner();
            case PropertySpec property -> property.type();
            case EventSpec event -> event.handler();
            case MethodSpec method -> method.returns();
        };
    }
}
