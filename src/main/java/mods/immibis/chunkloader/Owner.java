package mods.immibis.chunkloader;

public class Owner {
	public static final String SERVER_OWNER_STRING = "1";
	public static final String DATA_LOST_STRING = "3";
	public static final String NON_PLAYER_STRING = "4";
	
	public static String getPlayerOwnerString(String username) {
		return "2" + username;
	}

	public static String getDisplayString(String owner) {
		if(owner.equals("")) return "invalid";
		
		switch(owner.charAt(0)) {
		case '1': return "<server>";
		case '2': return owner.substring(1);
		case '3': return "<data lost; report this bug>";
		case '4': return "<non-player>";
		default: return "invalid:"+owner;
		}
	}

	public static String getUsername(String owner) {
		if(owner.length() > 0 && owner.charAt(0) == '2')
			return owner.substring(1);
		else
			return null;
	}
}
