// root/src/main/java/net/sugar27/quests/event/QuestEventHandler.java

package net.sugar27.quests.event;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.sugar27.quests.quest.DailyQuestManager;
import net.sugar27.quests.quest.QuestCriteriaType;
import net.sugar27.quests.quest.QuestEventContext;
import net.sugar27.quests.quest.QuestManager;
import net.sugar27.quests.quest.QuestProgressManager;

import java.util.Objects;

// Subscribes to NeoForge events and updates quest progress.
public class QuestEventHandler {
    private final QuestProgressManager progressManager = new QuestProgressManager();

    // Load quest definitions when the server starts.
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        QuestManager.get().loadAll();
        DailyQuestManager.get().loadCandidates();
        DailyQuestManager.get().ensureDailySelection(event.getServer());
    }

    // Sync quest data when a player logs in.
    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            DailyQuestManager.get().ensureDailySelection(player.getServer());
            progressManager.syncFull(player);
        }
    }

    // Handle block break events.
    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(Objects.requireNonNull(event.getState().getBlock()));
        QuestEventContext context = new QuestEventContext(player, QuestCriteriaType.BLOCK_BROKEN, blockId, 1, player.level(), player.getX(), player.getY(), player.getZ());
        progressManager.handleEvent(player, context);
    }

    // Handle item pickup events.
    @SubscribeEvent
    public void onItemPickup(ItemEntityPickupEvent.Post event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(Objects.requireNonNull(event.getItemEntity().getItem().getItem()));
        int count = event.getItemEntity().getItem().getCount();
        QuestEventContext context = new QuestEventContext(player, QuestCriteriaType.ITEM_ACQUIRED, itemId, count, player.level(), player.getX(), player.getY(), player.getZ());
        progressManager.handleEvent(player, context);
    }

    // Handle item crafted events.
    @SubscribeEvent
    public void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(Objects.requireNonNull(event.getCrafting().getItem()));
        int count = event.getCrafting().getCount();
        QuestEventContext context = new QuestEventContext(player, QuestCriteriaType.ITEM_CRAFTED, itemId, count, player.level(), player.getX(), player.getY(), player.getZ());
        progressManager.handleEvent(player, context);
    }

    // Handle entity kill events.
    @SubscribeEvent
    public void onEntityKilled(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(Objects.requireNonNull(event.getEntity().getType()));
        QuestEventContext context = new QuestEventContext(player, QuestCriteriaType.ENTITY_KILLED, entityId, 1, player.level(), player.getX(), player.getY(), player.getZ());
        progressManager.handleEvent(player, context);
    }

    // Handle location checks on a periodic player tick.
    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.tickCount % 40 != 0) {
            return;
        }
        if (QuestManager.get().getQuestsByCriteriaType(QuestCriteriaType.LOCATION_REACHED).isEmpty()) {
            return;
        }
        QuestEventContext context = new QuestEventContext(player, QuestCriteriaType.LOCATION_REACHED, null, 1, player.level(), player.getX(), player.getY(), player.getZ());
        progressManager.handleEvent(player, context);
    }
}
