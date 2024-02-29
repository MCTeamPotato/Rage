package com.teampotato.rage;

import com.teampotato.rage.api.RageHolder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import org.jetbrains.annotations.NotNull;

@Mod(Rage.MOD_ID)
public class Rage {
    public static final String MOD_ID = "rage";

    public Rage() {
        IEventBus forgeBus = MinecraftForge.EVENT_BUS;
        forgeBus.addListener(this::bumpOrUseRageOnAttack);
        forgeBus.addListener(this::bumpRageOnHurt);
        forgeBus.addListener(this::showParticleOnFullRage);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, CONFIG);
    }

    public static final ForgeConfigSpec CONFIG;
    public static final ForgeConfigSpec.BooleanValue NOTIFY_PLAYER_ON_RAGE_CHANGE, SHOW_PARTICLE_ON_FULL_RAGE;
    public static final ForgeConfigSpec.ConfigValue<Double> MAX_DAMAGE_BONUS, BASIC_DAMAGE_BONUS;
    public static final ForgeConfigSpec.IntValue FULL_RAGE_VALUE, GAINED_RAGE_PER_HURT_OR_ATTACK;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("Rage");
        SHOW_PARTICLE_ON_FULL_RAGE = builder.define("ShowParticleOnFullRage", true);
        NOTIFY_PLAYER_ON_RAGE_CHANGE = builder.define("NotifyPlayerOnRageChange", false);
        BASIC_DAMAGE_BONUS = builder.defineInRange("BasicDamageBonus", 3.0, 0.0, Double.MAX_VALUE);
        MAX_DAMAGE_BONUS = builder.defineInRange("MaxDamageBonus", 5.0, 0.0, Double.MAX_VALUE);
        FULL_RAGE_VALUE = builder.defineInRange("FullRageValue", 150, 0, Integer.MAX_VALUE);
        GAINED_RAGE_PER_HURT_OR_ATTACK = builder.defineInRange("GainedRagePerHurtOrAttack", 50, 0, Integer.MAX_VALUE);
        builder.pop();
        CONFIG = builder.build();
    }

    private void showParticleOnFullRage(TickEvent.PlayerTickEvent event) {
        if (!SHOW_PARTICLE_ON_FULL_RAGE.get()) return;
        if (event.phase != TickEvent.Phase.START) return;
        Player player = event.player;
        if (((RageHolder)player).rage$isFullRage() && player instanceof ServerPlayer) {
            ((ServerPlayer) player).getLevel().sendParticles(ParticleTypes.CRIT, player.getX(), player.getY(), player.getZ(), 8, 0.2, 0.2, 0.2, 0.0);
        }
    }

    private void bumpRageOnHurt(LivingHurtEvent event) {
        LivingEntity entity = event.getEntityLiving();
        if (entity.level.isClientSide()) return;
        if (shouldBumpRage(event.getSource())) ((RageHolder)entity).rage$bumpRage();
    }

    private static boolean shouldBumpRage(@NotNull DamageSource damageSource) {
        Entity entity = damageSource.getEntity();
        Entity directEntity = damageSource.getDirectEntity();
        return entity instanceof LivingEntity || directEntity instanceof LivingEntity;
    }

    private void bumpOrUseRageOnAttack(LivingHurtEvent event) {
        if (event.getEntity().level.isClientSide()) return;
        DamageSource damageSource = event.getSource();
        Entity entity = damageSource.getEntity();
        Entity directEntity = damageSource.getDirectEntity();
        processRageOnAttack(entity, event);
        if (directEntity != null && directEntity.equals(entity)) return;
        processRageOnAttack(directEntity, event);
    }

    private static void processRageOnAttack(Entity entity, LivingHurtEvent event) {
        if (entity instanceof LivingEntity) {
            if (((RageHolder)entity).rage$isFullRage()) {
                event.setAmount((float) (((RageHolder)entity).rage$getDamageBonus() * (double) event.getAmount()));
                ((RageHolder) entity).rage$clearRage();
            } else {
                ((RageHolder)entity).rage$bumpRage();
            }
        }
    }
}
