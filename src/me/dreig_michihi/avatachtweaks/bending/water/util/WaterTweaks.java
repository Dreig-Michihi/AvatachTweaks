package me.dreig_michihi.avatachtweaks.bending.water.util;

import com.projectkorra.projectkorra.util.ParticleEffect;
import me.dreig_michihi.avatachtweaks.TweaksGeneralMethods;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class WaterTweaks {
    public static void playFocusWaterEffect(final Block block) {
        ParticleEffect.WATER_WAKE.display(block.getLocation().clone().add(0.5, 0.5, 0.5), 3, 0.1, 0.2, 0.1, 0.075);
        TweaksGeneralMethods.displayColoredParticle("0094FF",block.getLocation().clone().add(0.5, 0.5, 0.5),1,0.2,0.3,0.2,0.01,0.25F+(float)(Math.random()));
    }
    public static void playFocusWaterEffect(final Block block, Player player) {
        Vector closeToPlayer = block.getLocation().clone().add(0.5,0.5,0.5).subtract(player.getEyeLocation()).toVector().normalize().multiply(-0.707);
        ParticleEffect.WATER_WAKE.display(block.getLocation().clone().add(0.5, 0.5, 0.5).add(closeToPlayer), 3, 0.1, 0.2, 0.1, 0.075);
        TweaksGeneralMethods.displayColoredParticle("0094FF",block.getLocation().clone().add(0.5, 0.5, 0.5).add(closeToPlayer),1,0.15,0.3,0.15,0.01,0.25F+(float)(Math.random()));
    }
    public static void playFocusIceEffect(final Block block) {
        ParticleEffect.WATER_WAKE.display(block.getLocation().clone().add(0.5, 0.5, 0.5), 3, 0.1, 0.2, 0.1, 0.075);
        TweaksGeneralMethods.displayColoredParticle("D3F6FF",block.getLocation().clone().add(0.5, 0.5, 0.5),1,0.1,0.5,0.1,0.01,0.35F+(float)(Math.random()));
    }
    public static void playFocusIceEffect(final Block block, Player player) {
        Vector closeToPlayer = block.getLocation().clone().add(0.5,0.5,0.5).subtract(player.getEyeLocation()).toVector().normalize().multiply(-0.707);
        ParticleEffect.WATER_WAKE.display(block.getLocation().clone().add(0.5, 0.5, 0.5).add(closeToPlayer), 3, 0.1, 0.2, 0.1, 0.075);
        TweaksGeneralMethods.displayColoredParticle("D3F6FF",block.getLocation().clone().add(0.5, 0.5, 0.5).add(closeToPlayer),1,0.1,0.5,0.1,0.01,0.35F+(float)(Math.random()));
    }
}
