# gmbal
Glassfish MBean Annotation Library
The GlassFish MBean Annotation Library (gmbal, pronounced "Gumball") is a library for using annotations to 
create Open MBeans. There is similar functionality in JSR 255 for JDK 7, but gmbal only requires JDK 5. 
Gmbal also supports JSR 77 ObjectNames and the GlassFish Version 3 AMX requirements for MBeans. As a 
consequence, gmbal-enabled classes will be fully manageable in GlassFish v3 using the standard GlassFish 
3 admin tools, while still being manageable with generic MBean tools when not run under GlassFish v3. 

See the companion project: https://github.com/javaee/gmbal-pfl

Released artifacts: 
* https://maven.java.net/content/repositories/releases/org/glassfish/gmbal/
* https://maven.java.net/content/repositories/releases/org/glassfish/external/management-api/
* https://maven.java.net/content/repositories/releases/org/glassfish/pfl/
