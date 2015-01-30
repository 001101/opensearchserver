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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.icepdf.core.tag.query.DocumentResult;

import com.jaeksoft.searchlib.SearchLibException;
import com.jaeksoft.searchlib.collapse.CollapseAbstract;
import com.jaeksoft.searchlib.function.expression.SyntaxError;
import com.jaeksoft.searchlib.index.ReaderAbstract;
import com.jaeksoft.searchlib.join.JoinResult;
import com.jaeksoft.searchlib.query.ParseException;
import com.jaeksoft.searchlib.render.Render;
import com.jaeksoft.searchlib.render.RenderCSV;
import com.jaeksoft.searchlib.render.RenderSearchJson;
import com.jaeksoft.searchlib.render.RenderSearchXml;
import com.jaeksoft.searchlib.request.AbstractSearchRequest;
import com.jaeksoft.searchlib.result.collector.DistanceInterface;
import com.jaeksoft.searchlib.result.collector.DocIdInterface;
import com.jaeksoft.searchlib.result.collector.ScoreInterface;
import com.jaeksoft.searchlib.util.Timer;
import com.jaeksoft.searchlib.webservice.CommonServices;
import com.opensearchserver.client.v2.search.FacetResult2;
import com.opensearchserver.client.v2.search.SearchResult2;

public abstract class AbstractResultSearch extends
		AbstractResult<AbstractSearchRequest> implements
		ResultDocumentsInterface<AbstractSearchRequest> {

	protected final ReaderAbstract reader;
	transient protected CollapseAbstract collapse;
	protected List<FacetResult2> facetResults;
	protected DocIdInterface docs;
	protected ScoreInterface scores;
	protected DistanceInterface distances;
	protected long numFound;
	protected float maxScore;
	protected long collapsedDocCount;
	private JoinResult[] joinResults;

	protected AbstractResultSearch(ReaderAbstract reader,
			AbstractSearchRequest searchRequest) {
		super(searchRequest);
		this.reader = reader;
		this.numFound = 0;
		this.maxScore = 0;
		this.collapsedDocCount = 0;
		this.docs = null;
		this.joinResults = JoinResult.EMPTY_ARRAY;
		if (searchRequest.getFacetFieldList().size() > 0)
			this.facetResults = new ArrayList<FacetResult2>(searchRequest
					.getFacetFieldList().size());
		collapse = CollapseAbstract.newInstance(searchRequest);
	}

	public ReaderAbstract getReader() {
		return reader;
	}

	public List<FacetResult2> getFacetList() {
		return facetResults;
	}

	protected void setJoinResults(JoinResult[] joinResults) {
		this.joinResults = joinResults == null ? JoinResult.EMPTY_ARRAY
				: joinResults;
	}

	public ResultDocument getDocument(final int pos) throws SearchLibException {
		return getDocument(pos, null);
	}

	@Override
	public abstract ResultDocument getDocument(final int pos, final Timer timer)
			throws SearchLibException;

	public Iterator<ResultDocument> iterator(Timer timer) {
		return new ResultDocumentIterator(this, timer);
	}

	@Override
	public Iterator<ResultDocument> iterator() {
		return new ResultDocumentIterator(this, null);
	}

	@Override
	public float getMaxScore() {
		return maxScore;
	}

	@Override
	public long getNumFound() {
		return numFound;
	}

	@Override
	public int getRequestStart() {
		return request.getStart();
	}

	@Override
	public int getRequestRows() {
		return request.getRows();
	}

	protected void setDocs(DocIdInterface docs) {
		this.docs = docs;
		this.scores = docs.getCollector(ScoreInterface.class);
		this.distances = docs.getCollector(DistanceInterface.class);
	}

	public int getDocLength() {
		if (docs == null)
			return 0;
		return docs.getSize();
	}

	@Override
	public int getDocumentCount() {
		int end = request.getEnd();
		int len = getDocLength();
		if (end > len)
			end = len;
		int start = request.getStart();
		if (start > end)
			return 0;
		return end - start;
	}

	public List<ResultDocument> getJoinDocumentList(int pos, Timer timer)
			throws SearchLibException {
		if (joinResults == null)
			return null;
		List<ResultDocument> joinResultDocuments = new ArrayList<ResultDocument>(
				joinResults.length);
		for (JoinResult joinResult : joinResults)
			if (joinResult.isReturnFields())
				joinResultDocuments.add(joinResult.getDocument(pos, timer));
		return joinResultDocuments;
	}

	@Override
	public DocIdInterface getDocs() {
		return docs;
	}

	public CollapseAbstract getCollapse() {
		return collapse;
	}

	@Override
	public long getCollapsedDocCount() {
		return collapsedDocCount;
	}

	@Override
	public float getScore(int pos) {
		if (scores == null)
			return 0;
		return scores.getScores()[pos];
	}

	@Override
	public Float getDistance(int pos) {
		if (distances == null)
			return null;
		return distances.getDistances()[pos];
	}

	@Override
	public int getCollapseCount(int pos) {
		return ResultDocument.getCollapseCount(docs, pos);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(numFound);
		sb.append(" founds.");
		if (docs != null) {
			sb.append(' ');
			sb.append(docs.getSize());
			sb.append(" docs.");
		}
		sb.append(" MaxScore: ");
		sb.append(maxScore);
		if (request != null) {
			sb.append(" - ");
			sb.append(request);
		}
		return sb.toString();
	}

	@Override
	protected Render getRenderXml() {
		return new RenderSearchXml(this);
	}

	@Override
	protected Render getRenderCsv() {
		return new RenderCSV(this);
	}

	@Override
	protected Render getRenderJson(boolean indent) {
		return new RenderSearchJson(this, indent);
	}

	public SearchResult2 getSearchResult() {
		try {
			SearchResult2 searchResult = new SearchResult2();
			searchResult.setQuery(request.getQueryParsed());
			searchResult.setStart(request.getStart());
			searchResult.setRows(request.getRows());
			searchResult.setNumFound(numFound);
			searchResult.setCollapsedDocCount(collapsedDocCount);
			searchResult.setTime(timer.tempDuration());
			searchResult.setMaxScore(maxScore);

			DocumentResult.populateDocumentList(result, documents);

			searchResult.setFacets(facetResults);
			return searchResult;
		} catch (ParseException e) {
			throw new CommonServices.CommonServiceException(e);
		} catch (SyntaxError e) {
			throw new CommonServices.CommonServiceException(e);
		} catch (SearchLibException e) {
			throw new CommonServices.CommonServiceException(e);
		} catch (IOException e) {
			throw new CommonServices.CommonServiceException(e);
		}
	}
}
