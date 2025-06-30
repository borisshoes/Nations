package net.borisshoes.nations.land;

import net.borisshoes.nations.Nations;
import net.borisshoes.nations.NationsConfig;
import net.borisshoes.nations.NationsRegistry;
import net.borisshoes.nations.gameplay.CapturePoint;
import net.borisshoes.nations.gameplay.Nation;
import net.borisshoes.nations.gameplay.NationChunk;
import net.borisshoes.nations.gameplay.StructurePlacer;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.LecternBlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.Tameable;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.*;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameRules;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

import static net.borisshoes.nations.Nations.*;
import static net.borisshoes.nations.cca.PlayerComponentInitializer.PLAYER_DATA;

public class NationsLand {
   public static final Identifier MINING_SPEED_MOD = Identifier.of(MOD_ID,"mining_speed_modifier");
   
   public static boolean stopBonemealGrow(World world, BlockPos pos, ServerPlayerEntity player, int range, boolean message){
      if(!world.getRegistryKey().equals(ServerWorld.OVERWORLD)) return false;
      if(PLAYER_DATA.get(player).bypassesClaims()) return false;
      ChunkPos chunkPos = new ChunkPos(pos);
      if (range > 0) {
         for(int i = -1; i <= 1; i++){
            for(int j = -1; j <= 1; j++){
               ChunkPos chunkPos1 = new ChunkPos(chunkPos.x+i, chunkPos.z+i);
               if(Nations.isClaimedAgainst(chunkPos1,player) || isSpawnChunk(pos)){
                  if(message) NationsLand.sendPermissionMessage(player, pos);
                  return true;
               }
            }
         }
      }
      return false;
   }
   
   public static boolean canFrostwalkerFreeze(World world, BlockPos pos, Entity entity, boolean message){
      if(!world.getRegistryKey().equals(ServerWorld.OVERWORLD)) return true;
      if (entity instanceof ServerPlayerEntity player) {
         if(PLAYER_DATA.get(player).bypassesClaims()) return true;
         ChunkPos chunkPos = new ChunkPos(pos);
         boolean isClaimed = Nations.isClaimedAgainst(chunkPos, player);
         if((isClaimed || isSpawnChunk(pos)) && message) NationsLand.sendPermissionMessage(player, pos);
         return !isClaimed;
      }
      return true;
   }
   
   public static boolean cancelEntityBlockCollision(World world, BlockPos pos, Entity entity, BlockState blockState, boolean message){
      if(!world.getRegistryKey().equals(ServerWorld.OVERWORLD)) return false;
      Entity causingEntity = entity;
      if(entity instanceof ProjectileEntity proj) causingEntity = proj.getOwner();
      if(entity instanceof ItemEntity itemEntity) causingEntity = itemEntity.getOwner();
      if(causingEntity instanceof ServerPlayerEntity player){
         if(PLAYER_DATA.get(player).bypassesClaims()) return false;
         if(blockState.getBlock() instanceof AbstractPressurePlateBlock){
            ChunkPos chunkPos = new ChunkPos(pos);
            boolean isClaimed = Nations.isClaimedAgainst(chunkPos, player);
            if(isClaimed && message) NationsLand.sendPermissionMessage(player, pos);
            return isClaimed;
         }
         return false;
      }else{
         return false;
      }
   }
   
   public static boolean canBreakTurtleEgg(World world, BlockPos pos, Entity entity, boolean message){
      if(!world.getRegistryKey().equals(ServerWorld.OVERWORLD)) return true;
      if(isSpawnChunk(pos)){
         if(entity instanceof ServerPlayerEntity player){
            if(PLAYER_DATA.get(player).bypassesClaims()) return true;
         }
         if(message && entity instanceof ServerPlayerEntity player) sendPermissionMessage(player,pos);
         return false;
      }
      ChunkPos chunkPos = new ChunkPos(pos);
      Entity causingEntity = entity;
      if(entity instanceof ProjectileEntity proj) causingEntity = proj.getOwner();
      if(entity instanceof ItemEntity itemEntity) causingEntity = itemEntity.getOwner();
      if(causingEntity instanceof ServerPlayerEntity player){
         if(PLAYER_DATA.get(player).bypassesClaims()) return true;
         boolean isClaimed = Nations.isClaimedAgainst(chunkPos, player);
         if(isClaimed && message) NationsLand.sendPermissionMessage(player, pos);
         return !isClaimed;
      }
      boolean isClaimed = !Nations.isClaimed(chunkPos).isEmpty();
      return !isClaimed;
   }
   
