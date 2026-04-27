package dev.sparkynox.xbettercapes.gui;

import dev.sparkynox.xbettercapes.cape.CapeEntry;
import dev.sparkynox.xbettercapes.cape.CapeRegistry;
import dev.sparkynox.xbettercapes.cape.CapeTextureManager;
import dev.sparkynox.xbettercapes.config.CapeConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.List;

public class CapeSelectScreen extends Screen {

    private static final int CARD_W    = 54;
    private static final int CARD_H    = 62;
    private static final int CARD_GAP  = 6;
    private static final int COLS      = 4;
    private static final int SCROLLBAR_W = 6;

    // Cape in 64x32 tex: pixels 0-21 wide, 0-16 tall (22x17 region)
    // We render that region scaled to PREVIEW size
    private static final int CAPE_U = 0, CAPE_V = 0;
    private static final int CAPE_REGION_W = 22, CAPE_REGION_H = 17;
    private static final int PREVIEW_W = 44, PREVIEW_H = 34; // 2x scale
    private static final int TEX_W = 64,  TEX_H = 32;

    private TextFieldWidget urlField;
    private List<CapeEntry> capes;
    private String statusMsg = "";

    private int scrollOffset = 0;
    private boolean draggingScroll = false;
    private int dragStartY = 0, dragStartOffset = 0;

    private int gridX, gridY, gridW, gridH;
    private int totalContentH;
    private int scrollbarX, scrollbarY, scrollbarH;
    private int bottomBarY;

    public CapeSelectScreen() {
        super(Text.literal("xBetter Capes"));
    }

