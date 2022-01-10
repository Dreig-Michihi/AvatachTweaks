package me.dreig_michihi.avatachtweaks;

import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ProjectKorra;
import me.dreig_michihi.avatachtweaks.util.TweaksColoredParticle;
import org.bukkit.Color;
import org.bukkit.Location;

public class TweaksGeneralMethods extends GeneralMethods {

    public TweaksGeneralMethods(ProjectKorra plugin) {
        super(plugin);
    }

    public static void displayColoredParticle(String hexVal, final Location loc, final int amount, final double offsetX, final double offsetY, final double offsetZ, final double extra, final float size) {
        int r = 0;
        int g = 0;
        int b = 0;

        if (hexVal.startsWith("#")) {
            hexVal = hexVal.substring(1);
        }

        if (hexVal.length() <= 6) {
            r = Integer.valueOf(hexVal.substring(0, 2), 16);
            g = Integer.valueOf(hexVal.substring(2, 4), 16);
            b = Integer.valueOf(hexVal.substring(4, 6), 16);
        }

        new TweaksColoredParticle(Color.fromRGB(r, g, b), size).display(loc, amount, offsetX, offsetY, offsetZ, extra);
    }
}
