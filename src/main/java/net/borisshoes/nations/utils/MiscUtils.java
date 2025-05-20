package net.borisshoes.nations.utils;

import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.borisshoes.nations.Nations;
import net.borisshoes.nations.gameplay.ResourceType;
import net.borisshoes.nations.items.GraphicalItem;
import net.borisshoes.nations.items.ResourceBullionItem;
import net.borisshoes.nations.items.ResourceCoinItem;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Pair;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector2i;

import java.util.*;

public class MiscUtils {
   
   private static final int CHUNK_SIZE = 16;
   
   public static void returnItems(Inventory inv, PlayerEntity player) {
      if (inv != null) {
         for(int i = 0; i < inv.size(); ++i) {
            ItemStack stack = inv.getStack(i).copy();
            if (!stack.isEmpty()) {
               inv.setStack(0, ItemStack.EMPTY);
               boolean bl = player.getInventory().insertStack(stack);
               ItemEntity itemEntity;
               if (bl && stack.isEmpty()) {
                  stack.setCount(1);
                  itemEntity = player.dropItem(stack, false);
                  if (itemEntity != null) {
                     itemEntity.setDespawnImmediately();
                  }
                  
                  player.currentScreenHandler.sendContentUpdates();
               } else {
                  itemEntity = player.dropItem(stack, false);
                  if (itemEntity != null) {
                     itemEntity.resetPickupDelay();
                     itemEntity.setOwner(player.getUuid());
                  }
               }
            }
         }
         
      }
   }
   
   public static <E extends Enum<E>> E cycleEnum(E current, boolean backwards) {
      E[] values = current.getDeclaringClass().getEnumConstants();
      int idx = current.ordinal();
      int shift = backwards ? -1 : 1;
      int nextIndex = (idx + shift + values.length) % values.length;
      return values[nextIndex];
   }
   
   public static boolean removeCoins(PlayerEntity player, ResourceType coinType, int amount) {
      if (player.isCreative()) return true;
      
      Item resourceCoinItem = coinType.getCoin();
      Item resourceBullionItem = coinType.getBullion();
      PlayerInventory inv = player.getInventory();
      
      int totalCoins   = 0;
      int totalBullions = 0;
      for (int i = 0; i < inv.size(); i++) {
         ItemStack stack = inv.getStack(i);
         if (stack.isEmpty()) continue;
         if (stack.getItem() instanceof ResourceCoinItem ci && ci.getType() == coinType) {
            totalCoins += stack.getCount();
         } else if (stack.getItem() instanceof ResourceBullionItem bi && bi.getType() == coinType) {
            totalBullions += stack.getCount();
         }
      }
      
      long totalValue = totalCoins + (long) totalBullions * 1000L;
      if (totalValue < amount) return false;
      int coinsToUse;
      int bullionsToUse;
      int leftoverChange = 0;
      
      if (totalCoins >= amount) {
         coinsToUse = amount;
         bullionsToUse = 0;
      } else {
         coinsToUse = totalCoins;
         long remaining = amount - totalCoins;
         bullionsToUse = (int) ((remaining + 999L) / 1000L);
         leftoverChange = (int) (bullionsToUse * 1000L - remaining);
      }
      
      Map<ItemStack, Integer> bullionRemovals = new LinkedHashMap<>();
      int needB = bullionsToUse;
      for (int i = 0; i < inv.size() && needB > 0; i++) {
         ItemStack stack = inv.getStack(i);
         if (stack.getItem() instanceof ResourceBullionItem bi && bi.getType() == coinType) {
            int take = Math.min(stack.getCount(), needB);
            bullionRemovals.put(stack, take);
            needB -= take;
         }
      }
      
      List<ItemStack> coinStacks = new ArrayList<>();
      for (int i = 0; i < inv.size(); i++) {
         ItemStack stack = inv.getStack(i);
         if (stack.getItem() instanceof ResourceCoinItem ci && ci.getType() == coinType) {
            coinStacks.add(stack);
         }
      }
      coinStacks.sort(Comparator.comparingInt(ItemStack::getCount));
      
      Map<ItemStack, Integer> coinRemovals = new LinkedHashMap<>();
      int needC = coinsToUse;
      for (ItemStack stack : coinStacks) {
         if (needC <= 0) break;
         int take = Math.min(stack.getCount(), needC);
         coinRemovals.put(stack, take);
         needC -= take;
      }
      
      for (var e : bullionRemovals.entrySet()) {
         ItemStack stack = e.getKey();
         int toRemove = e.getValue();
         if (stack.getCount() == toRemove) {
            inv.removeOne(stack);
         } else {
            stack.setCount(stack.getCount() - toRemove);
         }
      }
      for (var e : coinRemovals.entrySet()) {
         ItemStack stack = e.getKey();
         int toRemove = e.getValue();
         if (stack.getCount() == toRemove) {
            inv.removeOne(stack);
         } else {
            stack.setCount(stack.getCount() - toRemove);
         }
      }
      
      if (leftoverChange > 0) {
         int maxStack = resourceCoinItem.getMaxCount();
         List<ItemStack> changeStacks = new ArrayList<>();
         int left = leftoverChange;
         while (left > 0) {
            int give = Math.min(left, maxStack);
            changeStacks.add(new ItemStack(resourceCoinItem, give));
            left -= give;
         }
         MiscUtils.returnItems(new SimpleInventory(changeStacks.toArray(new ItemStack[0])), player);
      }
      
      return true;
   }
   
