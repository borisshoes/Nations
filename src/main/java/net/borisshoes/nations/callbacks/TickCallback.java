package net.borisshoes.nations.callbacks;

import net.borisshoes.arcananovum.ArcanaRegistry;
import net.borisshoes.arcananovum.utils.ArcanaItemUtils;
import net.borisshoes.nations.Nations;
import net.borisshoes.nations.NationsConfig;
import net.borisshoes.nations.NationsRegistry;
import net.borisshoes.nations.cca.INationsDataComponent;
import net.borisshoes.nations.cca.INationsProfileComponent;
import net.borisshoes.nations.gameplay.Nation;
import net.borisshoes.nations.gameplay.NationChunk;
import net.borisshoes.nations.gameplay.TimedEvents;
import net.borisshoes.nations.gameplay.WarManager;
import net.borisshoes.nations.land.NationsLand;
import net.borisshoes.nations.research.ResearchTech;
import net.borisshoes.nations.utils.ParticleEffectUtils;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Map;

import static net.borisshoes.arcananovum.ArcanaNovum.MOD_ID;
import static net.borisshoes.arcananovum.ArcanaRegistry.EQUIPMENT_ASSET_REGISTRY_KEY;
import static net.borisshoes.nations.Nations.SERVER_TIMER_CALLBACKS;
import static net.borisshoes.nations.cca.WorldDataComponentInitializer.NATIONS_DATA;

public class TickCallback {
   
