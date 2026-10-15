/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.diagnostic;

import dev.jstech.core.log.CoreLogs;
import java.lang.invoke.MethodHandles;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.slf4j.Logger;

/*
 * Makes the frame each section's work runs in: a small class of its own, named after the section, whose one method
 * runs the work. A sampling profiler reads the threads' stacks, so Spark, Java Flight Recorder or VisualVM show the
 * section by name, inside the frames of whatever called it, without the Core knowing which profiler is there or the
 * profiler knowing the Core. The class is an ordinary one, since the frames of a hidden class are left out of stacks.
 *
 * Its name is the section's, in the package of this class: jscore:energy_grid runs in Section$jscore$energy_grid,
 * whose source file reads as the section's id, so a stack trace shows
 * Section$jscore$energy_grid.run(jscore:energy_grid:1).
 */
final class SectionFrames {

    private static final Logger LOGGER = CoreLogs.DIAGNOSTICS.logger();
    private static final String PACKAGE = SectionFrames.class.getPackageName().replace('.', '/') + "/";
    private static final String FRAME = Type.getInternalName(ISectionFrame.class);
    private static final String OBJECT = Type.getInternalName(Object.class);
    private static final String RUNNABLE = Type.getInternalName(Runnable.class);
    private static final String SUPPLIER = Type.getInternalName(Supplier.class);
    private static final ISectionFrame DIRECT = new Direct();

    /* The names taken already, so two ids that read alike once made into a class name still get two classes. */
    private static final Set<String> TAKEN = ConcurrentHashMap.newKeySet();

    private SectionFrames() {
    }

    /* The frame named after id; one with no name of its own when the game will not take a class made here. */
    static ISectionFrame named(final ResourceLocation id) {
        try {
            final Class<?> made = MethodHandles.lookup().defineClass(bytes(freeName(id), id.toString()));
            return (ISectionFrame) made.getDeclaredConstructor().newInstance();
        } catch (final ReflectiveOperationException | LinkageError | RuntimeException refused) {
            LOGGER.warn("The section {} runs in no frame of its own name, so profilers show it unnamed", id, refused);
            return DIRECT;
        }
    }

    private static String freeName(final ResourceLocation id) {
        final String base = PACKAGE + "Section$" + plain(id.getNamespace()) + "$" + plain(id.getPath());
        String name = base;
        for (int n = 2; !TAKEN.add(name); n++) {
            name = base + "$" + n;
        }
        return name;
    }

    /* A part of an id as a part of a class name: what a path may hold and a class name may not becomes "_". */
    private static String plain(final String part) {
        return part.replace('/', '_').replace('.', '_').replace('-', '_');
    }

    /*
     * The class: public, final, holding nothing, with a constructor and the two ways of running work, each on line 1
     * of a source file named after the section.
     */
    private static byte[] bytes(final String name, final String source) {
        final ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        writer.visit(Opcodes.V21, Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL | Opcodes.ACC_SUPER, name, null, OBJECT,
                new String[] {FRAME});
        writer.visitSource(source, null);
        final MethodVisitor init = writer.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        init.visitCode();
        init.visitVarInsn(Opcodes.ALOAD, 0);
        init.visitMethodInsn(Opcodes.INVOKESPECIAL, OBJECT, "<init>", "()V", false);
        init.visitInsn(Opcodes.RETURN);
        init.visitMaxs(0, 0);
        init.visitEnd();
        final MethodVisitor run = writer.visitMethod(Opcodes.ACC_PUBLIC, "run", "(L" + RUNNABLE + ";)V", null, null);
        run.visitCode();
        lineOne(run);
        run.visitVarInsn(Opcodes.ALOAD, 1);
        run.visitMethodInsn(Opcodes.INVOKEINTERFACE, RUNNABLE, "run", "()V", true);
        run.visitInsn(Opcodes.RETURN);
        run.visitMaxs(0, 0);
        run.visitEnd();
        final MethodVisitor call = writer.visitMethod(Opcodes.ACC_PUBLIC, "call",
                "(L" + SUPPLIER + ";)L" + OBJECT + ";", null, null);
        call.visitCode();
        lineOne(call);
        call.visitVarInsn(Opcodes.ALOAD, 1);
        call.visitMethodInsn(Opcodes.INVOKEINTERFACE, SUPPLIER, "get", "()L" + OBJECT + ";", true);
        call.visitInsn(Opcodes.ARETURN);
        call.visitMaxs(0, 0);
        call.visitEnd();
        writer.visitEnd();
        return writer.toByteArray();
    }

    private static void lineOne(final MethodVisitor method) {
        final Label start = new Label();
        method.visitLabel(start);
        method.visitLineNumber(1, start);
    }

    /* The work run where it is, when no frame could be made. */
    private static final class Direct implements ISectionFrame {

        @Override
        public void run(final Runnable work) {
            work.run();
        }

        @Override
        public Object call(final Supplier<?> work) {
            return work.get();
        }
    }
}
