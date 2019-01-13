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

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Controls the rotation of spawns for the Fight caves.
 *
 * The spawning system works by rotating through FightCavesSpawnLocation::locations as a circular array.
 * Every wave, the index increments. Then, Each mob to be spawned is located by iterating from the current index for
 * the count of mobs to be spawned. The mobs are spawned from highest level to lowest level.
 * Kinda reminds me of the circle of fifths...
 */
public class FightCavesMobRotator
{
    // circular array
    public static FightCavesSpawnLocation[] locations = {
            FightCavesSpawnLocation.SOUTH_EAST,
            FightCavesSpawnLocation.SOUTH_WEST,
            FightCavesSpawnLocation.CENTER,
            FightCavesSpawnLocation.NORTH_WEST,
            FightCavesSpawnLocation.SOUTH_WEST,
            FightCavesSpawnLocation.SOUTH_EAST,
            FightCavesSpawnLocation.SOUTH,
            FightCavesSpawnLocation.NORTH_WEST,
            FightCavesSpawnLocation.CENTER,
            FightCavesSpawnLocation.SOUTH_EAST,
            FightCavesSpawnLocation.SOUTH_WEST,
            FightCavesSpawnLocation.SOUTH,
            FightCavesSpawnLocation.NORTH_WEST,
            FightCavesSpawnLocation.CENTER,
            FightCavesSpawnLocation.SOUTH
    };

    // current index into the locations, increases once per wave (rotation).
    private int currentIndex;

    FightCavesMobRotator(int rotation) {
        this.currentIndex = rotation;
    }

    /**
     * Perform a rotation (i.e Begin a new wave)
     * @param mobsInWave The amount of mobs to be spawned in this wave
     * @return The list of spawn locations to be used in this wave. Mobs should be spawned from highest level to lowest.
     */
    List<FightCavesSpawnLocation> rotate(int mobsInWave) {
        int idx = currentIndex++;
        return IntStream.range(idx, idx+mobsInWave)
                .mapToObj(FightCavesMobRotator::getLocation)
                .collect(Collectors.toList());
    }

    /**
     * Gets a spawn location by index, will loop back around if index is > 14
     * @param index The index into the spawn locations
     * @return The spawn location.
     */
    public static FightCavesSpawnLocation getLocation(int index) {
        return locations[index % locations.length];
    }
}