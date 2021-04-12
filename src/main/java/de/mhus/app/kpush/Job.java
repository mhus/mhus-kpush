package de.mhus.app.kpush;

import java.io.File;
import java.util.ArrayList;

import de.mhus.lib.core.MLog;
import de.mhus.lib.core.MThread;
import de.mhus.lib.core.config.IConfig;
import de.mhus.lib.core.util.IntValue;
import de.mhus.lib.errors.MException;

public class Job extends MLog implements Runnable {

    private IConfig config;
    private String name;
    private String description;
    private File file;
    private long fileModify;
    private volatile MThread thread;
    private volatile boolean isRunning = true;
    private ArrayList<Watch> watches = new ArrayList<>();
    private String namespace;
    private String container;
    private String pod;

    public Job(IConfig config, File file) throws MException {
        this.config = config;
        this.file = file;
        this.fileModify = file.lastModified();
        name = config.getString("name");
        namespace = config.getString("namespace");
        container = config.getString("container");
        pod = config.getString("pod");
        description = config.getString("description", "");
        for (IConfig watchC : config.getObjectList("watch"))
            watches.add(new WatchDefault(this, watchC));
    }

    public void startWatch() {
        if (thread != null) {
            log().w("job already running");
            return;
        }
        isRunning = true;
        thread = new MThread(this);
        thread.start();
    }

    public void stopWatch() {
        if (thread == null) {
            log().w("job not running");
            return;
        }
        log().i("Stopping",name);
        isRunning = false;
        try {
            thread.getThread().join();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        thread = null;
        log().i("Stopped",name);
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public void run() {
        
        init();
        
        while (isRunning) {
            push();
            MThread.sleepForSure(1000);
        }
    }
    
    public void push() {
        for (Watch watch : watches)
            try {
                if (!isRunning) break;
                watch.push();
            } catch (Throwable t) {
                log().e(t,name,watch.getName());
            }
    }

    public void init() {
        for (Watch watch : watches)
            try {
                watch.init();
            } catch (Throwable t) {
                log().e(t,name,watch.getName());
            }
    }

    public String getNamespace() {
        return namespace;
    }

    public String getContainer() {
        return container;
    }

    public String getPod() {
        return pod;
    }
    
    public int getFileCnt() {
        IntValue fileCnt = new IntValue();
        watches.forEach(w -> fileCnt.value+=w.getFileCnt());
        return fileCnt.value;
    }

}
