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

import javax.servlet.FilterConfig;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
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
 * RemoteDeploymentImpl abstracts the logic for {@link RemoteDeploymentFilter}
 * and {@link RemoteDeploymentServlet}. Thus, they differ only in the type of
 * configuration, but not in the way they work.
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
 * an error. After the expiration time, the temporary files are cleaned up. A
 * value 0 and smaller disables the clean up.
 *
 * <h3>Security Concept</h3>
 * Only PUT requests and matching Secret headers are accepted, otherwise the
 * filter behaves as if it does not exist. The filter reacts only after
 * sufficient authorization.
 *
 * RemoteDeploymentImpl 1.1.0 20211003<br>
 * Copyright (C) 2021 Seanox Software Solutions<br>
 * Alle Rechte vorbehalten.
 *
 * @author  Seanox Software Solutions
 * @version 1.1.0 20211003
 */
class RemoteDeploymentImpl {

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

    private void init(final String secret, final String destination, final String command, final String expiration)
            throws ServletException {

        if (Objects.isNull(secret)
                || secret.isBlank())
            throw new ServletException("Invalid parameter: " + PARAMETER_SECRET);
        this.secret = secret.trim();

        if (Objects.isNull(destination)
                || destination.isBlank())
            throw new ServletException("Invalid parameter: " + PARAMETER_DESTINATION);
        this.destination = new File(destination.trim());

        if (Objects.nonNull(command)
                && !command.isBlank())
            this.command = command.trim();

        if (Objects.nonNull(expiration)
                && !expiration.isBlank())
            if (!expiration.matches("^\\s*\\d{1,8}\\s*$"))
                throw new ServletException("Invalid parameter: " + PARAMETER_EXPIRATION);
            else this.expiration = Integer.valueOf(expiration.trim());
    }

    void init(final FilterConfig config)
            throws ServletException {
        this.init(config.getInitParameter(PARAMETER_SECRET),
                config.getInitParameter(PARAMETER_DESTINATION),
                config.getInitParameter(PARAMETER_COMMAND),
                config.getInitParameter(PARAMETER_EXPIRATION));
    }

    void init(final ServletConfig config)
            throws ServletException {
        this.init(config.getInitParameter(PARAMETER_SECRET),
                config.getInitParameter(PARAMETER_DESTINATION),
                config.getInitParameter(PARAMETER_COMMAND),
                config.getInitParameter(PARAMETER_EXPIRATION));
    }

    private static class PackageMeta {
        private String uuid;
        private String secret;
        private int number;
        private int count;
        private String checkSum;
    }

    private static PackageMeta detectPackageMeta(final HttpServletRequest request) {
        if (Objects.isNull(request))
            return null;
        final String packageMetaHeader = request.getHeader(HTTP_HEADER_PACKAGE);
        final String packageMetaHeaderPattern = "^(?i)([0-9a-z](?:[\\w-]*[0-9a-z])*)/([0-9a-z](?:[\\w-]*[0-9a-z])*)/(\\d+)/(\\d+)/((?:[0-9A-F]{2})+)$";
        if (Objects.isNull(packageMetaHeader)
                || !packageMetaHeader.matches(packageMetaHeaderPattern))
            return null;
        final PackageMeta packageMeta = new PackageMeta();
        packageMeta.uuid = packageMetaHeader.replaceAll(packageMetaHeaderPattern, "$1");
        packageMeta.secret = packageMetaHeader.replaceAll(packageMetaHeaderPattern, "$2");
        packageMeta.number = Integer.valueOf(packageMetaHeader.replaceAll(packageMetaHeaderPattern, "$3"));
        packageMeta.count = Integer.valueOf(packageMetaHeader.replaceAll(packageMetaHeaderPattern, "$4"));
        packageMeta.checkSum = packageMetaHeader.replaceAll(packageMetaHeaderPattern, "$5");
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

    static class UnsupportedRequestException extends ServletException {
    }

    void service(final HttpServletRequest request, final HttpServletResponse response)
            throws IOException, ServletException {

        // Only PUT requests and matching Secret headers are accepted,
        // otherwise the filter behaves as if it does not exist. This is the
        // security concept. The filter reacts only after authorization.
        if (Objects.isNull(request.getMethod())
                || !("PUT").equalsIgnoreCase(request.getMethod()))
            throw new UnsupportedRequestException();

        final PackageMeta packageMeta = RemoteDeploymentImpl.detectPackageMeta(request);
        if (Objects.isNull(packageMeta)
                || !this.secret.equals(packageMeta.secret))
            throw new UnsupportedRequestException();

        // The destination file can be sent chunks. In any case, the file is
        // created as a temporary file with a unique name and an index. The
        // unique name uses a UUID of the filter instance, so multiple filter
        // instances can use the same temp directory. If a file or a chunks
        // already exists, the request is responded with status 423 (locked).
        final String uuid = RemoteDeploymentImpl.UUID + "---" + packageMeta.uuid;
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
                try {checkSum = RemoteDeploymentImpl.calcFileCheckSum(packagePackFileFinal);
                } catch (Exception exception) {
                    throw new ServletException(exception);
                }
                if (!checkSum.equalsIgnoreCase(packageMeta.checkSum))
                    throw new ServletException(String.format("%s: Invalid checksum of %s", RemoteDeploymentImpl.class.getSimpleName(), this.destination));
                // If everything fits, the destination file is replaced.
                Files.move(packagePackFileFinal.toPath(), this.destination.toPath(), StandardCopyOption.REPLACE_EXISTING);

                // Execution of the command line command for the deployment.
                if (Objects.nonNull(this.command)) {
                    Process process = Runtime.getRuntime().exec(this.command);
                    String error = new String(process.getErrorStream().readAllBytes());
                    if (!error.isBlank())
                        throw new ServletException(String.format("%s: Error during script execution%n%s", RemoteDeploymentImpl.class.getSimpleName(), error.trim()));
                }
            }

        } finally {

            // All expired temporary files matching the UUID of this filter
            // instance will be cleaned.
            if (this.expiration > 0) {
                final long expiration = System.currentTimeMillis() -this.expiration;
                Arrays.stream(tempDirectory.listFiles())
                        .filter(file -> file.isFile()
                                && file.getName().startsWith(UUID + "---")
                                && file.lastModified() <= expiration)
                        .forEach(File::delete);
            }
        }
    }

    void destroy() {
        final File tempDirectory = new File(System.getProperty("java.io.tmpdir"));
        Arrays.stream(tempDirectory.listFiles())
                .filter(file -> file.isFile()
                        && file.getName().startsWith(UUID + "---"))
                .forEach(File::delete);
    }
}
