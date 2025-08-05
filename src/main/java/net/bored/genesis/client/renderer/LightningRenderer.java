package net.bored.genesis.client.renderer;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.bored.genesis.client.effects.LightningTrail;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;

/**
 * Handles the low-level OpenGL (via RenderSystem) drawing of the lightning trail mesh.
 * This renders a single, thin, glowing 3D beam for each trail.
 */
public class LightningRenderer {

    public static void render(PoseStack poseStack, Collection<LightningTrail> trails) {
        Vec3 cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        poseStack.pushPose();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        Matrix4f matrix = poseStack.last().pose();

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        RenderSystem.setShader(GameRenderer::getPositionColorShader); // No texture is needed
        RenderSystem.disableCull();

        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (LightningTrail trail : trails) {
            drawTrailVertices(buffer, matrix, trail);
        }
        tesselator.end();

        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc(); // Reset blend function to default
        RenderSystem.disableBlend();
        poseStack.popPose();
    }

    private static void drawTrailVertices(BufferBuilder buffer, Matrix4f matrix, LightningTrail trail) {
        Deque<LightningTrail.TrailPoint> points = trail.getPoints();
        if (points.size() < 2) return;

        float width = trail.getWidth();
        int r = (trail.getColor() >> 16) & 0xFF;
        int g = (trail.getColor() >> 8) & 0xFF;
        int b = trail.getColor() & 0xFF;

        Iterator<LightningTrail.TrailPoint> it = points.iterator();
        LightningTrail.TrailPoint prevPoint = it.next();

        Vector3f lookVector3f = Minecraft.getInstance().gameRenderer.getMainCamera().getLookVector();
        Vec3 cameraView = new Vec3(lookVector3f.x(), lookVector3f.y(), lookVector3f.z());

        while (it.hasNext()) {
            LightningTrail.TrailPoint currentPoint = it.next();

            long currentTime = System.currentTimeMillis();
            float agePrev = (float)(currentTime - prevPoint.creationTime) / trail.getMaxLifetime();
            int alphaPrev = (int)((1.0f - Mth.clamp(agePrev, 0.0f, 1.0f)) * 96);

            float ageCurr = (float)(currentTime - currentPoint.creationTime) / trail.getMaxLifetime();
            int alphaCurr = (int)((1.0f - Mth.clamp(ageCurr, 0.0f, 1.0f)) * 96);

            // Calculate orientation vectors for the beam
            Vec3 direction = currentPoint.position.subtract(prevPoint.position).normalize();
            Vec3 right = direction.cross(cameraView);
            if (right.lengthSqr() < 1.0E-6D) {
                right = direction.cross(new Vec3(0, 1, 0));
            }
            right = right.normalize().scale(width / 2.0);
            Vec3 up = direction.cross(right).normalize().scale(width / 2.0);

            // Calculate the 8 vertices that define this segment of the beam
            Vec3 prev_v1 = prevPoint.position.add(up).subtract(right); // Top-Left
            Vec3 prev_v2 = prevPoint.position.add(up).add(right);     // Top-Right
            Vec3 prev_v3 = prevPoint.position.subtract(up).add(right); // Bottom-Right
            Vec3 prev_v4 = prevPoint.position.subtract(up).subtract(right); // Bottom-Left

            Vec3 curr_v1 = currentPoint.position.add(up).subtract(right); // Top-Left
            Vec3 curr_v2 = currentPoint.position.add(up).add(right);     // Top-Right
            Vec3 curr_v3 = currentPoint.position.subtract(up).add(right); // Bottom-Right
            Vec3 curr_v4 = currentPoint.position.subtract(up).subtract(right); // Bottom-Left

            // Draw the 4 side faces of the beam segment
            // Top face
            buffer.vertex(matrix, (float)prev_v1.x, (float)prev_v1.y, (float)prev_v1.z).color(r, g, b, alphaPrev).endVertex();
            buffer.vertex(matrix, (float)prev_v2.x, (float)prev_v2.y, (float)prev_v2.z).color(r, g, b, alphaPrev).endVertex();
            buffer.vertex(matrix, (float)curr_v2.x, (float)curr_v2.y, (float)curr_v2.z).color(r, g, b, alphaCurr).endVertex();
            buffer.vertex(matrix, (float)curr_v1.x, (float)curr_v1.y, (float)curr_v1.z).color(r, g, b, alphaCurr).endVertex();

            // Right face
            buffer.vertex(matrix, (float)prev_v2.x, (float)prev_v2.y, (float)prev_v2.z).color(r, g, b, alphaPrev).endVertex();
            buffer.vertex(matrix, (float)prev_v3.x, (float)prev_v3.y, (float)prev_v3.z).color(r, g, b, alphaPrev).endVertex();
            buffer.vertex(matrix, (float)curr_v3.x, (float)curr_v3.y, (float)curr_v3.z).color(r, g, b, alphaCurr).endVertex();
            buffer.vertex(matrix, (float)curr_v2.x, (float)curr_v2.y, (float)curr_v2.z).color(r, g, b, alphaCurr).endVertex();

            // Bottom face
            buffer.vertex(matrix, (float)prev_v3.x, (float)prev_v3.y, (float)prev_v3.z).color(r, g, b, alphaPrev).endVertex();
            buffer.vertex(matrix, (float)prev_v4.x, (float)prev_v4.y, (float)prev_v4.z).color(r, g, b, alphaPrev).endVertex();
            buffer.vertex(matrix, (float)curr_v4.x, (float)curr_v4.y, (float)curr_v4.z).color(r, g, b, alphaCurr).endVertex();
            buffer.vertex(matrix, (float)curr_v3.x, (float)curr_v3.y, (float)curr_v3.z).color(r, g, b, alphaCurr).endVertex();

            // Left face
            buffer.vertex(matrix, (float)prev_v4.x, (float)prev_v4.y, (float)prev_v4.z).color(r, g, b, alphaPrev).endVertex();
            buffer.vertex(matrix, (float)prev_v1.x, (float)prev_v1.y, (float)prev_v1.z).color(r, g, b, alphaPrev).endVertex();
            buffer.vertex(matrix, (float)curr_v1.x, (float)curr_v1.y, (float)curr_v1.z).color(r, g, b, alphaCurr).endVertex();
            buffer.vertex(matrix, (float)curr_v4.x, (float)curr_v4.y, (float)curr_v4.z).color(r, g, b, alphaCurr).endVertex();

            prevPoint = currentPoint;
        }
    }
}
