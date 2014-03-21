package mods.immibis.chunkloader;

import net.minecraft.util.ResourceLocation;

public class R {
	public static class gui {
		public static final ResourceLocation fuel   = new ResourceLocation("dimanchor", "textures/gui/fueled.png");
		public static final ResourceLocation nofuel = new ResourceLocation("dimanchor", "textures/gui/unfueled.png");
	}
	
	// relative to textures/blocks
	public static class block {
		public static final String chunkloader = "dimanchor:chunkloader";
	}
	
	public static class string {
		public static class block {
			// tile. prefix, .name suffix
			public static final String chunkloader = "dimanchor";
		}
		public static class gui {
			public static final String setOwnerToYou = "gui.dimanchor.setOwnerToYou";
			public static final String setOwnerToServer = "gui.dimanchor.setOwnerToServer";
			public static final String shape = "gui.dimanchor.shape";
			public static final String square = "gui.dimanchor.shape.square";
			public static final String lineX = "gui.dimanchor.shape.lineX";
			public static final String lineZ = "gui.dimanchor.shape.lineZ";
			public static final String serverowned = "gui.dimanchor.owner.server";
			public static final String owner = "gui.dimanchor.owner";
			public static final String unlimited = "gui.dimanchor.unlimited";
			public static final String yourChunkLimit = "gui.dimanchor.limit.yours";
			public static final String theirChunkLimit = "gui.dimanchor.limit.other";
			public static final String area = "gui.dimanchor.area";
			public static final String areaNone = "gui.dimanchor.area.none";
			public static final String lastLine = "gui.dimanchor.lastline";
			public static final String active = "gui.dimanchor.active";
			public static final String inactive = "gui.dimanchor.inactive";
			public static final String fuelLeft = "gui.dimanchor.fuelleft";
		}
	}
	
}
