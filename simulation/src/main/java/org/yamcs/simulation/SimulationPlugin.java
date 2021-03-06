package org.yamcs.simulation;

import org.yamcs.Plugin;

public class SimulationPlugin implements Plugin {

    private String version;

    public SimulationPlugin() {
        Package pkg = getClass().getPackage();
        version = (pkg != null) ? pkg.getImplementationVersion() : null;
    }

    @Override
    public String getName() {
        return "Simulation";
    }

    @Override
    public String getDescription() {
        return "All-in-one configuration example using an embedded simulator";
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public String getVendor() {
        return "Space Applications Services";
    }
}