   public static boolean removeItems(PlayerEntity player, Item item, int count){
      if(player.isCreative()) return true;
      if(item instanceof ResourceCoinItem coinItem){
         return removeCoins(player,coinItem.getType(),count);
      }
      int remaining = count;
      PlayerInventory inv = player.getInventory();
      int[] slots = new int[inv.size()];
      for(int i = 0; i < inv.size() && remaining > 0; i++){
         ItemStack stack = inv.getStack(i);
         int stackCount = stack.getCount();
         if(stack.isOf(item)){
            if(remaining < stackCount){
               slots[i] = remaining;
               remaining = 0;
            }else{
               slots[i] = stackCount;
               remaining -= stackCount;
            }
         }
      }
      if(remaining > 0)return false;
      
      for(int i = 0; i < slots.length; i++){
         if(slots[i] <= 0) continue;
         inv.removeStack(i,slots[i]);
      }
      return true;
   }
   
   /**
    * Given a list of chunks (in chunk‐coordinates),
    * returns one or more closed, ordered loops of world‐space
    * Vector2i points tracing each boundary (outer + holes).
    */
   public static List<List<Vector2i>> getPerimeters(List<Vector2i> chunks) {
      // 1) Make a fast lookup of which chunks exist
      Set<Vector2i> chunkSet = new HashSet<>(chunks);
      
      // 2) Gather all boundary edges (undirected segments)
      Set<Edge> edges = new LinkedHashSet<>();
      for (Vector2i c : chunks) {
         int cx = c.x(), cz = c.y();
         // convert to world-space corner coords
         Vector2i bl = new Vector2i(cx*CHUNK_SIZE,     cz*CHUNK_SIZE);
         Vector2i br = new Vector2i((cx+1)*CHUNK_SIZE, cz*CHUNK_SIZE);
         Vector2i tr = new Vector2i((cx+1)*CHUNK_SIZE, (cz+1)*CHUNK_SIZE);
         Vector2i tl = new Vector2i(cx*CHUNK_SIZE,     (cz+1)*CHUNK_SIZE);
         
         // check each of the 4 neighbors in chunk‐space:
         if (!chunkSet.contains(new Vector2i(cx,     cz-1))) edges.add(new Edge(bl, br)); // south
         if (!chunkSet.contains(new Vector2i(cx+1,   cz  ))) edges.add(new Edge(br, tr)); // east
         if (!chunkSet.contains(new Vector2i(cx,     cz+1))) edges.add(new Edge(tr, tl)); // north
         if (!chunkSet.contains(new Vector2i(cx-1,   cz  ))) edges.add(new Edge(tl, bl)); // west
      }
      
      // 3) Stitch edges into closed loops
      List<List<Vector2i>> loops = new ArrayList<>();
      while (!edges.isEmpty()) {
         // start a new loop
         Iterator<Edge> it = edges.iterator();
         Edge startEdge = it.next();
         it.remove();
         
         List<Vector2i> loop = new ArrayList<>();
         Vector2i head = startEdge.a, tail = startEdge.b;
         loop.add(head);
         loop.add(tail);
         
         // walk until we close back on head
         while (!tail.equals(head)) {
            boolean foundNext = false;
            Iterator<Edge> iter2 = edges.iterator();
            while (iter2.hasNext()) {
               Edge e = iter2.next();
               if (e.a.equals(tail)) {
                  tail = e.b;
                  loop.add(tail);
                  iter2.remove();
                  foundNext = true;
                  break;
               } else if (e.b.equals(tail)) {
                  tail = e.a;
                  loop.add(tail);
                  iter2.remove();
                  foundNext = true;
                  break;
               }
            }
            if (!foundNext) {
               // Something’s wrong (shouldn’t happen on a fully connected boundary)
               break;
            }
         }
         
         loops.add(loop);
      }
      
      return loops;
   }
   
