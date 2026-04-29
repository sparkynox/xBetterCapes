package dev.sparkynox.xbettercapes.command;

import dev.sparkynox.xbettercapes.skin.SkinConfig;
import dev.sparkynox.xbettercapes.skin.SkinFetcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public class XSkinCommand {

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(
                ClientCommandManager.literal("xskin")

                    // /xskin reset
                    .then(ClientCommandManager.literal("reset")
                        .executes(ctx -> {
                            SkinConfig.selectedSkin = "NONE";
                            SkinConfig.save();
                            SkinFetcher.clearCache();
                            msg("Skin reset to default.");
                            return 1;
                        }))

                    // /xskin model classic|slim
                    .then(ClientCommandManager.literal("model")
                        .then(ClientCommandManager.argument("type",
                                StringArgumentType.word())
                            .executes(ctx -> {
                                String t = StringArgumentType.getString(ctx, "type").toLowerCase();
                                if (!t.equals("classic") && !t.equals("slim")) {
                                    msg("Usage: /xskin model classic|slim");
                                    return 0;
                                }
                                SkinConfig.skinModel = t;
                                SkinConfig.save();
                                msg("Model set to: " + t);
                                return 1;
                            })))

                    // /xskin check — debug: shows current skin state
                    .then(ClientCommandManager.literal("check")
                        .executes(ctx -> {
                            String cfg = SkinConfig.selectedSkin;
                            msg("Config: " + cfg);
                            msg("Model: " + SkinConfig.skinModel);
                            if (!cfg.equals("NONE")) {
                                net.minecraft.util.Identifier tex =
                                    dev.sparkynox.xbettercapes.skin.SkinFetcher.getSkin(cfg);
                                msg("Texture in cache: " + (tex != null ? tex.toString() : "NULL - still loading"));
                                if (ctx.getSource().getClient().player != null) {
                                    net.minecraft.client.util.SkinTextures st =
                                        ctx.getSource().getClient().player.getSkinTextures();
                                    msg("Active skin tex: " + st.texture());
                                    msg("Active model: " + st.model());
                                }
                            }
                            return 1;
                        }))

                    // /xskin <playerName or URL>
                    .then(ClientCommandManager.argument("target",
                            StringArgumentType.greedyString())
                        .executes(ctx -> {
                            String input = StringArgumentType.getString(ctx, "target").trim();
                            SkinFetcher.clearCache();

                            if (input.startsWith("http://") || input.startsWith("https://")) {
                                SkinConfig.selectedSkin = "url:" + input;
                                msg("Loading skin from URL...");
                            } else {
                                SkinConfig.selectedSkin = "player:" + input;
                                msg("Loading skin for: " + input);
                            }
                            SkinConfig.save();
                            SkinFetcher.getSkin(SkinConfig.selectedSkin);

                            // Poll async result and show feedback in chat
                            new Thread(() -> {
                                for (int i = 0; i < 30; i++) {
                                    try { Thread.sleep(500); } catch (Exception ignored) {}
                                    String s = SkinFetcher.statusMsg;
                                    if (s.contains("loaded") || s.contains("Rejoin")) {
                                        MinecraftClient.getInstance().execute(() ->
                                                msg("Done! Rejoin world to see your skin."));
                                        return;
                                    }
                                    if (s.startsWith("Error") || s.startsWith("Player not") ||
                                        s.startsWith("No skin") || s.startsWith("Not a valid") ||
                                        s.startsWith("Could") || s.startsWith("Download") ||
                                        s.startsWith("Empty")) {
                                        final String err = s;
                                        MinecraftClient.getInstance().execute(() -> msgErr(err));
                                        return;
                                    }
                                }
                            }, "xBC-CmdFeedback").start();
                            return 1;
                        }))

                    // /xskin — show current status
                    .executes(ctx -> {
                        if (SkinConfig.selectedSkin.equals("NONE")) {
                            msg("No skin set. Usage:");
                            msg("  /xskin <PlayerName>  — steal any player's skin");
                            msg("  /xskin <URL>         — load from texture URL");
                            msg("  /xskin model classic|slim");
                            msg("  /xskin reset");
                        } else {
                            msg("Active: " + SkinConfig.selectedSkin);
                            msg("Model: " + SkinConfig.skinModel);
                            msg("Use /xskin reset to remove.");
                        }
                        return 1;
                    })
            );
        });
    }

    private static void msg(String text) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null)
            mc.player.sendMessage(Text.literal("\u00a7b[xSkin]\u00a7r " + text), false);
    }

    private static void msgErr(String text) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null)
            mc.player.sendMessage(Text.literal("\u00a7c[xSkin] " + text), false);
    }
}
