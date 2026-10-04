/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program;

import dev.jstech.computers.block.part.CraftingInterfacePart;
import dev.jstech.computers.block.part.CraftingRouterPart;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.crafting.CraftingDispatch;
import dev.jstech.computers.crafting.CraftingFloor;
import dev.jstech.computers.crafting.NetworkRecipe;
import dev.jstech.computers.crafting.ProcessingPattern;
import dev.jstech.computers.program.iql.IqlBusStatement;
import dev.jstech.computers.program.iql.IqlCraftingStatement;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

/**
 * Applies a statement that sets a part of the crafting network to the Crafting Interface or the Crafting Input Router
 * of that name on the crafting cables of the Mainframe's Crafting Computers, as software sets it: the setting carries
 * the mark of what set it (a job by its name, a statement typed at a prompt as IQL), which the part's window shows
 * until a hand changes it. A router takes the settings a bus takes, refused where its era cannot be set to them.
 */
@TextHolder
public final class IqlCraftingSetter {

    private static final TextKey SET = TextKey.of("jsc.service.iql.crafting_set", "%s set");
    private static final TextKey RENAMED = TextKey.of("jsc.service.iql.crafting_renamed", "%s is now called %s");
    private static final TextKey NO_INTERFACE = TextKey.of("jsc.service.iql.no_interface",
            "no Crafting Interface named %s on this network");
    private static final TextKey NO_ROUTER = TextKey.of("jsc.service.iql.no_router",
            "no Crafting Input Router named %s on this network");
    private static final TextKey NO_PATTERN = TextKey.of("jsc.service.iql.no_pattern",
            "the interface %s holds no pattern called %s");
    private static final TextKey NOT_AN_INPUT = TextKey.of("jsc.service.iql.not_an_input", "%s is not an input of %s");
    private static final TextKey ROUTER_ELSEWHERE = TextKey.of("jsc.service.iql.router_elsewhere",
            "no router named %s on the cable of the interface %s");

    private IqlCraftingSetter() {
    }

    /** Sets what {@code statement} says on its part of the Mainframe's crafting network; {@code by} names who. */
    public static IqlEngine.Outcome apply(final MainframeBlockEntity mainframe, final IqlCraftingStatement statement,
                                          final String by) {
        if (!(mainframe.getLevel() instanceof ServerLevel level)) {
            return missing(statement);
        }
        return apply(level, mainframe.craftingComputerPositions(), statement, by);
    }

    /** Sets what {@code statement} says on its part of the crafting networks of {@code computers}. */
    public static IqlEngine.Outcome apply(final ServerLevel level, final List<BlockPos> computers,
                                          final IqlCraftingStatement statement, final String by) {
        final List<CraftingFloor> floors = CraftingDispatch.floors(level, computers);
        return statement.part() == IqlCraftingStatement.Part.INTERFACE
                ? onInterface(level, floors, statement, by) : onRouter(level, floors, statement, by);
    }

    /** The interface of that name on {@code computers}' crafting networks, or null. */
    @Nullable
    public static CraftingInterfacePart findInterface(final ServerLevel level, final List<BlockPos> computers,
                                                      final String name) {
        final Located found = locate(level, CraftingDispatch.floors(level, computers), name);
        return found == null ? null : found.part();
    }

    /** The router of that name on {@code computers}' crafting networks, or null. */
    @Nullable
    public static CraftingRouterPart findRouter(final ServerLevel level, final List<BlockPos> computers,
                                                final String name) {
        return router(level, CraftingDispatch.floors(level, computers), name);
    }

    private static IqlEngine.Outcome onInterface(final ServerLevel level, final List<CraftingFloor> floors,
                                                 final IqlCraftingStatement statement, final String by) {
        final Located at = locate(level, floors, statement.name());
        if (at == null) {
            return IqlEngine.Outcome.fail(NO_INTERFACE.with(statement.name()));
        }
        final CraftingInterfacePart part = at.part();
        switch (statement.change()) {
            case IqlCraftingStatement.Exclusive exclusive -> part.setExclusive(exclusive.oneAtATime(), by);
            case IqlCraftingStatement.MaxJobs most -> part.setMaxJobs(most.most(), by);
            case IqlCraftingStatement.Paused paused -> part.setPaused(paused.paused(), by);
            case IqlCraftingStatement.Rename rename -> {
                part.setName(rename.newName());
                return IqlEngine.Outcome.ok(RENAMED.with(statement.name(), part.name()));
            }
            case IqlCraftingStatement.Route route -> {
                return route(level, at, route, by);
            }
            case IqlCraftingStatement.RouterSetting ignored -> {
                return IqlEngine.Outcome.fail(NO_ROUTER.with(statement.name()));
            }
        }
        return IqlEngine.Outcome.ok(SET.with(statement.name()));
    }

