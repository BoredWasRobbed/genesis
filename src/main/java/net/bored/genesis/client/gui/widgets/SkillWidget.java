package net.bored.genesis.client.gui.widgets;

import com.mojang.blaze3d.systems.RenderSystem;
import net.bored.genesis.core.skills.Skill;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

public class SkillWidget extends AbstractWidget {

    private static final ResourceLocation WIDGETS_TEXTURE = new ResourceLocation("textures/gui/advancements/widgets.png");
    public final Skill skill;
    public final SkillState state;

    public enum SkillState {
        LOCKED,
        UNLOCKED,
        CAN_UNLOCK,
        DEACTIVATED
    }

    public SkillWidget(int x, int y, Skill skill, SkillState state) {
        super(x, y, 26, 26, skill.getName());
        this.skill = skill;
        this.state = state;
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int u = 0;
        int v = 128;

        switch (state) {
            case UNLOCKED:
                u = 26; // Unlocked frame
                break;
            case CAN_UNLOCK:
                u = 0; // Obtainable frame
                break;
            case LOCKED:
                u = 52; // Locked frame
                break;
            case DEACTIVATED:
                u = 26; // Unlocked frame, but we'll gray out the icon
                break;
        }

        guiGraphics.blit(WIDGETS_TEXTURE, getX(), getY(), u, v, this.width, this.height);

        if (state == SkillState.DEACTIVATED) {
            RenderSystem.setShaderColor(0.5F, 0.5F, 0.5F, 1.0F);
        }

        // --- NEW ROBUST ICON RENDERING ---
        String iconPath = this.skill.getIcon().toString();
        if (iconPath.contains("/")) { // It's a direct texture path
            // This blit method correctly scales the entire source texture into the target render area.
            guiGraphics.blit(this.skill.getIcon(), getX() + 5, getY() + 5, 16, 16, 0, 0, this.skill.getIconWidth(), this.skill.getIconHeight(), this.skill.getIconWidth(), this.skill.getIconHeight());
        } else { // It's an item ID
            Item iconItem = ForgeRegistries.ITEMS.getValue(this.skill.getIcon());
            if (iconItem == null) {
                iconItem = Items.FEATHER; // Fallback for invalid item IDs
            }
            guiGraphics.renderItem(new ItemStack(iconItem), getX() + 5, getY() + 5);
        }

        if (state == SkillState.DEACTIVATED) {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    public List<Component> getSkillTooltip() {
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(this.skill.getName());
        tooltip.add(this.skill.getDescription().copy().withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Cost: " + this.skill.getCost() + " SP").withStyle(ChatFormatting.DARK_GRAY));

        if (this.skill.isToggleable() && (this.state == SkillState.UNLOCKED || this.state == SkillState.DEACTIVATED)) {
            tooltip.add(Component.literal(""));
            tooltip.add(Component.literal("[Right-Click to Toggle]").withStyle(ChatFormatting.YELLOW));
        }
        return tooltip;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        this.defaultButtonNarrationText(narrationElementOutput);
    }
}
