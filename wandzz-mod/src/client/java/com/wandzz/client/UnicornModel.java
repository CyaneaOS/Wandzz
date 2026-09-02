package com.wandzz.client;

import com.wandzz.Wandzz;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Model jednorozca: geometria konia z vanilla plus rog.
 *
 * <p>Geometrii NIE odziedziczamy po {@code AbstractEquineModel}, tylko ja
 * przepisujemy. Powod jest jeden i bardzo przyziemny: tamte bryly maja UV
 * wprost z arkusza konia (nog (48,21), grzywa (56,36), ogon (42,36)), a arkusz
 * gracza jest zamalowany do x~40 - z tych wskazan model czytalby piksele
 * przezroczyste i jednorozec biegalb bez nog i bez ogona. Pozycje bryl, rozmiary
 * i pivoty sa kopiowane 1:1 z {@code AbstractEquineModel.createBodyMesh};
 * zmienione sa WYLACZNIE wskazniki UV i dolozony rog.
 *
 * <p>Generiki: w zrzucie zrodel Mojang klasy modelowe wygladaja na surowe
 * ({@code EntityModel extends Model}), bo dump gubi sygnatury generyczne.
 * Kompilator oczekuje parametru - {@code EntityModel<UnicornRenderState>}, tak
 * jak w FluffModel i ArcaneSpriteModel.
 *
 * <p>Arkusz: 64x64, sciezka {@code wandzz:textures/entity/unicorn_txt.png}.
 * Tabela UV to ten plik - {@code tools/unicorn_uv.py} go parse'uje i rysuje
 * siatke na kopii tekstury, wiec dokumentacja nie moze sie rozjesc z kodem.
 * Uwaga dla grafika: tuluw (10x10x22) potrzebuje na arkuszu paska
 * 64 px szerokosci x 32 wysokosci (od v=12), a zamalowane jest tylko 0..40.
 * Brakujace kolumny x40..64 to dziury w bokach tuluwia - domaluj je w edytorze,
 * model jest poprawny.
 */
public class UnicornModel extends EntityModel<UnicornRenderState> {

