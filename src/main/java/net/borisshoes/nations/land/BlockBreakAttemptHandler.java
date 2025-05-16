package net.borisshoes.nations.land;

import net.minecraft.util.math.BlockPos;

public interface BlockBreakAttemptHandler {
   void setBlockBreakAttemptFail(BlockPos pos, boolean instaBreak);
   
   BlockPos failedPos();
   
   boolean wasInstabreak();
}
