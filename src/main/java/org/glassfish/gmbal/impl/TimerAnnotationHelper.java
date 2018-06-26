/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2018 Oracle and/or its affiliates. All rights reserved.
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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.glassfish.gmbal.impl;

import java.util.Map;
import java.util.Set;
import org.glassfish.gmbal.Description;
import org.glassfish.gmbal.IncludeSubclass;
import org.glassfish.gmbal.InheritedAttribute;
import org.glassfish.gmbal.ManagedAttribute;
import org.glassfish.gmbal.ManagedData;
import org.glassfish.gmbal.ManagedObject;
import org.glassfish.gmbal.ManagedObjectManager;
import org.glassfish.gmbal.ManagedOperation;
import org.glassfish.gmbal.NameValue;
import org.glassfish.pfl.tf.timer.impl.TimerFactoryImpl;
import org.glassfish.pfl.tf.timer.spi.Controllable;
import org.glassfish.pfl.tf.timer.spi.LogEventHandler;
import org.glassfish.pfl.tf.timer.spi.Named;
import org.glassfish.pfl.tf.timer.spi.Statistics;
import org.glassfish.pfl.tf.timer.spi.StatsEventHandler;
import org.glassfish.pfl.tf.timer.spi.Timer;
import org.glassfish.pfl.tf.timer.spi.TimerEventController;
import org.glassfish.pfl.tf.timer.spi.TimerEventControllerBase;
import org.glassfish.pfl.tf.timer.spi.TimerEventHandler;
import org.glassfish.pfl.tf.timer.spi.TimerFactory;
import org.glassfish.pfl.tf.timer.spi.TimerGroup;

/** This class provides nested classes that carry the needed Gmbal annotations
 * for the various Timer classes and interface, and a method for registering all
 * of these annotations in gmbal.
 *
 * @author ken_admin
 */
public class TimerAnnotationHelper {
    public interface ManagedNamed extends Named {
        @ManagedAttribute
        @Description( "TimerFactory that created this Timer or TimerGroup" )
        @Override
        TimerFactory factory() ;

        @ManagedAttribute
        @NameValue
        @Description( "Name of this Timer or TimerGroup" )
        @Override
        String name() ;
    }

    @ManagedObject
    @IncludeSubclass( { Timer.class, TimerGroup.class, TimerFactory.class } )
    public interface ManagedControllable extends Controllable, ManagedNamed {
        @ManagedAttribute
        @Description( "The purpose of the Timer or TimerGroup" )
        @Override
        String description() ;

        @ManagedAttribute
        @Description( "An internal identifier for the Timer or TimerGroup" )
        @Override
        int id() ;

        @ManagedAttribute
        @Description( "Set of Timers or TimerGroups contained in a TimerGroup" )
        @Override
        Set<? extends Controllable> contents() ;

        @ManagedOperation
        @Description( "Enable this Timer, or all Timers and TimerGroups "
            + "contained in this TimerGroup" )
        @Override
        void enable() ;

        @ManagedOperation
        @Description( "Disable this Timer, or all Timers and TimerGroups "
            + "contained in this TimerGroup" )
        @Override
        void disable() ;

        @ManagedOperation
        @Description( "True if this Timer or TimerGroup is enabled" )
        @Override
        boolean isEnabled() ;
    }

    @ManagedData
    @Description( "Statistics recorded for a series of time intervals" )
    public class ManagedStatistics extends Statistics {
        public ManagedStatistics( long count, double min, double max,
            double average, double standardDeviation ) {
            super( count, min, max, average, standardDeviation ) ;
        }

        @ManagedAttribute
        @Description( "Total number of intervals recorded" )
        @Override
        public long count() { return super.count() ; }

        @ManagedAttribute
        @Description( "Minimum interval duration recorded" )
        @Override
        public double min() { return super.min() ; }

        @ManagedAttribute
        @Description( "Maximum interval duration recorded" )
        @Override
        public double max() { return super.max() ; }

        @ManagedAttribute
        @Description( "Average interval duration recorded" )
        @Override
        public double average() { return super.average() ; }

        @ManagedAttribute
        @Description( "Standard deviation of all durations recorded" )
        @Override
        public double standardDeviation() { return super.standardDeviation() ; }
    }

    public interface ManagedTimerEventHandler extends TimerEventHandler,
        ManagedNamed {
    }

    @ManagedObject
    @Description( "TimerEventHandler that records all TimerEvents in a log" )
    @InheritedAttribute( methodName="iterator",
        description="TimerEvents contained in this log in order of occurrence" )
    public interface ManagedLogEventHandler extends LogEventHandler,
        ManagedTimerEventHandler {
        @ManagedOperation
        @Description( "Discard all recorded timer events" )
        @Override
        void clear() ;
    }

