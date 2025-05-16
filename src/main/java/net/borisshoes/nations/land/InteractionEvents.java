package net.borisshoes.nations.land;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.borisshoes.nations.Nations;
import net.borisshoes.nations.NationsRegistry;
import net.borisshoes.nations.mixins.PersistentProjectileEntityAccessor;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.CrafterBlockEntity;
import net.minecraft.block.entity.LecternBlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Tameable;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.entity.vehicle.StorageMinecartEntity;
import net.minecraft.item.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.SlotChangedStateC2SPacket;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.registry.*;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.screen.LecternScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.*;

import java.util.List;

public class InteractionEvents {
   
   
   public static boolean breakBlocks(World world, PlayerEntity p, BlockPos pos, BlockState state, BlockEntity tile) {
      if (!breakBlocks(world, p, pos, world.getBlockState(pos), world.getBlockEntity(pos), false)) {
         BlockEntity blockEntity = world.getBlockEntity(pos);
         if (p instanceof ServerPlayerEntity player && blockEntity != null) {
            Packet<ClientPlayPacketListener> updatePacket = blockEntity.toUpdatePacket();
            if (updatePacket != null) {
               player.networkHandler.sendPacket(updatePacket);
            }
         }
         return false;
      }
      return true;
   }
   
   public static boolean breakBlocks(World world, PlayerEntity p, BlockPos pos, BlockState state, BlockEntity tile, boolean attempt) {
      if (!(p instanceof ServerPlayerEntity player) || p.isSpectator())
         return true;
      return NationsLand.canBreakBlock(world,player,pos,true);
   }
   
   public static ActionResult startBreakBlocks(PlayerEntity player, World world, Hand hand, BlockPos pos, Direction direction){
      BlockState state = world.getBlockState(pos);
      ActionResult result = breakBlocks(world, player, pos, state, world.getBlockEntity(pos), true) ? ActionResult.PASS : ActionResult.FAIL;
      if (player instanceof ServerPlayerEntity serverPlayer) {
         boolean failed = result == ActionResult.FAIL;
         ((BlockBreakAttemptHandler) serverPlayer.interactionManager).setBlockBreakAttemptFail(failed ? pos : null, failed && state.calcBlockBreakingDelta(player, world, pos) >= 1);
      }
      return result;
   }
   
   public static ActionResult useBlocks(PlayerEntity playerEntity, World world, Hand hand, BlockHitResult hitResult){
      if (playerEntity instanceof ServerPlayerEntity serverPlayer) {
         ItemUseBlockFlags flags = ItemUseBlockFlags.fromPlayer(serverPlayer);
         ActionResult res = InteractionEvents.useBlocksOther(playerEntity, world, hand, hitResult);
         if (res == ActionResult.SUCCESS)
            return res;
         flags.stopCanUseBlocks(res == ActionResult.FAIL);
         flags.stopCanUseItems(InteractionEvents.onItemUseBlock(new ItemUsageContext(playerEntity, hand, hitResult)) == ActionResult.FAIL);
         if (!flags.allowUseBlocks() && !flags.allowUseItems())
            return ActionResult.FAIL;
      }
      return ActionResult.PASS;
   }
   
   private static ActionResult onItemUseBlock(ItemUsageContext context){
      if (!(context.getPlayer() instanceof ServerPlayerEntity player) || context.getStack().isEmpty())
         return ActionResult.PASS;
      
      BlockPos interactPos = context.getBlockPos();
      ActionResult interact = itemUseOn(context.getWorld(), player, interactPos, context.getStack());
      if (interact != ActionResult.PASS)
         return interact;
      BlockPos placePos = new ItemPlacementContext(context).getBlockPos();
      return itemUseOn(context.getWorld(), player, placePos, context.getStack());
   }
   
   private static ActionResult itemUseOn(World world, ServerPlayerEntity player, BlockPos placePos, ItemStack stack) {
      if(!NationsLand.canUseItemOnBlock(world,player,placePos,stack,true)){
         BlockState other = world.getBlockState(placePos.up());
         player.networkHandler.sendPacket(new BlockUpdateS2CPacket(placePos.up(), other));
         updateHeldItem(player);
         return ActionResult.FAIL;
      }
      return ActionResult.PASS;
   }
   