   /**
    * Simple undirected edge between two Vector2i endpoints.
    * equals()/hashCode() ignore orientation, so A–B == B–A.
    */
   private record Edge(Vector2i a, Vector2i b) {
      @Override
      public boolean equals(Object o){
         if(this == o) return true;
         if(!(o instanceof Edge e)) return false;
         return (a.equals(e.a) && b.equals(e.b))
               || (a.equals(e.b) && b.equals(e.a));
      }
      
      @Override
      public int hashCode(){
         // order‐independent
         return a.hashCode() ^ b.hashCode();
      }
   }
   
   /**
    * Returns one continuous polyline (outer boundary + 0-width gap into each hole + hole boundary)
    * suitable for a single call to your drawing API.
    */
   public static List<Vector2i> getOrderedPerimeter(List<Vector2i> chunks) {
      // --- 1) build all loops exactly as before ---
      List<List<Vector2i>> loops = getPerimeters(chunks);
      
      // --- 2) separate outer vs holes by signed area (outer is the one with largest absolute area) ---
      int outerIdx = 0;
      long maxArea2 = 0;
      for (int i = 0; i < loops.size(); i++) {
         long a2 = Math.abs(signedArea2(loops.get(i)));
         if (a2 > maxArea2) {
            maxArea2 = a2;
            outerIdx = i;
         }
      }
      List<Vector2i> outer = new ArrayList<>(loops.get(outerIdx));
      // remove that from the list of loops:
      loops.remove(outerIdx);
      
      // --- prepare to collect all the “bridges” ---
      class Bridge {
         int insertIndex;
         List<Vector2i> spoke;      // [p_outer, p_hole]
         List<Vector2i> holeLoop;   // rotated so it starts+ends at p_hole
      }
      List<Bridge> bridges = new ArrayList<>();
      
      // --- 3) for each hole, find its entry point and corresponding outer‐boundary point ---
      for (List<Vector2i> hole : loops) {
         // remove duplicate closure if present
         if (hole.getFirst().equals(hole.getLast())) {
            hole = new ArrayList<>(hole.subList(0, hole.size() - 1));
         }
         // pick a hole‐vertex (here: the first one)
         Vector2i pHole = hole.getFirst();
         
         // find the nearest intersection on the outer loop by trying vertical then horizontal casts
         Vector2i pOuter = findNearestIntersection(pHole, outer);
         
         // insert pOuter into outer loop if it isn’t already a vertex
         if (!outer.contains(pOuter)) {
            for (int i = 0; i < outer.size(); i++) {
               Vector2i a = outer.get(i), b = outer.get((i + 1) % outer.size());
               // check horizontal segment
               if (a.y() == b.y() && a.y() == pOuter.y()
                     && between(pOuter.x(), a.x(), b.x())) {
                  outer.add(i + 1, pOuter);
                  break;
               }
               // check vertical segment
               if (a.x() == b.x() && a.x() == pOuter.x()
                     && between(pOuter.y(), a.y(), b.y())) {
                  outer.add(i + 1, pOuter);
                  break;
               }
            }
         }
         int outIdx = outer.indexOf(pOuter);
         
         // build the spoke segment
         List<Vector2i> spoke = Arrays.asList(pOuter, pHole);
         
         // rotate the hole loop so it starts (and ends) at pHole
         int start = hole.indexOf(pHole);
         List<Vector2i> rotated = new ArrayList<>(hole.size() + 1);
         for (int i = 0; i < hole.size(); i++) {
            rotated.add(hole.get((start + i) % hole.size()));
         }
         // close the hole ring
         rotated.add(pHole);
         
         // record this bridge
         Bridge b = new Bridge();
         b.insertIndex = outIdx;
         b.spoke       = spoke;
         b.holeLoop    = rotated;
         bridges.add(b);
      }
      
      // --- 4) sort bridges by where they go into the outer loop so splicing is easy ---
      bridges.sort(Comparator.comparingInt(b -> b.insertIndex));
      
      // --- 5) build the final single path ---
      List<Vector2i> result = new ArrayList<>();
      for (int i = 0; i < outer.size(); i++) {
         result.add(outer.get(i));
         for (Bridge b : bridges) {
            if (b.insertIndex == i) {
               // dive in
               //  - draw spoke down: skip the first point (already added), draw the rest
               for (int s = 1; s < b.spoke.size(); s++) {
                  result.add(b.spoke.get(s));
               }
               //  - trace the hole
               result.addAll(b.holeLoop);
               //  - return along the same spoke (reverse order)
               List<Vector2i> rev = new ArrayList<>(b.spoke);
               Collections.reverse(rev);
               result.addAll(rev);
            }
         }
      }
      
      return result;
   }
   
