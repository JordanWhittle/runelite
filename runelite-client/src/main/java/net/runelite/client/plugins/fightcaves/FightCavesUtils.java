/*
 * Copyright (c) 2019, Jordan Whittle <jordanwhittle1@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.client.plugins.fightcaves;

import net.runelite.api.Client;
import net.runelite.api.coords.WorldPoint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static net.runelite.api.Constants.CHUNK_SIZE;

class FightCavesUtils
{
    /**
     * Calculate how many mobs will appear on a wave
     * Algorithm works as follows:
     * Each mob is assigned a number (the first wave they appear at)
     * while current_wave > 0:
     * Find a mob with the highest wave in their first appearance
     * add the mob to the spawn list
     * current_wave -= the wave which the mob first appears
     *
     * @param wave The wave number to calculate for
     * @return A list of mobs in the wave, implicitly ordered by level
     */
    static List<FightCavesMob> calculateMobsForWave(int wave)
    {
        int counter = wave;
        List<FightCavesMob> mobs = new ArrayList<>();
        // while we still need to spawn mobs
        while (counter > 0)
        {
            final int c = counter;
            // find the mob that appears at the highest wave
            FightCavesMob mob = Arrays.stream(FightCavesMob.values())
                    .filter(m -> m.firstAppearance <= c)
                    .findFirst()
                    .orElseThrow(IllegalStateException::new);
            mobs.add(mob);
            counter -= mob.firstAppearance;
        }
        return mobs;
    }

    static WorldPoint getWorldPoint(Client client, int regionId, int regionX, int regionY)
    {
        WorldPoint worldPoint = new WorldPoint(
                ((regionId >>> 8) << 6) + regionX,
                ((regionId & 0xff) << 6) + regionY,
                0
        );

        if (!client.isInInstancedRegion())
        {
            return worldPoint;
        }

        // find instance chunks using the template point. there might be more than one.
        int[][][] instanceTemplateChunks = client.getInstanceTemplateChunks();
        for (int x = 0; x < instanceTemplateChunks[0].length; x++)
        {
            for (int y = 0; y < instanceTemplateChunks[0][x].length; y++)
            {
                int chunkData = instanceTemplateChunks[0][x][y];
                int rotation = chunkData >> 1 & 0x3;
                int templateChunkY = (chunkData >> 3 & 0x7FF) * CHUNK_SIZE;
                int templateChunkX = (chunkData >> 14 & 0x3FF) * CHUNK_SIZE;
                if (worldPoint.getX() >= templateChunkX
                        && worldPoint.getX() < templateChunkX + CHUNK_SIZE
                        && worldPoint.getY() >= templateChunkY
                        && worldPoint.getY() < templateChunkY + CHUNK_SIZE)
                {

                    WorldPoint p = new WorldPoint(client.getBaseX() + x * CHUNK_SIZE + (worldPoint.getX() & (CHUNK_SIZE - 1)),
                            client.getBaseY() + y * CHUNK_SIZE + (worldPoint.getY() & (CHUNK_SIZE - 1)),
                            worldPoint.getPlane());
                    p = rotate(p, rotation);
                    return p;
                }
            }
        }
        return null;
    }

    /**
     * Rotate the coordinates in the chunk according to chunk rotation
     *
     * @param point    point
     * @param rotation rotation
     * @return world point
     */
    private static WorldPoint rotate(WorldPoint point, int rotation)
    {
        int chunkX = point.getX() & -CHUNK_SIZE;
        int chunkY = point.getY() & -CHUNK_SIZE;
        int x = point.getX() & (CHUNK_SIZE - 1);
        int y = point.getY() & (CHUNK_SIZE - 1);
        switch (rotation)
        {
            case 1:
                return new WorldPoint(chunkX + y, chunkY + (CHUNK_SIZE - 1 - x), point.getPlane());
            case 2:
                return new WorldPoint(chunkX + (CHUNK_SIZE - 1 - x), chunkY + (CHUNK_SIZE - 1 - y), point.getPlane());
            case 3:
                return new WorldPoint(chunkX + (CHUNK_SIZE - 1 - y), chunkY + x, point.getPlane());
        }
        return point;
    }
}
