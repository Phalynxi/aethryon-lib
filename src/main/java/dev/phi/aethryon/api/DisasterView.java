package dev.phi.aethryon.api;

import java.util.List;

/** The disasters a client knows of right now (sent from the server by whatever mod runs them), for the visuals to draw. */
public interface DisasterView {
    List<Funnel> funnels();
}
