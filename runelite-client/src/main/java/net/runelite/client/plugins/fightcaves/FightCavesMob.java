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

import java.util.Arrays;

/**
 * Mobs for the fight caves.
 * It is important that these are ordered in this enum by their first appearance wave.
 * FightCavesUtils::calculateMobsForWave currently does not sort these values and thus depends on the order.
 * <p>
 * Healers could be added here but I have found their spawn positions to be useless information.
 * <p>
 * The level 22s that 45s split into are omitted from this list because they don't spawn at the beginning of a wave.
 */
public enum FightCavesMob
{
    TZTOK_JAD(63, 702),
    // healer would go here if desired
    KET_ZEK(31, 360),
    YT_MEJKOT(15, 180),
    TOK_XIL(7, 90),
    TZ_KEK(3, 45),
    TZ_KIH(1, 22);

    // the wave in which this mob first appears
    public final int firstAppearance;
    // the combat level of this mob
    public final int level;

    FightCavesMob(int firstAppearance, int level)
    {
        this.firstAppearance = firstAppearance;
        this.level = level;
    }

    /**
     * Find a mob using its combat level.
     *
     * @param level The combat level to search for
     * @return The FightCavesMob with the appropriate combat level, or null if no monster matches this level.
     */
    public static FightCavesMob getByLevel(int level)
    {
        return Arrays.stream(values())
                .filter(x -> x.level == level)
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);
    }
}
