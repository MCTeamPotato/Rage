package com.teampotato.rage.api.event;

import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingEvent;

public class FullRageAttackEvent extends LivingEvent {
    private final LivingEntity attacked, attacker;

    public FullRageAttackEvent(LivingEntity attacked, LivingEntity attacker) {
        super(attacked);
        this.attacked = attacked;
        this.attacker = attacker;
    }

    public LivingEntity getAttacked() {
        return attacked;
    }

    public LivingEntity getAttacker() {
        return attacker;
    }
}
