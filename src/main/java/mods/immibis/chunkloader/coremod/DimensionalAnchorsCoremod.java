package mods.immibis.chunkloader.coremod;

import java.util.Map;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;

public class DimensionalAnchorsCoremod implements IFMLLoadingPlugin {

	@Override
	public String[] getASMTransformerClass() {
		return null;
	}

	@Override
	public String getModContainerClass() {
		return "mods.immibis.chunkloader.coremod.DACoreModContainer";
	}

	@Override
	public String getSetupClass() {
		return null;
	}

	@Override
	public void injectData(Map<String, Object> data) {
	}

	@Override
	public String getAccessTransformerClass() {
		return "mods.immibis.chunkloader.coremod.DAAccessTransformer";
	}

}
