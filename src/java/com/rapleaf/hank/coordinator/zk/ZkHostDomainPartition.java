/**
 *  Copyright 2011 Rapleaf
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.rapleaf.hank.coordinator.zk;

import com.rapleaf.hank.coordinator.AbstractHostDomainPartition;
import com.rapleaf.hank.coordinator.DataLocationChangeListener;
import com.rapleaf.hank.zookeeper.*;
import org.apache.zookeeper.KeeperException;

import java.io.IOException;

public class ZkHostDomainPartition extends AbstractHostDomainPartition {

  private static final String CURRENT_VERSION_PATH_SEGMENT = "current_version";
  private static final String DELETABLE_PATH_SEGMENT = "selected_for_deletion";
  private final String path;
  private final int partNum;
  private final ZooKeeperPlus zk;

  private final WatchedInt currentDomainGroupVersion;
  private final WatchedBoolean deletable;
  private final DataLocationChangeListener dataLocationChangeListener;

  private static boolean doUsePartitionWatches = true;

  public static void setDoUsePartitionWatches(boolean value) {
    doUsePartitionWatches = value;
  }

  public static ZkHostDomainPartition create(ZooKeeperPlus zk,
                                             String domainPath,
                                             int partNum,
                                             DataLocationChangeListener dataLocationChangeListener) throws IOException {
    try {
      String hdpPath = ZkPath.append(domainPath, Integer.toString(partNum));
      zk.create(hdpPath, null);
      zk.create(ZkPath.append(hdpPath, CURRENT_VERSION_PATH_SEGMENT), null);
      zk.create(ZkPath.append(hdpPath, DELETABLE_PATH_SEGMENT), Boolean.FALSE.toString().getBytes());
      zk.create(ZkPath.append(hdpPath, DotComplete.NODE_NAME), null);

      return new ZkHostDomainPartition(zk, hdpPath, dataLocationChangeListener);
    } catch (Exception e) {
      throw new IOException(e);
    }
  }

  public ZkHostDomainPartition(ZooKeeperPlus zk, String path, DataLocationChangeListener dataLocationChangeListener)
      throws KeeperException, InterruptedException {
    this.zk = zk;
    this.path = path;
    this.partNum = Integer.parseInt(ZkPath.getFilename(path));
    this.dataLocationChangeListener = dataLocationChangeListener;

    if (doUsePartitionWatches) {
      currentDomainGroupVersion = new WatchedInt(zk, ZkPath.append(path, CURRENT_VERSION_PATH_SEGMENT), true);
      deletable = new WatchedBoolean(zk, ZkPath.append(path, DELETABLE_PATH_SEGMENT), true);
      deletable.addListener(new DeletableListener());
    } else {
      currentDomainGroupVersion = null;
      deletable = null;
    }
  }

  private class DeletableListener implements WatchedNodeListener<Boolean> {

    @Override
    public void onWatchedNodeChange(Boolean value) {
      fireDataLocationChangeListener();
    }
  }

  @Override
  public Integer getCurrentDomainGroupVersion() throws IOException {
    if (currentDomainGroupVersion != null) {
      return currentDomainGroupVersion.get();
    } else {
      try {
        return WatchedInt.get(zk, ZkPath.append(path, CURRENT_VERSION_PATH_SEGMENT));
      } catch (Exception e) {
        throw new IOException(e);
      }
    }
  }

  @Override
  public int getPartitionNumber() {
    return partNum;
  }

  @Override
  public boolean isDeletable() throws IOException {
    Boolean result;
    if (deletable != null) {
      result = deletable.get();
    } else {
      try {
        result = WatchedBoolean.get(zk, ZkPath.append(path, DELETABLE_PATH_SEGMENT));
      } catch (Exception e) {
        throw new IOException(e);
      }
    }
    if (result == null) {
      return true;
    } else {
      return result;
    }
  }

  @Override
  public void setDeletable(boolean deletable) throws IOException {
    try {
      this.deletable.set(deletable);
    } catch (Exception e) {
      throw new IOException(e);
    }
  }

  @Override
  public void setCurrentDomainGroupVersion(Integer version) throws IOException {
    try {
      currentDomainGroupVersion.set(version);
    } catch (Exception e) {
      throw new IOException(e);
    }
  }

  public void delete() throws IOException {
    try {
      zk.delete(ZkPath.append(path, DotComplete.NODE_NAME), -1);
      zk.deleteNodeRecursively(path);
    } catch (InterruptedException e) {
      throw new IOException(e);
    } catch (KeeperException e) {
      throw new IOException(e);
    }
  }

  public String getPath() {
    return path;
  }

  private void fireDataLocationChangeListener() {
    if (dataLocationChangeListener != null) {
      dataLocationChangeListener.onDataLocationChange();
    }
  }
}
