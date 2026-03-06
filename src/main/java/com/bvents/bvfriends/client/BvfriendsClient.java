package com.bvents.bvfriends.client;

import com.bvents.bvfriends.client.config.BvfriendsConfigManager;
import com.bvents.bvfriends.client.friend.FriendService;
import com.bvents.bvfriends.client.util.FriendStyleParser;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

public class BvfriendsClient implements ClientModInitializer {
    private static final String FRIEND_TEAM_NAME = "bvfriends_local";
    private static final double FRIEND_TOGGLE_DISTANCE = 40.0;
    private static final long TOGGLE_COOLDOWN_MS = 250L;
    private static final Set<UUID> LOCAL_GLOW_PLAYERS = ConcurrentHashMap.newKeySet();
    private static KeyBinding toggleFriendKey;
    private static KeyBinding openConfigKey;
    private long lastToggleMillis;
    private final Set<String> teamedFriends = new HashSet<>();
    private final Map<String, String> previousTeamByPlayer = new HashMap<>();

    @Override
    public void onInitializeClient() {
        BvfriendsConfigManager.load();
        registerKeybinds();
        registerTickHandlers();
        registerCommands();
    }

    private void registerKeybinds() {
        toggleFriendKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.bvfriends.toggle_friend",
                InputUtil.Type.MOUSE,
                GLFW.GLFW_MOUSE_BUTTON_MIDDLE,
                KeyBinding.Category.MISC
        ));
        openConfigKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.bvfriends.open_config",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                KeyBinding.Category.MISC
        ));
    }

    private void registerTickHandlers() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleFriendKey.wasPressed()) {
                long now = System.currentTimeMillis();
                if (now - lastToggleMillis >= TOGGLE_COOLDOWN_MS) {
                    lastToggleMillis = now;
                    toggleFriendFromCrosshair(client);
                }
            }
            while (openConfigKey.wasPressed()) {
                openConfigScreen(client);
            }
            updateFriendTeam(client);
        });
    }

    private void registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(buildFriendCommand("friend"));
            dispatcher.register(buildFriendCommand("friends"));
        });
    }

    private LiteralArgumentBuilder<FabricClientCommandSource> buildFriendCommand(String root) {
        return ClientCommandManager.literal(root)
                        .executes(ctx -> {
                            openConfigScreen(ctx.getSource().getClient());
                            return 1;
                        })
                        .then(ClientCommandManager.literal("add")
                                .then(ClientCommandManager.argument("name", StringArgumentType.word())
                                        .suggests(this::sggonlineplaters)
                                        .executes(ctx -> {
                                            String name = StringArgumentType.getString(ctx, "name");
                                            boolean added = FriendService.addFriend(name);
                                            sendResult(ctx.getSource().getClient(), name, added, true);
                                            return 1;
                                        })))
                        .then(ClientCommandManager.literal("remove")
                                .then(ClientCommandManager.argument("name", StringArgumentType.word())
                                        .suggests(this::sggfriend)
                                        .executes(ctx -> {
                                            String name = StringArgumentType.getString(ctx, "name");
                                            boolean removed = FriendService.removeFriend(name);
                                            sendResult(ctx.getSource().getClient(), name, removed, false);
                                            return 1;
                                        })))
        ;
    }

    private void openConfigScreen(MinecraftClient client) {
        client.send(() -> client.setScreen(BvfriendsConfigManager.createScreen(client.currentScreen)));
    }

    private void toggleFriendFromCrosshair(MinecraftClient client) {
        PlayerEntity player = findLookedAtPlayer(client, FRIEND_TOGGLE_DISTANCE);
        if (player == null) {
            sendSystemMessage(client, Text.translatable("message.bvfriends.look_at_player").formatted(Formatting.YELLOW));
            return;
        }

        String playerName = player.getGameProfile().name();
        boolean added = FriendService.toggleFriend(playerName);
        sendResult(client, playerName, true, added);
    }

    private PlayerEntity findLookedAtPlayer(MinecraftClient client, double maxDistance) {
        if (client.world == null) {
            return null;
        }

        Entity camera = client.getCameraEntity();
        if (camera == null) {
            return null;
        }

        Vec3d start = camera.getCameraPosVec(1.0F);
        Vec3d direction = camera.getRotationVec(1.0F);
        Vec3d end = start.add(direction.multiply(maxDistance));
        Box box = camera.getBoundingBox().stretch(direction.multiply(maxDistance)).expand(1.0, 1.0, 1.0);

        EntityHitResult hit = ProjectileUtil.raycast(
                camera,
                start,
                end,
                box,
                entity -> entity instanceof PlayerEntity && entity.isAlive() && !entity.isSpectator(),
                maxDistance * maxDistance
        );

        if (hit == null || !(hit.getEntity() instanceof PlayerEntity target)) {
            return null;
        }

        if (start.squaredDistanceTo(hit.getPos()) > maxDistance * maxDistance) {
            return null;
        }

        return target;
    }

    private void sendResult(MinecraftClient client, String name, boolean changed, boolean added) {
        if (!changed) {
            sendSystemMessage(client, Text.translatable("message.bvfriends.friend_already", name).formatted(Formatting.GRAY));
            return;
        }

        MutableText message = Text.translatable("message.bvfriends.friend_prefix").formatted(Formatting.GRAY)
                .append(Text.literal(name).formatted(Formatting.AQUA))
                .append(Text.translatable(added ? "message.bvfriends.friend_added_suffix" : "message.bvfriends.friend_removed_suffix")
                        .formatted(added ? Formatting.GREEN : Formatting.RED));
        sendSystemMessage(client, message);
    }

    private void sendSystemMessage(MinecraftClient client, Text text) {
        if (client.player != null) {
            client.player.sendMessage(text, false);
        }
    }

    private CompletableFuture<Suggestions> sggonlineplaters(com.mojang.brigadier.context.CommandContext<?> context, SuggestionsBuilder builder) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getNetworkHandler() == null) {
            return builder.buildFuture();
        }
        String remaining = builder.getRemaining().toLowerCase();
        for (var entry : client.getNetworkHandler().getPlayerList()) {
            String name = entry.getProfile().name();
            if (name.toLowerCase().startsWith(remaining)) {
                builder.suggest(name);
            }
        }
        return builder.buildFuture();
    }

    private CompletableFuture<Suggestions> sggfriend(com.mojang.brigadier.context.CommandContext<?> context, SuggestionsBuilder builder) {
        String remaining = builder.getRemaining().toLowerCase();
        for (String friend : FriendService.getSortedFriends()) {
            if (friend.toLowerCase().startsWith(remaining)) {
                builder.suggest(friend);
            }
        }
        return builder.buildFuture();
    }

    private void updateFriendTeam(MinecraftClient client) {
        if (client.world == null || client.getNetworkHandler() == null) {
            teamedFriends.clear();
            previousTeamByPlayer.clear();
            LOCAL_GLOW_PLAYERS.clear();
            return;
        }

        Scoreboard scoreboard = client.world.getScoreboard();
        Team team = scoreboard.getTeam(FRIEND_TEAM_NAME);
        if (team == null) {
            team = scoreboard.addTeam(FRIEND_TEAM_NAME);
        }

        team.setColor(FriendStyleParser.parseMainColorFormatting(BvfriendsConfigManager.get().friendOutlineStyle, Formatting.GREEN));

        Set<String> currentFriends = new HashSet<>();
        boolean outlineEnabled = BvfriendsConfigManager.get().friendOutlineEnabled;

        for (PlayerEntity player : client.world.getPlayers()) {
            String name = player.getGameProfile().name();
            boolean isFriend = FriendService.isFriend(name);
            Team currentTeam = scoreboard.getScoreHolderTeam(name);

            if (isFriend && outlineEnabled) {
                currentFriends.add(name);
                player.setGlowing(true);
                LOCAL_GLOW_PLAYERS.add(player.getUuid());

                if (!previousTeamByPlayer.containsKey(name)) {
                    previousTeamByPlayer.put(name, currentTeam == null ? "" : currentTeam.getName());
                }

                if (currentTeam != team) {
                    scoreboard.addScoreHolderToTeam(name, team);
                }
            } else {
                player.setGlowing(false);
                LOCAL_GLOW_PLAYERS.remove(player.getUuid());
                if (currentTeam == team) {
                    scoreboard.clearTeam(name);
                }
                restoreOriginalTeam(scoreboard, name);
            }
        }

        for (String old : new HashSet<>(teamedFriends)) {
            Team currentTeam = scoreboard.getScoreHolderTeam(old);
            if (!currentFriends.contains(old) && currentTeam == team) {
                scoreboard.clearTeam(old);
            }
            if (!currentFriends.contains(old)) {
                restoreOriginalTeam(scoreboard, old);
            }
        }

        teamedFriends.clear();
        teamedFriends.addAll(currentFriends);
    }

    public static boolean hasLocalGlowMark(PlayerEntity player) {
        return LOCAL_GLOW_PLAYERS.contains(player.getUuid());
    }

    private void restoreOriginalTeam(Scoreboard scoreboard, String name) {
        if (!previousTeamByPlayer.containsKey(name)) {
            return;
        }

        String previousTeamName = previousTeamByPlayer.remove(name);
        if (previousTeamName == null || previousTeamName.isEmpty()) {
            return;
        }

        Team previousTeam = scoreboard.getTeam(previousTeamName);
        if (previousTeam != null) {
            scoreboard.addScoreHolderToTeam(name, previousTeam);
        }
    }
}
