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

@Mixin(AbstractClientPlayerEntity.class)
public abstract class SkinMixin {

    @Inject(method = "getSkinTextures", at = @At("RETURN"), cancellable = true)
    private void xbettercapes_overrideSkin(CallbackInfoReturnable<SkinTextures> cir) {
        AbstractClientPlayerEntity self = (AbstractClientPlayerEntity)(Object)this;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || !mc.player.equals(self)) return;

        String cfg = SkinConfig.selectedSkin;
        if (cfg == null || cfg.equals("NONE")) return;

        Identifier skinTex = SkinFetcher.getSkin(cfg);
        if (skinTex == null) return;

        SkinTextures original = cir.getReturnValue();
        SkinTextures.Model model = "slim".equals(SkinConfig.skinModel)
                ? SkinTextures.Model.SLIM
                : SkinTextures.Model.WIDE;

        cir.setReturnValue(new SkinTextures(
                skinTex,
                original.textureUrl(),
                original.capeTexture(),
                original.elytraTexture(),
                model,
                original.secure()
        ));
    }
}
