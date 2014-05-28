package net.wjlafrance.grumble;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public @Slf4j class GrumbleMain {

	public static void main(String args[]) {
		try (ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("beans.xml")) {
			GrumbleBot bot = context.getBean("grumblebot", GrumbleBot.class);
			log.info("Starting bot!");
			bot.start();
		}
	}

}
