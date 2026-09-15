/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.system;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Every call the system answers, found by the type it is on, its name and the types a listing writes it with, each one
 * saying who answers it and what it costs.
 *
 * <p>Built once and never changed afterwards: a program resolves its calls against it when it loads, and a registry
 * that could change under a running program would make the same listing mean two things. The same call registered
 * twice is refused, because a listing could never say which of the two it meant.
 */
public final class IntrinsicRegistry {

    private final List<IntrinsicSpec> all;
    /** The entries by type, name and number of parameters, which is as far as a call tells them apart without its types. */
    private final Map<String, List<IntrinsicSpec>> byShape = new HashMap<>();

    private IntrinsicRegistry(final List<IntrinsicSpec> entries) {
        this.all = List.copyOf(entries);
        for (final IntrinsicSpec entry : this.all) {
            this.byShape.computeIfAbsent(MemberId.shape(entry.owner(), entry.name(), entry.parameters().size()),
                    ignored -> new ArrayList<>()).add(entry);
        }
    }

    /** A registry to fill. */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * The entry a call written this way reaches, or null when the system does not answer it. A declaration with exactly
     * those types wins over one that takes them through a type parameter.
     */
    public IntrinsicSpec find(final String owner, final String name, final List<String> written) {
        final List<IntrinsicSpec> candidates = this.byShape.get(MemberId.shape(owner, name, written.size()));
        if (candidates == null) {
            return null;
        }
        for (final IntrinsicSpec candidate : candidates) {
            if (candidate.parameters().equals(written)) {
                return candidate;
            }
        }
        for (final IntrinsicSpec candidate : candidates) {
            if (candidate.accepts(written)) {
                return candidate;
            }
        }
        return null;
    }

    /** Every entry, in the order it was registered. */
    public List<IntrinsicSpec> all() {
        return this.all;
    }

    /** Gathers the entries of a registry, which can be built once. */
    public static final class Builder {

        private final List<IntrinsicSpec> entries = new ArrayList<>();
        private final Set<String> written = new HashSet<>();
        private boolean built;

        private Builder() {
        }

        /** Registers a pure call made on the type itself. */
        public Builder onType(final String owner, final String name, final String returns, final IPureFunction function,
                              final String... parameters) {
            return this.add(IntrinsicSpec.pure(owner, name, List.of(parameters), returns, false, function));
        }

        /** Registers a pure call made on an object of the type, handed over ahead of the arguments. */
        public Builder onObject(final String owner, final String name, final String returns,
                                final IPureFunction function, final String... parameters) {
            return this.add(IntrinsicSpec.pure(owner, name, List.of(parameters), returns, true, function));
        }

        /** The registry, with nothing more to be added to it. */
        public IntrinsicRegistry build() {
            this.built = true;
            return new IntrinsicRegistry(this.entries);
        }

        private Builder add(final IntrinsicSpec entry) {
            if (this.built) {
                throw new IllegalStateException(entry.describe() + " came after the registry was built");
            }
            if (!this.written.add(entry.describe())) {
                throw new IllegalStateException(entry.describe() + " is registered twice");
            }
            this.entries.add(entry);
            return this;
        }
    }
}