    @Override
    protected void init() {
        capes = CapeRegistry.getBuiltinCapes();
        int rows      = (int) Math.ceil(capes.size() / (double) COLS);
        int contentGW = COLS * (CARD_W + CARD_GAP) - CARD_GAP;

        bottomBarY    = this.height - 46;
        gridX         = (this.width - contentGW) / 2;
        gridY         = 32;
        gridW         = contentGW;
        gridH         = bottomBarY - gridY - 4;
        totalContentH = rows * (CARD_H + CARD_GAP) - CARD_GAP;
        scrollbarX    = gridX + gridW + 6;
        scrollbarY    = gridY;
        scrollbarH    = gridH;
        scrollOffset  = 0;

        int cx = this.width / 2;

        urlField = new TextFieldWidget(this.textRenderer,
                cx - 125, bottomBarY + 4, 210, 18, Text.literal("Cape URL"));
        urlField.setPlaceholder(Text.literal("Paste image URL here..."));
        urlField.setMaxLength(512);
        this.addDrawableChild(urlField);

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Load"), btn -> {
            String url = urlField.getText().trim();
            if (url.startsWith("http://") || url.startsWith("https://")) {
                statusMsg = "Loading...";
                CapeEntry entry = CapeRegistry.fromUrl(url);
                CapeConfig.selectedCape = entry.toConfigString();
                CapeConfig.save();
                CapeTextureManager.prefetch(entry);
                statusMsg = "Set! Rejoin world to see.";
            } else {
                statusMsg = "Invalid URL!";
            }
        }).dimensions(cx + 90, bottomBarY + 4, 40, 18).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Close"), btn -> close())
                .dimensions(cx - 20, bottomBarY + 26, 40, 14).build());
    }

    private int maxScroll() { return Math.max(0, totalContentH - gridH); }
    private void clampScroll() { scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll())); }
    private int thumbH() {
        if (totalContentH <= gridH) return scrollbarH;
        return Math.max(20, scrollbarH * gridH / totalContentH);
    }
    private int thumbY() {
        int max = maxScroll();
        if (max == 0) return scrollbarY;
        return scrollbarY + (scrollbarH - thumbH()) * scrollOffset / max;
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, this.width, this.height, 0xFF060609);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, this.width, this.height, 0xFF060609);

        int px = gridX - 10, py = 2;
        int pw = gridW + SCROLLBAR_W + 22, ph = this.height - 4;
        ctx.fill(px, py, px + pw, py + ph, 0xFF0C0C16);
        drawBorder(ctx, px, py, pw, ph, 0xFF182030);

        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("xBetter Capes"), this.width / 2, 7, 0x00CCEE);
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("SparkyNox & SPA4RKIEE_XD"), this.width / 2, 17, 0x446677);
        ctx.fill(px + 4, 27, px + pw - 4, 28, 0xFF183040);

        // Scissor clip grid
        ctx.enableScissor(gridX, gridY, gridX + gridW, gridY + gridH);
        for (int i = 0; i < capes.size(); i++) {
            int col = i % COLS;
            int row = i / COLS;
            int cx  = gridX + col * (CARD_W + CARD_GAP);
            int cy  = gridY + row * (CARD_H + CARD_GAP) - scrollOffset;
            if (cy + CARD_H < gridY || cy > gridY + gridH) continue;
            drawCapeCard(ctx, capes.get(i), cx, cy, mouseX, mouseY);
        }
        ctx.disableScissor();

        // Top/bottom fade
        ctx.fill(gridX, gridY, gridX + gridW, gridY + 8, 0xBB060609);
        ctx.fill(gridX, gridY + gridH - 8, gridX + gridW, gridY + gridH, 0xBB060609);

        // Scrollbar
        if (maxScroll() > 0) {
            ctx.fill(scrollbarX, scrollbarY,
                     scrollbarX + SCROLLBAR_W, scrollbarY + scrollbarH, 0xFF0E0E18);
            boolean hov = mouseX >= scrollbarX && mouseX <= scrollbarX + SCROLLBAR_W
                       && mouseY >= thumbY() && mouseY <= thumbY() + thumbH();
            ctx.fill(scrollbarX, thumbY(),
                     scrollbarX + SCROLLBAR_W, thumbY() + thumbH(),
                     (draggingScroll || hov) ? 0xFF00AACC : 0xFF2A4A5A);
        }

        // Bottom bar
        ctx.fill(px + 2, bottomBarY - 2, px + pw - 2, bottomBarY - 1, 0xFF183040);
        ctx.drawTextWithShadow(textRenderer, Text.literal("URL:"),
                this.width / 2 - 125, bottomBarY - 10, 0x446677);
        if (!statusMsg.isEmpty()) {
            int col = statusMsg.startsWith("Invalid") ? 0xFF4444 : 0x00CC88;
            ctx.drawCenteredTextWithShadow(textRenderer,
                    Text.literal(statusMsg), this.width / 2, bottomBarY - 10, col);
        }

        super.render(ctx, mouseX, mouseY, delta);
    }

    private void drawBorder(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x,         y,         x + w,     y + 1,     color);
        ctx.fill(x,         y + h - 1, x + w,     y + h,     color);
        ctx.fill(x,         y,         x + 1,     y + h,     color);
        ctx.fill(x + w - 1, y,         x + w,     y + h,     color);
    }

    private void drawCapeCard(DrawContext ctx, CapeEntry entry,
                               int x, int y, int mouseX, int mouseY) {
        boolean selected = CapeConfig.selectedCape.equals(entry.toConfigString());
        boolean hovered  = mouseX >= x && mouseX < x + CARD_W
                        && mouseY >= y && mouseY < y + CARD_H
                        && mouseY >= gridY && mouseY < gridY + gridH;

        ctx.fill(x, y, x + CARD_W, y + CARD_H,
                selected ? 0xFF0A2030 : (hovered ? 0xFF121220 : 0xFF0A0A16));
        drawBorder(ctx, x, y, CARD_W, CARD_H,
                selected ? 0xFF0099BB : (hovered ? 0xFF223344 : 0xFF141424));
        if (selected)
            ctx.fill(x + 1, y + 1, x + CARD_W - 1, y + 2, 0xFF00BBDD);

        // Center preview in card
        int imgX = x + (CARD_W - PREVIEW_W) / 2;
        int imgY = y + 4;

        Identifier tex = CapeTextureManager.getTexture(entry);
        if (tex != null && entry.resourcePath != null) {
            // Draw ONLY the cape region (22x17) from the 64x32 texture,
            // scaled up to PREVIEW_W x PREVIEW_H
            ctx.drawTexture(RenderLayer::getGuiTextured, tex,
                    imgX, imgY,
                    (float) CAPE_U, (float) CAPE_V,
                    PREVIEW_W, PREVIEW_H,
                    CAPE_REGION_W, CAPE_REGION_H,  // source region size
                    TEX_W, TEX_H);                  // full texture size
        } else if (entry.resourcePath != null) {
            ctx.fill(imgX, imgY, imgX + PREVIEW_W, imgY + PREVIEW_H, 0xFF0E0E1C);
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("..."),
                    imgX + PREVIEW_W / 2, imgY + PREVIEW_H / 2 - 4, 0x223344);
        } else {
            // No Cape slot
            ctx.fill(imgX, imgY, imgX + PREVIEW_W, imgY + PREVIEW_H, 0xFF080810);
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("NONE"),
                    imgX + PREVIEW_W / 2, imgY + PREVIEW_H / 2 - 4, 0x223333);
        }

        String name = entry.displayName;
        if (textRenderer.getWidth(name) > CARD_W - 4)
            name = textRenderer.trimToWidth(name, CARD_W - 8) + "..";
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(name),
                x + CARD_W / 2, y + CARD_H - 10,
                selected ? 0x00BBDD : (hovered ? 0xAABBCC : 0x556677));

        if (selected)
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("v"),
                    x + CARD_W - 6, y + 4, 0x00EE88);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hA, double vA) {
        scrollOffset -= (int)(vA * (CARD_H + CARD_GAP));
        clampScroll();
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (maxScroll() > 0
                    && mouseX >= scrollbarX && mouseX <= scrollbarX + SCROLLBAR_W
                    && mouseY >= thumbY() && mouseY <= thumbY() + thumbH()) {
                draggingScroll = true;
                dragStartY = (int) mouseY;
                dragStartOffset = scrollOffset;
                return true;
            }
            if (mouseY >= gridY && mouseY < gridY + gridH) {
                for (int i = 0; i < capes.size(); i++) {
                    int cx = gridX + (i % COLS) * (CARD_W + CARD_GAP);
                    int cy = gridY + (i / COLS) * (CARD_H + CARD_GAP) - scrollOffset;
                    if (mouseX >= cx && mouseX < cx + CARD_W
                     && mouseY >= cy && mouseY < cy + CARD_H) {
                        CapeConfig.selectedCape = capes.get(i).toConfigString();
                        CapeConfig.save();
                        statusMsg = "";
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        if (draggingScroll && btn == 0 && maxScroll() > 0) {
            int range = scrollbarH - thumbH();
            if (range > 0)
                scrollOffset = dragStartOffset + (int)(my - dragStartY) * maxScroll() / range;
            clampScroll();
            return true;
        }
        return super.mouseDragged(mx, my, btn, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int btn) {
        if (btn == 0) draggingScroll = false;
        return super.mouseReleased(mx, my, btn);
    }

    @Override
    public void close() {
        CapeTextureManager.releaseUrlTextures();
        super.close();
    }

    @Override
    public boolean shouldPause() { return false; }
}
