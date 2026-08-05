package com.createastronautics.magnetic;

import com.createastronautics.CreateAstronautics;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, CreateAstronautics.MODID);

    // Server-side only: the effect (a downward nudge, see MagneticBootsEffectHandler) is invisible to
    // everyone but the player it's pulling, so there's no need to sync this to any client.
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Boolean>> MAGNETIC_BOOTS_ACTIVE =
            ATTACHMENT_TYPES.register("magnetic_boots_active", () -> AttachmentType.builder(() -> Boolean.FALSE).build());
}
