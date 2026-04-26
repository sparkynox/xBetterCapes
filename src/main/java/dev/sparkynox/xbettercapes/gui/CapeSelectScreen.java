package dev.sparkynox.xbettercapes.gui;

import dev.sparkynox.xbettercapes.cape.CapeEntry;
import dev.sparkynox.xbettercapes.cape.CapeRegistry;
import dev.sparkynox.xbettercapes.cape.CapeTextureManager;
import dev.sparkynox.xbettercapes.config.CapeConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.List;

/**
 * Cape selection GUI.
 * Layout:
 *   - Title bar at top
 *   - Grid of cape cards (image + name), 4 per row
 *   - Selected cape highlighted with cyan border
 *   - URL input field at bottom + "Load" button
 *   - "Close" button
 */
public class CapeSelectScreen extends Screen {

    private static final int CARD_W = 46;
    private static final int CARD_H = 56;
    private static final int CARD_GAP = 6;
    private static final int COLS = 4;

    // Cape preview card = 64x32 texture → we show 32x16 slice (the cape part)
    private static final int PREVIEW_W = 36;
    private static final int PREVIEW_H = 18;

    private TextFieldWidget urlField;
    private List<CapeEntry> capes;

    public CapeSelectScreen() {
        super(Text.literal("xBetter Capes"));
    }

    @Override
    protected void init() {
        capes = CapeRegistry.getBuiltinCapes();

        int centerX = this.width / 2;
        int bottomY = this.height - 30;

        // URL input field
        urlField = new TextFieldWidget(
                this.textRenderer,
                centerX - 120, bottomY - 22, 200, 18,
                Text.literal("Cape URL")
        );
        urlField.setPlaceholder(Text.literal("https://i.imgur.com/xxx.png"));
        urlField.setMaxLength(512);
        this.addDrawableChild(urlField);

        // Load URL button
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Load URL"), btn -> {
            String url = urlField.getText().trim();
            if (url.startsWith("http://") || url.startsWith("https://")) {
                CapeEntry urlCape = CapeRegistry.fromUrl(url);
                CapeConfig.selectedCape = urlCape.toConfigString();
                CapeConfig.save();
            }
        }).dimensions(centerX + 86, bottomY - 22, 60, 18).build());

        // Close button
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Close"), btn -> close())
                .dimensions(centerX - 25, bottomY, 50, 18).build());
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // Dark translucent background
        ctx.fillGradient(0, 0, this.width, this.height,
                0xCC0A0A0F, 0xCC0D0D1A);

        // Title
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("§b§lxBetter Capes §7by SparkyNox & SPA4RKIEE_XD"),
                this.width / 2, 10, 0xFFFFFF);

        // Grid
        int startX = (this.width - (COLS * (CARD_W + CARD_GAP) - CARD_GAP)) / 2;
        int startY = 28;

        for (int i = 0; i < capes.size(); i++) {
            CapeEntry entry = capes.get(i);
            int col = i % COLS;
            int row = i / COLS;
            int cx = startX + col * (CARD_W + CARD_GAP);
            int cy = startY + row * (CARD_H + CARD_GAP);

            drawCapeCard(ctx, entry, cx, cy, mouseX, mouseY);
        }

        super.render(ctx, mouseX, mouseY, delta);
    }

    private void drawCapeCard(DrawContext ctx, CapeEntry entry, int x, int y,
                              int mouseX, int mouseY) {
        boolean selected = CapeConfig.selectedCape.equals(entry.toConfigString());
        boolean hovered  = mouseX >= x && mouseX < x + CARD_W
                        && mouseY >= y && mouseY < y + CARD_H;

        // Card background
        int bg = selected ? 0xFF1A3A4A : (hovered ? 0xFF1A1A2E : 0xFF12121F);
        ctx.fill(x, y, x + CARD_W, y + CARD_H, bg);

        // Border
        int borderColor = selected ? 0xFF00E5FF : (hovered ? 0xFF444466 : 0xFF222233);
        ctx.drawBorder(x, y, CARD_W, CARD_H, borderColor);

        // Cape preview texture
        Identifier tex = CapeTextureManager.getTexture(entry);
        int imgX = x + (CARD_W - PREVIEW_W) / 2;
        int imgY = y + 4;

        if (tex != null && entry.resourcePath != null) {
            // Minecraft cape texture is 64x32; cape itself is at u=0,v=0 size 22x17 (scaled on 64x32)
            // We'll render the full texture scaled into the preview box
            ctx.drawTexture(tex, imgX, imgY, PREVIEW_W, PREVIEW_H, 0, 0, 22, 17, 64, 32);
        } else {
            // Placeholder grey box while loading
            ctx.fill(imgX, imgY, imgX + PREVIEW_W, imgY + PREVIEW_H, 0xFF333344);
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("..."),
                    imgX + PREVIEW_W / 2, imgY + 4, 0x888888);
        }

        // Cape name
        String name = entry.displayName;
        if (textRenderer.getWidth(name) > CARD_W - 2) {
            name = textRenderer.trimToWidth(name, CARD_W - 6) + "..";
        }
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(name),
                x + CARD_W / 2, y + CARD_H - 14, selected ? 0x00E5FF : 0xCCCCCC);

        // Selected checkmark
        if (selected) {
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§a✔"),
                    x + CARD_W / 2, y + CARD_H - 24, 0x00FF88);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int startX = (this.width - (COLS * (CARD_W + CARD_GAP) - CARD_GAP)) / 2;
            int startY = 28;

            for (int i = 0; i < capes.size(); i++) {
                CapeEntry entry = capes.get(i);
                int col = i % COLS;
                int row = i / COLS;
                int cx = startX + col * (CARD_W + CARD_GAP);
                int cy = startY + row * (CARD_H + CARD_GAP);

                if (mouseX >= cx && mouseX < cx + CARD_W
                 && mouseY >= cy && mouseY < cy + CARD_H) {
                    CapeConfig.selectedCape = entry.toConfigString();
                    CapeConfig.save();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void close() {
        // Release URL textures from VRAM when GUI closes
        CapeTextureManager.releaseUrlTextures();
        super.close();
    }

    @Override
    public boolean shouldPause() {
        return false; // Don't pause the game
    }
}
