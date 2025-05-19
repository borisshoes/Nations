package net.borisshoes.nations.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.borisshoes.arcananovum.ArcanaRegistry;
import net.borisshoes.arcananovum.core.ArcanaItem;
import net.borisshoes.arcananovum.gui.starlightforge.StarlightForgeInventoryListener;
import net.borisshoes.nations.NationsRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

@Mixin(StarlightForgeInventoryListener.class)
public class StarlightForgeInventoryListenerMixin {
   
   @ModifyExpressionValue(method = "validRecipe", at = @At(value = "INVOKE", target = "Lnet/borisshoes/arcananovum/recipes/arcana/ArcanaRecipe;satisfiesRecipe([[Lnet/minecraft/item/ItemStack;Lnet/borisshoes/arcananovum/blocks/forge/StarlightForgeBlockEntity;)Z"))
   private boolean nations_stopArcanaCraft(boolean original, @Local(ordinal = 1) ArcanaItem item){
      if(!original) return false;
      if(item.getId().equals(ArcanaRegistry.STARLIGHT_FORGE.getId())) return false;
      if(item.getId().equals(ArcanaRegistry.RADIANT_FLETCHERY.getId())) return false;
      if(item.getId().equals(ArcanaRegistry.MIDNIGHT_ENCHANTER.getId())) return false;
      if(item.getId().equals(ArcanaRegistry.TWILIGHT_ANVIL.getId())) return false;
      if(item.getId().equals(ArcanaRegistry.STELLAR_CORE.getId())) return false;
      if(item.getId().equals(ArcanaRegistry.ARCANE_SINGULARITY.getId())) return false;
      if(item.getId().equals(ArcanaRegistry.TRANSMUTATION_ALTAR.getId())) return false;
      if(item.getId().equals(ArcanaRegistry.STORMCALLER_ALTAR.getId())) return false;
      if(item.getId().equals(ArcanaRegistry.CELESTIAL_ALTAR.getId())) return false;
      if(item.getId().equals(ArcanaRegistry.STARPATH_ALTAR.getId())) return false;
      return true;
   }
}
