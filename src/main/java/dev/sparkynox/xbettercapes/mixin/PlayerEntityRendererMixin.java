package dev.sparkynox.xbettercapes.mixin;

import dev.sparkynox.xbettercapes.cape.CapeEntry;
import dev.sparkynox.xbettercapes.cape.CapeRegistry;
import dev.sparkynox.xbettercapes.cape.CapeTextureManager;
import dev.sparkynox.xbettercapes.config.CapeConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.CapeFeatureRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hooks into CapeFeatureRenderer.render() to substitute our cape texture.
 * Sets capeTexture on the render state before vanilla render runs.
 * No tick loops, no extra threads from this class.
 */
@Mixin(CapeFeatureRenderer.class)
public class PlayerEntityRendererMixin {

    @Inject(
        method = "render(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
        at = @At("HEAD")
    )
    private void xbettercapes_injectCape(
            PlayerEntityRenderState state,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            CallbackInfo ci) {

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        // Only affect local player — match by name from render state
        if (!mc.player.getName().getString().equals(state.name)) return;

        String cfg = CapeConfig.selectedCape;
        if (cfg == null || cfg.equals("NONE") || cfg.equals("builtin:none")) return;

        CapeEntry entry = resolveEntry(cfg);
        if (entry == null) return;

        Identifier tex = CapeTextureManager.getTexture(entry);
        if (tex != null) {
            state.capeTexture = tex;
        }
    }

    private static CapeEntry resolveEntry(String cfg) {
        if (cfg.startsWith("builtin:")) {
            String id = cfg.substring("builtin:".length());
            return CapeRegistry.getBuiltinCapes().stream()
                    .filter(e -> e.id.equals(id))
                    .findFirst().orElse(null);
        }
        if (cfg.startsWith("url:")) {
            return CapeRegistry.fromUrl(cfg.substring("url:".length()));
        }
        return null;
    }
}
