package com.bvents.bvfriends.mixin;

import com.bvents.bvfriends.client.config.BvfriendsConfigManager;
import com.bvents.bvfriends.client.friend.FriendService;
import com.bvents.bvfriends.client.util.FriendStyleParser;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerListHud.class)
public abstract class PlayerListHudMixin {
    @Inject(method = "collectPlayerEntries", at = @At("RETURN"), cancellable = true)
    private void bvfriends$sortFriendsFirst(CallbackInfoReturnable<List<PlayerListEntry>> cir) {
        List<PlayerListEntry> entries = new ArrayList<>(cir.getReturnValue());
        List<PlayerListEntry> friends = new ArrayList<>();
        List<PlayerListEntry> others = new ArrayList<>();

        for (PlayerListEntry entry : entries) {
            if (FriendService.isFriend(entry.getProfile().name())) {
                friends.add(entry);
            } else {
                others.add(entry);
            }
        }

        friends.sort((a, b) -> a.getProfile().name().compareToIgnoreCase(b.getProfile().name()));
        others.sort((a, b) -> a.getProfile().name().compareToIgnoreCase(b.getProfile().name()));

        List<PlayerListEntry> sorted = new ArrayList<>(entries.size());
        sorted.addAll(friends);
        sorted.addAll(others);
        cir.setReturnValue(sorted);
    }

    @Inject(method = "getPlayerName", at = @At("RETURN"), cancellable = true)
    private void bvfriends$styleFriendName(PlayerListEntry entry, CallbackInfoReturnable<Text> cir) {
        if (!FriendService.isFriend(entry.getProfile().name())) {
            return;
        }
        Text cleanName = Text.literal(entry.getProfile().name())
                .setStyle(FriendStyleParser.parseStyle(BvfriendsConfigManager.get().friendNameStyle, Text.empty().getStyle()));
        cir.setReturnValue(cleanName);
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void bvfriends$setFriendsFooter(CallbackInfo ci) {
        int friendsOnline = 0;
        for (PlayerListEntry entry : collectPlayerEntries()) {
            if (FriendService.isFriend(entry.getProfile().name())) {
                friendsOnline++;
            }
        }

        MutableText footer = Text.translatable("hud.bvfriends.friends_online").formatted(Formatting.WHITE)
                .append(Text.literal(String.valueOf(friendsOnline)).formatted(friendsOnline > 0 ? Formatting.GREEN : Formatting.RED));
        setFooter(footer);
    }

    @Shadow
    protected abstract List<PlayerListEntry> collectPlayerEntries();

    @Shadow
    public abstract void setFooter(Text footer);
}
