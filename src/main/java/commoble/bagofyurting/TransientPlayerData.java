package commoble.bagofyurting;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Class that holds information on players on the server
 * This should only be referenced from the server thread
 * Data should be considered temporary, no data is saved on Level save
 */
public class TransientPlayerData
{
	// players that are currently holding the sprint key
	private static final Set<UUID> playersOverridingSafetyList = new HashSet<UUID>();
	
	public static void setOverridingSafetyList(UUID id, boolean isSprinting)
	{
		if (isSprinting)
		{
			playersOverridingSafetyList.add(id);
		}
		else
		{
			playersOverridingSafetyList.remove(id);
		}
	}
	
	public static boolean isPlayerOverridingSafetyList(UUID id)
	{
		return playersOverridingSafetyList.contains(id);
	}
}