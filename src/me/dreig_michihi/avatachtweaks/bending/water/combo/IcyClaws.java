package me.dreig_michihi.avatachtweaks.bending.water.combo;

import com.projectkorra.projectkorra.Element;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.*;
import com.projectkorra.projectkorra.ability.util.ComboManager;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.avatar.AvatarState;
import com.projectkorra.projectkorra.command.Commands;
import com.projectkorra.projectkorra.configuration.ConfigManager;
import com.projectkorra.projectkorra.firebending.util.FireDamageTimer;
import com.projectkorra.projectkorra.util.ClickType;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.ParticleEffect;
import me.dreig_michihi.avatachtweaks.TweaksConfig;
import me.dreig_michihi.avatachtweaks.TweaksGeneralMethods;
import me.dreig_michihi.avatachtweaks.TweaksInfo;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class IcyClaws extends IceAbility implements AddonAbility, ComboAbility {

    private ArrayList<Location> clawsLoc;
    private ArrayList<Vector> clawsDir;
    @Attribute("Damage")
    private double damage;
    @Attribute("Speed")
    private double speed;
    @Attribute("Range")
    private double range;
    @Attribute("Radius")
    private double radius;
    private double gravity;
    @Attribute("Cooldown")
    private long cooldown=getConfig().getLong(TweaksConfig.getConfigPath(this,"Cooldown"));

    public IcyClaws(Player player){
        super(player);
        this.remove();
    }

    public IcyClaws(Player player, ArrayList<Vector> directions) {
        super(player);
        //player.sendMessage("IcyClaws!");
        if(CoreAbility.hasAbility(player, IcyClaws.class)) {
            return;
        }
        if(this.player.isDead()||!this.player.isOnline()||this.player.getGameMode().equals(GameMode.SPECTATOR))
            return;
        if(this.bPlayer==null)
            return;
        if(this.bPlayer.isOnCooldown("IcyClaws"))
            return;
        if(!this.bPlayer.canBendIgnoreBindsCooldowns(this)){
            return;
        }
        this.clawsLoc = new ArrayList<>();
        this.clawsDir = directions;
        this.damage = getConfig().getDouble(TweaksConfig.getConfigPath(this,"Damage"));
        this.speed = getConfig().getDouble(TweaksConfig.getConfigPath(this,"Speed"));
        this.radius = getConfig().getDouble(TweaksConfig.getConfigPath(this,"CollisionRadius"));
        this.range = getConfig().getDouble(TweaksConfig.getConfigPath(this,"Range"));
        this.gravity = getConfig().getDouble(TweaksConfig.getConfigPath(this,"Gravity"));
        //this.range = 15;
        for(Vector direction:directions)
            clawsLoc.add(this.player.getEyeLocation()
                    .add(direction.clone().multiply(0.5))
                    /*.add(new Vector(Math.random()*0.05,Math.random()*0.01,Math.random()*0.05))*/);
        //player.sendMessage("IcyClaws!!!");
        bPlayer.addCooldown(this);
        this.start();
    }

    @Override
    public void progress() {
        //player.sendMessage("clawsLoc.size()" + clawsLoc.size());
        //update directions;
        //ArrayList<Vector>updateDir
        //for(int i = clawsLoc.size()-1;i>=0;i--){
        //}
        if (clawsLoc.size() > 0)
            for(int i =0;i<clawsLoc.size();i++) {
                //player.sendMessage("test1");
                if(System.currentTimeMillis()>this.getStartTime()+250)
                    clawsDir.get(i).setY(Math.max(-1, clawsDir.get(i).getY() - this.gravity)).normalize();//абоба
                Vector direction = clawsDir.get(i);
                //player.sendMessage("test3");
                for (double j = 0.8; j >= 0.4; j -= 0.05) {
                    TweaksGeneralMethods.displayColoredParticle("D3F6FF",
                            clawsLoc.get(i)//начало когтя
                                    .add(direction.clone().normalize().multiply(0.8 - j))//коготь утончающийся
                            , 1, 0, 0, 0, 0, (float) (j - 0.2));
                }
                //if (System.currentTimeMillis() > this.getStartTime() + i * interval) {
                    //player.sendMessage("test2");
                    boolean collision = false;
                    for (double j = 0; j < this.speed && !collision; j += radius) {
                        clawsLoc.get(i).add(clawsDir.get(i).clone().multiply(radius));
                        Entity entity = GeneralMethods.getClosestEntity(clawsLoc.get(i), this.radius);
                        if (entity != null)
                            if (!(entity.getUniqueId() != this.player.getUniqueId()
                                    && !GeneralMethods.isRegionProtectedFromBuild(this, entity.getLocation())
                                    && !((entity instanceof Player)
                                    && Commands.invincible.contains(((Player) entity).getName()))))
                                entity = null;
                        if (clawsLoc.get(i).distanceSquared(this.player.getEyeLocation()) > this.range * this.range
                                || GeneralMethods.isSolid(clawsLoc.get(i).getBlock())
                                || entity != null) {
                            if (entity != null)
                                if (entity instanceof LivingEntity) {
                                    entity.setFireTicks(0);
                                    DamageHandler.damageEntity(entity, this.damage, this);
                                    ((LivingEntity) entity).setNoDamageTicks(0);
                                    AirAbility.breakBreathbendingHold(entity);
                                }
                            /*this.player.sendMessage("this.range: "+this.range);
                            this.player.sendMessage("this.range^2: "+this.range*this.range);
                            this.player.sendMessage("distanceSquared(): "+clawsLoc.get(i).distanceSquared(this.player.getEyeLocation()));
                            this.player.sendMessage("isSolid: "+GeneralMethods.isSolid(clawsLoc.get(i).getBlock()));
                            this.player.sendMessage("entity: "+entity);*/
                            ParticleEffect.WHITE_ASH.display(clawsLoc.get(i), 5, 0.5, 0.5, 0.5, 0.1);
                            ParticleEffect.BLOCK_DUST.display(clawsLoc.get(i), 5, 0.5, 0.5, 0.5, Material.FROSTED_ICE.createBlockData());

                            Objects.requireNonNull(clawsLoc.get(i).getWorld()).playSound(clawsLoc.get(i), Sound.BLOCK_GLASS_BREAK, 1, 1.2F);
                            clawsLoc.remove(i);
                            clawsDir.remove(i);
                            //removedClaws.add(i);
                            collision = true;
                        }
                    }
                    if (collision) {
                        i--;
                    }
                //}
            }
        else{
            this.remove();
        }
        //clawsLogic();
    }

    @Override
    public boolean isSneakAbility() {
        return false;
    }

    @Override
    public boolean isHarmlessAbility() {
        return false;
    }

    @Override
    public long getCooldown() {
        return this.cooldown;
    }

    @Override
    public String getName() {
        return "IcyClaws";
    }

    @Override
    public Location getLocation() {
        return null;
    }

    @Override
    public List<Location> getLocations() {
        return this.clawsLoc;
    }

    @Override
    public String getInstructions() {
        return getLanguageConfig().contains(TweaksConfig.getConfigPath(this,"Instructions"))?
                getLanguageConfig().getString(TweaksConfig.getConfigPath(this, "Instructions"))
                :super.getInstructions();
    }

    @Override
    public String getDescription() {
        return getLanguageConfig().contains(TweaksConfig.getConfigPath(this, "Description"))?
                getLanguageConfig().getString(TweaksConfig.getConfigPath(this, "Description"))
                :super.getDescription();
    }

    @Override
    public boolean isEnabled() {
        if (ConfigManager.defaultConfig.get().contains(TweaksConfig.getConfigPath()+".Rainbending.PhaseChange.Enabled"))
            return ConfigManager.defaultConfig.get().getBoolean(TweaksConfig.getConfigPath()+".Rainbending.PhaseChange.Enabled");
        else
            return true;
    }

    @Override
    public void load() {
        TweaksConfig.addDefault(this,"Damage",1);
        TweaksConfig.addDefault(this,"Speed", 1.2);
        TweaksConfig.addDefault(this,"CollisionRadius",0.5);
        TweaksConfig.addDefault(this,"Range", 40);
        TweaksConfig.addDefault(this,"Gravity", 0.03);
        TweaksConfig.addDefault(this, "Cooldown",5000);
        TweaksConfig.addLanguage(this,"Description",
                "Collect water from the air to create sharp icy claws that you can throw at enemies as Hama did!");
        TweaksConfig.addLanguage(this,"Instructions",
                "Return to \"PhaseChange\" while casting Rainbending combo and activate it.");
        TweaksConfig.saveDefaultConfig();
        TweaksConfig.saveLanguageConfig();
        TweaksInfo.loadLog(this);
    }

    @Override
    public void stop() {
        this.remove();
        TweaksInfo.stopLog(this);
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
    public Object createNewComboInstance(Player player) {
        return new IcyClaws(player);
    }

    @Override
    public ArrayList<ComboManager.AbilityInformation> getCombination() {
        final ArrayList<ComboManager.AbilityInformation> icyClaws = new ArrayList<>();
        icyClaws.add(new ComboManager.AbilityInformation("PhaseChange", ClickType.SHIFT_UP));
        icyClaws.add(new ComboManager.AbilityInformation("PhaseChange", ClickType.SHIFT_DOWN));
        icyClaws.add(new ComboManager.AbilityInformation("PhaseChange", ClickType.LEFT_CLICK));
        return icyClaws;
    }
}
