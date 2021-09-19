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

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Objects;

/**
 * The RemoteDeploymentFilter supports HTTP-based updating of Web applications.
 * The benefit is the data transmission in chunks and the optional use of the
 * command line, e.g. to stop and start of the Servlet-Container.
 *
 * <h3>Configuration (web.xml)</h3>
 * The RemoteDeploymentFilter is configured exclusively via the web.xml.
 * <pre>
 *   &lt;filter&gt;
 *     &lt;filter-name&gt;RemoteDeploymentFilter&lt;/filter-name&gt;
 *     &lt;filter-class&gt;com.seanox.RemoteDeploymentFilter&lt;/filter-class&gt;
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
 *   &lt;/filter&gt;
 *   &lt;filter-mapping&gt;
 *     &lt;filter-name&gt;RemoteDeploymentFilter&lt;/filter-name&gt;
 *     &lt;url-pattern&gt;/97C698B4EF93088CAF0A721A792D3AB6&lt;/url-pattern&gt;
 *   &lt;/filter-mapping&gt;
 * </pre>
 *
 * <h3>Parameter: destination</h3>
 * Filters and the update are called via a virtual path that is not publicly
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
 * Requests to the RemoteDeploymentFilter generate temporary files. Because the
 * data is transferred in chunks, the files must be retained. The expiration
 * time in milliseconds determines how long the expiration time is in case of
 * an error. After the expiration time, the temporary files are cleaned up.
 *
 * <h3>Parameter: url-pattern</h3>
 * Filters and the update are called via a virtual path that is not publicly
 * known. The path is a cryptic alias that refers to a concrete path in the
 * file system in the further configuration.
 *
 * <h3>Security Concept</h3>
 * Only PUT requests and matching Secret headers are accepted, otherwise the
 * filter behaves as if it does not exist. The filter reacts only after
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
 * RemoteDeploymentFilter 1.0.0 20210918<br>
 * Copyright (C) 2021 Seanox Software Solutions<br>
 * Alle Rechte vorbehalten.
 *
 * @author  Seanox Software Solutions
 * @version 1.0.0 20210918
 */
public class RemoteDeploymentFilter extends HttpFilter {

    private static final String UUID = java.util.UUID.randomUUID().toString().toUpperCase();

    private static final String PARAMETER_SECRET = "secret";
    private static final String PARAMETER_DESTINATION = "destination";
    private static final String PARAMETER_COMMAND = "command";
    private static final String PARAMETER_EXPIRATION = "expiration";

    private String secret;
    private File destination;
    private String command;
    private long expiration;

    private static final String HTTP_HEADER_PACKAGE = "Package";
    private static final String HTTP_HEADER_SECRET = "Secret";

    @Override
    public void init(final FilterConfig config)
            throws ServletException {

        final String secret = config.getInitParameter(PARAMETER_SECRET);
        if (Objects.isNull(secret)
                || secret.isBlank())
            throw new ServletException("Invalid parameter: " + PARAMETER_SECRET);
        this.secret = secret.trim();

        final String destination = config.getInitParameter(PARAMETER_DESTINATION);
        if (Objects.isNull(destination)
                || destination.isBlank())
            throw new ServletException("Invalid parameter: " + PARAMETER_DESTINATION);
        this.destination = new File(destination.trim());

        final String command = config.getInitParameter(PARAMETER_COMMAND);
        if (Objects.nonNull(command)
                && !command.isBlank())
        this.command = command.trim();

        final String expiration = config.getInitParameter(PARAMETER_EXPIRATION);
        if (Objects.nonNull(expiration)
                && !expiration.isBlank())
            if (!expiration.matches("^\\s*\\d{1,8}\\s*$"))
                throw new ServletException("Invalid parameter: " + PARAMETER_EXPIRATION);
            else this.expiration = Integer.valueOf(expiration.trim());
    }

    private static class PackageMeta {
        private String uuid;
        private int number;
        private int count;
        private String checkSum;
    }

    private static PackageMeta detectPackageMeta(final HttpServletRequest request) {
        if (Objects.isNull(request))
            return null;
        final String packageMetaHeader = request.getHeader(HTTP_HEADER_PACKAGE);
        final String packageMetaHeaderPattern = "^(?i)([\\w-]+)/(\\d+)/(\\d+)/((?:[0-9A-F]{2})+)$";
        if (Objects.isNull(packageMetaHeader)
                || !packageMetaHeader.matches(packageMetaHeaderPattern))
            return null;
        final PackageMeta packageMeta = new PackageMeta();
        packageMeta.uuid = packageMetaHeader.replaceAll(packageMetaHeaderPattern, "$1");
        packageMeta.number = Integer.valueOf(packageMetaHeader.replaceAll(packageMetaHeaderPattern, "$2"));
        packageMeta.count = Integer.valueOf(packageMetaHeader.replaceAll(packageMetaHeaderPattern, "$3"));
        packageMeta.checkSum = packageMetaHeader.replaceAll(packageMetaHeaderPattern, "$4");
        return packageMeta;
    }

    private static String calcFileCheckSum(final File file)
            throws Exception {
        final MessageDigest messageDigest = MessageDigest.getInstance("MD5");
        try (final InputStream inputStream = new BufferedInputStream(new FileInputStream(file))) {
            final byte[] buffer = new byte[0xFFFF];
            for (int size; (size = inputStream.read(buffer)) >= 0;)
                messageDigest.update(buffer, 0, size);
            final StringBuilder result = new StringBuilder();
            for (final byte digit : messageDigest.digest())
                result.append(String.format("%02x", digit));
            return result.toString().toUpperCase();
        }
    }

