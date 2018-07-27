package org.junit.platform.console.tasks;

import java.io.OutputStream;
import java.io.PrintStream;

/** Handles intercepting calls to System.out called during the tests. */
public class ConsoleInterceptor {

	/** Some callable code. */
	public interface Block {
		/** Execute the block of code. */
		void call();
	}

	/** Executes the given code and discards all System.out stream inputs. */
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
}