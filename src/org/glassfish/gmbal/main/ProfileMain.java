package org.glassfish.gmbal.main;

import java.io.IOException;
import org.glassfish.gmbal.ManagedObjectManagerFactory ;
import org.glassfish.gmbal.ManagedObjectManager ;
import org.glassfish.gmbal.ManagedObject ;
import org.glassfish.gmbal.NameValue;
import org.glassfish.gmbal.typelib.TypeEvaluator;

/**
 *
 * @author ken
 */
public class ProfileMain {
    private ManagedObjectManager mom ;

    @ManagedObject
    public static class MyRoot {
        @NameValue
        String name() {
            return "Root" ;
        }
    }

    public static void main( String[] args ) throws IOException {
        new ProfileMain() ;
    }

    public ProfileMain() throws IOException {
        final MyRoot myroot = new MyRoot() ;

        mom = ManagedObjectManagerFactory.createStandalone("test") ;
        mom.createRoot(myroot) ;
        mom.close();
        // TypeEvaluator.dumpEvalClassMap();
    }
}
