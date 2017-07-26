/*
 * SonarQube Java
 * Copyright (C) 2010-2017 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package eu.cqse.teamscale.jacoco.report;

import org.jacoco.core.data.*;

public abstract class ExecutionDataVisitor implements ISessionInfoVisitor, IExecutionDataVisitor {

    private ExecutionDataStore executionDataStore = new ExecutionDataStore();
    private SessionInfo session = null;

    @Override
    public void visitSessionInfo(SessionInfo info) {
        if(session != null) {
            processNextSession(session, executionDataStore);
        }

        executionDataStore = new ExecutionDataStore();
        if (!info.getId().contains(XMLCoverageWriter.SESSION_ID_SEPARATOR)) {
            session = null;
            return;
        }
        session = info;
    }

    @Override
    public void visitClassExecution(ExecutionData data) {
        executionDataStore.put(data);
    }

    public abstract void processNextSession(SessionInfo info, ExecutionDataStore executionDataStore);
}