    @ManagedObject
    @Description( "TimerEventHandler that accumulates statistics on events" )
    public interface ManagedStatsEventHandler extends StatsEventHandler,
        ManagedTimerEventHandler {
        @ManagedAttribute
        @Description( "A table giving statistics for each activated Timer "
            + "that had at least one TimerEvent" )
        @Override
        Map<Timer,Statistics> stats() ;

        @ManagedOperation
        @Description( "Discard all statistics on all Timers" )
        @Override
        void clear() ;
    }

    @ManagedObject
    @Description( "A simple TimerEventHandler that just displays TimerEvents "
        + "as they occur" )
    public class TracingEventHandler
	extends TimerFactoryImpl.TracingEventHandler
	implements ManagedTimerEventHandler {

	public TracingEventHandler( TimerFactory factory, String name ) {
	    super( factory, name ) ;
	}
    }


    @ManagedObject
    @Description( "A timer represents a particular action that has a "
        + "duration from ENTER to EXIT" )
    public interface ManagedTimer extends Timer, ManagedControllable {
        @ManagedAttribute
        @Description( "True if this Timer is enabled, and can generate "
            + "TimerEvents" )
        @Override
        boolean isActivated() ;
    }

    @ManagedObject
    @Description( "Controls entering and exiting Timers" )
    public class ManagedTimerEventController extends TimerEventController
        implements ManagedNamed {

        public ManagedTimerEventController( TimerFactory factory, String name ) {
            super( factory, name ) ;
        }

        @ManagedOperation
        @Description( "Enter a particular Timer" )
            @Override
        public void enter( Timer timer ) {
            super.enter( timer ) ;
        }

        @ManagedOperation
        @Description( "Exit a particular Timer" )
            @Override
        public void exit( Timer timer ) {
            super.exit( timer ) ;
        }
    }

    @ManagedObject
    @Description( "A group of Timers or other TimerGroups, "
        + "which may be enabled or disabled together" )
    public interface ManagedTimerGroup extends TimerGroup, ManagedControllable {
        @ManagedOperation
        @Description( "Add a new Timer or TimerGroup to this TimerGroup" )
        @Override
        boolean add( Controllable con ) ;

        @ManagedOperation
        @Description( "Remove a new Timer or TimerGroup from this TimerGroup" )
        @Override
        boolean remove( Controllable con ) ;
    }

    @ManagedObject
    @Description( "The Factory used to create and managed all objects "
        + "in the Timer framework" )
    public interface ManagedTimerFactory extends TimerFactory, ManagedTimerGroup {
        @ManagedAttribute
        @Description( "The total number of Controllabled IDs in use" )
        @Override
        int numberOfIds() ;

        @ManagedOperation
        @Description( "Look up a Timer or TimerGroup by its ID" )
        @Override
        Controllable getControllable( int id ) ;

        @ManagedOperation
        @Description( "Create a new LogEventHandler" )
        @Override
        LogEventHandler makeLogEventHandler( String name ) ;

        @ManagedOperation
        @Description( "Create a new TracingEventHandler" )
        @Override
        TimerEventHandler makeTracingEventHandler( String name ) ;

        @ManagedOperation
        @Description( "Create a new StatsEventHandler" )
        @Override
        StatsEventHandler makeStatsEventHandler( String name ) ;

        @ManagedOperation
        @Description( "Create a new Multi-Threaded StatsEventHandler" )
        @Override
        StatsEventHandler makeMultiThreadedStatsEventHandler( String name ) ;

        @ManagedOperation
        @Description( "Remove the TimerEventHandler from this factory" )
        @Override
        void removeTimerEventHandler( TimerEventHandler handler ) ;

        @ManagedOperation
        @Description( "Create a new Timer" )
        @Override
        Timer makeTimer( String name, String description )  ;

        @ManagedAttribute
        @Description( "All timers contained in this factory" )
        @Override
        Map<String,? extends Timer> timers() ;

        @ManagedOperation
        @Description( "Create a new TimerGroup" )
        @Override
        TimerGroup makeTimerGroup( String name, String description ) ;

        @ManagedAttribute
        @Description( "All timers contained in this factory" )
        @Override
        Map<String,? extends TimerGroup> timerGroups() ;

        @ManagedOperation
        @Description( "Create a new TimerEventController" )
        @Override
        TimerEventController makeController( String name ) ;

        @ManagedOperation
        @Description( "Remote the TimerEventController from this factory" )
        @Override
        void removeController( TimerEventControllerBase controller ) ;

        @ManagedAttribute
        @Description( "All explicitly enabled Timers and TimerGroups" )
        @Override
        Set<? extends Controllable> enabledSet() ;

        @ManagedAttribute
        @Description( "All activated Timers" )
        @Override
        Set<Timer> activeSet() ;
    }

    public static void registerTimerClasses( ManagedObjectManager mom ) {
        for (Class<?> cls : TimerAnnotationHelper.class.getDeclaredClasses()) {
            mom.addInheritedAnnotations(cls);
        }
    }
}
