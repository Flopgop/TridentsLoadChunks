package net.flamgop.tlc.mixin;

import net.flamgop.tlc.TLCConfig;
import net.flamgop.tlc.TicketTypes;
import net.flamgop.tlc.TridentsLoadChunks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.level.Level;
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

    @Unique private long ticketTimer = 0L;
    @Unique private int prevSectionX = 0;
    @Unique private int prevSectionZ = 0;

    protected ThrownTridentMixin(EntityType<? extends AbstractArrow> entityType, Level level) {
        super(entityType, level);
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
        if (this.level() instanceof ServerLevel) {
            if (this.isAlive() && shouldLoadChunks()) {
                prevSectionX = SectionPos.blockToSectionCoord(this.position().x());
                prevSectionZ = SectionPos.blockToSectionCoord(this.position().z());
            }
        }
    }


    @Inject(method = "tick", at = @At("TAIL"))
    public void tickTicketsPost(CallbackInfo ci) {
        if (this.level() instanceof ServerLevel serverLevel) {
            if (this.isAlive() && shouldLoadChunks()) {
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
