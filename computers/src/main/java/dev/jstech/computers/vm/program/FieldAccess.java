/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.program;

import dev.jstech.computers.vm.listing.AsmType;
import dev.jstech.computers.vm.listing.IOperand;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Reading and writing a program's fields: an object's own, and the ones a type keeps for itself.
 *
 * <p>A type's static fields live here, one holder per type, made the first time the type is touched, and are written
 * down with the program. A field of the program's own is read and written on its object with nothing looked up, which
 * was settled when the program loaded. A value the process or the language's core answers (the thread asking, the
 * program's arguments, the length of a text, what a widget shows) is read and written through what the program loaded
 * with, and what the machine keeps is asked of it.
 */
@TextHolder
final class FieldAccess {

    private final Process process;
    private final Heap heap;
    private final ProgramImage program;
    private final Map<String, Values.Obj> statics = new LinkedHashMap<>();
    private final Map<String, Values.Obj> view = Collections.unmodifiableMap(this.statics);

    /*
     * The three below are said the same way wherever a program reaches for a member that is not there, the values the
     * process answers and the widgets included, so each is declared once here and shared across the package.
     */
    /** A type, or a thing of one, that has no member by that name. */
    static final TextKey HAS_NO = TextKey.of("jsc.vm.field_access.has_no", "%s has no %s");
    /** A value read from something that is not an object. */
    static final TextKey NOTHING_TO_READ = TextKey.of("jsc.vm.field_access.nothing_to_read",
            "there is no %s to read here");
    /** A value written on something that is not an object. */
    static final TextKey NOTHING_TO_WRITE_ON = TextKey.of("jsc.vm.field_access.nothing_to_write_on",
            "there is no object to write %s on");

    FieldAccess(final Process process, final Heap heap, final ProgramImage program) {
        this.process = process;
        this.heap = heap;
        this.program = program;
    }

    /** Reads a field of the object on top of the stack and puts what it holds there instead. */
    void load(final Frame frame, final ProgramImage.ValueSite site, final int line) {
        final Object target = this.heap.alive(frame.pop(), line);
        final String name = site.field().name();
        if (site.handled() != null) {
            frame.push(site.handled().read().read(this.process, target, line));
            return;
        }
        if (site.world() >= 0 && this.process.worldCalls().binds(site.world())) {
            // Whether another program still runs is the machine's to say, not a field to go stale.
            final Object answer = this.process.worldCalls().answer(site.world(), target, WorldCalls.NOTHING, line);
            frame.push(this.heap.adopt(answer, line));
            return;
        }
        if (!(target instanceof Values.Obj object)) {
            throw new Halt(Halt.Reason.NO_SUCH_MEMBER, line, NOTHING_TO_READ.with(name));
        }
        if (site.own()) {
            frame.push(object.get(name));
            return;
        }
        if (site.world() >= 0 && "Process".equals(object.type())) {
            // Off any machine, the one program a program knows is going is itself, and none has ended with a code.
            final boolean itself = Integer.valueOf(this.process.machineId()).equals(object.get("Id"));
            frame.push("Running".equals(name) ? itself : 0);
            return;
        }
        frame.push(object.get(name));
    }

    /** Writes the value on top of the stack into a field of the object beneath it. */
    void store(final Frame frame, final ProgramImage.ValueSite site, final int line) {
        final String name = site.field().name();
        final Object value = frame.pop();
        final Object target = this.heap.alive(frame.pop(), line);
        if (site.handled() != null && site.handled().write() != null) {
            site.handled().write().write(this.process, target, value, line);
            return;
        }
        if (!(target instanceof Values.Obj object)) {
            throw new Halt(Halt.Reason.NO_OBJECT, line, NOTHING_TO_WRITE_ON.with(name));
        }
        object.set(name, value);
    }

    /** Reads a field a type keeps for itself, one of an enum's values, or one the runtime answers. */
    void loadStatic(final Frame frame, final ProgramImage.ValueSite site, final int line) {
        final IOperand.Field field = site.field();
        if (site.handled() != null) {
            frame.push(site.handled().read().read(this.process, null, line));
            return;
        }
        if (site.world() >= 0 && this.process.worldCalls().binds(site.world())) {
            final Object answer = this.process.worldCalls().answer(site.world(), null, WorldCalls.NOTHING, line);
            frame.push(this.heap.adopt(answer, line));
            return;
        }
        final TypeImage type = this.program.type(field.owner().value());
        if (type == null) {
            // A value of the world that the machine the program runs on does not answer.
            throw new Halt(Halt.Reason.NO_SUCH_MEMBER, line, HAS_NO.with(field.owner(), field.name()));
        }
        if (type.kind() == AsmType.Kind.ENUM) {
            frame.push(type.values().get(field.name()));
            return;
        }
        frame.push(this.statics(field.owner().value()).get(field.name()));
    }

    /** Writes the value on top of the stack into a field a type keeps for itself. */
    void storeStatic(final Frame frame, final ProgramImage.ValueSite site, final int line) {
        this.statics(site.field().owner().value()).set(site.field().name(), frame.pop());
    }

    /** The holder of a type's static fields, made the first time the type is touched. */
    Values.Obj statics(final String owner) {
        return this.statics.computeIfAbsent(owner, Values.Obj::new);
    }

    /** Every holder made so far, by type, in the order they were made, for the save. */
    Map<String, Values.Obj> statics() {
        return this.view;
    }
}
