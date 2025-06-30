package net.borisshoes.nations;

import com.mojang.serialization.Lifecycle;
import eu.pb4.polymer.core.api.item.PolymerItemGroupUtils;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import it.unimi.dsi.fastutil.Hash;
import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.borisshoes.arcananovum.ArcanaRegistry;
import net.borisshoes.arcananovum.core.ArcanaItem;
import net.borisshoes.nations.blocks.ContestBoundaryBlock;
import net.borisshoes.nations.gameplay.ResourceType;
import net.borisshoes.nations.items.*;
import net.borisshoes.nations.research.ResearchTech;
import net.borisshoes.nations.utils.ConfigUtils;
import net.borisshoes.nations.utils.MiscUtils;
import net.borisshoes.nations.utils.NationsColors;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.equipment.EquipmentAsset;
import net.minecraft.network.message.MessageType;
import net.minecraft.potion.Potion;
import net.minecraft.potion.Potions;
import net.minecraft.registry.*;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ChunkTicketType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.dimension.DimensionType;

import java.util.*;
import java.util.function.Supplier;

import static net.borisshoes.nations.Nations.MOD_ID;

@SuppressWarnings("unchecked")
public class NationsRegistry {
   public static final Registry<Item> ITEMS = new SimpleRegistry<>(RegistryKey.ofRegistry(Identifier.of(MOD_ID,"item")), Lifecycle.stable());
   public static final Registry<ResearchTech> RESEARCH = new SimpleRegistry<>(RegistryKey.ofRegistry(Identifier.of(MOD_ID,"research")), Lifecycle.stable());
   public static final Registry<NationsConfig.ConfigSetting<?>> CONFIG_SETTINGS = new SimpleRegistry<>(RegistryKey.ofRegistry(Identifier.of(MOD_ID,"config_settings")), Lifecycle.stable());
   public static final HashMap<Item,RegistryKey<ResearchTech>> LOCKED_ITEMS = new HashMap<>();
   public static final HashMap<Potion,RegistryKey<ResearchTech>> LOCKED_POTIONS = new HashMap<>();
   public static final HashMap<Potion,RegistryKey<ResearchTech>> POTION_TECHS = new HashMap<>();
   public static final HashMap<ArcanaItem,RegistryKey<ResearchTech>> ARCANA_TECHS = new HashMap<>();
   public static final HashMap<Pair<RegistryKey<Enchantment>,Integer>,RegistryKey<ResearchTech>> ENCHANT_TECHS = new HashMap<>();
   public static final HashMap<RegistryKey<Enchantment>, Item> ENCHANT_ITEM_MAP = new HashMap<>();
   public static final HashMap<RegistryKey<ResearchTech>, HashMap<Integer,RegistryKey<ResearchTech>>> NATION_BUFFS = new HashMap<>();
   
   public static final ChunkTicketType<ChunkPos> TICKET_TYPE = ChunkTicketType.create("nations.anchored", Comparator.comparingLong(ChunkPos::toLong));
   
   public static final RegistryKey<DamageType> CONTEST_DAMAGE = RegistryKey.of(RegistryKeys.DAMAGE_TYPE, Identifier.of(MOD_ID,"contest_damage"));
   
   public static final RegistryKey<? extends Registry<EquipmentAsset>> EQUIPMENT_ASSET_REGISTRY_KEY = RegistryKey.ofRegistry(Identifier.ofVanilla("equipment_asset"));
   
   public static final TagKey<Item> INFLUENCE_PROTECTED_ITEMS = TagKey.of(RegistryKeys.ITEM, Identifier.of(MOD_ID,"influence_protected_items"));
   public static final TagKey<Item> CLAIM_PROTECTED_ITEMS = TagKey.of(RegistryKeys.ITEM, Identifier.of(MOD_ID,"claim_protected_items"));
   public static final TagKey<Item> COIN_ITEMS = TagKey.of(RegistryKeys.ITEM, Identifier.of(MOD_ID,"coin_items"));
   
   public static final TagKey<Block> INFLUENCE_PROTECTED_BLOCKS = TagKey.of(RegistryKeys.BLOCK, Identifier.of(MOD_ID,"influence_protected_blocks"));
   public static final TagKey<Block> CLAIM_PROTECTED_BLOCKS = TagKey.of(RegistryKeys.BLOCK, Identifier.of(MOD_ID,"claim_protected_blocks"));
   
   public static final RegistryKey<MessageType> GLOBAL_MESSAGE = RegistryKey.of(RegistryKeys.MESSAGE_TYPE, Identifier.of(MOD_ID,"global_message"));
   public static final RegistryKey<MessageType> LOCAL_MESSAGE = RegistryKey.of(RegistryKeys.MESSAGE_TYPE, Identifier.of(MOD_ID,"local_message"));
   public static final RegistryKey<MessageType> NATION_MESSAGE = RegistryKey.of(RegistryKeys.MESSAGE_TYPE, Identifier.of(MOD_ID,"nation_message"));
   
   public static final Item GRAPHICAL_ITEM = registerItem("graphical_item", new GraphicalItem(new Item.Settings().maxCount(64)));
   public static final Item VICTORY_POINT_ITEM = registerItem("victory_point_item", new VictoryPointItem(new Item.Settings(),"victory_point_item"));
   public static final Item BUG_VOUCHER_ITEM = registerItem("bug_voucher", new BugVoucherItem(new Item.Settings(),"bug_voucher"));
   public static final Item COIN_PURSE_ITEM = registerItem("coin_purse", new CoinPurseItem(new Item.Settings(),"coin_purse"));
   public static final Item GROWTH_COIN_ITEM = registerItem("growth_coin", new ResourceCoinItem(new Item.Settings(), "growth_coin", NationsColors.GROWTH_COIN_COLOR, ResourceType.GROWTH));
   public static final Item MATERIAL_COIN_ITEM = registerItem("material_coin", new ResourceCoinItem(new Item.Settings(), "material_coin", NationsColors.MATERIAL_COIN_COLOR, ResourceType.MATERIAL));
   public static final Item RESEARCH_COIN_ITEM = registerItem("research_coin", new ResourceCoinItem(new Item.Settings(), "research_coin", NationsColors.RESEARCH_COIN_COLOR, ResourceType.RESEARCH));
   public static final Item GROWTH_BULLION_ITEM = registerItem("growth_bullion", new ResourceBullionItem(new Item.Settings(), "growth_bullion", NationsColors.GROWTH_COIN_COLOR, ResourceType.GROWTH));
   public static final Item MATERIAL_BULLION_ITEM = registerItem("material_bullion", new ResourceBullionItem(new Item.Settings(), "material_bullion", NationsColors.MATERIAL_COIN_COLOR, ResourceType.MATERIAL));
   public static final Item RESEARCH_BULLION_ITEM = registerItem("research_bullion", new ResourceBullionItem(new Item.Settings(), "research_bullion", NationsColors.RESEARCH_COIN_COLOR, ResourceType.RESEARCH));
   
   public static final Block CONTEST_BOUNDARY_BLOCK = registerBlock("contest_boundary_block", new ContestBoundaryBlock(
         AbstractBlock.Settings.create().strength(-1.0F, 3600000.0F).dropsNothing().allowsSpawning(Blocks::never).pistonBehavior(PistonBehavior.BLOCK).registryKey(RegistryKey.of(RegistryKeys.BLOCK,Identifier.of(MOD_ID,"contest_boundary_block")))
   ));
   
   public static final RegistryKey<World> CONTEST_DIM = RegistryKey.of(RegistryKeys.WORLD, Identifier.of(MOD_ID,"contest"));
   public static final RegistryKey<DimensionType> CONTEST_DIM_TYPE = RegistryKey.of(RegistryKeys.DIMENSION_TYPE, Identifier.of(MOD_ID,"contest"));
   
   public static final RegistryKey<ResearchTech> MECHANICS = of("mechanics");
   public static final RegistryKey<ResearchTech> SEMICONDUCTORS = of("semiconductors");
   public static final RegistryKey<ResearchTech> RESONATORS = of("resonators");
   public static final RegistryKey<ResearchTech> BRONZEWORKING = of("bronzeworking");
   public static final RegistryKey<ResearchTech> STEELWORKING = of("steelworking");
   public static final RegistryKey<ResearchTech> ARCHERY = of("archery");
   public static final RegistryKey<ResearchTech> BLACK_POWDER = of("black_powder");
   public static final RegistryKey<ResearchTech> ANCIENT_ALLOY = of("ancient_alloy");
   public static final RegistryKey<ResearchTech> CRYSTAL_COMPOSITE = of("crystal_composite");
   public static final RegistryKey<ResearchTech> HARDENED_PLATES = of("hardened_plates");
   public static final RegistryKey<ResearchTech> FIBERGLASS_COMPOSITE = of("fiberglass_composite");
   public static final RegistryKey<ResearchTech> GRAVITIC_WEAPONRY = of("gravitic_weaponry");
   public static final RegistryKey<ResearchTech> BASIC_ALCHEMY = of("basic_alchemy");
   public static final RegistryKey<ResearchTech> ENHANCED_ALCHEMY = of("enhanced_alchemy");
   public static final RegistryKey<ResearchTech> ADVANCED_ALCHEMY = of("advanced_alchemy");
   public static final RegistryKey<ResearchTech> POTENT_ALCHEMY = of("potent_alchemy");
   public static final RegistryKey<ResearchTech> ENDURING_ALCHEMY = of("enduring_alchemy");
   public static final RegistryKey<ResearchTech> PROLIFIC_ALCHEMY = of("prolific_alchemy");
   public static final RegistryKey<ResearchTech> TEMPERED_WEAPONS = of("tempered_weapons");
   public static final RegistryKey<ResearchTech> ANNEALED_ARMOR = of("annealed_armor");
   public static final RegistryKey<ResearchTech> ARCANA = of("arcana");
   public static final RegistryKey<ResearchTech> ALTARS = of("altars");
   public static final RegistryKey<ResearchTech> ENCHANTING = of("enchanting");
   public static final RegistryKey<ResearchTech> ENHANCED_ENCHANTING = of("enhanced_enchanting");
   public static final RegistryKey<ResearchTech> SMITHING = of("smithing");
   public static final RegistryKey<ResearchTech> FLETCHING = of("fletching");
   public static final RegistryKey<ResearchTech> FORGING = of("forging");
   public static final RegistryKey<ResearchTech> ADVANCED_ARCANA = of("advanced_arcana");
   public static final RegistryKey<ResearchTech> RUNIC_ARCHERY = of("runic_archery");
   public static final RegistryKey<ResearchTech> ARCHETYPE_CHANGE_ITEM = of("archetype_change_item");
   public static final RegistryKey<ResearchTech> BASIC_AUGMENTATION = of("basic_augmentation");
   public static final RegistryKey<ResearchTech> ENHANCED_AUGMENTATION = of("enhanced_augmentation");
   public static final RegistryKey<ResearchTech> ADVANCED_AUGMENTATION = of("advanced_augmentation");
   public static final RegistryKey<ResearchTech> MANIFEST_DESTINY = of("manifest_destiny");
   public static final RegistryKey<ResearchTech> AGRICULTURE = of("agriculture");
   public static final RegistryKey<ResearchTech> INFRASTRUCTURE = of("infrastructure");
   public static final RegistryKey<ResearchTech> PUBLIC_EDUCATION = of("public_education");
   public static final RegistryKey<ResearchTech> IMPERIALISM = of("imperialism");
   public static final RegistryKey<ResearchTech> COLONIALISM = of("colonialism");
   public static final RegistryKey<ResearchTech> SCHOLARSHIP = of("scholarship");
   public static final RegistryKey<ResearchTech> BARTERING = of("bartering");
   public static final RegistryKey<ResearchTech> MANIFEST_DESTINY_1 = of("manifest_destiny_1");
   public static final RegistryKey<ResearchTech> AGRICULTURE_1 = of("agriculture_1");
   public static final RegistryKey<ResearchTech> INFRASTRUCTURE_1 = of("infrastructure_1");
   public static final RegistryKey<ResearchTech> PUBLIC_EDUCATION_1 = of("public_education_1");
   public static final RegistryKey<ResearchTech> IMPERIALISM_1 = of("imperialism_1");
   public static final RegistryKey<ResearchTech> COLONIALISM_1 = of("colonialism_1");
   public static final RegistryKey<ResearchTech> SCHOLARSHIP_1 = of("scholarship_1");
   public static final RegistryKey<ResearchTech> BARTERING_1 = of("bartering_1");
   public static final RegistryKey<ResearchTech> MANIFEST_DESTINY_2 = of("manifest_destiny_2");
   public static final RegistryKey<ResearchTech> AGRICULTURE_2 = of("agriculture_2");
   public static final RegistryKey<ResearchTech> INFRASTRUCTURE_2 = of("infrastructure_2");
   public static final RegistryKey<ResearchTech> PUBLIC_EDUCATION_2 = of("public_education_2");
   public static final RegistryKey<ResearchTech> IMPERIALISM_2 = of("imperialism_2");
   public static final RegistryKey<ResearchTech> COLONIALISM_2 = of("colonialism_2");
   public static final RegistryKey<ResearchTech> SCHOLARSHIP_2 = of("scholarship_2");
   public static final RegistryKey<ResearchTech> BARTERING_2 = of("bartering_2");
   public static final RegistryKey<ResearchTech> MANIFEST_DESTINY_3 = of("manifest_destiny_3");
   public static final RegistryKey<ResearchTech> AGRICULTURE_3 = of("agriculture_3");
   public static final RegistryKey<ResearchTech> INFRASTRUCTURE_3 = of("infrastructure_3");
   public static final RegistryKey<ResearchTech> PUBLIC_EDUCATION_3 = of("public_education_3");
   public static final RegistryKey<ResearchTech> IMPERIALISM_3 = of("imperialism_3");
   public static final RegistryKey<ResearchTech> COLONIALISM_3 = of("colonialism_3");
   public static final RegistryKey<ResearchTech> SCHOLARSHIP_3 = of("scholarship_3");
   public static final RegistryKey<ResearchTech> BARTERING_3 = of("bartering_3");
   public static final RegistryKey<ResearchTech> MANIFEST_DESTINY_4 = of("manifest_destiny_4");
   public static final RegistryKey<ResearchTech> AGRICULTURE_4 = of("agriculture_4");
   public static final RegistryKey<ResearchTech> INFRASTRUCTURE_4 = of("infrastructure_4");
   public static final RegistryKey<ResearchTech> PUBLIC_EDUCATION_4 = of("public_education_4");
   public static final RegistryKey<ResearchTech> IMPERIALISM_4 = of("imperialism_4");
   public static final RegistryKey<ResearchTech> COLONIALISM_4 = of("colonialism_4");
   public static final RegistryKey<ResearchTech> SCHOLARSHIP_4 = of("scholarship_4");
   public static final RegistryKey<ResearchTech> BARTERING_4 = of("bartering_4");
   public static final RegistryKey<ResearchTech> MANIFEST_DESTINY_5 = of("manifest_destiny_5");
   public static final RegistryKey<ResearchTech> AGRICULTURE_5 = of("agriculture_5");
   public static final RegistryKey<ResearchTech> INFRASTRUCTURE_5 = of("infrastructure_5");
   public static final RegistryKey<ResearchTech> PUBLIC_EDUCATION_5 = of("public_education_5");
   public static final RegistryKey<ResearchTech> IMPERIALISM_5 = of("imperialism_5");
   public static final RegistryKey<ResearchTech> COLONIALISM_5 = of("colonialism_5");
   public static final RegistryKey<ResearchTech> SCHOLARSHIP_5 = of("scholarship_5");
   public static final RegistryKey<ResearchTech> BARTERING_5 = of("bartering_5");
   
   /*
    ======= Config Settings =======
    */
   
