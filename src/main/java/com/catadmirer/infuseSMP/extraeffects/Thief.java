package com.catadmirer.infuseSMP.extraeffects;

import com.catadmirer.infuseSMP.EffectConstants;
import com.catadmirer.infuseSMP.EffectIds;
import com.catadmirer.infuseSMP.Infuse;
import com.catadmirer.infuseSMP.Message;
import com.catadmirer.infuseSMP.Message.MessageType;
import com.catadmirer.infuseSMP.effects.InfuseEffect;
import com.catadmirer.infuseSMP.managers.CooldownManager;
import com.catadmirer.infuseSMP.events.EffectUnequipEvent;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

public class Thief extends InfuseEffect {
    private static final long DISGUISE_DURATION_MS = 3600L * 1000L;

    private static final Map<UUID, DisguiseData> disguisedPlayers = new HashMap<>();
    private static final Map<UUID, BossBar> disguiseBossBars = new HashMap<>();
    private static final Map<UUID, BukkitTask> disguiseTasks = new HashMap<>();

    private final Infuse plugin;

    public Thief() {
        this(false);
    }

    public Thief(boolean augmented) {
        super("thief", EffectIds.THIEF, augmented, EffectConstants.potionColor(EffectIds.THIEF), EffectConstants.ritualColor(EffectIds.THIEF));

        this.plugin = Infuse.getInstance();
    }

