package argon.mixin;

import argon.debug.ArgonProfiler;
import argon.layout.BoxOctree;
import argon.layout.TrojanVoxelShape;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.apache.commons.lang3.mutable.MutableObject;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement$Placer")
public abstract class JigsawPlacementPlacerMixin {

    @WrapOperation(
        method = "tryPlacingChildren",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/phys/shapes/Shapes;joinIsNotEmpty(Lnet/minecraft/world/phys/shapes/VoxelShape;Lnet/minecraft/world/phys/shapes/VoxelShape;Lnet/minecraft/world/phys/shapes/BooleanOp;)Z"
        )
    )
    private boolean argon$fastOctreeIntersection(VoxelShape shape1, VoxelShape shape2, BooleanOp op, Operation<Boolean> original, @Local(argsOnly = true) BoundingBox box) {
        ArgonProfiler.PIECES_TESTED.incrementAndGet();

        if (shape1 instanceof TrojanVoxelShape trojan) {
            ArgonProfiler.OCTREE_CHECKS_SAVED.incrementAndGet();
            return trojan.boxOctree.intersectsAnyBox(
                box.minX(), box.minY(), box.minZ(),
                box.maxX() + 1, box.maxY() + 1, box.maxZ() + 1
            );
        }
        return original.call(shape1, shape2, op);
    }

    @SuppressWarnings("deprecation")
    @Inject(
        method = "tryPlacingChildren",
        at = @At("HEAD")
    )
    private void argon$injectOctreeCarrier(
        StructurePoolElement poolElement,
        MutableObject<VoxelShape> shapeHolder,
        int depth,
        boolean doRigid,
        CallbackInfo ci
    ) {
        if (!(shapeHolder.getValue() instanceof TrojanVoxelShape)) {
            BoxOctree octree = new BoxOctree(new AABB(-30000, -512, -30000, 30000, 512, 30000));
            shapeHolder.setValue(new TrojanVoxelShape(octree));
        }
    }
}