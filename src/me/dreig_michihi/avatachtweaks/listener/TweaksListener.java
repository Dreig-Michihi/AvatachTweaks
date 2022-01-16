package me.dreig_michihi.avatachtweaks.listener;

import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.Element;
import com.projectkorra.projectkorra.ProjectKorra;
import com.projectkorra.projectkorra.ability.AirAbility;
import com.projectkorra.projectkorra.ability.CoreAbility;
import com.projectkorra.projectkorra.ability.WaterAbility;
import com.projectkorra.projectkorra.event.AbilityStartEvent;
import com.projectkorra.projectkorra.waterbending.*;
import com.projectkorra.projectkorra.waterbending.combo.IceBullet;
import com.projectkorra.projectkorra.waterbending.ice.IceBlast;
import com.projectkorra.projectkorra.waterbending.ice.IceSpikeBlast;
import com.projectkorra.projectkorra.waterbending.multiabilities.WaterArms;
import me.dreig_michihi.avatachtweaks.bending.air.AirBlast;
import me.dreig_michihi.avatachtweaks.bending.air.AirSpout;
import me.dreig_michihi.avatachtweaks.bending.air.AirSuction;
import me.dreig_michihi.avatachtweaks.bending.water.combo.Rainbending;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.weather.ThunderChangeEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.util.Vector;
import timingslib.projectkorra.MCTiming;

import java.util.Objects;

