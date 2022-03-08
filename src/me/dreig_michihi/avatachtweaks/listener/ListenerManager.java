package me.dreig_michihi.avatachtweaks.listener;

import com.projectkorra.projectkorra.ProjectKorra;
import com.projectkorra.projectkorra.configuration.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

public class ListenerManager {
    public static Listener listener;
    public static void Register(){
        ConfigManager.defaultConfig.get().addDefault("ExtraAbilities.Dreig_Michihi.AvatachTweaks.Earth.LavaDamageModifier", 1);
        ConfigManager.defaultConfig.get().addDefault("ExtraAbilities.Dreig_Michihi.AvatachTweaks.Earth.LavaTicksModifier", 1);
        if (listener==null) {
            listener = new TweaksListener();
            Bukkit.getServer().getPluginManager().registerEvents(listener, ProjectKorra.plugin);
        }
    }
    public static void Unregister(){
        if(listener!=null) {
            HandlerList.unregisterAll(listener);
            listener=null;
        }
    }
}
