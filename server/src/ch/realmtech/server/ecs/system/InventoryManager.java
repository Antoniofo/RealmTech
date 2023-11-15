package ch.realmtech.server.ecs.system;

import ch.realmtech.server.craft.CraftStrategy;
import ch.realmtech.server.ctrl.ItemManager;
import ch.realmtech.server.ecs.component.*;
import ch.realmtech.server.ecs.plugin.commun.SystemsAdminCommun;
import ch.realmtech.server.mod.RealmTechCoreMod;
import ch.realmtech.server.serialize.inventory.InventorySerializer;
import com.artemis.ComponentMapper;
import com.artemis.EntityEdit;
import com.artemis.Manager;
import com.artemis.annotations.Wire;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.UUID;
import java.util.function.Function;

public class InventoryManager extends Manager {
    private final static Logger logger = LoggerFactory.getLogger(InventoryManager.class);
    @Wire(name = "systemsAdmin")
    private SystemsAdminCommun systemsAdminCommun;
    @Wire
    private ItemManager itemManager;
    private ComponentMapper<InventoryComponent> mInventory;
    private ComponentMapper<TextureComponent> mTexture;
    private ComponentMapper<ItemComponent> mItem;
    private ComponentMapper<ChestComponent> mChest;
    private ComponentMapper<UuidComponent> mUuid;

    public boolean addItemToInventory(int inventoryId, int itemId) {
        return addItemToInventory(mInventory.get(inventoryId), itemId);
    }

    /**
     * Parcourt l'inventaire à la recherche d'un emplacement disponible pour ajouter l'item
     *
     * @param inventoryComponent L'entité où l'item sera ajouté.
     * @param itemId             l'item souhaité a ajouter.
     * @return vrai si l'item a été ajouté avec success.
     */
    public boolean addItemToInventory(InventoryComponent inventoryComponent, int itemId) {
        for (int i = 0; i < inventoryComponent.inventory.length; i++) {
            if (addItemToStack(inventoryComponent.inventory[i], itemId)) {
                return true;
            }
        }
        return false;
    }

    public boolean moveStackToStackNumber(int[] stackSrc, int[] stackDst, int nombre) {
        for (int i = 0, j = tailleStack(stackSrc) - 1; i < nombre && j >= 0; i++, j--) {
            if (!addItemToStack(stackDst, stackSrc[j])) {
                return false;
            }
            stackSrc[j] = 0;
        }
        return true;
    }

    public boolean addItemToStack(int[] stack, int itemId) {
        // pas d'item dans le stack alors ajout
        if (stack[0] == 0) {
            stack[0] = itemId;
            return true;
        }

        final ItemComponent inventoryItemComponent = mItem.get(stack[0]);
        final ItemComponent itemComponent = mItem.get(itemId);
        if (!(inventoryItemComponent.itemRegisterEntry == itemComponent.itemRegisterEntry)) {
            return false; // pas le même registre, le stack doit toujours être pour un même registre
        }

        for (int i = 0; i < stack.length; i++) {
            if (stack[i] == 0) {
                stack[i] = itemId;
                return true; // Il y a dans la stack un emplacement vide
            }
        }
        return false; // Il n'y pas d'emplacement dans la stack pour ajouter l'item
    }

    /**
     * Déplace une stack au maximum vers une autre stack
     * @param stackSrc la stack à déplacer.
     * @param stackDst la stack destination.
     * @return true si quelque chose a été déplacé
     */
    public boolean moveStackToStack(int[] stackSrc, int[] stackDst) {
        if (stackSrc[0] == 0) return false;
        if (stackDst[0] == 0) {
            System.arraycopy(stackSrc, 0, stackDst, 0, tailleStack(stackSrc)); // déplace toute la stack si la stack de destination est vide
            // vide la stack source
            final int tailleStack = tailleStack(stackSrc);
            for (int i = 0; i < tailleStack; i++) {
                stackSrc[i] = 0;
            }
            return true;
        }
        final ItemComponent itemComponentSrc = mItem.get(stackSrc[0]);
        final ItemComponent itemComponentDst = mItem.get(stackDst[0]);

        if (!(itemComponentSrc.itemRegisterEntry == itemComponentDst.itemRegisterEntry)) {
            return false;
        }
        if (stackDst.length < tailleStack(stackDst)) {
            return false; // on ne peut pas ajouter des items si la stack est déjà remplie
        }

        boolean deplace = false;
        // déplace de la stack source à la stack destination en vidant la stack source
        for (int i = tailleStack(stackSrc) - 1, j = tailleStack(stackDst); i >= 0 && j < stackDst.length; i--, j++) {
            stackDst[j] = stackSrc[i];
            stackSrc[i] = 0;
            deplace = true;
        }
        return deplace;
    }

