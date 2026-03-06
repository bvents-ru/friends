package com.bvents.bvfriends.client.compat;

import com.bvents.bvfriends.client.config.BvfriendsConfigManager;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public class ModMenuCompat implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return BvfriendsConfigManager::createScreen;
    }
}
