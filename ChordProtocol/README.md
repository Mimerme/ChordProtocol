# Chord Protocol Implementation

- A shitty implementation of the chord protocol in Java.
- ```Node.java``` is the core of the code
  - ```LocalNodes``` make RPC calls to other ```ChordNodes``` as specified in the original chord paper
  - https://people.eecs.berkeley.edu/~istoica/papers/2003/chord-ton.pdf
    - Code commented with ```//verify``` derive from the original psuedocode on Fig 6 and Fig. 5 an may not be 100% correct
- Might be useful as a boilerplate for future P2P applications