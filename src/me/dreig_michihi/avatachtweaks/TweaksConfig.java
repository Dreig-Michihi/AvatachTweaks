package me.dreig_michihi.avatachtweaks;

import com.projectkorra.projectkorra.Element;
import com.projectkorra.projectkorra.ProjectKorra;
import com.projectkorra.projectkorra.ability.ComboAbility;
import com.projectkorra.projectkorra.ability.CoreAbility;
import com.projectkorra.projectkorra.ability.PassiveAbility;
import com.projectkorra.projectkorra.configuration.ConfigManager;

public class TweaksConfig {
    static public void addDefault(CoreAbility ability, String parameterName, Object value){
        ConfigManager.defaultConfig.get().addDefault(getConfigPath(ability,parameterName), value);
    }

    static public void addLanguage(CoreAbility ability, String parameterName, Object value){
        ConfigManager.languageConfig.get().addDefault(getConfigPath(ability,parameterName), value);
    }

    static public void saveLanguageConfig(){
        ConfigManager.languageConfig.save();
    }

    static public void saveDefaultConfig(){
        ConfigManager.defaultConfig.save();
    }

    static public String getConfigPath(CoreAbility ability, String parameterName){
        return "ExtraAbilities.Dreig_Michihi.AvatachTweaks."
                +((ability.getElement() instanceof Element.SubElement)?
                ((Element.SubElement) ability.getElement()).getParentElement().getName()
                        :ability.getElement().getName())
                +"."
                +(ability instanceof PassiveAbility?"Passive."
                :ability instanceof ComboAbility?"Combo."
                :"")
                +ability.getName()+"."
                +parameterName;
    }
    static public String getConfigPath(){
        return "ExtraAbilities.Dreig_Michihi.AvatachTweaks";
    }
    static public boolean isEnabled(CoreAbility ability){
        if (ConfigManager.defaultConfig.get().contains(TweaksConfig.getConfigPath(ability, "Enabled")))
            return ConfigManager.defaultConfig.get().getBoolean(TweaksConfig.getConfigPath(ability, "Enabled"));
        else
            return true;
    }
}
