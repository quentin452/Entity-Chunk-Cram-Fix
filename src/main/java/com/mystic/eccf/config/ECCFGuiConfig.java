package com.mystic.eccf.config;

import net.minecraft.client.gui.GuiScreen;

import com.falsepattern.lib.config.ConfigException;
import com.falsepattern.lib.config.SimpleGuiConfig;
import com.mystic.eccf.Tags;

public class ECCFGuiConfig extends SimpleGuiConfig {

    public ECCFGuiConfig(GuiScreen parent) throws ConfigException {
        super(parent, ECCFeccfConfig.class, Tags.MODID, Tags.MODNAME);
    }
}
