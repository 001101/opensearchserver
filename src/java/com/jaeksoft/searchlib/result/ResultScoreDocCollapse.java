/**   
 * License Agreement for OpenSearchServer
 *
 * Copyright (C) 2008-2012 Emmanuel Keller / Jaeksoft
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

package com.jaeksoft.searchlib.result;

import org.apache.lucene.search.ScoreDoc;

final public class ResultScoreDocCollapse extends ResultScoreDoc {

	public static final ResultScoreDocCollapse[] EMPTY_ARRAY = new ResultScoreDocCollapse[0];

	public int collapseCount;

	public ResultScoreDocCollapse(int doc, float score) {
		super(doc, score);
		this.collapseCount = 0;
	}

	public ResultScoreDocCollapse(ScoreDoc sc) {
		super(sc);
		this.collapseCount = 0;
	}

}
