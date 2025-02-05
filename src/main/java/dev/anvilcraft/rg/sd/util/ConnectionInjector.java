package dev.anvilcraft.rg.sd.util;

import io.netty.channel.Channel;

public interface ConnectionInjector {
    void setChannel(Channel channel);
}
