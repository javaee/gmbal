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
package org.glassfish.gmbal.main;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.glassfish.gmbal.Description;
import org.glassfish.gmbal.ManagedAttribute;
import org.glassfish.gmbal.ManagedData;
import org.glassfish.gmbal.ManagedObjectManagerFactory ;
import org.glassfish.gmbal.ManagedObjectManager ;
import org.glassfish.gmbal.ManagedObject ;
import org.glassfish.gmbal.NameValue;

/**
 *
 * @author ken
 */
public class ProfileMain {
    private static ManagedObjectManager mom ;

    @ManagedData
    public static class Item {
	@ManagedAttribute
	private String name ;
	private double size ;
	private double mass ;

	@Override
	public int hashCode() {
	    return name.hashCode() ;
	}

	@Override
	public boolean equals(Object obj) {
	    if (obj == null) {
		return false;
	    }
	    if (getClass() != obj.getClass()) {
		return false;
	    }
	    final Item other = (Item) obj;
	    return true;
        }

        public Item( String name, double size, double mass ) {
	    this.name = name ;
	    this.size = size ;
	    this.mass = mass ;
	}

	@Description( "The name of this entity")
	@NameValue()
	String name() {
	    return name ;
	}

	@ManagedAttribute()
	@Description( "The size of this entity")
	double size() {
	    return size ;
	}

	@ManagedAttribute()
	@Description( "The mass of this entity" )
	double mass() {
	    return mass ;
	}
    }

    @ManagedObject 
    @Description( "A store of items" )
    public static class Store {
	private String name ;
	Map<Item,Integer> contents = new HashMap<Item,Integer>() ;

	public Store( String name ) {
	    this.name = name ;
	}

	public void addItem( Item item, int count ) {
	    Integer num = contents.get( item ) ;
	    if (num == null) {
		contents.put( item, count ) ;
	    } else {
		contents.put( item, num + count ) ;
	    }
	}

	@Override
	public boolean equals(Object obj) {
	    if (obj == null) {
		return false;
	    }
	    if (getClass() != obj.getClass()) {
		return false;
	    }
	    final Store other = (Store) obj;
	    if ((this.name == null) ? (other.name != null)
	        : !this.name.equals(other.name)) {
		return false;
	    }
	    return true;
	}
	
	@Override
	public int hashCode() {
	    return name.hashCode() ;
	}

        @ManagedAttribute
	@Description( "The number of items in the store" )
	Map<Item,Integer> inventory() {
	    return null ;
	}

	@NameValue
	@ManagedAttribute
	@Description( "The name of the store")
	String name() {
	    return name ;
	}
    }

    @ManagedObject
    public static class MyRoot {
	private List<Store> stores = new ArrayList<Store>() ;
	private List<Item> items = new ArrayList<Item>() ;

	public void addStore( Store store ) {
	    stores.add( store ) ;
	}

	public void addItem( Item item ) {
	    items.add( item ) ;
	}

        @NameValue
        String name() {
            return "Root" ;
        }

	@ManagedAttribute
	@Description( "All of the stores")
	List<Store> getStores() {
	    return stores ;
	}

	@ManagedAttribute
	@Description( "All possible items")
	List<Item> getItems() {
	    return items ;
	}

	@ManagedAttribute
	@Description( "Map of which stores contain a particular item")
	Map<Item,Set<Store>> getItemLocations() {
	    Map<Item,Set<Store>> result = new HashMap<Item,Set<Store>>() ;
	    for (Store store: stores) {
		for (Map.Entry<Item,Integer> ic : store.inventory().entrySet()) {
		    if (ic.getValue() > 0) {
			Set<Store> containers = result.get( ic.getKey() ) ;
			if (containers == null) {
			    containers = new HashSet<Store>() ;
			    result.put( ic.getKey(), containers ) ;
			}

			containers.add( store ) ;
		    }
		}
	    }

	    return result ;
	}
    }

    private static final String[] itemNames = {
	"RedBall", "BlueBall", "GreenBall",
	"RubberDuck", "RubberChicken", "4_inch_telescope", "8_inch_telescope",
	"Pipette", "Flask", "500ml_beaker", "1000ml_beaker",
	"WallClock", "Radio", "32GB_USB_Stick", "500GB_SATA_Drive"
    } ;

    private static final Item[] items = new Item[ itemNames.length ] ;

    // Just name stores "Store_XXX"
    private static final int NUM_STORES = 5000 ;

    private static void initializeStores( MyRoot myroot ) {
        int i = 0 ;
	for (String str : itemNames) {
	    Item item = new Item( str, 100*Math.random(),
		100*Math.random() ) ;
	    myroot.addItem(item);
	    items[i++] = item ;
	}

	for (int ctr=0; ctr<NUM_STORES; ctr++ ) {
	    Store store = new Store( "Store_" + ctr ) ;
	    for (Item item : items) {
		if (100*Math.random() < 20) {
		    store.addItem( item, 3 + (int)(11 * Math.random() ));
		}
	    }
	    myroot.addStore( store );
	}
    }

    private static void registerMBeans( ManagedObjectManager mom, 
	MyRoot myroot ) {

	for (Store store : myroot.getStores() ) {
	    mom.registerAtRoot( store ) ;
	}
    }

    // Create stores so that each store has a 20% probability of carrying any
    // of the items.  Assign size and mass to each item randomly from 1-100.
    public static void main( String[] args ) throws IOException {
        run() ;
    }

    public static void run() throws IOException {
        final MyRoot myroot = new MyRoot() ;
	initializeStores( myroot ) ;

        mom = ManagedObjectManagerFactory.createStandalone("test") ;
        mom.createRoot(myroot) ;
	registerMBeans( mom, myroot ) ;
        mom.close();
        // TypeEvaluator.dumpEvalClassMap();
    }
}
