package com.bvents.bvfriends.client.config;

public enum AttackBlockMode {
    PACKET_CANCEL,
    INPUT_BLOCK;

    public static AttackBlockMode fromConfig(String value) {
        if (value == null) {
            return PACKET_CANCEL;
        }
        try {
            return AttackBlockMode.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return PACKET_CANCEL;
        }
    }
}