   public static boolean canTeleportDragonEgg(World world, ServerPlayerEntity player, BlockPos pos, boolean message){
      if(!world.getRegistryKey().equals(ServerWorld.OVERWORLD)) return true;
      if(PLAYER_DATA.get(player).bypassesClaims()) return true;
      if(isSpawnChunk(pos)){
         if(message) sendPermissionMessage(player,pos);
         return false;
      }
      if(Nations.isClaimedAgainst(new ChunkPos(pos),player)){
         if(message) sendPermissionMessage(player,pos);
         return false;
      }
      return true;
   }
   
   public static boolean cannotLandOn(World world, Entity entity, BlockPos pos, BlockState state){
      if(!world.getRegistryKey().equals(ServerWorld.OVERWORLD)) return false;
      ChunkPos chunkPos = new ChunkPos(pos);
      if (entity instanceof ServerPlayerEntity player) {
         if(PLAYER_DATA.get(player).bypassesClaims()) return false;
         boolean isClaimed = Nations.isClaimedAgainst(chunkPos, player);
         boolean isInfluenced = Nations.isInfluencedAgainst(chunkPos, player);
         if(isSpawnChunk(pos)) return true;
         if(!isClaimed && !isInfluenced) return false;
         
         // Handled elsewhere
         if(!(state.getBlock() instanceof TurtleEggBlock || state.getBlock() instanceof FarmlandBlock)) return false;
         return (isClaimed && state.isIn(NationsRegistry.CLAIM_PROTECTED_BLOCKS)) || (isInfluenced && state.isIn(NationsRegistry.INFLUENCE_PROTECTED_BLOCKS));
      } else if (entity instanceof ProjectileEntity) {
         Entity owner = ((ProjectileEntity) entity).getOwner();
         if (owner instanceof ServerPlayerEntity player) {
            if(PLAYER_DATA.get(player).bypassesClaims()) return false;
            boolean isClaimed = Nations.isClaimedAgainst(chunkPos, player);
            boolean isInfluenced = Nations.isInfluencedAgainst(chunkPos, player);
            
            // Handled elsewhere
            if(!(state.getBlock() instanceof TurtleEggBlock || state.getBlock() instanceof FarmlandBlock)) return false;
            return (isClaimed && state.isIn(NationsRegistry.CLAIM_PROTECTED_BLOCKS)) || (isInfluenced && state.isIn(NationsRegistry.INFLUENCE_PROTECTED_BLOCKS));
         }
      }
      return false;
   }
   
   public static boolean canUseAtEntity(World world, ServerPlayerEntity player, Entity entity, boolean message){
      if(PLAYER_DATA.get(player).bypassesClaims()) return true;
      if(entity instanceof Monster) return true;
      if(!world.getRegistryKey().equals(ServerWorld.OVERWORLD)) return true;
      BlockPos pos = entity.getBlockPos();
      ChunkPos chunkPos = new ChunkPos(pos);
      boolean isClaimed = Nations.isClaimedAgainst(chunkPos, player);
      boolean isInfluenced = Nations.isInfluencedAgainst(chunkPos, player);
      if(!isClaimed && !isInfluenced) return true;
      
      if(isClaimed){
         if(entity instanceof ArmorStandEntity || entity instanceof MobEntity){
            if(message) sendPermissionMessage(player,pos);
            return false;
         }
      }
      return true;
   }
   
   public static boolean canUseEntity(World world, ServerPlayerEntity player, Entity entity, boolean message){
      if(PLAYER_DATA.get(player).bypassesClaims()) return true;
      BlockPos pos = entity.getBlockPos();
      if(entity instanceof MerchantEntity){
         if(message) sendPermissionMessage(player,pos);
         return false;
      }
      if(entity instanceof Monster) return true;
      if(!world.getRegistryKey().equals(ServerWorld.OVERWORLD)) return true;
      ChunkPos chunkPos = new ChunkPos(pos);
      boolean isClaimed = Nations.isClaimedAgainst(chunkPos, player);
      boolean isInfluenced = Nations.isInfluencedAgainst(chunkPos, player);
      if(!isClaimed && !isInfluenced) return true;
      
      if(isClaimed){
         if(entity instanceof Tameable tame){
            if (tame.getOwnerUuid() != null && tame.getOwnerUuid().equals(player.getUuid()))
               return true;
         }
         if(message) sendPermissionMessage(player,pos);
         return false;
      }
      return true;
   }
   
