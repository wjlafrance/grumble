package net.wjlafrance.grumble.data;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class UserList {

	private Map<Integer, User> sessions = new HashMap<>();

	public User getUserForSession(int session) {
		return sessions.computeIfAbsent(session, (newSession) -> {
			User newUser = new User();
			newUser.setSessionId(newSession);
			return newUser;
		});
	}

	public Optional<User> getUserForName(String name) {
		return sessions.values().stream().filter(user -> name.equals(user.getName())).findAny();
	}

	public void deleteUser(int session) {
		sessions.remove(session);
	}

}
