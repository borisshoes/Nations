package net.borisshoes.nations.items;

import eu.pb4.polymer.core.api.item.PolymerItem;
import net.borisshoes.nations.Nations;
import net.borisshoes.nations.gameplay.ResourceType;
import net.borisshoes.nations.utils.GenericTimer;
import net.borisshoes.nations.utils.MiscUtils;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.DamageResistantComponent;
import net.minecraft.component.type.DyedColorComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static net.borisshoes.nations.Nations.MOD_ID;

public class ResourceCoinItem extends Item implements PolymerItem {
   
   private final int color;
   private final String id;
   private final ResourceType type;
   
   public ResourceCoinItem(Settings settings, String id, int color, ResourceType type){
      super(settings.maxCount(99).rarity(Rarity.RARE)
            .registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(MOD_ID,id)))
            .component(DataComponentTypes.DAMAGE_RESISTANT, new DamageResistantComponent(DamageTypeTags.IS_FIRE))
            .component(DataComponentTypes.DYED_COLOR, new DyedColorComponent(color, false)));
      this.color = color;
      this.id = id;
      this.type = type;
   }
   
   @Override
   public ActionResult use(World world, PlayerEntity user, Hand hand){
      if(!user.isSneaking()) return super.use(world,user,hand);
      PlayerInventory inv = user.getInventory();
      Item coin = getType().getCoin();
      List<Pair<Integer,ItemStack>> sortedStacks = new ArrayList<>();
      
      int need = 1000;
      int found = 0;
      for(int i = 0 ; i < inv.size(); i++){
         if(inv.getStack(i).isOf(coin)){
            sortedStacks.add(new Pair<>(i,inv.getStack(i)));
            found += inv.getStack(i).getCount();
         }
      }
      sortedStacks.sort(Comparator.comparingInt(p -> p.getRight().getCount()));
      if(found < need) return super.use(world,user,hand);
      user.getItemCooldownManager().set(user.getStackInHand(hand), 20);
      
      for(Pair<Integer,ItemStack> pair : sortedStacks){
         ItemStack stack = pair.getRight();
         int take = Math.min(stack.getCount(), need);
         inv.removeStack(pair.getLeft(), take);
         need -= take;
         if(need <= 0) break;
      }
      
      Nations.addTickTimerCallback(new GenericTimer(1, () -> MiscUtils.returnItems(new SimpleInventory(new ItemStack(getType().getBullion(), 1)), user)));
      return ActionResult.SUCCESS;
   }
   
   public int getColor(){
      return color;
   }
   
   public ResourceType getType(){
      return type;
   }
   
   @Override
   public Item getPolymerItem(ItemStack itemStack, PacketContext packetContext){
      if(getTranslationKey().contains("material")) return Items.COPPER_INGOT;
      if(getTranslationKey().contains("research")) return Items.LAPIS_LAZULI;
      return Items.EMERALD;
   }
   
   @Override
   public @Nullable Identifier getPolymerItemModel(ItemStack stack, PacketContext context){
      return Identifier.of(MOD_ID,"resource_coin");
   }
}
