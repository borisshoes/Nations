package net.borisshoes.nations.mixins;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.minecraft.block.BlockState;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.sound.SoundEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(PersistentProjectileEntity.class)
public interface PersistentProjectileEntityAccessor {
   @Accessor("inBlockState")
   void setInBlockState(BlockState state);
   
   @Invoker("setInGround")
   void setArrowInGround(boolean flag);
   
   @Invoker("getHitSound")
   SoundEvent getSoundEvent();
   
   @Invoker("clearPiercingStatus")
   void resetPiercingStatus();
   
   @Accessor("piercedEntities")
   IntOpenHashSet getPiercedEntities();
   
   @Accessor("piercedEntities")
   void setPiercedEntities(IntOpenHashSet set);
   
   @Invoker("setPierceLevel")
   void setPiercingLevel(byte val);
}