    /**
     * Donne la taille de la stack. C'est-à-dire que les emplacements vides sont ignorés
     * @param stack la stack à compter.
     * @return la taille de la stack avec des emplacements remplie.
     */
    public static int tailleStack(int[] stack) {
        int count = 0;
        for (int i = 0; i < stack.length; i++) {
            if (stack[i] != 0) {
                count++;
            } else {
                break;
            }
        }
        return count;
    }

    public int[][] getInventory(int entityId) {
        return mInventory.get(entityId).inventory;
    }

    public static void clearStack(int[] stack) {
        Arrays.fill(stack, 0);
    }

    /**
     * Remove all items in this inventory from this world and set to 0 all slot in all stack.
     * @param inventory The inventory to remove.
     */
    public void removeInventory(int[][] inventory) {
        for (int[] stack : inventory) {
            deleteStack(stack);
        }
    }

    /**
     * Delete all items from in this stack from this world and set to 0 all slot.
     * @param stack The stack to remove.
     */
    private void deleteStack(int[] stack) {
        final int tailleStack = InventoryManager.tailleStack(stack);
        for (int i = 0; i < tailleStack; i++) {
            world.delete(stack[i]);
        }
        clearStack(stack);
    }

    /**
     * Delete the item on top of this stack.
     * @param stack The stack to remove.
     */
    public void deleteOneItem(int[] stack) {
        int taille = tailleStack(stack);
        if (taille == 0) return;
        world.delete(stack[taille - 1]);
        stack[taille - 1] = 0;
    }

    /**
     * Delete all item on top of this inventory
     * @param inventory The inventory to remove top item.
     */
    public void deleteAllOneItem(int[][] inventory) {
        for (int i = 0; i < inventory.length; i++) {
            deleteOneItem(inventory[i]);
        }
    }

    /**
     * For the stack to be moved, the src stack must not be empty and the items
     * in the source stack and the dst stack must be in the same register.
     * @param src The source stack.
     * @param dst The destination stack.
     */
    public boolean canMouveStack(int[] src, int[] dst) {
        if (src[0] == 0) return false;
        if (dst[0] == 0) return true;
        ItemComponent itemComponentSrc = mItem.get(src[0]);
        ItemComponent itemComponentDst = mItem.get(dst[0]);
        return dst.length - tailleStack(dst) >= tailleStack(src) && itemComponentSrc.itemRegisterEntry == itemComponentDst.itemRegisterEntry;
    }

    public static int getTopItem(int[] stack) {
        int tailleStack = tailleStack(stack);
        if (tailleStack > 0) {
            return stack[tailleStack - 1];
        } else {
            return 0;
        }
    }

    /**
     * Get the stack that contains this item id.
     * @param itemId The item id where is the stack to find.
     * @param inventoryId The inventory to search into.
     * @return The stack that contains the item id or null if stack was not found.
     */
    public int[] getStackContainsItem(int inventoryId, int itemId) {
        InventoryComponent inventoryComponent = mInventory.get(inventoryId);
        for (int[] stack : inventoryComponent.inventory) {
            for (int i : stack) {
                if (i == itemId) {
                    return stack;
                }
            }
        }
        return null;
    }

    /**
     * Delete a item in this inventory. This item can be anywhere in this inventory.
     * <strong>Delete</string> this item from this world.
     * @param inventoryId The inventory where the item will be remove.
     * @param itemId The item id to remove.
     * @return true if the operation was successful.
     */
    public boolean deleteItemInInventory(int inventoryId, int itemId) {
        int[] stack = getStackContainsItem(inventoryId, itemId);
        if (stack == null) return false;
        return deleteItemInStack(stack, itemId);
    }

    /**
     * Remove a item in this inventory. This item can be anywhere in this inventory.
     * <strong>Do not delete</strong> this item from this world.
     * @param inventoryId The inventory where the item will be remove.
     * @param itemId The item id to remove.
     * @return true if the operation was successful.
     */
    public boolean removeItemInInventory(int inventoryId, int itemId) {
        int[] stack = getStackContainsItem(inventoryId, itemId);
        if (stack == null) return false;
        return removeItemInStack(stack, itemId);
    }

