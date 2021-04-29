package de.mhus.app.kpush;

import java.io.File;
import java.util.List;
import java.util.function.BiConsumer;

import de.mhus.lib.core.MFile;
import de.mhus.lib.core.MLog;
import de.mhus.lib.core.node.INode;
import de.mhus.lib.errors.MException;

public abstract class Watch extends MLog {

    private static final int MAX_LEVEL = 1000;
    protected String source;
    protected String target;
    protected String name;
    protected Job job;
    private File sourceDir;
    protected int fileCnt;
    protected int todoCnt;
    private List<INode> filters;
    protected int fileTransferred;

    public Watch(Job job, INode config) throws MException {
        this.job = job;
        source = config.getString("source");
        target = config.getString("target");
        name = config.getString("name", source);
        sourceDir = MFile.toFile(source);
        if (config.isArray("filter"))
            filters = config.getObjectList("filter");
    }

    public abstract void init(long lastUpdateTime);
    
    public abstract void push(long lastUpdateTime);

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
            String path = fName + "/" + file.getName();
            if (!accepted(file, path))
                continue;
            if (file.isFile())
                action.accept(file, path);
            else
                forEachSourceFile(action, file, path, level+1);
        }
    }

    protected boolean accepted(File file, String path) {
        if (filters == null) return true;
        for (INode filter : filters) {
            String f = filter.getString("contains",null);
            if (f != null && path.contains(f)) return true;
            
            f = filter.getString("notcontains",null);
            if (f != null && path.contains(f)) return false;
            
            f = filter.getString("regex",null);
            if (f != null && path.matches(f)) return true;
            
            f = filter.getString("notregex",null);
            if (f != null && path.matches(f)) return false;
            
            f = filter.getString("namecontains",null);
            if (f != null && file.getName().matches(f)) return true;
            
            f = filter.getString("notnamecontains",null);
            if (f != null && file.getName().matches(f)) return false;
            
            f = filter.getString("dircontains",null);
            if (f != null && file.isDirectory() && file.getName().matches(f)) return true;
            
            f = filter.getString("notdircontains",null);
            if (f != null && file.isDirectory() && file.getName().matches(f)) return false;
            
        }
        if (file.isDirectory()) return true;
        return false;
    }

    public int getFileToDoCnt() {
        return todoCnt;
    }

    public int getFileCnt() {
        return fileCnt;
    }
    
    protected abstract void pushAll();

    protected int getFileTransferred() {
        return fileTransferred;
    }

}

