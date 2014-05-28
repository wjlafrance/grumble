package net.wjlafrance.grumble;

import java.io.IOException;

import MumbleProto.Mumble.Version;
import MumbleProto.Mumble.UserState;
import MumbleProto.Mumble.ChannelState;
import MumbleProto.Mumble.Ping;
import MumbleProto.Mumble.TextMessage;
import MumbleProto.Mumble.ServerSync;

public class GrumbleBot {

	private static final String HOSTNAME = "aegiscompany.net";
	private static final int PORT = 64738;
	private static final String USERNAME = "GrumbleBot";

	private MurmurThread connection;

	public GrumbleBot() {
		connection = new MurmurThread(HOSTNAME, PORT, USERNAME, (message) -> {
			if (message instanceof Version) {
				onVersion((Version) message);
			} else if (message instanceof ChannelState) {
				onChannelState((ChannelState) message);
			} else if (message instanceof UserState) {
				onUserState((UserState) message);
			} else if (message instanceof Ping) {
				onPing((Ping) message);
			} else if (message instanceof TextMessage) {
				onTextMessage((TextMessage) message);
			} else if (message instanceof ServerSync) {
				onServerSync((ServerSync) message);
			} else {
				System.out.format("Received unexpected response type! %s\n", message.getClass());
			}
		});
		connection.start();
	}

	private void onVersion(Version message) {
		System.out.format("Server version: %d, release: %s, OS: %s, OS version: %s\n",
				message.getVersion(), message.getRelease(), message.getOs(),
				message.getOsVersion());
	}

	private void onChannelState(ChannelState message) {
		Channel channel = new Channel();
		channel.setId(message.getChannelId());
		channel.setParentId(message.getParent());
		channel.setName(message.getName());
		channel.setDescription(message.getDescription());
		channel.setTemporary(message.getTemporary());
		channel.setPosition(message.getPosition());

		System.out.format("Channel state: %s\n", channel.toString());
	}

	private void onUserState(UserState message) {
		User user = new User();
		user.setSessionId(message.getSession());
		user.setId(message.getUserId());
		user.setName(message.getName());
		user.setChannelId(message.getChannelId());

		System.out.format("User state: %s\n", user.toString());
	}

	private void onPing(Ping message) {
		System.out.format("Ping received: %d good, %d late, %d lost, %d resync, %d tcp, %d udp, %f udp avg, %f udp var, %f tcp avg, %f tcp var\n",
				message.getGood(), message.getLate(), message.getLost(), message.getResync(), message.getUdpPackets(),
				message.getTcpPackets(), message.getUdpPingAvg(), message.getUdpPingVar(), message.getTcpPingAvg(),
				message.getTcpPingVar());
	}

	private void onTextMessage(TextMessage message) {
		System.out.format("TextMessage: [Actor %d]: %s\n", message.getActor(), message.getMessage());
	}

	private void onServerSync(ServerSync message) {
		System.out.println("--------------------");
		System.out.format("%s\n", message.getWelcomeText());
		System.out.println("--------------------");
	}

	public static void main(String args[]) {
		new GrumbleBot();
	}

}
