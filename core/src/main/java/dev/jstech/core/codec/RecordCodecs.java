/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.codec;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import dev.jstech.core.util.DeclarationError;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.jetbrains.annotations.ApiStatus;

/**
 * A codec for a record, written as the record is: one line a component, in the record's order, and as many as it
 * has, where the game's own builder stops at sixteen and asks for every getter by hand:
 *
 * <pre>{@code
 * public record Setting(String name, int level, boolean shown) {
 *     public static final Codec<Setting> CODEC = RecordCodecs.of(Setting.class)
 *             .field("name", Codec.STRING)
 *             .field("level", Codec.INT)
 *             .optional("shown", Codec.BOOL, true)
 *             .build();
 * }
 * }</pre>
 *
 * <p>Each component is read and made through the record's own accessor and constructor. The names are checked against
 * the record's as the codec is built, so a field written in the wrong place or with the wrong name stops the game as
 * it loads, saying which. That a component's codec makes a value of the component's type is the declaration's word.
 */
@ApiStatus.Experimental
public final class RecordCodecs {

    private RecordCodecs() {
    }

    /** Starts the codec of {@code type}. */
    public static <R extends Record> Builder<R> of(final Class<R> type) {
        return new Builder<>(type);
    }

    /**
     * The fields of a record's codec, one a component, in the record's order.
     *
     * @param <R> the record
     */
    @ApiStatus.Experimental
    public static final class Builder<R extends Record> {

        private final Class<R> type;
        private final List<String> names = new ArrayList<>();
        private final List<MapCodec<?>> fields = new ArrayList<>();

        private Builder(final Class<R> type) {
            this.type = type;
        }

        /** The next component, which every value written has. */
        public <F> Builder<R> field(final String name, final Codec<F> codec) {
            names.add(name);
            fields.add(codec.fieldOf(name));
            return this;
        }

        /** The next component, which a value written may leave out, read as {@code fallback} when it does. */
        public <F> Builder<R> optional(final String name, final Codec<F> codec, final F fallback) {
            names.add(name);
            fields.add(codec.optionalFieldOf(name, fallback));
            return this;
        }

        /**
         * The codec.
         *
         * @throws DeclarationError when the fields are not the record's components, by name and in order
         */
        public Codec<R> build() {
            return mapCodec().codec();
        }

        /** The codec as fields to put inside another one, as {@link MapCodec} is. */
        public MapCodec<R> mapCodec() {
            final RecordComponent[] components = type.getRecordComponents();
            final List<String> declared = Arrays.stream(components).map(RecordComponent::getName).toList();
            if (!declared.equals(names)) {
                throw DeclarationError.of(null, "the codec of the record " + type.getName(),
                        "names the fields " + names + " where the record has " + declared,
                        "write one field a component, with its name, in the record's order");
            }
            try {
                final List<MethodHandle> accessors = new ArrayList<>();
                for (final RecordComponent component : components) {
                    final Method accessor = component.getAccessor();
                    accessor.setAccessible(true);
                    accessors.add(MethodHandles.lookup().unreflect(accessor));
                }
                final Constructor<R> constructor = type.getDeclaredConstructor(
                        Arrays.stream(components).map(RecordComponent::getType).toArray(Class<?>[]::new));
                constructor.setAccessible(true);
                return new RecordMapCodec<>(type, List.copyOf(names), List.copyOf(fields), List.copyOf(accessors),
                        MethodHandles.lookup().unreflectConstructor(constructor));
            } catch (final ReflectiveOperationException | RuntimeException unreachable) {
                throw DeclarationError.of(null, "the record " + type.getName(),
                        "cannot be read and made by the Core: " + unreachable,
                        "make its constructor and accessors reachable, or write its codec by hand");
            }
        }
    }

    /** The fields of a record read and written one after the other, each through its own codec. */
    private static final class RecordMapCodec<R> extends MapCodec<R> {

        private final Class<R> type;
        private final List<String> names;
        private final List<MapCodec<?>> fields;
        private final List<MethodHandle> accessors;
        private final MethodHandle constructor;

        RecordMapCodec(final Class<R> type, final List<String> names, final List<MapCodec<?>> fields,
                       final List<MethodHandle> accessors, final MethodHandle constructor) {
            this.type = type;
            this.names = names;
            this.fields = fields;
            this.accessors = accessors;
            this.constructor = constructor;
        }

        @Override
        public <T> Stream<T> keys(final DynamicOps<T> ops) {
            return names.stream().map(ops::createString);
        }

        @Override
        public <T> DataResult<R> decode(final DynamicOps<T> ops, final MapLike<T> input) {
            final Object[] values = new Object[fields.size()];
            for (int i = 0; i < values.length; i++) {
                final DataResult<?> read = fields.get(i).decode(ops, input);
                final Optional<?> value = read.result();
                if (value.isEmpty()) {
                    // A result with no value carries an error saying why.
                    final String why = read.error().orElseThrow().message();
                    final String name = names.get(i);
                    return DataResult.error(() -> type.getSimpleName() + "." + name + ": " + why);
                }
                values[i] = value.get();
            }
            try {
                return DataResult.success(type.cast(constructor.invokeWithArguments(values)));
            } catch (final Throwable refused) {
                return DataResult.error(() -> type.getSimpleName() + " refused what was read: " + refused);
            }
        }

        @Override
        public <T> RecordBuilder<T> encode(final R input, final DynamicOps<T> ops, final RecordBuilder<T> prefix) {
            RecordBuilder<T> builder = prefix;
            for (int i = 0; i < fields.size(); i++) {
                try {
                    builder = encodeOne(fields.get(i), accessors.get(i).invoke(input), ops, builder);
                } catch (final Throwable unreadable) {
                    final String name = names.get(i);
                    return prefix.withErrorsFrom(DataResult.error(() -> type.getSimpleName() + "." + name
                            + " could not be read: " + unreadable));
                }
            }
            return builder;
        }

        @Override
        public String toString() {
            return "RecordCodec[" + type.getSimpleName() + "]";
        }

        /* Each field's codec takes the value of its own component, which the record's accessor handed over. */
        @SuppressWarnings("unchecked")
        private static <F, T> RecordBuilder<T> encodeOne(final MapCodec<F> field, final Object value,
                                                         final DynamicOps<T> ops, final RecordBuilder<T> builder) {
            return field.encode((F) value, ops, builder);
        }
    }
}
