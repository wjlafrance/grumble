package net.wjlafrance.grumble.commands;

import java.util.List;
import java.util.stream.Collectors;

import net.wjlafrance.grumble.data.User;
import net.wjlafrance.grumble.net.MessageSender;

import com.google.common.collect.ImmutableList;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

public @Slf4j class CommandRegistry {

	public @Setter @Getter int master;
	public @Setter @Getter String trigger;
	public @Setter @Getter List<CommandProcessor> commands;

	public void onTextMessage(User user, String text, MessageSender messageSender) {
		if (text.startsWith(trigger)) {
			List<String> tokens = ImmutableList.copyOf(text.split(" "));
			String command = tokens.get(0).replace(trigger, "");
			List<String> arguments = tokens.stream().skip(1).collect(Collectors.toList());

			if (master != user.getId()) {
				log.info("Unauthorized user {} tried to execute command {} with arguments {}", user, command, arguments);
				return;
			}

			log.info("User {} issued command {} with arguments {}", user, command, arguments);

			for (CommandProcessor processor : commands) {
				processor.process(user, command, arguments, messageSender);
			}
		}
	}

}
