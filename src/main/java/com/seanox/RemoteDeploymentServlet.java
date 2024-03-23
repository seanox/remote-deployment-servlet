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

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * The RemoteDeploymentServlet supports HTTP-based updating of Web
 * applications. The benefit is the data transmission in chunks and the
 * optional use of the command line, e.g. to stop and start of the
 * Servlet-Container.
 *
 * <h3>Why Servlet and Filter?</h3>
 * The filter has the advantage that it can run directly in the context of a
 * web application. The servlet uses its own context. This is useful if the
 * remote deployment should still work when the application's servlet is
 * unreachable due to an error at startup, then a new remote deployment when a
 * filter is used is not possible.
 *
 * <h3>Configuration (web.xml)</h3>
 * The RemoteDeploymentServlet is configured exclusively via the web.xml.
 * <pre>
 *   &lt;servlet&gt;
 *     &lt;servlet-name&gt;RemoteDeploymentServlet&lt;/servlet-name&gt;
 *     &lt;servlet-class&gt;com.seanox.RemoteDeploymentServlet&lt;/servlet-class&gt;
 *     &lt;init-param&gt;
 *       &lt;param-name&gt;secret&lt;/param-name&gt;
 *       &lt;param-value&gt;B43AA6F00D034661722495C388527735&lt;/param-value&gt;
 *     &lt;/init-param&gt;
 *     &lt;init-param&gt;
 *       &lt;param-name&gt;destination&lt;/param-name&gt;
 *       &lt;param-value&gt;D:\Tomcat\webapps\example.war&lt;/param-value&gt;
 *     &lt;/init-param&gt;
 *     &lt;init-param&gt;
 *       &lt;param-name&gt;command&lt;/param-name&gt;
 *       &lt;param-value&gt;
 *         cmd /C ping -n 60 127.0.0.1 &amp;gt; NUL
 *         &amp;amp;amp; net stop TomcatService
 *         &amp;amp;amp; ping -n 60 127.0.0.1 &amp;gt; NUL
 *         &amp;amp;amp; net start TomcatService
 *       &lt;/param-value&gt;
 *     &lt;/init-param&gt;
 *     &lt;init-param&gt;
 *       &lt;param-name&gt;expiration&lt;/param-name&gt;
 *       &lt;param-value&gt;300000&lt;/param-value&gt;
 *     &lt;/init-param&gt;
 *   &lt;/servlet&gt;
 *   &lt;servlet-mapping&gt;
 *     &lt;servlet-name&gt;RemoteDeploymentServlet&lt;/servlet-name&gt;
 *     &lt;url-pattern&gt;/97C698B4EF93088CAF0A721A792D3AB6&lt;/url-pattern&gt;
 *   &lt;/servlet-mapping&gt;
 * </pre>
 *
 * <h3>Parameter: destination</h3>
 * Servlets and the update are called via a virtual path that is not publicly
 * known. The path is a cryptic alias that refers to a physical path, which is
 * destination.
 *
 * <h3>Parameter: command</h3>
 * If the upload is successful, this command can be executed optionally.
 *
 * <h3>Parameter: secret</h3>
 * For additional security, a secret is needed, which is sent as an additional
 * HTTP request header.
 *
 * <h3>Parameter: expiration</h3>
 * Requests to the RemoteDeploymentServlet generate temporary files. Because
 * the data is transferred in chunks, the files must be retained. The
 * expiration time in milliseconds determines how long the expiration time is
 * in case of an error. After the expiration time, the temporary files are
 * cleaned up. A value 0 and smaller disables the clean up.
 *
 * <h3>Parameter: url-pattern</h3>
 * Servlets and the update are called via a virtual path that is not publicly
 * known. The path is a cryptic alias that refers to a concrete path in the
 * file system in the further configuration.
 *
 * <h3>Security Concept</h3>
 * Only PUT requests and matching Secret headers are accepted, otherwise the
 * servlet behaves as if it does not exist. The servlet reacts only after
 * sufficient authorization.
 *
 * <h3>Configuration Tomcat (server.xml)</b></dir>
 * Configuration of the host parameters (server - service - engine - host):
 * WAR files should be unpacked (unpackWARs: true) and the AutoDeployment must
 * be deactivated (autoDeploy: false). Otherwise, Tomcat will start an
 * uncontrolled restart of the app after uploading from remote deployment.
 * <pre>
 *  &lt;Host name="localhost" appBase="webapps" unpackWARs="true" autoDeploy="false"&gt;
 * </pre>
 *
 * <h3>Configuration Tomcat Service (only for Windows)</h3>
 * If the Tomcat runs as a service under Windows and uses its own user such as
 * LOCAL SERVICE, the user may need permission to restart the service.
 * An easy way is to use SysinternalsSuite Process Explorer (procexp.exe).
 * <ul>
 *   <li>Start the program as admin</li>
 *   <li>Open the properties of the process</li>
 *   <li>Open Services - Permissions</li>
 *   <li>Add the user and the permissions (mostly full access)</li>
 * </ul>
 *
 * @author  Seanox Software Solutions
 * @version 1.1.0 20211002
 */
public class RemoteDeploymentServlet extends HttpServlet {

    private RemoteDeploymentImpl remoteDeployment;

    @Override
    public void init(final ServletConfig config)
            throws ServletException {
        this.remoteDeployment = new RemoteDeploymentImpl();
        this.remoteDeployment.init(config);
    }

    @Override
    protected void service(final HttpServletRequest request, final HttpServletResponse response)
            throws IOException, ServletException {

        // Only PUT requests and matching Secret headers are accepted,
        // otherwise the Servlet behaves as if it does not exist. This is the
        // security concept. The Servlet reacts only after authorization.
        try {this.remoteDeployment.service(request, response);
        } catch (RemoteDeploymentImpl.UnsupportedRequestException exception) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.flushBuffer();
        }
    }

    @Override
    public void destroy() {
        this.remoteDeployment.destroy();
    }
}
