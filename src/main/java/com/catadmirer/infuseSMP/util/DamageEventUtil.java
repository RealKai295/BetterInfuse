package com.catadmirer.infuseSMP.util;

import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public final class DamageEventUtil {
    private DamageEventUtil() {}

    public static Player getPlayerAttacker(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        if (damager instanceof Player player) {
            return player;
        }

        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player shooter) {
            return shooter;
        }

        Entity causingEntity = event.getDamageSource().getCausingEntity();
        if (causingEntity instanceof Player player) {
            return player;
        }

        return null;
    }

    public static boolean isDamageType(EntityDamageByEntityEvent event, DamageType type) {
        DamageType damageType = event.getDamageSource().getDamageType();
        return damageType != null && damageType.equals(type);
    }
}