   public static ActionResult useBlocksOther(PlayerEntity playerEntity, World world, Hand hand, BlockHitResult hitResult){
      if (!(playerEntity instanceof ServerPlayerEntity player))
         return ActionResult.PASS;
      ItemStack stack = player.getStackInHand(hand);
      BlockPos blockPos = hitResult.getBlockPos();
      if(NationsLand.canUseOtherBlocks(world,player,stack,blockPos,true)){
         return ActionResult.PASS;
      }else{
         return ActionResult.FAIL;
      }
   }
   
   public static ActionResult useAtEntity(PlayerEntity player, World world, Hand hand, Entity entity, Object o){
      if (!(player instanceof ServerPlayerEntity serverPlayer) || player.isSpectator())
         return ActionResult.PASS;
      if(NationsLand.canUseAtEntity(world,serverPlayer,entity,true)){
         return ActionResult.PASS;
      }else{
         return ActionResult.FAIL;
      }
   }
   
   public static ActionResult useEntity(PlayerEntity p, World world, Hand hand, Entity entity){
      if (!(p instanceof ServerPlayerEntity player) || p.isSpectator())
         return ActionResult.PASS;
      
      if(NationsLand.canUseEntity(world,player,entity,true)){
         return ActionResult.PASS;
      }else{
         return ActionResult.FAIL;
      }
   }
   
   public static boolean projectileHit(ProjectileEntity proj, HitResult res) {
      if (proj.getWorld().isClient)
         return false;
      Entity owner = proj.getOwner();
      if (owner instanceof ServerPlayerEntity player) {
         if (res.getType() == HitResult.Type.BLOCK) {
            BlockHitResult blockRes = (BlockHitResult) res;
            BlockPos pos = blockRes.getBlockPos();
            BlockState state = proj.getWorld().getBlockState(pos);
            
            if (NationsLand.canUseArrowOnBlock(proj.getWorld(), player, pos, state, true)) {
               if (proj instanceof PersistentProjectileEntity pers) {
                  ((PersistentProjectileEntityAccessor) pers).setInBlockState(pers.getWorld().getBlockState(pos));
                  Vec3d vec3d = blockRes.getPos().subtract(pers.getX(), pers.getY(), pers.getZ());
                  pers.setVelocity(vec3d);
                  Vec3d vec3d2 = vec3d.normalize().multiply(0.05000000074505806D);
                  pers.setPos(pers.getX() - vec3d2.x, pers.getY() - vec3d2.y, pers.getZ() - vec3d2.z);
                  pers.playSound(((PersistentProjectileEntityAccessor) pers).getSoundEvent(), 1.0F, 1.2F / (pers.getWorld().random.nextFloat() * 0.2F + 0.9F));
                  ((PersistentProjectileEntityAccessor) pers).setArrowInGround(true);
                  pers.shake = 7;
                  pers.setCritical(false);
                  ((PersistentProjectileEntityAccessor) pers).setPiercingLevel((byte) 0);
                  pers.setSound(SoundEvents.ENTITY_ARROW_HIT);
                  ((PersistentProjectileEntityAccessor) pers).resetPiercingStatus();
               }
               return true;
            }else{
               return false;
            }
         } else if (res.getType() == HitResult.Type.ENTITY) {
            Entity hit = ((EntityHitResult) res).getEntity();
            boolean fail = attackSimple(player, hit) != ActionResult.PASS;
            if (fail && proj instanceof PersistentProjectileEntity pers && pers.getPierceLevel() > 0) {
               IntOpenHashSet pierced = ((PersistentProjectileEntityAccessor) pers).getPiercedEntities();
               if (pierced == null)
                  pierced = new IntOpenHashSet(5);
               pierced.add(hit.getId());
               ((PersistentProjectileEntityAccessor) pers).setPiercedEntities(pierced);
               ((PersistentProjectileEntityAccessor) pers).setPiercingLevel((byte) (pers.getPierceLevel() + 1));
            }
            return fail;
         }
      }
      return false;
   }
   
