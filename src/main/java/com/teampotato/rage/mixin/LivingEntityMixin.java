package com.teampotato.rage.mixin;

import com.teampotato.rage.Rage;
import com.teampotato.rage.api.RageHolder;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity implements RageHolder {
    @Unique private volatile int rage$currentRage;

    public LivingEntityMixin(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Override
    public int rage$getRage() {
        return this.rage$currentRage;
    }

    @Override
    public void rage$bumpRage() {
        final int origin = this.rage$currentRage;
        this.rage$currentRage = origin + Rage.GAINED_RAGE_PER_HURT_OR_ATTACK.get();
        this.rage$notifyPlayer();
    }

    @Override
    public void rage$clearRage() {
        this.rage$currentRage = 0;
        this.rage$notifyPlayer();
    }

    @Unique
    private void rage$notifyPlayer() {
        if (!Rage.NOTIFY_PLAYER_ON_RAGE_CHANGE.get()) return;
        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof Player) {
            ((Player)self).displayClientMessage(new TranslatableComponent("rage.notify", this.rage$getRage()), true);
        }
    }
}
