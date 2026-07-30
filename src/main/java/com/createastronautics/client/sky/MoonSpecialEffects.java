package com.createastronautics.client.sky;

/** Moon sky: same black, starless-moon starfield as deep space, but the sun still rises and sets. */
public class MoonSpecialEffects extends AbstractSpaceEffects {
    public MoonSpecialEffects() {
        super(true);
    }

    @Override
    protected boolean rendersSun() {
        return true;
    }
}
