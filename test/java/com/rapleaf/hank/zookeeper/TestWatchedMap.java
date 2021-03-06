package com.rapleaf.hank.zookeeper;

import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.data.Stat;

import com.rapleaf.hank.ZkTestCase;
import com.rapleaf.hank.zookeeper.WatchedMap.CompletionAwaiter;
import com.rapleaf.hank.zookeeper.WatchedMap.CompletionDetector;
import com.rapleaf.hank.zookeeper.WatchedMap.ElementLoader;

public class TestWatchedMap extends ZkTestCase {
  private static final class StringElementLoader implements ElementLoader<String> {
    @Override
    public String load(ZooKeeperPlus zk, String basePath, String relPath) {
      try {
        return new String(zk.getData(ZkPath.append(basePath, relPath), false, new Stat()));
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  public void testIt() throws Exception {
    Logger.getLogger("org.apache.zookeeper").setLevel(Level.ALL);

    final ZooKeeperPlus zk = getZk();
    final String colRoot = ZkPath.append(getRoot(), "collection");
    zk.create(colRoot, null, Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
    final ElementLoader<String> elementLoader = new ElementLoader<String>() {
      @Override
      public String load(ZooKeeperPlus zk, String basePath, String relPath) {
        try {
          return new String(zk.getData(ZkPath.append(basePath, relPath), false, new Stat()));
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }
    };
    final WatchedMap<String> c1 = new WatchedMap<String>(zk, colRoot, elementLoader);
    dumpZk();

    assertEquals(0, c1.size());
    zk.create(ZkPath.append(colRoot, "first"), "data".getBytes(), Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
    Thread.sleep(1000);
    assertEquals(1, c1.size());
  }

  public void testCompletionDetector() throws Exception {
    final ElementLoader<String> elementLoader = new StringElementLoader();
    final AtomicBoolean b = new AtomicBoolean(false);
    CompletionDetector completionDetector = new CompletionDetector() {
      @Override
      public void detectCompletion(ZooKeeperPlus zk, String basePath, final String relPath, final CompletionAwaiter awaiter) throws KeeperException, InterruptedException {
        b.set(true);
        new Thread(new Runnable() {
          @Override
          public void run() {
            try {
              Thread.sleep(1500);
            } catch (InterruptedException e) {
              e.printStackTrace();
            }
            awaiter.completed(relPath);
          }
        }).start();
      }
    };
    final WatchedMap<String> m = new WatchedMap<String>(getZk(), getRoot(), elementLoader,
        completionDetector);
    // no elements yet, so should be empty
    assertEquals(0, m.size());
    // create an element
    getZk().create(ZkPath.append(getRoot(), "node"), "blah".getBytes(), Ids.OPEN_ACL_UNSAFE,
        CreateMode.PERSISTENT);
    // wait for notification to propagate
    Thread.sleep(1000);
    // the detector should have been invoked...
    assertTrue(b.get());
    // ...but it still shouldn't have come through to the actual map, since
    // there's a delay in the completion detector.
    assertEquals(0, m.size());
    // after waiting a bit, the completion detector should notify the awaiter
    Thread.sleep(2000);
    assertEquals(1, m.size());
  }

  public void testDeletion() throws Exception {
    getZk().create(ZkPath.append(getRoot(), "map"), null, Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
    getZk().create(ZkPath.append(getRoot(), "map/1"), "2".getBytes(), Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
    final WatchedMap<String> m = new WatchedMap<String>(getZk(), ZkPath.append(getRoot(), "map"), new StringElementLoader());
    assertEquals(new HashMap<String, String>() {{
      put("1", "2");
    }}, m);
    getZk().delete(ZkPath.append(getRoot(), "map/1"), 0);
    Thread.sleep(1000);
    assertEquals(Collections.EMPTY_MAP, m);
  }
}