   // --------------------------------------------------------
   // helper: signed doubled‐area of a polygon (shoelace)
   private static long signedArea2(List<Vector2i> poly) {
      long sum = 0;
      for (int i = 0; i < poly.size(); i++) {
         Vector2i a = poly.get(i), b = poly.get((i + 1) % poly.size());
         sum += (long)a.x() * b.y() - (long)b.x() * a.y();
      }
      return sum;
   }
   
   // helper: point‐in‐range test
   private static boolean between(int v, int a, int b) {
      return (v >= Math.min(a,b)) && (v <= Math.max(a,b));
   }
   
   // --------------------------------------------------------
   // Try an axis–aligned “ray” from p into outer: down, up, right, left
   // Return the first intersection point on the outer loop.
   private static Vector2i findNearestIntersection(Vector2i p, List<Vector2i> outer) {
      Vector2i best = null;
      int bestDist = Integer.MAX_VALUE;
      
      // vertical casts
      for (Vector2i dirTest : new Vector2i[]{ new Vector2i(0,-1), new Vector2i(0,1) }) {
         for (int i = 0; i < outer.size(); i++) {
            Vector2i a = outer.get(i), b = outer.get((i+1)%outer.size());
            if (a.y()==b.y() && p.x()==p.x()           // horizontal edge
                  && between(p.x(), a.x(), b.x())) {
               int yEdge = a.y();
               int delta = (dirTest.y()<0) ? p.y()-yEdge : yEdge-p.y();
               if (delta>0 && delta<bestDist) {
                  bestDist = delta;
                  best = new Vector2i(p.x(), yEdge);
               }
            }
         }
         if (best!=null) return best;
      }
      
      // horizontal casts
      for (Vector2i dirTest : new Vector2i[]{ new Vector2i(1,0), new Vector2i(-1,0) }) {
         for (int i = 0; i < outer.size(); i++) {
            Vector2i a = outer.get(i), b = outer.get((i+1)%outer.size());
            if (a.x()==b.x() && p.y()==p.y()           // vertical edge
                  && between(p.y(), a.y(), b.y())) {
               int xEdge = a.x();
               int delta = (dirTest.x()>0) ? xEdge-p.x() : p.x()-xEdge;
               if (delta>0 && delta<bestDist) {
                  bestDist = delta;
                  best = new Vector2i(xEdge, p.y());
               }
            }
         }
         if (best!=null) return best;
      }
      
      throw new IllegalStateException("Could not find bridge into hole at " + p);
   }
   
