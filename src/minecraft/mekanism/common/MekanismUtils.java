package mekanism.common;

import ic2.api.Direction;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;

import mekanism.api.EnumColor;
import mekanism.api.IConfigurable;
import mekanism.api.InfuseObject;
import mekanism.api.Object3D;
import mekanism.common.IFactory.RecipeType;
import mekanism.common.Tier.EnergyCubeTier;
import mekanism.common.Tier.FactoryTier;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.network.packet.Packet3Chat;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeDirection;
import net.minecraftforge.liquids.ILiquid;
import net.minecraftforge.liquids.LiquidContainerRegistry;
import net.minecraftforge.liquids.LiquidStack;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;
import cpw.mods.fml.common.network.PacketDispatcher;
import cpw.mods.fml.server.FMLServerHandler;

/**
 * Utilities used by Mekanism. All miscellaneous methods are located here.
 * @author AidanBrady
 *
 */
public final class MekanismUtils
{
	/**
	 * Checks for a new version of Mekanism.
	 */
	public static boolean checkForUpdates(EntityPlayer entityplayer)
	{
		if(Mekanism.updateNotifications && Mekanism.latestVersionNumber != null && Mekanism.recentNews != null)
		{
			if(!Mekanism.latestVersionNumber.equals("null"))
			{
				ArrayList<IModule> list = new ArrayList<IModule>();
				
				for(IModule module : Mekanism.modulesLoaded)
				{
					if(Version.get(Mekanism.latestVersionNumber).comparedState(module.getVersion()) == 1)
					{
						list.add(module);
					}
				}
				
				if(Version.get(Mekanism.latestVersionNumber).comparedState(Mekanism.versionNumber) == 1 || !list.isEmpty())
				{
					entityplayer.addChatMessage(EnumColor.GREY + "------------- " + EnumColor.DARK_BLUE + "[Mekanism]" + EnumColor.GREY + " -------------");
					entityplayer.addChatMessage(EnumColor.GREY + " Using outdated version on one or more modules.");
					
					if(Version.get(Mekanism.latestVersionNumber).comparedState(Mekanism.versionNumber) == 1)
					{
						entityplayer.addChatMessage(EnumColor.INDIGO + " Mekanism: " + EnumColor.DARK_RED + Mekanism.versionNumber);
					}
					
					for(IModule module : list)
					{
						entityplayer.addChatMessage(EnumColor.INDIGO + " Mekanism" + module.getName() + ": " + EnumColor.DARK_RED + module.getVersion());
					}
					
					entityplayer.addChatMessage(EnumColor.GREY + " Consider updating to version " + EnumColor.DARK_GREY + Mekanism.latestVersionNumber);
					entityplayer.addChatMessage(EnumColor.GREY + " New features: " + EnumColor.INDIGO + Mekanism.recentNews);
					entityplayer.addChatMessage(EnumColor.GREY + "------------- " + EnumColor.DARK_BLUE + "[=======]" + EnumColor.GREY + " -------------");
					return true;
				}
				else if(Version.get(Mekanism.latestVersionNumber).comparedState(Mekanism.versionNumber) == -1)
				{
					entityplayer.addChatMessage(EnumColor.DARK_BLUE + "[Mekanism] " + EnumColor.GREY + "Using developer build " + EnumColor.DARK_GREY + Mekanism.versionNumber);
					return true;
				}
			}
			else {
				System.out.println("[Mekanism] Minecraft is in offline mode, could not check for updates.");
			}
		}
		
		return false;
	}
	
	/**
	 * Gets the latest version using getHTML and returns it as a string.
	 * @return latest version
	 */
	public static String getLatestVersion()
	{
		String[] text = getHTML("http://dl.dropbox.com/u/90411166/Mod%20Versions/Mekanism.txt").split(":");
		if(!text[0].contains("UTF-8") && !text[0].contains("HTML") && !text[0].contains("http")) return text[0];
		return "null";
	}
	
	/**
	 * Gets the recent news using getHTML and returns it as a string.
	 * @return recent news
	 */
	public static String getRecentNews()
	{
		String[] text = getHTML("http://dl.dropbox.com/u/90411166/Mod%20Versions/Mekanism.txt").split(":");
		if(text.length > 1 && !text[1].contains("UTF-8") && !text[1].contains("HTML") && !text[1].contains("http")) return text[1];
		return "null";
	}
	