    /**
     * Delete one item in this stack. The item can be anywhere in this stack.
     * <strong>Delete</string> this item from this world.
     * @param stack  The stack the contains the item.
     * @param itemId The item id to remove.
     * @return true if the operation was successful.
     */
    public boolean deleteItemInStack(int[] stack, int itemId) {
        if (removeItemInStack(stack, itemId)) {
            world.delete(itemId);
            return true;
        } else  {
            return false;
        }
    }

    /**
     * Remove one item in this stack. The item can be anywhere in this stack.
     * <strong>Do not delete</strong> the item from this world. See non static
     * method {@link #deleteItemInStack} for delete item.
     * @param stack  The stack the contains the item.
     * @param itemId The item id to remove.
     * @return true if the operation was successful.
     */
    public static boolean removeItemInStack(int[] stack, int itemId) {
        int index = -1;
        for (int i = 0; i < stack.length; i++) {
            if (stack[i] == itemId) {
                index = i;
                break;
            }
        }
        if (index == -1) {
            return false;
        }

        int[] resultArray = new int[stack.length];
        for (int i = 0, j = 0; j < stack.length; i++, j++) {
            if (i == index) {
                j++;
            }
            resultArray[i] = stack[j];
        }
        System.arraycopy(resultArray, 0, stack, 0, stack.length);
        return true;
    }

    /**
     * Give the {@link InventoryComponent} id who has this {@link UuidComponent}.
     * @param uuid The uuid value to test with
     * @return The corresponding inventory id or -1 if none inventory has this uuid value.
     */
    public int getInventoryByUUID(UUID uuid) {
        return systemsAdminCommun.uuidComponentManager.getRegisteredComponent(uuid, InventoryComponent.class);
    }

    /**
     * Give the {@link InventoryComponent} who has this {@link UuidComponent}.
     * @param uuid The uuid value to test with
     * @return The corresponding inventory id or null if none inventory has this {@link UuidComponent#uuid}.
     */
    public InventoryComponent getInventoryComponentByUUID(UUID uuid) {
        int inventoryId = getInventoryByUUID(uuid);
        return inventoryId != -1 ? mInventory.get(inventoryId) : null;
    }

    public void setInventory(UUID inventoryUUID, byte[] inventoryBytes) {
        int inventoryId = getInventoryByUUID(inventoryUUID);
        if (inventoryId == -1) return;
        Function<ItemManager, int[][]> inventoryGet = InventorySerializer.getFromBytes(world, inventoryBytes);
        int[][] newInventory = inventoryGet.apply(itemManager);
        int[][] inventory = mInventory.get(inventoryId).inventory;
        for (int i = 0; i < inventory.length; i++) {
            System.arraycopy(newInventory[i], 0, inventory[i], 0, newInventory[i].length);
        }
    }

    public synchronized void moveInventory(UUID srcInventoryUUID, UUID dstInventoryUUID, UUID[] itemsToMove, int slotIndex) {
        int srcInventoryId = getInventoryByUUID(srcInventoryUUID);
        int dstInventoryId = getInventoryByUUID(dstInventoryUUID);
        if (srcInventoryId == -1){
            logger.warn("The src inventory {} was not found", srcInventoryId);
            return;
        }

        if (dstInventoryId == -1){
            logger.warn("The src inventory {} was not found", srcInventoryId);
            return;
        }

        int[] itemsSrcId = new int[itemsToMove.length];
        for (UUID playerUuid : itemsToMove) {
            int itemId = world.getSystem(ItemManagerServer.class).getItemByUUID(playerUuid);
            if (itemId == -1) {
                logger.warn("The item id {} was not found", playerUuid);
                return;
            }
        }

        int[] srcStack = null;
        for (int i = 0; i < itemsSrcId.length; i++) {
            int[] stack = getStackContainsItem(srcInventoryId, itemsSrcId[i]);
            if (srcStack == null) {
                srcStack = stack;
            } else {
                if (srcStack != stack) {
                    logger.warn("Items are split between multiple stack");
                    return;
                }
            }
        }

        int[][] dstInventory = mInventory.get(dstInventoryId).inventory;
        if (slotIndex > dstInventory.length) {
            logger.warn("slot item is out of bound");
            return;
        }
        moveStackToStackNumber(srcStack, dstInventory[slotIndex], itemsToMove.length);


    }

    /**
     * Create a fully inventory.
     * @param entityId The entity to add the inventory component.
     * @param inventory
     * @param numberOfSlotParRow
     * @param numberOfRow
     * @param backgroundTextureName
     * @return The inventory newly created.
     */
    @Deprecated
    public InventoryComponent createInventoryComponent(int entityId, int[][] inventory, UUID uuid, int numberOfSlotParRow, int numberOfRow, String backgroundTextureName) {
        InventoryComponent inventoryComponent = world.edit(entityId).create(InventoryComponent.class).set(inventory, numberOfSlotParRow, numberOfRow, backgroundTextureName);
        systemsAdminCommun.uuidComponentManager.createRegisteredComponent(uuid, entityId);
        return inventoryComponent;
    }

