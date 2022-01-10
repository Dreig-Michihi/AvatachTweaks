package me.dreig_michihi.avatachtweaks.bending.air.util;

import com.projectkorra.projectkorra.ProjectKorra;
import me.dreig_michihi.avatachtweaks.bending.air.AirBlast;

public class AirbendingManager implements Runnable {

	public ProjectKorra plugin;

	public AirbendingManager(final ProjectKorra plugin) {
		this.plugin = plugin;
	}

	@Override
	public void run() {
		AirBlast.progressOrigins();
	}

}
