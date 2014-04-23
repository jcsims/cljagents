# cljagents

A small Clojure library for interacting with the
[Quagents](http://www.cs.rochester.edu/trac/quagents/wiki)
engine. Also includes several small examples, including some basic
core.logic examples.

## Usage

Communication with the engine happens over a socket (port 6000,
mostly), so you can run the compiled engine either natively or in a
virtual machine. I had the most luck during development with running
the engine in a Linux VM.

In it's current state, `cljagents` is designed to be used
interactively from a REPL. Once the engine is running (and the port is
available), the `user` namespace has multiple examples on using the
library. You'll always start with `start`, followed by either spawning
the bot with `cljagent.agent/spawn-bot` and controlling it manually,
or calling one of the `assignment` functions in the different problem
namespaces.

## License

The A* algorithm builds off Christophe Grand's excellent
[writeup](http://clj-me.cgrand.net/2010/09/04/a-in-clojure/)

Copyright © 2014 Chris Sims

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
