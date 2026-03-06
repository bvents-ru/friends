package com.bvents.bvfriends.mixin;

import com.bvents.bvfriends.client.BvfriendsClient;
import com.bvents.bvfriends.client.config.BvfriendsConfigManager;
import com.bvents.bvfriends.client.friend.FriendService;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class FriendGlowMixin {
    @Inject(method = "isGlowing", at = @At("HEAD"), cancellable = true)
    private void bvfriends$forceGlowForFriends(CallbackInfoReturnable<Boolean> cir) {
        if (!BvfriendsConfigManager.get().friendOutlineEnabled) {
            return;
        }

        Entity entity = (Entity) (Object) this;
        if (!(entity instanceof PlayerEntity player)) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) {
            return;
        }

        if (!BvfriendsClient.hasLocalGlowMark(player)) {
            return;
        }

        if (FriendService.isFriend(player.getGameProfile().name()) && BvfriendsConfigManager.get().friendOutlineEnabled) {
            cir.setReturnValue(true);
            return;
        }

        cir.setReturnValue(false);
    }
}
