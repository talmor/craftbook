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

import java.util.List;

import com.sk89q.craftbook.BlockSourceException;
import com.sk89q.craftbook.OutOfBlocksException;
import com.sk89q.craftbook.OutOfSpaceException;
import com.sk89q.craftbook.Vector;

/**
 * A collection of block bags.
 * 
 * @author Lymia
 */
public class CompoundBlockBag extends BlockBag {
    private List<BlockBag> sources;

    /**
     * Construct the instance.
     * 
     * @param bags
     */
    public CompoundBlockBag(List<BlockBag> bags) {
        this.sources = bags;
    }

    /**
     * Store a block.
     * 
     * @param id
     */
    public void storeBlock(int id) throws BlockSourceException {
        for (BlockBag b : sources)
            try {
                b.storeBlock(id);
                return;
            } catch (OutOfSpaceException e) {
            }
        throw new OutOfSpaceException(id);
    }

    /**
     * Get a block.
     * 
     * @param id
     */
    public void fetchBlock(int id) throws BlockSourceException {
        for (BlockBag b : sources)
            try {
                b.fetchBlock(id);
                return;
            } catch (OutOfBlocksException e) {
            }
        throw new OutOfBlocksException(id);
    }

    /**
     * Adds a position to be used a source.
     *
     * @param pos
     * @return
     */
    public void addSingleSourcePosition(int worldType, Vector pos) {
        for (BlockBag b : sources)
            b.addSingleSourcePosition(worldType, pos);
    }

    /**
     * Adds a position to be used a source.
     *
     * @param pos
     * @return
     */
    public void addSourcePosition(int worldType, Vector pos) {
        for (BlockBag b : sources)
            b.addSourcePosition(worldType, pos);
    }

    /**
     * Return the list of missing blocks.
     * 
     * @return
     */
    public void flushChanges() {
        for (BlockBag b : sources)
            b.flushChanges();
    }
}
