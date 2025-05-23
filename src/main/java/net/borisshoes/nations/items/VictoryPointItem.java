package net.borisshoes.nations.items;

import eu.pb4.polymer.core.api.item.PolymerItem;
import net.borisshoes.nations.Nations;
import net.borisshoes.nations.NationsRegistry;
import net.borisshoes.nations.gameplay.Nation;
import net.borisshoes.nations.gameplay.ResourceType;
import net.borisshoes.nations.utils.GenericTimer;
import net.borisshoes.nations.utils.MiscUtils;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.DamageResistantComponent;
import net.minecraft.component.type.DyedColorComponent;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.ArrayList;
import java.util.List;

import static net.borisshoes.nations.Nations.MOD_ID;

public class VictoryPointItem extends Item implements PolymerItem {
   
   public VictoryPointItem(Settings settings, String id){
      super(settings.maxCount(99).rarity(Rarity.EPIC)
            .registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(MOD_ID,id)))
            .component(DataComponentTypes.DAMAGE_RESISTANT, new DamageResistantComponent(DamageTypeTags.IS_FIRE)));
   }
   
   @Override
   public ActionResult use(World world, PlayerEntity user, Hand hand){
      if(!(user instanceof ServerPlayerEntity player)) return ActionResult.FAIL;
      Nation nation = Nations.getNation(player);
      if(nation == null) return ActionResult.FAIL;
      ItemStack stack = user.getStackInHand(hand);
      NbtComponent customData = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
      NbtCompound copy = customData.copyNbt();
      if(copy.contains(MOD_ID+".vp_value")){
         int value = copy.getInt(MOD_ID+".vp_value");
         nation.addVictoryPoints(value);
         user.getItemCooldownManager().set(stack,5);
         stack.decrementUnlessCreative(1,user);
         player.playSoundToPlayer(SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.PLAYERS, 0.3f,1);
         return ActionResult.SUCCESS;
      }
      return ActionResult.FAIL;
   }
   
   @Override
   public ItemStack getPolymerItemStack(ItemStack itemStack, TooltipType tooltipType, PacketContext context){
      ItemStack defaultStack = PolymerItem.super.getPolymerItemStack(itemStack, tooltipType, context);
      NbtComponent customData = itemStack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
      NbtCompound copy = customData.copyNbt();
      if(copy.contains(MOD_ID+".vp_value")){
         int value = copy.getInt(MOD_ID+".vp_value");
         LoreComponent lore = defaultStack.getOrDefault(DataComponentTypes.LORE,LoreComponent.DEFAULT);
         List<Text> currentLore = new ArrayList<>(lore.styledLines());
         currentLore.add(Text.translatable("text.nations.victory_point_item_lore",value));
         defaultStack.set(DataComponentTypes.LORE,new LoreComponent(currentLore,currentLore));
      }
      return defaultStack;
   }
   
   @Override
   public Item getPolymerItem(ItemStack itemStack, PacketContext packetContext){
      return Items.END_CRYSTAL;
   }
   
   @Override
   public @Nullable Identifier getPolymerItemModel(ItemStack stack, PacketContext context){
      return Identifier.of(MOD_ID,"victory_point");
   }
   
   public static ItemStack getWithValue(int value){
      ItemStack stack = new ItemStack(NationsRegistry.VICTORY_POINT_ITEM);
      NbtCompound comp = new NbtCompound();
      comp.putInt(MOD_ID+".vp_value",value);
      stack.set(DataComponentTypes.CUSTOM_DATA,NbtComponent.of(comp));
      return stack;
   }
}
