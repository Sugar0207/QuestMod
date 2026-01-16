// root/src/main/java/net/sugar27/quests/ShugaQuestsMod.java

package net.sugar27.quests;

import com.mojang.logging.LogUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.sugar27.quests.config.QuestClientConfig;
import net.sugar27.quests.command.QuestAdminCommand;
import net.sugar27.quests.event.QuestEventHandler;
import net.sugar27.quests.network.NetworkHandler;
import org.slf4j.Logger;

// The main mod entry point for Shuga Quests.
@Mod(ShugaQuestsMod.MODID)
public class ShugaQuestsMod {
    // The mod id used for registries, assets, and networking.
    public static final String MODID = "shuga_quests";
    // The shared logger for server-side diagnostics.
    public static final Logger LOGGER = LogUtils.getLogger();

    // Initialize mod systems and event listeners.
    public ShugaQuestsMod(IEventBus modEventBus, ModContainer modContainer) {
        // Register payloads and common setup hooks.
        modEventBus.addListener(NetworkHandler::registerPayloads);
        modEventBus.addListener(this::commonSetup);
        modContainer.registerConfig(ModConfig.Type.CLIENT, QuestClientConfig.SPEC);

        // Register server/gameplay events and admin commands.
        NeoForge.EVENT_BUS.register(new QuestEventHandler());
        NeoForge.EVENT_BUS.addListener(QuestAdminCommand::onRegisterCommands);

        // Register client-only hooks behind a dist check.
        if (FMLEnvironment.dist == Dist.CLIENT) {
            net.sugar27.quests.client.ClientSetup.init(modEventBus);
        }
    }

    // Common setup for any shared initialization.
    private void commonSetup(FMLCommonSetupEvent event) {
        // Log early so server operators know the mod is alive.
        LOGGER.info("Shuga Quests common setup complete");
    }
}


