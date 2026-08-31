package com.wandzz.item;

import com.wandzz.network.OpenBookPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

/**
 * Ksiega zaklec (wandzz:spell_book) - podrecznik gracza.
 *
 * Swiadomie TYLKO referencja, bez zadnej progresji: ksieza niczego nie
 * odblokowuje i nie ogranicza, bo lista zaklec wynika wylacznie z rdzeni
 * w rozdzce (patrz Spell#isProvidedBy). Dodanie "znajomosci zaklec" oznaczaloby
 * drugi system postepu za jeden ekran, ktory i tak pokazuje to samo.
 *
 * Otwarcie idzie przez serwer (pakiet S2C), tak jak stolik arcaniczny, zeby
 * predykcja klienta nie otwierala okna np. na zamrozonym graczu. Sama tresc
 * okna jest po stronie klienta - patrz SpellBookScreen.
 */
public class SpellBookItem extends Item {

    public SpellBookItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(final Level level, final Player player, final InteractionHand hand) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            ServerPlayNetworking.send(serverPlayer, OpenBookPayload.INSTANCE);
        }
        // SUCCESS = vanilla nie probuje dalej "uzyc" przedmiotu (ksiazka nie ma
        // zadnej innej akcji), a reka i tak macha.
        return InteractionResult.SUCCESS;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(final ItemStack itemStack, final Item.TooltipContext context,
            final TooltipDisplay display, final Consumer<Component> tooltip, final TooltipFlag flag) {

        tooltip.accept(Component.translatable("wandzz.book.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
