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
    private final List<ResourceLocation> exclusions;
    private final Component name;
    private final Component description;
    private final Component infoDescription;
    private final ResourceLocation icon; // New field for the icon
    private final int x;
    private final int y;
    private final SkillType type;
    private final boolean toggleable;

    public Skill(ResourceLocation id, int cost, List<ResourceLocation> prerequisites, List<ResourceLocation> exclusions, String name, String description, String infoDescription, ResourceLocation icon, int x, int y, SkillType type, boolean toggleable) {
        this.id = id;
        this.cost = cost;
        this.prerequisites = Collections.unmodifiableList(prerequisites);
        this.exclusions = Collections.unmodifiableList(exclusions);
        this.name = Component.literal(name);
        this.description = Component.literal(description);
        this.infoDescription = Component.literal(infoDescription);
        this.icon = icon;
        this.x = x;
        this.y = y;
        this.type = type;
        this.toggleable = toggleable;
    }

    public ResourceLocation getId() { return id; }
    public int getCost() { return cost; }
    public List<ResourceLocation> getPrerequisites() { return prerequisites; }
    public List<ResourceLocation> getExclusions() { return exclusions; }
    public Component getName() { return name; }
    public Component getDescription() { return description; }
    public Component getInfoDescription() { return infoDescription; }
    public ResourceLocation getIcon() { return icon; }
    public int getSkillX() { return x; }
    public int getSkillY() { return y; }
    public SkillType getType() { return type; }
    public boolean isToggleable() { return toggleable; }

    public static class Deserializer {
        String id;
        int cost;
        List<String> prerequisites;
        List<String> exclusions = Collections.emptyList();
        String name;
        String description;
        String info_description = "";
        String icon = "minecraft:feather"; // Default icon
        int x;
        int y;
        SkillType type = SkillType.PASSIVE;
        boolean toggleable = false;

        public Deserializer() {}

        public Deserializer(Skill skill) {
            this.id = skill.getId().toString();
            this.cost = skill.getCost();
            this.prerequisites = skill.getPrerequisites().stream().map(ResourceLocation::toString).collect(Collectors.toList());
            this.exclusions = skill.getExclusions().stream().map(ResourceLocation::toString).collect(Collectors.toList());
            this.name = skill.getName().getString();
            this.description = skill.getDescription().getString();
            this.info_description = skill.getInfoDescription().getString();
            this.icon = skill.getIcon().toString();
            this.x = skill.getSkillX();
            this.y = skill.getSkillY();
            this.type = skill.getType();
            this.toggleable = skill.isToggleable();
        }

        public Deserializer(FriendlyByteBuf buf) {
            this.id = buf.readUtf();
            this.cost = buf.readInt();
            this.prerequisites = buf.readList(FriendlyByteBuf::readUtf);
            this.exclusions = buf.readList(FriendlyByteBuf::readUtf);
            this.name = buf.readUtf();
            this.description = buf.readUtf();
            this.info_description = buf.readUtf();
            this.icon = buf.readUtf();
            this.x = buf.readInt();
            this.y = buf.readInt();
            this.type = buf.readEnum(SkillType.class);
            this.toggleable = buf.readBoolean();
        }

        public void toBytes(FriendlyByteBuf buf) {
            buf.writeUtf(id);
            buf.writeInt(cost);
            buf.writeCollection(prerequisites, FriendlyByteBuf::writeUtf);
            buf.writeCollection(exclusions, FriendlyByteBuf::writeUtf);
            buf.writeUtf(name);
            buf.writeUtf(description);
            buf.writeUtf(info_description);
            buf.writeUtf(icon);
            buf.writeInt(x);
            buf.writeInt(y);
            buf.writeEnum(type);
            buf.writeBoolean(toggleable);
        }

        public Skill build() {
            List<ResourceLocation> prereqIds = prerequisites.stream()
                    .map(ResourceLocation::new)
                    .collect(Collectors.toList());
            List<ResourceLocation> exclusionIds = exclusions.stream()
                    .map(ResourceLocation::new)
                    .collect(Collectors.toList());
            return new Skill(new ResourceLocation(id), cost, prereqIds, exclusionIds, name, description, info_description, new ResourceLocation(icon), x, y, type, toggleable);
        }
    }
}
