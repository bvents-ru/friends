package com.bvents.bvfriends.mixin;

import net.minecraft.client.option.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(KeyBinding.Category.class)
public interface KeyBindingCategoryAccessor {
    @Accessor("CATEGORIES")
    static List<KeyBinding.Category> bvfriends$getCategories() {
        throw new UnsupportedOperationException("Mixin accessor");
    }
}
