# ZooKeeper watcher app
Program watching for the creation of a specific ZooKeeper node (`/a`) and launching user-specified app (installed on local PC) when that node is created. When the node is deleted, the app is exited.  

### Additional features
1. When a descedant node is added to `/a`, the application recursively counts the number of such nodes and displays it.
2. Displaying a descedant tree of the `/a` node.