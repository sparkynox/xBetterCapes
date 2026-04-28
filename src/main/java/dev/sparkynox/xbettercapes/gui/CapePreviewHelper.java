package dev.sparkynox.xbettercapes.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;

/**
 * Draws a sub-region of a raw PNG texture using drawGuiTexture —
 * the correct 1.21.4 API for drawing a region from a raw texture.
 *
 * drawGuiTexture(renderLayers, sprite, texW, texH, u, v, x, y, drawW, drawH)
 * This draws a (drawW x drawH) rectangle from the texture region starting at (u,v).
 */
public class CapePreviewHelper {

    /**
     * @param tex       registered texture identifier
     * @param dstX,dstY screen position
     * @param dstW,dstH screen draw size
     * @param srcX,srcY pixel top-left in texture (u,v)
     * @param texW,texH full texture dimensions
     */
    public static void drawRegion(DrawContext ctx,
                                   Identifier tex,
                                   int dstX, int dstY,
                                   int dstW, int dstH,
                                   int srcX, int srcY,
                                   int texW, int texH) {
        // drawGuiTexture: (renderLayers, sprite, textureWidth, textureHeight,
        //                   u, v, x, y, width, height)
        ctx.drawGuiTexture(
                RenderLayer::getGuiTextured,
                tex,
                texW, texH,   // full texture size
                srcX, srcY,   // u, v = top-left of region to draw
                dstX, dstY,   // screen position
                dstW, dstH    // draw size (also = region size in this overload)
        );
    }
}
