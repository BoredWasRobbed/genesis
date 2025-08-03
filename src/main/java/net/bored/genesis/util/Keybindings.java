package net.bored.genesis.util;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public class Keybindings {

    public static final String KEY_CATEGORY_GENESIS = "key.category.genesis";
    public static final String KEY_OPEN_SKILL_TREE = "key.genesis.open_skill_tree";
    public static final String KEY_POWER = "key.genesis.power";
    public static final String KEY_VARIANT = "key.genesis.variant"; // New Variant Key
    public static final String KEY_ABILITY_1 = "key.genesis.ability_1";
    public static final String KEY_ABILITY_2 = "key.genesis.ability_2";
    public static final String KEY_ABILITY_3 = "key.genesis.ability_3";
    public static final String KEY_ABILITY_4 = "key.genesis.ability_4";
    public static final String KEY_ABILITY_5 = "key.genesis.ability_5";

    public static final KeyMapping OPEN_SKILL_TREE_KEY = new KeyMapping(KEY_OPEN_SKILL_TREE, KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, KEY_CATEGORY_GENESIS);
    public static final KeyMapping POWER_KEY = new KeyMapping(KEY_POWER, KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, KEY_CATEGORY_GENESIS);
    public static final KeyMapping VARIANT_KEY = new KeyMapping(KEY_VARIANT, KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_ALT, KEY_CATEGORY_GENESIS);

    public static final KeyMapping ABILITY_1_KEY = new KeyMapping(KEY_ABILITY_1, KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Z, KEY_CATEGORY_GENESIS);
    public static final KeyMapping ABILITY_2_KEY = new KeyMapping(KEY_ABILITY_2, KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_X, KEY_CATEGORY_GENESIS);
    public static final KeyMapping ABILITY_3_KEY = new KeyMapping(KEY_ABILITY_3, KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_C, KEY_CATEGORY_GENESIS);
    public static final KeyMapping ABILITY_4_KEY = new KeyMapping(KEY_ABILITY_4, KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, KEY_CATEGORY_GENESIS);
    public static final KeyMapping ABILITY_5_KEY = new KeyMapping(KEY_ABILITY_5, KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, KEY_CATEGORY_GENESIS);
}
