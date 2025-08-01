package net.bored.genesis.client.gui.widgets;

import com.mojang.blaze3d.systems.RenderSystem;
import net.bored.genesis.core.skills.Skill;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public class SkillWidget extends AbstractWidget {

    private static final ResourceLocation WIDGETS_TEXTURE = new ResourceLocation("textures/gui/advancements/widgets.png");
    public final Skill skill;
    public final SkillState state;

    public enum SkillState {
        LOCKED,
        UNLOCKED,
        CAN_UNLOCK
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
        }

        guiGraphics.blit(WIDGETS_TEXTURE, getX(), getY(), u, v, this.width, this.height);
        ResourceLocation icon = new ResourceLocation("minecraft", "textures/item/feather.png");
        guiGraphics.blit(icon, getX() + 5, getY() + 5, 0, 0, 16, 16, 16, 16);
    }

    /**
     * Renamed to avoid conflicts with the parent AbstractWidget class.
     */
    public List<Component> getSkillTooltip() {
        return List.of(
                this.skill.getName(),
                this.skill.getDescription(),
                Component.literal("Cost: " + this.skill.getCost() + " SP")
        );
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        this.defaultButtonNarrationText(narrationElementOutput);
    }
}
