package argon.mixin;

import argon.FrameCadence;
import argon.debug.ArgonDebugLines;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(targets = "net.minecraft.client.gui.components.DebugScreenOverlay", remap = false)
public class DebugScreenOverlayMixin {
    @Unique
    private static GuiGraphicsExtractor argon$extractor;

    @Unique
    private static boolean argon$wrote;

    @Inject(method = "extractLines(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Ljava/util/List;ZI)V",
            at = @At("HEAD"), require = 1, remap = false)
    private void argon$debugLine(GuiGraphicsExtractor extractor, List<String> lines, boolean leftSide, int width,
                                 CallbackInfo callbackInfo) {
        if (lines == null) {
            return;
        }
        rememberPass(extractor);
        int kept = stripExtraBrandLines(lines, leftSide);
        if (kept >= 0 || !leftSide || argon$wrote) {
            if (kept >= 0) {
                argon$wrote = true;
            }
            return;
        }
        lines.add(ArgonDebugLines.current(argon$pacing()));
        argon$wrote = true;
    }

    @Unique
    private static void rememberPass(GuiGraphicsExtractor extractor) {
        if (extractor == argon$extractor) {
            return;
        }
        argon$extractor = extractor;
        argon$wrote = false;
    }

    /**
     * Keeps the first brand line and drops the rest. The replacement text is only built when a
     * brand line is actually there to rewrite, which is what the right-side batches skip.
     */
    @Unique
    private static int stripExtraBrandLines(List<String> lines, boolean leftSide) {
        int kept = -1;
        for (int i = 0; i < lines.size(); ) {
            String existing = lines.get(i);
            if (!ArgonDebugLines.isBrandLine(existing)) {
                i++;
                continue;
            }
            if (!leftSide || kept >= 0 || argon$wrote) {
                lines.remove(i);
                continue;
            }
            String line = ArgonDebugLines.current(argon$pacing());
            if (!line.equals(existing)) {
                lines.set(i, line);
            }
            kept = i;
            i++;
        }
        return kept;
    }

    /**
     * Whether Argon is the one pacing frames.
     *
     * <p>Vanilla only calls the frame limiter while the limit is below the unlimited cut-off, so
     * the limiter mixin never sees an unlimited frame rate and the pacing state it keeps would
     * stay set from the last limited frame. The live limit is read here so the line reports the
     * frame rate that is actually being applied.
     */
    @Unique
    private static boolean argon$pacing() {
        int limit = Minecraft.getInstance().getFramerateLimitTracker().getFramerateLimit();
        return limit > 0 && limit < Options.UNLIMITED_FRAMERATE_CUTOFF && FrameCadence.isPacing();
    }
}
