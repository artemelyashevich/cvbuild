package com.bsu.cvbuilder.entity.resume.block;

public abstract class AbstractBlock {

    public abstract String getBlockName();

    public boolean isEnabled() {
        return true;
    }
}