    /** Warstwa modelu; rejestruje ja {@code WandzzClient}. */
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(Wandzz.MOD_ID, "unicorn"), "main");

    /** Stopnie -> radiany ({@code Math.PI / 180F}). */
    private static final float DEG = 0.017453292F;

    /** 30 stopni: kat szyi i ogona w modelu konia ({@code (float)Math.PI / 6F}). */
    private static final float RAD30 = 0.5235988F;

    private final ModelPart headParts;
    private final ModelPart mane;
    private final ModelPart leftEar;
    private final ModelPart rightEar;
    private final ModelPart tail;
    private final ModelPart leftHindLeg;
    private final ModelPart rightHindLeg;
    private final ModelPart leftFrontLeg;
    private final ModelPart rightFrontLeg;

    public UnicornModel(final ModelPart root) {
        // entityCutout (nie translucent): grzywa i ogon maja byc wycinane
        super(root, RenderTypes::entityCutout);
        this.headParts = root.getChild("head_parts");
        this.mane = this.headParts.getChild("mane");
        final ModelPart head = root.getChild("head_parts").getChild("head");
        this.leftEar = head.getChild("left_ear");
        this.rightEar = head.getChild("right_ear");
        this.tail = root.getChild("body").getChild("tail");
        this.leftHindLeg = root.getChild("left_hind_leg");
        this.rightHindLeg = root.getChild("right_hind_leg");
        this.leftFrontLeg = root.getChild("left_front_leg");
        this.rightFrontLeg = root.getChild("right_front_leg");
    }

    /**
     * Siatka. Nazwy czesci sa te same co w vanilla - dzieki temu podpis na
     * podgladzie z {@code tools/unicorn_uv.py} da sie porownywac z dowolnym
     * poradnikiem o modelach koni.
     */
    public static LayerDefinition createBodyLayer() {
        final MeshDefinition mesh = new MeshDefinition();
        final PartDefinition root = mesh.getRoot();

        // Tuluw: geometria jak u konia (10x10x22), ale ROZBITA na dwie bryly po
        // 11 kratek gleboci. Powod jest czysto arkuszowy: jedna bryla 10x10x22
        // rozwija sie do paska 64x32 (2d+2w na 2d+h), wiec na 64x64 nie ma gdzie
        // jej polozyc bez wejscia w przezroczystosc. Dwa pudla moga brac TE SAME
        // piksele (to legalne - MC nie laczy twarzy w jedna siatke), wiec tuluw
        // dostaje ten sam lawendowy pas dwa razy i zadnej dziury.
        final PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 14)
                        .addBox(-5.0F, -8.0F, -17.0F, 10.0F, 10.0F, 11.0F,
                                new CubeDeformation(0.05F))
                        .texOffs(0, 14)
                        .addBox(-5.0F, -8.0F, -6.0F, 10.0F, 10.0F, 11.0F,
                                new CubeDeformation(0.05F)),
                PartPose.offset(0.0F, 11.0F, 5.0F));
        // ogon 3x11x4 (vanilla ma 3x14x4): 3 kratki mniej, zeby cale rozwiniencie
        // miescilo sie w zamalowanym polu arkusza - patrz tools/unicorn_uv.py
        body.addOrReplaceChild("tail",
                CubeListBuilder.create().texOffs(24, 24)
                        .addBox(-1.5F, 0.0F, 0.0F, 3.0F, 11.0F, 4.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(0.0F, -5.0F, 2.0F, RAD30, 0.0F, 0.0F));

        // szyja: wisi na pivocie (0,4,-12) obraconym o 30 stopni - tak jak u
        // konia, dzieki czemu glowa nie jest wklejona w tuluw pod katem prostym
        final PartDefinition headParts = root.addOrReplaceChild("head_parts",
                CubeListBuilder.create().texOffs(13, 20)
                        .addBox(-2.05F, -6.0F, -2.0F, 4.0F, 12.0F, 7.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(0.0F, 4.0F, -12.0F, RAD30, 0.0F, 0.0F));
        final PartDefinition head = headParts.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 18)
                        .addBox(-3.0F, -11.0F, -2.0F, 6.0F, 5.0F, 7.0F, CubeDeformation.NONE),
                PartPose.ZERO);
        head.addOrReplaceChild("left_ear",
                CubeListBuilder.create().texOffs(1, 0)
                        .addBox(0.55F, -13.0F, 4.0F, 2.0F, 3.0F, 1.0F,
                                new CubeDeformation(-0.001F)),
                PartPose.ZERO);
        head.addOrReplaceChild("right_ear",
                CubeListBuilder.create().texOffs(1, 0)
                        .addBox(-2.55F, -13.0F, 4.0F, 2.0F, 3.0F, 1.0F,
                                new CubeDeformation(-0.001F)),
                PartPose.ZERO);
        // ROG (jedyna czesc, ktorej w vanilla nie ma): podstawa 2x5x2 i czubek
        // 1x3x1 - MC nie ma bryl stozkowych, wiec "stozek" sklada sie z dwoch
        // pudel. xRot=0.55 rad pochyla calosc do przodu (dodatni kierunek wokol
        // osi X przenosi czubek w -Z, czyli w strzode pyska). Oba pudela brana
        // TE SAME piksele (43,0): drugie UV wpadloby w nie zamalowana czesc
        // arkusza i czubek by znikal.
        head.addOrReplaceChild("wandzz_horn",
                CubeListBuilder.create().texOffs(31, 24)
                        .addBox(-1.0F, -5.0F, -1.0F, 2.0F, 5.0F, 2.0F, CubeDeformation.NONE)
                        .texOffs(31, 24)
                        .addBox(-0.5F, -8.0F, -0.5F, 1.0F, 3.0F, 1.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(0.0F, -10.0F, -2.0F, 0.55F, 0.0F, 0.0F));
        headParts.addOrReplaceChild("mane",
                CubeListBuilder.create().texOffs(30, 24)
                        .addBox(-1.0F, -11.0F, 5.01F, 2.0F, 16.0F, 2.0F, CubeDeformation.NONE),
                PartPose.ZERO);
        headParts.addOrReplaceChild("upper_mouth",
                CubeListBuilder.create().texOffs(7, 7)
                        .addBox(-2.0F, -11.0F, -7.0F, 4.0F, 5.0F, 5.0F, CubeDeformation.NONE),
                PartPose.ZERO);

        // Cztery nogi na jednym UV (vanilla doklada mirror() dla lewej strony);
        // (20,21) to pole zamalowane lawenda z niebieskimi kopytkami. Wszystkie
        // liczby sa literale - tools/unicorn_uv.py parsuje ten plik i rysuje na
        // ich podstawie siatke UV, a nie wyciagnelby wartosci z operatora ?: ani
        // ze zmiennych pomocniczego helpera.
        // -1.01F w Y i -1.9F w Z to numery z vanilla: mikroskopijny przesuw,
        // ktory rozbija Z-fighting miedzy noga a tuluwiem.
        root.addOrReplaceChild("left_hind_leg",
                CubeListBuilder.create().texOffs(23, 22).mirror()
                        .addBox(-3.0F, -1.01F, -1.0F, 4.0F, 11.0F, 4.0F, CubeDeformation.NONE),
                PartPose.offset(4.0F, 14.0F, 7.0F));
        root.addOrReplaceChild("right_hind_leg",
                CubeListBuilder.create().texOffs(23, 22)
                        .addBox(-1.0F, -1.01F, -1.0F, 4.0F, 11.0F, 4.0F, CubeDeformation.NONE),
                PartPose.offset(-4.0F, 14.0F, 7.0F));
        root.addOrReplaceChild("left_front_leg",
                CubeListBuilder.create().texOffs(23, 22).mirror()
                        .addBox(-3.0F, -1.01F, -1.9F, 4.0F, 11.0F, 4.0F, CubeDeformation.NONE),
                PartPose.offset(4.0F, 14.0F, -10.0F));
        root.addOrReplaceChild("right_front_leg",
                CubeListBuilder.create().texOffs(23, 22)
                        .addBox(-1.0F, -1.01F, -1.9F, 4.0F, 11.0F, 4.0F, CubeDeformation.NONE),
                PartPose.offset(-4.0F, 14.0F, -10.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    /**
     * Animacja koniowata: przekatne pary nog, klebiaca grzywa i ogon, glowa
     * patrzaca za graczem z pompujacy karkiem przy biegu.
     *
     * <p>Liczby sa przepisane z {@code AbstractEquineModel.setupAnim} i
     * {@code QuadrupedModel.setupAnim}: 0.6662F na cykl kroku, 0.8F zamach
     * przednich nog, 0.5F tylnych, mnoznik 0.2F w wodzie, dociecie yaw glowy do
     * +-20 stopni, ogon +0.75F do gory przy tempie. Przepisane, a nie
     * odziedziczone, bo vanilla liczy to ze stanu {@code EquineRenderState}
     * (rearing, jedzenie, siodlo, ageScale zalezne od jezdca), a nasz jednorozec
     * nie jest {@code Horse} i zadnej z tych wartosci nie wypelni - zostalyby
     * zera i model udawalby murek.
     *
     * <p>Chod bierze sie z {@code state.walkAnimationPos/Speed}, ktore
     * {@code LivingEntityRenderer.extractRenderState} wypelnia z
     * {@code entity.walkAnimation} - czyli za darmo, dla kazdej encji nalezacej
     * do LivingEntity (patrz tez komentarz w FluffModel).
     */
    @Override
    public void setupAnim(final UnicornRenderState state) {
        // super.setupAnim = Model#setupAnim, ktory wywoluja resetPose(): kaia
        // klatka startuje od pozycji z parta, wiec mozna tu spokojnie dokladac
        super.setupAnim(state);
        final float tempo = state.walkAnimationSpeed;
        final float faza = state.walkAnimationPos;
        final float woda = state.isInWater ? 0.2F : 1.0F;
        final float krok = Mth.cos(woda * faza * 0.6662F + (float) Math.PI) * tempo;

        this.leftFrontLeg.xRot = krok * 0.8F;
        this.rightFrontLeg.xRot = -krok * 0.8F;
        this.leftHindLeg.xRot = -krok * 0.5F;
        this.rightHindLeg.xRot = krok * 0.5F;

        float pitch = state.xRot * DEG;
        if (tempo > 0.2F) {
            pitch += Mth.cos(faza * 0.8F) * 0.15F * tempo;
        }
        this.headParts.xRot = RAD30 + pitch;
        this.headParts.yRot = Mth.clamp(state.yRot, -20.0F, 20.0F) * DEG;

        // uszy: leniwe drganie w bezruchu; przy biegu zamiera, zeby nie migotalo
        final float drganie = tempo < 0.02F ? Mth.cos(state.ageInTicks * 0.06F) : 0.0F;
        this.leftEar.yRot = drganie * 0.25F;
        this.rightEar.yRot = -drganie * 0.25F;

        this.tail.xRot = RAD30 + tempo * 0.75F;
        this.tail.yRot = tempo > 0.5F ? Mth.cos(state.ageInTicks * 0.7F) : 0.0F;
        this.tail.z += tempo * 2.0F;
        this.mane.xRot = tempo * 0.2F;

        // oskubany: "wlosem" jednorozca sa grzywa i ogon, wiec znika one, a nie
        // tuluw; rog zostaje, bo to kosc, nie sierosc
        this.mane.visible = !state.shorn;
        this.tail.visible = !state.shorn;
    }
}
