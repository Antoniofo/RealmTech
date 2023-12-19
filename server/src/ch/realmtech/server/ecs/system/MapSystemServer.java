package ch.realmtech.server.ecs.system;

import ch.realmtech.server.ServerContext;
import ch.realmtech.server.divers.Position;
import ch.realmtech.server.ecs.component.*;
import ch.realmtech.server.ecs.plugin.server.SystemsAdminServer;
import ch.realmtech.server.level.cell.CellManager;
import ch.realmtech.server.level.cell.Cells;
import ch.realmtech.server.packet.clientPacket.CellBreakPacket;
import ch.realmtech.server.packet.clientPacket.ChunkAMonterPacket;
import ch.realmtech.server.packet.clientPacket.ChunkAReplacePacket;
import ch.realmtech.server.registery.CellRegisterEntry;
import ch.realmtech.server.registery.ItemRegisterEntry;
import ch.realmtech.server.serialize.cell.CellArgs;
import ch.realmtech.server.serialize.exception.IllegalMagicNumbers;
import com.artemis.ComponentMapper;
import com.artemis.annotations.All;
import com.artemis.annotations.Wire;
import com.artemis.systems.IteratingSystem;
import com.badlogic.gdx.utils.Null;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipException;

@All(PlayerConnexionComponent.class)
public class MapSystemServer extends IteratingSystem implements CellManager {
    private final static Logger logger = LoggerFactory.getLogger(MapSystemServer.class);
    @Wire(name = "serverContext")
    private ServerContext serverContext;
    @Wire
    private SystemsAdminServer systemsAdminServer;

    private ComponentMapper<InfMapComponent> mInfMap;
    private ComponentMapper<InfChunkComponent> mChunk;
    private ComponentMapper<PositionComponent> mPosition;
    private ComponentMapper<ItemComponent> mItem;
    private ComponentMapper<PlayerConnexionComponent> mPlayerConnexion;
    private InfMapComponent infMapComponent;
    private SaveMetadataComponent infMetaDonnesComponent;
    int renderDistance;

    public void initMap() {
        infMapComponent = mInfMap.get(systemsAdminServer.tagManager.getEntityId("infMap"));
        infMetaDonnesComponent = infMapComponent.getMetaDonnesComponent(world);
    }

    @Override
    protected void begin() {
        renderDistance = serverContext.getOptionServer().renderDistance.get();
    }

