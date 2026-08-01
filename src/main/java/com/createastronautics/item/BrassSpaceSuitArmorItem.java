package com.createastronautics.item;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * An {@link ArmorItem} rendered as an animated GeckoLib model rather than vanilla's flat humanoid armor
 * layers - the model/animation/texture triple is supplied by a {@link GeoModel}, e.g.
 * {@link BrassSpaceSuitArmorModel}. Adapted from Northstar-Redux's {@code SpaceSuitArmorItem}, see
 * {@code THIRD_PARTY_NOTICES.md}.
 */
public class BrassSpaceSuitArmorItem extends ArmorItem implements GeoItem {
    private final AnimatableInstanceCache animatableCache = GeckoLibUtil.createInstanceCache(this);
    private final Supplier<GeoModel<BrassSpaceSuitArmorItem>> model;

    public BrassSpaceSuitArmorItem(Holder<ArmorMaterial> material, Type type, Properties properties, Supplier<GeoModel<BrassSpaceSuitArmorItem>> model) {
        super(material, type, properties);
        this.model = model;
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private GeoArmorRenderer<?> renderer;

            @Override
            public <T extends LivingEntity> HumanoidModel<?> getGeoArmorRenderer(@Nullable T livingEntity, ItemStack itemStack, @Nullable EquipmentSlot equipmentSlot, @Nullable HumanoidModel<T> original) {
                if (renderer == null) {
                    if (getType() == Type.HELMET) {
                        // The helmet's own texture is a glass dome over most of its front/sides (see the
                        // alpha channel), so only its renderer needs translucent blending. The other pieces
                        // are fully opaque and must stay on the default armorCutoutNoCull - translucent
                        // RenderTypes get deferred to the end-of-frame translucent pass, which stops them
                        // from writing depth in time to occlude things like Essential's clothing cosmetics.
                        renderer = new GeoArmorRenderer<>(model.get()) {
                            @Override
                            public RenderType getRenderType(BrassSpaceSuitArmorItem animatable, ResourceLocation texture, @Nullable MultiBufferSource bufferSource, float partialTick) {
                                return RenderType.entityTranslucent(texture);
                            }
                        };
                    } else {
                        renderer = new GeoArmorRenderer<>(model.get());
                    }
                }
                return renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 2, this::predicate));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animatableCache;
    }

    private PlayState predicate(AnimationState<BrassSpaceSuitArmorItem> event) {
        event.getController().setAnimation(RawAnimation.begin().thenLoop("idle"));
        return PlayState.CONTINUE;
    }
}
