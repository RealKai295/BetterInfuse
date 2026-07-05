package com.catadmirer.infuseSMP.commands;

import com.catadmirer.infuseSMP.Message;
import com.catadmirer.infuseSMP.Message.MessageType;
import com.catadmirer.infuseSMP.managers.DataManager;
import java.util.Comparator;
import java.util.List;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TrustListCommand implements CommandExecutor {
    private final DataManager dataManager;

    public TrustListCommand(DataManager dataManager) {
        this.dataManager = dataManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(new Message(MessageType.TRUST_CONSOLE_USAGE).toComponent());
            return true;
        }

        if (args.length != 0) {
            Message msg = new Message(MessageType.TRUSTLIST_INCORRECT_USAGE);
            msg.applyPlaceholder("label", label);
            player.sendMessage(msg.toComponent());
            return true;
        }

        List<OfflinePlayer> trusted = dataManager.getTrusted(player).stream()
                .sorted(Comparator.comparing(trustedPlayer -> {
                    String name = trustedPlayer.getName();
                    return name != null ? name : "";
                }, String.CASE_INSENSITIVE_ORDER))
                .toList();

        if (trusted.isEmpty()) {
            player.sendMessage(new Message(MessageType.TRUSTLIST_EMPTY).toComponent());
            return true;
        }

        player.sendMessage(new Message(MessageType.TRUSTLIST_HEADER).toComponent());

        for (OfflinePlayer trustedPlayer : trusted) {
            String name = trustedPlayer.getName();
            if (name == null) name = "Unknown";

            MessageType type = dataManager.areTeammates(player, trustedPlayer)
                    ? MessageType.TRUSTLIST_ENTRY_TEAMMATE
                    : MessageType.TRUSTLIST_ENTRY;

            Message msg = new Message(type);
            msg.applyPlaceholder("player", name);
            player.sendMessage(msg.toComponent());
        }

        return true;
    }
}
