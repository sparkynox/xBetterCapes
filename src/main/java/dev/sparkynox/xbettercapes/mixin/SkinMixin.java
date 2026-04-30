package dev.sparkynox.xbettercapes.mixin;

import dev.sparkynox.xbettercapes.cape.CapeEntry;
import dev.sparkynox.xbettercapes.cape.CapeRegistry;
import dev.sparkynox.xbettercapes.cape.CapeTextureManager;
import dev.sparkynox.xbettercapes.config.CapeConfig;
import dev.sparkynox.xbettercapes.skin.SkinConfig;
import dev.sparkynox.xbettercapes.skin.SkinFetcher;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Single mixin handles BOTH cape and skin override.
 * Merged to avoid conflict between two separate @Inject on same method.
 *
 * Priority:
 *   1. Cape selected → apply cape, keep original skin
 *   2. Skin selected → apply skin, keep original cape
 *   3. Both selected → apply both
 *   4. None → vanilla behavior
 */
@Mixin(value = AbstractClientPlayerEntity.class, priority = 1500)
public abstract class SkinMixin {

    @Inject(method = "getSkinTextures", at = @At("RETURN"), cancellable = true)
    private void xbettercapes_overrideTextures(CallbackInfoReturnable<SkinTextures> cir) {
        AbstractClientPlayerEntity self = (AbstractClientPlayerEntity)(Object) this;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || !mc.player.equals(self)) return;

        SkinTextures original = cir.getReturnValue();

        // ── Resolve skin texture ──────────────────────────────────────────────
        Identifier skinTex = original.texture(); // default = vanilla
        SkinTextures.Model model = original.model();

        String skinCfg = SkinConfig.selectedSkin;
        if (skinCfg != null && !skinCfg.equals("NONE")) {
            Identifier fetched = SkinFetcher.getSkin(skinCfg);
            if (fetched != null) {
                skinTex = fetched;
                model = "slim".equals(SkinConfig.skinModel)
                        ? SkinTextures.Model.SLIM
                        : SkinTextures.Model.WIDE;
            }
        }

        // ── Resolve cape texture ──────────────────────────────────────────────
        Identifier capeTex = original.capeTexture(); // default = vanilla/null

        String capeCfg = CapeConfig.selectedCape;
        if (capeCfg != null && !capeCfg.equals("NONE") && !capeCfg.equals("builtin:none")) {
            CapeEntry entry = resolveCapEntry(capeCfg);
            if (entry != null) {
                Identifier ct = CapeTextureManager.getTexture(entry);
                if (ct != null) capeTex = ct;
            }
        }

        // ── If nothing changed, skip ──────────────────────────────────────────
        if (skinTex == original.texture() && capeTex == original.capeTexture()) return;

        // ── Apply both ────────────────────────────────────────────────────────
        cir.setReturnValue(new SkinTextures(
                skinTex,
                null,       // null = don't re-fetch from any URL
                capeTex,
                capeTex,    // elytra uses same as cape
                model,
                false       // skip Mojang signature check (cracked compat)
        ));
    }

    private static CapeEntry resolveCapEntry(String cfg) {
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
