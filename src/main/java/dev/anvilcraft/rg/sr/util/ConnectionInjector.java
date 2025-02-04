package dev.anvilcraft.rg.sr.util;

import io.netty.channel.Channel;

public interface ConnectionInjector {
    void setChannel(Channel channel);
}
