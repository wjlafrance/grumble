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
import com.google.protobuf.GeneratedMessage;
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
		void receivedMessage(MessageType type, GeneratedMessage message);
	}

	public enum MessageType {
		Version,
		UDPTunnel,
		Authenticate,
		Ping,
		Reject,
		ServerSync,
		ChannelRemove,
		ChannelState,
		UserRemove,
		UserState,
		BanList,
		TextMessage,
		PermissionDenied,
		ACL,
		QueryUsers,
		CryptSetup,
		ContextActionAdd,
		ContextAction,
		UserList,
		VoiceTarget,
		PermissionQuery,
		CodecVersion,
		UserStats,
		RequestBlob,
		ServerConfig
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
				sendMessage(MessageType.Ping, Mumble.Ping.newBuilder().build());
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

	private void sendMessage(MessageType type, GeneratedMessage message) throws IOException {
		outputStream.writeShort(type.ordinal());
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

		// this is terrible
		if (MessageType.Version.ordinal() == type) {
			callback.receivedMessage(MessageType.Version, Mumble.Version.parseFrom(data));
		} else if (MessageType.UDPTunnel.ordinal() == type) {
			callback.receivedMessage(MessageType.UDPTunnel, Mumble.UDPTunnel.parseFrom(data));
		} else if (MessageType.Ping.ordinal() == type) {
			callback.receivedMessage(MessageType.Ping, Mumble.Ping.parseFrom(data));
		} else if (MessageType.ServerSync.ordinal() == type) {
			callback.receivedMessage(MessageType.ServerSync, Mumble.ServerSync.parseFrom(data));
		} else if (MessageType.ChannelState.ordinal() == type) {
			callback.receivedMessage(MessageType.ChannelState, Mumble.ChannelState.parseFrom(data));
		} else if (MessageType.UserState.ordinal() == type) {
			callback.receivedMessage(MessageType.UserState, Mumble.UserState.parseFrom(data));
		} else if (MessageType.ACL.ordinal() == type) {
			callback.receivedMessage(MessageType.ACL, Mumble.ACL.parseFrom(data));
		} else if (MessageType.CryptSetup.ordinal() == type) {
			callback.receivedMessage(MessageType.CryptSetup, Mumble.CryptSetup.parseFrom(data));
		} else if (MessageType.PermissionQuery.ordinal() == type) {
			callback.receivedMessage(MessageType.PermissionQuery, Mumble.PermissionQuery.parseFrom(data));
		} else if (MessageType.CodecVersion.ordinal() == type) {
			callback.receivedMessage(MessageType.CodecVersion, Mumble.CodecVersion.parseFrom(data));
		} else if (MessageType.ServerConfig.ordinal() == type) {
			callback.receivedMessage(MessageType.ServerConfig, Mumble.ServerConfig.parseFrom(data));
		} else {
			System.out.format("Unexpected message type received: %d\n", type);
		}
	}

	@Override public void run() {
		try {
			this.connectionState = State.Connecting;
			SocketFactory socketFactory = OverlyTrustingSSLContext.getInstance().getSocketFactory();
			try (Socket socket = socketFactory.createSocket(InetAddress.getByName(this.hostname), this.port)) {
				this.inputStream = new DataInputStream(socket.getInputStream());
				this.outputStream = new DataOutputStream(socket.getOutputStream());

				sendMessage(MessageType.Version, Mumble.Version.newBuilder().setVersion(1).setRelease("Grumble 1.0.0-SNAPSHOT").build());

				sendMessage(MessageType.Authenticate, Mumble.Authenticate.newBuilder().setUsername(this.username).build());

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
