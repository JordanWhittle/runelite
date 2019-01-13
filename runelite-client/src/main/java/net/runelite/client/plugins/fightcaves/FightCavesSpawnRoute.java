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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a route through the fight caves.
 * A route is a particular combination of spawns; the user is assigned a route upon entry to the caves and
 * the route persists until they physically exit the minigame. This route also persists through logout.
 */
class FightCavesSpawnRoute
{
    // the index of the route
    private final int routeNumber;
    // a list of waves, each wave has some spawn locations and mobs in it.
    private List<Map<FightCavesSpawnLocation, FightCavesMob>> spawnsPerWave = new ArrayList<>();

    /**
     * Construct a spawn route, calculating all spawns for the entire route.
     * @param routeNumber The route number, there are 15 possible routes at the time of writing (0-14)
     */
    FightCavesSpawnRoute(int routeNumber) {
        this.routeNumber = routeNumber;
        // rotator to spawn the mobs with
        FightCavesMobRotator rotator = new FightCavesMobRotator(routeNumber);
        // go through every wave
        for(int wave = 1; wave <= 63; wave++) {
            // calculate which mobs shall appear this wave
            List<FightCavesMob> mobsThisWave = FightCavesUtils.calculateMobsForWave(wave);
            // perform a rotation, getting the mobs for this wave
            List<FightCavesSpawnLocation> spawns = rotator.rotate(mobsThisWave.size());
            // map the spawn locations to the mobs
            Map<FightCavesSpawnLocation, FightCavesMob> waveSpawns = new HashMap<>();
            for(int j = 0; j < spawns.size(); j++) {
                waveSpawns.put(spawns.get(j), mobsThisWave.get(j));
            }
            spawnsPerWave.add(waveSpawns);
        }
    }

    /**
     * @return A list where every item is a wave (map of spawns).
     */
    List<Map<FightCavesSpawnLocation, FightCavesMob>> getSpawnsPerWave() {
        return spawnsPerWave;
    }
}
