package de.mhus.app.kpush;

import de.mhus.lib.core.MApi;
import de.mhus.lib.core.MArgs;
import de.mhus.lib.core.logging.Log;
import de.mhus.lib.core.logging.Log.LEVEL;

public class MainCli {

    public static void main(String[] args) throws Exception {
        
        MArgs margs = new MArgs(args,
        		MArgs.help("Tool to watch and push changed files in a kubernetes pod"),
        		MArgs.optVal('l', "Log level trace, debug, info, warn, error, fatal, default is INFO"),
        		MArgs.optVal('t', "Time interval, e.g. 1d, 1h, 1min, default is now"),
        		MArgs.optVal('i', "Interval in milliseconds for watch command, default is 5000"),
        		MArgs.optVal('c', "Configuration directory or file, default is '~/.kpush/config'"),
        		MArgs.arg("cmd", "Command:\ntest - Test push\ntouch - Touch sync to time interval\npush - Push files changed since last push,\npushall - Push all files\nwatch - Watch all files in a loop"),
        		MArgs.argAll("filter", "Filter configurations")
        		);
        if (!margs.isPrintUsage()) {
        	margs.printUsage();
        	System.exit(margs.isValid() ? 0 : 1);
        }
        
        String verbose = margs.getOption("v").getValue();
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
        
        String action = margs.getArgument(1).getValue("push");
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
