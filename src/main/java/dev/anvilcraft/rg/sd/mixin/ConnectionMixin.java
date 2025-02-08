package dev.anvilcraft.rg.sd.mixin;

import dev.anvilcraft.rg.sd.util.IConnectionInjector;
import io.netty.channel.Channel;
import net.minecraft.network.Connection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Connection.class)
abstract class ConnectionMixin implements IConnectionInjector {
    @Shadow
    private Channel channel;

    @Override
    @SuppressWarnings("AddedMixinMembersNamePattern")
    public void setChannel(Channel channel) {
        this.channel = channel;
    }
}