   public static boolean preventDamage(Entity entity, DamageSource source) {
      if (source.getAttacker() instanceof ServerPlayerEntity)
         return attackSimple((ServerPlayerEntity) source.getAttacker(), entity) != ActionResult.PASS;
      else if (source.isIn(DamageTypeTags.IS_EXPLOSION) && !entity.getWorld().isClient && !(entity instanceof ServerPlayerEntity || entity instanceof Monster)) {
         return !NationsLand.canExplosionDamage(entity);
      }
      return false;
   }
   
   public static boolean canEndermanInteract(EndermanEntity enderman, BlockPos pos) {
      if (enderman.getWorld().isClient || !enderman.getWorld().getRegistryKey().equals(ServerWorld.OVERWORLD))
         return true;
      ChunkPos chunkPos = new ChunkPos(pos);
      return !Nations.isClaimed(chunkPos).isEmpty();
   }
   
   public static boolean witherCanDestroy(WitherEntity wither) {
      if (wither.getWorld().isClient)
         return true;
      BlockPos.Mutable pos = new BlockPos.Mutable();
      for (int x = -1; x <= 1; x++){
         for(int z = -1; z <= 1; z++){
            pos.set(wither.getBlockPos(), x, 3, z);
            if(!NationsLand.canWitherDestroy(wither.getWorld(), pos))
               return false;
         }
      }
      return true;
   }
   
   public static ActionResult attackEntity(PlayerEntity player, World world, Hand hand, Entity entity, EntityHitResult entityHitResult){
      return attackSimple(player, entity);
   }
   
   private static ActionResult attackSimple(PlayerEntity p, Entity entity){
      if(!(p instanceof ServerPlayerEntity player) || p.isSpectator())
         return ActionResult.PASS;
      if(NationsLand.canDamage(player,entity,true)){
         return ActionResult.PASS;
      }else{
         return ActionResult.FAIL;
      }
   }
   
   public static ActionResult useItem(PlayerEntity playerEntity, World world, Hand hand){
      if (!(playerEntity instanceof ServerPlayerEntity player) || playerEntity.isSpectator())
         return ActionResult.PASS;
      ItemStack stack = player.getStackInHand(hand);
      
      if(NationsLand.canUseItem(world,player,stack,hand,true)){
         return ActionResult.PASS;
      }else{
         updateHeldItem(player);
         return ActionResult.FAIL;
      }
   }
   
   public static boolean preventFallOn(Entity entity, double heightDifference, boolean onGround, BlockState landedState, BlockPos landedPosition) {
      if (entity.getWorld().isClient)
         return false;
      return NationsLand.cannotLandOn(entity.getWorld(),entity,landedPosition,landedState);
   }
   
   public static boolean canBreakTurtleEgg(World world, BlockPos pos, Entity entity) {
      if (world.isClient)
         return false;
      return NationsLand.canBreakTurtleEgg(world,pos,entity,true);
   }
   
   public static boolean cancelEntityBlockCollision(BlockState state, World world, BlockPos pos, Entity entity) {
      if (world.isClient)
         return false;
      return NationsLand.cancelEntityBlockCollision(world,pos,entity,state,true);
   }
   
   public static boolean canFrostwalkerFreeze(ServerWorld world, BlockPos pos, LivingEntity entity) {
      return NationsLand.canFrostwalkerFreeze(world,pos,entity,true);
   }
   
