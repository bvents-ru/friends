package com.bvents.bvfriends.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.bvents.bvfriends.client.friend.FriendService;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.EnumControllerBuilder;
import dev.isxander.yacl3.api.controller.StringControllerBuilder;
import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class BvfriendsConfigManager {
    private static final String MOD_ID = "bvfriends";
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve(MOD_ID + ".json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final ConfigClassHandler<BvfriendsConfigModel> HANDLER = ConfigClassHandler
            .createBuilder(BvfriendsConfigModel.class)
            .id(Identifier.of(MOD_ID, "config"))
            .serializer(config -> GsonConfigSerializerBuilder.create(config)
                    .setPath(CONFIG_PATH)
                    .setJson5(false)
                    .build())
            .build();

    private BvfriendsConfigManager() {
    }

    public static void load() {
        HANDLER.load();
        cfgload();
        if (get().attackMode == null) {
            get().attackMode = AttackBlockMode.PACKET_CANCEL;
        }
        if (get().friendNameStyle == null || get().friendNameStyle.isBlank()) {
            get().friendNameStyle = "&a";
        }
        if (get().friendOutlineStyle == null || get().friendOutlineStyle.isBlank()) {
            get().friendOutlineStyle = "&a";
        }
        FriendService.syncFromConfig();
        save();
    }

    public static BvfriendsConfigModel get() {
        return HANDLER.instance();
    }

    public static void save() {
        FriendService.syncFromConfig();
        get().friends = new ArrayList<>(FriendService.getSortedFriends());
        HANDLER.save();
        cfgsave();
    }

    public static Screen createScreen(Screen parent) {
        YetAnotherConfigLib yacl = YetAnotherConfigLib.create(HANDLER, (defaults, config, builder) -> builder
                .title(Text.translatable("config.bvfriends.title"))
                .category(ConfigCategory.createBuilder()
                        .name(Text.translatable("config.bvfriends.category.main"))
                        .option(Option.<Boolean>createBuilder()
                                .name(Text.translatable("config.bvfriends.option.block_friend_attack.name"))
                                .description(OptionDescription.of(Text.translatable("config.bvfriends.option.block_friend_attack.desc")))
                                .binding(true, () -> config.blockFriendAttack, newValue -> config.blockFriendAttack = newValue)
                                .controller(opt -> dev.isxander.yacl3.api.controller.BooleanControllerBuilder.create(opt).yesNoFormatter())
                                .build())
                        .option(Option.<AttackBlockMode>createBuilder()
                                .name(Text.translatable("config.bvfriends.option.attack_mode.name"))
                                .description(OptionDescription.of(Text.translatable("config.bvfriends.option.attack_mode.desc")))
                                .binding(defaults.attackMode, () -> config.attackMode, newValue -> config.attackMode = newValue)
                                .controller(opt -> EnumControllerBuilder.create(opt)
                                        .enumClass(AttackBlockMode.class)
                                        .valueFormatter(mode -> switch (mode) {
                                            case PACKET_CANCEL -> Text.translatable("config.bvfriends.option.attack_mode.packet_cancel");
                                            case INPUT_BLOCK -> Text.translatable("config.bvfriends.option.attack_mode.input_block");
                                        }))
                                .build())
                        .option(Option.<String>createBuilder()
                                .name(Text.translatable("config.bvfriends.option.friend_name_style.name"))
                                .description(OptionDescription.of(Text.translatable("config.bvfriends.option.friend_name_style.desc")))
                                .binding(defaults.friendNameStyle, () -> config.friendNameStyle, newValue -> config.friendNameStyle = newValue)
                                .controller(StringControllerBuilder::create)
                                .build())
                        .option(Option.<Boolean>createBuilder()
                                .name(Text.translatable("config.bvfriends.option.friend_outline_enabled.name"))
                                .description(OptionDescription.of(Text.translatable("config.bvfriends.option.friend_outline_enabled.desc")))
                                .binding(defaults.friendOutlineEnabled, () -> config.friendOutlineEnabled, newValue -> config.friendOutlineEnabled = newValue)
                                .controller(opt -> dev.isxander.yacl3.api.controller.BooleanControllerBuilder.create(opt).yesNoFormatter())
                                .build())
                        .option(Option.<String>createBuilder()
                                .name(Text.translatable("config.bvfriends.option.friend_outline_style.name"))
                                .description(OptionDescription.of(Text.translatable("config.bvfriends.option.friend_outline_style.desc")))
                                .binding(defaults.friendOutlineStyle, () -> config.friendOutlineStyle, newValue -> config.friendOutlineStyle = newValue)
                                .controller(StringControllerBuilder::create)
                                .build())
                        .build())
                .save(BvfriendsConfigManager::save));
        return yacl.generateScreen(parent);
    }

    private static void cfgload() {
        if (!Files.exists(CONFIG_PATH)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
            BvfriendsConfigModel loaded = GSON.fromJson(reader, BvfriendsConfigModel.class);
            if (loaded == null) {
                return;
            }
            if (loaded.friends != null) {
                get().friends = new ArrayList<>(loaded.friends);
            }
            get().blockFriendAttack = loaded.blockFriendAttack;
            get().attackMode = loaded.attackMode;
            get().friendNameStyle = loaded.friendNameStyle;
            get().friendOutlineEnabled = loaded.friendOutlineEnabled;
            get().friendOutlineStyle = loaded.friendOutlineStyle;
        } catch (Exception ignored) {
        }
    }

    private static void cfgsave() {
        try {
            if (CONFIG_PATH.getParent() != null) {
                Files.createDirectories(CONFIG_PATH.getParent());
            }
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(get(), writer);
            }
        } catch (Exception ignored) {
        }
    }
}

