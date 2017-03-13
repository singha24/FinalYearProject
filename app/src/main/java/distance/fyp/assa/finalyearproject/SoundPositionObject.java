package distance.fyp.assa.finalyearproject;

/**
 * Created by Assa on 13/03/2017.
 */

public class SoundPositionObject {

    private float x;
    private float y;
    private float z;

    public SoundPositionObject(float x, float y, float z){
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getZ() {
        return z;
    }

    public void setX(float x) {
        this.x = x;
    }

    public void setY(float y) {
        this.y = y;
    }

    public void setZ(float z) {
        this.z = z;
    }
}
