package dev.sparkynox.xbettercapes.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;

/**
 * Draws the FRONT FACE of a Minecraft cape texture.
 * Works for ANY resolution (64x32, 128x64, 2000x1000, etc.)
 *
 * Front face in 64x32 cape texture: x=1, y=1, w=10, h=16
 * Same proportions apply to any resolution cape.
 *
 * Correct 1.21.4 Yarn API:
 * drawTexture(renderLayers, sprite, x, y, u, v, width, height, texW, texH)
 * Where u,v = pixel offset in source texture (float)
 * and texW,texH = full texture dimensions
 *
 * We set texW/texH to match whatever proportion we want,
 * so u=1,v=1,texW=64,texH=32 always gives the correct front face.
 */
public class CapePreviewHelper {

    public static void drawRegion(DrawContext ctx,
                                   Identifier tex,
                                   int dstX, int dstY,
                                   int dstW, int dstH,
                                   int srcX, int srcY,
                                   int texW, int texH) {
        // drawTexture(layer, sprite, x, y, u, v, drawW, drawH, texW, texH)
        // u=1, v=1 = pixel 1,1 in a 64x32 texture = front face top-left
        // This works for ANY resolution because GPU normalizes u/texW, v/texH
        ctx.drawTexture(
            RenderLayer::getGuiTextured,
            tex,
            dstX, dstY,
            1f, 1f,     // u, v — front face starts at pixel 1,1
            dstW, dstH, // draw size on screen
            64, 32      // reference texture size (always 64x32 ratio)
        );
    }
}
