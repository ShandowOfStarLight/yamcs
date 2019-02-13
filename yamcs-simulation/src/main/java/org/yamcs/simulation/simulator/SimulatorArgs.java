package org.yamcs.simulation.simulator;

import com.beust.jcommander.Parameter;

public class SimulatorArgs {

    @Parameter(names = "--telnet-port")
    public int telnetPort = 10023;

    @Parameter(names = "--tc-port")
    public Integer tcPort = 10025;
    
    @Parameter(names = "--tm-port")
    public Integer tmPort = 10015;
   
    @Parameter(names = "--tm2-port")
    public Integer tm2Port = 10016;
   
    @Parameter(names = "--los-port")
    public int losPort = 10115;

    @Parameter(names = "--tm-frame-type", description = "which frame type to send: TM, AOS or USLP")
    public String tmFrameType = "AOS";
    
    
    @Parameter(names = "--tm-frame-host", description = "the UDP host where to send TM/AOS/USLP frames")
    public String tmFrameHost = "localhost";
    
    @Parameter(names = "--tm-frame-port", description = "the UDP port where to send TM/AOS/USLP frames")
    public int tmFramePort = 10017;

    @Parameter(names = "--tm-frame-size", description = "the TM/AOS/USLP frame size (set to 0 to disable the frame functionality)")
    public int tmFrameSize = 0;
    
    @Parameter(names = "--tm-frame-freq", description = "the number of TM frames to send per second")
    public double tmFrameFreq = 10;
    
    
    @Parameter(names = "--perf-np", description = "performance test: number of packets. Set to 0 to disable sending the performance packets")
    public int perfNp = 0;
    
    @Parameter(names = "--perf-ps", description = "performance test: packet size")
    public int perfPs = 1400;
    
    @Parameter(names = "--perf-ms", description = "performance test: interval in between packets in milliseconds")
    public long perfMs = 100l;

}
