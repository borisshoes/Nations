package net.borisshoes.nations.mixins;

import net.borisshoes.nations.Nations;
import net.borisshoes.nations.cca.INationsProfileComponent;
import net.borisshoes.nations.gameplay.ChatHandler;
import net.borisshoes.nations.gameplay.WarManager;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.command.MessageCommand;
import net.minecraft.server.command.TeamMsgCommand;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayNetworkHandler.class)
public class ServerPlayNetworkHandlerMixin {
   
   @Shadow
   public ServerPlayerEntity player;

   @Inject(method = "handleDecoratedMessage", at = @At("HEAD"), cancellable = true)
   private void nations_injectChat(SignedMessage message, CallbackInfo ci){
      ServerPlayNetworkHandler handler = (ServerPlayNetworkHandler) (Object) this;
      INationsProfileComponent profile = Nations.getPlayer(handler.player);
      switch(profile.getChannel()){
         case LOCAL ->  ChatHandler.handleLocalChatMessage(message,handler.player);
         case NATION -> ChatHandler.handleNationChatMessage(message,handler.player);
         case GLOBAL -> ChatHandler.handleGlobalChatMessage(message,handler.player);
      }
      ci.cancel();
   }
}
