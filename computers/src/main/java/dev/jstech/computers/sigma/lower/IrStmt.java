/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.sigma.lower;

import dev.jstech.computers.sigma.ast.IExpr;
import dev.jstech.computers.sigma.ast.IStmt;
import dev.jstech.computers.sigma.sem.IBinding;
import java.util.List;

/**
 * What a method does, after the shorter ways of writing it have been turned into the longer ones.
 *
 * <p>There are fewer of these than there are ways to write a statement, which is the point: two of the shapes a
 * player can write are shorthand for things the machine already does, and by the time anything is written down
 * they are gone. What is left the writing stage takes one at a time, with every decision already made.
 *
 * <p>The decisions are the reason this is worth having. Walking a collection has to know whether the thing
 * being walked answers to a length or to a count, and whether each turn takes its own copy of what it found;
 * holding a lock has to know how many locks a way out of the middle of it has to let go of. Those are answers
 * about the program, not about stacks, and they are all here rather than being worked out again while the
 * assembly is being written.
 */
public sealed interface IrStmt {

    /** Statements in order. */
    record Block(List<IrStmt> statements) implements IrStmt {

        public Block {
            statements = List.copyOf(statements);
        }
    }

    /**
     * A statement still read straight from what the player wrote.
     *
     * <p>Everything that holds no other statement is one of these, because there is nothing inside it left to
     * reduce. A statement that holds others is never one, or what it holds would be hidden from this stage.
     */
    record Source(IStmt written) implements IrStmt {
    }

    /**
     * A statement that held others, with the line and the column it was written at: what its own instructions came
     * from, which a debugger stops at and an exception names, since the shape it was reduced to no longer says.
     */
    record At(int line, int column, IrStmt reduced) implements IrStmt {
    }

    record If(IExpr condition, IrStmt then, IrStmt otherwise) implements IrStmt {
    }

    record While(IExpr condition, IrStmt body) implements IrStmt {
    }

    record DoWhile(IExpr condition, IrStmt body) implements IrStmt {
    }

    record For(List<IrStmt> initializers, IExpr condition, List<IExpr> updates, IrStmt body)
            implements IrStmt {

        public For {
            initializers = List.copyOf(initializers);
            updates = List.copyOf(updates);
        }
    }

    record Switch(IExpr value, List<Section> sections) implements IrStmt {

        public Switch {
            sections = List.copyOf(sections);
        }

        /**
         * One arm: the values that reach it, what it does, whether it is the arm anything else reaches, and the arm
         * as it was written, which a goto case or a goto default lands at.
         */
        public record Section(List<IExpr> labels, List<IrStmt> statements, boolean fallback,
                              IStmt.SwitchSection source) {

            public Section {
                labels = List.copyOf(labels);
                statements = List.copyOf(statements);
            }
        }
    }

    /**
     * Walking a collection, with everything the walking needs already worked out.
     *
     * <p>An array answers to a length of its own and hands over a place by number; anything else answers to a
     * count and is asked for each element by name. Which of those it is, and what the asking is written with,
     * are questions about types, and they are answered before anything is written rather than again at every
     * line that walks something.
     *
     * @param source      the collection, worked out once before the first turn
     * @param overAnArray whether it is an array, which is reached in the shorter of the two ways
     * @param holder      the type that answers the count and each element, or null when it is an array
     * @param gives       what each turn finds, as the assembly names it, or null when it is an array
     * @param walker      the variable each turn puts what it found into
     * @param copies      whether each turn takes its own copy, which it does when what it found is a value
     */
    record ForEach(IExpr source, boolean overAnArray, String holder, String gives, IBinding.Variable walker,
                   boolean copies, IrStmt body) implements IrStmt {
    }

    /**
     * A value put somewhere of its own, under a name nothing else uses.
     *
     * <p>What holding a lock needs is that letting go names the very object that was taken, whatever the body
     * did to the name it was written under, and this is how that is said once the shorthand is gone. The value
     * is also left on the stack, which is what the monitor is taken on.
     */
    record Keep(Temporary place, IExpr value) implements IrStmt {
    }

    /** Taking the lock on the object kept in that place. */
    record MonitorEnter(Temporary place) implements IrStmt {
    }

    /** Letting it go again. */
    record MonitorExit(Temporary place) implements IrStmt {
    }

    /**
     * Somewhere to put a value that the program never named.
     *
     * <p>It is only an identity: where it ends up living is the writing stage's business, since only that
     * stage knows what else is already living there. Two mentions of the same one are the same place.
     */
    final class Temporary {
    }

    /**
     * Leaving a loop or going round it again, with the number of locks that leaving lets go of.
     *
     * <p>Counted here rather than kept on a stack while the assembly is written: how many locks stand between
     * a break and the loop it breaks out of is a fact about where the break was written.
     */
    record Break(boolean continuing, int unlocks, boolean leaves) implements IrStmt {

        /** One that leaves no protected stretch. */
        public Break(final boolean continuing, final int unlocks) {
            this(continuing, unlocks, false);
        }
    }

    /**
     * Leaving the method, letting go of every lock still held on the way; one that {@code leaves} a protected stretch
     * goes out by the method's way out, so the finally blocks it leaves run.
     */
    record Return(IExpr value, int unlocks, boolean leaves) implements IrStmt {

        /** One that leaves no protected stretch. */
        public Return(final IExpr value, final int unlocks) {
            this(value, unlocks, false);
        }
    }

    /** A place a jump lands at, known by what it belongs to: a label, a comefrom, a switch's arm or a block's end. */
    record Landing(Object key) implements IrStmt {
    }

    /**
     * A jump to the landing of {@code key}, letting go of the locks taken since the statement it lands in began; one
     * that {@code leaves} a protected stretch runs the finally blocks it leaves on the way.
     */
    record Jump(Object key, int unlocks, boolean leaves) implements IrStmt {

        /** One that leaves no protected stretch. */
        public Jump(final Object key, final int unlocks) {
            this(key, unlocks, false);
        }
    }

    /**
     * A try: what it protects, its catches in the order they are tried, and its finally, null when it has none.
     *
     * @param source the try as it was written
     */
    record Try(IStmt.Try source, IrStmt body, List<Catch> catches, IrStmt cleanup) implements IrStmt {

        public Try {
            catches = List.copyOf(catches);
        }

        /**
         * One catch: the type it takes as the listing names it, the local it puts the exception in, null when it names
         * none, and what it does.
         */
        public record Catch(IStmt.Catch source, String type, IBinding.Variable variable, IrStmt body) {
        }
    }

    /**
     * A labelled statement: gotos land at its start; when its region may be switched off it is passed over to
     * {@code regionEnd} while it is; and when a comefrom takes it, its way out jumps there, unless one of the regions
     * the comefrom stands in is switched off.
     *
     * @param regionEnd the landing its region ends at, or null when nothing switches it off
     * @param endsHere  whether that landing is right after the statement, rather than at the end of a block around it
     * @param comeFrom  the jump its way out takes, or null when no comefrom takes it
     * @param guards    the regions the comefrom stands in, by their labels
     */
    record Labeled(IStmt.Labeled source, IrStmt statement, Object regionEnd, boolean endsHere, Jump comeFrom,
                   List<String> guards) implements IrStmt {

        public Labeled {
            guards = List.copyOf(guards);
        }
    }
}
