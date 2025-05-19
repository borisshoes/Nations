package net.borisshoes.nations.utils;

import com.mojang.authlib.GameProfile;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.ChunkAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import eu.pb4.polymer.virtualentity.api.elements.*;
import eu.pb4.polymer.virtualentity.api.tracker.EntityTrackedData;
import net.borisshoes.nations.Nations;
import net.borisshoes.nations.NationsRegistry;
import net.borisshoes.nations.gui.ShopGui;
import net.borisshoes.nations.items.GraphicalItem;
import net.borisshoes.nations.research.ResearchTech;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.block.Block;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.apache.logging.log4j.Logger;
import org.joml.Vector3f;

import java.io.*;
import java.util.*;

import static net.borisshoes.arcananovum.ArcanaNovum.log;

public class ShopUtils {
   private final List<Pair<ItemStack,Pair<Item,Integer>>> offers = new ArrayList<>();
   private final File file;
   private final Logger logger;
   private final MinecraftServer server;
   private ElementHolder hologram;
   private HolderAttachment attachment;
   private int interactCooldown;
   
   public ShopUtils(File file, Logger logger, MinecraftServer server){
      this.file = file;
      this.logger = logger;
      this.server = server;
      this.read();
      this.save();
   }
   
   public List<Pair<ItemStack, Pair<Item, Integer>>> getOffers(){
      return offers;
   }
   
   public void tick(){
      ServerWorld serverWorld = server.getOverworld();
      if(server.getTicks() % 20 == 0){
         Vec3d pos = getHologramPos();
         PlayerEntity player = serverWorld.getClosestPlayer(pos.getX(),pos.getY(),pos.getZ(), 64, entity -> !entity.isSpectator());
         if(player != null && hologram == null){
            hologram = getNewHologram();
            attachment = ChunkAttachment.ofTicking(this.hologram,serverWorld,pos);
         }else if(player == null && hologram != null){
            hologram.destroy();
            hologram = null;
         }
      }
      if(serverWorld.getRandom().nextBetween(1,8000) == 1){
         Vec3d pos = getHologramPos();
         int dialog = serverWorld.getRandom().nextBetween(0,17);
         for(ServerPlayerEntity player : serverWorld.getPlayers(p -> p.squaredDistanceTo(pos) < 400)){
            player.sendMessage(Text.empty()
                  .append(Text.literal(" ~ ").formatted(Formatting.BOLD,Formatting.DARK_AQUA))
                  .append(Text.literal("Jeráld").formatted(Formatting.BOLD, Formatting.AQUA))
                  .append(Text.literal(" ~ \n").formatted(Formatting.BOLD,Formatting.DARK_AQUA))
                  .append(Text.translatable("text.nations.jerald_dialog_"+dialog).formatted(Formatting.DARK_AQUA))
            );
            player.playSoundToPlayer(SoundEvents.ENTITY_WANDERING_TRADER_TRADE, SoundCategory.PLAYERS, 1, 1);
         }
      }
   }
   
   private Vec3d getHologramPos(){
      return server.getOverworld().getSpawnPos().toBottomCenterPos();
   }
   
