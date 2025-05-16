package net.borisshoes.nations.land;

import net.minecraft.server.network.ServerPlayerEntity;

public interface ItemUseBlockFlags {
   void stopCanUseBlocks(boolean flag);
   
   void stopCanUseItems(boolean flag);
   
   boolean allowUseBlocks();
   
   boolean allowUseItems();
   
   static ItemUseBlockFlags fromPlayer(ServerPlayerEntity player) {
      return (ItemUseBlockFlags) player.interactionManager;
   }
}
