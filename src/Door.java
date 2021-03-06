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

import java.util.Set;
import java.util.HashSet;
import com.sk89q.craftbook.*;

/**
 * Door.
 *
 * @author sk89q
 */
public class Door extends SignOrientedMechanism {
    /**
     * Direction to extend the bridge.
     */
    private enum Direction {
        NORTH_SOUTH, // X
        WEST_EAST, // Z
    }

    /**
     * What doors can be made out of.
     */
    public static Set<Integer> allowedBlocks
            = new HashSet<Integer>();
    /**
     * Max door length.
     */
    public static int maxLength = 30;

    /**
     * Returns whether a block can be used for the door.
     * 
     * @param id
     * @return
     */
    private static boolean canUseBlock(int id) {
        return allowedBlocks.contains(id);
    }
    
    /**
     * Returns whether the door should pass through this block (and displace
     * it if needed).
     * 
     * @param t
     * @return
     */
    private static boolean canPassThrough(int t) {
        return t == 0 || t == BlockType.WATER || t == BlockType.STATIONARY_WATER
                || t == BlockType.LAVA || t == BlockType.STATIONARY_LAVA
                || t == BlockType.SNOW;
    }
    
    /**
     * Construct the instance.
     * 
     * @param pt
     */
    public Door(int worldType, Vector pt) {
        super(worldType, pt);
    }
    
    /**
     * Returns the direction of the bridge to open towards.
     * 
     * @return
     * @throws InvalidDirection
     */
    private Direction getDirection() throws InvalidDirectionException {
        int data = CraftBook.getBlockData(worldType, pt);

        if (data == 0x0 || data == 0x8) { // East-west
            return Direction.NORTH_SOUTH;
        } else if (data == 0x4 || data == 0xC) { // North-south
            return Direction.WEST_EAST;
        } else {
            throw new InvalidDirectionException();
        }
    }
    
    /**
     * Returns whether the door needs to be opened upwards. This is indicated
     * by [Door Up] on the sign as opposed to [Door Down].
     * 
     * @return
     */
    private boolean isUpwards() {
        return getSignIdentifier().equalsIgnoreCase("[Door Up]");
    }

    /**
     * Toggles the door closest to a location.
     *
     * @param player
     * @param bag
     * @return
     */
    public void playerToggleDoor(CraftBookPlayer player, BlockBag bag)
            throws BlockSourceException {
        try {
            setState(bag, null);
        } catch (InvalidDirectionException e) {
            player.printError("The sign is not oriented at a right angle.");
        } catch (UnacceptableTypeException e) {
            player.printError("The bridge is not made from an permitted material.");
        } catch (InvalidConstructionException e) {
            player.printError(e.getMessage());
        }
    }
    
    /**
     * Sets the door to be active.
     * 
     * @param bag
     */
    public void setActive(BlockBag bag) {
        try {
            setState(bag, false);
        } catch (InvalidDirectionException e) {
        } catch (UnacceptableTypeException e) {
        } catch (InvalidConstructionException e) {
        } catch (BlockSourceException e) {
        }
    }
    
    /**
     * Sets the door to be active.
     * 
     * @param bag
     */
    public void setInactive(BlockBag bag) {
        try {
            setState(bag, true);
        } catch (InvalidDirectionException e) {
        } catch (UnacceptableTypeException e) {
        } catch (InvalidConstructionException e) {
        } catch (BlockSourceException e) {
        }
    }
    
