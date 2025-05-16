package net.borisshoes.nations.utils;

import net.borisshoes.arcananovum.ArcanaNovum;
import net.borisshoes.nations.Nations;
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class ParticleEffectUtils {
   
   public static void netherRiftTeleport(ServerWorld world, Vec3d pos, int tick) {
      int animLength = 30;
      if (tick < 5) {
         world.spawnParticles(ParticleTypes.REVERSE_PORTAL, pos.x, pos.y + 0.5, pos.z, 30, 0.1, 0.4, 0.1, 0.2);
         world.spawnParticles(ParticleTypes.FALLING_OBSIDIAN_TEAR, pos.x, pos.y + 0.5, pos.z, 10, 0.6, 0.6, 0.6, 0.2);
      }
      
      circle(world, null, pos.subtract(0.0, 0.5, 0.0), ParticleTypes.WITCH, 1.0, 20, 1, 0.1, 0.0);
      if (tick < animLength) {
         Nations.addTickTimerCallback(world, new GenericTimer(1, () -> {
            netherRiftTeleport(world, pos, tick + 1);
         }));
      }
      
   }
   
   public static void netherRiftSpawning(ServerWorld world, BlockPos blockPos){
      Vec3d pos = blockPos.toCenterPos().add(0,2,0);
      spawnLongParticle(world,ParticleTypes.WITCH,pos.getX(),pos.getY(),pos.getZ(),0.25,1.25,0.25,0,2);
      spawnLongParticle(world,ParticleTypes.PORTAL,pos.getX(),pos.getY(),pos.getZ(),0.25,1.25,0.25,0.2,3);
   }
   
   public static void netherRift(ServerWorld world, BlockPos blockPos){
      Vec3d pos = blockPos.toCenterPos().add(0,2,0);
      spawnLongParticle(world,ParticleTypes.WITCH,pos.getX(),pos.getY(),pos.getZ(),0.2,1.25,0.2,0,5);
      spawnLongParticle(world,ParticleTypes.PORTAL,pos.getX(),pos.getY(),pos.getZ(),0.5,1.25,0.5,1,5);
      world.spawnParticles(ParticleTypes.WITCH,pos.getX(),pos.getY(),pos.getZ(),10,0.2,1.25,0.2,0);
      world.spawnParticles(ParticleTypes.DRIPPING_OBSIDIAN_TEAR,pos.getX(),pos.getY()+1,pos.getZ(),2,0.4,1.25,0.4,0);
      world.spawnParticles(ParticleTypes.PORTAL,pos.getX(),pos.getY(),pos.getZ(),10,0.5,1.25,0.5,1);
      world.spawnParticles(ParticleTypes.PORTAL,pos.getX(),pos.getY(),pos.getZ(),30,2,0.1,2,0.3);
   }
   
   public static void worldBorder(ServerWorld world, ServerPlayerEntity player, int border, double showDist){
      ParticleEffect type = world.getRegistryKey().equals(ServerWorld.NETHER) ? ParticleTypes.TRIAL_SPAWNER_DETECTION_OMINOUS : ParticleTypes.TRIAL_SPAWNER_DETECTION;
      for(Direction dir : Direction.values()){
         if(dir.getAxis() == Direction.Axis.Y) continue;
         double playerVal = player.getPos().multiply(dir.getDoubleVector()).length();
         
         if(playerVal > border - showDist){
            final double bigSpacing = 1;
            final double bigRadius = 40;
            
            for(double h = -bigRadius; h < bigRadius; h += bigSpacing){
               for(double v = -bigRadius; v < bigRadius; v += bigSpacing){
                  Vec3d particlePos;
                  if(dir.getAxis() == Direction.Axis.X){
                     particlePos = new Vec3d(0,player.getY()+v,player.getZ()+h);
                  }else{
                     particlePos = new Vec3d(player.getX()+h,player.getY()+v,0);
                  }
                  particlePos = particlePos.add(dir.getDoubleVector().multiply(border));
                  if(Math.abs(particlePos.x) > border || Math.abs(particlePos.z) > border) continue;
                  spawnLongParticle(world, type, particlePos.x, particlePos.y, particlePos.z, 0, 0, 0, 0.01, 1);
               }
            }
         }
         if(playerVal > border - 0.5*showDist){
            final double smallSpacing = 0.5;
            final double smallRadius = 10;
            
            for(double h = -smallRadius; h < smallRadius; h += smallSpacing){
               for(double v = -smallRadius; v < smallRadius; v += smallSpacing){
                  Vec3d particlePos;
                  if(dir.getAxis() == Direction.Axis.X){
                     particlePos = new Vec3d(0,player.getY()+v,player.getZ()+h);
                  }else{
                     particlePos = new Vec3d(player.getX()+h,player.getY()+v,0);
                  }
                  particlePos = particlePos.add(dir.getDoubleVector().multiply(border));
                  if(Math.abs(particlePos.x) > border || Math.abs(particlePos.z) > border) continue;
                  spawnLongParticle(world, type, particlePos.x, particlePos.y, particlePos.z, 0, 0, 0, 0.01, 1);
               }
            }
         }
      }
   }
   
   
   public static void lightningBolt(ServerWorld world, Vec3d p1, Vec3d p2, int numSegments, double maxDevDist, ParticleEffect type, int particlesPerBlock, int count, double delta, double speed, boolean longDist){
      if(numSegments <= 0) return;
      List<Vec3d> points = new ArrayList<>();
      points.add(p1);
      double dx = (p2.x-p1.x)/numSegments;
      double dy = (p2.y-p1.y)/numSegments;
      double dz = (p2.z-p1.z)/numSegments;
      for(int i = 0; i < numSegments-1; i++){
         double x = p1.x + dx*i;
         double y = p1.y + dy*i;
         double z = p1.z + dz*i;
         points.add(MiscUtils.randomSpherePoint(new Vec3d(x,y,z),maxDevDist));
      }
      points.add(p2);
      
      for(int i = 1; i < points.size(); i++){
         Vec3d ps = points.get(i-1);
         Vec3d pe = points.get(i);
         int intervals = (int) (pe.subtract(ps).length() * particlesPerBlock);
         
         if(longDist){
            line(world,null,ps,pe,type,intervals,count,delta,speed);
         }else{
            longDistLine(world,ps,pe,type,intervals,count,delta,speed);
         }
      }
   }
   
   public static void trackedAnimatedLightningBolt(ServerWorld world, Supplier<Vec3d> s1, Supplier<Vec3d> s2, int numSegments, double maxDevDist, ParticleEffect type, int particlesPerBlock, int count, double delta, double speed, boolean longDist, int persistMod, int duration){
      if(numSegments <= 0) return;
      List<Vec3d> points = new ArrayList<>();
      Vec3d p1 = s1.get();
      Vec3d p2 = s2.get();
      points.add(p1);
      double dx = (p2.x-p1.x)/numSegments;
      double dy = (p2.y-p1.y)/numSegments;
      double dz = (p2.z-p1.z)/numSegments;
      for(int i = 0; i < numSegments-1; i++){
         double x = p1.x + dx*i;
         double y = p1.y + dy*i;
         double z = p1.z + dz*i;
         points.add(MiscUtils.randomSpherePoint(new Vec3d(x,y,z),maxDevDist));
      }
      points.add(p2);
      
      int particleCount = 0;
      for(int i = 1; i < points.size(); i++){
         Vec3d ps = points.get(i-1);
         Vec3d pe = points.get(i);
         int intervals = (int) (pe.subtract(ps).length() * particlesPerBlock);
         
         particleCount += intervals;
      }
      
      float particlesPerTick = (float) particleCount / duration;
      HashMap<Supplier<Vec3d>, Integer> pp = new HashMap<>();
      
      int c = 0;
      for(int i = 1; i < points.size(); i++){
         Vec3d ps = points.get(i-1);
         Vec3d pe = points.get(i);
         int intervals = (int) (pe.subtract(ps).length() * particlesPerBlock);
         
         dx = (pe.x-ps.x)/intervals;
         dy = (pe.y-ps.y)/intervals;
         dz = (pe.z-ps.z)/intervals;
         for(int j = 0; j < intervals; j++){
            final double x = ps.x + dx * j;
            final double y = ps.y + dy * j;
            final double z = ps.z + dz * j;
            
            pp.put(() -> {
               Vec3d basis = p2.subtract(p1);
               Vec3d newBasis = s2.get().subtract(s1.get());
               double magDiff = newBasis.length() / basis.length();
               Quaternionf transform = new Quaternionf().rotationTo(basis.toVector3f(), newBasis.toVector3f());
               Vec3d deltaV = new Vec3d(x,y,z).subtract(p1);
               Vec3d newDeltaV = new Vec3d(transform.transform(deltaV.toVector3f()));
               Vec3d normalizedBasis = newBasis.normalize();
               double projectionMagnitude = newDeltaV.dotProduct(normalizedBasis);
               newDeltaV = normalizedBasis.multiply(projectionMagnitude * magDiff).add(newDeltaV.subtract(normalizedBasis.multiply(projectionMagnitude)));
               return newDeltaV.add(s1.get());
            },Math.round(c / particlesPerTick));
            c++;
         }
      }
      
      animatedLightningBoltHelper(world,pp,type,count,delta,speed,longDist,persistMod,0);
   }
   
   public static void animatedLightningBolt(ServerWorld world, Vec3d p1, Vec3d p2, int numSegments, double maxDevDist, ParticleEffect type, int particlesPerBlock, int count, double delta, double speed, boolean longDist, int persistMod, int duration){
      if(numSegments <= 0) return;
      List<Vec3d> points = new ArrayList<>();
      points.add(p1);
      double dx = (p2.x-p1.x)/numSegments;
      double dy = (p2.y-p1.y)/numSegments;
      double dz = (p2.z-p1.z)/numSegments;
      for(int i = 0; i < numSegments-1; i++){
         double x = p1.x + dx*i;
         double y = p1.y + dy*i;
         double z = p1.z + dz*i;
         points.add(MiscUtils.randomSpherePoint(new Vec3d(x,y,z),maxDevDist));
      }
      points.add(p2);
      
      int particleCount = 0;
      for(int i = 1; i < points.size(); i++){
         Vec3d ps = points.get(i-1);
         Vec3d pe = points.get(i);
         int intervals = (int) (pe.subtract(ps).length() * particlesPerBlock);
         
         particleCount += intervals;
      }
      
      float particlesPerTick = (float) particleCount / duration;
      HashMap<Supplier<Vec3d>, Integer> pp = new HashMap<>();
      
      int c = 0;
      for(int i = 1; i < points.size(); i++){
         Vec3d ps = points.get(i-1);
         Vec3d pe = points.get(i);
         int intervals = (int) (pe.subtract(ps).length() * particlesPerBlock);
         
         dx = (pe.x-ps.x)/intervals;
         dy = (pe.y-ps.y)/intervals;
         dz = (pe.z-ps.z)/intervals;
         for(int j = 0; j < intervals; j++){
            double x = ps.x + dx * j;
            double y = ps.y + dy * j;
            double z = ps.z + dz * j;
            
            pp.put(() -> new Vec3d(x,y,z),Math.round(c / particlesPerTick));
            c++;
         }
      }
      
      animatedLightningBoltHelper(world,pp,type,count,delta,speed,longDist,persistMod,0);
   }
   
   private static void animatedLightningBoltHelper(ServerWorld world, HashMap<Supplier<Vec3d>, Integer> points, ParticleEffect type, int count, double delta, double speed, boolean longDist, int persistMod, int tick){
      int highestTick = 0;
      for(Map.Entry<Supplier<Vec3d>, Integer> entry : points.entrySet()){
         int pTick = entry.getValue();
         Vec3d point = entry.getKey().get();
         if(pTick > highestTick) highestTick = pTick;
         
         if(!(persistMod > 0 && tick % persistMod == 0 && pTick < tick) && pTick != tick) continue;
         
         if(longDist){
            spawnLongParticle(world,type,point.x,point.y,point.z,delta,delta,delta,speed,count);
         }else{
            world.spawnParticles(type,point.x,point.y,point.z,count,delta,delta,delta,speed);
         }
      }
      
      if(tick < highestTick){
         Nations.addTickTimerCallback(world, new GenericTimer(1, () -> animatedLightningBoltHelper(world, points, type, count, delta, speed, longDist, persistMod, tick+1)));
      }
   }
   
   public static void longDistLine(ServerWorld world, Vec3d p1, Vec3d p2, ParticleEffect type, int intervals, int count, double delta, double speed){
      double dx = (p2.x-p1.x)/intervals;
      double dy = (p2.y-p1.y)/intervals;
      double dz = (p2.z-p1.z)/intervals;
      for(int i = 0; i < intervals; i++){
         double x = p1.x + dx*i;
         double y = p1.y + dy*i;
         double z = p1.z + dz*i;
         
         spawnLongParticle(world,type,x,y,z,delta,delta,delta,speed,count);
      }
   }
   
   public static void line(ServerWorld world, @Nullable ServerPlayerEntity player, Vec3d p1, Vec3d p2, ParticleEffect type, int intervals, int count, double delta, double speed){
      line(world, player, p1, p2, type, intervals, count, delta, speed,1);
   }
   
   public static void line(ServerWorld world, @Nullable ServerPlayerEntity player, Vec3d p1, Vec3d p2, ParticleEffect type, int intervals, int count, double delta, double speed, double percent){
      percent = MathHelper.clamp(percent,0,1);
      double dx = (p2.x-p1.x)/intervals;
      double dy = (p2.y-p1.y)/intervals;
      double dz = (p2.z-p1.z)/intervals;
      for(int i = 0; i < intervals; i++){
         if((double)i/intervals > percent && percent != 1) continue;
         double x = p1.x + dx*i;
         double y = p1.y + dy*i;
         double z = p1.z + dz*i;
         
         if(player == null){
            world.spawnParticles(type,x,y,z,count,delta,delta,delta,speed);
         }else{
            world.spawnParticles(player,type,false,true,x,y,z,count,delta,delta,delta,speed);
         }
      }
   }
   
   public static void longDistCircle(ServerWorld world, Vec3d center, ParticleEffect type, double radius, int intervals, int count, double delta, double speed){
      double dA = Math.PI * 2 / intervals;
      for(int i = 0; i < intervals; i++){
         double angle = dA * i;
         double x = radius * Math.cos(angle) + center.x;
         double z = radius * Math.sin(angle) + center.z;
         double y = center.y;
         
         spawnLongParticle(world,type,x,y,z,delta,delta,delta,speed,count);
      }
   }
   
   public static void circle(ServerWorld world, @Nullable ServerPlayerEntity player, Vec3d center, ParticleEffect type, double radius, int intervals, int count, double delta, double speed){
      circle(world,player,center,type,radius,intervals,count,delta,speed,0);
   }
   
   public static void circle(ServerWorld world, @Nullable ServerPlayerEntity player, Vec3d center, ParticleEffect type, double radius, int intervals, int count, double delta, double speed, double theta){
      double dA = Math.PI * 2 / intervals;
      for(int i = 0; i < intervals; i++){
         double angle = dA * i + theta;
         double x = radius * Math.cos(angle) + center.x;
         double z = radius * Math.sin(angle) + center.z;
         double y = center.y;
         
         if(player == null){
            world.spawnParticles(type,x,y,z,count,delta,delta,delta,speed);
         }else{
            world.spawnParticles(player,type,false,true,x,y,z,count,delta,delta,delta,speed);
         }
      }
   }
   
   public static List<Vec3d> getCirclePoints(Vec3d center, double radius, int intervals, double theta){
      List<Vec3d> points = new ArrayList<>();
      double dA = Math.PI * 2 / intervals;
      for(int i = 0; i < intervals; i++){
         double angle = dA * i + theta;
         double x = radius * Math.cos(angle) + center.x;
         double z = radius * Math.sin(angle) + center.z;
         double y = center.y;
         points.add(new Vec3d(x,y,z));
      }
      return points;
   }
   
   public static void longDistSphere(ServerWorld world, Vec3d center, ParticleEffect type, double radius, int points, int count, double delta, double speed, double theta){
      double phi = Math.PI * (3 - Math.sqrt(5));
      
      for(int i = 0; i < points; i++){
         // Fibonacci Sphere Equations
         double y = 1 - (i / (double)(points-1)) * 2;
         double r = Math.sqrt(1-y*y);
         double t = phi*i + theta;
         double x = Math.cos(t) * r;
         double z = Math.sin(t) * r;
         
         // Center Offset and Radius Scale
         Vec3d point = new Vec3d(x,y,z);
         point = point.multiply(radius).add(center.x, center.y, center.z);
         
         spawnLongParticle(world,type,point.x,point.y,point.z,delta,delta,delta,speed,count);
      }
   }
   
   public static void sphere(ServerWorld world, @Nullable ServerPlayerEntity player, Vec3d center, ParticleEffect type, double radius, int points, int count, double delta, double speed, double theta){
      double phi = Math.PI * (3 - Math.sqrt(5));
      
      for(int i = 0; i < points; i++){
         // Fibonacci Sphere Equations
         double y = 1 - (i / (double)(points-1)) * 2;
         double r = Math.sqrt(1-y*y);
         double t = phi*i + theta;
         double x = Math.cos(t) * r;
         double z = Math.sin(t) * r;
         
         // Center Offset and Radius Scale
         Vec3d point = new Vec3d(x,y,z);
         point = point.multiply(radius).add(center.x, center.y, center.z);
         
         if(player == null){
            world.spawnParticles(type,point.x,point.y,point.z,count,delta,delta,delta,speed);
         }else{
            world.spawnParticles(player,type,false,true,point.x,point.y,point.z,count,delta,delta,delta,speed);
         }
      }
   }
   // Notes about the Dust Particle, size goes from .01 to 4, you can use an int represented rgb value with new Vector3f(Vec3d.unpackRgb(int))
   
   public static void spawnLongParticle(ServerWorld world, ParticleEffect type, double x, double y, double z, double dx, double dy, double dz, double speed, int count){
      List<ServerPlayerEntity> players = world.getPlayers(player -> player.squaredDistanceTo(new Vec3d(x,y,z)) < 512*512);
      for(ServerPlayerEntity player : players){
         player.networkHandler.sendPacket(new ParticleS2CPacket(type,true,true,x,y,z,(float)dx,(float)dy,(float)dz,(float)speed,count));
      }
   }
   
   public static int adjustTime(int tick, double speedMod){
      return (int) (((int)(tick / speedMod)) * speedMod);
   }
}
