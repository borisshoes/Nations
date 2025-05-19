package net.borisshoes.nations.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.borisshoes.arcananovum.blocks.forge.StarlightForge;
import net.borisshoes.arcananovum.blocks.forge.StarlightForgeBlockEntity;
import net.borisshoes.arcananovum.gui.starlightforge.StarlightForgeGui;
import net.borisshoes.nations.Nations;
import net.borisshoes.nations.NationsRegistry;
import net.borisshoes.nations.gameplay.Nation;
import net.borisshoes.nations.gameplay.NationChunk;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Optional;

@Mixin(StarlightForgeGui.class)
public class StarlightForgeGuiMixin {

   @Final
   @Shadow
   private StarlightForgeBlockEntity blockEntity;
   
   @ModifyExpressionValue(method = "onAnyClick", at = @At(value = "INVOKE", target = "Lnet/borisshoes/arcananovum/gui/starlightforge/StarlightForgeInventoryListener;getEnhancedStack(Lnet/minecraft/inventory/Inventory;)Lnet/minecraft/item/ItemStack;"))
   private ItemStack nations_stopEnhancingEquipment(ItemStack original){
      if(original.isEmpty()) return original;
      Item item = original.getItem();
      if(!NationsRegistry.LOCKED_ITEMS.containsKey(item)) return original;
      if(!blockEntity.getWorld().getRegistryKey().equals(ServerWorld.OVERWORLD)) return ItemStack.EMPTY;
      ChunkPos chunkPos = new ChunkPos(blockEntity.getPos());
      NationChunk nationChunk = Nations.getChunk(chunkPos);
      if(nationChunk == null) return ItemStack.EMPTY;
      Nation nation = nationChunk.getControllingNation();
      if(nation == null || !nationChunk.isClaimed()) return ItemStack.EMPTY;
      return nation.canCraft(item) ? original : ItemStack.EMPTY;
   }
}
