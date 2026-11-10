/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.sigma.sem;

import dev.jstech.computers.sigma.SigmaError;
import dev.jstech.computers.sigma.ast.IExpr;
import dev.jstech.computers.sigma.ast.INode;
import dev.jstech.computers.sigma.ast.IStmt;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * The labels of one body, and every goto, comefrom, abstainfrom and reinstate that names one
 * (computers/docs/design/sigma.md).
 *
 * <p>Each statement is placed by its path: the statements around it, and where it stands in each. That is what the
 * rules ask about. A goto may leave a block, never enter one, so the place a label stands in has to be around the goto;
 * a comefrom is the same rule backwards, so the place it stands in has to be around its label; and a region switched
 * off by abstainfrom runs from its label to the end of the block it stands in. A lambda's body is a body of its own,
 * with labels of its own.
 */
@TextHolder
final class Labels {

    private final BodyScope scope;
    private final Map<String, IStmt.Labeled> byName = new LinkedHashMap<>();
    /** Where each label, goto and comefrom stands: the statements around it and where it stands in each. */
    private final Map<INode, List<Step>> paths = new IdentityHashMap<>();
    /** The switch each goto case or goto default stands in, the nearest one around it. */
    private final Map<IStmt.Goto, IStmt.Switch> switches = new IdentityHashMap<>();
    private final List<IStmt.Goto> gotos = new ArrayList<>();
    private final List<IStmt.ComeFrom> comeFroms = new ArrayList<>();
    private final List<IStmt.Abstain> abstains = new ArrayList<>();
    private final Deque<Step> walking = new ArrayDeque<>();
    private final Deque<IStmt.Switch> around = new ArrayDeque<>();
    /** The finally blocks of the body, which no jump leaves: only their own end does. */
    private final Set<INode> finallies = Collections.newSetFromMap(new IdentityHashMap<>());

    /** What a goto case names that the switch around it lacks. */
    private static final TextKey CASE_WITH_THAT_VALUE = TextKey.of("jsc.sigma.labels.case_with_that_value",
            "case with that value");

    /** One statement around another, and where in it the other stands: a block's place, or a branch's. */
    record Step(INode container, int index) {
    }

    private Labels(final BodyScope scope) {
        this.scope = scope;
    }

    /** Reads a body's labels and the statements that name them, reporting a label written twice. */
    static Labels of(final IStmt.Block body, final BodyScope scope) {
        final Labels labels = new Labels(scope);
        if (body != null) {
            labels.walkAll(body, body.statements());
        }
        return labels;
    }

    /** The labels an abstainfrom or a reinstate names, whose regions may be switched off when the program runs. */
    Set<String> switchable() {
        final Set<String> named = new HashSet<>();
        for (final IStmt.Abstain switched : this.abstains) {
            named.add(switched.label());
        }
        return named;
    }

    /**
     * Works out where every goto and comefrom goes, once the body is checked so a case's value is known, and reports
     * one that goes nowhere it may.
     */
    void resolve() {
        for (final IStmt.Abstain switched : this.abstains) {
            final IStmt.Labeled labeled = this.byName.get(switched.label());
            if (labeled == null) {
                this.scope.report(switched.line(), switched.column(), SigmaError.NO_SUCH_LABEL, switched.label());
            } else {
                this.scope.model().setAbstainable(labeled);
            }
        }
        for (final IStmt.Goto jump : this.gotos) {
            this.resolve(jump);
        }
        final Set<IStmt.Labeled> taken = Collections.newSetFromMap(new IdentityHashMap<>());
        for (final IStmt.ComeFrom from : this.comeFroms) {
            this.resolve(from, taken);
        }
    }

