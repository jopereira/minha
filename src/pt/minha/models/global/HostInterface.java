package pt.minha.models.global;

import pt.minha.api.SimulationException;

public interface HostInterface {
	void launch(long delay, String main, String[] args) throws SimulationException;
}
