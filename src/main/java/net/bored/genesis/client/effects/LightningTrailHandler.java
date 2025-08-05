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
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * The main client-side manager for the entire lightning effect.
 * It decides when to create, update, and render trails based on player actions.
 */
public class LightningTrailHandler {

    private final Map<UUID, List<LightningTrail>> activeTrails = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> wasPowerActiveLastTick = new ConcurrentHashMap<>();

    public LightningTrailHandler() {}

    public void onClientTick() {
        Player player = Minecraft.getInstance().player;
        if (player == null || Minecraft.getInstance().isPaused()) {
            activeTrails.clear();
            wasPowerActiveLastTick.clear();
            return;
        }

        activeTrails.values().forEach(list -> {
            list.forEach(LightningTrail::tick);
            list.removeIf(trail -> !trail.isAlive());
        });
        activeTrails.entrySet().removeIf(entry -> entry.getValue().isEmpty());

        AtomicReference<IPower> activePowerRef = new AtomicReference<>(null);
        player.getCapability(PowerCapability.POWER_MANAGER).ifPresent(powerManager -> {
            powerManager.getAllPowers().values().stream()
                    .filter(p -> p.getTrailColor() != 0)
                    .findFirst()
                    .ifPresent(activePowerRef::set);
        });
        IPower activePower = activePowerRef.get();

        boolean isPowerActiveThisTick = activePower != null;
        boolean wasActive = wasPowerActiveLastTick.getOrDefault(player.getUUID(), false);

        if (isPowerActiveThisTick && !wasActive) {
            List<LightningTrail> playerTrails = activeTrails.computeIfAbsent(player.getUUID(), k -> new ArrayList<>());
            // --- CHANGE: Create thinner trails ---
            // Torso trail
            playerTrails.add(new LightningTrail(activePower.getTrailColor(), 0.06f, 20, false));
            // Two leg trails
            playerTrails.add(new LightningTrail(activePower.getTrailColor(), 0.04f, 15, false));
            playerTrails.add(new LightningTrail(activePower.getTrailColor(), 0.04f, 15, false));
        }

        wasPowerActiveLastTick.put(player.getUUID(), isPowerActiveThisTick);
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
            List<LightningTrail> trunkTrails = playerTrails.stream().filter(t -> !t.isBranch).collect(Collectors.toList());
            if (trunkTrails.isEmpty()) return;

            List<LightningTrail> newBranches = new ArrayList<>();

            double pX = Mth.lerp(partialTick, player.xo, player.getX());
            double pY = Mth.lerp(partialTick, player.yo, player.getY());
            double pZ = Mth.lerp(partialTick, player.zo, player.getZ());
            Vec3 interpolatedPosition = new Vec3(pX, pY, pZ);

            Vec3 motion = player.getDeltaMovement();
            // --- CHANGE: Calculate new origin points for legs/torso ---
            Vec3 torsoOrigin = interpolatedPosition.add(0, player.getBbHeight() * 0.6, 0);
            Vec3 legOrigin = interpolatedPosition.add(0, player.getBbHeight() * 0.2, 0);

            Vec3 rightVec = player.getLookAngle().cross(new Vec3(0, 1, 0)).normalize().scale(0.3);

            for (int i = 0; i < trunkTrails.size(); i++) {
                LightningTrail trunk = trunkTrails.get(i);
                Vec3 originPoint;

                // Assign origins based on order
                if (i == 0) { // Torso trail
                    originPoint = torsoOrigin;
                } else if (i == 1) { // Right leg trail
                    originPoint = legOrigin.add(rightVec);
                } else { // Left leg trail (assuming i == 2)
                    originPoint = legOrigin.subtract(rightVec);
                }

                if (motion.lengthSqr() > 0.001) {
                    originPoint = originPoint.subtract(motion.normalize().scale(0.2));
                }

                trunk.generateSegments(originPoint, newBranches);
            }

            if (!newBranches.isEmpty()) {
                playerTrails.addAll(newBranches);
            }
        }

        LightningRenderer.render(poseStack, playerTrails);
    }
}
