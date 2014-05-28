package net.wjlafrance.grumble;

import java.util.HashMap;
import java.util.Map;

public class UserList {

	private Map<Integer, User> sessions = new HashMap<>();

	public User getUserForSession(int session) {
		return sessions.computeIfAbsent(session, (newSession) -> {
			User newUser = new User();
			newUser.setSessionId(newSession);
			return newUser;
		});
	}

	public void deleteUser(int session) {
		sessions.remove(session);
	}

}
