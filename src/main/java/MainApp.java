import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

public class MainApp implements org.apache.zookeeper.Watcher {

    private static final CountDownLatch countdown = new CountDownLatch(1);
    private static ZooKeeper zooKeeper;
    private static String appPath;
    private static Process externalProcess;

    public static void main(String[] args) {
        appPath = args[0];
        MainApp app = new MainApp();

        try {
            zooKeeper = new ZooKeeper("localhost:2181", 3000, app);

            countdown.await();
            System.out.println("Connection established.");

            zooKeeper.exists("/a", true);

            while (true) {
                Thread.sleep(Long.MAX_VALUE);
            }
        } catch (IOException | InterruptedException | KeeperException e) {
            System.err.println("Exception: " + e.getMessage());
        }

    }

    @Override
    public void process(WatchedEvent watchedEvent) {
        if (watchedEvent.getState() == Watcher.Event.KeeperState.SyncConnected) {
            if (watchedEvent.getType() == Watcher.Event.EventType.None) {
                countdown.countDown();
                return;
            }
        }

        String path = watchedEvent.getPath();
        if (path != null && path.equals("/a")) {
            if (watchedEvent.getType() == Watcher.Event.EventType.NodeCreated) {
                System.out.println("Node created");
                startProcess();
                setWatcher();
            } else if (watchedEvent.getType() == Watcher.Event.EventType.NodeDeleted) {
                System.out.println("Node deleted");
                stopProcess();
                setWatcher();
            }
        }
    }

    private void startProcess() {
        ProcessBuilder processBuilder = new ProcessBuilder(appPath);
        try {
            externalProcess = processBuilder.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void stopProcess() {
        if (externalProcess != null) {
            externalProcess.descendants().forEach(ProcessHandle::destroyForcibly);
            externalProcess.destroyForcibly();
            externalProcess = null;
        }
    }

    private void setWatcher() {
        try {
            zooKeeper.exists("/a", true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
