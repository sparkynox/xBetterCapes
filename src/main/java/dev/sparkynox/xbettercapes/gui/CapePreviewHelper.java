package dev.sparkynox.xbettercapes.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;

/**
 * Draws the FRONT FACE of a Minecraft cape texture.
 *
 * Works for ANY resolution (64x32, 128x64, 2000x1000, etc.)
 * Front face is always proportionally at:
 *   u1 = 1/64,  u2 = 11/64   (x: 1 to 11 in 64-wide texture)
 *   v1 = 1/32,  v2 = 17/32   (y: 1 to 17 in 32-tall texture)
 *
 * UV is resolution-independent — same ratio works for any size.
 */
public class CapePreviewHelper {

    // Cape front face UV — fixed ratio, works for 64x32, 128x64, 2000x1000, etc.
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
        // UV is always proportional — ignore absolute pixel params
        draw(ctx, tex, dstX, dstY, dstW, dstH, U1, V1, U2, V2);
    }

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
