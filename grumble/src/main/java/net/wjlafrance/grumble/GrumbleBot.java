package net.wjlafrance.grumble;

import MumbleProto.Mumble;

public class GrumbleBot {

	public static void main(String args[]) {
		Thread connection = new MurmurThread("aegiscompany.net", 64738, "GrumbleBot", (type, message) -> {
			switch (type) {
				case Version:
					Mumble.Version serverVersion = (Mumble.Version) message;
					System.out.format("Server version: %d, release: %s, OS: %s, OS version: %s\n",
							serverVersion.getVersion(), serverVersion.getRelease(), serverVersion.getOs(),
							serverVersion.getOsVersion());
					break;
				case UserState:
					Mumble.UserState userState = (Mumble.UserState) message;
					System.out.format("User state: user %s(%d) is in channel %d\n", userState.getName(),
							userState.getUserId(), userState.getChannelId());
					break;
				default:
					System.out.format("Received unexpected response type! %s\n", type);
					break;
			}
		});
		connection.start();
	}

}
