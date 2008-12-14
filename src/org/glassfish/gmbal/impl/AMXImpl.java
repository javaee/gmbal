/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2007-2007 Sun Microsystems, Inc. All rights reserved.
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
 * file and include the License file at glassfish/bootstrap/legal/LICENSE.txt.
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
 */

package org.glassfish.gmbal.impl;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.management.MBeanException;
import org.glassfish.gmbal.AMX;
import org.glassfish.gmbal.generic.Algorithms;
import org.glassfish.gmbal.generic.UnaryFunction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.management.Descriptor;
import javax.management.MBeanInfo;
import javax.management.ObjectName;
import javax.management.modelmbean.ModelMBeanInfoSupport;
import org.glassfish.gmbal.ManagedObjectManager;

/**
 *
 * @author ken
 */
public class AMXImpl implements AMX {
    private MBeanImpl mbean ;

    public AMXImpl( final MBeanImpl mb ) {
        this.mbean = mb ;
    }

    public String getName() {
        return mbean.name() ;
    }

    public Map<String,?> getMeta() {
        MBeanInfo mbi = mbean.getMBeanInfo() ;
        ModelMBeanInfoSupport  mmbi = (ModelMBeanInfoSupport)mbi ;
        Descriptor desc ;
        try {
            desc = mmbi.getMBeanDescriptor();
        } catch (MBeanException ex) {
            throw new GmbalException( ex ) ;
        }
        Map<String,Object> result = new HashMap<String,Object>() ;
        for (String key : desc.getFieldNames()) {
            result.put( key, desc.getFieldValue(key)) ;
        }
        return result ;
    }

    public AMX getContainer() {
        MBeanImpl parent = mbean.parent() ;
        if (parent != null) {
            return parent.facet( AMX.class, false ) ;
        } else {
            ManagedObjectManagerInternal mom = mbean.skeleton().mom() ;
            ObjectName rpn = mom.getRootParentName() ;
            if (rpn == null) {
                return null ;
            } else {
                return new AMXClient( mom.getMBeanServer(), rpn ) ;
            }
        }
    }

    public AMX[] getContained() {
        List<AMX> children = getContained( mbean.children().keySet() ) ;
        return children.toArray( new AMX[children.size()] ) ;
    }

    private static UnaryFunction<MBeanImpl,AMX> extract =
        new UnaryFunction<MBeanImpl,AMX>() {
            @SuppressWarnings("unchecked")
            public AMX evaluate( MBeanImpl mb ) {
                return mb.facet( AMX.class, false ) ;
            }
        } ;

   private List<AMX> getContained( Set<String> types ) {
        List<AMX> result = new ArrayList<AMX>() ;
        for (String str : types ) {
            result.addAll( Arrays.asList( getContained( str ) ) ) ;
        }
        return result ;
   }

    public AMX[] getContained(String type) {
        Collection<AMX> children = Algorithms.map( mbean.children().get( type ),
            extract ).values() ;
        return children.toArray( new AMX[children.size()] ) ;
    }
}
