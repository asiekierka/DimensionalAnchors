package mods.immibis.chunkloader.coremod;

import java.util.Arrays;

import cpw.mods.fml.common.DummyModContainer;
import cpw.mods.fml.common.ModMetadata;

public class DACoreModContainer extends DummyModContainer {
	public DACoreModContainer() {
		super(new ModMetadata());
		ModMetadata meta = super.getMetadata();
		meta.modId = "DimensionalAnchorsCore";
		meta.name = "Dimensional Anchors Coremod";
		meta.description = "Fixes certain things needed to run DimensionalAnchors";
		meta.authorList = Arrays.asList(new String[]{"immibis", "asiekierka"});
		meta.parent = "asielib";
		this.setEnabledState(true);
	}
	
	public Disableable canBeDisabled() {
		return Disableable.NEVER;
	}
}
