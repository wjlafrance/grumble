package net.wjlafrance.grumble.net;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import javax.net.SocketFactory;

import MumbleProto.Mumble;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

public @RequiredArgsConstructor @Slf4j class MurmurThread extends Thread {

	public static interface MessageCallback {
		void receivedMessage(Message message);
	}

	public static interface UdpCallback {
		void receivedPacket(UDPPacket packetType, int session, int sequence);
	}

	public enum UDPPacket {
		CELTAlpha,
		Ping,
		Speex,
		CELTBeta
	}

	private enum State {
		Disconnected,
		Connecting,
		Connected
	}

	private final String hostname;
	private final int port;
	private final String username;
	private final MessageCallback callback;
	private final UdpCallback udpCallback;

	private final Thread pingThread = new Thread(() -> {
		while (getConnectionState() == State.Connected) {
			try {
				Thread.sleep(15000);
			} catch (InterruptedException ex) {
				log.warn("Caught InterruptedException while sleeping in pingThread. Resuming immediately.", ex);
			}

			try {
				sendMessage(Mumble.Ping.newBuilder().setTimestamp(System.currentTimeMillis()).build());
				log.debug("Sent ping");
			} catch (IOException ex) {
				log.error("IOException while sending ping! Disconnecting.", ex);
				disconnect();
				return;
			}
		}
	});

	private @Getter State connectionState = State.Disconnected;

	private Socket socket;
	private DataOutputStream outputStream;
	private DataInputStream inputStream;

	public void disconnect() {
		try {
			if (null != socket) {
				socket.close();
			}
			if (null != outputStream) {
				outputStream.close();
			}
			if (null != inputStream) {
				inputStream.close();
			}
		} catch (IOException ex) {
			log.error("Caught IOException while closing socket and streams!", ex);
		}
		this.connectionState = State.Disconnected;
	}

	public void sendMessage(GeneratedMessage message) throws IOException {
		outputStream.writeShort(message.getDescriptorForType().getIndex());
		outputStream.writeInt(message.getSerializedSize());
		message.writeTo(outputStream);
	}

	private void receiveMessage(MessageCallback callback) throws IOException {
		int type = inputStream.readShort();
		int length = inputStream.readInt();

		byte[] data = new byte[length];
		int bytesRead = inputStream.read(data, 0, length);

		if (bytesRead != length) {
			throw new IOException("Didn't read expected amount of bytes.");
		}

		ImmutableList<Message> messageTypes = ImmutableList.of(
			Mumble.Version.newBuilder().getDefaultInstanceForType(),
			Mumble.UDPTunnel.newBuilder().getDefaultInstanceForType(),
			Mumble.Authenticate.newBuilder().getDefaultInstanceForType(),
			Mumble.Ping.newBuilder().getDefaultInstanceForType(),
			Mumble.Reject.newBuilder().getDefaultInstanceForType(),
			Mumble.ServerSync.newBuilder().getDefaultInstanceForType(),
			Mumble.ChannelRemove.newBuilder().getDefaultInstanceForType(),
			Mumble.ChannelState.newBuilder().getDefaultInstanceForType(),
			Mumble.UserRemove.newBuilder().getDefaultInstanceForType(),
			Mumble.UserState.newBuilder().getDefaultInstanceForType(),
			Mumble.BanList.newBuilder().getDefaultInstanceForType(),
			Mumble.TextMessage.newBuilder().getDefaultInstanceForType(),
			Mumble.PermissionDenied.newBuilder().getDefaultInstanceForType(),
			Mumble.ACL.newBuilder().getDefaultInstanceForType(),
			Mumble.QueryUsers.newBuilder().getDefaultInstanceForType(),
			Mumble.CryptSetup.newBuilder().getDefaultInstanceForType(),
			Mumble.ContextActionModify.newBuilder().getDefaultInstanceForType(),
			Mumble.ContextAction.newBuilder().getDefaultInstanceForType(),
			Mumble.UserList.newBuilder().getDefaultInstanceForType(),
			Mumble.VoiceTarget.newBuilder().getDefaultInstanceForType(),
			Mumble.PermissionQuery.newBuilder().getDefaultInstanceForType(),
			Mumble.CodecVersion.newBuilder().getDefaultInstanceForType(),
			Mumble.UserStats.newBuilder().getDefaultInstanceForType(),
			Mumble.RequestBlob.newBuilder().getDefaultInstanceForType(),
			Mumble.ServerConfig.newBuilder().getDefaultInstanceForType()
		);

		if (1 == type) { // UDP packet
			receiveUdpPacket(data);
		} else {
			try {
				Message message = messageTypes.get(type).getParserForType().parseFrom(data);
				callback.receivedMessage(message);
			} catch (InvalidProtocolBufferException ex) {
				log.warn("Invalid protocol buffer message received. Ignoring it.", ex);
			}
		}
	}

	private void receiveUdpPacket(byte[] packet) throws IOException {
		ByteArrayInputStream packetInputStream = new ByteArrayInputStream(packet);
		packetInputStream.skip(1);

		int type = packet[0] >> 5 & 0x07;
//		int target = packet[0] & 0x1F;
		int session = NetUtils.readVarint(packetInputStream);
		int sequence = NetUtils.readVarint(packetInputStream);

		switch (type) {
			case 0: udpCallback.receivedPacket(UDPPacket.CELTAlpha, session, sequence); break;
			case 1: udpCallback.receivedPacket(UDPPacket.Ping, 0, 0); break;
			case 2: udpCallback.receivedPacket(UDPPacket.Speex, session, sequence); break;
			case 3: udpCallback.receivedPacket(UDPPacket.CELTBeta, session, sequence); break;
			default:
				log.warn("Received unrecognized UDP packet type: {}", type);
				break;
		}
	}

	@Override public void run() {
		try {
			this.connectionState = State.Connecting;
			SocketFactory socketFactory = OverlyTrustingSSLContext.getInstance().getSocketFactory();
			this.socket = socketFactory.createSocket(InetAddress.getByName(this.hostname), this.port);
			this.inputStream = new DataInputStream(socket.getInputStream());
			this.outputStream = new DataOutputStream(socket.getOutputStream());

			sendMessage(Mumble.Version.newBuilder().setVersion(1).setRelease("Grumble 1.0.0-SNAPSHOT").build());

			sendMessage(Mumble.Authenticate.newBuilder().setUsername(this.username).build());

			this.connectionState = State.Connected;
			pingThread.start();

			while (this.connectionState == State.Connected) {
				receiveMessage(this.callback);
			}
		} catch (NoSuchAlgorithmException | KeyManagementException | IOException ex) {
			log.error("Caught exception in MurmurThread loop.", ex);
			disconnect();
		}
	}

}
