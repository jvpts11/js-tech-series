/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.sigma.ast;

import java.util.List;

/**
 * Every statement the language has.
 *
 * <p>Like the expressions, the hierarchy is closed so the stages after this one cannot quietly
 * forget a form. A statement that failed to parse is not represented at all: the parser reports it
 * and skips to the next one, so a tree never holds a hole a later stage would have to guard against.
 */
public sealed interface IStmt extends INode {

    /** A braced group with its own scope. */
    record Block(List<IStmt> statements, int line, int column) implements IStmt {
    }

    /** A conditional; {@code otherwise} is null when there is no else. */
    record If(IExpr condition, IStmt then, IStmt otherwise, int line, int column) implements IStmt {
    }

    /** A loop that tests before each pass. */
    record While(IExpr condition, IStmt body, int line, int column) implements IStmt {
    }

    /** A loop that tests after each pass. */
    record DoWhile(IStmt body, IExpr condition, int line, int column) implements IStmt {
    }

    /** The three-part loop; the condition is null when it was left out, which means forever. */
    record For(List<IStmt> initializers, IExpr condition, List<IExpr> updates, IStmt body, int line, int column)
            implements IStmt {
    }

    /** A loop over a collection or an array. */
    record ForEach(TypeRef type, String name, IExpr source, IStmt body, int line, int column) implements IStmt {
    }

    /** One group of labels and the statements they share. */
    record SwitchSection(List<IExpr> labels, boolean fallback, List<IStmt> statements, int line, int column)
            implements INode {
    }

    /** A choice between sections. */
    record Switch(IExpr value, List<SwitchSection> sections, int line, int column) implements IStmt {
    }

    /** Leaves the innermost loop or switch section. */
    record Break(int line, int column) implements IStmt {
    }

    /** Starts the innermost loop's next pass. */
    record Continue(int line, int column) implements IStmt {
    }

    /** Leaves the method; {@code value} is null in a method that returns nothing. */
    record Return(IExpr value, int line, int column) implements IStmt {
    }

    /**
     * A local variable; {@code initializer} is null when it was declared without one. A constant one, written with
     * {@code const}, holds a value known when compiling, which is written into every place that reads it.
     */
    record LocalDecl(TypeRef type, String name, IExpr initializer, boolean constant, int line, int column)
            implements IStmt {

        /** A local that can change. */
        public LocalDecl(final TypeRef type, final String name, final IExpr initializer, final int line,
                         final int column) {
            this(type, name, initializer, false, line, column);
        }
    }

    /** An expression evaluated for what it does rather than for its value. */
    record ExprStmt(IExpr expression, int line, int column) implements IStmt {
    }

    /** Frees an object and leaves the reference null. */
    record Dispose(IExpr target, int line, int column) implements IStmt {
    }

    /** Holds an object's lock for as long as the body runs, whichever way the body is left. */
    record Lock(IExpr target, IStmt body, int line, int column) implements IStmt {
    }

    /** {@code unsafe { ... }}: a block whose statements may hold pointers. */
    record Unsafe(Block body, int line, int column) implements IStmt {
    }

    /** A lone semicolon. */
    record Empty(int line, int column) implements IStmt {
    }

    /** A statement with a label in front of it, which goto, comefrom and abstainfrom name. */
    record Labeled(String label, IStmt statement, int line, int column) implements IStmt {
    }

    /**
     * A jump: to a label, to one of the cases of the switch it stands in, or to that switch's default.
     *
     * @param label     the label jumped to, or null for a case or the default
     * @param caseValue the case's value, or null for a label or the default
     */
    record Goto(Target target, String label, IExpr caseValue, int line, int column) implements IStmt {

        /** Where a goto jumps. */
        public enum Target {
            LABEL,
            CASE,
            DEFAULT
        }
    }

    /**
     * INTERCAL's COME FROM: when the statement marked {@code label} ends the ordinary way, control goes on just after
     * this one.
     */
    record ComeFrom(String label, int line, int column) implements IStmt {
    }

    /**
     * INTERCAL's ABSTAIN and REINSTATE: switches off the code from the statement marked {@code label} to the end of its
     * block, so it is passed over every time it is reached, or switches it on again.
     */
    record Abstain(String label, boolean reinstate, int line, int column) implements IStmt {
    }

    /**
     * INTERCAL's IGNORE and REMEMBER: makes every later write to a place vanish, or count again.
     *
     * @param place a local, a field or a static field
     */
    record Ignore(IExpr place, boolean remember, int line, int column) implements IStmt {
    }

    /**
     * A method declared inside a method, which reads the locals around it as a lambda does: what it does is the
     * lambda it is written as, and its name may be called anywhere in the block it stands in.
     *
     * @param body the parameters and the body, as a lambda holds them
     */
    record LocalFunction(TypeRef returnType, String name, IExpr.Lambda body, int line, int column)
            implements IStmt {
    }

    /**
     * A tuple taken apart into new locals, one for each of its places: {@code var (count, name) = Find();}.
     *
     * @param names the locals, each as the name it is written as
     */
    record Deconstruct(List<IExpr.Name> names, IExpr value, int line, int column) implements IStmt {

        public Deconstruct {
            names = List.copyOf(names);
        }
    }

    /**
     * One catch of a try.
     *
     * @param type the exceptions it takes, those of the type and of every type standing on it; null for all of them
     * @param name what the exception taken is called inside it, or null when it is not named
     */
    record Catch(TypeRef type, String name, Block body, int line, int column) implements INode {
    }

    /** A try, with its catches in the order they are tried and its finally, null when it has none. */
    record Try(Block body, List<Catch> catches, Block cleanup, int line, int column) implements IStmt {

        public Try {
            catches = List.copyOf(catches);
        }
    }

    /** Throws an exception; {@code value} is null for the one a catch took, thrown on as it was. */
    record Throw(IExpr value, int line, int column) implements IStmt {
    }
}
