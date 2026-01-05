package epilogue.hooks;

import epilogue.Epilogue;
import epilogue.event.EventManager;
import epilogue.event.types.EventType;
import epilogue.events.PacketEvent;
import epilogue.ui.chat.GuiChat;
import epiloguemixinbridge.IAccessorNetworkManager;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import net.minecraft.client.Minecraft;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.INetHandlerPlayClient;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Method;

public final class NetworkManagerHooks {
    private NetworkManagerHooks() {
    }

    public static void onChannelRead0(NetworkManager self, ChannelHandlerContext channelHandlerContext, Packet<?> packet, CallbackInfo callbackInfo) {
        if (!packet.getClass().getName().startsWith("net.minecraft.network.play.client")) {
            if (Epilogue.delayManager != null && Epilogue.delayManager.shouldDelay((Packet<INetHandlerPlayClient>) packet)) {
                callbackInfo.cancel();
            } else {
                handleTabCompleteResponse(self, packet);
                PacketEvent event = new PacketEvent(EventType.RECEIVE, packet);
                EventManager.call(event);
                if (event.isCancelled()) {
                    callbackInfo.cancel();
                }
            }
        }
    }

    private static void handleTabCompleteResponse(NetworkManager self, Packet<?> packet) {
        String name = packet.getClass().getName();
        if (!name.startsWith("net.minecraft.network.play.server")) {
            return;
        }
        if (!name.contains("TabComplete")) {
            return;
        }

        Channel channel = ((IAccessorNetworkManager) self).getChannel();
        if (channel == null) {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.currentScreen == null) {
            return;
        }

        try {
            Method getter = null;
            for (Method m : packet.getClass().getMethods()) {
                if (m.getParameterTypes().length == 0 && m.getReturnType().isArray() && m.getReturnType().getComponentType() == String.class) {
                    getter = m;
                    break;
                }
            }
            if (getter == null) {
                return;
            }
            String[] matches = (String[]) getter.invoke(packet);
            if (matches == null) {
                return;
            }

            if (mc.currentScreen instanceof GuiChat) {
                ((GuiChat) mc.currentScreen).onAutocompleteResponse(matches);
                return;
            }

            for (Method m : mc.currentScreen.getClass().getMethods()) {
                if (m.getName().equals("onAutocompleteResponse") && m.getParameterTypes().length == 1 && m.getParameterTypes()[0].isArray() && m.getParameterTypes()[0].getComponentType() == String.class) {
                    m.invoke(mc.currentScreen, (Object) matches);
                    return;
                }
            }
        } catch (Throwable ignored) {
        }
    }

    public static void onSendPacket(Packet<?> packet, CallbackInfo callbackInfo) {
        if (!packet.getClass().getName().startsWith("net.minecraft.network.play.server")) {
            PacketEvent event = new PacketEvent(EventType.SEND, packet);
            EventManager.call(event);
            if (event.isCancelled()) {
                callbackInfo.cancel();
            } else if (Epilogue.playerStateManager != null && Epilogue.blinkManager != null && Epilogue.lagManager != null) {
                if (!Epilogue.lagManager.isFlushing()) {
                    Epilogue.playerStateManager.handlePacket(packet);
                    if (Epilogue.blinkManager.isBlinking()) {
                        if (Epilogue.blinkManager.offerPacket(packet)) {
                            callbackInfo.cancel();
                            return;
                        }
                    }
                    if (Epilogue.lagManager.handlePacket(packet)) {
                        callbackInfo.cancel();
                    }
                }
            }
        }
    }

    public static void onSendPacket2(
            Packet<?> packet,
            GenericFutureListener<? extends Future<? super Void>> genericFutureListener,
            GenericFutureListener<? extends Future<? super Void>>[] arr,
            CallbackInfo callbackInfo
    ) {
        if (!packet.getClass().getName().startsWith("net.minecraft.network.play.server")) {
            if (Epilogue.playerStateManager != null && Epilogue.blinkManager != null && Epilogue.lagManager != null) {
                if (!Epilogue.lagManager.isFlushing()) {
                    Epilogue.playerStateManager.handlePacket(packet);
                    if (Epilogue.blinkManager.isBlinking()) {
                        if (Epilogue.blinkManager.offerPacket(packet)) {
                            callbackInfo.cancel();
                            return;
                        }
                    }
                    if (Epilogue.lagManager.handlePacket(packet)) {
                        callbackInfo.cancel();
                    }
                }
            }
        }
    }
}
