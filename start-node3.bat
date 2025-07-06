@echo off
echo Starting Node 3...
java -Dnode.id=node3 -Dserver.port=8093 -cp "target/classes" org.pr.dfs.node.SimpleHTTPNodeServer
pause