   private ElementHolder getNewHologram(){
      SimpleEntityElement jerald = new SimpleEntityElement(EntityType.WANDERING_TRADER);
      TextDisplayElement nametag = new TextDisplayElement(Text.literal("Jeráld").withColor(0x00cabe));
      InteractionElement click = new InteractionElement(new VirtualElement.InteractionHandler(){
         public void click(ServerPlayerEntity player){
            if(interactCooldown == 0){
               ShopGui gui = new ShopGui(player);
               gui.open();
               interactCooldown = 5;
            }
         }
         
         @Override
         public void interact(ServerPlayerEntity player, Hand hand){
            click(player);
         }
         
         @Override
         public void interactAt(ServerPlayerEntity player, Hand hand, Vec3d pos){
            click(player);
         }
         
         @Override
         public void attack(ServerPlayerEntity player){
            click(player);
         }
      });
      click.setSize(1.25f,1.75f);
      
      ElementHolder holder = new ElementHolder(){
         ServerWorld world = server.getOverworld();
         private SimpleEntityElement jeraldEntity = jerald;
         private final InteractionElement clickElem = click;
         private int tickCount = 0;
         
         @Override
         protected void onTick(){
            super.onTick();
            
            tickCount++;
            if(interactCooldown > 0) interactCooldown--;
            
            Vec3d centerPos = getHologramPos();
            PlayerEntity player = world.getClosestPlayer(centerPos.getX(),centerPos.getY(),centerPos.getZ(), 64, entity -> !entity.isSpectator());
            if(player != null){
               Vec3d vec3d = centerPos;
               double d = player.getX() - vec3d.x;
               double e = player.getY()+player.getEyeHeight(player.getPose()) - (vec3d.y+1.65);
               double f = player.getZ() - vec3d.z;
               double g = Math.sqrt(d * d + f * f);
               float pitch = MathHelper.wrapDegrees((float)(-(MathHelper.atan2(e, g) * 180.0F / (float)Math.PI)));
               float yaw = MathHelper.wrapDegrees((float)(MathHelper.atan2(f, d) * 180.0F / (float)Math.PI) - 90.0F);
               
               if(tickCount % 2 == 0){
                  this.removeElement(jeraldEntity);
                  jeraldEntity = new SimpleEntityElement(EntityType.WANDERING_TRADER);
                  jeraldEntity.setOffset(new Vec3d(0,0.0,0));
                  jeraldEntity.setYaw(yaw);
                  jeraldEntity.setPitch(pitch);
                  addElement(jeraldEntity);
               }
            }
            
            if(tickCount > 200){
               tickCount = 0;
            }
         }
      };
      nametag.setOffset(new Vec3d(0,2.25,0));
      jerald.setOffset(new Vec3d(0,0.0,0));
      click.setOffset(new Vec3d(0,0.25,0));
      
      nametag.setBillboardMode(DisplayEntity.BillboardMode.VERTICAL);
      
      holder.addElement(nametag);
      holder.addElement(jerald);
      holder.addElement(click);
      return holder;
   }
   
