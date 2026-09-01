package org.first.wandzz.client.input;


import org.first.wandzz.client.CastingData;
import org.lwjgl.glfw.GLFW;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.item.ItemStack;
import org.first.wandzz.item.ModItems;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MouseInput {

    private static final Logger log = LoggerFactory.getLogger(MouseInput.class);
    private static boolean wasPressed = false;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null) {
                ItemStack stack = client.player.getMainHandStack();

                if (stack.isOf(ModItems.WAND)){
                    long window = client.getWindow().getHandle();
                    boolean pressed = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;

                    if (!wasPressed && pressed) {
                        CastingData.startCasting();

                    }
                    else if (wasPressed && !pressed) {
                        CastingData.stopCasting();
                        double firstx = CastingData.getPointsCopy().getFirst().x;
                        double lastx = CastingData.getPointsCopy().getLast().x;
                        double distance = lastx - firstx;
                        if (distance > 100) {
                            System.out.println("fireball");
                        }
                        CastingData.clearPoints();

                    }
                    wasPressed = pressed;

                    if (CastingData.isCasting()) {
                        double x = client.mouse.getX();
                        double y = client.mouse.getY();

                        CastingData.addPoint(x, y);
                        System.out.println(CastingData.getPointCount());
                    }
                }
            }
        });


    }

}
