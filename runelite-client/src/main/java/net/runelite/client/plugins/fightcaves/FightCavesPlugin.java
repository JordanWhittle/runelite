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

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.NpcSpawned;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static net.runelite.api.Constants.CHUNK_SIZE;

/**
 * Fight caves plugin.
 * <p>
 * How it works:
 * Calculate every possible wave in the fight caves (there are 15 sets of 63 waves which make up all possibilities)
 * Whilst the player plays the fight caves, monitor what spawns where and what doesn't spawn where.
 * Every time you find some information (e.g wave 1 has a tz-kih spawning to the east), find all combinations which
 * have a tz-kih spawning to the east at wave 1.
 * After a few waves worth of criteria, the route will be known and the plugin will be able to predict the next waves
 * from that point onward.
 */
@PluginDescriptor(
        name = "Fight Caves",
        description = "Predicts mob spawns in the fight caves after watching you play for a short while.",
        tags = {"minigame", "fight cave"}
)
@Singleton
@Slf4j
public class FightCavesPlugin extends Plugin
{
    private static final String CONFIG_GROUP = "fightCaves";
    private static final int FIGHT_CAVES_REGION_ID = 9551;
    private static final Pattern WAVE_MESSAGE_PATTERN = Pattern.compile("<col=ef1020>Wave: (\\d+)</col>");
    private static final Gson gson = new Gson().newBuilder().serializeNulls().create();

    @Inject
    private ConfigManager configManager;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private FightCavesOverlay overlay;

    @Inject
    private Client client;

    private final FightCavesSpawnPredictor predictor = new FightCavesSpawnPredictor();
    private HashMap<Integer, FightCavesWaveCriteria> gatheredWaveCriteria = new HashMap<>();
    private int waveStartedAtTick;
    private int currentWave;
    private FightCavesSpawnRoute route;
    private int lastPossibleSpawns;
    private boolean wasInFightCavesLastCheck;

