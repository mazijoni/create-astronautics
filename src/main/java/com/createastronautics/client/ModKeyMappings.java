package com.createastronautics.client;

import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public class ModKeyMappings {
    public static final String CATEGORY = "key.categories.createastronautics";

    public static final KeyMapping TOGGLE_MAGNETIC_BOOTS = new KeyMapping(
            "key.createastronautics.toggle_magnetic_boots", GLFW.GLFW_KEY_Z, CATEGORY);
}
