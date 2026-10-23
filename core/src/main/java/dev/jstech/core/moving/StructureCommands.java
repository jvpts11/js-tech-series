/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import dev.jstech.core.command.CommandRoot;
import dev.jstech.core.moving.physics.PhysicsMotion;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * The commands of the moving structures, for those who build modpacks, maps and tests:
 * {@code /jstech structure assemble <from> <to>} and {@code assemble joined <block>} make one; {@code move},
 * {@code spin}, {@code swing}, {@code rails} and {@code stop} move it by the kit's rules, {@code physics} lets it go
 * by its physics; {@code freeze} holds it where it stands, in the air too, and {@code unfreeze} lets it go again;
 * {@code setdown} sets it on the ground; {@code remove} takes it away, its blocks with it; {@code export <name>} saves
 * its blocks as the game's structure file {@code name}; {@code list} names those loaded here.
 */
public final class StructureCommands {

    private static final String FROM = "from";
    private static final String TO = "to";
    private static final String BLOCK = "block";
    private static final String STRUCTURE = "structure";
    private static final String PLACE = "place";
    private static final String SPEED = "speed";
    private static final String RATE = "rate";
    private static final String HINGE = "hinge";
    private static final String ANGLE = "angle";
    private static final String NAME = "name";
    private static final SimpleCommandExceptionType NOT_ONE =
            new SimpleCommandExceptionType(GameText.component(StructureTexts.NOT_A_STRUCTURE));

    private StructureCommands() {
    }

    /** Declares the commands, from the kit's start. */
    static void declare() {
        CommandRoot.CORE.declare("structure", CommandRoot.GAME_MASTERS, (node, context) -> node
                .then(Commands.literal("assemble")
                        .then(Commands.argument(FROM, BlockPosArgument.blockPos())
                                .then(Commands.argument(TO, BlockPosArgument.blockPos())
                                        .executes(StructureCommands::assembleBox)))
                        .then(Commands.literal("joined")
                                .then(Commands.argument(BLOCK, BlockPosArgument.blockPos())
                                        .executes(StructureCommands::assembleJoined))))
                .then(Commands.literal("setdown")
                        .then(Commands.argument(STRUCTURE, EntityArgument.entity())
                                .executes(StructureCommands::setDown)))
                .then(Commands.literal("move")
                        .then(Commands.argument(STRUCTURE, EntityArgument.entity())
                                .then(Commands.argument(PLACE, Vec3Argument.vec3())
                                        .then(Commands.argument(SPEED, DoubleArgumentType.doubleArg(0.01, 1000))
                                                .executes(StructureCommands::moveTo)))))
                .then(Commands.literal("spin")
                        .then(Commands.argument(STRUCTURE, EntityArgument.entity())
                                .then(Commands.argument(RATE, DoubleArgumentType.doubleArg(-3600, 3600))
                                        .executes(StructureCommands::spin))))
                .then(Commands.literal("swing")
                        .then(Commands.argument(STRUCTURE, EntityArgument.entity())
                                .then(Commands.argument(HINGE, Vec3Argument.vec3())
                                        .then(Commands.argument(ANGLE, DoubleArgumentType.doubleArg(-360, 360))
                                                .then(Commands.argument(RATE,
                                                                DoubleArgumentType.doubleArg(0.01, 3600))
                                                        .executes(StructureCommands::swing))))))
                .then(Commands.literal("rails")
                        .then(Commands.argument(STRUCTURE, EntityArgument.entity())
                                .then(Commands.argument(SPEED, DoubleArgumentType.doubleArg(0.01, 200))
                                        .executes(StructureCommands::rails))))
                .then(Commands.literal("physics")
                        .then(Commands.argument(STRUCTURE, EntityArgument.entity())
                                .executes(StructureCommands::physics)))
                .then(Commands.literal("stop")
                        .then(Commands.argument(STRUCTURE, EntityArgument.entity())
                                .executes(StructureCommands::stop)))
                .then(Commands.literal("freeze")
                        .then(Commands.argument(STRUCTURE, EntityArgument.entity())
                                .executes(command -> freeze(command, true))))
                .then(Commands.literal("unfreeze")
                        .then(Commands.argument(STRUCTURE, EntityArgument.entity())
                                .executes(command -> freeze(command, false))))
                .then(Commands.literal("remove")
                        .then(Commands.argument(STRUCTURE, EntityArgument.entity())
                                .executes(StructureCommands::remove)))
                .then(Commands.literal("export")
                        .then(Commands.argument(STRUCTURE, EntityArgument.entity())
                                .then(Commands.argument(NAME, ResourceLocationArgument.id())
                                        .executes(StructureCommands::export))))
                .then(Commands.literal("list").executes(StructureCommands::list)));
    }

    private static int assembleBox(final CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final ServerLevel level = context.getSource().getLevel();
        final StructureResult made = MovingStructures.assemble(level, BlockPosArgument.getLoadedBlockPos(context,
                FROM), BlockPosArgument.getLoadedBlockPos(context, TO));
        return answer(context, made, StructureTexts.ASSEMBLED.with(made.blocks()));
    }

