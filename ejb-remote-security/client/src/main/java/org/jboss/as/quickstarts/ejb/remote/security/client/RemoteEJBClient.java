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

package org.jboss.as.quickstarts.ejb.remote.security.client;

import org.jboss.as.quickstarts.ejb.remote.security.SecureRemote;
import org.jboss.ejb.client.EJBClientContext;

import javax.ejb.EJBAccessException;
import javax.naming.Context;
import javax.naming.InitialContext;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * @author Jaikiran Pai
 */
public class RemoteEJBClient {

    private final ScopedEJBClientContextSelector<String> scopedEJBClientContextSelector;

    private enum EjbMethod {
        METHOD_FOR_ROLE_ONE,
        METHOD_FOR_ROLE_TWO,
        METHOD_FOR_ANY_ROLE,
        METHOD_FOR_DENY_ALL
    }

    public static void main(String[] args) throws Exception {
        final RemoteEJBClient remoteEJBClient = new RemoteEJBClient();
        remoteEJBClient.invokeOnSecuredSLSB();
    }

    public RemoteEJBClient() {
        // Let's get the current EJB client context, which might be possibly null
        // if our classpath doesn't have any jboss-ejb-client.properties (for example)
        final EJBClientContext defaultClientContext = EJBClientContext.getCurrent();
        // let's create our own scoped EJB client context selector
        this.scopedEJBClientContextSelector = new ScopedEJBClientContextSelector<String>(defaultClientContext);
        // now let's force the EJB client project to use our selector
        EJBClientContext.setSelector(this.scopedEJBClientContextSelector);
        // now let's lock the selector so that no other code in this application can change it
        EJBClientContext.lockSelector();
    }

    private void invokeOnSecuredSLSB() throws Exception {

        final String userOne = "userone";
        final String userTwo = "usertwo";

        final EJBInvoker ejbInvokerForRoleOne = new EJBInvoker(EjbMethod.METHOD_FOR_ROLE_ONE, userOne, "passone");
        final EJBInvoker ejbInvokerForRoleTwo = new EJBInvoker(EjbMethod.METHOD_FOR_ROLE_TWO, userTwo, "passtwo");
        // we use user-one for invoking on a EJB method allowing any role
        final EJBInvoker ejbInvokerForAnyRole = new EJBInvoker(EjbMethod.METHOD_FOR_ANY_ROLE, userOne, "passone");
        // we use user-two for invoking on a EJB method which doesn't any role to access it
        final EJBInvoker ejbInvokerForDenyAll = new EJBInvoker(EjbMethod.METHOD_FOR_DENY_ALL, userTwo, "passtwo");

        // we are going to do 4 invocations, one each in a separate thread
        final int NUM_THREADS = 4;
        final EJBInvoker[] ejbInvokers = new EJBInvoker[]{ejbInvokerForRoleOne, ejbInvokerForRoleTwo, ejbInvokerForAnyRole, ejbInvokerForDenyAll};
        final Future<String>[] invocationResults = new Future[NUM_THREADS];
        final ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        try {
            for (int i = 0; i < NUM_THREADS; i++) {
                // submit each invocation in a separate thread
                invocationResults[i] = executor.submit(ejbInvokers[i]);
            }
            // now get the results

            final String roleOneCaller = invocationResults[0].get();
            System.out.println("Caller prinicipal on server side for role one was " + roleOneCaller);
            if (!userOne.equals(roleOneCaller)) {
                throw new RuntimeException("Unexpected caller principal " + roleOneCaller + " for role one");
            }

            final String roleTwoCaller = invocationResults[1].get();
            System.out.println("Caller prinicipal on server side for role two was " + roleTwoCaller);
            if (!userTwo.equals(roleTwoCaller)) {
                throw new RuntimeException("Unexpected caller principal " + roleTwoCaller + " for role two");
            }

            final String permitAllCaller = invocationResults[2].get();
            System.out.println("Caller prinicipal on server side for @PermitAll was " + permitAllCaller);
            if (!userOne.equals(permitAllCaller)) {
                throw new RuntimeException("Unexpected caller principal " + permitAllCaller + " for @PermitAll method");
            }

            try {
                invocationResults[3].get();
                throw new RuntimeException("Invocation on a @DenyAll method was expected to fail");
            } catch (ExecutionException ee) {
                if (ee.getCause() instanceof EJBAccessException) {
                    // expected
                    System.out.println("Got the expected EJBAccessException for @DenyAll method invocation");
                } else {
                    throw ee;
                }
            }
        } finally {
            executor.shutdown();
        }
    }

