/**   
 * License Agreement for OpenSearchServer
 *
 * Copyright (C) 2011-2013 Emmanuel Keller / Jaeksoft
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
package com.jaeksoft.searchlib.webservice.query.search;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.jaeksoft.searchlib.SearchLibException;
import com.jaeksoft.searchlib.facet.Facet;
import com.jaeksoft.searchlib.facet.FacetList;
import com.jaeksoft.searchlib.function.expression.SyntaxError;
import com.jaeksoft.searchlib.query.ParseException;
import com.jaeksoft.searchlib.request.AbstractLocalSearchRequest;
import com.jaeksoft.searchlib.request.AbstractSearchRequest;
import com.jaeksoft.searchlib.result.AbstractResultSearch;
import com.jaeksoft.searchlib.webservice.CommonResult;
import com.jaeksoft.searchlib.webservice.CommonServices;
import com.jaeksoft.searchlib.webservice.query.document.DocumentResult;

@XmlRootElement(name = "result")
@XmlAccessorType(XmlAccessType.PUBLIC_MEMBER)
@JsonInclude(Include.NON_NULL)
public class SearchResult extends CommonResult {

	@XmlElement(name = "document")
	final public List<DocumentResult> documents;

	@XmlElement(name = "facet")
	final public List<FacetResult> facets;

	@XmlElement
	final public String query;

	@XmlAttribute
	final public int rows;

	@XmlAttribute
	final public int start;

	@XmlAttribute
	final public int numFound;

	@XmlAttribute
	final public long time;

	@XmlAttribute
	final public long collapsedDocCount;

	@XmlAttribute
	final public float maxScore;

	public SearchResult() {
		documents = null;
		query = null;
		facets = null;
		rows = 0;
		start = 0;
		numFound = 0;
		time = 0;
		collapsedDocCount = 0;
		maxScore = 0;
	}

	public SearchResult(AbstractResultSearch<?> result) {
		super(true, null);
		try {
			AbstractSearchRequest searchRequest = result.getRequest();
			documents = new ArrayList<DocumentResult>(0);
			facets = new ArrayList<FacetResult>(0);
			query = searchRequest instanceof AbstractLocalSearchRequest
					? ((AbstractLocalSearchRequest) searchRequest).getQueryParsed() : searchRequest.getQueryString();
			start = searchRequest.getStart();
			rows = searchRequest.getRows();
			numFound = result.getNumFound();
			collapsedDocCount = result.getCollapsedDocCount();
			time = result.getTimer().tempDuration();
			maxScore = result.getMaxScore();

			DocumentResult.populateDocumentList(result, documents);

			FacetList facetList = result.getFacetList();
			if (facetList != null)
				for (Facet facet : facetList)
					facets.add(new FacetResult(facet));

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
