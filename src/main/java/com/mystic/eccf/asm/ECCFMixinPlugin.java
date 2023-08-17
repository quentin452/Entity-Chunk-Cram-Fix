package com.mystic.eccf.asm;

import com.falsepattern.lib.config.ConfigException;
import com.falsepattern.lib.config.ConfigurationManager;
import com.falsepattern.lib.mixin.IMixin;
import com.falsepattern.lib.mixin.IMixinPlugin;
import com.falsepattern.lib.mixin.ITargetedMod;

import com.mystic.eccf.config.ECCFeccfConfig;
import org.apache.logging.log4j.Logger;


import lombok.Getter;

public class ECCFMixinPlugin implements IMixinPlugin {

    @Getter
    private final Logger logger = IMixinPlugin.createLogger("MultithreadingAndTweaks");

    public ECCFMixinPlugin() {
        try {
            ConfigurationManager.registerConfig(ECCFeccfConfig.class);
        } catch (ConfigException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ITargetedMod[] getTargetedModEnumValues() {
        return TargetedMod.values();
    }

    @Override
    public IMixin[] getMixinEnumValues() {
        return Mixin.values();
    }
}
