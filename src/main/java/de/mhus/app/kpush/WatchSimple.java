package de.mhus.app.kpush;

import java.io.File;
import java.io.IOException;
import java.util.List;

import de.mhus.lib.core.M;
import de.mhus.lib.core.MString;
import de.mhus.lib.core.MSystem;
import de.mhus.lib.core.MSystem.ScriptResult;
import de.mhus.lib.core.node.INode;
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
    
    public WatchSimple(Job job, INode config) throws MException {
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
                    log().i(job,name,"Init",todoCnt,n);
                    try {
                        List<String> cmd = job.kubectl(true);
                        cmd.add("exec");
                        cmd.add(job.getPod());
                        cmd.add("--");
                        cmd.add("ls");
                        cmd.add("-l");
                        cmd.add(target + n);
                        log().d(job,name,"Execute",cmd);
                        ScriptResult res = MSystem.execute(cmd.toArray(M.EMPTY_STRING_ARRAY));
                        log().d(job,name,"Result", res );
    //                    if (res.getError().contains("No such file or directory")) {
                        if (res.getRc() != 0) {
                            log().i(job,name,"copy",n);
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
                log().i(job,name,"Push",todoCnt,n);
                pushToK8s(f, n);
            }
        });
    }

    @Override
    public void test(long lastUpdated) {
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
                log().i(job,name,"Test",todoCnt,n);
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
            log().i(job,name,"Push",todoCnt,n);
            pushToK8s(f,n);
        });
    }

    private boolean pushToK8s(File f, String n) {
        try {
            List<String> cmd = job.kubectl(true);
            cmd.add("cp");
            cmd.add(f.getAbsolutePath());
            cmd.add(job.getPod() + ":" + target + n);
            log().d(job,name,"Execute",cmd);
            ScriptResult res = MSystem.execute(cmd.toArray(M.EMPTY_STRING_ARRAY));
            log().d(job,name,"Result",res);
//                if (res.getError().contains("No such file or directory")) {
            if (res.getRc() != 0) {
                String dir = MString.beforeLastIndex(n, '/');
                log().i(job,name,"mkdir",dir);
                cmd = job.kubectl(true);
                cmd.add("exec");
                cmd.add(job.getPod());
                cmd.add("--");
                cmd.add("mkdir");
                cmd.add("-p");
                cmd.add(target + dir);
                log().d(job,name,"Execute",cmd);
                ScriptResult res3 = MSystem.execute(cmd.toArray(M.EMPTY_STRING_ARRAY));
                log().d(job,name,"Result", res3 );
                if (res3.getRc() != 0) {
                    log().e("can't create directory",target,dir);
                    fileErrors++;
                    return false;
                }
                //create
                cmd = job.kubectl(true);
                cmd.add("cp");
                cmd.add(f.getAbsolutePath());
                cmd.add(job.getPod() + ":" + target + n);
                log().d(job,name,"Execute",cmd);
                ScriptResult res4 = MSystem.execute(cmd.toArray(M.EMPTY_STRING_ARRAY));
                log().d(job,name,"Result", res4 );
                if (res4.getRc() != 0) {
                    log().e(job,name,"can't create file",target,n);
                    fileErrors++;
                    return false;
                } else
                    fileTransferred++;
                
            } else
                fileTransferred++;
            
        } catch (IOException e) {
            // will fail if directory not exists
            
            // TODO Auto-generated catch block
            e.printStackTrace();
            return false;
        }
        return true;
     }
    
}