    @Provides
    FightCavesConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(FightCavesConfig.class);
    }

    @Override
    protected void startUp()
    {
        overlayManager.add(overlay);
    }

    @Override
    protected void shutDown()
    {
        overlayManager.remove(overlay);
    }

    boolean isInFightCaves()
    {
        return client.getMapRegions().length == 1 && client.getMapRegions()[0] == FIGHT_CAVES_REGION_ID;
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        if (event.getGameState() == GameState.LOGIN_SCREEN)
        {
            wasInFightCavesLastCheck = false;
        }
    }

    WorldPoint getWorldPoint(int regionX, int regionY)
    {
        WorldPoint worldPoint = new WorldPoint(
                ((FIGHT_CAVES_REGION_ID >>> 8) << 6) + regionX,
                ((FIGHT_CAVES_REGION_ID & 0xff) << 6) + regionY,
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

    private int getDistanceToAreaCenter(WorldPoint p, FightCavesArea area)
    {
        WorldPoint center = getWorldPoint(area.getCenterX(), area.getCenterY());
        return p.distanceTo(center);
    }

    /**
     * Find the visible spawn locations that the actor can see.
     *
     * @param actor The actor to check
     * @return A list of every spawn location where the actor would be able to see an npc spawn in it.
     */
    private List<FightCavesSpawnLocation> getVisibleSpawnLocations(Actor actor)
    {
        return Arrays.stream(FightCavesSpawnLocation.values())
                .filter(spawn -> getDistanceToAreaCenter(actor.getWorldLocation(), spawn.getSpawnArea()) <= 10)
                .collect(Collectors.toList());
    }

    @Subscribe
    public void onGameTick(GameTick tick)
    {
        boolean inCaves = isInFightCaves();
        if (inCaves)
        {
            if (!wasInFightCavesLastCheck)
            {
                System.out.println("Player entered caves");
                wasInFightCavesLastCheck = true;
                onEnterFightCaves();
            }
            if (route == null)
            {
                // if the wave has JUST started last tick
                if (currentWave > 0 && client.getTickCount() == waveStartedAtTick + 1)
                {
                    // check all of the spawn locations the player can see
                    for (FightCavesSpawnLocation spawnLocation : getVisibleSpawnLocations(client.getLocalPlayer()))
                    {
                        // find out what we have recorded for this wave
                        FightCavesWaveCriteria waveCriteria = gatheredWaveCriteria.get(currentWave);
                        Map<FightCavesSpawnLocation, FightCavesMob> specifiedSpawns = waveCriteria.getSpecifiedSpawns();
                        // if there is nothing specified to spawn at the place the current player is,
                        // we can be sure that this wave does not spawn anything here.
                        if (!specifiedSpawns.containsKey(spawnLocation))
                        {
                            specifiedSpawns.put(spawnLocation, null);
                            String message = "I can see that there is nothing in the " + spawnLocation.toString().toLowerCase() + "...";
                            saveGatheredWaveData();
                            client.addChatMessage(ChatMessageType.FILTERED, "", message, "");
                        }
                    }
                    // maybe we have gathered enough information to know what route we are on
                    attemptSolveSpawns();
                }
            }
        } else
        {
            if (wasInFightCavesLastCheck)
            {
                System.out.println("Player left caves");
                wasInFightCavesLastCheck = false;
                onExitFightCaves();
            }
        }
    }

    /**
     * Reset state, but not the config.
     */
    private void reset()
    {
        waveStartedAtTick = -1;
        currentWave = 0;
        route = null;
        lastPossibleSpawns = FightCavesMobRotator.locations.length;
        gatheredWaveCriteria.clear();
        // load up the saved data
        loadSavedData();
        // set the thing to be unpaused
        configManager.setConfiguration(CONFIG_GROUP, "isPaused", "false");
    }

    /**
     * Get the saved wave criteria and wave from a previous session, if they exist.
     */
    private void loadSavedData()
    {
        String json = configManager.getConfiguration(CONFIG_GROUP, "gatheredWaveCriteria");
        if (json != null && !json.isEmpty())
        {
            Type typeOfHashMap = new TypeToken<HashMap<Integer, FightCavesWaveCriteria>>()
            {
            }.getType();
            gatheredWaveCriteria = gson.fromJson(json, typeOfHashMap);
            System.out.println(json);
            attemptSolveSpawns();
        }
        String lastKnownWave = configManager.getConfiguration(CONFIG_GROUP, "lastKnownWave");
        if (lastKnownWave != null && !lastKnownWave.isEmpty())
        {
            try
            {
                final int savedWave = Integer.parseInt(lastKnownWave);
                System.out.println("Loading saved wave number " + savedWave);
                String isPaused = configManager.getConfiguration(CONFIG_GROUP, "isPaused");
                if ("true".equals(isPaused))
                {
                    currentWave = savedWave;
                    System.out.println("Waves were paused");
                } else
                {
                    System.out.println("Waves were not paused");
                    currentWave = savedWave - 1;
                }

            } catch (Exception ex)
            {
                ex.printStackTrace();
            }
        }
    }

    /**
     * Player has entered the fight cave. This happens when they log into it and when they walk into it.
     */
    private void onEnterFightCaves()
    {
        System.out.println("on enter fight cave");
        reset();
        client.addChatMessage(ChatMessageType.GAME, "", "Entering fight caves", "");

        if (route == null)
        {
            String message = "For fastest calculations, stand where you can see the south, south-east and center spawns. (optional)";
            client.addChatMessage(ChatMessageType.GAME, "", message, "");
        }
    }

    /**
     * User has exited the fight cave in-game. They have not logged out; they have walked out of the cave.
     */
    private void onExitFightCaves()
    {
        client.addChatMessage(ChatMessageType.GAME, "", "Exiting fight caves", "");
        clearConfig();
        currentWave = 0;
    }

    private void clearConfig()
    {
        // clear gathered wave criteria
        configManager.unsetConfiguration(CONFIG_GROUP, "gatheredWaveCriteria");
        // clear last known wave
        configManager.unsetConfiguration(CONFIG_GROUP, "lastKnownWave");
        configManager.unsetConfiguration(CONFIG_GROUP, "isPaused");
    }

    @Subscribe
    public void onChatMessage(ChatMessage message)
    {
        if (isInFightCaves() && message.getType() == ChatMessageType.SERVER)
        {
            // try and find a Wave message
            Matcher m = WAVE_MESSAGE_PATTERN.matcher(message.getMessage());
            if (m.matches())
            {
                int wave = Integer.parseInt(m.group(1));
                onWaveStart(wave);
            }

            if ("<col=ef1020>The Fight Cave has been paused. You may now log out.".equals(message.getMessage()))
            {
                configManager.setConfiguration(CONFIG_GROUP, "isPaused", "true");
            }
        }
    }

    @Subscribe
    public void onNpcSpawned(NpcSpawned event)
    {
        if (isInFightCaves())
        {
            if (client.getTickCount() == waveStartedAtTick)
            {
                onFightCaveNpcSpawned(event.getNpc());
            }
            // else, the npc has merely came close to the player and hasn't actually spawned
            // else { onFightCaveNpcCameClose(event.getNpc()); }
        }
    }

    /**
     * A wave has just started!
     *
     * @param wave The wave which started.
     */
    private void onWaveStart(int wave)
    {
        // record the tick that the wave started at, we will use this in the spawns to check if they just spawned
        // or if the client simply just started rendering them
        waveStartedAtTick = client.getTickCount();

        // if we don't currently have any data saved about the wave, make a criteria instance.
        if (!gatheredWaveCriteria.containsKey(wave))
        {
            gatheredWaveCriteria.put(wave, new FightCavesWaveCriteria());
        }
        currentWave = wave;
        // save the wave
        configManager.setConfiguration(CONFIG_GROUP, "lastKnownWave", wave);
    }

    /**
     * Find the spawn location that the actor is standing in
     *
     * @param actor The actor to check
     * @return The location the actor is standing in, or null if they aren't in one.
     */
    private FightCavesSpawnLocation getSpawnLocation(Actor actor)
    {
        WorldPoint worldPoint = WorldPoint.fromLocalInstance(client, actor.getLocalLocation());
        return Arrays.stream(FightCavesSpawnLocation.values())
                .filter(spawnLoc -> spawnLoc.getSpawnArea().contains(worldPoint))
                .findFirst().orElse(null);
    }

    /**
     * An NPC has spawned. This differs from the usual spawn callback in that it actually knows when the npc has SPAWNED
     * and not just popped into existence because you've walked near to it.
     *
     * @param npc The npc which just spawned.
     */
    private void onFightCaveNpcSpawned(NPC npc)
    {
        // if we have a route already, there is no point in caring about this
        if (route == null)
        {
            FightCavesSpawnLocation location = getSpawnLocation(npc);
            if (location != null)
            {
                // find out which mob in the enum we are dealing with
                FightCavesMob mob = FightCavesMob.getByLevel(npc.getCombatLevel());
                // record that we have seen this mob in this round, and the location of it
                gatheredWaveCriteria.get(currentWave).specify(location, mob);
                saveGatheredWaveData();
                String message = "I saw a " + mob.toString().toLowerCase() + " in the " + location.toString().toLowerCase() + "...";
                client.addChatMessage(ChatMessageType.GAME, "", message, "");
                // maybe we have gathered enough data to know which route we are on
                attemptSolveSpawns();
            }
        }
    }

    /**
     * Dump the gathered wave criteria to json in the config
     */
    private void saveGatheredWaveData()
    {
        String json = gson.toJson(gatheredWaveCriteria);
        configManager.setConfiguration(CONFIG_GROUP, "gatheredWaveCriteria", json);
    }

    /**
     * Try to match the current specified wave information to a route
     */
    private void attemptSolveSpawns()
    {
        if (route == null)
        {
            List<FightCavesSpawnRoute> routes = predictor.findMatchingRoutes(gatheredWaveCriteria);
            if (routes.size() == 1)
            {
                String message = "Solved! You will now be shown the spawns for the rest of the game!";
                saveGatheredWaveData();
                client.addChatMessage(ChatMessageType.GAME, "", message, "");
                route = routes.get(0);
            } else
            {
                if (routes.size() < lastPossibleSpawns)
                {
                    String message = "Narrowed down to " + routes.size() + " possible spawn patterns, not long now...";
                    client.addChatMessage(ChatMessageType.GAME, "", message, "");
                    lastPossibleSpawns = routes.size();
                }
            }
        }
    }

    /**
     * Gets the next wave spawn locations
     *
     * @return A map of locations with mobs in. If nothing spawns at a location, the location won't be in the key set.
     */
    Map<FightCavesSpawnLocation, FightCavesMob> getNextSpawns()
    {
        if (route != null)
        {
            if (currentWave > 0 && currentWave < 63)
            {
                // use currentWave as an index because getSpawnsPerWave() is 0-based, so using currentWave here actually
                // gets the next wave
                return route.getSpawnsPerWave().get(currentWave);
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
