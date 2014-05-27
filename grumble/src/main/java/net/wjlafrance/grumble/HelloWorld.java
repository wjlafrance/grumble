package net.wjlafrance.grumble;

public class HelloWorld {

	public static void main(String args[]) {
		Runnable r = () -> ( System.out.println("hi!") );
		r.run();
	}

}