   public static boolean canUseArrowOnBlock(World world, ServerPlayerEntity player, BlockPos pos, BlockState state, boolean message){
      if(!world.getRegistryKey().equals(ServerWorld.OVERWORLD)) return false;
      if(PLAYER_DATA.get(player).bypassesClaims()) return false;
      ChunkPos chunkPos = new ChunkPos(pos);
      boolean isClaimed = Nations.isClaimedAgainst(chunkPos, player);
      boolean isInfluenced = Nations.isInfluencedAgainst(chunkPos, player);
      if(!isClaimed && !isInfluenced) return false;
      
      boolean needsHandling = (isClaimed && state.isIn(NationsRegistry.CLAIM_PROTECTED_BLOCKS)) || (isInfluenced && state.isIn(NationsRegistry.INFLUENCE_PROTECTED_BLOCKS));
      if(needsHandling){
         if(message) sendPermissionMessage(player,pos);
      }
      return needsHandling;
   }
   
   public static boolean canPlaceBlock(World world, ServerPlayerEntity player, BlockPos pos, ItemStack stack, boolean message){
      if(isOutOfBounds(world.getRegistryKey(),pos)) return false;
      if(world.getRegistryKey().equals(NationsRegistry.CONTEST_DIM) && (stack.isOf(Items.END_CRYSTAL) || stack.isOf(Items.RESPAWN_ANCHOR))){
         if(message) sendPermissionMessage(player,pos);
         return false;
      }
      if(!world.getRegistryKey().equals(ServerWorld.OVERWORLD)) return true;
      if(PLAYER_DATA.get(player).bypassesClaims()) return true;
      ChunkPos chunkPos = new ChunkPos(pos);
      boolean isClaimed = Nations.isClaimedAgainst(chunkPos, player);
      boolean isCap = NationsLand.isCapturePointBlock(pos);
      boolean isNationCenter = NationsLand.isNationCenterBlock(pos);
      boolean isSpawn = NationsLand.isSpawnChunk(pos);
      if(!isClaimed && !isCap && !isNationCenter && !isSpawn) return true;
      
      if(message) sendPermissionMessage(player, pos);
      return false;
   }
   
   public static boolean canUseItemOnBlock(World world, ServerPlayerEntity player, BlockPos pos, ItemStack stack, boolean message){
      if(isOutOfBounds(world.getRegistryKey(),pos)) return false;
      if(world.getRegistryKey().equals(NationsRegistry.CONTEST_DIM) && (stack.isOf(Items.END_CRYSTAL) || stack.isOf(Items.RESPAWN_ANCHOR))){
         if(message) sendPermissionMessage(player,pos);
         return false;
      }
      if(!world.getRegistryKey().equals(ServerWorld.OVERWORLD)) return true;
      if(PLAYER_DATA.get(player).bypassesClaims()) return true;
      ChunkPos chunkPos = new ChunkPos(pos);
      boolean isClaimed = Nations.isClaimedAgainst(chunkPos, player);
      boolean isInfluenced = Nations.isInfluencedAgainst(chunkPos, player);
      boolean isCap = NationsLand.isCapturePointBlock(pos);
      boolean isNationCenter = NationsLand.isNationCenterBlock(pos);
      boolean isSpawn = NationsLand.isSpawnChunk(pos);
      if(!isClaimed && !isInfluenced && !isCap && !isNationCenter && !isSpawn) return true;
      boolean shouldStop = (isClaimed || isSpawn);
      
      boolean needsHandling = (shouldStop && stack.isIn(NationsRegistry.CLAIM_PROTECTED_ITEMS)) || (isInfluenced && stack.isIn(NationsRegistry.INFLUENCE_PROTECTED_ITEMS));
      if(!needsHandling && stack.contains(DataComponentTypes.JUKEBOX_PLAYABLE)){
         needsHandling = shouldStop;
      }
      if(needsHandling || shouldStop){
         if(message) sendPermissionMessage(player,pos);
         return false;
      }
      return true;
   }
   