    /* Routes an input of a pattern the interface holds through a router of its own cable, or back to its filters. */
    private static IqlEngine.Outcome route(final ServerLevel level, final Located at,
                                           final IqlCraftingStatement.Route route, final String by) {
        final CraftingInterfacePart part = at.part();
        final List<CraftingInterfacePart.HeldPattern> held = part.patterns();
        int index = -1;
        for (int i = 0; i < held.size() && index < 0; i++) {
            if (held.get(i).recipe().displayName().equalsIgnoreCase(route.pattern())) {
                index = i;
            }
        }
        if (index < 0) {
            return IqlEngine.Outcome.fail(NO_PATTERN.with(part.name(), route.pattern()));
        }
        final StorageKey input = StorageKey.byName(route.input());
        if (input == null || !inputsOf(held.get(index).recipe()).contains(input)) {
            return IqlEngine.Outcome.fail(NOT_AN_INPUT.with(route.input(), route.pattern()));
        }
        UUID router = null;
        if (route.router() != null) {
            for (final CraftingFloor.Site site : at.floor().reach(at.site()).routers()) {
                final CraftingRouterPart candidate = site.part(level, CraftingRouterPart.class);
                if (candidate != null && route.router().equalsIgnoreCase(candidate.name())) {
                    router = candidate.id();
                }
            }
            if (router == null) {
                return IqlEngine.Outcome.fail(ROUTER_ELSEWHERE.with(route.router(), part.name()));
            }
        }
        part.route(index, input, router, by);
        return IqlEngine.Outcome.ok(SET.with(part.name()));
    }

    private static IqlEngine.Outcome onRouter(final ServerLevel level, final List<CraftingFloor> floors,
                                              final IqlCraftingStatement statement, final String by) {
        final CraftingRouterPart router = router(level, floors, statement.name());
        if (router == null) {
            return IqlEngine.Outcome.fail(NO_ROUTER.with(statement.name()));
        }
        return switch (statement.change()) {
            case IqlCraftingStatement.Rename rename -> {
                router.setName(rename.newName());
                yield IqlEngine.Outcome.ok(RENAMED.with(statement.name(), router.name()));
            }
            case IqlCraftingStatement.RouterSetting setting -> IqlBusSetter.applyTo(router,
                    new IqlBusStatement(statement.name(), setting.setting()), by);
            default -> IqlEngine.Outcome.fail(NO_INTERFACE.with(statement.name()));
        };
    }

    @Nullable
    private static Located locate(final ServerLevel level, final List<CraftingFloor> floors, final String name) {
        for (final CraftingFloor floor : floors) {
            for (final CraftingFloor.Site site : floor.interfaces()) {
                final CraftingInterfacePart part = site.part(level, CraftingInterfacePart.class);
                if (part != null && name.equalsIgnoreCase(part.name())) {
                    return new Located(floor, site, part);
                }
            }
        }
        return null;
    }

    /* The router of that name on an interface's own cable, or loose on a network. */
    @Nullable
    private static CraftingRouterPart router(final ServerLevel level, final List<CraftingFloor> floors,
                                             final String name) {
        for (final CraftingFloor floor : floors) {
            final List<CraftingFloor.Site> sites = new ArrayList<>(floor.looseRouters());
            for (final CraftingFloor.Site site : floor.interfaces()) {
                sites.addAll(floor.reach(site).routers());
            }
            for (final CraftingFloor.Site site : sites) {
                final CraftingRouterPart router = site.part(level, CraftingRouterPart.class);
                if (router != null && name.equalsIgnoreCase(router.name())) {
                    return router;
                }
            }
        }
        return null;
    }

    private static List<StorageKey> inputsOf(final NetworkRecipe recipe) {
        final List<StorageKey> keys = new ArrayList<>();
        recipe.proc().ifPresent(pattern -> addInputs(pattern, keys));
        recipe.multi().ifPresent(multi -> multi.stages().forEach(stage -> stage.proc()
                .ifPresent(pattern -> addInputs(pattern, keys))));
        return keys;
    }

    private static void addInputs(final ProcessingPattern pattern, final List<StorageKey> into) {
        for (final ProcessingPattern.ProcessingInput in : pattern.inputs()) {
            into.add(in.key());
        }
    }

    private static IqlEngine.Outcome missing(final IqlCraftingStatement statement) {
        return IqlEngine.Outcome.fail(statement.part() == IqlCraftingStatement.Part.INTERFACE
                ? NO_INTERFACE.with(statement.name()) : NO_ROUTER.with(statement.name()));
    }

    /* An interface found by its name, with where it is. */
    private record Located(CraftingFloor floor, CraftingFloor.Site site, CraftingInterfacePart part) {
    }
}
