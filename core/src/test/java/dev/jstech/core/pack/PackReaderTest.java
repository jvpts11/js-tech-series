/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.pack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.core.util.DeclarationError;
import java.util.List;
import org.junit.jupiter.api.Test;

class PackReaderTest {

    private static final String FILE = "jstech/content/mypack.json";

    @Test
    void read_givesEachDeclarationOfAListInItsOrder() {
        final List<PackDeclaration> read = PackReader.read(FILE, """
                [{"type": "block", "id": "mypack:lamp", "name": "Lamp", "light": 15, "tool": "pickaxe"},
                 {"type": "item", "id": "mypack:token", "name": "Token", "stack": 16},
                 {"type": "machine", "id": "mypack:smelter", "name": "Smelter"}]
                """);

        assertEquals(List.of("mypack:lamp", "mypack:token", "mypack:smelter"),
                read.stream().map(PackDeclaration::id).toList());
        assertEquals(new PackDeclaration.Block("mypack:lamp", "Lamp", 1.5f, 6.0f, "stone", 15, "pickaxe", "", true),
                read.getFirst());
        assertEquals(new PackDeclaration.Item("mypack:token", "Token", 16), read.get(1));
    }

    @Test
    void read_takesOneObjectAsOneDeclaration() {
        final List<PackDeclaration> read = PackReader.read(FILE, """
                {"type": "processing", "id": "mypack:smelting", "name": "Smelting", "machine": "mypack:smelter"}
                """);

        assertEquals(List.of(new PackDeclaration.Processing("mypack:smelting", "Smelting", "mypack:smelter")), read);
    }

    @Test
    void read_givesWhatIsMissingItsDefault() {
        final PackDeclaration.Item token = (PackDeclaration.Item) PackReader.read(FILE,
                "{\"type\": \"item\", \"id\": \"mypack:token\", \"name\": \"Token\"}").getFirst();
        final PackDeclaration.Machine smelter = (PackDeclaration.Machine) PackReader.read(FILE,
                "{\"type\": \"machine\", \"id\": \"mypack:smelter\", \"name\": \"Smelter\"}").getFirst();

        assertEquals(64, token.stack());
        assertEquals(3.5f, smelter.strength());
    }

    @Test
    void read_readsAMaterialsColourFormsOresAndProperties() {
        final PackDeclaration.Material mythril = (PackDeclaration.Material) PackReader.read(FILE, """
                {"type": "material", "id": "mypack:mythril", "name": "Mythril", "kind": "metal", "colour": "#7AD7FF",
                 "liquid": true, "forms": ["ingot", "jscore:dust"], "ores": ["stone"],
                 "properties": {"jscore:density": 5200}}
                """).getFirst();

        assertEquals(0x7AD7FF, mythril.colour());
        assertTrue(mythril.liquid());
        assertEquals(List.of("ingot", "jscore:dust"), mythril.forms());
        assertEquals(List.of("stone"), mythril.ores());
        assertEquals(5200, mythril.properties().get("jscore:density").getAsInt());
    }

    @Test
    void read_refusesAnIdThatIsNotNamespaceAndPath() {
        final DeclarationError error = assertThrows(DeclarationError.class, () -> PackReader.read(FILE,
                "{\"type\": \"item\", \"id\": \"Token\", \"name\": \"Token\"}"));

        assertTrue(error.getMessage().contains(FILE), error.getMessage());
        assertTrue(error.getMessage().contains("namespace:path"), error.getMessage());
    }

    @Test
    void read_refusesATypeNoPackDeclares() {
        final DeclarationError error = assertThrows(DeclarationError.class, () -> PackReader.read(FILE,
                "{\"type\": \"dimension\", \"id\": \"mypack:moon\", \"name\": \"Moon\"}"));

        assertTrue(error.getMessage().contains("block, item, material"), error.getMessage());
    }

    @Test
    void read_refusesAThingWithNoName() {
        assertThrows(DeclarationError.class, () -> PackReader.read(FILE,
                "{\"type\": \"item\", \"id\": \"mypack:token\"}"));
    }

    @Test
    void read_refusesNumbersOutsideTheirRange() {
        assertThrows(DeclarationError.class, () -> PackReader.read(FILE,
                "{\"type\": \"block\", \"id\": \"mypack:lamp\", \"name\": \"Lamp\", \"light\": 16}"));
        assertThrows(DeclarationError.class, () -> PackReader.read(FILE,
                "{\"type\": \"item\", \"id\": \"mypack:token\", \"name\": \"Token\", \"stack\": 100}"));
    }

    @Test
    void read_refusesAColourThatIsNotHex() {
        assertThrows(DeclarationError.class, () -> PackReader.read(FILE,
                "{\"type\": \"material\", \"id\": \"mypack:mythril\", \"name\": \"Mythril\", \"colour\": \"blue\"}"));
    }

    @Test
    void read_refusesASoundTheCoreDoesNotKnow() {
        final DeclarationError error = assertThrows(DeclarationError.class, () -> PackReader.read(FILE,
                "{\"type\": \"block\", \"id\": \"mypack:lamp\", \"name\": \"Lamp\", \"sound\": \"bells\"}"));

        assertTrue(error.getMessage().contains("metal"), error.getMessage());
    }

    @Test
    void read_refusesAFileThatIsNotJson() {
        assertThrows(DeclarationError.class, () -> PackReader.read(FILE, "{not json"));
    }
}
