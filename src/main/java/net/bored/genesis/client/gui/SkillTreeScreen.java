package net.bored.genesis.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.bored.genesis.client.gui.widgets.SkillWidget;
import net.bored.genesis.core.skills.Skill;
import net.bored.genesis.core.skills.SkillTree;
import net.bored.genesis.network.PacketHandler;
import net.bored.genesis.network.packets.UnlockSkillC2SPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class SkillTreeScreen extends Screen {

    private static final int BORDER_MARGIN = 30;

    private final ResourceLocation powerId;
    private final SkillTree skillTree;
    private Set<ResourceLocation> unlockedSkills;
    private int skillPoints;
    private int level;
    private int experience;
    private int xpToNextLevel;

    private final Map<ResourceLocation, SkillWidget> skillWidgets = new HashMap<>();

    // --- Panning Variables ---
    private double panX = 0;
    private double panY = 0;
    private boolean isDragging = false;
    private double dragStartX, dragStartY;
    private double minPanX = 0, maxPanX = 0, minPanY = 0, maxPanY = 0;

    public SkillTreeScreen(ResourceLocation powerId, SkillTree skillTree, Set<ResourceLocation> unlockedSkills, int skillPoints, int level, int experience, int xpToNextLevel) {
        super(Component.literal("Skill Tree"));
        this.powerId = powerId;
        this.skillTree = skillTree;
        this.unlockedSkills = unlockedSkills;
        this.skillPoints = skillPoints;
        this.level = level;
        this.experience = experience;
        this.xpToNextLevel = xpToNextLevel;
    }

    public void updateData(Set<ResourceLocation> newUnlockedSkills, int newSkillPoints, int newLevel, int newExperience, int newXpToNextLevel) {
        this.unlockedSkills = newUnlockedSkills;
        this.skillPoints = newSkillPoints;
        this.level = newLevel;
        this.experience = newExperience;
        this.xpToNextLevel = newXpToNextLevel;
        this.populateSkillWidgets();
    }

    private void populateSkillWidgets() {
        this.clearWidgets();
        this.skillWidgets.clear();

        for (Skill skill : skillTree.getAllSkills().values()) {
            SkillWidget.SkillState state;
            boolean hasPrereqs = skill.getPrerequisites().stream().allMatch(unlockedSkills::contains);

            if (unlockedSkills.contains(skill.getId())) {
                state = SkillWidget.SkillState.UNLOCKED;
            } else if (hasPrereqs && skillPoints >= skill.getCost()) {
                state = SkillWidget.SkillState.CAN_UNLOCK;
            } else {
                state = SkillWidget.SkillState.LOCKED;
            }

            // The widget's position is its skill coordinate plus the current pan
            int widgetX = skill.getSkillX() + (int)panX;
            int widgetY = skill.getSkillY() + (int)panY;

            SkillWidget widget = new SkillWidget(widgetX, widgetY, skill, state);
            this.skillWidgets.put(skill.getId(), widget);
            this.addRenderableWidget(widget);
        }
    }

    private void calculatePanBounds() {
        if (skillTree.getAllSkills().isEmpty()) {
            this.minPanX = this.maxPanX = 0;
            this.minPanY = this.maxPanY = 0;
            return;
        }

        int contentMinX = skillTree.getAllSkills().values().stream().mapToInt(Skill::getSkillX).min().orElse(0);
        int contentMaxX = skillTree.getAllSkills().values().stream().mapToInt(s -> s.getSkillX() + 26).max().orElse(0);
        int contentMinY = skillTree.getAllSkills().values().stream().mapToInt(Skill::getSkillY).min().orElse(0);
        int contentMaxY = skillTree.getAllSkills().values().stream().mapToInt(s -> s.getSkillY() + 26).max().orElse(0);

        int contentWidth = contentMaxX - contentMinX;
        int contentHeight = contentMaxY - contentMinY;

        // --- Ground-up Rewrite of Panning Logic ---
        if (contentWidth > this.width) {
            this.minPanX = this.width - contentMaxX - BORDER_MARGIN;
            this.maxPanX = -contentMinX + BORDER_MARGIN;
        } else {
            this.minPanX = this.maxPanX = (this.width - contentWidth) / 2.0 - contentMinX;
        }

        if (contentHeight > this.height) {
            this.minPanY = this.height - contentMaxY - BORDER_MARGIN;
            this.maxPanY = -contentMinY + BORDER_MARGIN;
        } else {
            this.minPanY = this.maxPanY = (this.height - contentHeight) / 2.0 - contentMinY;
        }
    }

    @Override
    protected void init() {
        super.init();
        calculatePanBounds();
        this.panX = Mth.clamp((minPanX + maxPanX) / 2.0, minPanX, maxPanX);
        this.panY = Mth.clamp((minPanY + maxPanY) / 2.0, minPanY, maxPanY);
        populateSkillWidgets();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);

        renderLines(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        guiGraphics.fill(5, 5, 120, 50, 0x80000000);
        guiGraphics.drawString(this.font, this.title, 10, 10, 0xFFFFFF);
        guiGraphics.drawString(this.font, "Lvl: " + this.level + " (" + this.experience + "/" + this.xpToNextLevel + ")", 10, 25, 0xFFFFFF);
        guiGraphics.drawString(this.font, "SP: " + this.skillPoints, 10, 35, 0xFFFFFF);

        for (SkillWidget widget : this.skillWidgets.values()) {
            if (widget.isHoveredOrFocused()) {
                guiGraphics.renderComponentTooltip(this.font, widget.getSkillTooltip(), mouseX, mouseY);
                break;
            }
        }
    }

    private void renderLines(GuiGraphics guiGraphics) {
        for (Skill skill : this.skillTree.getAllSkills().values()) {
            for (ResourceLocation prereqId : skill.getPrerequisites()) {
                Skill prereqSkill = this.skillTree.getSkill(prereqId).orElse(null);
                if (prereqSkill != null) {
                    int x1 = skill.getSkillX() + 13 + (int)panX;
                    int y1 = skill.getSkillY() + 13 + (int)panY;
                    int x2 = prereqSkill.getSkillX() + 13 + (int)panX;
                    int y2 = prereqSkill.getSkillY() + 13 + (int)panY;

                    boolean unlocked = unlockedSkills.contains(skill.getId()) && unlockedSkills.contains(prereqId);
                    int color = unlocked ? 0xFFFFFFFF : 0xFF808080;

                    guiGraphics.hLine(x2, x1, y2, color);
                    guiGraphics.vLine(x1, y2, y1, color);
                }
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (SkillWidget widget : this.skillWidgets.values()) {
                if (widget.isMouseOver(mouseX, mouseY)) {
                    if (widget.state == SkillWidget.SkillState.CAN_UNLOCK) {
                        PacketHandler.sendToServer(new UnlockSkillC2SPacket(this.powerId, widget.skill.getId()));
                        return true;
                    }
                }
            }
        }
        if (button == 1) {
            this.isDragging = true;
            this.dragStartX = mouseX - panX;
            this.dragStartY = mouseY - panY;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double pMouseX, double pMouseY, int pButton) {
        if (pButton == 1) {
            this.isDragging = false;
        }
        return super.mouseReleased(pMouseX, pMouseY, pButton);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.isDragging && button == 1) {
            this.panX = Mth.clamp(mouseX - this.dragStartX, this.minPanX, this.maxPanX);
            this.panY = Mth.clamp(mouseY - this.dragStartY, this.minPanY, this.maxPanY);
            populateSkillWidgets();
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
