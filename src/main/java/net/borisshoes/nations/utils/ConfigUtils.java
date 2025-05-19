package net.borisshoes.nations.utils;

import com.mojang.brigadier.arguments.*;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.util.StringIdentifiable;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static net.borisshoes.nations.Nations.MOD_ID;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

@SuppressWarnings({"unchecked","rawtype"})
public class ConfigUtils {
   public Set<IConfigValue> values;
   private final File file;
   private final Logger logger;
   
   public ConfigUtils(File file, Logger logger, Set<IConfigValue> values){
      this.file = file;
      this.logger = logger;
      this.values = values;
      this.read();
      this.save();
   }
   
   public void read(){
      try(BufferedReader input = new BufferedReader(new InputStreamReader(new FileInputStream(file)))){
         logger.debug("Reading Nations config...");
         
         for(IConfigValue value : this.values){
            value.setValue(value.defaultValue);
         }
         
         while(input.ready()){
            String configLine = input.readLine();
            String trimmed = configLine.trim();
            if(trimmed.isEmpty()) continue;
            char firstChar = trimmed.charAt(0);
            
            if(firstChar == '!' || firstChar == '#') continue;
            if(!configLine.contains("=")) continue;
            
            int splitIndex = configLine.indexOf('=');
            String valueName = configLine.substring(0, splitIndex).strip();
            String valueValue = configLine.substring(splitIndex + 1).strip();
            
            for(IConfigValue value : this.values){
               if(!valueName.equals(value.name)) continue;
               Object defaultValue = value.defaultValue;
               try{
                  value.setValue(value.getFromString(valueValue));
               }catch(Exception e){
                  value.setValue(defaultValue);
               }
            }
         }
         
      }catch(FileNotFoundException ignored){
         logger.debug("Initialising Nations config...");
         this.values.forEach(value -> value.value = value.defaultValue);
      }catch(IOException e){
         logger.fatal("Failed to load Nations config file!");
         e.printStackTrace();
      }catch(Exception e){
         logger.fatal("Failed to parse Nations config");
         e.printStackTrace();
      }
   }
   