    /**
     * Toggles the door closest to a location.
     *
     * @param pt
     * @param direction
     * @param bag
     * @return
     */
    public boolean setState(BlockBag bag, Boolean toOpen)
            throws BlockSourceException, InvalidDirectionException,
            UnacceptableTypeException, InvalidConstructionException {
        
        Direction direction = getDirection();
        boolean upwards = isUpwards();

        Vector sideDir = null;
        Vector vertDir = upwards ? new Vector(0, 1, 0) : new Vector(0, -1, 0);

        if (direction == Direction.NORTH_SOUTH) {
            sideDir = new Vector(1, 0, 0);
        } else if(direction == Direction.WEST_EAST) {
            sideDir = new Vector(0, 0, 1);
        }
        
        World world = CraftBook.getWorld(worldType);
        
        int type = CraftBook.getBlockID(world, pt.add(vertDir));
        int data = 0;
        
        if(BlockType.isColorTypeBlock(type))
        	data = CraftBook.getBlockData(world, pt.add(vertDir));

        // Check construction
        if (!canUseBlock(type)) {
            throw new UnacceptableTypeException();
        }
        
        // Check sides
        if (CraftBook.getBlockID(world, pt.add(vertDir).add(sideDir)) != type
                || CraftBook.getBlockID(world, pt.add(vertDir).subtract(sideDir)) != type) {
            throw new InvalidConstructionException(
                    "The blocks for the door to the sides have to be the same.");
        }
        
        // Detect whether the door needs to be opened
        if (toOpen == null) {
            toOpen = !canPassThrough(CraftBook.getBlockID(world, pt.add(vertDir.multiply(2))));
        }
        
        Vector cur = pt.add(vertDir.multiply(2));
        boolean found = false;
        int dist = 0;
        
        // Find the other side
        for (int i = 0; i < maxLength + 2; i++) {
            int id = CraftBook.getBlockID(world, cur);

            if (id == BlockType.SIGN_POST) {
                SignText otherSignText = CraftBook.getSignText(world, cur);
                
                if (otherSignText != null) {
                    String line2 = otherSignText.getLine2();

                    if (line2.equalsIgnoreCase("[Door Up]")
                            || line2.equalsIgnoreCase("[Door Down]")
                            || line2.equalsIgnoreCase("[Door End]")) {
                        found = true;
                        dist = i - 1;
                        break;
                    }
                }
            }

            cur = cur.add(vertDir);
        }

        // Failed to find the other side!
        if (!found) {
            throw new InvalidConstructionException(
                    "[Door] sign required on other side (or it was too far away).");
        }

        Vector otherSideBlockPt = pt.add(vertDir.multiply(dist + 2));

        // Check the other side to see if it's built correctly
        if (CraftBook.getBlockID(world, otherSideBlockPt) != type
                || CraftBook.getBlockID(world, otherSideBlockPt.add(sideDir)) != type
                || CraftBook.getBlockID(world, otherSideBlockPt.subtract(sideDir)) != type) {
            throw new InvalidConstructionException(
            "The other side must be made with the same blocks.");
        }

        if (toOpen) {
            clearColumn(worldType, pt.add(vertDir.multiply(2)), vertDir, type, dist, bag);
            clearColumn(worldType, pt.add(vertDir.multiply(2).add(sideDir)), vertDir, type, dist, bag);
            clearColumn(worldType, pt.add(vertDir.multiply(2).subtract(sideDir)), vertDir, type, dist, bag);
        } else {
            setColumn(worldType, pt.add(vertDir.multiply(2)), vertDir, type, data, dist, bag);
            setColumn(worldType, pt.add(vertDir.multiply(2).add(sideDir)), vertDir, type, data, dist, bag);
            setColumn(worldType, pt.add(vertDir.multiply(2).subtract(sideDir)), vertDir, type, data, dist, bag);
        }

        bag.flushChanges();
        
        return true;
    }

    /**
     * Clears a row.
     *
     * @param origin
     * @param change
     * @param dist
     */
    private static void clearColumn(int worldType, Vector origin, Vector change, int type, int dist, BlockBag bag)
            throws BlockSourceException {
        for (int i = 0; i < dist; i++) {
            Vector p = origin.add(change.multiply(i));
            int t = CraftBook.getBlockID(worldType, p);
            if (t == type) {
                bag.setBlockID(worldType, p, 0);
            } else if (t != 0) {
                break;
            }
        }
    }

