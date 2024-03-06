package com.teampotato.rage;

import com.teampotato.rage.api.RageHolder;
import com.teampotato.rage.api.event.FullRageAttackEvent;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("resource")
@Mod(Rage.MOD_ID)
public class Rage {
    public static final String MOD_ID = "rage";

    public Rage() {
        IEventBus forgeBus = MinecraftForge.EVENT_BUS;
        forgeBus.addListener(this::bumpOrUseRageOnAttack);
        forgeBus.addListener(this::bumpRageOnHurt);
        forgeBus.addListener(this::showParticleOnFullRageLiving);
        forgeBus.addListener(this::showParticleOnFullRagePlayer);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, CONFIG);
    }

    public static final ForgeConfigSpec CONFIG;
    public static final ForgeConfigSpec.BooleanValue NOTIFY_PLAYER_ON_RAGE_CHANGE, ONLY_PLAYERS_HAVCE_RAGE, SHOW_PARTICLE_ON_FULL_RAGE, PLAY_DING_ON_FULL_RAGE_ATTACK, NOTIFY_PLAYER_ON_REACHING_FULL_RAGE;
    public static final ForgeConfigSpec.DoubleValue MAX_DAMAGE_BONUS, BASIC_DAMAGE_BONUS, DING_VOLUME, DING_PITCH;
    public static final ForgeConfigSpec.IntValue FULL_RAGE_VALUE, GAINED_RAGE_PER_HURT_OR_ATTACK, DECREASE_INTERVAL_TICKS, RAGE_THAT_ENTITY_LOSES_EVERY_INTERVAL;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("Rage");
        BASIC_DAMAGE_BONUS = builder.defineInRange("BasicDamageBonus", 3.0, 0.0, Double.MAX_VALUE);
        MAX_DAMAGE_BONUS = builder.defineInRange("MaxDamageBonus", 5.0, 0.0, Double.MAX_VALUE);
        FULL_RAGE_VALUE = builder.defineInRange("FullRageValue", 150, 0, Integer.MAX_VALUE);
        GAINED_RAGE_PER_HURT_OR_ATTACK = builder.defineInRange("GainedRagePerHurtOrAttack", 50, 0, Integer.MAX_VALUE);
        ONLY_PLAYERS_HAVCE_RAGE = builder.define("OnlyPlayersHaveRage", false);
        builder.push("Notify");
        SHOW_PARTICLE_ON_FULL_RAGE = builder.define("ShowParticleOnFullRage", true);
        NOTIFY_PLAYER_ON_RAGE_CHANGE = builder.define("NotifyPlayerOnRageChange", false);
        NOTIFY_PLAYER_ON_REACHING_FULL_RAGE = builder.define("NotifyPlayerOnFullRage", true);
        builder.pop();
        builder.push("Sound");
        PLAY_DING_ON_FULL_RAGE_ATTACK = builder.define("PlayDingOnFullRageAttack", true);
        DING_VOLUME = builder.defineInRange("DingVolume", 1.00, 0.00, Double.MAX_VALUE);
        DING_PITCH = builder.defineInRange("DingPitch", 1.00, 0.00, Double.MAX_VALUE);
        builder.pop();
        builder.push("Decrease");
        DECREASE_INTERVAL_TICKS = builder.defineInRange("DecreaseIntervalTicks", 40, 0, Integer.MAX_VALUE);
        RAGE_THAT_ENTITY_LOSES_EVERY_INTERVAL = builder.defineInRange("RageThatEntityLosesEveryInterval", 5, 0, Integer.MAX_VALUE);
        builder.pop();
        builder.pop();
        CONFIG = builder.build();
    }

    private void showParticleOnFullRagePlayer(TickEvent.PlayerTickEvent event) {
        if (!SHOW_PARTICLE_ON_FULL_RAGE.get()) return;
        if (event.phase != TickEvent.Phase.START) return;
        Player player = event.player;
        if (((RageHolder)player).rage$isFullRage() && player instanceof ServerPlayer) {
            ((ServerPlayer) player).serverLevel().sendParticles(ParticleTypes.CRIT, player.getX(), player.getY(), player.getZ(), 8, 0.2, 0.2, 0.2, 0.0);
        }
    }

    private void showParticleOnFullRageLiving(LivingEvent.LivingTickEvent event) {
        if (!SHOW_PARTICLE_ON_FULL_RAGE.get()) return;
        LivingEntity entity = event.getEntity();
        if (((RageHolder)entity).rage$isFullRage() && entity.level() instanceof ServerLevel) {
            ((ServerLevel) entity.level()).sendParticles(ParticleTypes.CRIT, entity.getX(), entity.getY(), entity.getZ(), 8, 0.2, 0.2, 0.2, 0.0);
        }
    }

    private void bumpRageOnHurt(LivingHurtEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) return;
        if (shouldBumpRage(event.getSource())) ((RageHolder)entity).rage$bumpRage();
    }

    private static boolean shouldBumpRage(@NotNull DamageSource damageSource) {
        Entity entity = damageSource.getEntity();
        Entity directEntity = damageSource.getDirectEntity();
        return entity instanceof LivingEntity || directEntity instanceof LivingEntity;
    }

    private void bumpOrUseRageOnAttack(LivingHurtEvent event) {
        LivingEntity attacked = event.getEntity();
        Level level = attacked.level();
        if (level.isClientSide()) return;
        DamageSource damageSource = event.getSource();
        Entity entity = damageSource.getEntity();
        Entity directEntity = damageSource.getDirectEntity();
        processRageOnAttack(entity, event, level, attacked);
        if (directEntity != null && directEntity.equals(entity)) return;
        processRageOnAttack(directEntity, event, level, attacked);
    }

    private static void processRageOnAttack(Entity sourceEntity, LivingHurtEvent event, Level level, LivingEntity attacked) {
        if (sourceEntity instanceof LivingEntity) {
            if (((RageHolder)sourceEntity).rage$isFullRage()) {
                event.setAmount((float) (((RageHolder)sourceEntity).rage$getDamageBonus() * (double) event.getAmount()));
                MinecraftForge.EVENT_BUS.post(new FullRageAttackEvent(attacked, (LivingEntity) sourceEntity));
                if (PLAY_DING_ON_FULL_RAGE_ATTACK.get()) {
                    level.playSound(null, attacked.blockPosition(), SoundEvents.ARROW_HIT_PLAYER, sourceEntity.getSoundSource(), DING_VOLUME.get().floatValue(), DING_PITCH.get().floatValue());
                }
                ((RageHolder) sourceEntity).rage$clearRage();
            } else {
                ((RageHolder)sourceEntity).rage$bumpRage();
            }
        }
    }
}
