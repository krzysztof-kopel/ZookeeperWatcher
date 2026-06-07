import org.apache.zookeeper.*;

import java.io.IOException;
import java.util.List;
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
            zooKeeper = new ZooKeeper("localhost:2181,localhost:2182,localhost:2183", 10000, app);

            countdown.await();
            System.out.println("Connection established.");
            zooKeeper.addWatch("/a", app, AddWatchMode.PERSISTENT_RECURSIVE);

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
        if (path != null) {
            if (watchedEvent.getType() == Watcher.Event.EventType.NodeCreated && path.equals("/a")) {
                System.out.println("Node created");
                startProcess();
            } else if (watchedEvent.getType() == Watcher.Event.EventType.NodeDeleted && path.equals("/a")) {
                System.out.println("Node deleted");
                stopProcess();
            } else if (watchedEvent.getType() == Event.EventType.NodeCreated && path.startsWith("/a")) {
                System.out.printf("Number of descendants: %d\n", this.countChildren("/a"));
                System.out.println("Descedant tree:");
                printTree("/a", 0);
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

    private int countChildren(String currentNode) {
        try {
            List<String> children = zooKeeper.getChildren(currentNode, false);
            int counter = children.size();
            return counter + children.stream()
                    .map(x -> countChildren(getChildPath(currentNode, x)))
                    .reduce(Integer::sum)
                    .orElse(0);
        } catch (KeeperException | InterruptedException e) {
            System.err.println("Exception: " + e.getMessage());
        }
        return 0;
    }

    private void printTree(String treeStart, int tabulation) {
        System.out.println("\t".repeat(tabulation) + treeStart);
        try {
            zooKeeper.getChildren(treeStart, false)
                    .forEach(x -> printTree(getChildPath(treeStart, x), tabulation + 1));
        } catch (KeeperException | InterruptedException e) {
            System.err.println("Exception: " + e.getMessage());
        }
    }

    private String getChildPath(String current, String child) {
        return current.endsWith("/") ? current + child : current + "/" + child;
    }
}
