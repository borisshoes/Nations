package net.borisshoes.nations.gameplay;

import net.borisshoes.nations.Nations;
import net.borisshoes.nations.NationsRegistry;
import net.minecraft.network.message.MessageType;
import net.minecraft.network.message.SentMessage;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

public class ChatHandler {
   
   public static void handleGlobalChatMessage(SignedMessage message, ServerPlayerEntity sender){
      MessageType.Parameters parameters = MessageType.params(NationsRegistry.GLOBAL_MESSAGE, sender).withTargetName(
            Text.translatable(ChatChannel.GLOBAL.getTranslationKey()).formatted(ChatChannel.GLOBAL.getColor())
      );
      SentMessage sentMessage = SentMessage.of(message);
      boolean bl = false;
      for(ServerPlayerEntity player : sender.getServer().getPlayerManager().getPlayerList()){
         boolean bl2 = player != sender && (sender.shouldFilterText() || player.shouldFilterText());
         player.sendChatMessage(sentMessage, bl2, parameters);
         bl |= bl2 && message.isFullyFiltered();
      }
      if (bl) {
         sender.sendMessage(PlayerManager.FILTERED_FULL_TEXT);
      }
   }
   
   public static void handleLocalChatMessage(SignedMessage message, ServerPlayerEntity sender){
      int range = sender.getServer().getPlayerManager().getViewDistance();
      MessageType.Parameters parameters = MessageType.params(NationsRegistry.LOCAL_MESSAGE, sender).withTargetName(
            Text.translatable(ChatChannel.LOCAL.getTranslationKey()).formatted(ChatChannel.LOCAL.getColor())
      );
      SentMessage sentMessage = SentMessage.of(message);
      boolean bl = false;
      for(ServerPlayerEntity player : sender.getServer().getPlayerManager().getPlayerList().stream().filter(p -> p.getWorld().getRegistryKey().equals(sender.getWorld().getRegistryKey()) && p.distanceTo(sender) <= range * 16).toList()){
         boolean bl2 = player != sender && (sender.shouldFilterText() || player.shouldFilterText());
         player.sendChatMessage(sentMessage, bl2, parameters);
         bl |= bl2 && message.isFullyFiltered();
      }
      if (bl) {
         sender.sendMessage(PlayerManager.FILTERED_FULL_TEXT);
      }
   }
   
   public static void handleNationChatMessage(SignedMessage message, ServerPlayerEntity sender){
      MessageType.Parameters parameters = MessageType.params(NationsRegistry.NATION_MESSAGE, sender).withTargetName(
            Text.translatable(ChatChannel.NATION.getTranslationKey()).formatted(ChatChannel.NATION.getColor())
      );
      SentMessage sentMessage = SentMessage.of(message);
      Nation senderNation = Nations.getNation(sender);
      if(senderNation == null) return;
      boolean bl = false;
      for(ServerPlayerEntity player : sender.getServer().getPlayerManager().getPlayerList().stream().filter(p -> senderNation.equals(Nations.getNation(p))).toList()){
         boolean bl2 = player != sender && (sender.shouldFilterText() || player.shouldFilterText());
         player.sendChatMessage(sentMessage, bl2, parameters);
         bl |= bl2 && message.isFullyFiltered();
      }
      if (bl) {
         sender.sendMessage(PlayerManager.FILTERED_FULL_TEXT);
      }
   }
}
