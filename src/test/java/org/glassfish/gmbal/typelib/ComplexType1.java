/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2018 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
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

package org.glassfish.gmbal.typelib;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author ken
 */
@ForceTypelibError
public class ComplexType1 {
    public static class CT2 extends ArrayList<ComplexType1> {
        public static class CT3 extends ArrayList<CT2> {
            public static class CT4 extends ArrayList<CT3> {
                public static class CT5 extends ArrayList<CT4> {
                    public static class CT6 extends ArrayList<CT5> {
                        public static class CT7 extends ArrayList<CT6> {
                            public static class CT8 extends ArrayList<CT7> {
                                public static class CT9 extends ArrayList<CT8> {
                                    public static class CT10 extends ArrayList<CT9> {
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public static class CT11 extends CT2.CT3.
        CT4.CT5.CT6.CT7.CT8.CT9.CT10 {

        public static class CT12 extends ArrayList<CT11> {
            public static class CT13 extends ArrayList<CT12> {
                public static class CT14 extends ArrayList<CT13> {
                    public static class CT15 extends ArrayList<CT14> {
                        public static class CT16 extends ArrayList<CT15> {
                            public static class CT17 extends ArrayList<CT16> {
                                public static class CT18 extends ArrayList<CT17> {
                                    public static class CT19 extends ArrayList<CT18> {
                                        public static class CT20 extends ArrayList<CT19> {
                                            @ForceTypelibError
                                            CT20 m1() {
                                                return null ;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public static class CT21 extends CT11.CT12.CT13.
        CT14.CT15.CT16.CT17.CT18.CT19.CT20 {
    }
}