   public void loadDefaultShop(){
      offers.clear();
      int glassPrice = 99;
      int terracottaPrice = 99;
      int concretePrice = 99;
      int dirtsPrice = 45;
      
      offers.add(new Pair<>(new ItemStack(NationsRegistry.GROWTH_COIN_ITEM,1), new Pair<>(NationsRegistry.MATERIAL_COIN_ITEM,3)));
      offers.add(new Pair<>(new ItemStack(NationsRegistry.GROWTH_COIN_ITEM,1), new Pair<>(NationsRegistry.RESEARCH_COIN_ITEM,3)));
      offers.add(new Pair<>(new ItemStack(NationsRegistry.MATERIAL_COIN_ITEM,1), new Pair<>(NationsRegistry.GROWTH_COIN_ITEM,3)));
      offers.add(new Pair<>(new ItemStack(NationsRegistry.MATERIAL_COIN_ITEM,1), new Pair<>(NationsRegistry.RESEARCH_COIN_ITEM,3)));
      offers.add(new Pair<>(new ItemStack(NationsRegistry.RESEARCH_COIN_ITEM,1), new Pair<>(NationsRegistry.MATERIAL_COIN_ITEM,3)));
      offers.add(new Pair<>(new ItemStack(NationsRegistry.RESEARCH_COIN_ITEM,1), new Pair<>(NationsRegistry.GROWTH_COIN_ITEM,3)));
      offers.add(new Pair<>(new ItemStack(NationsRegistry.GROWTH_BULLION_ITEM,1), new Pair<>(NationsRegistry.MATERIAL_BULLION_ITEM,3)));
      offers.add(new Pair<>(new ItemStack(NationsRegistry.GROWTH_BULLION_ITEM,1), new Pair<>(NationsRegistry.RESEARCH_BULLION_ITEM,3)));
      offers.add(new Pair<>(new ItemStack(NationsRegistry.MATERIAL_BULLION_ITEM,1), new Pair<>(NationsRegistry.GROWTH_BULLION_ITEM,3)));
      offers.add(new Pair<>(new ItemStack(NationsRegistry.MATERIAL_BULLION_ITEM,1), new Pair<>(NationsRegistry.RESEARCH_BULLION_ITEM,3)));
      offers.add(new Pair<>(new ItemStack(NationsRegistry.RESEARCH_BULLION_ITEM,1), new Pair<>(NationsRegistry.MATERIAL_BULLION_ITEM,3)));
      offers.add(new Pair<>(new ItemStack(NationsRegistry.RESEARCH_BULLION_ITEM,1), new Pair<>(NationsRegistry.GROWTH_BULLION_ITEM,3)));
      
      addBlockTagOffer(BlockTags.LOGS,99);
      
      offers.add(getNormalSell(Items.GRASS_BLOCK,dirtsPrice));
      offers.add(getNormalSell(Items.DIRT,dirtsPrice));
      offers.add(getNormalSell(Items.COARSE_DIRT,dirtsPrice));
      offers.add(getNormalSell(Items.ROOTED_DIRT,dirtsPrice));
      offers.add(getNormalSell(Items.DIRT_PATH,dirtsPrice));
      offers.add(getNormalSell(Items.PODZOL,dirtsPrice));
      offers.add(getNormalSell(Items.MYCELIUM,dirtsPrice));
      
      offers.add(getNormalSell(Items.COBBLESTONE,45));
      offers.add(getNormalSell(Items.STONE,99));
      offers.add(getNormalSell(Items.GRANITE,99));
      offers.add(getNormalSell(Items.DIORITE,99));
      offers.add(getNormalSell(Items.ANDESITE,99));
      offers.add(getNormalSell(Items.DEEPSLATE,99));
      offers.add(getNormalSell(Items.COBBLED_DEEPSLATE,99));
      offers.add(getNormalSell(Items.CALCITE,99));
      offers.add(getNormalSell(Items.TUFF,250));
      offers.add(getNormalSell(Items.DRIPSTONE_BLOCK,99));
      offers.add(getNormalSell(Items.SMOOTH_STONE,99));
      offers.add(getNormalSell(Items.STONE_BRICKS,99));
      offers.add(getNormalSell(Items.BRICKS,99));
      offers.add(getNormalSell(Items.MOSSY_STONE_BRICKS,99));
      offers.add(getNormalSell(Items.CRACKED_STONE_BRICKS,99));
      offers.add(getNormalSell(Items.CHISELED_STONE_BRICKS,99));
      offers.add(getNormalSell(Items.MOSSY_COBBLESTONE,99));
      offers.add(getNormalSell(Items.BASALT,99));
      offers.add(getNormalSell(Items.SMOOTH_BASALT,99));
      offers.add(getNormalSell(Items.BLACKSTONE,99));
      offers.add(getNormalSell(Items.POLISHED_BLACKSTONE_BRICKS,99));
      offers.add(getNormalSell(Items.CRACKED_POLISHED_BLACKSTONE_BRICKS,99));
      offers.add(getNormalSell(Items.GILDED_BLACKSTONE,250));
      
      addBlockTagOffer(BlockTags.TERRACOTTA,terracottaPrice);
      
      offers.add(getNormalSell(Items.WHITE_GLAZED_TERRACOTTA,terracottaPrice));
      offers.add(getNormalSell(Items.ORANGE_GLAZED_TERRACOTTA,terracottaPrice));
      offers.add(getNormalSell(Items.MAGENTA_GLAZED_TERRACOTTA,terracottaPrice));
      offers.add(getNormalSell(Items.LIGHT_BLUE_GLAZED_TERRACOTTA,terracottaPrice));
      offers.add(getNormalSell(Items.RED_GLAZED_TERRACOTTA,terracottaPrice));
      offers.add(getNormalSell(Items.YELLOW_GLAZED_TERRACOTTA,terracottaPrice));
      offers.add(getNormalSell(Items.LIME_GLAZED_TERRACOTTA,terracottaPrice));
      offers.add(getNormalSell(Items.GREEN_GLAZED_TERRACOTTA,terracottaPrice));
      offers.add(getNormalSell(Items.BLUE_GLAZED_TERRACOTTA,terracottaPrice));
      offers.add(getNormalSell(Items.CYAN_GLAZED_TERRACOTTA,terracottaPrice));
      offers.add(getNormalSell(Items.PINK_GLAZED_TERRACOTTA,terracottaPrice));
      offers.add(getNormalSell(Items.PURPLE_GLAZED_TERRACOTTA,terracottaPrice));
      offers.add(getNormalSell(Items.BROWN_GLAZED_TERRACOTTA,terracottaPrice));
      offers.add(getNormalSell(Items.BLACK_GLAZED_TERRACOTTA,terracottaPrice));
      offers.add(getNormalSell(Items.GRAY_GLAZED_TERRACOTTA,terracottaPrice));
      offers.add(getNormalSell(Items.LIGHT_GRAY_GLAZED_TERRACOTTA,terracottaPrice));
      
      addBlockTagOffer(BlockTags.SAND,99);
      
      offers.add(getNormalSell(Items.WHITE_STAINED_GLASS,glassPrice));
      offers.add(getNormalSell(Items.ORANGE_STAINED_GLASS,glassPrice));
      offers.add(getNormalSell(Items.MAGENTA_STAINED_GLASS,glassPrice));
      offers.add(getNormalSell(Items.LIGHT_BLUE_STAINED_GLASS,glassPrice));
      offers.add(getNormalSell(Items.RED_STAINED_GLASS,glassPrice));
      offers.add(getNormalSell(Items.YELLOW_STAINED_GLASS,glassPrice));
      offers.add(getNormalSell(Items.LIME_STAINED_GLASS,glassPrice));
      offers.add(getNormalSell(Items.GREEN_STAINED_GLASS,glassPrice));
      offers.add(getNormalSell(Items.BLUE_STAINED_GLASS,glassPrice));
      offers.add(getNormalSell(Items.CYAN_STAINED_GLASS,glassPrice));
      offers.add(getNormalSell(Items.PINK_STAINED_GLASS,glassPrice));
      offers.add(getNormalSell(Items.PURPLE_STAINED_GLASS,glassPrice));
      offers.add(getNormalSell(Items.BROWN_STAINED_GLASS,glassPrice));
      offers.add(getNormalSell(Items.BLACK_STAINED_GLASS,glassPrice));
      offers.add(getNormalSell(Items.GRAY_STAINED_GLASS,glassPrice));
      offers.add(getNormalSell(Items.LIGHT_GRAY_STAINED_GLASS,glassPrice));
      offers.add(getNormalSell(Items.GLASS,glassPrice));
      
      offers.add(getNormalSell(Items.WHITE_CONCRETE,concretePrice));
      offers.add(getNormalSell(Items.ORANGE_CONCRETE,concretePrice));
      offers.add(getNormalSell(Items.MAGENTA_CONCRETE,concretePrice));
      offers.add(getNormalSell(Items.LIGHT_BLUE_CONCRETE,concretePrice));
      offers.add(getNormalSell(Items.RED_CONCRETE,concretePrice));
      offers.add(getNormalSell(Items.YELLOW_CONCRETE,concretePrice));
      offers.add(getNormalSell(Items.LIME_CONCRETE,concretePrice));
      offers.add(getNormalSell(Items.GREEN_CONCRETE,concretePrice));
      offers.add(getNormalSell(Items.BLUE_CONCRETE,concretePrice));
      offers.add(getNormalSell(Items.CYAN_CONCRETE,concretePrice));
      offers.add(getNormalSell(Items.PINK_CONCRETE,concretePrice));
      offers.add(getNormalSell(Items.PURPLE_CONCRETE,concretePrice));
      offers.add(getNormalSell(Items.BROWN_CONCRETE,concretePrice));
      offers.add(getNormalSell(Items.BLACK_CONCRETE,concretePrice));
      offers.add(getNormalSell(Items.GRAY_CONCRETE,concretePrice));
      offers.add(getNormalSell(Items.LIGHT_GRAY_CONCRETE,concretePrice));
      
      offers.add(getNormalSell(Items.PRISMARINE,45));
      offers.add(getNormalSell(Items.PRISMARINE_BRICKS,99));
      offers.add(getNormalSell(Items.DARK_PRISMARINE,99));
      offers.add(getNormalSell(Items.SEA_LANTERN,250));
      
      offers.add(getNormalSell(Items.WAXED_COPPER_BLOCK,250));
      offers.add(getNormalSell(Items.WAXED_EXPOSED_COPPER,250));
      offers.add(getNormalSell(Items.WAXED_WEATHERED_COPPER,250));
      offers.add(getNormalSell(Items.WAXED_OXIDIZED_COPPER,250));
      
      offers.add(getNormalSell(Items.IRON_BLOCK,1000));
      
      offers.add(getNormalSell(Items.NETHERRACK,45));
      offers.add(getNormalSell(Items.CRIMSON_NYLIUM,99));
      offers.add(getNormalSell(Items.WARPED_NYLIUM,99));
      offers.add(getNormalSell(Items.NETHER_BRICKS,99));
      offers.add(getNormalSell(Items.RED_NETHER_BRICKS,250));
      offers.add(getNormalSell(Items.SOUL_SOIL,99));
      offers.add(getNormalSell(Items.SOUL_SAND,99));
      offers.add(getNormalSell(Items.MAGMA_BLOCK,99));
      offers.add(getNormalSell(Items.QUARTZ_BLOCK,99));
      offers.add(getNormalSell(Items.SMOOTH_QUARTZ,99));
      offers.add(getNormalSell(Items.END_STONE,99));
      offers.add(getNormalSell(Items.END_STONE_BRICKS,99));
      offers.add(getNormalSell(Items.END_ROD,250));
      offers.add(getNormalSell(Items.PURPUR_BLOCK,99));
      offers.add(getNormalSell(Items.PURPUR_PILLAR,99));
      
      offers.add(getNormalSell(Items.PEARLESCENT_FROGLIGHT,320));
      offers.add(getNormalSell(Items.OCHRE_FROGLIGHT,320));
      offers.add(getNormalSell(Items.VERDANT_FROGLIGHT,320));
      offers.add(getNormalSell(Items.SHROOMLIGHT,320));
      offers.add(getNormalSell(Items.REDSTONE_LAMP,250));
      offers.add(getNormalSell(Items.GLOWSTONE,500));
      offers.add(getNormalSell(Items.REDSTONE_BLOCK,500));
      
      offers.add(getNormalSell(Items.AMETHYST_BLOCK,250));
      offers.add(getNormalSell(Items.HONEYCOMB_BLOCK,250));
      offers.add(getNormalSell(Items.RESIN_BLOCK,99));
      offers.add(getNormalSell(Items.RESIN_BRICKS,99));
      offers.add(getNormalSell(Items.TUBE_CORAL_BLOCK,99));
      offers.add(getNormalSell(Items.BRAIN_CORAL_BLOCK,99));
      offers.add(getNormalSell(Items.BUBBLE_CORAL_BLOCK,99));
      offers.add(getNormalSell(Items.FIRE_CORAL_BLOCK,99));
      offers.add(getNormalSell(Items.HORN_CORAL_BLOCK,99));
      offers.add(getNormalSell(Items.ICE,99));
      offers.add(getNormalSell(Items.PACKED_ICE,250));
      offers.add(getNormalSell(Items.BLUE_ICE,500));
      offers.add(getNormalSell(Items.SCULK,250));
      
      addBlockTagOffer(BlockTags.FLOWERS,45);
      addBlockTagOffer(BlockTags.SMALL_FLOWERS,45);
      
      offers.add(getGrowthSell(Items.OBSIDIAN,500));
      offers.add(getGrowthSell(Items.CRYING_OBSIDIAN,1000));
      offers.add(new Pair<>(new ItemStack(Items.DRAGON_BREATH,4), new Pair<>(NationsRegistry.GROWTH_COIN_ITEM,100)));
      offers.add(new Pair<>(new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE,1), new Pair<>(NationsRegistry.GROWTH_COIN_ITEM,2500)));
      offers.add(new Pair<>(new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE,1), new Pair<>(NationsRegistry.RESEARCH_COIN_ITEM,2500)));
   }
   
   private Pair<ItemStack,Pair<Item,Integer>> getNormalSell(Item item, int price){
      return new Pair<>(new ItemStack(item,item.getMaxCount()), new Pair<>(NationsRegistry.MATERIAL_COIN_ITEM,price));
   }
   
   private Pair<ItemStack,Pair<Item,Integer>> getGrowthSell(Item item, int price){
      return new Pair<>(new ItemStack(item,item.getMaxCount()), new Pair<>(NationsRegistry.GROWTH_COIN_ITEM,price));
   }
   
   private Pair<ItemStack,Pair<Item,Integer>> getResearchSell(Item item, int price){
      return new Pair<>(new ItemStack(item,item.getMaxCount()), new Pair<>(NationsRegistry.RESEARCH_COIN_ITEM,price));
   }
   
   private void addItemTagOffer(TagKey<Item> tag, int price){
      for(RegistryEntry<Item> itemEntry : Registries.ITEM.getIndexedEntries()){
         try{
            if(itemEntry.isIn(tag)){
               offers.add(getNormalSell(itemEntry.value(),price));
            }
         }catch(Exception e){
            log(2,"Error getting tags for "+itemEntry.getIdAsString());
         }
      }
   }
   
   private void addBlockTagOffer(TagKey<Block> tag, int price){
      for(RegistryEntry<Item> itemEntry : Registries.ITEM.getIndexedEntries()){
         if(itemEntry.value() instanceof BlockItem blockItem){
            if(blockItem.getBlock() != null && blockItem.getBlock().getRegistryEntry().isIn(tag)){
               offers.add(getNormalSell(itemEntry.value(),price));
            }
         }
      }
   }
   
   public void read(){
      try(BufferedReader input = new BufferedReader(new InputStreamReader(new FileInputStream(file)))){
         logger.info("Reading Nations Shop Data...");
         offers.clear();
         
         String line;
         while ((line = input.readLine()) != null) {
            if (!line.startsWith(" - ")) {
               continue;
            }
            
            String trimmed = line.substring(3).replaceAll("\\s+", " ").trim();
            String[] parts = trimmed.split("\\s+", 3);
            if (parts.length < 3) {
               logger.warn("Skipping malformed shop line (too few parts): {}", line);
               continue;
            }
            
            String itemId = parts[0];
            int amount;
            try {
               amount = Integer.parseInt(parts[1]);
            } catch (NumberFormatException nfe) {
               logger.warn("Skipping shop line with invalid number '{}': {}", parts[1], line);
               continue;
            }
            String snbt = parts[2];
            
            NbtCompound comp = StringNbtReader.parse(snbt);
            ItemStack sellStack = ItemStack.fromNbt(server.getRegistryManager(),comp).orElse(null);
            if(sellStack == null){
               logger.warn("Skipping shop line with invalid sell stack data: {}", line);
               continue;
            }
            
            Item buyItem = Registries.ITEM.get(Identifier.of(itemId));
            if(buyItem == null || amount < 0){
               logger.warn("Skipping shop line with invalid buy item or count: {}", line);
               continue;
            }
            
            offers.add(new Pair<>(sellStack,new Pair<>(buyItem,amount)));
         }
      }catch(FileNotFoundException ignored){
         logger.info("Initialising Nations Shop Data...");
         loadDefaultShop();
      }catch(IOException e){
         logger.fatal("Failed to load Nations Shop Data file!");
         loadDefaultShop();
         e.printStackTrace();
      }catch(Exception e){
         logger.fatal("Failed to parse Nations Shop Data");
         loadDefaultShop();
         e.printStackTrace();
      }
   }
   
   public void save(){
      logger.debug("Updating Nations Shop Data...");
      try(BufferedWriter output = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)))){
         output.write("# Nations Shop Data File" + " | " + new Date());
         output.newLine();
         output.newLine();
         for(Pair<ItemStack, Pair<Item,Integer>> offer : offers){
            ItemStack sellStack = offer.getLeft();
            Pair<Item,Integer> buyStack = offer.getRight();
            String buyId = Registries.ITEM.getId(buyStack.getLeft()).toString();
            int count = buyStack.getRight();
            NbtElement nbt = sellStack.toNbt(server.getRegistryManager());
            String sellNbt = nbt.asString();
            String line = " - " + buyId + " " + count + " " + sellNbt;
            output.write(line);
            output.newLine();
            output.newLine();
         }
      }catch(IOException e){
         logger.fatal("Failed to save Nations Shop Data file!");
         e.printStackTrace();
      }
   }
   
   
}
