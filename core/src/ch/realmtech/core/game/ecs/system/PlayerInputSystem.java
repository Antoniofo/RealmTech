package ch.realmtech.core.game.ecs.system;

import ch.realmtech.core.RealmTech;
import ch.realmtech.core.game.ecs.plugin.SystemsAdminClient;
import ch.realmtech.server.ecs.component.CellComponent;
import ch.realmtech.server.ecs.component.InfMapComponent;
import ch.realmtech.server.ecs.component.ItemComponent;
import ch.realmtech.server.ecs.system.MapManager;
import ch.realmtech.server.packet.serverPacket.ItemToCellPlaceRequestPacket;
import com.artemis.BaseSystem;
import com.artemis.ComponentMapper;
import com.artemis.annotations.Wire;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Vector2;

import java.util.UUID;

public class PlayerInputSystem extends BaseSystem {
    @Wire(name = "context")
    private RealmTech context;
    @Wire
    private SystemsAdminClient systemsAdminClient;
    private ComponentMapper<CellComponent> mCell;
    private ComponentMapper<ItemComponent> mItem;

    @Override
    protected void processSystem() {
        Vector2 screenCoordinate = new Vector2(Gdx.input.getX(), Gdx.input.getY());
        Vector2 gameCoordinate = context.getEcsEngine().getGameCoordinate(screenCoordinate);
        int worldPosX = MapManager.getWorldPos(gameCoordinate.x);
        int worldPosY = MapManager.getWorldPos(gameCoordinate.y);

        InfMapComponent infMapComponent = context.getEcsEngine().getMapEntity().getComponent(InfMapComponent.class);
        int chunk = systemsAdminClient.mapManager.getChunk(MapManager.getChunkPos(worldPosX), MapManager.getChunkPos(worldPosY), infMapComponent.infChunks);
        if (chunk == -1) return;
        byte innerChunkX = MapManager.getInnerChunk(worldPosX);
        byte innerChunkY = MapManager.getInnerChunk(worldPosY);
        int topCell = systemsAdminClient.mapManager.getTopCell(chunk, innerChunkX, innerChunkY);
        if (Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
            systemsAdminClient.mapManager.addCellBeingMine(topCell);
        } else if (Gdx.input.isButtonPressed(Input.Buttons.RIGHT)) {
            CellComponent cellComponent = mCell.get(topCell);
            // interagie avec clique droite
            if (cellComponent.cellRegisterEntry.getCellBehavior().getInteragieClickDroit() != null) {
                cellComponent.cellRegisterEntry.getCellBehavior().getInteragieClickDroit().accept(context, topCell);
            } else {
                // place un bloc
                int selectItem = systemsAdminClient.getItemBarManager().getSelectItem();
                if (selectItem != 0) {
                    UUID itemUuid = systemsAdminClient.uuidComponentManager.getRegisteredComponent(selectItem).getUuid();
                    context.getConnexionHandler().sendAndFlushPacketToServer(new ItemToCellPlaceRequestPacket(itemUuid, worldPosX, worldPosY));
                }
            }
        }
    }
}
