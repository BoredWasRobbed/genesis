package net.bored.genesis.util;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public class Keybindings {

    public static final String KEY_CATEGORY_GENESIS = "key.category.genesis";
    public static final String KEY_OPEN_SKILL_TREE = "key.genesis.open_skill_tree";

    public static final KeyMapping OPEN_SKILL_TREE_KEY = new KeyMapping(
            KEY_OPEN_SKILL_TREE,
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_K, // Default key is 'K'
            KEY_CATEGORY_GENESIS
    );
}
