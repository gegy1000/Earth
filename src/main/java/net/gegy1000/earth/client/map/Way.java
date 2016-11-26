package net.gegy1000.earth.client.map;

import net.gegy1000.earth.server.util.MapPoint;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexBuffer;
import net.minecraft.util.math.AxisAlignedBB;
import org.lwjgl.opengl.GL11;

import javax.vecmath.Vector3d;
import java.util.List;
import java.util.Map;

public class Way implements MapObject {
    private final String name;
    private final List<MapPoint> points;
    private final AxisAlignedBB bounds;
    private final Vector3d center;
    private final double width;
    private final Type type;

    private VertexBuffer buffer;
    private boolean built;

    public Way(String name, List<MapPoint> points, double width, Type type) {
        this.name = name;
        this.points = points;
        this.type = type;
        if (this.type == Type.PATH) {
            width /= 2;
        }
        this.width = width;
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, minZ = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE, maxY = Double.MIN_VALUE, maxZ = Double.MIN_VALUE;
        double averageX = 0.0;
        double averageY = 0.0;
        double averageZ = 0.0;
        for (MapPoint point : points) {
            double x = point.getX();
            double y = point.getY();
            double z = point.getZ();
            averageX += x;
            averageY += y;
            averageZ += z;
            if (x < minX) {
                minX = x;
            }
            if (x > maxX) {
                maxX = x;
            }
            if (y < minY) {
                minY = y;
            }
            if (y > maxY) {
                maxY = y;
            }
            if (z < minZ) {
                minZ = z;
            }
            if (z > maxZ) {
                maxZ = z;
            }
        }
        this.center = new Vector3d(averageX, averageY, averageZ);
        this.bounds = new AxisAlignedBB(minX, minY - 0.5, minZ, maxX, maxY + 0.5, maxZ);
    }

    public String getName() {
        return this.name;
    }

    public List<MapPoint> getPoints() {
        return this.points;
    }

    public Type getType() {
        return this.type;
    }

    @Override
    public AxisAlignedBB getBounds() {
        return this.bounds;
    }

    @Override
    public boolean hasBuilt() {
        return this.built;
    }

    @Override
    public void build() {
        this.buffer = new VertexBuffer(DefaultVertexFormats.POSITION);
        this.buffer.bindBuffer();

        BUILDER.begin(GL11.GL_QUAD_STRIP, DefaultVertexFormats.POSITION);

        for (int i = 0; i < this.points.size(); i++) {
            MapPoint point = this.points.get(i);
            if (i < this.points.size() - 1) {
                MapPoint next = this.points.get(i + 1);
                double deltaX = next.getX() - point.getX();
                double deltaZ = next.getZ() - point.getZ();
                double length = Math.sqrt((deltaX * deltaX) + (deltaZ * deltaZ));
                double offsetX = (this.width * deltaZ / length) / 2;
                double offsetZ = (this.width * deltaX / length) / 2;
                BUILDER.pos(point.getX() - offsetX, point.getY(), point.getZ() + offsetZ).endVertex();
                BUILDER.pos(point.getX() + offsetX, point.getY(), point.getZ() - offsetZ).endVertex();
                BUILDER.pos(next.getX() - offsetX, next.getY(), next.getZ() + offsetZ).endVertex();
                BUILDER.pos(next.getX() + offsetX, next.getY(), next.getZ() - offsetZ).endVertex();
            }
        }

        this.finish(this.buffer);
        this.buffer.unbindBuffer();

        this.built = true;
    }

    @Override
    public void render() {
        GlStateManager.enableCull();
        GlStateManager.cullFace(GlStateManager.CullFace.FRONT);
        this.type.apply();

        this.buffer.bindBuffer();
        GlStateManager.glVertexPointer(3, GL11.GL_FLOAT, 12, 0);
        this.buffer.drawArrays(GL11.GL_QUAD_STRIP);
        this.buffer.unbindBuffer();

        GlStateManager.cullFace(GlStateManager.CullFace.BACK);
        GlStateManager.disableCull();
    }

    @Override
    public void enableState() {
        GlStateManager.glEnableClientState(GL11.GL_VERTEX_ARRAY);
    }

    @Override
    public void disableState() {
        GlStateManager.glDisableClientState(GL11.GL_VERTEX_ARRAY);
    }

    @Override
    public void delete() {
        if (this.buffer != null) {
            this.buffer.deleteGlBuffers();
        }
        this.built = false;
    }

    @Override
    public Vector3d getCenter() {
        return this.center;
    }

    public enum Type {
        LARGE_ROAD(64, 64, 64),
        ROAD(0, 0, 0),
        RAILWAY(200, 128, 0),
        STREAM(0, 140, 255),
        PATH(110, 60, 20),
        UNKNOWN(255, 0, 0);

        private float red;
        private float green;
        private float blue;

        Type(int red, int green, int blue) {
            this.red = red / 255.0F;
            this.green = green / 255.0F;
            this.blue = blue / 255.0F;
        }

        public void apply() {
            GlStateManager.color(this.red, this.green, this.blue, 1.0F);
        }

        public static Type fromTags(Map<String, String> tags) {
            String highway = tags.get("highway");
            String waterway = tags.get("waterway");
            String railway = tags.get("railway");
            if (highway != null) {
                switch (highway) {
                    case "motorway_link":
                    case "primary_link":
                    case "primary":
                    case "motorway":
                        return LARGE_ROAD;
                    case "secondary_link":
                    case "tertiary":
                    case "secondary":
                    case "residential":
                        return ROAD;
                    case "footway":
                    case "path":
                    case "steps":
                    case "pedestrian":
                    case "track":
                        return PATH;
                }
                return ROAD;
            }
            if (waterway != null) {
                switch (waterway) {
                    case "boatyard":
                    case "stream":
                    case "dock":
                        return STREAM;
                }
                return STREAM;
            }
            if (railway != null) {
                return RAILWAY;
            }
            return UNKNOWN;
        }
    }
}