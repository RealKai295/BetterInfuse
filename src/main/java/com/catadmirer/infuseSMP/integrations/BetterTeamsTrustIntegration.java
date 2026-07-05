package com.catadmirer.infuseSMP.integrations;

import com.booksaw.betterTeams.Team;
import com.catadmirer.infuseSMP.Infuse;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;

public class BetterTeamsTrustIntegration {
    private Boolean enabled;

    public void initialize() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("BetterTeams");
        enabled = plugin != null && plugin.isEnabled();
    }

    public boolean isEnabled() {
        if (enabled == null) {
            initialize();
        }

        return enabled;
    }

    public boolean areTeammates(OfflinePlayer first, OfflinePlayer second) {
        if (!isEnabled() || first == null || second == null) {
            return false;
        }

        if (first.getUniqueId().equals(second.getUniqueId())) {
            return true;
        }

        Team team = Team.getTeam(first);
        return team != null && team.getMembers().contains(second);
    }

    public List<OfflinePlayer> getTeammates(OfflinePlayer player) {
        if (!isEnabled() || player == null) {
            return List.of();
        }

        Team team = Team.getTeam(player);
        if (team == null) {
            return List.of();
        }

        UUID playerId = player.getUniqueId();
        List<OfflinePlayer> teammates = new ArrayList<>();
        for (OfflinePlayer teammate : team.getMembers().getOfflinePlayers()) {
            if (!teammate.getUniqueId().equals(playerId)) {
                teammates.add(teammate);
            }
        }

        return teammates;
    }

    public List<OfflinePlayer> mergeTrustedWithTeammates(OfflinePlayer truster, List<OfflinePlayer> trusted) {
        if (!isEnabled()) {
            return trusted;
        }

        Set<UUID> seen = new LinkedHashSet<>();
        List<OfflinePlayer> merged = new ArrayList<>();

        for (OfflinePlayer player : trusted) {
            if (seen.add(player.getUniqueId())) {
                merged.add(player);
            }
        }

        for (OfflinePlayer teammate : getTeammates(truster)) {
            if (seen.add(teammate.getUniqueId())) {
                merged.add(teammate);
            }
        }

        return merged;
    }

    public void logStatus() {
        if (isEnabled()) {
            Infuse.LOGGER.info("BetterTeams integration enabled — teammates are automatically trusted.");
        }
    }
}
