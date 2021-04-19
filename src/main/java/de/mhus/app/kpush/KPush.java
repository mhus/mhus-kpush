package de.mhus.app.kpush;

import java.io.File;
import java.util.ArrayList;

import de.mhus.lib.core.M;
import de.mhus.lib.core.MArgs;
import de.mhus.lib.core.MFile;
import de.mhus.lib.core.MLog;
import de.mhus.lib.core.config.IConfig;
import de.mhus.lib.core.config.IConfigFactory;
import de.mhus.lib.core.console.Console;
import de.mhus.lib.core.console.ConsoleTable;
import de.mhus.lib.errors.MException;

public class KPush extends MLog {

    private File configDir;
    private ArrayList<Job> jobs = new ArrayList<>();
    private MArgs args;
    private String homeDir;
    private int interval;
    
    public void init() throws MException {
        
        interval = M.to(getArguments().getValue("i", 0), 5000 );

        homeDir = System.getenv("KPUSH_HOME");
        if (homeDir == null)
            homeDir = "~/.kpush";
        String configDir = args.getValue("c", 0);
        if (configDir == null)
            this.configDir = MFile.toFile(homeDir + "/config");
        else
            this.configDir = MFile.toFile(configDir);
        if (!this.configDir.exists())
            log().w("Config directory not found",configDir);
        loadConfig();
        
        
    }

    private void loadConfig() throws MException {
        if (configDir.isFile()) {
            if (configDir.getName().endsWith(".yaml"))
                loadConfig(configDir);
        } else
            for (File file : configDir.listFiles()) {
                if (!file.isFile() || !file.getName().endsWith(".yaml"))
                    continue;
                
                loadConfig(file);
            }
    }
    
    private Job loadConfig(File file) throws MException {
        log().d("Load configuration",file);
        IConfig config = M.l(IConfigFactory.class).read(file);
        Job job = new Job(this, config, file);
        jobs.add(job);
        return job;
    }

    public void push() {
        jobs.forEach(j -> j.push() );
    }
    
    public void pushAll() {
        jobs.forEach(j -> j.pushAll() );
    }
    

    
    public void watch() {
        
        Console console = Console.create();
        System.out.println(console.getClass());
        jobs.forEach(j -> j.startWatch() ); 
        try {
            while (true) {
                console.cleanup();
                console.clearTerminal();
                ConsoleTable table = new ConsoleTable();
                table.fitToConsole();
                table.setHeaderValues("Name", "Left","Watched","Transferred","Last update");
                
                
                jobs.forEach(j -> table.addRowValues( j.getName(), j.getFileToDoCnt(), j.getFileCnt(), j.getFiledTransferred(), j.getLastUpdate() ) );
                table.print();
                
                Thread.sleep(interval);
                
                for (Job job : new ArrayList<>( jobs )) {
                    if (job.isConfigFileRemoved()) {
                        log().i("config removed",job.getName());
                        System.out.println(job.getName() + " removed");
                        job.stopWatch();
                        jobs.remove(job);
                    } else
                    if (job.isConfigFileChanged()) {
                        log().i("config changed",job.getName());
                        System.out.println(job.getName() + " changed");
                        job.stopWatch();
                        jobs.remove(job);
                        try {
                            job = loadConfig(job.getConfigFile());
                            job.startWatch();
                        } catch (MException e) {
                            log().e(e);
                        }
                        
                    }
                }
                
            }
        } catch (InterruptedException e) {
            System.out.println("Exited");
        }

        jobs.forEach(j -> j.stopWatch() ); 
    }
    
    public void setArguments(MArgs margs) {
        this.args = margs;
    }
    
    public MArgs getArguments() {
        if (args == null) return new MArgs(null);
        return args;
    }

    public long getInterval() {
        return interval;
    }
    
}
