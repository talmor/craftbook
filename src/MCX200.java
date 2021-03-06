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

import com.sk89q.craftbook.*;
import com.sk89q.craftbook.ic.*;

/**
 * Mob spawner.
 *
 * @author sk89q
 */
public class MCX200 extends BaseIC {
    /**
     * Get the title of the IC.
     *
     * @return
     */
    public String getTitle() {
        return "MOB SPAWNER CLR";
    }

    /**
     * Returns true if this IC requires permission to use.
     *
     * @return
     */
    public boolean requiresPermission() {
        return true;
    }
    
    /**
     * Validates the IC's environment. The position of the sign is given.
     * Return a string in order to state an error message and deny
     * creation, otherwise return null to allow.
     *
     * @param sign
     * @return
     */
    public String validateEnvironment(int worldType, Vector pos, SignText sign) {
        String id = sign.getLine3();
        String rider = sign.getLine4();

        if (id.length() == 0) {
            return "Specify a mob type on the third line.";
        }
        
        String[] args = id.split(":", 2);
        int color = getColor(args);
        
        if(color >= 0)
        	id = args[0];
        else if(color == -2)
        	return "Not a valid color value: " + args[1] + ".";
        
        String[] args2 = rider.split(":", 2);
        int colorRider = getColor(args2);
        if(colorRider >= 0)
        	rider = args2[0];
        else if(colorRider == -2)
        	return "Not a valid color value: " + args2[1] + ".";
        
        if (!Mob.isValid(id)) {
            return "Not a valid mob type: " + id + ".";
        } else if (rider.length() != 0 && !Mob.isValid(rider)) {
            return "Not a valid rider type: " + rider + ".";
        }

        return null;
    }

    /**
     * Think.
     *
     * @param chip
     */
    public void think(ChipState chip) {
        if (chip.getIn(1).is()) {
            String id = chip.getText().getLine3();
            String rider = chip.getText().getLine4();
            
            String[] args = id.split(":", 2);
            int color = getColor(args);
            
            if(color >= 0)
            	id = args[0];
            
            String[] args2 = rider.split(":", 2);
            int colorRider = getColor(args2);
            if(colorRider >= 0)
            	rider = args2[0];
            
            if (Mob.isValid(id)) {
                Vector pos = chip.getBlockPosition();
                int maxY = Math.min(128, pos.getBlockY() + 10);
                int x = pos.getBlockX();
                int z = pos.getBlockZ();

                for (int y = pos.getBlockY() + 1; y <= maxY; y++)
                {
                	int blockId = CraftBook.getBlockID(chip.getWorldType(), x, y, z);
                    if (BlockType.canPassThrough(blockId) || BlockType.isWater(blockId))
                    {
                        Location loc = new Location(x, y, z);
                        Mob mob = new Mob(id, loc);
                        if (rider.length() != 0 && Mob.isValid(rider)) {
                        	Mob mobRider = new Mob(rider);
                            mob.spawn(mobRider);
                            
                            if(colorRider >= 0)
                            	setMobColor(mobRider.getEntity(), colorRider);
                            
                        } else {
                            mob.spawn();
                        }
                        
                        if(color >= 0)
                        	setMobColor(mob.getEntity(), color);
                        
                        return;
                    }
                }
            }
        }
    }
    
    private int getColor(String[] args)
    {
    	int color;
    	
    	if(args.length < 2 || !isValidColorMob(args[0]) )
    		return -1;
    	
    	try
    	{
    		color = Integer.parseInt(args[1]);
    	}
    	catch(NumberFormatException e)
    	{
    		return -2;
    	}
    	
    	if(color < 0 || color > 15)
    		return -2;
    	
    	return color;
    }
    
    private void setMobColor(OEntityLiving entity, int color)
	{
    	if(entity instanceof OEntitySheep)
    	{
    		OEntitySheep sheep = (OEntitySheep)entity;
    		sheep.a_(color);
    	}
    	else if(entity instanceof OEntityCreeper)
    	{
    		//no real need for this check, but putting it here to be 
    		//strict so that it's easier to support more creeper types
    		//if more are ever created
    		if(color != 1)
    			return;
    		
    		OEntityCreeper creeper = (OEntityCreeper)entity;
    		creeper.Z().b(17, (byte)1);
    	}
    	else if(entity instanceof OEntityWolf)
    	{
    		//since a tamed wolf requires a player, I'm just allowing
    		//the option to create angry wolves and sitting wolves.
    		//neutral wolves have no color value (or are technically 0)
    		if(color != 2 && color != 1)
    			return;
    		
    		OEntityWolf wolf = (OEntityWolf)entity;
    		wolf.Z().b(16, (byte)color);
    	}
	}
    
    private boolean isValidColorMob(String mob)
    {
    	if( mob.equals("Sheep")
    		|| mob.equals("Creeper")
    		|| mob.equals("Wolf")
    		)
    	{
    		return true;
    	}
    	
    	return false;
    }
}
