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
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * TODO:
 *
 * RemoteDeploymentPush 0.9.0 20210912<br>
 * Copyright (C) 2021 Seanox Software Solutions<br>
 * Alle Rechte vorbehalten.
 *
 * @author  Seanox Software Solutions
 * @version 0.9.0 20210912
 */
public class RemoteDeploymentPush {

    private static boolean verbose;

    public static void main(String... arguments)
            throws Exception {

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
        System.out.printf("Packages:    %sx max. %d bytes%n", deployment.packageCount, deployment.packageSize);
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
            final List<String> options = Arrays.asList(arguments).stream()
                    .filter(Objects::nonNull)
                    .filter(entry -> entry.contains(":"))
                    .collect(Collectors.toList());
            final List<String> header = new ArrayList<>();
            for (int index = 0; index < options.size() -1; index++) {
                String entry = options.get(index);
                if (!("-h").equals(entry)
                        && !("-H").equals(entry))
                    continue;
                entry = options.get(index +1);
                if (entry.isBlank()
                        || entry.startsWith("-"))
                    continue;
                header.add(entry);
            }
            return !header.isEmpty() ? header.toArray(new String[0]) : null;
        }

        private static Proxy detectHttpProxy(final String... arguments) {
            if (Objects.isNull(arguments))
                return null;
            final List<String> options = Arrays.asList(arguments).stream().filter(Objects::nonNull).collect(Collectors.toList());
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
            final List<String> options = Arrays.asList(arguments).stream().filter(Objects::nonNull).collect(Collectors.toList());
            final int index = options.stream().map(String::toLowerCase).collect(Collectors.toList()).indexOf("-s");
            final int size = index >= 0 && index < options.size() -1 ? Integer.valueOf(options.get(index)) : -1;
            return size > 0 ? size : 4 *1024 *1024;
        }

        private static boolean detectVerbose(final String... arguments) {
            return Arrays.stream(arguments).map(String::toLowerCase).collect(Collectors.toList()).contains("-v");
        }

        private static String calcFileCheckSum(final File file)
                throws Exception {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            try (DigestInputStream digestInputStream = new DigestInputStream(
                    new BufferedInputStream(
                            new FileInputStream(file)), messageDigest)) {
                while (digestInputStream.read() != -1);
                messageDigest = digestInputStream.getMessageDigest();
            }
            StringBuilder result = new StringBuilder();
            for (byte digit : messageDigest.digest())
                result.append(String.format("%02x", digit));
            return result.toString().toUpperCase();
        }

        private Deployment(final String... arguments)
                throws Exception {

            if (Objects.isNull(arguments))
                throw new WrongArgumentState();
            final List<String> options = Arrays.asList(arguments).stream().filter(Objects::nonNull).collect(Collectors.toList());
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
                    final HttpURLConnection connection = this.createConnection();
                    connection.setDoInput(true);
                    connection.setRequestMethod("PUT");
                    if (Objects.nonNull(this.requestHeader))
                        for (final String property : this.requestHeader) {
                            final String propertyPattern = "^\\s*(.*?)\\s*(?::\\s*(.*?))?\\s*$";
                            final String propertyKey = property.replaceAll(propertyPattern, "$1");
                            final String propertyValue = property.replaceAll(propertyPattern, "$2");
                            if (propertyKey.isBlank()
                                    || !propertyValue.isBlank())
                                continue;
                            connection.setRequestProperty(propertyKey, propertyValue);
                        }
                    connection.setRequestProperty("Package",
                            String.format("%s/%s/%s/%s",
                                    this.uuid, packageNumber, this.packageCount, this.checkSum));
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
                            dataNumber--;
                        }
                    }

                    final int responseCode = connection.getResponseCode();
                    connection.disconnect();
                    if (responseCode != 201)
                        throw new AbortState(String.format("Package %d of %d failed (status %d)", packageNumber, Integer.valueOf(packageCount), responseCode));
                    System.out.println(String.format("Package %d of %d complete (status %d)", packageNumber, Integer.valueOf(packageCount), responseCode));
                }
            }
        }
    }

    private static abstract class AbstractState extends RuntimeException {

        private static Throwable detectOriginalCase(Throwable throwable) {
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

            if (Objects.nonNull(message)
                    && Objects.nonNull(cause))
                System.out.printf("%s: %s%n%n", message, cause.getMessage());
            else if (Objects.nonNull(message))
                System.out.printf("%s%n%n", message);
            else if (Objects.nonNull(cause))
                System.out.printf("%s: %s%n%n", cause.getClass().getSimpleName(), cause.getMessage());

            System.out.printf("%s [0.0.0 00000000]%n", RemoteDeploymentPush.class.getName());
            System.out.printf("usage: %s <url> <file> [options...]%n", RemoteDeploymentPush.class.getName());
            System.out.println(" -p Proxy as URL, default port 3128");
            System.out.println(" -h Additional HTTP request headers");
            System.out.println(" -s Package size in bytes, default 4194304 bytes)");
            System.out.println(" -v Verbose exceptions with stacktrace");

            System.exit(1);
        }
    }

    private static class AbortState extends AbstractState {

        AbortState(String message) {
            System.out.println(message);
            System.exit(1);
        }

        AbortState(Exception cause) {
            if (!RemoteDeploymentPush.verbose) {
                System.out.println(cause.getClass().getSimpleName() + ": " + cause.getMessage());
                if (!AbstractState.detectOriginalCase(cause).equals(cause))
                    System.out.println(AbstractState.detectOriginalCase(cause).getClass().getSimpleName()
                            + ": " + AbstractState.detectOriginalCase(cause).getMessage());
            } else cause.printStackTrace(System.out);
            System.exit(1);
        }
    }
}