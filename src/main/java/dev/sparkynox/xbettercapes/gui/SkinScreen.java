package dev.sparkynox.xbettercapes.gui;

import dev.sparkynox.xbettercapes.skin.SkinConfig;
import dev.sparkynox.xbettercapes.skin.SkinFetcher;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public class SkinScreen extends Screen {

    private final Screen parent;
    private TextFieldWidget inputField;
    private String mode = "player"; // "player" or "url"
    private String statusMsg = "";

    public SkinScreen(Screen parent) {
        super(Text.literal("xBetter Skins"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int cy = this.height / 2;

        // Input field
        inputField = new TextFieldWidget(this.textRenderer,
                cx - 130, cy - 8, 260, 18, Text.literal("Input"));
        inputField.setMaxLength(512);
        inputField.setFocused(true);
        updatePlaceholder();
        this.addDrawableChild(inputField);

        // Mode toggle button
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Mode: Player Name"), btn -> {
                    mode = mode.equals("player") ? "url" : "player";
                    btn.setMessage(Text.literal("Mode: " + (mode.equals("player") ? "Player Name" : "Texture URL")));
                    updatePlaceholder();
                }).dimensions(cx - 130, cy - 32, 170, 16).build());

        // Model toggle (Classic / Slim)
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Model: " + capitalize(SkinConfig.skinModel)), btn -> {
                    SkinConfig.skinModel = SkinConfig.skinModel.equals("classic") ? "slim" : "classic";
                    SkinConfig.save();
                    btn.setMessage(Text.literal("Model: " + capitalize(SkinConfig.skinModel)));
                }).dimensions(cx + 46, cy - 32, 84, 16).build());

        // Apply button
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Apply Skin"), btn -> applySkin())
                .dimensions(cx - 60, cy + 16, 80, 18).build());

        // Reset button
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Reset"), btn -> {
            SkinConfig.selectedSkin = "NONE";
            SkinConfig.save();
            SkinFetcher.clearCache();
            statusMsg = "Skin reset!";
        }).dimensions(cx + 26, cy + 16, 50, 18).build());

        // Back button
        this.addDrawableChild(ButtonWidget.builder(Text.literal("< Back"),
                btn -> this.client.setScreen(parent))
                .dimensions(cx - 130, cy + 16, 50, 18).build());
    }

    private void updatePlaceholder() {
        if (inputField == null) return;
        inputField.setPlaceholder(mode.equals("player")
                ? Text.literal("Enter player name (e.g. Notch)")
                : Text.literal("https://textures.minecraft.net/..."));
    }

    private void applySkin() {
        String input = inputField.getText().trim();
        if (input.isEmpty()) { statusMsg = "Enter a name or URL!"; return; }

        SkinFetcher.clearCache();

        if (mode.equals("player")) {
            SkinConfig.selectedSkin = "player:" + input;
        } else {
            if (!input.startsWith("http://") && !input.startsWith("https://")) {
                statusMsg = "URL must start with http(s)://";
                return;
            }
            SkinConfig.selectedSkin = "url:" + input;
        }
        SkinConfig.save();
        statusMsg = "Loading skin...";
        // Kick off fetch
        SkinFetcher.getSkin(SkinConfig.selectedSkin);
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, this.width, this.height, 0xFF060609);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, this.width, this.height, 0xFF060609);

        int cx = this.width / 2;
        int cy = this.height / 2;

        // Panel
        ctx.fill(cx-140, cy-48, cx+140, cy+42, 0xFF0C0C18);
        drawBorder(ctx, cx-140, cy-48, 280, 90, 0xFF182030);

        // Title
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("xBetter Skins"), cx, cy-42, 0x00CCEE);
        ctx.fill(cx-136, cy-33, cx+136, cy-32, 0xFF183040);

        // Current skin info
        String current = SkinConfig.selectedSkin.equals("NONE")
                ? "No skin set (using default)"
                : SkinConfig.selectedSkin;
        if (current.length() > 38) current = current.substring(0, 35) + "...";
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("Current: " + current), cx, cy - 22, 0x446677);

        // Status from fetcher or local
        String status = SkinFetcher.statusMsg.isEmpty() ? statusMsg : SkinFetcher.statusMsg;
        if (!status.isEmpty()) {
            boolean err = status.startsWith("Error")||status.startsWith("Not")||
                         status.startsWith("Could")||status.startsWith("Player not")||
                         status.startsWith("No skin")||status.startsWith("Download")||
                         status.startsWith("URL must")||status.startsWith("Enter");
            ctx.drawCenteredTextWithShadow(textRenderer,
                    Text.literal(status), cx, cy + 38, err ? 0xFF4444 : 0x00CC88);
        }

        super.render(ctx, mouseX, mouseY, delta);
    }

    private void drawBorder(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x,     y,     x+w, y+1,   color);
        ctx.fill(x,     y+h-1, x+w, y+h,   color);
        ctx.fill(x,     y,     x+1, y+h,   color);
        ctx.fill(x+w-1, y,     x+w, y+h,   color);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 257 || keyCode == 335) { applySkin(); return true; }
        if (keyCode == 256) { this.client.setScreen(parent); return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldPause() { return false; }
}
package dev.sparkynox.xbettercapes.gui;

