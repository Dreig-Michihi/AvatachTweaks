package me.dreig_michihi.avatachtweaks;

import com.projectkorra.projectkorra.ProjectKorra;
import com.projectkorra.projectkorra.ability.CoreAbility;

public class TweaksInfo {
    private static final String version = "AvatachTweaks V0.2.5";
    private static final String author = "Dreig_Michihi";

    public static String getVersion() {
        return version;
    }

    public static String getAuthor() {
        return author;
    }

    public static void loadLog(CoreAbility ability){
        ProjectKorra.log.info(ability.getName() + " by " + author + " " + version + " has been loaded!");
    }
    public static void stopLog(CoreAbility ability){
        ProjectKorra.log.info(ability.getName() + " by " + author + " " + version + " successfully disabled!");
    }
}