    private static int assembleJoined(final CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        final ServerLevel level = context.getSource().getLevel();
        final StructureResult made = MovingStructures.assembleJoined(level,
                BlockPosArgument.getLoadedBlockPos(context, BLOCK));
        return answer(context, made, StructureTexts.ASSEMBLED.with(made.blocks()));
    }

    private static int setDown(final CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final StructureResult done = MovingStructures.setDown(structure(context));
        return answer(context, done, StructureTexts.DISASSEMBLED.with(done.blocks()));
    }

    private static int moveTo(final CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final MovingStructure structure = structure(context);
        final Vec3 to = Vec3Argument.getVec3(context, PLACE);
        structure.move(new TowardMotion(structure.pose().movedTo(to.x, to.y, to.z),
                DoubleArgumentType.getDouble(context, SPEED), 0));
        return said(context, StructureTexts.MOVING.text());
    }

    private static int spin(final CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final MovingStructure structure = structure(context);
        structure.move(new SpinMotion(0, 1, 0, DoubleArgumentType.getDouble(context, RATE)));
        return said(context, StructureTexts.MOVING.text());
    }

    private static int swing(final CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final MovingStructure structure = structure(context);
        final Vec3 hinge = Vec3Argument.getVec3(context, HINGE);
        final Direction facing = context.getSource().getEntity() == null ? Direction.NORTH
                : context.getSource().getEntity().getDirection();
        final Direction axis = facing.getClockWise();
        structure.move(new TurnAboutMotion(hinge.x, hinge.y, hinge.z, axis.getStepX(), axis.getStepY(),
                axis.getStepZ(), DoubleArgumentType.getDouble(context, ANGLE),
                DoubleArgumentType.getDouble(context, RATE)));
        return said(context, StructureTexts.MOVING.text());
    }

    private static int rails(final CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final MovingStructure structure = structure(context);
        final Entity source = context.getSource().getEntity();
        final Direction heading = source == null ? Direction.NORTH : source.getDirection();
        final RailsMotion motion = RailsMotion.onRails(context.getSource().getLevel(), structure.pose(), heading,
                DoubleArgumentType.getDouble(context, SPEED));
        if (motion == null) {
            context.getSource().sendFailure(GameText.component(StructureTexts.NO_RAILS));
            return 0;
        }
        structure.move(motion);
        return said(context, StructureTexts.MOVING.text());
    }

    private static int physics(final CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        structure(context).move(PhysicsMotion.AT_REST);
        return said(context, StructureTexts.PHYSICAL.text());
    }

    private static int stop(final CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        structure(context).move(null);
        return said(context, StructureTexts.STOPPED.text());
    }

    private static int freeze(final CommandContext<CommandSourceStack> context, final boolean frozen)
            throws CommandSyntaxException {
        MovingStructures.freeze(structure(context), frozen);
        return said(context, frozen ? StructureTexts.FROZEN.text() : StructureTexts.UNFROZEN.text());
    }

    private static int remove(final CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final MovingStructure structure = structure(context);
        final int blocks = structure.blocks();
        MovingStructures.takeAway(structure);
        said(context, StructureTexts.REMOVED.with(blocks));
        return Math.max(1, blocks);
    }

    private static int export(final CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final MovingStructure structure = structure(context);
        final ResourceLocation id = ResourceLocationArgument.getId(context, NAME);
        if (!MovingStructures.export(structure, id)) {
            context.getSource().sendFailure(GameText.component(StructureTexts.NOT_EXPORTED.with(id.toString())));
            return 0;
        }
        said(context, StructureTexts.EXPORTED.with(structure.blocks(), id.toString()));
        return Math.max(1, structure.blocks());
    }

    private static int list(final CommandContext<CommandSourceStack> context) {
        final ServerLevel level = context.getSource().getLevel();
        int listed = 0;
        for (final IStructureBody body : LevelMovingStructures.of(level).all()) {
            if (body instanceof MovingStructure structure) {
                final BlockPos at = BlockPos.containing(structure.pose().x(), structure.pose().y(),
                        structure.pose().z());
                said(context, StructureTexts.LISTED.with(structure.id().asString(), structure.blocks(),
                        at.toShortString(), structure.resting() ? StructureTexts.AT_REST : StructureTexts.UNDER_WAY));
                listed++;
            }
        }
        if (listed == 0) {
            said(context, StructureTexts.NONE_LOADED.text());
        }
        return listed;
    }

    private static MovingStructure structure(final CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        final Entity entity = EntityArgument.getEntity(context, STRUCTURE);
        if (entity instanceof MovingStructureEntity structureEntity && structureEntity.structure() != null) {
            return structureEntity.structure();
        }
        throw NOT_ONE.create();
    }

    private static int answer(final CommandContext<CommandSourceStack> context, final StructureResult result,
                              final Text done) {
        if (!result.ok() && result.refusal() != null) {
            context.getSource().sendFailure(GameText.component(result.refusal()));
            return 0;
        }
        return said(context, done) == 1 ? Math.max(1, result.blocks()) : 0;
    }

    private static int said(final CommandContext<CommandSourceStack> context, final Text text) {
        context.getSource().sendSuccess(() -> GameText.component(text), true);
        return 1;
    }
}