    @Override
    protected void doFilter(final HttpServletRequest request, final HttpServletResponse response, final FilterChain chain)
            throws IOException, ServletException {

        // Only PUT requests and matching Secret headers are accepted,
        // otherwise the filter behaves as if it does not exist. This is the
        // security concept. The filter reacts only after authorization.
        if (Objects.isNull(request.getMethod())
                || !("PUT").equalsIgnoreCase(request.getMethod())
                || !this.secret.equals(request.getHeader(HTTP_HEADER_SECRET))) {
            super.doFilter(request, response, chain);
            return;
        }

        // Without Packet header the request is responded with status 400 (Bad
        // Request) because the filter does not know what to do.
        final PackageMeta packageMeta = RemoteDeploymentFilter.detectPackageMeta(request);
        if (Objects.isNull(packageMeta)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.flushBuffer();
            return;
        }

        // The destination file can be sent chunks. In any case, the file is
        // created as a temporary file with a unique name and an index. The
        // unique name uses a UUID of the filter instance, so multiple filter
        // instances can use the same temp directory. If a file or a chunks
        // already exists, the request is responded with status 423 (locked).
        final String uuid = RemoteDeploymentFilter.UUID + "---" + packageMeta.uuid;
        final File tempDirectory = new File(System.getProperty("java.io.tmpdir"));
        final File packageTempFile = new File(tempDirectory, uuid + "_" + packageMeta.number + ".temp");
        final File packageWorkFile = new File(tempDirectory, uuid + "_" + packageMeta.number + ".work");
        if (packageTempFile.exists()
                || packageWorkFile.exists()) {
            response.setStatus(423);
            response.flushBuffer();
            return;
        }

        try {

            // Two file extensions are used for saving: temp + work.
            // Temp means the data is still being loaded and the file is not
            // yet complete. Only with the successful end, the file extension
            // work is set. Thus, with asynchronous use, other threads can
            // identify only completed uploads.

            // In any case, the request is answered here as successful with
            // status 201, the rest continues to happen in the background.
            // There is no helpful response, since it is not known how long the
            // process takes and what shell commands may do.
            Files.copy(request.getInputStream(), packageTempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            final File packagePackFile = new File(tempDirectory, uuid + "_" + packageMeta.number + ".work");
            packageTempFile.renameTo(packagePackFile);
            request.getInputStream().close();
            response.setStatus(HttpServletResponse.SC_CREATED);
            response.flushBuffer();

            // The filter does not know at that moment how many chunks the
            // destination file consists of, how many parts have already been
            // received and whether the sending is complete. Assuming that the
            // data transfer can be asynchronous and the order of the chunks
            // can be unordered, the state of the sending  is analyzed
            // synchronously so that multiple instances do not do the same.
            synchronized (this) {

                // Chunks are counted, if one is missing the wait continues.
                for (int loop = 1; loop <= packageMeta.count; loop++)
                    if (!new File(tempDirectory, uuid + "_" + loop + ".work").exists())
                        return;

                // The chunks are merged into one file.
                final File packagePackFileFinal = new File(tempDirectory, uuid + ".work");
                try (FileOutputStream packagePackFileFinalOutputStream = new FileOutputStream(packagePackFileFinal)) {
                    for (int loop = 1; loop <= packageMeta.count; loop++) {
                        final File packagePackSourceFile = new File(tempDirectory, uuid + "_" + loop + ".work");
                        try (FileInputStream packagePackSourceFileInputStream = new FileInputStream(packagePackSourceFile)) {
                            final byte[] bytes = new byte[0xFFFF];
                            for (int size; (size = packagePackSourceFileInputStream.read(bytes)) >= 0; )
                                packagePackFileFinalOutputStream.write(bytes, 0, size);
                        }
                    }
                } finally {
                    // Clean up the temporary chunks after merging.
                    for (int loop = 1; loop <= packageMeta.count; loop++)
                        new File(tempDirectory, uuid + "_" + loop + ".work").delete();
                }

                // Checksum of the final file must match the checksum from the
                // Package header. Otherwise, it will cause a servlet
                // exception, which is ok because the response was closed.
                final String checkSum;
                try {checkSum = RemoteDeploymentFilter.calcFileCheckSum(packagePackFileFinal);
                } catch (Exception exception) {
                    throw new ServletException(exception);
                }
                if (!checkSum.equalsIgnoreCase(packageMeta.checkSum))
                    throw new ServletException(String.format("%s: Invalid checksum of %s", RemoteDeploymentFilter.class.getSimpleName(), this.destination));
                // If everything fits, the destination file is replaced.
                Files.move(packagePackFileFinal.toPath(), this.destination.toPath(), StandardCopyOption.REPLACE_EXISTING);

                // Execution of the command line command for the deployment.
                if (Objects.nonNull(this.command)) {
                    Process process = Runtime.getRuntime().exec(this.command);
                    String error = new String(process.getErrorStream().readAllBytes());
                    if (!error.isBlank())
                        throw new ServletException(String.format("%s: Error during script execution%n%s", RemoteDeploymentFilter.class.getSimpleName(), error.trim()));
                }
            }

        } finally {

            // All expired temporary files matching the UUID of this filter
            // instance will be cleaned.
            final long expiration = System.currentTimeMillis() -this.expiration;
            Arrays.stream(tempDirectory.listFiles())
                    .filter(file -> file.isFile() && file.getName().startsWith(UUID + "---") && file.lastModified() <= expiration)
                    .forEach(file -> file.delete());
        }
    }
}