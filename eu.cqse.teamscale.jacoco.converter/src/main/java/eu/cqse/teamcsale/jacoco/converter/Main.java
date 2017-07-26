package eu.cqse.teamcsale.jacoco.converter;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

import io.reactivex.schedulers.Schedulers;

public class Main {

	private IJacocoController controller;
	private ExecutionDataConverter converter;
	private IXmlStore store;

	public static void main(String[] args) throws Exception {
		Arguments arguments = new Arguments();
		JCommander.newBuilder().addObject(args).build().parse(args);
		new Main().run(arguments);
	}

	private void run(Arguments arguments) throws IOException {
		controller = new JacocoRemoteTCPController("localhost", arguments.port);
		converter = new ExecutionDataConverter();
		store = new TimestampedFileStore(Paths.get(arguments.outputDir));
		// TODO (FS) argument validation http://jcommander.org/#_download

		controller.connect().subscribeOn(Schedulers.single()).map(converter::convert).subscribe(store::store);

		// TODO (FS) wait for shutdown?
	}

	private static class Arguments {

		@Parameter(names = { "--classDir", "--jar", "-c" }, required = true, description = ""
				+ "The directories or zip/ear/jar/war/... files that contain the compiled Java classes being profiled."
				+ " Searches recursively, including inside zips.")
		private List<String> classDirectiesOrZips = new ArrayList<>();

		@Parameter(names = { "--port", "-p" }, required = true, description = ""
				+ "The port under which JaCoCo is listening for connections.")
		private int port = 0;

		@Parameter(names = { "--out", "-o" }, required = true, description = ""
				+ "The directory to write the generated XML reports to.")
		private String outputDir = "";

	}

}
