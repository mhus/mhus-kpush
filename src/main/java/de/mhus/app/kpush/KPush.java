package de.mhus.app.kpush;

import java.io.File;
import java.util.ArrayList;

import de.mhus.lib.core.M;
import de.mhus.lib.core.MLog;
import de.mhus.lib.core.MSystem;
import de.mhus.lib.core.config.IConfig;
import de.mhus.lib.core.config.IConfigFactory;
import de.mhus.lib.core.console.Console;
import de.mhus.lib.core.console.ConsoleTable;
import de.mhus.lib.errors.MException;

public class KPush extends MLog {

    private File configDir;
    private ArrayList<Job> jobs = new ArrayList<>();
    
    public KPush() throws MException {
        configDir = new File(MSystem.getUserHome(), "/.kpush/config");
        if (!configDir.exists())
            log().w("Config directory not found",configDir);
        loadConfig();
    }

    private void loadConfig() throws MException {
        for (File file : configDir.listFiles()) {
            if (!file.isFile() || !file.getName().endsWith(".yaml"))
                continue;
            
            log().d("Load configuration",file);
            IConfig config = M.l(IConfigFactory.class).read(file);
            Job job = new Job(config, file);
            jobs.add(job);
        }
    }
    
    public void push() {
        jobs.forEach(j -> j.init() );
        jobs.forEach(j -> j.push() );
    }
    
    public void watch() {
        
        Console console = Console.get();
        
        jobs.forEach(j -> j.startWatch() ); 
        
        try {
            while (true) {
                console.cleanup();
                ConsoleTable table = new ConsoleTable();
                table.setHeaderValues("Name", "Cnt");
                
                jobs.forEach(j -> table.addRowValues( j.getName(), j.getFileCnt() ) );
                table.print();
                
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            System.out.println("Exited");
        }

        jobs.forEach(j -> j.stopWatch() ); 
    }
    
    
}