package net.gudenau.deathbag.mixin;

import net.gudenau.deathbag.PlayerData;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends LivingEntity {
    @Shadow @Final private PlayerInventory inventory;
    
    @SuppressWarnings("ConstantConditions")
    protected PlayerEntityMixin() {
        super(null, null);
    }
    
    @Inject(
        method = "dropInventory",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/player/PlayerInventory;dropAll()V"
        ),
        cancellable = true
    )
    private void dropInventory(CallbackInfo ci) {
        if (!inventory.isEmpty() && !world.isClient()) {
            PlayerData.collectInventory((PlayerEntity) (Object) this);
            inventory.clear();
        }
        ci.cancel();
    }
}
