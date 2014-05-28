package net.wjlafrance.grumble;

import lombok.Data;

public @Data class Channel {
	private int id;
	private int parentId;
	private String name;
	private String description;
	private boolean temporary;
	private int position;
}
