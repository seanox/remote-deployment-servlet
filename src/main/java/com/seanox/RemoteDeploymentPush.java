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

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * TODO:
 *
 * RemoteDeploymentPush 0.9.0 20210919<br>
 * Copyright (C) 2021 Seanox Software Solutions<br>
 * Alle Rechte vorbehalten.
 *
 * @author  Seanox Software Solutions
 * @version 0.9.0 20210919
 */
public class RemoteDeploymentPush {

    private static boolean verbose;

    public static void main(String... arguments)
            throws Exception {

        // If RemoteDeploymentPush is run standalone, System.exit(1) is called
        // in case of an error, assuming that the call can be checked for
        // errors on the command line, which is possible with the error code.
        final String stackTraceElementFilter = RemoteDeploymentPush.class.getPackageName() + ".";
        final Stream<StackTraceElement> stackTraceElementStream = Arrays.stream(new Throwable().getStackTrace());
        if (stackTraceElementStream
                .filter(stackTraceElement -> !stackTraceElement.getClassName().startsWith(stackTraceElementFilter))
                .count() <= 0)
            Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                public void uncaughtException(final Thread thread, final Throwable throwable) {
                    System.exit(1);
                }
            });

        final Deployment deployment;
        try {deployment = Deployment.create(arguments);
        } catch (Exception exception) {
            throw new AbortState(exception);
        }

        RemoteDeploymentPush.verbose = deployment.verbose;

        System.out.printf("%s [0.0.0 00000000]%n", RemoteDeploymentPush.class.getName());
        System.out.println();
        System.out.printf("Destination: %s%n", deployment.destination);
        if (Objects.nonNull(deployment.httpProxy))
            System.out.printf("Proxy:       %s%n", deployment.httpProxy.address());
        System.out.printf("File:        %s%n", deployment.file.getCanonicalPath());
        System.out.printf("Checksum:    %s%n", deployment.checkSum);
        System.out.printf("Packages:    %sx up to %d bytes%n", deployment.packageCount, deployment.packageSize);
        System.out.printf("UUID:        %s%n", deployment.uuid);
        if (deployment.verbose)
            System.out.println("Verbose:     yes");
        System.out.println();
        try {deployment.push();
        } catch (Exception exception) {
            throw new AbortState(exception);
        }
    }

    private static class Deployment {

        private final URL destination;
        private final String[] requestHeader;
        private final Proxy httpProxy;
        private final File file;
        private final String checkSum;
        private final int packageCount;
        private final int packageSize;
        private final boolean verbose;
        private final String uuid;

        private static String[] detectRequestHeader(final String... arguments) {
            if (Objects.isNull(arguments))
                return null;
            final List<String> headers = new ArrayList<>();
            for (int index = 0; index < arguments.length -1; index++) {
                if (Objects.isNull(arguments[index])
                        || !("-h").equalsIgnoreCase(arguments[index]))
                    continue;
                if (Objects.isNull(arguments[index +1])
                        || arguments[index +1].isBlank()
                        || arguments[index +1].replaceAll(":", "") .isBlank())
                    continue;
                headers.add(arguments[index +1].trim());
            }
            return !headers.isEmpty() ? headers.toArray(new String[0]) : null;
        }

        private static Proxy detectHttpProxy(final String... arguments) {
            if (Objects.isNull(arguments))
                return null;
            final List<String> options = Arrays.stream(arguments).filter(Objects::nonNull).collect(Collectors.toList());
            final int index = options.stream().map(String::toLowerCase).collect(Collectors.toList()).indexOf("-p");
            if (index < 0
                    || index >= options.size() -1)
                return null;
            final String proxy = options.get(index +1);
            final String host = proxy.replaceAll("\\s*:.*$", "");
            final int port = proxy.matches(".*:.*") ? Integer.valueOf(proxy.replaceAll("^.*:\\s*", "")) : 3128;
            return new Proxy(Proxy.Type.HTTP, new InetSocketAddress(host, port));
        }

        private static int detectPackageSize(final String... arguments) {
            if (Objects.isNull(arguments))
                return 4 *1024 *1024;
            final List<String> options = Arrays.stream(arguments).filter(Objects::nonNull).collect(Collectors.toList());
            final int index = options.stream().map(String::toLowerCase).collect(Collectors.toList()).indexOf("-s");
            final int size = index >= 0 && index < options.size() -1 ? Integer.valueOf(options.get(index +1)) : -1;
            return size > 0 ? size : 4 *1024 *1024;
        }

        private static boolean detectVerbose(final String... arguments) {
            return Arrays.stream(arguments).map(String::toLowerCase).collect(Collectors.toList()).contains("-v");
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

        private Deployment(final String... arguments)
                throws Exception {

            if (Objects.isNull(arguments))
                throw new WrongArgumentState();
            final List<String> options = Arrays.stream(arguments).filter(Objects::nonNull).collect(Collectors.toList());
            if (options.size() < 2
                    || options.get(0).isBlank()
                    || options.get(1).isBlank())
                throw new WrongArgumentState();

            try {this.destination = new URL(options.get(0));
            } catch (MalformedURLException exception) {
                throw new WrongArgumentState("Invalid destination URL", exception);
            }

            this.file = new File(options.get(1));
            if (!this.file.exists()
                    || !this.file.isFile())
                throw new WrongArgumentState("Invalid path of data file: " + this.file);

            this.checkSum = Deployment.calcFileCheckSum(this.file);
            this.requestHeader = Deployment.detectRequestHeader(arguments);
            this.httpProxy = Deployment.detectHttpProxy(arguments);
            this.packageSize = Deployment.detectPackageSize(arguments);
            this.packageCount = (int)Math.ceil(this.file.length() /(double)(this.packageSize));
            this.verbose = Deployment.detectVerbose(arguments);
            this.uuid = UUID.randomUUID().toString().toUpperCase();
        }

        private static Deployment create(final String... arguments)
                throws Exception {
            return new Deployment(arguments);
        }

        private HttpURLConnection createConnection()
                throws IOException {
            if (Objects.nonNull(this.httpProxy))
                return (HttpURLConnection)this.destination.openConnection(this.httpProxy);
            return (HttpURLConnection)this.destination.openConnection();
        }

        private void push()
                throws IOException {
            try (InputStream inputStream = new BufferedInputStream(
                    new FileInputStream(this.file))) {
                long packageNumber = 0;
                long dataNumber = this.file.length();
                while (dataNumber > 0) {
                    final long timing = System.currentTimeMillis();
                    final HttpURLConnection connection = this.createConnection();
                    connection.setRequestMethod("PUT");
                    connection.setDoOutput(true);
                    if (Objects.nonNull(this.requestHeader))
                        for (final String property : this.requestHeader) {
                            final String propertyPattern = "^\\s*(.*?)\\s*(?::\\s*(.*?))?\\s*$";
                            final String propertyKey = property.replaceAll(propertyPattern, "$1");
                            final String propertyValue = property.replaceAll(propertyPattern, "$2");
                            if (propertyKey.isBlank()
                                    || propertyValue.isBlank())
                                continue;
                            connection.setRequestProperty(propertyKey, propertyValue);
                        }
                    connection.setRequestProperty("Package",
                            String.format("%s/%s/%s/%s",
                                    this.uuid, ++packageNumber, this.packageCount, this.checkSum));
                    connection.connect();

                    try (final OutputStream outputStream = connection.getOutputStream()) {
                        for (int dataWriteNumber = 0; dataWriteNumber < this.packageSize; dataWriteNumber++) {
                            final int digit = inputStream.read() & 0xFF;
                            if (digit < 0) {
                                if (dataNumber > 0)
                                    throw new EOFException(this.file.getCanonicalPath());
                                break;
                            }
                            outputStream.write(digit);
                            if (--dataNumber <= 0)
                                break;
                        }
                    }

                    final int responseCode = connection.getResponseCode();
                    connection.disconnect();

                    if (responseCode != 201)
                        throw new AbortState(String.format("Package %d of %d failed (status %d, %d ms)", packageNumber, packageCount, responseCode, System.currentTimeMillis() -timing));
                    System.out.printf("Package %d of %d complete (status %d, %d ms)%n", packageNumber, packageCount, responseCode, System.currentTimeMillis() -timing);
                }
            }
        }
    }

    private static abstract class AbstractState extends RuntimeException {

        private static Throwable detectOriginalCause(Throwable throwable) {
            while (Objects.nonNull(throwable.getCause())
                    && !throwable.equals(throwable.getCause()))
                throwable = throwable.getCause();
            return throwable;
        }
    }

    private static class WrongArgumentState extends AbstractState {

        WrongArgumentState() {
            this(null);
        }

        WrongArgumentState(String message) {
            this(message, null);
        }

        WrongArgumentState(String message, Exception cause) {

            System.out.printf("%s [0.0.0 00000000]%n", RemoteDeploymentPush.class.getName());

            if (Objects.nonNull(message)
                    && Objects.nonNull(cause))
                System.out.printf("%n%s: %s%n%n", message, cause.getMessage());
            else if (Objects.nonNull(message))
                System.out.printf("%n%s%n%n", message);
            else if (Objects.nonNull(cause))
                System.out.printf("%n%s: %s%n%n", cause.getClass().getSimpleName(), cause.getMessage());

            System.out.printf("usage: %s <url> <file> [options...]%n", RemoteDeploymentPush.class.getName());
            System.out.println(" -p Proxy as URL, default port 3128");
            System.out.println(" -h Additional HTTP request headers as <header>:<value>");
            System.out.println(" -s Chunk size in bytes, default 4194304 bytes)");
            System.out.println(" -v Verbose exceptions with stacktrace");
        }
    }

    private static class AbortState extends AbstractState {

        AbortState(String message) {
            System.out.println(message);
        }

        AbortState(Exception cause) {
            final Throwable originalCause = AbstractState.detectOriginalCause(cause);
            if (cause instanceof AbstractState)
                return;
            if (!RemoteDeploymentPush.verbose) {
                System.out.println(cause.getClass().getSimpleName() + ": " + cause.getMessage());
                if (!originalCause.equals(cause))
                    System.out.println(AbstractState.detectOriginalCause(cause).getClass().getSimpleName()
                            + ": " + AbstractState.detectOriginalCause(cause).getMessage());
            } else cause.printStackTrace(System.out);
        }
    }
}