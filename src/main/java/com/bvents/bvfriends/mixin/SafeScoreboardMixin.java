package com.bvents.bvfriends.mixin;

import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Scoreboard.class)
public abstract class SafeScoreboardMixin {
    @Inject(method = "removeScoreHolderFromTeam(Ljava/lang/String;Lnet/minecraft/scoreboard/Team;)V", at = @At("HEAD"), cancellable = true)
    private void bvfriends$ignoreMismatchedTeamRemoval(String scoreHolderName, Team team, CallbackInfo ci) {
        Scoreboard self = (Scoreboard) (Object) this;
        if (self.getScoreHolderTeam(scoreHolderName) != team) {
            ci.cancel();
        }
    }
}