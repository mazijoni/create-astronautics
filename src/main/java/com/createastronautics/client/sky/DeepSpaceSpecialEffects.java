package com.createastronautics.client.sky;

/** Deep Space sky: the plain night-without-a-moon starfield, and no sun either. */
public class DeepSpaceSpecialEffects extends AbstractSpaceEffects {
    public DeepSpaceSpecialEffects() {
        super(false);
    }

    @Override
    protected boolean rendersSun() {
        return false;
    }
}
