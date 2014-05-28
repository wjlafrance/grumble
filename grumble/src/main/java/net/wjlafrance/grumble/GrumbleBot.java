package net.wjlafrance.grumble;

import net.wjlafrance.grumble.data.Channel;
import net.wjlafrance.grumble.data.ChannelList;
import net.wjlafrance.grumble.data.User;
import net.wjlafrance.grumble.data.UserList;
import net.wjlafrance.grumble.net.MurmurThread;

import MumbleProto.Mumble.ChannelState;
import MumbleProto.Mumble.CodecVersion;
import MumbleProto.Mumble.Ping;
import MumbleProto.Mumble.ServerSync;
import MumbleProto.Mumble.TextMessage;
import MumbleProto.Mumble.UserState;
import MumbleProto.Mumble.Version;
import lombok.extern.slf4j.Slf4j;

public @Slf4j class GrumbleBot {

	private static final String HOSTNAME = "aegiscompany.net";
	private static final int PORT = 64738;
	private static final String USERNAME = "GrumbleBot";

	private final MurmurThread connection = new MurmurThread(HOSTNAME, PORT, USERNAME, (message) -> {
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

	private final UserList userList = new UserList();
	private final ChannelList channelList = new ChannelList();

	public GrumbleBot() {
		this.connection.start();
	}

	private void onVersion(Version message) {
		int serverVersion = message.getVersion();
		String serverVersionString = String.format("%d.%d.%d", serverVersion >> 16 & 0xFFFF, serverVersion >> 8 & 0xFF,
				serverVersion & 0xFF);

		log.info("Server version: {} (release: {}), OS: {} {}", serverVersionString, message.getRelease(),
				message.getOs(), message.getOsVersion());
	}

	private void onCodecVersion(CodecVersion message) {
		log.info("CodecVersion: alpha: {}, beta: {}, prefer alpha: {}", message.getAlpha(), message.getBeta(),
				message.getPreferAlpha() ? "true" : "false");
	}

	private void onChannelState(ChannelState message) {
		Channel channel = channelList.getChannelForId(message.getChannelId());
		if (message.hasParent()) {
			channel.setParentId(message.getParent());
		}
		if (message.hasName()) {
			channel.setName(message.getName());
		}
		if (message.hasTemporary()) {
			channel.setTemporary(message.getTemporary());
		}
		if (message.hasPosition()) {
			channel.setPosition(message.getPosition());
		}

		log.info("Channel state: {}", channel);
	}

	private void onUserState(UserState message) {
		User user = userList.getUserForSession(message.getSession());
		if (message.hasUserId()) {
			user.setId(message.getUserId());
		}
		if (message.hasName()) {
			user.setName(message.getName());
		}
		if (message.hasChannelId()) {
			user.setChannelId(message.getChannelId());
		}

		log.info("User state: {}", user);
	}

	private void onPing(Ping message) {
		log.debug("Ping received: {} good, {} late, {} lost, {} resync, {} tcp, {} udp, {} udp avg, {} udp var, {} tcp avg, {} tcp var",
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
