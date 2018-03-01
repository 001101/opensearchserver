/*
 * Copyright 2017-2018 Emmanuel Keller / Jaeksoft
 *  <p>
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  <p>
 *  http://www.apache.org/licenses/LICENSE-2.0
 *  <p>
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.jaeksoft.opensearchserver.front.accounts;

import com.jaeksoft.opensearchserver.Components;
import com.jaeksoft.opensearchserver.front.ServletTransaction;
import com.jaeksoft.opensearchserver.model.AccountRecord;
import com.jaeksoft.opensearchserver.model.PermissionRecord;
import com.jaeksoft.opensearchserver.services.AccountsService;
import com.jaeksoft.opensearchserver.services.PermissionsService;
import com.qwazr.database.annotations.TableRequestResultRecords;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;

public class AccountsTransaction extends ServletTransaction {

	private final static String TEMPLATE = "accounts/accounts.ftl";

	private final PermissionsService permissionsService;
	private final AccountsService accountsService;

	AccountsTransaction(final Components components, final HttpServletRequest request,
			final HttpServletResponse response) throws NoSuchMethodException, IOException, URISyntaxException {
		super(components, request, response, true);
		permissionsService = components.getPermissionsService();
		accountsService = components.getAccountsService();
	}

	@Override
	protected void doGet() throws IOException, ServletException {
		final TableRequestResultRecords<PermissionRecord> permissions =
				permissionsService.getPermissionsByUser(userRecord.getId(), 0, 1000);
		final Map<AccountRecord, PermissionRecord> results = accountsService.getAccountsByIds(permissions);
		request.setAttribute("accounts", results);
		doTemplate(TEMPLATE);
	}

}