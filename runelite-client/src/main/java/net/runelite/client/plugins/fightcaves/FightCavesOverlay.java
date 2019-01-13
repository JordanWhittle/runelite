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
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.client.util.ImageUtil;

import javax.inject.Inject;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/**
 * Overlay for the Fight Caves plugin.
 */
public class FightCavesOverlay extends Overlay
{
    private Client client;
    private FightCavesPlugin plugin;
    private Map<FightCavesMob, BufferedImage> mobImages = new HashMap<>();

    @Inject
    private FightCavesOverlay(Client client, FightCavesPlugin plugin)
    {
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_MAP);
        this.client = client;
        this.plugin = plugin;
        // load all of the mob images
        mobImages.put(FightCavesMob.TZ_KIH, loadImage("Tz-Kih"));
        mobImages.put(FightCavesMob.TZ_KEK, loadImage("Tz-Kek"));
        mobImages.put(FightCavesMob.TOK_XIL, loadImage("Tok-Xil"));
        mobImages.put(FightCavesMob.YT_MEJKOT, loadImage("Yt-MejKot"));
        mobImages.put(FightCavesMob.KET_ZEK, loadImage("Ket-Zek"));
        mobImages.put(FightCavesMob.TZTOK_JAD, loadImage("TzTok-Jad"));
    }

    private BufferedImage loadImage(String name)
    {
        return makeSmall(ImageUtil.getResourceStreamFromClass(getClass(), name + ".png"));
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (plugin.isInFightCaves())
        {
            Map<FightCavesSpawnLocation, FightCavesMob> spawns = plugin.getNextSpawns();
            if (spawns != null)
            {
                spawns.forEach((spawnLoc, mob) -> {
                    FightCavesArea area = spawnLoc.getSpawnArea();
                    WorldPoint point = FightCavesUtils.getWorldPoint(client, FightCavesPlugin.FIGHT_CAVES_REGION_ID, area.getCenterX(), area.getCenterY());
                    if (point != null)
                    {
                        if (mobImages.containsKey(mob))
                        {
                            OverlayUtil.renderImageLocation(client, graphics, LocalPoint.fromWorld(client, point), mobImages.get(mob), 128);
                        }
                    }
                });
            }
        }
        return null;
    }

    private static BufferedImage resize(BufferedImage img, int newW, int newH)
    {
        Image tmp = img.getScaledInstance(newW, newH, Image.SCALE_SMOOTH);
        BufferedImage dimg = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2d = dimg.createGraphics();
        g2d.drawImage(tmp, 0, 0, null);
        g2d.dispose();

        return dimg;
    }

    private static BufferedImage makeSmall(BufferedImage image)
    {
        return resize(image, 96, 96);
    }
}
