package de.mhus.app.kpush;

import de.mhus.lib.core.MArgs;
import de.mhus.lib.errors.MException;

public class Main {

    public static void main(String[] args) throws MException {
        
        MArgs margs = new MArgs(args);
        
        KPush inst = new KPush();
        
        String action = margs.getValue(MArgs.DEFAULT, 0);
        if (action == null) action = "push";
        switch (action) {
        case "watch":
            inst.watch();
            break;
        case "push":
            inst.push();
            break;
        default:
            System.out.println("Action unknown - aborting");
        }
    }

}
