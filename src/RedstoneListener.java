// $Id$
/*
 * CraftBook
 * Copyright (C) 2010 sk89q <http://www.sk89q.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
*/

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import olegbl.perlstone32.Perlstone32_1;

import com.sk89q.craftbook.*;
import com.sk89q.craftbook.ic.*;
import lymia.customic.*;
import lymia.perlstone.Perlstone_1_0;
import lymia.plc.PlcLang;
import lymia.util.Tuple2;

/**
 * Event listener for redstone enhancements such as redstone pumpkins and
 * integrated circuits. Redstone hooks for mechanisms are in
 * MechanismListener.
 * 
 * @author sk89q
 * @author Lymia
 */
public class RedstoneListener extends CraftBookDelegateListener
        implements CustomICAccepter, SignPatch.ExtensionListener, 
                   TickExtensionListener {
    
    /**
     * Currently registered ICs
     */
    private Map<String,RegisteredIC> icList = 
            new HashMap<String,RegisteredIC>(32);

    //[TODO]: create an array of instantICs. One for each world type.
    private Set<WorldBlockVector> instantICs = 
            new HashSet<WorldBlockVector>(32);

    private HashMap<String,PlcLang> plcLanguageList = 
            new HashMap<String,PlcLang>();
    
    private boolean checkCreatePermissions = false;
    private boolean redstonePumpkins = true;
    private boolean redstoneNetherstone = false;
    private boolean redstoneICs = true;
    private boolean redstonePLCs = true;
    private boolean redstonePLCsRequirePermission = true;
    private boolean listICs = true;
    private boolean listUnusuableICs = true;
    
    private boolean enableSelfTriggeredICs = true;
    private boolean restrictSelfTriggeredICs = false;
    
    private boolean updateSelfTriggeredICList = false;
    
    /**
     * Construct the object.
     * 
     * @param craftBook
     * @param listener
     */
    public RedstoneListener(CraftBook craftBook, CraftBookListener listener) {
        super(craftBook, listener);
        registerLang("perlstone_v1.0",new Perlstone_1_0());
        registerLang("perlstone32_v1",new Perlstone32_1());
    }

    /**
     * Loads relevant configuration.
     */
    @Override
    public void loadConfiguration() {
        checkCreatePermissions = properties.getBoolean(
                "check-create-permissions", false);

        redstonePumpkins = properties.getBoolean("redstone-pumpkins", true);
        redstoneNetherstone = properties.getBoolean("redstone-netherstone", false);
        redstoneICs = properties.getBoolean("redstone-ics", true);
        redstonePLCs = properties.getBoolean("redstone-plcs", true);
        redstonePLCsRequirePermission = properties.getBoolean(
                "redstone-plcs-require-permission", false);
        
        listICs = properties.getBoolean("enable-ic-list",true);
        listUnusuableICs = properties.getBoolean("ic-list-show-unusuable",true);
        
        enableSelfTriggeredICs = properties.getBoolean("enable-self-triggered-ics",true);
        restrictSelfTriggeredICs = properties.getBoolean("self-triggered-ics-require-premission",false);
        
        if(properties.containsKey("chunk-updated-self-triggered-ic-list"))
        	updateSelfTriggeredICList = properties.getBoolean("chunk-updated-self-triggered-ic-list",false);

        icList.clear();
        
        // Load custom ICs
        if (properties.getBoolean("custom-ics", true)) {
            try {
                CustomICLoader.load("custom-ics.txt", this, plcLanguageList);
                logger.info("Custom ICs for CraftBook loaded");
            } catch (CustomICException e) {
                Throwable cause = e.getCause();
                
                if (cause != null && !(cause instanceof FileNotFoundException)) {
                    logger.log(Level.WARNING,
                            "Failed to load custom IC file: " + e.getMessage());
                }
            }
        }
        
        addDefaultICs();
        
        try {
            Server s = etc.getServer();
            for(int i = 0; i < s.getMCServer().e.length; i++)
            {
	            for(Tuple2<Integer,Integer> chunkCoord:ChunkFinder.getLoadedChunks(s.getMCServer().e[i])) {
	            	World world = new World(s.getMCServer().e[i]);
	                int xs = (chunkCoord.a+1)<<4;
	                int ys = (chunkCoord.b+1)<<4;
	                for(int x=chunkCoord.a<<4;x<xs;x++) 
	                    for(int y=0;y<128;y++) 
	                        for(int z=chunkCoord.b<<4;z<ys;z++) 
	                            if(world.getBlockIdAt(x, y, z)==BlockType.WALL_SIGN)
	                                onSignAdded(world, x,y,z);
	            }
            }
        } catch(Throwable t) {
            System.err.println("Chunk finder failed: "+t.getClass());
            t.printStackTrace();
        }
    }
    
    /**
     * Populate the IC list with the default ICs.
     */
    private void addDefaultICs() {
        if (enableSelfTriggeredICs) {
            internalRegisterIC("MC0020", new MC0020(), ICType.ZISO);
            internalRegisterIC("MC0111", new MC1111(), ICType.ZISO);
            internalRegisterIC("MC0230", new MC1230(), ICType.ZISO);
            internalRegisterIC("MC0420", new MC1420(), ICType.ZISO);
            internalRegisterIC("MC0500", new MC1500(), ICType.ZISO);
            internalRegisterIC("MC0260", new MC1260(false), ICType.ZISO);
            internalRegisterIC("MC0261", new MC1261(false), ICType.ZISO);
            internalRegisterIC("MC0262", new MC1262(false), ICType.ZISO);
            
            internalRegisterIC("MCZ116", new MCX116(), ICType.ZISO);
            internalRegisterIC("MCZ117", new MCX117(), ICType.ZISO);
            internalRegisterIC("MCZ120", new MCX120(), ICType.ZISO);
            internalRegisterIC("MCZ121", new MCX121(), ICType.ZISO);
            internalRegisterIC("MCZ230", new MCX230(), ICType.ZISO);
            internalRegisterIC("MCZ231", new MCX231(), ICType.ZISO);
            internalRegisterIC("MCZ236", new MCX236(), ICType.ZISO);
            internalRegisterIC("MCZ238", new MCX238(), ICType.ZISO);
        }
        
        internalRegisterIC("MC1000", new MC1000(), ICType.SISO);
        internalRegisterIC("MC1001", new MC1001(), ICType.SISO);
        internalRegisterIC("MC1017", new MC1017(), ICType.SISO);
        internalRegisterIC("MC1018", new MC1018(), ICType.SISO);
        internalRegisterIC("MC1020", new MC1020(), ICType.SISO);
        internalRegisterIC("MC1025", new MC1025(), ICType.SISO);
        internalRegisterIC("MC1110", new MC1110(), ICType.SISO);
        internalRegisterIC("MC1111", new MC1111(), ICType.SISO);
        internalRegisterIC("MC1200", new MC1200(), ICType.SISO);
        internalRegisterIC("MC1201", new MC1201(), ICType.SISO);
        internalRegisterIC("MC1202", new MC1202(), ICType.SISO);
        internalRegisterIC("MC1205", new MC1205(), ICType.SISO);
        internalRegisterIC("MC1206", new MC1206(), ICType.SISO);
        internalRegisterIC("MC1230", new MC1230(), ICType.SISO);
        internalRegisterIC("MC1231", new MC1231(), ICType.SISO);
        internalRegisterIC("MC1240", new MC1240(), ICType.SISO);
        internalRegisterIC("MC1241", new MC1241(), ICType.SISO);
        internalRegisterIC("MC1250", new MC1250(), ICType.SISO);
        internalRegisterIC("MC1260", new MC1260(true), ICType.SISO);
        internalRegisterIC("MC1261", new MC1261(true), ICType.SISO);
        internalRegisterIC("MC1262", new MC1262(true), ICType.SISO);
        internalRegisterIC("MC1420", new MC1420(), ICType.SISO);
        internalRegisterIC("MC1500", new MC1500(), ICType.SISO);
        internalRegisterIC("MC1510", new MC1510(), ICType.SISO);
        internalRegisterIC("MC1511", new MC1511(), ICType.SISO);
        internalRegisterIC("MC1512", new MC1512(), ICType.SISO);

        internalRegisterIC("MC2020", new MC2020(), ICType.SI3O);
        
        internalRegisterIC("MC3020", new MC3020(), ICType._3ISO);
        internalRegisterIC("MC3002", new MC3002(), ICType._3ISO);
        internalRegisterIC("MC3003", new MC3003(), ICType._3ISO);
        internalRegisterIC("MC3021", new MC3021(), ICType._3ISO);
        internalRegisterIC("MC3030", new MC3030(), ICType._3ISO);
        internalRegisterIC("MC3031", new MC3031(), ICType._3ISO);
        internalRegisterIC("MC3032", new MC3032(), ICType._3ISO);
        internalRegisterIC("MC3033", new MC3033(), ICType._3ISO);
        internalRegisterIC("MC3034", new MC3034(), ICType._3ISO);
        internalRegisterIC("MC3036", new MC3036(), ICType._3ISO);
        internalRegisterIC("MC3040", new MC3040(), ICType._3ISO);
        internalRegisterIC("MC3101", new MC3101(), ICType._3ISO);
        internalRegisterIC("MC3231", new MC3231(), ICType._3ISO);
        internalRegisterIC("MC4000", new MC4000(), ICType._3I3O);
        internalRegisterIC("MC4010", new MC4010(), ICType._3I3O);
        internalRegisterIC("MC4100", new MC4100(), ICType._3I3O);
        internalRegisterIC("MC4110", new MC4110(), ICType._3I3O);
        internalRegisterIC("MC4200", new MC4200(), ICType._3I3O);

        internalRegisterPLC("MC5000", "perlstone_v1.0", ICType.VIVO);
        internalRegisterPLC("MC5001", "perlstone_v1.0", ICType._3I3O);
        
        internalRegisterPLC("MC5032", "perlstone32_v1", ICType.VIVO);
        internalRegisterPLC("MC5033", "perlstone32_v1", ICType._3I3O);
        
        internalRegisterIC("MCX112", new MCX112(), ICType.SISO);
        internalRegisterIC("MCX116", new MCX116(), ICType.SISO);
        internalRegisterIC("MCX117", new MCX117(), ICType.SISO);
        internalRegisterIC("MCX120", new MCX120(), ICType.SISO);
        internalRegisterIC("MCX121", new MCX121(), ICType.SISO);
        internalRegisterIC("MCX200", new MCX200(), ICType.SISO);
        internalRegisterIC("MCX201", new MCX201(), ICType.SISO);
        internalRegisterIC("MCX202", new MCX202(), ICType.SISO);
        internalRegisterIC("MCX207", new MCX207(), ICType.SISO);
        internalRegisterIC("MCX208", new MCX208(), ICType.SISO);
        internalRegisterIC("MCX209", new MCX209(), ICType.SISO);
        internalRegisterIC("MCX210", new MCX210(), ICType.SISO);
        internalRegisterIC("MCX230", new MCX230(), ICType.SISO);
        internalRegisterIC("MCX231", new MCX231(), ICType.SISO);
        internalRegisterIC("MCX233", new MCX233(), ICType.SISO);
        internalRegisterIC("MCX235", new MCX235(), ICType.SISO);
        internalRegisterIC("MCX236", new MCX236(), ICType.SISO);
        internalRegisterIC("MCX237", new MCX237(), ICType.SISO);
        internalRegisterIC("MCX238", new MCX238(), ICType.SISO);
        internalRegisterIC("MCX242", new MCX242(), ICType.SISO);
        internalRegisterIC("MCX243", new MCX243(), ICType.SISO);
        internalRegisterIC("MCX244", new MCX244(), ICType.SISO);
        internalRegisterIC("MCX245", new MCX245(), ICType.SISO);
        internalRegisterIC("MCX246", new MCX246(), ICType.SISO);
        internalRegisterIC("MCX255", new MCX255(), ICType.SISO);
        internalRegisterIC("MCX256", new MCX256(), ICType.SISO);
        
        internalRegisterIC("MCT233", new MCT233(), ICType._3ISO);
        internalRegisterIC("MCT246", new MCT246(), ICType._3ISO);
        
        internalRegisterIC("MCU113", new MCX113(), ICType.UISO);
        internalRegisterIC("MCU440", new MCX440(), ICType.UISO);
        internalRegisterIC("MCU700", new MCX700(), ICType.UISO);
        internalRegisterIC("MCU705", new MCX705(), ICType.UISO);
        
        internalRegisterIC("MCU701", new MCX701(), ICType.MISO);
        internalRegisterIC("MCU702", new MCX702(), ICType.MISO);
    }

    /**
     * Called when a sign is updated.
     * @param player
     * @param cblock
     * @return
     */
    public boolean onSignChange(Player player, Sign sign) {
    	World world = player.getWorld();
    	
        int type = CraftBook.getBlockID(world,
                sign.getX(), sign.getY(), sign.getZ());
        
        String line2 = sign.getText(1);
        int len = line2.length();
        
        // ICs
        if (line2.length() > 7
                && line2.substring(0, 3).equalsIgnoreCase("[MC") &&
                line2.charAt(7) == ']') {

            // Check to see if the player can even create ICs
            if (checkCreatePermissions
                    && !player.canUseCommand("/makeic")) {
                player.sendMessage(Colors.Rose
                        + "You don't have permission to make ICs.");
                CraftBook.dropSign(world, sign.getX(), sign.getY(), sign.getZ());
                return true;
            }

            String id = line2.substring(1, 7).toUpperCase();
            RegisteredIC ic = icList.get(id);

            if (ic != null) {
            	
                if (ic.isPlc) {
                    if (!redstonePLCs) {
                        player.sendMessage(Colors.Rose + "PLCs are not enabled.");
                        CraftBook.dropSign(world, sign.getX(), sign.getY(), sign.getZ());
                        return false;
                    }
                }
                
                if (!canCreateIC(player, id, ic)) {
                    player.sendMessage(Colors.Rose
                            + "You don't have permission to make " + id + ".");
                    CraftBook.dropSign(world, sign.getX(), sign.getY(), sign.getZ());
                    return true;
                } else {
                	
                	String options = line2.substring(8);
            		switch(options.length())
            		{
            			case 1:
            				break;
            			case 4:
            			case 7:
            				options = options.substring(1);
            				break;
            		}
            		
                	if(len > 9)
                	{
                		int[][] orders = getIOOrder(options);
                		if(orders[0] == null && orders[1] == null)
                		{
                			options = "";
                			//player.sendMessage(Colors.Rose + "Unrecognized options: " + line2.substring(8));
                		}
                		else
                			options = line2.substring(8);
                	}
                	
                    // To check the environment
                    Vector pos = new Vector(sign.getX(), sign.getY(), sign.getZ());
                    SignText signText = new SignText(
                        sign.getText(0), sign.getText(1), sign.getText(2),
                        sign.getText(3));

                    int worldType = world.getType().getId();
                    
                    // Maybe the IC is setup incorrectly
                    String envError = ic.ic.validateEnvironment(worldType, pos, signText);

                    if (signText.isChanged()) {
                        sign.setText(0, signText.getLine1());
                        sign.setText(1, signText.getLine2());
                        sign.setText(2, signText.getLine3());
                        sign.setText(3, signText.getLine4());
                    }

                    if (envError != null) {
                        player.sendMessage(Colors.Rose
                                + "Error: " + envError);
                        CraftBook.dropSign(world, sign.getX(), sign.getY(), sign.getZ());
                        return true;
                    } else {
                        sign.setText(0, ic.ic.getTitle());
                        sign.setText(1, "[" + id + "]"+options);
                    }
                    
                    if(enableSelfTriggeredICs && (ic.type.isSelfTriggered || ic.type.updateOnce) ) {
                        instantICs.add(new WorldBlockVector(worldType, pos));
                    }
                    
                    sign.update();
                }
                
                if (ic.isPlc && !redstonePLCs && redstoneICs) {
                    player.sendMessage(Colors.Rose + "Warning: PLCs are disabled.");
                }
            } else {
                sign.setText(1, Colors.Red + line2);
                player.sendMessage(Colors.Rose + "Unrecognized IC: " + id);
            }

            if (!redstoneICs) {
                player.sendMessage(Colors.Rose + "Warning: ICs are disabled.");
            } else if (type == BlockType.SIGN_POST) {
                player.sendMessage(Colors.Rose + "Warning: IC signs must be on a wall.");
            }

            return false;
        }
        
        return false;
    }
    
    public void onSignShow(Player player, Sign sign)
    {
    	if(!enableSelfTriggeredICs || !updateSelfTriggeredICList || sign.getBlock().getType() != BlockType.WALL_SIGN)
    		return;
    	
    	onSignAdded(player.getWorld().getType().getId(), sign);
    }

    /**
     * Handles the wire input at a block in the case when the wire is
     * directly connected to the block in question only.
     *
     * @param x
     * @param y
     * @param z
     * @param isOn
     */
    public void onDirectWireInput(World world, final Vector pt, boolean isOn, final Vector changed) {
        int type = CraftBook.getBlockID(world, pt);
        
        // Redstone pumpkins
        if (redstonePumpkins
                && (type == BlockType.PUMPKIN || type == BlockType.JACKOLANTERN)) {
            Boolean useOn = Redstone.testAnyInput(world, pt);

            int data = CraftBook.getBlockData(world, pt);
            if (useOn != null && useOn) {
                CraftBook.setBlockID(world, pt, BlockType.JACKOLANTERN);
            } else if (useOn != null) {
                CraftBook.setBlockID(world, pt, BlockType.PUMPKIN);
            }
            CraftBook.setBlockData(world, pt, data);
        // Redstone netherstone
        } else if (redstoneNetherstone
                && (type == BlockType.NETHERSTONE)) {
            Boolean useOn = Redstone.testAnyInput(world, pt);
            Vector above = pt.add(0, 1, 0);

            if (useOn != null && useOn && CraftBook.getBlockID(world, above) == 0) {
                CraftBook.setBlockID(world, above, BlockType.FIRE);
            } else if (useOn != null && !useOn && CraftBook.getBlockID(world, above) == BlockType.FIRE) {
                CraftBook.setBlockID(world, above, 0);
            }
        } else if (type == BlockType.WALL_SIGN
                || type == BlockType.SIGN_POST) {
            ComplexBlock cblock = world.getComplexBlock(
                    pt.getBlockX(), pt.getBlockY(), pt.getBlockZ());

            if (!(cblock instanceof Sign)) {
                return;
            }

            final Sign sign = (Sign)cblock;
            String line2 = sign.getText(1);
            //int len = line2.length();
            
            // ICs
            if (redstoneICs && type == BlockType.WALL_SIGN
                    && line2.length() > 7
                    && line2.substring(0, 3).equalsIgnoreCase("[MC")
                    && line2.charAt(7) == ']') {
            	
                final String id = line2.substring(1, 7).toUpperCase();

                final SignText signText = new SignText(sign.getText(0),
                        sign.getText(1), sign.getText(2), sign.getText(3));

                final RegisteredIC ic = icList.get(id);
                
                if (ic == null) {
                    sign.setText(1, Colors.Red + line2);
                    sign.update();
                    return;
                }
                
                if (ic.type.isSelfTriggered) {
                    return;
                }
                
                
                final char mode;
            	
                String options = line2.substring(8);
        		switch(options.length())
        		{
        			case 1:
        			case 4:
        			case 7:
        				mode = options.charAt(0);
        				options = options.substring(1);
        				break;
        			default:
        				mode = ' ';
        				break;
        		}
                
            	final int[] abc;
        		final int[] def;
        		int[][] orders = new int[][]{null, null};
        		
                if(line2.length() > 9)
            	{
            		orders = getIOOrder(options);
            	}
                
                if(orders[0] == null)
                	abc = new int[]{0, 1, 2};
                else
                	abc = orders[0];
                
                if(orders[1] == null)
                	def = new int[]{0, 1, 2};
                else
                	def = orders[1];
                
                
                final int worldType = world.getType().getId();
                
                final RedstoneListener thisListener = this;
                
                final int worldIndex = CraftBook.getWorldIndex(worldType);
                
                craftBook.getDelay(worldIndex).delayAction(
                        new TickDelayer.Action(world, pt.toBlockVector(), 2) {
                    @Override
                    public void run() {
                    	
                    	//not too happy about putting in the "extra" param to get the blockbag access cuz it's a hack.
                    	//hopefully I can change it later on.
                    	if(id.equals("MCX207") || id.equals("MCX208") || id.equals("MCX209") || id.equals("MCX210"))
                    		ic.think(worldType, pt, changed, signText, sign, craftBook.getDelay(worldIndex), mode, abc, def, listener.getBlockBag(worldType, pt));
                    	else if(id.equals("MCU440") || id.equals("MCU700") || id.equals("MCU701")
                    			|| id.equals("MCU702") || id.equals("MCU705"))
                    		ic.think(worldType, pt, changed, signText, sign, craftBook.getDelay(worldIndex), mode, abc, def, thisListener);
                        else
                        	ic.think(worldType, pt, changed, signText, sign, craftBook.getDelay(worldIndex), mode, abc, def, null);

                        if (signText.isChanged()) {
                            sign.setText(0, signText.getLine1());
                            sign.setText(1, signText.getLine2());
                            sign.setText(2, signText.getLine3());
                            sign.setText(3, signText.getLine4());
                            
                            if (signText.shouldUpdate()) {
                                sign.update();
                            }
                        }
                    }
                });
            }
        }
    }

    public void onTick() {
        if(!enableSelfTriggeredICs) return;
        
        //[TODO]: change when canary gets a "getWorldList" method
        OWorldServer[] worlds = etc.getMCServer().e;
        for(int i = 0; i < worlds.length; i++)
        {
        	World world = new World(worlds[i]);
        	
	        //XXX HACK: Do this in a more proper way later.
	        if(world.getTime()%2!=0) continue;
	        
	        int worldType = world.getType().getId();
	        
	        WorldBlockVector[] bv = this.instantICs.toArray(new WorldBlockVector[0]);
	        
	        for(WorldBlockVector pt:bv) {
	        	if(pt.getWorldType() != worldType)
	        		continue;
	        	
	            Sign sign = (Sign)world.getComplexBlock(pt.getBlockX(), pt.getBlockY(), pt.getBlockZ());
	            if(sign==null) {
	                this.instantICs.remove(pt);
	                continue;
	            }
	            String line2 = sign.getText(1);
	            if(!line2.startsWith("[MC")) {
	                this.instantICs.remove(pt);
	                continue;
	            }
	            
	            String id = line2.substring(1, 7).toUpperCase();
	            RegisteredIC ic = icList.get(id);
	            if (ic == null) {
	                sign.setText(1, Colors.Red + line2);
	                sign.update();
	                this.instantICs.remove(pt);
	                continue;
	            }
	            
	            if(ic.type.updateOnce)
	            {
	            	if(sign.getText(0).charAt(0) != '%')
	            	{
	            		this.instantICs.remove(pt);
	            		
	            		if(sign.getText(0).charAt(0) == '^')
	                		continue;
	            	}
	            }
	            else if(!ic.type.isSelfTriggered) {
	                this.instantICs.remove(pt);
	                continue;
	            }
	
	            SignText signText = new SignText(sign.getText(0),
	                    sign.getText(1), sign.getText(2), sign.getText(3));
	            
	            ic.think(worldType, pt, signText, sign, null);
	            
	            if (signText.isChanged()) {
	                sign.setText(0, signText.getLine1());
	                sign.setText(1, signText.getLine2());
	                sign.setText(2, signText.getLine3());
	                sign.setText(3, signText.getLine4());
	                
	                if (signText.shouldUpdate()) {
	                    sign.update();
	                }
	            }
	        }
        }
    }
    public void onSignAdded(World world, int x, int y, int z) {
        if(!enableSelfTriggeredICs) return;
            
        Sign sign = (Sign)world.getComplexBlock(x,y,z);
        
        onSignAdded(world.getType().getId(), sign);
    }
    
    public void onSignAdded(int worldType, Sign sign)
    {
    	String line2 = sign.getText(1);
        if(!line2.startsWith("[MC") || line2.length() < 8) return;
        
        String id = line2.substring(1, 7).toUpperCase();
        RegisteredIC ic = icList.get(id);
        if (ic == null) {
            sign.setText(1, Colors.Red + line2);
            sign.update();
            return;
        }

        if(!ic.type.isSelfTriggered && !ic.type.updateOnce) return;
        
        instantICs.add(new WorldBlockVector(worldType, sign.getX(),sign.getY(),sign.getZ()));
    }
    
    public boolean onBlockBreak(Player player, Block block)
    {
    	if(player == null || block == null || block.getType() != Block.Type.WallSign.getType())
    		return false;
    	
    	World world = player.getWorld();
    	
    	Sign sign = (Sign)world.getComplexBlock(block);
    	
    	String line2 = sign.getText(1);
    	if(!line2.startsWith("[MC") || line2.length() < 8)
        	return false;
    	
    	String id = line2.substring(1, 7).toUpperCase();
        RegisteredIC ic = icList.get(id);
        if (ic == null)
        	return false;
        
        if(!ic.type.updateOnce)
        	return false;
        
        SignText signText = new SignText(sign.getText(0), line2, sign.getText(2), sign.getText(3));
        Vector pos = new Vector(block.getX(), block.getY(), block.getZ());
        
        String message = ic.ic.clear(world.getType().getId(), pos, signText);
        if(message != null)
        {
        	//player.sendMessage(Colors.Rose + message);
        }
    	
    	return false;
    }
    
    /**
     * Called when a command is run
     *
     * @param player
     * @param split
     * @return whether the command was processed
     */
    @Override
    public boolean onCheckedCommand(Player player, String[] split)
            throws InsufficientArgumentsException,
            LocalWorldEditBridgeException {
        
        if (listICs && split[0].equalsIgnoreCase("/listics")
                && Util.canUse(player, "/listics")) {
            String[] lines = generateICText(player);
            int pages = ((lines.length - 1) / 10) + 1;
            int accessedPage;
            
            try {
                accessedPage = split.length == 1 ? 0 : Integer
                        .parseInt(split[1]) - 1;
                if (accessedPage < 0 || accessedPage >= pages) {
                    player.sendMessage(Colors.Rose + "Invalid page \""
                            + split[1] + "\"");
                    return true;
                }
            } catch (NumberFormatException e) {
                player.sendMessage(Colors.Rose + "Invalid page \"" + split[1]
                        + "\"");
                return true;
            }

            player.sendMessage(Colors.Blue + "CraftBook ICs (Page "
                    + (accessedPage + 1) + " of " + pages + "):");
            
            for (int i = accessedPage * 10; i < lines.length
                    && i < (accessedPage + 1) * 10; i++) {
                player.sendMessage(lines[i]);
            }

            return true;
        }

        return false;
    }

    /**
     * Used for the /listics command.
     * 
     * @param p
     * @return
     */
    private String[] generateICText(Player p) {
        ArrayList<String> icNameList = new ArrayList<String>();
        icNameList.addAll(icList.keySet());

        Collections.sort(icNameList);

        ArrayList<String> strings = new ArrayList<String>();
        for (String ic : icNameList) {
            RegisteredIC ric = icList.get(ic);
            boolean canUse = canCreateIC(p, ic, ric);
            boolean auto = ric.type.isSelfTriggered;
            if (listUnusuableICs) {
                strings.add(Colors.Rose + ic + " (" + ric.type.name + ")"
                        + (auto ? " (SELF-TRIGGERED)" : "") + ": "
                        + ric.ic.getTitle() + (canUse ? "" : " (RESTRICTED)"));
            } else if (canUse) {
                strings.add(Colors.Rose + ic + " (" + ric.type.name + ")"
                        + (auto ? " (SELF-TRIGGERED)" : "") + ": "
                        + ric.ic.getTitle());
            }
        }
        
        return strings.toArray(new String[0]);
    }

    /**
     * Checks if the player can create an IC.
     * 
     * @param player
     * @param id
     * @param ic
     */
    private boolean canCreateIC(Player player, String id, RegisteredIC ic) {
        return (!ic.ic.requiresPermission()
                && !(ic.isPlc && redstonePLCsRequirePermission)
                && !(ic.type.isSelfTriggered && restrictSelfTriggeredICs))
                || player.canUseCommand("/allic")
                || player.canUseCommand("/" + id.toLowerCase());
    }

    /**
     * Register a new IC. Defined by the interface CustomICAccepter.
     * 
     * @param name
     * @param ic
     * @param type
     */
    public void registerIC(String name, IC ic, String type)
            throws CustomICException {
        if (icList.containsKey(name)) {
            throw new CustomICException("IC already defined");
        }
        ICType icType = getICType(type);
        if(!enableSelfTriggeredICs && icType.isSelfTriggered) return;
        
        registerIC(name, ic, icType, false);
    }

    /**
     * Get an IC type from its type name.
     * 
     * @param type
     * @return
     * @throws CustomICException thrown if the type does not exist
     */
    private ICType getICType(String type) throws CustomICException {
        ICType typeObject = ICType.forName(type);
        
        if (typeObject == null) {
            throw new CustomICException("Invalid IC type: " + type);
        }
        
        return typeObject;
    }

    /**
     * Registers an non-PLC IC.
     * 
     * @param name
     * @param ic
     * @param type
     */
    private void internalRegisterIC(String name, IC ic, ICType type) {
        if (!icList.containsKey(name)) {
            registerIC(name, ic, type, false);
        }
    }

    /**
     * Registers a PLC
     * 
     * @param name
     * @param ic
     * @param type
     * @param isPlc
     */
    private void internalRegisterPLC(String name, String plclang, ICType type) {
        if (!icList.containsKey(name)) {
            registerIC(name, new DefaultPLC(plcLanguageList.get(plclang)), type, true);
        }
    }

    /**
     * Registers a new non-PLC IC.
     * 
     * @param name
     * @param ic
     * @param type
     */
    public void registerIC(String name, IC ic, ICType type) {
        registerIC(name, ic, type, false);
    }

    /**
     * Registers a new IC.
     * 
     * @param name
     * @param ic
     * @param isPlc
     */
    public void registerIC(String name, IC ic, ICType type, boolean isPlc) {
        icList.put(name, new RegisteredIC(ic, type, isPlc));
    }

    public void registerLang(String name, PlcLang language) {
        plcLanguageList.put(name, language);
        craftBook.getStateManager().addStateHolder(name, language);
    }
    
    public void run() {onTick();}
    
    
    private int[][] getIOOrder(String options)
    {
    	int[] abc = null;
    	int[] def = null;
    	int[][] output = new int[][]{null, null};
		
		switch(options.length())
		{
			case 3:
				abc = getOrder('a', 'b', 'c', options);
				if(abc == null)
					def = getOrder('d', 'e', 'f', options);
				break;
			case 6:
				abc = getOrder('a', 'b', 'c', options.substring(0, 3));
				def = getOrder('d', 'e', 'f', options.substring(3));
				break;
		}
		
		output[0] = abc;
		output[1] = def;
		
    	return output;
    }
    
    private int[] getOrder(char a, char b, char c, String input)
    {
    	int[] output = new int[]{-1, -1, -1};
    	
    	for(int i = 0; i < 3; i++)
    	{
    		if(input.charAt(i) == ' ')
    			return null;
    		else if(input.charAt(i) == a)
    		{
    			output[i] = 0;
    			a = ' ';
    		}
    		else if(input.charAt(i) == b)
    		{
    			output[i] = 1;
    			b = ' ';
    		}
    		else if(input.charAt(i) == c)
    		{
    			output[i] = 2;
    			c = ' ';
    		}
    		else
    			return null;
    	}
    	
    	return output;
    }

    /**
     * Storage class for registered ICs.
     */
    private static class RegisteredIC {
        final ICType type;
        final IC ic;
        final boolean isPlc;

        /**
         * Construct the object.
         * 
         * @param ic
         * @param type
         * @param isPlc
         */
        public RegisteredIC(IC ic, ICType type, boolean isPlc) {
            this.type = type;
            this.ic = ic;
            this.isPlc = isPlc;
        }

        /**
         * Think.
         * 
         * @param pt
         * @param changedRedstoneInput
         * @param signText
         * @param sign
         * @param r
         */
        void think(int worldType, Vector pt, Vector changedRedstoneInput, SignText signText,
                Sign sign, TickDelayer r, char mode, int[] orderIn, int[] orderOut, Object extra) {
            type.think(worldType, pt, changedRedstoneInput, signText, sign, ic, r, mode, orderIn, orderOut, extra);
        }

        /**
         * Think.
         * 
         * @param pt
         * @param changedRedstoneInput
         * @param signText
         * @param sign
         */
        void think(int worldType, Vector pt, SignText signText, Sign sign, Object extra) {
            type.think(worldType, pt, signText, sign, ic, extra);
        }
    }
    
}