   public static final NationsConfig.ConfigSetting<?> SPAWN_RADIUS_CFG = registerConfigSetting(new NationsConfig.NormalConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("spawnRadius", 2, new ConfigUtils.IntegerConfigValue.IntLimits(0,1875000))));
   
   public static final NationsConfig.ConfigSetting<?> SPAWN_DMZ_RADIUS_CFG = registerConfigSetting(new NationsConfig.NormalConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("spawnDMZRadius", 6, new ConfigUtils.IntegerConfigValue.IntLimits(0,1875000))));
   
   public static final NationsConfig.ConfigSetting<?> WORLD_BORDER_RADIUS_OVERWORLD_CFG = registerConfigSetting(new NationsConfig.NormalConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("worldBorderRadiusOverworld", 64, new ConfigUtils.IntegerConfigValue.IntLimits(1,1875000))));
   
   public static final NationsConfig.ConfigSetting<?> WORLD_BORDER_RADIUS_NETHER_CFG = registerConfigSetting(new NationsConfig.NormalConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("worldBorderRadiusNether", 16, new ConfigUtils.IntegerConfigValue.IntLimits(1,1875000))));
   
   public static final NationsConfig.ConfigSetting<?> RESEARCH_TIER_CFG = registerConfigSetting(new NationsConfig.NormalConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("researchTier", 0, new ConfigUtils.IntegerConfigValue.IntLimits(0,100))));
   
   public static final NationsConfig.ConfigSetting<?> CAPTURE_POINT_MIN_DIST_CFG = registerConfigSetting(new NationsConfig.NormalConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("capturePointMinDist", 10, new ConfigUtils.IntegerConfigValue.IntLimits(5,100))));
   
   public static final NationsConfig.ConfigSetting<?> SETTLE_RADIUS_CFG = registerConfigSetting(new NationsConfig.NormalConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("settleRadius", 8, new ConfigUtils.IntegerConfigValue.IntLimits(1,100))));
   
   public static final NationsConfig.ConfigSetting<?> NETHER_PORTALS_DISABLED_CFG = registerConfigSetting(new NationsConfig.NormalConfigSetting<>(
         new ConfigUtils.BooleanConfigValue("netherPortalsDisabled", false)));
   
   public static final NationsConfig.ConfigSetting<?> RIFTS_ENABLED_CFG = registerConfigSetting(new NationsConfig.NormalConfigSetting<>(
         new ConfigUtils.BooleanConfigValue("riftsEnabled", false)));
   
   public static final NationsConfig.ConfigSetting<?> RIFT_WARMUP_CFG = registerConfigSetting(new NationsConfig.NormalConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("riftWarmup", 900, new ConfigUtils.IntegerConfigValue.IntLimits(-1))));
   
   public static final NationsConfig.ConfigSetting<?> RIFT_DURATION_CFG = registerConfigSetting(new NationsConfig.NormalConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("riftDuration", 5400, new ConfigUtils.IntegerConfigValue.IntLimits(600))));
   
   public static final NationsConfig.ConfigSetting<?> RIFT_MIN_COOLDOWN_CFG = registerConfigSetting(new NationsConfig.NormalConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("riftMinCooldown", 240, new ConfigUtils.IntegerConfigValue.IntLimits(1))));
   
   public static final NationsConfig.ConfigSetting<?> RIFT_MAX_COOLDOWN_CFG = registerConfigSetting(new NationsConfig.NormalConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("riftMaxCooldown", 480, new ConfigUtils.IntegerConfigValue.IntLimits(1))));
   
   public static final NationsConfig.ConfigSetting<?> CAPTURE_POINT_AUCTION_DURATION_CFG = registerConfigSetting(new NationsConfig.NormalConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("capturePointAuctionDuration", 1440, new ConfigUtils.IntegerConfigValue.IntLimits(1))));
   
   public static final NationsConfig.ConfigSetting<?> CAPTURE_POINT_INFLUENCE_DISTANCE_MOD_CFG = registerConfigSetting(new NationsConfig.NormalConfigSetting<>(
         new ConfigUtils.DoubleConfigValue("capturePointInfluenceDistanceModifier", 50.0, new ConfigUtils.DoubleConfigValue.DoubleLimits(0))));
   
   public static final NationsConfig.ConfigSetting<?> CAPTURE_POINT_AUCTION_MOD_MIN_CFG = registerConfigSetting(new NationsConfig.NormalConfigSetting<>(
         new ConfigUtils.DoubleConfigValue("capturePointAuctionModifierMinimum", 0.5, new ConfigUtils.DoubleConfigValue.DoubleLimits(0,2))));
   
   public static final NationsConfig.ConfigSetting<?> CLAIM_COIN_COST_CFG = registerConfigSetting(new NationsConfig.NormalConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("claimCoinCost", 250, new ConfigUtils.IntegerConfigValue.IntLimits(1))));
   
   public static final NationsConfig.ConfigSetting<?> INFLUENCE_COIN_COST_CFG = registerConfigSetting(new NationsConfig.NormalConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("influenceCoinCost", 100, new ConfigUtils.IntegerConfigValue.IntLimits(1))));
   
   public static final NationsConfig.ConfigSetting<?> TERRITORY_COMPACTNESS_MINIMUM_CFG = registerConfigSetting(new NationsConfig.NormalConfigSetting<>(
         new ConfigUtils.DoubleConfigValue("territoryCompactnessMinimum", 0.25, new ConfigUtils.DoubleConfigValue.DoubleLimits(0,1))));
   
   public static final NationsConfig.ConfigSetting<?> TERRITORY_COMPACTNESS_COST_MIDPOINT_CFG = registerConfigSetting(new NationsConfig.NormalConfigSetting<>(
         new ConfigUtils.DoubleConfigValue("territoryCompactnessCostMidpoint", 0.5, new ConfigUtils.DoubleConfigValue.DoubleLimits(1e-12,1-1e-12))));
   
   public static final NationsConfig.ConfigSetting<?> TERRITORY_COMPACTNESS_COST_MAX_REDUCTION_CFG = registerConfigSetting(new NationsConfig.NormalConfigSetting<>(
         new ConfigUtils.DoubleConfigValue("territoryCompactnessCostMaxReduction", 0.75, new ConfigUtils.DoubleConfigValue.DoubleLimits(0,1.0))));
   
   public static final NationsConfig.ConfigSetting<?> TERRITORY_COMPACTNESS_COST_MAX_INCREASE_CFG = registerConfigSetting(new NationsConfig.NormalConfigSetting<>(
         new ConfigUtils.DoubleConfigValue("territoryCompactnessCostMaxIncrease", 5.0, new ConfigUtils.DoubleConfigValue.DoubleLimits(1.0))));
   
   public static final NationsConfig.ConfigSetting<?> TERRITORY_COST_MODIFIER_CFG = registerConfigSetting(new NationsConfig.NormalConfigSetting<>(
         new ConfigUtils.DoubleConfigValue("territoryCostModifier", 0.3, new ConfigUtils.DoubleConfigValue.DoubleLimits(0))));
   
   public static final NationsConfig.ConfigSetting<?> IMPROVEMENT_FARMLAND_COST_CFG = registerConfigSetting(new NationsConfig.NormalConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("improvementFarmlandCost", 250, new ConfigUtils.IntegerConfigValue.IntLimits(0))));
   
   public static final NationsConfig.ConfigSetting<?> IMPROVEMENT_MACHINERY_COST_CFG = registerConfigSetting(new NationsConfig.NormalConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("improvementMachineryCost", 150, new ConfigUtils.IntegerConfigValue.IntLimits(0))));
   
   public static final NationsConfig.ConfigSetting<?> IMPROVEMENT_ANCHORED_COST_CFG = registerConfigSetting(new NationsConfig.NormalConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("improvementAnchoredCost", 1000, new ConfigUtils.IntegerConfigValue.IntLimits(0))));
   
   public static final NationsConfig.ConfigSetting<?> VICTORY_POINTS_CAP_CFG = registerConfigSetting(new NationsConfig.NormalConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("victoryPointsCapturePointDaily", 25, new ConfigUtils.IntegerConfigValue.IntLimits(0))));
   
   public static final NationsConfig.ConfigSetting<?> VICTORY_POINTS_RESEARCH_CFG = registerConfigSetting(new NationsConfig.NormalConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("victoryPointsResearchComplete", 150, new ConfigUtils.IntegerConfigValue.IntLimits(0))));
   
   public static final NationsConfig.ConfigSetting<?> VICTORY_POINTS_RESEARCH_BONUS_CFG = registerConfigSetting(new NationsConfig.NormalConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("victoryPointsResearchFirst", 100, new ConfigUtils.IntegerConfigValue.IntLimits(0))));
   
   public static final NationsConfig.ConfigSetting<?> VICTORY_POINTS_DEATH_PENALTY_CFG = registerConfigSetting(new NationsConfig.NormalConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("victoryPointsDeathPenalty", 10, new ConfigUtils.IntegerConfigValue.IntLimits(0))));
   
   public static final NationsConfig.ConfigSetting<?> VICTORY_POINTS_KILL_CFG = registerConfigSetting(new NationsConfig.NormalConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("victoryPointsKill", 10, new ConfigUtils.IntegerConfigValue.IntLimits(0))));
   
   public static final NationsConfig.ConfigSetting<?> VICTORY_POINTS_LOGIN_CFG = registerConfigSetting(new NationsConfig.NormalConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("victoryPointsDailyLogin", 15, new ConfigUtils.IntegerConfigValue.IntLimits(0))));
   
   public static final NationsConfig.ConfigSetting<?> WAR_ENABLED_CFG = registerConfigSetting(new NationsConfig.NormalConfigSetting<>(
         new ConfigUtils.BooleanConfigValue("warEnabled", false)));
   
   public static final NationsConfig.ConfigSetting<?> WAR_DURATION_CFG = registerConfigSetting(new NationsConfig.NormalConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("warDuration", 120, new ConfigUtils.IntegerConfigValue.IntLimits(1))));
   
   public static final NationsConfig.ConfigSetting<?> WAR_CYCLES_CFG = registerConfigSetting(new NationsConfig.NormalConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("warCycles", 3, new ConfigUtils.IntegerConfigValue.IntLimits(1))));
   
   public static final NationsConfig.ConfigSetting<?> WAR_ATTACK_LIMIT_CFG = registerConfigSetting(new NationsConfig.NormalConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("warAttackLimit", 6, new ConfigUtils.IntegerConfigValue.IntLimits(1))));
   
   public static final NationsConfig.ConfigSetting<?> WAR_ATTACK_COST_CFG = registerConfigSetting(new NationsConfig.NormalConfigSetting<>(
         new ConfigUtils.DoubleConfigValue("warAttackCost", 3.0, new ConfigUtils.DoubleConfigValue.DoubleLimits(0))));
   
   public static final NationsConfig.ConfigSetting<?> WAR_ATTACK_CAPTURE_DURATION_CFG = registerConfigSetting(new NationsConfig.NormalConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("warAttackCaptureDuration", 60, new ConfigUtils.IntegerConfigValue.IntLimits(1))));
   
   public static final NationsConfig.ConfigSetting<?> WAR_CONTEST_DURATION_CFG = registerConfigSetting(new NationsConfig.NormalConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("warContestDuration", 10, new ConfigUtils.IntegerConfigValue.IntLimits(1))));
   
   public static final NationsConfig.ConfigSetting<?> WAR_DEFEND_WIN_DURATION = registerConfigSetting(new NationsConfig.NormalConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("warDefendWinDuration", 168, new ConfigUtils.IntegerConfigValue.IntLimits(1))));
   
   public static final NationsConfig.ConfigSetting<?> WAR_DEFEND_WIN_MULTIPLIER_CFG = registerConfigSetting(new NationsConfig.NormalConfigSetting<>(
         new ConfigUtils.DoubleConfigValue("warDefendWinMultiplier", 3.0, new ConfigUtils.DoubleConfigValue.DoubleLimits(1.0))));
   
   public static final NationsConfig.ConfigSetting<?> WAR_BLOCKADE_DURATION_CFG = registerConfigSetting(new NationsConfig.NormalConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("warBlockadeDuration", 168, new ConfigUtils.IntegerConfigValue.IntLimits(1))));
   
   public static final NationsConfig.ConfigSetting<?> WAR_DEFENSE_RADIUS_CFG = registerConfigSetting(new NationsConfig.NormalConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("warDefenseRadius", 64, new ConfigUtils.IntegerConfigValue.IntLimits(0))));
   
   public static final NationsConfig.ConfigSetting<?> WAR_MINIMUM_CAPTURE_POINT_DIFFERENCE_CFG = registerConfigSetting(new NationsConfig.NormalConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("warMinimumCapturePointDifference", 12, new ConfigUtils.IntegerConfigValue.IntLimits(0))));
   
   public static final NationsConfig.ConfigSetting<?> STACK_OVERDAMAGE_CFG = registerConfigSetting(new NationsConfig.NormalConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("stackOverdamageAmount", 5, new ConfigUtils.IntegerConfigValue.IntLimits(0))));
   
   public static final NationsConfig.ConfigSetting<?> TRESPASS_ALERTS_CFG = registerConfigSetting(new NationsConfig.NormalConfigSetting<>(
         new ConfigUtils.BooleanConfigValue("trespassAlerts", true)));
   
   public static final NationsConfig.ConfigSetting<?> DEATH_PROTECTOR_COOLDOWN_CFG = registerConfigSetting(new NationsConfig.NormalConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("deathProtectorCooldown", 60, new ConfigUtils.IntegerConfigValue.IntLimits(0))));
   
   public static final NationsConfig.ConfigSetting<?> COMBAT_LOG_DURATION_CFG = registerConfigSetting(new NationsConfig.NormalConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("combatLogDuration", 120, new ConfigUtils.IntegerConfigValue.IntLimits(0))));
   
   public static final NationsConfig.ConfigSetting<?> COMBAT_LOG_GRACE_PERIOD_CFG = registerConfigSetting(new NationsConfig.NormalConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("combatLogGracePeriod", 15, new ConfigUtils.IntegerConfigValue.IntLimits(0))));
   
   public static final NationsConfig.ConfigSetting<?> TICK_RESEARCH_CFG = registerConfigSetting(new NationsConfig.NormalConfigSetting<>(
         new ConfigUtils.BooleanConfigValue("tickResearch", true)));
   
   public static final NationsConfig.ConfigSetting<?> CAPTURE_POINT_COIN_GEN_CFG = registerConfigSetting(new NationsConfig.NormalConfigSetting<>(
         new ConfigUtils.BooleanConfigValue("capturePointCoinGeneration", true)));
   
   public static final NationsConfig.ConfigSetting<?> MONUMENT_COIN_GEN_CFG = registerConfigSetting(new NationsConfig.NormalConfigSetting<>(
         new ConfigUtils.BooleanConfigValue("monumentCoinGeneration", true)));
   
   public static final NationsConfig.ConfigSetting<?> CHUNK_YIELD_MODIFIER_CFG = registerConfigSetting(new NationsConfig.NormalConfigSetting<>(
         new ConfigUtils.DoubleConfigValue("chunkYieldModifier", 0.01, new ConfigUtils.DoubleConfigValue.DoubleLimits(0.0))));
   
   public static final NationsConfig.ConfigSetting<?> CHUNK_CACHE_UPDATES_PER_MINUTE_CFG = registerConfigSetting(new NationsConfig.NormalConfigSetting<>(
         new ConfigUtils.DoubleConfigValue("chunkCacheUpdatesPerMinute", 100.0, new ConfigUtils.DoubleConfigValue.DoubleLimits(0.0))));
   
   public static final NationsConfig.ConfigSetting<?> MANIFEST_DESTINY_REDUCTION_CFG = registerConfigSetting(new NationsConfig.NormalConfigSetting<>(
         new ConfigUtils.DoubleConfigValue("manifestDestinyReduction", 0.1, new ConfigUtils.DoubleConfigValue.DoubleLimits(0.0))));
   
   public static final NationsConfig.ConfigSetting<?> AGRICULTURE_INCREASE_CFG = registerConfigSetting(new NationsConfig.NormalConfigSetting<>(
         new ConfigUtils.DoubleConfigValue("agricultureIncrease", 0.15, new ConfigUtils.DoubleConfigValue.DoubleLimits(0.0))));
   
   public static final NationsConfig.ConfigSetting<?> INFRASTRUCTURE_INCREASE_CFG = registerConfigSetting(new NationsConfig.NormalConfigSetting<>(
         new ConfigUtils.DoubleConfigValue("infrastructureIncrease", 0.15, new ConfigUtils.DoubleConfigValue.DoubleLimits(0.0))));
   
   public static final NationsConfig.ConfigSetting<?> PUBLIC_EDUCATION_INCREASE_CFG = registerConfigSetting(new NationsConfig.NormalConfigSetting<>(
         new ConfigUtils.DoubleConfigValue("publicEducationIncrease", 0.15, new ConfigUtils.DoubleConfigValue.DoubleLimits(0.0))));
   
   public static final NationsConfig.ConfigSetting<?> IMPERIALISM_DECREASE_CFG = registerConfigSetting(new NationsConfig.NormalConfigSetting<>(
         new ConfigUtils.DoubleConfigValue("imperialismDecrease", 0.1, new ConfigUtils.DoubleConfigValue.DoubleLimits(0.0))));
   
   public static final NationsConfig.ConfigSetting<?> COLONIALISM_INCREASE_CFG = registerConfigSetting(new NationsConfig.NormalConfigSetting<>(
         new ConfigUtils.DoubleConfigValue("colonialismIncrease", 0.2, new ConfigUtils.DoubleConfigValue.DoubleLimits(0.0))));
   
   public static final NationsConfig.ConfigSetting<?> SCHOLARSHIP_INCREASE_CFG = registerConfigSetting(new NationsConfig.NormalConfigSetting<>(
         new ConfigUtils.DoubleConfigValue("scholarshipIncrease", 0.1, new ConfigUtils.DoubleConfigValue.DoubleLimits(0.0))));
   
   public static final NationsConfig.ConfigSetting<?> BARTERING_DISCOUNT_CFG = registerConfigSetting(new NationsConfig.NormalConfigSetting<>(
         new ConfigUtils.DoubleConfigValue("barteringDiscount", 0.15, new ConfigUtils.DoubleConfigValue.DoubleLimits(0.0))));
   
   public static final NationsConfig.ConfigSetting<?> BARTERING_CONVERSION_INCREASE_CFG = registerConfigSetting(new NationsConfig.NormalConfigSetting<>(
         new ConfigUtils.DoubleConfigValue("barteringConversionIncrease", 0.125, new ConfigUtils.DoubleConfigValue.DoubleLimits(0.0))));
   
   // Biome Coin Configs
   
   public static final NationsConfig.ConfigSetting<?> BIOME_COINS_THE_VOID_CFG = registerConfigSetting(new NationsConfig.BiomeConfigSetting<>(
         new ConfigUtils.IntArrayConfigValue("biomeCoinsTheVoid", new int[]{0,0,0}), BiomeKeys.THE_VOID));
   
   public static final NationsConfig.ConfigSetting<?> BIOME_COINS_PLAINS_CFG = registerConfigSetting(new NationsConfig.BiomeConfigSetting<>(
         new ConfigUtils.IntArrayConfigValue("biomeCoinsPlains", new int[]{7,2,1}), BiomeKeys.PLAINS));
   
   public static final NationsConfig.ConfigSetting<?> BIOME_COINS_SUNFLOWER_PLAINS_CFG = registerConfigSetting(new NationsConfig.BiomeConfigSetting<>(
         new ConfigUtils.IntArrayConfigValue("biomeCoinsSunflowerPlains", new int[]{8,2,2}), BiomeKeys.SUNFLOWER_PLAINS));
   
   public static final NationsConfig.ConfigSetting<?> BIOME_COINS_MEADOW_CFG = registerConfigSetting(new NationsConfig.BiomeConfigSetting<>(
         new ConfigUtils.IntArrayConfigValue("biomeCoinsMeadow", new int[]{7,2,2}), BiomeKeys.MEADOW));
   
   public static final NationsConfig.ConfigSetting<?> BIOME_COINS_CHERRY_GROVE_CFG = registerConfigSetting(new NationsConfig.BiomeConfigSetting<>(
         new ConfigUtils.IntArrayConfigValue("biomeCoinsCherryGrove", new int[]{6,3,3}), BiomeKeys.CHERRY_GROVE));
   
   public static final NationsConfig.ConfigSetting<?> BIOME_COINS_GROVE_CFG = registerConfigSetting(new NationsConfig.BiomeConfigSetting<>(
         new ConfigUtils.IntArrayConfigValue("biomeCoinsGrove", new int[]{7,2,2}), BiomeKeys.GROVE));
   
   public static final NationsConfig.ConfigSetting<?> BIOME_COINS_WARM_OCEAN_CFG = registerConfigSetting(new NationsConfig.BiomeConfigSetting<>(
         new ConfigUtils.IntArrayConfigValue("biomeCoinsWarmOcean", new int[]{7,1,4}), BiomeKeys.WARM_OCEAN));
   
   public static final NationsConfig.ConfigSetting<?> BIOME_COINS_LUKEWARM_OCEAN_CFG = registerConfigSetting(new NationsConfig.BiomeConfigSetting<>(
         new ConfigUtils.IntArrayConfigValue("biomeCoinsLukewarmOcean", new int[]{7,1,2}), BiomeKeys.LUKEWARM_OCEAN));
   
   public static final NationsConfig.ConfigSetting<?> BIOME_COINS_DEEP_LUKEWARM_OCEAN_CFG = registerConfigSetting(new NationsConfig.BiomeConfigSetting<>(
         new ConfigUtils.IntArrayConfigValue("biomeCoinsDeepLukewarmOcean", new int[]{7,1,3}), BiomeKeys.DEEP_LUKEWARM_OCEAN));
   
   public static final NationsConfig.ConfigSetting<?> BIOME_COINS_OCEAN_CFG = registerConfigSetting(new NationsConfig.BiomeConfigSetting<>(
         new ConfigUtils.IntArrayConfigValue("biomeCoinsOcean", new int[]{7,2,1}), BiomeKeys.OCEAN));
   
   public static final NationsConfig.ConfigSetting<?> BIOME_COINS_DEEP_OCEAN_CFG = registerConfigSetting(new NationsConfig.BiomeConfigSetting<>(
         new ConfigUtils.IntArrayConfigValue("biomeCoinsDeepOcean", new int[]{7,2,2}), BiomeKeys.DEEP_OCEAN));
   
   public static final NationsConfig.ConfigSetting<?> BIOME_COINS_COLD_OCEAN_CFG = registerConfigSetting(new NationsConfig.BiomeConfigSetting<>(
         new ConfigUtils.IntArrayConfigValue("biomeCoinsColdOcean", new int[]{6,2,2}), BiomeKeys.COLD_OCEAN));
   
   public static final NationsConfig.ConfigSetting<?> BIOME_COINS_DEEP_COLD_OCEAN_CFG = registerConfigSetting(new NationsConfig.BiomeConfigSetting<>(
         new ConfigUtils.IntArrayConfigValue("biomeCoinsDeepColdOcean", new int[]{6,2,3}), BiomeKeys.DEEP_COLD_OCEAN));
   
   public static final NationsConfig.ConfigSetting<?> BIOME_COINS_FROZEN_OCEAN_CFG = registerConfigSetting(new NationsConfig.BiomeConfigSetting<>(
         new ConfigUtils.IntArrayConfigValue("biomeCoinsFrozenOcean", new int[]{5,2,3}), BiomeKeys.FROZEN_OCEAN));
   
   public static final NationsConfig.ConfigSetting<?> BIOME_COINS_DEEP_FROZEN_OCEAN_CFG = registerConfigSetting(new NationsConfig.BiomeConfigSetting<>(
         new ConfigUtils.IntArrayConfigValue("biomeCoinsDeepFrozenOcean", new int[]{5,2,4}), BiomeKeys.DEEP_FROZEN_OCEAN));
   
   public static final NationsConfig.ConfigSetting<?> BIOME_COINS_LUSH_CAVES_CFG = registerConfigSetting(new NationsConfig.BiomeConfigSetting<>(
         new ConfigUtils.IntArrayConfigValue("biomeCoinsLushCaves", new int[]{9,2,4}), BiomeKeys.LUSH_CAVES));
   
   public static final NationsConfig.ConfigSetting<?> BIOME_COINS_RIVER_CFG = registerConfigSetting(new NationsConfig.BiomeConfigSetting<>(
         new ConfigUtils.IntArrayConfigValue("biomeCoinsRiver", new int[]{7,2,1}), BiomeKeys.RIVER));
   
   public static final NationsConfig.ConfigSetting<?> BIOME_COINS_SWAMP_CFG = registerConfigSetting(new NationsConfig.BiomeConfigSetting<>(
         new ConfigUtils.IntArrayConfigValue("biomeCoinsSwamp", new int[]{6,3,2}), BiomeKeys.SWAMP));
   
   public static final NationsConfig.ConfigSetting<?> BIOME_COINS_MANGROVE_SWAMP_CFG = registerConfigSetting(new NationsConfig.BiomeConfigSetting<>(
         new ConfigUtils.IntArrayConfigValue("biomeCoinsMangroveSwamp", new int[]{6,4,3}), BiomeKeys.MANGROVE_SWAMP));
   
   public static final NationsConfig.ConfigSetting<?> BIOME_COINS_JUNGLE_CFG = registerConfigSetting(new NationsConfig.BiomeConfigSetting<>(
         new ConfigUtils.IntArrayConfigValue("biomeCoinsJungle", new int[]{6,3,2}), BiomeKeys.JUNGLE));
   
   public static final NationsConfig.ConfigSetting<?> BIOME_COINS_SPARSE_JUNGLE_CFG = registerConfigSetting(new NationsConfig.BiomeConfigSetting<>(
         new ConfigUtils.IntArrayConfigValue("biomeCoinsSparseJungle", new int[]{7,2,2}), BiomeKeys.SPARSE_JUNGLE));
   
   public static final NationsConfig.ConfigSetting<?> BIOME_COINS_BAMBOO_JUNGLE_CFG = registerConfigSetting(new NationsConfig.BiomeConfigSetting<>(
         new ConfigUtils.IntArrayConfigValue("biomeCoinsBambooJungle", new int[]{7,3,2}), BiomeKeys.BAMBOO_JUNGLE));
   
   public static final NationsConfig.ConfigSetting<?> BIOME_COINS_FOREST_CFG = registerConfigSetting(new NationsConfig.BiomeConfigSetting<>(
         new ConfigUtils.IntArrayConfigValue("biomeCoinsForest", new int[]{3,6,1}), BiomeKeys.FOREST));
   
   public static final NationsConfig.ConfigSetting<?> BIOME_COINS_FLOWER_FOREST_CFG = registerConfigSetting(new NationsConfig.BiomeConfigSetting<>(
         new ConfigUtils.IntArrayConfigValue("biomeCoinsFlowerForest", new int[]{4,6,2}), BiomeKeys.FLOWER_FOREST));
   
   public static final NationsConfig.ConfigSetting<?> BIOME_COINS_BIRCH_FOREST_CFG = registerConfigSetting(new NationsConfig.BiomeConfigSetting<>(
         new ConfigUtils.IntArrayConfigValue("biomeCoinsBirchForest", new int[]{3,6,2}), BiomeKeys.BIRCH_FOREST));
   
   public static final NationsConfig.ConfigSetting<?> BIOME_COINS_OLD_GROWTH_BIRCH_FOREST_CFG = registerConfigSetting(new NationsConfig.BiomeConfigSetting<>(
         new ConfigUtils.IntArrayConfigValue("biomeCoinsOldGrowthBirchForest", new int[]{3,6,3}), BiomeKeys.OLD_GROWTH_BIRCH_FOREST));
   
   public static final NationsConfig.ConfigSetting<?> BIOME_COINS_OLD_GROWTH_PINE_TAIGA_CFG = registerConfigSetting(new NationsConfig.BiomeConfigSetting<>(
         new ConfigUtils.IntArrayConfigValue("biomeCoinsOldGrowthPineTaiga", new int[]{2,7,3}), BiomeKeys.OLD_GROWTH_PINE_TAIGA));
   
   public static final NationsConfig.ConfigSetting<?> BIOME_COINS_OLD_GROWTH_SPRUCE_TAIGA_CFG = registerConfigSetting(new NationsConfig.BiomeConfigSetting<>(
         new ConfigUtils.IntArrayConfigValue("biomeCoinsOldGrowthSpruceTaiga", new int[]{2,7,3}), BiomeKeys.OLD_GROWTH_SPRUCE_TAIGA));
   
   public static final NationsConfig.ConfigSetting<?> BIOME_COINS_TAIGA_CFG = registerConfigSetting(new NationsConfig.BiomeConfigSetting<>(
         new ConfigUtils.IntArrayConfigValue("biomeCoinsTaiga", new int[]{2,6,2}), BiomeKeys.TAIGA));
   
   public static final NationsConfig.ConfigSetting<?> BIOME_COINS_SNOWY_TAIGA_CFG = registerConfigSetting(new NationsConfig.BiomeConfigSetting<>(
         new ConfigUtils.IntArrayConfigValue("biomeCoinsSnowyTaiga", new int[]{1,6,3}), BiomeKeys.SNOWY_TAIGA));
   
   public static final NationsConfig.ConfigSetting<?> BIOME_COINS_SAVANNA_PLATEAU_CFG = registerConfigSetting(new NationsConfig.BiomeConfigSetting<>(
         new ConfigUtils.IntArrayConfigValue("biomeCoinsSavannaPlateau", new int[]{2,6,2}), BiomeKeys.SAVANNA_PLATEAU));
   
   public static final NationsConfig.ConfigSetting<?> BIOME_COINS_WINDSWEPT_HILLS_CFG = registerConfigSetting(new NationsConfig.BiomeConfigSetting<>(
         new ConfigUtils.IntArrayConfigValue("biomeCoinsWindsweptHills", new int[]{2,7,2}), BiomeKeys.WINDSWEPT_HILLS));
   
   public static final NationsConfig.ConfigSetting<?> BIOME_COINS_WINDSWEPT_GRAVELLY_HILLS_CFG = registerConfigSetting(new NationsConfig.BiomeConfigSetting<>(
         new ConfigUtils.IntArrayConfigValue("biomeCoinsWindsweptGravellyHills", new int[]{1,7,3}), BiomeKeys.WINDSWEPT_GRAVELLY_HILLS));
   
   public static final NationsConfig.ConfigSetting<?> BIOME_COINS_WINDSWEPT_FOREST_CFG = registerConfigSetting(new NationsConfig.BiomeConfigSetting<>(
         new ConfigUtils.IntArrayConfigValue("biomeCoinsWindsweptForest", new int[]{3,6,2}), BiomeKeys.WINDSWEPT_FOREST));
   
   public static final NationsConfig.ConfigSetting<?> BIOME_COINS_WINDSWEPT_SAVANNA_CFG = registerConfigSetting(new NationsConfig.BiomeConfigSetting<>(
         new ConfigUtils.IntArrayConfigValue("biomeCoinsWindsweptSavanna", new int[]{1,8,5}), BiomeKeys.WINDSWEPT_SAVANNA));
   
   public static final NationsConfig.ConfigSetting<?> BIOME_COINS_SNOWY_SLOPES_CFG = registerConfigSetting(new NationsConfig.BiomeConfigSetting<>(
         new ConfigUtils.IntArrayConfigValue("biomeCoinsSnowySlopes", new int[]{1,6,5}), BiomeKeys.SNOWY_SLOPES));
   
   public static final NationsConfig.ConfigSetting<?> BIOME_COINS_FROZEN_PEAKS_CFG = registerConfigSetting(new NationsConfig.BiomeConfigSetting<>(
         new ConfigUtils.IntArrayConfigValue("biomeCoinsFrozenPeaks", new int[]{1,7,4}), BiomeKeys.FROZEN_PEAKS));
   
   public static final NationsConfig.ConfigSetting<?> BIOME_COINS_JAGGED_PEAKS_CFG = registerConfigSetting(new NationsConfig.BiomeConfigSetting<>(
         new ConfigUtils.IntArrayConfigValue("biomeCoinsJaggedPeaks", new int[]{2,7,3}), BiomeKeys.JAGGED_PEAKS));
   
   public static final NationsConfig.ConfigSetting<?> BIOME_COINS_STONY_PEAKS_CFG = registerConfigSetting(new NationsConfig.BiomeConfigSetting<>(
         new ConfigUtils.IntArrayConfigValue("biomeCoinsStonyPeaks", new int[]{2,7,3}), BiomeKeys.STONY_PEAKS));
   
   public static final NationsConfig.ConfigSetting<?> BIOME_COINS_STONY_SHORE_CFG = registerConfigSetting(new NationsConfig.BiomeConfigSetting<>(
         new ConfigUtils.IntArrayConfigValue("biomeCoinsStonyShore", new int[]{3,5,2}), BiomeKeys.STONY_SHORE));
   
   public static final NationsConfig.ConfigSetting<?> BIOME_COINS_DRIPSTONE_CAVES_CFG = registerConfigSetting(new NationsConfig.BiomeConfigSetting<>(
         new ConfigUtils.IntArrayConfigValue("biomeCoinsDripstoneCaves", new int[]{1,9,5}), BiomeKeys.DRIPSTONE_CAVES));
   
   public static final NationsConfig.ConfigSetting<?> BIOME_COINS_ICE_SPIKES_CFG = registerConfigSetting(new NationsConfig.BiomeConfigSetting<>(
         new ConfigUtils.IntArrayConfigValue("biomeCoinsIceSpikes", new int[]{2,5,8}), BiomeKeys.ICE_SPIKES));
   
   public static final NationsConfig.ConfigSetting<?> BIOME_COINS_DESERT_CFG = registerConfigSetting(new NationsConfig.BiomeConfigSetting<>(
         new ConfigUtils.IntArrayConfigValue("biomeCoinsDesert", new int[]{1,3,6}), BiomeKeys.DESERT));
   
   public static final NationsConfig.ConfigSetting<?> BIOME_COINS_PALE_GARDEN_CFG = registerConfigSetting(new NationsConfig.BiomeConfigSetting<>(
         new ConfigUtils.IntArrayConfigValue("biomeCoinsPaleGarden", new int[]{3,2,8}), BiomeKeys.PALE_GARDEN));
   
   public static final NationsConfig.ConfigSetting<?> BIOME_COINS_BADLANDS_CFG = registerConfigSetting(new NationsConfig.BiomeConfigSetting<>(
         new ConfigUtils.IntArrayConfigValue("biomeCoinsBadlands", new int[]{2,4,6}), BiomeKeys.BADLANDS));
   
   public static final NationsConfig.ConfigSetting<?> BIOME_COINS_ERODED_BADLANDS_CFG = registerConfigSetting(new NationsConfig.BiomeConfigSetting<>(
         new ConfigUtils.IntArrayConfigValue("biomeCoinsErodedBadlands", new int[]{1,5,6}), BiomeKeys.ERODED_BADLANDS));
   
   public static final NationsConfig.ConfigSetting<?> BIOME_COINS_WOODED_BADLANDS_CFG = registerConfigSetting(new NationsConfig.BiomeConfigSetting<>(
         new ConfigUtils.IntArrayConfigValue("biomeCoinsWoodedBadlands", new int[]{2,5,6}), BiomeKeys.WOODED_BADLANDS));
   
   public static final NationsConfig.ConfigSetting<?> BIOME_COINS_DEEP_DARK_CFG = registerConfigSetting(new NationsConfig.BiomeConfigSetting<>(
         new ConfigUtils.IntArrayConfigValue("biomeCoinsDeepDark", new int[]{1,5,9}), BiomeKeys.DEEP_DARK));
   
   public static final NationsConfig.ConfigSetting<?> BIOME_COINS_MUSHROOM_FIELDS_CFG = registerConfigSetting(new NationsConfig.BiomeConfigSetting<>(
         new ConfigUtils.IntArrayConfigValue("biomeCoinsMushroomFields", new int[]{5,1,9}), BiomeKeys.MUSHROOM_FIELDS));
   
   public static final NationsConfig.ConfigSetting<?> BIOME_COINS_DARK_FOREST_CFG = registerConfigSetting(new NationsConfig.BiomeConfigSetting<>(
         new ConfigUtils.IntArrayConfigValue("biomeCoinsDarkForest", new int[]{3,3,6}), BiomeKeys.DARK_FOREST));
   
   public static final NationsConfig.ConfigSetting<?> BIOME_COINS_FROZEN_RIVER_CFG = registerConfigSetting(new NationsConfig.BiomeConfigSetting<>(
         new ConfigUtils.IntArrayConfigValue("biomeCoinsFrozenRiver", new int[]{3,3,4}), BiomeKeys.FROZEN_RIVER));
   
   public static final NationsConfig.ConfigSetting<?> BIOME_COINS_BEACH_CFG = registerConfigSetting(new NationsConfig.BiomeConfigSetting<>(
         new ConfigUtils.IntArrayConfigValue("biomeCoinsBeach", new int[]{3,4,3}), BiomeKeys.BEACH));
   
   public static final NationsConfig.ConfigSetting<?> BIOME_COINS_SNOWY_BEACH_CFG = registerConfigSetting(new NationsConfig.BiomeConfigSetting<>(
         new ConfigUtils.IntArrayConfigValue("biomeCoinsSnowyBeach", new int[]{3,3,4}), BiomeKeys.SNOWY_BEACH));
   
   public static final NationsConfig.ConfigSetting<?> BIOME_COINS_SAVANNA_CFG = registerConfigSetting(new NationsConfig.BiomeConfigSetting<>(
         new ConfigUtils.IntArrayConfigValue("biomeCoinsSavanna", new int[]{3,4,3}), BiomeKeys.SAVANNA));
   
   public static final NationsConfig.ConfigSetting<?> BIOME_COINS_SNOWY_PLAINS_CFG = registerConfigSetting(new NationsConfig.BiomeConfigSetting<>(
         new ConfigUtils.IntArrayConfigValue("biomeCoinsSnowyPlains", new int[]{3,3,4}), BiomeKeys.SNOWY_PLAINS));
   
   public static final NationsConfig.ConfigSetting<?> BIOME_COINS_NETHER_WASTES_CFG = registerConfigSetting(new NationsConfig.BiomeConfigSetting<>(
         new ConfigUtils.IntArrayConfigValue("biomeCoinsNetherWastes", new int[]{0,0,0}), BiomeKeys.NETHER_WASTES));
   
   public static final NationsConfig.ConfigSetting<?> BIOME_COINS_WARPED_FOREST_CFG = registerConfigSetting(new NationsConfig.BiomeConfigSetting<>(
         new ConfigUtils.IntArrayConfigValue("biomeCoinsWarpedForest", new int[]{0,0,0}), BiomeKeys.WARPED_FOREST));
   
   public static final NationsConfig.ConfigSetting<?> BIOME_COINS_CRIMSON_FOREST_CFG = registerConfigSetting(new NationsConfig.BiomeConfigSetting<>(
         new ConfigUtils.IntArrayConfigValue("biomeCoinsCrimsonForest", new int[]{0,0,0}), BiomeKeys.CRIMSON_FOREST));
   
   public static final NationsConfig.ConfigSetting<?> BIOME_COINS_SOUL_SAND_VALLEY_CFG = registerConfigSetting(new NationsConfig.BiomeConfigSetting<>(
         new ConfigUtils.IntArrayConfigValue("biomeCoinsSoulSandValley", new int[]{0,0,0}), BiomeKeys.SOUL_SAND_VALLEY));
   
   public static final NationsConfig.ConfigSetting<?> BIOME_COINS_BASALT_DELTAS_CFG = registerConfigSetting(new NationsConfig.BiomeConfigSetting<>(
         new ConfigUtils.IntArrayConfigValue("biomeCoinsBasaltDeltas", new int[]{0,0,0}), BiomeKeys.BASALT_DELTAS));
   
   public static final NationsConfig.ConfigSetting<?> BIOME_COINS_THE_END_CFG = registerConfigSetting(new NationsConfig.BiomeConfigSetting<>(
         new ConfigUtils.IntArrayConfigValue("biomeCoinsTheEnd", new int[]{0,0,0}), BiomeKeys.THE_END));
   
   public static final NationsConfig.ConfigSetting<?> BIOME_COINS_END_HIGHLANDS_CFG = registerConfigSetting(new NationsConfig.BiomeConfigSetting<>(
         new ConfigUtils.IntArrayConfigValue("biomeCoinsEndHighlands", new int[]{0,0,0}), BiomeKeys.END_HIGHLANDS));
   
   public static final NationsConfig.ConfigSetting<?> BIOME_COINS_END_MIDLANDS_CFG = registerConfigSetting(new NationsConfig.BiomeConfigSetting<>(
         new ConfigUtils.IntArrayConfigValue("biomeCoinsEndMidlands", new int[]{0,0,0}), BiomeKeys.END_MIDLANDS));
   
   public static final NationsConfig.ConfigSetting<?> BIOME_COINS_SMALL_END_ISLANDS_CFG = registerConfigSetting(new NationsConfig.BiomeConfigSetting<>(
         new ConfigUtils.IntArrayConfigValue("biomeCoinsSmallEndIslands", new int[]{0,0,0}), BiomeKeys.SMALL_END_ISLANDS));
   
   public static final NationsConfig.ConfigSetting<?> BIOME_COINS_END_BARRENS_CFG = registerConfigSetting(new NationsConfig.BiomeConfigSetting<>(
         new ConfigUtils.IntArrayConfigValue("biomeCoinsEndBarrens", new int[]{0,0,0}), BiomeKeys.END_BARRENS));
   
   // Research Cost Configs
   
   public static final NationsConfig.ConfigSetting<?> RESEARCH_COST_MECHANICS_CFG = registerConfigSetting(new NationsConfig.ResearchConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("researchCostMechanics",1000, new ConfigUtils.IntegerConfigValue.IntLimits(1)), MECHANICS));
   public static final NationsConfig.ConfigSetting<?> RESEARCH_RATE_MECHANICS_CFG = registerConfigSetting(new NationsConfig.ResearchConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("researchRateMechanics",1000, new ConfigUtils.IntegerConfigValue.IntLimits(1)), MECHANICS));
   
   public static final NationsConfig.ConfigSetting<?> RESEARCH_COST_SEMICONDUCTORS_CFG = registerConfigSetting(new NationsConfig.ResearchConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("researchCostSemiconductors",5000, new ConfigUtils.IntegerConfigValue.IntLimits(1)), SEMICONDUCTORS));
   public static final NationsConfig.ConfigSetting<?> RESEARCH_RATE_SEMICONDUCTORS_CFG = registerConfigSetting(new NationsConfig.ResearchConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("researchRateSemiconductors",2500, new ConfigUtils.IntegerConfigValue.IntLimits(1)), SEMICONDUCTORS));
   
   public static final NationsConfig.ConfigSetting<?> RESEARCH_COST_RESONATORS_CFG = registerConfigSetting(new NationsConfig.ResearchConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("researchCostResonators",7500, new ConfigUtils.IntegerConfigValue.IntLimits(1)), RESONATORS));
   public static final NationsConfig.ConfigSetting<?> RESEARCH_RATE_RESONATORS_CFG = registerConfigSetting(new NationsConfig.ResearchConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("researchRateResonators",4000, new ConfigUtils.IntegerConfigValue.IntLimits(1)), RESONATORS));
   
   public static final NationsConfig.ConfigSetting<?> RESEARCH_COST_BRONZEWORKING_CFG = registerConfigSetting(new NationsConfig.ResearchConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("researchCostBronzeworking",1000, new ConfigUtils.IntegerConfigValue.IntLimits(1)), BRONZEWORKING));
   public static final NationsConfig.ConfigSetting<?> RESEARCH_RATE_BRONZEWORKING_CFG = registerConfigSetting(new NationsConfig.ResearchConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("researchRateBronzeworking",1000, new ConfigUtils.IntegerConfigValue.IntLimits(1)), BRONZEWORKING));
   
   public static final NationsConfig.ConfigSetting<?> RESEARCH_COST_STEELWORKING_CFG = registerConfigSetting(new NationsConfig.ResearchConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("researchCostSteelworking",1000, new ConfigUtils.IntegerConfigValue.IntLimits(1)), STEELWORKING));
   public static final NationsConfig.ConfigSetting<?> RESEARCH_RATE_STEELWORKING_CFG = registerConfigSetting(new NationsConfig.ResearchConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("researchRateSteelworking",1000, new ConfigUtils.IntegerConfigValue.IntLimits(1)), STEELWORKING));
   
   public static final NationsConfig.ConfigSetting<?> RESEARCH_COST_ARCHERY_CFG = registerConfigSetting(new NationsConfig.ResearchConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("researchCostArchery",1000, new ConfigUtils.IntegerConfigValue.IntLimits(1)), ARCHERY));
   public static final NationsConfig.ConfigSetting<?> RESEARCH_RATE_ARCHERY_CFG = registerConfigSetting(new NationsConfig.ResearchConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("researchRateArchery",1000, new ConfigUtils.IntegerConfigValue.IntLimits(1)), ARCHERY));
   
   public static final NationsConfig.ConfigSetting<?> RESEARCH_COST_BLACK_POWDER_CFG = registerConfigSetting(new NationsConfig.ResearchConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("researchCostBlackPowder",1000, new ConfigUtils.IntegerConfigValue.IntLimits(1)), BLACK_POWDER));
   public static final NationsConfig.ConfigSetting<?> RESEARCH_RATE_BLACK_POWDER_CFG = registerConfigSetting(new NationsConfig.ResearchConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("researchRateBlackPowder",1000, new ConfigUtils.IntegerConfigValue.IntLimits(1)), BLACK_POWDER));
   
   public static final NationsConfig.ConfigSetting<?> RESEARCH_COST_ANCIENT_ALLOY_CFG = registerConfigSetting(new NationsConfig.ResearchConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("researchCostAncientAlloy",5000, new ConfigUtils.IntegerConfigValue.IntLimits(1)), ANCIENT_ALLOY));
   public static final NationsConfig.ConfigSetting<?> RESEARCH_RATE_ANCIENT_ALLOY_CFG = registerConfigSetting(new NationsConfig.ResearchConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("researchRateAncientAlloy",2500, new ConfigUtils.IntegerConfigValue.IntLimits(1)), ANCIENT_ALLOY));
   
   public static final NationsConfig.ConfigSetting<?> RESEARCH_COST_CRYSTAL_COMPOSITE_CFG = registerConfigSetting(new NationsConfig.ResearchConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("researchCostCrystalComposite",5000, new ConfigUtils.IntegerConfigValue.IntLimits(1)), CRYSTAL_COMPOSITE));
   public static final NationsConfig.ConfigSetting<?> RESEARCH_RATE_CRYSTAL_COMPOSITE_CFG = registerConfigSetting(new NationsConfig.ResearchConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("researchRateCrystalComposite",2500, new ConfigUtils.IntegerConfigValue.IntLimits(1)), CRYSTAL_COMPOSITE));
   
   public static final NationsConfig.ConfigSetting<?> RESEARCH_COST_HARDENED_PLATES_CFG = registerConfigSetting(new NationsConfig.ResearchConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("researchCostHardenedPlates",5000, new ConfigUtils.IntegerConfigValue.IntLimits(1)), HARDENED_PLATES));
   public static final NationsConfig.ConfigSetting<?> RESEARCH_RATE_HARDENED_PLATES_CFG = registerConfigSetting(new NationsConfig.ResearchConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("researchRateHardenedPlates",2500, new ConfigUtils.IntegerConfigValue.IntLimits(1)), HARDENED_PLATES));
   
   public static final NationsConfig.ConfigSetting<?> RESEARCH_COST_FIBERGLASS_COMPOSITE_CFG = registerConfigSetting(new NationsConfig.ResearchConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("researchCostFiberglassComposite",5000, new ConfigUtils.IntegerConfigValue.IntLimits(1)), FIBERGLASS_COMPOSITE));
   public static final NationsConfig.ConfigSetting<?> RESEARCH_RATE_FIBERGLASS_COMPOSITE_CFG = registerConfigSetting(new NationsConfig.ResearchConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("researchRateFiberglassComposite",2500, new ConfigUtils.IntegerConfigValue.IntLimits(1)), FIBERGLASS_COMPOSITE));
   
   public static final NationsConfig.ConfigSetting<?> RESEARCH_COST_BASIC_ALCHEMY_CFG = registerConfigSetting(new NationsConfig.ResearchConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("researchCostBasicAlchemy",5000, new ConfigUtils.IntegerConfigValue.IntLimits(1)), BASIC_ALCHEMY));
   public static final NationsConfig.ConfigSetting<?> RESEARCH_RATE_BASIC_ALCHEMY_CFG = registerConfigSetting(new NationsConfig.ResearchConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("researchRateBasicAlchemy",2500, new ConfigUtils.IntegerConfigValue.IntLimits(1)), BASIC_ALCHEMY));
   
   public static final NationsConfig.ConfigSetting<?> RESEARCH_COST_ENHANCED_ALCHEMY_CFG = registerConfigSetting(new NationsConfig.ResearchConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("researchCostEnhancedAlchemy",7500, new ConfigUtils.IntegerConfigValue.IntLimits(1)), ENHANCED_ALCHEMY));
   public static final NationsConfig.ConfigSetting<?> RESEARCH_RATE_ENHANCED_ALCHEMY_CFG = registerConfigSetting(new NationsConfig.ResearchConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("researchRateEnhancedAlchemy",4000, new ConfigUtils.IntegerConfigValue.IntLimits(1)), ENHANCED_ALCHEMY));
   
   public static final NationsConfig.ConfigSetting<?> RESEARCH_COST_ADVANCED_ALCHEMY_CFG = registerConfigSetting(new NationsConfig.ResearchConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("researchCostAdvancedAlchemy",7500, new ConfigUtils.IntegerConfigValue.IntLimits(1)), ADVANCED_ALCHEMY));
   public static final NationsConfig.ConfigSetting<?> RESEARCH_RATE_ADVANCED_ALCHEMY_CFG = registerConfigSetting(new NationsConfig.ResearchConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("researchRateAdvancedAlchemy",5000, new ConfigUtils.IntegerConfigValue.IntLimits(1)), ADVANCED_ALCHEMY));
   
   public static final NationsConfig.ConfigSetting<?> RESEARCH_COST_POTENT_ALCHEMY_CFG = registerConfigSetting(new NationsConfig.ResearchConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("researchCostPotentAlchemy",7500, new ConfigUtils.IntegerConfigValue.IntLimits(1)), POTENT_ALCHEMY));
   public static final NationsConfig.ConfigSetting<?> RESEARCH_RATE_POTENT_ALCHEMY_CFG = registerConfigSetting(new NationsConfig.ResearchConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("researchRatePotentAlchemy",4000, new ConfigUtils.IntegerConfigValue.IntLimits(1)), POTENT_ALCHEMY));
   
   public static final NationsConfig.ConfigSetting<?> RESEARCH_COST_ENDURING_ALCHEMY_CFG = registerConfigSetting(new NationsConfig.ResearchConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("researchCostEnduringAlchemy",7500, new ConfigUtils.IntegerConfigValue.IntLimits(1)), ENDURING_ALCHEMY));
   public static final NationsConfig.ConfigSetting<?> RESEARCH_RATE_ENDURING_ALCHEMY_CFG = registerConfigSetting(new NationsConfig.ResearchConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("researchRateEnduringAlchemy",4000, new ConfigUtils.IntegerConfigValue.IntLimits(1)), ENDURING_ALCHEMY));
   
   public static final NationsConfig.ConfigSetting<?> RESEARCH_COST_PROLIFIC_ALCHEMY_CFG = registerConfigSetting(new NationsConfig.ResearchConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("researchCostProlificAlchemy",7500, new ConfigUtils.IntegerConfigValue.IntLimits(1)), PROLIFIC_ALCHEMY));
   public static final NationsConfig.ConfigSetting<?> RESEARCH_RATE_PROLIFIC_ALCHEMY_CFG = registerConfigSetting(new NationsConfig.ResearchConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("researchRateProlificAlchemy",5000, new ConfigUtils.IntegerConfigValue.IntLimits(1)), PROLIFIC_ALCHEMY));
   
   public static final NationsConfig.ConfigSetting<?> RESEARCH_COST_TEMPERED_WEAPONS_CFG = registerConfigSetting(new NationsConfig.ResearchConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("researchCostTemperedWeapons",7500, new ConfigUtils.IntegerConfigValue.IntLimits(1)), TEMPERED_WEAPONS));
   public static final NationsConfig.ConfigSetting<?> RESEARCH_RATE_TEMPERED_WEAPONS_CFG = registerConfigSetting(new NationsConfig.ResearchConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("researchRateTemperedWeapons",4000, new ConfigUtils.IntegerConfigValue.IntLimits(1)), TEMPERED_WEAPONS));
   
   public static final NationsConfig.ConfigSetting<?> RESEARCH_COST_ANNEALED_ARMOR_CFG = registerConfigSetting(new NationsConfig.ResearchConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("researchCostAnnealedArmor",7500, new ConfigUtils.IntegerConfigValue.IntLimits(1)), ANNEALED_ARMOR));
   public static final NationsConfig.ConfigSetting<?> RESEARCH_RATE_ANNEALED_ARMOR_CFG = registerConfigSetting(new NationsConfig.ResearchConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("researchRateAnnealedArmor",4000, new ConfigUtils.IntegerConfigValue.IntLimits(1)), ANNEALED_ARMOR));
   
   public static final NationsConfig.ConfigSetting<?> RESEARCH_COST_ARCANA_CFG = registerConfigSetting(new NationsConfig.ResearchConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("researchCostArcana",7500, new ConfigUtils.IntegerConfigValue.IntLimits(1)), ARCANA));
   public static final NationsConfig.ConfigSetting<?> RESEARCH_RATE_ARCANA_CFG = registerConfigSetting(new NationsConfig.ResearchConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("researchRateArcana",4000, new ConfigUtils.IntegerConfigValue.IntLimits(1)), ARCANA));
   
   public static final NationsConfig.ConfigSetting<?> RESEARCH_COST_ALTARS_CFG = registerConfigSetting(new NationsConfig.ResearchConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("researchCostAltars",7500, new ConfigUtils.IntegerConfigValue.IntLimits(1)), ALTARS));
   public static final NationsConfig.ConfigSetting<?> RESEARCH_RATE_ALTARS_CFG = registerConfigSetting(new NationsConfig.ResearchConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("researchRateAltars",5000, new ConfigUtils.IntegerConfigValue.IntLimits(1)), ALTARS));
   
   public static final NationsConfig.ConfigSetting<?> RESEARCH_COST_ENCHANTING_CFG = registerConfigSetting(new NationsConfig.ResearchConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("researchCostEnchanting",7500, new ConfigUtils.IntegerConfigValue.IntLimits(1)), ENCHANTING));
   public static final NationsConfig.ConfigSetting<?> RESEARCH_RATE_ENCHANTING_CFG = registerConfigSetting(new NationsConfig.ResearchConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("researchRateEnchanting",5000, new ConfigUtils.IntegerConfigValue.IntLimits(1)), ENCHANTING));
   
   public static final NationsConfig.ConfigSetting<?> RESEARCH_COST_ENHANCED_ENCHANTING_CFG = registerConfigSetting(new NationsConfig.ResearchConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("researchCostEnhancedEnchanting",7500, new ConfigUtils.IntegerConfigValue.IntLimits(1)), ENHANCED_ENCHANTING));
   public static final NationsConfig.ConfigSetting<?> RESEARCH_RATE_ENHANCED_ENCHANTING_CFG = registerConfigSetting(new NationsConfig.ResearchConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("researchRateEnhancedEnchanting",6000, new ConfigUtils.IntegerConfigValue.IntLimits(1)), ENHANCED_ENCHANTING));
   
   public static final NationsConfig.ConfigSetting<?> RESEARCH_COST_SMITHING_CFG = registerConfigSetting(new NationsConfig.ResearchConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("researchCostSmithing",7500, new ConfigUtils.IntegerConfigValue.IntLimits(1)), SMITHING));
   public static final NationsConfig.ConfigSetting<?> RESEARCH_RATE_SMITHING_CFG = registerConfigSetting(new NationsConfig.ResearchConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("researchRateSmithing",5000, new ConfigUtils.IntegerConfigValue.IntLimits(1)), SMITHING));
   
   public static final NationsConfig.ConfigSetting<?> RESEARCH_COST_FLETCHING_CFG = registerConfigSetting(new NationsConfig.ResearchConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("researchCostFletching",7500, new ConfigUtils.IntegerConfigValue.IntLimits(1)), FLETCHING));
   public static final NationsConfig.ConfigSetting<?> RESEARCH_RATE_FLETCHING_CFG = registerConfigSetting(new NationsConfig.ResearchConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("researchRateFletching",5000, new ConfigUtils.IntegerConfigValue.IntLimits(1)), FLETCHING));
   
   public static final NationsConfig.ConfigSetting<?> RESEARCH_COST_GRAVITIC_WEAPONRY_CFG = registerConfigSetting(new NationsConfig.ResearchConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("researchCostGraviticWeaponry",7500, new ConfigUtils.IntegerConfigValue.IntLimits(1)), GRAVITIC_WEAPONRY));
   public static final NationsConfig.ConfigSetting<?> RESEARCH_RATE_GRAVITIC_WEAPONRY_CFG = registerConfigSetting(new NationsConfig.ResearchConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("researchRateGraviticWeaponry",5000, new ConfigUtils.IntegerConfigValue.IntLimits(1)), GRAVITIC_WEAPONRY));
   
   public static final NationsConfig.ConfigSetting<?> RESEARCH_COST_FORGING_CFG = registerConfigSetting(new NationsConfig.ResearchConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("researchCostForging",7500, new ConfigUtils.IntegerConfigValue.IntLimits(1)), FORGING));
   public static final NationsConfig.ConfigSetting<?> RESEARCH_RATE_FORGING_CFG = registerConfigSetting(new NationsConfig.ResearchConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("researchRateForging",6000, new ConfigUtils.IntegerConfigValue.IntLimits(1)), FORGING));
   
   public static final NationsConfig.ConfigSetting<?> RESEARCH_COST_ADVANCED_ARCANA_CFG = registerConfigSetting(new NationsConfig.ResearchConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("researchCostAdvancedArcana",10000, new ConfigUtils.IntegerConfigValue.IntLimits(1)), ADVANCED_ARCANA));
   public static final NationsConfig.ConfigSetting<?> RESEARCH_RATE_ADVANCED_ARCANA_CFG = registerConfigSetting(new NationsConfig.ResearchConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("researchRateAdvancedArcana",7000, new ConfigUtils.IntegerConfigValue.IntLimits(1)), ADVANCED_ARCANA));
   
   public static final NationsConfig.ConfigSetting<?> RESEARCH_COST_RUNIC_ARCHERY_CFG = registerConfigSetting(new NationsConfig.ResearchConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("researchCostRunicArchery",7500, new ConfigUtils.IntegerConfigValue.IntLimits(1)), RUNIC_ARCHERY));
   public static final NationsConfig.ConfigSetting<?> RESEARCH_RATE_RUNIC_ARCHERY_CFG = registerConfigSetting(new NationsConfig.ResearchConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("researchRateRunicArchery",6000, new ConfigUtils.IntegerConfigValue.IntLimits(1)), RUNIC_ARCHERY));
   
   public static final NationsConfig.ConfigSetting<?> RESEARCH_COST_ARCHETYPE_CHANGE_ITEM_CFG = registerConfigSetting(new NationsConfig.ResearchConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("researchCostArchetypeChangeItem",7500, new ConfigUtils.IntegerConfigValue.IntLimits(1)), ARCHETYPE_CHANGE_ITEM));
   public static final NationsConfig.ConfigSetting<?> RESEARCH_RATE_ARCHETYPE_CHANGE_ITEM_CFG = registerConfigSetting(new NationsConfig.ResearchConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("researchRateArchetypeChangeItem",6000, new ConfigUtils.IntegerConfigValue.IntLimits(1)), ARCHETYPE_CHANGE_ITEM));
   
   public static final NationsConfig.ConfigSetting<?> RESEARCH_COST_BASIC_AUGMENTATION_CFG = registerConfigSetting(new NationsConfig.ResearchConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("researchCostBasicAugmentation",7500, new ConfigUtils.IntegerConfigValue.IntLimits(1)), BASIC_AUGMENTATION));
   public static final NationsConfig.ConfigSetting<?> RESEARCH_RATE_BASIC_AUGMENTATION_CFG = registerConfigSetting(new NationsConfig.ResearchConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("researchRateBasicAugmentation",6000, new ConfigUtils.IntegerConfigValue.IntLimits(1)), BASIC_AUGMENTATION));
   
   public static final NationsConfig.ConfigSetting<?> RESEARCH_COST_ENHANCED_AUGMENTATION_CFG = registerConfigSetting(new NationsConfig.ResearchConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("researchCostEnhancedAugmentation",10000, new ConfigUtils.IntegerConfigValue.IntLimits(1)), ENHANCED_AUGMENTATION));
   public static final NationsConfig.ConfigSetting<?> RESEARCH_RATE_ENHANCED_AUGMENTATION_CFG = registerConfigSetting(new NationsConfig.ResearchConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("researchRateEnhancedAugmentation",7000, new ConfigUtils.IntegerConfigValue.IntLimits(1)), ENHANCED_AUGMENTATION));
   
   public static final NationsConfig.ConfigSetting<?> RESEARCH_COST_ADVANCED_AUGMENTATION_CFG = registerConfigSetting(new NationsConfig.ResearchConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("researchCostAdvancedAugmentation",10000, new ConfigUtils.IntegerConfigValue.IntLimits(1)), ADVANCED_AUGMENTATION));
   public static final NationsConfig.ConfigSetting<?> RESEARCH_RATE_ADVANCED_AUGMENTATION_CFG = registerConfigSetting(new NationsConfig.ResearchConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("researchRateAdvancedAugmentation",7500, new ConfigUtils.IntegerConfigValue.IntLimits(1)), ADVANCED_AUGMENTATION));
   
   public static final NationsConfig.ConfigSetting<?> RESEARCH_COST_POTIONS_PER_TIER_CFG = registerConfigSetting(new NationsConfig.NormalConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("researchCostPotionsPerTier", 1000, new ConfigUtils.IntegerConfigValue.IntLimits(1))));
   public static final NationsConfig.ConfigSetting<?> RESEARCH_RATE_POTIONS_CFG = registerConfigSetting(new NationsConfig.NormalConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("researchRatePotions", 5000, new ConfigUtils.IntegerConfigValue.IntLimits(1))));
   
   public static final NationsConfig.ConfigSetting<?> RESEARCH_COST_ARCANA_ITEMS_PER_RARITY_CFG = registerConfigSetting(new NationsConfig.NormalConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("researchCostArcanaItemsPerRarity", 1500, new ConfigUtils.IntegerConfigValue.IntLimits(1))));
   public static final NationsConfig.ConfigSetting<?> RESEARCH_RATE_ARCANA_ITEMS_CFG = registerConfigSetting(new NationsConfig.NormalConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("researchRateArcanaItems", 5000, new ConfigUtils.IntegerConfigValue.IntLimits(1))));
   
   public static final NationsConfig.ConfigSetting<?> RESEARCH_COST_ENCHANTMENTS_SINGLE_PER_LEVEL_CFG = registerConfigSetting(new NationsConfig.NormalConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("researchCostEnchantmentsSinglePerLevel", 2500, new ConfigUtils.IntegerConfigValue.IntLimits(1))));
   public static final NationsConfig.ConfigSetting<?> RESEARCH_COST_ENCHANTMENTS_DOUBLE_PER_LEVEL_CFG = registerConfigSetting(new NationsConfig.NormalConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("researchCostEnchantmentsDoublePerLevel", 1000, new ConfigUtils.IntegerConfigValue.IntLimits(1))));
   public static final NationsConfig.ConfigSetting<?> RESEARCH_COST_ENCHANTMENTS_TRIPLE_PER_LEVEL_CFG = registerConfigSetting(new NationsConfig.NormalConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("researchCostEnchantmentsTriplePerLevel", 1000, new ConfigUtils.IntegerConfigValue.IntLimits(1))));
   public static final NationsConfig.ConfigSetting<?> RESEARCH_COST_ENCHANTMENTS_QUADRUPLE_PER_LEVEL_CFG = registerConfigSetting(new NationsConfig.NormalConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("researchCostEnchantmentsQuadruplePerLevel", 750, new ConfigUtils.IntegerConfigValue.IntLimits(1))));
   public static final NationsConfig.ConfigSetting<?> RESEARCH_COST_ENCHANTMENTS_QUINTUPLE_PER_LEVEL_CFG = registerConfigSetting(new NationsConfig.NormalConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("researchCostEnchantmentsQuintuplePerLevel", 500, new ConfigUtils.IntegerConfigValue.IntLimits(1))));
   public static final NationsConfig.ConfigSetting<?> RESEARCH_RATE_ENCHANTMENTS_CFG = registerConfigSetting(new NationsConfig.NormalConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("researchRateEnchantments", 5000, new ConfigUtils.IntegerConfigValue.IntLimits(1))));
   
   public static final NationsConfig.ConfigSetting<?> RESEARCH_COST_TIER_1_BUFF_CFG = registerConfigSetting(new NationsConfig.NormalConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("researchCostTier1Buff", 1000, new ConfigUtils.IntegerConfigValue.IntLimits(1))));
   public static final NationsConfig.ConfigSetting<?> RESEARCH_RATE_TIER_1_BUFF_CFG = registerConfigSetting(new NationsConfig.NormalConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("researchRateTier1Buff", 1000, new ConfigUtils.IntegerConfigValue.IntLimits(1))));
   
   public static final NationsConfig.ConfigSetting<?> RESEARCH_COST_TIER_2_BUFF_CFG = registerConfigSetting(new NationsConfig.NormalConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("researchCostTier2Buff", 3500, new ConfigUtils.IntegerConfigValue.IntLimits(1))));
   public static final NationsConfig.ConfigSetting<?> RESEARCH_RATE_TIER_2_BUFF_CFG = registerConfigSetting(new NationsConfig.NormalConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("researchRateTier2Buff", 2500, new ConfigUtils.IntegerConfigValue.IntLimits(1))));
   
   public static final NationsConfig.ConfigSetting<?> RESEARCH_COST_TIER_3_BUFF_CFG = registerConfigSetting(new NationsConfig.NormalConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("researchCostTier3Buff", 5000, new ConfigUtils.IntegerConfigValue.IntLimits(1))));
   public static final NationsConfig.ConfigSetting<?> RESEARCH_RATE_TIER_3_BUFF_CFG = registerConfigSetting(new NationsConfig.NormalConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("researchRateTier3Buff", 4000, new ConfigUtils.IntegerConfigValue.IntLimits(1))));
   
   public static final NationsConfig.ConfigSetting<?> RESEARCH_COST_TIER_4_BUFF_CFG = registerConfigSetting(new NationsConfig.NormalConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("researchCostTier4Buff", 7500, new ConfigUtils.IntegerConfigValue.IntLimits(1))));
   public static final NationsConfig.ConfigSetting<?> RESEARCH_RATE_TIER_4_BUFF_CFG = registerConfigSetting(new NationsConfig.NormalConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("researchRateTier4Buff", 6000, new ConfigUtils.IntegerConfigValue.IntLimits(1))));
   
   public static final NationsConfig.ConfigSetting<?> RESEARCH_COST_TIER_5_BUFF_CFG = registerConfigSetting(new NationsConfig.NormalConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("researchCostTier5Buff", 10000, new ConfigUtils.IntegerConfigValue.IntLimits(1))));
   public static final NationsConfig.ConfigSetting<?> RESEARCH_RATE_TIER_5_BUFF_CFG = registerConfigSetting(new NationsConfig.NormalConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("researchRateTier5Buff", 7500, new ConfigUtils.IntegerConfigValue.IntLimits(1))));
   
   static{
      registerTech(MECHANICS, new ResearchTech(MECHANICS,1,new RegistryKey[]{},RESEARCH_COST_MECHANICS_CFG,RESEARCH_RATE_MECHANICS_CFG).withShowStack(Items.PISTON)
            .addCraftLock(Items.PISTON, Items.STICKY_PISTON, Items.DISPENSER, Items.DROPPER));
//      registerTech(BRONZEWORKING, new ResearchTech(BRONZEWORKING,1,new RegistryKey[]{},RESEARCH_COST_BRONZEWORKING_CFG,RESEARCH_RATE_BRONZEWORKING_CFG).withShowStack(Items.IRON_SWORD)
//            .addCraftLock(Items.IRON_AXE, Items.IRON_SWORD));
//      registerTech(STEELWORKING, new ResearchTech(STEELWORKING,1,new RegistryKey[]{},RESEARCH_COST_STEELWORKING_CFG,RESEARCH_RATE_STEELWORKING_CFG).withShowStack(Items.IRON_CHESTPLATE)
//            .addCraftLock(Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS));
      registerTech(ARCHERY, new ResearchTech(ARCHERY,1,new RegistryKey[]{},RESEARCH_COST_ARCHERY_CFG,RESEARCH_RATE_ARCHERY_CFG).withShowStack(Items.BOW)
            .addCraftLock(Items.BOW, Items.ARROW));
      registerTech(BLACK_POWDER, new ResearchTech(BLACK_POWDER,1,new RegistryKey[]{},RESEARCH_COST_BLACK_POWDER_CFG,RESEARCH_RATE_BLACK_POWDER_CFG).withShowStack(Items.TNT)
            .addCraftLock(Items.TNT, Items.FIREWORK_ROCKET, Items.FIREWORK_STAR));
      registerTech(BASIC_ALCHEMY, new ResearchTech(BASIC_ALCHEMY,1,new RegistryKey[]{},RESEARCH_COST_BASIC_ALCHEMY_CFG,RESEARCH_RATE_BASIC_ALCHEMY_CFG).withShowStack(Items.BREWING_STAND)
            .addCraftLock(Items.BREWING_STAND)
            .addPotionLock(Potions.AWKWARD,Potions.WATER,Potions.MUNDANE,Potions.THICK,Potions.WATER_BREATHING,Potions.FIRE_RESISTANCE,Potions.NIGHT_VISION,Potions.LEAPING,Potions.SLOW_FALLING));
      registerTech(ANCIENT_ALLOY, new ResearchTech(ANCIENT_ALLOY,1,new RegistryKey[]{},RESEARCH_COST_ANCIENT_ALLOY_CFG,RESEARCH_RATE_ANCIENT_ALLOY_CFG).withShowStack(Items.NETHERITE_INGOT)
            .addCraftLock(Items.NETHERITE_INGOT, Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE));
      registerTech(CRYSTAL_COMPOSITE, new ResearchTech(CRYSTAL_COMPOSITE,1,new RegistryKey[]{BRONZEWORKING},RESEARCH_COST_CRYSTAL_COMPOSITE_CFG,RESEARCH_RATE_CRYSTAL_COMPOSITE_CFG).withShowStack(Items.DIAMOND_SWORD)
            .addCraftLock(Items.DIAMOND_SWORD, Items.DIAMOND_AXE));
      registerTech(HARDENED_PLATES, new ResearchTech(HARDENED_PLATES,1,new RegistryKey[]{STEELWORKING},RESEARCH_COST_HARDENED_PLATES_CFG,RESEARCH_RATE_HARDENED_PLATES_CFG).withShowStack(Items.DIAMOND_CHESTPLATE)
            .addCraftLock(Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS));
      
      
      registerTech(SEMICONDUCTORS, new ResearchTech(SEMICONDUCTORS,2,new RegistryKey[]{MECHANICS},RESEARCH_COST_SEMICONDUCTORS_CFG,RESEARCH_RATE_SEMICONDUCTORS_CFG).withShowStack(Items.REPEATER)
            .addCraftLock(Items.REPEATER, Items.DAYLIGHT_DETECTOR, Items.COMPARATOR, Items.OBSERVER));
      registerTech(FIBERGLASS_COMPOSITE, new ResearchTech(FIBERGLASS_COMPOSITE,2,new RegistryKey[]{ARCHERY},RESEARCH_COST_FIBERGLASS_COMPOSITE_CFG,RESEARCH_RATE_FIBERGLASS_COMPOSITE_CFG).withShowStack(Items.CROSSBOW)
            .addCraftLock(Items.CROSSBOW));
      registerTech(ENHANCED_ALCHEMY, new ResearchTech(ENHANCED_ALCHEMY,2,new RegistryKey[]{BASIC_ALCHEMY},RESEARCH_COST_ENHANCED_ALCHEMY_CFG,RESEARCH_RATE_ENHANCED_ALCHEMY_CFG).withShowStack(Items.POTION)
            .addPotionLock(Potions.SWIFTNESS,Potions.SLOWNESS,Potions.REGENERATION,Potions.INVISIBILITY,Potions.WEAKNESS,Potions.OOZING,Potions.INFESTED,Potions.WEAVING,Potions.WIND_CHARGED));
      registerTech(POTENT_ALCHEMY, new ResearchTech(POTENT_ALCHEMY,2,new RegistryKey[]{BASIC_ALCHEMY},RESEARCH_COST_POTENT_ALCHEMY_CFG,RESEARCH_RATE_POTENT_ALCHEMY_CFG).withShowStack(Items.GLOWSTONE_DUST));
      registerTech(ENDURING_ALCHEMY, new ResearchTech(ENDURING_ALCHEMY,2,new RegistryKey[]{BASIC_ALCHEMY},RESEARCH_COST_ENDURING_ALCHEMY_CFG,RESEARCH_RATE_ENDURING_ALCHEMY_CFG).withShowStack(Items.REDSTONE));
      registerTech(TEMPERED_WEAPONS, new ResearchTech(TEMPERED_WEAPONS,2,new RegistryKey[]{ANCIENT_ALLOY,CRYSTAL_COMPOSITE},RESEARCH_COST_TEMPERED_WEAPONS_CFG,RESEARCH_RATE_TEMPERED_WEAPONS_CFG).withShowStack(Items.NETHERITE_SWORD)
            .addCraftLock(Items.NETHERITE_SWORD, Items.NETHERITE_AXE));
      registerTech(ANNEALED_ARMOR, new ResearchTech(ANNEALED_ARMOR,2,new RegistryKey[]{ANCIENT_ALLOY,HARDENED_PLATES},RESEARCH_COST_ANNEALED_ARMOR_CFG,RESEARCH_RATE_ANNEALED_ARMOR_CFG).withShowStack(Items.NETHERITE_CHESTPLATE)
            .addCraftLock(Items.NETHERITE_HELMET, Items.NETHERITE_CHESTPLATE, Items.NETHERITE_LEGGINGS, Items.NETHERITE_BOOTS));
      registerTech(ARCANA, new ResearchTech(ARCANA,2,new RegistryKey[]{},RESEARCH_COST_ARCANA_CFG,RESEARCH_RATE_ARCANA_CFG).withShowStack(ArcanaRegistry.ARCANE_TOME.getPrefItemNoLore())
            .addArcanaLock(ArcanaRegistry.MAGNETISM_CHARM,ArcanaRegistry.TELESCOPING_BEACON,ArcanaRegistry.ANCIENT_DOWSING_ROD,ArcanaRegistry.AQUATIC_EVERSOURCE,ArcanaRegistry.CONTAINMENT_CIRCLET,
                  ArcanaRegistry.CHEST_TRANSLOCATOR, ArcanaRegistry.FRACTAL_SPONGE, ArcanaRegistry.TEMPORAL_MOMENT, ArcanaRegistry.WILD_GROWTH_CHARM, ArcanaRegistry.PEARL_OF_RECALL,
                  ArcanaRegistry.EVERLASTING_ROCKET, ArcanaRegistry.CLEANSING_CHARM, ArcanaRegistry.LIGHT_CHARM, ArcanaRegistry.EXOTIC_MATTER, ArcanaRegistry.RUNIC_MATRIX));
      
      
//      registerTech(RESONATORS, new ResearchTech(RESONATORS,3,new RegistryKey[]{SEMICONDUCTORS},RESEARCH_COST_RESONATORS_CFG,RESEARCH_RATE_RESONATORS_CFG).withShowStack(Items.COMPARATOR)
//            .addCraftLock(Items.COMPARATOR, Items.OBSERVER));
      
      registerTech(PROLIFIC_ALCHEMY, new ResearchTech(PROLIFIC_ALCHEMY,3,new RegistryKey[]{ENHANCED_ALCHEMY},RESEARCH_COST_PROLIFIC_ALCHEMY_CFG,RESEARCH_RATE_PROLIFIC_ALCHEMY_CFG).withShowStack(Items.SPLASH_POTION));
      registerTech(ADVANCED_ALCHEMY, new ResearchTech(ADVANCED_ALCHEMY,3,new RegistryKey[]{ENHANCED_ALCHEMY},RESEARCH_COST_ADVANCED_ALCHEMY_CFG,RESEARCH_RATE_ADVANCED_ALCHEMY_CFG).withShowStack(Items.POTION)
            .addPotionLock(Potions.HARMING,Potions.TURTLE_MASTER,Potions.STRENGTH,Potions.HEALING,Potions.POISON));
      registerTech(ALTARS, new ResearchTech(ALTARS,3,new RegistryKey[]{ARCANA},RESEARCH_COST_ALTARS_CFG,RESEARCH_RATE_ALTARS_CFG).withShowStack(ArcanaRegistry.TRANSMUTATION_ALTAR.getPrefItemNoLore())
            .addArcanaLock(ArcanaRegistry.CELESTIAL_ALTAR, ArcanaRegistry.STARPATH_ALTAR, ArcanaRegistry.STORMCALLER_ALTAR, ArcanaRegistry.TRANSMUTATION_ALTAR));
      registerTech(ENCHANTING, new ResearchTech(ENCHANTING,3,new RegistryKey[]{ARCANA},RESEARCH_COST_ENCHANTING_CFG,RESEARCH_RATE_ENCHANTING_CFG).withShowStack(Items.ENCHANTING_TABLE)
            .addEnchantLock(new Pair<>(Enchantments.BANE_OF_ARTHROPODS,1),new Pair<>(Enchantments.BANE_OF_ARTHROPODS,2),new Pair<>(Enchantments.BANE_OF_ARTHROPODS,3), new Pair<>(Enchantments.LUCK_OF_THE_SEA,1),
                  new Pair<>(Enchantments.LUCK_OF_THE_SEA,2), new Pair<>(Enchantments.PROJECTILE_PROTECTION,1), new Pair<>(Enchantments.PROJECTILE_PROTECTION,2), new Pair<>(Enchantments.VANISHING_CURSE,1),
                  new Pair<>(Enchantments.BINDING_CURSE,1), new Pair<>(Enchantments.SWEEPING_EDGE,1), new Pair<>(Enchantments.FEATHER_FALLING,1), new Pair<>(Enchantments.BLAST_PROTECTION,1),
                  new Pair<>(Enchantments.BLAST_PROTECTION,2), new Pair<>(Enchantments.QUICK_CHARGE,1), new Pair<>(Enchantments.FIRE_PROTECTION,1), new Pair<>(Enchantments.FIRE_PROTECTION,2),
                  new Pair<>(Enchantments.UNBREAKING,1), new Pair<>(Enchantments.IMPALING,1), new Pair<>(Enchantments.IMPALING,2), new Pair<>(Enchantments.PIERCING,1), new Pair<>(Enchantments.PIERCING,2),
                  new Pair<>(Enchantments.RESPIRATION,1), new Pair<>(Enchantments.KNOCKBACK,1), new Pair<>(Enchantments.PUNCH,1), new Pair<>(Enchantments.BREACH,1), new Pair<>(Enchantments.RIPTIDE,1),
                  new Pair<>(Enchantments.LOOTING,1), new Pair<>(Enchantments.DENSITY,1), new Pair<>(Enchantments.LURE,1), new Pair<>(Enchantments.LURE,2), new Pair<>(Enchantments.FORTUNE,1),
                  new Pair<>(Enchantments.SMITE,1), new Pair<>(Enchantments.SMITE,2), new Pair<>(Enchantments.POWER,1), new Pair<>(Enchantments.POWER,2),
                  new Pair<>(Enchantments.LOYALTY,1), new Pair<>(Enchantments.LOYALTY,2), new Pair<>(Enchantments.SHARPNESS,1), new Pair<>(Enchantments.PROTECTION,1), new Pair<>(Enchantments.WIND_BURST,1),
                  new Pair<>(ArcanaRegistry.FATE_ANCHOR,1), new Pair<>(Enchantments.EFFICIENCY,1), new Pair<>(Enchantments.EFFICIENCY,2))
            .addArcanaLock(ArcanaRegistry.SPAWNER_HARNESS, ArcanaRegistry.CONTINUUM_ANCHOR));
      registerTech(SMITHING, new ResearchTech(SMITHING,3,new RegistryKey[]{ARCANA,TEMPERED_WEAPONS,ANNEALED_ARMOR},RESEARCH_COST_SMITHING_CFG,RESEARCH_RATE_SMITHING_CFG).withShowStack(Items.ANVIL)
            .addArcanaLock(ArcanaRegistry.MAGMATIC_EVERSOURCE, ArcanaRegistry.ARCANISTS_BELT));
      registerTech(FLETCHING, new ResearchTech(FLETCHING,3,new RegistryKey[]{ARCANA,FIBERGLASS_COMPOSITE,ENHANCED_ALCHEMY},RESEARCH_COST_FLETCHING_CFG,RESEARCH_RATE_FLETCHING_CFG).withShowStack(Items.FLETCHING_TABLE)
            .addArcanaLock(ArcanaRegistry.ALCHEMICAL_ARBALEST, ArcanaRegistry.OVERFLOWING_QUIVER));
      registerTech(GRAVITIC_WEAPONRY, new ResearchTech(GRAVITIC_WEAPONRY,3,new RegistryKey[]{},RESEARCH_COST_GRAVITIC_WEAPONRY_CFG,RESEARCH_RATE_GRAVITIC_WEAPONRY_CFG).withShowStack(Items.MACE)
            .addCraftLock(Items.MACE));
      registerTech(ARCHETYPE_CHANGE_ITEM, new ResearchTech(ARCHETYPE_CHANGE_ITEM,3,new RegistryKey[]{},RESEARCH_COST_ARCHETYPE_CHANGE_ITEM_CFG,RESEARCH_RATE_ARCHETYPE_CHANGE_ITEM_CFG).withShowStack(ArchetypeRegistry.CHANGE_ITEM)
            .addCraftLock(ArchetypeRegistry.CHANGE_ITEM));
      
      registerTech(ENHANCED_ENCHANTING, new ResearchTech(ENHANCED_ENCHANTING,4,new RegistryKey[]{ENCHANTING},RESEARCH_COST_ENHANCED_ENCHANTING_CFG,RESEARCH_RATE_ENHANCED_ENCHANTING_CFG).withShowStack(Items.ENCHANTED_BOOK)
            .addEnchantLock(new Pair<>(Enchantments.BANE_OF_ARTHROPODS,4), new Pair<>(Enchantments.BANE_OF_ARTHROPODS,5), new Pair<>(Enchantments.PROJECTILE_PROTECTION,3),
                  new Pair<>(Enchantments.PROJECTILE_PROTECTION,4), new Pair<>(Enchantments.LUCK_OF_THE_SEA,3), new Pair<>(Enchantments.SWEEPING_EDGE,2),
                  new Pair<>(Enchantments.FEATHER_FALLING,2), new Pair<>(Enchantments.BLAST_PROTECTION,3), new Pair<>(Enchantments.BLAST_PROTECTION,4), new Pair<>(Enchantments.SHARPNESS,2),
                  new Pair<>(Enchantments.SHARPNESS,3), new Pair<>(Enchantments.FIRE_PROTECTION,3), new Pair<>(Enchantments.FIRE_PROTECTION,4), new Pair<>(Enchantments.PROTECTION,2),
                  new Pair<>(Enchantments.PROTECTION,3), new Pair<>(Enchantments.QUICK_CHARGE,2), new Pair<>(Enchantments.IMPALING,3), new Pair<>(Enchantments.IMPALING,4),
                  new Pair<>(Enchantments.DEPTH_STRIDER,1), new Pair<>(Enchantments.FROST_WALKER,1), new Pair<>(Enchantments.UNBREAKING,2), new Pair<>(Enchantments.SWIFT_SNEAK,1),
                  new Pair<>(Enchantments.RESPIRATION,2), new Pair<>(Enchantments.KNOCKBACK,2), new Pair<>(Enchantments.WIND_BURST,2), new Pair<>(Enchantments.RIPTIDE,2),
                  new Pair<>(Enchantments.RIPTIDE,3), new Pair<>(Enchantments.FLAME,1), new Pair<>(Enchantments.LURE,3), new Pair<>(Enchantments.PUNCH,2), new Pair<>(Enchantments.LOOTING,2),
                  new Pair<>(Enchantments.LOYALTY,3), new Pair<>(Enchantments.FORTUNE,2), new Pair<>(Enchantments.SILK_TOUCH,1), new Pair<>(Enchantments.PIERCING,3),
                  new Pair<>(Enchantments.THORNS,1), new Pair<>(Enchantments.THORNS,2), new Pair<>(Enchantments.CHANNELING,1), new Pair<>(Enchantments.SMITE,3), new Pair<>(Enchantments.SMITE,4),
                  new Pair<>(Enchantments.SOUL_SPEED,1), new Pair<>(Enchantments.POWER,3), new Pair<>(Enchantments.POWER,4), new Pair<>(Enchantments.FIRE_ASPECT,1),
                  new Pair<>(Enchantments.AQUA_AFFINITY,1), new Pair<>(Enchantments.BREACH,2), new Pair<>(Enchantments.BREACH,3), new Pair<>(Enchantments.DENSITY,2), new Pair<>(Enchantments.DENSITY,3),
                  new Pair<>(Enchantments.EFFICIENCY,3), new Pair<>(Enchantments.EFFICIENCY,4), new Pair<>(Enchantments.MULTISHOT,1)));
      registerTech(FORGING, new ResearchTech(FORGING,4,new RegistryKey[]{SMITHING},RESEARCH_COST_FORGING_CFG,RESEARCH_RATE_FORGING_CFG).withShowStack(Items.BLAST_FURNACE)
            .addArcanaLock(ArcanaRegistry.SOULSTONE, ArcanaRegistry.IGNEOUS_COLLIDER, ArcanaRegistry.BINARY_BLADES, ArcanaRegistry.GRAVITON_MAUL, ArcanaRegistry.SHADOW_STALKERS_GLAIVE, ArcanaRegistry.SHIELD_OF_FORTITUDE,
                  ArcanaRegistry.SOJOURNER_BOOTS, ArcanaRegistry.TOTEM_OF_VENGEANCE));
      registerTech(RUNIC_ARCHERY, new ResearchTech(RUNIC_ARCHERY,4,new RegistryKey[]{FLETCHING},RESEARCH_COST_RUNIC_ARCHERY_CFG,RESEARCH_RATE_RUNIC_ARCHERY_CFG).withShowStack(ArcanaRegistry.RUNIC_BOW.getPrefItemNoLore())
            .addArcanaLock(ArcanaRegistry.RUNIC_BOW, ArcanaRegistry.RUNIC_QUIVER, ArcanaRegistry.ARCANE_FLAK_ARROWS, ArcanaRegistry.BLINK_ARROWS, ArcanaRegistry.CONCUSSION_ARROWS,
                  ArcanaRegistry.ENSNAREMENT_ARROWS, ArcanaRegistry.EXPULSION_ARROWS, ArcanaRegistry.GRAVITON_ARROWS, ArcanaRegistry.PHOTONIC_ARROWS, ArcanaRegistry.SIPHONING_ARROWS, ArcanaRegistry.SMOKE_ARROWS,
                  ArcanaRegistry.STORM_ARROWS, ArcanaRegistry.TETHER_ARROWS, ArcanaRegistry.TRACKING_ARROWS));
      registerTech(BASIC_AUGMENTATION, new ResearchTech(BASIC_AUGMENTATION,4,new RegistryKey[]{SMITHING},RESEARCH_COST_BASIC_AUGMENTATION_CFG,RESEARCH_RATE_BASIC_AUGMENTATION_CFG).withShowStack(ArcanaRegistry.CATALYTIC_MATRIX.getPrefItemNoLore())
            .addArcanaLock(ArcanaRegistry.CATALYTIC_MATRIX, ArcanaRegistry.MUNDANE_CATALYST, ArcanaRegistry.EMPOWERED_CATALYST, ArcanaRegistry.EXOTIC_CATALYST));
      
      registerTech(ADVANCED_ARCANA, new ResearchTech(ADVANCED_ARCANA,5,new RegistryKey[]{ENHANCED_ENCHANTING,FORGING},RESEARCH_COST_ADVANCED_ARCANA_CFG,RESEARCH_RATE_ADVANCED_ARCANA_CFG).withShowStack(Items.LECTERN)
            .addEnchantLock(new Pair<>(Enchantments.FEATHER_FALLING,3), new Pair<>(Enchantments.FEATHER_FALLING,4), new Pair<>(Enchantments.DEPTH_STRIDER,2), new Pair<>(Enchantments.DEPTH_STRIDER,3),
                  new Pair<>(Enchantments.SWEEPING_EDGE,3), new Pair<>(Enchantments.SWIFT_SNEAK,2), new Pair<>(Enchantments.SWIFT_SNEAK,3), new Pair<>(Enchantments.SOUL_SPEED,2),
                  new Pair<>(Enchantments.SOUL_SPEED,3), new Pair<>(Enchantments.QUICK_CHARGE,3), new Pair<>(Enchantments.SHARPNESS,4), new Pair<>(Enchantments.SHARPNESS,5),
                  new Pair<>(Enchantments.UNBREAKING,3), new Pair<>(Enchantments.FROST_WALKER,2), new Pair<>(Enchantments.RESPIRATION,3), new Pair<>(Enchantments.FIRE_ASPECT,2),
                  new Pair<>(Enchantments.DENSITY,4), new Pair<>(Enchantments.DENSITY,5), new Pair<>(Enchantments.INFINITY,1), new Pair<>(Enchantments.SMITE,5),
                  new Pair<>(Enchantments.POWER,5), new Pair<>(Enchantments.THORNS,3), new Pair<>(Enchantments.LOOTING,3), new Pair<>(Enchantments.FORTUNE,3), new Pair<>(Enchantments.IMPALING,5),
                  new Pair<>(Enchantments.PIERCING,4), new Pair<>(Enchantments.EFFICIENCY,5), new Pair<>(Enchantments.PROTECTION,4), new Pair<>(Enchantments.BREACH,4), new Pair<>(Enchantments.WIND_BURST,3))
            .addArcanaLock(ArcanaRegistry.NUL_MEMENTO,ArcanaRegistry.SPAWNER_INFUSER,ArcanaRegistry.AEQUALIS_SCIENTIA));
//      registerTech(ENHANCED_AUGMENTATION, new ResearchTech(ENHANCED_AUGMENTATION,6,new RegistryKey[]{BASIC_AUGMENTATION},RESEARCH_COST_ENHANCED_AUGMENTATION_CFG,RESEARCH_RATE_ENHANCED_AUGMENTATION_CFG).withShowStack(ArcanaRegistry.EMPOWERED_CATALYST.getPrefItemNoLore())
//            .addArcanaLock(ArcanaRegistry.EMPOWERED_CATALYST, ArcanaRegistry.EXOTIC_CATALYST));
      registerTech(ADVANCED_AUGMENTATION, new ResearchTech(ADVANCED_AUGMENTATION,5,new RegistryKey[]{BASIC_AUGMENTATION},RESEARCH_COST_ADVANCED_AUGMENTATION_CFG,RESEARCH_RATE_ADVANCED_AUGMENTATION_CFG).withShowStack(ArcanaRegistry.SOVEREIGN_CATALYST.getPrefItemNoLore())
            .addArcanaLock(ArcanaRegistry.SOVEREIGN_CATALYST, ArcanaRegistry.DIVINE_CATALYST));
      
      setupNationBuffTechs();
   }
   
   static {
      // Armor protection
      ENCHANT_ITEM_MAP.put(Enchantments.PROTECTION,            Items.GOLDEN_LEGGINGS);
      ENCHANT_ITEM_MAP.put(Enchantments.FIRE_PROTECTION,       Items.GOLDEN_LEGGINGS);
      ENCHANT_ITEM_MAP.put(Enchantments.BLAST_PROTECTION,      Items.GOLDEN_LEGGINGS);
      ENCHANT_ITEM_MAP.put(Enchantments.PROJECTILE_PROTECTION, Items.GOLDEN_LEGGINGS);
      // Mobility & utility
      ENCHANT_ITEM_MAP.put(Enchantments.FEATHER_FALLING, Items.GOLDEN_BOOTS);
      ENCHANT_ITEM_MAP.put(Enchantments.DEPTH_STRIDER,   Items.GOLDEN_BOOTS);
      ENCHANT_ITEM_MAP.put(Enchantments.FROST_WALKER,    Items.GOLDEN_BOOTS);
      ENCHANT_ITEM_MAP.put(Enchantments.RESPIRATION,     Items.GOLDEN_HELMET);
      ENCHANT_ITEM_MAP.put(Enchantments.AQUA_AFFINITY,   Items.GOLDEN_HELMET);
      ENCHANT_ITEM_MAP.put(Enchantments.THORNS,          Items.GOLDEN_CHESTPLATE);
      ENCHANT_ITEM_MAP.put(Enchantments.SOUL_SPEED,      Items.GOLDEN_BOOTS);
      ENCHANT_ITEM_MAP.put(Enchantments.SWIFT_SNEAK,     Items.GOLDEN_LEGGINGS);
      // Weapon damage
      ENCHANT_ITEM_MAP.put(Enchantments.SHARPNESS,            Items.GOLDEN_SWORD);
      ENCHANT_ITEM_MAP.put(Enchantments.SMITE,                Items.GOLDEN_SWORD);
      ENCHANT_ITEM_MAP.put(Enchantments.BANE_OF_ARTHROPODS,   Items.GOLDEN_SWORD);
      ENCHANT_ITEM_MAP.put(Enchantments.KNOCKBACK,            Items.GOLDEN_SWORD);
      ENCHANT_ITEM_MAP.put(Enchantments.FIRE_ASPECT,          Items.GOLDEN_SWORD);
      ENCHANT_ITEM_MAP.put(Enchantments.LOOTING,              Items.GOLDEN_SWORD);
      ENCHANT_ITEM_MAP.put(Enchantments.SWEEPING_EDGE,        Items.GOLDEN_SWORD);
      // Tool efficiency
      ENCHANT_ITEM_MAP.put(Enchantments.EFFICIENCY,   Items.GOLDEN_PICKAXE);
      ENCHANT_ITEM_MAP.put(Enchantments.SILK_TOUCH,   Items.GOLDEN_PICKAXE);
      ENCHANT_ITEM_MAP.put(Enchantments.UNBREAKING,   Items.GOLDEN_PICKAXE);
      ENCHANT_ITEM_MAP.put(Enchantments.FORTUNE,      Items.GOLDEN_PICKAXE);
      ENCHANT_ITEM_MAP.put(Enchantments.MENDING,      Items.GOLDEN_PICKAXE);
      // Ranged
      ENCHANT_ITEM_MAP.put(Enchantments.POWER,     Items.BOW);
      ENCHANT_ITEM_MAP.put(Enchantments.PUNCH,     Items.BOW);
      ENCHANT_ITEM_MAP.put(Enchantments.FLAME,     Items.BOW);
      ENCHANT_ITEM_MAP.put(Enchantments.INFINITY,  Items.BOW);
      // Fishing
      ENCHANT_ITEM_MAP.put(Enchantments.LUCK_OF_THE_SEA, Items.FISHING_ROD);
      ENCHANT_ITEM_MAP.put(Enchantments.LURE,            Items.FISHING_ROD);
      // Trident
      ENCHANT_ITEM_MAP.put(Enchantments.LOYALTY,    Items.TRIDENT);
      ENCHANT_ITEM_MAP.put(Enchantments.IMPALING,   Items.TRIDENT);
      ENCHANT_ITEM_MAP.put(Enchantments.RIPTIDE,    Items.TRIDENT);
      ENCHANT_ITEM_MAP.put(Enchantments.CHANNELING, Items.TRIDENT);
      // Crossbow
      ENCHANT_ITEM_MAP.put(Enchantments.MULTISHOT,    Items.CROSSBOW);
      ENCHANT_ITEM_MAP.put(Enchantments.QUICK_CHARGE, Items.CROSSBOW);
      ENCHANT_ITEM_MAP.put(Enchantments.PIERCING,     Items.CROSSBOW);
      // Curses (appear on armor)
      ENCHANT_ITEM_MAP.put(Enchantments.BINDING_CURSE,    Items.GOLDEN_CHESTPLATE);
      ENCHANT_ITEM_MAP.put(Enchantments.VANISHING_CURSE,  Items.GOLDEN_PICKAXE);
      // Mace
      ENCHANT_ITEM_MAP.put(Enchantments.DENSITY,    Items.MACE);
      ENCHANT_ITEM_MAP.put(Enchantments.BREACH,     Items.MACE);
      ENCHANT_ITEM_MAP.put(Enchantments.WIND_BURST, Items.MACE);
      // Arcana
      ENCHANT_ITEM_MAP.put(ArcanaRegistry.FATE_ANCHOR, Items.GOLDEN_PICKAXE);
   }
   
   private static Block registerBlock(String id, Block block){
      Identifier identifier = Identifier.of(MOD_ID,id);
      Registry.register(Registries.BLOCK, identifier, block);
      return block;
   }
   
   private static Item registerItem(String id, Item item){
      Identifier identifier = Identifier.of(MOD_ID,id);
      Registry.register(ITEMS, identifier, Registry.register(Registries.ITEM, identifier, item));
      return item;
   }
   
   private static void setupNationBuffTechs(){
      registerTech(MANIFEST_DESTINY_1, new ResearchTech(MANIFEST_DESTINY_1,1,new RegistryKey[]{},RESEARCH_COST_TIER_1_BUFF_CFG,RESEARCH_RATE_TIER_1_BUFF_CFG).withShowStack(Items.DIRT_PATH)
            .setBuff(MANIFEST_DESTINY));
      registerTech(AGRICULTURE_1, new ResearchTech(AGRICULTURE_1,1,new RegistryKey[]{},RESEARCH_COST_TIER_1_BUFF_CFG,RESEARCH_RATE_TIER_1_BUFF_CFG).withShowStack(Items.WHEAT)
            .setBuff(AGRICULTURE));
      registerTech(INFRASTRUCTURE_1, new ResearchTech(INFRASTRUCTURE_1,1,new RegistryKey[]{},RESEARCH_COST_TIER_1_BUFF_CFG,RESEARCH_RATE_TIER_1_BUFF_CFG).withShowStack(Items.BRICKS)
            .setBuff(INFRASTRUCTURE));
      registerTech(PUBLIC_EDUCATION_1, new ResearchTech(PUBLIC_EDUCATION_1,1,new RegistryKey[]{},RESEARCH_COST_TIER_1_BUFF_CFG,RESEARCH_RATE_TIER_1_BUFF_CFG).withShowStack(Items.BOOKSHELF)
            .setBuff(PUBLIC_EDUCATION));
      registerTech(IMPERIALISM_1, new ResearchTech(IMPERIALISM_1,1,new RegistryKey[]{},RESEARCH_COST_TIER_1_BUFF_CFG,RESEARCH_RATE_TIER_1_BUFF_CFG).withShowStack(GraphicalItem.with(GraphicalItem.GraphicItems.MONUMENT))
            .setBuff(IMPERIALISM));
      registerTech(COLONIALISM_1, new ResearchTech(COLONIALISM_1,1,new RegistryKey[]{},RESEARCH_COST_TIER_1_BUFF_CFG,RESEARCH_RATE_TIER_1_BUFF_CFG).withShowStack(GraphicalItem.with(GraphicalItem.GraphicItems.GROWTH_CAPTURE_POINT))
            .setBuff(COLONIALISM));
      registerTech(SCHOLARSHIP_1, new ResearchTech(SCHOLARSHIP_1,1,new RegistryKey[]{},RESEARCH_COST_TIER_1_BUFF_CFG,RESEARCH_RATE_TIER_1_BUFF_CFG).withShowStack(Items.EXPERIENCE_BOTTLE)
            .setBuff(SCHOLARSHIP));
      registerTech(BARTERING_1, new ResearchTech(BARTERING_1,1,new RegistryKey[]{},RESEARCH_COST_TIER_1_BUFF_CFG,RESEARCH_RATE_TIER_1_BUFF_CFG).withShowStack(MATERIAL_COIN_ITEM)
            .setBuff(BARTERING));
      
      registerTech(MANIFEST_DESTINY_2, new ResearchTech(MANIFEST_DESTINY_2,2,new RegistryKey[]{MANIFEST_DESTINY_1},RESEARCH_COST_TIER_2_BUFF_CFG,RESEARCH_RATE_TIER_2_BUFF_CFG).withShowStack(Items.DIRT_PATH)
            .setBuff(MANIFEST_DESTINY));
      registerTech(AGRICULTURE_2, new ResearchTech(AGRICULTURE_2,2,new RegistryKey[]{AGRICULTURE_1},RESEARCH_COST_TIER_2_BUFF_CFG,RESEARCH_RATE_TIER_2_BUFF_CFG).withShowStack(Items.WHEAT)
            .setBuff(AGRICULTURE));
      registerTech(INFRASTRUCTURE_2, new ResearchTech(INFRASTRUCTURE_2,2,new RegistryKey[]{INFRASTRUCTURE_1},RESEARCH_COST_TIER_2_BUFF_CFG,RESEARCH_RATE_TIER_2_BUFF_CFG).withShowStack(Items.BRICKS)
            .setBuff(INFRASTRUCTURE));
      registerTech(PUBLIC_EDUCATION_2, new ResearchTech(PUBLIC_EDUCATION_2,2,new RegistryKey[]{PUBLIC_EDUCATION_1},RESEARCH_COST_TIER_2_BUFF_CFG,RESEARCH_RATE_TIER_2_BUFF_CFG).withShowStack(Items.BOOKSHELF)
            .setBuff(PUBLIC_EDUCATION));
      registerTech(IMPERIALISM_2, new ResearchTech(IMPERIALISM_2,2,new RegistryKey[]{IMPERIALISM_1},RESEARCH_COST_TIER_2_BUFF_CFG,RESEARCH_RATE_TIER_2_BUFF_CFG).withShowStack(GraphicalItem.with(GraphicalItem.GraphicItems.MONUMENT))
            .setBuff(IMPERIALISM));
      registerTech(COLONIALISM_2, new ResearchTech(COLONIALISM_2,2,new RegistryKey[]{COLONIALISM_1},RESEARCH_COST_TIER_2_BUFF_CFG,RESEARCH_RATE_TIER_2_BUFF_CFG).withShowStack(GraphicalItem.with(GraphicalItem.GraphicItems.GROWTH_CAPTURE_POINT))
            .setBuff(COLONIALISM));
      registerTech(SCHOLARSHIP_2, new ResearchTech(SCHOLARSHIP_2,2,new RegistryKey[]{SCHOLARSHIP_1},RESEARCH_COST_TIER_2_BUFF_CFG,RESEARCH_RATE_TIER_2_BUFF_CFG).withShowStack(Items.EXPERIENCE_BOTTLE)
            .setBuff(SCHOLARSHIP));
      registerTech(BARTERING_2, new ResearchTech(BARTERING_2,2,new RegistryKey[]{BARTERING_1},RESEARCH_COST_TIER_2_BUFF_CFG,RESEARCH_RATE_TIER_2_BUFF_CFG).withShowStack(MATERIAL_COIN_ITEM)
            .setBuff(BARTERING));
      
      registerTech(MANIFEST_DESTINY_3, new ResearchTech(MANIFEST_DESTINY_3,3,new RegistryKey[]{MANIFEST_DESTINY_2},RESEARCH_COST_TIER_3_BUFF_CFG,RESEARCH_RATE_TIER_3_BUFF_CFG).withShowStack(Items.DIRT_PATH)
            .setBuff(MANIFEST_DESTINY));
      registerTech(AGRICULTURE_3, new ResearchTech(AGRICULTURE_3,3,new RegistryKey[]{AGRICULTURE_2},RESEARCH_COST_TIER_3_BUFF_CFG,RESEARCH_RATE_TIER_3_BUFF_CFG).withShowStack(Items.WHEAT)
            .setBuff(AGRICULTURE));
      registerTech(INFRASTRUCTURE_3, new ResearchTech(INFRASTRUCTURE_3,3,new RegistryKey[]{INFRASTRUCTURE_2},RESEARCH_COST_TIER_3_BUFF_CFG,RESEARCH_RATE_TIER_3_BUFF_CFG).withShowStack(Items.BRICKS)
            .setBuff(INFRASTRUCTURE));
      registerTech(PUBLIC_EDUCATION_3, new ResearchTech(PUBLIC_EDUCATION_3,3,new RegistryKey[]{PUBLIC_EDUCATION_2},RESEARCH_COST_TIER_3_BUFF_CFG,RESEARCH_RATE_TIER_3_BUFF_CFG).withShowStack(Items.BOOKSHELF)
            .setBuff(PUBLIC_EDUCATION));
      registerTech(IMPERIALISM_3, new ResearchTech(IMPERIALISM_3,3,new RegistryKey[]{IMPERIALISM_2},RESEARCH_COST_TIER_3_BUFF_CFG,RESEARCH_RATE_TIER_3_BUFF_CFG).withShowStack(GraphicalItem.with(GraphicalItem.GraphicItems.MONUMENT))
            .setBuff(IMPERIALISM));
      registerTech(COLONIALISM_3, new ResearchTech(COLONIALISM_3,3,new RegistryKey[]{COLONIALISM_2},RESEARCH_COST_TIER_3_BUFF_CFG,RESEARCH_RATE_TIER_3_BUFF_CFG).withShowStack(GraphicalItem.with(GraphicalItem.GraphicItems.GROWTH_CAPTURE_POINT))
            .setBuff(COLONIALISM));
      registerTech(SCHOLARSHIP_3, new ResearchTech(SCHOLARSHIP_3,3,new RegistryKey[]{SCHOLARSHIP_2},RESEARCH_COST_TIER_3_BUFF_CFG,RESEARCH_RATE_TIER_3_BUFF_CFG).withShowStack(Items.EXPERIENCE_BOTTLE)
            .setBuff(SCHOLARSHIP));
      registerTech(BARTERING_3, new ResearchTech(BARTERING_3,3,new RegistryKey[]{BARTERING_2},RESEARCH_COST_TIER_3_BUFF_CFG,RESEARCH_RATE_TIER_3_BUFF_CFG).withShowStack(MATERIAL_COIN_ITEM)
            .setBuff(BARTERING));
      
      registerTech(MANIFEST_DESTINY_4, new ResearchTech(MANIFEST_DESTINY_4,4,new RegistryKey[]{MANIFEST_DESTINY_3},RESEARCH_COST_TIER_4_BUFF_CFG,RESEARCH_RATE_TIER_4_BUFF_CFG).withShowStack(Items.DIRT_PATH)
            .setBuff(MANIFEST_DESTINY));
      registerTech(AGRICULTURE_4, new ResearchTech(AGRICULTURE_4,4,new RegistryKey[]{AGRICULTURE_3},RESEARCH_COST_TIER_4_BUFF_CFG,RESEARCH_RATE_TIER_4_BUFF_CFG).withShowStack(Items.WHEAT)
            .setBuff(AGRICULTURE));
      registerTech(INFRASTRUCTURE_4, new ResearchTech(INFRASTRUCTURE_4,4,new RegistryKey[]{INFRASTRUCTURE_3},RESEARCH_COST_TIER_4_BUFF_CFG,RESEARCH_RATE_TIER_4_BUFF_CFG).withShowStack(Items.BRICKS)
            .setBuff(INFRASTRUCTURE));
      registerTech(PUBLIC_EDUCATION_4, new ResearchTech(PUBLIC_EDUCATION_4,4,new RegistryKey[]{PUBLIC_EDUCATION_3},RESEARCH_COST_TIER_4_BUFF_CFG,RESEARCH_RATE_TIER_4_BUFF_CFG).withShowStack(Items.BOOKSHELF)
            .setBuff(PUBLIC_EDUCATION));
      registerTech(IMPERIALISM_4, new ResearchTech(IMPERIALISM_4,4,new RegistryKey[]{IMPERIALISM_3},RESEARCH_COST_TIER_4_BUFF_CFG,RESEARCH_RATE_TIER_4_BUFF_CFG).withShowStack(GraphicalItem.with(GraphicalItem.GraphicItems.MONUMENT))
            .setBuff(IMPERIALISM));
      registerTech(COLONIALISM_4, new ResearchTech(COLONIALISM_4,4,new RegistryKey[]{COLONIALISM_3},RESEARCH_COST_TIER_4_BUFF_CFG,RESEARCH_RATE_TIER_4_BUFF_CFG).withShowStack(GraphicalItem.with(GraphicalItem.GraphicItems.GROWTH_CAPTURE_POINT))
            .setBuff(COLONIALISM));
      registerTech(SCHOLARSHIP_4, new ResearchTech(SCHOLARSHIP_4,4,new RegistryKey[]{SCHOLARSHIP_3},RESEARCH_COST_TIER_4_BUFF_CFG,RESEARCH_RATE_TIER_4_BUFF_CFG).withShowStack(Items.EXPERIENCE_BOTTLE)
            .setBuff(SCHOLARSHIP));
      registerTech(BARTERING_4, new ResearchTech(BARTERING_4,4,new RegistryKey[]{BARTERING_3},RESEARCH_COST_TIER_4_BUFF_CFG,RESEARCH_RATE_TIER_4_BUFF_CFG).withShowStack(MATERIAL_COIN_ITEM)
            .setBuff(BARTERING));
      
      registerTech(MANIFEST_DESTINY_5, new ResearchTech(MANIFEST_DESTINY_5,5,new RegistryKey[]{MANIFEST_DESTINY_4},RESEARCH_COST_TIER_5_BUFF_CFG,RESEARCH_RATE_TIER_5_BUFF_CFG).withShowStack(Items.DIRT_PATH)
            .setBuff(MANIFEST_DESTINY));
      registerTech(AGRICULTURE_5, new ResearchTech(AGRICULTURE_5,5,new RegistryKey[]{AGRICULTURE_4},RESEARCH_COST_TIER_5_BUFF_CFG,RESEARCH_RATE_TIER_5_BUFF_CFG).withShowStack(Items.WHEAT)
            .setBuff(AGRICULTURE));
      registerTech(INFRASTRUCTURE_5, new ResearchTech(INFRASTRUCTURE_5,5,new RegistryKey[]{INFRASTRUCTURE_4},RESEARCH_COST_TIER_5_BUFF_CFG,RESEARCH_RATE_TIER_5_BUFF_CFG).withShowStack(Items.BRICKS)
            .setBuff(INFRASTRUCTURE));
      registerTech(PUBLIC_EDUCATION_5, new ResearchTech(PUBLIC_EDUCATION_5,5,new RegistryKey[]{PUBLIC_EDUCATION_4},RESEARCH_COST_TIER_5_BUFF_CFG,RESEARCH_RATE_TIER_5_BUFF_CFG).withShowStack(Items.BOOKSHELF)
            .setBuff(PUBLIC_EDUCATION));
      registerTech(IMPERIALISM_5, new ResearchTech(IMPERIALISM_5,5,new RegistryKey[]{IMPERIALISM_4},RESEARCH_COST_TIER_5_BUFF_CFG,RESEARCH_RATE_TIER_5_BUFF_CFG).withShowStack(GraphicalItem.with(GraphicalItem.GraphicItems.MONUMENT))
            .setBuff(IMPERIALISM));
      registerTech(COLONIALISM_5, new ResearchTech(COLONIALISM_5,5,new RegistryKey[]{COLONIALISM_4},RESEARCH_COST_TIER_5_BUFF_CFG,RESEARCH_RATE_TIER_5_BUFF_CFG).withShowStack(GraphicalItem.with(GraphicalItem.GraphicItems.GROWTH_CAPTURE_POINT))
            .setBuff(COLONIALISM));
      registerTech(SCHOLARSHIP_5, new ResearchTech(SCHOLARSHIP_5,5,new RegistryKey[]{SCHOLARSHIP_4},RESEARCH_COST_TIER_5_BUFF_CFG,RESEARCH_RATE_TIER_5_BUFF_CFG).withShowStack(Items.EXPERIENCE_BOTTLE)
            .setBuff(SCHOLARSHIP));
      registerTech(BARTERING_5, new ResearchTech(BARTERING_5,5,new RegistryKey[]{BARTERING_4},RESEARCH_COST_TIER_5_BUFF_CFG,RESEARCH_RATE_TIER_5_BUFF_CFG).withShowStack(MATERIAL_COIN_ITEM)
            .setBuff(BARTERING));
   }
   
   private static ResearchTech registerTech(RegistryKey<ResearchTech> key, ResearchTech tech){
      Registry.register(RESEARCH,key,tech);
      for(Item item : tech.getCraftLocked()){
         LOCKED_ITEMS.put(item,key);
      }
      for(RegistryEntry<Potion> entry : tech.getPotionLocked()){
         LOCKED_POTIONS.put(entry.value(),key);
         
         Supplier<Integer> costGetter = () -> tech.getTier() * NationsConfig.getInt(RESEARCH_COST_POTIONS_PER_TIER_CFG);
         Supplier<Integer> rateGetter = () -> NationsConfig.getInt(RESEARCH_RATE_POTIONS_CFG);
         RegistryKey<ResearchTech> subKey = of(entry.getKey().get().getValue().getPath());
         registerTech(subKey,new ResearchTech(subKey,-1,new RegistryKey[]{key},costGetter,rateGetter));
         POTION_TECHS.put(entry.value(),subKey);
      }
      
      for(ArcanaItem arcanaItem : tech.getArcanaLocked()){
         Supplier<Integer> costGetter = () -> (arcanaItem.getRarity().rarity+1) * NationsConfig.getInt(RESEARCH_COST_ARCANA_ITEMS_PER_RARITY_CFG);
         Supplier<Integer> rateGetter = () -> NationsConfig.getInt(RESEARCH_RATE_ARCANA_ITEMS_CFG);
         RegistryKey<ResearchTech> subKey = of(arcanaItem.getId());
         registerTech(subKey,new ResearchTech(subKey,-1,new RegistryKey[]{key},costGetter,rateGetter));
         ARCANA_TECHS.put(arcanaItem,subKey);
      }
      
      
      for(Pair<RegistryKey<Enchantment>, Integer> pair : tech.getEnchantLocked()){
         int level = pair.getRight();
         RegistryKey<Enchantment> enchant = pair.getLeft();
         
         Supplier<Integer> costGetter = () -> {
            Enchantment enchantment = MiscUtils.getEnchantment(enchant).value();
            return level * switch(enchantment.getMaxLevel()){
               case 1 -> NationsConfig.getInt(RESEARCH_COST_ENCHANTMENTS_SINGLE_PER_LEVEL_CFG);
               case 2 -> NationsConfig.getInt(RESEARCH_COST_ENCHANTMENTS_DOUBLE_PER_LEVEL_CFG);
               case 3 -> NationsConfig.getInt(RESEARCH_COST_ENCHANTMENTS_TRIPLE_PER_LEVEL_CFG);
               case 4 -> NationsConfig.getInt(RESEARCH_COST_ENCHANTMENTS_QUADRUPLE_PER_LEVEL_CFG);
               default -> NationsConfig.getInt(RESEARCH_COST_ENCHANTMENTS_QUINTUPLE_PER_LEVEL_CFG);
            };
         };
         Supplier<Integer> rateGetter = () -> NationsConfig.getInt(RESEARCH_RATE_ENCHANTMENTS_CFG);
         RegistryKey<ResearchTech>[] keys = new RegistryKey[level];
         for(int i = 0; i < level; i++){
            if(i == 0){
               keys[i] = key;
            }else{
               keys[i] = of(enchant.getValue().getPath()+"."+i);
            }
         }
         RegistryKey<ResearchTech> subKey = of(enchant.getValue().getPath()+"."+level);
         registerTech(subKey,new ResearchTech(subKey,-1,keys,costGetter,rateGetter));
         ENCHANT_TECHS.put(new Pair<>(enchant,level),subKey);
      }
      
      return tech;
   }
   
   private static RegistryKey<ResearchTech> of(String id){
      return RegistryKey.of(RESEARCH.getKey(),Identifier.of(MOD_ID,id));
   }
   
   private static NationsConfig.ConfigSetting<?> registerConfigSetting(NationsConfig.ConfigSetting<?> setting){
      Registry.register(CONFIG_SETTINGS,Identifier.of(MOD_ID,setting.getId()),setting);
      return setting;
   }
   
   public static void initialize(){
      PolymerResourcePackUtils.addModAssets(MOD_ID);
      
      final ItemGroup ITEM_GROUP = PolymerItemGroupUtils.builder().displayName(Text.translatable("itemGroup.nations_items")).icon(() -> new ItemStack(RESEARCH_COIN_ITEM)).entries((displayContext, entries) -> {
         entries.add(new ItemStack(COIN_PURSE_ITEM));
         entries.add(new ItemStack(GROWTH_COIN_ITEM));
         entries.add(new ItemStack(MATERIAL_COIN_ITEM));
         entries.add(new ItemStack(RESEARCH_COIN_ITEM));
         entries.add(new ItemStack(GROWTH_BULLION_ITEM));
         entries.add(new ItemStack(MATERIAL_BULLION_ITEM));
         entries.add(new ItemStack(RESEARCH_BULLION_ITEM));
         entries.add(new ItemStack(VICTORY_POINT_ITEM));
         entries.add(new ItemStack(BUG_VOUCHER_ITEM));
      }).build();
      
      PolymerItemGroupUtils.registerPolymerItemGroup(Identifier.of(MOD_ID,"nations_items"), ITEM_GROUP);
   }
   
   public static void withServer(MinecraftServer server){
   
   }
}
