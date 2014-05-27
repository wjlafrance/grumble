package net.wjlafrance.grumble;

import MumbleProto.Mumble.Version;
import MumbleProto.Mumble.UserState;
import MumbleProto.Mumble.Ping;
import MumbleProto.Mumble.TextMessage;

public class GrumbleBot {

	private static final String HOSTNAME = "aegiscompany.net";
	private static final int PORT = 64738;
	private static final String USERNAME = "GrumbleBot";

	public GrumbleBot() {
		Thread connection = new MurmurThread(HOSTNAME, PORT, USERNAME, (message) -> {
			if (message instanceof Version) {
				onVersion((Version) message);
			} else if (message instanceof UserState) {
				onUserState((UserState) message);
			} else if (message instanceof Ping) {
				onPing((Ping) message);
			} else if (message instanceof TextMessage) {
				onTextMessage((TextMessage) message);
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

	private void onUserState(UserState message) {
		System.out.format("User state: user %s(id %d, actor %d) is in channel %d\n", message.getName(),
				message.getActor(), message.getUserId(), message.getChannelId());
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

	public static void main(String args[]) {
		new GrumbleBot();
	}

}