    private void resolve(final IStmt.Goto jump) {
        if (jump.target() == IStmt.Goto.Target.LABEL) {
            final IStmt.Labeled labeled = this.byName.get(jump.label());
            if (labeled == null) {
                this.scope.report(jump.line(), jump.column(), SigmaError.NO_SUCH_LABEL, jump.label());
                return;
            }
            final INode holder = containerOf(labeled);
            if (!containers(this.paths.get(jump)).contains(holder)) {
                this.scope.report(jump.line(), jump.column(), SigmaError.LABEL_OUT_OF_REACH, jump.label());
                return;
            }
            if (this.leavesFinally(this.paths.get(jump), containers(this.paths.get(labeled)))) {
                this.scope.report(jump.line(), jump.column(), SigmaError.FINALLY_EXIT);
                return;
            }
            this.scope.model().setJump(jump, new SemanticModel.Jump(labeled, holder));
            return;
        }
        final IStmt.Switch choice = this.switches.get(jump);
        final boolean toCase = jump.target() == IStmt.Goto.Target.CASE;
        if (choice == null) {
            this.scope.report(jump.line(), jump.column(), SigmaError.NO_SWITCH, toCase ? "case" : "default");
            return;
        }
        final IStmt.SwitchSection section = toCase ? this.sectionOf(choice, jump.caseValue())
                : choice.sections().stream().filter(IStmt.SwitchSection::fallback).findFirst().orElse(null);
        if (section == null) {
            this.scope.report(jump.line(), jump.column(), SigmaError.NO_SUCH_CASE,
                    toCase ? CASE_WITH_THAT_VALUE.text() : Text.literal("default"));
            return;
        }
        if (this.leavesFinally(this.paths.get(jump), landsAmong(this.paths.get(jump), choice, section))) {
            this.scope.report(jump.line(), jump.column(), SigmaError.FINALLY_EXIT);
            return;
        }
        this.scope.model().setJump(jump, new SemanticModel.Jump(section, choice));
    }

    /* The section of the switch whose case has the value the goto names, which it knows when compiling. */
    private IStmt.SwitchSection sectionOf(final IStmt.Switch choice, final IExpr value) {
        final ITypeSymbol type = this.scope.model().typeOf(choice.value());
        final Constants.Known wanted = this.known(value, type);
        if (wanted == null) {
            return null;
        }
        for (final IStmt.SwitchSection section : choice.sections()) {
            for (final IExpr label : section.labels()) {
                final Constants.Known found = this.known(label, type);
                if (found != null && Objects.equals(found.value(), wanted.value())) {
                    return section;
                }
            }
        }
        return null;
    }

    private Constants.Known known(final IExpr value, final ITypeSymbol type) {
        final Constants.Known found = this.scope.constants().fold(value);
        return found == null || type == null ? found : Constants.converted(found, type);
    }

    /*
     * A comefrom takes the way out of the statement its label marks, so it stands in a block around that label, as a
     * goto at the label's end would have to; and a label is taken by one comefrom at most.
     */
    private void resolve(final IStmt.ComeFrom from, final Set<IStmt.Labeled> taken) {
        final IStmt.Labeled labeled = this.byName.get(from.label());
        if (labeled == null) {
            this.scope.report(from.line(), from.column(), SigmaError.NO_SUCH_LABEL, from.label());
            return;
        }
        final List<Step> place = this.paths.get(from);
        final INode holder = place.getLast().container();
        if (!containers(this.paths.get(labeled)).contains(holder)) {
            this.scope.report(from.line(), from.column(), SigmaError.COMEFROM_OUT_OF_REACH, from.label());
            return;
        }
        if (!taken.add(labeled)) {
            this.scope.report(from.line(), from.column(), SigmaError.COMEFROM_TWICE, from.label());
            return;
        }
        // Its jump goes from the label's end to it, so a label in a finally it does not stand in is one left.
        if (this.leavesFinally(this.paths.get(labeled), containers(place))) {
            this.scope.report(from.line(), from.column(), SigmaError.FINALLY_EXIT);
            return;
        }
        this.scope.model().setJump(from, new SemanticModel.Jump(labeled, holder));
        this.scope.model().setComeFrom(labeled, from, this.regionsAround(place));
    }

    /* The switched-off regions a statement at that place stands in, by their labels. */
    private List<String> regionsAround(final List<Step> place) {
        final List<String> regions = new ArrayList<>();
        for (final IStmt.Abstain switched : this.abstains) {
            final IStmt.Labeled labeled = this.byName.get(switched.label());
            if (labeled == null || regions.contains(switched.label())) {
                continue;
            }
            final Step start = this.paths.get(labeled).getLast();
            final boolean toTheEnd = start.container() instanceof IStmt.Block
                    || start.container() instanceof IStmt.SwitchSection;
            for (final Step step : place) {
                if (step.container() == start.container()
                        && (toTheEnd ? step.index() >= start.index() : step.index() == start.index())) {
                    regions.add(switched.label());
                    break;
                }
            }
        }
        return regions;
    }

