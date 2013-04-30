/* 
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 *  Copyright (c) 2007-2010 Oracle and/or its affiliates. All rights reserved.
 *  
 *  The contents of this file are subject to the terms of either the GNU
 *  General Public License Version 2 only ("GPL") or the Common Development
 *  and Distribution License("CDDL") (collectively, the "License").  You
 *  may not use this file except in compliance with the License.  You can
 *  obtain a copy of the License at
 *  https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 *  or packager/legal/LICENSE.txt.  See the License for the specific
 *  language governing permissions and limitations under the License.
 *  
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License file at glassfish/bootstrap/legal/LICENSE.txt.
 *  
 *  GPL Classpath Exception:
 *  Oracle designates this particular file as subject to the "Classpath"
 *  exception as provided by Oracle in the GPL Version 2 section of the License
 *  file that accompanied this code.
 *  
 *  Modifications:
 *  If applicable, add the following below the License Header, with the fields
 *  enclosed by brackets [] replaced by your own identifying information:
 *  "Portions Copyright [year] [name of copyright owner]"
 *  
 *  Contributor(s):
 *  If you wish your version of this file to be governed by only the CDDL or
 *  only the GPL Version 2, indicate your decision by adding "[Contributor]
 *  elects to include this software in this distribution under the [CDDL or GPL
 *  Version 2] license."  If you don't indicate a single choice of license, a
 *  recipient has the option to distribute your version of this file under
 *  either the CDDL, the GPL Version 2 or to extend the choice of license to
 *  its licensees as provided above.  However, if you add GPL Version 2 code
 *  and therefore, elected the GPL Version 2 license, then the option applies
 *  only if the new code is made subject to such option by the copyright
 *  holder.
 */ 

package org.glassfish.gmbal.impl ;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import javax.management.Descriptor;
import javax.management.modelmbean.DescriptorSupport;

public class DescriptorUtility {
    private DescriptorUtility() {}

    public static final Descriptor EMPTY_DESCRIPTOR =
        makeDescriptor( new HashMap<String,Object>() );

    public static Descriptor makeDescriptor( Map<String, ?> fields ) {
        if (fields == null) {
            throw Exceptions.self.nullMap() ;
        }
        SortedMap<String, Object> map =
            new TreeMap<String, Object>(String.CASE_INSENSITIVE_ORDER);
        for (Map.Entry<String, ?> entry : fields.entrySet()) {
            String name = entry.getKey();
            if (name == null || name.equals("")) {
                throw Exceptions.self.badFieldName() ;
            }
            if (map.containsKey(name)) {
                throw Exceptions.self.duplicateFieldName( name ) ;
            }
            map.put(name, entry.getValue());
        }
        int size = map.size();
        String[] names = map.keySet().toArray(new String[size]);
        Object[] values = map.values().toArray(new Object[size]);
        return new DescriptorSupport( names, values ) ;
    }

    // If descriptors contain the same names, later descriptors in the
    // sequence override the earlier ones.
    public static Descriptor union(Descriptor... descriptors) {
        Map<String, Object> map =
            new TreeMap<String, Object>(String.CASE_INSENSITIVE_ORDER);
        for (Descriptor d : descriptors) {
            if (d != null) {
                String[] names = d.getFieldNames();
                for (String n : names) {
                    Object v = d.getFieldValue(n);
                    map.put(n, v);
                }
            }
        }

        return makeDescriptor(map);
    }
}