    @Override
    protected void process(int playerId) {
        PositionComponent positionPlayerComponent = mPosition.get(playerId);
        if (positionPlayerComponent == null) return;
        PlayerConnexionComponent playerConnexionComponent = mPlayerConnexion.get(playerId);
        int chunkPosX = MapManager.getChunkPos((int) positionPlayerComponent.x);
        int chunkPosY = MapManager.getChunkPos((int) positionPlayerComponent.y);
        if (playerConnexionComponent.ancienChunkPos == null || !(playerConnexionComponent.ancienChunkPos[0] == chunkPosX && playerConnexionComponent.ancienChunkPos[1] == chunkPosY)) {
            List<Position> chunkADamnerPos = trouveChunkADamner(playerConnexionComponent.chunkPoss, chunkPosX, chunkPosY, renderDistance);

            int indexDamner = 0;
            for (int i = -renderDistance + chunkPosX; i <= renderDistance + chunkPosX; i++) {
                for (int j = -renderDistance + chunkPosY; j <= renderDistance + chunkPosY; j++) {
                    boolean changement = chunkSansChangement(playerConnexionComponent.chunkPoss, i, j);
                    if (changement) {
                        int newChunkId = getCacheOrGenerateChunk(infMapComponent, infMetaDonnesComponent, i, j);
                        InfChunkComponent infChunkComponent = mChunk.get(newChunkId);
                        int newChunkPosX = infChunkComponent.chunkPosX;
                        int newChunkPosY = infChunkComponent.chunkPosY;
                        if (indexDamner < chunkADamnerPos.size()) {
                            Position chunkPos = chunkADamnerPos.get(indexDamner++);
                            int oldChunk = systemsAdminServer.mapManager.getChunk(chunkPos.x(), chunkPos.y(), infMapComponent.infChunks);
                            InfChunkComponent infChunkComponentOld = mChunk.get(oldChunk);
                            int oldChunkPosX = infChunkComponentOld.chunkPosX;
                            int oldChunkPosy = infChunkComponentOld.chunkPosY;
                            serverContext.getServerHandler().sendPacketTo(new ChunkAReplacePacket(
                                    newChunkPosX,
                                    newChunkPosY,
                                    serverContext.getSerializerController().getChunkSerializerController().encode(infChunkComponent),
                                    oldChunkPosX,
                                    oldChunkPosy
                            ), playerConnexionComponent.channel);
                            Position.replace(playerConnexionComponent.chunkPoss, oldChunkPosX, oldChunkPosy, newChunkPosX, newChunkPosY);
                            // world.getSystem(MapManager.class).replaceChunk(infMapComponent.infChunks, oldChunk, newChunkId);
                            infMapComponent.infChunks = damneChunkServer(infMapComponent.infChunks, oldChunk, infMetaDonnesComponent);
                            infMapComponent.infChunks = systemsAdminServer.mapManager.ajouterChunkAMap(infMapComponent.infChunks, newChunkId);
                        } else {
                            serverContext.getServerHandler().sendPacketTo(new ChunkAMonterPacket(
                                    serverContext.getSerializerController().getChunkSerializerController().encode(infChunkComponent)
                            ), playerConnexionComponent.channel);
                            playerConnexionComponent.chunkPoss.add(new Position(newChunkPosX, newChunkPosY));
                            infMapComponent.infChunks = systemsAdminServer.mapManager.ajouterChunkAMap(infMapComponent.infChunks, newChunkId);
                        }
                    }
                }
                if (playerConnexionComponent.ancienChunkPos == null) {
                    playerConnexionComponent.ancienChunkPos = new int[2];
                }
                playerConnexionComponent.ancienChunkPos[0] = chunkPosX;
                playerConnexionComponent.ancienChunkPos[1] = chunkPosY;
                // la limite d'update de chunk pour ce process est atteint
//                    if (indexDamner >= dataCtrl.option.chunkParUpdate.get()) {
//                        break stop;
//                    }
            }
        }
//            if (indexDamner < chunkADamner.size()) {
//                for (int i = indexDamner; i < chunkADamner.size(); i++) {
//                    final int chunkId = chunkADamner.get(i);
//                    damneChunk(chunkId, infMetaDonnesComponent);
//                    playerConnexionComponent.infChunks = world.getSystem(MapManager.class).supprimerChunkAMap(playerConnexionComponent.infChunks, chunkId);
//                }
//            }
    }

    private boolean chunkSansChangement(List<Position> chunkPoss, int i, int j) {
        boolean trouve = false;
        for (Position position : chunkPoss) {
            if (position.x() == i && position.y() == j) {
                trouve = true;
                break;
            }
        }
        return !trouve;
    }

    public List<Position> trouveChunkADamner(List<Position> poss, int chunkPosX, int chunkPosY, int renderDistance) {
        List<Position> ret = new ArrayList<>(2 * renderDistance + 1);
        for (Position position : poss) {
            if (!chunkEstDansLaRenderDistance(position, chunkPosX, chunkPosY, renderDistance)) {
                ret.add(position);
            }
        }
        return ret;
    }

    public int[] damneChunkServer(int[] infChunks, int chunkId, SaveMetadataComponent saveMetadataComponent) {
        try {
            systemsAdminServer.saveInfManager.saveInfChunk(chunkId, SaveInfManager.getSavePath(saveMetadataComponent.saveName));
            systemsAdminServer.mapManager.supprimeChunk(chunkId);
        } catch (IOException e) {
            InfChunkComponent infChunkComponent = mChunk.get(chunkId);
            logger.error("Le chunk {},{} n'a pas été sauvegardé correctement", infChunkComponent.chunkPosX, infChunkComponent.chunkPosY);
        }
        return systemsAdminServer.mapManager.supprimerChunkAMap(infChunks, chunkId);
    }

    public boolean chunkEstDansLaRenderDistance(Position position, int posX, int posY, int renderDistance) {
        int dstX = Math.abs(posX - position.x());
        int dstY = Math.abs(posY - position.y());
        return dstX <= renderDistance && dstY <= renderDistance;
    }

