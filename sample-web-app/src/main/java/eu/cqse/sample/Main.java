package eu.cqse.sample;

import spark.Request;
import spark.Response;

import static spark.Spark.port;
import static spark.Spark.get;

/** Main class. */
public class Main {

	public Main() {
		port(8081);
		get("/hello", this::hello);
		get(":path", this::catchAll);
	}

	private String hello(Request request, Response response) throws InterruptedException {
		Thread.sleep(123);
		return "Hello World!";
	}

	private String catchAll(Request request, Response response) throws InterruptedException {
		Thread.sleep(456);
		return "Hello " + request.params(":path") + "!";
	}

	/** Main method. */
	public static void main(String[] args) {
		new Main();
	}
}
