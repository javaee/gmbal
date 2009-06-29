/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2009 Sun Microsystems, Inc. All rights reserved.
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

package org.glassfish.probe.provider;

import java.util.Vector;

/**
 *
 * @author abbagani
 */
public class StatsProviderManager {

   private StatsProviderManager(){
   }

   
   synchronized public static boolean register(String configElement, PluginPoint pp,
                                    String subTreeRoot, Object statsProvider) {
      //Ideally want to start this in a thread, so we can reduce the startup time
      if (spmd == null) {
          //Make an entry into the toBeRegistered map
          toBeRegistered.add(
                  new StatsProviderRegistryElement(configElement, pp,
                                    subTreeRoot, statsProvider));
      } else {
          spmd.register(configElement, pp,subTreeRoot,statsProvider);
          return true;
      }
       return false;
   }


   synchronized public static void setStatsProviderManagerDelegate(
                                    StatsProviderManagerDelegate lspmd) {
      //System.out.println("in StatsProviderManager.setStatsProviderManagerDelegate ***********");
      if (lspmd == null) {
          //Should log and throw an exception
          return;
      }

      //Assign the Delegate
      spmd = lspmd;

      //System.out.println("Running through the toBeRegistered array to call register ***********");

      //First register the pending StatsProviderRegistryElements
      for (StatsProviderRegistryElement spre : toBeRegistered) {
          spmd.register(spre.configElement, spre.pp, spre.subTreeRoot,
                        spre.statsProvider);
      }

      //Now that you registered the pending calls, Clear the toBeRegistered store
      toBeRegistered.clear();
   }

   //variables
   static StatsProviderManagerDelegate spmd; // populate this during our initilaization process
   static Vector<StatsProviderRegistryElement> toBeRegistered = new Vector();
   
   private static class StatsProviderRegistryElement {
       String configElement;
       PluginPoint pp;
       String subTreeRoot;
       Object statsProvider;
       public StatsProviderRegistryElement(String configElement, PluginPoint pp,
                                    String subTreeRoot, Object statsProvider) {
           this.configElement = configElement;
           this.pp = pp;
           this.subTreeRoot = subTreeRoot;
           this.statsProvider = statsProvider;
       }
   }

}