    private int getCacheOrGenerateChunk(InfMapComponent infMapComponent, SaveMetadataComponent saveMetadataComponent, int chunkX, int chunkY) {
        // regarde si le chunk est déjà present dans la map
        int chunkId = systemsAdminServer.mapManager.getChunk(chunkX, chunkY, infMapComponent.infChunks);
        if (chunkId != -1) {
            return chunkId;
        }

        try {
            chunkId = systemsAdminServer.saveInfManager.readSavedInfChunk(chunkX, chunkY, saveMetadataComponent.saveName);
        } catch (FileNotFoundException | BufferUnderflowException | IllegalMagicNumbers | ZipException | EOFException e) {
            if (e instanceof BufferUnderflowException || e instanceof EOFException) logger.error("The chunk {},{} was corrupted", chunkX, chunkY);
            if (e instanceof IllegalMagicNumbers) logger.error("The chunk {},{} was not recognise has a chunk file. Maybe the chunk version is < 9", chunkX, chunkY);
            if (e instanceof ZipException) logger.error("The chunk {},{} was not compressed", chunkX, chunkY);
            logger.info("Generating the chunk {},{}", chunkX, chunkY);

            chunkId = systemsAdminServer.mapManager.generateNewChunk(saveMetadataComponent, chunkX, chunkY);
            try {
                systemsAdminServer.saveInfManager.saveInfChunk(chunkId, SaveInfManager.getSavePath(saveMetadataComponent.saveName));
            } catch (IOException ex) {
                logger.error(e.getMessage(), ex);
                throw new RuntimeException(ex);
            }
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
        return chunkId;
    }

    @Override
    public void breakCell(int worldPosX, int worldPosY, @Null ItemRegisterEntry itemDropRegisterEntry, int playerSrc) {
        InfMapComponent infMapComponent = serverContext.getEcsEngineServer().getMapEntity().getComponent(InfMapComponent.class);
        int chunk = systemsAdminServer.mapManager.getChunk(MapManager.getChunkPos(worldPosX), MapManager.getChunkPos(worldPosY), infMapComponent.infChunks);
        int topCell = systemsAdminServer.mapManager.getTopCell(chunk, MapManager.getInnerChunk(worldPosX), MapManager.getInnerChunk(worldPosY));
        if (topCell == -1) return;
        systemsAdminServer.mapManager.damneCell(chunk, topCell);
        if (itemDropRegisterEntry != null) {
            systemsAdminServer.itemManagerServer.newItemOnGround(worldPosX, worldPosY, itemDropRegisterEntry, UUID.randomUUID());
        }
        serverContext.getServerHandler().broadCastPacketInRange(new CellBreakPacket(worldPosX, worldPosY), playerSrc);
    }

    public int placeItemToBloc(UUID itemToPlaceUuid, int worldPosX, int worldPosY) {
        int itemId = systemsAdminServer.uuidComponentManager.getRegisteredComponent(itemToPlaceUuid, ItemComponent.class);
        if (itemId == -1) return -1;
        ItemComponent itemComponent = mItem.get(itemId);
        CellRegisterEntry placeCell = itemComponent.itemRegisterEntry.getItemBehavior().getPlaceCell();
        if (placeCell == null) return -1;

        int chunkPosX = MapManager.getChunkPos(worldPosX);
        int chunkPosY = MapManager.getChunkPos(worldPosY);

        InfMapComponent infMapComponent = serverContext.getEcsEngineServer().getMapEntity().getComponent(InfMapComponent.class);
        int chunkId = systemsAdminServer.mapManager.getChunk(chunkPosX, chunkPosY, infMapComponent.infChunks);

        // can only place on item per layer
        if (systemsAdminServer.mapManager.getCell(chunkId, worldPosX, worldPosY, itemComponent.itemRegisterEntry.getItemBehavior().getPlaceCell().getCellBehavior().getLayer()) != -1) {
            return -1;
        }

        byte innerChunkX = MapManager.getInnerChunk(worldPosX);
        byte innerChunkY = MapManager.getInnerChunk(worldPosY);
        return systemsAdminServer.mapManager.newCellInChunk(chunkId, new CellArgs(placeCell, Cells.getInnerChunkPos(innerChunkX, innerChunkY)));
    }
}
