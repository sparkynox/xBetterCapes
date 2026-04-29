package dev.sparkynox.xbettercapes.mixin;

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
 * Priority 1500 — runs after most mods but before rendering.
 * Completely replaces the returned SkinTextures with our custom skin.
 * textureUrl = null prevents Minecraft from re-fetching from Mojang CDN.
 * secure = false prevents UUID-based validation on cracked accounts.
 */
@Mixin(value = AbstractClientPlayerEntity.class, priority = 1500)
public abstract class SkinMixin {

    @Inject(method = "getSkinTextures", at = @At("RETURN"), cancellable = true)
    private void xbettercapes_overrideSkin(CallbackInfoReturnable<SkinTextures> cir) {
        AbstractClientPlayerEntity self = (AbstractClientPlayerEntity)(Object) this;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || !mc.player.equals(self)) return;

        String cfg = SkinConfig.selectedSkin;
        if (cfg == null || cfg.equals("NONE")) return;

        Identifier skinTex = SkinFetcher.getSkin(cfg);

        // If texture not ready yet, still block server skin from showing
        // by returning our override as soon as texture is available
        if (skinTex == null) return;

        SkinTextures original = cir.getReturnValue();
        SkinTextures.Model model = "slim".equals(SkinConfig.skinModel)
                ? SkinTextures.Model.SLIM
                : SkinTextures.Model.WIDE;

        cir.setReturnValue(new SkinTextures(
                skinTex,   // our skin texture
                null,      // null = don't re-fetch from any URL
                original.capeTexture(),
                original.elytraTexture(),
                model,
                false      // false = skip Mojang signature check (cracked compat)
        ));
    }
}
