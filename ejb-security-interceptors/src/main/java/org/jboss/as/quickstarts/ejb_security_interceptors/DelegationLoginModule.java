package org.jboss.as.quickstarts.ejb_security_interceptors;

import java.security.Principal;
import java.security.acl.Group;

import javax.security.auth.login.LoginException;

import org.jboss.security.auth.spi.AbstractServerLoginModule;

public class DelegationLoginModule extends AbstractServerLoginModule {

    
    
    @Override
    public boolean login() throws LoginException {
        if (super.login() == true) {
            log.debug("super.login()==true");
            return true;
        }
        
        return super.login();
    }

    @Override
    protected Principal getIdentity() {
        return null;
    }

    @Override
    protected Group[] getRoleSets() throws LoginException {

        return null;
    }

}
