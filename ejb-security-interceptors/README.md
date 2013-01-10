ejb-security-interceptors:  Using client and server side interceptors to switch the identity for an EJB call.
====================
Author: Darran Lofthouse
Level: Advanced
Technologies: EJB, Security
Summary: Demonstrates how interceptors can be used to switch the identity for EJB calls on a call by call basis.
Target Product: EAP

What is it?
-----------



System requirements
-------------------




Configure Maven
---------------

If you have not yet done so, you must [Configure Maven](../README.md#mavenconfiguration) before testing the quickstarts.


Add the Application Users
---------------

This quick start is built around the default 'ApplicationRealm' as configured in the AS7 / EAP 6 distribution, the following three 
users should be added using the add-user utility.

'ConnectionUser' with role 'User' and password 'ConnectionPassword1!'.
'AppUserOne' with roles 'User' and 'RoleOne', any password can be specified for this user.
'AppUserTwo' with roles 'User' and 'RoleTwo', again any password can be specified for this user.  

The first user is used for establishing the actual connection to the server, the subsequent two users are the users that this
quickstart demonstrates can be switched to on demand.



Start JBoss Enterprise Application Platform 6 or JBoss AS 7
-------------------------



Build and Deploy the Quickstart
-------------------------

_NOTE: The following build command assumes you have configured your Maven user settings. If you have not, you must include Maven setting arguments on the command line. See [Build and Deploy the Quickstarts](../README.md#buildanddeploy) for complete instructions and additional options._

1. Make sure you have started the JBoss Server as described above.
2. Open a command line and navigate to the root directory of this quickstart.
3. Type this command to build and deploy the archive:

        mvn clean package jboss-as:deploy

4. This will deploy `target/jboss-as-ejb-security.war` to the running instance of the server.


Access the application 
---------------------




Undeploy the Archive
--------------------

1. Make sure you have started the JBoss Server as described above.
2. Open a command line and navigate to the root directory of this quickstart.
3. When you are finished testing, type this command to undeploy the archive:

        mvn jboss-as:undeploy


Run the Quickstart in JBoss Developer Studio or Eclipse
-------------------------------------
You can also start the server and deploy the quickstarts from Eclipse using JBoss tools. For more information, see [Use JBoss Developer Studio or Eclipse to Run the Quickstarts](../README.md#useeclipse) 


Debug the Application
------------------------------------

If you want to debug the source code or look at the Javadocs of any library in the project, run either of the following commands to pull them into your local repository. The IDE should then detect them.

    mvn dependency:sources
    mvn dependency:resolve -Dclassifier=javadoc
