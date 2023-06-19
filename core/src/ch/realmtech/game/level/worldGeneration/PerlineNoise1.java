package ch.realmtech.game.level.worldGeneration;

import java.util.Random;

public class PerlineNoise1 implements GeneratePerlinNoise {
    private final float roughness;
    private float[][] grid;
    private Random rand;

    public PerlineNoise1(float roughness) {
        this.roughness = roughness;
    }

    @Override
    public float[][] generate(Random rand, int worldWith, int worldHigh) {
        this.rand = rand;
        grid = new float[worldWith][worldHigh];
        int xh = grid.length - 1;
        int yh = grid[0].length - 1;

        // set the corner points
        grid[0][0] = rand.nextFloat() - 0.5f;
        grid[0][yh] = rand.nextFloat() - 0.5f;
        grid[xh][0] = rand.nextFloat() - 0.5f;
        grid[xh][yh] = rand.nextFloat() - 0.5f;

        // generate the fractal
        generate(0, 0, xh, yh);
        return grid;
    }

    @Override
    public float get(int x, int y) {
        return 0;
    }

    private float roughen(float v, int l, int h) {
        return v + roughness * (float) (rand.nextGaussian() * (h - l));
    }


    // generate the fractal
    private void generate(int xl, int yl, int xh, int yh) {
        int xm = (xl + xh) / 2;
        int ym = (yl + yh) / 2;
        if ((xl == xm) && (yl == ym)) return;

        grid[xm][yl] = 0.5f * (grid[xl][yl] + grid[xh][yl]);
        grid[xm][yh] = 0.5f * (grid[xl][yh] + grid[xh][yh]);
        grid[xl][ym] = 0.5f * (grid[xl][yl] + grid[xl][yh]);
        grid[xh][ym] = 0.5f * (grid[xh][yl] + grid[xh][yh]);

        float v = roughen(0.5f * (grid[xm][yl] + grid[xm][yh]), xl + yl, yh + xh);
        grid[xm][ym] = v;
        grid[xm][yl] = roughen(grid[xm][yl], xl, xh);
        grid[xm][yh] = roughen(grid[xm][yh], xl, xh);
        grid[xl][ym] = roughen(grid[xl][ym], yl, yh);
        grid[xh][ym] = roughen(grid[xh][ym], yl, yh);

        generate(xl, yl, xm, ym);
        generate(xm, yl, xh, ym);
        generate(xl, ym, xm, yh);
        generate(xm, ym, xh, yh);
    }
}
