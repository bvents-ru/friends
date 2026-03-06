package com.bvents.bvfriends.mixin;

import com.bvents.bvfriends.client.config.AttackBlockMode;
import com.bvents.bvfriends.client.config.BvfriendsConfigManager;
import com.bvents.bvfriends.client.friend.FriendService;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    @Shadow
    public Entity targetedEntity;

    @Inject(method = "doAttack", at = @At("HEAD"), cancellable = true)
    private void bvfriends$blockAttackButtonForFriends(CallbackInfoReturnable<Boolean> cir) {
        if (!BvfriendsConfigManager.get().blockFriendAttack) {
            return;
        }
        if (BvfriendsConfigManager.get().attackMode != AttackBlockMode.INPUT_BLOCK) {
            return;
        }
        if (targetedEntity instanceof PlayerEntity player && FriendService.isFriend(player.getGameProfile().name())) {
            cir.setReturnValue(false);
        }
    }
}
