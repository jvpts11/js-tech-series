/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.gui.screen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.List;

/**
 * The JSON schema of a screen's file, so an editor completes one as it is typed: the screen's own fields, and for a
 * component the kinds there are, the fields every component has and those of one that holds others, and the fields of
 * each kind once its {@code type} is written. Made from the components' codecs by the data generation, so it never
 * falls behind them; a field's value is left free, since a codec says the names of its fields and not the shapes of
 * their values. Pure.
 */
public final class ScreenSchema {

    /** The schema's own address, in the version of JSON schema editors read the most. */
    public static final String DRAFT = "http://json-schema.org/draft-07/schema#";

    private static final String TYPE = "type";
    private static final String PROPERTIES = "properties";
    private static final String ELEMENT = "#/definitions/element";

    private ScreenSchema() {
    }

    /**
     * The schema of a screen's file holding components of {@code kinds}, each component writing {@code common} and,
     * when it holds others, {@code layout}.
     */
    public static JsonObject of(final List<Kind> kinds, final List<String> common, final List<String> layout) {
        final JsonObject schema = new JsonObject();
        schema.addProperty("$schema", DRAFT);
        schema.addProperty(TYPE, "object");
        final JsonObject top = new JsonObject();
        top.add("$schema", typed("string"));
        final JsonObject width = typed("integer");
        width.addProperty("minimum", 32);
        width.addProperty("maximum", 1024);
        top.add("width", width);
        top.add("title", typed("string"));
        top.add("style", typed("string"));
        top.add("root", reference());
        schema.add(PROPERTIES, top);
        schema.add("required", names(List.of("root")));
        final JsonObject definitions = new JsonObject();
        definitions.add("element", element(kinds, common, layout));
        schema.add("definitions", definitions);
        return schema;
    }

    /* A component: its kind, the fields of every one, and each kind's own once it is written. */
    private static JsonObject element(final List<Kind> kinds, final List<String> common, final List<String> layout) {
        final JsonObject element = typed("object");
        final JsonObject fields = new JsonObject();
        final JsonObject type = typed("string");
        type.add("enum", names(kinds.stream().map(Kind::name).toList()));
        fields.add(TYPE, type);
        common.forEach(name -> fields.add(name, new JsonObject()));
        layout.forEach(name -> fields.add(name, new JsonObject()));
        final JsonObject children = typed("array");
        children.add("items", reference());
        fields.add("children", children);
        element.add(PROPERTIES, fields);
        element.add("required", names(List.of(TYPE)));
        final JsonArray each = new JsonArray();
        for (final Kind kind : kinds) {
            final JsonObject named = new JsonObject();
            final JsonObject is = new JsonObject();
            final JsonObject constant = new JsonObject();
            constant.addProperty("const", kind.name());
            is.add(TYPE, constant);
            final JsonObject condition = new JsonObject();
            condition.add(PROPERTIES, is);
            named.add("if", condition);
            final JsonObject own = new JsonObject();
            kind.fields().forEach(name -> own.add(name, new JsonObject()));
            final JsonObject then = new JsonObject();
            then.add(PROPERTIES, own);
            named.add("then", then);
            each.add(named);
        }
        element.add("allOf", each);
        return element;
    }

    private static JsonObject typed(final String type) {
        final JsonObject typed = new JsonObject();
        typed.addProperty(TYPE, type);
        return typed;
    }

    private static JsonObject reference() {
        final JsonObject reference = new JsonObject();
        reference.addProperty("$ref", ELEMENT);
        return reference;
    }

    private static JsonArray names(final List<String> names) {
        final JsonArray array = new JsonArray();
        names.forEach(array::add);
        return array;
    }

    /**
     * A kind of component as the schema says it.
     *
     * @param name   its {@code type} as a file writes it
     * @param fields its own fields' names
     * @param holds  whether it holds other components
     */
    public record Kind(String name, List<String> fields, boolean holds) {

        public Kind {
            fields = List.copyOf(fields);
        }
    }
}
