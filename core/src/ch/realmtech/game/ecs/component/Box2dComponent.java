package ch.realmtech.game.ecs.component;

import com.artemis.Component;
import com.badlogic.gdx.physics.box2d.Body;

public class Box2dComponent extends Component {
    public Body body;
    public float widthWorld;
    public float heightWorld;

    public void init(float widthWorld, float heightWorld, Body body) {
        this.widthWorld = widthWorld;
        this.heightWorld = heightWorld;
        this.body = body;
    }
}
