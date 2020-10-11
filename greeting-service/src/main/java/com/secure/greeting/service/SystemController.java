/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */

package com.secure.greeting.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.Map;
import java.util.Date;
import java.util.List;
import java.io.StringWriter;
import java.util.Properties;
import java.util.Enumeration;

@RestController
public class SystemController {

    public static final Logger LOG = LogManager.getLogger(SystemController.class);
    private String pageTitle = "REST Controller System Information";

    @GetMapping("/system")
    public String system(HttpServletRequest request) {

        HttpSession session = request.getSession();

        StringWriter out = new StringWriter();

        out.write("<html>");
        out.write("<head>");
        out.write("<title>" + this.pageTitle + "</title>");
        out.write("</head>");

        out.write("<body>");
        out.write("<font size='12'>");
        out.write(this.pageTitle);
        out.write("</font><br><br>");

        PageVisits pageVisits = getSessionObj(session);
        pageVisits.increment();

        LOG.info("=============================================");
        LOG.info("Page Visits = " + pageVisits.getValue());
        LOG.info("Session ID = " + session.getId());
        LOG.info("=============================================");

        out.write("<hr>");
        out.write("Number of Visits = <font size='14'>" + pageVisits.getValue()
                + "</font><br>");
        out.write("Session ID = " + session.getId() + "<br>");
        out.write("Session Creation Time = " + new Date(session.getCreationTime()) + "<br>");
        out.write("Session Last Access Time = " + new Date(session.getLastAccessedTime())
                + "<br>");
        out.write("HttpServletRequest.isSecure(): " + request.isSecure() + "<br>");
        out.write("Your IP Address = " + request.getRemoteAddr() + "<br>");

        out.write("<br>");
        out.write("<hr>");

        Map<String, String> env = System.getenv();
        for (String envName : env.keySet()) {
            if (envName.startsWith("WEBSITE")) {
                out.write(String.format("%s = %s%n",
                        envName,
                        env.get(envName)));
                out.write("<br>");
            }
        }

        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();

        out.write("<hr>");
        out.write("Java VM Name = " + runtime.getVmName() + "<br>");
        out.write("Java VM Vendor = " + runtime.getVmVendor() + "<br>");
        out.write("Java VM Version = " + runtime.getSpecVersion() + "<br>");
        out.write("Java VM Full Version = " + System.getProperty("java.runtime.version") + "<br>");
        out.write("<hr>");

        List<String> stringList = runtime.getInputArguments();

        for (String arg : stringList){
            out.write("JVM ARG: ");
            out.write(arg);
            out.write("<br>");
        }

        out.write("<br>");
        out.write("<hr>");

        Properties properties = System.getProperties();
        Enumeration keys = properties.keys();
        String key = new String();
        while (keys.hasMoreElements()){
            key = (String)keys.nextElement();
            out.write("System Property: ");
            out.write(key + " = ");
            out.write((String) properties.get(key));
            out.write("<br>");
        }

        out.write("</body>");
        out.write("</html>");

        return out.toString();

    }

    private PageVisits getSessionObj(HttpSession session) {

        PageVisits pageVisits = (PageVisits)session.getAttribute("Analytics");
        if (pageVisits == null) {
            pageVisits = new PageVisits();
            session.setAttribute("Analytics", pageVisits);
        }

        return pageVisits;
    }

}