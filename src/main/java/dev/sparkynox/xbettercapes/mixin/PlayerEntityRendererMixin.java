package dev.sparkynox.xbettercapes.mixin;

import dev.sparkynox.xbettercapes.cape.CapeEntry;
import dev.sparkynox.xbettercapes.cape.CapeRegistry;
import dev.sparkynox.xbettercapes.cape.CapeTextureManager;
import dev.sparkynox.xbettercapes.config.CapeConfig;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Overrides the cape texture returned for the LOCAL player only.
 * Remote players are unaffected — no server-side changes, no packets.
 *
 * We hook into getCapeTexture() which is called by the renderer each frame.
 * No tick loops, no scheduled tasks, no extra threads from this class.
 */
@Mixin(PlayerEntityRenderer.class)
public class PlayerEntityRendererMixin {

    @Inject(
        method = "getCapeTexture",
        at = @At("HEAD"),
        cancellable = true
    )
    private void xbettercapes_overrideCape(
            AbstractClientPlayerEntity player,
            CallbackInfoReturnable<Identifier> cir) {

        // Only override for the local player
        net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();
        if (mc.player == null || !mc.player.equals(player)) return;

        String cfg = CapeConfig.selectedCape;
        if (cfg == null || cfg.equals("NONE") || cfg.equals("builtin:none")) return;

        CapeEntry entry = resolveEntry(cfg);
        if (entry == null) return;

        Identifier tex = CapeTextureManager.getTexture(entry);
        if (tex != null) {
            cir.setReturnValue(tex);
        }
        // If tex is null (still loading), fall through to vanilla (no cape shown yet)
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
