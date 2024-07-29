package com.teamscale.jacoco.agent.logging;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import com.teamscale.client.ProfilerLogEntry;
import com.teamscale.client.TeamscaleClient;
import com.teamscale.jacoco.agent.options.AgentOptions;
import retrofit2.Call;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class LogToTeamscaleAppender extends AppenderBase<ILoggingEvent> {

	private String profilerId;
	private TeamscaleClient teamscaleClient;
	private int batchSize = 10;
	private Duration flushInterval = Duration.ofSeconds(3);
	private final List<ProfilerLogEntry> logBuffer = new ArrayList<>();
	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

	public void setTeamscaleClient(TeamscaleClient teamscaleClient) {
		this.teamscaleClient = teamscaleClient;
	}

	public void setProfilerId(String profilerId) {
		this.profilerId = profilerId;
	}

	public void setBatchSize(int batchSize) {
		this.batchSize = batchSize;
	}

	public void setFlushInterval(Duration flushInterval) {
		this.flushInterval = flushInterval;
	}

	@Override
	public void start() {
		super.start();
		scheduler.scheduleAtFixedRate(this::flush, flushInterval.toMillis(), flushInterval.toMillis(), TimeUnit.MILLISECONDS);
	}

	@Override
	protected void append(ILoggingEvent eventObject) {
		synchronized (logBuffer) {
			logBuffer.add(formatLog(eventObject));
			if (logBuffer.size() >= batchSize) {
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

	private void sendLogs(List<ProfilerLogEntry> logs) {
		CompletableFuture.runAsync(() -> {
			try {
				Call<Void> call = teamscaleClient.service.postProfilerLog(profilerId, logs);
				retrofit2.Response<Void> response = call.execute();
				if (!response.isSuccessful()) {
					throw new RuntimeException("Failed to send log: HTTP error code : " + response.code());
				}
			} catch (Exception e) {
				e.printStackTrace(); // Handle exceptions appropriately in production code
			}
		});
	}

	@Override
	public void stop() {
		scheduler.shutdown();
		try {
			if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
				scheduler.shutdownNow();
			}
		} catch (InterruptedException e) {
			scheduler.shutdownNow();
		}
		flush(); // Ensure remaining logs are sent
		super.stop();
	}


	public static void addTeamscaleAppenderTo(LoggerContext context, AgentOptions agentOptions) {
		LogToTeamscaleAppender logToTeamscaleAppender = new LogToTeamscaleAppender();
		logToTeamscaleAppender.setContext(context);
		logToTeamscaleAppender.setProfilerId(agentOptions.configurationViaTeamscale.getProfilerId());
		logToTeamscaleAppender.setTeamscaleClient(agentOptions.createTeamscaleClient());
		logToTeamscaleAppender.start();

		Logger rootLogger = context.getLogger(Logger.ROOT_LOGGER_NAME);
		rootLogger.addAppender(logToTeamscaleAppender);
	}


}