public class TweaksListener implements Listener {
    public static boolean serverVersion16 = ProjectKorra.plugin.getServer().getVersion().contains("1.16");

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerSneak(final PlayerToggleSneakEvent event) {
        final Player player = event.getPlayer();
        final BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);
        if (bPlayer == null) {
            return;
        }
        //player.sendMessage("TweaksListener onPlayerSneak");
        final CoreAbility coreAbil = bPlayer.getBoundAbility();
        final String abil = bPlayer.getBoundAbilityName();
        if (coreAbil == null) {
            return;
        }//здесь было !player.isSneaking(), но я убрал это, чтобы при отпускании Sneak также создавалось облачко
        if (/*!player.isSneaking() &&*/ bPlayer.canBendIgnoreCooldowns(coreAbil)) {
            if (coreAbil instanceof AirAbility && bPlayer.isElementToggled(Element.AIR)) {
                if (bPlayer.canCurrentlyBendWithWeapons()) {
                    if (abil.equalsIgnoreCase("AirBlast")) {
                        AirBlast.setOrigin(player);
                    } else if (abil.equalsIgnoreCase("AirSuction")) {
                        new AirSuction(player);
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerSwing(final PlayerInteractEvent event) {
        final Player player = event.getPlayer();
        final BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);
        if (bPlayer == null) {
            return;
        }
        //player.sendMessage("TweaksListener onPlayerSwing");
        String abil = bPlayer.getBoundAbilityName();
        final CoreAbility coreAbil = bPlayer.getBoundAbility();
        if (coreAbil == null) {
            return;
        }
        if (bPlayer.canBendIgnoreCooldowns(coreAbil)) {
            if (coreAbil instanceof AirAbility && bPlayer.isElementToggled(Element.AIR)) {
                if (bPlayer.canCurrentlyBendWithWeapons()) {
                    if (abil.equalsIgnoreCase("AirSpout")) {
                        new AirSpout(player);
                    } else if (abil.equalsIgnoreCase("AirBlast")) {
                        new AirBlast(player);
                    } else if (abil.equalsIgnoreCase("AirSuction")) {
                        if (player.isSneaking())
                            AirSuction.easyJump(player);
                        else
                            AirSuction.shoot(player);
                    }
                }
            }

            if (coreAbil instanceof WaterAbility && bPlayer.isElementToggled(Element.WATER)) {
                if (bPlayer.canCurrentlyBendWithWeapons()) {
                    if(CoreAbility.hasAbility(player, Rainbending.class)){
                        CoreAbility.getAbility(player, Rainbending.class).doLeftClick();
                    }
                }
            }
        }

    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerMove(final PlayerMoveEvent event) {
        final Player player = event.getPlayer();
        final BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);
        if (Objects.requireNonNull(event.getTo()).getX() == event.getFrom().getX() && event.getTo().getY() == event.getFrom().getY() && event.getTo().getZ() == event.getFrom().getZ()) {
            return;
        }
        try (MCTiming timing = ProjectKorra.timing("PlayerMoveSpoutCheck").startTiming()) {
            if (CoreAbility.hasAbility(player, AirSpout.class)) {
                Vector vel = new Vector();
                vel.setX(event.getTo().getX() - event.getFrom().getX());
                vel.setZ(event.getTo().getZ() - event.getFrom().getZ());

                final double currspeed = vel.length();
                final double maxspeed = .2;
                if (currspeed > maxspeed) {
                    // apply only if moving set a factor
                    vel = vel.normalize().multiply(maxspeed);
                    // apply the new velocity
                    event.getPlayer().setVelocity(vel);
                }
                //return;
            }
        }
    }

    /*@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onAbilityStart(final AbilityStartEvent event){
        if(CoreAbility.hasAbility(event.getAbility().getPlayer(), Rainbending.class))
        switch (event.getAbility().getName()){
            case ("OctopusForm"):
                event.getAbility().getPlayer().sendMessage("1");
                break;
            case("IceBlast"):
                event.getAbility().getPlayer().sendMessage("2");
                break;
            case("Surge"):
                event.getAbility().getPlayer().sendMessage("3");
                break;
            case("IceSpike"):
                event.getAbility().getPlayer().sendMessage("4");
                break;
            case ("WaterManipulation"):
                event.getAbility().getPlayer().sendMessage("5");
                break;
            case ("WaterSpout"):
                event.getAbility().getPlayer().sendMessage("6");
                break;
            case ("Torrent"):
                event.getAbility().getPlayer().sendMessage("7");
                break;
            case ("WaterArms"):
                event.getAbility().getPlayer().sendMessage("8");
                break;
        }
    }*/

    private static boolean loopFix = false;
    @EventHandler
    public void onWeatherChange(final WeatherChangeEvent event) {
        /*for (Player p : ProjectKorra.plugin.getServer().getOnlinePlayers()) {
            p.sendMessage("Сработал onWeatherChange");
        }*/
        if (!loopFix) {
            if (!event.toWeatherState()) {//if rain ended
                loopFix = true;
                event.getWorld().setStorm(false);
                loopFix = false;
               /* for (Player p : ProjectKorra.plugin.getServer().getOnlinePlayers()) {
                    p.sendMessage("Установлена ясная погода");
                    p.sendMessage("ДОЖДЬ КОНЧИЛСЯ!");
                }*/
            }
        }
        /*else {
            for (Player p : ProjectKorra.plugin.getServer().getOnlinePlayers()) {
                p.sendMessage("Сработал loopFix");
            }
        }*/
    }
    @EventHandler
    public void onThunderChange(final ThunderChangeEvent event) {
        /*for (Player p : ProjectKorra.plugin.getServer().getOnlinePlayers()) {
            p.sendMessage("Сработал onWeatherChange");
        }*/
        if (!loopFix) {
            if (!event.toThunderState()) {//if thunder ended
                loopFix = true;
                event.getWorld().setThundering(false);
                loopFix = false;
               /* for (Player p : ProjectKorra.plugin.getServer().getOnlinePlayers()) {
                    p.sendMessage("Установлена ясная погода");
                    p.sendMessage("ДОЖДЬ КОНЧИЛСЯ!");
                }*/
            }
        }
        /*else {
            for (Player p : ProjectKorra.plugin.getServer().getOnlinePlayers()) {
                p.sendMessage("Сработал loopFix");
            }
        }*/
    }

}
