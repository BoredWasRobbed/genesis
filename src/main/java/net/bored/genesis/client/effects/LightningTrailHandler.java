package net.bored.genesis.client.effects;

import com.mojang.blaze3d.vertex.PoseStack;
import net.bored.genesis.client.renderer.LightningRenderer;
import net.bored.genesis.core.capabilities.PowerCapability;
import net.bored.genesis.core.powers.IPower;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * The main client-side manager for the entire lightning effect.
 * This version generates continuous, branching trails from multiple body points.
 */
public class LightningTrailHandler {

    private final Map<UUID, List<LightningTrail>> activeTrails = new ConcurrentHashMap<>();
    private final Random random = new Random();

    public LightningTrailHandler() {}

    public void onClientTick() {
        Player player = Minecraft.getInstance().player;
        if (player == null || Minecraft.getInstance().isPaused()) {
            activeTrails.clear();
            return;
        }

        // 1. Tick all trails for all players and remove any that are fully faded.
        activeTrails.values().forEach(list -> {
            list.forEach(LightningTrail::tick);
            list.removeIf(trail -> !trail.isAlive());
        });
        activeTrails.entrySet().removeIf(entry -> entry.getValue().isEmpty());


        // 2. Determine if a power is currently active.
        AtomicReference<IPower> activePowerRef = new AtomicReference<>(null);
        player.getCapability(PowerCapability.POWER_MANAGER).ifPresent(powerManager -> {
            powerManager.getAllPowers().values().stream()
                    .filter(p -> p.getTrailColor() != 0)
                    .findFirst()
                    .ifPresent(activePowerRef::set);
        });
        IPower activePower = activePowerRef.get();

        if (activePower != null) {
            // --- FIX FOR STATE ISSUES ---
            // 3. If power is active, ensure trunk trails exist.
            // This logic will run on login, after standing still, and after re-toggling.
            List<LightningTrail> playerTrails = activeTrails.computeIfAbsent(player.getUUID(), k -> new ArrayList<>());
            boolean hasTrunks = playerTrails.stream().anyMatch(t -> !t.isBranch);

            if (!hasTrunks) {
                // If there are no trunks, it means we need to create a new set.
                // Clear any lingering branches just in case.
                playerTrails.clear();

                int trailCount = 15;
                for (int i = 0; i < trailCount; i++) {
                    float width = 0.03f + random.nextFloat() * 0.04f; // Random width
                    // --- EDIT: Set a fixed lifetime for all trails ---
                    int lifetime = 20;
                    float yOffset = 0.1f + random.nextFloat() * 0.9f; // Random height from feet to head
                    float xzOffsetFactor = random.nextFloat(); // Random distance from center
                    boolean startsOnRightSide = random.nextBoolean();
                    playerTrails.add(new LightningTrail(activePower.getTrailColor(), width, lifetime, false, yOffset, xzOffsetFactor, startsOnRightSide));
                }
            }
        }
    }

    public void onRender(PoseStack poseStack, float partialTick) {
        Player player = Minecraft.getInstance().player;
        if (player == null || Minecraft.getInstance().isPaused()) return;

        List<LightningTrail> playerTrails = activeTrails.get(player.getUUID());
        if (playerTrails == null || playerTrails.isEmpty()) return;

        boolean powerActive = player.getCapability(PowerCapability.POWER_MANAGER)
                .map(pm -> pm.getAllPowers().values().stream().anyMatch(p -> p.getTrailColor() != 0))
                .orElse(false);

        if (powerActive) {
            // Get all trunk trails to update them from different body points
            List<LightningTrail> trunkTrails = playerTrails.stream().filter(t -> !t.isBranch).collect(Collectors.toList());
            if (trunkTrails.isEmpty()) return;

            List<LightningTrail> newBranches = new ArrayList<>();

            double pX = Mth.lerp(partialTick, player.xo, player.getX());
            double pY = Mth.lerp(partialTick, player.yo, player.getY());
            double pZ = Mth.lerp(partialTick, player.zo, player.getZ());
            Vec3 interpolatedPosition = new Vec3(pX, pY, pZ);

            Vec3 motion = player.getDeltaMovement();

            for (LightningTrail trunk : trunkTrails) {
                float height = player.getBbHeight();
                float yPos = height * trunk.yOffset;

                Vec3 rightVec = player.getLookAngle().cross(new Vec3(0, 1, 0)).normalize().scale(0.3 * trunk.xzOffsetFactor);
                if(!trunk.startsOnRightSide) rightVec = rightVec.reverse();

                Vec3 originPoint = interpolatedPosition.add(rightVec.x, yPos, rightVec.z);

                if (motion.lengthSqr() > 0.001) {
                    originPoint = originPoint.subtract(motion.normalize().scale(0.2));
                }

                trunk.generateSegments(originPoint, newBranches);
            }

            // Add any newly created branches to the main list
            if (!newBranches.isEmpty()) {
                playerTrails.addAll(newBranches);
            }
        }

        // Render ALL trails (trunks and branches) for the player.
        LightningRenderer.render(poseStack, playerTrails);
    }
}
