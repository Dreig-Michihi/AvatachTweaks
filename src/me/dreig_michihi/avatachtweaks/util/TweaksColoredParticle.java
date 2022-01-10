package me.dreig_michihi.avatachtweaks.util;

import com.projectkorra.projectkorra.util.ColoredParticle;
import com.projectkorra.projectkorra.util.ParticleEffect;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;

public class TweaksColoredParticle extends ColoredParticle {

    private final Particle.DustOptions dust;

    public TweaksColoredParticle(final Color color, final float size) {
        super(color, size);
        this.dust = new Particle.DustOptions(color, size);
    }
    //DREIG-start
    public void display(final Location loc, final int amount, final double offsetX, final double offsetY, final double offsetZ, final double extra) {
        ParticleEffect.REDSTONE.display(loc, amount, offsetX, offsetY, offsetZ, extra, this.dust);
    }
    //Dreig-end
}
