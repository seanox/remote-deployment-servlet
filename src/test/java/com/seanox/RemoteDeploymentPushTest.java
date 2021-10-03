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
import java.io.PrintStream;

public class RemoteDeploymentPushTest {

    private static final PrintStream OUTPUT = System.out;

    @BeforeAll
    static void startServlet() {
        Application.main();
    }

    @BeforeEach
    @AfterEach
    void cleanUp() {
        System.setOut(OUTPUT);
    }

    @Test
    void test_1() {
        final ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputBuffer));
        Throwable throwable = Assertions.assertThrows(Exception.class, RemoteDeploymentPush::main);
        OUTPUT.println(outputBuffer);
        Assertions.assertEquals("AbortState", throwable.getClass().getSimpleName());
        final String outputText = outputBuffer.toString();
        final String failedPattern = "usage: com.seanox.RemoteDeploymentPush";
        if (!outputText.contains(failedPattern))
            Assertions.fail("Missing output: " + failedPattern);
    }

    @Test
    void test_2() {
        final ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputBuffer));
        Throwable throwable = Assertions.assertThrows(Exception.class, () ->
            RemoteDeploymentPush.main("xxx")
        );
        OUTPUT.println(outputBuffer);
        Assertions.assertEquals("AbortState", throwable.getClass().getSimpleName());
        final String outputText = outputBuffer.toString();
        final String failedPattern = "usage: com.seanox.RemoteDeploymentPush";
        if (!outputText.contains(failedPattern))
            Assertions.fail("Missing output: " + failedPattern);
        if (outputText.contains("Invalid destination URL"))
            Assertions.fail("Output contains unexpected: Invalid destination URL");
        if (outputText.contains("Invalid secret"))
            Assertions.fail("Output contains unexpected: Invalid secret");
        if (outputText.contains("Invalid path of data file"))
            Assertions.fail("Output contains unexpected: Invalid path of data file");
    }

    @Test
    void test_3() {
        final ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputBuffer));
        Throwable throwable = Assertions.assertThrows(Exception.class, () ->
            RemoteDeploymentPush.main("xxx", "xxx", "xxx")
        );
        OUTPUT.println(outputBuffer);
        Assertions.assertEquals("AbortState", throwable.getClass().getSimpleName());
        final String outputText = outputBuffer.toString();
        final String failedPattern = "usage: com.seanox.RemoteDeploymentPush";
        if (!outputText.contains(failedPattern))
            Assertions.fail("Missing output: " + failedPattern);
        if (!outputText.contains("Invalid destination URL"))
            Assertions.fail("Missing: Invalid destination URL");
        if (outputText.contains("Invalid secret"))
            Assertions.fail("Output contains unexpected: Invalid secret");
        if (outputText.contains("Invalid path of data file"))
            Assertions.fail("Output contains unexpected: Invalid path of data file");
    }

    @Test
    void test_4() {
        final ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputBuffer));
        Throwable throwable = Assertions.assertThrows(Exception.class, () ->
                RemoteDeploymentPush.main("http://127.0.0.1/xxx", "_xxx", "xxx")
        );
        OUTPUT.println(outputBuffer);
        Assertions.assertEquals("AbortState", throwable.getClass().getSimpleName());
        final String outputText = outputBuffer.toString();
        final String failedPattern = "usage: com.seanox.RemoteDeploymentPush";
        if (!outputText.contains(failedPattern))
            Assertions.fail("Missing output: " + failedPattern);
        if (outputText.contains("Invalid destination URL"))
            Assertions.fail("Output contains unexpected: Invalid destination URL");
        if (!outputText.contains("Invalid secret"))
            Assertions.fail("Missing: Invalid secret");
    }

    @Test
    void test_5() {
        final ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputBuffer));
        Throwable throwable = Assertions.assertThrows(Exception.class, () ->
            RemoteDeploymentPush.main("http://127.0.0.1/xxx", "xxx", "xxx")
        );
        OUTPUT.println(outputBuffer);
        Assertions.assertEquals("AbortState", throwable.getClass().getSimpleName());
        final String outputText = outputBuffer.toString();
        final String failedPattern = "usage: com.seanox.RemoteDeploymentPush";
        if (!outputText.contains(failedPattern))
            Assertions.fail("Missing output: " + failedPattern);
        if (outputText.contains("Invalid destination URL"))
            Assertions.fail("Output contains unexpected: Invalid destination URL");
        if (outputText.contains("Invalid secret"))
            Assertions.fail("Output contains unexpected: Invalid secret");
        if (!outputText.contains("Invalid path of data file"))
            Assertions.fail("Missing: Invalid path of data file");
    }
}
