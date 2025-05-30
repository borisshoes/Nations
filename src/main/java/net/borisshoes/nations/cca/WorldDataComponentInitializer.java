package net.borisshoes.nations.cca;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistryV3;
import org.ladysnake.cca.api.v3.world.WorldComponentFactoryRegistry;
import org.ladysnake.cca.api.v3.world.WorldComponentInitializer;

import static net.borisshoes.nations.Nations.MOD_ID;

public class WorldDataComponentInitializer implements WorldComponentInitializer {
   public static final ComponentKey<INationsDataComponent> NATIONS_DATA = ComponentRegistryV3.INSTANCE.getOrCreate(Identifier.of(MOD_ID, "nation_data"), INationsDataComponent.class);
   
   @Override
   public void registerWorldComponentFactories(WorldComponentFactoryRegistry registry){
      registry.registerFor(ServerWorld.OVERWORLD, NATIONS_DATA, NationsDataComponent.class, world -> new NationsDataComponent());
   }
}