   public static boolean canUseItem(World world, ServerPlayerEntity player, ItemStack stack, Hand hand, boolean message){
      BlockPos pos = player.getBlockPos();
      if(world.getRegistryKey().equals(NationsRegistry.CONTEST_DIM) && (stack.isOf(Items.END_CRYSTAL) || stack.isOf(Items.RESPAWN_ANCHOR))){
         if(message) sendPermissionMessage(player,pos);
         return false;
      }
      if(!world.getRegistryKey().equals(ServerWorld.OVERWORLD)) return true;
      if(PLAYER_DATA.get(player).bypassesClaims()) return true;
      BlockHitResult hitResult = getPlayerHitResult(world, player, RaycastContext.FluidHandling.SOURCE_ONLY);
      if (hitResult.getType() == HitResult.Type.BLOCK) {
         pos = new ItemPlacementContext(player, hand, stack, hitResult).getBlockPos();
      }
      ChunkPos chunkPos = new ChunkPos(pos);
      boolean isClaimed = Nations.isClaimedAgainst(chunkPos, player);
      boolean isInfluenced = Nations.isInfluencedAgainst(chunkPos, player);
      boolean isCap = NationsLand.isCapturePointBlock(pos);
      boolean isNationCenter = NationsLand.isNationCenterBlock(pos);
      boolean isSpawn = NationsLand.isSpawnChunk(pos);
      if(!isClaimed && !isInfluenced && !isCap && !isNationCenter && !isSpawn) return true;
      boolean shouldStop = (isClaimed || isNationCenter || isCap || isSpawn);
      
      boolean needsHandling = (shouldStop && stack.isIn(NationsRegistry.CLAIM_PROTECTED_ITEMS)) || (isInfluenced && stack.isIn(NationsRegistry.INFLUENCE_PROTECTED_ITEMS));
      if(needsHandling){
         if(message) sendPermissionMessage(player,pos);
         return false;
      }
      if(shouldStop && (stack.getItem() instanceof BoneMealItem || stack.getItem() == Items.LILY_PAD)){
         BlockPos update = pos;
         if(stack.getItem() == Items.LILY_PAD){
            BlockHitResult upResult = hitResult.withBlockPos(hitResult.getBlockPos().up());
            update = new ItemPlacementContext(new ItemUsageContext(player, hand, upResult)).getBlockPos();
         }
         player.networkHandler.sendPacket(new BlockUpdateS2CPacket(update, world.getBlockState(update)));
         if(message) sendPermissionMessage(player,pos);
         return false;
      }
      return true;
   }
   
   public static boolean canBreakBlock(World world, ServerPlayerEntity player, BlockPos pos, boolean message){
      if(world.getBlockState(pos).getRegistryEntry().getIdAsString().contains("universal_graves")) return true;
      if(isOutOfBounds(world.getRegistryKey(),pos)) return false;
      if(!world.getRegistryKey().equals(ServerWorld.OVERWORLD)) return true;
      if(PLAYER_DATA.get(player).bypassesClaims()) return true;
      if(isSpawnChunk(pos)){
         if(message) sendPermissionMessage(player,pos);
         return false;
      }
      if(Nations.isClaimedAgainst(new ChunkPos(pos),player)){
         if(message) sendPermissionMessage(player,pos);
         return false;
      }
      if(NationsLand.isCapturePointBlock(pos) || NationsLand.isNationCenterBlock(pos)){
         if(message) sendPermissionMessage(player,pos);
         return false;
      }
      return true;
   }
   
