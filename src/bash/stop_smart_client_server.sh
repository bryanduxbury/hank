for PID in `jps -l | grep com.rapleaf.hank.client.SmartClientDaemon | cut -d ' ' -f 1`; do
  echo "Killing $PID"
  kill $PID
done