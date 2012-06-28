package org.jboss.as.quickstarts.ejb.remote.security;/*
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

import org.jboss.ejb3.annotation.SecurityDomain;

import javax.annotation.Resource;
import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJBContext;
import javax.ejb.Remote;
import javax.ejb.Stateless;

/**
 * @author Jaikiran Pai
 */
@Stateless
@SecurityDomain("application-realm-backed-security-domain")
@Remote (SecureRemote.class)
public class SecuredSLSB implements SecureRemote {

    @Resource
    private EJBContext ejbContext;

    @RolesAllowed("role-one")
    @Override
    public String allowRoleOne() {
        return this.ejbContext.getCallerPrincipal().getName();
    }

    @RolesAllowed("role-two")
    @Override
    public String allowRoleTwo() {
        return this.ejbContext.getCallerPrincipal().getName();
    }

    @PermitAll
    @Override
    public String allowEveryone() {
        return this.ejbContext.getCallerPrincipal().getName();
    }

    @DenyAll
    @Override
    public void allowNone() {
        // do nothing
    }
}
