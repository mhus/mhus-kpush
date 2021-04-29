package de.mhus.app.kpush;

import de.mhus.lib.core.node.INode;
import de.mhus.lib.errors.MException;

public class WatchFactory {

    public static Watch create(Job job, INode config) throws MException {
        String algorithm = job.getConfig().getString("algorithm", "simple");
        switch (algorithm) {
        case "simple":
            return new WatchSimple(job, config);
        default:
            throw new MException("Algorithm not found",job.getName());
        }
    }

}
