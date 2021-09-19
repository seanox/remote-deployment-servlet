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

import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;

import java.util.HashMap;

@SpringBootApplication
class Application extends SpringBootServletInitializer {

    static void main(final String... options) {
        final SpringApplication springApplication = new SpringApplication(Application.class);
        springApplication.setBannerMode(Banner.Mode.CONSOLE);
        springApplication.run(options);
    }

    @Bean
    FilterRegistrationBean remoteDeploymentFilterRegistration() {
        final FilterRegistrationBean registration = new FilterRegistrationBean();
        registration.setFilter(new RemoteDeploymentFilter());
        registration.setInitParameters(new HashMap<>() {{
            put("secret", "A1B2C3D4E5F6G7H8");
            put("destination", "output_1.png");
            put("command", "cmd /C ping -n 3 127.0.0.1 > NUL && dir /B > output_2.txt && ping -n 3 127.0.0.1 > NUL && dir /B > output_3.txt");
            put("expiration", "300000");
        }});
        registration.addUrlPatterns("/0123456789ABCDEF");
        return registration;
    }
}