/**   
 * License Agreement for OpenSearchServer
 *
 * Copyright (C) 2013 Emmanuel Keller / Jaeksoft
 * 
 * http://www.open-search-server.com
 * 
 * This file is part of OpenSearchServer.
 *
 * OpenSearchServer is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 * OpenSearchServer is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with OpenSearchServer. 
 *  If not, see <http://www.gnu.org/licenses/>.
 **/

package com.jaeksoft.searchlib.script;

import java.io.Closeable;

import org.apache.commons.lang.exception.ExceptionUtils;

import com.jaeksoft.searchlib.Logging;
import com.jaeksoft.searchlib.config.Config;
import com.jaeksoft.searchlib.scheduler.TaskLog;
import com.jaeksoft.searchlib.utils.Variables;

public abstract class AbstractScriptRunner implements Closeable {

	private final ScriptCommandContext context;
	private final Variables variables;

	public class ScriptLine {

		public final String id;
		public final String command;
		public final String[] parameters;

		public ScriptLine(final String id, final String command,
				final String[] parameters) {
			this.id = id;
			this.command = command;
			if (parameters != null)
				for (int i = 0; i < parameters.length; i++)
					parameters[i] = variables.replace(parameters[i]);
			this.parameters = parameters;
		}

	}

	protected AbstractScriptRunner(Config config, Variables variables,
			TaskLog taskLog) {
		this.context = new ScriptCommandContext(config, taskLog);
		this.variables = variables != null ? variables : new Variables();
	}

	protected abstract void beforeRun(final ScriptCommandContext context,
			final Variables variables) throws ScriptException;

	protected abstract ScriptLine nextScriptLine(final Variables variables)
			throws ScriptException;

	protected abstract void updateScriptLine(final ScriptLine scriptLine,
			final Variables variables, final String errorMsg)
			throws ScriptException;

	public final void run() throws ScriptException {
		try {
			beforeRun(context, variables);
			CommandEnum[] commandFinder = null;
			String lastScriptError = null;
			ScriptLine scriptLine = null;
			while ((scriptLine = nextScriptLine(variables)) != null) {
				String currentScriptError = null;
				try {
					CommandEnum commandEnum = CommandEnum
							.find(scriptLine.command);
					if (commandFinder != null) {
						// On error next_command is active, looking for next
						// statement
						boolean bFind = false;
						for (CommandEnum cmd : commandFinder) {
							if (cmd == commandEnum) {
								bFind = true;
								break;
							}
						}
						if (!bFind) {
							updateScriptLine(scriptLine, variables,
									"ignored due to previous error");
							continue;
						}
						commandFinder = null;
					}
					CommandAbstract commandAbstract = commandEnum
							.getNewInstance();
					commandAbstract.run(context, scriptLine.id,
							scriptLine.parameters);
				} catch (Exception e) {
					Throwable t = ExceptionUtils.getRootCause(e);
					currentScriptError = t != null ? ExceptionUtils
							.getMessage(t) : ExceptionUtils.getMessage(e);
					lastScriptError = currentScriptError;
					switch (context.getOnError()) {
					case FAILURE:
						throw new ScriptException(e);
					case RESUME:
						Logging.warn(e);
						break;
					case NEXT_COMMAND:
						Logging.warn(e);
						commandFinder = context.getOnErrorNextCommands();
						break;
					}
				}
				updateScriptLine(scriptLine, variables, currentScriptError);
			}
			afterRun(lastScriptError, variables);
		} finally {
			close();
		}
	}

	protected abstract void afterRun(final String lastScriptError,
			final Variables variables) throws ScriptException;

	@Override
	public void close() {
		if (context != null)
			context.close();
	}
}