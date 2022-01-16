package me.dreig_michihi.avatachtweaks.bending.water.combo;

import com.projectkorra.projectkorra.Element;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ProjectKorra;
import com.projectkorra.projectkorra.ability.*;
import com.projectkorra.projectkorra.ability.util.ComboManager;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.util.ClickType;
import com.projectkorra.projectkorra.util.ParticleEffect;
import com.projectkorra.projectkorra.waterbending.*;
import com.projectkorra.projectkorra.waterbending.healing.HealingWaters;
import com.projectkorra.projectkorra.waterbending.ice.IceBlast;
import com.projectkorra.projectkorra.waterbending.ice.IceSpikeBlast;
import com.projectkorra.projectkorra.waterbending.multiabilities.WaterArms;
import commonslang3.projectkorra.lang3.ArrayUtils;
import me.dreig_michihi.avatachtweaks.TweaksConfig;
import me.dreig_michihi.avatachtweaks.TweaksGeneralMethods;
import me.dreig_michihi.avatachtweaks.TweaksInfo;
import me.dreig_michihi.avatachtweaks.listener.ListenerManager;
import me.dreig_michihi.avatachtweaks.listener.TweaksListener;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class Rainbending extends WaterAbility implements AddonAbility, ComboAbility {

    private BossBar chargeBar;
    @Attribute("Cooldown")
    private long cooldown = getConfig().getLong(TweaksConfig.getConfigPath(this,"Cooldown"));
    private double chargeAmount;
    private double chargeCount;
    private static double coverageEachBlockContribution;
    private static double drainingBlockContribution;
    @Attribute("ArcMax")
    private double arcMax = 16;
    @Attribute("ArcMin")
    private double arcMin = 4;
    private double arcCurr = arcMax;
    @Attribute("ArcStep")
    private double arcStep = 0.5;
    private Location origin;
    private Location previousOrigin;
    private Vector direction;
    private String chosenAbility =" ";
    private boolean started = false;
    private boolean correctAbility = false;
    private Queue<Block> dryedBlocks = new ArrayDeque<>();

    private static double coverageWaterSourceChance;
    private static double coverageIceSnowSourceChance;
    private static double coveragePlantSourceChance;
    private static double coverageDrainChanceRaining;
    private static double coverageDrainChanceThundering;
    private static int drainedBlocksCapacity;
    private static double coverageRadius;
    private static int waterParticles;
    private static double maxChargeForParticles;
    private static boolean drainingOnOriginMoveParticles;
    private Permission perm;

    public Rainbending(Player player) {
        super(player);
        if(CoreAbility.hasAbility(player, Rainbending.class)) {
            return;
        }
        if(this.player.isDead()||!this.player.isOnline()||this.player.getGameMode().equals(GameMode.SPECTATOR))
            return;
        if(this.bPlayer==null)
            return;
        if(this.bPlayer.isOnCooldown("Rainbending"))
            return;
        if(!this.bPlayer.canBendIgnoreBindsCooldowns(this)){
            return;
        }
        start();
    }

    private void updateChargeReserve(String ability){
        this.chargeAmount = getConfig().getDouble(TweaksConfig.getConfigPath(this,ability+".ChargeAmount"));
        updateChargeBarProgress();
    }

    private void updateChargeBarProgress(){
        this.chargeBar.setProgress(Math.max(0, Math.min(chargeCount/chargeAmount, 1)));
    }

    private void displayWater(final Location location) {
        ParticleEffect.WATER_WAKE.display(location, (int) (waterParticles*(Math.min(1,this.chargeCount/maxChargeForParticles))),
                0.05, 0.1, 0.05, -0.01);
        //TweaksGeneralMethods.displayColoredParticle("0094FF",location,1,0.2,0.3,0.2,0.01, size);
    }

    public static ArrayList<Vector> getClawDirections(Player player, double arc){
        ArrayList<Vector> directions = new ArrayList<>();
        //player.sendMessage("arc: "+arc);
        //player.sendMessage("cameraMovement: "+cameraMovement);
        for (double i = -arc; i <= arc; i += arc/2) {
            final double angle = Math.toRadians(i);
            final Vector direction = player.getEyeLocation().getDirection().clone();
            double x, z, vx, vz;
            x = direction.getX();
            z = direction.getZ();

            vx = x * Math.cos(angle) - z * Math.sin(angle);
            vz = x * Math.sin(angle) + z * Math.cos(angle);

            direction.setX(vx);
            direction.setZ(vz);
            directions.add(direction);
            //for (double j = 0.5; j >= 0.35; j -= 0.03) {
            //    TweaksGeneralMethods.displayColoredParticle("CCFFFF", this.origin.clone().add(direction.clone().multiply(0.25)).add(direction.clone().normalize().multiply(0.5 - j)), 1, 0, 0, 0, 0, (float) (j - 0.2));
            //}
        }
        return directions;
    }

    private void drawClaws() {
        for(Vector direction: getClawDirections(player, arcCurr)){
            for (double j=0.5;j>=0.35;j-=0.03) {
                TweaksGeneralMethods.displayColoredParticle(( bPlayer.isOnCooldown("IcyClaws")&&!hasAbility(player,IcyClaws.class))?"0094FF":"D3F6FF",
                        this.player.getEyeLocation()
                                .add(direction.clone().multiply(2))//начало когтя
                                .add(direction.clone().normalize().multiply(0.5 - j))//коготь утончающийся
                        , 1, 0, 0, 0, 0, (float) (j - 0.2));
            }
        }
        if ((bPlayer.isOnCooldown("IcyClaws") && !hasAbility(player, IcyClaws.class))) {
            if(Math.random()<0.20)
            WaterAbility.playWaterbendingSound(this.origin);
        } else {
            if(Math.random()<0.20)
            IceAbility.playIcebendingSound(this.origin);
        }
    }

    public void doLeftClick() {
        //player.sendMessage("doLeftClick");
        if (started)
            if (chargeBar.getProgress() == 1 && correctAbility) {
                //player.sendMessage("Left Click with " + bPlayer.getBoundAbilityName());
                if(chosenAbility.equals("PhaseChange")){
                    //player.sendMessage("IcyClaws?");
                    if(!bPlayer.isOnCooldown("IcyClaws")) {
                        new IcyClaws(player, getClawDirections(player, arcCurr));
                        this.remove();
                    }
                    return;
                }
                if(bPlayer.isOnCooldown(chosenAbility))
                    return;
                Material oldData = this.origin.getBlock().getType();
                switch (bPlayer.getBoundAbilityName()){
                    case ("OctopusForm"): {
                        final Block reverseBlock = this.origin.getBlock();
                        reverseBlock.setType(Material.WATER);
                        this.player.setSneaking(false);
                        new OctopusForm(player);//left click
                        this.player.setSneaking(true);
                        OctopusForm.form(player);//shift
                        reverseBlock.setType(oldData);
                        break;
                    }
                    case("IceBlast"): {
                        //проверить, как выделяются source'ы возможно там static массив
                        if (CoreAbility.hasAbility(player, IceBlast.class))
                            for (CoreAbility coreAbility : CoreAbility.getAbilities(player, IceBlast.class))
                                coreAbility.remove();
                        final Block reverseBlock = this.origin.getBlock();
                        reverseBlock.setType(Material.ICE);
                        IceBlast iceBlast = new IceBlast(player);
                        iceBlast.setSourceBlock(reverseBlock);
                        iceBlast.setLocation(reverseBlock.getLocation());
                        iceBlast.setPrepared(true);
                        iceBlast.start();
                        reverseBlock.setType(oldData);
                        IceBlast.activate(player);
                       /* new BukkitRunnable(){
                            @Override
                            public void run() {
                                new IceBlast((player));
                            }
                        }.runTaskLater(ProjectKorra.plugin,5);
                        new BukkitRunnable(){
                            @Override
                            public void run() {
                                reverseBlock.setType(oldData);
                            }
                        }.runTaskLater(ProjectKorra.plugin,10);
                        new BukkitRunnable(){
                            @Override
                            public void run() {
                                IceBlast.activate(player);
                            }
                        }.runTaskLater(ProjectKorra.plugin,20);*/
                        break;
                    }
                    case("Surge"): {
                        final Block reverseBlock = this.origin.getBlock();
                        reverseBlock.setType(Material.WATER);
                        if(CoreAbility.hasAbility(player, SurgeWall.class))
                            for(CoreAbility coreAbility: CoreAbility.getAbilities(player, SurgeWall.class))
                                coreAbility.remove();
                        if(CoreAbility.hasAbility(player, SurgeWave.class))
                            for(CoreAbility coreAbility: CoreAbility.getAbilities(player, SurgeWave.class))
                                coreAbility.remove();
                        player.setSneaking(false);
                        new SurgeWall((player));
                        player.setSneaking(true);
                        reverseBlock.setType(oldData);
                        SurgeWall.form(player);
                        break;
                    }
                    case("IceSpike"): {
                        final Block reverseBlock = this.origin.getBlock();
                        reverseBlock.setType(Material.WATER);
                        IceSpikeBlast iceSpikeBlast = new IceSpikeBlast(player);
                        iceSpikeBlast.setFirstDestination(this.origin);
                        IceSpikeBlast.activate(player);
                        reverseBlock.setType(oldData);
                        break;
                    }
                    case ("WaterManipulation"): {
                        final Block reverseBlock = this.origin.getBlock();
                        reverseBlock.setType(Material.WATER);
                        WaterManipulation wm = new WaterManipulation(player, this.origin.getBlock());
                        wm.moveWater();
                        reverseBlock.setType(oldData);
                        break;
                    }
                    case ("Torrent"): {
                        final Block reverseBlock = this.origin.getBlock();
                        reverseBlock.setType(Material.WATER);
                        if(CoreAbility.hasAbility(player, Torrent.class))
                            for(CoreAbility coreAbility: CoreAbility.getAbilities(player, Torrent.class))
                                coreAbility.remove();
                        new Torrent((player));
                        reverseBlock.setType(oldData);
                        /*new BukkitRunnable(){
                            @Override
                            public void run() {
                                if(CoreAbility.hasAbility(player, Torrent.class))
                                    for(CoreAbility coreAbility: CoreAbility.getAbilities(player, Torrent.class))
                                        coreAbility.remove();
                                new Torrent((player));
                                reverseBlock.setType(oldData);
                            }
                        }.runTaskLater(ProjectKorra.plugin,1);*/
                        break;
                    }
                    case ("WaterSpout"): {
                        final Block reverseBlock = this.origin.getBlock();
                        reverseBlock.setType(Material.WATER);
                        WaterSpoutWave waterSpout = new WaterSpoutWave(player, WaterSpoutWave.AbilityType.CLICK);
                        waterSpout.setOrigin(this.origin);
                        reverseBlock.setType(oldData);
                        break;
                    }
                    case ("WaterArms"): {
                        final Block reverseBlock = this.origin.getBlock();
                        reverseBlock.setType(Material.WATER);
                        new WaterArms(player);
                        reverseBlock.setType(oldData);
                        break;
                    }
                    case ("HealingWaters"): {
                        final Block reverseBlock = this.player.getLocation().getBlock();
                        reverseBlock.setType(Material.WATER);
                        HealingWaters healingWaters = new HealingWaters(player);
                        healingWaters.setAttribute("ChargeTime", 0);
                        new BukkitRunnable(){
                            @Override
                            public void run() {
                                reverseBlock.setType(oldData);
                            }
                        }.runTaskLater(ProjectKorra.plugin, 1);
                    }
                }
                this.remove();
            }
    }

    private void releaseSneak() {
        //player.sendMessage("releaseSneak");
        if (started)
            if (chargeBar.getProgress() == 1 && correctAbility) {
                //player.sendMessage("Sneak released with " + bPlayer.getBoundAbilityName());
                if(chosenAbility.equals("PhaseChange")){
                    //player.sendMessage("IcyClaws?");
                    new IcyClaws(player, getClawDirections(player, arcCurr));
                    this.remove();
                    return;
                }
                Material oldData = this.origin.getBlock().getType();
                switch (bPlayer.getBoundAbilityName()){
                    case ("WaterArms"): {
                        final Block reverseBlock = this.origin.getBlock();
                        reverseBlock.setType(Material.WATER);
                        new WaterArms(player);
                        reverseBlock.setType(oldData);
                        break;
                    }
                    case ("Surge"): {
                        this.origin.add(this.direction);
                        final Block reverseBlock = this.origin.getBlock();
                        reverseBlock.setType(Material.WATER);
                        if (CoreAbility.hasAbility(player, SurgeWall.class))
                            for (CoreAbility coreAbility : CoreAbility.getAbilities(player, SurgeWall.class))
                                coreAbility.remove();
                        if (CoreAbility.hasAbility(player, SurgeWave.class))
                            for (CoreAbility coreAbility : CoreAbility.getAbilities(player, SurgeWave.class))
                                coreAbility.remove();
                        player.setSneaking(true);
                        SurgeWall.form(player);
                        reverseBlock.setType(oldData);
                        new SurgeWall(player);
                        player.setSneaking(false);
                    }
                }
            }
        this.remove();
    }

    @Override
    public void remove() {
        if (started) {
            this.chargeBar.removeAll();
            bPlayer.addCooldown(this);
        }
        super.remove();
    }

    private boolean isAbilityEnabled(String name){
        return getConfig().getBoolean(TweaksConfig.getConfigPath(this,name+".Enabled"));
    }

    private void update() {
        this.direction = this.player.getEyeLocation().getDirection();
        this.origin = this.player.getEyeLocation().add(direction.clone().multiply(2));
        if (started) {
            if (previousOrigin != null && chosenAbility.equals("PhaseChange")) {
            /*this.cameraMovement = Math.min(1,this.origin.clone().subtract(this.previousOrigin).length());
            if (this.player.getEyeLocation().distanceSquared(this.origin)
            <this.player.getEyeLocation().distanceSquared(this.previousOrigin)) {
                this.cameraMovement *= -1;
            }*/
                if (this.origin.clone().subtract(this.previousOrigin).length() > 0.06
                        && this.player.getEyeLocation().distanceSquared(this.origin)
                        > this.player.getEyeLocation().distanceSquared(this.previousOrigin)) {
                    if (arcCurr < arcMax)
                        arcCurr += arcStep;
                } else {
                    if (arcCurr > arcMin)
                        arcCurr -= arcStep;
                }
            } else {
                arcCurr = arcMax;
            }
        }
        this.previousOrigin = this.origin;
    }

    @Override
    public void progress() {
        if (player.isSneaking()) {
            if (!started) {
                if (bPlayer.getBoundAbilityName().equals("PhaseChange")) {
                    return;
                } else {
                    started = true;
                    this.chargeCount = 0;
                    this.chargeBar = Bukkit.getServer().createBossBar("", BarColor.WHITE, BarStyle.SOLID);
                    this.chargeBar.setProgress(0);
                    this.chargeBar.addPlayer(player);
                    //updateChargeReserve(bPlayer.getBoundAbilityName());
                    //updateChargeBarProgress();
                }
            }
        } else {
            //if (started) {
            //действия для Surge и WaterArms здесь! Если chargeBar фулл
            releaseSneak();
            return;
            //}
        }

        this.update();
        if (GeneralMethods.isSolid(this.origin.getBlock())) {
            this.origin.add(this.direction.clone().multiply(-1.5));
            if (GeneralMethods.isSolid(this.origin.getBlock())) {
                ParticleEffect.WATER_SPLASH.display(this.origin, 15, 0.4, 0.4, 0.4);
                this.remove();
                return;
            }
        }

        if (chargeCount > 0) {
            if (chargeBar.getProgress() < 1 || !chosenAbility.equals("PhaseChange")) {
                displayWater(this.origin);
                arcCurr = arcMax;
                if (Math.random() < 0.25)
                    WaterAbility.playWaterbendingSound(this.origin);
            } else {
                    drawClaws();
            }
        }


        if (!chosenAbility.equals(bPlayer.getBoundAbilityName())) {
            //player.sendMessage("Ability changed!");
            chosenAbility = bPlayer.getBoundAbilityName();
            switch (chosenAbility) {
                case ("OctopusForm"):
                case ("IceBlast"):
                case ("Surge"):
                case ("IceSpike"):
                case ("WaterManipulation"):
                case ("WaterSpout"):
                case ("Torrent"):
                case ("WaterArms"):
                case ("HealingWaters"):
                case ("PhaseChange"):
                    if (isAbilityEnabled(chosenAbility)) {
                        if (chosenAbility.equals("PhaseChange")&&
                        Math.random()<0.25)
                            IceAbility.playIcebendingSound(this.origin);
                        correctAbility = true;
                        this.chargeBar.setColor(BarColor.BLUE);
                        break;
                    }
                default:
                    correctAbility = false;
                    this.chargeBar.setColor(BarColor.WHITE);
                    this.chargeBar.setTitle("Choose another ability");
                    //player.sendMessage("Choose another ability return;");
                    return;
            }
            updateChargeReserve(chosenAbility);
        }
        if (!correctAbility)
            return;

        if (chargeBar.getProgress() < 1) {
            if(drainingOnOriginMoveParticles)
                for (Block b : dryedBlocks) {
                    if (!b.equals(this.origin.getBlock()) && ThreadLocalRandom.current().nextBoolean()) {
                        Location raindropLoc = b.getLocation().add(.5, .5, .5)
                                .add(ThreadLocalRandom.current().nextDouble(-0.5, 0.5),
                                        ThreadLocalRandom.current().nextDouble(-0.5, 0.5),
                                        ThreadLocalRandom.current().nextDouble(-0.5, 0.5));
                        if (Objects.equals(raindropLoc.getWorld(), origin.getWorld())) {
                            Vector toOrigin = origin.clone().subtract(raindropLoc).toVector().normalize();
                            ParticleEffect.WATER_WAKE.display(raindropLoc, 0,
                                    toOrigin.getX(), toOrigin.getY(), toOrigin.getZ(), 0.05);
                        }
                    }
                }

            if (!dryedBlocks.contains(this.origin.getBlock()) && this.origin.getBlock().getTemperature() < 0.95) {
                this.chargeCount += drainingBlockContribution;
                if (chargeCount > chargeAmount)
                    chargeCount = chargeAmount;
                dryedBlocks.offer(this.origin.getBlock());//добавление в конец очереди
                if (dryedBlocks.size() > drainedBlocksCapacity)
                    dryedBlocks.poll();//удаление из конца очереди
                else new BukkitRunnable() {
                    @Override
                    public void run() {
                        Block b = dryedBlocks.poll();//удаление из конца очереди
                        if (b != null)
                            ParticleEffect.DOLPHIN.display(b.getLocation().add(.5, .5, .5), 3, .5, .5, .5);
                    }
                }.runTaskLater(ProjectKorra.plugin, isRain(this.origin.getBlock()) ? 10 : 40);//0.5 сек либо 2 сек
            }
            for (Block b : GeneralMethods.getBlocksAroundPoint(origin, coverageRadius)) {
                if (isWater(b) || isIce(b) || isSnow(b) || isPlant(b) || isRain(b))
                    if ((isWater(b) && (Math.random() < coverageWaterSourceChance))
                            || ((isIce(b) || isSnow(b)) && (Math.random() < coverageIceSnowSourceChance))
                            || (isPlant(b) && (Math.random() < coveragePlantSourceChance))
                            || ((isRain(b) && (Math.random() < coverageDrainChanceRaining || (Math.random() < coverageDrainChanceThundering && b.getWorld().isThundering()))))) {
                        Location raindropLoc = b.getLocation().add(.5, .5, .5)
                                .add(ThreadLocalRandom.current().nextDouble(-0.5, 0.5),
                                        ThreadLocalRandom.current().nextDouble(-0.5, 0.5),
                                        ThreadLocalRandom.current().nextDouble(-0.5, 0.5));
                        Vector toOrigin = origin.clone().subtract(raindropLoc).toVector().normalize();
                        if (!TweaksListener.serverVersion16)
                            if (ElementalAbility.isAir(b.getType()) && b.getTemperature() < 0.15 && ThreadLocalRandom.current().nextBoolean())
                                player.getWorld().spawnParticle(Particle.SNOWFLAKE, raindropLoc, 0,
                                        toOrigin.getX(), toOrigin.getY(), toOrigin.getZ(), 0.1);
                        ParticleEffect.WATER_WAKE.display(raindropLoc, 0,
                                toOrigin.getX(), toOrigin.getY(), toOrigin.getZ(), 0.2);
                        this.chargeCount += coverageEachBlockContribution;
                        if (chargeCount > chargeAmount)
                            chargeCount = chargeAmount;
                    }
            }
            updateChargeBarProgress();
            this.chargeBar.setTitle(chosenAbility + ": " + Math.ceil(chargeBar.getProgress() * 100) + "%");
        } else {
            if (chargeCount > chargeAmount) {
                this.chargeCount -= coverageEachBlockContribution * 20;
                ParticleEffect.WATER_DROP.display(origin, 3, 0.1, 0.1, 0.1);
                updateChargeBarProgress();
            }
            this.chargeBar.setTitle(chosenAbility + ": " + (
                    bPlayer.isOnCooldown(chosenAbility)
                            || (chosenAbility.equals("PhaseChange")
                            && bPlayer.isOnCooldown("IcyClaws")) ?
                            "Wait Cooldown!"
                            : chosenAbility.equals("Surge")
                            || chosenAbility.equals("WaterArms")
                            || chosenAbility.equals("PhaseChange") ?
                            "Left Click or Release Sneak!"
                            : "Left Click!"));
        }
    }

    public static boolean isRain(Block block){
        World w = block.getWorld();
        return (!w.isClearWeather())
                && ElementalAbility.isAir(block.getType())
                && block.getTemperature() < 0.95
                && block.getLightFromSky()==15;
    }

    @Override
    public boolean isSneakAbility() {
        return true;
    }

    @Override
    public boolean isHarmlessAbility() {
        return true;
    }

    @Override
    public long getCooldown() {
        return this.cooldown;
    }

    @Override
    public String getName() {
        return "Rainbending";
    }

    @Override
    public Location getLocation() {
        return this.origin;
    }

    public double getArcCurr() {
        return arcCurr;
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
                :super.getInstructions();
    }

    @Override
    public boolean isEnabled() {
        return TweaksConfig.isEnabled(this);
    }

    @Override
    public void load() {
        TweaksConfig.addDefault(this, "Enabled", true);
        TweaksConfig.addDefault(this, "Cooldown", 0);
        TweaksConfig.addDefault(this, "WaterParticles", 12);
        TweaksConfig.addDefault(this, "DrainingBlockContribution", 20);
        TweaksConfig.addDefault(this, "DrainedBlocksCapacity", 20);
        TweaksConfig.addDefault(this, "DrainingOnOriginMoveParticles", true);
        TweaksConfig.addDefault(this, "CoverageRadius", 7);
        TweaksConfig.addDefault(this, "CoverageEachBlockContribution", 1);
        TweaksConfig.addDefault(this, "CoverageWaterSourceChance", 0.03);
        TweaksConfig.addDefault(this, "CoverageIceSnowSourceChance", 0.02);
        TweaksConfig.addDefault(this, "CoveragePlantSourceChance", 0.01);
        TweaksConfig.addDefault(this, "CoverageDrainChanceRaining", 0.005);
        TweaksConfig.addDefault(this, "CoverageDrainChanceThundering", 0.007);
        TweaksConfig.addDefault(this, "OctopusForm.ChargeAmount", 1000);
        TweaksConfig.addDefault(this, "OctopusForm.Enabled", true);
        TweaksConfig.addDefault(this, "IceBlast.ChargeAmount", 1000);
        TweaksConfig.addDefault(this, "IceBlast.Enabled", true);
        TweaksConfig.addDefault(this, "Surge.ChargeAmount", 1000);
        TweaksConfig.addDefault(this, "Surge.Enabled", true);
        TweaksConfig.addDefault(this, "IceSpike.ChargeAmount", 500);
        TweaksConfig.addDefault(this, "IceSpike.Enabled", true);
        TweaksConfig.addDefault(this, "WaterManipulation.ChargeAmount", 500);
        TweaksConfig.addDefault(this, "WaterManipulation.Enabled", true);
        TweaksConfig.addDefault(this, "WaterSpout.ChargeAmount", 1500);
        TweaksConfig.addDefault(this, "WaterSpout.Enabled", true);
        TweaksConfig.addDefault(this, "Torrent.ChargeAmount", 1500);
        TweaksConfig.addDefault(this, "Torrent.Enabled", true);
        TweaksConfig.addDefault(this, "WaterArms.ChargeAmount", 1500);
        TweaksConfig.addDefault(this, "WaterArms.Enabled", true);
        TweaksConfig.addDefault(this, "HealingWaters.ChargeAmount", 1500);
        TweaksConfig.addDefault(this, "HealingWaters.Enabled", true);
        TweaksConfig.addDefault(this, "PhaseChange.ChargeAmount", 200);
        TweaksConfig.addDefault(this, "PhaseChange.Enabled", true);
        TweaksConfig.addLanguage(this,"Description",
                "As Hama has demonstrated, waterbenders are capable of drawing water directly from the air." +
                        " If it's raining, getting water becomes even easier.");
        TweaksConfig.addLanguage(this,"Instructions",
                "PhaseChange(Release Sneak) -> PhaseChange(Hold Sneak) " +
                        "-> Choose ability and collect water " +
                        "-> Follow the instructions in the ChargeBar (Be careful not to shake the camera in the last step)");
        coverageRadius = getConfig().getDouble(TweaksConfig.getConfigPath(this, "CoverageRadius"));
        drainingOnOriginMoveParticles = getConfig().getBoolean(TweaksConfig.getConfigPath(this, "DrainingOnOriginMoveParticles"));
        coverageWaterSourceChance = getConfig().getDouble(TweaksConfig.getConfigPath(this, "CoverageWaterSourceChance"));
        coverageIceSnowSourceChance = getConfig().getDouble(TweaksConfig.getConfigPath(this, "CoverageIceSnowSourceChance"));
        coveragePlantSourceChance = getConfig().getDouble(TweaksConfig.getConfigPath(this, "CoveragePlantSourceChance"));
        coverageDrainChanceRaining = getConfig().getDouble(TweaksConfig.getConfigPath(this, "CoverageDrainChanceRaining"));
        coverageDrainChanceThundering = getConfig().getDouble(TweaksConfig.getConfigPath(this, "CoverageDrainChanceThundering"));
        coverageEachBlockContribution = getConfig().getDouble(TweaksConfig.getConfigPath(this, "CoverageEachBlockContribution"));
        drainingBlockContribution = getConfig().getDouble(TweaksConfig.getConfigPath(this, "DrainingBlockContribution"));
        drainedBlocksCapacity = getConfig().getInt(TweaksConfig.getConfigPath(this, "DrainedBlocksCapacity"));
        waterParticles = getConfig().getInt(TweaksConfig.getConfigPath(this, "WaterParticles"));
        maxChargeForParticles = Collections.max(Arrays.asList(ArrayUtils.toObject(new double[]{
            getConfig().getDouble(TweaksConfig.getConfigPath(this,"OctopusForm.ChargeAmount")),
                    getConfig().getDouble(TweaksConfig.getConfigPath(this,"IceBlast.ChargeAmount")),
                    getConfig().getDouble(TweaksConfig.getConfigPath(this,"Surge.ChargeAmount")),
                    getConfig().getDouble(TweaksConfig.getConfigPath(this,"IceSpike.ChargeAmount")),
                    getConfig().getDouble(TweaksConfig.getConfigPath(this,"WaterManipulation.ChargeAmount")),
                    getConfig().getDouble(TweaksConfig.getConfigPath(this,"WaterSpout.ChargeAmount")),
                    getConfig().getDouble(TweaksConfig.getConfigPath(this,"Torrent.ChargeAmount")),
                    getConfig().getDouble(TweaksConfig.getConfigPath(this,"WaterArms.ChargeAmount")),
                    getConfig().getDouble(TweaksConfig.getConfigPath(this,"HealingWaters.ChargeAmount")),
                    getConfig().getDouble(TweaksConfig.getConfigPath(this,"PhaseChange.ChargeAmount"))
        })));
        TweaksConfig.saveDefaultConfig();
        TweaksConfig.saveLanguageConfig();
        ListenerManager.Register();
        this.perm = new Permission("bending.ability.Rainbending");
        this.perm.setDefault(PermissionDefault.TRUE);
        TweaksInfo.loadLog(this);
    }

    @Override
    public void stop() {
        ListenerManager.Unregister();
        ProjectKorra.plugin.getServer().getPluginManager().removePermission(this.perm);
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
        return new Rainbending(player);
    }

    @Override
    public ArrayList<ComboManager.AbilityInformation> getCombination() {
        final ArrayList<ComboManager.AbilityInformation> dropletsBending = new ArrayList<>();
        dropletsBending.add(new ComboManager.AbilityInformation("PhaseChange", ClickType.SHIFT_UP));
        dropletsBending.add(new ComboManager.AbilityInformation("PhaseChange", ClickType.SHIFT_DOWN));
        return dropletsBending;
    }
}