   public static List<List<ChunkPos>> getConnectedSections(List<ChunkPos> chunks){
      List<List<ChunkPos>> sections = new ArrayList<>();
      if (chunks == null || chunks.isEmpty()) return sections;
      
      Set<ChunkPos> chunkSet = new HashSet<>(chunks);
      Set<ChunkPos> visited  = new HashSet<>();
      
      int[][] dirs = { {1, 0}, {-1, 0}, {0, 1}, {0, -1} };
      
      for (ChunkPos start : chunks) {
         if (visited.contains(start)) continue;
         
         List<ChunkPos> component = new ArrayList<>();
         Queue<ChunkPos> q     = new LinkedList<>();
         q.add(start);
         visited.add(start);
         
         while (!q.isEmpty()) {
            ChunkPos curr = q.poll();
            component.add(curr);
            
            for (int[] d : dirs) {
               ChunkPos neigh = new ChunkPos(curr.x + d[0], curr.z + d[1]);
               if (chunkSet.contains(neigh) && !visited.contains(neigh)) {
                  visited.add(neigh);
                  q.add(neigh);
               }
            }
         }
         sections.add(component);
      }
      return sections;
   }
   
   public static Vec3d randomSpherePoint(Vec3d center, double range){
      Random random = new Random();
      double x = random.nextGaussian();
      double y = random.nextGaussian();
      double z = random.nextGaussian();
      
      double mag = Math.sqrt(x*x + y*y + z*z);
      x /= mag; y /= mag; z /= mag;
      
      double r = range* Math.cbrt(random.nextDouble());
      
      return new Vec3d(x*r,y*r,z*r).add(center);
   }
   
   public static Vec3d randomSpherePoint(Vec3d center, double maxRange, double minRange){
      Random random = new Random();
      double x = random.nextGaussian();
      double y = random.nextGaussian();
      double z = random.nextGaussian();
      
      double mag = Math.sqrt(x*x + y*y + z*z);
      x /= mag; y /= mag; z /= mag;
      
      double r = maxRange*Math.cbrt(random.nextDouble(minRange / maxRange,1));
      
      return new Vec3d(x*r,y*r,z*r).add(center);
   }
   
   public static UUID getUUID(String str){
      try{
         return UUID.fromString(str);
      }catch(Exception e){
         return UUID.fromString(Nations.BLANK_UUID);
      }
   }
   
   public static RegistryEntry<Enchantment> getEnchantment(RegistryKey<Enchantment> key){
      if(Nations.SERVER == null){
         Nations.log(2,"Attempted to access Enchantment "+key.toString()+" before DRM is available");
         return null;
      }
      Optional<RegistryEntry.Reference<Enchantment>> opt = Nations.SERVER.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT).getOptional(key);
      return opt.orElse(null);
   }
   
   public static <T> List<T> listToPage(List<T> items, int page, int pageSize){
      if(page <= 0){
         return items;
      }else if(pageSize*(page-1) >= items.size()){
         return new ArrayList<>();
      }else{
         return items.subList(pageSize*(page-1), Math.min(items.size(), pageSize*page));
      }
   }
   
   public static MutableText getTimeDiff(long timeDiff){
      timeDiff /= 1000;
      long subtract = timeDiff;
      long daysDif = subtract / 86400;
      subtract -= daysDif * 86400;
      long hoursDif = subtract / 3600;
      subtract -= hoursDif * 3600;
      long minutesDif = subtract / 60;
      subtract -= minutesDif * 60;
      long secondsDiff = subtract;
      
      MutableText text = Text.literal("");
      boolean needSpace = false;
      if(daysDif > 0){
         text.append(Text.literal(daysDif+" "));
         text.append(Text.translatable("text.nations.days"));
         needSpace = true;
      }
      if(hoursDif > 0){
         if(needSpace) text.append(Text.literal(" "));
         text.append(Text.literal(hoursDif+" "));
         text.append(Text.translatable("text.nations.hours"));
         needSpace = true;
      }
      if(minutesDif > 0){
         if(needSpace) text.append(Text.literal(" "));
         text.append(Text.literal(minutesDif+" "));
         text.append(Text.translatable("text.nations.minutes"));
         needSpace = true;
      }
      if(secondsDiff > 0){
         if(needSpace) text.append(Text.literal(" "));
         text.append(Text.literal(secondsDiff+" "));
         text.append(Text.translatable("text.nations.seconds"));
      }
      return text;
   }
}
