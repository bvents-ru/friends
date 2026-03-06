package com.bvents.bvfriends.client.config;

import dev.isxander.yacl3.config.v2.api.SerialEntry;
import java.util.ArrayList;
import java.util.List;

public class BvfriendsConfigModel {
    @SerialEntry
    public List<String> friends = new ArrayList<>();
    @SerialEntry
    public boolean blockFriendAttack = true;
    @SerialEntry
    public AttackBlockMode attackMode = AttackBlockMode.PACKET_CANCEL;
    @SerialEntry
    public String friendNameStyle = "&a";
    @SerialEntry
    public boolean friendOutlineEnabled = true;
    @SerialEntry
    public String friendOutlineStyle = "&a";
}

