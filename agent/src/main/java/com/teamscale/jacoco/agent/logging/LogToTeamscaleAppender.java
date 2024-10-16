package com.teamscale.jacoco.agent.logging;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import com.teamscale.client.ProfilerLogEntry;
import com.teamscale.client.TeamscaleClient;
import com.teamscale.jacoco.agent.options.AgentOptions;
import org.checkerframework.checker.nullness.qual.Nullable;
import retrofit2.Call;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class LogToTeamscaleAppender extends AppenderBase<ILoggingEvent> {

	/** Flush the logs after N elements are in the queue */
	private static final int BATCH_SIZE = 50;

	/** Flush the logs in the given time interval */
	private static final Duration FLUSH_INTERVAL = Duration.ofSeconds(3);

	/** The unique ID of the profiler */
	private String profilerId;

	/** The service client for sending logs to Teamscale */
	private TeamscaleClient teamscaleClient;

	/** Buffer for unsent logs */
	private final List<ProfilerLogEntry> logBuffer = new ArrayList<>();

	/** Scheduler for sending logs after the configured time interval */
	private final ScheduledExecutorService scheduler;

	public LogToTeamscaleAppender() {
		this.scheduler = Executors.newScheduledThreadPool(1, r -> {
			// Make the thread a daemon so that it does not prevent the JVM from terminating.
			Thread t = Executors.defaultThreadFactory().newThread(r);
			t.setDaemon(true);
			return t;
		});
	}

	@Override
	public void start() {
		super.start();
		scheduler.scheduleAtFixedRate(this::flush, FLUSH_INTERVAL.toMillis(), FLUSH_INTERVAL.toMillis(), TimeUnit.MILLISECONDS);
	}

	@Override
	protected void append(ILoggingEvent eventObject) {
		synchronized (logBuffer) {
			logBuffer.add(formatLog(eventObject));
			if (logBuffer.size() >= BATCH_SIZE) {
				flush();
			}
		}
	}

	private ProfilerLogEntry formatLog(ILoggingEvent eventObject) {
		long timestamp = eventObject.getTimeStamp();
		String message = eventObject.getFormattedMessage();
		String severity = eventObject.getLevel().toString();
		return new ProfilerLogEntry(timestamp, message, severity);
	}

	private void flush() {
		List<ProfilerLogEntry> logsToSend;
		synchronized (logBuffer) {
			if (logBuffer.isEmpty()) {
				return;
			}
			logsToSend = new ArrayList<>(logBuffer);
			logBuffer.clear();
		}
		sendLogs(logsToSend);
	}

	/** Send logs in a separate thread */
	private void sendLogs(List<ProfilerLogEntry> logs) {
		CompletableFuture.runAsync(() -> {
			try {
				if (teamscaleClient == null) {
					// There might be no connection configured.
					return;
				}

				Call<Void> call = teamscaleClient.service.postProfilerLog(profilerId, logs);
				retrofit2.Response<Void> response = call.execute();
				if (!response.isSuccessful()) {
					throw new IllegalStateException("Failed to send log: HTTP error code : " + response.code());
				}
			} catch (Exception e) {
				System.err.println("Sending logs to Teamscale failed: " + e.getMessage());
			}
		});
	}

	@Override
	public void stop() {
		// Already flush here once to make sure that we do not miss too much.
		flush();

		scheduler.shutdown();
		try {
			if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
				scheduler.shutdownNow();
			}
		} catch (InterruptedException e) {
			scheduler.shutdownNow();
		}

		// A final flush after the scheduler has been shut down.
		flush();

		super.stop();
	}

	public void setTeamscaleClient(TeamscaleClient teamscaleClient) {
		this.teamscaleClient = teamscaleClient;
	}

	public void setProfilerId(String profilerId) {
		this.profilerId = profilerId;
	}

	/**
	 * Add the {@link com.teamscale.jacoco.agent.logging.LogToTeamscaleAppender} to the logging configuration
	 * and enable/start it.
	 */
	public static void addTeamscaleAppenderTo(LoggerContext context, AgentOptions agentOptions) {
		@Nullable TeamscaleClient client = agentOptions.createTeamscaleClient(
				false);
		if (client == null) {
			return;
		}

		LogToTeamscaleAppender logToTeamscaleAppender = new LogToTeamscaleAppender();
		logToTeamscaleAppender.setContext(context);
		logToTeamscaleAppender.setProfilerId(agentOptions.configurationViaTeamscale.getProfilerId());
		logToTeamscaleAppender.setTeamscaleClient(client);
		logToTeamscaleAppender.start();

		Logger rootLogger = context.getLogger(Logger.ROOT_LOGGER_NAME);
		rootLogger.addAppender(logToTeamscaleAppender);
	}

}
