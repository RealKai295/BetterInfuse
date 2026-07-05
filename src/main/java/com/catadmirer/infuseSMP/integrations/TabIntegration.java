package com.catadmirer.infuseSMP.integrations;

import com.catadmirer.infuseSMP.Infuse;
import com.catadmirer.infuseSMP.effects.InfuseEffect;
import com.catadmirer.infuseSMP.extraeffects.Apophis;
import me.neznamy.tab.api.TabAPI;
import me.neznamy.tab.api.TabPlayer;
import me.neznamy.tab.api.event.player.PlayerLoadEvent;
import me.neznamy.tab.api.event.plugin.TabLoadEvent;
import me.neznamy.tab.api.nametag.NameTagManager;
import me.neznamy.tab.api.tablist.TabListFormatManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class TabIntegration {
    public static final Component APOPHIS_NAME = Component.text("Apophis", NamedTextColor.DARK_PURPLE);
    private static final String APOPHIS_TAB_NAME = "&5Apophis";

    private final Infuse plugin;
    private Boolean enabled;

    public TabIntegration(Infuse plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        Plugin tab = Bukkit.getPluginManager().getPlugin("TAB");
        enabled = tab != null && tab.isEnabled();
    }

    public boolean isEnabled() {
        if (enabled == null) {
            initialize();
        }

        return enabled;
    }

    public void registerHandlers() {
        if (!isEnabled()) {
            return;
        }

        TabAPI.getInstance().getEventBus().register(PlayerLoadEvent.class, event -> {
            Player player = Bukkit.getPlayer(event.getPlayer().getUniqueId());
            if (player != null && hasApophis(player)) {
                applyTabFormatting(event.getPlayer());
            }
        });

        TabAPI.getInstance().getEventBus().register(TabLoadEvent.class, event -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (hasApophis(player)) {
                    applyTabFormatting(player);
                }
            }
        });
    }

    public void applyApophisName(Player player) {
        player.displayName(APOPHIS_NAME);
        player.playerListName(APOPHIS_NAME);
        player.customName(null);
        player.setCustomNameVisible(false);

        if (isEnabled()) {
            applyTabFormatting(player);
        }
    }

    public void removeApophisName(Player player, Component displayName, Component customName, boolean customNameVisible) {
        Component restoredDisplay = displayName == null ? Component.text(player.getName()) : displayName;
        player.displayName(restoredDisplay);
        player.playerListName(restoredDisplay);
        player.customName(customName);
        player.setCustomNameVisible(customNameVisible);

        clearTabFormatting(player);
    }

    private void applyTabFormatting(Player player) {
        if (!isEnabled()) {
            return;
        }

        runWhenTabPlayerReady(player, this::applyTabFormatting);
    }

    private void applyTabFormatting(TabPlayer tabPlayer) {
        TabListFormatManager tabListFormat = TabAPI.getInstance().getTabListFormatManager();
        if (tabListFormat != null) {
            tabListFormat.setName(tabPlayer, APOPHIS_TAB_NAME);
        }

        NameTagManager nameTagManager = TabAPI.getInstance().getNameTagManager();
        if (nameTagManager != null) {
            nameTagManager.showNameTag(tabPlayer);
        }
    }

    private void clearTabFormatting(Player player) {
        if (!isEnabled()) {
            return;
        }

        runWhenTabPlayerReady(player, this::clearTabFormatting);
    }

    private void clearTabFormatting(TabPlayer tabPlayer) {
        TabListFormatManager tabListFormat = TabAPI.getInstance().getTabListFormatManager();
        if (tabListFormat != null) {
            tabListFormat.setName(tabPlayer, null);
        }

        NameTagManager nameTagManager = TabAPI.getInstance().getNameTagManager();
        if (nameTagManager != null) {
            nameTagManager.showNameTag(tabPlayer);
        }
    }

    private void runWhenTabPlayerReady(Player player, java.util.function.Consumer<TabPlayer> action) {
        TabPlayer tabPlayer = TabAPI.getInstance().getPlayer(player.getUniqueId());
        if (tabPlayer != null) {
            action.accept(tabPlayer);
            return;
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            TabPlayer loaded = TabAPI.getInstance().getPlayer(player.getUniqueId());
            if (loaded != null) {
                action.accept(loaded);
            }
        }, 1L);
    }

    private boolean hasApophis(Player player) {
        InfuseEffect apophis = InfuseEffect.fromString("apophis");
        return apophis != null && plugin.getDataManager().hasEffect(player, apophis);
    }

    public void logStatus() {
        if (isEnabled()) {
            Infuse.LOGGER.info("TAB integration enabled — Apophis disguises will override tablist and nametags.");
        }
    }
}