    /* The statement a label stands in: a block, a switch's section, or the statement whose branch it is. */
    private INode containerOf(final IStmt.Labeled labeled) {
        return this.paths.get(labeled).getLast().container();
    }

    private static Set<INode> containers(final List<Step> place) {
        final Set<INode> found = Collections.newSetFromMap(new IdentityHashMap<>());
        for (final Step step : place) {
            found.add(step.container());
        }
        return found;
    }

    private void walkAll(final INode container, final List<IStmt> statements) {
        for (int i = 0; i < statements.size(); i++) {
            this.walk(statements.get(i), container, i);
        }
    }

    private void walk(final IStmt statement, final INode container, final int index) {
        if (statement == null) {
            return;
        }
        this.walking.addLast(new Step(container, index));
        switch (statement) {
            case IStmt.Labeled labeled -> {
                this.paths.put(labeled, List.copyOf(this.walking));
                if (this.byName.putIfAbsent(labeled.label(), labeled) != null) {
                    this.scope.report(labeled.line(), labeled.column(), SigmaError.LABEL_TWICE, labeled.label());
                }
                this.walk(labeled.statement(), labeled, 0);
            }
            case IStmt.Goto jump -> {
                this.paths.put(jump, List.copyOf(this.walking));
                this.gotos.add(jump);
                if (!this.around.isEmpty()) {
                    this.switches.put(jump, this.around.peek());
                }
            }
            case IStmt.ComeFrom from -> {
                this.paths.put(from, List.copyOf(this.walking));
                this.comeFroms.add(from);
            }
            case IStmt.Abstain switched -> this.abstains.add(switched);
            case IStmt.Block block -> this.walkAll(block, block.statements());
            case IStmt.If branch -> {
                this.walk(branch.then(), branch, 0);
                this.walk(branch.otherwise(), branch, 1);
            }
            case IStmt.While loop -> this.walk(loop.body(), loop, 0);
            case IStmt.DoWhile loop -> this.walk(loop.body(), loop, 0);
            case IStmt.For loop -> this.walk(loop.body(), loop, 0);
            case IStmt.ForEach loop -> this.walk(loop.body(), loop, 0);
            case IStmt.Lock held -> this.walk(held.body(), held, 0);
            case IStmt.Unsafe unsafe -> this.walk(unsafe.body(), unsafe, 0);
            case IStmt.Switch choice -> {
                this.around.push(choice);
                for (final IStmt.SwitchSection section : choice.sections()) {
                    this.walkAll(section, section.statements());
                }
                this.around.pop();
            }
            case IStmt.Try guarded -> {
                this.walk(guarded.body(), guarded, 0);
                for (int i = 0; i < guarded.catches().size(); i++) {
                    this.walk(guarded.catches().get(i).body(), guarded, i + 1);
                }
                if (guarded.cleanup() != null) {
                    this.finallies.add(guarded.cleanup());
                    this.walk(guarded.cleanup(), guarded, guarded.catches().size() + 1);
                }
            }
            default -> { }
        }
        this.walking.removeLast();
    }

    /* Whether a jump from a statement at {@code from} to one standing in {@code to} leaves a finally on the way. */
    private boolean leavesFinally(final List<Step> from, final Set<INode> to) {
        for (final Step step : from) {
            if (this.finallies.contains(step.container()) && !to.contains(step.container())) {
                return true;
            }
        }
        return false;
    }

    /* The statements a goto case or a goto default lands among: those around the switch, and its section's. */
    private static Set<INode> landsAmong(final List<Step> from, final IStmt.Switch choice,
                                         final IStmt.SwitchSection section) {
        final Set<INode> found = Collections.newSetFromMap(new IdentityHashMap<>());
        for (final Step step : from) {
            found.add(step.container());
            if (choice.sections().contains(step.container())) {
                break;
            }
        }
        found.add(section);
        return found;
    }
}
