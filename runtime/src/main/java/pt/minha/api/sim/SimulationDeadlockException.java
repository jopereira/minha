package pt.minha.api.sim;

/**
 * The simulation has fininshed without completing a synchronous invocation.
 * This means that the simulation is in a deadlock.
 */
public class SimulationDeadlockException extends RuntimeException {
}
