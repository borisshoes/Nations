package net.borisshoes.nations.callbacks;

import com.mojang.brigadier.CommandDispatcher;
import net.borisshoes.nations.Nations;
import net.borisshoes.nations.NationsCommands;
import net.borisshoes.nations.gameplay.ChatChannel;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.ChunkSectionPos;

import static com.mojang.brigadier.arguments.DoubleArgumentType.doubleArg;
import static com.mojang.brigadier.arguments.DoubleArgumentType.getDouble;
import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.LongArgumentType.getLong;
import static com.mojang.brigadier.arguments.LongArgumentType.longArg;
import static com.mojang.brigadier.arguments.StringArgumentType.*;
import static net.minecraft.command.argument.EntityArgumentType.getPlayer;
import static net.minecraft.command.argument.EntityArgumentType.player;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class CommandRegisterCallback {
   
   public static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess commandRegistryAccess, CommandManager.RegistrationEnvironment registrationEnvironment){
      dispatcher.register(literal("nations").requires(source -> source.hasPermissionLevel(2))
            .then(literal("survey").requires(source -> source.hasPermissionLevel(2))
                  .then(argument("radius", integer(0,32)).executes(context -> NationsCommands.adminSurvey(context, getInteger(context,"radius")))))
            .then(literal("initializeWorld").requires(source -> source.hasPermissionLevel(2)).executes(NationsCommands::initializeWorld))
            .then(literal("test").requires(source -> source.hasPermissionLevel(2)).executes(NationsCommands::test))
            .then(literal("refresh").requires(source -> source.hasPermissionLevel(2)).executes(NationsCommands::refresh))
            .then(literal("bypassClaims").requires(source -> source.hasPermissionLevel(2)).executes(NationsCommands::bypassClaims))
            .then(literal("forceWar").requires(source -> source.hasPermissionLevel(2)).executes(NationsCommands::forceWar))
            .then(literal("forceRift").requires(source -> source.hasPermissionLevel(2)).executes(NationsCommands::forceRift))
            .then(literal("forceHourlyTick").requires(source -> source.hasPermissionLevel(2)).executes(NationsCommands::forceHourlyTick))
            .then(literal("forceDailyTick").requires(source -> source.hasPermissionLevel(2)).executes(NationsCommands::forceDailyTick))
            .then(literal("closeRift").requires(source -> source.hasPermissionLevel(2)).executes(NationsCommands::cancelRift))
            .then(literal("research").requires(source -> source.hasPermissionLevel(2))
                  .then(literal("grant")
                        .then(argument("nation_id", word()).suggests(NationsCommands::getNationSuggestions)
                              .then(argument("research_id", word()).suggests(NationsCommands::getResearchSuggestions)
                                    .executes(context -> NationsCommands.adminResearch(context, getString(context, "nation_id"), getString(context, "research_id"), true, false)))))
                  .then(literal("revoke")
                        .then(argument("nation_id", word()).suggests(NationsCommands::getNationSuggestions)
                              .then(argument("research_id", word()).suggests(NationsCommands::getResearchSuggestions)
                                    .executes(context -> NationsCommands.adminResearch(context, getString(context, "nation_id"), getString(context, "research_id"), false, false))
                                    .then(literal("removePostReqs")
                                          .executes(context -> NationsCommands.adminResearch(context, getString(context, "nation_id"), getString(context, "research_id"), false, true)))))))
            .then(literal("giveVP").requires(source -> source.hasPermissionLevel(2))
                  .then(argument("nation_id", word()).suggests(NationsCommands::getNationSuggestions)
                        .then(argument("count", integer()).executes(context -> NationsCommands.giveVictoryPoints(context, getString(context, "nation_id"), getInteger(context, "count"))))))
            .then(literal("changeColors").requires(source -> source.hasPermissionLevel(2))
                  .then(argument("nation_id", word()).suggests(NationsCommands::getNationSuggestions)
                        .then(argument("text_color_main", string())
                              .then(argument("text_color_sub", string())
                                    .then(argument("dye_color", string()).suggests((context, builder) -> NationsCommands.getEnumSuggestions(context,builder, DyeColor.class))
                                          .executes(context -> NationsCommands.updateColors(context, getString(context,"nation_id"), getString(context, "text_color_main"), getString(context, "text_color_sub"), getString(context, "dye_color"))))))))
            .then(literal("changeName").requires(source -> source.hasPermissionLevel(2))
                  .then(argument("nation_id", word()).suggests(NationsCommands::getNationSuggestions)
                        .then(argument("nation_name", string())
                              .executes(context -> NationsCommands.changeName(context, getString(context,"nation_id"), getString(context, "nation_name"))))))
            .then(literal("nick").requires(source -> source.hasPermissionLevel(2))
                  .then(argument("player", player())
                        .executes(context -> NationsCommands.nickPlayer(context, getPlayer(context,"player"), ""))
                        .then(argument("nickname", word())
                              .executes(context -> NationsCommands.nickPlayer(context, getPlayer(context,"player"), getString(context, "nickname"))))))
            .then(literal("delete").requires(source -> source.hasPermissionLevel(2))
                  .then(argument("nation_id", word()).suggests(NationsCommands::getNationSuggestions)
                        .executes(context -> NationsCommands.deleteNation(context, getString(context, "nation_id")))))
            .then(literal("create").requires(source -> source.hasPermissionLevel(2))
                  .then(argument("nation_id", word())
                        .then(argument("nation_name", string())
                              .then(argument("text_color_main", string())
                                    .then(argument("text_color_sub", string())
                                          .then(argument("dye_color", string()).suggests((context, builder) -> NationsCommands.getEnumSuggestions(context,builder, DyeColor.class))
                                                .executes(context -> NationsCommands.createNation(context, getString(context,"nation_id"), getString(context, "nation_name"), getString(context, "text_color_main"), getString(context, "text_color_sub"), getString(context, "dye_color")))))))))
            .then(literal("join").requires(source -> source.hasPermissionLevel(2))
                  .then(argument("nation_id", word()).suggests(NationsCommands::getNationSuggestions)
                        .executes(context -> NationsCommands.joinNation(context, getString(context,"nation_id"), context.getSource().getPlayer()))
                        .then(argument("player", player())
                              .executes(context -> NationsCommands.joinNation(context, getString(context,"nation_id"), getPlayer(context, "player"))))))
            .then(literal("leave").requires(source -> source.hasPermissionLevel(2))
                  .executes(context -> NationsCommands.leaveNation(context, context.getSource().getPlayer()))
                  .then(argument("player", player())
                        .executes(context -> NationsCommands.leaveNation(context, getPlayer(context, "player")))))
            .then(literal("addShopItem").requires(source -> source.hasPermissionLevel(2))
                  .then(argument("price",integer())
                        .executes(context -> NationsCommands.addShopItem(context, getInteger(context, "price")))))
            .then(literal("announce").requires(source -> source.hasPermissionLevel(2))
                  .then(argument("message", greedyString())
                        .executes(context -> NationsCommands.announce(context, getString(context,"message")))))
            .then(literal("sendMail").requires(source -> source.hasPermissionLevel(2))
                  .then(argument("nation_id", word()).suggests(NationsCommands::getNationSuggestions)
                        .executes(context -> NationsCommands.sendMail(context, getString(context, "nation_id")))))
            .then(literal("setNextWar").requires(source -> source.hasPermissionLevel(2))
                  .then(argument("time", longArg())
                        .executes(context -> NationsCommands.setNextWar(context, getLong(context, "time")))))
            .then(literal("getNextWar").requires(source -> source.hasPermissionLevel(2)).executes(NationsCommands::getNextWar))
            .then(literal("transferCap").requires(source -> source.hasPermissionLevel(2))
                  .then(argument("nation_id", word()).suggests(NationsCommands::getNationSuggestions)
                        .executes(context -> NationsCommands.transferCapturePoint(context, getString(context, "nation_id"), ChunkSectionPos.from(context.getSource().getPosition())))
                        .then(argument("x", integer())
                              .then(argument("z", integer())
                                    .executes(context -> NationsCommands.transferCapturePoint(context, getString(context, "nation_id"), ChunkSectionPos.from(getInteger(context,"x"),0,getInteger(context,"z"))))))))
            .then(literal("modifyCap").requires(source -> source.hasPermissionLevel(2))
                  .then(argument("modifier", doubleArg())
                        .then(argument("duration", integer(0))
                              .executes(context -> NationsCommands.modifyCapturePoint(context, getDouble(context, "modifier"), getInteger(context,"duration"), ChunkSectionPos.from(context.getSource().getPosition())))
                              .then(argument("x", integer())
                                    .then(argument("z", integer())
                                          .executes(context -> NationsCommands.modifyCapturePoint(context, getDouble(context, "modifier"), getInteger(context,"duration"), ChunkSectionPos.from(getInteger(context,"x"),0,getInteger(context,"z")))))))))
      );
      
      dispatcher.register(literal("lc").executes(context -> NationsCommands.changeChatChannel(context, ChatChannel.LOCAL)));
      dispatcher.register(literal("gc").executes(context -> NationsCommands.changeChatChannel(context, ChatChannel.GLOBAL)));
      dispatcher.register(literal("nc").executes(context -> NationsCommands.changeChatChannel(context, ChatChannel.NATION)));
      
      dispatcher.register(literal("nation")
            .then(literal("survey").executes(NationsCommands::nationSurvey))
            .then(literal("settle").executes(NationsCommands::nationSettle))
            .then(literal("promote")
                  .then(argument("player", player())
                        .executes(context -> NationsCommands.changePerms(context, getPlayer(context, "player"), true))))
            .then(literal("demote")
                  .then(argument("player", player())
                        .executes(context -> NationsCommands.changePerms(context, getPlayer(context, "player"), false))))
            .then(literal("chunk")
                  .executes(context -> NationsCommands.openChunkMenu(context, ChunkSectionPos.from(context.getSource().getPosition())))
                  .then(argument("x", integer())
                        .then(argument("z", integer())
                              .executes(context -> NationsCommands.openChunkMenu(context, ChunkSectionPos.from(getInteger(context,"x"),0,getInteger(context,"z")))))))
            .then(literal("channel")
                  .then(literal("nation").executes(context -> NationsCommands.changeChatChannel(context, ChatChannel.NATION)))
                  .then(literal("local").executes(context -> NationsCommands.changeChatChannel(context, ChatChannel.LOCAL)))
                  .then(literal("global").executes(context -> NationsCommands.changeChatChannel(context, ChatChannel.GLOBAL))))
            .then(literal("researchStatus")
                  .executes(NationsCommands::researchStatus))
            .then(literal("leaderboard")
                  .executes(NationsCommands::leaderboard))
            .then(literal("toggleTrespassAlerts")
                  .executes(NationsCommands::toggleTrespassAlerts))
      );
      
      dispatcher.register(Nations.CONFIG.generateCommand());
   }
   
   
   
}
