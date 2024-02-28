package com.teampotato.rage.api;

public interface RageHolder {
    int rage$getRage();
    void rage$bumpRage();
    void rage$clearRage();
    boolean rage$isFullRage();
    float rage$getDamageBonus();
}
