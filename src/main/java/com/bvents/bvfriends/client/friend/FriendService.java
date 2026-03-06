package com.bvents.bvfriends.client.friend;

import com.bvents.bvfriends.client.config.BvfriendsConfigManager;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

public final class FriendService {
    private FriendService() {
    }

    public static void syncFromConfig() {
        normalizeInConfig();
    }

    public static boolean isFriend(String name) {
        if (name == null || name.isBlank()) {
            return false;
        }
        String normalized = normalize(name);
        return normalizeInConfig().contains(normalized);
    }

    public static boolean addFriend(String name) {
        String normalized = normalize(name);
        if (normalized.isBlank()) {
            return false;
        }
        List<String> friends = normalizeInConfig();
        boolean added = !friends.contains(normalized);
        if (added) {
            friends.add(normalized);
            BvfriendsConfigManager.save();
        }
        return added;
    }

    public static boolean removeFriend(String name) {
        String normalized = normalize(name);
        boolean removed = normalizeInConfig().remove(normalized);
        if (removed) {
            BvfriendsConfigManager.save();
        }
        return removed;
    }

    public static boolean toggleFriend(String name) {
        if (isFriend(name)) {
            removeFriend(name);
            return false;
        }
        addFriend(name);
        return true;
    }

    public static String normalize(String name) {
        return name.trim().toLowerCase(Locale.ROOT);
    }

    public static List<String> getSortedFriends() {
        List<String> list = new ArrayList<>(normalizeInConfig());
        list.sort(Comparator.naturalOrder());
        return list;
    }

    private static List<String> normalizeInConfig() {
        if (BvfriendsConfigManager.get().friends == null) {
            BvfriendsConfigManager.get().friends = new ArrayList<>();
            return BvfriendsConfigManager.get().friends;
        }

        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String name : BvfriendsConfigManager.get().friends) {
            if (name == null) {
                continue;
            }
            String normalizedName = normalize(name);
            if (!normalizedName.isBlank()) {
                normalized.add(normalizedName);
            }
        }

        BvfriendsConfigManager.get().friends = new ArrayList<>(normalized);
        return BvfriendsConfigManager.get().friends;
    }
}

