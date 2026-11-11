package dev.nova.client.module.modules;

import dev.nova.client.module.ModuleManager;

/** Kleine Abstimmung zwischen FreeCam und FreeLook, damit nie beide gleichzeitig laufen. */
final class ModuleCompat {
    private ModuleCompat() {}

    static void stopFreeLook() {
        ModuleManager mm = ModuleManager.get();
        if (mm != null && mm.freeLook.isEnabled()) mm.freeLook.setEnabled(false);
    }

    static boolean freeCamActive() {
        ModuleManager mm = ModuleManager.get();
        return mm != null && mm.freeCam.active();
    }
}
