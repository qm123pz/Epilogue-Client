package epilogue.module.modules.combat;

import epilogue.event.EventTarget;
import epilogue.events.PacketEvent;
import epilogue.events.Render3DEvent;
import epilogue.events.TickEvent;
import epilogue.event.types.EventType;
import epilogue.module.Module;
import epilogue.util.RenderUtil;
import epilogue.value.values.BooleanValue;
import epilogue.value.values.FloatValue;
import epilogue.value.values.IntValue;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.INetHandler;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.server.S13PacketDestroyEntities;
import net.minecraft.network.play.server.S14PacketEntity;
import net.minecraft.network.play.server.S18PacketEntityTeleport;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.Deque;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;

import static epilogue.util.MinecraftInstance.mc;

public class BackTrack extends Module {

    //This BackTrack By MyauPlus

    public final IntValue latency = new IntValue("Track MS", 200, 1, 1000);
    public final FloatValue enemyDistance = new FloatValue("Max Track Range", 6.0F, 3.1F, 6.0F);
    public final BooleanValue onlyCombat = new BooleanValue("Only Attack", true);
    public final BooleanValue predictPosition = new BooleanValue("Render Real Pos", true);
    public final BooleanValue disableOnWorldChange = new BooleanValue("Disable On World Change", false);

    private final Queue<TimedPacket> packetQueue = new ConcurrentLinkedQueue<>();
    private final Deque<Vec3> positionHistory = new ConcurrentLinkedDeque<>();
    private final Deque<Vec3> recentPositions = new ConcurrentLinkedDeque<>();

    private Vec3 realTargetPos;
    private Vec3 lastRealTargetPos;
    private EntityPlayer target;
    private int attackTicks;

    public BackTrack() {
        super("BackTrack", false);
    }

    @Override
    public String[] getSuffix() {
        return isEnabled() ? new String[]{latency.getValue() + "ms"} : new String[]{""};
    }

    @Override
    public void onEnabled() {
        packetQueue.clear();
        positionHistory.clear();
        recentPositions.clear();
        realTargetPos = null;
        lastRealTargetPos = null;
        target = null;
        attackTicks = 0;
    }

    @Override
    public void onDisabled() {
        releasePackets();
        packetQueue.clear();
        positionHistory.clear();
        recentPositions.clear();
        realTargetPos = null;
        lastRealTargetPos = null;
        target = null;
        attackTicks = 0;
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (!this.isEnabled()) return;
        if (event.getType() != EventType.PRE) return;

        if (target != null) {
            attackTicks++;
        }

        updateTargetLogic();
        processPacketQueue();

        if (packetQueue.isEmpty() && target != null) {
            realTargetPos = target.getPositionVector();
        }
    }

    private void updateTargetLogic() {
        if (target == null || realTargetPos == null) return;

        try {
            Vec3 currentPos = target.getPositionVector();
            recentPositions.addLast(currentPos);

            if (recentPositions.size() > 5) {
                recentPositions.removeFirst();
            }

            if (recentPositions.size() == 5) {
                Vec3 oldestPos = recentPositions.getFirst();
                if (oldestPos.distanceTo(currentPos) > 5.0) {
                    resetAndRelease();
                    return;
                }
            }

            positionHistory.addLast(currentPos);
            if (positionHistory.size() > 10) {
                positionHistory.removeFirst();
            }

            if (attackTicks > 7 || realTargetPos.distanceTo(mc.thePlayer.getPositionVector()) > enemyDistance.getValue()) {
                resetAndRelease();
                return;
            }

            lastRealTargetPos = realTargetPos;

        } catch (Exception e) {
            resetAndRelease();
        }
    }

    private void processPacketQueue() {
        long maxDelay = latency.getValue();
        long currentTime = System.currentTimeMillis();

        while (!packetQueue.isEmpty()) {
            TimedPacket timedPacket = packetQueue.peek();
            if (timedPacket == null) break;

            if (currentTime - timedPacket.timestamp >= maxDelay) {
                packetQueue.poll();
                dispatchPacket(timedPacket.packet);
            } else {
                break;
            }
        }
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) throws InterruptedException {
        if (!this.isEnabled()) return;
        if (!this.predictPosition.getValue() || target == null || realTargetPos == null || lastRealTargetPos == null) {
            return;
        }

        Vec3 renderPos = getSmoothedPosition(event.getPartialTicks());

        Color color = new Color(0xFFFF0000);

        float size = target.getCollisionBorderSize();
        double width = target.width / 2.0 + size;
        double height = target.height + size;

        AxisAlignedBB aabb = new AxisAlignedBB(
                renderPos.xCoord - width, renderPos.yCoord, renderPos.zCoord - width,
                renderPos.xCoord + width, renderPos.yCoord + height, renderPos.zCoord + width
        ).offset(
                -mc.getRenderManager().viewerPosX,
                -mc.getRenderManager().viewerPosY,
                -mc.getRenderManager().viewerPosZ
        );

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.disableDepth();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);

        RenderUtil.drawFilledBox(aabb, color.getRed(), color.getGreen(), color.getBlue());

