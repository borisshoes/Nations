package net.borisshoes.nations.mixins;

import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.Inject;

@Mixin(PlayerManager.class)
public interface PlayerManagerAccessor {
   
   @Invoker("savePlayerData")
   void savePlayerNbtData(ServerPlayerEntity player);
}
