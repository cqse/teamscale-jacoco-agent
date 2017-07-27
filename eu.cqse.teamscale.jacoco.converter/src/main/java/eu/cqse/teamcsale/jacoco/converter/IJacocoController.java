package eu.cqse.teamcsale.jacoco.converter;

import java.io.IOException;

import org.jacoco.core.data.ExecutionData;

import io.reactivex.Observable;

public interface IJacocoController extends AutoCloseable {

	Observable<ExecutionData> connect() throws IOException;

	void dump(boolean reset) throws IOException;

	@Override
	void close() throws IOException;

}