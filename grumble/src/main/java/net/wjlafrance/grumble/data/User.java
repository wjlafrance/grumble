package net.wjlafrance.grumble.data;

import lombok.Data;

public @Data class User {
	private String name;
	private int id;
	private int sessionId;
	private int channelId;
}
