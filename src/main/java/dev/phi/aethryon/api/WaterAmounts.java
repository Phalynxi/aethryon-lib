package dev.phi.aethryon.api;

/** Precise water amounts from the server's water simulation: block positions (BlockPos.asLong) and amounts (0 to 120 in eighths of a percent of a block, negative: the block settled). */
public record WaterAmounts(long[] pos, byte[] amount) {}
