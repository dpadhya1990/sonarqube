/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.ws;

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.WebService;
import org.sonar.server.exceptions.ServerException;

import javax.servlet.http.HttpServletResponse;

public class RequestVerifier {
  private RequestVerifier() {
    // static methods only
  }

  public static void verifyRequest(WebService.Action action, Request request) {
    // verify the HTTP verb
    if (action.isPost() && !"POST".equals(request.method())) {
      throw new ServerException(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "HTTP method POST is required");
    }
  }
}