    private class EJBInvoker implements Callable<String> {

        private final String userName;
        private final String password;
        private final EjbMethod methodToInvoke;

        EJBInvoker(final EjbMethod methodToInvoke, final String userName, final String password) {
            this.userName = userName;
            this.password = password;
            this.methodToInvoke = methodToInvoke;

        }

        @Override
        public String call() throws Exception {
            // setup the EJB client context
            this.setupEJBClientContext();
            // let's first create a InitialContext
            final Properties jndiContextProps = new Properties();
            jndiContextProps.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
            final Context jndiContext = new InitialContext(jndiContextProps);
            final String appName = "";
            final String moduleName = "jboss-as-ejb-remote-security-app";
            final String distinctName = "";
            final String beanName = "SecuredSLSB";
            final String beanInterface = SecureRemote.class.getName();
            final String jndiName = "ejb:" + appName + "/" + moduleName + "/" + distinctName + "/" + beanName + "!" + beanInterface;
            // lookup the remote interface of the bean
            final SecureRemote remoteSecuredBean = (SecureRemote) jndiContext.lookup(jndiName);
            // now it's time to invoke on the bean. Let's first set the "scope" of the selector
            // so that the correct EJB client context is used
            // In this example, we are using the username as the "scope" but it's ultimately up to the
            // application to decide what they want to use as scope
            scopedEJBClientContextSelector.setCurrentScope(this.userName);
            try {
                switch (this.methodToInvoke) {
                    case METHOD_FOR_ANY_ROLE:
                        return remoteSecuredBean.allowEveryone();
                    case METHOD_FOR_DENY_ALL:
                        remoteSecuredBean.allowNone();
                        throw new RuntimeException("An EJBException was expected for invoking on a @DenyAll method, but did not receive any");
                    case METHOD_FOR_ROLE_ONE:
                        return remoteSecuredBean.allowRoleOne();
                    case METHOD_FOR_ROLE_TWO:
                        return remoteSecuredBean.allowRoleTwo();
                    default:
                        throw new IllegalArgumentException("Unknown EJB method to invoke upon " + this.methodToInvoke);
                }
            } finally {
                // we are done with the invocation, so clear the current "scope"
                scopedEJBClientContextSelector.clearCurrentScope();
                // In this example, we no longer need any of the EJB client contexts and the underlying connection.
                // So let's just clear it
                scopedEJBClientContextSelector.unregisterScopedEJBClientContexts();
            }
        }

        private void setupEJBClientContext() {
            // create the EJB client context config properties, using this specific username
            // and password. The rest of the properties are common
            final Properties ejbClientContextProperties = this.createEJBClientContextProperties();
            // Register a EJB client context for this scope (==username)
            // we use the user name as the "scope" for our EJB client context selector
            // but that's just an example. The scope EJB client context selector is generic and
            // and depending on the application's needs, an appropriate scope can be used
            scopedEJBClientContextSelector.registerScopedEJBClientContext(userName, ejbClientContextProperties);
        }

        private Properties createEJBClientContextProperties() {
            final Properties ejbClientContextProps = new Properties();
            final String connectionName = "foo-bar-connection";
            ejbClientContextProps.put("remote.connectionprovider.create.options.org.xnio.Options.SSL_ENABLED", "false");
            // add a property which lists the connections that we are configuring. In
            // this example, we are just configuring a single connection named "foo-bar-connection"
            ejbClientContextProps.put("remote.connections", connectionName);
            // add a property which points to the host server of the "foo-bar-connection"
            ejbClientContextProps.put("remote.connection." + connectionName + ".host", "localhost");
            // add a property which points to the port on which the server is listening for EJB invocations
            ejbClientContextProps.put("remote.connection." + connectionName + ".port", "4447");
            // add the username and password properties which will be used to establish this connection
            ejbClientContextProps.put("remote.connection." + connectionName + ".username", userName);
            ejbClientContextProps.put("remote.connection." + connectionName + ".password", password);
            // disable "silent" auth, which gets triggered if the client is on the same machine as the server
            ejbClientContextProps.put("remote.connection." + connectionName + ".connect.options.org.xnio.Options.SASL_DISALLOWED_MECHANISMS", "JBOSS-LOCAL-USER");
//            ejbClientContextProps.put("remote.connection." + connectionName + ".connect.options.org.xnio.Options.SASL_POLICY_NOANONYMOUS", "false");
//            ejbClientContextProps.put("remote.connection." + connectionName + ".connect.options.org.xnio.Options.SASL_POLICY_NOPLAINTEXT", "false");

            return ejbClientContextProps;
        }

    }


}
