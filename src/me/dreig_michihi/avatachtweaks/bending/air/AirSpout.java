package me.dreig_michihi.avatachtweaks.bending.air;

import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ProjectKorra;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.AirAbility;
import com.projectkorra.projectkorra.ability.ElementalAbility;
import com.projectkorra.projectkorra.ability.util.Collision;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.configuration.ConfigManager;
import com.projectkorra.projectkorra.util.ParticleEffect;
import me.dreig_michihi.avatachtweaks.TweaksConfig;
import me.dreig_michihi.avatachtweaks.TweaksGeneralMethods;
import me.dreig_michihi.avatachtweaks.TweaksInfo;
import me.dreig_michihi.avatachtweaks.listener.ListenerManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class AirSpout extends AirAbility implements AddonAbility {

	private static final Integer[] DIRECTIONS = { 0, 1, 2, 3, 5, 6, 7, 8 };

	private int angle;
	private long animTime;
	private long interval;
	@Attribute(Attribute.DURATION)
	private long duration;
	@Attribute(Attribute.COOLDOWN)
	private long cooldown;
	@Attribute(Attribute.HEIGHT)
	private double height;
	private List<Location> particlesLocations = new ArrayList<>();
	private double rotation=0;

	private double staminaReserve;
	private double staminaValue;
	private double minSpeed;
	private double additionalSpeed;
	private double staminaCost;
	private double staminaRecovery;
	private long additionalCooldown;

	private static final String STAMINA_BAR_TITLE = "AirSpout Stamina";
	private BossBar staminaBar;

	public AirSpout(final Player player) {
		super(player);



		if (!this.bPlayer.canBend(this)) {
			return;
		}
		this.angle = 0;
		this.cooldown = getConfig().getLong("Abilities.Air.AirSpout.Cooldown");
		this.duration = getConfig().getLong("Abilities.Air.AirSpout.Duration");
		this.animTime = System.currentTimeMillis();
		this.interval = getConfig().getLong("Abilities.Air.AirSpout.Interval");
		this.height = getConfig().getDouble("Abilities.Air.AirSpout.Height");

		this.staminaReserve = getConfig().getDouble(TweaksConfig.getConfigPath(this,"StaminaReserve"));
		this.staminaValue = getConfig().getDouble(TweaksConfig.getConfigPath(this,"StaminaOnStart"));
		this.staminaValue = Math.min(staminaValue, staminaReserve);
		this.minSpeed = getConfig().getDouble(TweaksConfig.getConfigPath(this,"MinSpeed"));
		this.additionalSpeed = getConfig().getDouble(TweaksConfig.getConfigPath(this,"AdditionalSpeed"));
		this.staminaCost = getConfig().getDouble(TweaksConfig.getConfigPath(this,"StaminaCost"));
		this.staminaRecovery = getConfig().getDouble(TweaksConfig.getConfigPath(this,"StaminaRecovery"));
		this.additionalCooldown = getConfig().getLong(TweaksConfig.getConfigPath(this,"AdditionalCooldown"));

		final AirSpout spout = getAbility(player, AirSpout.class);
		if (spout != null) {
			spout.remove();
			return;
		}
		final double heightRemoveThreshold = 2;
		if (!this.isWithinMaxSpoutHeight(heightRemoveThreshold)) {
			return;
		}

		this.flightHandler.createInstance(player, this.getName());

		if (this.bPlayer.isAvatarState()) {
			this.height = getConfig().getDouble("Abilities.Avatar.AvatarState.Air.AirSpout.Height");
		}

		this.staminaBar = Bukkit.getServer().createBossBar(STAMINA_BAR_TITLE, BarColor.WHITE, BarStyle.SEGMENTED_10);
		this.staminaBar.addPlayer(player);
		this.staminaBar.setProgress(0);
		this.start();
	}

	/**
	 * This method was used for the old collision detection system. Please see
	 * {@link Collision} for the new system.
	 */
	@Deprecated
	public static boolean removeSpouts(Location loc0, final double radius, final Player sourceplayer) {
		boolean removed = false;
		for (final AirSpout spout : getAbilities(AirSpout.class)) {
			if (!spout.player.equals(sourceplayer)) {
				final Location loc1 = spout.player.getLocation().getBlock().getLocation();
				loc0 = loc0.getBlock().getLocation();
				final double dx = loc1.getX() - loc0.getX();
				final double dy = loc1.getY() - loc0.getY();
				final double dz = loc1.getZ() - loc0.getZ();

				final double distance = Math.sqrt(dx * dx + dz * dz);

				if (distance <= radius && dy > 0 && dy < spout.height) {
					spout.remove();
					removed = true;
				}
			}
		}
		return removed;
	}
	private float defaultFlySpeed=-1;
	private void allowFlight() {
		if(defaultFlySpeed==-1)
		defaultFlySpeed = this.player.getFlySpeed();
		this.player.setFlySpeed(0.05F);
		if (!this.player.getAllowFlight()) {
			this.player.setAllowFlight(true);
		}
		if (!this.player.isFlying()) {
			this.player.setFlying(true);
		}
	}

	private void removeFlight() {
		if(defaultFlySpeed>=0)
		this.player.setFlySpeed(defaultFlySpeed);
		if (this.player.isFlying()) {
			this.player.setFlying(false);
		}
		if (this.player.getAllowFlight()) {
			this.player.setAllowFlight(false);
		}
	}

	private boolean isWithinMaxSpoutHeight(final double threshold) {
		final Block ground = this.getGround();
		if (ground == null) {
			return false;
		}
		final double playerHeight = this.player.getLocation().getY();
		if (playerHeight > ground.getLocation().getY() + this.height + threshold) {
			return false;
		}
		return true;
	}

	private Block getGround() {
		final Block standingblock = this.player.getLocation().getBlock();
		for (int i = 0; i <= this.height + 5; i++) {
			final Block block = standingblock.getRelative(BlockFace.DOWN, i);
			if (GeneralMethods.isSolid(block) || ElementalAbility.isWater(block)) {
				return block;
			}
		}
		return null;
	}
	@Override
	public void progress() {
		if (this.player.isDead() || !this.player.isOnline() || !this.bPlayer.canBendIgnoreBinds(this) || !this.bPlayer.canBind(this)) {
			this.remove();
			return;
		} else if (this.duration != 0 && System.currentTimeMillis() > this.getStartTime() + this.duration) {
			//this.bPlayer.addCooldown(this);
			this.remove();
			return;
		}
		final double heightRemoveThreshold = 2;
		if (!this.isWithinMaxSpoutHeight(heightRemoveThreshold)) {
			//this.bPlayer.addCooldown(this);
			this.remove();
			return;
		}

		final Block eyeBlock = this.player.getEyeLocation().getBlock();
		if (ElementalAbility.isWater(eyeBlock) || GeneralMethods.isSolid(eyeBlock)) {
			this.remove();
			return;
		}

		this.player.setFallDistance(0);
		this.player.setSprinting(false);
		this.rotateAirColumn(this.player.getLocation());
		if (ThreadLocalRandom.current().nextInt(4) == 0) {
			playAirbendingSound(this.player.getLocation());
		}

		staminaBar.setProgress(Math.max(0, Math.min(1, staminaValue / staminaReserve)));
		//bossBar
		if (this.staminaBar.getProgress() > .75) {
			this.staminaBar.setColor(BarColor.WHITE);
			this.staminaBar.setTitle(ChatColor.WHITE + STAMINA_BAR_TITLE);
		} else if (this.staminaBar.getProgress() > .50) {
			this.staminaBar.setColor(BarColor.GREEN);
			this.staminaBar.setTitle(ChatColor.GREEN + STAMINA_BAR_TITLE);
		} else if (this.staminaBar.getProgress() > .25) {
			this.staminaBar.setColor(BarColor.YELLOW);
			this.staminaBar.setTitle(ChatColor.YELLOW + STAMINA_BAR_TITLE);
		} else {
			this.staminaBar.setColor(BarColor.RED);
			this.staminaBar.setTitle(ChatColor.RED + STAMINA_BAR_TITLE);
		}
		final Block block = this.getGround();
		if (block != null) {
			final double dy = this.player.getLocation().getY() - block.getY();
			if (this.player.isSneaking() && this.bPlayer.getBoundAbilityName().equalsIgnoreCase(this.getName())) {
				if (staminaValue >= staminaCost)
					staminaValue -= staminaCost;
				else
					staminaValue = 0;
				if (dy > this.height && this.player.getEyeLocation().getDirection().getY() > 0)
					this.player.setVelocity(this.player.getEyeLocation().getDirection().setY(0).normalize().multiply(minSpeed + additionalSpeed * (staminaValue / 100)));
				else
					this.player.setVelocity(this.player.getEyeLocation().getDirection().multiply(minSpeed + additionalSpeed * (staminaValue / 100)));
			} else if (staminaValue < staminaReserve)
				staminaValue += staminaRecovery;
			if (dy > this.height) {
				this.removeFlight();
			} else {
				this.allowFlight();
			}
		} else {
			this.remove();
		}
	}
	@Override
	public void remove() {
		super.remove();
		this.staminaBar.removeAll();
		for(Location loc:this.particlesLocations){
			ParticleEffect.EXPLOSION_NORMAL.display(loc,3,0.3,0.1,0.3,Math.random()*0.3);
		}
		bPlayer.addCooldown(this,  this.cooldown+(long)(((staminaReserve-staminaValue)/staminaReserve)*additionalCooldown));
		this.flightHandler.removeInstance(this.player, this.getName());
	}

	private int randRotation=1;
	private void rotateAirColumn(final Location location) {
		//ParticleEffect.EXPLOSION_NORMAL.display(this.player.getLocation(),1);
		if (System.currentTimeMillis() >= this.animTime + this.interval) {
			this.rotation += 0.4;
			int i = 0;
			int j = 0;
			playAirbendingParticles(this.player.getLocation().add(0,.2,0),1,0.3,0.2,0.3);
			for (Location loc : this.particlesLocations) {
				loc.add(0, -((player.getLocation().distance(loc)/this.height)>0.5?0.6:0.5), 0);
				if(i<360)
					i+=(360/ (long) particlesLocations.size());
				else
					i=0;
				if(j>0)
					j-=(360/ (long) particlesLocations.size());
				else
					j=360;
				//player.sendMessage("i="+i);
				//player.sendMessage("j="+j);
				if(Math.random()<0.002F) {
					//player.sendMessage("randRotation*="+randRotation);
					randRotation *= -1;
				}
				//height += 0.5;
				double angle = (j * Math.PI / 180);
				double x = Math.random()*(3*((float)j/(float)360)) * Math.cos((angle + this.rotation)*randRotation);
				double z = Math.random()*(3*((float)j/(float)360)) * Math.sin((angle + this.rotation)*randRotation);
				/*double x = Math.random()*(player.getLocation().distance(loc)/2>2.5?2.5:player.getLocation().distance(loc)/2<0.5?0.5:player.getLocation().distance(loc)/2) * Math.cos(angle*randRotation + this.rotation*randRotation);
				double z = Math.random()*(player.getLocation().distance(loc)/2>2.5?2.5:player.getLocation().distance(loc)/2<0.5?0.5:player.getLocation().distance(loc)/2) * Math.sin(angle*randRotation + this.rotation*randRotation);*/
				Location newLoc1 = loc.clone().add(x,0,z);
				angle = (i * Math.PI / 180);
				x = /*(i%2==0?1:-1)**/Math.random()*(player.getLocation().distance(loc)/2>0.7?0.7: Math.max(player.getLocation().distance(loc) / 2, 0.2)) * Math.cos(angle + this.rotation);
				z = /*(i%2==0?1:-1)**/Math.random()*(player.getLocation().distance(loc)/2>0.7?0.7: Math.max(player.getLocation().distance(loc) / 2, 0.2)) * Math.sin(angle + this.rotation);
				Location newLoc2 = newLoc1.clone().add(x, 0, z);
				TweaksGeneralMethods.displayColoredParticle("FFFFFF",newLoc2,1,0,0,0,0.01, 0.8F);
				playAirbendingParticles(newLoc2, 1,Math.random()*0.1,Math.random()*0.1,Math.random()*0.1);
			}
			for(int k = 0; k< (long) particlesLocations.size(); k++) {
				if (GeneralMethods.isSolid(particlesLocations.get(k).getBlock()) ||
						isWater(particlesLocations.get(k).getBlock()) ||
						isLava(particlesLocations.get(k).getBlock())) {
					if (isLava(particlesLocations.get(k).getBlock()))
						ParticleEffect.SMOKE_LARGE.display(particlesLocations.get(k).getBlock().getRelative(BlockFace.UP).getLocation().add(.5, .5, .5), 1, .5, 0, .5, 0.1);
					else
					ParticleEffect.CLOUD.display(particlesLocations.get(k).getBlock().getRelative(BlockFace.UP).getLocation().add(.5, .5, .5), 2, .5, 0, .5, 0.1);
					particlesLocations.remove(k);
					k--;
				}
			}
			particlesLocations.add(this.player.getLocation().clone().add(0,1,0));
		}
	}

	@Override
	public String getName() {
		return "AirSpout";
	}

	@Override
	public Location getLocation() {
		return this.player != null ? this.player.getLocation() : null;
	}

	@Override
	public long getCooldown() {
		return this.cooldown;
	}

	@Override
	public boolean isSneakAbility() {
		return false;
	}

	@Override
	public boolean isHarmlessAbility() {
		return true;
	}

	@Override
	public boolean isCollidable() {
		return true;
	}

	@Override
	public List<Location> getLocations() {
		final Location topLoc = this.player.getLocation().getBlock().getLocation();
		final double ySpacing = 3;
		final ArrayList<Location> locations = new ArrayList<>(particlesLocations);
		//for (double i = 0; i <= this.height; i += ySpacing) {
		//	locations.add(topLoc.clone().add(0, -i, 0));
		//}
		return locations;
	}

	public int getAngle() {
		return this.angle;
	}

	public void setAngle(final int angle) {
		this.angle = angle;
	}

	public long getAnimTime() {
		return this.animTime;
	}

	public void setAnimTime(final long animTime) {
		this.animTime = animTime;
	}

	public long getInterval() {
		return this.interval;
	}

	public void setInterval(final long interval) {
		this.interval = interval;
	}

	public double getHeight() {
		return this.height;
	}

	public void setHeight(final double height) {
		this.height = height;
	}

	public void setCooldown(final long cooldown) {
		this.cooldown = cooldown;
	}

	@Override
	public boolean isEnabled() {return TweaksConfig.isEnabled(this);}

	@Override
	public void load() {
		TweaksConfig.addDefault(this, "Enabled", true);
		TweaksConfig.addDefault(this, "StaminaReserve", 100);
		TweaksConfig.addDefault(this, "StaminaOnStart", 50);
		TweaksConfig.addDefault(this, "StaminaCost", 1.5);
		TweaksConfig.addDefault(this, "StaminaRecovery", 1);
		TweaksConfig.addDefault(this, "MinSpeed", 0.25);
		TweaksConfig.addDefault(this, "AdditionalSpeed", 0.75);
		TweaksConfig.addDefault(this, "AdditionalCooldown", 7000);
		TweaksConfig.saveDefaultConfig();
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
}