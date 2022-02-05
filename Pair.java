public class Pair
{
    public final int address;
    public final boolean dirty_bit;
    public final boolean hit;

    public Pair(int address, boolean dirty_bit, boolean hit)
    {
        this.address = address;
        this.dirty_bit = dirty_bit;
        this.hit = hit;
    }
}