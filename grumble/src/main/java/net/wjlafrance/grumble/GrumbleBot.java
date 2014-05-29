package net.wjlafrance.grumble;

import java.util.stream.Collectors;

import net.wjlafrance.grumble.commands.CommandRegistry;
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
import MumbleProto.Mumble.PermissionDenied;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

public @Slf4j class GrumbleBot {

	private @Getter @Setter String hostname;
	private @Getter @Setter int port;
	private @Getter @Setter String username;
	private @Getter @Setter String password;

	private @Getter @Setter UserList userList;
	private @Getter @Setter ChannelList channelList;

	private @Getter @Setter CommandRegistry commands;

	private MurmurThread connection;

	public void start() {
		this.connection = new MurmurThread(hostname, port, username, password);

		this.connection.setCallback((message) -> {
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
			} else if (message instanceof PermissionDenied) {
				onPermissionDenied((PermissionDenied) message);
			} else {
				log.warn("Received unexpected message from server: {}", message.getClass());
			}
		});

		this.connection.setUdpCallback((packet, session, sequence, data) -> {
			switch (packet) {
				case CELTAlpha:
					log.debug("Received CELT alpha encoded voice data, user {}, sequence {}, {} bytes",
							userList.getUserForSession(session).getName(), sequence, data.available());
					break;
				case Ping:
					break;
				case Speex:
					log.debug("Received Speex encoded voice data, user {}, sequence {}, {} bytes",
							userList.getUserForSession(session).getName(), sequence, data.available());
					break;
				case CELTBeta:
					log.debug("Received CELT beta encoded voice data, user {}, sequence {}, {} bytes",
							userList.getUserForSession(session).getName(), sequence, data.available());
					break;
			}
		});

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
		log.info("CodecVersion: alpha: 0x{}, beta: 0x{}, prefer alpha: {}", Integer.toHexString(message.getAlpha()),
				Integer.toHexString(message.getBeta()), message.getPreferAlpha() ? "true" : "false");
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

		if (this.username.equals(user.getName())) {
			this.channelList.setCurrentChannel(channelList.getChannelForId(message.getChannelId()));
		}
	}

	private void onPing(Ping message) {
		log.debug("Ping received: {} good, {} late, {} lost, {} resync, {} tcp, {} udp, {} udp avg, {} udp var, {} tcp avg, {} tcp var",
				message.getGood(), message.getLate(), message.getLost(), message.getResync(), message.getUdpPackets(),
				message.getTcpPackets(), message.getUdpPingAvg(), message.getUdpPingVar(), message.getTcpPingAvg(),
				message.getTcpPingVar());
	}

	private void onTextMessage(TextMessage message) {
		log.info("TextMessage: [{} to users {}, channels {}]: {}", userList.getUserForSession(message.getActor()),
				message.getSessionList().stream().map(id -> userList.getUserForSession(id).getName()).collect(Collectors.joining(", ")),
				message.getChannelIdList().stream().map(id -> channelList.getChannelForId(id).getName()).collect(Collectors.joining(", ")),
				message.getMessage());

		commands.onTextMessage(userList.getUserForSession(message.getActor()), message.getMessage(), this.connection);
	}

	private void onServerSync(ServerSync message) {
		log.info("Server sync: {}", message.getWelcomeText());
	}

	private void onPermissionDenied(PermissionDenied message) {
		log.warn("Permission denied: {}, {}, {}", message.getPermission(), message.getType(), message.getReason());
	}

}