   public static boolean growBonemeal(ItemUsageContext context) {
      if (context.getPlayer() instanceof ServerPlayerEntity serverPlayer) {
         BlockState state = serverPlayer.getWorld().getBlockState(context.getBlockPos());
         BlockPos.Mutable pos = context.getBlockPos().mutableCopy();
         /**
          * {@link InteractEvents#onItemUseBlock} handles this case already.
          * Sadly need to check again. In case its used in a claim. Less expensive than aoe check
          */
         int range = 0;
         Registry<ConfiguredFeature<?, ?>> registry = serverPlayer.getServerWorld().getRegistryManager().getOrThrow(RegistryKeys.CONFIGURED_FEATURE);
         if (state.getBlock() instanceof MossBlock) {
            
            VegetationPatchFeatureConfig cfg = featureRange(registry, UndergroundConfiguredFeatures.MOSS_PATCH_BONEMEAL, VegetationPatchFeatureConfig.class);
            if (cfg != null) {
               range = cfg.horizontalRadius.getMax() + 1;
               pos.set(pos.getX(), pos.getY() + cfg.verticalRange + 1, pos.getZ());
            }
         } else if (state.getBlock() instanceof GrassBlock) {
            range = 4;
         } else if (state.isOf(Blocks.CRIMSON_NYLIUM)) {
            NetherForestVegetationFeatureConfig cfg = featureRange(registry, NetherConfiguredFeatures.CRIMSON_FOREST_VEGETATION_BONEMEAL, NetherForestVegetationFeatureConfig.class);
            if (cfg != null) {
               range = cfg.spreadWidth;
               pos.set(pos.getX(), pos.getY() + cfg.spreadHeight + 1, pos.getZ());
            }
         } else if (state.isOf(Blocks.WARPED_NYLIUM)) {
            NetherForestVegetationFeatureConfig cfg = featureRange(registry, NetherConfiguredFeatures.WARPED_FOREST_VEGETATION_BONEMEAL, NetherForestVegetationFeatureConfig.class);
            NetherForestVegetationFeatureConfig cfg2 = featureRange(registry, NetherConfiguredFeatures.NETHER_SPROUTS_BONEMEAL, NetherForestVegetationFeatureConfig.class);
            TwistingVinesFeatureConfig cfg3 = featureRange(registry, NetherConfiguredFeatures.TWISTING_VINES_BONEMEAL, TwistingVinesFeatureConfig.class);
            int w1 = cfg == null ? 0 : cfg.spreadWidth;
            int w2 = cfg2 == null ? 0 : cfg2.spreadWidth;
            int w3 = cfg3 == null ? 0 : cfg3.spreadWidth();
            int h1 = cfg == null ? 0 : cfg.spreadHeight;
            int h2 = cfg2 == null ? 0 : cfg2.spreadHeight;
            int h3 = cfg3 == null ? 0 : cfg3.spreadHeight();
            range = Math.max(Math.max(w1, w2), w3);
            int y = Math.max(Math.max(h1, h2), h3);
            pos.set(pos.getX(), pos.getY() + y + 1, pos.getZ());
         }
         return NationsLand.stopBonemealGrow(serverPlayer.getWorld(),pos,serverPlayer,range,true);
      }
      return false;
   }
   
   public static <T extends FeatureConfig> T featureRange(Registry<ConfiguredFeature<?, ?>> registry, RegistryKey<ConfiguredFeature<?, ?>> key, Class<T> clss) {
      return registry.getEntry(key.getValue()).map(r -> {
         if (clss.isInstance(r.value().config()))
            return (T) r.value().config();
         return null;
      }).orElse(null);
   }
   
   private static void executeSignCommand(BlockEntity blockEntity, BlockPos pos, ServerPlayerEntity player) {
      if (blockEntity instanceof SignBlockEntity sign)
         sign.runCommandClickEvent(player, player.getServerWorld(), pos, sign.isPlayerFacingFront(player));
   }
   
   public static boolean contains(Identifier id, BlockEntity blockEntity, List<String> idList, List<String> tagList) {
      return idList.contains(id.getNamespace()) || idList.contains(id.toString());
   }
   
   /**
    * -2 == Main inventory update
    */
   private static void updateHeldItem(ServerPlayerEntity player) {
      player.networkHandler.sendPacket(new ScreenHandlerSlotUpdateS2CPacket(-2, 0, player.getInventory().selectedSlot, player.getInventory().getMainHandStack()));
      player.networkHandler.sendPacket(new ScreenHandlerSlotUpdateS2CPacket(-2, 0, 40, player.getInventory().getStack(40)));
   }
}
