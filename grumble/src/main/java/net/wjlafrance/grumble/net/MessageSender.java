package net.wjlafrance.grumble.net;

import com.google.protobuf.Message;

public interface MessageSender {

	void sendMessage(Message message);

}
