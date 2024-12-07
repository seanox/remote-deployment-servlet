<p>
  <a href="https://github.com/seanox/remote-deployment-servlet/pulls"
      title="Development is waiting for new issues / requests / ideas"
    ><img src="https://img.shields.io/badge/development-passive-blue?style=for-the-badge"
  ></a>  
  <a href="https://github.com/seanox/remote-deployment-servlet/issues"
    ><img src="https://img.shields.io/badge/maintenance-active-green?style=for-the-badge"
  ></a>
  <a href="http://seanox.de/contact"
    ><img src="https://img.shields.io/badge/support-active-green?style=for-the-badge"
  ></a>
</p>


# Description

In some cases where deployment with modern containers is not yet possible, a
JEE application can be updated with the manager because a restart of the
application server is required, command line execution is needed, or the
application is so large that it cannot be transferred with the upload load
limit, for example, for continuous delivery. For these cases, the Remote
Deployment Servlet has been created.

The following is provided:

__RemoteDeploymentFilter__ to receive deployment packets sent as PUT requests
and in fragments if necessary, and to execute optional command line
instructions. Sending is a normal HTTP request with one additional header. The
request can be sent with various tools.

Example of configuration via web.xml

```xml
<web-app ...>
  ...
  <filter>
    <filter-name>RemoteDeploymentFilter</filter-name>
    <filter-class>com.seanox.RemoteDeploymentFilter</filter-class>
    <init-param>
      <param-name>secret</param-name>
      <param-value>B43AA6F00D034661722495C388527735</param-value>
    </init-param>
    <init-param>
      <param-name>destination</param-name>
      <param-value>D:\Tomcat\webapps\application.war</param-value>
    </init-param>
    <init-param>
      <param-name>command</param-name>
      <param-value>
        cmd /C ping -n 60 127.0.0.1 &amp;gt; NUL
        &amp;amp; net stop TomcatService
        &amp;amp; ping -n 60 127.0.0.1 &amp;gt; NUL
        &amp;amp; net start TomcatService      
      </param-value>
    </init-param>    
    <init-param>
      <param-name>expiration</param-name>
      <param-value>300000</param-value>
    </init-param>
  </filter>
  <filter-mapping>
    <filter-name>RemoteDeploymentFilter</filter-name>
    <url-pattern>/97C698B4EF93088CAF0A721A792D3AB6</url-pattern>
  </filter-mapping>
  ...
</web-app>
```
> [!IMPORTANT]  
> Automatic deployment must be disabled for the servlet container. For example,
> for Tomcat, this can be configured in `server.xml` via `autoDeploy`.

> [!IMPORTANT]  
> The project provides releases for the Servlet API 4 and 6. The major number in
> the release number always references the servlet API, followed by the major,
> minor and patch number of the release. Therefore, both versions are always
> described in the following documentation.


__RemoteDeploymentServlet__ The servlet is an alternative to the filter and is
useful when the remote deployment is to run outside the context of the
application. This makes the remote deployment accessible and usable even if the
application is not available due to errors at startup.

The servlet is configured analogously to the filter. It is recommended to use
the servlet as a standalone WAR so that all advantages can be used.

Example of configuration via web.xml

```xml
<web-app ...>
  ...
  <servlet>
    <servlet-name>RemoteDeploymentServlet</servlet-name>
    <servlet-class>com.seanox.RemoteDeploymentServlet</servlet-class>
    <init-param>
      ...
    </init-param>
  </servlet>
  <servlet-mapping>
    <servlet-name>RemoteDeploymentServlet</servlet-name>
    <url-pattern>/97C698B4EF93088CAF0A721A792D3AB6</url-pattern>
  </servlet-mapping>
  ...
</web-app>
```


__RemoteDeploymentPush__ is a command line tool without further dependencies
and sends files, if necessary in chunks, using the Package header in the HTTP
request and was developed specifically for RemoteDeploymentFilter.

```
usage: java -jar seanox-remote-deployment-4.1.0.0.jar <url> <secret> <file>
  -p Proxy as URL, default port 3128
  -h Additional HTTP request headers as <header>:<value>
  -s Chunk size in bytes, default 4194304 bytes
  -v Verbose exceptions with stacktrace
```

```
usage: java -jar seanox-remote-deployment-6.1.0.0.jar <url> <secret> <file>
  -p Proxy as URL, default port 3128
  -h Additional HTTP request headers as <header>:<value>
  -s Chunk size in bytes, default 4194304 bytes
  -v Verbose exceptions with stacktrace
```

Structure of the HTTP request

```
PUT /<endpoint> HTTP/1.0
Package: <uuid>/<secret>/<package-number>/<package-count>/<check-sum>
...
```


# Licence Agreement
LIZENZBEDINGUNGEN - Seanox Software Solutions ist ein Open-Source-Projekt, im
Folgenden Seanox Software Solutions oder kurz Seanox genannt.

Diese Software unterliegt der Version 2 der Apache License.

Copyright (C) 2024 Seanox Software Solutions

Licensed under the Apache License, Version 2.0 (the "License"); you may not use
this file except in compliance with the License. You may obtain a copy of the
License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed
under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
CONDITIONS OF ANY KIND, either express or implied. See the License for the
specific language governing permissions and limitations under the License.


# System Requirement
Java 11 or higher


# Downloads
https://mvnrepository.com/artifact/com.seanox/seanox-remote-deployment  
https://mvnrepository.com/artifact/com.seanox/seanox-remote-deployment/4.1.0.0
```xml
<dependency>
    <groupId>com.seanox</groupId>
    <artifactId>seanox-remote-deployment</artifactId>
    <version>4.1.0.0</version>
</dependency>
```
https://mvnrepository.com/artifact/com.seanox/seanox-remote-deployment/6.1.0.0
```xml
<dependency>
    <groupId>com.seanox</groupId>
    <artifactId>seanox-remote-deployment</artifactId>
    <version>6.1.0.0</version>
</dependency>
```

# Changes 
## 1.0.0 20241208  
BF: Review: Optimization/Corrections  
BF: Update of dependencies  
CR: Build: Change to jakarta.servlet-api (4.x)  
CR: Build: Optimization for jakarta.servlet-api javax + jakarta  
CR: Build: New approach for the version number  

[Read more](https://raw.githubusercontent.com/seanox/remote-deployment-servlet/master/CHANGES)


# Contact
[Issues](https://github.com/seanox/remote-deployment-servlet/issues)  
[Requests](https://github.com/seanox/remote-deployment-servlet/pulls)  
[Mail](https://seanox.com/contact)  
