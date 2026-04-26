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

        this.addDrawableChild(ButtonWidget.builder(Text.literal("✖ Close"), btn -> close())
                .dimensions(cx - 25, by, 50, 18).build());
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fillGradient(0, 0, this.width, this.height, 0xD00A0A12, 0xD0101020);
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("§b§lxBetter Capes  §8| §7SparkyNox & SPA4RKIEE_XD"),
                this.width / 2, 8, 0xFFFFFF);
        ctx.fill(this.width / 2 - 120, 18, this.width / 2 + 120, 19, 0xFF00E5FF);

        int gridW = COLS * (CARD_W + CARD_GAP) - CARD_GAP;
        int startX = (this.width - gridW) / 2;
        int startY = 26;

        for (int i = 0; i < capes.size(); i++) {
            int col = i % COLS;
            int row = i / COLS;
            drawCapeCard(ctx, capes.get(i),
                    startX + col * (CARD_W + CARD_GAP),
                    startY + row * (CARD_H + CARD_GAP),
                    mouseX, mouseY);
        }

        ctx.drawTextWithShadow(textRenderer, Text.literal("§7Image URL:"),
                this.width / 2 - 120, this.height - 38, 0xAAAAAA);

        super.render(ctx, mouseX, mouseY, delta);
    }

    private void drawCapeCard(DrawContext ctx, CapeEntry entry,
                               int x, int y, int mouseX, int mouseY) {
        boolean selected = CapeConfig.selectedCape.equals(entry.toConfigString());
        boolean hovered  = mouseX >= x && mouseX < x + CARD_W
                        && mouseY >= y && mouseY < y + CARD_H;

        ctx.fill(x, y, x + CARD_W, y + CARD_H,
                selected ? 0xFF0D2A36 : (hovered ? 0xFF181828 : 0xFF0F0F1C));
        ctx.drawBorder(x, y, CARD_W, CARD_H,
                selected ? 0xFF00E5FF : (hovered ? 0xFF334455 : 0xFF1E1E30));
        if (selected)
            ctx.drawBorder(x - 1, y - 1, CARD_W + 2, CARD_H + 2, 0x5500E5FF);

        int imgX = x + (CARD_W - PREVIEW_W) / 2;
        int imgY = y + 6;

        Identifier tex = CapeTextureManager.getTexture(entry);
        if (tex != null && entry.resourcePath != null) {
            // Correct 1.21.4 signature:
            // drawTexture(Function<Identifier,RenderLayer>, Identifier, x, y, u, v, w, h, texW, texH)
            ctx.drawTexture(RenderLayer::getGuiTextured, tex,
                    imgX, imgY,
                    0f, 0f,
                    PREVIEW_W, PREVIEW_H,
                    64, 32);
        } else if (entry.resourcePath != null) {
            ctx.fill(imgX, imgY, imgX + PREVIEW_W, imgY + PREVIEW_H, 0xFF1A1A2A);
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§8..."),
                    imgX + PREVIEW_W / 2, imgY + 6, 0x555566);
        } else {
            ctx.fill(imgX, imgY, imgX + PREVIEW_W, imgY + PREVIEW_H, 0xFF111118);
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§c✖"),
                    imgX + PREVIEW_W / 2, imgY + 6, 0xFF4444);
        }

        String name = entry.displayName;
        if (textRenderer.getWidth(name) > CARD_W - 4)
            name = textRenderer.trimToWidth(name, CARD_W - 8) + "..";
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(name),
                x + CARD_W / 2, y + CARD_H - 11,
                selected ? 0x00E5FF : (hovered ? 0xEEEEEE : 0xAAAAAA));

        if (selected)
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§a✔"),
                    x + CARD_W - 8, y + 4, 0x00FF88);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int gridW = COLS * (CARD_W + CARD_GAP) - CARD_GAP;
            int startX = (this.width - gridW) / 2;
            int startY = 26;
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
