package dev.anvilcraft.rg.sr.mixin;

import dev.anvilcraft.rg.sr.util.ConnectionInjector;
import io.netty.channel.Channel;
import net.minecraft.network.Connection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Connection.class)
public class ConnectionMixin implements ConnectionInjector {
    @Shadow
    private Channel channel;

    @Override
    @SuppressWarnings("AddedMixinMembersNamePattern")
    public void setChannel(Channel channel) {
        this.channel = channel;
    }
}
