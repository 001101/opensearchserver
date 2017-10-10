/*
 * License Agreement for OpenSearchServer
 * <p>
 * Copyright (C) 2010-2017 Emmanuel Keller / Jaeksoft
 * <p>
 * http://www.open-search-server.com
 * <p>
 * This file is part of OpenSearchServer.
 * <p>
 * OpenSearchServer is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * OpenSearchServer is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with OpenSearchServer.
 * If not, see <http://www.gnu.org/licenses/>.
 */
package com.jaeksoft.searchlib.crawler.database;

import com.jaeksoft.searchlib.Client;
import com.jaeksoft.searchlib.config.Config;
import com.jaeksoft.searchlib.crawler.common.process.CrawlMasterAbstract;
import com.jaeksoft.searchlib.process.ThreadItem;
import com.jaeksoft.searchlib.util.InfoCallback;
import com.jaeksoft.searchlib.util.Variables;

public class DatabaseCrawlMaster extends CrawlMasterAbstract<DatabaseCrawlMaster, DatabaseCrawlThread> {

	public DatabaseCrawlMaster(Config config) {
		super(config, "DatabaseCrawler");
	}

	@Override
	public DatabaseCrawlThread getNewThread(Client client, ThreadItem<?, DatabaseCrawlThread> databaseCrawl,
			Variables variables, InfoCallback infoCallback) {
		if (databaseCrawl instanceof DatabaseCrawlSql)
			return new DatabaseCrawlSqlThread(client, this, (DatabaseCrawlSql) databaseCrawl, variables, infoCallback);
		if (databaseCrawl instanceof DatabaseCrawlMongoDb)
			return new DatabaseCrawlMongoDbThread(client, this, (DatabaseCrawlMongoDb) databaseCrawl, variables,
					infoCallback);
		if (databaseCrawl instanceof DatabaseCrawlCassandra)
			return new DatabaseCrawlCassandraThread(client, this, (DatabaseCrawlCassandra) databaseCrawl, variables,
					infoCallback);
		return null;
	}

	@Override
	protected DatabaseCrawlThread[] getNewArray(int size) {
		return new DatabaseCrawlThread[size];
	}
}
