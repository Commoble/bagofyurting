package commoble.bagofyurting.storage;

import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.server.MinecraftServer;

public class DataIdNBTHelper
{
    private static final String ID = "yurtdata_id";

    public static boolean contains(@Nonnull CompoundNBT nbt) {
        return !nbt.getString(ID).isEmpty();
    }

    @Nonnull
    public static String get(@Nonnull CompoundNBT nbt) {
        return nbt.getString(ID);
    }

    public static void set(@Nonnull CompoundNBT nbt, @Nonnull String id) {
        nbt.putString(ID, id);
    }

    @Nullable
    public static String remove(@Nonnull CompoundNBT nbt) {
        String id = nbt.getString(ID);
        if (id.isEmpty())
        {
            return null;
        }
        else
        {
            nbt.remove(ID);
            return id;
        }
    }

    @Nonnull
    public static String generate(MinecraftServer server) {
        UUID id;
        do
        {
            id = UUID.randomUUID();
        }
        while (StorageManager.has(server, id.toString()));
        return id.toString();
    }
}