import dev.sparkynox.xbettercapes.skin.SkinConfig;
import dev.sparkynox.xbettercapes.skin.SkinFetcher;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public class SkinScreen extends Screen {

    private final Screen parent;
    private TextFieldWidget inputField;
    private String mode = "player"; // "player" or "url"
    private String statusMsg = "";

    public SkinScreen(Screen parent) {
        super(Text.literal("xBetter Skins"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int cy = this.height / 2;

        // Input field
        inputField = new TextFieldWidget(this.textRenderer,
                cx - 130, cy - 8, 260, 18, Text.literal("Input"));
        inputField.setMaxLength(512);
        inputField.setFocused(true);
        updatePlaceholder();
        this.addDrawableChild(inputField);

        // Mode toggle button
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Mode: Player Name"), btn -> {
                    mode = mode.equals("player") ? "url" : "player";
                    btn.setMessage(Text.literal("Mode: " + (mode.equals("player") ? "Player Name" : "Texture URL")));
                    updatePlaceholder();
                }).dimensions(cx - 130, cy - 32, 170, 16).build());

        // Model toggle (Classic / Slim)
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Model: " + capitalize(SkinConfig.skinModel)), btn -> {
                    SkinConfig.skinModel = SkinConfig.skinModel.equals("classic") ? "slim" : "classic";
                    SkinConfig.save();
                    btn.setMessage(Text.literal("Model: " + capitalize(SkinConfig.skinModel)));
                }).dimensions(cx + 46, cy - 32, 84, 16).build());

        // Apply button
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Apply Skin"), btn -> applySkin())
                .dimensions(cx - 60, cy + 16, 80, 18).build());

        // Reset button
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Reset"), btn -> {
            SkinConfig.selectedSkin = "NONE";
            SkinConfig.save();
            SkinFetcher.clearCache();
            statusMsg = "Skin reset!";
        }).dimensions(cx + 26, cy + 16, 50, 18).build());

        // Back button
        this.addDrawableChild(ButtonWidget.builder(Text.literal("< Back"),
                btn -> this.client.setScreen(parent))
                .dimensions(cx - 130, cy + 16, 50, 18).build());
    }

    private void updatePlaceholder() {
        if (inputField == null) return;
        inputField.setPlaceholder(mode.equals("player")
                ? Text.literal("Enter player name (e.g. Notch)")
                : Text.literal("https://textures.minecraft.net/..."));
    }

    private void applySkin() {
        String input = inputField.getText().trim();
        if (input.isEmpty()) { statusMsg = "Enter a name or URL!"; return; }

        SkinFetcher.clearCache();

        if (mode.equals("player")) {
            SkinConfig.selectedSkin = "player:" + input;
        } else {
            if (!input.startsWith("http://") && !input.startsWith("https://")) {
                statusMsg = "URL must start with http(s)://";
                return;
            }
            SkinConfig.selectedSkin = "url:" + input;
        }
        SkinConfig.save();
        statusMsg = "Loading skin...";
        // Kick off fetch
        SkinFetcher.getSkin(SkinConfig.selectedSkin);
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, this.width, this.height, 0xFF060609);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, this.width, this.height, 0xFF060609);

        int cx = this.width / 2;
        int cy = this.height / 2;

        // Panel
        ctx.fill(cx-140, cy-48, cx+140, cy+42, 0xFF0C0C18);
        drawBorder(ctx, cx-140, cy-48, 280, 90, 0xFF182030);

        // Title
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("xBetter Skins"), cx, cy-42, 0x00CCEE);
        ctx.fill(cx-136, cy-33, cx+136, cy-32, 0xFF183040);

        // Current skin info
        String current = SkinConfig.selectedSkin.equals("NONE")
                ? "No skin set (using default)"
                : SkinConfig.selectedSkin;
        if (current.length() > 38) current = current.substring(0, 35) + "...";
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("Current: " + current), cx, cy - 22, 0x446677);

        // Status from fetcher or local
        String status = SkinFetcher.statusMsg.isEmpty() ? statusMsg : SkinFetcher.statusMsg;
        if (!status.isEmpty()) {
            boolean err = status.startsWith("Error")||status.startsWith("Not")||
                         status.startsWith("Could")||status.startsWith("Player not")||
                         status.startsWith("No skin")||status.startsWith("Download")||
                         status.startsWith("URL must")||status.startsWith("Enter");
            ctx.drawCenteredTextWithShadow(textRenderer,
                    Text.literal(status), cx, cy + 38, err ? 0xFF4444 : 0x00CC88);
        }

        super.render(ctx, mouseX, mouseY, delta);
    }

    private void drawBorder(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x,     y,     x+w, y+1,   color);
        ctx.fill(x,     y+h-1, x+w, y+h,   color);
        ctx.fill(x,     y,     x+1, y+h,   color);
        ctx.fill(x+w-1, y,     x+w, y+h,   color);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 257 || keyCode == 335) { applySkin(); return true; }
        if (keyCode == 256) { this.client.setScreen(parent); return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldPause() { return false; }
}
