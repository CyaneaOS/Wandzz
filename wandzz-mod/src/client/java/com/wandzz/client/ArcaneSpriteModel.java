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

/**
 * Model ducha arkanu: bryl tulowia 5x6x7, glowa 4x4x3, dwa skrzydla 5x2x4,
 * ogon 2x2x4 - mapa 32x32, jedna warstwa.
 *
 * Rozklad UV (to jest kontrakt na teksture
 * {@code wandzz:textures/entity/arcane_sprite.png}, ktora MA MIEC 32x32 - przy
 * innej wielkosci gra skaluje UV i ksztalt sie rozjedzie):
 *   body  texOffs(0,0)   glowa texOffs(0,13)
 *   lewe  texOffs(14,13) prawe texOffs(14,19)   ogon texOffs(0,20)
 *
 * Warstwe rejestruje {@code WandzzClient} przez
 * {@code EntityModelLayerRegistry.registerModelLayer}, a NIE przez
 * {@code ModelLayers} - ta klasa jest vanilla i ma prywatny rejestr.
 */
public class ArcaneSpriteModel extends EntityModel<ArcaneSpriteRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(Wandzz.MOD_ID, "arcane_sprite"), "main");

    /** Stopnie -> radiany, zeby nie powtarzac tej stalej w czterech miejscach. */
    private static final float DEG = 0.017453292F;

    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart leftWing;
    private final ModelPart rightWing;
    private final ModelPart tail;

    public ArcaneSpriteModel(final ModelPart root) {
        // entityCutout (nie NoCull): liscie za dukem maja go zaslanac, inaczej
        // duh przejswieca przez korone jak rentgen
        super(root, RenderTypes::entityCutout);
        this.body = root.getChild("body");
        this.head = this.body.getChild("head");
        this.leftWing = this.body.getChild("left_wing");
        this.rightWing = this.body.getChild("right_wing");
        this.tail = this.body.getChild("tail");
    }

    public static LayerDefinition createBodyLayer() {
        final MeshDefinition mesh = new MeshDefinition();
        final PartDefinition rootDef = mesh.getRoot();

        final PartDefinition bodyDef = rootDef.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0).addBox(-2.5F, -3.0F, -3.5F, 5.0F, 6.0F, 7.0F),
                PartPose.offset(0.0F, 20.0F, 0.0F));
        bodyDef.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 13).addBox(-2.0F, -3.0F, -2.0F, 4.0F, 4.0F, 3.0F),
                PartPose.offset(0.0F, -1.0F, -3.5F));
        bodyDef.addOrReplaceChild("left_wing",
                CubeListBuilder.create().texOffs(14, 13).addBox(0.0F, -1.0F, -2.0F, 5.0F, 2.0F, 4.0F),
                PartPose.offset(2.5F, -1.0F, 0.0F));
        bodyDef.addOrReplaceChild("right_wing",
                CubeListBuilder.create().texOffs(14, 19).addBox(-5.0F, -1.0F, -2.0F, 5.0F, 2.0F, 4.0F),
                PartPose.offset(-2.5F, -1.0F, 0.0F));
        bodyDef.addOrReplaceChild("tail",
                CubeListBuilder.create().texOffs(0, 20).addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 4.0F),
                PartPose.offset(0.0F, 1.0F, 3.5F));

        return LayerDefinition.create(mesh, 32, 32);
    }

    @Override
    public void setupAnim(final ArcaneSpriteRenderState state) {
        super.setupAnim(state);

        // w locie pelne machniecie, przy wiszeniu tylko drzenie skrzydel
        final float swing = (state.perched ? 0.12F : 1.0F) * state.wingPhase;
        this.leftWing.zRot = 0.45F + swing * 0.55F;
        this.rightWing.zRot = -0.45F - swing * 0.55F;
        this.leftWing.yRot = swing * 0.2F;
        this.rightWing.yRot = -swing * 0.2F;

        this.head.yRot = state.yRot * DEG;
        this.tail.yRot = swing * 0.25F;

        // wiszacy duch wisi GLowA W DOL (jak nietoperz) - to jest caly komunikat sylwetkowy: "to drzewo jest magiczne"
        final float hang = state.perched ? 2.6F : 0.0F;
        this.body.xRot = hang;
        this.head.xRot = -hang * 0.55F;
    }
}
