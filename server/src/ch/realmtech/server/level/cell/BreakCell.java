package ch.realmtech.server.level.cell;


import ch.realmtech.server.newRegistry.NewItemEntry;
import com.artemis.World;
import com.badlogic.gdx.utils.Null;

public interface BreakCell {
    boolean breakCell(CellManager cellManager, World world, int chunkId, int cellId, @Null NewItemEntry itemUseByPlayer, int playerSrc);
}
