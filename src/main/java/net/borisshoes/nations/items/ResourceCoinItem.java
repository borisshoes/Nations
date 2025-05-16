package net.borisshoes.nations.items;

import eu.pb4.polymer.core.api.item.PolymerItem;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.DamageResistantComponent;
import net.minecraft.component.type.DyedColorComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;

import static net.borisshoes.nations.Nations.MOD_ID;

public class ResourceCoinItem extends Item implements PolymerItem {
   
   private final int color;
   private final String id;
   
   public ResourceCoinItem(Settings settings, String id, int color){
      super(settings.maxCount(99).rarity(Rarity.RARE)
            .registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(MOD_ID,id)))
            .component(DataComponentTypes.DAMAGE_RESISTANT, new DamageResistantComponent(DamageTypeTags.IS_FIRE))
            .component(DataComponentTypes.DYED_COLOR, new DyedColorComponent(color, false)));
      this.color = color;
      this.id = id;
   }
   
   public int getColor(){
      return color;
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
