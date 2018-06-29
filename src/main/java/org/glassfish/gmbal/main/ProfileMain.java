/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2018 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.gmbal.main;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import javax.management.ObjectName;
import org.glassfish.gmbal.Description;
import org.glassfish.gmbal.ManagedAttribute;
import org.glassfish.gmbal.ManagedData;
import org.glassfish.gmbal.ManagedObjectManagerFactory ;
import org.glassfish.gmbal.ManagedObjectManager ;
import org.glassfish.gmbal.ManagedObject ;
import org.glassfish.gmbal.NameValue;
import org.glassfish.gmbal.AMXClient;
import org.glassfish.pfl.basic.contain.Pair;

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
	    return other.name.equals( name ) ;
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
    private static final Random random = new Random() ;

    private static void initializeStores( MyRoot myroot ) {
        int i = 0 ;
	for (String str : itemNames) {
	    Item item = new Item( str, random.nextInt(100),
		random.nextInt(100) ) ;
	    myroot.addItem(item);
	    items[i++] = item ;
	}

	for (int ctr=0; ctr<NUM_STORES; ctr++ ) {
	    Store store = new Store( "Store_" + ctr ) ;
	    for (Item item : items) {
		if (random.nextInt( 100 ) < 20) {
		    store.addItem( item, 3 + random.nextInt( 11 ) );
		}
	    }
	    myroot.addStore( store );
	}
    }

    private static void checkAttributes( ManagedObjectManager mom,
	MyRoot myroot ) {

	for (Store store : myroot.getStores() ) {
	    ObjectName oname = mom.getObjectName(store) ;
 	    AMXClient amx = new AMXClient( mom.getMBeanServer(), oname ) ;

	    Object res = amx.getAttribute("Name") ;
	    if (!res.equals( store.name() )) {
	        throw new IllegalStateException( "bad store name" ) ;
	    }
	}
    }

    private static void registerMBeans( ManagedObjectManager mom, 
	MyRoot myroot ) {

	for (Store store : myroot.getStores() ) {
	    mom.registerAtRoot( store ) ;
	}
    }

    public static class Timings {
	private List<Pair<String,Long>> durations ;
	private long start ;

	public Timings() {
	    start = System.currentTimeMillis() ;
	    durations = new ArrayList<Pair<String,Long>>() ;
	}

	public void add( String msg ) {
	    long elapsed = System.currentTimeMillis() - start ;

	    Pair<String,Long> entry = new Pair<String,Long>(
		msg, elapsed ) ;
	    durations.add( entry ) ;

	    start = System.currentTimeMillis() ;
	}

	public void dump( String msg ) {
	    System.out.println( msg );
	    for (Pair<String,Long> entry : durations) {
		System.out.printf( "\t%10d:\t%s\n",
		    entry.second(), entry.first() ) ;
	    }
	}
    }

    public static void run( boolean isWarmup, int count ) throws IOException {
	Timings timings = new Timings() ;

        final MyRoot myroot = new MyRoot() ;
	initializeStores( myroot ) ;
	timings.add( "Set up the data" ) ;

        mom = ManagedObjectManagerFactory.createStandalone("test") ;
	timings.add( "Create ManagedObjectManager" ) ;

        mom.createRoot(myroot) ;
	timings.add( "Create the root" ) ;

	registerMBeans( mom, myroot ) ;
	timings.add( "Register " + NUM_STORES + " MBeans" ) ;

	checkAttributes( mom, myroot ) ;
	timings.add( "Fetch 1 attribute on " + NUM_STORES + " MBeans" ) ;

        mom.close();
	timings.add( "Close the ManagedObjectManager" ) ;

	String type = isWarmup ? "Warmup" : "Benchmark" ;

	timings.dump( type + ": Iteration " + count ) ;
    }

    private static void msg( String arg ) {
	System.out.println( arg ) ;
    }

    // Create stores so that each store has a 20% probability of carrying any
    // of the items.  Assign size and mass to each item randomly from 1-100.
    public static void main( String[] args ) throws IOException {
	msg( "Warming up" ) ;
	for (int ctr=0; ctr<10; ctr++ ) {
	    run( true, ctr ) ;
	}

	msg( "Timing" ) ;
	long start = System.currentTimeMillis() ;
	run( false, 0 ) ;
	long duration = System.currentTimeMillis() - start ;

	long numBeans = NUM_STORES + 1 ;

	msg( "It took " + duration + " milliseconds to test "
	    + numBeans + " MBeans" ) ;

	msg( "That is " + (numBeans*1000)/duration
	    + " MBean register/getAttribute/unregister calls per second" ) ;
    }
}
