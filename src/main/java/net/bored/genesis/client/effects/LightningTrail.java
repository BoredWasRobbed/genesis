package net.bored.genesis.client.effects;

import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Random;

/**
 * Data-holder class representing a single, continuous lightning bolt trail.
 * Manages the points that make up the trail's path, its color, and lifetime.
 */
public class LightningTrail {

    /**
     * A helper class to store position and creation time for each point,
     * allowing for smooth, per-segment fading.
     */
    public static class TrailPoint {
        public final Vec3 position;
        public final long creationTime;

        public TrailPoint(Vec3 position) {
            this.position = position;
            this.creationTime = System.currentTimeMillis();
        }
    }

    private final Deque<TrailPoint> points = new ArrayDeque<>();
    private Vec3 lastAddedPoint = null;
    private final int color;
    private final float width;
    private final long maxLifetimeMillis;
    private final Random random = new Random();
    public final boolean isBranch;

    // --- FIELDS FOR STABLE ORIGIN ---
    public final float yOffset;
    public final float xzOffsetFactor;
    public final boolean startsOnRightSide; // --- NEW FIELD ---

    public LightningTrail(int color, float width, int lifetimeTicks, boolean isBranch, float yOffset, float xzOffsetFactor, boolean startsOnRightSide) {
        this.color = color;
        this.width = width;
        this.maxLifetimeMillis = lifetimeTicks * 50L;
        this.isBranch = isBranch;
        this.yOffset = yOffset;
        this.xzOffsetFactor = xzOffsetFactor;
        this.startsOnRightSide = startsOnRightSide;
    }

    // Constructor for branches, which don't need their own origin points
    public LightningTrail(int color, float width, int lifetimeTicks, boolean isBranch) {
        this(color, width, lifetimeTicks, isBranch, 0, 0, false);
    }

    public Deque<TrailPoint> getPoints() {
        return points;
    }

    public int getColor() {
        return color;
    }

    public float getWidth() {
        return width;
    }

    public long getMaxLifetime() {
        return maxLifetimeMillis;
    }

    public boolean isAlive() {
        return !points.isEmpty();
    }

    public void tick() {
        long currentTime = System.currentTimeMillis();
        while (!points.isEmpty() && (currentTime - points.peekFirst().creationTime) > maxLifetimeMillis) {
            points.removeFirst();
        }
    }

    /**
     * Generates the jagged segments for the lightning trail between the last known point and a new position.
     */
    public void generateSegments(Vec3 newPosition, List<LightningTrail> newBranches) {
        if (lastAddedPoint == null) {
            this.lastAddedPoint = newPosition;
            this.points.add(new TrailPoint(newPosition));
            return;
        }

        if (lastAddedPoint.distanceToSqr(newPosition) < 0.01) {
            return;
        }

        List<Vec3> segmentPoints = new ArrayList<>();
        segmentPoints.add(lastAddedPoint);
        segmentPoints.add(newPosition);

        float displacement = isBranch ? 0.1f : 0.2f;
        int iterations = isBranch ? 0 : 1;

        for (int i = 0; i < iterations; i++) {
            List<Vec3> newPoints = new ArrayList<>();
            newPoints.add(segmentPoints.get(0));

            for (int j = 0; j < segmentPoints.size() - 1; j++) {
                Vec3 p1 = segmentPoints.get(j);
                Vec3 p2 = segmentPoints.get(j + 1);
                Vec3 mid = p1.lerp(p2, 0.5);
                Vec3 offsetDir = p2.subtract(p1).normalize().cross(new Vec3(random.nextFloat() - 0.5, random.nextFloat() - 0.5, random.nextFloat() - 0.5).normalize());
                if (offsetDir.lengthSqr() == 0) {
                    offsetDir = new Vec3(1, 0, 0);
                }
                mid = mid.add(offsetDir.scale((random.nextFloat() * 2 - 1) * displacement));
                newPoints.add(mid);
                newPoints.add(p2);
            }
            segmentPoints = newPoints;
            displacement /= 2.0f;
        }

        for(int i = 1; i < segmentPoints.size(); i++) {
            TrailPoint newPoint = new TrailPoint(segmentPoints.get(i));
            this.points.addLast(newPoint);

            if (newBranches != null && !isBranch && random.nextFloat() < 0.15f) {
                LightningTrail branch = new LightningTrail(this.color, this.width * 0.6f, 10, true);
                Vec3 branchEnd = newPoint.position.add(new Vec3(random.nextFloat() - 0.5, random.nextFloat() - 0.5, random.nextFloat() - 0.5).normalize().scale(2.0));
                branch.generateSegments(branchEnd, null);
                newBranches.add(branch);
            }
        }

        this.lastAddedPoint = newPosition;
    }
}
