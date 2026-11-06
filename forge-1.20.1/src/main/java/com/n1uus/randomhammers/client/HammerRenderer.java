package com.n1uus.randomhammers.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.n1uus.randomhammers.HammerConfig;
import com.n1uus.randomhammers.HammerEntity;
import com.n1uus.randomhammers.RandomHammers;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Рисует молоток двумя раскрашенными кубами: голова 3 блока шириной + ручка по центру. */
public class HammerRenderer extends EntityRenderer<HammerEntity> {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(new ResourceLocation(RandomHammers.MODID, "hammer"), "main");
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(RandomHammers.MODID, "textures/entity/hammer.png");

    private final ModelPart head;
    private final ModelPart handle;

    public HammerRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        ModelPart root = ctx.bakeLayer(LAYER);
        head = root.getChild("head");
        handle = root.getChild("handle");
        shadowRadius = 1.2F;
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // ноги на y = 24 (как у ванильных моделей); вверх — отрицательный y
        root.addOrReplaceChild("handle", CubeListBuilder.create().texOffs(0, 40)
                .addBox(-4F, -32F, -4F, 8F, 32F, 8F), PartPose.offset(0F, 24F, 0F));
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-24F, -48F, -8F, 48F, 16F, 16F), PartPose.offset(0F, 24F, 0F));
        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void render(HammerEntity e, float entityYaw, float partialTick, PoseStack ps,
                       MultiBufferSource buffers, int light) {
        float sx = HammerConfig.SIZE_X.get().floatValue();
        float sy = HammerConfig.SIZE_Y.get().floatValue();
        float t = (e.tickCount + partialTick) / 20.0F; // секунды

        ps.pushPose();
        ps.scale(sx, sy, sx);
        ps.mulPose(Axis.YP.rotationDegrees(180.0F - Mth.rotLerp(partialTick, e.yBodyRotO, e.yBodyRot)));
        // вращение вокруг центра молотка (1.5 блока над землёй)
        ps.translate(0.0, 1.5, 0.0);
        ps.mulPose(Axis.XP.rotationDegrees(HammerConfig.ROTATION_X.get().floatValue()
                + HammerConfig.SPIN_X.get().floatValue() * t));
        ps.mulPose(Axis.YP.rotationDegrees(HammerConfig.ROTATION_Y.get().floatValue()
                + HammerConfig.SPIN_Y.get().floatValue() * t));
        ps.translate(0.0, -1.5, 0.0);
        ps.scale(-1.0F, -1.0F, 1.0F);
        ps.translate(0.0, -1.501, 0.0);

        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        int overlay = LivingEntityRenderer.getOverlayCoords(e, 0.0F);
        draw(head, e.getHeadColor(), ps, vc, light, overlay);
        draw(handle, e.getHandleColor(), ps, vc, light, overlay);
        ps.popPose();

        super.render(e, entityYaw, partialTick, ps, buffers, light);
    }

    private static void draw(ModelPart part, int rgb, PoseStack ps, VertexConsumer vc, int light, int overlay) {
        part.render(ps, vc, light, overlay,
                ((rgb >> 16) & 0xFF) / 255.0F, ((rgb >> 8) & 0xFF) / 255.0F, (rgb & 0xFF) / 255.0F, 1.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(HammerEntity e) {
        return TEXTURE;
    }
}
