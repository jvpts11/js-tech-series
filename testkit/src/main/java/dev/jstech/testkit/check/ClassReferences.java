/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core Test Kit.
 */
package dev.jstech.testkit.check;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jetbrains.annotations.Nullable;

/**
 * What a compiled class is and reaches, read from its file: what it extends and implements, the fields and methods it
 * declares, and, from its table of constants, the one place every class, field and method it names is written, the
 * classes it names and the fields and methods it uses, each with its owner. Nothing is loaded and nothing runs.
 *
 * @param name       the class itself, in binary form ({@code dev/jstech/example/Kiln})
 * @param superName  what it extends, none for {@code java/lang/Object} itself
 * @param interfaces what it implements
 * @param declared   the fields and methods it declares, with whether each is public or protected
 * @param types      every class it names
 * @param members    every field and method it uses, each with its owner
 */
public record ClassReferences(String name, @Nullable String superName, List<String> interfaces,
                              Set<Declared> declared, Set<String> types, Set<Member> members) {

    private static final int MAGIC = 0xCAFEBABE;
    private static final int PUBLIC = 0x0001;
    private static final int PROTECTED = 0x0004;
    private static final int STATIC = 0x0008;
    /* A class named in a descriptor or a generic signature: L followed by its binary name up to ; or <. */
    private static final Pattern IN_SIGNATURE = Pattern.compile("L([\\w/$]+)[;<]");

    /**
     * Reads the class in {@code bytes}.
     *
     * @throws IOException when they are no class file
     */
    public static ClassReferences read(final byte[] bytes) throws IOException {
        final DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes));
        if (in.readInt() != MAGIC) {
            throw new IOException("not a class file");
        }
        in.readUnsignedShort();
        in.readUnsignedShort();
        final int count = in.readUnsignedShort();
        final String[] utf8 = new String[count];
        final int[] classIndex = new int[count];
        final int[][] refs = new int[count][];
        final int[][] nameAndType = new int[count][];
        final boolean[] isClass = new boolean[count];
        for (int i = 1; i < count; i++) {
            final int tag = in.readUnsignedByte();
            switch (tag) {
                case 1 -> utf8[i] = in.readUTF();
                case 3, 4 -> in.readInt();
                case 5, 6 -> {
                    in.readLong();
                    i++;
                }
                case 7 -> {
                    isClass[i] = true;
                    classIndex[i] = in.readUnsignedShort();
                }
                case 8, 16, 19, 20 -> in.readUnsignedShort();
                case 9, 10, 11 -> refs[i] = new int[] {in.readUnsignedShort(), in.readUnsignedShort(), tag};
                case 12 -> nameAndType[i] = new int[] {in.readUnsignedShort(), in.readUnsignedShort()};
                case 15 -> {
                    in.readUnsignedByte();
                    in.readUnsignedShort();
                }
                case 17, 18 -> {
                    in.readUnsignedShort();
                    in.readUnsignedShort();
                }
                default -> throw new IOException("a constant of an unknown kind, " + tag);
            }
        }
        in.readUnsignedShort();
        final int self = in.readUnsignedShort();
        final int superIndex = in.readUnsignedShort();
        final int interfaceCount = in.readUnsignedShort();
        final List<String> interfaces = new ArrayList<>(interfaceCount);
        for (int i = 0; i < interfaceCount; i++) {
            interfaces.add(utf8[classIndex[in.readUnsignedShort()]]);
        }
        final Set<Declared> declared = new LinkedHashSet<>();
        for (final boolean field : new boolean[] {true, false}) {
            final int many = in.readUnsignedShort();
            for (int i = 0; i < many; i++) {
                final int access = in.readUnsignedShort();
                final String memberName = utf8[in.readUnsignedShort()];
                final String descriptor = utf8[in.readUnsignedShort()];
                declared.add(new Declared(memberName, descriptor, field, (access & (PUBLIC | PROTECTED)) != 0,
                        (access & STATIC) != 0));
                skipAttributes(in);
            }
        }
        final Set<String> types = new LinkedHashSet<>();
        final Set<Member> members = new LinkedHashSet<>();
        for (int i = 1; i < count; i++) {
            if (isClass[i]) {
                types.add(arrayFree(utf8[classIndex[i]]));
            } else if (refs[i] != null) {
                final String owner = arrayFree(utf8[classIndex[refs[i][0]]]);
                final int[] described = nameAndType[refs[i][1]];
                members.add(new Member(owner, utf8[described[0]], utf8[described[1]], refs[i][2] == 9));
            } else if (utf8[i] != null) {
                final Matcher named = IN_SIGNATURE.matcher(utf8[i]);
                while (named.find()) {
                    types.add(named.group(1));
                }
            }
        }
        return new ClassReferences(utf8[classIndex[self]], superIndex == 0 ? null : utf8[classIndex[superIndex]],
                List.copyOf(interfaces), Set.copyOf(declared), Set.copyOf(types), Set.copyOf(members));
    }

    /** Whether it declares the field or method {@code name} of {@code descriptor}, a field by its name alone. */
    public boolean declares(final String memberName, final String descriptor, final boolean field) {
        return this.declared.stream().anyMatch(one -> one.field() == field && one.name().equals(memberName)
                && (field || one.descriptor().equals(descriptor)));
    }

    private static void skipAttributes(final DataInputStream in) throws IOException {
        final int many = in.readUnsignedShort();
        for (int i = 0; i < many; i++) {
            in.readUnsignedShort();
            in.skipNBytes(in.readInt() & 0xFFFFFFFFL);
        }
    }

    /* A class named as an array, {@code [Lfoo/Bar;}, as the class it is an array of. */
    private static String arrayFree(final String name) {
        String plain = name;
        while (plain.startsWith("[")) {
            plain = plain.substring(1);
        }
        return plain.startsWith("L") && plain.endsWith(";") ? plain.substring(1, plain.length() - 1) : plain;
    }

    /**
     * A field or method a class uses.
     *
     * @param owner      the class it is used through, in binary form
     * @param name       its name, {@code <init>} for a constructor
     * @param descriptor its descriptor
     * @param field      whether it is a field
     */
    public record Member(String owner, String name, String descriptor, boolean field) {
    }

    /**
     * A field or method a class declares.
     *
     * @param name       its name
     * @param descriptor its descriptor
     * @param field      whether it is a field
     * @param visible    whether it is public or protected, so another mod's class can reach it
     * @param isStatic   whether it belongs to the class rather than to each of its objects
     */
    public record Declared(String name, String descriptor, boolean field, boolean visible, boolean isStatic) {
    }
}
