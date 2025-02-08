package dev.anvilcraft.rg.sd.util;

import io.netty.channel.Channel;

public interface IConnectionInjector {
    void setChannel(Channel channel);
}
