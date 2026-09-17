/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.listing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * The names that say which type something belongs to, and the fact that they are still written the same way.
 *
 * <p>Naming a type is now a different thing from naming a member of one, so the two cannot be handed over in
 * each other's place. None of that reaches the listing: what a line says is what it always said, which is what
 * lets every program already on a disk keep loading.
 */
class TypeNameTest {

    @Test
    void write_aFieldOfAnotherType_readsAsItAlwaysDid() {
        assertEquals("Tests.Monitor.counts", new IOperand.Field("Tests.Monitor", "counts").write());
    }

    /** A field of the type the method is already in names no owner, and says only itself. */
    @Test
    void write_aFieldOfTheTypeAround_namesNoOwner() {
        assertEquals("counts", new IOperand.Field((String) null, "counts").write());
        assertNull(new IOperand.Field((String) null, "counts").owner());
    }

    @Test
    void write_aMethod_readsAsItAlwaysDid() {
        assertEquals("string.Concat(string, int) -> string",
                new IOperand.Method("string", "Concat", List.of("string", "int"), "string").write());
    }

    @Test
    void write_aConstructorAndAType_readAsTheyAlwaysDid() {
        assertEquals("Tests.Point(int, int)",
                new IOperand.Constructor("Tests.Point", List.of("int", "int")).write());
        assertEquals("Tests.Point", new IOperand.Type("Tests.Point").write());
    }

    /**
     * A name of nothing is refused where it is made, rather than written into a listing nothing can answer.
     *
     * <p>The one thing it was ever worth confusing a type name with is a member name, and the pair now differ
     * by type; what is left to guard against is a name that is not a name at all.
     */
    @Test
    void construct_aNameThatNamesNothing_isRefused() {
        assertThrows(IllegalArgumentException.class, () -> new TypeName(""));
        assertThrows(IllegalArgumentException.class, () -> new TypeName("   "));
        assertThrows(NullPointerException.class, () -> new TypeName(null));
    }

    /** Nothing stays nothing, since a field of the type around it has no owner to name. */
    @Test
    void of_nothing_staysNothing() {
        assertNull(TypeName.of(null));
        assertEquals("int", TypeName.of("int").value());
    }
}
