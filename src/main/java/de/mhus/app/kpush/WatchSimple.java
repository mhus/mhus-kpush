package de.mhus.app.kpush;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import de.mhus.lib.core.M;
import de.mhus.lib.core.MString;
import de.mhus.lib.core.MSystem;
import de.mhus.lib.core.MSystem.ScriptResult;
import de.mhus.lib.core.config.IConfig;
import de.mhus.lib.errors.MException;

/**
 * This implementation will use the kubectl command cp and exec ls / mkdir to control the container.
 * It will not delete files if removed locally.
 * 
 * @author mikehummel
 *
 */
public class WatchSimple extends Watch {

    int cnt = 0;
    
    public WatchSimple(Job job, IConfig config) throws MException {
        super(job, config);
    }

    @Override
    public void init(long lastUpdated) {
                
        todoCnt = 0;
        if (!job.getConfig().getBoolean("ignoreInit", true)) {
            forEachSourceFile( (f,n) -> {
                cnt++;
                if (f.lastModified() > lastUpdated) {
                    todoCnt++; 
                }
            });
            fileCnt = cnt;
            forEachSourceFile( (f,n) -> {
                if (f.lastModified() > lastUpdated) {
                    todoCnt--; 
                    log().i("Init",name,todoCnt,n);
                    try {
                        List<String> cmd = kubectl();
                        cmd.add("exec");
                        cmd.add(job.getPod());
                        cmd.add("--");
                        cmd.add("ls");
                        cmd.add("-l");
                        cmd.add(target + n);
                        ScriptResult res = MSystem.execute(cmd.toArray(M.EMPTY_STRING_ARRAY));
    
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
    }
    
    @Override
    public void push(long lastUpdated) {
        todoCnt = 0;
        cnt = 0;
        forEachSourceFile( (f,n) -> {
            cnt++;
            if (f.lastModified() > lastUpdated) {
                todoCnt++; 
            }
        });
        fileCnt = cnt;
        forEachSourceFile( (f,n) -> {
            if (f.lastModified() > lastUpdated) {
                todoCnt--; 
                log().i("Update",name,todoCnt,n);
                pushToK8s(f, n);
            }
        });
    }

    @Override
    public void pushAll() {
        todoCnt = 0;
        forEachSourceFile( (f,n) -> {
            todoCnt++; 
        });
        forEachSourceFile( (f,n) -> {
            todoCnt--; 
            log().i("Update",name,todoCnt,n);
            pushToK8s(f,n);
        });
    }

    private void pushToK8s(File f, String n) {
        try {
            List<String> cmd = kubectl();
            cmd.add("cp");
            cmd.add(f.getAbsolutePath());
            cmd.add(job.getPod() + ":" + target + n);
            ScriptResult res = MSystem.execute(cmd.toArray(M.EMPTY_STRING_ARRAY));
            
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
                if (res3.getRc() != 0) {
                    log().e("can't create directory",target,dir);
                    return;
                }
                //create
                ScriptResult res4 = MSystem.execute(
                        "/usr/local/bin/kubectl",
                        "--namespace",job.getNamespace(), 
                        "-c", job.getContainer(), 
                        "cp", f.getAbsolutePath(),job.getPod() + ":" + target + n);
                log().d( res4 );
                if (res4.getRc() != 0) {
                    log().e("can't create file",target,n);
                    return;
                } else
                    fileTransferred++;
                
            } else
                fileTransferred++;
            
        } catch (IOException e) {
            // will fail if directory not exists
            
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
     }

    private List<String> kubectl() {
        LinkedList<String> cmd = new LinkedList<>();
        cmd.add( job.getConfig().getString("kubectl", "kubectl"));
        if (job.getNamespace() != null) {
            cmd.add("--namespace");
            cmd.add(job.getNamespace()); 
        }
        if (job.getContainer() != null) {
            cmd.add("-c");
            cmd.add(job.getContainer());
        }
        return cmd;
    }
    
}
