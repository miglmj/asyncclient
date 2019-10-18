# Introduction to osoclient

Asynchronoous client for a service running on localhost:5000, with a /squareme endpoint that accepts a query parameter called "num" and returns the square of num.

## Code structure

### core.clj

#### Data
Defines two parameters for execution.
- Upper bound for random number generation (set to 100)
- Lot size for number of requests to generate when none provided through command line arg.

#### Functions
- `generate-request`: accepts an optional parameter for number of requests to generate. Fires off n number of random requests using async client.

### cache.clj
#### Functions
##### Public API
###### `cache-set` 
Arguments:
- key
- value
- ttl (ms)
###### `cache-get` 
Arguments:
- key

Returns:
- value if it exists in cache and is valid, nil otherwise

### client.clj
#### Data
Sets up execution parameters and atoms for tracking execution state

#### Async Implementation
`client` sets up channels for communication between threads. There are functions that create either `go` blocks or threads to asynchronously read from a channel, perform an operation, and place their output in the appropriate output channel. I'm using the term "execution node" to describe them, and connecting them with channels to form a pipeline of operations.

Channels connect our execution nodes, so we set up 5 channels to handle different cases.
- `input queue`: We begin execution by putting numbers into this pipeline
- `need-request queue`: When we need a number to be requested from the server (i.e. there is no valid value in our cache for this number) we put it into this pipeline.
- `need-caching queue`: When we have a number and its square value, and we want to store it in cache, we write to this channel.
- `output queue`: When we want to output a number and its square to the user, we put it in this channel.
- `counting queue`: When we want to count the number of requests that have been handled, we write to this channel. (i.e. a listener on this channel will be able to listen on it in order to keep track of whether we're done with our load test)

The execution nodes are set up as below:

##### Execution Nodes
###### cache-checker
Checks cache, sends hits to output, sends misses to be queried from backend.

Arguments:
- input channel: Pull numbers to check from cache from here. Pass `input queue` as this parameter.
- hit-channel: broadcast any cache hits through this channel. Pass `output queue` as this parameter. Therefore, for cache hits, we skip the other execution nodes and go straight to printing.
- miss-channel: broadcast any cache misses through this channel. Pass `needs-request queue` so for any cache hits, we call our server for the square value.

##### query-handler
Makes async request on input, with callback.

Arguments:
- input channel: Pull numbers to query from here. Pass `needs-request queue` as this parameter.
- success channel: To be passed to callback function. Pass `needs-caching queue` as this parameter. Any hits will be sent to cache-setter to be stored in cache.
- failure channel: To be passed to callback function. Pass `input-queue` so any failures are retried.

##### response-handler
Callback function that is passed when making async request.

Arguments:
- success channel: Put succesful results here. Set to `needs-caching queue`.
- failure channel: Put failed queried numbers here. Set to `input queue`.

##### cache-setter
On input, saves values to cache.

Arguments:
- input channel: Gets values to save from this channel. Pass `needs-caching queue` as this parameter.
- output channel: passes along data to this channel. Pass `output queue` as this argument.

##### printer
Takes all input and prints to stdout.
This node could, for example, be replaced with one that saves to a db, keeps track of some state and sends an email at some condition, etc.

Arguments:
- input channel: takes input to print
- output channel: passes along output to this channel.

##### counter
Keeps a running count of each message it receives. When this count hits our total number of requests, prints some statistics.

Arguments:
- input-channel


