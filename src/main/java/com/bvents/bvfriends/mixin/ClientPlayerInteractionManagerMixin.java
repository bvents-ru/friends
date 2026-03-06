package com.bvents.bvfriends.mixin;

import com.bvents.bvfriends.client.config.BvfriendsConfigManager;
import com.bvents.bvfriends.client.friend.FriendService;
import com.bvents.bvfriends.client.config.AttackBlockMode;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerInteractionManager.class)
public class ClientPlayerInteractionManagerMixin {
    @Inject(method = "attackEntity", at = @At("HEAD"), cancellable = true)
    private void bvfriends$cancelAttackOnFriend(PlayerEntity player, Entity target, CallbackInfo ci) {
        if (!BvfriendsConfigManager.get().blockFriendAttack) {
            return;
        }
        if (BvfriendsConfigManager.get().attackMode != AttackBlockMode.PACKET_CANCEL) {
            return;
        }
        if (target instanceof PlayerEntity targetPlayer && FriendService.isFriend(targetPlayer.getGameProfile().name())) {
            ci.cancel();
        }
    }
}

