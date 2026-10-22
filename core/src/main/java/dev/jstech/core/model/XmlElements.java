/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.model;

import java.io.ByteArrayInputStream;
import java.util.function.Consumer;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.jetbrains.annotations.Nullable;

/**
 * Walks the elements of an XML file one by one, as they open and close, without holding the file as a tree: a mesh
 * of a million triangles is read as it goes. Names are taken without their prefix, as a file may name its namespaces
 * as it likes; no DTD is read and nothing from outside the file is fetched. Pure.
 */
final class XmlElements {

    private final XMLStreamReader reader;

    private static final XMLInputFactory FACTORY = factory();

    private XmlElements(final XMLStreamReader reader) {
        this.reader = reader;
    }

    /**
     * Walks {@code bytes}, handing each element to {@code opened} as it opens and its name to {@code closed} as it
     * closes.
     *
     * @throws ModelFormatException when it is not XML that reads
     */
    static void walk(final String name, final byte[] bytes, final Consumer<XmlElements> opened,
                     final Consumer<String> closed) {
        try {
            final XMLStreamReader reader = FACTORY.createXMLStreamReader(new ByteArrayInputStream(bytes));
            try {
                final XmlElements element = new XmlElements(reader);
                while (reader.hasNext()) {
                    final int event = reader.next();
                    if (event == XMLStreamConstants.START_ELEMENT) {
                        opened.accept(element);
                    } else if (event == XMLStreamConstants.END_ELEMENT) {
                        closed.accept(reader.getLocalName());
                    }
                }
            } finally {
                reader.close();
            }
        } catch (final XMLStreamException broken) {
            throw new ModelFormatException("the file " + name + " is not XML that reads: " + broken.getMessage(),
                    broken);
        }
    }

    /** The open element's name, without its prefix. */
    String name() {
        return this.reader.getLocalName();
    }

    /** Its attribute {@code name}, whatever its prefix, or none. */
    @Nullable
    String attribute(final String name) {
        for (int i = 0; i < this.reader.getAttributeCount(); i++) {
            if (this.reader.getAttributeLocalName(i).equals(name)) {
                return this.reader.getAttributeValue(i);
            }
        }
        return null;
    }

    /** Its attribute {@code name}, or {@code otherwise}. */
    String attribute(final String name, final String otherwise) {
        final String value = attribute(name);
        return value == null ? otherwise : value;
    }

    /** Its attribute {@code name} as a number, or {@code otherwise}. */
    float number(final String name, final float otherwise) {
        final String value = attribute(name);
        return value == null ? otherwise : Float.parseFloat(value);
    }

    /** Its attribute {@code name} as a whole number, or {@code otherwise}. */
    int whole(final String name, final int otherwise) {
        final String value = attribute(name);
        return value == null ? otherwise : Integer.parseInt(value.strip());
    }

    private static XMLInputFactory factory() {
        final XMLInputFactory factory = XMLInputFactory.newDefaultFactory();
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        factory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, true);
        return factory;
    }
}
