package com.createastronautics.block;

import com.simibubi.create.content.kinetics.fan.AirCurrent;
import com.simibubi.create.content.kinetics.fan.IAirCurrentSource;

/**
 * Create's {@link AirCurrent#tick()} does 3 things: spawns its generic air-flow particle, shoves around any
 * entity caught in the stream, and runs fan processing (smoking/blasting/etc.) on items in the stream or on
 * a belt. A life-support fan shouldn't be a wind cannon, so this drops the particle (replaced by the
 * oxygen-gated mist in {@link OxygenFanBlockEntity}) and the entity shove entirely, keeping only belt-based
 * item processing - {@link #findEntities()} is also a no-op since nothing is ever pushed, so there's no
 * reason to pay for the entity search that would otherwise feed it.
 */
public class OxygenAirCurrent extends AirCurrent {
    public OxygenAirCurrent(IAirCurrentSource source) {
        super(source);
    }

    @Override
    public void tick() {
        if (this.direction == null) {
            this.rebuild();
        }
        this.tickAffectedHandlers();
    }

    @Override
    public void findEntities() {
    }
}
