[![Open in Visual Studio Code](https://classroom.github.com/assets/open-in-vscode-2e0aaae1b6195c2367325f4f02e2d04e9abb55f0b24a779b69b11b9e10269abc.svg)](https://classroom.github.com/online_ide?assignment_repo_id=19663142&assignment_repo_type=AssignmentRepo)

# Distance Vector Algorithm Pseudocode

## Main Program Structure
```
PROGRAM DistanceVector:
    WHILE input is not finished:
        // Phase 1: Read topology information
        routers = READ_ROUTER_NAMES()
        initial_topology = READ_INITIAL_TOPOLOGY()
        
        // Initialize and run DV algorithm
        INITIALIZE_DISTANCE_TABLES(routers, initial_topology)
        RUN_DV_ALGORITHM()
        PRINT_FINAL_ROUTING_TABLES()
        
        // Phase 2: Handle topology updates
        updates = READ_TOPOLOGY_UPDATES()
        IF updates is not empty:
            UPDATE_TOPOLOGY(updates)
            RUN_DV_ALGORITHM()
            PRINT_FINAL_ROUTING_TABLES()
```

## Data Structure Definitions
```
STRUCTURE Router:
    name: String
    neighbors: Map<String, Integer>  // neighbor name -> direct link cost
    distance_table: Map<String, Map<String, Integer>>  // destination -> (next hop -> distance)
    routing_table: Map<String, RouteEntry>  // destination -> route entry

STRUCTURE RouteEntry:
    destination: String
    next_hop: String
    distance: Integer
```

## Core Algorithm Functions

### Initialization Function
```
FUNCTION INITIALIZE_DISTANCE_TABLES(routers, topology):
    FOR each router in routers:
        // Initialize distance table
        FOR each destination in routers:
            IF destination == router.name:
                CONTINUE
            
            router.distance_table[destination] = {}
            FOR each neighbor in router.neighbors:
                IF neighbor == destination AND direct link exists:
                    router.distance_table[destination][neighbor] = direct link cost
                ELSE:
                    router.distance_table[destination][neighbor] = INF
```

### Distance Vector Algorithm Main Loop
```
FUNCTION RUN_DV_ALGORITHM():
    t = 0
    converged = FALSE
    
    WHILE NOT converged:
        // Print distance tables at current step
        PRINT_DISTANCE_TABLES_AT_STEP(t)
        
        // Check for convergence
        IF t > 0 AND NO_CHANGES_IN_LAST_ITERATION():
            converged = TRUE
            BREAK
        
        // Prepare update messages
        updates = PREPARE_UPDATES()
        
        // All routers simultaneously process received updates
        FOR each router in routers:
            PROCESS_UPDATES(router, updates)
        
        // Update routing tables
        UPDATE_ROUTING_TABLES()
        
        t = t + 1
```

### Prepare Update Messages
```
FUNCTION PREPARE_UPDATES():
    updates = {}
    
    FOR each router in routers:
        updates[router.name] = {}
        
        // Send distance vector to each neighbor
        FOR each neighbor in router.neighbors:
            distance_vector = {}
            
            FOR each destination in routers:
                IF destination != router.name:
                    best_distance = GET_BEST_DISTANCE(router, destination)
                    distance_vector[destination] = best_distance
            
            updates[router.name][neighbor] = distance_vector
    
    RETURN updates
```

### Process Received Updates
```
FUNCTION PROCESS_UPDATES(router, all_updates):
    changed = FALSE
    
    FOR each neighbor in router.neighbors:
        IF neighbor exists in all_updates:
            received_vector = all_updates[neighbor][router.name]
            
            FOR each destination in received_vector:
                IF destination != router.name:
                    // Calculate new distance via this neighbor
                    new_distance = router.neighbors[neighbor] + received_vector[destination]
                    
                    // Update distance table
                    IF new_distance < router.distance_table[destination][neighbor]:
                        router.distance_table[destination][neighbor] = new_distance
                        changed = TRUE
    
    RETURN changed
```

### Get Best Distance
```
FUNCTION GET_BEST_DISTANCE(router, destination):
    min_distance = INF
    
    // Check all next hop options in alphabetical order
    FOR each next_hop in SORTED(router.distance_table[destination].keys()):
        distance = router.distance_table[destination][next_hop]
        IF distance < min_distance:
            min_distance = distance
    
    RETURN min_distance
```

### Update Routing Tables
```
FUNCTION UPDATE_ROUTING_TABLES():
    FOR each router in routers:
        FOR each destination in routers:
            IF destination != router.name:
                best_distance = INF
                best_next_hop = NULL
                
                // Choose best next hop in alphabetical order
                FOR each next_hop in SORTED(router.distance_table[destination].keys()):
                    distance = router.distance_table[destination][next_hop]
                    
                    IF distance < best_distance:
                        best_distance = distance
                        best_next_hop = next_hop
                    ELSE IF distance == best_distance AND next_hop < best_next_hop:
                        best_next_hop = next_hop  // Alphabetical order priority
                
                // Update routing table entry
                router.routing_table[destination] = RouteEntry{
                    destination: destination,
                    next_hop: best_next_hop,
                    distance: best_distance
                }
```

## Input Processing Functions

### Read Router Names
```
FUNCTION READ_ROUTER_NAMES():
    routers = []
    
    WHILE TRUE:
        line = READ_LINE()
        IF line == "START":
            BREAK
        routers.APPEND(line.trim())
    
    RETURN SORTED(routers)  // Sort in alphabetical order
```

### Read Initial Topology
```
FUNCTION READ_INITIAL_TOPOLOGY():
    topology = {}
    
    WHILE TRUE:
        line = READ_LINE()
        IF line == "UPDATE":
            BREAK
        
        parts = line.split()
        router1 = parts[0]
        router2 = parts[1]
        weight = INTEGER(parts[2])
        
        IF weight == -1:
            // Remove link
            REMOVE_LINK(topology, router1, router2)
        ELSE:
            // Add or update link
            ADD_LINK(topology, router1, router2, weight)
    
    RETURN topology
```

### Read Topology Updates
```
FUNCTION READ_TOPOLOGY_UPDATES():
    updates = {}
    
    WHILE TRUE:
        line = READ_LINE()
        IF line == "END":
            BREAK
        
        parts = line.split()
        router1 = parts[0]
        router2 = parts[1]
        weight = INTEGER(parts[2])
        
        updates[router1 + "-" + router2] = weight
    
    RETURN updates
```

## Output Functions

### Print Distance Tables
```
FUNCTION PRINT_DISTANCE_TABLES_AT_STEP(t):
    FOR each router in SORTED(routers):
        PRINT "Distance Table of router " + router.name + " at t=" + t + ":"
        
        // Print table header
        PRINT "     "
        FOR each destination in SORTED(other_routers):
            PRINT destination + "    "
        PRINT NEWLINE
        
        // Print each row
        FOR each next_hop in SORTED(router.neighbors):
            PRINT next_hop + "    "
            FOR each destination in SORTED(other_routers):
                distance = router.distance_table[destination][next_hop]
                IF distance == INF:
                    PRINT "INF  "
                ELSE:
                    PRINT distance + "   "
            PRINT NEWLINE
        
        PRINT NEWLINE  // Blank line separator
```

### Print Routing Tables
```
FUNCTION PRINT_FINAL_ROUTING_TABLES():
    FOR each router in SORTED(routers):
        PRINT "Routing Table of router " + router.name + ":"
        
        FOR each destination in SORTED(other_routers):
            route = router.routing_table[destination]
            IF route.distance == INF:
                PRINT destination + ",INF,INF"
            ELSE:
                PRINT destination + "," + route.next_hop + "," + route.distance
        
        PRINT NEWLINE  // Blank line separator
```
