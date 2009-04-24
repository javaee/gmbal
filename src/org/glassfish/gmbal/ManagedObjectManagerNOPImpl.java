/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.glassfish.gmbal;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.ResourceBundle;
import javax.management.MBeanServer;
import javax.management.NotificationEmitter;
import javax.management.ObjectName;

/**
 *
 * @author ken_admin
 */
public class ManagedObjectManagerNOPImpl implements ManagedObjectManager {
	public static final ManagedObjectManager self = 
	    new ManagedObjectManagerNOPImpl() ;

	private ManagedObjectManagerNOPImpl() {}

	public void suspendJMXRegistration() {
	    // NOP
	}

	public void resumeJMXRegistration() {
	    // NOP
	}

	public NotificationEmitter createRoot() {
	    return null ;
	}

	public NotificationEmitter createRoot(Object root) {
	    return null ;
	}

	public NotificationEmitter createRoot(Object root, String name) {
	    return null ;
	}

	public Object getRoot() {
	    return null ;
	}

	public NotificationEmitter register(Object parent, Object obj, String name) {
	    return null ;
	}

	public NotificationEmitter register(Object parent, Object obj) {
	    return null ;
	}

	public NotificationEmitter registerAtRoot(Object obj, String name) {
	    return null ;
	}

	public NotificationEmitter registerAtRoot(Object obj) {
	    return null ;
	}

	public void unregister(Object obj) {
	    // NOP
	}

	public ObjectName getObjectName(Object obj) {
	    return null ;
	}

	public Object getObject(ObjectName oname) {
	    return null ;
	}

	public void stripPrefix(String... str) {
	    // NOP
	}

	public String getDomain() {
	    return null ;
	}

	public void setMBeanServer(MBeanServer server) {
	    // NOP
	}

	public MBeanServer getMBeanServer() {
	    return null ;
	}

	public void setResourceBundle(ResourceBundle rb) {
	    // NOP
	}

	public ResourceBundle getResourceBundle() {
	    return null ;
	}

	public void addAnnotation(AnnotatedElement element, Annotation annotation) {
	    // NOP
	}

	public void setRegistrationDebug(RegistrationDebugLevel level) {
	    // NOP
	}

	public void setRuntimeDebug(boolean flag) {
	    // NOP
	}

	public String dumpSkeleton(Object obj) {
	    return "" ;
	}

	public void close() throws IOException {
	    // NOP
	}
}
