package com.mystic.eccf.config;

import com.falsepattern.lib.config.Config;
import com.mystic.eccf.Tags;

@Config(modid = Tags.MODID)
public class ECCFeccfConfig {

    // Make inconfig ingame
    // make categories

    @Config.Comment("Maximum number of entities can spawn in a chunk.")
    @Config.DefaultInt(25)
    @Config.RangeInt(min = 0, max = 250)
    @Config.RequiresWorldRestart
    public static int maxEntitiesPerChunk;
    @Config.Comment("Here is a blacklist to make sure that mobs in the blacklist can spawn without limie.")
    @Config.DefaultStringList({ "WitherBoss", "EnderDragon", "SnowMan" })
    public static String[] entityBlacklistIds;

}
