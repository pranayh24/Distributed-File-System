@echo off
echo Starting Node 1...
java -Dnode.id=node1 -Dserver.port=8091 -cp "target/classes" org.pr.dfs.node.SimpleHTTPNodeServer
pause
