package org.junit.platform.console.tasks;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;

public class ConsoleInterceptor {

	public interface Block {
		void call();
	}

	public static void ignoreOut(Block block) {
		OutputStream discardingOutputStream = new OutputStream() {
			@Override
			public void write(int b) {
				//NOP
			}
		};
		PrintStream printStream = new PrintStream(discardingOutputStream, true);
		PrintStream oldStream = System.out;
		System.setOut(printStream);
		try {
			block.call();
		} finally {
			System.setOut(oldStream);
		}
	}

	public static String copyOut(Block block) throws Exception {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		PrintStream printStream = new PrintStream(bos, true);
		PrintStream oldStream = System.out;
		System.setOut(printStream);
		try {
			block.call();
		} finally {
			System.setOut(oldStream);
		}
		return bos.toString();
	}
}