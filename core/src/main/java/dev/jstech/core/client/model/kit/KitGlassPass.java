/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.model.kit;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

/*
 * The models' see-through faces (their glass) held back while the world's things are drawn and drawn after them all.
 * The game draws a way of drawing as soon as another is asked for, and the models of one look are drawn together only
 * once the block entities are done (KitInstancing): glass drawn among them would stand in front of whatever is drawn
 * behind it afterwards, a ship's cabin and its seats behind its windows, and show through to the ground beyond. Held
 * here from the sky to the block entities' end, the glass is drawn last, over everything solid behind it. A model
 * drawn at any other time, in a screen or in the hand, draws its glass at once, as before.
 */
final class KitGlassPass {

    private static final Map<RenderType, BufferBuilder> HELD = new LinkedHashMap<>();
    private static final Map<RenderType, ByteBufferBuilder> MEMORY = new HashMap<>();
    private static boolean holding;

    private KitGlassPass() {
    }

    /**
     * Where faces drawn {@code type} go: held for the end of the world's solid things while they are being drawn into
     * the game's own buffers, else into {@code buffers} at once.
     */
    static VertexConsumer buffer(final MultiBufferSource buffers, final RenderType type) {
        if (!holding || buffers != Minecraft.getInstance().renderBuffers().bufferSource()) {
            return buffers.getBuffer(type);
        }
        return HELD.computeIfAbsent(type, key -> new BufferBuilder(MEMORY.computeIfAbsent(key,
                each -> new ByteBufferBuilder(each.bufferSize())), key.mode(), key.format()));
    }

    /** Begins holding the glass back, as the world's things begin to be drawn. */
    static void hold() {
        holding = true;
    }

    /** Draws the glass held back, sorted from the far to the near as the game sorts its own, and stops holding. */
    static void draw() {
        holding = false;
        for (final Map.Entry<RenderType, BufferBuilder> held : HELD.entrySet()) {
            final MeshData mesh = held.getValue().build();
            if (mesh == null) {
                continue;
            }
            final RenderType type = held.getKey();
            if (type.sortOnUpload()) {
                mesh.sortQuads(MEMORY.get(type), RenderSystem.getVertexSorting());
            }
            type.draw(mesh);
        }
        HELD.clear();
    }

    /** Lets the memory the glass was held in go, as the models are read again. */
    static void forget() {
        HELD.clear();
        MEMORY.values().forEach(ByteBufferBuilder::close);
        MEMORY.clear();
    }
}
