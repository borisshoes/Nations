package net.borisshoes.nations.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.borisshoes.arcananovum.blocks.ContinuumAnchor;
import net.borisshoes.nations.Nations;
import net.borisshoes.nations.gameplay.NationChunk;
import net.borisshoes.nations.land.NationsLand;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.block.*;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

@Mixin(ServerWorld.class)
public class ServerWorldMixin {
   
   @ModifyVariable(method = "tickChunk", at = @At("HEAD"), ordinal = 0, argsOnly = true)
   private int nations_modifyGrowthTicks(int original, WorldChunk chunk, int tickSpeed){
      ServerWorld serverWorld = (ServerWorld)(Object)this;
      int ticks = NationsLand.getRandomTickCount(serverWorld.getRegistryKey(),chunk.getPos());
      if(ticks == 0){
         return original;
      }else{
         return ticks;
      }
   }
   
   @ModifyExpressionValue(method = "tickChunk", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/BlockState;hasRandomTicks()Z"))
   private boolean nations_stopGrowthTicks(boolean original, WorldChunk chunk, int randomTickSpeed, @Local BlockState blockState){
      if(!original) return false;
      ChunkPos chunkPos = chunk.getPos();
      if(blockState.getBlock() instanceof PlantBlock && !(blockState.getBlock() instanceof SaplingBlock)) return NationsLand.growthTicksEnabled(chunk.getWorld().getRegistryKey(),chunkPos);
      if(blockState.getBlock() instanceof CactusBlock) return NationsLand.growthTicksEnabled(chunk.getWorld().getRegistryKey(),chunkPos);
      if(blockState.getBlock() instanceof SugarCaneBlock) return NationsLand.growthTicksEnabled(chunk.getWorld().getRegistryKey(),chunkPos);
      if(blockState.getBlock() instanceof CocoaBlock) return NationsLand.growthTicksEnabled(chunk.getWorld().getRegistryKey(),chunkPos);
      if(blockState.getBlock() instanceof PointedDripstoneBlock) return NationsLand.growthTicksEnabled(chunk.getWorld().getRegistryKey(),chunkPos);
      return true;
   }
   
   @Inject(method = "tick", at = @At(value = "INVOKE", target = "Ljava/util/List;isEmpty()Z"))
   private void nations_resetWorldIdle(BooleanSupplier shouldKeepTicking, CallbackInfo ci){
      ServerWorld serverWorld = (ServerWorld)(Object)this;
      if(Nations.getChunks().stream().anyMatch(NationChunk::isAnchored)){
         serverWorld.resetIdleTimeout();
      }
   }
   
   @ModifyReturnValue(method = "isChunkLoaded", at = @At("RETURN"))
   private boolean nations_isChunkLoaded(boolean original, long chunkPos){
      ServerWorld serverWorld = (ServerWorld)(Object)this;
      if(!original && serverWorld.getRegistryKey().equals(ServerWorld.OVERWORLD) && Nations.isChunkAnchored(new ChunkPos(chunkPos))){
         return true;
      }
      return original;
   }
   
   @ModifyReturnValue(method = "shouldTick(Lnet/minecraft/util/math/ChunkPos;)Z", at = @At("RETURN"))
   private boolean nations_shouldTick(boolean original, ChunkPos chunkPos){
      ServerWorld serverWorld = (ServerWorld)(Object)this;
      if(!original && serverWorld.getRegistryKey().equals(ServerWorld.OVERWORLD) && Nations.isChunkAnchored(chunkPos)){
         return true;
      }
      return original;
   }
   
   @ModifyReturnValue(method = "shouldTickEntity(Lnet/minecraft/util/math/BlockPos;)Z", at = @At("RETURN"))
   private boolean nations_shouldTickEntity(boolean original, BlockPos pos){
      ServerWorld serverWorld = (ServerWorld)(Object)this;
      if(!original && serverWorld.getRegistryKey().equals(ServerWorld.OVERWORLD) && Nations.isChunkAnchored(serverWorld.getChunk(pos).getPos())){
         return true;
      }
      return original;
   }
   
   @ModifyReturnValue(method = "shouldTick(Lnet/minecraft/util/math/BlockPos;)Z", at = @At("RETURN"))
   private boolean nations_shouldTick(boolean original, BlockPos pos){
      ServerWorld serverWorld = (ServerWorld)(Object)this;
      if(!original && serverWorld.getRegistryKey().equals(ServerWorld.OVERWORLD) && Nations.isChunkAnchored(serverWorld.getChunk(pos).getPos())){
         return true;
      }
      return original;
   }
   
   @ModifyReturnValue(method = "shouldTickBlocksInChunk", at = @At("RETURN"))
   private boolean nations_shouldTickBlocksInChunk(boolean original, long chunkPos){
      ServerWorld serverWorld = (ServerWorld)(Object)this;
      if(!original && serverWorld.getRegistryKey().equals(ServerWorld.OVERWORLD) && Nations.isChunkAnchored(new ChunkPos(chunkPos))){
         return true;
      }
      return original;
   }
   
   @ModifyExpressionValue(method = "shouldTickEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ChunkTicketManager;shouldTickEntities(J)Z"))
   private boolean nations_injectedChunkManagerCall(boolean original, BlockPos pos){
      ServerWorld serverWorld = (ServerWorld)(Object)this;
      if(!original && serverWorld.getRegistryKey().equals(ServerWorld.OVERWORLD) && Nations.isChunkAnchored(new ChunkPos(pos))) return true;
      return original;
   }
}
