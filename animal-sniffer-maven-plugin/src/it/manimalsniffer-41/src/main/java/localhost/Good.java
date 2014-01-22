package localhost;
import java.util.Collection;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.ScheduledFuture;

@JavaGenerationDependent
public class Good {
    void m() {
        ScheduledFuture<String> s = null;
        Collection<ScheduledFuture<String>> queue = new DelayQueue<ScheduledFuture<String>>() {
            @Override public boolean offer(ScheduledFuture<String> e) {
                return e.isCancelled();
            }
        };
        queue.add(s);
    }
}
