/* 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2008-2009 Sun Microsystems, Inc. All rights reserved.
 * 
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License. You can obtain
 * a copy of the License at https://glassfish.dev.java.net/public/CDDL+GPL.html
 * or glassfish/bootstrap/legal/LICENSE.txt.  See the License for the specific 
 * language governing permissions and limitations under the License.
 * 
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/LICENSE.TXT.
 * Sun designates this particular file as subject to the "Classpath" exception
 * as provided by Sun in the GPL Version 2 section of the License file that
 * accompanied this code.  If applicable, add the following below the License
 * Header, with the fields enclosed by brackets [] replaced by your own
 * identifying information: "Portions Copyrighted [year]
 * [name of copyright owner]"
 * 
 * Contributor(s):
 * 
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 * 
 */ 
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

/** NOP impl of ManagedObjectManager used when annotations and ManagedObjectManager
 * are needed, but MBeans are not.  This allows using gmbal to optionally support
 * MBeans.  This is the implementation of the ManagedObjectManager that is used when
 * the full implementation is not available.
 *
 * @author ken_admin
 */
class ManagedObjectManagerNOPImpl implements ManagedObjectManager {
    static final ManagedObjectManager self =
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

    public void setTypelibDebug(int level) {
        // NOP
    }
}
