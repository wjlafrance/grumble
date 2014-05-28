package net.wjlafrance.grumble;

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
import lombok.RequiredArgsConstructor;

	/*
	private static void debugOutput(byte[] bytes) {
		for (int i = 0; i < bytes.length; i += 16) {
			for (int j = i; j < (i + 16); j++) {
				if (j < bytes.length) {
					System.out.format("%02x ", bytes[j]);
				} else {
					System.out.print("   ");
				}
			}
			System.out.print("   ");
			for (int j = i; j < (i + 16); j++) {
				if (j < bytes.length) {
					char c = (char) bytes[j];
					if (Character.isAlphabetic(c) || Character.isDigit(c)) {
						System.out.format("%s", Character.valueOf(c).toString());
					} else {
						System.out.print(".");
					}
				} else {
					System.out.print(" ");
				}
			}
			System.out.println();
		}
		System.out.println();
	}
	*/

public @RequiredArgsConstructor class MurmurThread extends Thread {

	public static interface MessageCallback {
		void receivedMessage(Message message);
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

	private State connectionState = State.Disconnected;

	private Thread pingThread = new Thread(() -> {
		while (connectionState == State.Connected) {
			try {
				sendMessage(Mumble.Ping.newBuilder().build());
				System.out.println("Sent ping.");
			} catch (IOException ex) {
				ex.printStackTrace();
				connectionState = State.Disconnected;
				return;
			}
			try {
				Thread.sleep(15000);
			} catch (InterruptedException ex) {
				ex.printStackTrace();
			}
		}
	});

	private DataOutputStream outputStream;
	private DataInputStream inputStream;

	private void sendMessage(GeneratedMessage message) throws IOException {
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
			System.out.format("Received UDP packet (%d bytes)\n", length);
			return;
		}

		try {
			Message message = messageTypes.get(type).getParserForType().parseFrom(data);
			callback.receivedMessage(message);
		} catch (InvalidProtocolBufferException ex) {
			ex.printStackTrace();
		}
	}

	@Override public void run() {
		try {
			this.connectionState = State.Connecting;
			SocketFactory socketFactory = OverlyTrustingSSLContext.getInstance().getSocketFactory();
			try (Socket socket = socketFactory.createSocket(InetAddress.getByName(this.hostname), this.port)) {
				this.inputStream = new DataInputStream(socket.getInputStream());
				this.outputStream = new DataOutputStream(socket.getOutputStream());

				sendMessage(Mumble.Version.newBuilder().setVersion(1).setRelease("Grumble 1.0.0-SNAPSHOT").build());

				sendMessage(Mumble.Authenticate.newBuilder().setUsername(this.username).build());

				this.connectionState = State.Connected;
				pingThread.start();

				while (this.connectionState == State.Connected) {
					receiveMessage(this.callback);
				}
			}
		} catch (NoSuchAlgorithmException | KeyManagementException | IOException ex) {
			ex.printStackTrace();
			connectionState = State.Disconnected;
		}
	}

}
