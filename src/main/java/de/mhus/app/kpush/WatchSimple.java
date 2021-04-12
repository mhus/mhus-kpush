package de.mhus.app.kpush;

import java.io.File;
import java.io.IOException;

import de.mhus.lib.core.MString;
import de.mhus.lib.core.MSystem;
import de.mhus.lib.core.MSystem.ScriptResult;
import de.mhus.lib.core.config.IConfig;
import de.mhus.lib.errors.MException;

public class WatchSimple extends Watch {

    long lastUpdated = 0;
    
    public WatchSimple(Job job, IConfig config) throws MException {
        super(job, config);
    }

    @Override
    public void init() {
                
        fileCnt = 0;
        long updateTime = System.currentTimeMillis();
        
        if (job.getConfig().getBoolean("ignoreInit", false)) {
            forEachSourceFile( (f,n) -> {
                if (f.lastModified() > lastUpdated) {
                    fileCnt++; 
                }
            });
            forEachSourceFile( (f,n) -> {
                if (f.lastModified() > lastUpdated) {
                    fileCnt--; 
                    log().i("Init",name,fileCnt,n);
                    try {
                        ScriptResult res = MSystem.execute(
                                "/usr/local/bin/kubectl",
                                "--namespace",job.getNamespace(), 
                                "-c", job.getContainer(), 
                                "exec", job.getPod(), "--", "ls", "-l", target + n);
    
                        log().d( res );
    //                    if (res.getError().contains("No such file or directory")) {
                        if (res.getRc() != 0) {
                            log().i("copy",n);
                            pushToK8s(f, n);
                        }
                        
                    } catch (IOException e) {
                        // will fail if directory not exists
                        
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            });
        }
        lastUpdated = updateTime;
    }
    
    @Override
    public void push() {
        fileCnt = 0;
        long updateTime = System.currentTimeMillis();
        forEachSourceFile( (f,n) -> {
            if (f.lastModified() > lastUpdated) {
                fileCnt++; 
            }
        });
        forEachSourceFile( (f,n) -> {
            if (f.lastModified() > lastUpdated) {
                fileCnt--; 
                log().i("Update",name,fileCnt,n);
                pushToK8s(f, n);
            }
        });
        lastUpdated = updateTime;
    }

    @Override
    public void pushAll() {
        fileCnt = 0;
        long updateTime = System.currentTimeMillis();
        forEachSourceFile( (f,n) -> {
            fileCnt++; 
        });
        forEachSourceFile( (f,n) -> {
            fileCnt--; 
            log().i("Update",name,fileCnt,n);
            pushToK8s(f,n);
        });
        lastUpdated = updateTime;
    }

    private void pushToK8s(File f, String n) {
        try {
            ScriptResult res = MSystem.execute(
                    "/usr/local/bin/kubectl",
                    "--namespace",job.getNamespace(), 
                    "-c", job.getContainer(), 
                    "cp", f.getAbsolutePath(),job.getPod() + ":" + target + n);
            
            log().d(res);
//                if (res.getError().contains("No such file or directory")) {
            if (res.getRc() != 0) {
                String dir = MString.beforeLastIndex(n, '/');
                log().i("mkdir",dir);
                ScriptResult res3 = MSystem.execute(
                        "/usr/local/bin/kubectl",
                        "--namespace",job.getNamespace(), 
                        "-c", job.getContainer(), 
                        "exec", job.getPod(), "--", "mkdir", "-p", target + dir);
                log().d( res3 );
                
                //create
                ScriptResult res4 = MSystem.execute(
                        "/usr/local/bin/kubectl",
                        "--namespace",job.getNamespace(), 
                        "-c", job.getContainer(), 
                        "cp", f.getAbsolutePath(),job.getPod() + ":" + target + n);
                log().d( res4 );
                
            }
            
        } catch (IOException e) {
            // will fail if directory not exists
            
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
     }
    
}