   public static void onTick(MinecraftServer server){
      try{
         INationsDataComponent data = NATIONS_DATA.get(server.getOverworld());
         
         // Tick Timer Callbacks
         SERVER_TIMER_CALLBACKS.removeIf(tickTimers(server)::contains);
         
         
         for(ServerPlayerEntity player : server.getPlayerManager().getPlayerList()){
            INationsProfileComponent profile = Nations.getPlayer(player);
            profile.tick();
            
            if(server.getTicks() % 20 == 0){
               Vec3d pos = player.getPos();
               float largestPos = (float) Math.max(Math.abs(pos.x), Math.abs(pos.z));
               int border = Integer.MAX_VALUE;
               if(player.getServerWorld().getRegistryKey().equals(ServerWorld.OVERWORLD)){
                  border = NationsConfig.getInt(NationsRegistry.WORLD_BORDER_RADIUS_OVERWORLD_CFG) * 16;
               }else if(player.getServerWorld().getRegistryKey().equals(ServerWorld.NETHER)){
                  border = NationsConfig.getInt(NationsRegistry.WORLD_BORDER_RADIUS_NETHER_CFG) * 16;
               }
               
               float dist = largestPos - border;
               if(!player.isCreative() && !player.isSpectator()){
                  if(dist > 0){
                     float damage = dist * 0.25f;
                     player.sendMessage(Text.translatable("text.nations.border_danger").formatted(Formatting.BOLD,Formatting.RED), true);
                     player.damage(player.getServerWorld(),player.getDamageSources().outsideBorder(),damage);
                  }else if(dist > -16){
                     player.sendMessage(Text.translatable("text.nations.border_warning").formatted(Formatting.GOLD), true);
                  }
               }
               
               ItemStack helmetStack = player.getEquippedStack(EquipmentSlot.HEAD);
               if(!helmetStack.isEmpty() && helmetStack.getItem() instanceof ArmorItem && !helmetStack.isOf(ArcanaRegistry.NUL_MEMENTO.getItem())){
                  EquippableComponent equippableComponent = helmetStack.get(DataComponentTypes.EQUIPPABLE);
                  if(equippableComponent.assetId().isPresent() && !equippableComponent.assetId().get().toString().contains("empty")){
                     EquippableComponent newComp = EquippableComponent.builder(equippableComponent.slot()).equipSound(equippableComponent.equipSound()).model(RegistryKey.of(EQUIPMENT_ASSET_REGISTRY_KEY, Identifier.of(MOD_ID,"empty"))).build();
                     helmetStack.set(DataComponentTypes.EQUIPPABLE,newComp);
                  }
               }
               
               PlayerInventory inv = player.getInventory();
               for(int i=0; i<inv.size();i++){
                  ItemStack item = inv.getStack(i);
                  if(item.hasEnchantments() && !profile.bypassesClaims() && !ArcanaItemUtils.isArcane(item)){
                     ItemEnchantmentsComponent enchants = item.getEnchantments();
                     ItemEnchantmentsComponent.Builder newEnchants = new ItemEnchantmentsComponent.Builder(enchants);
                     newEnchants.remove(ench -> {
                        for(Map.Entry<Pair<RegistryKey<Enchantment>, Integer>, RegistryKey<ResearchTech>> entry : NationsRegistry.ENCHANT_TECHS.entrySet()){
                           Pair<RegistryKey<Enchantment>, Integer> pair = entry.getKey();
                           if(pair.getLeft().toString().equals(ench.getKey().get().toString())){
                              return false;
                           }
                        }
                        return true;
                     });
                     
                     if(profile.getNation() != null){
                        for(RegistryEntry<Enchantment> enchantment : newEnchants.getEnchantments()){
                           int level = newEnchants.getLevel(enchantment);
                           int newLevel = 1;
                           for(int j = level; j >= 1; j--){
                              boolean canSupport = profile.getNation().canEnchant(enchantment.getKey().get(),j);
                              if(canSupport){
                                 newLevel = Math.min(level,newLevel+1);
                                 break;
                              }
                           }
                           newEnchants.set(enchantment,newLevel);
                        }
                     }
                     
                     EnchantmentHelper.set(item,newEnchants.build());
                  }
               }
            }
            
            if(player.getServerWorld().getRegistryKey().equals(ServerWorld.OVERWORLD)){
               BlockPos pos = player.getBlockPos();
               NationChunk nationChunk = Nations.getChunk(new ChunkPos(pos));
               String lastTerritory = profile.lastTerritory();
               String territory = "";
               Nation nation = null;
               if(NationsLand.isSpawnChunk(pos)){
                  territory = ".spawn";
               }else if(nationChunk != null && (nation = nationChunk.getControllingNation()) != null){
                  territory = nation.getId();
               }
               
               if(!territory.equals(lastTerritory)){
                  int titleCd = profile.titleCooldown();
                  if(titleCd == 0){
                     MutableText titleText = Text.translatable("text.nations.wilderness").formatted(Formatting.GRAY);
                     if(territory.equals(".spawn")){
                        titleText = Text.translatable("text.nations.spawn").formatted(Formatting.DARK_AQUA);
                     }else if(!territory.isBlank()){
                        titleText = nation.getFormattedNameTag(false);
                     }
                     if(titleText != null){
                        player.networkHandler.sendPacket(new TitleFadeS2CPacket(10, 20, 10));
                        player.networkHandler.sendPacket(new TitleS2CPacket(titleText));
                        profile.resetTitleCooldown();
                        
                        if(nation != null && !nation.equals(profile.getNation())){
                           nation.alertTrespass(player);
                        }
                     }
                  }
                  profile.setLastTerritory(territory);
               }
            }
         }
         
         TimedEvents.tickTimedEvents(server);
         WarManager.tickWar(server);
         
         if(server.getTicks() % 20 == 0){
            PlayerConnectionCallback.tickLogoutTracker(server);
         }
      }catch(Exception e){
         e.printStackTrace();
      }
   }
   
   @NotNull
   private static ArrayList<TickTimerCallback> tickTimers(MinecraftServer server){
      ArrayList<TickTimerCallback> toRemove = new ArrayList<>();
      for(TickTimerCallback t : SERVER_TIMER_CALLBACKS){
         if(t.decreaseTimer() == 0){
            t.onTimer();
            toRemove.add(t);
         }
      }
      return toRemove;
   }
}
