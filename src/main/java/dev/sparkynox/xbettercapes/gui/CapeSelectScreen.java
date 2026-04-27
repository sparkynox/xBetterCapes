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

    private static final int CARD_W = 54;
    private static final int CARD_H = 62;
    private static final int CARD_GAP = 6;
    private static final int COLS = 4;
    private static final int PREVIEW_W = 40;
    private static final int PREVIEW_H = 20;

    private TextFieldWidget urlField;
    private List<CapeEntry> capes;

    public CapeSelectScreen() {
        super(Text.literal("xBetter Capes"));
    }

    @Override
    protected void init() {
        capes = CapeRegistry.getBuiltinCapes();

        int cx = this.width / 2;
        int by = this.height - 30;

        urlField = new TextFieldWidget(this.textRenderer,
                cx - 120, by - 24, 200, 18, Text.literal("Cape URL"));
        urlField.setPlaceholder(Text.literal("https://i.imgur.com/xxx.png"));
        urlField.setMaxLength(512);
        this.addDrawableChild(urlField);

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Load URL"), btn -> {
            String url = urlField.getText().trim();
            if (url.startsWith("http://") || url.startsWith("https://")) {
                CapeConfig.selectedCape = CapeRegistry.fromUrl(url).toConfigString();
                CapeConfig.save();
            }
        }).dimensions(cx + 86, by - 24, 60, 18).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("x Close"), btn -> close())
                .dimensions(cx - 25, by, 50, 18).build());
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // Sodium-compatible background — plain fill, no gradient, no blur
        // Two layers: full dark base + slightly lighter panel area
        ctx.fill(0, 0, this.width, this.height, 0xE0050508);

        // Panel background behind the grid
        int gridW = COLS * (CARD_W + CARD_GAP) - CARD_GAP;
        int panelX = (this.width - gridW) / 2 - 8;
        int panelY = 2;
        int panelW = gridW + 16;
        int panelH = this.height - 8;
        ctx.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xCC0D0D18);

        // Panel border
        drawRect(ctx, panelX, panelY, panelW, panelH, 0xFF1A2A3A);

        // Title
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("xBetter Capes"),
                this.width / 2, 8, 0x00E5FF);
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("by SparkyNox & SPA4RKIEE_XD"),
                this.width / 2, 18, 0x557788);

        // Divider line
        ctx.fill(panelX + 4, 28, panelX + panelW - 4, 29, 0xFF1E4A5A);

        // Cape grid
        int startX = (this.width - gridW) / 2;
        int startY = 34;

        for (int i = 0; i < capes.size(); i++) {
            int col = i % COLS;
            int row = i / COLS;
            drawCapeCard(ctx, capes.get(i),
                    startX + col * (CARD_W + CARD_GAP),
                    startY + row * (CARD_H + CARD_GAP),
                    mouseX, mouseY);
        }

        ctx.drawTextWithShadow(textRenderer, Text.literal("Image URL:"),
                this.width / 2 - 120, this.height - 38, 0x557788);

        super.render(ctx, mouseX, mouseY, delta);
    }

    /** Draw a plain border rectangle — Sodium safe, no blending tricks */
    private void drawRect(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x,         y,         x + w,     y + 1,     color); // top
        ctx.fill(x,         y + h - 1, x + w,     y + h,     color); // bottom
        ctx.fill(x,         y,         x + 1,     y + h,     color); // left
        ctx.fill(x + w - 1, y,         x + w,     y + h,     color); // right
    }

    private void drawCapeCard(DrawContext ctx, CapeEntry entry,
                               int x, int y, int mouseX, int mouseY) {
        boolean selected = CapeConfig.selectedCape.equals(entry.toConfigString());
        boolean hovered  = mouseX >= x && mouseX < x + CARD_W
                        && mouseY >= y && mouseY < y + CARD_H;

        // Card fill
        int bg = selected ? 0xFF0A2535 : (hovered ? 0xFF151525 : 0xFF0C0C1A);
        ctx.fill(x, y, x + CARD_W, y + CARD_H, bg);

        // Card border — manual drawRect for Sodium compat
        int borderColor = selected ? 0xFF00AACC : (hovered ? 0xFF2A4455 : 0xFF181828);
        drawRect(ctx, x, y, CARD_W, CARD_H, borderColor);

        // Selected: extra highlight line on top
        if (selected) {
            ctx.fill(x + 1, y + 1, x + CARD_W - 1, y + 2, 0xFF00E5FF);
        }

        // Cape preview
        int imgX = x + (CARD_W - PREVIEW_W) / 2;
        int imgY = y + 6;

        Identifier tex = CapeTextureManager.getTexture(entry);
        if (tex != null && entry.resourcePath != null) {
            ctx.drawTexture(RenderLayer::getGuiTextured, tex,
                    imgX, imgY, 0f, 0f,
                    PREVIEW_W, PREVIEW_H, 64, 32);
        } else if (entry.resourcePath != null) {
            // Loading
            ctx.fill(imgX, imgY, imgX + PREVIEW_W, imgY + PREVIEW_H, 0xFF111120);
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("..."),
                    imgX + PREVIEW_W / 2, imgY + 6, 0x334455);
        } else {
            // No cape slot
            ctx.fill(imgX, imgY, imgX + PREVIEW_W, imgY + PREVIEW_H, 0xFF0A0A14);
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("NONE"),
                    imgX + PREVIEW_W / 2, imgY + 6, 0x334444);
        }

        // Name
        String name = entry.displayName;
        if (textRenderer.getWidth(name) > CARD_W - 4)
            name = textRenderer.trimToWidth(name, CARD_W - 8) + "..";
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(name),
                x + CARD_W / 2, y + CARD_H - 11,
                selected ? 0x00E5FF : (hovered ? 0xCCDDEE : 0x778899));

        // Checkmark
        if (selected)
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("v"),
                    x + CARD_W - 6, y + 4, 0x00FF88);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int gridW = COLS * (CARD_W + CARD_GAP) - CARD_GAP;
            int startX = (this.width - gridW) / 2;
            int startY = 34;
            for (int i = 0; i < capes.size(); i++) {
                int cx = startX + (i % COLS) * (CARD_W + CARD_GAP);
                int cy = startY + (i / COLS) * (CARD_H + CARD_GAP);
                if (mouseX >= cx && mouseX < cx + CARD_W
                 && mouseY >= cy && mouseY < cy + CARD_H) {
                    CapeConfig.selectedCape = capes.get(i).toConfigString();
                    CapeConfig.save();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void close() {
        CapeTextureManager.releaseUrlTextures();
        super.close();
    }

    @Override
    public boolean shouldPause() { return false; }
}
