package net.bored.genesis.client.gui;

import net.bored.genesis.client.gui.widgets.SkillWidget;
import net.bored.genesis.core.skills.Skill;
import net.bored.genesis.core.skills.SkillTree;
import net.bored.genesis.network.PacketHandler;
import net.bored.genesis.network.packets.BindAbilityC2SPacket;
import net.bored.genesis.network.packets.ToggleSkillC2SPacket;
import net.bored.genesis.network.packets.UnlockSkillC2SPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class SkillTreeScreen extends Screen {

    private final ResourceLocation powerId;
    private final SkillTree skillTree;
    private Set<ResourceLocation> unlockedSkills;
    private Set<ResourceLocation> activeSkills;
    private int skillPoints;
    private int level;
    private int experience;
    private int xpToNextLevel;
    private Map<Integer, ResourceLocation> abilityBindings;

    private final Map<ResourceLocation, SkillWidget> skillWidgets = new HashMap<>();
    private final Button[] abilitySlotButtons = new Button[5];
    private final Set<ResourceLocation> excludedSkillsCache = new HashSet<>();

    private ResourceLocation selectedSkillForBinding = null;

    private int contentCenterX = 0;
    private int contentCenterY = 0;
    private static final int HORIZONTAL_OFFSET = 60;

    public SkillTreeScreen(ResourceLocation powerId, SkillTree skillTree, Set<ResourceLocation> unlockedSkills, Set<ResourceLocation> activeSkills, int skillPoints, int level, int experience, int xpToNextLevel, Map<Integer, ResourceLocation> abilityBindings) {
        super(Component.literal("Skill Tree"));
        this.powerId = powerId;
        this.skillTree = skillTree;
        this.unlockedSkills = unlockedSkills;
        this.activeSkills = activeSkills;
        this.skillPoints = skillPoints;
        this.level = level;
        this.experience = experience;
        this.xpToNextLevel = xpToNextLevel;
        this.abilityBindings = abilityBindings;
    }

    public void updateData(Set<ResourceLocation> newUnlockedSkills, Set<ResourceLocation> newActiveSkills, int newSkillPoints, int newLevel, int newExperience, int newXpToNextLevel, Map<Integer, ResourceLocation> newAbilityBindings) {
        this.unlockedSkills = newUnlockedSkills;
        this.activeSkills = newActiveSkills;
        this.skillPoints = newSkillPoints;
        this.level = newLevel;
        this.experience = newExperience;
        this.xpToNextLevel = newXpToNextLevel;
        this.abilityBindings = newAbilityBindings;
        this.populateSkillWidgets();
        this.updateAbilitySlotButtons();
        this.setFocused(null);
    }

    private void getExcludedSkills(Set<ResourceLocation> excluded) {
        // Find skills directly excluded by an unlocked skill
        for (ResourceLocation unlockedId : unlockedSkills) {
            skillTree.getSkill(unlockedId).ifPresent(unlockedSkill -> {
                for (ResourceLocation exclusionId : unlockedSkill.getExclusions()) {
                    if (!excluded.contains(exclusionId)) {
                        excluded.add(exclusionId);
                        recursivelyExcludeChildren(exclusionId, excluded);
                    }
                }
            });
        }
    }

    private void recursivelyExcludeChildren(ResourceLocation parentId, Set<ResourceLocation> excluded) {
        // Find all skills that have the excluded skill as a prerequisite
        for (Skill potentialChild : skillTree.getAllSkills().values()) {
            if (potentialChild.getPrerequisites().contains(parentId) && !excluded.contains(potentialChild.getId())) {
                excluded.add(potentialChild.getId());
                recursivelyExcludeChildren(potentialChild.getId(), excluded);
            }
        }
    }


    private void populateSkillWidgets() {
        this.clearWidgets();
        this.skillWidgets.clear();
        this.excludedSkillsCache.clear();
        getExcludedSkills(this.excludedSkillsCache);

        for (Skill skill : skillTree.getAllSkills().values()) {
            if (this.excludedSkillsCache.contains(skill.getId())) {
                continue; // Skip rendering this skill and its entire branch
            }

            SkillWidget.SkillState state;
            boolean hasPrereqs = skill.getPrerequisites().stream().allMatch(unlockedSkills::contains);

            if (unlockedSkills.contains(skill.getId())) {
                if (skill.isToggleable()) {
                    state = activeSkills.contains(skill.getId()) ? SkillWidget.SkillState.UNLOCKED : SkillWidget.SkillState.DEACTIVATED;
                } else {
                    state = SkillWidget.SkillState.UNLOCKED;
                }
            } else if (hasPrereqs && skillPoints >= skill.getCost()) {
                state = SkillWidget.SkillState.CAN_UNLOCK;
            } else {
                state = SkillWidget.SkillState.LOCKED;
            }

            int widgetX = (this.width / 2) - this.contentCenterX + skill.getSkillX() + HORIZONTAL_OFFSET;
            int widgetY = (this.height / 2) - this.contentCenterY + skill.getSkillY();

            SkillWidget widget = new SkillWidget(widgetX, widgetY, skill, state);
            this.skillWidgets.put(skill.getId(), widget);
            this.addRenderableWidget(widget);
        }

        for (int i = 0; i < 5; i++) {
            this.addRenderableWidget(this.abilitySlotButtons[i]);
        }
    }

    private void updateAbilitySlotButtons() {
        for (int i = 0; i < 5; i++) {
            final int slot = i;
            ResourceLocation boundSkillId = this.abilityBindings.get(slot);
            if (boundSkillId != null) {
                skillTree.getSkill(boundSkillId).ifPresent(skill -> {
                    abilitySlotButtons[slot].setMessage(skill.getName());
                });
            } else {
                abilitySlotButtons[slot].setMessage(Component.literal("[ ]"));
            }
        }
    }

    private void calculateContentCenter() {
        if (skillTree.getAllSkills().isEmpty()) {
            this.contentCenterX = 0;
            this.contentCenterY = 0;
            return;
        }

        int minX = skillTree.getAllSkills().values().stream().mapToInt(Skill::getSkillX).min().orElse(0);
        int maxX = skillTree.getAllSkills().values().stream().mapToInt(s -> s.getSkillX() + 26).max().orElse(0);
        int minY = skillTree.getAllSkills().values().stream().mapToInt(Skill::getSkillY).min().orElse(0);
        int maxY = skillTree.getAllSkills().values().stream().mapToInt(s -> s.getSkillY() + 26).max().orElse(0);

        this.contentCenterX = minX + (maxX - minX) / 2;
        this.contentCenterY = minY + (maxY - minY) / 2;
    }

    @Override
    protected void init() {
        super.init();
        calculateContentCenter();

        for (int i = 0; i < 5; i++) {
            final int slot = i;
            this.abilitySlotButtons[i] = Button.builder(Component.literal("[ ]"), (btn) -> {
                if (this.selectedSkillForBinding != null) {
                    PacketHandler.sendToServer(new BindAbilityC2SPacket(slot, this.selectedSkillForBinding));
                    this.abilityBindings.put(slot, this.selectedSkillForBinding);
                    this.selectedSkillForBinding = null;
                    updateAbilitySlotButtons();
                }
            }).bounds(10, 60 + (i * 25), 100, 20).build();
        }

        populateSkillWidgets();
        updateAbilitySlotButtons();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);

        renderLines(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        guiGraphics.fill(5, 5, 120, 55, 0x80000000);
        guiGraphics.drawString(this.font, this.title, 10, 10, 0xFFFFFF);
        guiGraphics.drawString(this.font, "Lvl: " + this.level + " (" + this.experience + "/" + this.xpToNextLevel + ")", 10, 25, 0xFFFFFF);
        guiGraphics.drawString(this.font, "SP: " + this.skillPoints, 10, 40, 0xFFFFFF);

        // --- NEW TOOLTIP LOGIC ---
        // This is a more robust way to handle tooltips, preventing the "sticky" bug.
        // It finds the specific widget the mouse is currently over and renders its tooltip.
        this.skillWidgets.values().stream()
                .filter(widget -> widget.isMouseOver(mouseX, mouseY))
                .findFirst()
                .ifPresent(widget -> guiGraphics.renderComponentTooltip(this.font, widget.getSkillTooltip(), mouseX, mouseY));


        if (this.selectedSkillForBinding != null) {
            guiGraphics.drawCenteredString(this.font, "Binding Skill... (Press an ability slot)", this.width / 2, 10, 0xFFFF55);
        }
    }

    private void renderLines(GuiGraphics guiGraphics) {
        for (Skill skill : this.skillTree.getAllSkills().values()) {
            if (this.excludedSkillsCache.contains(skill.getId())) continue;

            for (ResourceLocation prereqId : skill.getPrerequisites()) {
                if (this.excludedSkillsCache.contains(prereqId)) continue;

                Skill prereqSkill = this.skillTree.getSkill(prereqId).orElse(null);
                if (prereqSkill != null) {
                    int x1 = (this.width / 2) - this.contentCenterX + skill.getSkillX() + 13 + HORIZONTAL_OFFSET;
                    int y1 = (this.height / 2) - this.contentCenterY + skill.getSkillY() + 13;
                    int x2 = (this.width / 2) - this.contentCenterX + prereqSkill.getSkillX() + 13 + HORIZONTAL_OFFSET;
                    int y2 = (this.height / 2) - this.contentCenterY + prereqSkill.getSkillY() + 13;

                    boolean unlocked = unlockedSkills.contains(skill.getId()) && unlockedSkills.contains(prereqId);
                    int color = unlocked ? 0xFFFFFFFF : 0xFF808080;

                    drawLine(guiGraphics, x1, y1, x2, y2, color);
                }
            }
        }
    }

    private void drawLine(GuiGraphics guiGraphics, int x1, int y1, int x2, int y2, int color) {
        int dx = Math.abs(x2 - x1);
        int dy = -Math.abs(y2 - y1);
        int sx = x1 < x2 ? 1 : -1;
        int sy = y1 < y2 ? 1 : -1;
        int err = dx + dy;
        while (true) {
            guiGraphics.fill(x1, y1, x1 + 1, y1 + 1, color);
            if (x1 == x2 && y1 == y2) break;
            int e2 = 2 * err;
            if (e2 >= dy) { err += dy; x1 += sx; }
            if (e2 <= dx) { err += dx; y1 += sy; }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (SkillWidget widget : this.skillWidgets.values()) {
            if (widget.isMouseOver(mouseX, mouseY)) {
                if (button == 0 && widget.state == SkillWidget.SkillState.CAN_UNLOCK) {
                    PacketHandler.sendToServer(new UnlockSkillC2SPacket(this.powerId, widget.skill.getId()));
                    this.setFocused(null);
                    return true;
                }
                if (button == 1) { // Right-click
                    if (widget.skill.isToggleable() && (widget.state == SkillWidget.SkillState.UNLOCKED || widget.state == SkillWidget.SkillState.DEACTIVATED)) {
                        PacketHandler.sendToServer(new ToggleSkillC2SPacket(widget.skill.getId()));
                        if (this.activeSkills.contains(widget.skill.getId())) {
                            this.activeSkills.remove(widget.skill.getId());
                        } else {
                            this.activeSkills.add(widget.skill.getId());
                        }
                        this.populateSkillWidgets();
                        this.setFocused(null);
                        return true;
                    }
                    if (widget.state == SkillWidget.SkillState.UNLOCKED && widget.skill.getType() == Skill.SkillType.ACTIVE) {
                        this.selectedSkillForBinding = widget.skill.getId();
                        this.setFocused(null);
                        return true;
                    }
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
