package net.borisshoes.nations.items;

import eu.pb4.polymer.core.api.item.PolymerItem;
import net.borisshoes.arcananovum.utils.SoundUtils;
import net.borisshoes.arcananovum.utils.TextUtils;
import net.borisshoes.nations.gameplay.ResourceType;
import net.borisshoes.nations.gui.BugVoucherGui;
import net.borisshoes.nations.utils.MiscUtils;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.*;
import net.minecraft.item.consume.UseAction;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.apache.commons.lang3.math.Fraction;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static net.borisshoes.nations.Nations.MOD_ID;

public class CoinPurseItem extends Item implements PolymerItem {
   
   public static final int SIZE = 54;
   
   public CoinPurseItem(Settings settings, String id){
      super(settings.maxCount(1).rarity(Rarity.EPIC)
            .registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(MOD_ID,id)))
            .component(DataComponentTypes.CONTAINER,ContainerComponent.DEFAULT)
            .component(DataComponentTypes.CONSUMABLE, ConsumableComponent.builder()
                  .consumeSeconds(10).useAction(UseAction.BUNDLE).consumeParticles(false)
                  .sound(Registries.SOUND_EVENT.getEntry(SoundEvents.ITEM_BUNDLE_DROP_CONTENTS)).build())
            .component(DataComponentTypes.DAMAGE_RESISTANT, new DamageResistantComponent(DamageTypeTags.IS_FIRE)));
   }
   
   public static boolean isValidItem(ItemStack item){
      return item.getItem() instanceof ResourceCoinItem || item.getItem() instanceof ResourceBullionItem;
   }
   
   @Override
   public ActionResult use(World world, PlayerEntity user, Hand hand){
      ItemStack stack = user.getStackInHand(hand);
      ContainerComponent contentsComponent = stack.getOrDefault(DataComponentTypes.CONTAINER,ContainerComponent.DEFAULT);
      if(contentsComponent.streamNonEmpty().findAny().isPresent()){
         user.setCurrentHand(hand);
         return ActionResult.SUCCESS_SERVER;
      }else{
         return ActionResult.PASS;
      }
   }
   
   @Override
   public boolean onStackClicked(ItemStack stack, Slot slot, ClickType clickType, PlayerEntity player) {
      ContainerComponent contentsComponent = stack.get(DataComponentTypes.CONTAINER);
      if (contentsComponent == null) {
         return false;
      } else {
         ItemStack itemStack = slot.getStack();
         List<ItemStack> builder = contentsComponent.stream().toList();
         
         if (clickType == ClickType.LEFT && !itemStack.isEmpty()) {
            if(!isValidItem(itemStack)){
               playInsertFailSound(player);
            }else{
               int count = itemStack.getCount();
               Pair<ContainerComponent,ItemStack> addPair = MiscUtils.tryAddStackToContainerComp(contentsComponent,SIZE,itemStack);
               if(count == addPair.getRight().getCount()){
                  playInsertFailSound(player);
               }else{
                  playInsertSound(player);
                  stack.set(DataComponentTypes.CONTAINER, addPair.getLeft());
               }
            }
            onContentChanged(player,stack);
            return true;
         } else if (clickType == ClickType.RIGHT && itemStack.isEmpty()) {
            ItemStack removed = ItemStack.EMPTY;
            for(ItemStack purseStack : builder.reversed()){
               if(!purseStack.isEmpty()){
                  removed = purseStack.copyAndEmpty();
                  playRemoveOneSound(player);
                  break;
               }
            }
            
            if(!removed.isEmpty()){
               ItemStack itemStack3 = slot.insertStack(removed);
               if (itemStack3.getCount() > 0) {
                  Pair<ContainerComponent,ItemStack> addPair = MiscUtils.tryAddStackToContainerComp(contentsComponent,SIZE,itemStack3);
                  stack.set(DataComponentTypes.CONTAINER, addPair.getLeft());
               } else {
                  stack.set(DataComponentTypes.CONTAINER,ContainerComponent.fromStacks(builder));
                  playRemoveOneSound(player);
               }
            }else{
               return false;
            }
            
            onContentChanged(player,stack);
            return true;
         } else {
            return false;
         }
      }
   }
   
   @Override
   public boolean onClicked(ItemStack stack, ItemStack otherStack, Slot slot, ClickType clickType, PlayerEntity player, StackReference cursorStackReference) {
      if (clickType == ClickType.LEFT && otherStack.isEmpty()) {
         return false;
      } else {
         ContainerComponent contentsComponent = stack.get(DataComponentTypes.CONTAINER);
         if (contentsComponent == null) {
            return false;
         } else {
            ArrayList<ItemStack> builder = new ArrayList<>();
            contentsComponent.iterateNonEmptyCopy().forEach(builder::add);
            
            if (clickType == ClickType.LEFT && !otherStack.isEmpty()) {
               if(!isValidItem(otherStack)){
                  playInsertFailSound(player);
               }else if (slot.canTakePartial(player)) {
                  int count = otherStack.getCount();
                  Pair<ContainerComponent,ItemStack> addPair = MiscUtils.tryAddStackToContainerComp(contentsComponent,SIZE,otherStack);
                  if(count == addPair.getRight().getCount()){
                     playInsertFailSound(player);
                  }else{
                     playInsertSound(player);
                     stack.set(DataComponentTypes.CONTAINER, addPair.getLeft());
                  }
               } else {
                  playInsertFailSound(player);
               }
               
               onContentChanged(player,stack);
               return true;
            } else if (clickType == ClickType.RIGHT && otherStack.isEmpty()) {
               boolean found = false;
               for(ItemStack itemStack : builder.reversed()){
                  if(!itemStack.isEmpty()){
                     cursorStackReference.set(itemStack.copyAndEmpty());
                     playRemoveOneSound(player);
                     found = true;
                     break;
                  }
               }
               
               if(found){
                  stack.set(DataComponentTypes.CONTAINER,ContainerComponent.fromStacks(builder));
                  onContentChanged(player,stack);
                  return true;
               }else{
                  return false;
               }
            } else {
               return false;
            }
         }
      }
   }
   
   private static void onContentChanged(PlayerEntity user, ItemStack stack) {
      ContainerComponent contentsComponent = stack.getOrDefault(DataComponentTypes.CONTAINER,ContainerComponent.DEFAULT);
      stack.set(DataComponentTypes.CONTAINER,ContainerComponent.fromStacks(contentsComponent.streamNonEmpty().toList()));
      List<Text> lore = new ArrayList<>();
      int gc = 0, mc = 0, rc = 0;
      for(ItemStack itemStack : contentsComponent.iterateNonEmpty()){
         int count = itemStack.getCount();
         if(itemStack.getItem() instanceof ResourceCoinItem coin){
            switch(coin.getType()){
               case GROWTH -> gc += count;
               case MATERIAL -> mc += count;
               case RESEARCH -> rc += count;
            }
         }else if(itemStack.getItem() instanceof ResourceBullionItem bullion){
            switch(bullion.getType()){
               case GROWTH -> gc += 1000 * count;
               case MATERIAL -> mc += 1000 * count;
               case RESEARCH -> rc += 1000 * count;
            }
         }
      }
      if((gc + mc + rc) > 0){
         lore.add(Text.translatable("text.nations.purse_contains"));
         if(gc > 0){
            lore.add(Text.translatable("text.nations.purse_coin",
                  Text.literal(String.format("%,d",gc)).formatted(Formatting.GREEN,Formatting.BOLD),
                  Text.translatable(ResourceType.GROWTH.getTranslation()).formatted(Formatting.DARK_GREEN),
                  Text.translatable("text.nations.coins").formatted(Formatting.DARK_GREEN)
            ).formatted(Formatting.GREEN));
         }
         if(mc > 0){
            lore.add(Text.translatable("text.nations.purse_coin",
                  Text.literal(String.format("%,d",mc)).formatted(Formatting.GOLD,Formatting.BOLD),
                  Text.translatable(ResourceType.MATERIAL.getTranslation()).formatted(Formatting.RED),
                  Text.translatable("text.nations.coins").formatted(Formatting.RED)
            ).formatted(Formatting.GOLD));
         }
         if(rc > 0){
            lore.add(Text.translatable("text.nations.purse_coin",
                  Text.literal(String.format("%,d",rc)).formatted(Formatting.AQUA,Formatting.BOLD),
                  Text.translatable(ResourceType.RESEARCH.getTranslation()).formatted(Formatting.DARK_AQUA),
                  Text.translatable("text.nations.coins").formatted(Formatting.DARK_AQUA)
            ).formatted(Formatting.AQUA));
         }
      }
      
      stack.set(DataComponentTypes.LORE,new LoreComponent(lore.stream().map(TextUtils::removeItalics).collect(Collectors.toCollection(ArrayList::new))));
   }
   
   @Override
   public boolean isItemBarVisible(ItemStack stack) {
      ContainerComponent contentsComponent = stack.getOrDefault(DataComponentTypes.CONTAINER,ContainerComponent.DEFAULT);
      return !contentsComponent.copyFirstStack().isEmpty();
   }
   
   @Override
   public int getItemBarStep(ItemStack stack) {
      ContainerComponent contentsComponent = stack.getOrDefault(DataComponentTypes.CONTAINER,ContainerComponent.DEFAULT);
      int nonEmpty = (int) contentsComponent.streamNonEmpty().count();
      return Math.min(1 + MathHelper.multiplyFraction(Fraction.getFraction(nonEmpty,SIZE), 12), 13);
   }
   
   @Override
   public int getItemBarColor(ItemStack stack) {
      ContainerComponent contentsComponent = stack.getOrDefault(DataComponentTypes.CONTAINER,ContainerComponent.DEFAULT);
      ItemStack firstStack = contentsComponent.copyFirstStack();
      if(!firstStack.isEmpty()){
         if(firstStack.getItem() instanceof ResourceCoinItem coin){
            return coin.getColor();
         }else if(firstStack.getItem() instanceof ResourceBullionItem bullion){
            return bullion.getColor();
         }
      }
      return 0xdddddd;
   }
   
   private boolean dropFirstBundledStack(ItemStack stack, PlayerEntity player) {
      ContainerComponent contentsComponent = stack.get(DataComponentTypes.CONTAINER);
      if (contentsComponent != null && !contentsComponent.copyFirstStack().isEmpty()) {
         Optional<ItemStack> optional = popFirstBundledStack(stack, player, contentsComponent);
         if (optional.isPresent()) {
            player.dropItem((ItemStack)optional.get(), true);
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }
   
   private static Optional<ItemStack> popFirstBundledStack(ItemStack stack, PlayerEntity player, ContainerComponent contentsComponent) {
      List<ItemStack> stacks = new ArrayList<>();
      ItemStack itemStack = ItemStack.EMPTY;
      for(ItemStack purseStack : contentsComponent.iterateNonEmptyCopy()){
         if(itemStack.isEmpty()){
            itemStack = purseStack;
         }else{
            stacks.add(purseStack);
         }
      }
      
      if (!itemStack.isEmpty()) {
         playRemoveOneSound(player);
         stack.set(DataComponentTypes.CONTAINER, ContainerComponent.fromStacks(stacks));
         onContentChanged(player,stack);
         return Optional.of(itemStack);
      } else {
         return Optional.empty();
      }
   }
   
   private void dropContentsOnUse(World world, PlayerEntity player, ItemStack stack) {
      if (this.dropFirstBundledStack(stack, player)) {
         playDropContentsSound(world, player);
         player.incrementStat(Stats.USED.getOrCreateStat(this));
      }
   }
   
   @Override
   public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
      if (user instanceof PlayerEntity playerEntity) {
         int i = this.getMaxUseTime(stack, user);
         boolean bl = remainingUseTicks == i;
         if (bl || remainingUseTicks < i - 10 && remainingUseTicks % 2 == 0) {
            this.dropContentsOnUse(world, playerEntity, stack);
         }
      }
   }
   
   @Override
   public int getMaxUseTime(ItemStack stack, LivingEntity user) {
      return 200;
   }
   
   @Override
   public UseAction getUseAction(ItemStack stack) {
      return UseAction.BUNDLE;
   }
   
   @Override
   public void onItemEntityDestroyed(ItemEntity entity) {
      ContainerComponent contentsComponent = entity.getStack().get(DataComponentTypes.CONTAINER);
      if (contentsComponent != null) {
         entity.getStack().set(DataComponentTypes.CONTAINER, ContainerComponent.DEFAULT);
         ItemUsage.spawnItemContents(entity, contentsComponent.iterateNonEmptyCopy());
      }
   }
   
   private static void playRemoveOneSound(Entity entity) {
      if(entity instanceof ServerPlayerEntity player) player.playSoundToPlayer(SoundEvents.ITEM_BUNDLE_REMOVE_ONE, SoundCategory.PLAYERS, 0.8F, 0.8F + entity.getWorld().getRandom().nextFloat() * 0.4F);
   }
   
   private static void playInsertSound(Entity entity) {
      if(entity instanceof ServerPlayerEntity player) player.playSoundToPlayer(SoundEvents.ITEM_BUNDLE_INSERT, SoundCategory.PLAYERS, 0.8F, 0.8F + entity.getWorld().getRandom().nextFloat() * 0.4F);
   }
   
   private static void playInsertFailSound(Entity entity) {
      if(entity instanceof ServerPlayerEntity player) player.playSoundToPlayer(SoundEvents.ITEM_BUNDLE_INSERT_FAIL, SoundCategory.PLAYERS, 1.0f, 1.0f);
   }
   
   private static void playDropContentsSound(World world, Entity entity) {
      world.playSound(
            null, entity.getBlockPos(), SoundEvents.ITEM_BUNDLE_DROP_CONTENTS, SoundCategory.PLAYERS, 0.8F, 0.8F + entity.getWorld().getRandom().nextFloat() * 0.4F
      );
   }
   
   @Override
   public Item getPolymerItem(ItemStack itemStack, PacketContext packetContext){
      return Items.BUNDLE;
   }
   
   @Override
   public @Nullable Identifier getPolymerItemModel(ItemStack stack, PacketContext context){
      return Identifier.of(MOD_ID,"coin_purse");
   }
}
