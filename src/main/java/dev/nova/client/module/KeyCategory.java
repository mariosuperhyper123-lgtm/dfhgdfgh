package dev.nova.client.module;

import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

/** Eigene Kategorie unter Optionen → Steuerung (Name: key.category.novaclient.main). */
public final class KeyCategory {
    public static final KeyMapping.Category MAIN =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath("novaclient", "main"));

    private KeyCategory() {}
}
