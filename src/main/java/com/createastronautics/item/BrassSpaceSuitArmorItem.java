package com.createastronautics.item;

import com.createastronautics.fluid.ModDataComponents;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
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
 * <p>
 * Every piece (including the helmet) renders as one uniform, fully opaque {@link GeoArmorRenderer} - the
 * helmet's see-through glass visor is a separate overlay drawn on top by
 * {@link com.createastronautics.client.BrassSpaceSuitHelmetLayer}, not a second render pass here.
 */
public class BrassSpaceSuitArmorItem extends ArmorItem implements GeoItem {
    private final AnimatableInstanceCache animatableCache = GeckoLibUtil.createInstanceCache(this);
    private final Supplier<GeoModel<BrassSpaceSuitArmorItem>> model;
    private final int oxygenCapacityMb;

    public BrassSpaceSuitArmorItem(Holder<ArmorMaterial> material, Type type, Properties properties, Supplier<GeoModel<BrassSpaceSuitArmorItem>> model) {
        this(material, type, properties, model, 0);
    }

    /**
     * @param oxygenCapacityMb Capacity of this piece's built-in oxygen tank, or 0 for pieces that don't carry
     *                         one (only the chestplate does today) - determines whether the inventory bar
     *                         ({@link #isBarVisible}) shows at all.
     */
    public BrassSpaceSuitArmorItem(Holder<ArmorMaterial> material, Type type, Properties properties, Supplier<GeoModel<BrassSpaceSuitArmorItem>> model, int oxygenCapacityMb) {
        super(material, type, properties);
        this.model = model;
        this.oxygenCapacityMb = oxygenCapacityMb;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return oxygenCapacityMb > 0 && !getOxygenContent(stack).isEmpty();
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Mth.clamp(Math.round(13.0F * getOxygenContent(stack).getAmount() / oxygenCapacityMb), 0, 13);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x3AB3E0;
    }

    private static SimpleFluidContent getOxygenContent(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.OXYGEN_CONTENT.get(), SimpleFluidContent.EMPTY);
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private GeoArmorRenderer<?> renderer;

            @Override
            public <T extends LivingEntity> HumanoidModel<?> getGeoArmorRenderer(@Nullable T livingEntity, ItemStack itemStack, @Nullable EquipmentSlot equipmentSlot, @Nullable HumanoidModel<T> original) {
                if (renderer == null) {
                    renderer = new GeoArmorRenderer<>(model.get());
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
