package net.flamgop.tlc.mixin;

import net.flamgop.tlc.TLCConfig;
import net.flamgop.tlc.TicketTypes;
import net.flamgop.tlc.TridentsLoadChunks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@SuppressWarnings("resource")
@Mixin(ThrownTrident.class)
public abstract class ThrownTridentMixin extends AbstractArrow {

    @Shadow @Final private static EntityDataAccessor<Byte> ID_LOYALTY;

    @Shadow private boolean dealtDamage;
    @Unique private long ticketTimer = 0L;
    @Unique private int prevSectionX = 0;
    @Unique private int prevSectionZ = 0;
    @Unique private boolean hasChanneling;

    protected ThrownTridentMixin(EntityType<? extends AbstractArrow> entityType, Level level) {
        super(entityType, level);
    }

    @Unique
    private void checkForChanneling(Level level, ItemStack stack) {
        level.registryAccess().lookup(Registries.ENCHANTMENT)
                .flatMap(registry -> registry.get(Enchantments.CHANNELING))
                .ifPresentOrElse(
                        ref -> hasChanneling = stack.getEnchantments().getLevel(ref) > 0,
                        () -> hasChanneling = false
                );
    }

    @Inject(method = "<init>(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;)V", at = @At("TAIL"))
    public void checkEnchantInit1(Level level, LivingEntity livingEntity, ItemStack itemStack, CallbackInfo ci) {
        this.checkForChanneling(level, itemStack);
    }
    @Inject(method = "<init>(Lnet/minecraft/world/level/Level;DDDLnet/minecraft/world/item/ItemStack;)V", at = @At("TAIL"))
    public void checkEnchantInit2(Level level, double d, double e, double f, ItemStack itemStack, CallbackInfo ci) {
        this.checkForChanneling(level, itemStack);
    }

    @Override
    protected void onHitBlock(@NonNull BlockHitResult blockHitResult) {
        super.onHitBlock(blockHitResult);
        BlockPos hitPos = blockHitResult.getBlockPos();
        if (TridentsLoadChunks.instance().config().lightningParity() && this.hasChanneling && this.level() instanceof ServerLevel level) {
            BlockState state = this.level().getBlockState(hitPos);
            if (level.isRaining() && hitPos.getY() == level.getHeight(Heightmap.Types.WORLD_SURFACE, hitPos.getX(), hitPos.getZ()) - 1 && state.is(Blocks.LIGHTNING_ROD)) {
                BlockPos lightningPos = hitPos.above(1);
                LightningBolt lightningBolt = EntityType.LIGHTNING_BOLT.create(level, EntitySpawnReason.EVENT);
                if (lightningBolt != null) {
                    lightningBolt.snapTo(Vec3.atBottomCenterOf(lightningPos));
                    lightningBolt.setVisualOnly(false);
                    level.addFreshEntity(lightningBolt);
                }
            }
        }
    }

    @Unique
    private static @Nullable Entity findOwnerIncludingDeadPlayer(ServerLevel serverLevel, UUID uUID) {
        Entity entity = serverLevel.getEntityInAnyDimension(uUID);
        return entity != null ? entity : serverLevel.getServer().getPlayerList().getPlayer(uUID);
    }

    @Unique
    private boolean shouldLoadChunks() {
        if (!(this.level() instanceof ServerLevel sl && this.entityData.get(ID_LOYALTY) > 0)) return false;
        if (this.owner == null || !(findOwnerIncludingDeadPlayer(sl, this.owner.getUUID()) instanceof ServerPlayer player)) return false;
        return player.level() == sl;
    }

    @Inject(method = "tick", at = @At("HEAD"))
    public void tickTicketsPre(CallbackInfo ci) {
        if (this.level() instanceof ServerLevel serverLevel) {
            if (this.isAlive()) {
                TLCConfig config = TridentsLoadChunks.instance().config();
                if (config.preventVoidDeath()) {
                    Entity owner = this.owner != null ? findOwnerIncludingDeadPlayer(serverLevel, this.owner.getUUID()) : null;
                    if (owner instanceof ServerPlayer serverPlayer) {
                        if (this.position().y() < serverPlayer.position().y() && this.position().y() < this.level().getMinY()) {
                            this.dealtDamage = true;
                        }
                    }
                }
                if (this.shouldLoadChunks()) {
                    prevSectionX = SectionPos.blockToSectionCoord(this.position().x());
                    prevSectionZ = SectionPos.blockToSectionCoord(this.position().z());
                }
            }
        }
    }


    @Inject(method = "tick", at = @At("TAIL"))
    public void tickTicketsPost(CallbackInfo ci) {
        if (this.level() instanceof ServerLevel serverLevel) {
            if (this.isAlive() && this.shouldLoadChunks()) {
                Entity owner = this.owner != null ? findOwnerIncludingDeadPlayer(serverLevel, this.owner.getUUID()) : null;
                BlockPos blockPos = BlockPos.containing(this.position());
                if (!(owner instanceof ServerPlayer player)) return;
                TLCConfig config = TridentsLoadChunks.instance().config();
                if (this.chunkPosition().distanceSquared(owner.chunkPosition()) > config.maxLoadDistance() * config.maxLoadDistance()) {
                    if (config.teleport()) {
                        if (this.tryPickup(player)) {
                            player.take(this, 1);
                            this.discard();
                        }
                    }
                    return;
                }
                if ((--this.ticketTimer <= 0L || prevSectionX != SectionPos.blockToSectionCoord(blockPos.getX()) || prevSectionZ != SectionPos.blockToSectionCoord(blockPos.getZ()))) {
                    serverLevel.getChunkSource().addTicketWithRadius(TicketTypes.TRIDENT, this.chunkPosition(), 2);
                    ticketTimer = TicketTypes.TRIDENT.timeout();
                }
            }
        }
    }
}
