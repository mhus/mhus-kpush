package de.mhus.app.kpush;

import de.mhus.lib.core.MApi;
import de.mhus.lib.core.MArgs;
import de.mhus.lib.core.logging.Log;
import de.mhus.lib.core.logging.Log.LEVEL;

public class MainCli {

    public static void main(String[] args) throws Exception {
        
        MArgs margs = new MArgs(args);
        
        String verbose = margs.getValue("v", null, 0);
        if (verbose != null) {
            MApi.setDirtyTrace(false);
            LEVEL level = Log.LEVEL.DEBUG;
            verbose = verbose.toLowerCase();
            if (verbose.equals("trace"))
                level = Log.LEVEL.TRACE;
            if (verbose.equals("info"))
                level = Log.LEVEL.INFO;
            if (verbose.equals("warn"))
                level = Log.LEVEL.WARN;
            if (verbose.equals("error"))
                level = Log.LEVEL.ERROR;
            if (verbose.equals("fatal"))
                level = Log.LEVEL.FATAL;
            MApi.get().getLogFactory().setDefaultLevel(level);
        }
        
        KPush inst = new KPush();
        inst.setArguments(margs);
        inst.init();
        
        String action = margs.getValue(MArgs.DEFAULT, 0);
        if (action == null) action = "push";
        switch (action) {
        case "test":
            inst.test();
            break;
        case "reset":
            inst.reset();
            break;
        case "touch":
            inst.touch();
            break;
        case "watch":
            inst.watch();
            break;
        case "push":
            inst.push();
            break;
        case "pushall":
            inst.pushAll();
            break;
        default:
            System.out.println("Action unknown - aborting");
        }
    }

}
