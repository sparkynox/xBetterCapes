package dev.sparkynox.xbettercapes.mixin;

import dev.sparkynox.xbettercapes.cape.CapeEntry;
import dev.sparkynox.xbettercapes.cape.CapeRegistry;
import dev.sparkynox.xbettercapes.cape.CapeTextureManager;
import dev.sparkynox.xbettercapes.config.CapeConfig;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Injects into AbstractClientPlayerEntity#getSkinTextures().
 * Replaces capeTexture in the returned SkinTextures record with our custom cape.
 * Works on cracked/offline accounts — no Mojang auth needed.
 * Only affects the local player.
 */
@Mixin(AbstractClientPlayerEntity.class)
public abstract class PlayerEntityRendererMixin {

    @Inject(method = "getSkinTextures", at = @At("RETURN"), cancellable = true)
    private void xbettercapes_overrideCape(CallbackInfoReturnable<SkinTextures> cir) {
        AbstractClientPlayerEntity self = (AbstractClientPlayerEntity) (Object) this;

        // Only override for the local player
        if (!self.isMainPlayer()) return;

        String cfg = CapeConfig.selectedCape;
        if (cfg == null || cfg.equals("NONE") || cfg.equals("builtin:none")) return;

        CapeEntry entry = resolveEntry(cfg);
        if (entry == null) return;

        Identifier tex = CapeTextureManager.getTexture(entry);
        if (tex == null) return; // Still loading — keep original (no cape) for now

        SkinTextures original = cir.getReturnValue();
        cir.setReturnValue(new SkinTextures(
                original.texture(),
                original.textureUrl(),
                tex,   // our custom cape
                tex,   // elytra uses same texture (standard MC behaviour)
                original.model(),
                original.secure()
        ));
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
