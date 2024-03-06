package com.teampotato.rage.mixin;

import com.google.common.base.Suppliers;
import com.teampotato.rage.Rage;
import com.teampotato.rage.api.RageHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity implements RageHolder {
    @Unique private volatile int rage$currentRage;
    @Unique private int rage$decreaseInterval;

    public LivingEntityMixin(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Override
    public int rage$getRage() {
        if (Rage.ONLY_PLAYERS_HAVCE_RAGE.get() && !EntityType.PLAYER.equals(this.getType())) return 0;
        return this.rage$currentRage;
    }

    @Override
    public void rage$bumpRage() {
        this.rage$currentRage = this.rage$getRage() + Rage.GAINED_RAGE_PER_HURT_OR_ATTACK.get();
        this.rage$notifyPlayer();
    }

    @Override
    public void rage$clearRage() {
        this.rage$currentRage = 0;
        this.rage$notifyPlayer();
    }

    @Override
    public void rage$decreaseRage() {
        int lost = Rage.RAGE_THAT_ENTITY_LOSES_EVERY_INTERVAL.get();
        this.rage$currentRage = lost >= this.rage$getRage() ? 0 : this.rage$getRage() - lost;
    }

    @Unique private static final Supplier<CommandSourceStack> COMMAND_SOURCE_STACK_SUPPLIER = Suppliers.memoize(() -> ServerLifecycleHooks.getCurrentServer().createCommandSourceStack().withSuppressedOutput());

    @Unique
    private void rage$notifyPlayer() {
        if (Rage.NOTIFY_PLAYER_ON_REACHING_FULL_RAGE.get()) {
            LivingEntity self = (LivingEntity) (Object) this;
            if (this.rage$isFullRage() && self instanceof Player) {
                ((Player)self).displayClientMessage(Component.translatable("rage.notify.full"), true);
                ServerLifecycleHooks.getCurrentServer().getCommands().performPrefixedCommand(COMMAND_SOURCE_STACK_SUPPLIER.get(), "advancement grant " + ((Player) self).getGameProfile().getName() + " only rage:full_rage");
            }
        }

        if (Rage.NOTIFY_PLAYER_ON_RAGE_CHANGE.get()) {
            LivingEntity self = (LivingEntity) (Object) this;
            if (self instanceof Player) {
                ((Player) self).displayClientMessage(Component.translatable("rage.notify", this.rage$getRage()), true);
            }
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void onTick(CallbackInfo ci) {
        this.rage$decreaseInterval = this.rage$decreaseInterval + 1;
        if (this.rage$decreaseInterval == Rage.DECREASE_INTERVAL_TICKS.get()) {
            this.rage$decreaseRage();
            this.rage$decreaseInterval = 0;
        }
    }
}
