/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.listing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.computers.cannon.CannonCompiler;
import dev.jstech.computers.cannon.SourceFile;
import dev.jstech.computers.vm.program.Loaded;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * A listing the compiler wrote is read back by the machine the same way the compiler meant it, for
 * programs written the way a player writes them: several files, a namespace named like a class, a
 * record, a class holding one, and an entry point reached through all of that.
 */
class ListingRoundTripTest {

    /** The program as a player wrote it: a namespace called Main with a class called Main in it. */
    private static final String HELLO_WORLD = """
            using System.IO.Console;
            using Main.Handler;

            namespace Main{
                public class Main {
                    public static void Main() {
                        Console.PrintLine("Hello World!");

                        Console.PrintLine("Type an item and it's qty \\n");
                        Handler h;
                        string name = Console.ReadLine();
                        int qty = name.Length;

                        h = new Handler(name, qty);

                        Console.PrintLine($"The amount of {name}: {h.getItem()}");
                    }
                }
            }
            """;

    private static final String CLASS_TO_INHERIT = """
            using System.IO.Console;

            namespace Main {
                public class Handler{
                    Item i;
                    public Handler(string Name, int Qty){
                        Console.PrintLine("Handler called!");
                        i = new Item(Name,Qty);
                    }

                    public Item getItem(){
                        return i;
                    }
                }

                public record Item(string Name, int Qty);
            }
            """;

    /** Reads a listing the way the machine does: a reader, and nothing when the listing has a problem. */
    private static Loaded readBack(final String assembly) {
        final AsmReader reader = new AsmReader(assembly);
        final AsmProgram program = reader.read();
        assertFalse(reader.hasProblems(), () -> "the listing reads back clean:\n" + String.join("\n", reader.problems()
                .stream().map(ListingProblem::format).toList()) + "\n--- listing ---\n" + assembly);
        return Loaded.of(program);
    }

    @Test
    void compile_thenRead_findsTheEntryPointOfANamespaceNamedLikeItsClass() {
        final CannonCompiler.Result built = CannonCompiler.compile(List.of(
                new SourceFile("Hello_World.can", HELLO_WORLD),
                new SourceFile("ClassToInherit.can", CLASS_TO_INHERIT)));
        assertTrue(built.ok(), () -> String.join("\n", built.lines()));
        final Loaded program = readBack(built.assembly());
        assertNotNull(program.entryPoint(), "the machine finds where to start");
        assertEquals("Main.Main", program.entryPoint());
    }
}
