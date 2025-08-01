package net.bored.genesis.core.skills;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import net.bored.genesis.Genesis;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Manages loading and storing all SkillTree definitions from JSON files.
 * This listener is triggered during server startup and on /reload.
 */
public class SkillTreeManager extends SimpleJsonResourceReloadListener {

    private static final Gson GSON = new GsonBuilder().create();
    private static final String DIRECTORY = "skill_trees";

    private Map<ResourceLocation, SkillTree> skillTrees = new HashMap<>();

    // Helper class for GSON to parse the top-level JSON structure
    private static class SkillTreeFile {
        List<Skill.Deserializer> skills;
    }

    public SkillTreeManager() {
        super(GSON, DIRECTORY);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> jsonFiles, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<ResourceLocation, SkillTree> loadedTrees = new HashMap<>();
        Genesis.LOGGER.info("Starting to load skill trees. Found {} files.", jsonFiles.size());

        jsonFiles.forEach((fileId, jsonElement) -> {
            try {
                // The fileId provided by the listener is already the correct ID for the skill tree
                // (e.g., for 'data/genesis/skill_trees/speedster.json', the fileId is 'genesis:speedster').
                // The complex substring logic was incorrect and has been removed.
                ResourceLocation treeId = fileId;

                SkillTreeFile fileContents = GSON.fromJson(jsonElement, SkillTreeFile.class);

                if (fileContents != null && fileContents.skills != null) {
                    Map<ResourceLocation, Skill> skills = new HashMap<>();
                    for (Skill.Deserializer ds : fileContents.skills) {
                        Skill skill = ds.build();
                        skills.put(skill.getId(), skill);
                    }
                    loadedTrees.put(treeId, new SkillTree(skills));
                    Genesis.LOGGER.info("Successfully loaded skill tree: " + treeId);
                } else {
                    Genesis.LOGGER.error("Failed to parse skill tree file, content or 'skills' array is null: " + fileId);
                }
            } catch (Exception e) {
                Genesis.LOGGER.error("Exception while loading skill tree from file: " + fileId, e);
            }
        });

        this.skillTrees = loadedTrees;
    }

    /**
     * Gets a loaded skill tree by its ID.
     * @param treeId The ID of the skill tree (e.g., genesis:speedster).
     * @return An Optional containing the SkillTree if found.
     */
    public Optional<SkillTree> getSkillTree(ResourceLocation treeId) {
        return Optional.ofNullable(skillTrees.get(treeId));
    }
}
