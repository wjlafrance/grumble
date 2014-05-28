package net.wjlafrance.grumble;

import MumbleProto.Mumble.Version;
import MumbleProto.Mumble.CodecVersion;
import MumbleProto.Mumble.UserState;
import MumbleProto.Mumble.ChannelState;
import MumbleProto.Mumble.Ping;
import MumbleProto.Mumble.TextMessage;
import MumbleProto.Mumble.ServerSync;
import lombok.extern.slf4j.Slf4j;

public @Slf4j class GrumbleBot {

	private static final String HOSTNAME = "aegiscompany.net";
	private static final int PORT = 64738;
	private static final String USERNAME = "GrumbleBot";

	private final MurmurThread connection;

	public GrumbleBot() {
		this.connection = new MurmurThread(HOSTNAME, PORT, USERNAME, (message) -> {
			if (message instanceof Version) {
				onVersion((Version) message);
			} else if (message instanceof CodecVersion) {
				onCodecVersion((CodecVersion) message);
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
				log.warn("Received unexpected message from server: {}", message.getClass());
			}
		});
		connection.start();
	}

	private void onVersion(Version message) {
		int serverVersion = message.getVersion();
		String serverVersionString = String.format("%d.%d.%d", serverVersion >> 16 & 0xFFFF, serverVersion >> 8 & 0xFF,
				serverVersion & 0xFF);

		log.info("Server version: {} (release: {}), OS: {} {}", serverVersionString, message.getRelease(),
				message.getOs(), message.getOsVersion());
	}

	private void onCodecVersion(CodecVersion message) {
		int alpha = message.getAlpha();
		int beta = message.getBeta();
		String alphaString = String.format("%d.%d.%d", alpha >> 16 & 0xFFFF, alpha >> 8 & 0xFF, alpha & 0xFF);
		String betaString = String.format("%d.%d.%d", beta >> 16 & 0xFFFF, beta >> 8 & 0xFF, beta & 0xFF);

		log.info("CodecVersion: alpha: {}, beta: {}, prefer alpha: {}", alphaString, betaString,
				message.getPreferAlpha() ? "true" : "false");
	}

	private void onChannelState(ChannelState message) {
		Channel channel = new Channel();
		channel.setId(message.getChannelId());
		channel.setParentId(message.getParent());
		channel.setName(message.getName());
		channel.setDescription(message.getDescription());
		channel.setTemporary(message.getTemporary());
		channel.setPosition(message.getPosition());

		log.info("Channel state: {}", channel.toString());
	}

	private void onUserState(UserState message) {
		User user = new User();
		user.setSessionId(message.getSession());
		user.setId(message.getUserId());
		user.setName(message.getName());
		user.setChannelId(message.getChannelId());

		log.info("User state: {}", user.toString());
	}

	private void onPing(Ping message) {
		log.debug("Ping received: {} good, {} late, {} lost, {} resync, {} tcp, {} udp, {} udp avg, {} udp var, {} tcp avg, {} tcp var\n",
				message.getGood(), message.getLate(), message.getLost(), message.getResync(), message.getUdpPackets(),
				message.getTcpPackets(), message.getUdpPingAvg(), message.getUdpPingVar(), message.getTcpPingAvg(),
				message.getTcpPingVar());
	}

	private void onTextMessage(TextMessage message) {
		log.info("TextMessage: Actor {} to sessions {}, channels {}:\n{}", message.getActor(), message.getSessionList(),
				message.getChannelIdList(), message.getMessage());
	}

	private void onServerSync(ServerSync message) {
		log.info("Server sync: {}", message.getWelcomeText());
	}

	public static void main(String args[]) {
		new GrumbleBot();
	}

}
