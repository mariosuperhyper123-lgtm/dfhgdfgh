package dev.nova.client.module.modules;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.decoration.ArmorStand;

/**
 * Unsichtbarer Kamera-Körper für FreeCam und FreeLook. Er ist nicht Teil der Welt und wird
 * direkt gesetzt (alte = neue Position/Drehung), damit nichts nachzieht oder ruckelt.
 */
public final class CameraEntity extends ArmorStand {
    public CameraEntity(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z);
        this.noPhysics = true;
        this.setInvisible(true);
    }

    /**
     * Gilt als Zuschauer: Minecraft blendet dann keine Chunks aus, wenn die Kamera in Blöcken
     * steckt, und zeigt kein "in der Wand"-Overlay.
     */
    @Override
    public boolean isSpectator() {
        return true;
    }

    /** Setzt die Augenposition und die Blickrichtung ohne Interpolation. */
    public void place(double eyeX, double eyeY, double eyeZ, float yaw, float pitch) {
        this.setPos(eyeX, eyeY - this.getEyeHeight(), eyeZ);
        // Bei Lebewesen kommt die Links/Rechts-Blickrichtung vom Kopf, deshalb alles mitdrehen
        this.setYRot(yaw);
        this.setXRot(pitch);
        this.setYHeadRot(yaw);
        this.setYBodyRot(yaw);
        this.yHeadRotO = yaw;
        this.yBodyRotO = yaw;
        this.setOldPosAndRot();
    }
}
