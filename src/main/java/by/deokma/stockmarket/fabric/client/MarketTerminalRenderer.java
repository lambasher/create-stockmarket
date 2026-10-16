package by.deokma.stockmarket.fabric.client;

import by.deokma.stockmarket.block.MarketTerminalBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.Random;

/**
 * Renders an animated ender eye in the centre of the Market Terminal block.
 * <p>
 * The block model is rendered normally by the vanilla pipeline (RenderShape.MODEL).
 * This BER only adds the eye on top.
 * <p>
 * Every 2 seconds (40 ticks) the eye picks a new random rotation axis and
 * smoothly spins 360° around it.
 */
public class MarketTerminalRenderer implements BlockEntityRenderer<MarketTerminalBlockEntity> {

    private static final ResourceLocation EYE_TEXTURE =
            new ResourceLocation("minecraft", "textures/item/ender_eye.png");

    // Centre of the glass cage: upper frame y=8..14 → centre y=11 → 11/16 ≈ 0.6875
    private static final float EYE_Y = 0.6875f;
    private static final float EYE_SIZE = 0.22f;

    // Bobbing
    private static final float BOB_SPEED = 0.05f;
    private static final float BOB_HEIGHT = 0.04f;

    // Spin: one full 360° per 40 ticks (2 s)
    private static final int SPIN_PERIOD_TICKS = 40;

    private final Random rng = new Random();
    private float axX = 0, axY = 1, axZ = 0;
    private long lastCycle = -1;

    public MarketTerminalRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public void render(MarketTerminalBlockEntity be, float partialTick,
                       PoseStack pose, MultiBufferSource buffers,
                       int packedLight, int packedOverlay) {

        long gameTime = be.getLevel() != null ? be.getLevel().getGameTime() : 0;
        long cycle = gameTime / SPIN_PERIOD_TICKS;

        if (cycle != lastCycle) {
            lastCycle = cycle;
            long seed = be.getBlockPos().asLong() ^ (cycle * 0x9E3779B97F4A7C15L);
            rng.setSeed(seed);
            float theta = rng.nextFloat() * Mth.TWO_PI;
            float phi = rng.nextFloat() * Mth.PI;
            axX = Mth.sin(phi) * Mth.cos(theta);
            axY = Mth.cos(phi);
            axZ = Mth.sin(phi) * Mth.sin(theta);
        }

        float progress = ((gameTime % SPIN_PERIOD_TICKS) + partialTick) / SPIN_PERIOD_TICKS;
        float spinDeg = progress * 360f;
        float bob = Mth.sin((gameTime + partialTick) * BOB_SPEED) * BOB_HEIGHT;

        pose.pushPose();
        pose.translate(0.5, EYE_Y + bob, 0.5);

        org.joml.Quaternionf q = new org.joml.Quaternionf()
                .rotationAxis((float) Math.toRadians(spinDeg), axX, axY, axZ);
        pose.mulPose(q);

        // RenderType.eyes gives the glowing effect (ignores block light)
        VertexConsumer vc = buffers.getBuffer(RenderType.eyes(EYE_TEXTURE));
        PoseStack.Pose last = pose.last();
        org.joml.Matrix4f pose4 = last.pose();
        org.joml.Matrix3f normal3 = last.normal();
        float s = EYE_SIZE;

        // Front face
        vc.vertex(pose4, -s, -s, 0).color(255, 255, 255, 255)
                .uv(0, 1).overlayCoords(packedOverlay).uv2(0xF000F0).normal(normal3, 0, 0, 1).endVertex();
        vc.vertex(pose4, s, -s, 0).color(255, 255, 255, 255)
                .uv(1, 1).overlayCoords(packedOverlay).uv2(0xF000F0).normal(normal3, 0, 0, 1).endVertex();
        vc.vertex(pose4, s, s, 0).color(255, 255, 255, 255)
                .uv(1, 0).overlayCoords(packedOverlay).uv2(0xF000F0).normal(normal3, 0, 0, 1).endVertex();
        vc.vertex(pose4, -s, s, 0).color(255, 255, 255, 255)
                .uv(0, 0).overlayCoords(packedOverlay).uv2(0xF000F0).normal(normal3, 0, 0, 1).endVertex();

        // Back face — visible from all sides
        vc.vertex(pose4, s, -s, 0).color(255, 255, 255, 255)
                .uv(0, 1).overlayCoords(packedOverlay).uv2(0xF000F0).normal(normal3, 0, 0, -1).endVertex();
        vc.vertex(pose4, -s, -s, 0).color(255, 255, 255, 255)
                .uv(1, 1).overlayCoords(packedOverlay).uv2(0xF000F0).normal(normal3, 0, 0, -1).endVertex();
        vc.vertex(pose4, -s, s, 0).color(255, 255, 255, 255)
                .uv(1, 0).overlayCoords(packedOverlay).uv2(0xF000F0).normal(normal3, 0, 0, -1).endVertex();
        vc.vertex(pose4, s, s, 0).color(255, 255, 255, 255)
                .uv(0, 0).overlayCoords(packedOverlay).uv2(0xF000F0).normal(normal3, 0, 0, -1).endVertex();

        pose.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(MarketTerminalBlockEntity be) {
        return true;
    }
}
