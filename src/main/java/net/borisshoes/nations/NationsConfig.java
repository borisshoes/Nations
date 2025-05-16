package net.borisshoes.nations;

import net.borisshoes.nations.research.ResearchTech;
import net.borisshoes.nations.utils.ConfigUtils;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.biome.Biome;

import java.util.Locale;
import java.util.Objects;

import static net.borisshoes.nations.Nations.log;

public class NationsConfig {
   public static int getInt(NationsConfig.ConfigSetting<?> setting){
      try{
         return (int) Nations.CONFIG.getValue(setting.getName());
      }catch(Exception e){
         log(3,"Failed to get Integer config for "+setting.getName());
         log(3,e.toString());
      }
      return 0;
   }
   
   public static boolean getBoolean(NationsConfig.ConfigSetting<?> setting){
      try{
         return (boolean) Nations.CONFIG.getValue(setting.getName());
      }catch(Exception e){
         log(3,"Failed to get Boolean config for "+setting.getName());
         log(3,e.toString());
      }
      return false;
   }
   
   public static double getDouble(NationsConfig.ConfigSetting<?> setting){
      try{
         return (double) Nations.CONFIG.getValue(setting.getName());
      }catch(Exception e){
         log(3,"Failed to get Boolean config for "+setting.getName());
         log(3,e.toString());
      }
      return 0;
   }
   
   public static int[] getBiomeConfigValue(RegistryKey<Biome> key){
      try{
         for(ConfigSetting<?> configSetting : NationsRegistry.CONFIG_SETTINGS){
            if(configSetting instanceof BiomeConfigSetting<?> biomeConfigSetting){
               if(biomeConfigSetting.biomeKey.equals(key)){
                  return (int[]) Nations.CONFIG.getValue(configSetting.getName());
               }
            }
         }
      }catch(Exception e){
         log(3,"Failed to get Boolean config for "+key.getValue());
         log(3,e.toString());
      }
      return new int[]{0,0,0};
   }
   
   public record NormalConfigSetting<T>(ConfigUtils.IConfigValue<T> setting) implements ConfigSetting<T>{
      public NormalConfigSetting(ConfigUtils.IConfigValue<T> setting){
         this.setting = Objects.requireNonNull(setting);
      }
      
      public ConfigUtils.IConfigValue<T> makeConfigValue(){
         return setting;
      }
      
      public String getId(){
         return setting.getName().replaceAll("([a-z])([A-Z]+)", "$1_$2").toLowerCase(Locale.ROOT);
      }
      
      public String getName(){
         return setting.getName();
      }
   }
   
   public record BiomeConfigSetting<T>(ConfigUtils.IConfigValue<T> setting, RegistryKey<Biome> biomeKey) implements ConfigSetting<T>{
      public BiomeConfigSetting(ConfigUtils.IConfigValue<T> setting, RegistryKey<Biome> biomeKey){
         this.setting = Objects.requireNonNull(setting);
         this.biomeKey = Objects.requireNonNull(biomeKey);
      }
      
      public ConfigUtils.IConfigValue<T> makeConfigValue(){
         return setting;
      }
      
      public String getId(){
         return setting.getName().replaceAll("([a-z])([A-Z]+)", "$1_$2").toLowerCase(Locale.ROOT);
      }
      
      public String getName(){
         return setting.getName();
      }
   }
   
   public record ResearchConfigSetting<T>(ConfigUtils.IConfigValue<T> setting, RegistryKey<ResearchTech> researchKey) implements ConfigSetting<T>{
      public ResearchConfigSetting(ConfigUtils.IConfigValue<T> setting, RegistryKey<ResearchTech> researchKey){
         this.setting = Objects.requireNonNull(setting);
         this.researchKey = Objects.requireNonNull(researchKey);
      }
      
      public ConfigUtils.IConfigValue<T> makeConfigValue(){
         return setting;
      }
      
      public String getId(){
         return setting.getName().replaceAll("([a-z])([A-Z]+)", "$1_$2").toLowerCase(Locale.ROOT);
      }
      
      public String getName(){
         return setting.getName();
      }
   }
   
   public interface ConfigSetting<T>{
      ConfigUtils.IConfigValue<T> makeConfigValue();
      String getId();
      String getName();
   }
}
