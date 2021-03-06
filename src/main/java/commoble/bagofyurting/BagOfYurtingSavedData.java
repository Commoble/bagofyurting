package commoble.bagofyurting;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.world.storage.WorldSavedData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.util.HashMap;

public class BagOfYurtingSavedData extends WorldSavedData
{
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String NBT_KEY = "dataName";
    private BagOfYurtingData data;

    public BagOfYurtingSavedData(String name)
    {
        super(name);
    }

    public void setData(BagOfYurtingData data)
    {
        this.data = data;
        markDirty();
    }

    public BagOfYurtingData getDataOrDefault()
    {
        return data == null ? new BagOfYurtingData(new HashMap<>()) : data;
    }

    @Nonnull
    @Override
    public CompoundNBT write(@Nonnull CompoundNBT compound)
    {
        if (data == null)
        {
            LOGGER.warn("Tried to save BagOfYurtingSavedData without having data to save...");
        }
        else
        {
            data.writeIntoNBT(compound);
        }
        return compound;
    }

    @Override
    public void read(@Nonnull CompoundNBT nbt)
    {
        this.data = BagOfYurtingData.read(nbt);
    }

    public void writeNameIntoNbt(@Nonnull CompoundNBT nbt)
    {
        nbt.putString(NBT_KEY, getName());
    }

    public static BagOfYurtingSavedData create(String id)
    {
        return new BagOfYurtingSavedData(formatSavedDataName(id));
    }

    public static String formatSavedDataName(String id)
    {
        return "bagofyurting#" + id;
    }

    public static boolean doesNBTContainDataId(CompoundNBT nbt) {
        return !nbt.getString(NBT_KEY).isEmpty();
    }

    public static String getId(@Nonnull CompoundNBT nbt)
    {
        return nbt.getString(NBT_KEY);
    }

    public static void cleanIdFromNBT(@Nonnull CompoundNBT compoundNBT) {
        compoundNBT.remove(NBT_KEY);
    }
}
