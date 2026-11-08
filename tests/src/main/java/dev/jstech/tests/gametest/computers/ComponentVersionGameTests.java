/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.computers;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.os.media.MediaKind;
import dev.jstech.computers.registry.ComputingComponents;
import dev.jstech.core.persistence.SaveLayout;
import dev.jstech.tests.JsTests;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * J's Computers' item data is saved with the version of its layout beside it, so a later change to what a component
 * holds can bring an item saved before it up to date, and an item saved before components had versions still reads
 * as it was.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ComponentVersionGameTests {

    private static final String ARENA = "empty";
    private static final ResourceLocation CD_ROM = ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "cd_rom");
    private static final ResourceLocation FRAMES_95 =
            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "frames_95");
    private static final String LABEL = "Old label";
    private static final String COMPONENTS = "components";

    private ComponentVersionGameTests() {
    }

    @GameTest(template = ARENA)
    public static void everySavedComponent_carriesTheVersionOfItsLayout(final GameTestHelper helper) {
        final List<String> bare = new ArrayList<>();
        for (final Map.Entry<ResourceKey<DataComponentType<?>>, DataComponentType<?>> entry
                : BuiltInRegistries.DATA_COMPONENT_TYPE.entrySet()) {
            final ResourceLocation id = entry.getKey().location();
            final DataComponentType<?> type = entry.getValue();
            if (id.getNamespace().equals(JsComputers.MODID) && type.codec() != null
                    && !type.codec().toString().startsWith("SaveLayout[" + id + "]")) {
                bare.add(id.toString());
            }
        }
        helper.assertTrue(bare.isEmpty(), "every component J's Computers saves carries its layout's version; not "
                + bare);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void anItemSavedBeforeVersions_readsAsItWasAndIsSavedWithThem(final GameTestHelper helper) {
        final HolderLookup.Provider registries = helper.getLevel().registryAccess();
        // An installer disc as an older version of the mod saved it: each component's value bare.
        final CompoundTag before = new CompoundTag();
        before.putString("id", CD_ROM.toString());
        before.putInt("count", 1);
        final CompoundTag components = new CompoundTag();
        components.putString(id("media_kind"), "os_install");
        components.putString(id("media_payload"), FRAMES_95.toString());
        components.putString(id("volume_label"), LABEL);
        before.put(COMPONENTS, components);

        final ItemStack read = ItemStack.parseOptional(registries, before);
        assertDisc(helper, read, "read from before versions");

        // Saved again, each component carries its version beside its value.
        final CompoundTag saved = (CompoundTag) read.save(registries);
        final Tag label = saved.getCompound(COMPONENTS).get(id("volume_label"));
        helper.assertTrue(label instanceof CompoundTag wrapped && wrapped.getInt(SaveLayout.VERSION_KEY) == 1
                && StringTag.valueOf(LABEL).equals(wrapped.get(SaveLayout.VALUE_KEY)),
                "a component is saved as its version and its value: " + label);
        assertDisc(helper, ItemStack.parseOptional(registries, saved), "read back as saved now");
        helper.succeed();
    }

    private static void assertDisc(final GameTestHelper helper, final ItemStack disc, final String how) {
        helper.assertTrue(disc.getItem() == BuiltInRegistries.ITEM.get(CD_ROM), "the disc, " + how + ": " + disc);
        helper.assertTrue(disc.get(ComputingComponents.MEDIA_KIND.get()) == MediaKind.OS_INSTALL
                && FRAMES_95.equals(disc.get(ComputingComponents.MEDIA_PAYLOAD.get()))
                && LABEL.equals(disc.get(ComputingComponents.VOLUME_LABEL.get())),
                "its kind, its installer and its label, " + how + ": " + disc.getComponents());
    }

    private static String id(final String path) {
        return JsComputers.MODID + ":" + path;
    }
}
