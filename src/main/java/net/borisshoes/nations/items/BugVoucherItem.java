package net.borisshoes.nations.items;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import eu.pb4.polymer.core.api.item.PolymerItem;
import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.borisshoes.arcananovum.ArcanaRegistry;
import net.borisshoes.nations.Nations;
import net.borisshoes.nations.NationsRegistry;
import net.borisshoes.nations.gameplay.Nation;
import net.borisshoes.nations.gui.BugVoucherGui;
import net.borisshoes.nations.utils.MiscUtils;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.*;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.HashMap;

import static net.borisshoes.nations.Nations.MOD_ID;
import static net.borisshoes.nations.Nations.log;

public class BugVoucherItem extends Item implements PolymerItem {
   
   public static HashMap<ItemStack,Integer> loot = new HashMap<>();
   
   public static void fillLoot(MinecraftServer server){
      loot.put(new ItemStack(ArchetypeRegistry.CHANGE_ITEM,1),250);
      loot.put(new ItemStack(ArchetypeRegistry.CHANGE_ITEM,2),100);
      loot.put(new ItemStack(NationsRegistry.RESEARCH_BULLION_ITEM,1),250);
      loot.put(new ItemStack(NationsRegistry.RESEARCH_BULLION_ITEM,3),150);
      loot.put(new ItemStack(NationsRegistry.RESEARCH_BULLION_ITEM,5),100);
      loot.put(new ItemStack(NationsRegistry.GROWTH_BULLION_ITEM,1),250);
      loot.put(new ItemStack(NationsRegistry.GROWTH_BULLION_ITEM,3),150);
      loot.put(new ItemStack(NationsRegistry.GROWTH_BULLION_ITEM,5),100);
      loot.put(new ItemStack(NationsRegistry.MATERIAL_BULLION_ITEM,1),500);
      loot.put(new ItemStack(NationsRegistry.MATERIAL_BULLION_ITEM,3),350);
      loot.put(new ItemStack(NationsRegistry.MATERIAL_BULLION_ITEM,5),250);
      loot.put(VictoryPointItem.getWithValue(250),500);
      loot.put(VictoryPointItem.getWithValue(500),250);
      loot.put(VictoryPointItem.getWithValue(750),100);
      loot.put(VictoryPointItem.getWithValue(1500),25);
      loot.put(ArcanaRegistry.MUNDANE_CATALYST.getPrefItem(),100);
      loot.put(ArcanaRegistry.EMPOWERED_CATALYST.getPrefItem(),50);
      loot.put(ArcanaRegistry.EXOTIC_CATALYST.getPrefItem(),25);
      loot.put(ArcanaRegistry.SOVEREIGN_CATALYST.getPrefItem(),5);
      loot.put(ArcanaRegistry.DIVINE_CATALYST.getPrefItem(),1);
      
      ItemStack kbStick = new ItemStack(Items.WOODEN_SWORD);
      kbStick.set(DataComponentTypes.MAX_DAMAGE,50);
      kbStick.set(DataComponentTypes.ITEM_NAME, Text.translatable("item.nations.knockback_stick"));
      kbStick.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE,true);
      kbStick.remove(DataComponentTypes.REPAIRABLE);
      kbStick.set(DataComponentTypes.RARITY,Rarity.EPIC);
      kbStick.set(DataComponentTypes.ITEM_MODEL,Identifier.ofVanilla("stick"));
      kbStick.remove(DataComponentTypes.ENCHANTABLE);
      AttributeModifiersComponent.Builder attrBuilder = AttributeModifiersComponent.builder();
      attrBuilder.add(EntityAttributes.ATTACK_KNOCKBACK, new EntityAttributeModifier(Identifier.of(MOD_ID,"knockback"),1000,EntityAttributeModifier.Operation.ADD_VALUE), AttributeModifierSlot.MAINHAND);
      attrBuilder.add(EntityAttributes.ATTACK_DAMAGE, new EntityAttributeModifier(Identifier.of(MOD_ID,"damage"),0,EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL), AttributeModifierSlot.MAINHAND);
      kbStick.set(DataComponentTypes.ATTRIBUTE_MODIFIERS,attrBuilder.build().withShowInTooltip(false));
      ItemEnchantmentsComponent.Builder enchBuilder = new ItemEnchantmentsComponent.Builder(ItemEnchantmentsComponent.DEFAULT);
      enchBuilder.set(MiscUtils.getEnchantment(Enchantments.KNOCKBACK),10);
      kbStick.set(DataComponentTypes.ENCHANTMENTS,enchBuilder.build().withShowInTooltip(false));
      
