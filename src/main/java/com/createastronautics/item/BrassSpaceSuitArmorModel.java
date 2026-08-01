package com.createastronautics.item;

import com.createastronautics.CreateAstronautics;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/** Points GeckoLib at the Brass Space Suit's geometry/animation/texture. */
public class BrassSpaceSuitArmorModel extends GeoModel<BrassSpaceSuitArmorItem> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(CreateAstronautics.MODID, "geo/armor/brass_space_suit.geo.json");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(CreateAstronautics.MODID, "textures/armor/brass_space_suit.png");
    private static final ResourceLocation ANIMATIONS = ResourceLocation.fromNamespaceAndPath(CreateAstronautics.MODID, "animations/armor/brass_space_suit.animation.json");

    @Override
    public ResourceLocation getModelResource(BrassSpaceSuitArmorItem object) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(BrassSpaceSuitArmorItem object) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(BrassSpaceSuitArmorItem animatable) {
        return ANIMATIONS;
    }
}
