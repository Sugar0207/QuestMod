// root/src/main/java/net/sugar27/quests/quest/QuestReward.java

package net.sugar27.quests.quest;

import com.google.gson.JsonObject;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

// Represents a reward that can be applied when a quest completes.
public record QuestReward(
        QuestRewardType type,
        ResourceLocation item,
        int count,
        int amount,
        ResourceLocation effect,
        int duration,
        int amplifier,
        String command,
        ResourceLocation advancement
) {
    // Parse a reward entry from JSON.
    public static QuestReward fromJson(JsonObject json) {
        QuestRewardType type = QuestRewardType.fromString(getString(json, "type"));
        ResourceLocation item = getResource(json, "item");
        int count = json.has("count") ? json.get("count").getAsInt() : 1;
        int amount = json.has("amount") ? json.get("amount").getAsInt() : 0;
        ResourceLocation effect = getResource(json, "effect");
        int duration = json.has("duration") ? json.get("duration").getAsInt() : 0;
        int amplifier = json.has("amplifier") ? json.get("amplifier").getAsInt() : 0;
        String command = getString(json, "command");
        ResourceLocation advancement = getResource(json, "id");
        return new QuestReward(type, item, count, amount, effect, duration, amplifier, command, advancement);
    }

    // Apply this reward to the given player.
    public void apply(ServerPlayer player) {
        switch (type) {
            case ITEM -> grantItem(player);
            case XP -> player.giveExperiencePoints(amount);
            case EFFECT -> grantEffect(player);
            case COMMAND -> runCommand(player);
            case ADVANCEMENT -> grantAdvancement(player);
        }
    }

    // Provide a simple description string for the UI.
    public String describe() {
        return Objects.requireNonNull(switch (Objects.requireNonNull(type)) {
            case ITEM -> "Item x" + count + " (" + Objects.requireNonNullElse(item, "unknown") + ")";
            case XP -> "XP +" + amount;
            case EFFECT -> "Effect " + Objects.requireNonNullElse(effect, "unknown") + " " + duration + "t";
            case COMMAND -> "Command";
            case ADVANCEMENT -> "Advancement " + Objects.requireNonNullElse(advancement, "unknown");
        });
    }

    // Provide a localized description component for the UI.
    public Component describeComponent() {
        return Objects.requireNonNull(switch (Objects.requireNonNull(type)) {
            case ITEM -> Component.translatable("quest.reward.item", getItemName(), count);
            case XP -> Component.translatable("quest.reward.xp", amount);
            case EFFECT -> Component.translatable("quest.reward.effect", getEffectName(), getEffectSeconds());
            case COMMAND -> Component.translatable("quest.reward.command");
            case ADVANCEMENT -> Component.translatable("quest.reward.advancement", getAdvancementName());
        });
    }

    // Serialize this reward into a network buffer.
    public void writeToBuf(FriendlyByteBuf buf) {
        buf.writeEnum(Objects.requireNonNull(type));
        buf.writeBoolean(item != null);
        if (item != null) {
            buf.writeResourceLocation(item);
        }
        buf.writeVarInt(count);
        buf.writeVarInt(amount);
        buf.writeBoolean(effect != null);
        if (effect != null) {
            buf.writeResourceLocation(effect);
        }
        buf.writeVarInt(duration);
        buf.writeVarInt(amplifier);
        buf.writeUtf(Objects.requireNonNull(Objects.requireNonNullElse(command, "")));
        buf.writeBoolean(advancement != null);
        if (advancement != null) {
            buf.writeResourceLocation(advancement);
        }
    }

    // Deserialize a reward from a network buffer.
    public static QuestReward readFromBuf(FriendlyByteBuf buf) {
        QuestRewardType type = buf.readEnum(QuestRewardType.class);
        ResourceLocation item = buf.readBoolean() ? buf.readResourceLocation() : null;
        int count = buf.readVarInt();
        int amount = buf.readVarInt();
        ResourceLocation effect = buf.readBoolean() ? buf.readResourceLocation() : null;
        int duration = buf.readVarInt();
        int amplifier = buf.readVarInt();
        String command = buf.readUtf();
        ResourceLocation advancement = buf.readBoolean() ? buf.readResourceLocation() : null;
        return new QuestReward(type, item, count, amount, effect, duration, amplifier, command, advancement);
    }

    // Grant an item stack to the player.
    private void grantItem(ServerPlayer player) {
        if (item == null) {
            return;
        }
        ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.getValue(item), count);
        player.getInventory().placeItemBackInInventory(stack);
    }

    // Grant a potion effect to the player.
    private void grantEffect(ServerPlayer player) {
        if (effect == null) {
            return;
        }
        BuiltInRegistries.MOB_EFFECT
                .get(Objects.requireNonNull(effect))
                .ifPresent(effectHolder -> player.addEffect(new MobEffectInstance(Objects.requireNonNull(effectHolder), duration, amplifier)));
    }

    // Execute a server command on behalf of the player.
    private void runCommand(ServerPlayer player) {
        if (command == null || command.isEmpty()) {
            return;
        }
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        CommandSourceStack source = Objects.requireNonNull(player.createCommandSourceStack().withSuppressedOutput());
        server.getCommands().performPrefixedCommand(source, Objects.requireNonNull(command));
    }

    // Grant an advancement to the player.
    private void grantAdvancement(ServerPlayer player) {
        if (advancement == null) {
            return;
        }
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        var advancementHolder = server.getAdvancements().get(Objects.requireNonNull(advancement));
        if (advancementHolder == null) {
            return;
        }
        var progress = player.getAdvancements().getOrStartProgress(advancementHolder);
        for (String criterion : progress.getRemainingCriteria()) {
            player.getAdvancements().award(advancementHolder, Objects.requireNonNull(criterion));
        }
    }

    // Safely read string values from JSON.
    private static String getString(JsonObject json, String key) {
        return json.has(key) ? Objects.requireNonNull(json.get(key).getAsString()) : null;
    }

    // Safely read resource locations from JSON.
    private static ResourceLocation getResource(JsonObject json, String key) {
        if (!json.has(key)) {
            return null;
        }
        return ResourceLocation.tryParse(Objects.requireNonNull(json.get(key).getAsString()));
    }

    private Component getItemName() {
        if (item == null) {
            return Component.translatable("quest.reward.unknown");
        }
        ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.getValue(item));
        return stack.getHoverName();
    }

    private Component getEffectName() {
        if (effect == null) {
            return Component.translatable("quest.reward.unknown");
        }
        var mobEffect = BuiltInRegistries.MOB_EFFECT.get(effect);
        if (mobEffect.isEmpty()) {
            return Component.literal(effect.toString());
        }
        return Component.translatable(mobEffect.get().value().getDescriptionId());
    }

    private Component getAdvancementName() {
        if (advancement == null) {
            return Component.translatable("quest.reward.unknown");
        }
        return Component.literal(advancement.toString());
    }

    private int getEffectSeconds() {
        return Math.max(0, duration) / 20;
    }
}


