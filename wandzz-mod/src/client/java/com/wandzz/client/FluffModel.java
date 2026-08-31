package com.wandzz.client;

import com.wandzz.Wandzz;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Kwadruped 32x32 uzywany przez wszystkie nowe encje moda.
 *
 * Rozklad UV (kontrakt na tekstury {@code wandzz:textures/entity/<mob>.png};
 * kazda MU SI miec 32x32 - przy innej wielkosci gra skaluje UV i ksztalt sie
 * rozjedzie, dokladnie jak przy duchu arkanu):
 *   body texOffs(0,0)   glowa texOffs(0,13)   roek texOffs(16,0)   nogi texOffs(0,20)
 *
 * Skala (boss 1.7x) jest WPEKOWANA w bryly, a nie w PoseStack: 1.21.11 nie ma
 * juz {@code scaleModel}/{@code preRenderCallback}, ktorymi sie to kiedys robilo,
 * a reczne skalowanie macierzy w rendererze psuloby cien. Mnozone sa wymiary i
 * offsety, UV zostaje - dla placeholdera, ktorego teksture robi gracz, to bez
 * znaczenia, a przy wlasnej grafice wystarczy grac wiekszy detal na brylach.
 */
public class FluffModel extends EntityModel<FluffRenderState> {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(Wandzz.MOD_ID, "fluff"), "main");
    public static final ModelLayerLocation LAYER_BOSS = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(Wandzz.MOD_ID, "fluff_boss"), "main");

    /** Stopnie -> radiany (ta sama stale co w ArcaneSpriteModel). */
    private static final float DEG = 0.017453292F;

    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart horn;
    private final ModelPart legFrontLeft;
    private final ModelPart legFrontRight;
    private final ModelPart legBackLeft;
    private final ModelPart legBackRight;

    public FluffModel(final ModelPart root) {
        // entityCutout (nie translucent): siersc ma byc wycinana, nie przezroczysta
        super(root, RenderTypes::entityCutout);
        this.body = root.getChild("body");
        this.head = this.body.getChild("head");
        this.horn = this.head.getChild("horn");
        this.legFrontLeft = root.getChild("leg_front_left");
        this.legFrontRight = root.getChild("leg_front_right");
        this.legBackLeft = root.getChild("leg_back_left");
        this.legBackRight = root.getChild("leg_back_right");
    }

    public static LayerDefinition createBodyLayer() {
        return build(1.0F);
    }

    public static LayerDefinition createBossBodyLayer() {
        return build(1.7F);
    }

    private static LayerDefinition build(final float k) {
        final MeshDefinition mesh = new MeshDefinition();
        final PartDefinition rootDef = mesh.getRoot();

        final PartDefinition bodyDef = rootDef.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-3.5F * k, -4.0F * k, -5.0F * k, 7.0F * k, 6.0F * k, 10.0F * k),
                PartPose.offset(0.0F, 19.0F * k, 0.0F));
        final PartDefinition headDef = bodyDef.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 13)
                        .addBox(-2.0F * k, -3.0F * k, -3.0F * k, 4.0F * k, 5.0F * k, 4.0F * k),
                PartPose.offset(0.0F, -2.5F * k, -5.0F * k));
        headDef.addOrReplaceChild("horn",
                CubeListBuilder.create().texOffs(16, 0)
                        .addBox(-0.5F * k, -4.0F * k, -0.5F * k, 1.0F * k, 4.0F * k, 1.0F * k),
                PartPose.offset(0.0F, -2.0F * k, -1.5F * k));

        addLeg(rootDef, "leg_front_left", 3.0F * k, -4.0F * k, k);
        addLeg(rootDef, "leg_front_right", -3.0F * k, -4.0F * k, k);
        addLeg(rootDef, "leg_back_left", 3.0F * k, 4.0F * k, k);
        addLeg(rootDef, "leg_back_right", -3.0F * k, 4.0F * k, k);
        return LayerDefinition.create(mesh, 32, 32);
    }

    private static void addLeg(final PartDefinition rootDef, final String name,
            final float x, final float z, final float k) {
        rootDef.addOrReplaceChild(name,
                CubeListBuilder.create().texOffs(0, 20)
                        .addBox(-1.0F * k, 0.0F, -1.0F * k, 2.0F * k, 6.0F * k, 2.0F * k),
                PartPose.offset(x, 19.0F * k - 1.0F, z));
    }

    @Override
    public void setupAnim(final FluffRenderState state) {
        super.setupAnim(state);

        // chod liczony z ageInTicks: wlasonie LivingEntityRenderState ma gotowe
        // walking-animation tylko dla encji z wlasnym stanem (CowRenderState...),
        // a my stan dzielimy miedzy trzy typy - stad wlasna sinusoida
        final float swing = Mth.sin(state.ageInTicks * 0.35F) * 0.55F;
        this.legFrontLeft.xRot = swing;
        this.legFrontRight.xRot = -swing;
        this.legBackLeft.xRot = -swing;
        this.legBackRight.xRot = swing;

        this.head.yRot = state.yRot * DEG;
        this.head.xRot = state.xRot * DEG;
        // oskubany jednorozec kladzie rozek i siada nizej - sygnal "odrost czeka"
        this.horn.xRot = state.shorn ? -0.6F : 0.0F;
        if (state.shorn) {
            this.body.y -= 0.4F;
        }
    }
}
