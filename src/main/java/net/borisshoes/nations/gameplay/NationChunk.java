package net.borisshoes.nations.gameplay;

import net.borisshoes.nations.Nations;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtString;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.ChunkPos;

import static net.borisshoes.nations.Nations.log;

public class NationChunk {
   
   private final ChunkPos location;
   private String controllingNationId;
   private String capturePointId;
   private boolean influenced;
   private boolean claimed;
   private int farmlandLvl;
   private boolean machinery;
   private boolean anchored;
   private boolean explosionsOverridden;
   private double coinMultiplier;
   private boolean arena;
   
   public NationChunk(ChunkPos location){
      this.location = location;
      this.controllingNationId = null;
      this.capturePointId = null;
      this.influenced = false;
      this.claimed = false;
      this.machinery = false;
      this.anchored = false;
      this.explosionsOverridden = false;
      this.arena = false;
      this.farmlandLvl = 0;
      this.coinMultiplier = 1;
   }
   
   private NationChunk(ChunkPos location, String controllingNationId, String capturePointId, boolean influenced, boolean claimed, boolean machinery, boolean anchored, boolean explosionsOverridden, boolean arena, int farmlandLvl, double coinMultiplier){
      this.location = location;
      this.controllingNationId = controllingNationId;
      this.capturePointId = capturePointId;
      this.influenced = influenced;
      this.claimed = claimed;
      this.machinery = machinery;
      this.anchored = anchored;
      this.explosionsOverridden = explosionsOverridden;
      this.farmlandLvl = farmlandLvl;
      this.coinMultiplier = coinMultiplier;
   }
   
   public void reset(){
      this.controllingNationId = null;
      this.capturePointId = null;
      this.influenced = false;
      this.claimed = false;
      this.machinery = false;
      this.anchored = false;
      this.explosionsOverridden = false;
      this.farmlandLvl = 0;
      this.coinMultiplier = 1;
   }
   
   public void setControllingNationId(String controllingNationId){
      Nation oldNation = Nations.getNation(controllingNationId);
      if(oldNation != null){
         oldNation.getChunks().remove(this);
      }
      
      this.controllingNationId = controllingNationId;
      
      Nation newNation = Nations.getNation(controllingNationId);
      if(newNation != null){
         newNation.getChunks().add(this);
         
         CapturePoint capturePoint = Nations.getCapturePoint(getPos());
         if(capturePoint != null && (oldNation == null || !oldNation.equals(newNation))){
            capturePoint.transferOwnership(Nations.SERVER.getOverworld(),newNation);
            capturePoint.cancelAuction();
            MutableText announcement = Text.translatable("text.nations.cap_annex",
                  capturePoint.getType().getText().formatted(Formatting.BOLD),
                  Text.translatable("text.nations.capture_point").formatted(Formatting.BOLD,capturePoint.getType().getTextColor()),
                  Text.literal(capturePoint.getChunkPos().toString()).formatted(Formatting.YELLOW,Formatting.BOLD),
                  newNation.getFormattedName().formatted(Formatting.BOLD)
            ).formatted(Formatting.DARK_AQUA);
            Nations.announce(announcement);
         }
      }
   }
   
   public void setClaimed(boolean claimed){
      this.claimed = claimed;
   }
   
   public void setInfluenced(boolean influenced){
      this.influenced = influenced;
   }
   
   public boolean isInfluenced(){
      return influenced;
   }
   
   public boolean isClaimed(){
      return claimed;
   }
   
   public boolean areExplosionsOverridden(){
      return explosionsOverridden;
   }
   
   public void setExplosionsOverridden(boolean explosionsOverridden){
      this.explosionsOverridden = explosionsOverridden;
   }
   
   public boolean isArena(){
      return arena;
   }
   
   public void setArena(boolean arena){
      this.arena = arena;
   }
   
   public boolean isAnchored(){
      return anchored;
   }
   
   public void setAnchored(boolean anchored){
      this.anchored = anchored;
   }
   
   public boolean hasMachinery(){
      return machinery;
   }
   
   public void setMachinery(boolean machinery){
      this.machinery = machinery;
   }
   
   public int getFarmlandLvl(){
      return farmlandLvl;
   }
   
   public void setFarmlandLvl(int farmlandLvl){
      this.farmlandLvl = farmlandLvl;
   }
   
   public double getCoinMultiplier(){
      return coinMultiplier;
   }
   
   public void setCoinMultiplier(double coinMultiplier){
      this.coinMultiplier = coinMultiplier;
   }
   
   public Nation getControllingNation(){
      if(controllingNationId == null) return null;
      return Nations.getNation(controllingNationId);
   }
   
   public NbtCompound getPosNbt(){
      NbtCompound pos = new NbtCompound();
      pos.putInt("x",location.x);
      pos.putInt("z",location.z);
      return pos;
   }
   
   public ChunkPos getPos(){
      return this.location;
   }
   
   @Override
   public boolean equals(Object obj){
      if(this == obj) return true;
      return obj instanceof NationChunk chunk && chunk.getPos().equals(this.getPos());
   }
   
   public NbtCompound saveToNbt(NbtCompound compound){
      compound.put("pos",getPosNbt());
      compound.putString("nation",controllingNationId == null ? "" : controllingNationId);
      compound.putString("capturePoint",capturePointId == null ? "" : capturePointId);
      compound.putBoolean("influenced",influenced);
      compound.putBoolean("claimed",claimed);
      compound.putBoolean("machinery",machinery);
      compound.putBoolean("anchored",anchored);
      compound.putBoolean("explosions",explosionsOverridden);
      compound.putBoolean("arena",arena);
      compound.putInt("farmlandLvl",farmlandLvl);
      compound.putDouble("coinMultiplier",coinMultiplier);
      return compound;
   }
   
   public static NationChunk loadFromNbt(NbtCompound compound){
      try{
         NbtCompound pos = compound.getCompound("pos");
         String nationId = compound.getString("nation");
         String capId = compound.getString("capturePoint");
         
         return new NationChunk(
               new ChunkPos(pos.getInt("x"), pos.getInt("z")),
               nationId.isEmpty() ? null : nationId,
               capId.isEmpty() ? null : capId,
               compound.getBoolean("influenced"),
               compound.getBoolean("claimed"),
               compound.getBoolean("machinery"),
               compound.getBoolean("anchored"),
               compound.getBoolean("explosions"),
               compound.getBoolean("arena"),
               compound.getInt("farmlandLvl"),
               compound.getDouble("coinMultiplier")
         );
      }catch(Exception e){
         log(3,e.toString());
         return null;
      }
   }
}
