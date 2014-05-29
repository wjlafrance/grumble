package net.wjlafrance.grumble.commands;

import java.util.List;
import java.util.stream.Collectors;

import net.wjlafrance.grumble.data.Channel;
import net.wjlafrance.grumble.data.ChannelList;
import net.wjlafrance.grumble.data.User;
import net.wjlafrance.grumble.net.MessageSender;

import MumbleProto.Mumble.TextMessage;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

public @Slf4j class SayCommand implements CommandProcessor {

	private @Getter @Setter ChannelList channelList;

	@Override public void process(User user, String command, List<String> arguments, MessageSender messageSender) {
		if ("say".equals(command)) {
			Channel channel = channelList.getCurrentChannel();
			if (null == channel) {
				log.warn("Trying to issue a say command when we're apparently not in a channel.");
				return;
			}

			messageSender.sendMessage(TextMessage.newBuilder()
					.addChannelId(channel.getId())
					.setMessage(arguments.stream().collect(Collectors.joining(" ")))
					.build());
		}
	}

}
