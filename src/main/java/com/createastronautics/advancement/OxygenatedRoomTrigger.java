package com.createastronautics.advancement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

/**
 * Fires whenever a player is standing in a sealed, oxygen-fan-supplied room in a no-atmosphere dimension -
 * the same check {@link com.createastronautics.PlayerEnvironmentHandler} already uses to suspend
 * suffocation/suit oxygen drain there (see {@code OxygenFanRegistry.isOxygenated}). There's no extra
 * condition of its own beyond the standard "player" predicate every trigger supports - whether the room is
 * genuinely breathable is decided by the caller before this is ever triggered.
 */
public class OxygenatedRoomTrigger extends SimpleCriterionTrigger<OxygenatedRoomTrigger.TriggerInstance> {
    @Override
    public Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer player) {
        this.trigger(player, instance -> true);
    }

    public record TriggerInstance(Optional<ContextAwarePredicate> player) implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(builder -> builder.group(
                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player)
        ).apply(builder, TriggerInstance::new));
    }
}