      ItemStack fullStick = kbStick.copy();
      fullStick.setDamage(0);
      ItemStack halfStick = kbStick.copy();
      halfStick.setDamage(25);
      ItemStack quarterStick = kbStick.copy();
      quarterStick.setDamage(40);
      
      loot.put(fullStick,100);
      loot.put(halfStick,250);
      loot.put(quarterStick,500);
      
      // 8 13 4
      String[] animalIds = {"minecraft:pig","minecraft:cow","minecraft:sheep","minecraft:rabbit","minecraft:chicken","minecraft:squid","minecraft:bee","minecraft:mooshroom"};
      String[] mobIds = {"minecraft:zombie","minecraft:skeleton","minecraft:spider","minecraft:creeper","minecraft:stray","minecraft:bogged","minecraft:drowned","minecraft:witch","minecraft:pillager","minecraft:zombified_piglin","minecraft:vindicator","minecraft:guardian","minecraft:elder_guardian"};
      String[] rareMobIds = {"minecraft:enderman","minecraft:blaze","minecraft:wither_skeleton","minecraft:evoker"};
      String compoundStr = "{Delay:0s,MaxNearbyEntities:25s,MaxSpawnDelay:400s,MinSpawnDelay:200s,RequiredPlayerRange:25s,SpawnCount:4s,SpawnData:{entity:{id:\"%s\"}},SpawnPotentials:[],SpawnRange:4s,id:\"minecraft:mob_spawner\"}";
      
      for(String animalId : animalIds){
         try{
            ItemStack spawner = new ItemStack(Items.SPAWNER);
            NbtCompound compound = StringNbtReader.parse(compoundStr.replace("%s",animalId));
            spawner.set(DataComponentTypes.BLOCK_ENTITY_DATA, NbtComponent.of(compound));
            loot.put(spawner,50);
         }catch(CommandSyntaxException e){
            log(3,"Error Parsing "+animalId+" Mob Spawner for Voucher Loot");
         }
      }
      
      for(String mobId : mobIds){
         try{
            ItemStack spawner = new ItemStack(Items.SPAWNER);
            NbtCompound compound = StringNbtReader.parse(compoundStr.replace("%s",mobId));
            spawner.set(DataComponentTypes.BLOCK_ENTITY_DATA, NbtComponent.of(compound));
            loot.put(spawner,15);
         }catch(CommandSyntaxException e){
            log(3,"Error Parsing "+mobId+" Mob Spawner for Voucher Loot");
         }
      }
      
      for(String rareMobId : rareMobIds){
         try{
            ItemStack spawner = new ItemStack(Items.SPAWNER);
            NbtCompound compound = StringNbtReader.parse(compoundStr.replace("%s",rareMobId));
            spawner.set(DataComponentTypes.BLOCK_ENTITY_DATA, NbtComponent.of(compound));
            loot.put(spawner,10);
         }catch(CommandSyntaxException e){
            log(3,"Error Parsing "+rareMobId+" Mob Spawner for Voucher Loot");
         }
      }
   }
   
   public BugVoucherItem(Settings settings, String id){
      super(settings.maxCount(1).rarity(Rarity.EPIC)
            .registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(MOD_ID,id)))
            .component(DataComponentTypes.DAMAGE_RESISTANT, new DamageResistantComponent(DamageTypeTags.IS_FIRE)));
   }
   
   @Override
   public ActionResult use(World world, PlayerEntity user, Hand hand){
      if(!(user instanceof ServerPlayerEntity player)) return ActionResult.FAIL;
      BugVoucherGui gui = new BugVoucherGui(player);
      gui.open();
      user.getItemCooldownManager().set(user.getStackInHand(hand),5);
      return ActionResult.SUCCESS;
   }
   
   @Override
   public Item getPolymerItem(ItemStack itemStack, PacketContext packetContext){
      return Items.PAPER;
   }
   
   @Override
   public @Nullable Identifier getPolymerItemModel(ItemStack stack, PacketContext context){
      return Identifier.of(MOD_ID,"bug_voucher");
   }
}
