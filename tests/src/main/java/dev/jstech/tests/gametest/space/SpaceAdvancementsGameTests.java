/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.space;

import dev.jstech.core.advancement.AdvancementIcons;
import dev.jstech.core.advancement.EventTrigger;
import dev.jstech.core.advancement.MilestoneTrigger;
import dev.jstech.core.progression.Knowledges;
import dev.jstech.core.progression.Milestone;
import dev.jstech.core.progression.Milestones;
import dev.jstech.space.JsSpace;
import dev.jstech.space.advancement.SpaceEvents;
import dev.jstech.space.api.SpaceAdvancementTab;
import dev.jstech.space.datagen.advancement.SpaceAdvancementTabs;
import dev.jstech.space.exploration.SpaceMilestones;
import dev.jstech.tests.JsTests;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * J's Space's advancements and milestones as the server loads them: a tab for leaving the Earth and one for each world,
 * each root on the night sky with its world's picture for an icon, waiting only for events the mod declares; and the
 * firsts of spaceflight, in order, each a piece of knowledge too.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class SpaceAdvancementsGameTests {

    private static final String ARENA = "empty";
    /* The firsts the design lists, from the first suborbital flight to the first galaxy. */
    private static final int FIRSTS = 19;

    private SpaceAdvancementsGameTests() {
    }

    @GameTest(template = ARENA)
    public static void tabs_eachStandOnTheNightSkyWithItsWorldsPicture(final GameTestHelper helper) {
        final Set<ResourceLocation> pictures = new HashSet<>();
        for (final SpaceAdvancementTab tab : SpaceAdvancementTab.values()) {
            final AdvancementHolder root = helper.getLevel().getServer().getAdvancements().get(tab.root());
            helper.assertTrue(root != null && root.value().parent().isEmpty(), tab + " has its root loaded");
            final DisplayInfo display = root.value().display().orElseThrow();
            helper.assertTrue(display.getBackground().filter(SpaceAdvancementTabs.NIGHT_SKY::equals).isPresent(),
                    tab + " stands on the night sky: " + display.getBackground());
            final ResourceLocation picture = AdvancementIcons.pictureOf(display.getIcon());
            helper.assertTrue(picture != null && ships(picture), tab + " shows a picture the mod ships: " + picture);
            pictures.add(picture);
        }
        helper.assertTrue(pictures.size() == SpaceAdvancementTab.values().length,
                "each tab shows its own picture: " + pictures);
        helper.succeed();
    }

    /** Every event an advancement waits for is one the code can report, and every milestone one that is declared. */
    @GameTest(template = ARENA)
    public static void advancements_waitOnlyForDeclaredEventsAndMilestones(final GameTestHelper helper) {
        final Set<String> events = declaredEvents();
        int checked = 0;
        for (final AdvancementHolder holder : helper.getLevel().getServer().getAdvancements().getAllAdvancements()) {
            if (!JsSpace.MODID.equals(holder.id().getNamespace())) {
                continue;
            }
            for (final Criterion<?> criterion : holder.value().criteria().values()) {
                if (criterion.triggerInstance() instanceof EventTrigger.Instance instance) {
                    helper.assertTrue(instance.event().getNamespace().equals(JsSpace.MODID)
                            && events.contains(instance.event().getPath()),
                            holder.id() + " waits for " + instance.event() + ", which nothing reports");
                } else if (criterion.triggerInstance() instanceof MilestoneTrigger.Instance instance) {
                    helper.assertTrue(Milestones.byId(instance.milestone()).isPresent(),
                            holder.id() + " waits for " + instance.milestone() + ", which no mod declares");
                }
                checked++;
            }
        }
        helper.assertTrue(checked >= SpaceAdvancementTab.values().length, "the advancements are loaded: " + checked);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void milestones_areTheFirstsInOrderEachAlsoKnowledge(final GameTestHelper helper) {
        helper.assertTrue(Milestones.of(ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, "firsts"))
                .equals(SpaceMilestones.FIRSTS), "the firsts are declared as one set, in their order");
        helper.assertTrue(SpaceMilestones.FIRSTS.size() == FIRSTS, "all the firsts: " + SpaceMilestones.FIRSTS.size());
        helper.assertTrue(SpaceMilestones.FIRSTS.getFirst() == SpaceMilestones.SUBORBITAL_FLIGHT
                && SpaceMilestones.FIRSTS.getLast() == SpaceMilestones.GALAXY,
                "from the first suborbital flight to the first galaxy");
        for (final Milestone milestone : SpaceMilestones.FIRSTS) {
            helper.assertTrue(Knowledges.byId(milestone.id()).isPresent(), milestone.id() + " is knowledge too");
        }
        helper.succeed();
    }

    /** Whether the mod of a texture's namespace ships it, its whole path given. */
    private static boolean ships(final ResourceLocation texture) {
        return Files.exists(ModList.get().getModFileById(texture.getNamespace()).getFile()
                .findResource(("assets/" + texture.getNamespace() + "/" + texture.getPath()).split("/")));
    }

    /* The events the mod declares, its constants of names. */
    private static Set<String> declaredEvents() {
        final Set<String> events = new HashSet<>();
        for (final Field field : SpaceEvents.class.getFields()) {
            if (Modifier.isStatic(field.getModifiers()) && field.getType() == String.class) {
                try {
                    events.add((String) field.get(null));
                } catch (final IllegalAccessException unreadable) {
                    throw new IllegalStateException("cannot read " + field.getName(), unreadable);
                }
            }
        }
        return events;
    }
}
