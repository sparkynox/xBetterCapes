package dev.sparkynox.xbettercapes.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;

/**
 * Draws the FRONT FACE of a Minecraft cape texture.
 *
 * Cape texture layout (64x32 PNG):
 *   Front face pixels: x=1, y=1, width=10, height=16
 *
 * We use raw VertexConsumer with manual UV — same logic as the website's
 * canvas renderCapeCanvas() function:
 *   u1 = 1/64,  u2 = 11/64
 *   v1 = 1/32,  v2 = 17/32
 *
 * This is the ONLY correct way — drawGuiTexture ignores regionW/regionH.
 */
public class CapePreviewHelper {

    // Front face UV in 64x32 texture
    private static final float U1 = 1f  / 64f;   // 0.015625
    private static final float U2 = 11f / 64f;   // 0.171875
    private static final float V1 = 1f  / 32f;   // 0.03125
    private static final float V2 = 17f / 32f;   // 0.53125

    public static void drawRegion(DrawContext ctx,
                                   Identifier tex,
                                   int dstX, int dstY,
                                   int dstW, int dstH,
                                   int srcX, int srcY,
                                   int texW, int texH) {
        // srcX/srcY/texW/texH params kept for API compat but we use fixed UV above
        draw(ctx, tex, dstX, dstY, dstW, dstH, U1, V1, U2, V2);
    }

    /**
     * Draw any arbitrary UV region — for custom textures with different sizes.
     * @param u1,v1  top-left UV (0.0–1.0)
     * @param u2,v2  bottom-right UV (0.0–1.0)
     */
    public static void draw(DrawContext ctx, Identifier tex,
                             int x, int y, int w, int h,
                             float u1, float v1, float u2, float v2) {
        Matrix4f mat = ctx.getMatrices().peek().getPositionMatrix();
        VertexConsumer vc = ctx.getVertexConsumers().getBuffer(
                RenderLayer.getGuiTextured(tex));

        vc.vertex(mat, x,     y    ).texture(u1, v1).color(0xFFFFFFFF);
        vc.vertex(mat, x,     y + h).texture(u1, v2).color(0xFFFFFFFF);
        vc.vertex(mat, x + w, y + h).texture(u2, v2).color(0xFFFFFFFF);
        vc.vertex(mat, x + w, y    ).texture(u2, v1).color(0xFFFFFFFF);

        ctx.getVertexConsumers().draw();
    }
}