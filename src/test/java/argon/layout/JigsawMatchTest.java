package argon.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.core.BlockPos;
import net.minecraft.core.FrontAndTop;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.JigsawBlock;
import net.minecraft.world.level.block.entity.JigsawBlockEntity;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Checks Argon's attach rule against the vanilla one it replaces.
 *
 * <p>Argon answers {@code JigsawBlock.canAttach} with its own matcher, because vanilla reads the
 * front and the top out of two property lookups per block while both live in the single
 * {@code ORIENTATION} state. The matcher has to give the same answer for every input vanilla
 * would see, and it is checked here over the whole cross product of parent and child
 * orientations, joint types and name matches rather than over a handful of examples.
 */
class JigsawMatchTest {
    private static final Identifier TARGET = Identifier.withDefaultNamespace("argon_target");
    private static final Identifier OTHER = Identifier.withDefaultNamespace("argon_other");
    private static final ResourceKey<StructureTemplatePool> POOL =
        ResourceKey.create(Registries.TEMPLATE_POOL, Identifier.withDefaultNamespace("argon_pool"));

    /**
     * The jigsaw block and its state live in the game registries, so the registries have to be up
     * before a block state can be read.
     */
    @BeforeAll
    static void bootstrapGame() {
        Bootstrap.bootStrap();
    }

    @Test
    @DisplayName("Argon's attach rule answers the same as vanilla's for every orientation, joint and name")
    void matchesVanilla() {
        int checked = 0;
        for (FrontAndTop parentOrientation : FrontAndTop.values()) {
            for (FrontAndTop childOrientation : FrontAndTop.values()) {
                for (JigsawBlockEntity.JointType joint : JigsawBlockEntity.JointType.values()) {
                    StructureTemplate.JigsawBlockInfo parent =
                        jigsaw(parentOrientation, joint, TARGET, TARGET);
                    for (Identifier childName : new Identifier[] {TARGET, OTHER, null}) {
                        StructureTemplate.JigsawBlockInfo child =
                            jigsaw(childOrientation, joint, childName, OTHER);
                        assertEquals(
                            JigsawBlock.canAttach(parent, child),
                            JigsawMatch.canAttach(parent, child),
                            "parent " + parentOrientation + " joint " + joint + " child " + childOrientation
                                + " child name " + childName
                        );
                        checked++;
                    }
                }
            }
        }
        assertEquals(FrontAndTop.values().length * FrontAndTop.values().length
            * JigsawBlockEntity.JointType.values().length * 3, checked);
    }

    @Test
    @DisplayName("a connector only attaches to the face it points at")
    void onlyTheOppositeFaceAttaches() {
        StructureTemplate.JigsawBlockInfo parent = jigsaw(FrontAndTop.NORTH_UP, JigsawBlockEntity.JointType.ALIGNED, TARGET, TARGET);
        StructureTemplate.JigsawBlockInfo facing = jigsaw(FrontAndTop.SOUTH_UP, JigsawBlockEntity.JointType.ALIGNED, TARGET, OTHER);
        StructureTemplate.JigsawBlockInfo sideways = jigsaw(FrontAndTop.EAST_UP, JigsawBlockEntity.JointType.ALIGNED, TARGET, OTHER);

        assertEquals(true, JigsawMatch.canAttach(parent, facing));
        assertEquals(false, JigsawMatch.canAttach(parent, sideways));
    }

    @Test
    @DisplayName("a rollable joint attaches whatever the top is")
    void rollableIgnoresTheTop() {
        // A jigsaw that points straight up has its top on a side, so the two jigsaws can face
        // each other and still disagree about which way is up.
        StructureTemplate.JigsawBlockInfo parent =
            jigsaw(FrontAndTop.UP_NORTH, JigsawBlockEntity.JointType.ROLLABLE, TARGET, TARGET);
        StructureTemplate.JigsawBlockInfo child =
            jigsaw(FrontAndTop.DOWN_SOUTH, JigsawBlockEntity.JointType.ROLLABLE, TARGET, OTHER);

        assertEquals(JigsawBlock.canAttach(parent, child), JigsawMatch.canAttach(parent, child));
        assertEquals(true, JigsawMatch.canAttach(parent, child));
    }

    @Test
    @DisplayName("a child with no name matches any target")
    void anUnnamedChildMatchesAnything() {
        StructureTemplate.JigsawBlockInfo parent = jigsaw(FrontAndTop.NORTH_UP, JigsawBlockEntity.JointType.ALIGNED, TARGET, TARGET);
        StructureTemplate.JigsawBlockInfo child = jigsaw(FrontAndTop.SOUTH_UP, JigsawBlockEntity.JointType.ALIGNED, null, OTHER);

        assertEquals(true, JigsawMatch.canAttach(parent, child));
        assertEquals(JigsawBlock.canAttach(parent, child), JigsawMatch.canAttach(parent, child));
    }

    private static StructureTemplate.JigsawBlockInfo jigsaw(FrontAndTop orientation,
                                                            JigsawBlockEntity.JointType joint,
                                                            Identifier name,
                                                            Identifier target) {
        return new StructureTemplate.JigsawBlockInfo(
            BlockPos.ZERO,
            Blocks.JIGSAW.defaultBlockState().setValue(JigsawBlock.ORIENTATION, orientation),
            joint,
            name,
            POOL,
            target,
            0,
            0
        );
    }
}
