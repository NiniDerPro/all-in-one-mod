package dev.aio.core.platform;

import java.util.Map;

/**
 * Dorfbewohner-Beruf.
 *
 * @param jobBlock  Item des Arbeitsblocks (leer bei Berufen ohne, z. B. Nitwit)
 * @param tradeSets Trade-Set-ID je Stufe (1 Neuling … 5 Meister)
 */
public record Profession(String id, String name, String jobBlock, Map<Integer, String> tradeSets) {}
