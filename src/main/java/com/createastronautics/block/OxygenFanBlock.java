package com.createastronautics.block;

import com.simibubi.create.content.kinetics.fan.EncasedFanBlock;
import com.simibubi.create.content.kinetics.fan.EncasedFanBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;

/**
 * A reskinned clone of Create's own Encased Fan - kinetic-driven air current, entity pushing, item
 * processing (smoking/blasting/splashing/haunting), and wrench behaviour (plain click re-faces the block,
 * sneak-click picks it up) all come straight from {@link EncasedFanBlock} unchanged. The only override
 * needed is the block entity type, since {@link EncasedFanBlock#getBlockEntityType()} is hardcoded to
 * Create's own registry entry rather than being derived from the block instance.
 */
public class OxygenFanBlock extends EncasedFanBlock {
    public OxygenFanBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntityType<? extends EncasedFanBlockEntity> getBlockEntityType() {
        return ModBlockEntities.OXYGEN_FAN.get();
    }
}
