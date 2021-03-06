/**
 *
 * Copyright (c) 2009-2016 Freedomotic team http://freedomotic.com
 *
 * This file is part of Freedomotic
 *
 * This Program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2, or (at your option) any later version.
 *
 * This Program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * Freedomotic; see the file COPYING. If not, see
 * <http://www.gnu.org/licenses/>.
 */
package com.freedomotic.webserver;

import com.freedomotic.api.EventTemplate;
import com.freedomotic.api.Protocol;
import com.freedomotic.app.Freedomotic;
import com.freedomotic.exceptions.UnableToExecuteException;
import com.freedomotic.reactions.Command;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.security.KeyStore;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ssl.SslSocketConnector;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.webapp.WebAppContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Gabriel Pulido de Torres
 */
public class ApplicationServer extends Protocol {

    private static final Logger LOG = LoggerFactory.getLogger(ApplicationServer.class.getName());

//TODO: read from config file
    private static final String WEBAPP_CTX = "/";
    private static Server server;
    private final int port = configuration.getIntProperty("PORT", 8080);
    private final String webappDir = configuration.getStringProperty("WEBAPP_DIR", "/webapps/gwt_client");
    private final String warFile = configuration.getStringProperty("WAR_FILE", "");
    private final boolean enableSSL = configuration.getBooleanProperty("ENABLE_SSL", true);
    private final int sslPort = configuration.getIntProperty("SSL_PORT", 8443);
    private final String keystorePassword = configuration.getStringProperty("KEYSTORE_PASSWORD", "password");
    private final String keystoreFile = configuration.getStringProperty("KEYSTORE_FILE", "keystore");
    private final String keystoreType = configuration.getStringProperty("KEYSTORE_TYPE", "JKS");

    public ApplicationServer() {
        super("Application Server", "/webserver/applicationserver-manifest.xml");

        //TODO: check that the war file is correct.
    }

    /**
     * start the web server . if enable ssl is on then with ssl.
     */
    @Override
    public void onStart() {
        String dir = new File(this.getFile().getParent() + File.separator + webappDir).getAbsolutePath();
        server = new Server(port);
        LOG.info("Webserver now listens on port {}", port);

        if (enableSSL) {
            enableSSL();
        }

        if (!warFile.isEmpty()) {
            startServerWithWarFile(dir);
        } else {
            startServerWithoutWarfile(dir);
        }

    }

    /**
     * stop the sever.
     */
    @Override
    public void onStop() {
        try {
            server.stop();
            setDescription(configuration.getStringProperty("description", this.getName()));
        } catch (Exception ex) {
            LOG.error(Freedomotic.getStackTraceInfo(ex));
        }

    }

    @Override
    protected void onRun() {
        //throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    protected void onCommand(Command c) throws IOException, UnableToExecuteException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    protected boolean canExecute(Command c) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    protected void onEvent(EventTemplate event) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private void enableSSL() {
        String keyStorePath = this.getFile().getParent() + "/data/" + keystoreFile;
        try(FileInputStream keyStoreInputStream = new FileInputStream(keyStorePath))  {
            KeyStore keyStore = KeyStore.getInstance(keystoreType);
            keyStore.load(keyStoreInputStream, keystorePassword.toCharArray());
            SslContextFactory sslContextFactory = new SslContextFactory();
            sslContextFactory.setKeyStore(keyStore);
            sslContextFactory.setKeyStorePassword(keystorePassword);
            sslContextFactory.setKeyStoreType(keystoreType);
            SslSocketConnector sslConnector = new SslSocketConnector(sslContextFactory);
            sslConnector.setPort(sslPort);
            sslConnector.setMaxIdleTime(30000);
            LOG.info("Webserver now listens on SSL port {}", sslPort);
            server.addConnector(sslConnector);
        } catch (Exception ex) {
            LOG.error("Cannot load java keystore for reason: ", Freedomotic.getStackTraceInfo(ex));
        }
    }

    private void startServerWithWarFile(String dir) {
        try {
            WebAppContext webapp = new WebAppContext();
            webapp.setContextPath(WEBAPP_CTX);
            webapp.setWar(dir + WEBAPP_CTX + warFile);
            server.setHandler(webapp);

            //print the URL to visit as plugin description
            InetAddress addr = InetAddress.getLocalHost();
            String hostname = addr.getHostName();
            //strip away the '.war' extension and put all together
            URL url = new URL("http://" + hostname + ":" + port + WEBAPP_CTX + warFile.substring(0, warFile.lastIndexOf('.')));
            setDescription("Visit " + url.toString());
            server.start();
        } catch (FileNotFoundException nf) {
            LOG.warn("Cannot find WAR file {} into directory {}", warFile, dir);
        } catch (Exception ex) {
            LOG.error("Generic exception", Freedomotic.getStackTraceInfo(ex));
        }
    }

    private void startServerWithoutWarfile(String dir) {
        try {
            ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
            context.setContextPath(WEBAPP_CTX);
            context.setResourceBase(new File(dir + WEBAPP_CTX).getAbsolutePath());
            context.addServlet(DefaultServlet.class, "/*");
            server.setHandler(context);
            server.start();
        } catch (Exception ex) {
            LOG.error(Freedomotic.getStackTraceInfo(ex));
        }
    }
}
