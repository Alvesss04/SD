Assignment 2

Deadlines

2nd Assignment (online - code + form):

Submission date: May 26th, 23h59, with 1 hour tolerance.
Submission form: https://forms.gle/WMjbzDUjJQB2sxmc9
Objective

The objective of the second assignment is to extend the first assignment with the following features:

Security;
Interaction with external services;
Fault tolerance - you will implement fault tolerance of the Messages server.
The design of the solution, including the architecture and the service interfaces remains identical to those of the first assignment, except for the indications to the contrary in this specification.

Compatibility with the pre-defined interfaces and operations must be observed.

Features

Security (max: 4 points)

“The design of the solution, including its architecture and service interfaces, remains the same as the first assignment unless this specification indicates otherwise.”

The objective of this feature is to make the system secure, preventing unauthorized elements from executing operations on the Messages service and Users service. In the original design, client operations already required a password to perform the indicated operation. To enforce security, the solution must include the following use of secure channels.

In client/server interactions, clients will authenticate servers relying on servers’ public key certificates. Servers will authenticate clients checking clients’ passwords (already included in the original design).

In server/server interactions (e.g. when forwarding messages between Messages servers), it is also necessary to guarantee that the server starting the connection is authenticated. This can be done by sending shared secret between the servers, which can be passed as a parameter when starting the program. This ensures that certain operations can only be executed between servers and cannot be invoked by clients.

Interaction with an external service (option E1) (max: 6 points)

The objective of this feature is to implement a Messages server (respecting the Messages Service API) that interacts with an external email service via a REST API with OAuth authentication. The use of the Zoho Mail service is suggested.

This new Messages server will interact with the other components of the system, in a similar way as a Messages server in the first project. To this end, it must implement the Message REST interface, and be able to access other Messages servers in other domains and the Users server in its domain. The only difference is that, instead of storing the inbox in a local database using Hibernate, it will use the external service to store the inbox. A message can be stored in the user mailbox by sending an email to the Zoho Mail account it is been used. The properties of the Message - id, sender, destination, creationTime - in the message body. Sugestion: append the properties in the end of the contents, using a separator (e.g. ------).

In the context of the system being developed, this messages server, belonging to a domain, will have a single user.

For the implementation of this feature, we suggest the use of the ScribeJava library, as presented in the practical classes (see lab 9).

NOTE: A single server of this type will be launched in the system - no need to handle concurrent accesses to the external service from multiple servers.

NOTE: To allow for testing this service automatically, using the Tester, it is necessary to start with a clean state, i.e., with an empty mailbox. To achieve this, the Tester will pass as the first parameter of this Messages server the value true to indicate that the previous state should be ignored. If the Tester passes the value false, the saved state should be used by the server.

NOTE: This server must expose the Messages REST interface.

Fault tolerance - messages server (alternative F1 - Kafka) (max: 8 points)

Implement a solution that allows tolerating failures in a machine running a messages server in a domain, by replicating the messages server with a state machine replication solution, using an indirect communication system - e.g. Kafka.

The solution should allow any domain to be replicated. More than one domain may be replicated in the system. The solution must tolerate the failure of any messages server that is being replicated.

The solution must ensure that a client always reads the state of a server that has a version as up-to-date as the version of a server which was previously accessed (i.e., if a client reads version 2 from Server X, from then on they must read version >=2 from any Server X’). To achieve this, the server may add headers to the responses sent to the clients, which will be sent in any following operations executed by the same client (the Tester will resend all headers starting with X-MESSAGES).

NOTE: A single Kafka server will be launched in the Tester environment. Kafka CANNOT be used for inter-domain communication/replication; it may only be used for replication of the messages server within a domain.

Fault tolerance - messages server (alternative F2a - primary/secondary) (max: 10 points)

Implement a solution that allows tolerating failures in a machine running a messages server, by replicating the messages server with a state machine replication solution, implementing the primary/secondary protocol. The solution must tolerate the failure of any server. In case the primary server fails, the system must ensure that it is still possible to perform reads, but it does not need to allow the execution of writes.

The solution must allow replicating the messages server in different domains.

The solution must ensure that a client always reads the state of a server that has a version as up-to-date as the version of a server which was previously accessed. To achieve this, the server can add headers to the responses sent to the clients, which will be sent in any following operations executed by the same client (the Tester will resend all headers starting with X-MESSAGES).

Note: The program must support the failure of 1 server, with the protocols implemented configured for this.

NOTE: This server must expose a REST interface.

Tests: TBD.

Fault tolerance - messages server (alternative F2b - primary/secondary, no mask of primary faults) (max: 7 points)

Implement a solution that allows tolerating failures in a machine running a messages server in a domain, replicating the messages server with a state machine replication solution, implementing the primary/secondary protocol.

The solution should allow to replicate any domain, with more than one domain being replicated in the system. The solution must tolerate the failure of any secondary server that is being replicated (but it does not need to tolerate the fault of the primary); in case of failure of the primary server, the system must ensure that it is still possible to perform reads, but it does not need to allow the execution of writes.

The solution must ensure that a client always reads the state of a server that has a version as up-to-date as the version of a server which was previously accessed. To achieve this, the server can add headers to the responses sent to the clients, which will be sent in any following operations executed by the same client (the Tester will resend all headers starting with X-MESSAGES).

Note: The program must support the failure of 1 server, with the protocols implemented configured for this.

Depreciation Factors

The submitted code should follow good programming practices. Unnecessary code repetition, inclusion of magic constants, use of inadequate data structures, etc., may incur a penalty. (max: 2 points)

Lack of robustness and erratic behavior of the solution are grounds for penalty.

NOTE: The solution should continue to account for temporary communication channel failures.

Execution

The assignment can be done in groups of 1 or 2 students. Students in the same group do not need to be in the same practical class, although this is strongly recommended.

Groups in this second assignment may be different from the first assignment.

As a base for this second assignment, students can use their implementation of the first assignment or one of the implementations available at the following links, provided as such:

https://github.com/preguica/sd2526.trab1sol.v1

https://github.com/preguica/sd2526.trab1sol.v2

Evaluation

The assignment evaluation will take into account the following criteria:

Developed features and their conformity with the specification, based on the results of the automatic test suite;
Quality of the solution;
Quality of the developed code.
The student’s final grade is individual and will be less than or equal to the assignment grade, depending on the discussion of the assignment, to be held following the submission of the assignment.

Test Suite

The test suite intended to verify the conformity of the solution with the specification is available at Tester.

Development Environment

All provided support material assumes development in a Linux environment with Java 17. Validation of the assignment through the automatic test suite will use Docker technology.

Change History

24/4/24

Specification

Initial version.