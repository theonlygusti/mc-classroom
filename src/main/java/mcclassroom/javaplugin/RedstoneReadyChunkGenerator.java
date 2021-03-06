package mcclassroom.javaplugin;

import java.util.Random;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.util.noise.SimplexOctaveGenerator;

public class RedstoneReadyChunkGenerator extends ChunkGenerator {
  int currentHeight = 56;

  @Override
  public ChunkData generateChunkData(World world, Random random, int chunkX, int chunkZ, BiomeGrid biome) {
    SimplexOctaveGenerator generator = new SimplexOctaveGenerator(new Random(world.getSeed()), 8);
    ChunkData chunk = createChunkData(world);
    generator.setScale(0.000D);

    for(int X=0; X<16; X++) {
      for(int Z=0; Z<16; Z++) {
        currentHeight = (int)(generator.noise(chunkX*16+X, chunkZ*16+Z, 0.5D, 0.5D) * 15D + 50D);
        for (int i = currentHeight; i>3; i--) chunk.setBlock(X, i, Z, Material.SANDSTONE);
        chunk.setBlock(X, 3, Z, Material.STONE);
        chunk.setBlock(X, 2, Z, Material.STONE);
        chunk.setBlock(X, 1, Z, Material.STONE);
        chunk.setBlock(X, 0, Z, Material.BEDROCK);
      }
    }
    return chunk;
  }
}

