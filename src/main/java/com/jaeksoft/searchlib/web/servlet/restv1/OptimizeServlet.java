/**   
 * License Agreement for OpenSearchServer
 *
 * Copyright (C) 2008-2009 Emmanuel Keller / Jaeksoft
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

package com.jaeksoft.searchlib.web.servlet.restv1;

import javax.servlet.annotation.WebServlet;

import com.jaeksoft.searchlib.SearchLibException;
import com.jaeksoft.searchlib.user.Role;
import com.jaeksoft.searchlib.user.User;

@WebServlet(name = "Optimize", urlPatterns = { "/optimize" })
public class OptimizeServlet extends AbstractServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = -369063857059673597L;

	@Override
	protected void doRequest(ServletTransaction transaction)
			throws ServletException {
		try {
			String indexName = transaction.getIndexName();
			User user = transaction.getLoggedUser();
			if (user != null && !user.hasRole(indexName, Role.INDEX_UPDATE))
				throw new SearchLibException("Not permitted");
			transaction.getClient().optimize();
			transaction.addXmlResponse("Status", "OK");
		} catch (Exception e) {
			throw new ServletException(e);
		}
	}

}
