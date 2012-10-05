package localhost;
import java.util.Collection;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
public class Bad {
    void m() throws Exception {
        ScheduledFuture<String> s = null;
        Collection<ScheduledFuture<String>> queue = new DelayQueue<ScheduledFuture<String>>() {
            @Override public boolean offer(ScheduledFuture<String> e) {
                return e.isCancelled();
            }
        };
        queue.add(s);
        Executors.callable(new Runnable() {public void run() {}}).call();
    }
}
