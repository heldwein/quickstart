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

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import java.io.IOException;

/**
 * A {@link CallbackHandler} which just sets up the {@link NameCallback} with username as "peter"
 * and {@link PasswordCallback} with password as "griffin1234#". This user name and password are hardcoded
 * here just for the sake of demonstration of how the {@link CallbackHandler}s come into play during a
 * authentication negotiation for connection creation, for remote EJB client applications. These however
 * need not (must not) be hardcoded and instead a typical JAAS login process can be carried out on the client side.
 * <p/>
 * Note that this user name and password is the same as what we created on the server side using the <code>add-user</code>
 * script in the "ApplicationRealm".
 * <p/>
 * For more details on how this callback handler comes into picture during authentication negotiation, take a look
 * at the javadocs of {@link CustomServerLoginHandshakeCallbackHandler}
 *
 * @author Jaikiran Pai
 */
public class ClientLoginCallbackHandler implements CallbackHandler {

    private final String userName = "peter";
    private final String password = "griffin1234#";

    public ClientLoginCallbackHandler() {
    }

    @Override
    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        // handle the callbacks
        for (final Callback current : callbacks) {
            if (current instanceof NameCallback) {
                final NameCallback ncb = (NameCallback) current;
                // set the user name
                ncb.setName(this.userName);

            } else if (current instanceof PasswordCallback) {
                final PasswordCallback pcb = (PasswordCallback) current;
                // set the password
                pcb.setPassword(this.password.toCharArray());
            } else {
                throw new UnsupportedCallbackException(current);
            }
        }
    }
}
