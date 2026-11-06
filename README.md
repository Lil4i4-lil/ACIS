# Coursework in the discipline "Architecture of Computing and Information Systems"

Topic of work "DEVELOPMENT OF A MODEL OF A SINGLE-CHIP RISC PROCESSOR".

## Design assignment

The command system must satisfy the following requirements:

1. Memory access operations are separated from data processing operations. Operations related to data conversion are performed on the register ─ register principle.
2. In general, hardware supports operations on signed and unsigned integers, as well as on numbers represented in floating point (FP) format.
3. The command system must be functionally complete and include general purpose commands, commands for processing numbers with PT and privileged commands.
4. The processor must have a vectored interrupt system.
5. For processors with traditional (Princeton) architecture, mechanisms to support multitasking must be provided.

Data formats:

- 8-bit integers (signed and unsigned),
- 16-bit integers (signed and unsigned),
- 32-bit integers (signed and unsigned),
- 32-bit floating point numbers.

Number of registers: 64

Register width: 32

Register type: universal

Main memory: 4GB

Data bus width: 64

Address and data bus: combined (multiplexed AD)

Memory word transferred per bus cycle: 8 bytes. Registers and instructions stay 32-bit; the bus fetches two instructions in one 64-bit read (instruction prefetch) and uses byte enables for 8/16/32-bit stores. A multiplexed access takes two bus cycles: address phase, then data phase.

I/O organization method: isolated

Coprocessor: None
