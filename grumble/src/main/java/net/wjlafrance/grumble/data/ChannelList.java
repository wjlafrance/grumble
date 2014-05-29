package net.wjlafrance.grumble.data;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ChannelList {

	private Map<Integer, Channel> channels = new HashMap<>();

	public Channel getChannelForId(int id) {
		return channels.computeIfAbsent(id, (newId) -> {
			Channel newChannel = new Channel();
			newChannel.setId(newId);
			return newChannel;
		});
	}

	public Optional<Channel> getChannelForName(String name) {
		return channels.values().stream().filter(channel -> name.equals(channel.getName())).findAny();
	}

	public void deleteChannel(int id) {
		channels.remove(id);
	}

}
