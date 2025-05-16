package net.borisshoes.nations.utils;

import net.borisshoes.nations.callbacks.TickTimerCallback;

public class GenericTimer extends TickTimerCallback {
   private final Runnable task;
   
   public GenericTimer(int time, Runnable task){
      super(time,null,null);
      this.task = task;
   }
   
   @Override
   public void onTimer(){
      task.run();
   }
}
