// Made with Blockbench 5.1.6
// Exported for Minecraft version 1.17+ for Yarn
// Paste this class into your mod and generate all required imports
public class unicorn_mob extends EntityModel<Entity> {
	private final ModelPart noga lewa gorna;
	private final ModelPart noga lewa tylnia gorna;
	private final ModelPart noga prawa tylnia gorna;
	private final ModelPart noga prawa tylnia dolna;
	private final ModelPart noga lewa dolna;
	private final ModelPart noga lewa tylnia dolna;
	private final ModelPart glowa;
	private final ModelPart ogon;
	private final ModelPart cialo;
	private final ModelPart noga prawa gorna;
	private final ModelPart noga prawa dolna;
	private final ModelPart wlosy;
	private final ModelPart szyja;
	public unicorn_mob(ModelPart root) {
		this.noga lewa gorna = root.getChild("noga lewa gorna");
		this.noga lewa tylnia gorna = root.getChild("noga lewa tylnia gorna");
		this.noga prawa tylnia gorna = root.getChild("noga prawa tylnia gorna");
		this.noga prawa tylnia dolna = root.getChild("noga prawa tylnia dolna");
		this.noga lewa dolna = root.getChild("noga lewa dolna");
		this.noga lewa tylnia dolna = root.getChild("noga lewa tylnia dolna");
		this.glowa = root.getChild("glowa");
		this.ogon = root.getChild("ogon");
		this.cialo = root.getChild("cialo");
		this.noga prawa gorna = root.getChild("noga prawa gorna");
		this.noga prawa dolna = this.noga prawa gorna.getChild("noga prawa dolna");
		this.wlosy = root.getChild("wlosy");
		this.szyja = root.getChild("szyja");
	}
	public static TexturedModelData getTexturedModelData() {
		ModelData modelData = new ModelData();
		ModelPartData modelPartData = modelData.getRoot();
		ModelPartData noga lewa gorna = modelPartData.addChild("noga lewa gorna", ModelPartBuilder.create().uv(10, 28).cuboid(-1.0F, 0.0F, -1.0F, 2.0F, 3.0F, 3.0F, new Dilation(0.0F)), ModelTransform.pivot(-2.0F, 18.0F, -5.0F));

		ModelPartData noga lewa tylnia gorna = modelPartData.addChild("noga lewa tylnia gorna", ModelPartBuilder.create().uv(30, 18).cuboid(-5.0F, -6.0F, -2.0F, 2.0F, 3.0F, 3.0F, new Dilation(0.0F)), ModelTransform.pivot(2.0F, 24.0F, 5.0F));

		ModelPartData noga prawa tylnia gorna = modelPartData.addChild("noga prawa tylnia gorna", ModelPartBuilder.create().uv(30, 18).cuboid(3.0F, -9.0F, 8.0F, 2.0F, 3.0F, 3.0F, new Dilation(0.0F)), ModelTransform.pivot(-2.0F, 27.0F, -5.0F));

		ModelPartData noga prawa tylnia dolna = modelPartData.addChild("noga prawa tylnia dolna", ModelPartBuilder.create().uv(30, 18).cuboid(-1.0F, -3.0F, -2.0F, 2.0F, 3.0F, 3.0F, new Dilation(0.0F)), ModelTransform.pivot(2.0F, 24.0F, 5.0F));

		ModelPartData noga lewa dolna = modelPartData.addChild("noga lewa dolna", ModelPartBuilder.create().uv(10, 28).cuboid(-1.0F, -3.0F, -1.0F, 2.0F, 3.0F, 3.0F, new Dilation(0.0F)), ModelTransform.pivot(-2.0F, 24.0F, -5.0F));

		ModelPartData noga lewa tylnia dolna = modelPartData.addChild("noga lewa tylnia dolna", ModelPartBuilder.create().uv(30, 18).cuboid(-3.0F, 5.0F, -5.0F, 2.0F, 3.0F, 3.0F, new Dilation(0.0F)), ModelTransform.pivot(0.0F, 16.0F, 8.0F));

		ModelPartData glowa = modelPartData.addChild("glowa", ModelPartBuilder.create(), ModelTransform.pivot(1.0F, 9.0F, -6.0F));

		ModelPartData cube_r1 = glowa.addChild("cube_r1", ModelPartBuilder.create().uv(36, 35).cuboid(-1.0F, -2.5F, -1.0F, 1.0F, 2.0F, 1.0F, new Dilation(0.0F)), ModelTransform.of(1.0F, -3.0F, -1.0F, 0.3354F, 0.2211F, -0.013F));

		ModelPartData cube_r2 = glowa.addChild("cube_r2", ModelPartBuilder.create().uv(36, 13).cuboid(0.0F, -2.0F, -1.0F, 1.0F, 2.0F, 1.0F, new Dilation(0.0F)), ModelTransform.of(-3.0F, -3.0F, -1.0F, 0.3672F, -0.1185F, -0.1336F));

		ModelPartData cube_r3 = glowa.addChild("cube_r3", ModelPartBuilder.create().uv(36, 7).cuboid(-0.6F, -5.0F, -0.5F, 1.0F, 5.0F, 1.0F, new Dilation(0.0F)), ModelTransform.of(-1.0F, -2.0F, -3.0F, 0.3491F, -0.0873F, -0.0436F));

		ModelPartData cube_r4 = glowa.addChild("cube_r4", ModelPartBuilder.create().uv(0, 18).cuboid(-3.0F, -4.0F, -5.0F, 4.0F, 4.0F, 6.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, 0.0F, 0.0F, 0.4527F, -0.0099F, -0.0184F));

		ModelPartData ogon = modelPartData.addChild("ogon", ModelPartBuilder.create().uv(36, 0).cuboid(-2.0F, 5.0F, 13.0F, 2.0F, 6.0F, 1.0F, new Dilation(0.0F))
		.uv(30, 35).cuboid(-2.0F, 3.0F, 12.0F, 2.0F, 6.0F, 1.0F, new Dilation(0.0F)), ModelTransform.pivot(1.0F, 9.0F, -6.0F));

		ModelPartData cialo = modelPartData.addChild("cialo", ModelPartBuilder.create().uv(0, 0).cuboid(-3.0F, -12.0F, -6.0F, 6.0F, 6.0F, 12.0F, new Dilation(0.0F)), ModelTransform.pivot(0.0F, 24.0F, 0.0F));

		ModelPartData noga prawa gorna = modelPartData.addChild("noga prawa gorna", ModelPartBuilder.create().uv(10, 28).cuboid(-1.0F, -6.0F, -1.0F, 2.0F, 3.0F, 3.0F, new Dilation(0.0F)), ModelTransform.pivot(2.0F, 24.0F, -5.0F));

		ModelPartData noga prawa dolna = noga prawa gorna.addChild("noga prawa dolna", ModelPartBuilder.create().uv(0, 28).cuboid(-1.0F, -3.0F, -1.0F, 2.0F, 3.0F, 3.0F, new Dilation(0.0F)), ModelTransform.pivot(0.0F, 0.0F, 0.0F));

		ModelPartData wlosy = modelPartData.addChild("wlosy", ModelPartBuilder.create(), ModelTransform.pivot(0.0F, 24.0F, 0.0F));

		ModelPartData cube_r5 = wlosy.addChild("cube_r5", ModelPartBuilder.create().uv(30, 27).cuboid(-2.0F, -4.8F, -1.0F, 4.0F, 7.0F, 1.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, -15.0F, -4.0F, 0.4363F, 0.0F, 0.0F));

		ModelPartData szyja = modelPartData.addChild("szyja", ModelPartBuilder.create(), ModelTransform.pivot(0.0F, 24.0F, 0.0F));

		ModelPartData cube_r6 = szyja.addChild("cube_r6", ModelPartBuilder.create().uv(20, 18).cuboid(-1.0F, -5.0F, -1.0F, 2.0F, 7.0F, 3.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, -12.0F, -6.0F, 0.4363F, 0.0F, 0.0F));
		return TexturedModelData.of(modelData, 64, 64);
	}
	@Override
	public void setAngles(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
	}
	@Override
	public void render(MatrixStack matrices, VertexConsumer vertexConsumer, int light, int overlay, float red, float green, float blue, float alpha) {
		noga lewa gorna.render(matrices, vertexConsumer, light, overlay, red, green, blue, alpha);
		noga lewa tylnia gorna.render(matrices, vertexConsumer, light, overlay, red, green, blue, alpha);
		noga prawa tylnia gorna.render(matrices, vertexConsumer, light, overlay, red, green, blue, alpha);
		noga prawa tylnia dolna.render(matrices, vertexConsumer, light, overlay, red, green, blue, alpha);
		noga lewa dolna.render(matrices, vertexConsumer, light, overlay, red, green, blue, alpha);
		noga lewa tylnia dolna.render(matrices, vertexConsumer, light, overlay, red, green, blue, alpha);
		glowa.render(matrices, vertexConsumer, light, overlay, red, green, blue, alpha);
		ogon.render(matrices, vertexConsumer, light, overlay, red, green, blue, alpha);
		cialo.render(matrices, vertexConsumer, light, overlay, red, green, blue, alpha);
		noga prawa gorna.render(matrices, vertexConsumer, light, overlay, red, green, blue, alpha);
		wlosy.render(matrices, vertexConsumer, light, overlay, red, green, blue, alpha);
		szyja.render(matrices, vertexConsumer, light, overlay, red, green, blue, alpha);
	}
}