   public void save(){
      logger.debug("Updating Nations config...");
      try(BufferedWriter output = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)))){
         output.write("# Nations Configuration File" + " | " + new Date());
         output.newLine();
         output.newLine();
         
         for(IConfigValue value : this.values){
            if(value.comment != null){
               output.write("# "+ Text.translatable(value.comment).getString());
               output.newLine();
            }
            output.write(value.name + " = " + value.getValueString());
            output.newLine();
         }
      }catch(IOException e){
         logger.fatal("Failed to save Nations config file!");
         e.printStackTrace();
      }
   }
   
   public LiteralArgumentBuilder<ServerCommandSource> generateCommand(){
      LiteralArgumentBuilder<ServerCommandSource> out =
            literal("nations").then(literal("config").requires(source -> source.hasPermissionLevel(4))
                  .executes(ctx -> {
                     values.stream().filter(v -> v.command != null).forEach(value ->
                           ctx.getSource().sendFeedback(()-> MutableText.of(new TranslatableTextContent(value.command.getterText, null, new String[] {String.valueOf(value.getValueString())})), false));
                     return 1;
                  }));
      values.stream().filter(v -> v.command != null).forEach(value ->
            out.then(literal("config").then(literal(value.name)
                  .executes(ctx -> {
                     ctx.getSource().sendFeedback(()->MutableText.of(new TranslatableTextContent(value.command.getterText, null, new String[] {String.valueOf(value.getValueString())})), false);
                     return 1;
                  })
                  .then(argument(value.name, value.getArgumentType()).suggests(value::getSuggestions)
                        .executes(ctx -> {
                           value.value = value.parseArgumentValue(ctx);
                           ((CommandContext<ServerCommandSource>) ctx).getSource().sendFeedback(()->MutableText.of(new TranslatableTextContent(value.command.setterText, null, new String[] {String.valueOf(value.getValueString())})), true);
                           this.save();
                           return 1;
                        })))));
      return out;
   }
   
   public Object getValue(String name){
      return values.stream().filter(value -> value.name.equals(name)).findFirst().map(iConfigValue -> iConfigValue.value).orElse(null);
   }
   
   public abstract static class IConfigValue<T>{
      protected final T defaultValue;
      protected final String name;
      protected final String comment;
      protected final Command command;
      protected T value;
      
      public IConfigValue(@NotNull String name, T defaultValue, @Nullable String comment, @Nullable Command command){
         this.name = name;
         this.defaultValue = defaultValue;
         this.comment = comment;
         this.command = command;
      }
      
      public String getName(){
         return name;
      }
      
      public abstract T getFromString(String value);
      
      public abstract ArgumentType<?> getArgumentType();
      
      public abstract T parseArgumentValue(CommandContext<ServerCommandSource> ctx);
      
      public abstract CompletableFuture<Suggestions> getSuggestions(CommandContext<ServerCommandSource> ctx, SuggestionsBuilder builder);
      
      public abstract String getValueString();
      
      private void setValue(T value){
         this.value = value;
      }
   }
   
   public static class DoubleConfigValue extends IConfigValue<Double> {
      protected final double defaultValue;
      private final DoubleLimits limits;
      
      public DoubleConfigValue(@NotNull String name, Double defaultValue, DoubleLimits limits){
         super(name, defaultValue, getTranslation(name,"comment"), new ConfigUtils.Command(name, true));
         this.defaultValue = defaultValue;
         this.limits = limits;
      }
      
      public DoubleConfigValue(@NotNull String name, Double defaultValue, DoubleLimits limits, @Nullable String comment, @Nullable Command command){
         super(name, defaultValue, comment, command);
         this.defaultValue = defaultValue;
         this.limits = limits;
      }
      
      public DoubleConfigValue(@NotNull String name, Double defaultValue, DoubleLimits limits, @Nullable Command command){
         this(name, defaultValue, limits, null, command);
      }
      
      @Override
      public Double getFromString(String value){
         return Double.parseDouble(value);
      }
      
      @Override
      public ArgumentType<Double> getArgumentType(){
         return DoubleArgumentType.doubleArg(limits.min, limits.max);
      }
      
      @Override
      public Double parseArgumentValue(CommandContext<ServerCommandSource> ctx){
         return DoubleArgumentType.getDouble(ctx, name);
      }
      
      public CompletableFuture<Suggestions> getSuggestions(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder){
         return builder.buildFuture();
      }
      
      @Override
      public String getValueString(){
         return this.value.toString();
      }
      
      public static class DoubleLimits {
         double min = Double.MIN_VALUE, max = Double.MAX_VALUE;
         
         public DoubleLimits(){}
         
         public DoubleLimits(double min){
            this.min = min;
         }
         
         public DoubleLimits(double min, double max){
            this.min = min;
            this.max = max;
         }
      }
   }
   
   public static class IntArrayConfigValue extends IConfigValue<int[]> {
      protected final int[] defaultValue;
      
      public IntArrayConfigValue(@NotNull String name, int[] defaultValue){
         super(name, defaultValue, getTranslation(name,"comment"), new ConfigUtils.Command(name, true));
         this.defaultValue = defaultValue;
      }
      
      public IntArrayConfigValue(@NotNull String name, int[] defaultValue, @Nullable String comment, @Nullable Command command){
         super(name, defaultValue, comment, command);
         this.defaultValue = defaultValue;
      }
      
      public IntArrayConfigValue(@NotNull String name, int[] defaultValue, @Nullable Command command){
         this(name, defaultValue, null, command);
      }
      
      @Override
      public int[] getFromString(String value){
         try {
            value = value.trim();
            if (value.startsWith("[") && value.endsWith("]")) {
               value = value.substring(1, value.length() - 1).trim();
            }
            if (value.isEmpty()) {
               return new int[0];
            }
            
            String[] tokens = value.split("[,\\s]+");
            int[] result = new int[tokens.length];
            for (int i = 0; i < tokens.length; i++) {
               result[i] = Integer.parseInt(tokens[i]);
            }
            
            return result;
         } catch (Exception e) {
            return defaultValue;
         }
      }
      
      @Override
      public ArgumentType<?> getArgumentType(){
         return StringArgumentType.greedyString();
      }
      
      @Override
      public int[] parseArgumentValue(CommandContext<ServerCommandSource> ctx){
         String str = StringArgumentType.getString(ctx, name);
         return getFromString(str);
      }
      
      @Override
      public CompletableFuture<Suggestions> getSuggestions(CommandContext<ServerCommandSource> ctx, SuggestionsBuilder builder){
         return builder.buildFuture();
      }
      
      @Override
      public String getValueString(){
         return Arrays.toString(this.value);
      }
   }
   
   public static class IntegerConfigValue extends IConfigValue<Integer> {
      protected final int defaultValue;
      private final IntLimits limits;
      
      public IntegerConfigValue(@NotNull String name, Integer defaultValue, IntLimits limits){
         super(name, defaultValue, getTranslation(name,"comment"), new ConfigUtils.Command(name, true));
         this.defaultValue = defaultValue;
         this.limits = limits;
      }
      
      public IntegerConfigValue(@NotNull String name, Integer defaultValue, IntLimits limits, @Nullable String comment, @Nullable Command command){
         super(name, defaultValue, comment, command);
         this.defaultValue = defaultValue;
         this.limits = limits;
      }
      
      public IntegerConfigValue(@NotNull String name, Integer defaultValue, IntLimits limits, @Nullable Command command){
         this(name, defaultValue, limits, null, command);
      }
      
      @Override
      public Integer getFromString(String value){
         return Integer.parseInt(value);
      }
      
      @Override
      public ArgumentType<Integer> getArgumentType(){
         return IntegerArgumentType.integer(limits.min, limits.max);
      }
      
      @Override
      public Integer parseArgumentValue(CommandContext<ServerCommandSource> ctx){
         return IntegerArgumentType.getInteger(ctx, name);
      }
      
      public CompletableFuture<Suggestions> getSuggestions(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder){
         if(limits.min > 0 && limits.max - limits.min < 10000){
            String start = builder.getRemaining().toLowerCase(Locale.ROOT);
            Set<String> nums = new HashSet<>();
            for(int i = limits.min; i <= limits.max; i++){
               nums.add(String.valueOf(i));
            }
            nums.stream().filter(s -> s.startsWith(start)).forEach(builder::suggest);
         }
         return builder.buildFuture();
      }
      
      @Override
      public String getValueString(){
         return this.value.toString();
      }
      
      public static class IntLimits {
         int min = Integer.MIN_VALUE, max = Integer.MAX_VALUE;
         
         public IntLimits(){}
         
         public IntLimits(int min){
            this.min = min;
         }
         
         public IntLimits(int min, int max){
            this.min = min;
            this.max = max;
         }
      }
   }
   
   public static class BooleanConfigValue extends IConfigValue<Boolean> {
      protected final boolean defaultValue;
      
      public BooleanConfigValue(@NotNull String name, boolean defaultValue){
         super(name, defaultValue, getTranslation(name,"comment"), new ConfigUtils.Command(name, true));
         this.defaultValue = defaultValue;
      }
      
      public BooleanConfigValue(@NotNull String name, boolean defaultValue, @Nullable String comment, @Nullable Command command){
         super(name, defaultValue, comment, command);
         this.defaultValue = defaultValue;
      }
      
      @Override
      public Boolean getFromString(String value){
         return Boolean.parseBoolean(value);
      }
      
      @Override
      public ArgumentType<Boolean> getArgumentType(){
         return BoolArgumentType.bool();
      }
      
      @Override
      public Boolean parseArgumentValue(CommandContext<ServerCommandSource> ctx){
         return BoolArgumentType.getBool(ctx, name);
      }
      
      public CompletableFuture<Suggestions> getSuggestions(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder){
         Set<String> options = new HashSet<>();
         options.add("true");
         options.add("false");
         String start = builder.getRemaining().toLowerCase(Locale.ROOT);
         options.stream().filter(s -> s.toLowerCase(Locale.ROOT).startsWith(start)).forEach(builder::suggest);
         return builder.buildFuture();
      }
      
      @Override
      public String getValueString(){
         return this.value.toString();
      }
   }
   
   public static class StringConfigValue extends IConfigValue<String> {
      protected final String defaultValue;
      protected final String[] options;
      
      public StringConfigValue(@NotNull String name, String defaultValue, @Nullable String... options){
         super(name, defaultValue, getTranslation(name,"comment"), new ConfigUtils.Command(name, true));
         this.defaultValue = defaultValue;
         this.options = options;
      }
      
      public StringConfigValue(@NotNull String name, String defaultValue, @Nullable String comment, @Nullable Command command, @Nullable String... options){
         super(name, defaultValue, comment, command);
         this.defaultValue = defaultValue;
         this.options = options;
      }
      
      @Override
      public String getFromString(String value){
         return value;
      }
      
      @Override
      public ArgumentType<String> getArgumentType(){
         return StringArgumentType.greedyString();
      }
      
      @Override
      public String parseArgumentValue(CommandContext<ServerCommandSource> ctx){
         return StringArgumentType.getString(ctx, name);
      }
      
      public CompletableFuture<Suggestions> getSuggestions(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder){
         String start = builder.getRemaining().toLowerCase(Locale.ROOT);
         Arrays.stream(options).filter(s -> s.toLowerCase(Locale.ROOT).startsWith(start)).forEach(builder::suggest);
         return builder.buildFuture();
      }
      
      @Override
      public String getValueString(){
         return this.value;
      }
   }
   
   public static class EnumConfigValue<K extends Enum<K> & StringIdentifiable> extends IConfigValue<K>{
      protected final K defaultValue;
      private final Class<K> typeClass;
      
      public EnumConfigValue(@NotNull String name, K defaultValue, Class<K> typeClass){
         super(name, defaultValue, getTranslation(name,"comment"), new ConfigUtils.Command(name, true));
         this.defaultValue = defaultValue;
         this.typeClass = typeClass;
      }
      
      public EnumConfigValue(@NotNull String name, K defaultValue, @Nullable String comment, @Nullable Command command, Class<K> typeClass){
         super(name, defaultValue, comment, command);
         this.defaultValue = defaultValue;
         this.typeClass = typeClass;
      }
      
      public EnumConfigValue(@NotNull String name, K defaultValue, @Nullable Command command, Class<K> typeClass){
         this(name, defaultValue, null, command, typeClass);
      }
      
      @Override
      public K getFromString(String value){
         for(K k : EnumSet.allOf(typeClass)){
            if(k.asString().equalsIgnoreCase(value)){
               return k;
            }
         }
         throw new IllegalArgumentException("Could not map "+ value +" to enum "+typeClass.getName());
      }
      
      @Override
      public ArgumentType<String> getArgumentType(){
         return StringArgumentType.string();
      }
      
      @Override
      public K parseArgumentValue(CommandContext<ServerCommandSource> ctx){
         String parsedString = StringArgumentType.getString(ctx, name);
         return K.valueOf(this.typeClass,parsedString);
      }
      
      public CompletableFuture<Suggestions> getSuggestions(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder){
         Set<String> options = new HashSet<>();
         for(K k : EnumSet.allOf(typeClass)){
            options.add(k.asString());
         }
         String start = builder.getRemaining().toLowerCase(Locale.ROOT);
         options.stream().filter(s -> s.toLowerCase(Locale.ROOT).startsWith(start)).forEach(builder::suggest);
         return builder.buildFuture();
      }
      
      @Override
      public String getValueString(){
         return this.value.toString();
      }
   }
   
   public static class Command {
      protected String setterText;
      protected String getterText;
      protected String errorText;
      
      public Command(String name, boolean withError){
         this.getterText = getTranslation(name,"getter");
         this.setterText = getTranslation(name,"setter");
         if(withError){
            this.errorText = getTranslation(name,"error");
         }else{
            this.errorText = null;
         }
      }
      
      public Command(String getterText, String setterText, @Nullable String errorText){
         this.getterText = getterText;
         this.setterText = setterText;
         this.errorText = errorText;
      }
      
      public Command(String getterText, String setterText){
         this(getterText, setterText, null);
      }
   }
   
   public static <K extends Enum<K> & StringIdentifiable> CompletableFuture<Suggestions> getEnumSuggestions(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder, Class<K> enumClass){
      Set<String> options = new HashSet<>();
      for(K k : EnumSet.allOf(enumClass)){
         options.add(k.asString());
      }
      String start = builder.getRemaining().toLowerCase(Locale.ROOT);
      options.stream().filter(s -> s.toLowerCase(Locale.ROOT).startsWith(start)).forEach(builder::suggest);
      return builder.buildFuture();
   }
   
   public static <K extends Enum<K> & StringIdentifiable> K parseEnum(String string, Class<K> enumClass){
      Optional<K> opt = EnumSet.allOf(enumClass).stream().filter(en -> en.asString().equals(string)).findFirst();
      return opt.orElse(null);
   }
   
   private static String getTranslation(String name, String suffix){
      if(suffix.equals("error")) return "command."+MOD_ID+".error";
      if(suffix.equals("setter") || suffix.equals("getter")) suffix = "getter_setter";
      return "command."+MOD_ID+"."+name.replaceAll("([a-z])([A-Z]+)", "$1_$2").toLowerCase(Locale.ROOT)+"."+suffix;
   }
   
}
