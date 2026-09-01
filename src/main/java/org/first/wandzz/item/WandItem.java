package org.first.wandzz.item;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;


public class WandItem extends Item {

    public WandItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult use(
            World world,
            PlayerEntity player,
            Hand hand
    ) {

        if (!world.isClient()) {

            System.out.println(
                    "Różdżka użyta!"
            );

        }


        return ActionResult.SUCCESS;
    }
}

