package com.catadmirer.infuseSMP.managers;

import com.catadmirer.infuseSMP.EffectIds;
import com.catadmirer.infuseSMP.Infuse;
import com.catadmirer.infuseSMP.events.EffectEquipEvent;
import com.catadmirer.infuseSMP.events.EffectUnequipEvent;
import com.catadmirer.infuseSMP.integrations.TabIntegration;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class ApophisManager implements Listener {
    private static final MiniMessage mm = MiniMessage.miniMessage();
    private static final String APOPHIS_PROFILE_NAME = "Apophis";

    private final Infuse plugin;
    private final TabIntegration tabIntegration;
    private final Map<UUID, Integer> refreshGeneration = new HashMap<>();
    private final Map<UUID, SavedDisguise> savedDisguises = new HashMap<>();

    private final ProfileProperty APOPHIS_SKIN = new ProfileProperty(
            "textures",
            "ewogICJ0aW1lc3RhbXAiIDogMTcxNzg4NTA2MDQwNywKICAicHJvZmlsZUlkIiA6ICJlZGUyYzdhMGFjNjM0MTNiYjA5ZDNmMGJlZTllYzhlYyIsCiAgInByb2ZpbGVOYW1lIiA6ICJ0aGVEZXZKYWRlIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlL2MwOTBmY2NjMjBmMWM3ZWMyMDBkNGVkMDUxMjQwNjM3ZmRmNjE5ZDg1Nzg0NWZhNWRmNWJkMzM1MWJiMjBkOCIKICAgIH0KICB9Cn0=",
            "mBgGwS28lqNz7rJCysD9SElJpA5q+34uTZK68JFXIFzuoN31KQg2VHjVDz+/nAr0yXdRwOrgL5rnRb2NbKBPyKSWdcB8A1nVHeNMpoJ5c5CzEERyOROUiTRxge/MIhYL7Fkj67fkh7Sc/l7BwDAf7/7OIgiAIleUTLZ9COnIN15gylTBldOo3JOka8TTNrI1i4QmnMsbgT0luQZzrUMRtZxIHNwx+26IevzCE+hpNdwiYqnDVZdayDLPVy1vv+i3C7AJGd9b7/2/qv0YmWxvT3uKrPR8+9fbSWltGx9ikrdXO17FrGc5u0gqmPWAaSSWw/NJmMhPenILh7/MvXA8mO2m7JeuhnM/EYzdOMB3qzvkUEVddFIngPl6LNE8XG1R+APFBsbpnpybB7dQphSud5DNfuZijqLDd735kykYlRMzw5VVGf7fONheLzSV42XRsIU+5IazHvmAZ4pxr72+r9bbS9vRW38ZgQIy6p8r4tLv9jfmqmcS9lEn1CAgDLAqZWGzIWeIgOdDsrWH4ia/1gj6oZVefRCr2dAS84NsOQUdoJDbS8G0+ArN+CWgnlcwOJCS6MB5kBmQl2FPvwLcSnnRcS66XKfH28Bu2/J3Hu5zRWbONuOLQTbYFxwftUtvS1IORKBCfWvlJTx5G/mz1KOGW89iOCpW8jdx8EmzpRI="
    );

    private record SavedDisguise(
            String originalName,
            Component displayName,
            Component customName,
            boolean customNameVisible,
            String textureValue,
            String textureSignature
    ) {}

    public ApophisManager(Infuse plugin) {
        this.plugin = plugin;
        this.tabIntegration = new TabIntegration(plugin);
    }

    public TabIntegration getTabIntegration() {
        return tabIntegration;
    }

    @EventHandler
    public void equipApophis(EffectEquipEvent event) {
        if (event.getEffect().getId() != EffectIds.APOPHIS) return;

        Player target = event.getPlayer();
        // Making sure the disguise file is created
        ensureDisguiseSaved(target);
        applyApophisDisguise(target);
        scheduleDisguiseRefresh(target);
    }

    @EventHandler
    public void unequipApophis(EffectUnequipEvent event) {
        if (event.getEffect().getId() != EffectIds.APOPHIS) return;

        Player target = event.getPlayer();
        if (!target.isOnline()) {
            Infuse.LOGGER.warn("Could not remove {0}'s disguise as they are not online.", target.getName());
            return;
        }

        clearApophisDisguise(target);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }

            if (hasApophisEquipped(player)) {
                ensureDisguiseSaved(player);
                applyApophisDisguise(player);
                scheduleDisguiseRefresh(player);
            } else if (isApophisProfile(player)) {
                clearApophisDisguise(player);
            }
        }, 1L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (!hasApophisEquipped(event.getPlayer())) {
            savedDisguises.remove(event.getPlayer().getUniqueId());
        }
    }

    private void ensureDisguiseSaved(Player target) {
        UUID uuid = target.getUniqueId();
        // Skipping players who already have a disguise file
        if (savedDisguises.containsKey(uuid)) {
            return;
        }

        SavedDisguise disguise = loadDisguiseFromFile(uuid).orElseGet(() -> captureDisguiseState(target));
        savedDisguises.put(uuid, disguise);
        writeDisguiseToFile(uuid, disguise);
    }

    private SavedDisguise captureDisguiseState(Player target) {
        Optional<ProfileProperty> textures = target.getPlayerProfile().getProperties().stream()
                .filter(property -> "textures".equals(property.getName()))
                .findFirst();

        String textureValue = textures.map(ProfileProperty::getValue).orElse("");
        String textureSignature = textures.map(ProfileProperty::getSignature).orElse(null);

        return new SavedDisguise(
                target.getName(),
                target.displayName(),
                target.customName(),
                target.isCustomNameVisible(),
                textureValue,
                textureSignature
        );
    }

    private Optional<SavedDisguise> loadDisguiseFromFile(UUID uuid) {
        // Getting the player's skin info from the disguise file
        File disguiseFile = disguiseFile(uuid);
        if (!disguiseFile.exists()) {
            return Optional.empty();
        }

        try (Scanner scanner = new Scanner(disguiseFile)) {
            Component displayName = null;
            String textureValue = "";
            String textureSignature = null;
            Component customName = null;
            boolean customNameVisible = false;

            // Getting the player's name
            if (scanner.hasNextLine()) {
                displayName = mm.deserialize(scanner.nextLine());
            }

            // Getting the property value
            if (scanner.hasNextLine()) {
                textureValue = scanner.nextLine();
            }

            // Getting the property signature
            if (scanner.hasNextLine()) {
                textureSignature = scanner.nextLine();
                if ("null".equals(textureSignature)) {
                    textureSignature = null;
                }
            }

            if (scanner.hasNextLine()) {
                String customNameLine = scanner.nextLine();
                if (!customNameLine.isEmpty()) {
                    customName = mm.deserialize(customNameLine);
                }
            }

            if (scanner.hasNextLine()) {
                customNameVisible = Boolean.parseBoolean(scanner.nextLine());
            }

            return Optional.of(new SavedDisguise(
                    null,
                    displayName,
                    customName,
                    customNameVisible,
                    textureValue,
                    textureSignature
            ));
        } catch (IOException err) {
            Infuse.LOGGER.error("Failed to read disguise data from {}", disguiseFile.getPath(), err);
            return Optional.empty();
        }
    }

    private void writeDisguiseToFile(UUID uuid, SavedDisguise disguise) {
        // Getting the disguise file for the player
        File disguiseFile = disguiseFile(uuid);
        disguiseFile.getParentFile().mkdirs();

        try (FileWriter writer = new FileWriter(disguiseFile)) {
            // Writing the urls to disk
            writer.write(mm.serialize(disguise.displayName() == null ? Component.text("") : disguise.displayName()));
            writer.write("\n");

            if (!hasSavedTextures(disguise.textureValue())) {
                writer.write("null\nnull");
            } else {
                writer.write(disguise.textureValue());
                writer.write("\n");
                writer.write(String.valueOf(disguise.textureSignature()));
            }

            writer.write("\n");
            writer.write(disguise.customName() == null ? "" : mm.serialize(disguise.customName()));
            writer.write("\n");
            writer.write(String.valueOf(disguise.customNameVisible()));
        } catch (IOException err) {
            Infuse.LOGGER.error("Failed to write disguise data to {}", disguiseFile.getPath(), err);
        }
    }

    private void applyApophisDisguise(Player target) {
        if (!hasApophisEquipped(target)) {
            return;
        }

        // Changing the player's skin
        applyApophisProfile(target);
        // Hiding the player's name
        tabIntegration.applyApophisName(target);
    }

    private void clearApophisDisguise(Player target) {
        cancelDisguiseRefresh(target);

        // Getting the player's skin info from the disguise file
        SavedDisguise saved = savedDisguises.remove(target.getUniqueId());
        String originalName = saved != null && saved.originalName() != null ? saved.originalName() : target.getName();

        if (saved != null) {
            // Getting the player's name
            tabIntegration.removeApophisName(
                    target,
                    saved.displayName() == null ? Component.text(originalName) : saved.displayName(),
                    saved.customName(),
                    saved.customNameVisible()
            );
            restorePlayerIdentity(target, originalName, saved.textureValue(), saved.textureSignature());
        } else {
            tabIntegration.removeApophisName(target, Component.text(originalName), null, false);
            restorePlayerIdentity(target, originalName, "", null);
        }

        // Deleting the disguise file
        deleteDisguiseFile(target.getUniqueId());
        scheduleRestoreVerification(target, originalName, saved);

        if (isApophisProfile(target)) {
            forceRestoreIdentity(target);
        }
    }

    private void forceRestoreIdentity(Player target) {
        tabIntegration.removeApophisName(target, Component.text(target.getName()), null, false);
        restorePlayerSkinFromMojang(target);
    }

    private void scheduleRestoreVerification(Player target, String originalName, SavedDisguise saved) {
        int generationAtUnequip = refreshGeneration.getOrDefault(target.getUniqueId(), 0);

        for (long delay : new long[] {1L, 5L, 20L}) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!target.isOnline() || hasApophisEquipped(target)) {
                    return;
                }

                if (refreshGeneration.getOrDefault(target.getUniqueId(), 0) != generationAtUnequip) {
                    return;
                }

                if (!isApophisProfile(target)) {
                    return;
                }

                if (saved != null) {
                    restorePlayerIdentity(target, originalName, saved.textureValue(), saved.textureSignature());
                }

                if (isApophisProfile(target)) {
                    forceRestoreIdentity(target);
                }
            }, delay);
        }
    }

    private boolean isApophisProfile(Player player) {
        return APOPHIS_PROFILE_NAME.equals(player.getPlayerProfile().getName());
    }

    private void applyApophisProfile(Player target) {
        PlayerProfile profile = Bukkit.createProfileExact(target.getUniqueId(), APOPHIS_PROFILE_NAME);
        profile.setProperty(APOPHIS_SKIN);
        target.setPlayerProfile(profile);
        refreshPlayerForViewers(target);
    }

    private void restorePlayerIdentity(Player target, String originalName, String textureValue, String textureSignature) {
        PlayerProfile profile = Bukkit.createProfileExact(target.getUniqueId(), originalName);
        if (hasSavedTextures(textureValue)) {
            profile.setProperty(new ProfileProperty("textures", textureValue, normalizeSignature(textureSignature)));
        }
        target.setPlayerProfile(profile);
        refreshPlayerForViewers(target);
    }

    private void restorePlayerSkinFromMojang(Player target) {
        org.bukkit.profile.PlayerProfile profile = Bukkit.createProfile(target.getUniqueId(), target.getName());
        profile.update().thenAcceptAsync(updated -> Bukkit.getScheduler().runTask(plugin, () -> {
            if (!target.isOnline() || hasApophisEquipped(target)) {
                return;
            }

            applyBukkitProfile(target, updated);
            refreshPlayerForViewers(target);
        }), runnable -> Bukkit.getScheduler().runTask(plugin, runnable));
    }

    private void applyBukkitProfile(Player target, org.bukkit.profile.PlayerProfile source) {
        UUID uuid = source.getUniqueId() != null ? source.getUniqueId() : target.getUniqueId();
        String name = source.getName() != null ? source.getName() : target.getName();
        PlayerProfile profile = Bukkit.createProfileExact(uuid, name);
        profile.setTextures(source.getTextures());
        target.setPlayerProfile(profile);
    }

    private void refreshPlayerForViewers(Player target) {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (viewer.equals(target)) {
                continue;
            }

            viewer.hidePlayer(plugin, target);
            viewer.showPlayer(plugin, target);
        }
    }

    private void scheduleDisguiseRefresh(Player target) {
        UUID uuid = target.getUniqueId();
        int generation = refreshGeneration.getOrDefault(uuid, 0) + 1;
        refreshGeneration.put(uuid, generation);

        for (long delay : new long[] {1L, 5L, 20L}) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!target.isOnline() || !hasApophisEquipped(target)) {
                    return;
                }

                if (refreshGeneration.getOrDefault(uuid, 0) != generation) {
                    return;
                }

                applyApophisDisguise(target);
            }, delay);
        }
    }

    private void cancelDisguiseRefresh(Player target) {
        refreshGeneration.merge(target.getUniqueId(), 1, Integer::sum);
    }

    private boolean hasApophisEquipped(Player target) {
        var dataManager = plugin.getDataManager();
        var effect1 = dataManager.getEffect(target.getUniqueId(), "1");
        var effect2 = dataManager.getEffect(target.getUniqueId(), "2");
        return (effect1 != null && effect1.getId() == EffectIds.APOPHIS)
                || (effect2 != null && effect2.getId() == EffectIds.APOPHIS);
    }

    private boolean hasActiveDisguise(Player player) {
        return isApophisProfile(player);
    }

    private static boolean hasSavedTextures(String textureValue) {
        return textureValue != null && !textureValue.isEmpty() && !"null".equals(textureValue);
    }

    private static String normalizeSignature(String textureSignature) {
        return "null".equals(textureSignature) ? null : textureSignature;
    }

    private File disguiseFile(UUID uuid) {
        return new File(plugin.getDataFolder(), "data/ApophisPlayers/" + uuid + ".yml");
    }

    private void deleteDisguiseFile(UUID uuid) {
        File disguiseFile = disguiseFile(uuid);
        if (disguiseFile.exists()) {
            disguiseFile.delete();
        }
    }
}
