@echo off
echo Starting Node 2...
java -Dnode.id=node2 -Dserver.port=8092 -cp "target/classes" org.pr.dfs.node.SimpleHTTPNodeServer
pause