   public static boolean canUseOtherBlocks(World world, ServerPlayerEntity player, ItemStack stack, BlockPos pos, boolean message){
      if(world.getBlockState(pos).getRegistryEntry().getIdAsString().contains("universal_graves")) return true;
      if(isOutOfBounds(world.getRegistryKey(),pos)) return false;
      if(world.getRegistryKey().equals(NationsRegistry.CONTEST_DIM) && (stack.isOf(Items.END_CRYSTAL) || stack.isOf(Items.RESPAWN_ANCHOR))){
         if(message) sendPermissionMessage(player,pos);
         return false;
      }
      if(!world.getRegistryKey().equals(ServerWorld.OVERWORLD)) return true;
      if(PLAYER_DATA.get(player).bypassesClaims()) return true;
      if(isSpawnChunk(pos)){
         if(message) sendPermissionMessage(player,pos);
         return false;
      }
      if(isWartime()) return true;
      ChunkPos chunkPos = new ChunkPos(pos);
      boolean isClaimed = Nations.isClaimedAgainst(chunkPos, player);
      boolean isInfluenced = Nations.isInfluencedAgainst(chunkPos, player);
      BlockState state = world.getBlockState(pos);
      BlockEntity blockEntity = world.getBlockEntity(pos);
      
      if(state.isOf(Blocks.ENCHANTING_TABLE)) return false;
      if(!isClaimed && !isInfluenced) return true;
      boolean needsHandling = (isClaimed && state.isIn(NationsRegistry.CLAIM_PROTECTED_BLOCKS)) || (isInfluenced && state.isIn(NationsRegistry.INFLUENCE_PROTECTED_BLOCKS));
      if (!(state.getBlock() instanceof AbstractPressurePlateBlock) && needsHandling) {
         if (state.getBlock() instanceof DoorBlock) {
            DoubleBlockHalf half = state.get(DoorBlock.HALF);
            if (half == DoubleBlockHalf.LOWER) {
               BlockState other = world.getBlockState(pos.up());
               player.networkHandler.sendPacket(new BlockUpdateS2CPacket(pos.up(), other));
            } else {
               BlockState other = world.getBlockState(pos.down());
               player.networkHandler.sendPacket(new BlockUpdateS2CPacket(pos.down(), other));
            }
         }
         executeSignCommand(blockEntity, pos, player);
         if(message) sendPermissionMessage(player,pos);
         return false;
      }
      if (blockEntity != null && !(player.shouldCancelInteraction() && !stack.isEmpty())) {
         if (blockEntity instanceof LecternBlockEntity) {
            if (!isClaimed)
               return true;
            if (state.get(LecternBlock.HAS_BOOK))
               LockedLecternScreenHandler.create(player, (LecternBlockEntity) blockEntity, pos);
            if(message) sendPermissionMessage(player,pos);
            return false;
         }
         if (blockEntity instanceof SignBlockEntity) {
            if (!isClaimed)
               return true;
            if(message) sendPermissionMessage(player,pos);
            return false;
         }
         if (!isClaimed)
            return true;
         if(message) sendPermissionMessage(player,pos);
         return false;
      }
      if (isClaimed) {
         executeSignCommand(blockEntity, pos, player);
         if(message) sendPermissionMessage(player,pos);
         return false;
      }else{
         return true;
      }
   }
   
   public static boolean canDamage(ServerPlayerEntity player, Entity entity, boolean message){
      if(!entity.getWorld().getRegistryKey().equals(ServerWorld.OVERWORLD)) return true;
      if(PLAYER_DATA.get(player).bypassesClaims()) return true;
      BlockPos pos = entity.getBlockPos();
      NationChunk nationChunk = Nations.getChunk(new ChunkPos(pos));
      if(nationChunk != null && nationChunk.isArena()) return true;
      if(entity instanceof Monster && !entity.hasCustomName()) return true;
      if(isSpawnChunk(pos)){
         if(message) sendPermissionMessage(player,pos);
         return false;
      }
      if(Nations.isWartime() && entity instanceof ServerPlayerEntity) return true;
      
      if(entity instanceof ServerPlayerEntity otherPlayer){
         if(Nations.isInfluencedAgainst(new ChunkPos(pos),player)){
            if(message) sendPermissionMessage(player,pos);
            return false;
         }
      }else{ // Is animal or is named
         if(Nations.isInfluencedAgainst(new ChunkPos(pos),player)){
            if(message) sendPermissionMessage(player,pos);
            return false;
         }
      }
      return true;
   }
   
   public static boolean canExplodeBlocks(World world, BlockPos pos){
      if(!world.getRegistryKey().equals(ServerWorld.OVERWORLD)) return true;
      if(isSpawnChunk(pos) || isCapturePointBlock(pos) || isNationCenterBlock(pos)){
         return false;
      }
      if(!Nations.isClaimed(new ChunkPos(pos)).isEmpty()){
         NationChunk nationChunk = Nations.getChunk(new ChunkPos(pos));
         if(nationChunk != null && nationChunk.isClaimed() && nationChunk.areExplosionsOverridden()) return true;
         return false;
      }
      return true;
   }
   
