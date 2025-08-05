package net.bored.genesis.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
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
import net.minecraft.util.Mth;

import java.util.HashMap;
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

    private ResourceLocation selectedSkillForBinding = null;

    // --- Pan and Zoom Variables ---
    private float zoom = 1.0f;
    private double cameraX = 0;
    private double cameraY = 0;
    private boolean isDragging = false;
    private double lastMouseX = 0;
    private double lastMouseY = 0;

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

    private void populateSkillWidgets() {
        this.clearWidgets();
        this.skillWidgets.clear();

        for (Skill skill : skillTree.getAllSkills().values()) {
            // Find if this skill is excluded by any unlocked skill
            Optional<ResourceLocation> excludingSkillId = Optional.empty();
            for (ResourceLocation unlockedSkillId : unlockedSkills) {
                Optional<Skill> unlockedSkillOpt = skillTree.getSkill(unlockedSkillId);
                if (unlockedSkillOpt.isPresent() && unlockedSkillOpt.get().getExclusions().contains(skill.getId())) {
                    excludingSkillId = Optional.of(unlockedSkillId);
                    break;
                }
            }

            if (excludingSkillId.isPresent()) {
                Optional<Component> reason = skillTree.getSkill(excludingSkillId.get()).map(Skill::getName);
                SkillWidget widget = new SkillWidget(skill.getSkillX(), skill.getSkillY(), skill, SkillWidget.SkillState.EXCLUDED, reason);
                this.skillWidgets.put(skill.getId(), widget);
                this.addRenderableWidget(widget);
                continue; // Skip to the next skill
            }


            SkillWidget.SkillState state;
            boolean hasPrereqs;

            if (skill.unlocksWithAnyPrerequisite()) {
                hasPrereqs = skill.getPrerequisites().isEmpty() || skill.getPrerequisites().stream().anyMatch(unlockedSkills::contains);
            } else {
                hasPrereqs = skill.getPrerequisites().stream().allMatch(unlockedSkills::contains);
            }

            if (unlockedSkills.contains(skill.getId())) {
                state = (skill.isToggleable() && !activeSkills.contains(skill.getId()))
                        ? SkillWidget.SkillState.DEACTIVATED
                        : SkillWidget.SkillState.UNLOCKED;
            } else if (hasPrereqs && skillPoints >= skill.getCost()) {
                state = SkillWidget.SkillState.CAN_UNLOCK;
            } else {
                state = SkillWidget.SkillState.LOCKED;
            }

            SkillWidget widget = new SkillWidget(skill.getSkillX(), skill.getSkillY(), skill, state);
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

    @Override
    protected void init() {
        super.init();
        // Center the camera on the middle of the skill tree content initially
        int minX = skillTree.getAllSkills().values().stream().mapToInt(Skill::getSkillX).min().orElse(0);
        int maxX = skillTree.getAllSkills().values().stream().mapToInt(s -> s.getSkillX() + 26).max().orElse(0);
        int minY = skillTree.getAllSkills().values().stream().mapToInt(Skill::getSkillY).min().orElse(0);
        int maxY = skillTree.getAllSkills().values().stream().mapToInt(s -> s.getSkillY() + 26).max().orElse(0);
        this.cameraX = -(minX + (maxX - minX) / 2.0);
        this.cameraY = -(minY + (maxY - minY) / 2.0);

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

        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();

        // --- Apply Pan and Zoom Transformations ---
        poseStack.translate(this.width / 2.0, this.height / 2.0, 0); // Center origin
        poseStack.scale(this.zoom, this.zoom, 1.0f);
        poseStack.translate(this.cameraX, this.cameraY, 0);

        // Render skill tree elements inside the transformed space
        renderLines(guiGraphics);
        for (SkillWidget widget : this.skillWidgets.values()) {
            widget.render(guiGraphics, mouseX, mouseY, partialTick);
        }

        poseStack.popPose(); // End transformation

        // --- Render Static UI Elements (unaffected by pan/zoom) ---
        guiGraphics.fill(5, 5, 120, 55, 0x80000000);
        guiGraphics.drawString(this.font, this.title, 10, 10, 0xFFFFFF);
        guiGraphics.drawString(this.font, "Lvl: " + this.level + " (" + this.experience + "/" + this.xpToNextLevel + ")", 10, 25, 0xFFFFFF);
        guiGraphics.drawString(this.font, "SP: " + this.skillPoints, 10, 40, 0xFFFFFF);

        for (Button button : this.abilitySlotButtons) {
            button.render(guiGraphics, mouseX, mouseY, partialTick);
        }

        // --- Tooltip Logic (needs transformed mouse coords) ---
        double worldMouseX = (mouseX - this.width / 2.0) / this.zoom - this.cameraX;
        double worldMouseY = (mouseY - this.height / 2.0) / this.zoom - this.cameraY;

        this.skillWidgets.values().stream()
                .filter(widget -> widget.isMouseOver(worldMouseX, worldMouseY))
                .findFirst()
                .ifPresent(widget -> guiGraphics.renderComponentTooltip(this.font, widget.getSkillTooltip(), mouseX, mouseY));

        if (this.selectedSkillForBinding != null) {
            guiGraphics.drawCenteredString(this.font, "Binding Skill...", this.width / 2, 10, 0xFFFF55);
        }
    }

    private void renderLines(GuiGraphics guiGraphics) {
        for (SkillWidget widget : this.skillWidgets.values()) {
            Skill skill = widget.skill;
            for (ResourceLocation prereqId : skill.getPrerequisites()) {
                SkillWidget prereqWidget = this.skillWidgets.get(prereqId);
                if (prereqWidget != null) {
                    int x1 = skill.getSkillX() + 13;
                    int y1 = skill.getSkillY() + 13;
                    int x2 = prereqWidget.getX() + 13;
                    int y2 = prereqWidget.getY() + 13;

                    boolean unlocked = unlockedSkills.contains(skill.getId()) && unlockedSkills.contains(prereqId);
                    int color = unlocked ? 0xFFFFFFFF : 0xFF808080;
                    drawLine(guiGraphics, x1, y1, x2, y2, color);
                }
            }
        }
    }

    private void drawLine(GuiGraphics guiGraphics, int x1, int y1, int x2, int y2, int color) {
        // This is a simple implementation of Bresenham's line algorithm
        int dx = Math.abs(x2 - x1);
        int dy = -Math.abs(y2 - y1);
        int sx = x1 < x2 ? 1 : -1;
        int sy = y1 < y2 ? 1 : -1;
        int err = dx + dy;
        while (true) {
            guiGraphics.fill(x1, y1, x1 + 1, y1 + 1, color);
            if (x1 == x2 && y1 == y2) break;
            int e2 = 2 * err;
            if (e2 >= dy) {
                err += dy;
                x1 += sx;
            }
            if (e2 <= dx) {
                err += dx;
                y1 += sy;
            }
        }
    }

    // --- Input Handlers for Pan and Zoom ---

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double worldMouseX = (mouseX - this.width / 2.0) / this.zoom - this.cameraX;
        double worldMouseY = (mouseY - this.height / 2.0) / this.zoom - this.cameraY;

        for (Button uiButton : this.abilitySlotButtons) {
            if (uiButton.mouseClicked(mouseX, mouseY, button)) {
                this.setFocused(uiButton);
                return true;
            }
        }

        for (SkillWidget widget : this.skillWidgets.values()) {
            if (widget.isMouseOver(worldMouseX, worldMouseY)) {
                if (widget.state == SkillWidget.SkillState.EXCLUDED) {
                    return true;
                }
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

        // If no widget was clicked, start panning
        if (button == 0) {
            this.isDragging = true;
            this.lastMouseX = mouseX;
            this.lastMouseY = mouseY;
            return true;
        }

        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            this.isDragging = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.isDragging) {
            this.cameraX += (mouseX - this.lastMouseX) / this.zoom;
            this.cameraY += (mouseY - this.lastMouseY) / this.zoom;
            this.lastMouseX = mouseX;
            this.lastMouseY = mouseY;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        float oldZoom = this.zoom;
        this.zoom = Mth.clamp(this.zoom + (float)delta * 0.1f, 0.5f, 2.0f);

        // --- Zoom centering logic ---
        // Adjust camera position to keep the point under the cursor stationary
        double zoomFactor = (1.0 / oldZoom) - (1.0 / this.zoom);
        this.cameraX += (mouseX - this.width / 2.0) * zoomFactor;
        this.cameraY += (mouseY - this.height / 2.0) * zoomFactor;

        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
