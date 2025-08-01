package net.bored.genesis.core.skills;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class Skill {

    public enum SkillType {
        PASSIVE,
        ACTIVE
    }

    private final ResourceLocation id;
    private final int cost;
    private final List<ResourceLocation> prerequisites;
    private final Component name;
    private final Component description;
    private final int x;
    private final int y;
    private final SkillType type;

    public Skill(ResourceLocation id, int cost, List<ResourceLocation> prerequisites, String name, String description, int x, int y, SkillType type) {
        this.id = id;
        this.cost = cost;
        this.prerequisites = Collections.unmodifiableList(prerequisites);
        this.name = Component.literal(name);
        this.description = Component.literal(description);
        this.x = x;
        this.y = y;
        this.type = type;
    }

    public ResourceLocation getId() { return id; }
    public int getCost() { return cost; }
    public List<ResourceLocation> getPrerequisites() { return prerequisites; }
    public Component getName() { return name; }
    public Component getDescription() { return description; }
    public int getSkillX() { return x; }
    public int getSkillY() { return y; }
    public SkillType getType() { return type; }

    public static class Deserializer {
        String id;
        int cost;
        List<String> prerequisites;
        String name;
        String description;
        int x;
        int y;
        SkillType type = SkillType.PASSIVE; // Default to passive if not specified

        public Deserializer() {}

        public Deserializer(Skill skill) {
            this.id = skill.getId().toString();
            this.cost = skill.getCost();
            this.prerequisites = skill.getPrerequisites().stream().map(ResourceLocation::toString).collect(Collectors.toList());
            this.name = skill.getName().getString();
            this.description = skill.getDescription().getString();
            this.x = skill.getSkillX();
            this.y = skill.getSkillY();
            this.type = skill.getType();
        }

        public Deserializer(FriendlyByteBuf buf) {
            this.id = buf.readUtf();
            this.cost = buf.readInt();
            this.prerequisites = buf.readList(FriendlyByteBuf::readUtf);
            this.name = buf.readUtf();
            this.description = buf.readUtf();
            this.x = buf.readInt();
            this.y = buf.readInt();
            this.type = buf.readEnum(SkillType.class);
        }

        public void toBytes(FriendlyByteBuf buf) {
            buf.writeUtf(id);
            buf.writeInt(cost);
            buf.writeCollection(prerequisites, FriendlyByteBuf::writeUtf);
            buf.writeUtf(name);
            buf.writeUtf(description);
            buf.writeInt(x);
            buf.writeInt(y);
            buf.writeEnum(type);
        }

        public Skill build() {
            List<ResourceLocation> prereqIds = prerequisites.stream()
                    .map(ResourceLocation::new)
                    .collect(Collectors.toList());
            return new Skill(new ResourceLocation(id), cost, prereqIds, name, description, x, y, type);
        }
    }
}
