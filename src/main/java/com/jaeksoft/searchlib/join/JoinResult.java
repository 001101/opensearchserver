/**   
 * License Agreement for OpenSearchServer
 *
 * Copyright (C) 2012-2015 Emmanuel Keller / Jaeksoft
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

package com.jaeksoft.searchlib.join;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeSet;

import com.jaeksoft.searchlib.SearchLibException;
import com.jaeksoft.searchlib.function.expression.SyntaxError;
import com.jaeksoft.searchlib.query.ParseException;
import com.jaeksoft.searchlib.request.AbstractSearchRequest;
import com.jaeksoft.searchlib.result.AbstractResultSearch;
import com.jaeksoft.searchlib.result.ResultDocument;
import com.jaeksoft.searchlib.result.ResultSearchSingle;
import com.jaeksoft.searchlib.result.collector.CollectorInterface;
import com.jaeksoft.searchlib.result.collector.JoinDocInterface;
import com.jaeksoft.searchlib.result.collector.JoinScoreInterface;
import com.jaeksoft.searchlib.util.Timer;

public class JoinResult {

	public static final JoinResult[] EMPTY_ARRAY = new JoinResult[0];

	public final int joinPosition;

	private final String paramPosition;

	private final boolean returnFields;

	private transient ResultSearchSingle foreignResult;

	private transient JoinDocInterface joinDocInterface;

	private transient JoinScoreInterface joinScoreInterface;

	private TreeSet<String> fieldNameSet;

	private Map<String, Map<String, Long>> facetList;

	public JoinResult(int joinPosition, String paramPosition,
			boolean returnFields) {
		this.joinPosition = joinPosition;
		this.paramPosition = paramPosition;
		this.returnFields = returnFields;
		this.fieldNameSet = null;
		this.facetList = null;
	}

	public void setForeignResult(ResultSearchSingle foreignResult) {
		this.foreignResult = foreignResult;
		fieldNameSet = new TreeSet<String>();
		AbstractSearchRequest request = foreignResult.getRequest();
		request.getReturnFieldList().populate(fieldNameSet);
		request.getSnippetFieldList().populate(fieldNameSet);
	}

	public void setJoinDocInterface(CollectorInterface collectorInterface) {
		if (!(collectorInterface instanceof JoinDocInterface))
			return;
		this.joinDocInterface = (JoinDocInterface) collectorInterface;
		this.joinScoreInterface = collectorInterface
				.getCollector(JoinScoreInterface.class);
	}

	public AbstractResultSearch getForeignResult() {
		return foreignResult;
	}

	public boolean isReturnFields() {
		return returnFields;
	}

	final public ResultDocument getDocument(long pos, Timer timer)
			throws SearchLibException {
		try {
			if (joinDocInterface == null)
				return null;
			float score = joinScoreInterface != null ? joinScoreInterface
					.getForeignScore(pos, joinPosition) : 0;
			return new ResultDocument(foreignResult.getRequest(), fieldNameSet,
					joinDocInterface.getForeignDocId(pos, joinPosition),
					foreignResult.getReader(), score, paramPosition, 0, timer);
		} catch (IOException e) {
			throw new SearchLibException(e);
		} catch (ParseException e) {
			throw new SearchLibException(e);
		} catch (SyntaxError e) {
			throw new SearchLibException(e);
		}
	}

	public void addFacet(String name, Map<String, Long> terms) {
		if (facetList == null)
			facetList = new LinkedHashMap<String, Map<String, Long>>();
		facetList.put(name.intern(), terms);
	}

	public void populate(Map<String, Map<String, Long>> facetResults) {
		if (facetList == null)
			return;
		for (Map.Entry<String, Map<String, Long>> facetResult : facetList
				.entrySet())
			facetResults.put(facetResult.getKey(), facetResult.getValue());
	}
}
