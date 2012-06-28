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

import org.jboss.ejb.client.ContextSelector;
import org.jboss.ejb.client.EJBClientConfiguration;
import org.jboss.ejb.client.EJBClientContext;
import org.jboss.ejb.client.PropertiesBasedEJBClientConfiguration;
import org.jboss.ejb.client.remoting.ConfigBasedEJBClientContextSelector;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @author Jaikiran Pai
 */
public class ScopedEJBClientContextSelector<T> implements ContextSelector<EJBClientContext> {

    private final ThreadLocal<Map<T, EJBClientContext>> scopedEJBClientContextsPerThread = new ThreadLocal<Map<T, EJBClientContext>>() {
        @Override
        protected Map<T, EJBClientContext> initialValue() {
            return new HashMap<T, EJBClientContext>();
        }
    };

    private final ThreadLocal<T> currentScope = new ThreadLocal<T>();

    private final EJBClientContext defaultEJBClientContext;

    public ScopedEJBClientContextSelector(final EJBClientContext defaultEJBClientContext) {
        this.defaultEJBClientContext = defaultEJBClientContext;
    }

    public ScopedEJBClientContextSelector(final Properties defaultEJBClientContextConfigProps) {
        this.defaultEJBClientContext = this.createEJBClientContext(defaultEJBClientContextConfigProps);
    }

    public void setCurrentScope(final T scope) {
        this.currentScope.set(scope);
    }

    public void clearCurrentScope() {
        this.currentScope.set(null);
    }

    public void registerScopedEJBClientContext(final T scope, final EJBClientContext ejbClientContext) {
        if (scope == null) {
            throw new IllegalArgumentException("Scope cannot be null for a scoped EJB client context");
        }
        final Map<T, EJBClientContext> scopedEJBClientContexts = this.scopedEJBClientContextsPerThread.get();
        final EJBClientContext alreadyRegisteredContext = scopedEJBClientContexts.get(scope);
        if (alreadyRegisteredContext != null) {
            throw new IllegalStateException("An EJB client context is already registered for scope " + scope);
        }
        scopedEJBClientContexts.put(scope, ejbClientContext);
    }

    public void registerScopedEJBClientContext(final T scope, final Properties ejbClientContextConfigProperties) {
        if (scope == null) {
            throw new IllegalArgumentException("Scope cannot be null for a scoped EJB client context");
        }
        final EJBClientContext newlyCreatedEJBClientContext = this.createEJBClientContext(ejbClientContextConfigProperties);
        // register the newly created EJB client context
        this.registerScopedEJBClientContext(scope, newlyCreatedEJBClientContext);
    }

    public void unregisterScopedEJBClientContexts() {
        this.scopedEJBClientContextsPerThread.remove();
    }

    @Override
    public EJBClientContext getCurrent() {
        // get the current scope first
        final T scope = this.currentScope.get();
        if (scope == null) {
            // use the default EJB client context (if any) in the absence of a scope
            return this.defaultEJBClientContext;
        }
        final Map<T, EJBClientContext> clientContexts = this.scopedEJBClientContextsPerThread.get();
        return clientContexts.get(scope);

    }

    private EJBClientContext createEJBClientContext(final Properties properties) {
        // Create a EJB client context configuration out of the properties
        final EJBClientConfiguration ejbClientConfiguration = new PropertiesBasedEJBClientConfiguration(properties);
        // Now create a EJB client context out of that configuration
        final ContextSelector<EJBClientContext> contextSelector = new ConfigBasedEJBClientContextSelector(ejbClientConfiguration);
        return contextSelector.getCurrent();

    }
}
