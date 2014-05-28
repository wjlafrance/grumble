package net.wjlafrance.grumble.data;

import java.util.HashMap;
import java.util.Map;

public class ChannelList {

	private Map<Integer, Channel> channels = new HashMap<>();

	public Channel getChannelForId(int id) {
		return channels.computeIfAbsent(id, (newId) -> {
			Channel newChannel = new Channel();
			newChannel.setId(newId);
			return newChannel;
		});
	}

	public void deleteChannel(int id) {
		channels.remove(id);
	}

}