    /**
     * Create a empty inventory with a random uuid.
     * @param entityId The entity to add the inventory component.
     * @param numberOfSlotParRow
     * @param numberOfRow
     * @param backgroundTextureName
     * @return The inventory newly created.
     */
    @Deprecated
    public InventoryComponent createInventoryComponent(int entityId, int numberOfSlotParRow, int numberOfRow, String backgroundTextureName) {
        return createInventoryComponent(entityId, UUID.randomUUID(), numberOfSlotParRow, numberOfRow, backgroundTextureName);
    }

    /**
     * Create a empty inventory.
     * @param entityId The entity to add the inventory component.
     * @param numberOfSlotParRow
     * @param numberOfRow
     * @param backgroundTextureName
     * @return The inventory newly created.
     */
    @Deprecated
    public InventoryComponent createInventoryComponent(int entityId, UUID uuid, int numberOfSlotParRow, int numberOfRow, String backgroundTextureName) {
        InventoryComponent inventoryComponent = world.edit(entityId).create(InventoryComponent.class).set(numberOfSlotParRow, numberOfRow, backgroundTextureName);
        systemsAdminCommun.uuidComponentManager.createRegisteredComponent(uuid, entityId);
        return inventoryComponent;
    }

    public int createChest(int motherEntity, UUID inventoryUuid, int numberOfSlotParRow, int numberOfRow) {
        int inventoryId = world.create();
        world.edit(motherEntity).create(ChestComponent.class).set(inventoryId);

        EntityEdit inventoryEdit = world.edit(inventoryId);
        inventoryEdit.create(UuidComponent.class).set(inventoryUuid);
        inventoryEdit.create(InventoryComponent.class).set(numberOfSlotParRow, numberOfRow);
        inventoryEdit.create(InventoryUiComponent.class).set();
        return inventoryId;
    }

    public int createChest(int motherEntity, int[][] inventory, UUID inventoryUuid, int numberOfSlotParRow, int numberOfRow) {
        int inventoryId = world.create();
        world.edit(motherEntity).create(ChestComponent.class).set(inventoryId);

        EntityEdit inventoryEdit = world.edit(inventoryId);
        inventoryEdit.create(UuidComponent.class).set(inventoryUuid);
        inventoryEdit.create(InventoryComponent.class).set(inventory, numberOfSlotParRow, numberOfRow);
        inventoryEdit.create(InventoryUiComponent.class).set();
        return inventoryId;
    }
    /** @return a table with the first index of crafting inventoryId and the seconde index of crafting result inventoryId */
    public int[] createCraftingTable(int motherEntity, UUID craftingInventoryUuid, int craftingNumberOfSlotParRow, int craftingNumberOfRow, UUID craftingResultInventoryUuid) {
        int craftingInventoryId = world.create();
        int craftingResultInventoryId = world.create();

        world.edit(motherEntity).create(CraftingTableComponent.class).set(craftingInventoryId, craftingResultInventoryId, CraftStrategy.craftingStrategyCraftingTable());
        EntityEdit craftingInventoryEdit = world.edit(craftingInventoryId);
        craftingInventoryEdit.create(UuidComponent.class).set(craftingInventoryUuid);
        craftingInventoryEdit.create(InventoryComponent.class).set(craftingNumberOfSlotParRow, craftingNumberOfRow);
        craftingInventoryEdit.create(InventoryUiComponent.class).set();
        craftingInventoryEdit.create(CraftingComponent.class).set(RealmTechCoreMod.CRAFT, craftingResultInventoryId);

        EntityEdit craftingResultInventoryEdit = world.edit(craftingResultInventoryId);
        craftingResultInventoryEdit.create(UuidComponent.class).set(craftingResultInventoryUuid);
        craftingResultInventoryEdit.create(InventoryComponent.class).set(1, 1);
        craftingResultInventoryEdit.create(InventoryUiComponent.class).set();
        return new int[]{craftingInventoryId, craftingResultInventoryId};
    }

    public InventoryComponent getChestInventory(int motherEntity) {
        return mInventory.get(mChest.get(motherEntity).getInventoryId());
    }
    public int getChestInventoryId(int motherEntity) {
        return mChest.get(motherEntity).getInventoryId();
    }

}
