/*
 * Copyright 2007 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

/*
 * @test
 * @summary Tests com.sun.beans.TypeResolver
 * @author Eamonn McManus
 * @author Ken Cavanaugh
 */

package org.glassfish.gmbal ;

import java.util.List;

import java.util.Map;
import org.glassfish.gmbal.typelib.EvaluatedClassDeclaration ;
import org.glassfish.gmbal.typelib.EvaluatedMethodDeclaration ;
import org.glassfish.gmbal.typelib.EvaluatedType;
import org.glassfish.gmbal.typelib.TypeEvaluator ;

public class TestTypelibDecls {
    public static EvaluatedMethodDeclaration getMethod(
        EvaluatedClassDeclaration cdecl, String name )  {

        // First check in cdecl
        for (EvaluatedMethodDeclaration mdecl : cdecl.methods() ) {
            if (mdecl.name().equals( name) ) {
                return mdecl ;
            }
        }

        // If not found, try the inherited EvaluatedClassDeclarations
        for (EvaluatedClassDeclaration ecd : cdecl.inheritance()) {
            EvaluatedMethodDeclaration emd = getMethod( ecd, name ) ;
            if (emd != null) {
                return emd ;
            }
        }

        return null ;
    }

    private static EvaluatedClassDeclaration self = 
        (EvaluatedClassDeclaration) TypeEvaluator.getEvaluatedType(
            TestTypelibDecls.class ) ;

    private static EvaluatedMethodDeclaration getMethod( String name ) {
        return getMethod( self, name ) ;
    }

    List<Integer> getListInteger() { return null ; }
    public static final EvaluatedType LIST_INTEGER =
        getMethod( "getListInteger" ).returnType() ;

    List<Object> getListObject() { return null ; }
    public static final EvaluatedType LIST_OBJECT =
        getMethod( "getListObject" ).returnType() ;

    List<String> getListString() { return null ; }
    public static final EvaluatedType LIST_STRING =
        getMethod( "getListString" ).returnType() ;

    List<List<String>> getListListString() { return null ; }
    public static final EvaluatedType LIST_LIST_STRING =
        getMethod( "getListListString" ).returnType() ;

    List<List<Object>> getListListObject() { return null ; }
    public static final EvaluatedType LIST_LIST_OBJECT =
        getMethod( "getListListObject" ).returnType() ;

    Map<Object,Object> getMapObjectObject() { return null ; }
    public static final EvaluatedType MAP_OBJECT_OBJECT =
        getMethod( "getMapObjectObject" ).returnType() ;

    Map<String,Integer> getMapStringInteger() { return null ; }
    public static final EvaluatedType MAP_STRING_INTEGER =
        getMethod( "getMapStringInteger" ).returnType() ;

}
