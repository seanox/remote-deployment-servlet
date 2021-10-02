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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;

public class RemoteDeploymentServletTest {

    private static final PrintStream OUTPUT = System.out;

    @BeforeAll
    static void startServlet() {
        Application.main();
    }

    private static final File OUTPUT_1 = new File("./output_servlet_1.png");
    private static final File OUTPUT_2 = new File("./output_servlet_2.txt");
    private static final File OUTPUT_3 = new File("./output_servlet_3.txt");

    @BeforeEach
    @AfterEach
    void cleanUp() {
        OUTPUT_1.delete();
        OUTPUT_2.delete();
        OUTPUT_3.delete();
    }

    @Test
    void test_1()
            throws Exception {
        final ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputBuffer));
        RemoteDeploymentPush.main("http://127.0.0.1:8080/FEDCBA9876543210",
                "8H7G6F5E4D3C2B1A",
                "./src/test/resources/example.png",
                "-v");
        OUTPUT.println(outputBuffer);
        final String outputText = outputBuffer.toString();
        for (int index = 1; index < 6; index++) {
            final String completePattern = String.format("Package %d of 6 complete (status 201,", index);
            if (!outputText.contains(completePattern))
                Assertions.fail("Missing output: " + completePattern);
        }
        // Some things happen with a time delay.
        // The merging of the chunks and only then the command line is executed.
        if (OUTPUT_1.exists())
            Assertions.fail(OUTPUT_1 + " already exists");
        Thread.sleep(1000);
        if (!OUTPUT_1.exists())
            Assertions.fail("Missing: " + OUTPUT_1);
        if (OUTPUT_2.exists())
            Assertions.fail(OUTPUT_2 + " already exists");
        if (OUTPUT_3.exists())
            Assertions.fail(OUTPUT_3 + " already exists");
        Thread.sleep(10000);
        if (!OUTPUT_2.exists())
            Assertions.fail("Missing: " + OUTPUT_2);
        if (!OUTPUT_3.exists())
            Assertions.fail("Missing: " + OUTPUT_3);
    }

    @Test
    void test_2() {
        final ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputBuffer));
        Throwable throwable = Assertions.assertThrows(Exception.class, () ->
                RemoteDeploymentPush.main("http://127.0.0.1:8080/FEDCBA9876543210",
                        "8H7G6F5E4D3C2B1Ax",
                        "./src/test/resources/example.png",
                        "-v")
        );
        OUTPUT.println(outputBuffer);
        Assertions.assertEquals("AbortState", throwable.getClass().getSimpleName());
        final String outputText = outputBuffer.toString();
        final String failedPattern = String.format("Package %d of 6 failed (status 404,", 1);
        if (!outputText.contains(failedPattern))
            Assertions.fail("Missing output: " + failedPattern);
    }

    @Test
    void test_3() {
        final ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputBuffer));
        Throwable throwable = Assertions.assertThrows(Exception.class, () ->
            RemoteDeploymentPush.main("http://127.0.0.1:8080/FEDCBA9876543210",
                    "8H7G6F5E4D3C2B1A",
                    "./src/test/resources/example.png_",
                    "-v")
        );
        OUTPUT.println(outputBuffer);
        Assertions.assertEquals("AbortState", throwable.getClass().getSimpleName());
        final String outputText = outputBuffer.toString();
        final String failedPattern = "Invalid path of data file: .\\src\\test\\resources\\example.png_";
        if (!outputText.contains(failedPattern))
            Assertions.fail("Missing output: " + failedPattern);
    }

    @Test
    void test_4() {
        final ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputBuffer));
        Throwable throwable = Assertions.assertThrows(Exception.class, () ->
            RemoteDeploymentPush.main("http://127.0.0.1:8080/FEDCBA9876543210_",
                    "8H7G6F5E4D3C2B1A",
                    "./src/test/resources/example.png",
                    "-v")
        );
        OUTPUT.println(outputBuffer);
        Assertions.assertEquals("AbortState", throwable.getClass().getSimpleName());
        final String outputText = outputBuffer.toString();
        final String failedPattern = String.format("Package %d of 6 failed (status 404,", 1);
        if (!outputText.contains(failedPattern))
            Assertions.fail("Missing output: " + failedPattern);
    }
}
