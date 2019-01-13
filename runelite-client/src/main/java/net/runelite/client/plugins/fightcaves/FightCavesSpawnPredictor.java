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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Spawn predictor for the Fight Caves.
 * This class manages all 15 possible spawn routes, and allows for locating routes by specifying waves.
 */
class FightCavesSpawnPredictor {
    private List<FightCavesSpawnRoute> routes = new ArrayList<>();

    FightCavesSpawnPredictor() {
        // populate all 15 routes
        for(int i = 0; i < FightCavesMobRotator.locations.length; i++) {
            routes.add(new FightCavesSpawnRoute(i));
        }
    }

    /**
     * Check if a route matches some criteria. You can add as much or as little criteria as you like.
     * @param route The route to be checking.
     * @param criteriaMap The criteria. key= wave, value = criteria for that particular wave
     * @return true if the route matches, false otherwise.
     */
    private boolean doesRouteMatchCriteria(FightCavesSpawnRoute route, Map<Integer, FightCavesWaveCriteria> criteriaMap) {
        for (Integer wave : criteriaMap.keySet()) {
            // -1 here because spawns per wave is a List and we are indexing it by the wave
            Map<FightCavesSpawnLocation, FightCavesMob> routeSpawns = route.getSpawnsPerWave().get(wave - 1);
            Map<FightCavesSpawnLocation, FightCavesMob> criteriaSpawns = criteriaMap.get(wave).getSpecifiedSpawns();

            for(FightCavesSpawnLocation spawnLocation: criteriaSpawns.keySet()) {
                FightCavesMob expectedMob = criteriaSpawns.get(spawnLocation);
                // if we are expecting a mob
                if(expectedMob != null) {
                    // the wave might have our mob
                    if(routeSpawns.containsKey(spawnLocation)) {
                        // if the mobs don't match, it's not a match
                        if(expectedMob != routeSpawns.get(spawnLocation)) {
                            return false;
                        }
                    } else {
                        // wave doesn't have our mob
                        return false;
                    }
                } else { // we explicitly expect that there is not a mob here
                    // the wave might have a mob here, which would be bad
                    // however it might have a null here so let's see
                    if(routeSpawns.containsKey(spawnLocation)) {
                        // ensure that nothing spawns here
                        if(routeSpawns.get(spawnLocation) != null) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    /**
     * Find all routes which match a given set of criteria.
     * The idea here is that you include things that you know to be true in the criteria.
     * For example, if I know that wave 30 in the route I care about has a ket zek in the South, I can specify
     * only that piece of information even if there are several other mobs spawning on that wave.
     * You can assert that spawns don't have anything in them too by making the spawn location point to a null value.
     * @param criteria The criteria to search with.
     * @return All routes which match the criteria.
     */
    List<FightCavesSpawnRoute> findMatchingRoutes(Map<Integer, FightCavesWaveCriteria> criteria) {
        return routes.stream()
                .filter(r -> doesRouteMatchCriteria(r, criteria))
                .collect(Collectors.toList());
    }
}
