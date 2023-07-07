package ch.realmtech.game.clickAndDrop;


import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;

public interface ClickAndDropEvent {
    /** @return vrai si le click en drop commence avec cette actor*/
    Actor clickStart(final ClickActorAndSlot actor, final int[] stackActive, final InputEvent event);
    /** @return null si le click prend fin sinon le nouvel acteur à affich */
    Actor clickStop(final ClickActorAndSlot clickActorAndSlotSrc, final int[] stackActive, final ClickActorAndSlot clickActorAndSlotDst, int button);
}
