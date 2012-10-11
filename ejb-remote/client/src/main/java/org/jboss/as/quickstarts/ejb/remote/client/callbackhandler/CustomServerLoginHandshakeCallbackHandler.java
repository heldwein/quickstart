/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.quickstarts.ejb.remote.client.callbackhandler;

import org.jboss.security.SecurityContext;
import org.jboss.security.SecurityContextAssociation;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.security.sasl.RealmCallback;
import java.io.IOException;
import java.net.URL;
import java.security.Principal;

/**
 * A {@link CallbackHandler} which will handle callbacks that are issued when the server is negotiating a
 * connection authentication with the client.
 * <p/>
 * This callback handler will first check if a client side JAAS login has already been performed and if it hasn't
 * then it will issue a {@link javax.security.auth.login.LoginContext#login()} during the callback handling. Once
 * the client side login completes, this callback handler then uses the APIs provided by Picketbox to fetch the
 * logged in principal and credentials. This principal and credentials are then passed on to the server via the callbacks.
 * <p/>
 * A typical flow where this callback handler comes into picture is as follows:
 * <ol>
 *     <li>Client application boots</li>
 *     <li>EJB client API find a jboss-ejb-client.properties in the classpath and starts creating EJB receivers out of it</li>
 *     <li>The EJB receiver creation triggers a connection request to the server which is secured</li>
 *     <li>The secure server then starts negotiating with the incoming client connection request for authentication credentials
 *          via the {@link Callback}s
 *     </li>
 *     <li>Since this callback handler is configured in the jboss-ejb-client.properties the EJB client API instantiates
 *          an instance of this class and lets it handle the {@link Callback}s from the server
 *     </li>
 *     <li>This callback handler in its {@link #handle(javax.security.auth.callback.Callback[])} method then checks
 *          if there's already been a client side (JAAS) login. It uses the Picketbox APIs to get to that info.
 *          If there hasn't yet been a login, then it triggers a JAAS {@link javax.security.auth.login.LoginContext#login()}
 *          which uses our configured <code>auth.conf</code> JAAS login module stack which looks like this:
 *          <code>
 *              myapp-jaas-stack{
 *                  org.jboss.security.ClientLoginModule required;
 *              };
 *          </code>
 *          So the JAAS login module stack is configured to just use the org.jboss.security.ClientLoginModule which
 *          doesn't really do any authentication but just sets up the security context with whatever callback information
 *          the {@link ClientLoginCallbackHandler} provided.
 *          Note that the login module stack need not just contain the org.jboss.security.ClientLoginModule, it might
 *          have other login modules too which might do actual client side authentication.
 *          During this login process, the JAAS login module uses our {@link ClientLoginCallbackHandler} since that's what
 *          we have passed on to the constructor of {@link LoginContext} in our {@link #doJaasLogin()} method. See the javadoc
 *          of {@link ClientLoginCallbackHandler} for more details on it
 *     </li>
 *     <li>
 *         Once the login process (if at all it was initiated) completes successfully in this callback handler, this callback
 *         handler then uses Picketbox APIs to fetch the {@link Principal} and credential of the logged-in user and
 *         uses that information to setup the {@link Callback}s it has been handling. This information that has now
 *         been setup in the {@link Callback}s will then be available to the server side during the authentication
 *         negotiating and if they pass the rules, the connection creation succeeds and ultimately a EJB receiver
 *         capabale of handling requests to EJBs deployed on the server will be created and registered in the EJB client
 *         context for this client application
 *     </li>
 * </ol>
 *
 * @author Jaikiran Pai
 */
public class CustomServerLoginHandshakeCallbackHandler implements CallbackHandler {

    private static final String LOGIN_CONFIG = "myapp-jaas-stack";

    static {
        // Set the auth login config file to be used for the JAAS login module stack. In our
        // case we are using a file named auth.conf
        final String loginConfigFilename = "auth.conf";
        final URL loginConfigFile = CustomServerLoginHandshakeCallbackHandler.class.getClassLoader().getResource(loginConfigFilename);
        if (loginConfigFile == null) {
            throw new RuntimeException("Could not find login config file: " + loginConfigFilename);
        }
        System.out.println("Found login config file at " + loginConfigFile.toExternalForm());
        System.setProperty("java.security.auth.login.config", loginConfigFile.toExternalForm());
    }

    @Override
    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        // get the security context (if there's already one)
        SecurityContext securityContext = SecurityContextAssociation.getSecurityContext();
        if (securityContext == null) {
            // establish a security context by doing a login in
            try {
                this.doJaasLogin();
            } catch (LoginException e) {
                throw new IOException("Failed to login using JAAS login config: " + LOGIN_CONFIG, e);
            }
            // fetch the new associated security context
            securityContext = SecurityContextAssociation.getSecurityContext();
            // if it's still not available, we have a problem
            if (securityContext == null) {
                throw new IllegalStateException("No security context available even after a JAAS login");
            }
        }

        // now handle the callbacks
        for (final Callback current : callbacks) {
            if (current instanceof RealmCallback) {
                final RealmCallback rcb = (RealmCallback) current;
                final String defaultText = rcb.getDefaultText();
                rcb.setText(defaultText); // For now just use the realm suggested.
            } else if (current instanceof NameCallback) {
                final NameCallback ncb = (NameCallback) current;
                // fetch the principal that was associated via our JAAS login
                final Principal principal = SecurityContextAssociation.getPrincipal();
                if (principal == null) {
                    throw new IllegalStateException("No principal available for handling NameCallback");
                }
                // set the principal name into the NameCallback
                ncb.setName(principal.getName());

            } else if (current instanceof PasswordCallback) {
                final PasswordCallback pcb = (PasswordCallback) current;
                // fetch the credential that was associated via the JAAS login
                final Object credential = SecurityContextAssociation.getCredential();
                if (credential == null) {
                    throw new IllegalStateException("No credential available for handling PasswordCallback");
                }
                // Well, we know we set the password to a char[] in our client side callback handler
                // (@see ClientLoginCallbackHandler#handle()). So let's just cast it
                if (credential instanceof char[]) {
                    pcb.setPassword((char[]) credential);
                } else {
                    throw new IOException("Unknown credential type while handling PasswordCallback");
                }
            } else {
                throw new UnsupportedCallbackException(current);
            }
        }
    }

    private void doJaasLogin() throws LoginException {
        final LoginContext loginContext = new LoginContext(LOGIN_CONFIG, new ClientLoginCallbackHandler());
        loginContext.login();
    }
}
