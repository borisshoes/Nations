package net.borisshoes.nations.mixins;

import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.border.WorldBorderListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {
   
   @Redirect(method = "createWorlds", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/border/WorldBorder;addListener(Lnet/minecraft/world/border/WorldBorderListener;)V"))
   private void nations_decoupleBorders(WorldBorder instance, WorldBorderListener listener){
      // Blank Redirect
   }
   
   @Redirect(method = "createWorlds", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/PlayerManager;setMainWorld(Lnet/minecraft/server/world/ServerWorld;)V"))
   private void nations_decoupleBorderListeners(PlayerManager instance, ServerWorld world){
      MinecraftServer server = (MinecraftServer)(Object) this;
      for(ServerWorld serverWorld : server.getWorlds()){
         serverWorld.getWorldBorder().addListener(new WorldBorderListener() {
            @Override
            public void onSizeChange(WorldBorder border, double size) {
               instance.sendToAll(new WorldBorderSizeChangedS2CPacket(border));
            }
            
            @Override
            public void onInterpolateSize(WorldBorder border, double fromSize, double toSize, long time) {
               instance.sendToAll(new WorldBorderInterpolateSizeS2CPacket(border));
            }
            
            @Override
            public void onCenterChanged(WorldBorder border, double centerX, double centerZ) {
               instance.sendToAll(new WorldBorderCenterChangedS2CPacket(border));
            }
            
            @Override
            public void onWarningTimeChanged(WorldBorder border, int warningTime) {
               instance.sendToAll(new WorldBorderWarningTimeChangedS2CPacket(border));
            }
            
            @Override
            public void onWarningBlocksChanged(WorldBorder border, int warningBlockDistance) {
               instance.sendToAll(new WorldBorderWarningBlocksChangedS2CPacket(border));
            }
            
            @Override
            public void onDamagePerBlockChanged(WorldBorder border, double damagePerBlock) {
            }
            
            @Override
            public void onSafeZoneChanged(WorldBorder border, double safeZoneRadius) {
            }
         });
      }
   }
}
