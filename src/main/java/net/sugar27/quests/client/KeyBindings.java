// root/src/main/java/net/sugar27/quests/client/KeyBindings.java

package net.sugar27.quests.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.sugar27.quests.client.gui.QuestScreen;
import org.lwjgl.glfw.GLFW;

// Registers and handles the quest screen key binding.
public final class KeyBindings {
    // The key mapping used to open the quest screen.
    public static KeyMapping OPEN_SCREEN;

    // Utility class; no instantiation.
    private KeyBindings() {
    }

    // Register the key mapping with the client registry.
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        OPEN_SCREEN = new KeyMapping(
                "key.shuga_quests.open_screen",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                "mod.shuga_quests.title"
        );
        event.register(OPEN_SCREEN);
    }

    // Listen for key presses and open the quest screen.
    public static void onClientTick(ClientTickEvent.Post event) {
        if (OPEN_SCREEN != null && OPEN_SCREEN.consumeClick()) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player != null) {
                minecraft.setScreen(new QuestScreen());
            }
        }
    }
}


