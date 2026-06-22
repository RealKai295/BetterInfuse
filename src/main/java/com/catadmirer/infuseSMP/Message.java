package com.catadmirer.infuseSMP;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.TagPattern;
import net.kyori.adventure.text.minimessage.translation.Argument;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class Message {
    // Text serializers
    public static final MiniMessage mm = MiniMessage.miniMessage();
    public static final LegacyComponentSerializer la = LegacyComponentSerializer.legacyAmpersand();

    private TranslatableComponent message;

    public Message(MessageType messageType) {
        message = Component.translatable().key(messageType.name().toLowerCase()).build();
    }

    public void applyPlaceholder(@TagPattern String placeholder, Object value) {
        this.message = message.arguments(Argument.component(placeholder, mm.deserialize(String.valueOf(value))));
    }

    public void applyPlaceholders(Map<String,Object> placeholders) {
        placeholders.forEach(this::applyPlaceholder);
    }

    public List<Component> toComponentList() {
        if (!message.arguments().isEmpty()) {
            throw new IllegalStateException("Not all placeholders have been registered.");
        }

        return Stream.concat(Stream.of("<i:false>"), Stream.of(mm.serialize(message).split("\n"))).map(mm::deserialize).toList();
    }

    public Component toComponent() {
        if (!message.arguments().isEmpty()) {
            throw new IllegalStateException("Not all placeholders have been registered.");
        }

        return Component.text("").decoration(TextDecoration.ITALIC, false).append(message);
    }

    /**
     * Helper function that allows minimessage translation for an arbitrary string.
     *
     * @param message The minimessage string to translate
     *
     * @return The {@link Component} that can be sent to players.
     */
    public static Component toComponent(String message) {
        return MiniMessage.miniMessage().deserialize("<i:false>" + message);
    }

    public enum MessageType {
        EFFECT_BROADCAST,
        DISCORD_BROADCAST,
        EFFECT_FINISHED,
        REGULAR_BROADCAST,
        SLOT_EMPTY,
        EFFECT_NONE_EQUIPPED,
        WITHDRAW_INVALID,
        TRUST_CONSOLE_USAGE,
        TRUST_INCORRECT_USAGE,
        TRUST_NO_PLAYER,
        TRUST_SELF,
        TRUST_ADDED,
        TRUST_ALREADY_TRUSTED,
        TRUST_REMOVED,
        TRUST_NOT_TRUSTED,
        EFFECT_NO_BREWING,
        DEATH_MESSAGE,
        CONTROLS_USAGE,
        CONTROLS_INVALID_PARAM,
        INFUSE_INVALID_PARAM,
        INFUSE_INVALID_SLOT,
        INFUSE_CONTROLS_USAGE,
        INFUSE_CONTROLS_SUCCESS,
        INFUSE_SETEFFECT_USAGE,
        INFUSE_SETEFFECT_SUCCESS,
        INFUSE_GIVEEFFECT_USAGE,
        INFUSE_GIVEEFFECT_SUCCESS,
        INFUSE_CLEAREFFECTS_USAGE,
        INFUSE_CLEAREFFECTS_SUCCESS,
        INFUSE_COOLDOWN_USAGE,
        INFUSE_COOLDOWN_SUCCESS,
        JOIN_ABILITY_NOTIFY,
        DRAIN_SUCCESS,
        DRAIN_CANCELLED,
        EFFECT_EQUIPPED,
        SWAP_NO_EFFECTS,
        SWAP_SUCCESS,
        THIEF_STEAL,
        RECIPE_NOT_FOUND,
        RECIPE_DISABLED,
        ERROR_INV_FULL,
        ERROR_NOT_PLAYER,
        ERROR_NOT_OP,
        ERROR_INVALID_COMMAND,
        ERROR_RITUAL_ACTIVE,
        ERROR_TARGET_NOT_FOUND,

        // Effect messages
        EMERALD_NAME,
        EMERALD_LORE,
        AUG_EMERALD_NAME,
        AUG_EMERALD_LORE,
        ENDER_NAME,
        ENDER_LORE,
        AUG_ENDER_NAME,
        AUG_ENDER_LORE,
        FEATHER_NAME,
        FEATHER_LORE,
        AUG_FEATHER_NAME,
        AUG_FEATHER_LORE,
        FIRE_NAME,
        FIRE_LORE,
        AUG_FIRE_NAME,
        AUG_FIRE_LORE,
        FROST_NAME,
        FROST_LORE,
        AUG_FROST_NAME,
        AUG_FROST_LORE,
        HASTE_NAME,
        HASTE_LORE,
        AUG_HASTE_NAME,
        AUG_HASTE_LORE,
        HEART_NAME,
        HEART_LORE,
        AUG_HEART_NAME,
        AUG_HEART_LORE,
        INVIS_NAME,
        INVIS_LORE,
        AUG_INVIS_NAME,
        AUG_INVIS_LORE,
        OCEAN_NAME,
        OCEAN_LORE,
        AUG_OCEAN_NAME,
        AUG_OCEAN_LORE,
        REGEN_NAME,
        REGEN_LORE,
        AUG_REGEN_NAME,
        AUG_REGEN_LORE,
        SPEED_NAME,
        SPEED_LORE,
        AUG_SPEED_NAME,
        AUG_SPEED_LORE,
        STRENGTH_NAME,
        STRENGTH_LORE,
        AUG_STRENGTH_NAME,
        AUG_STRENGTH_LORE,
        THUNDER_NAME,
        THUNDER_LORE,
        AUG_THUNDER_NAME,
        AUG_THUNDER_LORE,

        // Extra effect messages
        APOPHIS_NAME,
        APOPHIS_LORE,
        AUG_APOPHIS_NAME,
        AUG_APOPHIS_LORE,
        THIEF_NAME,
        THIEF_LORE,
        AUG_THIEF_NAME,
        AUG_THIEF_LORE;
    }
}