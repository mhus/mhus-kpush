package de.mhus.app.kpush;

import java.io.File;
import java.util.function.BiConsumer;

import de.mhus.lib.core.MLog;
import de.mhus.lib.core.config.IConfig;
import de.mhus.lib.errors.MException;

public abstract class Watch extends MLog {

    private static final int MAX_LEVEL = 1000;
    protected String source;
    protected String target;
    protected String name;
    protected Job job;
    private File sourceDir;
    protected int fileCnt;

    public Watch(Job job, IConfig config) throws MException {
        this.job = job;
        source = config.getString("source");
        target = config.getString("target");
        name = config.getString("name", source);
        sourceDir = new File(source);
    }

    public abstract void init();
    
    public abstract void push();

    public String getName() {
        return name;
    }

    protected void forEachSourceFile(BiConsumer<File, String> action) {
        if (!sourceDir.exists() || !sourceDir.isDirectory())
            return;
        forEachSourceFile(action, sourceDir, "", 0);
    }
    
    private void forEachSourceFile(BiConsumer<File, String> action, File dir, String fName, int level) {
        if (level > MAX_LEVEL) {
            log().e("max level reached",name);
            return;
        }
        if (!sourceDir.exists()) {
            return; // should not happen
        }
        
        for (File file : dir.listFiles()) {
            if (!accepted(file))
                continue;
            if (file.isFile())
                action.accept(file, fName + "/" + file.getName());
            else
                forEachSourceFile(action, file, fName + "/" + file.getName(), level+1);
        }
    }

    protected boolean accepted(File file) {
        // TODO implement filtering
        return true;
    }

    public int getFileCnt() {
        return fileCnt;
    }

    protected abstract void pushAll();

}

