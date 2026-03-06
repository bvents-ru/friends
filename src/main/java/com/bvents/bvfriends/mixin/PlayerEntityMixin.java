package com.bvents.bvfriends.mixin;

import com.bvents.bvfriends.client.config.BvfriendsConfigManager;
import com.bvents.bvfriends.client.friend.FriendService;
import com.bvents.bvfriends.client.util.FriendStyleParser;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public class PlayerEntityMixin {
    @Inject(method = "getDisplayName", at = @At("RETURN"), cancellable = true)
    private void bvfriends$styleFriendNameplate(CallbackInfoReturnable<Text> cir) {
        PlayerEntity self = (PlayerEntity) (Object) this;
        if (!FriendService.isFriend(self.getGameProfile().name())) {
            return;
        }

        MutableText base = Text.literal(self.getGameProfile().name());
        cir.setReturnValue(base.setStyle(FriendStyleParser.parseStyle(BvfriendsConfigManager.get().friendNameStyle, base.getStyle())));
    }
}
