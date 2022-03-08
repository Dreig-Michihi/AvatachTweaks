package me.dreig_michihi.avatachtweaks.bending.earth;

import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ProjectKorra;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.ComboAbility;
import com.projectkorra.projectkorra.ability.LavaAbility;
import com.projectkorra.projectkorra.ability.util.ComboManager;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.configuration.ConfigManager;
import com.projectkorra.projectkorra.util.ClickType;
import com.projectkorra.projectkorra.util.ParticleEffect;
import me.dreig_michihi.avatachtweaks.TweaksConfig;
import me.dreig_michihi.avatachtweaks.TweaksInfo;
import me.dreig_michihi.avatachtweaks.listener.ListenerManager;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Objects;

public class ShakingOffLava extends LavaAbility implements ComboAbility, AddonAbility {
    //Config
    @Attribute(Attribute.RADIUS)
    private static double radius = 6;
    @Attribute(Attribute.COOLDOWN)
    private static long cooldown = 6000;
    @Attribute(Attribute.DURATION)
    private static long resistTime = 2000;

    private static long start;
    private static int animationTicks;

    public ShakingOffLava(Player player) {
        super(player);

        if (bPlayer.isOnCooldown(this)) {
            return;
        }
        if (player.getFireTicks() <= 0)
            return;
        start = System.currentTimeMillis();
        for (Entity entity : GeneralMethods.getEntitiesAroundPoint(player.getLocation().add(0, 0.5, 0), radius)) {
            if ((entity instanceof LivingEntity) && entity.getEntityId() != player.getEntityId()) {
                ParticleEffect.LAVA.display(((LivingEntity) entity).getEyeLocation(), 5, 1, 1, 1, 0.2);
                entity.setFireTicks(player.getFireTicks());
            }
        }
        ParticleEffect.DRIP_LAVA.display(player.getLocation(), Math.min((int) radius * 20, 100), radius*animationTicks/10, radius*animationTicks/10, radius*animationTicks/10, 0.5);
        player.setFireTicks(0);
        if (player.hasPotionEffect(PotionEffectType.FIRE_RESISTANCE)) {
            if (Objects.requireNonNull(player.getPotionEffect(PotionEffectType.FIRE_RESISTANCE)).getDuration() < resistTime)
                player.addPotionEffect(PotionEffectType.FIRE_RESISTANCE.createEffect((int) (resistTime * 0.02), 1));
        } else
            player.addPotionEffect(PotionEffectType.FIRE_RESISTANCE.createEffect((int) (resistTime * 0.02), 1));
        animationTicks=0;
        Objects.requireNonNull(player.getLocation().getWorld()).playSound(player.getLocation(), Sound.ITEM_BUCKET_EMPTY_LAVA,1,1);
        start();
        bPlayer.addCooldown(this);
    }

    @Override
    public void progress() {
        if (this.player.isDead() || !this.player.isOnline()) {
            super.remove();
            return;
        } else if (GeneralMethods.isRegionProtectedFromBuild(this, this.player.getLocation())) {
            super.remove();
            return;
        }
        if (System.currentTimeMillis() > start + resistTime) {
            super.remove();
            return;
        }
        if (animationTicks < 10) {
            ParticleEffect.LAVA.display(player.getLocation(), 5, radius*animationTicks/10, radius*animationTicks/10, radius*animationTicks/10, 0);
            ParticleEffect.FLAME.display(player.getLocation().add(0,1,0), 10, 0.3,1,0.3, 0.5);
            ParticleEffect.DRIP_LAVA.display(player.getLocation(), Math.min((int) radius * 20, 50), radius*animationTicks/10, radius*animationTicks/10, radius*animationTicks/10, 0.5);
            player.addPotionEffect(PotionEffectType.FIRE_RESISTANCE.createEffect(1, 1));
            Objects.requireNonNull(player.getLocation().getWorld()).playSound(player.getLocation(), Sound.BLOCK_LAVA_EXTINGUISH,(float)((10 - animationTicks) /10),(float)((animationTicks) /10));
            animationTicks++;
        }
        ParticleEffect.DRIP_LAVA.display(player.getLocation().add(0,1,0), 2, 0.3,0.6,0.3);
        player.setFireTicks(0);
    }

    @Override
    public boolean isSneakAbility() {
        return true;
    }

    @Override
    public boolean isHarmlessAbility() {
        return false;
    }

    @Override
    public long getCooldown() {
        return cooldown;
    }

    @Override
    public String getName() {
        return "ShakingOffLava";
    }

    @Override
    public Location getLocation() {
        return this.player.getLocation();
    }

    @Override
    public Object createNewComboInstance(Player player) {
        if(player.getFireTicks()>0)
            return new ShakingOffLava(player);
        else
            return null;
    }

    @Override
    public void load() {
        TweaksConfig.addDefault(this, "Enabled", true);
        TweaksConfig.addDefault(this, "Radius", 6);
        TweaksConfig.addDefault(this, "Cooldown", 6000);
        TweaksConfig.addDefault(this, "FireResistTime", 2000);
        TweaksConfig.addDefault(this, "SneakComboAbility", "LavaFlow");
        TweaksConfig.addDefault( this, "LavaDamageModifier",1);
        TweaksConfig.addDefault( this, "LavaTicksModifier",1);
        TweaksConfig.saveDefaultConfig();
        radius = getConfig().getDouble(TweaksConfig.getConfigPath(this, "Radius"));
        cooldown = getConfig().getLong(TweaksConfig.getConfigPath(this, "Cooldown"));
        resistTime = getConfig().getLong(TweaksConfig.getConfigPath(this, "FireResistTime"));
        ListenerManager.Register();
        ProjectKorra.log.info(this.getName() + " by " + this.getAuthor() + " " + this.getVersion() + " has been loaded!");
    }

    @Override
    public void stop() {
        ProjectKorra.log.info("Successfully disabled " + getName() + " by " + getAuthor());
        ListenerManager.Unregister();
        super.remove();
    }

    @Override
    public String getAuthor() {
        return TweaksInfo.getAuthor();
    }

    @Override
    public String getVersion() {
        return TweaksInfo.getVersion();
    }

    @Override
    public ArrayList<ComboManager.AbilityInformation> getCombination() {

        ArrayList<ComboManager.AbilityInformation> combo = new ArrayList<>();
        combo.add(new ComboManager.AbilityInformation(getConfig().getString(TweaksConfig.getConfigPath(this, "SneakComboAbility")), ClickType.SHIFT_DOWN));
        combo.add(new ComboManager.AbilityInformation(getConfig().getString(TweaksConfig.getConfigPath(this, "SneakComboAbility")), ClickType.SHIFT_UP));
        return combo;
    }

    @Override
    public String getDescription() {
        return "Shake off the flames with a protective layer of earth on your body, turning it into lava and spewing out to the sides around you, transferring the fire ticks to others!";
    }

    @Override
    public String getInstructions() {
        return getConfig().getString(TweaksConfig.getConfigPath(this, "SneakComboAbility"))+" (Tap Shift)";
    }
}