   public static boolean canExplosionDamage(Entity entity){
      if(!entity.getWorld().getRegistryKey().equals(ServerWorld.OVERWORLD)) return true;
      BlockPos pos = entity.getBlockPos();
      if(isSpawnChunk(pos)){
         return false;
      }
      if(Nations.isWartime() && entity instanceof ServerPlayerEntity) return true;
      if(!Nations.isClaimed(new ChunkPos(pos)).isEmpty()){
         return false;
      }
      return true;
   }
   
   public static boolean canWitherDestroy(World world, BlockPos pos){
      if(!world.getRegistryKey().equals(ServerWorld.OVERWORLD)) return true;
      if(isSpawnChunk(pos)){
         return false;
      }
      if(!Nations.isClaimed(new ChunkPos(pos)).isEmpty()){
         return false;
      }
      if(isCapturePointBlock(pos)){
         return false;
      }
      return true;
   }
   
   public static boolean canPistonPush(World world, BlockPos from, BlockPos to, BlockPos opposite){
      if(!world.getRegistryKey().equals(ServerWorld.OVERWORLD)) return true;
      String fromClaim = Nations.isClaimed(new ChunkPos(from));
      String toClaim = Nations.isClaimed(new ChunkPos(to));
      String oppClaim = Nations.isClaimed(new ChunkPos(opposite));
      
      if(!isSpawnChunk(from) && (isSpawnChunk(to) || isSpawnChunk(opposite))){
         return false;
      }
      if(fromClaim.equals(toClaim) && !fromClaim.equals(oppClaim) && !oppClaim.isEmpty()){
         return false;
      }
      if(!fromClaim.equals(toClaim) && !world.getBlockState(from).isAir() && (!fromClaim.isEmpty() || !toClaim.isEmpty())){
         return false;
      }
      if(NationsLand.isCapturePointBlock(from) || NationsLand.isNationCenterBlock(from)){
         return false;
      }
      if(NationsLand.isCapturePointBlock(to) || NationsLand.isNationCenterBlock(to)){
         return false;
      }
      if(NationsLand.isCapturePointBlock(opposite) || NationsLand.isNationCenterBlock(opposite)){
         return false;
      }
      return true;
   }
   
   public static boolean canFlow(World world, BlockPos from, BlockPos to){
      if(!world.getRegistryKey().equals(ServerWorld.OVERWORLD)) return true;
      String fromClaim = Nations.isClaimed(new ChunkPos(from));
      String toClaim = Nations.isClaimed(new ChunkPos(to));
      return toClaim.isEmpty() || toClaim.equals(fromClaim);
   }
   
   public static boolean canFireSpread(World world, BlockPos pos){
      if(!world.getRegistryKey().equals(ServerWorld.OVERWORLD)) return true;
      if(isSpawnChunk(pos)){
         return false;
      }
      return Nations.isClaimed(new ChunkPos(pos)).isEmpty();
   }
   
   public static void sendPermissionMessage(ServerPlayerEntity player, BlockPos pos){
      player.sendMessage(Text.translatable("text.nations.claim_warning").formatted(Formatting.RED,Formatting.ITALIC),true);
   }
   
   public static boolean isBeaconPos(World world, BlockPos pos){
      if(!world.getRegistryKey().equals(ServerWorld.OVERWORLD)) return false;
      ChunkPos chunk = new ChunkPos(pos);
      NationChunk nChunk = Nations.getChunk(chunk);
      if(nChunk == null) return false;
      Nation nation = nChunk.getControllingNation();
      
      CapturePoint cap = Nations.getCapturePoint(chunk);
      if(cap != null){
         BlockPos beaconPos = cap.getBeaconPos();
         if(pos.getX() == beaconPos.getX() && pos.getY() >= beaconPos.getY() && pos.getZ() == beaconPos.getZ()){
            return true;
         }
      }else if(nation != null){
         BlockPos centerPos = nation.getFoundingPos();
         if(!(new ChunkPos(centerPos).equals(chunk))) return false;
         if((pos.getX() == centerPos.getX()+7 || pos.getX() == centerPos.getX()+8) && (pos.getZ() == centerPos.getZ()+7 || pos.getZ() == centerPos.getZ()+8) && pos.getY() > nation.getFoundHeight()){
            return true;
         }
      }
      return false;
   }
   
