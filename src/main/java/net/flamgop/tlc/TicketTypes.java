package net.flamgop.tlc;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.TicketType;

@SuppressWarnings({"PointlessBitwiseExpression", "unused"})
public class TicketTypes {
    private static final int PERSIST                = 1 << 0;
    private static final int LOAD                   = 1 << 1;
    private static final int SIMULATE               = 1 << 2;
    private static final int KEEP_DIMENSION_ACTIVE  = 1 << 3;
    private static final int EXPIRE_IF_UNLOADED     = 1 << 4;

    public static final TicketType TRIDENT = register("trident", 40L, LOAD | SIMULATE | KEEP_DIMENSION_ACTIVE);

    @SuppressWarnings("SameParameterValue")
    private static TicketType register(String name, long timeout, @TicketType.Flags int flags) {
        return Registry.register(BuiltInRegistries.TICKET_TYPE, name, new TicketType(timeout, flags));
    }

    public static void initialize() {
    }
}
