package net.borisshoes.nations.cca;

import net.minecraft.util.Identifier;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistryV3;
import org.ladysnake.cca.api.v3.entity.EntityComponentFactoryRegistry;
import org.ladysnake.cca.api.v3.entity.EntityComponentInitializer;
import org.ladysnake.cca.api.v3.entity.RespawnCopyStrategy;

import static net.borisshoes.nations.Nations.MOD_ID;

public class PlayerComponentInitializer implements EntityComponentInitializer {
   public static final ComponentKey<INationsProfileComponent> PLAYER_DATA = ComponentRegistryV3.INSTANCE.getOrCreate(Identifier.of(MOD_ID, "profile"), INationsProfileComponent.class);
   
   @Override
   public void registerEntityComponentFactories(EntityComponentFactoryRegistry registry){
      registry.registerForPlayers(PLAYER_DATA, NationsProfileComponent::new, RespawnCopyStrategy.ALWAYS_COPY);
   }
}
