/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.glassfish.probe.provider;

/**
 *
 * @author abbagani
 */
public enum PluginPoint {

    SERVER ("server", "server"),
    APPLICATIONS ("applications", "server/applications");

    String name;
    String path;

    PluginPoint(String lname, String lpath) {
        name = lname;
        path = lpath;
    }

    public String getName() {
        return name;
    }
    
    public String getPath() {
        return path;
    }
}
