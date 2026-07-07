package net.flamgop.tlc.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.flamgop.tlc.TridentsLoadChunks;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.storage.loot.predicates.WeatherCheck;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Enchantments.class)
public class EnchantmentsMixin {
    @WrapOperation(
            method = "bootstrap",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/storage/loot/predicates/WeatherCheck$Builder;setThundering(Z)Lnet/minecraft/world/level/storage/loot/predicates/WeatherCheck$Builder;",
                    ordinal = 0
            )
    )
    private static WeatherCheck.Builder channelingWeatherCheckPostAttack(WeatherCheck.Builder instance, boolean thundering, Operation<WeatherCheck.Builder> original) {
        if (TridentsLoadChunks.instance().config().lightningParity()) return instance.setRaining(true);
        else return original.call(instance, thundering);
    }

    @WrapOperation(
            method = "bootstrap",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/storage/loot/predicates/WeatherCheck$Builder;setThundering(Z)Lnet/minecraft/world/level/storage/loot/predicates/WeatherCheck$Builder;",
                    ordinal = 1
            )
    )
    private static WeatherCheck.Builder channelingWeatherCheckHitBlock(WeatherCheck.Builder instance, boolean thundering, Operation<WeatherCheck.Builder> original) {
        if (TridentsLoadChunks.instance().config().lightningParity()) return instance.setRaining(true);
        else return original.call(instance, thundering);
    }
}
