package ch.realmtechCommuns.packet.clientPacket;

import java.util.UUID;

public interface ClientExecute {
    void connexionJoueurReussit(final float x, final float y, UUID uuid);

    void autreJoueur(float x, float y, UUID uuid);
}
