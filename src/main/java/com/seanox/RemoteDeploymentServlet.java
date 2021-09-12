/**
 * LIZENZBEDINGUNGEN - Seanox Software Solutions ist ein Open-Source-Projekt,
 * im Folgenden Seanox Software Solutions oder kurz Seanox genannt.
 * Diese Software unterliegt der Version 2 der GNU General Public License.
 *
 * Remote Deployment Servlet
 * Copyright (C) 2021 Seanox Software Solutions
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of version 2 of the GNU General Public License as published by the
 * Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package com.seanox;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.Objects;

/**
 * TODO:
 *
 * <b>Security Concept</b>
 * <p>
 *   Invalid as well as incomplete requests are answered with status 404.
 * </p>
 *
 * RemoteDeploymentServlet 1.0.0 20210912<br>
 * Copyright (C) 2021 Seanox Software Solutions<br>
 * Alle Rechte vorbehalten.
 *
 * @author  Seanox Software Solutions
 * @version 1.0.0 20210912
 */
public class RemoteDeploymentServlet extends HttpServlet {

    /** Constant for parameter: secret */
    private static final String PARAMETER_SECRET = "secret";

    /** Constant for parameter: destination */
    private static final String PARAMETER_DESTINATION = "destination";

    /** Constant for parameter: command */
    private static final String PARAMETER_COMMAND = "command";

    /** Constant for parameter: timeout */
    private static final String PARAMETER_TIMEOUT = "timeout";

    private String secret;

    private File destination;

    private String command;

    private long timeout;

    @Override
    public void init(final ServletConfig config)
            throws ServletException {

        final String secret = config.getInitParameter(PARAMETER_SECRET);
        if (Objects.isNull(secret)
                || secret.isBlank())
            throw new ServletException("Invalid parameter: " + PARAMETER_SECRET);
        this.secret = secret.trim();

        final String destination = config.getInitParameter(PARAMETER_DESTINATION);
        if (Objects.isNull(destination)
                || destination.isBlank()
                || !new File(destination.trim()).isFile())
            throw new ServletException("Invalid parameter: " + PARAMETER_DESTINATION);
        this.destination = new File(destination.trim());

        final String command = config.getInitParameter(PARAMETER_COMMAND);
        if (Objects.nonNull(command)
                && !command.isBlank())
        this.command = command.trim();

        final String timeout = config.getInitParameter(PARAMETER_TIMEOUT);
        if (Objects.nonNull(timeout)
                && !timeout.isBlank())
            if (!timeout.matches("^\\s*\\d{1,8}\\s*$"))
                throw new ServletException("Invalid parameter: " + PARAMETER_DESTINATION);
            else this.timeout = Integer.valueOf(timeout.trim());
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // TODO:
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (Objects.isNull(request.getMethod())
                || !("PUT").equalsIgnoreCase(request.getMethod()))
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        else super.service(request, response);
    }
}