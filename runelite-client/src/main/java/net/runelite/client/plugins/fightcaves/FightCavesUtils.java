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
import java.util.Arrays;
import java.util.List;

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
}
