/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.glassfish.gmbal.logex;

import java.util.logging.Level;

/** Enum corresponding to java.util.logging.Level that can be used in annotations.
 *
 * @author ken
 */
public enum LogLevel {
    CONFIG() {
        @Override
        public Level getLevel() { return Level.CONFIG ; }
    },

    FINE {
        @Override
        public Level getLevel() { return Level.FINE ; }
    },

    FINER {
        @Override
        public Level getLevel() { return Level.FINER ; }
    },

    FINEST {
        @Override
        public Level getLevel() { return Level.FINEST ; }
    },

    INFO {
        @Override
        public Level getLevel() { return Level.INFO ; }
    },

    SEVERE {
        @Override
        public Level getLevel() { return Level.SEVERE ; }
    },

    WARNING {
        public Level getLevel() { return Level.WARNING ; }
    } ;

    public abstract Level getLevel() ;
}
