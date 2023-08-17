package com.mystic.eccf.config;

import net.minecraft.client.gui.GuiScreen;

import com.falsepattern.lib.config.ConfigException;
import com.falsepattern.lib.config.SimpleGuiConfig;
import com.falsepattern.lib.internal.Tags;

public class MultithreadingandtweaksGuiConfig extends SimpleGuiConfig {

    public MultithreadingandtweaksGuiConfig(GuiScreen parent) throws ConfigException {
        super(parent, MultithreadingandtweaksMultithreadingConfig.class, Tags.MODID, Tags.MODNAME);
    }
}
