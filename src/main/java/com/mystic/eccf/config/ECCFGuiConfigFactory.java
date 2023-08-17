package com.mystic.eccf.config;

import net.minecraft.client.gui.GuiScreen;

import com.falsepattern.lib.config.SimpleGuiFactory;

public class ECCFGuiConfigFactory implements SimpleGuiFactory {

    @Override
    public Class<? extends GuiScreen> mainConfigGuiClass() {
        return ECCFGuiConfig.class;
    }
}
