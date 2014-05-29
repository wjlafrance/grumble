package net.wjlafrance.grumble.commands;

import java.util.List;

import net.wjlafrance.grumble.data.User;
import net.wjlafrance.grumble.net.MessageSender;

public interface CommandProcessor {

	void process(User user, String command, List<String> arguments, MessageSender messageSender);

}