        GlStateManager.enableDepth();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    private Vec3 getSmoothedPosition(float partialTicks) {
        if (positionHistory.isEmpty()) {
            return lastRealTargetPos.addVector(
                    (realTargetPos.xCoord - lastRealTargetPos.xCoord) * partialTicks,
                    (realTargetPos.yCoord - lastRealTargetPos.yCoord) * partialTicks,
                    (realTargetPos.zCoord - lastRealTargetPos.zCoord) * partialTicks
            );
        }

        double totalWeight = 0;
        double x = 0, y = 0, z = 0;

        Object[] history = positionHistory.toArray();
        int size = history.length;

        for (int i = 0; i < size; i++) {
            double weight = (i + 1) / (double) size;
            Vec3 pos = (Vec3) history[i];

            x += pos.xCoord * weight;
            y += pos.yCoord * weight;
            z += pos.zCoord * weight;
            totalWeight += weight;
        }

        double currentWeight = 1.5;
        x += realTargetPos.xCoord * currentWeight;
        y += realTargetPos.yCoord * currentWeight;
        z += realTargetPos.zCoord * currentWeight;
        totalWeight += currentWeight;

        return new Vec3(x / totalWeight, y / totalWeight, z / totalWeight);
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (!this.isEnabled()) return;
        if (event.isCancelled() || mc.thePlayer == null || mc.thePlayer.ticksExisted < 20) return;

        Packet<?> packet = event.getPacket();

        if (event.getType() == EventType.RECEIVE) {
            handleReceivePacket(event, packet);
        } else if (event.getType() == EventType.SEND) {
            handleSendPacket(packet);
        }
    }

    private void handleReceivePacket(PacketEvent event, Packet<?> packet) {
        if (target == null) return;

        boolean shouldIntercept = false;

        if (packet instanceof S14PacketEntity) {
            S14PacketEntity wrapper = (S14PacketEntity) packet;
            Entity entity = wrapper.getEntity(mc.theWorld);

            if (entity != null && entity.getEntityId() == target.getEntityId()) {
                realTargetPos = realTargetPos.addVector(
                        wrapper.func_149062_c() / 32.0D,
                        wrapper.func_149061_d() / 32.0D,
                        wrapper.func_149064_e() / 32.0D
                );
                shouldIntercept = true;
            }
        } else if (packet instanceof S18PacketEntityTeleport) {
            S18PacketEntityTeleport wrapper = (S18PacketEntityTeleport) packet;
            if (wrapper.getEntityId() == target.getEntityId()) {
                realTargetPos = new Vec3(wrapper.getX() / 32.0D, wrapper.getY() / 32.0D, wrapper.getZ() / 32.0D);
                shouldIntercept = true;
            }
        } else if (packet instanceof S13PacketDestroyEntities) {
            S13PacketDestroyEntities wrapper = (S13PacketDestroyEntities) packet;
            for (int id : wrapper.getEntityIDs()) {
                if (id == target.getEntityId()) {
                    resetAndRelease();
                    return;
                }
            }
        }

        if (shouldIntercept) {
            packetQueue.add(new TimedPacket(packet, System.currentTimeMillis()));
            event.setCancelled(true);
        }
    }

    private void handleSendPacket(Packet<?> packet) {
        if (packet instanceof C02PacketUseEntity) {
            C02PacketUseEntity wrapper = (C02PacketUseEntity) packet;

            if (onlyCombat.getValue() && wrapper.getAction() != C02PacketUseEntity.Action.ATTACK)
                return;

            Entity entity = wrapper.getEntityFromWorld(mc.theWorld);
            if (entity instanceof EntityPlayer) {
                EntityPlayer player = (EntityPlayer) entity;

                if (target != null && player.getEntityId() == target.getEntityId()) {
                    attackTicks = 0;
                    return;
                }

                target = player;
                realTargetPos = player.getPositionVector();
                lastRealTargetPos = realTargetPos;

                positionHistory.clear();
                recentPositions.clear();
                positionHistory.add(realTargetPos);
                recentPositions.add(realTargetPos);

                attackTicks = 0;
            }
        }
    }

    @SubscribeEvent
    public void onWorldChange(WorldEvent.Load event) {
        if (!this.isEnabled()) return;
        if (disableOnWorldChange.getValue() && isEnabled()) {
            toggle();
        }
    }

    private void resetAndRelease() {
        target = null;
        realTargetPos = null;
        lastRealTargetPos = null;
        positionHistory.clear();
        recentPositions.clear();
        releasePackets();
    }

    private void releasePackets() {
        if (packetQueue.isEmpty()) return;

        while (!packetQueue.isEmpty()) {
            TimedPacket tp = packetQueue.poll();
            if (tp != null) {
                dispatchPacket(tp.packet);
            }
        }
    }

    private void dispatchPacket(Packet<?> packet) {
        if (mc.theWorld == null || mc.getNetHandler() == null) return;

        try {
            handlePacketProcess(packet, mc.getNetHandler());
        } catch (Exception ignored) {
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends INetHandler> void handlePacketProcess(Packet<T> packet, INetHandler netHandler) {
        packet.processPacket((T) netHandler);
    }

    private static class TimedPacket {
        public final Packet<?> packet;
        public final long timestamp;

        public TimedPacket(Packet<?> packet, long timestamp) {
            this.packet = packet;
            this.timestamp = timestamp;
        }
    }
}