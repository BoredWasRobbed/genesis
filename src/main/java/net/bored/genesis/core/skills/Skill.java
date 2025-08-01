package net.bored.genesis.core.skills;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class Skill {

    private final ResourceLocation id;
    private final int cost;
    private final List<ResourceLocation> prerequisites;
    private final Component name;
    private final Component description;
    private final int x; // X position on the skill tree grid
    private final int y; // Y position on the skill tree grid

    public Skill(ResourceLocation id, int cost, List<ResourceLocation> prerequisites, String name, String description, int x, int y) {
        this.id = id;
        this.cost = cost;
        this.prerequisites = Collections.unmodifiableList(prerequisites);
        this.name = Component.literal(name);
        this.description = Component.literal(description);
        this.x = x;
        this.y = y;
    }

    public ResourceLocation getId() { return id; }
    public int getCost() { return cost; }
    public List<ResourceLocation> getPrerequisites() { return prerequisites; }
    public Component getName() { return name; }
    public Component getDescription() { return description; }
    // Renamed to avoid conflicts with parent widget classes
    public int getSkillX() { return x; }
    public int getSkillY() { return y; }

    public static class Deserializer {
        String id;
        int cost;
        List<String> prerequisites;
        String name;
        String description;
        int x;
        int y;

        public Deserializer() {}

        public Deserializer(Skill skill) {
            this.id = skill.getId().toString();
            this.cost = skill.getCost();
            this.prerequisites = skill.getPrerequisites().stream().map(ResourceLocation::toString).collect(Collectors.toList());
            this.name = skill.getName().getString();
            this.description = skill.getDescription().getString();
            this.x = skill.getSkillX(); // Use the corrected method name
            this.y = skill.getSkillY(); // Use the corrected method name
        }

        public Deserializer(FriendlyByteBuf buf) {
            this.id = buf.readUtf();
            this.cost = buf.readInt();
            this.prerequisites = buf.readList(FriendlyByteBuf::readUtf);
            this.name = buf.readUtf();
            this.description = buf.readUtf();
            this.x = buf.readInt();
            this.y = buf.readInt();
        }

        public void toBytes(FriendlyByteBuf buf) {
            buf.writeUtf(id);
            buf.writeInt(cost);
            buf.writeCollection(prerequisites, FriendlyByteBuf::writeUtf);
            buf.writeUtf(name);
            buf.writeUtf(description);
            buf.writeInt(x);
            buf.writeInt(y);
        }

        public Skill build() {
            List<ResourceLocation> prereqIds = prerequisites.stream()
                    .map(ResourceLocation::new)
                    .collect(Collectors.toList());
            return new Skill(new ResourceLocation(id), cost, prereqIds, name, description, x, y);
        }
    }
}