	/**
	 * Returns one line of HTML from the url.
	 * @param urlToRead - URL to read from.
	 * @return HTML text from the url.
	 */
	public static String getHTML(String urlToRead) 
	{
		StringBuilder sb = new StringBuilder();
		URL url;
		HttpURLConnection conn;
		BufferedReader rd;
		String line;
		String result = "";
		try {
			url = new URL(urlToRead);
			conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			while ((line = rd.readLine()) != null) 
			{
				result += line;
				sb.append(line);
			}
			rd.close();
		} catch (Exception e) {
			result = "null";
			System.err.println("[Mekanism] An error occured while connecting to URL '" + urlToRead + ".'");
		}
		return result;
	}
	
	/**
	 * Sends a Packet3Chat packet to the defined player, with the defined message.
	 * @param player - Player to send packet to.
	 * @param msg - message sent to player.
	 */
	public static void sendChatMessageToPlayer(String playerUsername, String msg)
	{
		EntityPlayer player = FMLServerHandler.instance().getServer().getConfigurationManager().getPlayerForUsername(playerUsername);
		Packet3Chat chatPacket = new Packet3Chat(msg);
		
		if(player != null)
		{
			((EntityPlayerMP)player).playerNetServerHandler.sendPacketToPlayer(chatPacket);
		}
	}
	
	/**
	 * Returns the closest teleporter between a selection of one or two.
	 */
	public static Object3D getClosestCoords(Teleporter.Code teleCode, EntityPlayer player)
	{
		if(Mekanism.teleporters.get(teleCode).size() == 1)
		{
			return Mekanism.teleporters.get(teleCode).get(0);
		}
		else {
			int dimensionId = player.worldObj.provider.dimensionId;
			
			Object3D coords0 = Mekanism.teleporters.get(teleCode).get(0);
			Object3D coords1 = Mekanism.teleporters.get(teleCode).get(1);
			
			int distance0 = (int)player.getDistance(coords0.xCoord, coords0.yCoord, coords0.zCoord);
			int distance1 = (int)player.getDistance(coords1.xCoord, coords1.yCoord, coords1.zCoord);
			
			if(dimensionId == coords0.dimensionId && dimensionId != coords1.dimensionId)
			{
				return coords0;
			}
			else if(dimensionId == coords1.dimensionId && dimensionId != coords0.dimensionId)
			{
				return coords1;
			}
			else if(dimensionId == coords0.dimensionId && dimensionId == coords1.dimensionId)
			{
				if(distance0 < distance1)
				{
					return coords0;
				}
				else if(distance0 > distance1)
				{
					return coords1;
				}
			}
			else if(dimensionId != coords0.dimensionId && dimensionId != coords1.dimensionId)
			{
				if(distance0 < distance1)
				{
					return coords0;
				}
				else if(distance0 > distance1)
				{
					return coords1;
				}
			}
		}
		
		return null;
	}
	
	/**
	 * Sends the defined message to all players.
	 * @param msg - message to send
	 */
	public static void sendChatMessageToAllPlayers(String msg)
	{
		PacketDispatcher.sendPacketToAllPlayers(new Packet3Chat(msg));
	}
	
