package de.mhus.app.kpush;

import de.mhus.lib.core.MApi;
import de.mhus.lib.core.MArgs;
import de.mhus.lib.core.logging.Log;
import de.mhus.lib.core.logging.Log.LEVEL;

public class MainCli {

    public static void main(String[] args) throws Exception {
        
        MArgs margs = new MArgs(
                args
                ,
                MArgs.help("Push files into k8s containers"),
                MArgs.arg("command", false, 
                          "test   : Show what it would do (default)\n"
                        + "reset  : Set the last update time to 1970\n"
                        + "touch  : Touch the last update time to now or a time period with -t\n"
                        + "watch  : push in a loop until Ctrl+C\n"
                        + "push   : Push once and set last update time to now, use -t to filter changed files\n"
                        + "pushall: Push all files ignoring the last update time\n"
                        + "info   : Show info to all targets or details for specific"),
                MArgs.argAll("filter", "specify targets to execute, if not set all targets will be executed"),
                MArgs.opt('l', null, 1, false, "Log level"),
                MArgs.opt('i', null, 1, false, "Interval in ms"),
                MArgs.opt('c', null, 1, false, "Config directory or file"),
                MArgs.opt('t', null, 1, false, "Time period, e.g. 10min 1h 1d"),
                MArgs.allowOtherOptions()
                );
        
        if (margs.isPrintUsage()) { 
            margs.printUsage();
            System.exit(margs.isValid() ? 0 : 1);
        }
        
        String verbose = margs.getOption("l").getValue();

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
        
        String action = margs.getArgument(1).getValue("test");
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
        case "info":
            inst.showInfo();
            break;
        default:
            System.out.println("Action unknown - aborting");
        }
    }

}
