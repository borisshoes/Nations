package net.borisshoes.nations.land;

import net.borisshoes.nations.mixins.LecternBlockEntityAccessor;
import net.fabricmc.fabric.api.screenhandler.v1.FabricScreenHandlerFactory;
import net.minecraft.block.entity.LecternBlockEntity;
import net.minecraft.client.gui.screen.ingame.LecternScreen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.screen.LecternScreenHandler;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

public class LockedLecternScreenHandler extends LecternScreenHandler {
   private final BlockPos blockPos;
   
   public LockedLecternScreenHandler(int syncId, Inventory inventory, PropertyDelegate propertyDelegate, BlockPos blockPos) {
      super(syncId, inventory, propertyDelegate);
      this.blockPos = blockPos;
   }
   
   public static void create(ServerPlayerEntity player, LecternBlockEntity lectern, BlockPos blockPos) {
      NamedScreenHandlerFactory fac = new NamedScreenHandlerFactory() {
         
         @Override
         public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player){
            return new LockedLecternScreenHandler(syncId, ((LecternBlockEntityAccessor) lectern).getInv(), ((LecternBlockEntityAccessor) lectern).getProp(), blockPos);
         }
         
         @Override
         public Text getDisplayName() {
            return lectern.getDisplayName();
         }
      };
      player.openHandledScreen(fac);
   }
   
   @Override
   public boolean onButtonClick(PlayerEntity player, int id){
      if (id == 3 && player instanceof ServerPlayerEntity serverPlayer) {
         NationsLand.sendPermissionMessage(serverPlayer, this.blockPos);
         return false;
      }
      return super.onButtonClick(player, id);
   }
}
