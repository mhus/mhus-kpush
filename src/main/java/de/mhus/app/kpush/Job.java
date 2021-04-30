package de.mhus.app.kpush;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;

import de.mhus.lib.core.MDate;
import de.mhus.lib.core.MFile;
import de.mhus.lib.core.MLog;
import de.mhus.lib.core.MProperties;
import de.mhus.lib.core.MThread;
import de.mhus.lib.core.node.INode;
import de.mhus.lib.core.util.IntValue;
import de.mhus.lib.errors.MException;

public class Job extends MLog implements Runnable {

    long lastUpdated = System.currentTimeMillis();
    private File lastUpdatedFile;
        
    private INode config;
    private String name;
    private String description;
    private File cfgFile;
    private long cfgFileModify;
    private volatile MThread thread;
    private volatile boolean isRunning = true;
    private ArrayList<Watch> watches = new ArrayList<>();
    private String namespace;
    private String container;
    private String pod;
    private KPush kpush;
    private long interval;
    private volatile Date lastUpdateStart;
    private boolean disabled = false;

    public Job(KPush kpush, INode config, File file) throws MException {
        this.kpush = kpush;
        this.config = config;
        this.cfgFile = file;
        this.cfgFileModify = file.lastModified();
        name = config.getString("name", MFile.getFileNameOnly(file.getName()).toUpperCase() );
        disabled = config.getBoolean("disabled", false);
        namespace = config.getString("namespace", null);
        container = config.getString("container", null);
        pod = config.getString("pod");
        description = config.getString("description", "");
        for (INode watchC : config.getObjectList("watch"))
            watches.add(WatchFactory.create(this, watchC));
        interval = config.getLong("interval", kpush.getInterval());
        
        lastUpdatedFile = new File(getConfigFile().getParent(), MFile.getFileNameOnly( "kpush." + getConfigFile().getName() ) + ".dat" );
        if (getKPush().getArguments().contains("r"))
            lastUpdated = 0;
        else
            loadLastUpdated();
        
    }

    public void startWatch() {
        if (thread != null) {
            log().w("job already running",this);
            return;
        }
        isRunning = true;
        thread = new MThread(this);
        thread.start();
    }

    public void stopWatch() {
        if (thread == null) {
            log().w("job not running",this);
            return;
        }
        log().i("Stopping",this);
        isRunning = false;
        try {
            thread.getThread().join();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        thread = null;
        log().i("Stopped",this);
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
            MThread.sleepForSure(disabled ? 30000 : interval);
        }
    }
    
    public void pushAll() {
        if (disabled) {
            log().i(this,"disabled");
            return;
        }
        lastUpdateStart = new Date();
        for (Watch watch : watches)
            try {
                if (!isRunning) break;
                if (!watch.isDisabled()) {
                    log().i(this,watch,"pushAll");
                    watch.pushAll();
                }
            } catch (Throwable t) {
                log().e(t,this,watch.getName());
            }
        lastUpdated = lastUpdateStart.getTime();
        saveLastUpdated();
    }
   
    public void push() {
        if (disabled) {
            log().i(this,"disabled");
            return;
        }
        lastUpdateStart = new Date();
        
        for (Watch watch : watches)
            try {
                if (!isRunning) break;
                if (!watch.isDisabled()) {
                    log().i(this,watch,"push");
                    watch.push(lastUpdated);
                } else
                    log().i(this,watch,"disabled");
            } catch (Throwable t) {
                log().e(t,name,this,watch.getName());
            }
        lastUpdated = lastUpdateStart.getTime();
        saveLastUpdated();
    }

    public void init() {
        if (disabled) {
            log().i(this,"disabled init");
            return;
        }
        for (Watch watch : watches)
            try {
                if (!watch.isDisabled()) {
                    log().i(this,watch,"init");
                    watch.init(lastUpdated);
                }
            } catch (Throwable t) {
                log().e(t,this,watch.getName());
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
    
    public int getFileToDoCnt() {
        IntValue fileCnt = new IntValue();
        watches.forEach(w -> fileCnt.value+=w.getFileToDoCnt());
        return fileCnt.value;
    }

    public int getFileCnt() {
        IntValue fileCnt = new IntValue();
        watches.forEach(w -> fileCnt.value+=w.getFileCnt());
        return fileCnt.value;
    }
    
    public INode getConfig() {
        return config;
    }
    
    public boolean isConfigFileChanged() {
        if (isConfigFileRemoved()) return true;
        return cfgFile.lastModified() != cfgFileModify;
    }
    
    public boolean isConfigFileRemoved() {
        return !cfgFile.exists() && !cfgFile.isFile();
    }

    public File getConfigFile() {
        return cfgFile;
    }

    public KPush getKPush() {
        return kpush;
    }
    
    public Date getLastUpdate() {
        return lastUpdateStart;
    }

    private void loadLastUpdated() {
        if (!getConfig().getBoolean("rememberLastUpdated", true)) return;
        if (lastUpdatedFile.exists() && lastUpdatedFile.isFile()) {
            try {
                log().d("load lastUpdated from",this,lastUpdatedFile);
                MProperties p = MProperties.load(lastUpdatedFile);
                lastUpdated = p.getLong("lastUpdated", lastUpdated);
                log().d("last update",this,MDate.toIsoDateTime(lastUpdated));
            } catch (Throwable t) {
                log().w(t,this);
            }
        }
    }
    
    private void saveLastUpdated() {
        if (!getConfig().getBoolean("rememberLastUpdated", true)) return;
        try {
            log().d("save lastUpdated to",this,lastUpdatedFile,MDate.toIsoDateTime(lastUpdated));
            MProperties p = new MProperties();
            p.setLong("lastUpdated", lastUpdated);
            p.save(lastUpdatedFile);
        } catch (Throwable t) {
            log().w(t,this);
        }
    }

    public long getFileTransferred() {
        IntValue fileCnt = new IntValue();
        watches.forEach(w -> fileCnt.value+=w.getFileTransferred());
        return fileCnt.value;
    }

    public long getFileErrors() {
        IntValue fileCnt = new IntValue();
        watches.forEach(w -> fileCnt.value+=w.getFileErrors());
        return fileCnt.value;
    }
    
    public boolean isDisabled() {
        return disabled;
    }
    
    @Override
    public String toString() {
        return name;
    }

    public void touchTime(long time) {
        lastUpdated = time;
        saveLastUpdated();
    }
        
}