    @Override
    public void equip(Player owner) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.unlistPlayer(owner);
        }
    }

    @Override
    public void unequip(Player owner) {
        clearDisguise(owner);

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.listPlayer(owner);
        }
    }

    @Override
    public void applyPassives(Player owner) {}

    @Override
    public void activateSpark(Player owner) {
        UUID playerUUID = owner.getUniqueId();
        if (CooldownManager.isOnCooldown(playerUUID, "thief")) return;

        owner.playSound(owner.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1, 1);

        // Applying cooldowns and durations for the effect
        long cooldown = plugin.getMainConfig().cooldown(this);
        long duration = plugin.getMainConfig().duration(this);

        CooldownManager.setTimes(playerUUID, "thief", duration, cooldown);
    }

    @Override
    public InfuseEffect getRegularVersion() {
        return new Thief();
    }

    @Override
    public InfuseEffect getAugmentedVersion() {
        return new Thief(true);
    }

    @Override
    public Message getName() {
        return new Message(augmented ? MessageType.AUG_THIEF_NAME : MessageType.THIEF_NAME);
    }

    @Override
    public Message getLore() {
        return new Message(augmented ? MessageType.AUG_THIEF_LORE : MessageType.THIEF_LORE);
    }

    private void activateEffect(Player player, @NotNull InfuseEffect effect, Entity victim) {
        Message msg = new Message(MessageType.THIEF_STEAL);
        msg.applyPlaceholder("victim", victim.getName());
        msg.applyPlaceholder("effect_name", effect.getName().toComponent());
        player.sendMessage(msg.toComponent());

        // Activating the stolen spark.
        effect.activateSpark(player);

        UUID playerUUID = player.getUniqueId();

        // Removing cooldowns from the stolen spark
        CooldownManager.clearSpecificCooldown(playerUUID, effect.getKey());
        CooldownManager.clearSpecificDuration(playerUUID, effect.getKey());

        // Applying cooldowns for the thief effect
        long cooldown = plugin.getMainConfig().cooldown(effect);
        long duration = plugin.getMainConfig().duration(effect);

        CooldownManager.setTimes(playerUUID, "thief_stolen", duration, cooldown * 2);
    }

    /**
     * Disguises a thief user into another player.
     * Overrides the thief user's name and skin.
     *
     * @param thiefUser The thief user to disguise
     * @param player The player to disguise the thief as
     */
    private void disguise(Player thiefUser, Player victim) {
        if (disguisedPlayers.containsKey(thiefUser.getUniqueId())) {
            removeDisguise(thiefUser);
        }

        // Storing the killer's original skin
        Optional<ProfileProperty> thiefTextures = getTexturesProperty(thiefUser);
        disguisedPlayers.put(
                thiefUser.getUniqueId(),
                new DisguiseData(
                        thiefUser.getName(),
                        thiefUser.customName(),
                        thiefUser.displayName(),
                        thiefUser.playerListName(),
                        thiefUser.isCustomNameVisible(),
                        thiefTextures.map(ProfileProperty::getValue).orElse(""),
                        thiefTextures.map(ProfileProperty::getSignature).orElse(null),
                        victim.getName()
                )
        );

        Optional<ProfileProperty> victimTextures = getTexturesProperty(victim);
        String textureValue = victimTextures.map(ProfileProperty::getValue).orElse("");
        String textureSignature = victimTextures.map(ProfileProperty::getSignature).orElse(null);

        Component victimDisplayName = victim.displayName();
        Component victimListName = victim.playerListName();

        // Taking the dead player's name
        thiefUser.displayName(victimDisplayName);
        thiefUser.playerListName(victimListName);
        thiefUser.customName(null);
        thiefUser.setCustomNameVisible(false);

        // Taking the dead player's skin
        applyVictimDisguise(thiefUser, victim.getName(), textureValue, textureSignature);

        long disguiseEndTime = System.currentTimeMillis() + DISGUISE_DURATION_MS; // 1 hour

        // Showing the disguise timer bossbar
        BossBar bossBar = Bukkit.createBossBar("Disguise", BarColor.PINK, BarStyle.SOLID);
        bossBar.setProgress(1);
        bossBar.addPlayer(thiefUser);
        disguiseBossBars.put(thiefUser.getUniqueId(), bossBar);

        // Starting the task to update the bossbar and eventually revert the disguise.
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                long timeLeft = disguiseEndTime - System.currentTimeMillis();

                if (!thiefUser.isOnline() || !disguisedPlayers.containsKey(thiefUser.getUniqueId())) {
                    bossBar.removePlayer(thiefUser);
                    disguiseBossBars.remove(thiefUser.getUniqueId());
                    disguiseTasks.remove(thiefUser.getUniqueId());
                    cancel();
                    return;
                }

                if (timeLeft <= 0) {
                    removeDisguise(thiefUser);
                    cancel();
                    return;
                }

                bossBar.setProgress(Math.clamp((double) timeLeft / DISGUISE_DURATION_MS, 0.0, 1.0));
            }
        }.runTaskTimer(plugin, 0, 20);

        disguiseTasks.put(thiefUser.getUniqueId(), task);
    }

    /**
     * Removes a disguise from a player.
     * Sets a player's skin and name to what they were before they disguised.
     *
     * @param player The player to remove the disguise from
     */
    private void removeDisguise(Player player) {
        // Getting the original data for the player
        DisguiseData originalData = disguisedPlayers.remove(player.getUniqueId());
        cancelDisguiseTask(player);

        if (originalData != null) {
            // Resetting the player's name
            restoreIdentity(
                    player,
                    originalData.originalName(),
                    originalData.customName(),
                    originalData.displayName(),
                    originalData.playerListName(),
                    originalData.customNameVisible(),
                    originalData.textureValue(),
                    originalData.textureSignature()
            );
            return;
        }

        forceRestoreIdentity(player);
    }

    private void clearDisguise(Player player) {
        removeDisguise(player);

        if (isDisguisedProfile(player)) {
            forceRestoreIdentity(player);
        }
    }

    private void forceRestoreIdentity(Player player) {
        cancelDisguiseTask(player);

        player.customName(null);
        player.setCustomNameVisible(false);
        player.displayName(Component.text(player.getName()));
        player.playerListName(Component.text(player.getName()));

        restoreOriginalSkinFromMojang(player);
    }

    private void restoreIdentity(
            Player player,
            String originalName,
            Component customName,
            Component displayName,
            Component playerListName,
            boolean customNameVisible,
            String textureValue,
            String textureSignature
    ) {
        player.customName(customName);
        player.displayName(displayName == null ? Component.text(originalName) : displayName);
        player.playerListName(playerListName == null ? Component.text(originalName) : playerListName);
        player.setCustomNameVisible(customNameVisible);

        // Resetting the player's skin
        PlayerProfile profile = Bukkit.createProfileExact(player.getUniqueId(), originalName);
        if (hasSavedTextures(textureValue)) {
            profile.setProperty(new ProfileProperty("textures", textureValue, normalizeSignature(textureSignature)));
            player.setPlayerProfile(profile);
            refreshPlayerForViewers(player);
            return;
        }

        restoreOriginalSkinFromMojang(player);
    }

    private void cancelDisguiseTask(Player player) {
        BukkitTask task = disguiseTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }

        BossBar bossBar = disguiseBossBars.remove(player.getUniqueId());
        if (bossBar != null) {
            bossBar.removePlayer(player);
        }
    }

    private boolean isDisguisedProfile(Player player) {
        String profileName = player.getPlayerProfile().getName();
        return profileName != null && !profileName.equals(player.getName());
    }

    private void applyVictimDisguise(Player thiefUser, String victimName, String textureValue, String textureSignature) {
        PlayerProfile profile = Bukkit.createProfileExact(thiefUser.getUniqueId(), victimName);
        if (hasSavedTextures(textureValue)) {
            profile.setProperty(new ProfileProperty("textures", textureValue, normalizeSignature(textureSignature)));
        }
        thiefUser.setPlayerProfile(profile);
        refreshPlayerForViewers(thiefUser);
    }

    private void restoreOriginalSkinFromMojang(Player player) {
        org.bukkit.profile.PlayerProfile profile = Bukkit.createProfile(player.getUniqueId(), player.getName());
        profile.update().thenAcceptAsync(updated -> Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }

            PlayerProfile paperProfile = Bukkit.createProfileExact(player.getUniqueId(), player.getName());
            paperProfile.setTextures(updated.getTextures());
            player.setPlayerProfile(paperProfile);
            refreshPlayerForViewers(player);
        }), runnable -> Bukkit.getScheduler().runTask(plugin, runnable));
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

    private static Optional<ProfileProperty> getTexturesProperty(Player player) {
        return player.getPlayerProfile().getProperties().stream()
                .filter(property -> "textures".equals(property.getName()))
                .findFirst();
    }

    private static boolean hasSavedTextures(String textureValue) {
        return textureValue != null && !textureValue.isEmpty() && !"null".equals(textureValue);
    }

    private static String normalizeSignature(String textureSignature) {
        return "null".equals(textureSignature) ? null : textureSignature;
    }

    private record DisguiseData(
            String originalName,
            Component customName,
            Component displayName,
            Component playerListName,
            boolean customNameVisible,
            String textureValue,
            String textureSignature,
            String victimName
    ) {}

    //// Listeners ////
    //// These are only registered once, so they need to be able to handle being used for every player, no matter what effects they actually have

    @EventHandler
    public void onEffectUnequip(EffectUnequipEvent event) {
        if (event.getEffect().getId() != EffectIds.THIEF) {
            return;
        }

        clearDisguise(event.getPlayer());
    }

    // Hiding thief effect users from players who recently joined
    @EventHandler
    public void hideThievesOnJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }

            if (!plugin.getDataManager().hasEffect(player, this) && isDisguisedProfile(player)) {
                forceRestoreIdentity(player);
            }
        }, 1L);

        if (plugin.getDataManager().hasEffect(player, this)) {
            Bukkit.getOnlinePlayers().forEach(p -> p.unlistPlayer(player));
        }

        for (Player otherPlayer : Bukkit.getOnlinePlayers()) {
            if (!plugin.getDataManager().hasEffect(otherPlayer, this)) continue;

            player.unlistPlayer(otherPlayer);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        disguisedPlayers.remove(player.getUniqueId());
        cancelDisguiseTask(player);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player deadPlayer = event.getEntity();

        // If a disguised player dies, revert their disguise
        if (disguisedPlayers.containsKey(deadPlayer.getUniqueId())) {
            removeDisguise(deadPlayer);
        }

        if (!(event.getDamageSource().getCausingEntity() instanceof Player killer)) return;

        // If a player with the thief effect kills someone, they should disguise themselves as the player they kill
        if (plugin.getDataManager().hasEffect(killer, this)) {
            disguise(killer, deadPlayer);
        }
    }

    @EventHandler
    public void onPlayerHit(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!(event.getDamager() instanceof Player player)) return;
        if (!plugin.getDataManager().hasEffect(player, this)) return;

        UUID playerUUID = player.getUniqueId();
        if (!CooldownManager.isEffectActive(playerUUID, "thief")) return;

        InfuseEffect leftEffect = plugin.getDataManager().getEffect(victim.getUniqueId(), "1");
        InfuseEffect rightEffect = plugin.getDataManager().getEffect(victim.getUniqueId(), "2");

        if (leftEffect != null && rightEffect != null) {
            activateEffect(player, Math.random() > 0.5 ? leftEffect : rightEffect, victim);
        } else if (leftEffect != null) {
            activateEffect(player, leftEffect, victim);
        } else if (rightEffect != null) {
            activateEffect(player, rightEffect, victim);
        } else return;

        CooldownManager.setDuration(playerUUID, "thief", 0);
    }
}
