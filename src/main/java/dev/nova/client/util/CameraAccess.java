package dev.nova.client.util;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Liest Position und Drehung der echten Spielkamera. Damit passen ESP und Tracer auch dann,
 * wenn eine andere Mod (z. B. eine fremde FreeCam) die Kamera verschiebt.
 * Per Reflection, weil sich die Namen zwischen Minecraft-Versionen ändern; schlägt das fehl,
 * wird null zurückgegeben und der alte Weg benutzt.
 */
public final class CameraAccess {
    public record State(Vec3 pos, float yaw, float pitch) {}

    private static boolean failed;

    private CameraAccess() {}

    public static State read(Minecraft mc) {
        if (failed) return null;
        try {
            Object camera = get(mc.gameRenderer, "mainCamera", "getMainCamera");
            Object pos = get(camera, "position", "getPosition");
            Object yaw = get(camera, "yRot", "getYRot");
            Object pitch = get(camera, "xRot", "getXRot");
            if (pos instanceof Vec3 v && yaw instanceof Float y && pitch instanceof Float p) {
                return new State(v, y, p);
            }
        } catch (Throwable ignored) {
        }
        failed = true;
        return null;
    }

    /** Versucht nacheinander Methoden und Felder mit den angegebenen Namen. */
    private static Object get(Object target, String... names) throws Exception {
        for (String name : names) {
            for (Class<?> c = target.getClass(); c != null; c = c.getSuperclass()) {
                try {
                    Method m = c.getDeclaredMethod(name);
                    m.setAccessible(true);
                    return m.invoke(target);
                } catch (NoSuchMethodException ignored) {
                }
                try {
                    Field f = c.getDeclaredField(name);
                    f.setAccessible(true);
                    return f.get(target);
                } catch (NoSuchFieldException ignored) {
                }
            }
        }
        throw new NoSuchMethodException(String.join("/", names));
    }
}
