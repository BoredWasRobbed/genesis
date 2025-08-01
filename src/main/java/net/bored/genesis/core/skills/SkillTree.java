package net.bored.genesis.core.skills;

import com.google.common.collect.ImmutableMap;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.Optional;

/**
 * Represents the complete structure of a skill tree for a power.
 * This object holds all possible skills and their relationships.
 */
public class SkillTree {

    private final Map<ResourceLocation, Skill> skills;

    public SkillTree(Map<ResourceLocation, Skill> skills) {
        this.skills = ImmutableMap.copyOf(skills);
    }

    /**
     * Gets a skill from the tree by its ID.
     * @param skillId The ID of the skill.
     * @return An Optional containing the Skill if found.
     */
    public Optional<Skill> getSkill(ResourceLocation skillId) {
        return Optional.ofNullable(skills.get(skillId));
    }

    /**
     * @return An unmodifiable map of all skills in this tree.
     */
    public Map<ResourceLocation, Skill> getAllSkills() {
        return skills;
    }
}
