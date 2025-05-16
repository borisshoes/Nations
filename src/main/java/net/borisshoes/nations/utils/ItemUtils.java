package net.borisshoes.nations.utils;

import net.borisshoes.nations.Nations;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;

public class ItemUtils {
   public static NbtCompound getNationsTag(ItemStack stack){
      if(stack == null) return new NbtCompound();
      NbtComponent nbtComponent = stack.get(DataComponentTypes.CUSTOM_DATA);
      if(nbtComponent == null) return new NbtCompound();
      NbtCompound data = nbtComponent.copyNbt();
      if(data != null && data.contains(Nations.MOD_ID, NbtElement.COMPOUND_TYPE)){
         return data.getCompound(Nations.MOD_ID);
      }
      return new NbtCompound();
   }
   
   public static int getIntProperty(ItemStack stack, String key){
      NbtCompound nationsTag = getNationsTag(stack);
      return  nationsTag == null || !nationsTag.contains(key, NbtElement.INT_TYPE) ? 0 : nationsTag.getInt(key);
   }
   
   public static String getStringProperty(ItemStack stack, String key){
      NbtCompound nationsTag = getNationsTag(stack);
      return  nationsTag == null || !nationsTag.contains(key, NbtElement.STRING_TYPE) ? "" : nationsTag.getString(key);
   }
   
   public static boolean getBooleanProperty(ItemStack stack, String key){
      NbtCompound nationsTag = getNationsTag(stack);
      return nationsTag == null || !nationsTag.contains(key, NbtElement.BYTE_TYPE) ? false : nationsTag.getBoolean(key);
   }
   
   public static double getDoubleProperty(ItemStack stack, String key){
      NbtCompound nationsTag = getNationsTag(stack);
      return  nationsTag == null || !nationsTag.contains(key, NbtElement.DOUBLE_TYPE) ? 0.0 : nationsTag.getDouble(key);
   }
   
   public static float getFloatProperty(ItemStack stack, String key){
      NbtCompound nationsTag = getNationsTag(stack);
      return  nationsTag == null || !nationsTag.contains(key, NbtElement.FLOAT_TYPE) ? 0.0f : nationsTag.getFloat(key);
   }
   
   public static long getLongProperty(ItemStack stack, String key){
      NbtCompound nationsTag = getNationsTag(stack);
      return  nationsTag == null || !nationsTag.contains(key, NbtElement.LONG_TYPE) ? 0 : nationsTag.getLong(key);
   }
   
   public static NbtList getListProperty(ItemStack stack, String key, int listType){
      NbtCompound nationsTag = getNationsTag(stack);
      return  nationsTag == null || !nationsTag.contains(key, NbtElement.LIST_TYPE) ? new NbtList() : nationsTag.getList(key,listType);
   }
   
   public static NbtCompound getCompoundProperty(ItemStack stack, String key){
      NbtCompound nationsTag = getNationsTag(stack);
      return  nationsTag == null || !nationsTag.contains(key, NbtElement.COMPOUND_TYPE) ? new NbtCompound() : nationsTag.getCompound(key);
   }
   
   public static void putProperty(ItemStack stack, String key, int property){
      putProperty(stack, key, NbtInt.of(property));
   }
   
   public static void putProperty(ItemStack stack, String key, boolean property){
      putProperty(stack, key, NbtByte.of(property));
   }
   
   public static void putProperty(ItemStack stack, String key, double property){
      putProperty(stack,key,NbtDouble.of(property));
   }
   
   public static void putProperty(ItemStack stack, String key, float property){
      putProperty(stack,key,NbtFloat.of(property));
   }
   
   public static void putProperty(ItemStack stack, String key, String property){
      putProperty(stack,key,NbtString.of(property));
   }
   
   public static void putProperty(ItemStack stack, String key, NbtElement property){
      NbtCompound nationsTag = getNationsTag(stack);
      nationsTag.put(key,property);
      NbtComponent nbtComponent = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
      NbtCompound data = nbtComponent.copyNbt();
      data.put(Nations.MOD_ID,nationsTag);
      NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, data);
   }
   
   public static boolean hasProperty(ItemStack stack, String key){
      NbtCompound nationsTag = getNationsTag(stack);
      return nationsTag.contains(key);
   }
   
   public static boolean hasProperty(ItemStack stack, String key, int type){
      NbtCompound nationsTag = getNationsTag(stack);
      return nationsTag.contains(key,type);
   }
   
   public static boolean removeProperty(ItemStack stack, String key){
      if(hasProperty(stack,key)){
         NbtCompound nationsTag = getNationsTag(stack);
         nationsTag.remove(key);
         if(nationsTag.isEmpty()){
            NbtComponent nbtComponent = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
            NbtCompound data = nbtComponent.copyNbt();
            data.remove(Nations.MOD_ID);
            NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, data);
         }else{
            NbtComponent nbtComponent = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
            NbtCompound data = nbtComponent.copyNbt();
            data.put(Nations.MOD_ID,nationsTag);
            NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, data);
         }
         return true;
      }
      return false;
   }
}