   public static boolean isCapturePointBlock(BlockPos pos){
      ChunkPos chunk = new ChunkPos(pos);
      NationChunk nChunk = Nations.getChunk(chunk);
      if(nChunk == null) return false;
      CapturePoint cap = Nations.getCapturePoint(chunk);
      if(cap == null) return false;
      
      StructurePlacer.Structure structure = Nations.CAPTURE_POINT_STRUCTURES.get(cap.getType());
      BlockPos origin = chunk.getBlockPos(3,cap.getY(),3);
      int[][][] pattern = structure.statePattern();
      BlockPos offset = pos.subtract(origin);
      int x = pattern.length;
      int y = pattern[0].length;
      int z = pattern[0][0].length;
      if(offset.getX() < 0 || offset.getX() >= x || offset.getY() < 0 || offset.getY() >= y || offset.getZ() < 0 || offset.getZ() >= z) return false;
      if(offset.getX() >= 2 && offset.getX() <= 8 && offset.getZ() >= 2 && offset.getZ() <= 8 && offset.getY() <= 9) return true; // Center area
      int patternInd = pattern[offset.getX()][offset.getY()][offset.getZ()];
      return patternInd != -1 && !structure.blockStates().get(patternInd).isAir();
   }
   
   public static boolean isNationCenterBlock(BlockPos pos){
      ChunkPos chunk = new ChunkPos(pos);
      NationChunk nChunk = Nations.getChunk(chunk);
      if(nChunk == null) return false;
      Nation nation = nChunk.getControllingNation();
      if(nation == null) return false;
      BlockPos centerPos = nation.getFoundingPos();
      if(!(new ChunkPos(centerPos).equals(chunk))) return false;
      
      StructurePlacer.Structure structure = Nations.NATION_STRUCTURE;
      int[][][] pattern = structure.statePattern();
      BlockPos offset = pos.subtract(centerPos);
      int x = pattern.length;
      int y = pattern[0].length;
      int z = pattern[0][0].length;
      if(offset.getX() < 0 || offset.getX() >= x || offset.getY() < 0 || offset.getY() >= y || offset.getZ() < 0 || offset.getZ() >= z) return false;
      int patternInd = pattern[offset.getX()][offset.getY()][offset.getZ()];
      return patternInd != -1 && !structure.blockStates().get(patternInd).isAir();
   }
   
   public static boolean isSpawnDMZChunk(BlockPos pos){
      ChunkPos chunkPos = new ChunkPos(pos);
      int radius = NationsConfig.getInt(NationsRegistry.SPAWN_RADIUS_CFG) + NationsConfig.getInt(NationsRegistry.SPAWN_DMZ_RADIUS_CFG);
      return chunkPos.x >= -radius && chunkPos.x < radius && chunkPos.z >= -radius && chunkPos.z < radius;
   }
   
   public static boolean isSpawnChunk(BlockPos pos){
      ChunkPos chunkPos = new ChunkPos(pos);
      int radius = NationsConfig.getInt(NationsRegistry.SPAWN_RADIUS_CFG);
      return chunkPos.x >= -radius && chunkPos.x < radius && chunkPos.z >= -radius && chunkPos.z < radius;
   }
   
   public static BlockPos moveInBounds(RegistryKey<World> world, BlockPos pos){
      int border;
      if (world.equals(ServerWorld.OVERWORLD)) {
         border = NationsConfig.getInt(NationsRegistry.WORLD_BORDER_RADIUS_OVERWORLD_CFG);
      } else if (world.equals(ServerWorld.NETHER)) {
         border = NationsConfig.getInt(NationsRegistry.WORLD_BORDER_RADIUS_NETHER_CFG);
      } else {
         return pos;
      }
      border *= 16;
      
      int clampedX = MathHelper.clamp(pos.getX(), -border, border);
      int clampedZ = MathHelper.clamp(pos.getZ(), -border, border);
      if (clampedX == pos.getX() && clampedZ == pos.getZ()) {
         return pos;
      } else {
         return new BlockPos(clampedX, pos.getY(), clampedZ);
      }
   }
   
   public static boolean isOutOfBounds(RegistryKey<World> world, ChunkPos pos){
      return isOutOfBounds(world,pos.getBlockPos(8,0,8));
   }
   
