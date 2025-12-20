package epilogue.events;

import epilogue.event.events.Cancellable;

/* loaded from: Arcane 8.10.jar:qwq/menu/event/impl/events/player/SlowDownEvent.class */
public abstract class SlowDownEvent implements Cancellable {
    private float strafe;
    private float forward;
    private boolean sprinting;

    public void setStrafe(float strafe) {
        this.strafe = strafe;
    }

    public void setForward(float forward) {
        this.forward = forward;
    }

    public void setSprinting(boolean sprinting) {
        this.sprinting = sprinting;
    }

    public SlowDownEvent(float strafe, float forward, boolean sprinting) {
        this.strafe = strafe;
        this.forward = forward;
        this.sprinting = sprinting;
    }

    public float getStrafe() {
        return this.strafe;
    }

    public float getForward() {
        return this.forward;
    }

    public boolean isSprinting() {
        return this.sprinting;
    }
}
