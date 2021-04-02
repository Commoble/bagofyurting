package commoble.bagofyurting.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.annotation.Nullable;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import commoble.bagofyurting.BagOfYurtingData;
import commoble.bagofyurting.BagOfYurtingMod;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.FolderName;

/**
 * This class manages storage of {@link BagOfYurtingData}. Should be called only on server side
 */
public class StorageManager
{
    private static final Logger LOGGER = LogManager.getLogger(StorageManager.class);
    private static final Queue<Pair<String, BagOfYurtingData>> dataToSave = new LinkedBlockingQueue<>();
    private static final Map<String, BagOfYurtingData> dirtyMap = new HashMap<>();
    private static final Queue<String> dataToRemove = new LinkedBlockingQueue<>();

    public static void save(String id, BagOfYurtingData data)
    {
        dataToSave.add(Pair.of(id, data));
        dirtyMap.put(id, data);
        // Ensure that generated id will not be removed in onSave
        dataToRemove.remove(id);
    }

    public static void remove(String id)
    {
        dirtyMap.remove(id);
        // If id is queued to save, then cancel it and skip deleting
        if (!dataToSave.removeIf(pair -> pair.getLeft().equals(id)))
        {
            dataToRemove.add(id);
        }
    }

    @Nullable
    public static BagOfYurtingData load(MinecraftServer server, String id)
    {
        if (dirtyMap.containsKey(id))
        {
            return dirtyMap.get(id);
        }

        Path directory = getSaveDirectory(server);
        if (directory == null)
        {
            return null;
        }
        else
        {
            Path file = directory.resolve(id + ".dat");
            if (!Files.exists(file))
            {
                return null;
            }

            CompoundNBT nbt;
            try
            {
                nbt = CompressedStreamTools.readCompressed(file.toFile());
            }
            catch (IOException e)
            {
            	LOGGER.error("Unable to load save data for Bag of Yurting:", e);
                return null;
            }
            return BagOfYurtingData.read(nbt);
        }
    }

    public static boolean has(MinecraftServer server, String id)
    {
        if (dirtyMap.containsKey(id))
        {
            return true;
        }

        Path directory = getSaveDirectory(server);
        if (directory == null)
        {
            return false;
        }
        else
        {
            return Files.exists(directory.resolve(id + ".dat"));
        }
    }

    public static void onSave(ServerWorld world)
    {
        profile(world, "onSave", () ->
        {
            Path saveDirectory = getSaveDirectory(world.getServer());
            if (saveDirectory == null)
            {
                return;
            }

            profile(world, "saveQueued", () -> saveQueued(saveDirectory));
            profile(world, "removeQueued", () -> removeQueued(saveDirectory));
        });
    }

    private static void saveQueued(Path saveDirectory)
    {
        List<Pair<String, BagOfYurtingData>> toReSave = new ArrayList<>(dataToSave.size());
        while (!dataToSave.isEmpty())
        {
            Pair<String, BagOfYurtingData> pair = dataToSave.poll();
            Path file = saveDirectory.resolve(pair.getLeft() + ".dat");
            CompoundNBT nbt = new CompoundNBT();
            pair.getRight().writeIntoNBT(nbt);
            try
            {
                CompressedStreamTools.writeCompressed(nbt, file.toFile());
                dirtyMap.remove(pair.getLeft());
            }
            catch (IOException e)
            {
                LOGGER.error("Could not save Bag of Yurting data with id: " + pair.getLeft());
                toReSave.add(pair);
            }
        }
        dataToSave.addAll(toReSave);
    }

    private static void removeQueued(Path saveDirectory)
    {
        List<String> failedRemove = new ArrayList<>(dataToRemove.size());
        while (!dataToRemove.isEmpty())
        {
            String id = dataToRemove.poll();
            Path file = saveDirectory.resolve(id + ".dat").toAbsolutePath();
            try
            {
                Files.delete(file);
            }
            catch (IOException e)
            {
                LOGGER.error("Could not delete Bag of Yurting data with id: " + id, e);
                failedRemove.add(id);
            }
        }
        dataToRemove.addAll(failedRemove);
    }

    @Nullable
    private static Path getSaveDirectory(MinecraftServer server)
    {
        Path dir = server.func_240776_a_(new FolderName(BagOfYurtingMod.MODID));
        if (!Files.exists(dir))
        {
            try
            {
                Files.createDirectory(dir);
            }
            catch (IOException e)
            {
            	LOGGER.error("Failed to create Bag of Yurting mod folder: " + dir, e);
            	return null;
            }
        }
        return dir;
    }

    private static void profile(ServerWorld world, String name, Runnable runnable)
    {
        world.getProfiler().startSection(BagOfYurtingMod.MODID + "#" + name);
        runnable.run();
        world.getProfiler().endSection();
    }
}
