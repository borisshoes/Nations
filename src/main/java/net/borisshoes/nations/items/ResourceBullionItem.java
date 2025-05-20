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
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.ArrayList;
import java.util.List;

import static net.borisshoes.nations.Nations.MOD_ID;

public class ResourceBullionItem  extends Item implements PolymerItem {
   
   private final int color;
   private final String id;
   private final ResourceType type;
   
   public ResourceBullionItem(Settings settings, String id, int color, ResourceType type){
      super(settings.maxCount(99).rarity(Rarity.EPIC)
            .registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(MOD_ID,id)))
            .component(DataComponentTypes.DAMAGE_RESISTANT, new DamageResistantComponent(DamageTypeTags.IS_FIRE))
            .component(DataComponentTypes.DYED_COLOR, new DyedColorComponent(color, false)));
      this.color = color;
      this.id = id;
      this.type = type;
   }
   
   @Override
   public ActionResult use(World world, PlayerEntity user, Hand hand){
      if(!user.isSneaking()) return super.use(world, user, hand);
      user.getItemCooldownManager().set(user.getStackInHand(hand),20);
      user.getStackInHand(hand).decrement(1);
      List<ItemStack> change = new ArrayList<>();
      for (int rem = 1000, max = getType().getCoin().getMaxCount(); rem > 0; rem -= Math.min(rem, max))
         change.add(new ItemStack(getType().getCoin(), Math.min(rem, max)));
      Nations.addTickTimerCallback(new GenericTimer(1, () -> MiscUtils.returnItems(new SimpleInventory(change.toArray(new ItemStack[0])), user)));
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
      if(getTranslationKey().contains("material")) return Items.COPPER_BLOCK;
      if(getTranslationKey().contains("research")) return Items.LAPIS_BLOCK;
      return Items.EMERALD_BLOCK;
   }
   
   @Override
   public @Nullable Identifier getPolymerItemModel(ItemStack stack, PacketContext context){
      return Identifier.of(MOD_ID,"resource_bullion");
   }
}