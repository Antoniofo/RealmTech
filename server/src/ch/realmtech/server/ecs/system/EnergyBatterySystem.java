package ch.realmtech.server.ecs.system;

import ch.realmtech.server.ServerContext;
import ch.realmtech.server.ecs.component.CellComponent;
import ch.realmtech.server.ecs.component.EnergyBatteryComponent;
import ch.realmtech.server.ecs.plugin.server.SystemsAdminServer;
import ch.realmtech.server.energy.EnergyTransportStatus;
import ch.realmtech.server.packet.clientPacket.EnergyBatterySetEnergyPacket;
import com.artemis.ComponentMapper;
import com.artemis.annotations.All;
import com.artemis.annotations.Wire;
import com.artemis.systems.IteratingSystem;
import com.badlogic.gdx.utils.IntSet;

import java.util.UUID;

@All({EnergyBatteryComponent.class, CellComponent.class})
public class EnergyBatterySystem extends IteratingSystem {
    @Wire(name = "serverContext")
    private ServerContext serverContext;
    @Wire
    private SystemsAdminServer systemsAdminServer;
    private ComponentMapper<EnergyBatteryComponent> mEnergyBattery;
    private ComponentMapper<CellComponent> mCell;

    public final IntSet dirtyEnergyBattery = new IntSet();

    @Override
    protected void process(int entityId) {
        EnergyBatteryComponent energyBatteryComponent = mEnergyBattery.get(entityId);
        if (energyBatteryComponent.isEnergyBatteryReceiver()) {
            EnergyTransportStatus energyTransportStatus = systemsAdminServer.energyManager.findEnergyToFeed(entityId);
            if (energyTransportStatus != null) {
                int batteryEmitterId = energyTransportStatus.batteryEmitterId();
                EnergyBatteryComponent energyBatteryComponentEmitter = mEnergyBattery.get(batteryEmitterId);
                energyBatteryComponentEmitter.removeStored(1);
                energyBatteryComponent.addStored(1);

                dirtyEnergyBattery.add(energyTransportStatus.batteryEmitterId());
                dirtyEnergyBattery.add(energyTransportStatus.batteryReceiverId());
            }
        }
    }

    @Override
    protected void end() {
        if (!dirtyEnergyBattery.isEmpty()) {
            IntSet.IntSetIterator intSetIterator = dirtyEnergyBattery.iterator();
            while (intSetIterator.hasNext) {
                int energyBatteryId = intSetIterator.next();
                UUID energyBatteryUuid = systemsAdminServer.uuidComponentManager.getRegisteredComponent(energyBatteryId).getUuid();

                 EnergyBatteryComponent energyBatteryComponent = mEnergyBattery.get(energyBatteryId);
                serverContext.getServerHandler().broadCastPacket(new EnergyBatterySetEnergyPacket(energyBatteryUuid, energyBatteryComponent.getStored()));
            }
            dirtyEnergyBattery.clear();
        }
    }
}
