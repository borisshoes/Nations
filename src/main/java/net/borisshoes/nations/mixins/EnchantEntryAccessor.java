package net.borisshoes.nations.mixins;

import net.borisshoes.arcananovum.gui.midnightenchanter.MidnightEnchanterGui;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.registry.entry.RegistryEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(targets = "net.borisshoes.arcananovum.gui.midnightenchanter.MidnightEnchanterGui$EnchantEntry")
public interface EnchantEntryAccessor {

   @Invoker("enchantment")
   RegistryEntry<Enchantment> getEnchantment();
   
   @Invoker("level")
   int getLevel();
}