	/**
	 * Checks if the mod doesn't need an update.
	 * @return if mod doesn't need an update
	 */
	public static boolean noUpdates()
	{
		if(Mekanism.latestVersionNumber.contains("null"))
		{
			return true;
		}
		
		if(Mekanism.versionNumber.comparedState(Version.get(Mekanism.latestVersionNumber)) == -1)
		{
			return false;
		}
		
		for(IModule module : Mekanism.modulesLoaded)
		{
			if(module.getVersion().comparedState(Version.get(Mekanism.latestVersionNumber)) == -1)
			{
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * Checks if Minecraft is running in offline mode.
	 * @return if mod is running in offline mode.
	 */
	public static boolean isOffline()
	{
		try {
			new URL("http://www.apple.com").openConnection().connect();
			return true;
		} catch (IOException e)
		{
			return false;
		}
	}
	
	/**
	 * Sets the defined world's time to the defined time.
	 * @param world - world to set time
	 * @param paramInt - hour to set time to
	 */
	public static void setHourForward(World world, int paramInt)
	{
		long l1 = world.getWorldTime() / 24000L * 24000L;
	    long l2 = l1 + 24000L + paramInt * 1000;
	    world.setWorldTime(l2);
	}
	
	/**
	 * Creates a fake explosion at the declared player, with only sounds and effects. No damage is caused to either blocks or the player.
	 * @param entityplayer - player to explode
	 */
	public static void doFakeEntityExplosion(EntityPlayer entityplayer)
	{
		World world = entityplayer.worldObj;
		world.spawnParticle("hugeexplosion", entityplayer.posX, entityplayer.posY, entityplayer.posZ, 0.0D, 0.0D, 0.0D);
		world.playSoundAtEntity(entityplayer, "random.explode", 1.0F, 1.0F);
	}
	
	/**
	 * Creates a fake explosion at the declared coords, with only sounds and effects. No damage is caused to either blocks or the player.
	 * @param world - world where the explosion will occur
	 * @param x - x coord
	 * @param y - y coord
	 * @param z - z coord
	 */
	public static void doFakeBlockExplosion(World world, int x, int y, int z)
	{
		world.spawnParticle("hugeexplosion", x, y, z, 0.0D, 0.0D, 0.0D);
		world.playSound(x, y, z, "random.explode", 1.0F, 1.0F, true);
	}
	
	/**
	 * Copies an ItemStack and returns it with a defined stackSize.
	 * @param itemstack - stack to change size
	 * @param size - size to change to
	 * @return resized ItemStack
	 */
	public static ItemStack getStackWithSize(ItemStack itemstack, int size)
	{
		ItemStack newStack = itemstack.copy();
		newStack.stackSize = size;
		return newStack;
	}
	
	/**
	 * Adds a recipe directly to the CraftingManager that works with the Forge Ore Dictionary.
	 * @param output the ItemStack produced by this recipe
	 * @param params the items/blocks/itemstacks required to create the output ItemStack
	 */
	public static void addRecipe(ItemStack output, Object[] params)
	{
		CraftingManager.getInstance().getRecipeList().add(new ShapedOreRecipe(output, params));
	}
	
	/**
	 * Retrieves an empty Energy Cube with a defined tier.
	 * @param tier - tier to add to the Energy Cube
	 * @return empty energy cube with defined tier
	 */
	public static ItemStack getEnergyCube(EnergyCubeTier tier)
	{
		ItemStack itemstack = ((ItemBlockEnergyCube)new ItemStack(Mekanism.EnergyCube).getItem()).getUnchargedItem(tier);
		return itemstack;
	}
	
	/**
	 * Retrieves a Factory with a defined tier and recipe type.
	 * @param tier - tier to add to the Factory
	 * @param type - recipe type to add to the Factory
	 * @return factory with defined tier and recipe type
	 */
	public static ItemStack getFactory(FactoryTier tier, RecipeType type)
	{
		ItemStack itemstack = new ItemStack(Mekanism.MachineBlock, 1, 5+tier.ordinal());
		((IFactory)itemstack.getItem()).setRecipeType(type.ordinal(), itemstack);
		return itemstack;
	}
	
	/**
	 * Checks if a machine is in it's active state.
	 * @param world
	 * @param x
	 * @param y
	 * @param z
	 * @return if machine is active
	 */
    public static boolean isActive(IBlockAccess world, int x, int y, int z)
    {
    	TileEntity tileEntity = (TileEntity)world.getBlockTileEntity(x, y, z);
    	if(tileEntity != null)
    	{
    		if(tileEntity instanceof IActiveState)
    		{
    			return ((IActiveState)tileEntity).getActive();
    		}
    	}
    	
    	return false;
    }
    
    /**
     * Gets the left side of a certain orientation.
     * @param orientation
     * @return left side
     */
    public static ForgeDirection getLeft(int orientation)
    {
    	switch(orientation)
    	{
    		case 2:
    			return ForgeDirection.EAST;
    		case 3:
    			return ForgeDirection.WEST;
    		case 4:
    			return ForgeDirection.SOUTH;
    		default:
    			return ForgeDirection.NORTH;
    	}
    }
    
    /**
     * Gets the right side of a certain orientation.
     * @param orientation
     * @return right side
     */
    public static ForgeDirection getRight(int orientation)
    {
    	return getLeft(orientation).getOpposite();
    }
    
    /**
     * Checks to see if a specified ItemStack is stored in the Ore Dictionary with the specified name.
     * @param check - ItemStack to check
     * @param oreDict - name to check with
     * @return if the ItemStack has the Ore Dictionary key
     */
    public static boolean oreDictCheck(ItemStack check, String oreDict)
    {
    	boolean hasResource = false;
    	
    	for(ItemStack ore : OreDictionary.getOres(oreDict))
    	{
    		if(ore.isItemEqual(check))
    		{
    			hasResource = true;
    		}
    	}
    	
    	return hasResource;
    }
    
    /**
     * Returns an integer facing that converts a world-based orientation to a machine-based oriention.
     * @param side - world based
     * @param blockFacing - what orientation the block is facing
     * @return machine orientation
     */
    public static int getBaseOrientation(int side, int blockFacing)
    {
    	if(blockFacing == 3 || side == 1 || side == 0)
    	{
    		if(side == 2 || side == 3)
    		{
    			return ForgeDirection.getOrientation(side).getOpposite().ordinal();
    		}
    		
    		return side;
    	}
    	else if(blockFacing == 2)
    	{
    		if(side == 2 || side == 3)
    		{
    			return side;
    		}
    		
    		return ForgeDirection.getOrientation(side).getOpposite().ordinal();
    	}
    	else if(blockFacing == 4)
    	{
    		return getRight(side).ordinal();
    	}
    	else if(blockFacing == 5)
    	{
    		return getLeft(side).ordinal();
    	}
    	
    	return side;
    }
    
    /**
     * Increments the output type of a machine's side.
     * @param config - configurable machine
     * @param side - side to increment output of
     */
    public static void incrementOutput(IConfigurable config, int side)
    {
    	int max = config.getSideData().size()-1;
    	int current = config.getSideData().indexOf(config.getSideData().get(config.getConfiguration()[side]));
    	
    	if(current < max)
    	{
    		config.getConfiguration()[side] = (byte)(current+1);
    	}
    	else if(current == max)
    	{
    		config.getConfiguration()[side] = 0;
    	}
    }
    
    /**
     * Gets the operating ticks required for a machine via it's upgrades.
     * @param speedUpgrade - number of speed upgrades
     * @param def - the original, default ticks required
     * @return max operating ticks
     */
    public static int getTicks(int speedUpgrade, int def)
    {
        return (int) (def * Math.pow(10, (-speedUpgrade/9.0)));
    }
    
    /**
     * Gets the energy required per tick for a machine via it's upgrades.
     * @param speedUpgrade - number of speed upgrades
     * @param energyUpgrade - number of energy upgrades
     * @param def - the original, default energy required
     * @return max energy per tick
     */
    public static int getEnergyPerTick(int speedUpgrade, int energyUpgrade, int def)
    {
        return (int) (def * Math.pow(10, ((speedUpgrade-energyUpgrade)/9.0)));
    }
    
    /**
     * Gets the energy required per tick for a machine via it's upgrades.
     * @param speedUpgrade - number of speed upgrades
     * @param energyUpgrade - number of energy upgrades
     * @param def - the original, default energy required
     * @return max energy per tick
     */
    public static double getEnergyPerTick(int speedUpgrade, int energyUpgrade, double def)
    {
        return (def * Math.pow(10, ((speedUpgrade-energyUpgrade)/9.0)));
    }
    
    /**
     * Gets the maximum energy for a machine via it's upgrades.
     * @param energyUpgrade - number of energy upgrades
     * @param def - original, default max energy
     * @return max energy
     */
    public static double getEnergy(int energyUpgrade, double def)
    {
        return (int) (def * Math.pow(10, (energyUpgrade/9.0)));
    }
    
    /**
     * Places a fake bounding block at the defined location.
     * @param world - world to place block in
     * @param x - x coordinate
     * @param y - y coordinate
     * @param z - z coordinate
     * @param orig - original block
     */
    public static void makeBoundingBlock(World world, int x, int y, int z, Object3D orig)
    {
		world.setBlock(x, y, z, Mekanism.BoundingBlock.blockID);
		
		if(!world.isRemote)
		{
			((TileEntityBoundingBlock)world.getBlockTileEntity(x, y, z)).setMainLocation(orig.xCoord, orig.yCoord, orig.zCoord);
		}
    }
    
    /**
     * Updates a block's light value and marks it for a render update.
     * @param world - world the block is in
     * @param x - x coord
     * @param y - y coord
     * @param z - z coord
     */
    public static void updateBlock(World world, int x, int y, int z)
    {
		world.markBlockForRenderUpdate(x, y, z);
		world.updateAllLightTypes(x, y, z);
    }
    
    /**
     * Converts a ForgeDirection enum value to it's corresponding value in IndustrialCraft's 'Direction.'  Using values()[ordinal()] will not work in this situation,
     * as IC2 uses different values from base MC direction theory.
     * @param side - ForgeDirection value
     * @return Direction value
     */
    public static Direction toIC2Direction(ForgeDirection side)
    {
    	switch(side)
    	{
    		case DOWN:
    			return Direction.YN;
    		case UP:
    			return Direction.YP;
    		case NORTH:
    			return Direction.ZN;
    		case SOUTH:
    			return Direction.ZP;
    		case WEST:
    			return Direction.XN;
    		default:
    			return Direction.XP;
    	}
    }
    
    /**
     * Whether or not a certain block is considered a liquid.
     * @param world - world the block is in
     * @param x - x coordinate
     * @param y - y coordinate
     * @param z - z coordinate
     * @return if the block is a liquid
     */
    public static boolean isLiquid(World world, int x, int y, int z)
    {
    	return getLiquid(world, x, y, z) != null;
    }
    
    /**
     * Gets a liquid from a certain location.
     * @param world - world the block is in
     * @param x - x coordinate
     * @param y - y coordinate
     * @param z - z coordinate
     * @return the liquid at the certain location, null if it doesn't exist
     */
    public static LiquidStack getLiquid(World world, int x, int y, int z)
    {
    	int id = world.getBlockId(x, y, z);
    	int meta = world.getBlockMetadata(x, y, z);
    	
    	if(id == 0)
    	{
    		return null;
    	}
    	
    	if((id == Block.waterStill.blockID || id == Block.waterMoving.blockID) && meta == 0)
    	{
    		return new LiquidStack(Block.waterStill.blockID, LiquidContainerRegistry.BUCKET_VOLUME, 0);
    	}
    	else if((id == Block.lavaStill.blockID || id == Block.lavaMoving.blockID) && meta == 0)
    	{
    		return new LiquidStack(Block.lavaStill.blockID, LiquidContainerRegistry.BUCKET_VOLUME, 0);
    	}
    	else if(Block.blocksList[id] instanceof ILiquid)
    	{
    		ILiquid liquid = (ILiquid)Block.blocksList[id];
    	
    		if(liquid.isMetaSensitive())
    		{
    			return new LiquidStack(liquid.stillLiquidId(), LiquidContainerRegistry.BUCKET_VOLUME, liquid.stillLiquidMeta());
    		}
    		else if(meta == 0)
    		{
    			return new LiquidStack(liquid.stillLiquidId(), LiquidContainerRegistry.BUCKET_VOLUME, 0);
    		}
    	}
    	
    	return null;
    }
    
    /**
     * Gets the liquid ID at a certain location, 0 if there isn't one
     * @param world - world the block is in
     * @param x - x coordinate
     * @param y - y coordinate
     * @param z - z coordinate
     * @return liquid ID
     */
    public static int getLiquidId(World world, int x, int y, int z)
    {
    	int id = world.getBlockId(x, y, z);
    	int meta = world.getBlockMetadata(x, y, z);
    	
    	if(id == 0)
    	{
    		return 0;
    	}
    	
    	if(id == Block.waterStill.blockID || id == Block.waterMoving.blockID)
    	{
    		return Block.waterStill.blockID;
    	}
    	else if(id == Block.lavaStill.blockID || id == Block.lavaMoving.blockID)
    	{
    		return Block.lavaStill.blockID;
    	}
    	else if(Block.blocksList[id] instanceof ILiquid)
    	{
    		ILiquid liquid = (ILiquid)Block.blocksList[id];
    	
			return liquid.stillLiquidId();
    	}
    	
    	return 0;
    }
    
    /**
     * Whether or not a block is a dead liquid.
     * @param world - world the block is in
     * @param x - x coordinate
     * @param y - y coordinate
     * @param z - z coordinate
     * @return if the block is a dead liquid
     */
    public static boolean isDeadLiquid(World world, int x, int y, int z)
    {
    	int id = world.getBlockId(x, y, z);
    	int meta = world.getBlockMetadata(x, y, z);
    	
    	if(id == 0)
    	{
    		return false;
    	}
    	
    	if((id == Block.waterStill.blockID || id == Block.waterMoving.blockID) && meta != 0)
    	{
    		return true;
    	}
    	else if((id == Block.lavaStill.blockID || id == Block.lavaMoving.blockID) && meta != 0)
    	{
    		return true;
    	}
    	else if(Block.blocksList[id] instanceof ILiquid)
    	{
    		ILiquid liquid = (ILiquid)Block.blocksList[id];
    	
    		if(liquid.isMetaSensitive())
    		{
    			return liquid.stillLiquidMeta() != meta || liquid.stillLiquidId() != id;
    		}
    		else if(meta != 0)
    		{
    			return true;
    		}
    	}
    	
    	return false;
    }
    
    /**
     * FML doesn't really do GUIs the way it's supposed to -- opens Electric Chest GUI on client and server.
     * Call this method server-side only!
     * @param player - player to open GUI
     * @param tileEntity - TileEntity of the chest, if it's not an item
     * @param inventory - IInventory of the item, if it's not a block
	 * @param isBlock - whether or not this electric chest is in it's block form
     */
    public static void openElectricChestGui(EntityPlayerMP player, TileEntityElectricChest tileEntity, IInventory inventory, boolean isBlock)
    {
		player.incrementWindowID();
		player.closeInventory();
		int id = player.currentWindowId;
		PacketHandler.sendChestOpenToPlayer(player, tileEntity, 0, id, isBlock);
		player.openContainer = new ContainerElectricChest(player.inventory, tileEntity, inventory, isBlock);
		player.openContainer.windowId = id;
		player.openContainer.addCraftingToCrafters(player);
    }
    
    /**
     * Grabs an inventory from the world's caches, and removes all the world's references to it.
     * @param world - world the cache is stored in
     * @param id - inventory ID to pull
     * @return
     */
    public static DynamicTankCache pullInventory(World world, int id)
    {
    	DynamicTankCache toReturn = Mekanism.dynamicInventories.get(id);
    	Mekanism.dynamicInventories.remove(id);
    	
    	for(Object3D obj : Mekanism.inventoryLocations.get(id))
    	{
    		TileEntityDynamicTank tileEntity = (TileEntityDynamicTank)obj.getTileEntity(world);
    		
    		if(tileEntity != null)
    		{
    			tileEntity.cachedLiquid = null;
    			tileEntity.inventory = new ItemStack[2];
    			tileEntity.inventoryID = -1;
    		}
    	}
    	
    	Mekanism.inventoryLocations.remove(id);
    	
    	return toReturn;
    }
    
    /**
     * Updates a dynamic tank cache with the defined inventory ID with the parameterized values.
     * @param inventoryID
     * @param liquid
     * @param inventory
     * @param tileEntity
     */
    public static void updateCache(int inventoryID, LiquidStack liquid, ItemStack[] inventory, TileEntityDynamicTank tileEntity)
    {
    	if(!Mekanism.dynamicInventories.containsKey(inventoryID))
    	{
    		DynamicTankCache cache = new DynamicTankCache();
    		cache.inventory = inventory;
    		cache.liquid = liquid;
    		
    		Mekanism.dynamicInventories.put(inventoryID, cache);
    		
    		HashSet<Object3D> set = new HashSet<Object3D>();
    		set.add(Object3D.get(tileEntity));
    		
    		Mekanism.inventoryLocations.put(inventoryID, set);
    		
    		return;
    	}
    	
    	Mekanism.dynamicInventories.get(inventoryID).inventory = inventory;
    	Mekanism.dynamicInventories.get(inventoryID).liquid = liquid;
    	
    	if(!Mekanism.inventoryLocations.containsKey(inventoryID))
    	{
    		HashSet<Object3D> set = new HashSet<Object3D>();
    		set.add(Object3D.get(tileEntity));
    		
    		Mekanism.inventoryLocations.put(inventoryID, set);
    		
    		return;
    	}
    	
    	if(!Mekanism.inventoryLocations.get(inventoryID).contains(Object3D.get(tileEntity)))
    	{
    		Mekanism.inventoryLocations.get(inventoryID).add(Object3D.get(tileEntity));
    	}
    }
    
    /**
     * Grabs a unique inventory ID for a dynamic tank.
     * @return unique inventory ID
     */
    public static int getUniqueInventoryID()
    {
    	int id = 0;
    	
    	while(true)
    	{
    		for(Integer i : Mekanism.dynamicInventories.keySet())
    		{
    			if(id == i)
    			{
    				id++;
    				continue;
    			}
    		}
    		
    		return id;
    	}
    }
}
