package mods.immibis.chunkloader.coremod;

import java.io.IOException;

import cpw.mods.fml.common.asm.transformers.AccessTransformer;

public class DAAccessTransformer extends AccessTransformer {
	public DAAccessTransformer() throws IOException {
		super("DimensionalAnchors_at.cfg");
	}
}
