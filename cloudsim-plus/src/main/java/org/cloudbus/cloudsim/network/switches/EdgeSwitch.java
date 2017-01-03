/*
 * Title:        CloudSim Toolkit
 * Description:  CloudSim (Cloud Simulation) Toolkit for Modeling and Simulation of Clouds
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2009-2012, The University of Melbourne, Australia
 */
package org.cloudbus.cloudsim.network.switches;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.events.SimEvent;
import org.cloudbus.cloudsim.datacenters.network.NetworkDatacenter;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.hosts.network.NetworkHost;
import org.cloudbus.cloudsim.network.HostPacket;
import org.cloudbus.cloudsim.util.Conversion;
import org.cloudbus.cloudsim.vms.Vm;

/**
 * This class represents an Edge AbstractSwitch in a Datacenter network. It interacts
 * with other Datacenter in order to exchange packets.
 *
 * <br>Please refer to following publication for more details:<br>
 * <ul>
 * <li>
 * <a href="http://dx.doi.org/10.1109/UCC.2011.24">
 * Saurabh Kumar Garg and Rajkumar Buyya, NetworkCloudSim: Modelling Parallel
 * Applications in Cloud Simulations, Proceedings of the 4th IEEE/ACM
 * International Conference on Utility and Cloud Computing (UCC 2011, IEEE CS
 * Press, USA), Melbourne, Australia, December 5-7, 2011.
 * </a>
 * </ul>
 *
 * @author Saurabh Kumar Garg
 * @author Manoel Campos da Silva Filho
 *
 * @since CloudSim Toolkit 3.0
 *
 */
public class EdgeSwitch extends AbstractSwitch {
    /**
     * The level (layer) of the switch in the network topology.
     */
    public static final int LEVEL = 2;

    /**
     * Default downlink bandwidth of EdgeSwitch in Megabits/s.
     * It also represents the uplink bandwidth of connected hosts.
     */
    public static final long DOWNLINK_BW = 100 * 8;

    /**
     * Default number of ports that defines the number of
     * {@link Host} that can be connected to the switch.
     */
    public static final int PORTS = 4;

    /**
     * Default switching delay in milliseconds.
     */
    public static final double SWITCHING_DELAY = 0.00157;

    /**
     * Instantiates a EdgeSwitch specifying Datacenter that are connected to its
     * downlink and uplink ports, and corresponding bandwidths. In this switch,
     * downlink ports aren't connected to other switch but to hosts.
     *
     * @param simulation The CloudSim instance that represents the simulation the Entity is related to
     * @param dc The Datacenter where the switch is connected to
     */
    public EdgeSwitch(CloudSim simulation, NetworkDatacenter dc) {
        super(simulation, dc);

        setUplinkBandwidth(AggregateSwitch.DOWNLINK_BW);
        setDownlinkBandwidth(DOWNLINK_BW);
        setSwitchingDelay(SWITCHING_DELAY);
        setPorts(PORTS);
    }

    @Override
    protected void processPacketDown(SimEvent ev) {
        super.processPacketDown(ev);

        HostPacket netPkt = (HostPacket) ev.getData();
        Vm receiverVm = netPkt.getVmPacket().getDestination();
        // packet is to be received by host
        NetworkHost host = getVmHost(receiverVm);
        netPkt.setDestination(host);
        getPacketToHostMap().putIfAbsent(host, new ArrayList<>());
        getPacketToHostMap().get(host).add(netPkt);
    }

    @Override
    protected void processPacketUp(SimEvent ev) {
        super.processPacketUp(ev);

        HostPacket hostPkt = (HostPacket) ev.getData();
        Vm receiverVm = hostPkt.getVmPacket().getDestination();

        // packet is received from host
        // packet is to be sent to aggregate level or to another host in the same level
        NetworkHost host = getVmHost(receiverVm);
        hostPkt.setDestination(host);

        // packet needs to go to a host which is connected directly to switch
        if (!Objects.isNull(host)) {
            addPacketToBeSentToHost(host, hostPkt);
            return;
        }

        // otherwise, packet is to be sent to upper switch
        /**
         * @todo ASSUMPTION: EACH EDGE is connected to one aggregate level switch.
         * If there are more than one Aggregate level switch, the following code has to be modified.
        */
        Switch sw = getUplinkSwitches().get(0);
        addPacketToBeSentToUplinkSwitch(sw, hostPkt);
    }

    @Override
    public int getLevel() {
        return LEVEL;
    }

}