   public static boolean isOutOfBounds(RegistryKey<World> world, BlockPos pos){
      int overworldBorder = NationsConfig.getInt(NationsRegistry.WORLD_BORDER_RADIUS_OVERWORLD_CFG);
      int netherBorder = NationsConfig.getInt(NationsRegistry.WORLD_BORDER_RADIUS_NETHER_CFG);
      if(world.equals(ServerWorld.OVERWORLD)){
         return Math.max(Math.abs(pos.getX()), Math.abs(pos.getZ())) > (overworldBorder * 16);
      }else if(world.equals(ServerWorld.NETHER)){
         return Math.max(Math.abs(pos.getX()), Math.abs(pos.getZ())) > (netherBorder * 16);
      }else{
         return false;
      }
   }
   
   protected static BlockHitResult getPlayerHitResult(World level, PlayerEntity player, RaycastContext.FluidHandling fluidMode) {
      float xRot = player.getPitch();
      float yRot = player.getYaw();
      Vec3d eye = player.getEyePos();
      float h = MathHelper.cos(-yRot * MathHelper.RADIANS_PER_DEGREE - MathHelper.PI);
      float i = MathHelper.sin(-yRot * MathHelper.RADIANS_PER_DEGREE - MathHelper.PI);
      float j = -MathHelper.cos(-xRot * MathHelper.RADIANS_PER_DEGREE);
      float k = MathHelper.sin(-xRot * MathHelper.RADIANS_PER_DEGREE);
      float l = i * j;
      float n = h * j;
      Vec3d vec32 = eye.add(l * 5.0D, k * 5.0D, n * 5.0D);
      return level.raycast(new RaycastContext(eye, vec32, RaycastContext.ShapeType.OUTLINE, fluidMode, player));
   }
   
   private static void executeSignCommand(BlockEntity blockEntity, BlockPos pos, ServerPlayerEntity player) {
      if (blockEntity instanceof SignBlockEntity sign)
         sign.runCommandClickEvent(player, player.getServerWorld(), pos, sign.isPlayerFacingFront(player));
   }
   
   public static boolean redstoneEnabled(RegistryKey<World> world, ChunkPos chunkPos){
      if(isOutOfBounds(world,chunkPos)) return false;
      if(!world.equals(ServerWorld.OVERWORLD)) return false;
      NationChunk chunk = Nations.getChunk(chunkPos);
      if(chunk == null) return false;
      return chunk.hasMachinery();
   }
   
   public static int getRandomTickCount(RegistryKey<World> world, ChunkPos chunkPos){
      if(!growthTicksEnabled(world,chunkPos)) return 0;
      NationChunk chunk = Nations.getChunk(chunkPos);
      if(chunk == null) return 0;
      return chunk.getFarmlandLvl() * SERVER.getGameRules().getInt(GameRules.RANDOM_TICK_SPEED);
   }
   
   public static boolean growthTicksEnabled(RegistryKey<World> world, ChunkPos chunkPos){
      if(isOutOfBounds(world,chunkPos)) return false;
      if(!world.equals(ServerWorld.OVERWORLD)) return false;
      NationChunk chunk = Nations.getChunk(chunkPos);
      if(chunk == null) return false;
      return chunk.getFarmlandLvl() > 0;
   }
   
   public static boolean shouldKeepInventory(RegistryKey<World> world, ChunkPos chunkPos, ServerPlayerEntity player){
      if(!world.equals(ServerWorld.OVERWORLD)) return false;
      if(isSpawnChunk(chunkPos.getBlockPos(0,0,0))) return true;
      if(isWartime()) return false;
      Nation nation = Nations.getNation(player);
      if(nation == null) return false;
      NationChunk nationChunk = Nations.getChunk(chunkPos);
      if(nationChunk == null) return false;
      if(nationChunk.isArena()) return true;
      if(nationChunk.isClaimed() && nationChunk.getControllingNation().equals(nation)){
         return true;
      }
      return false;
   }
   
   public static boolean unclaimedOrSameNation(ChunkPos pos, Nation nation){
      NationChunk nationChunk = Nations.getChunk(pos);
      if(nationChunk == null) return false;
      return nationChunk.getControllingNation() == null || nation.getChunks().contains(nationChunk);
   }
}
