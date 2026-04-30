package dev.sparkynox.xbettercapes.mixin;

import net.minecraft.client.render.entity.feature.CapeFeatureRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Kept as placeholder — cape + skin override is now handled entirely
 * in SkinMixin via getSkinTextures() injection.
 * This class is intentionally empty but must remain registered in mixins.json.
 */
@Mixin(CapeFeatureRenderer.class)
public class PlayerEntityRendererMixin {
    // Cape rendering handled by SkinMixin
}
