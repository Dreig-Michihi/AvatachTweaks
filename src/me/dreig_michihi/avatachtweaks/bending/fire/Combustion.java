package me.dreig_michihi.avatachtweaks.bending.fire;

import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.Element;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ProjectKorra;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.CombustionAbility;
import com.projectkorra.projectkorra.ability.ElementalAbility;
import com.projectkorra.projectkorra.ability.util.Collision;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.util.ParticleEffect;
import com.projectkorra.projectkorra.util.TempBlock;
import me.dreig_michihi.avatachtweaks.TweaksConfig;
import me.dreig_michihi.avatachtweaks.TweaksInfo;
import me.dreig_michihi.avatachtweaks.listener.ListenerManager;
import me.dreig_michihi.avatachtweaks.util.TempFallingBlock;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class Combustion extends CombustionAbility implements AddonAbility {


    private List<Material> ignoreBloks = Arrays.asList(
            Material.BEDROCK, Material.CHEST, Material.TRAPPED_CHEST, Material.OBSIDIAN,
            Material.NETHER_PORTAL, Material.END_PORTAL, Material.END_PORTAL_FRAME, Material.DROPPER, Material.FURNACE,
            Material.DISPENSER, Material.HOPPER, Material.BEACON, Material.BARRIER, Material.SPAWNER
    );

    private enum CombustStage {CHARGING, LAUNCH, CURVE, EXPLOSION}

    @Attribute(Attribute.RANGE)
    private static double range;
    @Attribute(Attribute.SELECT_RANGE)
    private static double minAimRange;
    @Attribute(Attribute.CHARGE_DURATION)
    private static long chargeTime;
    private static long prepareTime;
    @Attribute(Attribute.SPEED)
    private static double speed;
    @Attribute(Attribute.DAMAGE)
    private static double damage;
    private static double damageInWater;
    @Attribute(Attribute.RADIUS)
    private static double explosionRadius;
    private static double extraWaterRadius;
    private static double maxAngle;
    private static double ringsSize;
    private boolean waterdropsParticles;
    @Attribute(Attribute.COOLDOWN)
    private static long cooldown;
    private static long forfeit;
    private static long curveStartTime;
    private static int fireTicks;
    private static long blocksRegen;

    private CombustStage currentStage;
    private Location origin;
    private Vector originDirection;
    private Location location;
    private Location startCurveLocation;
    private Location aimCurveLocation;
    private Location endCurveLocation;
    private Vector direction;
    private BossBar chargeBar;
    private double particlesAngle = 3 * Math.PI;
    private Set<Entity> affectedEntities;
    private double progress = 0;
    private double progressStep;
    private double aimAngle = 0;
    private boolean selfExplosion;
    private OnDamage onDamage;
    private boolean secondSmallExplode = false;
    private long shootTime;

    public Combustion(Player player) {
        super(player);
        if (!this.bPlayer.canBend(this)) {
            return;
        }
        this.currentStage = CombustStage.CHARGING;
        this.chargeBar = Bukkit.getServer().createBossBar("Combustion Charging...", BarColor.WHITE, BarStyle.SEGMENTED_6);
        this.chargeBar.setProgress(0);
        this.chargeBar.addPlayer(player);
        affectedEntities = new HashSet<>();
        updateCharging();
        this.onDamage = new OnDamage(this.bPlayer);
        Bukkit.getPluginManager().registerEvents(onDamage, ProjectKorra.plugin);
        this.start();
    }

    public CombustStage getCurrentStage() {
        return this.currentStage;
    }

    private boolean ShouldRemove() {
        return (!player.isOnline()
                || player.isDead()
                || player.getWorld() != this.getLocation().getWorld()
                || !bPlayer.canBendIgnoreBindsCooldowns(this));
    }

    private boolean ShouldStartCurve() {
        return (System.currentTimeMillis() - shootTime)>curveStartTime;
    }

    private boolean EntityCollisionsCheck(Location center, double radius) {
        LivingEntity entity = GeneralMethods.getClosestLivingEntity(center, radius);
        return (entity != null && entity != this.player);
    }

    private boolean BlockCollisionsCheck(Location center, double radius) {
        Vector x = new Vector(this.direction.getZ(), 0, -this.direction.getX()).normalize();
        Vector y = direction.clone().crossProduct(x).normalize();
        for (double angle = particlesAngle; angle < 2 * Math.PI + particlesAngle; angle += Math.PI) {
            Location side = center.clone()
                    .add(x.clone().multiply(Math.cos(angle)).multiply(radius))
                    .add(y.clone().multiply(Math.sin(angle)).multiply(radius));
            if (GeneralMethods.isSolid(side.getBlock()) || side.getBlock().isLiquid())
                return true;
        }
        return false;
    }

    private void UpdateChargeBar() {
        double spendMs = System.currentTimeMillis() - this.getStartTime();
        if(chargeBar.getColor()==BarColor.WHITE&&spendMs>chargeTime) {
            this.chargeBar.setColor(BarColor.RED);
            this.chargeBar.setTitle("Combustion");
        }
        double subtract = spendMs / (chargeTime+prepareTime);
        double progress = Math.min(1, subtract);
        this.chargeBar.setProgress(progress);
    }

    @Override
    public void handleCollision(final Collision collision) {
        super.handleCollision(collision);

        if (collision.isRemovingFirst()) {
            currentStage = CombustStage.EXPLOSION;
        }
    }

    @Override
    public void progress() {
        if (ShouldRemove()) {
            this.remove();
            return;
        }
        switch (this.currentStage) {
            case CHARGING: {
                if (!updateCharging()) {
                    this.remove();
                    return;
                }//просто отменить способность, если она не была нормально дозаряжена
                break;
            }
            case LAUNCH: {
                if (ShouldStartCurve()) {
                    renderSmallExplosion(this.location);
                    this.startCurveLocation = this.location;
                    Vector aimDirection = this.player.getEyeLocation().getDirection();
                    if (originDirection.angle(aimDirection) > aimAngle) {
                        aimDirection = originDirection.clone().rotateAroundAxis(originDirection.clone().crossProduct(aimDirection), aimAngle);
                    }
                    this.endCurveLocation = this.player.getEyeLocation();
                    boolean hit = false;
                    while (endCurveLocation.distance(origin) < range && !GeneralMethods.isSolid(endCurveLocation.getBlock()) && !hit) {//пока дистанция позволяет и блок в локации не цельный
                        LivingEntity entity = GeneralMethods.getClosestLivingEntity(endCurveLocation,
                                endCurveLocation.distance(origin) < startCurveLocation.distance(origin) ? 0.1 : explosionRadius / 2);
                        if (entity != null && entity != this.player) {
                            endCurveLocation = entity.getLocation().add(0, 0.5, 0);
                            hit = true;
                        } else
                            endCurveLocation.add(aimDirection.clone().multiply(0.5));
                    }
                    if (endCurveLocation.distance(origin) < minAimRange)
                        this.endCurveLocation = this.player.getEyeLocation().add(aimDirection.clone().multiply(minAimRange));
                    this.aimCurveLocation = this.startCurveLocation.clone().add(this.originDirection.clone().multiply((startCurveLocation.distance(endCurveLocation)) * 0.66));
                    Location middlePoint = getBezierLocation(0.5);
                    this.progressStep = speed / (startCurveLocation.distance(middlePoint) + middlePoint.distance(endCurveLocation)); //скорость/примерная длина кривой
                    this.currentStage = CombustStage.CURVE;
                } else if (!updateLaunch()) {
                    explode();
                }
                break;
            }
            case CURVE: {
                if (!updateCurve()) {
                    explode();
                }
                break;
            }
            case EXPLOSION: {
                if (!updateExplosion())
                    this.remove();
                break;
            }
            default: {
                this.remove();
            }
        }
    }

    private boolean updateCharging() {
        this.originDirection = this.player.getEyeLocation().getDirection();
        this.direction = this.originDirection;
        this.origin = this.player.getEyeLocation().add(direction.clone().crossProduct(
                                new Vector(direction.getZ(), 0, -direction.getX())).normalize()
                        .multiply(player.isSneaking() ? 0.3 : 0.4))
                .add(this.direction.clone().multiply(0.35));
        this.location = this.origin.clone();
        if (ThreadLocalRandom.current().nextFloat() < 0.2) {
            ParticleEffect.SMOKE_NORMAL.display(this.location, 0, 0, -1, 0, 0.05);
        }
        UpdateChargeBar();
        if (selfExplosion) {
            ThreadLocalRandom random = ThreadLocalRandom.current();
            if (this.progress >= 1 || !secondSmallExplode) {
                secondSmallExplode = true;
                this.progress = 0;
                Location rLoc = this.location.add(new Vector(random.nextDouble(-1, 1), random.nextDouble(-0.6, 0.6), random.nextDouble(-1, 1)).multiply(2));
                renderSmallExplosion(rLoc);
            }
            this.progress += 0.15;
        }
        double barProgress = chargeBar.getProgress();
        if (!bPlayer.getBoundAbilityName().equals(this.getName())//сменил слот?
                || !player.isSneaking()//встал?
                || barProgress >= 1) {//или зарядил полностью
            if (selfExplosion) {
                if (barProgress >= 1
                ||(barProgress >= 0.75&&!bPlayer.getBoundAbilityName().equals(this.getName()))) {
                    explode();
                    this.particlesAngle = Math.PI * 1.9;//чтобы не было большой задержки перед самоподрывом и его нельзя было избежать, просто летя в это время на каком-нибудь FireJet
                    this.bPlayer.addCooldown(this, cooldown + forfeit);
                }
            } else {
                if (chargeBar.getColor()==BarColor.RED) {
                    shoot();
                } else {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean updateLaunch() {
        double stepCount = Math.ceil(speed / 0.1);
        for (int i = 0; i <= stepCount; i++) {
            location.add(originDirection.clone().multiply(speed / stepCount));
            render();
            if (checkCollisions()) {
                return false;//столкновение = взрыв
            }
        }
        return true;
    }

    private boolean updateCurve() {
        double stepCount = Math.ceil(speed / 0.1);
        Location oldLocation = this.location.clone();
        for (int i = 0; i <= stepCount; i++) {
            if (progress < 1)
                progress += progressStep / stepCount;
            if (progress > 1)
                progress = 1;
            this.location = getBezierLocation(progress);
            render();
            if (checkCollisions()) {
                return false;//столкновение = взрыв
            }
        }
        if (!secondSmallExplode && progress > 0.6) {
            renderSmallExplosion(this.location);
            secondSmallExplode = true;
        }
        this.direction = (this.location.toVector().subtract(oldLocation.toVector()));

        if (progress >= 1) {
            explode();
        }
        return true;
    }

    private boolean updateExplosion() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        if (particlesAngle < Math.PI * 2) {
            //world.playSound(this.location.toLocation(world), Sound.ENTITY_CREEPER_PRIMED, 6f, (float) particlesAngle);//эксперимент с звуками
            playFirebendingParticles(this.location, 10, -0.1, 0.1f, 0.1f, 0.1f);
            particlesAngle += Math.PI / 2.5;//за пол секунды полный круг
            if (particlesAngle >= Math.PI * 2) {
                ParticleEffect.EXPLOSION_HUGE.display(location, 1);
                Objects.requireNonNull(location.getWorld()).playSound(this.location, Sound.ENTITY_GENERIC_EXPLODE, 6, 0f);
                location.getWorld().playSound(this.location, Sound.ENTITY_GENERIC_EXPLODE, 6, 0.8f);
            }
            return true;
        } else {
            for (Entity entity : GeneralMethods.getEntitiesAroundPoint(this.location, explosionRadius)) {
                if (!affectedEntities.contains(entity)) {
                    affectEntity(entity);
                    affectedEntities.add(entity);
                }
            }
            boolean containedWater = false;
            for (Block block : GeneralMethods.getBlocksAroundPoint(this.location, explosionRadius)) {
                if (!GeneralMethods.isRegionProtectedFromBuild(this, this.location)) {
                    if (!ElementalAbility.isAir(block.getType())
                            && !ElementalAbility.isLava(block)
                            && !ElementalAbility.isWater(block)
                            && !ignoreBloks.contains(block.getType())) {
                        BlockData bd = block.getBlockData();
                        new TempBlock(block, Material.AIR.createBlockData(), blocksRegen);
                        if (random.nextBoolean()) {
                            /*TempFallingBlock tmpBlock = */new TempFallingBlock(block.getLocation(),
                                    bd, direction.clone().multiply(-1).add(new Vector(
                                    random.nextDouble(-0.5, 0.5),
                                    random.nextDouble(-0.5, 0.5),
                                    random.nextDouble(-0.5, 0.5))).normalize(),
                                    this
                            );
                        }
                    } else if (ElementalAbility.isWater(block)) {
                        containedWater=true;
                        ParticleEffect.WATER_DROP.display(this.location, 15, explosionRadius*2, explosionRadius*2, explosionRadius*2, 0.5);
                    }
                }
            }
            if(containedWater) {
                if(waterdropsParticles) {
                    for (Entity entity : GeneralMethods.getEntitiesAroundPoint(this.location, explosionRadius + extraWaterRadius)) {
                        if (entity instanceof LivingEntity) {
                            if (ElementalAbility.isWater(entity.getLocation().getBlock())) {
                                ((LivingEntity) entity).damage(damageInWater);
                            }
                        }
                    }
                    Objects.requireNonNull(location.getWorld()).playSound(location, Sound.BLOCK_FIRE_EXTINGUISH, 6, 0.7f);
                }
            }
            for (int i = 0; i < 30; i++) {
                Location randLoc = this.location.clone().add(new Vector(
                        random.nextDouble(-1, 1),
                        random.nextDouble(-1, 1),
                        random.nextDouble(-1, 1)
                ).multiply(random.nextDouble(1) * explosionRadius));
                playFirebendingParticles(this.location, 15, random.nextDouble(0.1, 0.3)
                        , random.nextDouble((explosionRadius / 3))
                        , random.nextDouble((explosionRadius / 3))
                        , random.nextDouble((explosionRadius / 3)));
                ParticleEffect.EXPLOSION_LARGE.display(randLoc, 1);
                ParticleEffect.EXPLOSION_NORMAL.display(randLoc, 1, 0, 0, 0, 0.4);
            }
            //world.playSound(this.location.toLocation(world), Sound.ENTITY_GENERIC_EXPLODE, 6, 0.2f);
            return false;
        }
    }

    private void affectEntity(Entity entity) {
        if (entity instanceof LivingEntity) {
            Vector vec = (((LivingEntity) entity).getEyeLocation().toVector().subtract(this.location.toVector())).normalize();
            if (vec.getY() > 0.5)
                vec.setY(0.5);
            if (vec.getY() < -0.5)
                vec.setY(-0.5);
            entity.setVelocity(vec.multiply(3));
            ((LivingEntity) entity).damage(damage, this.player);
            entity.setFireTicks(fireTicks);
        } else {
            entity.setVelocity((entity.getLocation().add(0, 0.5, 0).toVector()
                    .subtract(this.location.toVector())).normalize());
            entity.setFireTicks(fireTicks);
        }
    }

    private void explode() {
        this.particlesAngle = 0;//нужно для инь-янь анимации перед взрывом
        this.currentStage = CombustStage.EXPLOSION;//чтобы начал прогрессировать код взрыва
        if (!this.secondSmallExplode)
            renderSmallExplosion(this.location);
        ParticleEffect.FLASH.display(this.location, 1);
    }

    private boolean checkCollisions() {
        return BlockCollisionsCheck(this.location.clone().add(direction), 0.15)
                || EntityCollisionsCheck(this.location.clone().add(direction), explosionRadius / 3);
    }

    private Location getBezierLocation(double progress) {
        return getSegmentPoint(
                getSegmentPoint(startCurveLocation, aimCurveLocation, progress),
                getSegmentPoint(aimCurveLocation, endCurveLocation, progress),
                progress);
    }

    private Location getSegmentPoint(Location from, Location to, double progress) {
        return from.clone().add((to.toVector().subtract(from.toVector())).multiply(progress));
    }
    private void shoot() {
        shootTime = System.currentTimeMillis();
        this.aimAngle = ((this.chargeBar.getProgress()-(chargeTime/(double)(chargeTime+prepareTime)))
                /(1-(chargeTime/(double)(chargeTime+prepareTime))))*maxAngle;
        //this.aimAngle = this.chargeBar.getProgress() * maxAngle;//чем больше "заряжена" способность, тем сильнее можно будет сменить ей направление.
        this.direction = this.originDirection;
        ParticleEffect.EXPLOSION_NORMAL.display(location, 0, direction.getX(), direction.getY(), direction.getZ(), 0.01);
        Objects.requireNonNull(location.getWorld()).playSound(this.location, Sound.ENTITY_ZOMBIE_INFECT, 0.4f, 1.5f);
        location.getWorld().playSound(this.location, Sound.ENTITY_ZOMBIE_INFECT, 0.4f, 0f);
        this.chargeBar.removePlayer(this.player);
        this.currentStage = CombustStage.LAUNCH;
        this.bPlayer.addCooldown(this);
    }

    private void playFirebendingParticles(Location loc, int amount, double extra, double xOffset, double yOffset, double zOffset) {
        if (this.getBendingPlayer().canUseSubElement(Element.SubElement.BLUE_FIRE)) {
            ParticleEffect.SOUL_FIRE_FLAME.display(loc, amount, xOffset, yOffset, zOffset, extra);
        } else {
            ParticleEffect.FLAME.display(loc, amount, xOffset, yOffset, zOffset, extra);
        }
    }

    private void render() {
        if (this.currentStage != CombustStage.EXPLOSION) {
            Vector x = new Vector(this.direction.getZ(), 0, -this.direction.getX()).normalize();
            Vector y = this.direction.clone().crossProduct(x).normalize();
            double r = 0.2 - 0.2 * this.origin.distance(this.location) / range;
            Location left = this.location.clone()
                    .add(x.clone().multiply(Math.cos(particlesAngle)).multiply(r))
                    .add(y.clone().multiply(Math.sin(particlesAngle)).multiply(r));
            Location right = this.location.clone()
                    .add(x.clone().multiply(Math.cos(particlesAngle + Math.PI)).multiply(r))
                    .add(y.clone().multiply(Math.sin(particlesAngle + Math.PI)).multiply(r));
            ParticleEffect.REDSTONE.display(location, 3, 0.01, 0.01, 0.01,
                    new org.bukkit.Particle.DustOptions
                            (Color.WHITE,
                                    0.65f));
            ParticleEffect.REDSTONE.display(left, 1, 0, 0, 0,
                    new org.bukkit.Particle.DustOptions
                            (Color.GRAY,
                                    0.5f));
            ParticleEffect.REDSTONE.display(right, 1, 0, 0, 0,
                    new org.bukkit.Particle.DustOptions
                            (Color.GRAY,
                                    0.5f));
            particlesAngle += Math.PI / 20;
            if (particlesAngle >= 4 * Math.PI) {//каждые 3 оборота
                renderRing(this.direction, location);
                particlesAngle -= 4 * Math.PI;
                Objects.requireNonNull(location.getWorld()).playSound(this.location, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.4f, (float) progress);//эксперимент с звуками
            }
        } else {
            ParticleEffect.EXPLOSION_HUGE.display(location, 1);
        }
    }

    private void renderRing(Vector direction, Location location) {
        double randStep = ThreadLocalRandom.current().nextDouble(2 * Math.PI / 20);
        double randRingSize = ThreadLocalRandom.current().nextDouble(0.2* ringsSize, 0.35* ringsSize);
        Vector x = new Vector(this.direction.getZ(), 0, -this.direction.getX()).normalize();
        Vector y = direction.clone().crossProduct(x).normalize();
        for (double angle = randStep; angle < 2 * Math.PI + randStep; angle += 2 * Math.PI / 20) {
            Location side = location.clone()
                    .add(x.clone().multiply(Math.cos(angle)).multiply(explosionRadius))
                    .add(y.clone().multiply(Math.sin(angle)).multiply(explosionRadius));
            Vector extension = (side.toVector().subtract(location.toVector())).normalize().add(direction.clone().multiply(3)).normalize();
            //CLOUD
            ParticleEffect.CLOUD.display(location, 0, extension.getX(), extension.getY(), extension.getZ(), randRingSize);
        }
    }

    private void renderSmallExplosion(Location location) {
        //FLASH.display(this.location.toLocation(world));
        playFirebendingParticles(location, 5, 0.025, 0.1f, 0.1f, 0.1f);
        playFirebendingParticles(location, 5, 0.05, 0.1f, 0.1f, 0.1f);
        playFirebendingParticles(location, 5, 0.1, 0.1f, 0.1f, 0.1f);
        playFirebendingParticles(location, 5, 0.15, 0.1f, 0.1f, 0.1f);
        playFirebendingParticles(location, 5, 0.2, 0.1f, 0.1f, 0.1f);
        Objects.requireNonNull(location.getWorld()).playSound(location, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 5, 1.2f);
        location.getWorld().playSound(location, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 5, 0.0001f);
    }

    @Override
    public void remove() {
        super.remove();
        EntityDamageEvent.getHandlerList().unregister(this.onDamage);
        this.chargeBar.removePlayer(this.player);
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
        return "Combustion";
    }

    @Override
    public Location getLocation() {
        return this.location;
    }

    @Override
    public boolean isEnabled() {
        return TweaksConfig.isEnabled(this);
    }

    @Override
    public String getInstructions() {
        return super.getInstructions();
    }

    @Override
    public String getDescription() {
        return super.getDescription();
    }

    @Override
    public void load() {
        TweaksConfig.addDefault(this, "Enabled", true);
        TweaksConfig.addDefault(this, "ChargeTime", 1000);
        TweaksConfig.addDefault(this, "PrepareTime", 2000);
        TweaksConfig.addDefault(this, "CurveStartTime", 290);
        TweaksConfig.addDefault(this, "Cooldown", 3000);
        TweaksConfig.addDefault(this, "ForfeitCooldown", 15000);
        TweaksConfig.addDefault(this, "FireTicks", 80);
        TweaksConfig.addDefault(this, "BlocksRegen", 20000);
        TweaksConfig.addDefault(this, "Range", 80);
        TweaksConfig.addDefault(this, "MinAimRange", 30);
        TweaksConfig.addDefault(this, "Speed", 2.5);
        TweaksConfig.addDefault(this, "Damage", 7.0);
        TweaksConfig.addDefault(this, "DamageInWaterExtraRadius", 3.0);
        TweaksConfig.addDefault(this, "ExplosionRadius", 3.5);
        TweaksConfig.addDefault(this, "ExtraRadiusInWater", 3.5);
        TweaksConfig.addDefault(this, "MaxAngle", 0.84);
        TweaksConfig.addDefault(this, "RingsSize", 1.25);
        TweaksConfig.addDefault(this, "WaterdropsParticles", true);
        TweaksConfig.saveDefaultConfig();
        chargeTime = getConfig().getLong(TweaksConfig.getConfigPath(this, "ChargeTime"));
        prepareTime = getConfig().getLong(TweaksConfig.getConfigPath(this, "PrepareTime"));
        cooldown = getConfig().getLong(TweaksConfig.getConfigPath(this, "Cooldown"));
        curveStartTime = getConfig().getLong(TweaksConfig.getConfigPath(this, "CurveStartTime"));
        forfeit = getConfig().getLong(TweaksConfig.getConfigPath(this, "ForfeitCooldown"));
        fireTicks = getConfig().getInt(TweaksConfig.getConfigPath(this, "FireTicks"));
        blocksRegen = getConfig().getLong(TweaksConfig.getConfigPath(this, "BlocksRegen"));
        range = getConfig().getDouble(TweaksConfig.getConfigPath(this, "Range"));
        minAimRange = getConfig().getDouble(TweaksConfig.getConfigPath(this, "MinAimRange"));
        speed = getConfig().getDouble(TweaksConfig.getConfigPath(this, "Speed"));
        damage = getConfig().getDouble(TweaksConfig.getConfigPath(this, "Damage"));
        damageInWater = getConfig().getDouble(TweaksConfig.getConfigPath(this, "DamageInWaterExtraRadius"));
        explosionRadius = getConfig().getDouble(TweaksConfig.getConfigPath(this, "ExplosionRadius"));
        extraWaterRadius = getConfig().getDouble(TweaksConfig.getConfigPath(this, "ExtraRadiusInWater"));
        maxAngle = getConfig().getDouble(TweaksConfig.getConfigPath(this, "MaxAngle"));
        ringsSize = getConfig().getDouble(TweaksConfig.getConfigPath(this, "RingsSize"));
        waterdropsParticles = getConfig().getBoolean(TweaksConfig.getConfigPath(this, "WaterdropsParticles"));
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

    private static class OnDamage implements Listener {
        private final BendingPlayer user;

        public OnDamage(BendingPlayer user) {
            this.user = user;
        }

        @EventHandler(ignoreCancelled = true)
        public void onAbilityUserDamage(EntityDamageEvent event) {
            Entity entity = event.getEntity();
            if (entity instanceof Player) {
                if (entity == user.getPlayer()) {
                    final Combustion comb = getAbility(user.getPlayer(), Combustion.class);
                    if (comb != null && comb.getCurrentStage() == CombustStage.CHARGING) {
                        comb.selfExplosion = true;
                    }
                }
            }
        }
    }
}
