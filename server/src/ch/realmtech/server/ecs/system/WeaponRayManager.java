package ch.realmtech.server.ecs.system;

import ch.realmtech.server.ServerContext;
import ch.realmtech.server.ecs.component.Box2dComponent;
import ch.realmtech.server.ecs.component.InvincibilityComponent;
import ch.realmtech.server.ecs.component.PositionComponent;
import ch.realmtech.server.ecs.plugin.server.SystemsAdminServer;
import ch.realmtech.server.ia.IaComponent;
import ch.realmtech.server.packet.clientPacket.MobDeletePacket;
import ch.realmtech.server.packet.clientPacket.ParticleAddPacket;
import com.artemis.Aspect;
import com.artemis.ComponentMapper;
import com.artemis.Manager;
import com.artemis.annotations.Wire;
import com.artemis.utils.Bag;
import com.artemis.utils.IntBag;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Fixture;
import io.netty.channel.Channel;

import java.util.UUID;

public class WeaponRayManager extends Manager {
    @Wire(name = "serverContext")
    private ServerContext serverContext;
    @Wire
    private SystemsAdminServer systemsAdminServer;
    @Wire(name = "physicWorld")
    private com.badlogic.gdx.physics.box2d.World physicWorld;

    private ComponentMapper<Box2dComponent> mBox2d;
    private ComponentMapper<PositionComponent> mPos;

    private IntBag rayCast(Vector2 vectorStart, Vector2 vectorEnd, Aspect.Builder aspectBuilder, BodyHitsCallback callback) {
        IntBag entities = world.getAspectSubscriptionManager().get(aspectBuilder.all(Box2dComponent.class).exclude(InvincibilityComponent.class)).getEntities();

        Bag<Body> bodyHits = new Bag<>();
        physicWorld.rayCast((fixture, point, normal, fraction) -> callback.reportRayFixture(bodyHits, fixture, point, normal, fraction), vectorStart, vectorEnd);

        IntBag entityHits = new IntBag();
        for (int i = 0; i < entities.size(); i++) {
            for (int j = 0; j < bodyHits.size(); j++) {
                int entityId = entities.get(i);
                if (mBox2d.get(entityId).body == bodyHits.get(j)) {
                    entityHits.add(entityId);
                }
            }
        }

        return entityHits;
    }

    public int getMobHit(int playerId, Vector2 vectorClick) {
        PositionComponent playerPos = mPos.get(playerId);

        Vector2 vectorStart = new Vector2(playerPos.x, playerPos.y);
        Vector2 vectorEnd = vectorClick.sub(vectorStart).setLength(100);
        IntBag mobs = rayCast(vectorStart, vectorEnd, Aspect.all(IaComponent.class), getFirstHit());

        if (!mobs.isEmpty()) {
            return mobs.get(0);
        } else {
            return -1;
        }
    }

    public void playerWeaponShot(Channel clientChannel, Vector2 vectorClick) {
        int playerId = serverContext.getSystemsAdminServer().getPlayerManagerServer().getPlayerByChannel(clientChannel);
        int mobId = getMobHit(playerId, vectorClick);
        if (mobId != -1) {
            PositionComponent mobPosition = mPos.get(mobId);
            UUID mobUuid = systemsAdminServer.getUuidEntityManager().getEntityUuid(mobId);
            serverContext.getServerConnexion().sendPacketTo(new ParticleAddPacket(ParticleAddPacket.Particles.HIT, mobPosition.toVector2()), clientChannel);

            if (systemsAdminServer.getMobManager().attackMob(mobId, 5)) {
                systemsAdminServer.getMobManager().destroyMob(mobId);
                serverContext.getServerConnexion().sendPacketTo(new MobDeletePacket(mobUuid), clientChannel);
            } else {
                systemsAdminServer.getMobManager().knockBackMob(mobId, new Vector2(100, 0));
                world.edit(mobId).create(InvincibilityComponent.class).set(60);
            }
        }
    }

    private interface BodyHitsCallback {
        float reportRayFixture(Bag<Body> bodyAccumulator, Fixture fixture, Vector2 point, Vector2 normal, float fraction);
    }

    private BodyHitsCallback getFirstHit() {
        return (bodyAccumulator, fixture, point, normal, fraction) -> {
            bodyAccumulator.add(fixture.getBody());
            return 0;
        };
    }
}
