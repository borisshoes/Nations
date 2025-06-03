package net.borisshoes.nations.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.borisshoes.arcananovum.utils.ArcanaItemUtils;
import net.borisshoes.nations.Nations;
import net.borisshoes.nations.NationsConfig;
import net.borisshoes.nations.NationsRegistry;
import net.borisshoes.nations.gameplay.Nation;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ItemStack.class)
public class ItemStackMixin {
   
   @ModifyExpressionValue(method = "calculateDamage", at = @At(value = "INVOKE", target = "Lnet/minecraft/enchantment/EnchantmentHelper;getItemDamage(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/item/ItemStack;I)I"))
   private int nations_overdamageStack(int original, int baseDamage, ServerWorld world, @Nullable ServerPlayerEntity player){
      ItemStack stack = (ItemStack) (Object) this;
      ItemEnchantmentsComponent enchants = stack.getEnchantments();
      int extra = NationsConfig.getInt(NationsRegistry.STACK_OVERDAMAGE_CFG);
      int count = 1;
      if(ArcanaItemUtils.isArcane(stack)) return original;
      if(!NationsRegistry.LOCKED_ITEMS.containsKey(stack.getItem())) return original;
      if(!(player instanceof ServerPlayerEntity serverPlayer)) return original;
      Nation nation = Nations.getNation(serverPlayer);
      if(nation == null) return original;
      for(RegistryEntry<Enchantment> enchantment : enchants.getEnchantments()){
         if(!nation.canEnchant(enchantment.getKey().get(),enchants.getLevel(enchantment))){
            count++;
         }
      }
      return nation.canCraft(stack.getItem()) ? original : original + (extra*count);
   }
}