    /**
     * Clears a row.
     *
     * @param origin
     * @param change
     * @param dist
     */
    private static void setColumn(int worldType, Vector origin, Vector change, int type, int data, int dist, BlockBag bag)
            throws BlockSourceException {
        for (int i = 0; i < dist; i++) {
            Vector p = origin.add(change.multiply(i));
            int t = CraftBook.getBlockID(worldType, p);
            if (canPassThrough(t)) {
                bag.setBlockID(worldType, p, type, data);
            } else if (t != type) {
                break;
            }
        }
    }
    
    
    public static boolean isOpen(Sign sign)
    {
    	World world = sign.getWorld();
    	int worldType = world.getType().getId();
    	
    	int direction = CraftBook.getBlockData(worldType, sign.getX(), sign.getY(), sign.getZ());
    	
		boolean upwards = sign.getText(1).equalsIgnoreCase("[Door Up]");
		
		Vector sideDir = null;
		Vector vertDir = upwards ? new Vector(0, 1, 0) : new Vector(0, -1, 0);
		
		if (direction == 0x0 || direction == 0x8) { //north south
		    sideDir = new Vector(1, 0, 0);
		} else if(direction == 0x4 || direction == 0xC) { //west east
		    sideDir = new Vector(0, 0, 1);
		}
		
		Vector pt = new Vector(sign.getX(), sign.getY(), sign.getZ());
		
		int type = CraftBook.getBlockID(world, pt.add(vertDir));
		
		// Check construction
		if (!canUseBlock(type)) {
		    return false;
		}
		
		// Check sides
		if (CraftBook.getBlockID(world, pt.add(vertDir).add(sideDir)) != type
		        || CraftBook.getBlockID(world, pt.add(vertDir).subtract(sideDir)) != type) {
		    return false;
		}
		
		// Detect whether the door needs to be opened
		boolean toOpen = canPassThrough(CraftBook.getBlockID(world, pt.add(vertDir.multiply(2))));
		
		Vector cur = pt.add(vertDir.multiply(2));
		boolean found = false;
		int dist = 0;
		
		// Find the other side
		for (int i = 0; i < maxLength + 2; i++) {
		    int id = CraftBook.getBlockID(world, cur);
		
		    if (id == BlockType.SIGN_POST) {
		        SignText otherSignText = CraftBook.getSignText(world, cur);
		        
		        if (otherSignText != null) {
		            String line2 = otherSignText.getLine2();
		
		            if (line2.equalsIgnoreCase("[Door Up]")
		                    || line2.equalsIgnoreCase("[Door Down]")
		                    || line2.equalsIgnoreCase("[Door End]")) {
		                found = true;
		                dist = i - 1;
		                break;
		            }
		        }
		    }
		
		    cur = cur.add(vertDir);
		}
		
		// Failed to find the other side!
		if (!found) {
		    return false;
		}
		
		Vector otherSideBlockPt = pt.add(vertDir.multiply(dist + 2));
		
		// Check the other side to see if it's built correctly
		if (CraftBook.getBlockID(world, otherSideBlockPt) != type
		        || CraftBook.getBlockID(world, otherSideBlockPt.add(sideDir)) != type
		        || CraftBook.getBlockID(world, otherSideBlockPt.subtract(sideDir)) != type) {
		    return false;
		}
		
		return toOpen;
	}
    
    
    /**
     * Validates the sign's environment.
     * 
     * @param signText
     * @return false to deny
     */
    public static boolean validateEnvironment(CraftBookPlayer player,
            Vector pt, SignText signText) {
        
        if (signText.getLine2().equalsIgnoreCase("[Door Up]")) {
            signText.setLine2("[Door Up]");
        } else if (signText.getLine2().equalsIgnoreCase("[Door Down]")) {
            signText.setLine2("[Door Down]");
        } else {
            signText.setLine2("[Door]");
        }
        
        player.print("Door really created!");
        
        return true;
    }
    
    /**
     * Thrown when the sign is an invalid direction.
     */
    private static class InvalidDirectionException extends Exception {
        private static final long serialVersionUID = -3183606604247616362L;
    }
    
    /**
     * Thrown when the bridge type is unacceptable.
     */
    private static class UnacceptableTypeException extends Exception {
        private static final long serialVersionUID = 8340723004466483212L;
    }
    
    /**
     * Thrown when the bridge type is not constructed correctly.
     */
    private static class InvalidConstructionException extends Exception {
        private static final long serialVersionUID = 4943494589521864491L;

        /**
         * Construct the object.
         * 
         * @param msg
         */
        public InvalidConstructionException(String msg) {
            super(msg);
        }
    }
}
