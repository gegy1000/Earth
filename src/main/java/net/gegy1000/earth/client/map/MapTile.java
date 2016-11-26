package net.gegy1000.earth.client.map;

import net.gegy1000.earth.server.util.MapPoint;
import net.gegy1000.earth.server.util.osm.OpenStreetMap;
import net.gegy1000.earth.server.world.gen.EarthGenerator;
import net.gegy1000.earth.server.world.gen.WorldTypeEarth;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

public class MapTile {
    public static final int SIZE = 16;
    public static final int SHIFT = 4;

    private final int x;
    private final int z;
    private final int tileX;
    private final int tileZ;

    private final BlockPos.MutableBlockPos center;

    private final Set<MapObject> mapObjects = new HashSet<>();

    public MapTile(int x, int z) {
        this.x = x;
        this.z = z;
        this.tileX = x >>> SHIFT;
        this.tileZ = z >>> SHIFT;
        int centerOffset = SIZE >> 1;
        this.center = new BlockPos.MutableBlockPos(this.x + centerOffset, 0, this.z + centerOffset);
    }

    public int getX() {
        return this.x;
    }

    public int getZ() {
        return this.z;
    }

    public int getMaxX() {
        return this.x + SIZE;
    }

    public int getMaxZ() {
        return this.z + SIZE;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MapTile) {
            MapTile tile = (MapTile) obj;
            return tile.tileX == this.tileX && tile.tileZ == this.tileZ;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.tileZ << 8 | this.tileX;
    }

    public BlockPos getCenter(int y) {
        this.center.setY(y);
        return this.center;
    }

    public void load(World world) {
        EarthGenerator generator = WorldTypeEarth.getGenerator(world);
        double minX = generator.toLongitude(this.getX());
        double maxX = generator.toLongitude(this.getMaxX());
        double minZ = generator.toLatitude(this.getZ());
        double maxZ = generator.toLatitude(this.getMaxZ());
        MapPoint startPoint = new MapPoint(world, Math.min(minZ, maxZ), Math.min(minX, maxX));
        MapPoint endPoint = new MapPoint(world, Math.max(minZ, maxZ), Math.max(minX, maxX));
        try {
            InputStream in = OpenStreetMap.openStream(startPoint, endPoint);
            if (in != null) {
                OpenStreetMap.TileData data = OpenStreetMap.parse(world, in);
                this.apply(data);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Set<MapObject> getMapObjects() {
        return this.mapObjects;
    }

    public void apply(OpenStreetMap.TileData data) {
        this.mapObjects.addAll(data.getWays());
        this.mapObjects.addAll(data.getBuildings());
    }

    public void delete() {
        for (MapObject mapObject : this.mapObjects) {
            mapObject.delete();
        }
    }
}
