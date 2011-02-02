package org.erlide.cover.core;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.erlide.core.erlang.ErlModelException;
import org.erlide.core.erlang.ErlangCore;
import org.erlide.core.erlang.IErlProject;
import org.erlide.cover.runtime.launch.CoverLaunchData;
import org.erlide.cover.runtime.launch.FrameworkType;
import org.erlide.cover.runtime.launch.LaunchType;

/**
 * Settings for performing coverage.
 * 
 * @author Aleksandra Lipiec <aleksandra.lipiec@erlang.solutions.com>
 *
 */
public class CoverSettings {
    
    private LaunchType type;
    private List<CoverObject> objs;
    private FrameworkType frameworkType;

    /**
     * Create coverage settings, depend mainly on launch type
     * @param t
     * @param data
     */
    public CoverSettings(LaunchType t, CoverLaunchData data) {
        objs = new LinkedList<CoverObject>();
        type = t;
        frameworkType = data.getFramework();
        
        if(data == null)
            return;
        
        switch(t) {
        case MODULE: 
            IProject p = ResourcesPlugin.getWorkspace().getRoot()
                .getProject(data.getProject());
            String path = p.getLocation().toString() + "/src/" + data.getModule();
            //TODO: better mechanizm for finding file paths within project
            String pathEbin = p.getLocation().toString() + "/ebin";
            objs.add(new CoverObject(CoverObject.MODULE, 
                    data.getModule().replace(".erl", ""), path, path, pathEbin));
            break;
        case ALL:
            System.out.println("all: " + data.getFile());
            StringBuffer bf = new StringBuffer(ResourcesPlugin.getWorkspace().
                    getRoot().getRawLocation().toString());
            bf.append("/").append(data.getFile()).append("/");
            
            System.out.println(bf);
            String pathSrc = bf.toString() + "src";
            String pathTst = bf.toString() + "test";
            pathEbin = bf.toString() + "ebin";
            objs.add(new CoverObject(CoverObject.PROJ, pathSrc, pathTst, pathEbin));
            break;
        case APPLICATION:
            //TODO: finding application - should be simmilar to finding module
            break;
        case CUSTOM:
            //TODO: custom settings - list of files(?), tests(?)
            break;            
        }
    }
    
    public LaunchType getType() {
        return type;
    }
    
    public String getTypeAsString() {
        return type.name().toLowerCase();
    }

    public List<CoverObject> objects() {
        return objs;
    }
        
    public String getFramework() {
        return frameworkType.name().toLowerCase();
    }
    

}
